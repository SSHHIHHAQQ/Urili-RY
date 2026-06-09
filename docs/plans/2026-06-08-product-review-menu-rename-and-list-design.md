# 商品审核多类型变更审核实施计划

日期：2026-06-08

状态：计划稿。菜单 seed 已改名为「商品审核」；审核表、接口、页面和远端库 DML 尚未执行，需确认本计划后再进入实现。

## 1. 目标

把审核中心的「商品审核」从单一“新增商品审核”升级为“商品变更审核”：

- 新增商品需要审核。
- 已有商品新增 SKU 需要审核。
- 已有商品编辑商品资料需要审核。
- 已有 SKU 编辑资料需要审核。
- SKU 价格、供货价、币种等价格类变更需要审核。

审核通过后才把变更应用到正式商品主数据；审核驳回后正式商品主数据不被污染。

## 2. 总体设计结论

采用：

```text
统一审核底座 + review_type 区分业务类型 + 类型化列表/详情/生效逻辑
```

不建议每个审核类型各建一套主表，例如 `product_new_review`、`product_price_review`。原因：

- 提交、待审、通过、驳回、撤回、日志、权限、列表分页和统计都是共性流程。
- 分表会让审核中心查询、权限和历史追溯碎片化。
- 后续如果允许一个审核单同时修改商品资料和价格，独立表会难以保持事务一致。

建议用统一主表承载审核单，用明细和快照描述“本次变更影响哪些对象、改了什么”。

## 3. 审核类型

数据库持久化的 `review_type`：

| review_type | 名称 | 审核对象 | 通过后生效 |
| --- | --- | --- | --- |
| `NEW_PRODUCT` | 新增商品审核 | 新 SPU + 新 SKU | SPU/SKU 从 `DRAFT` 进入 `READY` |
| `ADD_SKU` | 新增 SKU 审核 | 已有 SPU 下的新 SKU | 新 SKU 从 `DRAFT` 进入 `READY`，原 SKU 不变 |
| `EDIT_PRODUCT_INFO` | 商品资料变更审核 | SPU 标题、卖点、主图、详情、类目属性、发货仓库等 | 更新 SPU 资料，销售状态不变 |
| `EDIT_SKU_INFO` | SKU 资料变更审核 | SKU 图、规格、尺寸重量、仓库/来源绑定等 | 更新对应 SKU 资料，销售状态不变 |
| `EDIT_PRICE` | 价格变更审核 | SKU 供货价、销售价、币种 | 更新对应 SKU 价格，销售状态不变 |

页面上的 `全部` 是筛选项，不写入数据库：

```text
全部 / 新增商品 / 新增SKU / 商品资料变更 / SKU资料变更 / 价格变更
```

审核状态与审核类型分开：

```text
PENDING / APPROVED / REJECTED / WITHDRAWN
```

类型回答“审什么”，状态回答“流程走到哪一步”。

## 4. 业务状态流

### 4.1 新增商品

```text
保存商品草稿
  -> 提交 NEW_PRODUCT 审核
  -> 审核中冻结编辑
  -> 通过：SPU/SKU 进入 READY
  -> 驳回：保持 DRAFT，允许修改后重新提交
```

### 4.2 已有商品新增 SKU

```text
已有 SPU 保持原销售状态
  -> 新 SKU 先作为 DRAFT 保存
  -> 提交 ADD_SKU 审核
  -> 审核中冻结这些新 SKU
  -> 通过：新 SKU 进入 READY
  -> 驳回：新 SKU 保持 DRAFT，可修改后重新提交或删除
```

如果 SPU 已经 `ON_SALE`，通过后新 SKU 仍进入 `READY`，不直接 `ON_SALE`。上架动作继续由商城商品列表控制。

### 4.3 商品资料变更

```text
读取当前正式 SPU
  -> 生成 AFTER 快照
  -> 提交 EDIT_PRODUCT_INFO 审核
  -> 正式 SPU 暂不更新
  -> 通过：把 AFTER 快照应用到 product_spu / 属性 / 图片 / 仓库绑定
  -> 驳回：正式 SPU 不变
```

### 4.4 SKU 资料变更

```text
读取当前正式 SKU
  -> 生成 AFTER 快照
  -> 提交 EDIT_SKU_INFO 审核
  -> 正式 SKU 暂不更新
  -> 通过：把 AFTER 快照应用到 product_sku / SKU 图片 / 来源绑定
  -> 驳回：正式 SKU 不变
```

### 4.5 价格变更

```text
读取当前 SKU 价格
  -> 生成价格 AFTER 快照和风险摘要
  -> 提交 EDIT_PRICE 审核
  -> 正式 SKU 价格暂不更新
  -> 通过：更新供货价 / 销售价 / 币种
  -> 驳回：价格不变
```

