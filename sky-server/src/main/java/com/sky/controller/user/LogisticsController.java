package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.entity.Logistics;
import com.sky.result.Result;
import com.sky.service.LogisticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端物流查询接口。
 */
@RestController("userLogisticsController")
@RequestMapping("/user/logistics")
@Api(tags = "用户端物流接口")
public class LogisticsController {

    @Autowired
    private LogisticsService logisticsService;

    @GetMapping("/{orderId}")
    @ApiOperation("查询订单物流")
    public Result<Logistics> getByOrderId(@PathVariable Long orderId) {
        return Result.success(logisticsService.getByOrderId(orderId, BaseContext.getCurrentId()));
    }
}
