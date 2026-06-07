# 2026-06-07 三端 P0/P1 SQL Guard 自动发现收口记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按快速推进模式只修 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本轮聚焦增量 SQL 自动发现和 dynamic DDL helper 的 guard 覆盖；不做浏览器运行态验收、不做截图/DOM 检测、不做 UI 细调。

## 新增问题

- P1：`SqlExecutionGuardContractTest` 的自动发现原先只匹配 `202606*.sql`，后续新增 `202607...sql` 或其他日期前缀脚本会掉出 high-impact SQL guard 覆盖。
- P1：dynamic DDL helper 虽然部分能被宽泛字面匹配碰到，但合同里没有明确表达 `set @ddl = concat('alter table ...')` / `prepare stmt from @ddl` 属于高影响 SQL，后续容易误删或绕过。
- P1：`20260604_source_product_library_sku_candidate_fields.sql` 是 dynamic DDL helper 脚本，应进入显式 guard 清单，固定确认 token 和 fail-closed 合同。

## 已修复问题

- `SqlExecutionGuardContractTest`：
  - 将 `DATED_JUNE_2026_SQL_FILE` 改为 `DATED_SQL_FILE`，模式从 `202606\d{2}.*\.sql` 泛化为 `20\d{6}.*\.sql`。
  - `datedHighImpactSqlScriptsMustBeAutoDiscoveredAndGuarded()` 和 `datedSqlScriptsMustNotUseBareCreateIndex()` 均改用通用日期前缀 SQL 发现。
  - 新增 `DYNAMIC_HIGH_IMPACT_SQL_HINT`，显式识别 `set @ddl = concat('alter table ...')`、`create table/view/index`、`drop table/view`、`truncate table`、`rename table` 等 dynamic DDL helper。
  - `containsHighImpactSql(...)` 同时检查普通 high-impact SQL hint 和 dynamic DDL hint。
  - 将 `20260604_source_product_library_sku_candidate_fields.sql` 加入显式 `assertGuard(...)` 清单。
- `AGENTS.md` 已补充日期前缀增量 SQL 和 dynamic DDL helper 必须纳入 guard 的规则。
- `docs/architecture/reuse-ledger.md` 已登记 `DATED_SQL_FILE` 和 dynamic DDL high-impact hint 复用规则。
- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 已追加本检查点。

## 残留问题

- P1：端内 role-menu 当前已有本端存在性校验，但 `seller_menu` / `buyer_menu` ID 空间仍可能重叠；跨端提交同数字 ID 仍可能绑定成本端同号菜单，后续应做端内菜单 ID 段隔离或稳定 `businessKey` 方案。
- P1：seller/buyer 前端仍有未接通的“指定密码重置” service 导出；当前 UI 口径是默认重置为 `U12346`，后续建议删除未接通前端导出，不新增 UI 弹窗。
- 历史残留已收口：`integration` fresh bootstrap schema 策略已由 `docs/plans/2026-06-07-three-terminal-p0p1-integration-bootstrap-chain-record.md` 固定为 bootstrap 后必跑 SQL 清单。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`31` 个测试通过。

## 未验证原因

- 未做浏览器运行态验收、截图、DOM 检测：用户已明确当前快速推进模式无需浏览器验证。
- 未连接远程 MySQL / Redis，未执行 SQL，未写远程数据：本轮只修改静态合同测试和 Markdown 记录。

## 权限检查结果

- 本轮不新增后端接口、不新增菜单权限、不修改端内权限模型。
- SQL guard 的覆盖范围扩大到未来日期命名增量脚本和 dynamic DDL helper，属于执行安全合同，不改变运行时权限。

## 字典/选项复用检查结果

- 本轮未新增字典类型、字典数据或前端选项。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记日期前缀 SQL 自动发现和 dynamic DDL helper high-impact hint 规则。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；本轮最终同步输出 `Synced 5 changed files`，`Modified: 5 - 239 nodes in 996ms`。记录更新后复跑输出 `Already up to date`。

## 大文件合理性判断结果

- `SqlExecutionGuardContractTest.java` 已超过 500 行，但本轮继续追加 SQL 执行安全合同，职责仍集中在 SQL guard；暂不拆分。

## 重复代码检查结果

- 没有复制 Java 业务逻辑或 React 业务逻辑。
- dynamic DDL helper 检测通过统一 regex 进入 `containsHighImpactSql(...)`，未在多个测试里散写匹配逻辑。

## 子 Agent 使用记录

- 本轮实现基于前序已完成并关闭的 `gpt-5.4` SQL guard 子 Agent 只读结论；未额外开启重复扫描子 Agent。

## 一句话总结

SQL guard 自动发现已经从单月脚本扩展到所有日期前缀增量 SQL，并显式纳入 dynamic DDL helper 的高影响 SQL 判定。
