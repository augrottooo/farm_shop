package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.dto.SkuStockUpdateDTO;
import com.sky.entity.ProductSku;
import com.sky.entity.StockLog;
import com.sky.exception.ProductBusinessException;
import com.sky.mapper.ProductSkuMapper;
import com.sky.mapper.StockLogMapper;
import com.sky.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class StockServiceImpl implements StockService {

    @Autowired
    private ProductSkuMapper productSkuMapper;
    @Autowired
    private StockLogMapper stockLogMapper;

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
}
