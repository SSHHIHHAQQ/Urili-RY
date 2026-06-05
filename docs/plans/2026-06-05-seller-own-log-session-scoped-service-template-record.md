# 卖家端当前账号日志 session-scoped Service 模板执行记录

日期：2026-06-05

## 背景

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为当前方向。

本轮只处理一类问题：卖家端当前账号登录日志/操作日志列表的数据范围由 Service 内的 `PortalLoginSession` 强制收敛，而不是由 Controller 直接覆盖查询 DTO。

不处理的内容：

- 不复制买家端。
- 不改 SQL、表结构、菜单或权限。
- 不改前端接口、页面或 token 持久化。
- 不执行远程数据库 DDL/DML。

## 已完成

- `ISellerService` 新增卖家端当前账号日志专用方法：
  - `selectSellerOwnLoginLogList(PortalLoginSession, PortalLoginLog)`
  - `selectSellerOwnOperLogList(PortalLoginSession, PortalOperLog)`
- `SellerServiceImpl` 新增 session-scoped 查询模板：
  - 先校验当前 session 对应的卖家主体和卖家账号仍有效。
  - 创建干净查询对象。
  - 强制使用 session 中的 `subjectId` / `accountId`。
  - 登录日志只保留 `userName`、`ipaddr`、`status`、`params.beginTime`、`params.endTime`。
  - 操作日志只保留 `title`、`operName`、`status`、`params.beginTime`、`params.endTime`。
- `SellerPortalController` 中 `/seller/account/login-logs` 和 `/seller/account/oper-logs` 改为调用 own-log Service 方法。
- `SellerServiceImplTest` 新增两个测试，覆盖：
  - 前端伪造 `subjectId` / `accountId` 会被 session 覆盖。
  - 安全筛选字段会保留。
  - 额外 `params` 不会进入 mapper 查询对象。

## 并行审计结论

- 当前 controller 覆盖 `subjectId` / `accountId` 的实现可用，但 service 原本只是透传 mapper，不适合作为后续模板。
- 当前 mapper 实际使用的登录日志筛选字段是 `subjectId`、`accountId`、`userName`、`ipaddr`、`status`、`params.beginTime`、`params.endTime`。
- 当前 mapper 实际使用的操作日志筛选字段是 `subjectId`、`accountId`、`title`、`operName`、`status`、`params.beginTime`、`params.endTime`。
- buyer 与 seller 当前同构，卖家验收通过后复制买家只需要替换 terminal、service、controller、mapper、测试名和文案。
- 6 个子 agent 均已关闭。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -Dtest=SellerServiceImplTest test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller test`：通过，`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalRouteOwnershipTest test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\controller\SellerPortalController.java RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\ISellerService.java RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\service\impl\SellerServiceImpl.java RuoYi-Vue\seller\src\test\java\com\ruoyi\seller\service\impl\SellerServiceImplTest.java docs\plans\2026-06-05-seller-own-log-session-scoped-service-template-record.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\architecture\reuse-ledger.md`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，本轮代码变更后的首次同步输出 `Synced 4 changed files`；记录回填后最终复跑输出 `Already up to date`。

## 当前判断

- 卖家端当前账号日志列表已经形成标准模板：Controller 负责入口、鉴权、取 session、分页；Service 负责数据范围安全边界。
- 管理端全量审计接口继续走通用 `selectSellerLoginLogList` / `selectSellerOperLogList`，不受本切片影响。
- 买家端本轮没有复制，下一切片应在卖家模板验收通过后按同构规则复制。
