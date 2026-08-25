package com.sky.state;

import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.enumeration.OrderStatus;
import com.sky.exception.OrderBusinessException;
import org.springframework.stereotype.Component;

/**
 * 订单状态机。
 *
 * 订单表 status 是唯一权威状态，物流表只保存发货时的展示快照。
 */
@Component
public class OrderStateMachine {

    public boolean canTransition(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            return false;
        }

        if (from == OrderStatus.PENDING_PAYMENT) {
            return to == OrderStatus.WAITING_SHIP || to == OrderStatus.CANCELLED;
        }
        if (from == OrderStatus.WAITING_SHIP) {
            return to == OrderStatus.TRANSPORTING || to == OrderStatus.CANCELLED;
        }
        if (from == OrderStatus.TRANSPORTING) {
            return to == OrderStatus.SIGNED;
        }
        if (from == OrderStatus.SIGNED) {
            return to == OrderStatus.COMPLETED;
        }
        return false;
    }

    public boolean canTransition(Integer from, Integer to) {
        return canTransition(OrderStatus.of(from), OrderStatus.of(to));
    }

    public void transition(Orders order, OrderStatus to) {
        if (order == null || !canTransition(order.getStatus(), to.getCode())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        order.setStatus(to.getCode());
    }
}
