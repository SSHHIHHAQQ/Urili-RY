# 2026-06-08 三端隔离 P0/P1 快速推进：路由入口权限与 SQL null-safe guard

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。

本轮只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 执行情况

- 历史记录（已过期口径）：按用户最新规则，优先使用 GPT-5.3 Codex，工具模型为 `gpt-5.3-codex-spark`。
- 平台返回 GPT-5.3 Codex 额度不可用，提示需等到 `2026-06-14 15:12` 后再试；因此按 fallback 规则改用 `gpt-5.4`。
- 本轮收敛并关闭 6 个 `gpt-5.4` 只读子 Agent。
- 采纳的 P1：
  - `react-ui/src/services/session.ts` 的运行时补丁路由仍使用旧商品分销权限合同，和静态路由 guard 不一致。
  - 商品分销列表页新增/编辑入口只看商品主权限，缺 seller/category/categoryAttribute 依赖权限时仍可进入后续 403 或不可维护状态。
  - 端内权限 seed 和账号锁定 seed 的 `parent_id <> p_parent_id` 在 MySQL `NULL` 下不是 fail-closed，脏数据可绕过 slot guard。
- 未发现新的确定 P0。

## 已完成

- `react-ui/src/services/session.ts` 将 `/product/distribution/create`、`/product/distribution/edit/:spuId` 的运行时补丁路由同步为 `authorityMode: 'all'`，并补齐 `seller:admin:list`、`product:category:list`、`product:categoryAttribute:preview` 依赖权限。
- `react-ui/src/pages/Product/Distribution/index.tsx` 新增 `canMaintainDistributionProductDependencies`、`canCreateDistributionProduct`，并让 SPU/SKU 编辑入口和新增入口都与路由依赖权限一致。
- `react-ui/tests/product-distribution-permission-guard.test.ts` 固定运行时补丁路由、列表新增/编辑入口和编辑页保存动作的权限合同。
- `RuoYi-Vue/sql/*.sql` 中相关 seed helper 统一改为 `coalesce(parent_id, -1) <> p_parent_id`，避免 `NULL` 绕过 slot guard。
- `SqlExecutionGuardContractTest` 同步要求账号锁定 sys_menu seed 和 seller/buyer 端内 permission seed 使用 null-safe parent guard。

## 验证结果

- 历史记录（已过期命令口径）：`cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/product-distribution-permission-guard.test.ts tests/remote-menu-route-guard.test.ts --runInBand`：当时通过，2 个 suite / 17 个测试；当前公开 `npm run test:unit` 入口已收口为 `verify-three-terminal`，复核请使用 `npm run verify:three-terminal` 或直接调用 Jest 二进制。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test`：通过，54 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；4 个前端 guard、React typecheck、12 个 Jest suite / 66 个测试、后端 reactor `test-compile` 和后端三端合同测试全部通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅有 LF/CRLF 换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；`Modified: 4 - 183 nodes in 984ms`。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 当前残留项

- P2：`/seller/direct-login`、`/buyer/direct-login` 的 POST body 脱敏可补执行级单测，直接喂 `Map` body 断言 `directLoginToken` 不进入 `oper_param`。
- P2：终端级 `select*MenuById` / `delete*MenuById` 可进一步下推 ID 区间断言；当前按物理分表、ID 区间 seed guard 和写入前 terminal menu 校验先不升 P1。
- P2：`20260607_terminal_menu_id_range_isolation.sql` 的事务块可补显式 `EXIT HANDLER ... ROLLBACK`。
