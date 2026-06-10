# 2026-06-06 Seller/Buyer Portal + Admin P0/P1 审计（仅读）

## 审计范围
- 模块：`RuoYi-Vue/seller`、`RuoYi-Vue/buyer`
- 关注范围：`portal` 与 `admin` 下的 Controller 与 Service 当前实现（只读）
- 目标对齐：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

## 结论快照（只报 P0/P1）
- P0：未发现明显 P0。
- P1：发现 4 个待修复点，均为会话查询链路里“仅凭 URL 端路径 ID 查询未做前置归属存在性校验”。

## 编译风险（P0/P1）
- 历史当轮运行验证：`mvn -pl seller -DskipTests compile -q`、`mvn -pl buyer -DskipTests compile -q`
  - 结果：两条命令均返回成功（exit 0）
  - 结论：当前切片内未见编译阻断风险。
- 当前可复用验证命令必须带 reactor 依赖：`mvn -pl seller,buyer -am -DskipTests compile -q`。

## P1：接口缺失（P1）
- P1 级问题未发现：
  - `AdminSellerController` 与 `AdminBuyerController` 调用的 service 方法在实现层均有对应定义；`mvn compile` 已覆盖接口/实现匹配一致。
  - 未发现未定义方法导致的运行前编译失败风险。

## P1：权限缺失（P0/P1）
- 当前切片内未发现明显权限缺失：
  - `admin` 侧会话/账号/菜单/角色/日志/免密代入接口均有 `@PreAuthorize` 保护。
  - `portal` 侧会话与业务接口以 `@PortalPreAuthorize + @Anonymous` + `PortalSessionContext.requireSession("seller|buyer")` 组合进行鉴权和上下文拉取。
- 风险说明：登录接口（`SellerPortalAuthController`/`BuyerPortalAuthController` 的 `/login`、`/direct-login`）无 `@PreAuthorize`，但为登录入口行为，当前不列为本轮 P0/P1 风险。

## P1：串端与前端传 ID 信任检查
- 仅检出下列会话查询链路：`admin` 列表查询未对 `sellerId/buyerId` 做存在性与归属前置校验。

1) Seller 管理端会话列表
- 风险点
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:170-183`
    - `GET /{sellerId}/sessions/list` 调 `sellerService.selectSellerSessionList(sellerId)`
    - `GET /{sellerId}/accounts/{accountId}/sessions/list` 调 `sellerService.selectSellerAccountSessionList(sellerId, accountId)`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:263-272`
    - `selectSellerSessionList`/`selectSellerAccountSessionList` 直接透传 `sellerId/accountId` 到 mapper
- 风险说明
  - 当前方法不会先确认 `sellerId` 存在，也不会确认 `sellerAccountId` 的归属关系。
  - 即使权限控制层面已限制接口入口，此处仍属于“信任前端路径 ID”风险的 P1 级输入边界问题（空壳 ID/无效 ID 会静默行为；账号归属异常未显式拒绝）。

2) Buyer 管理端会话列表
- 风险点
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:168-183`
    - 对应 `buyerService.selectBuyerSessionList` / `selectBuyerAccountSessionList` 的调用
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:263-272`
    - `selectBuyerSessionList`/`selectBuyerAccountSessionList` 直接透传 `buyerId/accountId` 到 mapper
- 风险说明
  - 与 Seller 相同，缺少服务层级入口前置校验。

### 最小修复建议（不改行为边界，只补完整性）
- [Seller] `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `selectSellerSessionList(Long sellerId)` 中先 `selectSellerById(sellerId);` 再查询会话
  - `selectSellerAccountSessionList(Long sellerId, Long sellerAccountId)` 中先 `selectSellerById(sellerId);`，再 `selectSellerAccountById(sellerId, sellerAccountId)` 校验归属
- [Buyer] `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - `selectBuyerSessionList(Long buyerId)` 中先 `selectBuyerById(buyerId);` 再查询会话
  - `selectBuyerAccountSessionList(Long buyerId, Long buyerAccountId)` 中先 `selectBuyerById(buyerId);`，再 `selectBuyerAccountById(buyerId, buyerAccountId)` 校验归属
- 可选增强（保持一致性）
  - 在 `seller`、`buyer` 的会话列表 service 方法抛出统一异常信息（如“主体不存在/账号不属于该主体”）以便审计与前端提示统一。

## P1：sys_* 混用（账号/角色/菜单/部门/日志/会话）
- `rg` 在 `RuoYi-Vue/seller` 与 `RuoYi-Vue/buyer` 范围未检出 `sys_user/sys_role/sys_menu/sys_dept/sys_oper_log/sys_login_info` 直接使用。
- 结论：本切片内未见管理端以 `sys_*` 复用到 `seller/buyer` 账号、角色、部门、菜单、日志、会话体系。

## 关键命令与范围
- 历史编译命令（仅记录当轮事实，不作为当前可复用门禁）：
  - `cd RuoYi-Vue; mvn -pl seller -DskipTests compile -q`
  - `cd RuoYi-Vue; mvn -pl buyer -DskipTests compile -q`
- 当前可复用编译门禁：
  - `cd RuoYi-Vue; mvn -pl seller,buyer -am -DskipTests compile -q`
- 文件范围（本报告仅读）：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
- 参考上下文：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java:187-201`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java:187-201`
