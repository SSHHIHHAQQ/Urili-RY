# 2026-06-06 管理端卖家管理菜单权限只读检查

## 检查范围

- 工作区：`E:\Urili-Ruoyi`
- 规则：已先读取 `AGENTS.md`。
- 本次只读检查代码和 SQL，不连接 MySQL/Redis，不执行远端 DDL/DML，不启动服务。
- 目标：核对管理端是否能通过初始化 SQL、前端权限标识和后端 `@PreAuthorize` 看到并控制卖家管理。

## 总体结论

当前代码和综合初始化 SQL 的权限命名整体一致：管理端卖家管理入口是 `主体管理 / 卖家管理`，前端页面组件是 `Seller/index`，后端接口使用 `seller:admin:*` 权限族，前端按钮也按同一权限族做显示控制。

但只看代码和 SQL 后仍有三个主要风险：

1. 初始化 SQL 只创建 `sys_menu`，没有给普通管理角色写入 `sys_role_menu` 授权。超级管理员可因 `user_id=1` 和 `*:*:*` 看见全部菜单；普通管理账号必须额外绑定 `2010`、`2011` 和对应按钮权限，否则登录后菜单可能不显示或按钮隐藏/接口 403。
2. `seller:admin:directLogin` 只在综合 seed `seller_buyer_management_seed.sql` 里看到，未在独立增量 `20260604_portal_direct_login_ticket.sql` 中补 `sys_menu` 权限按钮。若运行库只执行了免密票据表增量但没重跑综合 seed，前端免密登录会隐藏，后端也会 403。
3. 前端部分弹窗有组合权限依赖：只授予按钮表面权限不一定够。例如账号弹窗需要 `seller:admin:account:list` 同时还会请求 `seller:admin:dept:query`；角色新增/编辑、菜单编辑也有查询类权限依赖。低权限角色如果只给操作权限，可能出现按钮可见但弹窗加载或提交失败。

## 菜单路径和权限标识

| 层级 | menu_id | 菜单 | 后端 path | 前端实际路由 | component | perms |
| --- | ---: | --- | --- | --- | --- | --- |
| 一级目录 | 2010 | 主体管理 | `partner` | `/partner` | `Layout` | 空 |
| 二级菜单 | 2011 | 卖家管理 | `seller` | `/partner/seller` | `Seller/index` | `seller:admin:list` |

依据：

- `RuoYi-Vue/sql/seller_buyer_management_seed.sql:578-582`
- `react-ui/src/services/session.ts:36-54` 会把 `Seller/index` 解析为 `src/pages/Seller/index.tsx`
- `react-ui/src/pages/Seller/index.tsx:1` 当前组件存在并挂载 `PartnerManagementPage`

## 管理端卖家权限点

| 功能 | 权限标识 | SQL 来源 | 后端 `@PreAuthorize` | 前端显示控制 |
| --- | --- | --- | --- | --- |
| 卖家列表 | `seller:admin:list` | 综合 seed | `AdminSellerController.list` | 菜单权限 / 页面列表接口 |
| 卖家查询 | `seller:admin:query` | 综合 seed | `AdminSellerController.get` | 详情/编辑前置查询 |
| 卖家新增 | `seller:admin:add` | 综合 seed | `AdminSellerController.add` | 新增按钮 |
| 卖家修改 | `seller:admin:edit` | 综合 seed | `AdminSellerController.edit` | 编辑按钮 |
| 卖家启停 | `seller:admin:changeStatus` | 综合 seed | `AdminSellerController.changeStatus` | 状态开关 |
| 重置主账号密码 | `seller:admin:resetPwd` | 综合 seed | `AdminSellerController.resetOwnerPassword` | 更多/重置密码 |
| 卖家免密登录 | `seller:admin:directLogin` | 综合 seed | `AdminSellerController.directLogin*` | 更多/登录卖家端 |
| 强制踢出 | `seller:admin:forceLogout` | 综合 seed、强退增量 | `AdminSellerController.sessions/logout*` | 会话/强制踢出 |
| 卖家端菜单管理 | `seller:admin:menu:list/query/add/edit/remove` | 综合 seed、端权限增量 | `AdminSellerMenuController` | 菜单弹窗 |
| 卖家端角色管理 | `seller:admin:role:list/query/add/edit/remove` | 综合 seed、端权限增量 | `AdminSellerRoleController` | 角色弹窗 |
| 卖家端部门管理 | `seller:admin:dept:list/query/add/edit/remove` | 综合 seed、端权限增量 | `AdminSellerDeptController` | 部门弹窗 |
| 登录日志 | `seller:admin:loginLog:list` | 综合 seed、审计增量 | `AdminSellerController.loginLogs` | 审计弹窗 Tab |
| 操作日志 | `seller:admin:operLog:list` | 综合 seed、审计增量 | `AdminSellerController.operLogs` | 审计弹窗 Tab |
| 免密票据 | `seller:admin:ticket:list` | 综合 seed、审计增量 | `AdminSellerController.directLoginTickets` | 审计弹窗 Tab |
| 卖家账号列表 | `seller:admin:account:list` | 综合 seed、账号增量 | `AdminSellerController.accounts` | 账号按钮 |
| 卖家账号新增 | `seller:admin:account:add` | 综合 seed、账号增量 | `AdminSellerController.addAccount` | 账号弹窗新增 |
| 卖家账号修改 | `seller:admin:account:edit` | 综合 seed、账号增量 | `AdminSellerController.editAccount` | 账号行编辑 |
| 卖家账号锁定/解锁 | `seller:admin:account:lock` | 综合 seed、账号增量、锁定增量 | `AdminSellerController.lockAccount/unlockAccount` | 账号更多菜单 |
| 卖家账号重置密码 | `seller:admin:account:resetPwd` | 综合 seed、账号增量 | `AdminSellerController.resetPassword/resetDefaultPassword` | 账号更多菜单 |
| 卖家账号角色查询 | `seller:admin:account:role:query` | 综合 seed、账号增量 | `AdminSellerController.accountRoles` | 分配角色前置 |
| 卖家账号角色分配 | `seller:admin:account:role:edit` | 综合 seed、账号增量 | `AdminSellerController.assignAccountRoles` | 分配角色提交 |

