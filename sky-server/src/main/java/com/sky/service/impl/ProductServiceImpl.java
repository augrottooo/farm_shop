package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.StatusConstant;
import com.sky.dto.ProductDTO;
import com.sky.dto.ProductPageQueryDTO;
import com.sky.entity.Product;
import com.sky.entity.ProductSku;
import com.sky.mapper.ProductMapper;
import com.sky.mapper.ProductSkuMapper;
import com.sky.result.PageResult;
import com.sky.service.ProductService;
import com.sky.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Transactional
    public void save(ProductDTO productDTO) {
        Product product = new Product();
        BeanUtils.copyProperties(productDTO, product);
        if (product.getStatus() == null) {
            product.setStatus(StatusConstant.DISABLE);
        }
        if (product.getTraceEnabled() == null) {
            product.setTraceEnabled(1);
        }
        productMapper.insert(product);

        Long productId = product.getId();
        List<ProductSku> skus = productDTO.getSkus();
        if (skus != null && !skus.isEmpty()) {
            for (int i = 0; i < skus.size(); i++) {
                ProductSku sku = skus.get(i);
                sku.setProductId(productId);
                if (sku.getStock() == null) {
                    sku.setStock(100);
                }
                if (sku.getStatus() == null) {
                    sku.setStatus(StatusConstant.DISABLE);
                }
                if (sku.getSort() == null) {
                    sku.setSort(i + 1);
                }
            }
            productSkuMapper.insertBatch(skus);
        }
    }

    public PageResult pageQuery(ProductPageQueryDTO productPageQueryDTO) {
        PageHelper.startPage(productPageQueryDTO.getPage(), productPageQueryDTO.getPageSize());
        Page<ProductVO> page = productMapper.pageQuery(productPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    public ProductVO getById(Long id) {
        Product product = productMapper.getById(id);
        List<ProductSku> skus = productSkuMapper.listByProductId(id);
        ProductVO productVO = new ProductVO();
        BeanUtils.copyProperties(product, productVO);
        productVO.setSkus(skus);
        return productVO;
    }

    @Transactional
    public void update(ProductDTO productDTO) {
        Product product = new Product();
        BeanUtils.copyProperties(productDTO, product);
        if (product.getTraceEnabled() == null) {
            product.setTraceEnabled(1);
        }
        productMapper.update(product);

        Long productId = productDTO.getId();
        productSkuMapper.deleteByProductId(productId);
        List<ProductSku> skus = productDTO.getSkus();
        if (skus != null && !skus.isEmpty()) {
            skus.forEach(sku -> {
                sku.setProductId(productId);
                if (sku.getStock() == null) {
                    sku.setStock(100);
                }
            });
            productSkuMapper.insertBatch(skus);
        }
    }

    public void startOrStop(Integer status, Long id) {
        Product product = Product.builder()
                .id(id)
                .status(status)
                .build();
        productMapper.update(product);
        productSkuMapper.updateStatusByProductId(id, status);
    }

    public List<Product> listByCategory(Long categoryId) {
        Product product = Product.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return productMapper.list(product);
    }

    public List<ProductSku> listSkuByProductId(Long productId) {
        return productSkuMapper.listByProductId(productId);
    }
}
