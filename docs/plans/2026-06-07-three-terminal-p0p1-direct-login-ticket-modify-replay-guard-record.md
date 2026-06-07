# 2026-06-07 三端 P0/P1 免密票据表 MODIFY 可重放 Guard 记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按快速推进模式只修 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本轮聚焦 `20260604_portal_direct_login_ticket.sql` 中免密票据审计表定义收敛的可重放风险；不做浏览器运行态验收、不做截图/DOM 检测、不做 UI 细调。

## 新增问题

- P1：`20260604_portal_direct_login_ticket.sql` 原先对 `portal_direct_login_ticket` 使用整段裸 `ALTER TABLE ... MODIFY`，脚本重放时会无条件执行 DDL，也可能在后续 schema 合法演进后被旧脚本回压。
- P1：`PortalDirectLoginTicketSqlContractTest` 原先显式要求 `modify token_hash` / `modify expire_time` 存在，但没有要求条件 helper 或禁止顶层裸 `MODIFY`，等于把裸 DDL 固化成合同。

## 已修复问题

- `20260604_portal_direct_login_ticket.sql` 新增 `modify_portal_direct_login_ticket_columns_if_needed()`。
- helper 先通过 `information_schema.columns` 确认 `portal_direct_login_ticket` 目标审计列完整；缺列时主动 `signal sqlstate '45000'`，错误信息为 `portal_direct_login_ticket expected columns are required before ticket column modify`。
- helper 比对 `data_type`、`character_maximum_length`、`is_nullable` 和 `column_default`；只有当前定义不满足目标定义时才执行动态 `MODIFY`。
- 原顶层裸 `ALTER TABLE portal_direct_login_ticket ... MODIFY ...` 已改为 helper 调用，并保持在 `assert_no_invalid_direct_login_ticket_rows()` 之后、`recreate_index_if_mismatch(...)` 之前。
- `PortalDirectLoginTicketSqlContractTest` 改为要求 helper、顺序和禁止顶层裸 `MODIFY`。
- `SqlExecutionGuardContractTest` 新增 `portalDirectLoginTicketMigrationMustUseReplaySafeModifyHelper()`，作为同类 SQL guard 合同。

## 残留问题

- P1：旧 `portal_direct_login:{token_hash}` Redis key 仍有兼容读取路径；当前新发 token 已只写 `portal_direct_login:{terminal}:{token_hash}`，wrong-terminal 已在 ticket 层拒绝，下一切片可改为仅读新 key、旧 key 只允许清理。
- P2：`sys_menu 2010` 仍有多个脚本写同一最终签名，但当前已由台账和合同约束为 top owner + 同签名兼容 seed；不是本轮 P1。
- P1 交付风险：部分菜单 owner/SQL guard 修复仍处于脏工作区，后续需要统一稳定交付，避免切回旧 revision 后风险回归。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=PortalDirectLoginTicketSqlContractTest,SqlExecutionGuardContractTest" test`：通过，`PortalDirectLoginTicketSqlContractTest` 2 个测试、`SqlExecutionGuardContractTest` 29 个测试，合计 31 个测试通过。
- `cd E:\Urili-Ruoyi; rg -n "alter table portal_direct_login_ticket\s*$|modify_portal_direct_login_ticket_columns_if_needed|portal_direct_login_ticket expected columns|modify token_hash|modify expire_time" RuoYi-Vue\sql\20260604_portal_direct_login_ticket.sql RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\PortalDirectLoginTicketSqlContractTest.java RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SqlExecutionGuardContractTest.java`：通过，迁移脚本不再存在顶层裸 `ALTER TABLE portal_direct_login_ticket` 行，定义收敛只在 helper 动态 DDL 内出现。

## 未验证原因

- 未做浏览器运行态验收、截图、DOM 检测：用户已明确当前快速推进模式无需浏览器验证。
- 未连接远程 MySQL / Redis，未执行 SQL，未写远程数据：本轮是 SQL 脚本与静态合同测试修复，不需要对远程库执行迁移。
- 未启动后端：本轮不涉及运行态接口或权限行为变更。

## 权限检查结果

- 本轮未改管理端、卖家端、买家端权限判断逻辑。
- 本轮只加强免密票据表迁移脚本的 DDL 可重放性，不改变免密票据签发、消费、审计权限语义。

## 字典/选项复用检查结果

- 本轮未新增字典类型、字典数据或前端选项。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，把 `modify_portal_direct_login_ticket_columns_if_needed()` 纳入 SQL DDL 可重放 helper 与条件 modify guard 规则。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 返回 `Synced 2 changed files`，`Modified: 2 - 81 nodes in 1.2s`。

## 大文件合理性判断结果

- `SqlExecutionGuardContractTest.java` 已超过 500 行，但职责仍集中在 SQL 执行 guard 合同；本轮只追加同类静态合同，不在快速推进切片中拆分。
- `PortalDirectLoginTicketSqlContractTest.java` 仍是单一专项合同测试，新增顺序 helper 后职责未扩散。
- `20260604_portal_direct_login_ticket.sql` 保持单脚本单职责：免密票据审计表建表、历史半成品表自愈、非法 legacy 行 fail-closed、索引修复和结构断言。

## 重复代码检查结果

- 新 helper 只在 `20260604_portal_direct_login_ticket.sql` 内局部使用，未复制 Java Service 或 React 业务逻辑。
- SQL 动态 DDL helper 与既有脚本内局部 helper 模式一致，符合当前 MySQL 脚本没有 include 机制的写法。

## 子 Agent 使用记录

- 按用户最新要求先尝试 `gpt-5.3-codex-spark` 子 Agent；平台返回额度限制，恢复时间为 `2026-06-13 01:59`，失败 Agent 已关闭。
- 回退使用 6 个 `gpt-5.4` 只读子 Agent，分别复核 legacy Redis key、`sys_menu 2010` 兼容 owner、`business_menu_seed` 专用菜单 owner、免密票据 SQL 裸 `MODIFY`、端内 oper_log 结构化审计和 fresh bootstrap schema 缺口。
- 本轮采纳免密票据 SQL 裸 `MODIFY` 结论；`business_menu_seed` 菜单 owner 和端内 oper_log 结构化审计当前代码面已收口，未作为本轮 P1 修改。

## 一句话总结

本轮把免密票据审计表迁移脚本中 18 列无条件裸 `MODIFY` 收进 replay-safe 条件 helper，并用专项合同和 SQL guard 合同锁住；未执行远程 SQL。
