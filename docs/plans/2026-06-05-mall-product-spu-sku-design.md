# 商城商品 SPU/SKU 首版设计方案

日期：2026-06-05

状态：已确认并进入首版实现。SQL 执行需另按数据库执行记录确认目标数据源。

## 1. 背景与目标

本方案用于落地管理端菜单：

```text
商品管理 / 商城商品列表
```

当前菜单仍是占位入口：

- 菜单 ID：`2402`
- 路径：`product/distribution`
- 当前组件：`Common/PlannedPage/index`
- 当前权限：`product:distribution:list`

首版目标是建立正式商城商品的 SPU/SKU 数据模型，并提供管理端手工创建、编辑、查看、状态切换和列表展示能力。

本阶段方案已确认，首版实现已按本方案推进；建表和菜单 SQL 已生成到脚本文件，未在本文档中记录运行库执行结果。

## 2. 已确认口径

- 商城商品列表展示正式商城商品，不是来源商品库。
- 首版允许新增商城商品相关表，但必须先提交 Markdown 方案并确认。
- 商品必须绑定一个卖家。
- 一个 SPU 下允许多个 SKU。
- 系统生成主编码，允许填写卖家侧编码：
  - 系统 SPU
  - 系统 SKU
  - 客户 SPU
  - 客户 SKU
- 客户 SPU / 客户 SKU 中的“客户”指绑定卖家；数据库建议命名为 `seller_spu_code` / `seller_sku_code`，前端展示为“客户SPU / 客户SKU”。
- 系统 SPU / SKU 全局唯一；客户 SPU / SKU 在同一卖家下唯一。
- 商品分类复用现有 `product_category`，且只能选择末级可发布分类。
- 商品类目属性值复用现有类目属性模板能力，首版统一挂在 SPU 上。
- SKU 固定规格直接作为 `product_sku` 表字段，不做可配置规格模板。
- SKU 固定规格字段包括：
  - 颜色
  - 尺寸
  - 材质
  - 风格
  - 型号
  - 商品数量
  - 容量
- SKU 固定规格字段均允许为空；单位由卖家或运营写入字段文本，不单独拆单位字段。
- SKU 物流/包装字段包括长度、宽度、高度和重量，固定展示在 SKU 表格中，不进入规格属性勾选区，也不参与 SKU 矩阵组合生成。
- 供货价、销售价、币种放在 SKU 维度。
- 销售价允许低于供货价，但保存或上架时需要风险提示，不做硬阻断。
- 库存按 SKU 维度拆，并且未来按仓库读取；本阶段只设计库存读取边界，不建库存事实表。
- 商品图片需要：
  - SPU 主图
  - SPU 轮播图
  - SKU 主图或规格示意图
- 图片上传复用现有 `/common/upload` 和 `FileStorageService`，业务表只保存资源路径，不保存 data URL。
- 状态包括：
  - 草稿
  - 待上架
  - 已上架
  - 已下架
  - 停用
- 本菜单只展示状态和详情，不承载审核动作；审核流程后续放在审核中心。
- 首版创建来源只落 `ADMIN_MANUAL`，但保留来源字段便于后续接入卖家提交、来源商品库生成等来源。

## 3. 业务边界

### 3.1 本菜单承载

- 管理端查看商城商品 SPU 列表。
- 管理端展开查看 SKU 列表。
- 管理端手工新增 SPU 和 SKU。
- 管理端编辑 SPU 基础信息、SKU 信息、图片和 SPU 类目属性值。
- 管理端查看只读详情。
- 管理端切换 SPU / SKU 销售状态。
- 读取并展示库存聚合结果。

### 3.2 本菜单不承载

- 不承载卖家端商品发布页面。
- 不承载商品审核动作。
- 不承载库存调整、库存流水、仓库维护。
- 不承载来源商品同步。
- 不承载来源 SKU 到商城商品的自动生成。
- 不承载订单、履约、财务结算逻辑。
- 不承载复杂价格规则、阶梯价、买家等级价或促销价。

## 4. 模块归属

商城商品主数据属于 `product` 商品共享领域，但当前入口是管理端能力：

