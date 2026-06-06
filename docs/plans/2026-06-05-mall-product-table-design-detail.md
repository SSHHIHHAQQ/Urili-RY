# 商城商品 SPU/SKU 表结构详细说明

日期：2026-06-05

状态：已确认。SQL 执行稿已输出，运行库执行需另按数据库执行记录确认目标数据源。

## 1. 首版拟新增表

首版建议新增 4 张表：

```text
product_spu
product_sku
product_attribute_value
product_image
```

这 4 张表只解决商城商品正式列表的主数据、SKU、类目属性值和图片管理。

首版不新增库存表、仓库表、审核表、订单表、财务表，也不做库存调整和库存流水。库存后续由 `inventory` 模块按 `sku_id + warehouse_id` 提供读取能力。

## 2. `product_spu` 商城商品 SPU 主表

### 2.1 表逻辑

`product_spu` 保存商城商品的款式级主数据。一行代表一个商城商品主体，也就是买家在商城里看到的一个商品。

例如：

```text
SPU：男士基础款 T 恤
SKU：黑色/L、黑色/M、白色/L
```

`product_spu` 承载：

- 商品归属哪个卖家。
- 商品属于哪个平台类目。
- 商品名称、卖点、主图。
- 系统 SPU 和客户 SPU。
- SPU 销售状态。
- 创建来源。

`product_spu` 不承载：

- SKU 供货价、销售价。
- SKU 规格。
- 仓库库存。
- 库存流水。
- 审核记录。
- 订单和财务结算。

### 2.2 字段说明

| 字段名 | 类型建议 | 必填 | 默认值 | 中文说明 |
| --- | --- | --- | --- | --- |
| `spu_id` | bigint | 是 | 自增 | SPU 主键。系统内部唯一 ID，用于关联 SKU、图片、属性值等表。前端展示和外部系统对接不使用这个字段作为业务编码。 |
| `system_spu_code` | varchar(64) | 是 | 后端生成 | 系统 SPU 编码。由系统生成，全局唯一，用于平台内部识别一个商品主体，例如 `SPU202606050001`。不允许前端手工覆盖。 |
| `seller_spu_code` | varchar(128) | 否 | 空字符串 | 客户 SPU，也就是卖家自己的 SPU 编码。前端展示名用“客户SPU”。同一卖家下建议唯一，不要求跨卖家唯一。 |
| `seller_id` | bigint | 是 | 无 | 绑定卖家主体 ID。表示该商城商品归属于哪个卖家。管理端手工创建也必须绑定卖家。 |
| `seller_no` | varchar(64) | 否 | 空字符串 | 卖家编号快照。用于列表展示和历史追溯，避免卖家编号后续变化时旧商品展示缺失。真实卖家关系仍以 `seller_id` 为准。 |
| `seller_name` | varchar(255) | 否 | 空字符串 | 卖家名称快照。用于列表快速展示。卖家改名后是否同步快照，后续由业务规则决定。 |
| `category_id` | bigint | 是 | 无 | 商品分类 ID，引用现有 `product_category.category_id`。只允许选择末级且可发布分类。 |
| `category_code` | varchar(64) | 否 | 空字符串 | 分类编码快照，来自 `product_category.category_code`。用于列表展示、导出和历史追溯。 |
| `category_name` | varchar(255) | 否 | 空字符串 | 分类名称快照，来自 `product_category.category_name` 或完整分类路径。用于列表展示。 |
| `product_name` | varchar(255) | 是 | 无 | 商品名称。SPU 级字段，展示给管理端和后续商城前台。 |
| `selling_point` | varchar(500) | 否 | 空字符串 | 商品卖点或短描述。用于列表、详情或后续前台摘要展示，不保存长详情。 |
| `main_image_url` | varchar(1000) | 否 | 空字符串 | SPU 主图资源路径。通过 `/common/upload` 上传后保存返回路径。列表优先读取该字段。 |
| `spu_status` | varchar(32) | 是 | `DRAFT` | SPU 销售状态。建议值：`DRAFT` 草稿、`READY` 待上架、`ON_SALE` 已上架、`OFF_SALE` 已下架、`DISABLED` 停用。 |
| `source_type` | varchar(32) | 是 | `ADMIN_MANUAL` | 商品创建来源。首版只使用 `ADMIN_MANUAL`，后续可扩展 `SELLER_SUBMIT`、`SOURCE_PRODUCT`。 |
| `source_ref_type` | varchar(32) | 否 | 空字符串 | 来源对象类型，预留字段。后续从来源商品库生成时应使用 `SOURCE_SKU_GROUP`。管理端手工创建时为空。 |
| `source_ref_id` | varchar(128) | 否 | 空字符串 | 来源对象 ID，预留字段。后续从来源商品库生成时保存稳定 `sourceSkuGroupKey`，不保存单条 `connection_code + master_sku`。管理端手工创建时为空。 |
| `del_flag` | char(1) | 是 | `0` | 逻辑删除标识。`0` 表示存在，`2` 表示删除。首版不建议开放删除操作，停用替代删除。 |
| `remark` | varchar(500) | 否 | 空字符串 | 管理端备注，只用于内部查看，不作为前台商品描述。 |
| `create_by` | varchar(64) | 否 | 空字符串 | 创建人账号，沿用若依审计字段。 |
| `create_time` | datetime | 否 | null | 创建时间。 |
| `update_by` | varchar(64) | 否 | 空字符串 | 最后更新人账号，沿用若依审计字段。 |
| `update_time` | datetime | 否 | null | 最后更新时间。 |

