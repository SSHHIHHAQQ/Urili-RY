# 三端隔离核心 SQL 只读审计（P0/P1）

日期：2026-06-06
工作区：`E:\Urili-Ruoyi`
范围：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 约束下，仅审 `RuoYi-Vue/sql` 中三端隔离核心 SQL：`20260604_three_terminal_isolation_migration.sql`、`seller_buyer_management_seed.sql`、`20260604_portal_direct_login_ticket.sql`、`20260605_*account*`、`20260606_admin_partner_page_direct_login_seed.sql`。
执行方式：只读；未改业务代码；未执行远程 DDL/DML。

## 一句话结论

远程库当前未命中 live P0；核心账号/角色/菜单/部门/日志/会话表、关键列、唯一索引、生成列、direct-login 审计表均已到位。P1 主要集中在脚本幂等与“部分执行自愈”能力，现网当前是健康的，但分批回放时仍有脚本级残留风险。

## 数据源确认

- 后端激活配置：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 启用 `druid` profile。
- 远程 MySQL 来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`
- 只读确认结果：
  - 当前库：`fenxiao`
  - 版本：`8.0.30-cynos-3.1.16.003`
  - 兼容性结论：远程库支持 `STORED GENERATED`，本次涉及的 OWNER 生成列语法在当前库兼容。

## 新增问题

### P1-1：`20260606_admin_partner_page_direct_login_seed.sql` 的“补齐 split seed 缺口”声明不成立，缺少父菜单 `2010`

- 文件/行：
  - [RuoYi-Vue/sql/20260606_admin_partner_page_direct_login_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260606_admin_partner_page_direct_login_seed.sql#L1) 第 1-3 行
  - [RuoYi-Vue/sql/20260606_admin_partner_page_direct_login_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260606_admin_partner_page_direct_login_seed.sql#L12) 第 12-23 行
  - [RuoYi-Vue/sql/seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql#L573) 第 573-586 行
- 计划依据：
  - [docs/plans/2026-06-04-three-terminal-isolation-control-plan.md](E:\Urili-Ruoyi\docs\plans\2026-06-04-three-terminal-isolation-control-plan.md#L419) 第 419 行起要求管理端具备端内权限基础。
- 证据：
  - `20260606_admin_partner_page_direct_login_seed.sql` 注释写明“在不回放 `seller_buyer_management_seed.sql` 时补齐 gap”，但实际只 upsert 了 `2011/2012/2205/2215`。
  - 顶级“主体管理”菜单 `2010` 仅在 `seller_buyer_management_seed.sql` 第 578-586 行创建。
  - 如果环境缺 `2010` 只回放 split seed，`2011/2012` 会挂到不存在的父节点下；MySQL 无外键时报错，但前端菜单树可能直接缺页或孤儿化。
  - 远程库当前只读查询已确认 `2010/2011/2012/2205/2215` 都存在，因此这不是现网命中故障，而是脚本回放风险。
- 建议：
  - 直接把 `2010` 补进 `20260606_admin_partner_page_direct_login_seed.sql`；
  - 或把文件头注释改成强依赖 `seller_buyer_management_seed.sql`，不要继续宣称能单独补齐。

### P1-2：`20260604_portal_direct_login_ticket.sql` 只有 `CREATE TABLE IF NOT EXISTS`，对“已存在但列/索引不完整”的环境没有修复能力

- 文件/行：
  - [RuoYi-Vue/sql/20260604_portal_direct_login_ticket.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_portal_direct_login_ticket.sql#L8) 第 8-33 行
- 计划依据：
  - [docs/plans/2026-06-04-three-terminal-isolation-control-plan.md](E:\Urili-Ruoyi\docs\plans\2026-06-04-three-terminal-isolation-control-plan.md#L229) 第 229 行起要求 `portal_direct_login_ticket` 承载 direct-login 审计票据。
- 证据：
  - 脚本仅在表不存在时建表，没有 `add_column_if_missing` / `add_index_if_missing`。
  - 该表核心依赖列/索引包括：`token_hash` 唯一键、`(terminal,target_subject_id,target_account_id)`、`(acting_admin_id,create_time)`、`(status,expire_time)`。
  - 远程库 `SHOW CREATE TABLE portal_direct_login_ticket` 已确认当前这些列和索引都存在，所以现网当前可用。
- 风险：
  - 若某环境先落了早期简化版表结构，再单独回放本文件，脚本会静默跳过，最终留下“表存在但审计/消费索引不全”的半执行状态。
- 建议：
  - 参照 `20260604_three_terminal_isolation_migration.sql` 的 helper 模式，为该表补一份显式迁移；
  - 至少补 `information_schema.columns/statistics` 检查和自愈。

### P1-3：OWNER 唯一性脚本按“列名/索引名存在”判定，不校验生成列表达式本身，部分执行环境可能被误判为已完成

- 文件/行：
  - [RuoYi-Vue/sql/20260605_terminal_owner_account_unique_constraint.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260605_terminal_owner_account_unique_constraint.sql#L53) 第 53-60 行
  - [RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql#L150) 第 150-201 行
- 计划依据：
  - [docs/plans/2026-06-04-three-terminal-isolation-control-plan.md](E:\Urili-Ruoyi\docs\plans\2026-06-04-three-terminal-isolation-control-plan.md#L381) 第 381-395 行要求端账号能独立承载登录身份。
- 证据：
  - 脚本只判断 `owner_unique_seller_id` / `owner_unique_buyer_id` 以及对应唯一索引名是否存在，不比较 `GENERATED ALWAYS AS (...) STORED` 的真实定义。
  - 如果某环境手工建过同名普通列、或表达式错误，脚本会因为“名字已存在”而跳过，不会自愈。
  - 远程库当前 `information_schema.columns.extra` 已确认：
    - `seller_account.owner_unique_seller_id = STORED GENERATED`
    - `buyer_account.owner_unique_buyer_id = STORED GENERATED`
  - 远程只读计数也确认无重复 OWNER、无重复用户名，因此当前现网未命中该问题。
- 建议：
  - 增加对 `information_schema.columns.generation_expression` 的校验；
  - 对不匹配定义的环境显式 `SIGNAL`，避免误判为已完成。

## 已修复问题

- 本次为只读审计，未执行修复。

## 残留问题

- 当前未发现 live P0。
- 当前未发现会直接阻断远程库账号/角色/菜单/部门/日志/会话可用性的 live P1。
- 仍有上述 3 个脚本级 P1 残留，集中在“分批回放”和“部分执行自愈”。

## 远程库当前状态证据

### 核心表存在性

只读查询确认以下 21 张核心表均存在：

- `seller` / `buyer`
- `seller_account` / `buyer_account`
- `seller_dept` / `buyer_dept`
- `seller_role` / `buyer_role`
- `seller_menu` / `buyer_menu`
- `seller_account_role` / `buyer_account_role`
- `seller_role_menu` / `buyer_role_menu`
- `seller_login_log` / `buyer_login_log`
- `seller_oper_log` / `buyer_oper_log`
- `seller_session` / `buyer_session`
- `portal_direct_login_ticket`

### 账号表关键列与唯一约束

计划要求的卖家/买家端账号承载体见：

- [docs/plans/2026-06-04-three-terminal-isolation-control-plan.md](E:\Urili-Ruoyi\docs\plans\2026-06-04-three-terminal-isolation-control-plan.md#L65) 第 65-141 行
- [RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql#L81) 第 81-201 行

远程只读结果：

- `seller_account` / `buyer_account` 均已具备：
  - `user_name`
  - `nick_name`
  - `password`
  - `lock_status`
  - `lock_reason`
  - `last_login_ip`
  - `last_login_time`
  - `pwd_update_time`
  - `owner_unique_*` 且为 `STORED GENERATED`
- `user_id` 已删除，符合计划第 337-341 行。
- 唯一索引存在：
  - `uk_seller_account_username`
  - `uk_buyer_account_username`
  - `uk_seller_account_owner`
  - `uk_buyer_account_owner`
- 只读计数结果：
  - OWNER 重复：`0`
  - 用户名重复：`0`
  - `user_name/nick_name/password/lock_status/lock_reason` 脏值：`0`

### 菜单/权限种子

远程只读查询确认以下管理端菜单均已落库：

- `2011/2012`：卖家管理 / 买家管理
- `2200-2255`：卖家/买家查询、增改、停用、重置密码、免密登录、强制踢出、菜单、角色、部门、登录日志、操作日志、ticket 列表
- `2310-2323`：卖家/买家账号列表、增改、锁定、重置密码、角色查询、角色分配

对应文件：

- [RuoYi-Vue/sql/seller_buyer_management_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\seller_buyer_management_seed.sql#L573) 第 573-796 行
- [RuoYi-Vue/sql/20260605_admin_seller_account_permission_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260605_admin_seller_account_permission_seed.sql#L3) 第 3-46 行
- [RuoYi-Vue/sql/20260605_admin_buyer_account_permission_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260605_admin_buyer_account_permission_seed.sql#L3) 第 3-46 行
- [RuoYi-Vue/sql/20260606_admin_partner_page_direct_login_seed.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260606_admin_partner_page_direct_login_seed.sql#L7) 第 7-41 行

### 角色/菜单/账号绑定闭环

远程只读计数确认：

- `seller_owner_without_role = 0`
- `buyer_owner_without_role = 0`
- `seller_role_without_menu = 0`
- `buyer_role_without_menu = 0`

说明当前远程库至少已经形成：

- OWNER 账号 -> OWNER 角色
- OWNER 角色 -> 端内菜单

的基本闭环。

### 日志/会话表

对应计划与 SQL：

- [docs/plans/2026-06-04-three-terminal-isolation-control-plan.md](E:\Urili-Ruoyi\docs\plans\2026-06-04-three-terminal-isolation-control-plan.md#L25) 第 25-26 行
- [RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql](E:\Urili-Ruoyi\RuoYi-Vue\sql\20260604_three_terminal_isolation_migration.sql#L359) 第 359-480 行

远程只读结果：

- `seller_session` / `buyer_session` 均存在主键和账号索引、过期时间索引。
- `seller_login_log` / `buyer_login_log` 均已具备主体列、账号列及复合索引。
- `seller_oper_log` / `buyer_oper_log` 均已具备主体列、账号列及复合索引。
- 现网已有数据：
  - `seller_session = 96`
  - `buyer_session = 62`
  - `seller_login_log = 140`
  - `buyer_login_log = 91`
  - `seller_oper_log = 284`
  - `buyer_oper_log = 199`

这说明“表存在但完全不可用”的风险在当前远程库未命中。

## 权限检查结果

- 管理端卖家/买家页与 direct-login 权限点存在：
  - `seller:admin:list`
  - `buyer:admin:list`
  - `seller:admin:directLogin`
  - `buyer:admin:directLogin`
- 管理端账号维护权限点存在：
  - `seller:admin:account:list/add/edit/lock/resetPwd/role:query/role:edit`
  - `buyer:admin:account:list/add/edit/lock/resetPwd/role:query/role:edit`
- 端内只读基础权限闭环存在：
  - `seller:account:list` / `buyer:account:list`
  - `seller:dept:list` / `buyer:dept:list`
  - `seller:role:list` / `buyer:role:list`

## 字典/选项复用检查结果

- 远程库已存在：
  - `seller_account_role`
  - `buyer_account_role`
  - `seller_account_lock_status`
  - `buyer_account_lock_status`
- 锁定状态字典值 `0/1` 与账号角色字典值 `OWNER/ADMIN/STAFF` 均已落库。
- 本次审计未发现“账号锁定依赖字段已存在但字典未落库”的 live P1。

## 复用台账检查结果

- 本次为 SQL 只读审计，未涉及新增实现，不更新 `docs/architecture/reuse-ledger.md`。
- 但从审计角度看，`20260605_admin_seller_account_permission_seed.sql`、`20260605_admin_buyer_account_permission_seed.sql`、`20260606_admin_partner_page_direct_login_seed.sql` 之间存在明显重复 seed 片段；本次未纳入 P0/P1，只记录为后续治理项。

## 大文件合理性判断结果

- 本次不涉及代码文件新增/扩写。
- `seller_buyer_management_seed.sql` 为综合 seed，大但职责集中，当前主要风险不是体量，而是“综合 seed + 拆分 seed”并行后带来的回放依赖不透明。

## 重复代码检查结果

- `20260605_admin_seller_account_permission_seed.sql` 与 `20260605_admin_buyer_account_permission_seed.sql` 基本为镜像模板。
- `20260606_admin_partner_page_direct_login_seed.sql` 与 `seller_buyer_management_seed.sql` 中对应菜单片段重复。
- 该重复目前未直接造成现网不可用，因此未升为 P0/P1；但它放大了“部分执行后误以为补齐”的概率。

## 验证命令

以下均为只读：

```powershell
Get-Content -Raw docs/plans/2026-06-04-three-terminal-isolation-control-plan.md
Get-Content -Raw RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml
Get-Content -Raw RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml
Get-Content -Raw RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql
Get-Content -Raw RuoYi-Vue/sql/seller_buyer_management_seed.sql
Get-Content -Raw RuoYi-Vue/sql/20260604_portal_direct_login_ticket.sql
Get-Content -Raw RuoYi-Vue/sql/20260605_terminal_owner_account_unique_constraint.sql
Get-Content -Raw RuoYi-Vue/sql/20260605_admin_seller_account_permission_seed.sql
Get-Content -Raw RuoYi-Vue/sql/20260605_admin_buyer_account_permission_seed.sql
Get-Content -Raw RuoYi-Vue/sql/20260605_seller_account_lock_control.sql
Get-Content -Raw RuoYi-Vue/sql/20260605_buyer_account_lock_control.sql
Get-Content -Raw RuoYi-Vue/sql/20260606_admin_partner_page_direct_login_seed.sql
```

```python
# 只读 pymysql 核对：
# 1. VERSION()/DATABASE()
# 2. information_schema.tables / columns / statistics
# 3. SHOW CREATE TABLE seller_account / buyer_account / portal_direct_login_ticket
# 4. sys_menu / sys_dict_type / sys_dict_data / sys_config 查询
# 5. 重复 OWNER、重复用户名、空值/脏值计数
```

## 未验证原因

- 未执行任何远程 DDL/DML，遵守本次只读约束。
- 未验证应用运行时界面展示，仅做 SQL 和远程 schema 级审计。
- 未补跑 CodeGraph；本次没有代码更新。

## CodeGraph 更新结果

- 未执行。原因：本次无代码改动，且用户要求只读审计。

## 下一步建议

1. 先补 `20260606_admin_partner_page_direct_login_seed.sql` 的 `2010` 父菜单或改正文件头依赖声明。
2. 给 `20260604_portal_direct_login_ticket.sql` 增加列/索引自愈逻辑，避免“表已存在即跳过”。
3. 给 OWNER 生成列脚本增加 `generation_expression` 校验和 fail-closed 报错。
4. 后续若要真正清理 split-seed 风险，建议把“综合 seed”和“增量 seed”的依赖顺序写进单独执行说明，而不是只靠文件名传达。
