package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.dto.SkuStockUpdateDTO;
import com.sky.entity.ProductSku;
import com.sky.entity.OrderDetail;
import com.sky.entity.StockLog;
import com.sky.exception.ProductBusinessException;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.ProductSkuMapper;
import com.sky.mapper.StockLogMapper;
import com.sky.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.core.io.ClassPathResource;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class StockServiceImpl implements StockService {

    @Autowired
    private ProductSkuMapper productSkuMapper;
    @Autowired
    private StockLogMapper stockLogMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<Long> deductStockScript;

    @PostConstruct
    public void init() {
        deductStockScript = new DefaultRedisScript<>();
        deductStockScript.setLocation(new ClassPathResource("lua/deductStock.lua"));
        deductStockScript.setResultType(Long.class);

        // 应用启动时把上架 SKU 的数据库库存同步到 Redis，避免缓存 key 不存在。
        try {
            List<ProductSku> skuList = productSkuMapper.listOnSale();
            if (skuList != null) {
                skuList.forEach(sku -> stringRedisTemplate.opsForValue()
                        .set(stockKey(sku.getId()), String.valueOf(sku.getStock())));
            }
        } catch (Exception ex) {
            log.warn("SKU库存Redis预热失败，后续下单将按Redis异常降级数据库扣减", ex);
        }
    }

    public boolean reserveStock(Long skuId, Integer count) {
        validateCount(count);
        try {
            Long result = stringRedisTemplate.execute(
                    deductStockScript,
                    Collections.singletonList(stockKey(skuId)),
                    String.valueOf(count));
            if (result == null || result == 0L) {
                throw new ProductBusinessException(MessageConstant.STOCK_NOT_ENOUGH);
            }
            return true;
        } catch (ProductBusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            // Redis 异常时降级走数据库扣减
            return false;
        }
    }

    public void restoreReservedStock(Long skuId, Integer count) {
        validateCount(count);
        try {
            stringRedisTemplate.opsForValue().increment(stockKey(skuId), count);
        } catch (Exception ex) {
            // 下单失败后的 Redis 补偿失败，只记录日志，后续可人工/定时补偿
            // 当前阶段不抛出，避免影响主流程
            ex.printStackTrace();
        }
    }

    @Transactional
    public void deductStock(Long skuId, Integer count, Long orderId) {
        validateCount(count);

        int affectedRows = productSkuMapper.deductStock(skuId, count);
        if (affectedRows == 0) {
            throw new ProductBusinessException(MessageConstant.STOCK_NOT_ENOUGH);
        }

        ProductSku sku = getSkuOrThrow(skuId);
        StockLog stockLog = StockLog.builder()
                .skuId(skuId)
                .businessType(StockLog.DEDUCT)
                .bizId(orderId)
                .changeCount(count)
                .beforeStock(sku.getStock() + count)
                .afterStock(sku.getStock())
                .remark("订单扣减库存")
                .createTime(LocalDateTime.now())
                .build();
        stockLogMapper.insert(stockLog);
        syncRedisStock(skuId, sku.getStock());
    }

    @Transactional
    public void restoreStock(Long orderId, Long skuId, Integer count) {
        validateCount(count);

        int affectedRows = productSkuMapper.restoreStock(skuId, count);
        if (affectedRows == 0) {
            throw new ProductBusinessException(MessageConstant.PRODUCT_SKU_NOT_FOUND);
        }

        ProductSku sku = getSkuOrThrow(skuId);
        StockLog stockLog = StockLog.builder()
                .skuId(skuId)
                .businessType(StockLog.RESTORE)
                .bizId(orderId)
                .changeCount(count)
                .beforeStock(sku.getStock() - count)
                .afterStock(sku.getStock())
                .remark("订单回补库存")
                .createTime(LocalDateTime.now())
                .build();
        stockLogMapper.insert(stockLog);

        syncRedisStock(skuId, sku.getStock());
    }

    @Transactional
    public void setStock(SkuStockUpdateDTO stockUpdateDTO) {
        if (stockUpdateDTO == null || stockUpdateDTO.getSkuId() == null) {
            throw new ProductBusinessException(MessageConstant.PRODUCT_SKU_NOT_FOUND);
        }
        if (stockUpdateDTO.getStock() == null || stockUpdateDTO.getStock() < 0) {
            throw new ProductBusinessException(MessageConstant.STOCK_VALUE_INVALID);
        }

        ProductSku oldSku = getSkuOrThrow(stockUpdateDTO.getSkuId());
        int affectedRows = productSkuMapper.updateStock(
                stockUpdateDTO.getSkuId(),
                stockUpdateDTO.getStock());
        if (affectedRows == 0) {
            throw new ProductBusinessException(MessageConstant.PRODUCT_SKU_NOT_FOUND);
        }

        StockLog stockLog = StockLog.builder()
                .skuId(stockUpdateDTO.getSkuId())
                .businessType(StockLog.INIT)
                .bizId(null)
                .changeCount(stockUpdateDTO.getStock() - oldSku.getStock())
                .beforeStock(oldSku.getStock())
                .afterStock(stockUpdateDTO.getStock())
                .remark("管理端设置SKU绝对库存")
                .createTime(LocalDateTime.now())
                .build();
        stockLogMapper.insert(stockLog);

        try {
            stringRedisTemplate.opsForValue().set(
                    stockKey(stockUpdateDTO.getSkuId()),
                    String.valueOf(stockUpdateDTO.getStock()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Transactional
    public void restoreStockByOrderId(Long orderId) {
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);
        if (orderDetails == null || orderDetails.isEmpty()) {
            return;
        }

        for (OrderDetail orderDetail : orderDetails) {
            if (orderDetail.getSkuId() == null || orderDetail.getNumber() == null) {
                continue;
            }
            restoreStock(orderId, orderDetail.getSkuId(), orderDetail.getNumber());
        }
    }

    private ProductSku getSkuOrThrow(Long skuId) {
        ProductSku sku = productSkuMapper.getById(skuId);
        if (sku == null) {
            throw new ProductBusinessException(MessageConstant.PRODUCT_SKU_NOT_FOUND);
        }
        return sku;
    }

    private void validateCount(Integer count) {
        if (count == null || count <= 0) {
            throw new ProductBusinessException(MessageConstant.STOCK_VALUE_INVALID);
        }
    }

    private String stockKey(Long skuId) {
        return "stock:sku:" + skuId;
    }

    private void syncRedisStock(Long skuId, Integer stock) {
        try {
            stringRedisTemplate.opsForValue().set(stockKey(skuId), String.valueOf(stock));
        } catch (Exception ex) {
            log.warn("SKU库存同步Redis失败，skuId={}, stock={}", skuId, stock, ex);
        }
    }
}
