# 2026-06-05 管理端免密票据端类型过滤契约记录

## 背景

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，补强管理端免密票据审计列表的端类型过滤契约。

目标是防止后续把 seller/buyer 共用的 `portal_direct_login_ticket` 查询做成只相信前端 `terminal` 参数，从而导致管理端卖家入口查到买家票据，或买家入口查到卖家票据。

## 已完成

- 更新 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminDirectLoginPermissionContractTest.java`。
- 增加 seller/buyer service 层检查：`selectSellerDirectLoginTicketList(...)` 必须设置 `query.setTerminal("seller")`，`selectBuyerDirectLoginTicketList(...)` 必须设置 `query.setTerminal("buyer")`。
- 增加共享 mapper XML 检查：`PortalDirectLoginTicketMapper.xml` 的列表查询必须包含 `terminal` 条件。
- 更新 `docs/architecture/reuse-ledger.md`，登记 ticket 审计列表不能相信前端 terminal 参数。
- 只读子 agent 已完成六个方向审计，并已关闭；其中 SQL/seed 审计发现“端内权限 seed 尚未并入综合初始化 seed”，该问题属于后续 SQL 初始化一致性切片，本轮不混入。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=AdminDirectLoginPermissionContractTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。本轮同时修复了 `react-ui/src/pages/Product/categoryTree.ts` 的 `toCategoryOption(...)` 返回类型，让前端类型门禁恢复。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/pages/Product/categoryTree.ts`：通过。

## 当前判断

- seller/buyer 管理端免密票据审计列表现在有静态契约守卫：权限点独立、前端 tab 权限显隐、service 层 terminal 强制过滤和 mapper terminal 条件均被同一个测试覆盖。
- 该守卫不替代真实低权限浏览器负向验收；真实低权限账号的前后端拒绝证据已在 seller/buyer 低权限检查点单独记录。
- 本轮没有执行 SQL，没有连接远程 MySQL / Redis，也没有改接口业务逻辑或页面交互。

## 后续单独切片

- SQL/seed 审计发现端内权限 seed 目前依赖多个独立 SQL，新环境如果只跑 `seller_buyer_management_seed.sql` 可能缺少端内 `seller_menu` / `buyer_menu` 权限和角色菜单授权。后续应单独处理综合 seed 合并或初始化说明，不与本轮测试守卫混合。