- 后端管理端接口建议继续放在 `RuoYi-Vue/product` 的 `/product/admin/**` 下。
- 管理端权限继续走若依 `sys_menu` / `sys_role`。
- 当前 React 页面放在 `react-ui/src/pages/Product/**`。
- 后续卖家端商品发布入口应放在 `seller` 端模块。
- 后续买家端商品浏览入口应放在 `buyer` 端模块。
- `product` 不是第四个终端，不应承载 seller/buyer 端内权限入口。

## 5. 核心模型

### 5.1 SPU

SPU 表示商城商品款式或商品主体，一行代表一个可展示商品。

示例：

```text
男士基础款 T 恤
```

SPU 负责：

- 商品名称
- 绑定卖家
- 商品分类
- 主图和轮播图
- 类目属性值
- 主状态
- 来源信息

### 5.2 SKU

SKU 表示某个具体可售规格，一行代表一个可售或可管理规格。

示例：

```text
男士基础款 T 恤 / 黑色 / L
男士基础款 T 恤 / 白色 / M
```

SKU 负责：

- 系统 SKU
- 客户 SKU
- 固定规格字段
- SKU 主图
- 供货价
- 销售价
- 币种
- SKU 状态
- 库存读取键

### 5.3 规格摘要

规格摘要是给列表和 SKU 子表展示用的自动拼接文本，不让操作员手填。

示例：

```text
黑色 / L / 500g / 棉 / 简约 / TX-001 / 1件 / 330ml
```

拼接来源为 SKU 固定规格字段，空字段跳过。字段本身仍分别保存，规格摘要可以在接口层或前端展示层生成，不建议作为唯一事实字段。

## 6. 状态设计

### 6.1 状态值

SPU 和 SKU 均使用同一套销售状态 code：

| code | label | 说明 |
| --- | --- | --- |
| `DRAFT` | 草稿 | 尚未准备上架 |
| `READY` | 待上架 | 资料基本齐备，等待平台上架 |
| `ON_SALE` | 已上架 | 对商城可见或可售 |
| `OFF_SALE` | 已下架 | 暂停销售 |
| `DISABLED` | 停用 | 不再参与销售流程 |

### 6.2 SPU 和 SKU 双层控制

- SPU 状态决定商品是否出现在商城列表、搜索和分类页。
- SKU 状态决定某个规格是否可以买、显示价格和参与库存判断。
- SPU `ON_SALE` 且至少一个 SKU `ON_SALE` 时，商品才具备对外销售条件。
- SPU `OFF_SALE` 或 `DISABLED` 时，即使 SKU 是 `ON_SALE`，也不对外生效。
- SPU `ON_SALE` 下新增 SKU 时，新 SKU 默认 `DRAFT` 或 `READY`，不能自动进入可售状态。

### 6.3 推荐状态流转

```text
DRAFT -> READY -> ON_SALE -> OFF_SALE
DRAFT -> DISABLED
READY -> DISABLED
OFF_SALE -> READY
OFF_SALE -> DISABLED
DISABLED -> DRAFT
```

首版不引入审核状态。后续卖家提交商品时，审核状态放在审核中心；审核通过后进入商城商品列表，建议默认成为 `READY`。

### 6.4 上架硬校验

SPU 切换为 `ON_SALE` 前必须满足：

- 已绑定卖家。
- 已选择末级可发布分类。
- 已上传 SPU 主图。
- 至少存在 1 个可上架 SKU。
- 至少 1 个 SKU 可以切换或已经处于 `ON_SALE`。

SKU 切换为 `ON_SALE` 前必须满足：

- 系统 SKU 存在。
- 供货价存在。
- 销售价存在。
- 币种存在。
- SKU 未停用。

风险提示但不阻断：

- 销售价低于供货价。
- 当前可售库存为 0。
- SKU 主图为空。

## 7. 表结构建议

### 7.1 `product_spu`

业务目的：保存商城商品 SPU 主数据。

不承载：SKU 价格、SKU 库存、仓库库存、库存流水、订单、财务结算。