### 2.3 约束与索引建议

- 主键：`spu_id`
- 唯一索引：`system_spu_code`
- 唯一约束建议：同一卖家下 `seller_spu_code` 唯一；空值是否参与唯一约束需要在 DDL 阶段确认。
- 普通索引：
  - `seller_id`
  - `category_id`
  - `spu_status`
  - `source_type`
  - `update_time`

## 3. `product_sku` 商城商品 SKU 表

### 3.1 表逻辑

`product_sku` 保存商品的具体可售规格。一行代表一个 SKU。

SKU 是价格、库存读取和后续下单的核心粒度。一个 SPU 可以有多个 SKU。

例如：

```text
SPU：男士基础款 T 恤
SKU：黑色/L
SKU：黑色/M
SKU：白色/L
```

`product_sku` 承载：

- 系统 SKU。
- 客户 SKU。
- 固定规格字段。
- SKU 图片。
- 供货价、销售价、币种。
- SKU 销售状态。

`product_sku` 不承载：

- 仓库库存数量。
- 库存锁定数量。
- 库存流水。
- 订单占用。
- SKU 审核记录。

### 3.2 字段说明

| 字段名 | 类型建议 | 必填 | 默认值 | 中文说明 |
| --- | --- | --- | --- | --- |
| `sku_id` | bigint | 是 | 自增 | SKU 主键。系统内部唯一 ID，后续库存、订单、仓库库存读取都应以该字段作为稳定关联键。 |
| `spu_id` | bigint | 是 | 无 | 所属 SPU ID，关联 `product_spu.spu_id`。一个 SPU 下可以有多个 SKU。 |
| `seller_id` | bigint | 是 | 无 | 卖家主体 ID，冗余自 SPU。用于支撑同一卖家下客户 SKU 唯一、SKU 筛选和后续库存/订单侧快速判断归属。该字段由后端根据 SPU 写入，不信任前端传值。 |
| `system_sku_code` | varchar(64) | 是 | 后端生成 | 系统 SKU 编码。由系统生成，全局唯一，例如 `SKU202606050001`。后续库存、订单、外部映射可展示该编码。 |
| `seller_sku_code` | varchar(128) | 否 | 空字符串 | 客户 SKU，也就是卖家自己的 SKU 编码。前端展示名用“客户SKU”。同一卖家下建议唯一，不要求跨卖家唯一。 |
| `color` | varchar(128) | 否 | 空字符串 | 颜色。SKU 固定规格字段之一，纯文本保存，例如“黑色”“红色”。不拆字典，不拆单位。 |
| `size` | varchar(128) | 否 | 空字符串 | 尺寸。SKU 固定规格字段之一，纯文本保存，例如“L”“30cm”。 |
| `length_value` | varchar(128) | 否 | 空字符串 | 长度。SKU 级物流/包装字段，纯文本保存，单位由卖家或运营自己写，例如“30cm”。不参与 SKU 规格矩阵生成。 |
| `width_value` | varchar(128) | 否 | 空字符串 | 宽度。SKU 级物流/包装字段，纯文本保存，单位由卖家或运营自己写，例如“20cm”。不参与 SKU 规格矩阵生成。 |
| `height_value` | varchar(128) | 否 | 空字符串 | 高度。SKU 级物流/包装字段，纯文本保存，单位由卖家或运营自己写，例如“8cm”。不参与 SKU 规格矩阵生成。 |
| `weight` | varchar(128) | 否 | 空字符串 | 重量。SKU 级物流/包装字段，纯文本保存，单位由卖家或运营自己写，例如“500g”“0.5kg”。不参与 SKU 规格矩阵生成。 |
| `material` | varchar(128) | 否 | 空字符串 | 材质。SKU 固定规格字段之一，例如“棉”“不锈钢”。 |
| `style` | varchar(128) | 否 | 空字符串 | 风格。SKU 固定规格字段之一，例如“简约”“户外”。 |
| `model` | varchar(128) | 否 | 空字符串 | 型号。SKU 固定规格字段之一，例如厂家型号、规格型号或卖家内部型号。 |
| `package_quantity` | varchar(128) | 否 | 空字符串 | 商品数量或包装数量。由卖家自己填写含义，例如“1件”“12个/箱”。不作为库存数量。 |
| `capacity` | varchar(128) | 否 | 空字符串 | 容量。SKU 固定规格字段之一，纯文本保存，单位由卖家或运营自己写，例如“330ml”“1L”。 |
| `sku_image_url` | varchar(1000) | 否 | 空字符串 | SKU 主图或规格示意图资源路径。用于展示某个规格自己的图片，例如颜色图、包装图。 |
| `supply_price` | decimal(18,4) | 是 | 无 | 供货价。卖家给商城供货的价格，放在 SKU 维度。金额字段必须使用 decimal，不使用 float/double。 |
| `sale_price` | decimal(18,4) | 是 | 无 | 销售价。商城面向买家销售的价格，放在 SKU 维度。允许低于供货价，但需要风险提示。 |
| `currency_code` | varchar(16) | 是 | 无 | 币种 code，例如 `USD`、`CNY`。业务可用币种应读取 `finance_currency` 启用币种，不直接硬编码。 |
| `sku_status` | varchar(32) | 是 | `DRAFT` | SKU 销售状态。建议值同 SPU：`DRAFT`、`READY`、`ON_SALE`、`OFF_SALE`、`DISABLED`。 |
| `sort_order` | int | 是 | `0` | SKU 排序。用于同一 SPU 下 SKU 展示顺序。 |
| `del_flag` | char(1) | 是 | `0` | 逻辑删除标识。`0` 表示存在，`2` 表示删除。首版不建议开放删除，停用替代删除。 |
| `remark` | varchar(500) | 否 | 空字符串 | SKU 内部备注，不展示给买家。 |
| `create_by` | varchar(64) | 否 | 空字符串 | 创建人账号。 |
| `create_time` | datetime | 否 | null | 创建时间。 |
| `update_by` | varchar(64) | 否 | 空字符串 | 最后更新人账号。 |
| `update_time` | datetime | 否 | null | 最后更新时间。 |