价格审核必须突出风险：低于供货价、币种变化、涨跌幅、影响 SKU 数。

## 5. 审核列表设计

### 5.1 页面结构

建议用一个页面，不拆多个路由：

```text
商品审核
  类型 Tabs：全部 / 新增商品 / 新增SKU / 商品资料变更 / SKU资料变更 / 价格变更
  状态筛选：待审核 / 已通过 / 已驳回 / 已撤回
  ProTable 列表
```

默认进入：

```text
类型=全部
状态=PENDING
```

### 5.2 固定列

所有类型都展示：

- 审核类型
- 商品摘要：主图、标题、系统 SPU、客户 SPU
- 卖家
- 审核状态
- 风险等级 / 风险标签
- 提交来源
- 提交人
- 提交时间
- 操作

### 5.3 类型化摘要列

不同类型在列表中间展示不同重点：

| 类型 | 列表重点 |
| --- | --- |
| `NEW_PRODUCT` | SKU 数、价格区间、币种、仓库摘要、类目 |
| `ADD_SKU` | 新增 SKU 数、新 SKU 价格区间、仓库/来源 SKU 绑定摘要 |
| `EDIT_PRODUCT_INFO` | 变更字段数、标题/主图/详情/类目属性是否变化 |
| `EDIT_SKU_INFO` | 影响 SKU 数、规格/图片/尺寸/仓库/来源绑定变化 |
| `EDIT_PRICE` | 影响 SKU 数、原价格区间、新价格区间、毛利风险 |

行内操作：

- 待审核：查看、通过、驳回、审核记录。
- 非待审核：查看、审核记录。

遵守当前前端规则：直接展示不超过 2 个高频操作，其余放入「更多」。

## 6. 审核详情设计

详情页统一结构，但按类型默认展开不同区块：

- 审核摘要
- 变更前后对比
- 商品买家预览
- SKU 明细
- 发货仓库
- 审核记录

默认展开规则：

| 类型 | 默认重点 |
| --- | --- |
| `NEW_PRODUCT` | 买家预览、SKU 明细、类目属性、仓库 |
| `ADD_SKU` | 新增 SKU 明细、SKU 图、价格、仓库/来源绑定 |
| `EDIT_PRODUCT_INFO` | SPU 字段 diff、主图/详情对比、属性变化 |
| `EDIT_SKU_INFO` | SKU 字段 diff、规格/尺寸/图片/仓库变化 |
| `EDIT_PRICE` | 价格 diff、供货价/销售价/币种、毛利风险 |

审核员不应该靠肉眼找变化；后端应提供 `diff_summary` 和 `diff_json`，前端用高亮展示差异。

## 7. 数据库表设计方向

新增业务表前仍需单独确认字段级设计。当前计划建议 4 张表。

### 7.1 `product_review_request`

审核单主表，支撑列表和流程状态。

核心字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `review_id` | bigint | 审核单主键 |
| `review_no` | varchar(64) | 审核单号 |
| `review_type` | varchar(32) | 审核类型 |
| `review_status` | varchar(32) | 审核状态 |
| `spu_id` | bigint | 商品 SPU ID |
| `system_spu_code` | varchar(64) | 系统 SPU 快照 |
| `seller_id` | bigint | 卖家 ID 快照 |
| `seller_name` | varchar(255) | 卖家名称快照 |
| `category_id` | bigint | 类目 ID 快照 |
| `category_name` | varchar(255) | 类目名称快照 |
| `product_name_before` | varchar(255) | 变更前商品标题快照 |
| `product_name_after` | varchar(255) | 变更后商品标题快照 |
| `main_image_url_before` | varchar(1000) | 变更前主图快照 |
| `main_image_url_after` | varchar(1000) | 变更后主图快照 |
| `submit_terminal` | varchar(16) | `ADMIN` / `SELLER` |
| `submit_subject_id` | bigint | 提交主体 ID；seller 端为 sellerId，admin 端可为空 |
| `submit_account_id` | bigint | 提交账号 ID |
| `submit_user_name` | varchar(64) | 提交人 |
| `submit_time` | datetime | 提交时间 |
| `reviewer_id` | bigint | 审核人管理端用户 ID |
| `reviewer_name` | varchar(64) | 审核人账号 |
| `review_time` | datetime | 审核时间 |
| `review_reason` | varchar(500) | 通过说明或驳回原因 |
| `risk_level` | varchar(16) | `LOW` / `MEDIUM` / `HIGH` |
| `risk_summary` | varchar(1000) | 风险标签摘要 |
| `item_count` | int | 本次影响对象数量 |
| `sku_count` | int | 本次涉及 SKU 数 |
| `price_before_min` | decimal(18,4) | 变更前最低价 |
| `price_before_max` | decimal(18,4) | 变更前最高价 |
| `price_after_min` | decimal(18,4) | 变更后最低价 |
| `price_after_max` | decimal(18,4) | 变更后最高价 |
| `currency_summary` | varchar(64) | 币种摘要 |
| `warehouse_summary` | varchar(500) | 仓库摘要 |
| `diff_summary` | varchar(1000) | 字段变化摘要 |
| `active_pending_key` | varchar(128) | 待审冲突控制 key |
| `del_flag` | char(1) | 删除标志 |
| `create_by` | varchar(64) | 创建者 |
| `create_time` | datetime | 创建时间 |
| `update_by` | varchar(64) | 更新者 |
| `update_time` | datetime | 更新时间 |
| `remark` | varchar(500) | 备注 |

