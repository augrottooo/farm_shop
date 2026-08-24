package com.sky.controller.admin;

import com.sky.dto.ProductDTO;
import com.sky.dto.ProductPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.ProductService;
import com.sky.vo.ProductVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品管理
 */
@RestController("adminProductController")
@RequestMapping("/admin/product")
@Api(tags = "商品相关接口")
@Slf4j
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping
    @ApiOperation("新增商品")
    public Result<String> save(@RequestBody ProductDTO productDTO) {
        productService.save(productDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("商品分页查询")
    public Result<PageResult> page(ProductPageQueryDTO productPageQueryDTO) {
        return Result.success(productService.pageQuery(productPageQueryDTO));
    }

    @GetMapping("/{id}")
    @ApiOperation("商品详情")
    public Result<ProductVO> getById(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }

    @PutMapping
    @ApiOperation("修改商品")
    public Result<String> update(@RequestBody ProductDTO productDTO) {
        productService.update(productDTO);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation("商品上下架")
    public Result<String> startOrStop(@PathVariable Integer status, Long id) {
        productService.startOrStop(status, id);
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("按分类查询商品")
    public Result<List<com.sky.entity.Product>> list(Long categoryId) {
        return Result.success(productService.listByCategory(categoryId));
    }
}