### 3.3 约束与索引建议

- 主键：`sku_id`
- 唯一索引：`system_sku_code`
- 唯一约束建议：同一卖家下 `seller_sku_code` 唯一；空值是否参与唯一约束需要在 DDL 阶段确认。
- 普通索引：
  - `spu_id`
  - `seller_id`
  - `sku_status`
  - `currency_code`
  - `update_time`

### 3.4 规格摘要说明

规格摘要不是独立字段，建议由接口或前端按以下字段自动拼接：

```text
color / size / material / style / model / package_quantity / capacity
```

空字段跳过。例如：

```text
黑色 / L / 棉 / 简约
```

不建议保存规格摘要为事实字段，避免规格字段修改后摘要不同步。

## 4. `product_attribute_value` 商城商品类目属性值表

### 4.1 表逻辑

`product_attribute_value` 保存商品按类目属性模板填写的属性值。

这里的“属性值”不是 SKU 固定规格。它来自现有商品分类和类目属性模板。

例如：

- 分类是 T 恤时，类目模板可能要求填写“适用季节”“领型”“面料成分”。
- 分类是水杯时，类目模板可能要求填写“保温时长”“杯盖类型”“适用人群”。

这些字段会随商品分类变化，所以不适合固定写死在 `product_spu` 或 `product_sku` 表里。