| 字段 | 类型建议 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `spu_id` | bigint | 是 | 自增 | SPU 主键 |
| `system_spu_code` | varchar(64) | 是 | 系统生成 | 系统 SPU，全局唯一 |
| `seller_spu_code` | varchar(128) | 否 | `''` | 客户 SPU，同一卖家下唯一 |
| `seller_id` | bigint | 是 | 无 | 绑定卖家主体 ID |
| `seller_no` | varchar(64) | 否 | `''` | 卖家编号快照 |
| `seller_name` | varchar(255) | 否 | `''` | 卖家名称快照，用于列表展示 |
| `category_id` | bigint | 是 | 无 | 商品分类 ID，只允许末级可发布分类 |
| `category_code` | varchar(64) | 否 | `''` | 分类编码快照 |
| `category_name` | varchar(255) | 否 | `''` | 分类名称快照 |
| `product_name` | varchar(255) | 是 | 无 | 商品名称 |
| `selling_point` | varchar(500) | 否 | `''` | 卖点摘要 |
| `main_image_url` | varchar(1000) | 否 | `''` | SPU 主图资源路径 |
| `spu_status` | varchar(32) | 是 | `DRAFT` | SPU 销售状态 |
| `source_type` | varchar(32) | 是 | `ADMIN_MANUAL` | 创建来源 |
| `source_ref_type` | varchar(32) | 否 | `''` | 来源对象类型，预留 |
| `source_ref_id` | varchar(128) | 否 | `''` | 来源对象 ID，预留 |
| `del_flag` | char(1) | 是 | `0` | `0` 存在，`2` 删除 |
| `remark` | varchar(500) | 否 | `''` | 备注 |
| `create_by` | varchar(64) | 否 | `''` | 创建人 |
| `create_time` | datetime | 否 | null | 创建时间 |
| `update_by` | varchar(64) | 否 | `''` | 更新人 |
| `update_time` | datetime | 否 | null | 更新时间 |

建议约束和索引：

- 主键：`spu_id`
- 唯一：`system_spu_code`
- 唯一：`seller_id + seller_spu_code`，空值策略需在 DDL 中确认
- 索引：`seller_id`
- 索引：`category_id`
- 索引：`spu_status`
- 索引：`source_type`
- 索引：`update_time`

### 7.2 `product_sku`

业务目的：保存商城商品 SKU、固定规格、价格和 SKU 状态。

不承载：仓库库存事实、库存流水、订单占用库存。

| 字段 | 类型建议 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `sku_id` | bigint | 是 | 自增 | SKU 主键 |
| `spu_id` | bigint | 是 | 无 | 所属 SPU |
| `system_sku_code` | varchar(64) | 是 | 系统生成 | 系统 SKU，全局唯一 |
| `seller_sku_code` | varchar(128) | 否 | `''` | 客户 SKU，同一卖家下唯一 |
| `color` | varchar(128) | 否 | `''` | 颜色 |
| `size` | varchar(128) | 否 | `''` | 尺寸 |
| `length_value` | varchar(128) | 否 | `''` | 长度，含单位文本 |
| `width_value` | varchar(128) | 否 | `''` | 宽度，含单位文本 |
| `height_value` | varchar(128) | 否 | `''` | 高度，含单位文本 |
| `weight` | varchar(128) | 否 | `''` | 重量，含单位文本 |
| `material` | varchar(128) | 否 | `''` | 材质 |
| `style` | varchar(128) | 否 | `''` | 风格 |
| `model` | varchar(128) | 否 | `''` | 型号 |
| `package_quantity` | varchar(128) | 否 | `''` | 商品数量或包装数量，由卖家填写含义 |
| `capacity` | varchar(128) | 否 | `''` | 容量，含单位文本 |
| `sku_image_url` | varchar(1000) | 否 | `''` | SKU 主图或规格示意图 |
| `supply_price` | decimal(18,4) | 是 | 无 | 供货价 |
| `sale_price` | decimal(18,4) | 是 | 无 | 销售价 |
| `currency_code` | varchar(16) | 是 | 无 | 币种 code |
| `sku_status` | varchar(32) | 是 | `DRAFT` | SKU 销售状态 |
| `sort_order` | int | 是 | `0` | SKU 排序 |
| `del_flag` | char(1) | 是 | `0` | `0` 存在，`2` 删除 |
| `remark` | varchar(500) | 否 | `''` | 备注 |
| `create_by` | varchar(64) | 否 | `''` | 创建人 |
| `create_time` | datetime | 否 | null | 创建时间 |
| `update_by` | varchar(64) | 否 | `''` | 更新人 |
| `update_time` | datetime | 否 | null | 更新时间 |

