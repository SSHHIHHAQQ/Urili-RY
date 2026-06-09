# Seller/Buyer 管理端接口 P0/P1 扫描报告（只读）

- 日期：2026-06-08
- 范围：`RuoYi-Vue/seller`、`RuoYi-Vue/buyer` 后端管理端（Admin Seller/Buyer + permission/role/menu/dept/service/mapper）
- 规则依据：`AGENTS.md` + `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`
- 状态：**只读扫描，未做代码修改**

## 结论（按严重级别）

### P0
- **无明确 P0 缺口**。

### P1
- **无明确 P1 缺口**。

## 关键核验（问题导向）

### 1) `AdminSellerController` / `AdminBuyerController` 权限标识完整性
- 证据：
  - seller 所有端点方法均有 `@PreAuthorize`，例如 `list`、`get`、`add`、`edit`、`sessions`、`forceLogout`、`directLogin`、`loginLogs` 等（`E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\controller\AdminSellerController.java:44-227`）。
  - buyer 对应方法同构且无缺口（`E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\controller\AdminBuyerController.java:44-227`）。
  - 角色/菜单/部门管理控制器同构对齐（映射数与权限注解数一致）。
    - seller role/menu/dept controller 主要权限点：
      - `E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\controller\AdminSellerRoleController.java:33-81`
      `E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\controller\AdminSellerMenuController.java:32-79`
      `E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\controller\AdminSellerDeptController.java:31-68`
    - buyer 同构对应：
      `E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\controller\AdminBuyerRoleController.java:33-81`
      `E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\controller\AdminBuyerMenuController.java:31-78`
      `E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\controller\AdminBuyerDeptController.java:31-68`
- 建议：继续保持 seller/buyer 两端一一映射的授权策略；新增端点时优先复制并同步测试覆盖。

### 2) seller/buyer 串端风险
- 证据：
  - 菜单校验依赖端内 ID 段和端内校验器（`assertTerminalMenuId` + `assertTerminalMenuComponent` + `assertTerminalMenuPerms`），见：
    - seller：`E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerPortalPermissionServiceImpl.java:304-322`
    - buyer：`E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerPortalPermissionServiceImpl.java:304-322`
  - 菜单表计数 SQL 仅接受各端 ID 段（seller 100000-199999，buyer 200000-299999）：
    - `SellerPortalPermissionMapper.xml:324`
    - `BuyerPortalPermissionMapper.xml:324`
- 建议：若未来历史表结构从独立端内菜单表切到跨端共享表，需将菜单计数与角色权限操作改为显式 subject 约束（当前环境下未发现串端。）。

### 3) 裸 accountId 查询
- 证据：管理端账号/会话/日志等服务层都使用 `sellerId + accountId` / `buyerId + accountId` 的双主键查询。
  - `SellerMapper.java`: `selectSellerAccountByIdAndSellerId`（`E:\Urili-Ruoyi\RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\mapper\SellerMapper.java:34`）
  - `BuyerMapper.java`: `selectBuyerAccountByIdAndBuyerId`（`E:\Urili-Ruoyi\RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\mapper\BuyerMapper.java:34`）
  - 日志范围也强制用 `resolveSellerAdminLogSubjectId/resolveBuyerAdminLogSubjectId`：
    - `...\SellerServiceImpl.java:481-493`
    - `...\BuyerServiceImpl.java:481-493`
- 建议：保留该契约，不新增仅按 accountId 的 mapper/service 接口。

### 4) 是否复用 `sys_user/sys_role/sys_menu`
- 证据：在 seller/buyer 两个管理域内扫描 `SysUser` `SysRole` `SysMenu` 关键字，计数为 0（无命中）。
- 建议：继续保持；账号/角色/菜单隔离保持在 seller/buyer 表与实体链路。

### 5) 卖家已修但买家未同步
- 证据：seller 与 buyer 在 AdminController、角色/菜单/部门控制器、权限服务方法上为同构映射（方法数和权限语义对齐），未见 buyer 少一套权限动作。
- 建议：该项当前通过。

---

## 结论
- 本次只读扫描未发现需要马上阻断发布的 **P0/P1** 缺口。
- 可继续把该结论固化为验收记录，并在后续新端口/新端点新增时按此规则复用对照。
