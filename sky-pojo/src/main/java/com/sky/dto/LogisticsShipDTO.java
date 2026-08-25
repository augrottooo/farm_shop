package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 模拟发货参数。
 */
@Data
public class LogisticsShipDTO implements Serializable {

    private Long orderId;
}
