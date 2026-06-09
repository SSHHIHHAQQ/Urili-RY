# 2026-06-07 三端 P0/P1 快速推进：管理端授权收窄与免密退出审计记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 本轮边界

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未修改 source-product / integration 旁支脏改。
- 历史记录（已过期口径）：存量 6 个子 Agent 已全部关闭；后续新增子 Agent 按 AGENTS 规则优先使用 GPT-5.3 Codex，不可用时再降级 `gpt-5.4`。

## 新增问题

- P0：已跟踪配置文件曾出现明文运行配置风险。本轮已将 `application.yml` 中 URILI 加密配置保留为环境变量占位，不输出任何密钥值。
- P1：`20260606_admin_partner_role_menu_grant.sql` 首段授权使用 `seller:admin:%` / `buyer:admin:%` 前缀通配，可能把同前缀漂移菜单一起授给 admin。
- P1：seller/buyer direct-login session 主动退出时，退出登录日志退化为普通日志，可能丢失 `ticketId`、acting admin 和 reason。

## 已修复问题

- `RuoYi-Vue/sql/20260606_admin_partner_role_menu_grant.sql`
  - 首段 `sys_role_menu` 授权收窄为签名确认过的 `menu_id in (2010, 2011, 2012)`。
  - 子按钮仍从已授权的 seller/buyer 管理页面向下补权，不再通过裸权限前缀做首段全局授权。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminDirectLoginPermissionContractTest.java`
  - 增加契约守卫，禁止 admin partner 授权脚本回退到首段 `seller:admin:%` / `buyer:admin:%` 通配授权。
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `logoutSeller(...)` 在 session 为 direct-login 时使用 `PortalTokenSupport.buildDirectLoginLog(..., session)` 写退出日志。
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - `logoutBuyer(...)` 按卖家模板机械复制同构修复。
- `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java`
  - 新增 direct-login 退出日志审计字段保留测试。
- `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`
  - 新增 direct-login 退出日志审计字段保留测试。
- `docs/architecture/reuse-ledger.md`
  - 补充 direct-login 退出日志模板和管理端授权脚本范围模板。

## 残留问题

- P1：强制踢出和密码重置踢出仍只保留汇总日志，尚未做到逐 session direct-login 审计闭环；这需要 mapper 查询在线 session 结构扩展，下一切片处理。
- P1：部分免密失败路径如果 ticket 完全无法解析，仍只能写普通失败日志；若要记录 ticket 级失败上下文，需要调整 `PortalDirectLoginSupport.consumeToken(...)` 的失败上下文返回方式。
- P1：`SqlExecutionGuardContractTest` 对新增高影响 SQL 的自动发现仍不完整，source-product 旁支 SQL 需要另起切片处理。
- P1：`verify-three-terminal` 的后端模块覆盖范围与仓库实际测试模块是否要包含 `finance`，需要在验证治理切片里收口。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=AdminDirectLoginPermissionContractTest test`：通过，`1` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test`：通过，`6` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`46` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`46` 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；结果为 `Already up to date`。
- 配置敏感值扫描：通过；`application.yml` / `application-druid.yml` 中敏感键未发现非环境变量明文值，`UnsafeSensitiveConfigCount=0`。

## 未验证原因

- 未做浏览器、截图、DOM 或 UI 细调验收：用户已明确本阶段无需浏览器运行态验收。
- 未执行远程 SQL：本轮没有新增 DDL/DML，也没有修改远程库必要。
- 未启动后端：本轮已用 Maven 契约和 service 测试覆盖修改点。

## 权限检查结果

- 管理端 seller/buyer 授权脚本不再按裸权限前缀全量授权。
- `AdminDirectLoginPermissionContractTest` 已守住 direct-login / ticket 权限标识和 admin partner 授权范围。

## 字典/选项复用检查结果

- 本轮未新增业务字典、选项或前端下拉。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，记录 direct-login 退出日志和管理端授权脚本模板。

## CodeGraph 更新结果

- `codegraph sync .` 已执行，返回 `Already up to date`。

## 大文件合理性判断结果

- 本轮修改的 Java service 文件属于既有同构 service，新增逻辑为 1 个短分支；未引入新的大文件。
- `docs/architecture/reuse-ledger.md` 和目标追踪文档为长期追加记录，不作为本轮拆分对象。

## 重复代码检查结果

- seller 先修，buyer 按同构模板机械复制，仅替换命名和文案。
- 未新增新的公共抽象，因为当前已有 `PortalTokenSupport.buildDirectLoginLog(..., session)` 可复用。
