# 2026-06-08 三端 P0/P1 快速推进记录：Split Seed 事务与 Product 编译收口

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 执行情况

- 按用户最新要求，本轮直接使用 6 个 `gpt-5.4` 只读子 Agent，不再把 GPT-5.3 Codex 作为首选。
- 6 个子 Agent 覆盖 seller/buyer 后端隔离、React route/access/request/token、SQL seed、verify gate、共享业务域和 seller/buyer 对称性。
- 6 个子 Agent 均已关闭。
- 采纳的 P0/P1：`product` 模块编译断口、split permission seed 的脏菜单预检缺口、事务/完成断言缺口、fresh bootstrap direct-login 审计字段合同缺口。
- 其他结论未发现新的可坐实 P0/P1；P2 仅记录，不阻塞当前推进。

## 已完成

- `ProductDistributionServiceImpl` 恢复 `OfficialSourceSaveContext`，让来源绑定快照和官方仓库批量上下文可编译。
- `ProductDistributionServiceImplTest` 的 recording service 补齐新增 facade 方法，避免 product 测试编译断口。
- `20260604_portal_product_category_permission_seed.sql`、`20260604_seller_product_schema_permission_seed.sql`、`20260604_buyer_product_schema_permission_seed.sql` 补齐 terminal menu 脏权限、空组件、重复权限 fail-closed 预检。
- 六个 split permission seed 增加 `start transaction`、完成态断言和 `commit` 前校验：
  - `20260604_portal_account_list_permission_seed.sql`
  - `20260604_portal_dept_role_list_permission_seed.sql`
  - `20260604_portal_product_category_permission_seed.sql`
  - `20260604_seller_product_schema_permission_seed.sql`
  - `20260604_buyer_product_schema_permission_seed.sql`
  - `20260607_portal_self_audit_permission_seed.sql`
- `SqlExecutionGuardContractTest` 增加 split seed 脏菜单预检、事务完成断言和 `seller_buyer_management_seed.sql` fresh bootstrap direct-login 审计/票据表合同。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，75 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalAccountIsolationTest,TerminalSqlIsolationContractTest,TerminalRoleMenuMapperIsolationContractTest,TerminalSeedPermissionContractTest,PortalDirectLoginAuthContractTest,PortalPasswordChangeContractTest,PortalSelfAuditSerializationTest,AdminDirectLoginPermissionContractTest,AdminAccountPermissionUiContractTest,SqlExecutionGuardContractTest,PortalDirectLoginTicketSqlContractTest" test`：通过，107 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductDistributionServiceImplTest,ProductDistributionMapperContractTest,ProductModuleBoundaryContractTest" test`：通过，18 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 guard、React typecheck、20 个 Jest suite / 148 个测试、后端 reactor `test-compile`、后端三端合同测试全部通过。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 当前残留项

- P2：Portal 异常 terminal 兜底仍可进一步去掉硬编码 seller 登录回退；当前不构成 P0/P1。
- P2：`system/user` 旧 service URL 可后续统一补 `/api` 前缀；当前不在本轮 P0/P1 范围。
- P2：废弃/no-op SQL 文件可补合同锁死 abort/no-op 语义；当前未影响本轮 split seed 收口。