建议索引：

- `uk_product_review_no(review_no)`
- `idx_product_review_type_status(review_type, review_status, submit_time)`
- `idx_product_review_status_time(review_status, submit_time)`
- `idx_product_review_spu(spu_id, submit_time)`
- `idx_product_review_seller(seller_id, submit_time)`
- `idx_product_review_submit(submit_terminal, submit_time)`

并在 service 层做待审冲突拦截：首版同一 SPU 同一时间只允许存在一个 `PENDING` 审核单，避免资料变更和价格变更并发覆盖。

### 7.2 `product_review_item`

审核对象明细表，描述一张审核单影响哪些 SPU/SKU。

核心字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `item_id` | bigint | 明细主键 |
| `review_id` | bigint | 审核单 ID |
| `item_type` | varchar(16) | `SPU` / `SKU` |
| `change_type` | varchar(32) | `CREATE` / `UPDATE` / `PRICE_UPDATE` |
| `spu_id` | bigint | SPU ID |
| `sku_id` | bigint | SKU ID，SPU 级变更可为空 |
| `system_sku_code` | varchar(64) | 系统 SKU 快照 |
| `seller_sku_code` | varchar(128) | 客户 SKU 快照 |
| `item_status` | varchar(32) | 明细状态，首版跟随主单 |
| `before_hash` | varchar(128) | 变更前快照 hash |
| `after_hash` | varchar(128) | 变更后快照 hash |
| `diff_summary` | varchar(1000) | 本对象变化摘要 |
| `risk_summary` | varchar(1000) | 本对象风险摘要 |
| `sort_order` | int | 展示排序 |
| `create_time` | datetime | 创建时间 |

建议索引：

- `idx_product_review_item_review(review_id, sort_order)`
- `idx_product_review_item_spu(spu_id, review_id)`
- `idx_product_review_item_sku(sku_id, review_id)`

### 7.3 `product_review_snapshot`

审核快照表，保存审核时的 before / after / diff。

核心字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `snapshot_id` | bigint | 快照主键 |
| `review_id` | bigint | 审核单 ID |
| `item_id` | bigint | 审核明细 ID，可为空 |
| `snapshot_role` | varchar(16) | `BEFORE` / `AFTER` / `DIFF` |
| `payload_type` | varchar(32) | `SPU` / `SKU` / `ATTRIBUTES` / `IMAGES` / `WAREHOUSES` / `PRICE` |
| `payload_json` | longtext | 快照 JSON |
| `payload_hash` | varchar(128) | 快照 hash |
| `create_time` | datetime | 创建时间 |

建议索引：

- `idx_product_review_snapshot_review(review_id, snapshot_role)`
- `idx_product_review_snapshot_item(item_id, snapshot_role)`

### 7.4 `product_review_operation_log`

审核操作日志表，记录提交、通过、驳回、撤回。

核心字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `log_id` | bigint | 日志主键 |
| `review_id` | bigint | 审核单 ID |
| `spu_id` | bigint | SPU ID |
| `operation_type` | varchar(32) | `SUBMIT` / `APPROVE` / `REJECT` / `WITHDRAW` |
| `before_status` | varchar(32) | 操作前审核状态 |
| `after_status` | varchar(32) | 操作后审核状态 |
| `operator_terminal` | varchar(16) | 操作端 |
| `operator_id` | bigint | 操作账号 ID |
| `operator_name` | varchar(64) | 操作人 |
| `operation_time` | datetime | 操作时间 |
| `reason` | varchar(500) | 操作原因 |
| `remark` | varchar(500) | 备注 |

建议索引：

- `idx_product_review_log_review(review_id, operation_time)`
- `idx_product_review_log_spu(spu_id, operation_time)`
- `idx_product_review_log_type(operation_type, operation_time)`

## 8. 正式商品表写入规则

