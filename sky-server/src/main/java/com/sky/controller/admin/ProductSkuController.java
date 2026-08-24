package com.sky.controller.admin;

import com.sky.dto.SkuStockUpdateDTO;
import com.sky.result.Result;
import com.sky.service.StockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品 SKU 管理接口
 */
@RestController("adminProductSkuController")
@RequestMapping("/admin/product/sku")
@Api(tags = "商品SKU相关接口")
public class ProductSkuController {

    @Autowired
    private StockService stockService;

    @PutMapping("/stock")
    @ApiOperation("设置SKU绝对库存")
    public Result<String> setStock(@RequestBody SkuStockUpdateDTO stockUpdateDTO) {
        stockService.setStock(stockUpdateDTO);
        return Result.success();
    }
}