首版规则：

- 类目属性值统一挂在 SPU 上。
- SKU 只保存固定规格字段。
- 为后续扩展保留 `owner_type` / `owner_id`，但首版只使用 `owner_type='SPU'`。

### 4.2 字段说明

| 字段名 | 类型建议 | 必填 | 默认值 | 中文说明 |
| --- | --- | --- | --- | --- |
| `value_id` | bigint | 是 | 自增 | 属性值主键。 |
| `owner_type` | varchar(16) | 是 | `SPU` | 属性值归属类型。首版固定为 `SPU`，表示属性值归属于商品主体。后续如确需 SKU 级动态属性，可扩展为 `SKU`。 |
| `owner_id` | bigint | 是 | 无 | 归属对象 ID。首版等于 `spu_id`。保留该字段是为了后续兼容不同归属类型。 |
| `spu_id` | bigint | 是 | 无 | SPU ID。即使已有 `owner_id`，仍冗余该字段，方便按商品查询所有属性值。 |
| `category_id` | bigint | 是 | 无 | 商品保存属性值时选择的分类 ID。用于追溯属性值来自哪个分类 schema。 |
| `category_schema_version` | int | 否 | null | 分类属性规则版本。保存商品当时使用的类目模板版本，后续分类规则变化时可用于历史回显。 |
| `attribute_id` | bigint | 是 | 无 | 属性 ID，来自 `product_attribute.attribute_id`。 |
| `attribute_code` | varchar(64) | 是 | 无 | 属性编码快照，来自 `product_attribute.attribute_code`。保存快照便于导出、追溯和后续属性被改名时回显。 |
| `attribute_name` | varchar(128) | 否 | 空字符串 | 属性名称快照，来自 `product_attribute.attribute_name`。用于历史回显。 |
| `attribute_type` | varchar(32) | 是 | 无 | 属性类型快照，例如 `TEXT`、`NUMBER`、`SINGLE_SELECT`、`MULTI_SELECT`。用于决定读取哪个值字段。 |
| `value_code` | varchar(128) | 否 | 空字符串 | 单选属性的选项 code。比如属性是“是否带电池”，选项 code 可为 `YES`。 |
| `value_text` | text | 否 | null | 文本属性值。适合普通文本、长文本或不需要数值计算的值。 |
| `value_number` | decimal(18,4) | 否 | null | 数值属性值。适合重量、长度、功率等需要数值校验或范围筛选的类目属性。 |
| `value_date` | date | 否 | null | 日期属性值。适合上市日期、有效期等日期类型属性。 |
| `value_json` | text | 否 | null | 复杂属性值。适合多选、文件列表、复合结构等，例如多选 code 数组。 |
| `create_by` | varchar(64) | 否 | 空字符串 | 创建人账号。 |
| `create_time` | datetime | 否 | null | 创建时间。 |
| `update_by` | varchar(64) | 否 | 空字符串 | 最后更新人账号。 |
| `update_time` | datetime | 否 | null | 最后更新时间。 |

### 4.3 约束与索引建议

- 主键：`value_id`
- 唯一约束：`owner_type + owner_id + attribute_id`
- 普通索引：
  - `spu_id`
  - `category_id`
  - `attribute_code`

### 4.4 值字段使用规则

不同属性类型使用不同值字段：

| 属性类型 | 推荐保存字段 |
| --- | --- |
| `TEXT` | `value_text` |
| `NUMBER` | `value_number` |
| `BOOLEAN` | `value_code` 或 `value_text` |
| `SINGLE_SELECT` | `value_code` |
| `MULTI_SELECT` | `value_json` |
| `DATE` | `value_date` |
| `FILE` | `value_json` |

## 5. `product_image` 商城商品图片表

### 5.1 表逻辑

`product_image` 保存商品图片资源，主要用于 SPU 轮播图和后续扩展图片。

为了列表读取快，SPU 主图仍冗余在 `product_spu.main_image_url`；SKU 主图仍冗余在 `product_sku.sku_image_url`。

`product_image` 承载：

- SPU 主图记录。
- SPU 轮播图。
- SKU 主图记录。
- 图片排序。

