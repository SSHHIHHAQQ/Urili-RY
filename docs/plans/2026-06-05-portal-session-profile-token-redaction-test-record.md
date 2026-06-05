# 2026-06-05 端内会话响应 tokenId 脱敏测试记录

## 目标

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理一个切片：固定 `PortalSessionProfile.tokenId` 不得序列化输出给 seller/buyer 端和管理端会话列表响应。

本切片不新增接口，不改 SQL，不执行远程数据库 DDL/DML，不改变 seller/buyer 会话查询逻辑，不复制买家，也不启动三端前端物理拆分。

## 已完成

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalSessionProfileTest.java`。
- 测试覆盖：
  - `terminal`、`subjectId`、`accountId`、`current` 等会话展示字段可正常序列化。
  - `tokenId` 字段名不会出现在 JSON 中。
  - `tokenId` 的内部值不会出现在 JSON 中。
- 保持现有 `PortalSessionProfile.tokenId` 的内部 getter/setter 能力，继续供后端判断 `current` 使用。

## 子 agent 结论

- 文档审计建议下一类工作应进入“端内真实业务接口范围控制模板”，不是立即做三端前端物理拆分。
- seller 后端审计未发现当前 seller portal Controller 直接接收前端传入 `sellerId` 作为端内数据范围。
- seller 后端审计指出当前商品 Schema 是全局只读配置，暂未造成跨卖家数据泄露；下一类更适合做 seller 端商品 SPU/SKU 列表、详情或状态等真实业务接口模板。
- 推荐下一刀：seller 端“我的商城商品”只读查询后端模板，从 `PortalSessionContext` 推导 sellerId，不相信前端传入 `sellerId` / `subjectId` / `accountId`。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=PortalSessionProfileTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalRouteOwnershipTest,PortalSessionProfileTest" test`：通过，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\domain\PortalSessionProfileTest.java docs\plans\2026-06-05-portal-session-profile-token-redaction-test-record.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\architecture\reuse-ledger.md`：通过，仅有 LF/CRLF 工作区换行提示。
- 相关文件尾随空白检查：通过。
- 相关文件冲突标记检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，首次同步输出 `Synced 3 changed files`；记录回填后最终复跑输出 `Already up to date`。

## 当前判断

- 端内和管理端会话列表继续可以复用 `PortalSessionProfile`，但 `tokenId` 只能作为后端内部字段。
- 该守卫降低后续会话 UI、会话列表接口或 DTO 调整时误把 `tokenId` 暴露给前端的回归风险。
- 下一切片更适合进入 seller 端“我的商城商品”只读查询后端模板，卖家验收通过后再评估买家浏览模板；不要机械按 `buyerId` 复制商品拥有关系。
