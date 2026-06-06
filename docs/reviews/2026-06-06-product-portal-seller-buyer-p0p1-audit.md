# 三端隔离 P0/P1 审计（Product 模块 + Seller/Buyer Portal 商品接口）

## 结论先行
- **P0：无明确阻断级缺陷**
- **P1：`buyer` portal 商品列表/详情域未按 `buyerId`（当前 buyer 主体）约束**

审计范围：`RuoYi-Vue/` 后端商品/Portal 接口 + `react-ui/src/pages/Portal/Home/` 与 `react-ui/src/services/portal/session.*`

---

## 1）seller 已迁移而 buyer 未复制的 facade/service
- 结论：**未见“seller 已有而 buyer 缺失”**，两端 portal 商品读取链路均有对应 controller/service/facade/domain。
- 证据
  - seller 分发列表 + 单条 + SKU：
    `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductDistributionController.java:27-63`
  - seller schema 接口：
    `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductSchemaController.java:27-40`
  - seller facade：
    `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/ISellerPortalProductService.java:12-18`
    `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java:21-104`
  - buyer 分发列表 + 单条 + SKU：
    `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalProductDistributionController.java:27-63`
  - buyer schema 接口：
    `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalProductSchemaController.java:27-40`
  - buyer facade：
    `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/IBuyerPortalProductService.java:12-18`
    `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImpl.java:24-76`

---

## 2）商品数据范围是否可能串 seller / buyer
- 结论：
  - **seller** 侧是按会话 subject 严格约束 seller 商品范围（会话注入 sellerId 后做查询+二次校验）。
  - **buyer** 侧当前是“公开在售可见”查询，未带任何 `buyerId` 维度。
- 证据
  - seller 构造查询时写入 `scoped.setSellerId(session.getSubjectId())`：
    `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java:54-71`
  - seller 二次校验 `product.getSellerId()` 与 session subject 一致：
    `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java:93-104`
  - seller 使用 `productDistributionService.selectProductList(...)`（受 `sellerId` 查询条件约束）：
    `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java:32-34`
  - buyer 的 `buildVisibleProductQuery` 只放 keyword/name/category，无 subject 维度：
    `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImpl.java:58-71`
  - buyer 使用 `selectOnSaleProductList`（SQL 仅按 `ON_SALE` / `NORMAL` + 常规搜索过滤）：
    `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImpl.java:32-37`
    `RuoYi-Vue/product/src/main/resources/mapper/product/ProductDistributionMapper.xml:280-287`
    `RuoYi-Vue/product/src/main/resources/mapper/product/ProductDistributionMapper.xml:312-316`

---

## 3）portal 商品接口是否信任前端 subjectId
- 结论：当前证据看不出前端 `subjectId` 直接作为权限/范围来源；会话主体来自 token+服务端缓存与 aspect 注入。
- 证据
  - 前端会将以下 scope 字段从 query 里清理（`subjectId`、`sellerId`、`buyerId` 等）：
    `react-ui/src/services/portal/session.ts:7-11`, `react-ui/src/services/portal/session.ts:23-27`
  - 入口请求不传前端 subject 参数给商品接口（仅业务过滤）：
    `react-ui/src/pages/Portal/Home/SellerOwnDistributionProductList.tsx:276-284`
    `react-ui/src/pages/Portal/Home/BuyerDistributionProductList.tsx:240-244`
  - controller 均从 `PortalSessionContext.requireSession("seller|buyer")` 获取会话：
    `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductDistributionController.java:39-44`
    `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalProductDistributionController.java:39-44`
  - `PortalPreAuthorizeAspect` 在调用前通过 `portalPermissionChecker.requireAuthorized(...)` 设置 `PortalSessionContext`：
    `RuoYi-Vue/ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/PortalPreAuthorizeAspect.java:30-44`
  - token 会话从请求头解析并按 terminal+tokenId 查 redis，不依赖 query 参数：
    `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java:90-121`

---

## 4）权限 seed 是否覆盖
- 结论：`seller/buyer` portal 商品类权限与 owner 角色菜单绑定已在主 seed 中有落地，且有补种文件。
- 证据
  - 主种子：seller 商品类 perms：
    `RuoYi-Vue/sql/seller_buyer_management_seed.sql:854-857`
  - 主种子：buyer 商品类 perms：
    `RuoYi-Vue/sql/seller_buyer_management_seed.sql:874-877`
  - 主种子：seller/buyer owner 菜单关联包含上述商品 perms：
    `RuoYi-Vue/sql/seller_buyer_management_seed.sql:918-942`
  - 扩展 seed（schema/category 的补丁）：
    - `RuoYi-Vue/sql/20260604_seller_product_schema_permission_seed.sql:42-47`
    - `RuoYi-Vue/sql/20260604_buyer_product_schema_permission_seed.sql:43-47`
    - `RuoYi-Vue/sql/20260604_portal_product_category_permission_seed.sql:3-16,21-47`

---

## P0/P1 归档
- **P0（阻断级）**：未发现由前端注入 `subjectId` 绕过权限导致跨端/跨主体访问 seller/buyer 商品的直接证据，且控制链路已在 Portal session+`@PortalPreAuthorize`。
- **P1（高优先行为）**：买家端商品查询逻辑仅按“在售/正常”过滤，未按 buyer 主体可见性（如 buyer 维度白名单）做范围约束；若产品域设计要求 buyer-only 可见性，则需补齐。

---

## 最小修复建议（不改造过度）
1. 明确业务语义后收敛：
   - 若 buyer 端应为“对接入 buyer 可见商品”，在 `BuyerPortalProductServiceImpl#buildVisibleProductQuery` 与 `BuyerPortalProductMapper` 查询链路补齐 buyer 维度过滤（例如 `buyerId` 或 buyer-product 可见关系表）。
   - 同步补充测试：buyer 空 subject、他方买家/非可见 spu 的列表与详情访问均应拒绝/过滤。
2. 如保持 buyer“公共在售目录”模型，则在业务文档与审计记录中固化该边界，避免后续误判为隔离缺失。
3. 补充一条只读回归验证：确保 buyer/seller portal 商品菜单与按钮权限都走各自终端 menus（seller/buyer）且不依赖 `sys_*` 菜单 perm。
