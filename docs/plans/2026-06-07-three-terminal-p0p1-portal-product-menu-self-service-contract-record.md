# 2026-06-07 三端 P0/P1 Portal 商品、菜单写入与自助可见面合同记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按快速推进模式只修 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本轮不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用记录

- 先按最新规则尝试 `gpt-5.3-codex-spark` 子 Agent；平台返回额度限制，失败 Agent 已关闭。
- 随后回退使用 6 个 `gpt-5.4` 只读子 Agent，分别扫描 SQL seed、端内菜单权限、React token/route guard、管理端账号与免密、自助接口、验证脚本清单。
- 已采纳 P1：
  - `updateMenu` 需要补齐空 `perms`、非法 `component` 的 fail-closed 回归测试。
  - role-menu Mapper XML 需要静态合同锁定只读写当前端表。
  - Portal 自助资料、账号、部门、角色和自助日志接口需要静态合同锁定可见 DTO 与日志查询白名单。
- 未采纳为本轮改动：
  - SQL seed 子 Agent 未发现新的 P0/P1。
  - React guard、管理端账号/免密子 Agent 未发现新的 P0/P1。
  - `integration` 报告覆盖表述漂移仅作 Markdown 口径修正，不改变当前验证脚本逻辑。

## 已完成

- 新增 `PortalProductEndpointPermissionContractTest`：
  - 固定 seller/buyer 商品分类和商品 schema portal 接口必须使用当前端 `@PortalPreAuthorize` 权限。
  - 固定 seller/buyer 商城商品 portal 接口必须从 `PortalSessionContext.requireSession(...)` 推导端内身份，不接受前端传入主体/账号 scope。
  - 固定接口必须走当前端 service 调用，不把 seller/buyer 可见范围混在一起。
- 新增 `TerminalRoleMenuMapperIsolationContractTest`：
  - 固定 seller role-menu SQL 只读写 `seller_menu` / `seller_role` / `seller_role_menu`。
  - 固定 buyer role-menu SQL 只读写 `buyer_menu` / `buyer_role` / `buyer_role_menu`。
  - 显式禁止 `sys_menu` 和对端表进入端内角色绑菜单 Mapper。
- 新增 `PortalSelfServiceSurfaceContractTest`：
  - 固定 `profile`、`accountProfile`、`accounts`、`depts`、`roles` 必须映射为 Portal 可见 Profile DTO。
  - 固定自助登录/操作日志返回 `PortalOwnLoginLogProfile` / `PortalOwnOperLogProfile`，不暴露 `subjectId`、`accountId`、`directLoginTicketId`、`actingAdmin*`、`operParam`、`jsonResult` 等内部审计字段。
  - 固定自助日志查询 builder 只能复制白名单筛选条件，并强制覆盖当前 session 的 `subjectId + accountId`。
- seller/buyer 端内菜单测试补齐：
  - `updateMenuRejectsBlankPermsAndInvalidComponentBeforeMapperWrite` 对称覆盖卖家、买家。
  - 回归断言写 Mapper 前失败，`updateMenuCallCount = 0`。
- `react-ui/scripts/verify-three-terminal.mjs` 已把新增后端合同测试加入三端统一验证清单。
- `docs/plans/2026-06-07-three-terminal-p0p1-verify-list-drift-integration-coverage-record.md` 校准 `integration` 口径：当前保证是编译闭环覆盖，不是 integration 模块 surefire 必跑报告覆盖。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer,ruoyi-system -am "-Dtest=SellerPortalPermissionServiceImplMenuTreeTest,BuyerPortalPermissionServiceImplMenuTreeTest,TerminalRoleMenuMapperIsolationContractTest,PortalProductEndpointPermissionContractTest,PortalSelfServiceSurfaceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，seller/buyer/system 相关测试均通过。
- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 guard、TypeScript、6 个 Jest suite / 30 个测试、后端 reactor test-compile 和后端三端合同链路均通过。

## 未验证原因

- 未做浏览器运行态验收、截图、DOM 检测：用户已明确当前快速推进模式无需浏览器验证。
- 未连接远程 MySQL / Redis，未执行 SQL，未写远程数据：本轮只补测试合同、验证清单和 Markdown 记录。

## 文件大小判断

- `PortalProductEndpointPermissionContractTest.java` 当前 312 行，触发 300 行职责自检。判断结果：暂不拆分，因为职责单一，只固定 portal 商品端入口的权限、session scope 和端 service 调用；拆分会重复方法解析 helper，降低可读性。
- `PortalSelfServiceSurfaceContractTest.java` 当前 290 行，未超过 300 行自检线。
- `TerminalRoleMenuMapperIsolationContractTest.java` 当前 121 行。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 6 changed files`、`Added: 3, Modified: 3 - 257 nodes in 777ms`。

## 残留

- 当前没有新增 P0/P1 残留。
- `integration` 仍只有编译闭环覆盖；如果后续需要 report 级 guard，必须在 `integration` 模块新增明确契约测试后再加入 `backendTestClasses`。
