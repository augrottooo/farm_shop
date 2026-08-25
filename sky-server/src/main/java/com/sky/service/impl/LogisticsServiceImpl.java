package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.sky.constant.MessageConstant;
import com.sky.entity.AddressBook;
import com.sky.entity.Logistics;
import com.sky.entity.Orders;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.LogisticsMapper;
import com.sky.mapper.OrderMapper;
import com.sky.service.LogisticsService;
import com.sky.state.OrderStateMachine;
import com.sky.enumeration.OrderStatus;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LogisticsServiceImpl implements LogisticsService {

    private static final String MOCK_CARRIER_CODE = "MOCK";
    private static final String MOCK_CARRIER_NAME = "模拟快递";

    @Autowired
    private LogisticsMapper logisticsMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private OrderStateMachine orderStateMachine;
    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    @Transactional
    public void ship(Long orderId) {
        Orders order = getOrderOrThrow(orderId);
        orderStateMachine.transition(order, OrderStatus.TRANSPORTING);

        int affectedRows = orderMapper.updateStatusByIdAndStatus(
                orderId,
                OrderStatus.TRANSPORTING.getCode(),
                OrderStatus.WAITING_SHIP.getCode());
        if (affectedRows == 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        LocalDateTime now = LocalDateTime.now();
        AddressBook addressBook = addressBookMapper.getById(order.getAddressBookId());
        Logistics logistics = Logistics.builder()
                .orderId(orderId)
                .carrierCode(MOCK_CARRIER_CODE)
                .carrierName(MOCK_CARRIER_NAME)
                .trackingNo("MOCK-" + order.getNumber())
                // 物流表只保存发货时的展示快照，订单 status 才是权威状态。
                .shipStatus(1)
                .shippedAt(now)
                .receiverName(firstNonBlank(order.getConsignee(),
                        addressBook == null ? null : addressBook.getConsignee()))
                .receiverPhone(firstNonBlank(order.getPhone(),
                        addressBook == null ? null : addressBook.getPhone()))
                .receiverAddress(resolveAddress(order, addressBook))
                .createTime(now)
                .updateTime(now)
                .build();
        logisticsMapper.insert(logistics);

        Map<String, Object> message = new HashMap<>();
        message.put("type", 3);
        message.put("orderId", orderId);
        message.put("content", "订单号：" + order.getNumber() + "已发货");
        webSocketServer.sendToAllClient(JSON.toJSONString(message));
    }

    @Override
    public Logistics getByOrderId(Long orderId, Long userId) {
        Orders order = getOrderOrThrow(orderId);
        if (userId == null || !userId.equals(order.getUserId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        List<Logistics> list = logisticsMapper.listByOrderId(orderId);
        if (list == null || list.isEmpty()) {
            throw new OrderBusinessException("物流信息不存在");
        }
        return list.get(0);
    }

    private Orders getOrderOrThrow(Long orderId) {
        Orders order = orderMapper.getById(orderId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return order;
    }

    private String resolveAddress(Orders order, AddressBook addressBook) {
        if (order.getAddress() != null && !order.getAddress().trim().isEmpty()) {
            return order.getAddress();
        }
        if (addressBook == null) {
            return null;
        }
        return joinAddress(addressBook.getProvinceName(), addressBook.getCityName(),
                addressBook.getDistrictName(), addressBook.getDetail());
    }

    private String joinAddress(String... parts) {
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                result.append(part);
            }
        }
        return result.length() == 0 ? null : result.toString();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.trim().isEmpty() ? first : second;
    }
}
