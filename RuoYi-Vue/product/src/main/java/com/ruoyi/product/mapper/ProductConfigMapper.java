package com.ruoyi.product.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.product.domain.ProductAttribute;
import com.ruoyi.product.domain.ProductAttributeOption;
import com.ruoyi.product.domain.ProductCategory;
import com.ruoyi.product.domain.ProductCategoryAttribute;

/**
 * 商品配置 Mapper。
 */
public interface ProductConfigMapper
{
    List<ProductCategory> selectCategoryList(ProductCategory query);

    ProductCategory selectCategoryById(@Param("categoryId") Long categoryId);

    ProductCategory selectCategoryByCode(@Param("categoryCode") String categoryCode);

    int countCategoryNameByParent(@Param("parentId") Long parentId, @Param("categoryName") String categoryName,
        @Param("excludeCategoryId") Long excludeCategoryId);

    int countChildCategories(@Param("categoryId") Long categoryId);

    int countCategoryAttributes(@Param("categoryId") Long categoryId);

    int insertCategory(ProductCategory category);

    int updateCategory(ProductCategory category);

    int updateCategoryPublishEnabled(@Param("categoryId") Long categoryId, @Param("publishEnabled") String publishEnabled,
        @Param("updateBy") String updateBy);

    int deleteCategoryById(@Param("categoryId") Long categoryId, @Param("updateBy") String updateBy);

    int increaseCategorySchemaVersion(@Param("categoryId") Long categoryId, @Param("updateBy") String updateBy);

    List<ProductAttribute> selectAttributeList(ProductAttribute query);

    List<ProductAttribute> selectEnabledAttributeList();

    ProductAttribute selectAttributeById(@Param("attributeId") Long attributeId);

    ProductAttribute selectAttributeByCode(@Param("attributeCode") String attributeCode);

    int insertAttribute(ProductAttribute attribute);

    int updateAttribute(ProductAttribute attribute);

    int deleteAttributeById(@Param("attributeId") Long attributeId, @Param("updateBy") String updateBy);

    int countAttributeCategoryBindings(@Param("attributeId") Long attributeId);

    List<ProductAttributeOption> selectOptionList(@Param("attributeId") Long attributeId);

    ProductAttributeOption selectOptionById(@Param("optionId") Long optionId);

    int countOptionCode(@Param("attributeId") Long attributeId, @Param("optionCode") String optionCode,
        @Param("excludeOptionId") Long excludeOptionId);

    int insertOption(ProductAttributeOption option);

    int updateOption(ProductAttributeOption option);

    int deleteOptionById(@Param("optionId") Long optionId, @Param("updateBy") String updateBy);

    List<ProductCategoryAttribute> selectDirectCategoryAttributeList(@Param("categoryId") Long categoryId);

    ProductCategoryAttribute selectCategoryAttributeById(@Param("categoryAttributeId") Long categoryAttributeId);

    ProductCategoryAttribute selectCategoryAttribute(@Param("categoryId") Long categoryId,
        @Param("attributeId") Long attributeId);

    List<ProductCategoryAttribute> selectCategoryAttributeRulesByCategoryIds(@Param("categoryIds") List<Long> categoryIds);

    int insertCategoryAttribute(ProductCategoryAttribute categoryAttribute);

    int updateCategoryAttribute(ProductCategoryAttribute categoryAttribute);

    int deleteCategoryAttributeById(@Param("categoryAttributeId") Long categoryAttributeId,
        @Param("updateBy") String updateBy);
}
