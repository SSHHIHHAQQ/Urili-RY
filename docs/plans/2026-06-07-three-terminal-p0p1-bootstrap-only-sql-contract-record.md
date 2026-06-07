# 2026-06-07 三端 P0/P1 Bootstrap-only SQL 静态合同记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按快速推进模式只修 P0/P1。本轮聚焦 `RuoYi-Vue/sql/ry_20260417.sql` 和 `RuoYi-Vue/sql/quartz.sql` 的脚本分类边界：它们是官方初始化基线，不是普通增量 SQL。

## 新增问题

- P1：`ry_20260417.sql` 和 `quartz.sql` 含破坏性 `DROP TABLE`，误在已有本地库或远程库执行会造成 P0 数据破坏。
- P1：此前只有 AGENTS 和历史记录口头说明“初始化基线”，缺少静态合同固定这两份脚本必须保持 bootstrap-only 分类。
- P1：普通高影响增量 SQL 已有 confirm-token guard，但这两份官方基线脚本职责不同，不应被混入增量脚本确认机制。

## 已修复问题

- `ry_20260417.sql` / `quartz.sql` 增加 `URILI_BOOTSTRAP_ONLY_SQL` 哨兵注释，明确 `bootstrap-only baseline initialization` 和 `must not be treated as an incremental migration`。
- `SqlExecutionGuardContractTest` 新增 `destructiveBootstrapSqlMustStayExplicitlyBootstrapOnly`：
  - 固定两份基线脚本必须保留 bootstrap-only 哨兵。
  - 固定两份基线脚本含 `DROP TABLE IF EXISTS` 时，哨兵必须出现在破坏性语句之前。
  - 固定除 `ry_20260417.sql` / `quartz.sql` 之外的 SQL 文件不得出现裸破坏性 `DROP TABLE IF EXISTS`。
- `AGENTS.md`、`README.md` 和 `docs/architecture/reuse-ledger.md` 已同步说明：两份脚本只用于全新数据库初始化，不得作为已有库的增量迁移回放。

## 残留问题

- 本轮不改两份官方初始化脚本的建表、插入或删表语义，不把它们改造成可重放增量 SQL。
- 本轮不执行远程 SQL，不改 Docker 初始化流程。
- `20260606_upstream_inventory_dimension_sync.sql` 重复 owning `2307/2308/2309` integration 菜单按钮的问题，已由 `2026-06-07 三端 P0/P1 上游库存菜单 Owner 收敛记录` 收口。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`25` 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。

## 未验证原因

- 未连接远程 MySQL / Redis，未执行 SQL，未写远程数据：本轮只加脚本分类哨兵和静态合同。
- 未做浏览器运行态验收、截图、DOM 检测：用户已明确当前快速推进模式无需浏览器验证。

## 权限检查结果

- 本轮未改管理端、卖家端、买家端权限判断逻辑。
- SQL 合同只固定初始化基线和增量脚本边界，不改变 `sys_menu`、`seller_menu`、`buyer_menu` 权限语义。

## 字典/选项复用检查结果

- 本轮未新增字典、选项或前端枚举。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记 bootstrap-only 初始化基线和增量 SQL guard 的边界。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 1 changed files`，`Modified: 1 - 56 nodes in 912ms`。

## 大文件合理性判断结果

- `SqlExecutionGuardContractTest.java` 已超过 500 行，但职责仍集中在 SQL 执行和脚本分类 guard 合同；本轮只追加同类静态合同，不拆分。
- `ry_20260417.sql` / `quartz.sql` 是官方初始化基线，本轮只加头部分类注释，不改 SQL 语义。

## 重复代码检查结果

- 两份 SQL 使用相同 bootstrap-only 哨兵，便于测试统一识别。
- 未新增 Java 业务逻辑或 React 业务逻辑重复。

## 子 Agent 使用记录

- 本轮使用并关闭 6 个 `gpt-5.4` 只读子 Agent。
- 子 Agent 结论：bootstrap-only 静态合同优先于旧库存菜单 owner 收敛；`SqlExecutionGuardContractTest` 已被 `verify-three-terminal` 覆盖；Markdown 应新增独立检查点；`20260606_upstream_inventory_dimension_sync.sql` 的 `2307/2308/2309` 重复 owner 当时保留为下一片 P1，现已由后续检查点收口。

## 一句话总结

本轮把 `ry_20260417.sql` / `quartz.sql` 从“口头初始化基线”固定为可测试的 bootstrap-only SQL 分类，防止它们被误当作普通增量脚本回放。
