package com.sky.controller.user;

import com.sky.entity.Product;
import com.sky.entity.ProductSku;
import com.sky.result.Result;
import com.sky.service.ProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端商品浏览接口
 */
@RestController("userProductController")
@RequestMapping("/user/product")
@Api(tags = "C端-商品浏览接口")
@Slf4j
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/list")
    @ApiOperation("根据分类id查询商品")
    public Result<List<Product>> list(Long categoryId) {
        return Result.success(productService.listByCategory(categoryId));
    }

    @GetMapping("/sku/{productId}")
    @ApiOperation("根据商品id查询SKU列表")
    public Result<List<ProductSku>> skuList(@PathVariable Long productId) {
        return Result.success(productService.listSkuByProductId(productId));
    }
}
