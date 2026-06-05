# 买家端当前账号日志 session-scoped Service 模板复制记录

日期：2026-06-05

## 背景

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为当前方向，接在卖家端当前账号日志 session-scoped Service 模板验收之后。

本轮只处理一类问题：把已验证的卖家端当前账号登录日志/操作日志 Service 范围收敛模板按同构规则复制到买家端。

不处理的内容：

- 不改 SQL。
- 不改前端。
- 不改接口路径。
- 不改权限模型。
- 不执行远程数据库 DDL/DML。
- 不重新设计筛选字段或端内日志页面。

## 已完成

- 更新 `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/IBuyerService.java`，新增：
  - `selectBuyerOwnLoginLogList(PortalLoginSession, PortalLoginLog)`
  - `selectBuyerOwnOperLogList(PortalLoginSession, PortalOperLog)`
- 更新 `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`：
  - 新增 `assertBuyerSessionAccount(PortalLoginSession)`，校验当前买家端账号仍属于当前买家主体。
  - 新增 `buildBuyerOwnLoginLogQuery(...)`，创建干净查询对象并强制写入 session 中的 `subjectId` / `accountId`。
  - 新增 `buildBuyerOwnOperLogQuery(...)`，创建干净查询对象并强制写入 session 中的 `subjectId` / `accountId`。
  - 新增 `copyTimeRangeParams(...)`，只复制 `beginTime` / `endTime`。
  - `selectBuyerOwnSessionList(...)` 复用 `assertBuyerSessionAccount(...)`。
- 更新 `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java`：
  - Controller 只负责 `PortalSessionContext.requireSession("buyer")`、分页和返回表格。
  - Controller 不再直接覆盖日志查询 DTO 的 `subjectId` / `accountId`。
- 更新 `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`：
  - 验证前端伪造 `subjectId` / `accountId` 会被 session 覆盖。
  - 验证安全筛选字段保留。
  - 验证额外 `params` 不会被带入 mapper 查询对象。
- 更新 `docs/architecture/reuse-ledger.md`，登记买家端 session-scoped Service 模板。
- 更新 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 顶部当前状态。

## 并行审计结论

- 6 个只读子 agent 已完成并关闭。
- buyer/seller 两端 Mapper 支持的日志筛选字段一致：
  - 登录日志：`userName`、`ipaddr`、`status`、`params.beginTime`、`params.endTime`。
  - 操作日志：`title`、`operName`、`status`、`params.beginTime`、`params.endTime`。
- buyer 可复用 `selectBuyerAccountById(...)` 做主体和账号归属校验；本轮已抽出无副作用的 `assertBuyerSessionAccount(...)`。
- 文档口径采用追加检查点，不回改早期历史检查点；顶部当前状态和复用台账改为双端已完成。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -Dtest=BuyerServiceImplTest test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer test`：通过，`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalRouteOwnershipTest test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\controller\BuyerPortalController.java RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\IBuyerService.java RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\service\impl\BuyerServiceImpl.java RuoYi-Vue\buyer\src\test\java\com\ruoyi\buyer\service\impl\BuyerServiceImplTest.java docs\plans\2026-06-05-buyer-own-log-session-scoped-service-template-record.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\architecture\reuse-ledger.md`：通过，仅有 LF/CRLF 工作区换行提示。
- 相关文件尾随空白检查：通过。
- 相关文件冲突标记检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，本轮代码变更后的首次同步输出 `Synced 4 changed files`；记录回填后最终复跑输出 `Already up to date`。

## 当前判断

- 买家端当前账号日志列表已经按卖家标准模板完成同构复制：Controller 负责入口和分页，Service 负责 session-scoped 数据范围。
- seller/buyer 双端当前账号日志接口现在均不再依赖 Controller 覆盖 DTO 作为数据范围安全边界。
- 管理端全量审计接口仍继续走 `selectBuyerLoginLogList` / `selectBuyerOperLogList`，没有被端内 own-log 模板影响。
