# 2026-06-09 三端分离路线偏移核查

- 核查时间：2026-06-09 23:55，Asia/Shanghai
- 核查目的：确认买家端/卖家端框架是否被掺入商品审核、库存、WMS 等非当前需求业务
- 核查范围：`RuoYi-Vue/seller`、`RuoYi-Vue/buyer`、`react-ui/src/pages/Portal`、`react-ui/src/services/portal`、`react-ui/src/services/seller`、`react-ui/src/services/buyer`、`RuoYi-Vue/sql` 以及 product/inventory/integration 管理端控制器权限
- 执行边界：只读核查；未修改业务代码；未执行远端 MySQL DDL/DML；未读取或写入 Redis；未启动或重启服务

## 1. 结论

当前没有证据表明买家端/卖家端正在做“商品审核、库存、WMS”这些业务功能。

但有一个范围偏移需要明确：买家端/卖家端 portal 已经接入了商品相关只读样板，包括：

- 商品分类 / 商品 schema 预览。
- 卖家端“我的商城商品”列表、详情、SKU。
- 买家端“商城商品”列表、详情、SKU。
- `seller_buyer_management_seed.sql` 默认写入并授权 `seller:product:*` / `buyer:product:*` 相关端内权限。

这部分不是商品审核，也不是库存或 WMS；它更像是为了验证 portal 权限、session scope 和共享 product 只读接口而做的业务样板。按用户当前收窄后的需求“只要买家端/卖家端框架能登录、能调整，业务菜单后续慢慢做”，这部分应视为超出当前最小框架目标。

## 2. 未发现跑偏的点

### 商品审核

- 未在 `RuoYi-Vue/seller/src/main`、`RuoYi-Vue/buyer/src/main`、`react-ui/src/pages/Portal` 中发现 `ProductReview`、`商品审核`、`review` 作为 seller/buyer 端业务审核入口。
- 当前商品审核相关代码位于管理端 / product 域，例如 `RuoYi-Vue/product/src/main/java/com/ruoyi/product/controller/AdminProductReviewController.java` 和 `react-ui/src/pages/Product/Review/`。
- 判断：没有把商品审核做进买家端/卖家端。

### 库存

- 未在 seller/buyer portal 范围发现库存业务入口。
- 当前库存总览、库存调整审核等权限位于 admin/inventory 域，例如 `AdminInventoryOverviewController` 使用 `inventory:overview:*`，`AdminInventoryAdjustmentReviewController` 使用 `review:inventoryAdjustment:*`。
- 判断：没有把库存业务做进买家端/卖家端。

### WMS / 上游同步

- 未在 seller/buyer portal 范围发现 WMS 或 upstream 同步入口。
- 当前 WMS/领星/上游同步相关代码位于 integration 管理端域，例如 `AdminUpstreamSystemController`、`UpstreamSystemMapper.xml` 和 `integration:upstream:*` 权限。
- 判断：没有把 WMS / 上游同步做进买家端/卖家端。

## 3. 已经进入买家端/卖家端的超范围商品样板

### 前端 portal 首页

`react-ui/src/pages/Portal/Home/index.tsx` 会根据端内权限展示商品块：

- `product:category:list` + `product:schema:query`：显示商品 schema 预览。
- `product:distribution:list` / `product:distribution:query`：显示 seller/buyer 商品列表和详情。

对应组件：

- `SellerProductSchemaPreview.tsx`
- `BuyerProductSchemaPreview.tsx`
- `SellerOwnDistributionProductList.tsx`
- `BuyerDistributionProductList.tsx`

### 后端 seller/buyer portal 商品只读接口

已存在 seller 端商品只读接口：

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductSchemaController.java`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductDistributionController.java`

已存在 buyer 端商品只读接口：

- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalProductSchemaController.java`
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalProductDistributionController.java`

这些接口都使用 `@PortalPreAuthorize`、`@PortalLog` 和 `PortalSessionContext.requireSession(...)`，从安全边界上看没有明显串端问题；问题在于它们超出了当前“只做框架”的最小需求。

### SQL seed

`RuoYi-Vue/sql/seller_buyer_management_seed.sql` 默认写入端内商品权限：

- seller:
  - `seller:product:category:list`
  - `seller:product:schema:query`
  - `seller:product:distribution:list`
  - `seller:product:distribution:query`
- buyer:
  - `buyer:product:category:list`
  - `buyer:product:schema:query`
  - `buyer:product:distribution:list`
  - `buyer:product:distribution:query`

这些是 `menu_type = 'F'` 的隐藏按钮权限，不是完整菜单页，但默认授权后 portal 首页会显示对应商品区块。

## 4. 是否路线跑偏

按“完整三端业务系统”口径，不算跑偏，因为 seller/buyer 未来确实需要商品相关能力。

按用户当前明确口径“只做买家端/卖家端框架，能登录进去调整，业务菜单以后再做”，这里有轻微跑偏：

- 商品审核：没有跑偏到 seller/buyer。
- 库存：没有跑偏到 seller/buyer。
- WMS：没有跑偏到 seller/buyer。
- 商品浏览/schema 样板：已经进入 seller/buyer portal，属于当前阶段可撤出的超范围内容。

## 5. 建议纠偏方案

### 推荐方案：先隐藏，不大删

预计 `1-2` 小时。

做法：

1. 从 portal 首页隐藏商品 schema 和商品列表区块，只保留主体资料、当前账号、账号列表、部门、角色、会话、修改密码、退出等框架能力。
2. 从默认 owner seed 授权中移除 `seller:product:*` / `buyer:product:*`，或保留权限定义但不默认授权。
3. 保留后端商品只读接口代码和测试作为后续业务复用资产，暂不继续扩展。
4. 更新三端验证和记录，明确当前验收范围不含 seller/buyer 商品业务。

优点：风险小、最快回到用户当前需求；后续要恢复商品浏览时成本低。

### 更彻底方案：撤出 portal 商品样板代码

预计 `3-5` 小时。

做法：

1. 删除 portal 首页商品组件和 service 调用。
2. 下线 seller/buyer 商品 schema/distribution controller、service、DTO 和相关测试。
3. 清理 seed 中商品权限定义和授权。
4. 同步修改 manifest、reuse-ledger 和三端验证。

缺点：后续做 seller/buyer 商品业务时还要重新恢复一部分已经写好的安全边界代码。

## 6. 时间重新预估

如果采用推荐方案“先隐藏，不大删”，当前三端最小框架离验收大约还需要：

| 任务 | 预计时间 |
| --- | ---: |
| 隐藏 seller/buyer portal 商品区块并收窄默认授权 | 1-2 小时 |
| 复跑三端 gate 和必要前端合同测试 | 1 小时 |
| admin/seller/buyer 登录、直登、强退、日志、菜单运行态 smoke | 2-4 小时 |
| 写最终验收记录和剩余业务菜单待办 | 1 小时 |

总计：约 `5-8` 小时，保守按 `1 个工作日`。

## 7. 当前判断

没有看到“买家端/卖家端在做商品审核、库存、WMS”的路线大偏移。

但 seller/buyer portal 商品浏览样板已经超出当前最小框架目标。建议下一步先按推荐方案隐藏或撤出默认授权，把验收目标收回到“登录、账号、角色、菜单、权限、会话、日志、免密代入、强退”这条主线。