## 可能导致菜单不显示的风险

### P1：普通管理角色没有自动绑定 `sys_role_menu`

本次检查未发现卖家管理 seed 为普通管理角色插入 `sys_role_menu`。若不是超级管理员，`/getRouters` 只按 `sys_role_menu` 关联返回 `M/C` 菜单；只插入 `sys_menu` 不会自动显示。

风险表现：

- 登录后左侧没有 `主体管理` 或没有 `卖家管理`。
- 即使按钮权限存在，父菜单 `2010` 未绑定时，`2011` 会因父节点缺失在前端菜单树中变成孤儿节点。

最低授权应同时包含：

- `2010` 主体管理
- `2011` 卖家管理
- 需要控制的按钮权限，例如 `2200-2206`、`2220-2229`、`2240-2244`、`2250-2252`、`2310-2315`、`2322`

### P1：增量 seed 顺序不当会产生孤儿按钮权限

`20260604_portal_permission_admin_menu_seed.sql`、`20260604_portal_force_logout_menu_seed.sql`、`20260604_portal_audit_admin_menu_seed.sql`、`20260605_admin_seller_account_permission_seed.sql` 都把按钮挂到 `parent_id=2011`。如果这些增量早于 `seller_buyer_management_seed.sql` 执行，MySQL 不会因无外键阻止插入，但权限按钮会挂在尚不存在的菜单父节点下。

风险表现：

- 权限字符串在 `sys_menu` 里存在，但角色菜单树无法正确勾选或显示层级不完整。
- 后续再补 `2011` 后，需要确认角色授权是否包含这些按钮。

### P2：运行库若仍是旧 component，会显示占位页

当前 SQL 已使用 `Seller/index`。前端动态路由会加载 `src/pages/Seller/index.tsx`，路径是匹配的。

如果运行库仍保留旧值，例如 `Urili/Seller/index`，前端会尝试加载 `src/pages/Urili/Seller/index.tsx`，不存在时会降级到规划中占位页。由于本次禁止连接数据库，这一点未做运行库确认。

## 可能导致按钮无权限或接口 403 的风险

### P1：免密登录按钮缺少独立权限增量

`seller:admin:directLogin` 在综合 seed 中存在，但独立的 `20260604_portal_direct_login_ticket.sql` 只创建 `portal_direct_login_ticket` 表，没有补 `sys_menu` 权限按钮。

风险表现：

- 只执行免密登录增量而未重跑综合 seed 时，前端 `登录卖家端` 按钮隐藏。
- 即使前端误显示，后端 `@PreAuthorize("@ss.hasPermi('seller:admin:directLogin')")` 会拒绝。

### P2：账号弹窗需要账号权限和部门查询权限

`PartnerAccountModal` 打开时并行请求：

- `getAccounts` -> `seller:admin:account:list`
- `getDeptTree` -> `seller:admin:dept:query`

若角色只有 `seller:admin:account:list`，没有 `seller:admin:dept:query`，账号弹窗存在加载失败或部门选项为空风险。

### P2：角色管理需要角色权限和菜单树查询权限

