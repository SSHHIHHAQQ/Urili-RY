# 2026-06-08 三端隔离 P0/P1 快速推进记录：SQL Guard、Product 边界与验证闸门

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

## 范围

- 只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。
- 不做浏览器运行态验收、截图、DOM 检测或 UI 细调。
- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。

## 子 Agent

- 历史记录（已过期口径）：按最新规则优先尝试 6 个 GPT-5.3 Codex 子 Agent，工具模型 `gpt-5.3-codex-spark`。
- 平台返回额度限制：`You've hit your usage limit for GPT-5.3-Codex-Spark. Switch to another model now, or try again at Jun 14th, 2026 3:12 PM.`
- 6 个 GPT-5.3 Codex 子 Agent 均已关闭。
- 按 fallback 规则启动并关闭 6 个 `gpt-5.4` 只读子 Agent。
- fallback 切片覆盖：seller/buyer 后端隔离、Portal 鉴权和免密登录、SQL guard、React guard/service、product/inventory/integration/warehouse 共享域、验证闸门。

## 采纳的 P1

- `20260608_product_review.sql` 对 `sys_dict` / `sys_menu` 写入缺少完整 seed target guard、schema ready 断言、事务边界和完成断言。
- `20260608_overseas_channel_carrier_menu_restructure.sql` 只对删除目标做了精确 guard，未对 reparent/upsert 目标和最终完成状态做 fail-closed 断言。
- `ProductDistributionMapper.xml` 仍由 product 模块直接读写 integration/warehouse 事实表，违反共享域边界。
- 三端验证闸门未把 integration 测试目录纳入关键后端测试自动发现。
- 商品审核只有源码合同，缺少 `ProductReviewServiceImpl` 运行态测试覆盖审计 ID、提交/审批日志和详情日志隔离。

## 已完成

- `20260608_product_review.sql` 增加 schema ready、seed target、completion 三类断言，并将字典和菜单 DML 包进事务。
- `20260608_overseas_channel_carrier_menu_restructure.sql` 增加重排目标签名 guard 和 completion 断言。
- `SqlExecutionGuardContractTest` 增加两份 SQL 的显式 guard 合同和顺序断言。
- 新增 integration-owned `ISourceSkuPairingProjectionService` 及实现，将来源 SKU pairing projection 的读写从 product mapper 移到 integration service/mapper。
- `ProductDistributionServiceImpl` 改为通过 integration port 同步/删除来源 SKU pairing projection。
- `ProductDistributionMapperContractTest` 固定 product mapper 不再直接出现 `upstream_system_sku_pairing` / `upstream_system_warehouse_pairing`。
- 新增 `ProductReviewServiceImplTest`，运行态覆盖商品审核提交、重复待审拦截、审批生效、审计 ID 写入、详情不返回操作日志。
- `react-ui/tests/three-terminal.manifest.json` 纳入 `ProductReviewServiceImplTest`。
- `verify-three-terminal.mjs` 的关键后端测试自动发现纳入 `integration/src/test/java`。
- `verify-three-terminal-backend-gate.test.ts` 增加 integration 自动发现合同。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：修复前基线通过，14 个 Jest suites / 94 个测试、React typecheck、后端 reactor `test-compile`、后端三端合同均通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dtest=ProductReviewServiceImplTest,ProductDistributionMapperContractTest,ProductDistributionServiceImplTest,ProductModuleBoundaryContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，19 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，64 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/verify-three-terminal-backend-gate.test.ts --runInBand`：通过，1 个 suite / 3 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅有当前工作区 LF/CRLF 换行提示。

## 边界与残留

- 本轮未跑浏览器、截图、DOM、UI 细调验收。
- 本轮未执行远程数据库变更。
- `--check-manifest` 只作为清单/发现规则检查，不等价于三端发布闸门；发布或大合并仍应跑完整 `npm run verify:three-terminal`。
- P2：`verify-three-terminal` 仍允许纯 re-export 的 `.test.js` twin 存在，后续可以统一清理生成副本。
