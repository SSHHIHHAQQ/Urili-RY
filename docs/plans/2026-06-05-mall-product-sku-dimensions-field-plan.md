# 商城商品 SKU 三边尺寸字段补充方案

日期：2026-06-05

状态：已确认并实施。

## 1. 背景

新增/编辑商品页的 SKU 表格需要维护三边尺寸和重量。当前 `product_sku` 已有 `weight` 字段，但没有长度、宽度、高度字段；前端如果只加输入框会造成保存丢失或后端字段不一致。因此该需求涉及 SKU 表字段、后端领域对象、Mapper 保存链路、前端类型和 SKU 表格列的同步调整。

## 2. 业务定位

三边尺寸和重量属于 SKU 级物流/包装属性，用于后续仓配、运费、发货、申报或上游平台同步，不参与颜色、尺码等销售规格组合生成。

因此它们不放入“规格属性”勾选区，也不作为颜色/尺寸那类可生成 SKU 矩阵的维度；它们应固定出现在 SKU 表格中，由每个 SKU 单独填写。

## 3. 字段方案

### 3.1 复用现有字段

`product_sku.weight`

- 中文名：重量
- 类型：`varchar(128)`
- 是否必填：首版不强制必填
- 单位规则：不固定单位，卖家/管理员按业务约定自行填写
- 用途：保存 SKU 重量，例如 `0.35kg`、`350g`

### 3.2 新增字段

`product_sku.length_value`

- 中文名：长度
- 类型：`varchar(128)`
- 是否必填：首版不强制必填
- 默认值：空字符串
- 单位规则：不固定单位，卖家/管理员自行填写
- 用途：保存 SKU 长度，例如 `30cm`

`product_sku.width_value`

- 中文名：宽度
- 类型：`varchar(128)`
- 是否必填：首版不强制必填
- 默认值：空字符串
- 单位规则：不固定单位，卖家/管理员自行填写
- 用途：保存 SKU 宽度，例如 `20cm`

`product_sku.height_value`

- 中文名：高度
- 类型：`varchar(128)`
- 是否必填：首版不强制必填
- 默认值：空字符串
- 单位规则：不固定单位，卖家/管理员自行填写
- 用途：保存 SKU 高度，例如 `8cm`

## 4. 代码影响

- 后端 `ProductSku` 增加 `lengthValue`、`widthValue`、`heightValue`。
- MyBatis `ProductDistributionMapper.xml` 增加查询映射、insert 和 update 字段。
- SQL 初始化脚本 `20260605_mall_product_distribution_seed.sql` 增加字段定义。
- 如需同步运行库，需要单独执行 `ALTER TABLE product_sku ADD COLUMN ...`，执行前必须确认当前激活数据源。
- 前端 `distribution-product.d.ts` 增加 `lengthValue`、`widthValue`、`heightValue`。
- `SkuMatrixEditor` 固定展示“长度 / 宽度 / 高度 / 重量”四列，位置放在 SKU 图之后、价格之前。
- 详情抽屉和 SKU 展开表后续可把这四个字段纳入规格摘要或单独列展示。

## 5. 不做的事

- 不新增独立 SKU 尺寸表。
- 不新增单位字典。
- 不把长度、宽度、高度加入“规格属性”勾选区。
- 不强制校验数字格式，避免和“单位自行填写”的规则冲突。
- 不在库存模块未落地前用这些字段计算库存或运费。

## 6. 执行结果

- 用户已确认：长度、宽度、高度直接作为 `product_sku` 固定字段；重量复用现有 `weight` 字段。
- 已更新 `ProductSku`、`ProductDistributionMapper.xml`、`ProductDistributionServiceImpl`、前端 SKU 类型和 SKU 矩阵编辑器。
- 已新增运行库同步脚本 `RuoYi-Vue/sql/20260605_mall_product_sku_dimension_fields.sql`。
- 已确认当前激活数据源后同步运行库 `product_sku`，新增 `length_value`、`width_value`、`height_value` 三列。
- 已通过管理端详情接口验证演示 SKU 返回 `lengthValue`、`widthValue`、`heightValue` 和 `weight`。
