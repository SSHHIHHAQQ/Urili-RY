# 2026-06-08 三端隔离 P0/P1 快速推进记录：库存失效行清理与验证闸门收口

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 执行

- 历史记录（已过期口径）：优先尝试 6 个 GPT-5.3 Codex 子 Agent，工具模型为 `gpt-5.3-codex-spark`。
- 平台返回额度限制，提示到 `2026-06-14 15:12` 后再试；6 个失败 Agent 已关闭。
- 按 fallback 规则启动并关闭 6 个 `gpt-5.4` 只读子 Agent。
- fallback 切片覆盖 seller/buyer 后端隔离、Portal 鉴权和 direct-login、SQL guard、React guard/401、product/inventory/integration、verify gate。

## 采纳的 P1

- `20260604_three_terminal_isolation_migration.sql` 中 seller/buyer account normalize 的断言只锁定待规范化子集，但实际 `update seller_account` / `update buyer_account` 没有 `where`，DML 作用域扩大到整表。
- 仓库配对、SKU 配对增删后只刷新了上游库存快照和来源库存读模型，没有同步刷新库存总览 read model。
- `20260608_inventory_overview_sku_baseline_refresh.sql` 只 upsert 当前 `inventory_sku_warehouse_stock`，没有删除当前规则不再生成的失效 stock 行，后续 read model 聚合可能继续算入脏行。
- `react-ui/tests/*.test.js` 与同名 `.test.ts` 共存时会被 verifier 当成生成镜像静默跳过；如果 JS twin 漂移，`verify-three-terminal` 原先不会失败。
- 非日期前缀的高影响 SQL 仍偏向手工白名单，后续新 seed 可能绕过自动发现。

## 已完成

- `20260604_three_terminal_isolation_migration.sql` 的 seller/buyer normalize update 增加与预览断言一致的 `where` 谓词。
- `SqlExecutionGuardContractTest` 固定 normalize update 必须带目标谓词，并新增所有增量高影响 SQL 的统一自动发现合同。
- `UpstreamSystemServiceImpl` 在仓库配对、SKU 配对增删成功后调用 `refreshSourceInventoryOverview(...)`。
- `InventoryOverviewRefreshContractTest` 固定配对变更必须刷新库存总览。
- `20260608_inventory_overview_sku_baseline_refresh.sql` 新增 obsolete stock count/signature 预览变量、断言过程、事务内 `delete st from inventory_sku_warehouse_stock st where not exists (...)`，只删除当前规则不再生成的 stock_key。
- `SqlExecutionGuardContractTest` 固定 obsolete stock 删除必须先断言、在事务内执行，并发生在当前 stock upsert 之前。
- `verify-three-terminal.mjs` 对同名 `.test.js` twin 增加 fail-closed 校验：必须是精确纯 re-export 到同名 `.test.ts/.tsx`。
- `react-ui/tests/*.test.js` 统一改成纯 re-export 镜像。
- `verify-three-terminal-backend-gate.test.ts` 增加行为级自测：临时制造漂移 JS twin，确认 `--check-manifest` 会失败。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，63 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl inventory "-Dtest=InventoryOverviewRefreshContractTest" test`：通过，1 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl integration,inventory,ruoyi-system -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/verify-three-terminal-backend-gate.test.ts --runInBand`：通过，1 个 suite / 2 个测试。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：已执行并通过。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未运行完整 `npm run verify:three-terminal`；本轮只跑了与修复相关的最小必要验证。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## P2 记录

- direct-login 页 token timeout 失败回传可以更快，目前管理端仍会通过自身 bridge 超时收敛。
- `app.tsx` 与 `requestErrorConfig.ts` 的 401 分流逻辑仍有重复，当前行为一致，后续可抽共享 helper。
- 管理端审计查询的 accountId + subjectId fail-closed 主要靠运行时服务层守卫，后续可增加更强的架构合同。
- `verify-three-terminal` 仍允许纯 re-export 的 `.test.js` twin 存在，后续可以统一清理生成副本。
