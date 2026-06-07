# 2026-06-07 三端 P0/P1 Integration Bootstrap 残留口径清理记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本轮不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 本轮判断

当前代码基线已经存在 `docs/architecture/integration-bootstrap-required-sql.md`，并由 `SqlExecutionGuardContractTest#integrationFreshBootstrapMustDeclareMandatoryPostSeedSqlChain` 固定 fresh bootstrap 后置 SQL 链路。

因此旧记录中“`integration` fresh bootstrap schema 策略仍未定 / 仍有缺口”的同主题残留已经过期，应改为“已固定为 bootstrap 后必跑 SQL 清单”。这次只修 Markdown 状态口径，不改 SQL、不执行数据库。

## 已更新记录

- `docs/plans/2026-06-07-three-terminal-p0p1-verify-list-drift-integration-coverage-record.md`
- `docs/plans/2026-06-07-three-terminal-p0p1-read-model-staging-phone-filter-record.md`
- `docs/plans/2026-06-07-three-terminal-p0p1-source-product-read-model-sql-contract-record.md`
- `docs/plans/2026-06-07-three-terminal-p0p1-source-warehouse-stock-read-model-sql-contract-record.md`
- `docs/plans/2026-06-07-three-terminal-p0p1-sql-guard-auto-discovery-record.md`
- `docs/plans/2026-06-07-three-terminal-p0p1-seed-profile-guard-record.md`
- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest#integrationFreshBootstrapMustDeclareMandatoryPostSeedSqlChain" test`：通过，1 个测试通过。

## 未执行事项

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图或 DOM 验收。