角色新增会请求卖家端菜单树，角色编辑会请求角色详情和角色菜单树：

- 新增角色：需要 `seller:admin:role:add`，同时打开表单需要 `seller:admin:menu:query`
- 编辑角色：按钮按 `seller:admin:role:edit` 显示，但加载详情需要 `seller:admin:role:query`
- 角色菜单树接口也使用 `seller:admin:role:query`

若只给 add/edit，不给 query 类权限，按钮可能可见但表单加载失败。

### P2：菜单编辑需要 `menu:query`

菜单弹窗编辑按钮按 `seller:admin:menu:edit` 显示，但打开编辑表单会调用 `getMenu`，后端需要 `seller:admin:menu:query`。若只给 edit 不给 query，编辑入口可能显示但详情加载 403。

### P2：部门弹窗需要 `dept:list` 和 `dept:query`

部门弹窗打开时并行请求部门列表和部门树：

- `listDepts` -> `seller:admin:dept:list`
- `getDeptTree` -> `seller:admin:dept:query`

若只给 `dept:list`，部门弹窗会有树加载失败风险。

## 权限检查结果

- 后端卖家主体、账号、菜单、角色、部门、日志、票据、免密和强退接口均有 `@PreAuthorize`。
- 目前发现的后端权限字符串都能在综合 seed 中找到。
- 前端 `Seller/index.tsx` 已对账号域覆盖 `accountPermissions`，避免落入共享组件默认的主体级权限。
- 前端共享组件的按钮显示权限与后端权限命名大体一致，但若角色授权没有包含前置查询权限，仍可能出现按钮可见但接口 403。

## 字典/选项复用检查结果

本次重点不是字典，但检查到账号锁定状态前端读取 `${moduleKey}_account_lock_status`，卖家侧即 `seller_account_lock_status`；`20260605_seller_account_lock_control.sql` 和综合 seed 中均有对应字段/字典初始化。

## 复用台账检查结果

本次是只读审查，没有新增或抽取复用能力；未更新复用台账。

## CodeGraph 更新结果

未执行 `codegraph sync .`。原因：本次为只读检查，只新增 Markdown 检查报告，不修改业务代码或索引相关内容。

## 大文件合理性判断结果

未新增或修改业务大文件。本报告文件职责单一，仅记录本次菜单权限检查。

## 重复代码检查结果

未做业务代码修改。当前卖家/买家管理前端继续复用 `PartnerManagementPage` 及其弹窗组件，没有新增重复实现。

## 已修复问题

本次没有修复代码或 SQL，只输出只读检查结论。

## 新增问题

1. 普通管理角色需要显式绑定 `2010/2011` 及按钮权限，当前 seed 不自动授权。
2. `seller:admin:directLogin` 缺少独立权限增量，依赖综合 seed。
3. 低权限角色要补齐组合权限依赖，特别是账号弹窗的 `dept:query`、角色表单的 `menu:query/role:query`、菜单编辑的 `menu:query`。

## 残留问题

- 未确认运行库真实 `sys_menu` / `sys_role_menu` 状态，因为本次明确禁止连接远端数据库。
- 未确认当前登录账号实际是否为超级管理员或已绑定完整角色菜单。
- 未执行浏览器登录验证，因此菜单真实可见性没有运行时证据。

## 验证命令

```powershell
Get-Content -Path .\AGENTS.md -Encoding UTF8
rg --files -g "*.sql"
rg -n -i "seller|卖家|sys_menu|seller:" RuoYi-Vue react-ui docs -g "*.sql" -g "*.java" -g "*.ts" -g "*.tsx" -g "*.js" -g "*.jsx" -g "*.md"
rg -n "@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|@PreAuthorize" RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\controller\AdminSellerController.java
rg -n "@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|@PreAuthorize" RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\controller\AdminSellerMenuController.java
rg -n "@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|@PreAuthorize" RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\controller\AdminSellerRoleController.java
rg -n "@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|@PreAuthorize" RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\controller\AdminSellerDeptController.java
rg -n "permPrefix|accountPermissions|hasAuditPermission|hasPerms" react-ui\src\components\PartnerManagement\PartnerManagementPage.tsx
rg -n "sys_role_menu|2010|2011|seller:admin:list" RuoYi-Vue\sql docs -g "*.sql" -g "*.md"
```

## 未验证原因

- 未连接 MySQL/Redis：遵守本次任务“不连接或写远端 DB，只看代码和 SQL”的要求。
- 未运行 Maven/前端构建：本次目标是权限静态检查，且不需要编译才能确认权限字符串对应关系。
- 未做浏览器验证：需要真实登录和运行库菜单数据，本次任务范围禁止数据库确认。