建议约束和索引：

- 主键：`sku_id`
- 唯一：`system_sku_code`
- 唯一：`seller_id + seller_sku_code` 可通过冗余 `seller_id` 或联表校验实现，DDL 前需确认
- 索引：`spu_id`
- 索引：`sku_status`
- 索引：`currency_code`
- 索引：`update_time`

备注：

- 如果不在 `product_sku` 冗余 `seller_id`，则同卖家客户 SKU 唯一需要 Service 层通过 SPU 查卖家后校验。
- 如果希望数据库直接唯一约束，建议在 `product_sku` 冗余 `seller_id`。
- 本方案推荐冗余 `seller_id`，降低查询和唯一校验复杂度。

### 7.3 `product_attribute_value`

业务目的：保存按类目属性模板填写的商品属性值。

首版统一挂在 SPU 上，不做 SKU 级动态属性。为后续兼容，建议保留归属字段。

| 字段 | 类型建议 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `value_id` | bigint | 是 | 自增 | 属性值主键 |
| `owner_type` | varchar(16) | 是 | `SPU` | 归属类型，首版只使用 `SPU` |
| `owner_id` | bigint | 是 | 无 | 归属 ID，首版为 `spu_id` |
| `spu_id` | bigint | 是 | 无 | SPU ID，便于按商品查询 |
| `category_id` | bigint | 是 | 无 | 发布时分类 ID |
| `category_schema_version` | int | 否 | null | 分类规则版本 |
| `attribute_id` | bigint | 是 | 无 | 属性 ID |
| `attribute_code` | varchar(64) | 是 | 无 | 属性 code 快照 |
| `attribute_name` | varchar(128) | 否 | `''` | 属性名称快照 |
| `attribute_type` | varchar(32) | 是 | 无 | 属性类型快照 |
| `value_code` | varchar(128) | 否 | `''` | 单选值 code |
| `value_text` | text | 否 | null | 文本值 |
| `value_number` | decimal(18,4) | 否 | null | 数值 |
| `value_date` | date | 否 | null | 日期 |
| `value_json` | text | 否 | null | 多选、文件等复杂值 |
| `create_by` | varchar(64) | 否 | `''` | 创建人 |
| `create_time` | datetime | 否 | null | 创建时间 |
| `update_by` | varchar(64) | 否 | `''` | 更新人 |
| `update_time` | datetime | 否 | null | 更新时间 |

建议约束和索引：

- 主键：`value_id`
- 唯一：`owner_type + owner_id + attribute_id`
- 索引：`spu_id`
- 索引：`category_id`
- 索引：`attribute_code`

### 7.4 `product_image`

业务目的：保存 SPU 轮播图和可扩展图片资源。SPU 主图和 SKU 主图仍在主表冗余保存，便于列表高频读取。

| 字段 | 类型建议 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `image_id` | bigint | 是 | 自增 | 图片主键 |
| `owner_type` | varchar(16) | 是 | 无 | `SPU` 或 `SKU` |
| `owner_id` | bigint | 是 | 无 | 归属 ID |
| `spu_id` | bigint | 是 | 无 | SPU ID |
| `sku_id` | bigint | 否 | null | SKU ID，SKU 图使用 |
| `image_url` | varchar(1000) | 是 | 无 | 资源路径 |
| `image_role` | varchar(32) | 是 | 无 | `MAIN`、`GALLERY`、`SKU_MAIN` |
| `sort_order` | int | 是 | `0` | 排序 |
| `create_by` | varchar(64) | 否 | `''` | 创建人 |
| `create_time` | datetime | 否 | null | 创建时间 |

首版建议：

