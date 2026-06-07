# 2026-06-07 三端 P0/P1：端内权限前缀与 SQL 索引重放 Guard 收口记录

## 参考方向

- 参考方案：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`
- 当前模式：只处理 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失。
- 明确不做：浏览器运行态验收、截图、DOM 检测、UI 细调。

## 子 Agent 使用情况

- 本轮按用户确认的当前规则启动 `6` 个 `gpt-5.4` 只读扫描 Agent。
- 已收到并采纳的 P0/P1：
  - P0：`@PortalPreAuthorize(terminal = "seller")` / `buyer` 未强制校验 `hasPermi` 端前缀，可能让 seller 误写 buyer 权限或反向误写后仍被 seed 覆盖。
  - P1：DATED SQL 自动 guard 未把裸 `CREATE INDEX` 纳入可重放风险收口。
  - P1：`PartnerMenuModal` 组件路径校验未归一化 `./` 和大小写，`./Buyer/...` / `./Seller/...` 可能绕过跨端组件校验。
- 已记录但不在本切片混改的 P1：
  - 2026-06-07 追记：`20260606_admin_partner_role_menu_grant.sql` 的子按钮授权 wildcard 风险已由后续白名单/合同检查点收口为显式批准清单。
  - seller/buyer 账户详情 Mapper 仍有裸 `accountId` 查询入口，现有 Java 层大多已补主体校验，但后续应下推 `sellerId + accountId` / `buyerId + accountId` 到 Mapper 层。
  - 端内 `oper_log` 免密代入审计字段仍主要拼入 `oper_param`，后续应补结构化审计列或独立审计表。
  - 管理端触发强退/密码重置导致的端内会话失效日志，terminal 侧登录日志缺少执行管理员结构化字段。

## 已完成

- `TerminalSeedPermissionContractTest` 增加强制端前缀校验：
  - `terminal = "seller"` 的 `@PortalPreAuthorize` 如声明 `hasPermi`，必须以 `seller:` 开头。
  - `terminal = "buyer"` 的 `@PortalPreAuthorize` 如声明 `hasPermi`，必须以 `buyer:` 开头。
  - 同时保留原有“声明权限必须进入 `seller_buyer_management_seed.sql`”校验。
- `SqlExecutionGuardContractTest` 增强高影响 SQL 自动发现：
  - `CREATE INDEX` / `CREATE UNIQUE INDEX` 纳入高影响语句检测。
  - 新增 dated SQL 禁止裸 `CREATE INDEX` 合同，要求使用可重放 helper。
- `20260604_source_product_library_sku_candidate_fields.sql` 中 4 个裸索引改为 `create_index_if_missing(...)`。
- `20260605_product_distribution_status_price_log.sql` 中 4 个裸索引改为 `create_index_if_missing(...)`。
- `PartnerMenuModal.tsx` 增加 `normalizeMenuTarget(...)`，组件路径和路由路径统一去掉前导 `./` / `/`，首段转小写后再做 opposite terminal 和 admin/shared root 校验。
- `check-partner-management-template.mjs` 增加菜单组件路径归一化静态断言，防止后续移除 `./` 归一化或大小写归一化。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test`：通过，`8` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalSeedPermissionContractTest test`：修改前通过，作为基线确认。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalSeedPermissionContractTest,SqlExecutionGuardContractTest" test`：通过，`9` 个测试通过。
- `cd E:\Urili-Ruoyi; rg -n -g "*.sql" "(?i)^\s*create\s+(unique\s+)?index" RuoYi-Vue\sql`：无匹配，当前 SQL 目录无裸 `CREATE INDEX`。
- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\check-partner-management-template.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 `3` 个 suite / `9` 个测试通过，后端三端契约 ruoyi-system `100`、ruoyi-framework `15`、product `1`、seller `80`、buyer `81` 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 2 changed files`、`Modified: 2 - 85 nodes in 532ms`。

## 边界说明

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。
- 本检查点未做浏览器、截图、DOM 或 UI 细调验收。
- 本检查点只收 `CREATE INDEX` 可重放风险；旧 SQL 中裸 `ALTER TABLE ADD COLUMN` 仍作为后续 P1 分批处理。

## 残留 P1

- 管理端授权脚本 `20260606_admin_partner_role_menu_grant.sql` 的按钮授权 wildcard 风险已由后续白名单/合同检查点收口为显式批准清单。
- seller/buyer 账户详情相关 Mapper 应补 `subjectId + accountId` 绑定查询，并替换已有带主体上下文的调用点。
- 端内操作日志 direct-login 审计字段应结构化，不应长期只拼入 `oper_param`。
- 管理端强退、密码重置触发的 terminal 侧登录日志应补执行管理员字段或独立会话审计表。
- 管理端 `/seller`、`/buyer` 静态路由兜底仍可补入 `check-partner-management-template.mjs`。
