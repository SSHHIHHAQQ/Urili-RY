# 2026-06-07 三端 P0/P1 快速推进：管理端强退登录日志 Actor 记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按当前快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 历史记录（已过期口径）：本轮按用户要求优先尝试 GPT-5.3 Codex；由于当前已知额度限制，本轮实际使用并关闭 6 个 `gpt-5.4` 只读扫描子 Agent。
- 本切片采纳 seller/buyer 登录日志 actor 扫描结论：`seller_login_log` / `buyer_login_log` 已有 `acting_admin_id` 和 `acting_admin_name` 字段，不需要 DDL。
- 其它子 Agent 结论已作为后续 P1 排队：静态 `/seller` / `/buyer` 路由 fallback guard、admin 角色 SQL wildcard 授权、旧 SQL 动态补列 guard、端内 oper_log direct-login 结构化审计。

## 新增问题

- 管理端锁定账号、停用主体/账号、强制踢出、重置密码后触发的强退登录日志，普通会话只记录 `FORCE_LOGOUT` 或 `PASSWORD_RESET_FORCE_LOGOUT`，没有结构化记录当前后台操作者。
- 端内自己改密码也复用了强退 helper；如果直接无条件读取若依 `SecurityUtils`，会误伤端内自助改密路径。
- direct-login 会话的 `acting_admin_*` 字段本来表示免密票据签发人；不能在强退时直接覆盖成当前踢出人，否则会把 issuer 语义改坏。

## 已修复问题

- seller/buyer 强退 helper 增加 `auditCurrentAdmin` 内部参数。
- 管理端触发路径继续默认 `auditCurrentAdmin=true`：锁定、停用、强制踢出、重置密码触发的普通会话强退日志会写入当前 admin 的 `actingAdminId` / `actingAdminName`。
- 端内自助改密路径显式传 `auditCurrentAdmin=false`，避免要求端内请求具备若依后台登录态。
- direct-login 会话强退日志保留原 direct-login ticket issuer：`directLogin=true`、`directLoginTicketId`、`actingAdminId`、`actingAdminName`、`directLoginReason` 不被当前强退操作者覆盖。
- seller/buyer 单测同步覆盖：
  - 重置密码混合普通会话与 direct-login 会话时，普通会话写当前 admin，direct-login 会话保留原 issuer。
  - 管理端锁定账号强退普通会话时写当前 admin。
  - 端内自助改密强退普通会话时不写后台 admin。
- 已更新复用台账：`docs/architecture/reuse-ledger.md`。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`48` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`48` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=PortalLoginSessionConsistencyContractTest" test`：通过，`2` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 `3` 个 suite / `9` 个测试通过，后端 ruoyi-system `100`、ruoyi-framework `15`、product `1`、seller `80`、buyer `81` 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；结果为 `Synced 5 changed files`，`Modified: 5 - 631 nodes in 1.1s`。

## 未验证原因

- 未做浏览器运行态验收、截图、DOM 检测或 UI 细调；这是用户明确要求的快速推进边界。
- 未执行远程 MySQL DDL/DML，未读取或写入 Redis；本切片只用现有登录日志字段。
- 未启动或重启后端；本切片通过 service 单测和三端契约入口验证。

## 权限检查结果

- 本轮未新增后端接口，也未新增权限标识。
- 管理端强退、锁定、重置密码仍沿用现有若依后台权限控制和 `@Log` 管理端操作日志。
- 端内自助改密不依赖若依 `sys_user` 登录态，不会因本轮 actor 写入被串到管理端账号体系。

## 字典/选项复用检查结果

- 本轮未新增字典、选项或状态枚举。
- 强退原因仍沿用现有 `FORCE_LOGOUT` / `PASSWORD_RESET_FORCE_LOGOUT` 文本。

## 复用台账检查结果

- 已在 `docs/architecture/reuse-ledger.md` 增加“管理端强退登录日志 Actor 模板”。
- 后续 seller/buyer 同构强退或会话失效类动作，必须先判断是管理端动作还是端内自助动作，再决定是否写当前后台 admin。

## CodeGraph 更新结果

- `codegraph sync .` 已执行并通过；结果为 `Synced 5 changed files`，`Modified: 5 - 631 nodes in 1.1s`。

## 大文件合理性判断结果

- `SellerServiceImpl.java` / `BuyerServiceImpl.java` 属于既有同构 service，文件较大但本轮只补强退审计 helper 参数与 actor 写入，未新增业务模块或跨职责重构。
- `SellerServiceImplTest.java` / `BuyerServiceImplTest.java` 已超过阈值，但本轮只补既有测试用例断言和小型 helper；拆分测试会引入额外结构性改动，不符合当前 P0/P1 快速模式。

## 重复代码检查结果

- seller 先落模板，buyer 按同构模式机械复制。
- helper 命名和测试断言保持 seller/buyer 对称，没有新增跨模块公共抽象；当前两端 service 仍按模块独立维护。

## 残留问题

- direct-login 会话被后台强退时，当前 schema 只能结构化保存一个 admin 身份；本轮保留原免密签发人，不把当前踢出人覆盖进去。如需同时结构化保存 issuer 与 operator，需要单独设计字段或事件审计方案。
- 强退无在线会话时仍不会生成登录日志；是否需要记录“尝试强退但无在线会话”应另定审计口径。
- 静态 `/seller` / `/buyer` 路由 fallback guard 已由 `docs/plans/2026-06-07-three-terminal-p0p1-static-partner-route-guard-record.md` 的回归保护补强收口。
- `20260606_admin_partner_role_menu_grant.sql` wildcard 授权已由后续白名单/合同检查点收口为明确白名单。
- 旧 SQL 动态补列 guard 和端内 `oper_log` direct-login 结构化审计仍需后续单独处理。
