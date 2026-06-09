# 草稿商品来源 SKU 配对保存后回显修复记录

## 背景

草稿状态商品在编辑页配对来源 SKU 后点击保存，页面提示保存成功；但再次打开编辑页时，SKU 行仍显示未配对，表现为“来源 SKU 没有保存”。

## 根因

保存链路中，官方仓 SKU 来源绑定会写入 `product_sku_source_binding`，并且保存后有 active 绑定反查校验。

真正缺失的是读取链路：`ProductDistributionMapper.xml` 的 SKU 查询已经通过 `skuSourceBindingJoin` 左连接 `product_sku_source_binding`，也通过 `skuSourceBindingSelect` 选出了 `source_dimension_group_key`、`master_sku` 等字段；但 `ProductSkuResult` 没有把这些列映射到 `ProductSku` 的来源绑定属性。

因此详情接口重新查询时，SQL 查到了来源绑定，但 Java 对象里的 `sourceDimensionGroupKey` / `sourceSkuGroupKey` / `masterSku` 等字段为空，前端编辑页就显示成未配对。

## 修复

1. 在 `ProductSkuResult` 中补齐 active 来源绑定字段映射：
   - `sourceBindingId`
   - `sourceScope`
   - `sourceSkuGroupKey`
   - `sourceDimensionGroupKey`
   - `masterSku`
   - `masterProductNameSnapshot`
   - 来源 payload、WMS payload、尺寸重量、来源仓库、绑定状态、锁定状态等字段。
2. 保持 `ProductSpuResult` 不映射 SKU 来源绑定字段，避免 SPU 和 SKU 事实混淆。
3. 增加 mapper 合同测试，固定“SKU 详情必须能回显 active 来源绑定字段”。

## 验证

- `mvn -pl product -am "-Dtest=ProductDistributionMapperContractTest,ProductDistributionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- `mvn -pl ruoyi-admin -am "-Dmaven.test.skip=true" package` 通过。
- 后端已用 `start-backend-local.ps1 -Restart` 重启，`http://127.0.0.1:8080` 返回 200。

## 影响范围

本次不新增表、不改业务状态流转、不改前端配对交互。修复的是保存后再次编辑时的数据回显。