- SPU 主图 1 张。
- SPU 轮播图最多 9 张。
- SKU 主图每个 SKU 1 张。
- 主图路径冗余在 `product_spu.main_image_url` 和 `product_sku.sku_image_url`。

### 7.5 状态日志表是否首版需要

建议首版暂不单独建状态日志表，先复用若依 `@Log` 操作日志记录状态切换。

如果后续需要在商品详情里展示完整状态流转轨迹，再补：

```text
product_status_log
```

## 8. 编码规则

首版建议使用简单系统编码：

```text
SPUyyyyMMddNNNN
SKUyyyyMMddNNNN
```

示例：

```text
SPU202606050001
SKU202606050001
```

要求：

- 系统编码由后端生成。
- 不允许前端传入覆盖系统编码。
- 客户 SPU / SKU 允许前端填写。
- 后续如需多租户、类目、卖家前缀编码，再单独设计可配置编码规则。

## 9. 价格与币种

首版 SKU 价格字段：

- 供货价：`supply_price`
- 销售价：`sale_price`
- 币种：`currency_code`

币种来源：

- 优先读取 `finance_currency` 启用币种。
- 不直接硬编码币种。
- 不直接使用 `currency_code` 字典全集作为业务可用币种。

校验：

- 供货价、销售价必须为非负数。
- 币种必须为启用币种。
- 销售价低于供货价时提示风险，但允许保存。

## 10. 库存读取边界

本方案不建库存事实表。

库存未来应由 `inventory` 模块提供，推荐读取维度：

```text
sku_id + warehouse_id
```

未来库存聚合对象建议至少包含：

```text
skuId
availableQuantity
lockedQuantity
totalQuantity
warehouseCount
warehouseBreakdown[]
```

商城商品列表首版在库存模块未完成前：

- 总可售库存展示 `--` 或 `0`，最终展示方式实现前确认。
- 仓库数展示 `--`。
- 不允许在商品列表中手工编辑库存。
- 不允许在 `product` 模块写库存调整逻辑。

未来库存模块接入后：

- SPU 列表总库存 = 所属 SKU 可售库存聚合。
- SKU 子表库存 = 当前 SKU 可售库存聚合。
- 详情中可展示 SKU + 仓库拆分库存。

## 11. 与现有商品分类和类目属性模板的关系

复用现有：

- `product_category`
- `product_attribute`
- `product_attribute_option`
- `product_category_attribute`
- `IProductConfigService.previewCategorySchema(categoryId)`

规则：

- 新增或编辑 SPU 时必须选择商品分类。
- 只能选择末级且可发布分类。
- 选择分类后，通过现有 schema 预览能力获取需要填写的类目属性。
- 商品属性值首版统一保存到 `product_attribute_value`，归属为 `owner_type='SPU'`。
- 不在前端重复实现类目属性继承合并逻辑。

## 12. 管理端页面设计

### 12.1 页面结构

建议使用“SPU 主表 + SKU 展开子表”：

- 主表一行一个 SPU。
- 展开行展示该 SPU 下 SKU。
- 行操作提供：
  - 查看
  - 编辑
  - 状态切换

不建议首版做成完全扁平的 SKU 大表。原因：

- SPU 和 SKU 信息混在一行会导致列过多。
- SPU 图片、卖家、分类、状态与 SKU 价格、规格、库存职责不同。
- 展开子表能兼顾列表扫描和 SKU 明细。

### 12.2 主表建议列

| 列 | 说明 |
| --- | --- |
| 商品图 | SPU 主图 |
| 系统 SPU | `system_spu_code` |
| 客户 SPU | `seller_spu_code` |
| 商品名称 | `product_name` |
| 卖家 | `seller_name` |
| 类目 | `category_name` |
| SKU 数 | 当前 SPU 下未删除 SKU 数 |
| 价格区间 | SKU 销售价最小值到最大值 |
| 供货价区间 | SKU 供货价最小值到最大值 |
| 币种 | 如果多币种，展示“多币种” |
| 总可售库存 | 从库存模块读取，首版无库存模块时展示占位 |
| 仓库数 | 从库存模块读取，首版展示占位 |
| 状态 | SPU 状态 |
| 更新时间 | `update_time` |
| 操作 | 查看、编辑、更多 |

