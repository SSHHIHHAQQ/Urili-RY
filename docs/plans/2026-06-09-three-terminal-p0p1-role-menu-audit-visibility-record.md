# 三端隔离 P0/P1 快速推进记录：角色菜单与审计可见性

时间：2026-06-09 01:50，本机 `Asia/Shanghai`。

## 参考方向

- 主参考：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 执行模式：只处理 P0/P1，即编译、guard、接口、权限、串端、service/字段缺失。
- 跳过项：不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

## 子 Agent 使用记录

- 实际模型：`gpt-5.4`。
- 数量：6 个。
- 状态：全部关闭。
- 覆盖切片：
  - seller/buyer 角色菜单写入链路。
  - 管理端控制权和审计可见性。
  - seller/buyer portal 自助 DTO、会话和日志。
  - SQL seed / migration guard。
  - React service、权限、路由、sidecar。
  - 三端验证闸门。

## 采纳的 P1

### P1-1：role-menu Mapper 写入缺少 SQL 层端 ID 范围兜底

现有 service 已在写前做 `assertRoleMenusExist(...)`，但 mapper `batchSellerRoleMenu` / `batchBuyerRoleMenu` 仍主要依赖本端表 join。为防止未来绕过 service 或脏数据导致跨 ID 段写入，本轮在 SQL 层补端 ID 范围条件，并用合同测试固定。

已完成：
- `SellerPortalPermissionMapper.xml`
  - `batchSellerRoleMenu` 增加 `m.seller_menu_id >= 100000` 和 `< 200000`。
- `BuyerPortalPermissionMapper.xml`
  - `batchBuyerRoleMenu` 增加 `m.buyer_menu_id >= 200000` 和 `< 300000`。
- `TerminalRoleMenuMapperIsolationContractTest`
  - 固定 seller/buyer role-menu batch 写入必须带端 ID 范围。

### P1-2：selectMenuById 读取历史脏菜单时缺端 ID 范围断言

如果历史库里存在跨端或错误 ID 段的菜单行，仅靠 Mapper 表名无法在 service detail 入口统一 fail-closed。本轮在 seller/buyer 权限 service 的 `selectMenuById` 返回前增加端 ID 范围断言，并补单测。

已完成：
- `SellerPortalPermissionServiceImpl.selectMenuById(...)`
  - 返回前调用 `PortalPermissionSupport.assertTerminalMenuId(menu.getMenuId(), "seller")`。
- `BuyerPortalPermissionServiceImpl.selectMenuById(...)`
  - 返回前调用 `PortalPermissionSupport.assertTerminalMenuId(menu.getMenuId(), "buyer")`。
- `SellerPortalPermissionServiceImplTest`
  - 新增 `selectMenuByIdRejectsMenuOutsideSellerTerminalRange`。
- `BuyerPortalPermissionServiceImplTest`
  - 新增 `selectMenuByIdRejectsMenuOutsideBuyerTerminalRange`。

### P1-3：管理端审计弹窗不能直接看到当前后台操作人

后端和 Mapper 已返回 `actingAdminId`、`actingAdminName`、`directLoginTicketId`、`directLoginReason`，但 `PartnerAuditModal` 的登录日志和操作日志没有展示这些字段。本轮只改前端审计可见性，不动接口和表结构。

已完成：
- `PartnerAuditModal.tsx`
  - 登录日志列表增加 `后台操作人` 列。
  - 操作日志列表增加 `后台操作人` 列。
  - 登录日志展开详情增加后台操作人、后台操作人 ID、免密票据 ID、代入原因。
  - 操作日志展开详情增加后台操作人、后台操作人 ID、免密票据 ID、代入原因。
- `partner-audit-modal.test.ts`
  - 新增静态合同，防止登录/操作审计字段再次从弹窗中消失。

## 复核后不采纳为本轮 P1

### 管理控制动作“无在线会话时没有 seller/buyer 审计”

子 Agent 提示该风险后，本地复核当前代码：

- `SellerServiceImpl.recordSellerForceLogoutAudit(...)` 在 `sessions` 为空时，会构造 `PortalLoginLog` 并写入 `seller_login_log`。
- `BuyerServiceImpl` 当前结构与 seller 对称。

因此当前代码不是“只有在线会话才记录强退审计”。该点不作为本轮 P1 修复项。

### 商品、商城、库存 SQL seed 写后完成态断言

SQL seed 子 Agent 发现以下跨业务 seed guard 风险：

- `20260604_product_category_attribute_seed.sql` 缺少事务和写后完成态校验。
- `20260605_mall_product_distribution_seed.sql` 的 expected signature 主要锁计划写入集合，不直接校验写后真实完成态。
- `20260607_inventory_overview_platform_stock.sql` 存在同类写后完成态风险。

该发现是 P1 级 guard 风险，但属于商品/商城/库存 seed 重写切片，不属于本轮 seller/buyer 账号权限样板切片。本轮已记录，下一 P1 切片单独处理，避免一次同时扩到多个业务 seed。

## P2 记录

- portal 自助 DTO 仍有部分内部字段靠 `@JsonIgnore` 隐藏，后续可改成物理无敏感字段的瘦 DTO。
- portal 自助日志查询入口仍复用内部日志实体作为入参，service 目前会重建白名单 query，后续可改成瘦 query DTO。
- `react-ui/src/services/system/user.js` 仍是手写镜像，后续可改成单行 re-export。
- `session.ts` 与 `RemoteMenuRouteGuard.tsx` 的静态 authority 仍是双份定义，后续可抽共享常量。
- `warehouse` 模块当前没有模块内测试，三端闸门主要通过 `ruoyi-system` 的仓库路由合同覆盖。
- `lint` 链路弱于 `verify-three-terminal`，完整 guard 仍以 `verify-three-terminal` 为准。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npx jest --runTestsByPath tests/partner-audit-modal.test.ts --runInBand --config jest.config.ts`
  - 通过，1 个测试套件 / 5 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalRoleMenuMapperIsolationContractTest" test`
  - 通过，1 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am "-Dtest=SellerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，seller 17 个测试、buyer 17 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs`
  - 通过。
  - 前端 guard、React typecheck、22 个 Jest suites / 163 个测试通过。
  - 后端 reactor `test-compile` 通过。
  - 后端三端合同通过。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过，只有当前工作区已有 LF/CRLF 换行提示，无空白错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过，CodeGraph 返回 `Synced 20 changed files`。

## 数据源和远端影响

- 本轮没有执行 DDL。
- 本轮没有执行 DML。
- 本轮没有读取或写入远端 MySQL。
- 本轮没有读取或写入远端 Redis。
- 本轮没有启动或重启后端服务。
- 本轮没有做浏览器、截图、DOM 或 UI 细调验收。

## 当前判断

- seller/buyer role-menu 写入链路现在同时具备 service 写前全量校验和 mapper 端 ID 范围兜底。
- seller/buyer 菜单详情读取遇到跨端 ID 段会 fail-closed。
- 管理端审计弹窗现在能直接看到当前后台操作人和免密审计字段，不再只能从票据页间接查。
- 三端完整验证闸门通过，当前变更没有引入新的账号体系混用或权限闸门回退。

## 后续 P1 切片建议

1. 单独处理商品、商城、库存三份 SQL seed 的写后完成态断言和按钮菜单漂移问题。
2. 如继续做管理端控制权审计，可把 `sys_oper_log` 中的后台控制动作聚合进 partner audit 视图；该项需要先定接口范围，避免和端内审计表混在一起。
