# KAT-F-CA012 报价试算商品测试数据执行方案

## 目标

为报价方案 1（测试1）补齐可用于费用试算的商城商品 SKU。目标是让 `KAT-F-CA012` 仓库下至少有几个已经绑定上游履约仓来源 SKU、且来源仓有库存的商品，后续可以跑 `/finance/admin/fee-estimate/calculate`。

## 当前只读核对结果

- 当前后端配置从 `.env.local` 注入 `RUOYI_DB_*` 和 `RUOYI_REDIS_*`，本次只读核对通过后端接口完成，没有读取本地 Docker 默认库。
- 报价方案 1：
  - 方案名称：测试1
  - 方案类型：`BILLING`
  - 费用来源：`EXTERNAL_ESTIMATE`
  - 币种：`USD`
  - 绑定仓库：`KAT-F-CA012`
  - 启用客户渠道：`UPS-S`、`USPS`、`USPS-SF`
- 系统仓 `KAT-F-CA012`：
  - warehouseId：8
  - 仓库名称：KAT测试仓-CA012
  - 类型：官方仓
  - 国家/币种：US / USD
  - 状态：正常
- 上游配对：
  - 履约仓配对：`LX-KAT库存仓-58F5A0B6` / `US015` / `FULFILLMENT` / ACTIVE
  - 报价仓配对：`LX-KAT-91B1E277` / `CA012` / `QUOTE` / ACTIVE
- 当前商城 SKU：
  - `sourceWarehouseCode=KAT-F-CA012` 查询为空。
  - 费用试算 SKU 列表里也没有 `KAT-F-CA012` 的可选 SKU。

## 可用来源 SKU

以下来源 SKU 均来自履约仓 `LX-KAT库存仓-58F5A0B6`，来源仓名为 `KAT履约测试仓（有库存）`，都在 `US015` 上有正库存，并且尺寸重量完整、当前未绑定商城 SKU。

| 来源 SKU | 来源商品名 | 可用库存示例 | 币种 | 来源绑定 key |
|---|---|---:|---|---|
| CESHIAL0008 | 猫咪书签测试 | 39+ | USD | `OFFICIAL_MASTER:4a58c40f0eb8d90536aeec1764e1a440defd9a1caebdae55936065f9ec9a03aa:dd24e827e3b825bda92469adb799195de244d15fef2151b6324192c7dba9e107` |
| CESHIAL0012 | 回形针便利贴套装测试 | 有库存 | USD | `OFFICIAL_MASTER:534d5b5691f1577c0881c70d07962d53acc0ef7786e5ad296943af410061a2a8:e145f1ab0c129195ee24d787aab83e057ffd126383f83f1fd55a79cd95fa78f4` |
| CESHIAL0019 | 马尼拉文件夹测试 | 59+ | USD | `OFFICIAL_MASTER:53f30b0c33f83527c4c26a786873b8f31f00e4ccdbf57b286790ff94ca83b713:41aa23411bf8d7a83f742143e8141fec1e1d1092a321762593015aab28de342a` |

## 拟写入方式

通过管理端商品新增接口写入，不直接改表：

- 接口：`POST /product/admin/distribution-products`
- 原因：该接口会走完整业务逻辑，自动生成系统 SPU/SKU，校验来源 SKU 尺寸重量，派生官方仓，写入 `product_sku_source_binding`，并同步上游 SKU 配对投影。

拟新增 3 个草稿 SPU，每个 SPU 1 个 SKU：

| 商品 | 卖家 | 类目 | 仓库类型 | SKU 绑定 |
|---|---|---|---|---|
| KAT CA012 试算商品-猫咪书签 | `sellerId=41` Hefei Mingtai Storage Solutions Co., Ltd. | `categoryId=446` T恤 | `official` | CESHIAL0008 |
| KAT CA012 试算商品-便利贴套装 | `sellerId=41` Hefei Mingtai Storage Solutions Co., Ltd. | `categoryId=446` T恤 | `official` | CESHIAL0012 |
| KAT CA012 试算商品-马尼拉文件夹 | `sellerId=41` Hefei Mingtai Storage Solutions Co., Ltd. | `categoryId=446` T恤 | `official` | CESHIAL0019 |

类目 446 有必填属性 `clothing_length`，写入时统一使用默认选项 `CROP`。SKU 规格最少需要一个字段，写入时使用 `model` 分别填来源 SKU 编码。

## 验证步骤

1. 新增 3 个商品后，查询 `/product/admin/distribution-products/skus/list?sourceWarehouseCode=KAT-F-CA012`，确认能看到 3 个 SKU。
2. 查询 `/finance/admin/fee-estimate/skus/list?sourceWarehouseCode=KAT-F-CA012`，确认费用试算 SKU 选择器能看到 3 个 SKU。
3. 调用 `/finance/admin/fee-estimate/calculate`：
   - `quoteSchemeId=1`
   - `selectionMode=AUTO`
   - `warehouseCodes=["KAT-F-CA012"]`
   - 目的地先使用美国地址
   - 包裹行选择新增的 SKU
4. 记录返回结果：
   - 如果能跑到外部试算，记录每个渠道的结果。
   - 如果失败，记录失败码和失败位置。

## 当前缺口

`URILI_LINGXING_FEE_ESTIMATE_PATH` 当前未配置。也就是说，商品和绑定补齐后，链路很可能能走到领星外部试算适配器，但会失败在“领星费用试算接口路径未配置”。

需要二选一：

1. 只先创建商品并验证 SKU 列表、候选仓库和渠道链路，外部费用试算暂时接受预期失败。
2. 同时配置真实 `URILI_LINGXING_FEE_ESTIMATE_PATH` 后重启后端，再跑完整外部费用试算。该 path 需要确认真实领星接口路径。

## 回滚方式

如果需要回滚测试数据，优先通过商品删除接口删除草稿商品：

- `DELETE /product/admin/distribution-products/{spuId}`

该接口会释放未锁定的来源 SKU 绑定。若商品后续被提交审核或上架，不能再按草稿删除，需要另写回滚方案。
