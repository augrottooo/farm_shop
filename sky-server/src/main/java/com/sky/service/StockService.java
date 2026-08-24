package com.sky.service;

import com.sky.dto.SkuStockUpdateDTO;

/**
 * SKU 库存服务
 */
public interface StockService {

    /**
     * 条件扣减库存并记录扣减流水
     */
    void deductStock(Long skuId, Integer count, Long orderId);

    /**
     * 回补库存并记录回补流水。
     * 当前阶段只提供方法，不接入取消订单流程。
     */
    void restoreStock(Long orderId, Long skuId, Integer count);

    /**
     * 设置 SKU 绝对库存并记录初始化/调整流水
     */
    void setStock(SkuStockUpdateDTO stockUpdateDTO);
}
