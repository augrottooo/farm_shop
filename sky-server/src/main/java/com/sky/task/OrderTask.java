package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderService orderService;
    /**
     * 处理超时订单的方法
     */
    @Scheduled(cron = "0 * * * * ? ")
    public void processTimeoutOrder(){
        log.info("定时处理超时订单：{}", LocalDateTime.now());
        // 当前时间往前推15分钟
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);

        // select * from orders where status = ? and order_time < (当前时间 - 15分钟)
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);

        //循环更新订单状态
        if(ordersList != null && ordersList.size() > 0) {
            for (Orders orders : ordersList) {
                try {
                    orderService.cancelTimeoutOrder(orders.getId());
                } catch (Exception ex) {
                    log.error("超时订单回补失败，orderId={}", orders.getId(), ex);
                }
            }
        }
    }

}
