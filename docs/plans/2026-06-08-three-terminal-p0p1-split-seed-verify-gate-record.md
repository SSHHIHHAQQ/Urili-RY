# 2026-06-08 三端 P0/P1 快速推进：Split Seed 与验证闸门收口记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。

当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 情况

- 历史记录（已过期口径）：按用户最新规则优先尝试 6 个 GPT-5.3 Codex 子 Agent，工具模型为 `gpt-5.3-codex-spark`；平台返回额度限制，提示需等到 `2026-06-14 15:12` 后再试，6 个失败 Agent 已关闭。
- 按 fallback 规则启动并关闭 6 个 `gpt-5.4` 只读子 Agent。
- 采纳的 P1：
  - `react-ui/scripts/verify-three-terminal.mjs` 的公开脚本自检只在 `--check-manifest` 路径执行，正常 `verify:three-terminal` 没有同步 fail-closed。
  - `SqlExecutionGuardContractTest` 的自动发现 SQL 确认顺序校验绑定到任意 `call assert_`，没有绑定到真正的 `assert_*_confirmed()` 调用。
  - `20260604_portal_product_category_permission_seed.sql`、`20260604_seller_product_schema_permission_seed.sql`、`20260604_buyer_product_schema_permission_seed.sql` 会把端内基础权限授予所有启用角色，而不是只授予 `owner` 角色。
  - 端内 owner 角色就绪和账号绑定检查必须同时要求 `role_key='owner'`、`status='0'`、`del_flag='0'`。
- 未发现新的确定 P0。

## 已完成

- `react-ui/scripts/verify-three-terminal.mjs`
  - 在正常执行路径也调用 `assertPublicTestScriptsUseThreeTerminalVerifier()`，不再只依赖 `--check-manifest`。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - 新增 `autoDiscoveredSqlGuardOrderMustUseConfirmedProcedureCall()`，固定高影响 SQL 的确认顺序必须使用实际 `_confirmed()` 调用，不能被无关 assert 伪装通过。
  - `assertConfirmationCallBeforeDml(...)` 改为匹配 `CONFIRM_CALL`，再比较其位置与首个高影响 DDL/DML 的顺序。
  - 新增 `terminalOwnerRoleSeedScriptsMustUseActiveOwnerRolesBeforeAccountBinding()`，固定端内 owner 角色和账号绑定检查。
  - `assertTerminalPermissionSeedMenuGuard(...)` 要求 split permission seed 授权范围包含 `and r.role_key = 'owner'`。
- SQL seed
  - `20260604_portal_product_category_permission_seed.sql` 的 seller/buyer category 权限只授给启用 owner 角色。
  - `20260604_seller_product_schema_permission_seed.sql` 和 `20260604_buyer_product_schema_permission_seed.sql` 的 schema 权限只授给启用 owner 角色。
  - `20260604_portal_account_list_permission_seed.sql`、schema seed 和 `20260606_legacy_disable_sys_seller_buyer_roles.sql` 的 owner 就绪/账号绑定判断补齐启用状态约束。
- `docs/architecture/reuse-ledger.md`
  - 登记公开测试入口、确认过程顺序和 split permission seed owner 授权范围规则。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test`：通过，57 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
  - 4 个前端 guard 通过。
  - React typecheck 通过。
  - 12 个 Jest suite / 66 个测试通过。
  - 后端 reactor `test-compile` 通过。
  - 后端三端合同测试通过。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## P2 记录

- `admin` 与 `portal` JWT 仍共用 `token.secret`，当前靠 claim 形状、terminal 和 Redis namespace 隔离；现有合同覆盖该边界，本轮不升 P1。
- `lint` 不是三端总闸门，如果后续流程把 `lint` 当发布前唯一入口，需要单独治理。
- `app.tsx` 与 `requestErrorConfig.ts` 的 401 分流逻辑仍有重复维护风险，当前未发现串端。
- seller/buyer controller 中仍有部分中文注释或 `@Log` 标题乱码，影响可读性，不阻塞本轮 P0/P1。
- `selectSourceWarehouseStockList` 的 `repositoryScope` 未进入 mapper 过滤，当前接口由 connection path 收敛，后续跨连接复用前需补。
- 关键测试自动发现仍依赖命名和路径启发式；新增关键合同测试应继续显式纳入 `three-terminal.manifest.json`。
