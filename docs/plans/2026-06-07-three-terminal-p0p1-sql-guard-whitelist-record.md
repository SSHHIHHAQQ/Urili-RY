# 2026-06-07 三端 P0/P1 快速推进：SQL Guard 与管理端按钮白名单记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按当前快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调；本轮未执行远程 MySQL / Redis DDL/DML。

## 子 Agent 使用情况

- 本切片集成 compaction 前已经完成的 6 个只读子 Agent 复核结果，覆盖 admin partner 授权 SQL、legacy `sys_user` 回填、非日期高影响 seed guard、文档一致性和残留风险。
- 当前工具可用模型名包含 `gpt-5.3-codex-spark` 与 `gpt-5.4`；用户最新要求后未再新增子 Agent。后续新增子 Agent 时按用户要求优先 `gpt-5.3-codex-spark`，不可用再降级 `gpt-5.4`。
- 已完成的 6 个子 Agent 后续统一关闭，不继续挂起。

## 新增问题

- P1：`20260606_admin_partner_role_menu_grant.sql` 和 `20260606_admin_partner_non_admin_button_grant_cleanup.sql` 原先通过 `child.perms like 'seller:admin:%'` / `buyer:admin:%` 扫描按钮权限，未来新增同前缀按钮时可能被自动纳入授权或清理范围。
- P1：legacy `sys_user` 回填 helper 虽然已有双确认，但仍以全部 `user_id is not null` 行为目标，缺少人工预览后的 `account_id` 白名单和 expected_count 目标校验。
- P1：`business_menu_seed.sql`、`currency_configuration_seed.sql`、`upstream_system_management_seed.sql`、`warehouse_us_address_seed.sql` 都包含 DDL/DML，但不是 `202606*.sql` 命名，原自动发现合同不会覆盖这些高影响 seed。
- P1：SQL 高影响语句识别未覆盖 `replace into`、`insert ignore into`、`drop table`、`truncate table`、`rename table`、`create view`、`drop view` 和 `delete alias from` 这类变体。

## 已修复问题

- 管理端买家/卖家菜单授权脚本改为显式按钮白名单：
  - 保留 `page_menu.perms in ('seller:admin:list', 'buyer:admin:list')` 的父页面边界。
  - 子按钮必须同时命中明确的 `child.menu_id in (...)` 和 `child.perms in (...)`。
  - 覆盖已确认的 seller/buyer 管理端主体、菜单、角色、部门、日志、工单和账号按钮，不再按前缀通配扩散。
- 非 admin 按钮授权清理脚本同步改为同一套显式白名单，避免清理未来非目标按钮。
- legacy `sys_user` 回填 helper 增加：
  - `@legacy_seller_account_ids` / `@legacy_buyer_account_ids`。
  - `@legacy_seller_expected_count` / `@legacy_buyer_expected_count`。
  - `assert_legacy_seller_backfill_targets()` / `assert_legacy_buyer_backfill_targets()`。
  - 回填 preview、guard、DML 均限定在人工确认的账号 ID 白名单内。
- 非日期高影响 seed 增加 fail-closed guard：
  - `business_menu_seed.sql`：`@confirm_business_menu_seed = APPLY_BUSINESS_MENU_SEED`。
  - `currency_configuration_seed.sql`：`@confirm_currency_configuration_seed = APPLY_CURRENCY_CONFIGURATION_SEED`。
  - `upstream_system_management_seed.sql`：`@confirm_upstream_system_management_seed = APPLY_UPSTREAM_SYSTEM_MANAGEMENT_SEED`。
  - `warehouse_us_address_seed.sql`：`@confirm_warehouse_us_address_seed = APPLY_WAREHOUSE_US_ADDRESS_SEED`。
- `SqlExecutionGuardContractTest` 扩展高影响语句识别，并把上述非日期 seed 纳入显式合同。
- `AdminDirectLoginPermissionContractTest` 增加按钮授权白名单合同，禁止回退到 `seller/buyer:admin:%` 通配。
- `SqlExecutionGuardContractTest` 增加 legacy `sys_role` cleanup 目标白名单与 expected_count 的防回归合同。