筛选项建议：

- 商品关键词
- 系统 SPU
- 客户 SPU
- 系统 SKU
- 客户 SKU
- 卖家
- 商品分类
- 商品状态
- 币种
- 更新时间

### 12.3 SKU 展开子表建议列

| 列 | 说明 |
| --- | --- |
| SKU 图 | SKU 主图 |
| 系统 SKU | `system_sku_code` |
| 客户 SKU | `seller_sku_code` |
| SKU 规格 | 自动规格摘要 |
| 尺寸重量 | `length_value` / `width_value` / `height_value` / `weight` |
| 供货价 | `supply_price` |
| 销售价 | `sale_price` |
| 币种 | `currency_code` |
| 可售库存 | 从库存模块读取，首版占位 |
| 仓库数 | 从库存模块读取，首版占位 |
| 状态 | SKU 状态 |

### 12.4 新增和编辑

内容较多，建议使用整页表单或大抽屉，不使用普通小弹窗。

表单分区：

1. SPU 基础信息
2. 卖家绑定
3. 商品分类
4. 类目属性
5. SPU 图片
6. SKU 列表
7. 价格信息
8. 状态设置

新增默认：

- SPU 状态：`DRAFT`
- SKU 状态：`DRAFT`
- 来源：`ADMIN_MANUAL`

### 12.5 查看详情

详情建议只读，编辑走单独入口。

详情区块：

- SPU 基础信息
- 卖家信息
- 商品分类
- SPU 图片
- 类目属性值
- SKU 列表
- 库存聚合占位或读取结果
- 来源信息
- 操作日志入口或状态记录占位

## 13. 后端接口建议

基础路径：

```text
/product/admin/distribution-products
```

接口建议：

| 方法 | 路径 | 用途 | 权限 |
| --- | --- | --- | --- |
| GET | `/list` | SPU 分页列表 | `product:distribution:list` |
| GET | `/{spuId}` | 查看详情 | `product:distribution:query` |
| POST | `` | 新增 SPU/SKU | `product:distribution:add` |
| PUT | `/{spuId}` | 编辑 SPU/SKU | `product:distribution:edit` |
| PUT | `/{spuId}/status` | 切换 SPU 状态 | `product:distribution:status` |
| PUT | `/{spuId}/skus/{skuId}/status` | 切换 SKU 状态 | `product:distribution:status` |
| GET | `/{spuId}/skus` | 查询 SKU 列表 | `product:distribution:query` |

首版不建议提供物理删除接口；使用停用替代删除。

## 14. 前端文件建议

新增页面：

```text
react-ui/src/pages/Product/Distribution/index.tsx
```

可按复杂度拆分：

```text
react-ui/src/pages/Product/Distribution/components/ProductFormDrawer.tsx
react-ui/src/pages/Product/Distribution/components/ProductDetailDrawer.tsx
react-ui/src/pages/Product/Distribution/components/SkuTable.tsx
react-ui/src/pages/Product/Distribution/constants.ts
react-ui/src/pages/Product/Distribution/utils.ts
```

新增 service：

```text
react-ui/src/services/product/distributionProduct.ts
```

新增类型：

```text
react-ui/src/types/product/distribution-product.d.ts
```

页面规则：

- 使用 `ProTable`。
- 筛选区使用 `getPersistedProTableSearch(...)`。
- 下拉选择器复用 `SEARCHABLE_SELECT_PROPS`。
- 商品分类选择复用现有分类树工具。
- 页面不额外写页面级标题。
- 表格主数据区应撑满可视区域。
- 操作列最多保留 2 个高频操作，其余放入“更多”下拉。

## 15. 权限和菜单

建议权限点：

```text
product:distribution:list
product:distribution:query
product:distribution:add
product:distribution:edit
product:distribution:status
```

当前菜单已有：

```text
product:distribution:list
```

正式实现时需要补按钮权限 SQL，并将菜单组件从：

```text
Common/PlannedPage/index
```

调整为：