图片文件本身不进入数据库。数据库只保存通过 `/common/upload` 上传后返回的资源路径。

### 5.2 字段说明

| 字段名 | 类型建议 | 必填 | 默认值 | 中文说明 |
| --- | --- | --- | --- | --- |
| `image_id` | bigint | 是 | 自增 | 图片记录主键。 |
| `owner_type` | varchar(16) | 是 | 无 | 图片归属类型。建议值：`SPU` 表示 SPU 图片，`SKU` 表示 SKU 图片。 |
| `owner_id` | bigint | 是 | 无 | 归属对象 ID。`owner_type=SPU` 时为 `spu_id`；`owner_type=SKU` 时为 `sku_id`。 |
| `spu_id` | bigint | 是 | 无 | SPU ID。无论图片归属 SPU 还是 SKU，都保存 SPU ID，方便查询某个商品的全部图片。 |
| `sku_id` | bigint | 否 | null | SKU ID。SKU 图片时填写；SPU 图片时为空。 |
| `image_url` | varchar(1000) | 是 | 无 | 图片资源路径。只保存资源路径，不保存 base64 或 data URL。 |
| `image_role` | varchar(32) | 是 | 无 | 图片角色。建议值：`MAIN` SPU 主图、`GALLERY` SPU 轮播图、`SKU_MAIN` SKU 主图。 |
| `sort_order` | int | 是 | `0` | 图片排序。同一归属对象和同一图片角色下按该字段排序。 |
| `create_by` | varchar(64) | 否 | 空字符串 | 创建人账号。 |
| `create_time` | datetime | 否 | null | 创建时间。 |

### 5.3 约束与索引建议

- 主键：`image_id`
- 普通索引：
  - `owner_type + owner_id`
  - `spu_id`
  - `sku_id`
  - `image_role`

首版业务限制建议：

- SPU 主图最多 1 张。
- SPU 轮播图最多 9 张。
- 每个 SKU 主图最多 1 张。

这些限制建议由后端 Service 校验，不完全依赖数据库唯一索引。

## 6. 首版暂不新增的表

### 6.1 不新增库存表

库存不放在 `product` 模块，原因：

- 库存需要按仓库拆。
- 库存会涉及可售、锁定、占用、调整、流水。
- 库存后续应归 `inventory` 模块。

商城商品列表只读取库存聚合结果，不允许手工编辑库存。

未来库存模块建议按以下维度设计：

```text
sku_id + warehouse_id
```

### 6.2 不新增审核表

审核流程后续放在审核中心，不混入商城商品列表。

商城商品列表只保存销售状态：

```text
DRAFT
READY
ON_SALE
OFF_SALE
DISABLED
```

卖家提交商品后的审核状态，应由审核中心独立保存。

### 6.3 不新增状态日志表

首版状态切换先复用若依操作日志 `@Log`。

如果后续需要在商品详情展示完整状态轨迹，再单独设计：

```text
product_status_log
```

## 7. 表关系总览

```text
seller
  1 ── n product_spu

product_category
  1 ── n product_spu

product_spu
  1 ── n product_sku
  1 ── n product_attribute_value
  1 ── n product_image

product_sku
  1 ── n product_image
```

说明：

- `product_spu.seller_id` 绑定卖家。
- `product_spu.category_id` 绑定商品分类。
- `product_sku.spu_id` 绑定 SPU。
- `product_attribute_value.spu_id` 保存 SPU 级动态类目属性。
- `product_image` 同时支持 SPU 图片和 SKU 图片。

## 8. 当前推荐结论

首版只新增 4 张表即可支撑“商城商品列表”的正式 SPU/SKU 结构：

```text
product_spu
product_sku
product_attribute_value
product_image
```

这套结构可以支持：

- 一个 SPU 下多个 SKU。
- 后续在已存在 SPU 上追加 SKU。
- SPU 和 SKU 双层上架控制。
- 卖家绑定。
- 客户 SPU / 客户 SKU。
- SKU 维度价格。
- SKU 固定规格。
- SPU 类目动态属性。
- SPU 主图、轮播图和 SKU 图。
- 后续库存模块按 SKU 读取库存。

确认本设计后，下一步再输出 SQL DDL、字典和菜单权限 seed 的执行稿。