## 残留问题

- 本切片未执行 SQL，也未连接远程库；这些脚本仍只是代码与合同层面收口。
- 历史 Markdown 中已经记录过的“SQL guard 队列待处理”结论，以本记录为最新状态；没有逐个回写旧记录。
- `warehouse_us_address_seed.sql` 里原有注释/字段注释存在历史编码显示问题，本切片只加 guard，不做数据内容或编码清理。
- 管理端按钮白名单当前直接写在两个 SQL helper 中；后续如果按钮集继续增长，建议改成单独 seed 表或可复用临时表构造，但本轮快速模式不引入新结构。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,AdminDirectLoginPermissionContractTest" test`：通过，`10` 个测试通过。
- `cd E:\Urili-Ruoyi; rg -n "child\.perms\s+like\s+'(?:seller|buyer):admin:%'|m\.perms\s+like\s+'(?:seller|buyer):admin:%'" RuoYi-Vue\sql RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture`：只命中测试文件里的防回归断言，SQL 文件无命中。
- `cd E:\Urili-Ruoyi; rg -n "@confirm_business_menu_seed|@confirm_currency_configuration_seed|@confirm_upstream_system_management_seed|@confirm_warehouse_us_address_seed|call assert_business_menu_seed_confirmed|call assert_currency_configuration_seed_confirmed|call assert_upstream_system_management_seed_confirmed|call assert_warehouse_us_address_seed_confirmed" RuoYi-Vue\sql RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SqlExecutionGuardContractTest.java`：命中 4 个 seed 和合同测试。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅输出既有 CRLF 提示。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，输出 `three-terminal verification passed.`；前端 4 个 test suite、12 个测试通过；后端 ruoyi-system 103、ruoyi-framework 15、product 1、seller 83、buyer 84 个测试通过。

## 未验证原因

- 未执行浏览器运行态验收、截图和 DOM 检测：用户已明确当前快速推进模式无需浏览器、截图、DOM 或 UI 细调。
- 未连接远程 MySQL / Redis：本切片只修改 SQL 脚本和合同测试，不执行远程 DDL/DML。
- 未手工运行 SQL helper：远程 DDL/DML 需要按 AGENTS 记录目标数据源、执行计划和影响范围；本轮没有运行需求。

## 权限检查结果

- 管理端 seller/buyer 页面菜单仍只授予 admin role，且按钮授权必须在已签名父页面下。
- 非 admin 清理 helper 只删除预览确认角色中的已确认管理端 partner 按钮授权，不再按前缀扩大。
- 本切片不改 seller/buyer 端内权限模型，不引入 `sys_*` 复用。

## 字典/选项复用检查结果

- 本切片未新增字典、业务选项或 code/label 映射。
- 已有 currency、upstream、US address seed 只增加执行确认 guard，不改变字典或选项数据。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记非日期高影响 SQL seed guard、legacy 回填目标白名单、管理端按钮授权显式白名单的复用规则。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 2 changed files`。

## 大文件合理性判断结果

- `warehouse_us_address_seed.sql` 是历史生成的大型数据 seed，本轮只在文件头部增加 guard，不拆分数据文件。
- `SqlExecutionGuardContractTest` 已超过轻量测试规模，但当前职责集中在 SQL 安全执行合同；本轮新增的是同一职责下的防回归断言，拆分会增加快速模式下的集成成本。
- 两个 admin partner SQL helper 中按钮白名单重复，属于本轮 P1 快速收口的可接受重复；后续按钮集膨胀时再抽成更集中的构造方式。

## 重复代码检查结果

- 非日期 seed 的确认 guard 复用现有 `top_menu_seed.sql` / `warehouse_management_seed.sql` 模式。
- seller/buyer legacy 回填目标校验保持对称结构，避免只修卖家不修买家。
- 管理端按钮白名单在 grant 和 cleanup 中保持同一组 `menu_id` / `perms`，并由合同测试固定关键哨兵。