```text
Product/Distribution/index
```

权限规则：

- 管理端接口使用 `@PreAuthorize("@ss.hasPermi('product:distribution:xxx')")`。
- 新增、编辑、状态切换使用 `@Log`。
- 前端按钮通过 `access.hasPerms(...)` 控制展示。
- 不使用 `@PortalPreAuthorize`。

## 16. 字典和选项

建议进入若依字典或集中常量：

```text
product_sales_status
product_source_type
```

币种：

- 业务币种下拉读取 `finance_currency` 启用币种。

SKU 固定规格：

- 首版不进字典。
- 直接作为表单文本字段。
- 长度、宽度、高度和重量归为 SKU 级物流/包装字段，仍直接保存到 `product_sku`，但不用于生成 SKU 规格矩阵。

## 17. 与卖家审核流程的后续关系

后续正式流程可能是：

```text
卖家提交商品 -> 审核中心审核 -> 审核通过 -> 进入商城商品列表
```

建议规则：

- 审核状态由审核中心维护。
- 商城商品列表只维护销售状态。
- 审核通过后进入商城商品列表时，默认销售状态为 `READY`。
- 审核驳回的数据不进入正式商城商品列表，或只在审核中心可见。
- 管理端手工创建商品使用 `source_type=ADMIN_MANUAL`，可绕过审核，但必须记录操作日志。
- 卖家提交进入正式商品后使用 `source_type=SELLER_SUBMIT`。

## 18. 与来源商品库的后续关系

来源商品库当前是 integration 来源 SKU 快照，不是商城商品事实源。

后续如果从来源商品库生成商城商品：

- 生成结果建议进入 `DRAFT`。
- `source_type` 使用 `SOURCE_PRODUCT`。
- `source_ref_type` 可填 `UPSTREAM_SKU`。
- `source_ref_id` 可用 `connection_code + master_sku`。
- 来源快照只作为默认值，不反向覆盖商城商品事实。

该能力不在首版实现。

## 19. 验证标准

后续实现完成后至少验证：

- 管理端有权限用户可访问“商品管理 / 商城商品列表”。
- 无权限用户不能访问接口。
- 列表分页由后端完成。
- 可按商品、SPU、SKU、卖家、分类、状态、币种筛选。
- 新增商品时系统自动生成 SPU/SKU。
- 客户 SPU / SKU 在同一卖家下唯一。
- 商品只能选择末级可发布分类。
- 类目属性值按现有 schema 生成并保存。
- SPU 可追加多个 SKU。
- 已上架 SPU 下新增 SKU 默认不自动可售。
- SPU 上架必须通过硬校验。
- SKU 上架必须通过硬校验。
- 销售价低于供货价时给出风险提示。
- 库存不可在商品列表手工编辑。
- 图片上传复用 `/common/upload`。
- 后端新增、编辑、状态切换均有权限和操作日志。
- 不输出密码、token、密钥或外部系统敏感字段。

## 20. 暂不实施事项

- 不建库存事实表。
- 不建仓库表。
- 不做卖家端发布页面。
- 不做审核中心。
- 不做来源商品库生成商城商品。
- 不做阶梯价、促销价、买家等级价。
- 不做订单可售校验。
- 不做库存扣减、锁定和流水。
- 不做商品删除，首版使用停用。

## 21. 推荐首版实施顺序

确认本方案后建议按以下顺序推进：

1. 输出并确认 SQL 设计执行稿。
2. 新增商品 SPU/SKU/属性值/图片表和权限 seed。
3. 新增后端 domain、mapper、service、controller。
4. 新增管理端 service、types 和页面。
5. 接入商品分类、类目属性 schema、卖家选择、币种选择和图片上传。
6. 实现状态切换和上架校验。
7. 写入复用台账和执行记录。
8. 运行后端编译、前端类型检查、页面浏览器验证和 `codegraph sync .`。

## 22. 当前结论

首版应按“SPU 主表 + SKU 子表 + SPU 属性值 + 图片表”的结构落地商城商品列表。

库存、审核、来源商品生成和卖家端发布都先保持边界，不混入本次商品列表实现。
