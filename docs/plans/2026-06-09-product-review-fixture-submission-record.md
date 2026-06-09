# 商品审核测试数据提交记录

## 执行目标

应用户要求，在商城商品列表侧创建 25 条待审核数据，用于商品审核菜单测试。

目标分布：

- 新增商品审核：5 条
- 新增 SKU 审核：5 条
- 商品资料变更审核：5 条
- SKU 资料变更审核：5 条
- 价格变更审核：5 条

说明：当前系统已有的价格审核入口是 `EDIT_PRICE` 销售价变更接口 `/product/admin/distribution-products/skus/sale-prices`，不是供货价变更专用入口。本次按现有商品列表侧价格审核能力提交 5 条 `EDIT_PRICE`。

## 执行方式

- 执行时间：2026-06-09 01:41 - 01:45
- 执行入口：管理端后端接口 `http://127.0.0.1:8080`
- 登录账号：admin
- 数据写入方式：只通过商品列表侧/审核相关后端 API 执行，未直接执行 SQL。
- 后端重启：为修正 SKU 资料变更类型判断，先重新打包并重启 `ruoyi-admin.jar`。

## 关键修正

提交 SKU 资料变更前发现审核类型判断会把 `product_spu_warehouse.id` 这类仓库绑定行技术字段纳入 SPU 快照差异，导致“只改 SKU”可能被误判为“商品资料变更”。

已将商品审核 SPU 对比中的仓库快照归一化为稳定业务字段：

- `warehouseId`
- `warehouseCode`
- `warehouseName`
- `warehouseKind`
- `settlementCurrency`
- `sellerId`

## 最终待审核分布

接口校验 `/product/admin/reviews/list?reviewStatus=PENDING` 结果：

| 审核类型 | 数量 |
| --- | ---: |
| `NEW_PRODUCT` | 5 |
| `ADD_SKU` | 5 |
| `EDIT_PRODUCT_INFO` | 5 |
| `EDIT_SKU_INFO` | 5 |
| `EDIT_PRICE` | 5 |
| 合计 | 25 |

## 待审核明细

### NEW_PRODUCT

| reviewId | SPU | 商品 |
| ---: | --- | --- |
| 2 | `SPU202606090001` | Audit Fixture New Product 1 |
| 19 | `SPU202606090002` | Audit Fixture NEWP Product 1 |
| 20 | `SPU202606090003` | Audit Fixture NEWP Product 2 |
| 21 | `SPU202606090004` | Audit Fixture NEWP Product 3 |
| 22 | `SPU202606090005` | Audit Fixture NEWP Product 4 |

### ADD_SKU

| reviewId | SPU | 商品 |
| ---: | --- | --- |
| 3 | `SPU202606050017` | A类加厚吸水浴巾 |
| 4 | `SPU202606050016` | 亲肤磨毛被套 |
| 5 | `SPU202606050012` | 城市缓震跑步鞋 |
| 6 | `SPU202606050009` | 收腰碎花雪纺连衣裙 |
| 7 | `SPU202606050007` | 女士基础纯棉短袖T恤 |

### EDIT_PRODUCT_INFO

| reviewId | SPU | 商品 |
| ---: | --- | --- |
| 8 | `SPUREAL202606080007` | Audit Fixture Product Info 1 QA20260608174103 |
| 9 | `SPUREAL202606080006` | Audit Fixture Product Info 2 QA20260608174103 |
| 10 | `SPUREAL202606080004` | Audit Fixture Product Info 3 QA20260608174103 |
| 11 | `SPUREAL202606080003` | Audit Fixture Product Info 4 QA20260608174103 |
| 12 | `SPUREAL202606080002` | Audit Fixture Product Info 5 QA20260608174103 |

### EDIT_SKU_INFO

| reviewId | SPU | 商品 |
| ---: | --- | --- |
| 13 | `SPUREAL202606080001` | 男士免烫牛津长袖衬衫 |
| 23 | `SPUREAL202606080005` | 防泼水多隔层通勤双肩包 |
| 25 | `SPU202606090006` | Audit Fixture BASE Product 1 |
| 27 | `SPU202606090007` | Audit Fixture BASE Product 2 |
| 29 | `SPU202606090008` | Audit Fixture BASE Product 3 |

### EDIT_PRICE

| reviewId | SPU | 商品 |
| ---: | --- | --- |
| 15 | `SPU202606050002` | 速干弹力男士Polo衫 |
| 14 | `SPU202606050004` | 直筒水洗男士牛仔裤 |
| 17 | `SPUDEMO202606050003` | 防泼水通勤双肩包 |
| 16 | `SPUDEMO202606050004` | 纯棉宽松短袖 T 恤 |
| 18 | `SPUDEMO202606050002` | 城市缓震跑步鞋 |

## 额外说明

- 为补足 `EDIT_SKU_INFO` 候选，额外创建了 3 个基准商品并通过其新增商品审核，再提交 SKU 资料变更审核：`SPU202606090006`、`SPU202606090007`、`SPU202606090008`。
- 尝试使用官方仓商品 `SPU202606050020` 提交 SKU 资料变更时，后端返回“官方仓 SKU 必须选择来源 SKU”，因此没有把该商品计入最终 25 条待审核数据。
- 本次最终验证只统计 `reviewStatus=PENDING` 的审核单，结果为 25 条。

## 验证

- `mvn -pl product -Dtest=ProductReviewServiceImplTest test`：通过，10 tests。
- `mvn -pl ruoyi-admin -am package -DskipTests`：通过。
- `http://127.0.0.1:8080/captchaImage`：返回 `code=200`，后端就绪。
- 审核列表接口统计：`NEW_PRODUCT` / `ADD_SKU` / `EDIT_PRODUCT_INFO` / `EDIT_SKU_INFO` / `EDIT_PRICE` 各 5 条。
