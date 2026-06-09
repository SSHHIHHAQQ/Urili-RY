# 2026-06-08 三端隔离 P0/P1 快速推进记录：Terminal Log Scope Index Guard

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

## 范围

- 只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。
- 不做浏览器运行态验收、截图、DOM 检测或 UI 细调。
- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。

## 子 Agent

- 历史记录（已过期口径）：按 AGENTS 规则优先 GPT-5.3 Codex；前序同日已确认 `gpt-5.3-codex-spark` 额度限制持续到 `2026-06-14 15:12`，本轮直接使用 fallback `gpt-5.4`。
- 启动并关闭 6 个 `gpt-5.4` 只读子 Agent。
- 切片覆盖：
  - seller/buyer 后端账号、角色、菜单、部门、日志、会话隔离
  - Portal token/session/direct-login/Redis key/审计日志/强退/密码重置
  - SQL guard 与日期增量脚本
  - React token/401/远程菜单/权限 guard/service/manifest
  - product/inventory/integration/warehouse 共享域边界
  - 验证闸门与 manifest 覆盖

## 采纳的 P1

- `20260606_terminal_log_scope_indexes.sql` 包含动态 DDL，但 `SqlExecutionGuardContractTest` 只做通用确认 token guard，缺少文件级合同测试。
- 风险：后续如果删掉列前置校验、删掉索引结果校验，或把索引重建移到前置校验之前，通用 guard 仍可能通过。

## 已完成

- `SqlExecutionGuardContractTest` 新增 `terminalLogScopeIndexesMustKeepDynamicDdlGuarded`。
- 新合同固定：
  - `assert_column_exists(...)` 必须存在，并且 8 个列校验先于任何 `recreate_index_if_mismatch(...)`。
  - `recreate_index_if_mismatch(...)` 必须只用于 `seller_login_log`、`buyer_login_log`、`seller_oper_log`、`buyer_oper_log`。
  - 8 个索引重建目标必须与对应账号/主体时间列一致。
  - 8 个 `assert_index_definition(...)` 必须存在，并在全部索引重建后执行。
  - 动态 DDL `prepare stmt from @ddl;` 保持在重建 helper 内，出现次数固定为 2。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer,ruoyi-system -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，65 个测试。
- 子 Agent 验证补充：
  - seller/buyer 后端隔离切片：`mvn -pl seller,buyer,ruoyi-system -am test -DskipITs` 通过。
  - direct-login/session/log 切片：`node scripts/check-portal-token-isolation.mjs` 通过；相关后端契约测试通过。
  - 共享域边界切片：product/inventory/integration/seller/buyer 相关合同测试通过。
  - 前端与验证闸门切片：manifest 自校验通过，未发现新的 P0/P1。

## 边界与残留

- 本轮未执行远程数据库或 Redis 验证；结论覆盖代码面、SQL 合同和定向测试面。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 除本次已修 P1 外，6 个只读切片未发现新的 P0/P1。
