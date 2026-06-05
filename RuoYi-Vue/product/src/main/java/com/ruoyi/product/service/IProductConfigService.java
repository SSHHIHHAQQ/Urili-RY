package com.ruoyi.product.service;

import java.util.List;
import com.ruoyi.product.domain.ProductAttribute;
import com.ruoyi.product.domain.ProductAttributeOption;
import com.ruoyi.product.domain.ProductCategory;
import com.ruoyi.product.domain.ProductCategoryAttribute;

/**
 * 商品分类与属性配置服务。
 */
public interface IProductConfigService
{
    List<ProductCategory> selectCategoryList(ProductCategory query);

    List<ProductCategory> selectCategoryPath(Long categoryId);

    ProductCategory selectCategoryById(Long categoryId);

    int insertCategory(ProductCategory category);

    int updateCategory(ProductCategory category);

    int deleteCategoryById(Long categoryId);

    List<ProductAttribute> selectAttributeList(ProductAttribute query);

    List<ProductAttribute> selectEnabledAttributeList(ProductAttribute query);

    ProductAttribute selectAttributeById(Long attributeId);

    int insertAttribute(ProductAttribute attribute);

    int updateAttribute(ProductAttribute attribute);

    int deleteAttributeById(Long attributeId);

    List<ProductAttributeOption> selectOptionList(Long attributeId);

    int insertOption(Long attributeId, ProductAttributeOption option);

    int updateOption(Long attributeId, Long optionId, ProductAttributeOption option);

    int deleteOptionById(Long attributeId, Long optionId);

    List<ProductCategoryAttribute> selectDirectCategoryAttributeList(Long categoryId);

    ProductCategoryAttribute selectCategoryAttributeById(Long categoryAttributeId);

    int saveCategoryAttribute(ProductCategoryAttribute categoryAttribute);

    int deleteCategoryAttributeById(Long categoryAttributeId);

    List<ProductCategoryAttribute> previewCategorySchema(Long categoryId);
}
