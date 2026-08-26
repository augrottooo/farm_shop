package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.sky.constant.MessageConstant;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.StatusConstant;
import com.sky.dto.ProductDTO;
import com.sky.dto.ProductPageQueryDTO;
import com.sky.entity.Product;
import com.sky.entity.ProductSku;
import com.sky.exception.ProductBusinessException;
import com.sky.mapper.ProductMapper;
import com.sky.mapper.ProductSkuMapper;
import com.sky.result.PageResult;
import com.sky.service.ProductService;
import com.sky.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final String PRODUCT_LIST_CACHE_PREFIX = "cache:product:list:category:";
    private static final String PRODUCT_LIST_LOCK_PREFIX = "lock:product:list:category:";
    private static final String ALL_CATEGORY_SUFFIX = "all";
    private static final long LOCK_TTL_SECONDS = 10L;
    private static final long PRODUCT_LIST_TTL_SECONDS = 1800L;
    private static final int PRODUCT_LIST_RANDOM_TTL_SECONDS = 600;
    private static final long EMPTY_LIST_TTL_SECONDS = 120L;
    private static final int EMPTY_LIST_RANDOM_TTL_SECONDS = 30;
    private static final int CACHE_RETRY_TIMES = 5;
    private static final long CACHE_RETRY_SLEEP_MILLIS = 50L;

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductSkuMapper productSkuMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<Long> releaseLockScript;

    @PostConstruct
    public void initCacheScripts() {
        releaseLockScript = new DefaultRedisScript<>();
        releaseLockScript.setResultType(Long.class);
        releaseLockScript.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) " +
                        "else return 0 end");
    }

    @Transactional
    public void save(ProductDTO productDTO) {
        Product product = new Product();
        BeanUtils.copyProperties(productDTO, product);
        if (product.getStatus() == null) {
            product.setStatus(StatusConstant.DISABLE);
        }
        productMapper.insert(product);

        Long productId = product.getId();
        List<ProductSku> skus = productDTO.getSkus();
        if (skus != null && !skus.isEmpty()) {
            for (int i = 0; i < skus.size(); i++) {
                ProductSku sku = skus.get(i);
                sku.setProductId(productId);
                if (sku.getStock() == null) {
                    sku.setStock(100);
                }
                if (sku.getStatus() == null) {
                    sku.setStatus(StatusConstant.DISABLE);
                }
                if (sku.getSort() == null) {
                    sku.setSort(i + 1);
                }
            }
            productSkuMapper.insertBatch(skus);
        }

        evictProductListCacheAfterCommit(product.getCategoryId());
    }

    public PageResult pageQuery(ProductPageQueryDTO productPageQueryDTO) {
        PageHelper.startPage(productPageQueryDTO.getPage(), productPageQueryDTO.getPageSize());
        Page<ProductVO> page = productMapper.pageQuery(productPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    public ProductVO getById(Long id) {
        Product product = productMapper.getById(id);
        List<ProductSku> skus = productSkuMapper.listByProductId(id);
        ProductVO productVO = new ProductVO();
        BeanUtils.copyProperties(product, productVO);
        productVO.setSkus(skus);
        return productVO;
    }

    @Transactional
    public void update(ProductDTO productDTO) {
        Product oldProduct = productMapper.getById(productDTO.getId());
        if (oldProduct == null) {
            throw new ProductBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }

        Product product = new Product();
        BeanUtils.copyProperties(productDTO, product);
        productMapper.update(product);

        Long productId = productDTO.getId();
        productSkuMapper.deleteByProductId(productId);
        List<ProductSku> skus = productDTO.getSkus();
        if (skus != null && !skus.isEmpty()) {
            skus.forEach(sku -> {
                sku.setProductId(productId);
                if (sku.getStock() == null) {
                    sku.setStock(100);
                }
            });
            productSkuMapper.insertBatch(skus);
        }

        evictProductListCacheAfterCommit(oldProduct.getCategoryId(), productDTO.getCategoryId());
    }

    @Transactional
    public void startOrStop(Integer status, Long id) {
        Product oldProduct = productMapper.getById(id);
        if (oldProduct == null) {
            throw new ProductBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }

        Product product = Product.builder()
                .id(id)
                .status(status)
                .build();
        productMapper.update(product);
        productSkuMapper.updateStatusByProductId(id, status);

        evictProductListCacheAfterCommit(oldProduct.getCategoryId());
    }

    public List<Product> listByCategory(Long categoryId) {
        String cacheKey = productListCacheKey(categoryId);
        List<Product> cachedList = getProductListFromCache(cacheKey);
        if (cachedList != null) {
            return cachedList;
        }

        String lockKey = productListLockKey(categoryId);
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(locked)) {
            try {
                cachedList = getProductListFromCache(cacheKey);
                if (cachedList != null) {
                    return cachedList;
                }

                List<Product> productList = queryEnabledProductList(categoryId);
                cacheProductList(cacheKey, productList);
                return productList;
            } finally {
                releaseLock(lockKey, lockValue);
            }
        }

        return waitAndRetryProductList(categoryId, cacheKey);
    }

    public List<ProductSku> listSkuByProductId(Long productId) {
        return productSkuMapper.listByProductId(productId);
    }

    private List<Product> queryEnabledProductList(Long categoryId) {
        Product product = Product.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return productMapper.list(product);
    }

    private List<Product> getProductListFromCache(String cacheKey) {
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cacheValue == null) {
            return null;
        }
        return JSON.parseArray(cacheValue, Product.class);
    }

    private void cacheProductList(String cacheKey, List<Product> productList) {
        if (productList == null || productList.isEmpty()) {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    "[]",
                    Duration.ofSeconds(EMPTY_LIST_TTL_SECONDS + randomSeconds(EMPTY_LIST_RANDOM_TTL_SECONDS)));
            return;
        }

        stringRedisTemplate.opsForValue().set(
                cacheKey,
                JSON.toJSONString(productList),
                Duration.ofSeconds(PRODUCT_LIST_TTL_SECONDS + randomSeconds(PRODUCT_LIST_RANDOM_TTL_SECONDS)));
    }

    private List<Product> waitAndRetryProductList(Long categoryId, String cacheKey) {
        for (int i = 0; i < CACHE_RETRY_TIMES; i++) {
            try {
                Thread.sleep(CACHE_RETRY_SLEEP_MILLIS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }

            List<Product> cachedList = getProductListFromCache(cacheKey);
            if (cachedList != null) {
                return cachedList;
            }
        }

        List<Product> productList = queryEnabledProductList(categoryId);
        cacheProductList(cacheKey, productList);
        return productList;
    }

    private void evictProductListCacheAfterCommit(Long... categoryIds) {
        Set<String> cacheKeys = new LinkedHashSet<>();
        cacheKeys.add(productListCacheKey(null));
        if (categoryIds != null) {
            for (Long categoryId : categoryIds) {
                cacheKeys.add(productListCacheKey(categoryId));
            }
        }

        Runnable evictAction = () -> {
            for (String cacheKey : cacheKeys) {
                stringRedisTemplate.delete(cacheKey);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictAction.run();
                }
            });
            return;
        }

        evictAction.run();
    }

    private void releaseLock(String lockKey, String lockValue) {
        try {
            stringRedisTemplate.execute(releaseLockScript, Collections.singletonList(lockKey), lockValue);
        } catch (Exception ex) {
            log.warn("商品列表缓存锁释放失败，lockKey={}", lockKey, ex);
        }
    }

    private String productListCacheKey(Long categoryId) {
        return PRODUCT_LIST_CACHE_PREFIX + categoryKeyPart(categoryId);
    }

    private String productListLockKey(Long categoryId) {
        return PRODUCT_LIST_LOCK_PREFIX + categoryKeyPart(categoryId);
    }

    private String categoryKeyPart(Long categoryId) {
        return categoryId == null ? ALL_CATEGORY_SUFFIX : String.valueOf(categoryId);
    }

    private int randomSeconds(int bound) {
        return bound <= 0 ? 0 : ThreadLocalRandom.current().nextInt(bound + 1);
    }
}
