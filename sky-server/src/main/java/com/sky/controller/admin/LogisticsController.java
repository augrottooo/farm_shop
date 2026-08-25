package com.sky.controller.admin;

import com.sky.dto.LogisticsShipDTO;
import com.sky.result.Result;
import com.sky.service.LogisticsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 模拟物流管理接口。
 */
@RestController("adminLogisticsController")
@RequestMapping("/admin/logistics")
@Api(tags = "模拟物流管理接口")
public class LogisticsController {

    @Autowired
    private LogisticsService logisticsService;

    @PostMapping("/ship")
    @ApiOperation("模拟发货")
    public Result<String> ship(@RequestBody LogisticsShipDTO shipDTO) {
        logisticsService.ship(shipDTO.getOrderId());
        return Result.success();
    }
}
