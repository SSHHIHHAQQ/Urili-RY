# 2026-06-08 三端 P0/P1 Seed Completion 与 Verifier Guard 收口记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，在快速推进模式下只修 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。

本轮不做浏览器运行态验收、截图、DOM 检测或 UI 细调；不执行远程 MySQL/Redis DDL/DML。

## 子 Agent 使用

按用户最新要求，本轮 6 个子 Agent 全部使用 `gpt-5.4`，未再把 GPT-5.3 Codex 作为首选。

| 子 Agent | 切片 | 结论处理 |
| --- | --- | --- |
| Epicurus | SQL seed / guard | 采纳 P1：4 个 seed 缺事务与完成断言 |
| Volta | verifier / 文档口径 | 采纳 P1：补 verifier 自测负例；确认 `gpt-5.4` 现行口径 |
| Erdos | React route / service guard | 未发现 P0/P1，P2 记录 |
| Ramanujan | seller/buyer 后端隔离 | 未发现 P0/P1，P2 记录 |
| Hubble | shared domain 边界 | 未发现 P0/P1，P2 记录 |
| Avicenna | portal auth/session/direct-login/log | 未发现 P0/P1，P2 记录 |

6 个子 Agent 均已关闭。

## 已完成

- `AGENTS.md` 当前口径已确认为：子 Agent 默认使用 `gpt-5.4`，除非用户在当前任务中重新明确要求，否则不要再把 GPT-5.3 Codex 作为首选。
- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 的现行口径索引已保持 `gpt-5.4` 默认；历史 GPT-5.3 相关记录仅作为历史事实，不作为当前规则。
- `RuoYi-Vue/sql/warehouse_management_seed.sql` 增加 DML 事务、完成断言和完成后清理顺序。
- `RuoYi-Vue/sql/upstream_system_management_seed.sql` 增加 DML 事务、字典和菜单完成断言。
- `RuoYi-Vue/sql/currency_configuration_seed.sql` 增加 DML 事务、字典、业务 seed 和菜单完成断言。
- `RuoYi-Vue/sql/warehouse_us_address_seed.sql` 增加 DML 事务、`us_state/us_city` 精确数量与边界样本完成断言。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java` 固化 4 个 seed 的事务和完成断言合同。
- `react-ui/tests/verify-three-terminal-backend-gate.test.ts` 增加两个负例：
  - 公开 test 脚本绕过 `verify-three-terminal` 时必须失败。
  - 新增未登记 `guard:*` 脚本时 manifest 检查必须失败。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`
  - 通过，73 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/verify-three-terminal-backend-gate.test.ts --runInBand`
  - 通过，1 个 suite / 6 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`
  - 通过。

## 未执行

- 未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图、DOM 或 UI 细调验收。

## P2 记录

- `warehouse_us_address_seed.sql` 仍是大体量 seed；本轮已补事务和完成断言，后续如继续扩大数据集，建议拆成 bootstrap DDL 与可回放 DML seed。
- 历史目标追踪文件中仍保留大量 GPT-5.3 执行事实；顶部现行口径已覆盖，后续只在确有误导风险的段落继续标注过期。
- shared domain 边界仍有若干只读 SQL 直接 join 跨模块读模型或事实表，当前未构成 P0/P1；后续可通过 projection/facade 收敛。
