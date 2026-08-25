package com.sky.service;

import com.sky.entity.Logistics;

public interface LogisticsService {

    /**
     * 模拟发货并创建物流展示快照。
     */
    void ship(Long orderId);

    /**
     * 查询当前用户自己的物流信息。
     */
    Logistics getByOrderId(Long orderId, Long userId);
}