### 8.1 可以先写正式表的情况

新增商品和新增 SKU 可以先写入正式商品表，但必须保持不可售状态：

- 新增商品：SPU/SKU 为 `DRAFT`。
- 新增 SKU：SKU 为 `DRAFT`，原 SPU 状态不变。

这些数据不进入买家可见列表，审核通过后才进入 `READY`。

### 8.2 不能先写正式表的情况

已有商品资料、已有 SKU 资料和价格变更不能先改正式表：

- 不提前改 `product_spu` 标题、主图、详情。
- 不提前改 `product_sku` 规格、尺寸、图片、仓库绑定。
- 不提前改供货价、销售价、币种。

这些变更先进入 `product_review_snapshot.AFTER`，审核通过后由 service 事务化应用。

## 9. 权限和接口计划

沿用现有菜单权限前缀，后续再决定是否统一重命名：

- `review:productDistribution:list`
- `review:productDistribution:query`
- `review:productDistribution:approve`
- `review:productDistribution:reject`
- `review:productDistribution:log`

建议后端接口放在 product 模块的管理端入口：

```text
GET    /product/admin/reviews/list
GET    /product/admin/reviews/{reviewId}
POST   /product/admin/reviews/{reviewId}/approve
POST   /product/admin/reviews/{reviewId}/reject
POST   /product/admin/reviews/{reviewId}/withdraw
GET    /product/admin/reviews/{reviewId}/logs
```

商品提交审核入口按来源区分：

```text
POST /product/admin/distribution-products/{spuId}/submit-review
POST /seller/product/distribution-products/{spuId}/submit-review
```

seller 端接口必须从 portal token 推导 sellerId，不允许相信前端传入 `sellerId`。

## 10. 实施阶段

### 阶段 1：确认字段级表设计

- 基于本计划输出 SQL 字段级设计稿。
- 明确字典项：审核类型、审核状态、风险等级、操作类型。
- 确认是否首版允许撤回。
- 确认同一 SPU 是否只允许一个待审单。

### 阶段 2：数据库迁移

- 新增 4 张审核表。
- 新增审核类型、审核状态、风险等级字典。
- 补审核中心按钮权限。
- SQL 必须带确认 token、`45000` fail-closed、索引 guard。
- 远端执行前单独生成执行记录并等待确认。

### 阶段 3：后端审核底座

- 新增审核领域对象、Mapper、Service、Controller。
- 新增提交审核、通过、驳回、撤回。
- 新增 before/after/diff 快照构造。
- 新增待审冲突 guard。
- 审核通过时按 `review_type` 应用变更。

### 阶段 4：收紧商品现有状态入口

- 商城商品列表不能再直接执行 `DRAFT -> READY` 绕过审核。
- `DRAFT` 商品只允许提交审核。
- `READY -> ON_SALE`、`ON_SALE -> OFF_SALE`、`OFF_SALE -> ON_SALE` 继续保留在商城商品列表。

### 阶段 5：管理端页面

- 商品审核页面替换占位页。
- 类型 Tabs + 状态筛选 + ProTable。
- 不同类型展示不同业务摘要列。
- 详情页支持 before/after/diff。
- 通过和驳回弹窗必须填写必要信息。

### 阶段 6：商品编辑链路接入

- 管理端新增商品保存草稿后提交审核。
- 管理端已有商品编辑资料、编辑 SKU、调价改为生成审核单。
- 新增 SKU 走 `ADD_SKU`。
- 审核中冻结相关 SPU/SKU 编辑。

### 阶段 7：seller 端接入

- seller 端补新增/编辑/提交审核入口。
- seller 范围继续从 token 推导。
- 驳回原因在 seller 端可见。
- seller 端不能看到内部审核字段，例如审核人管理端 ID、内部 diff 原始 JSON。

### 阶段 8：验证和记录

- 后端单元测试：新增商品、新增 SKU、资料变更、价格变更、驳回不生效、通过才生效。
- 合同测试：权限点、菜单 seed、SQL guard、DRAFT 不能绕过审核。
- 前端测试：权限按钮、类型 Tabs、状态筛选、详情 diff 展示。
- API 烟测：提交、列表、详情、通过、驳回。
- 浏览器验证：商品审核页、商品列表状态变化、驳回回到草稿。
- 更新复用台账和执行记录。
- 运行 `codegraph sync .`。

## 11. 当前不做

- 不做跨模块通用审核引擎。
- 不做 SKU 级部分通过；首版同一审核单整体通过或整体驳回。
- 不做并发多待审单；首版同一 SPU 同时只允许一个待审审核单。
- 不做买家端审核能力。
- 不在未确认表设计前新增 DDL、Entity、Mapper、Service、Controller 或页面。
