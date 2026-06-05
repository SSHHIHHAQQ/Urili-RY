# PortalTokenSupport 端 token 隔离测试记录

## 背景

当前三端独立改造以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。管理端继续使用若依 `TokenService` 和 `login_tokens:` 缓存；卖家端、买家端通过 `PortalTokenSupport` 签发端 token，并使用 `portal_login_tokens:{terminal}:{tokenId}` 缓存端会话。

本轮按“每个切片只改一类东西”的节奏推进，只补自动化测试守卫，不改业务代码、不改前端、不执行 SQL、不连接远程 MySQL 或 Redis。

## 本次改动

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalTokenSupportTest.java`。
- 使用测试内 `RecordingRedisCache` 替代真实 Redis，不读取或写入远程 Redis。
- 使用测试内 request 代理绑定 `RequestContextHolder`，只模拟 `Authorization`、`User-Agent` 和远端 IP。
- 覆盖 `createLogin(...)` 使用 `portal_login_tokens:seller:seller_...` 写入端内 Redis key，并验证不会写入管理端 `login_tokens:` 前缀。
- 覆盖 `getSession(...)` 必须同时满足 JWT claim terminal 与 Redis session terminal 匹配。
- 覆盖 seller token 不能作为 buyer session 使用，`requireSession("buyer")` 返回未授权异常。
- 覆盖 `deleteLoginTokens(...)` 和 `deleteLoginToken(...)` 按指定 terminal 拼接删除 key。

## 设计判断

- 当前短期模式是“共享 token secret / header / expireTime，但通过 JWT claim 和 Redis key 区分 seller、buyer”。本测试把这个边界固定下来。
- 测试只验证 `PortalTokenSupport` 的端隔离行为，不扩大到验证码、登录失败限流、独立 Spring Security filter chain 或真实 HTTP 链路。
- `PortalTokenSupportTest.java` 最终 288 行，低于 300 行大文件自检阈值；职责单一，不拆分。

## 验证结果

- `cd RuoYi-Vue; mvn -pl ruoyi-system -Dtest=PortalTokenSupportTest test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `cd RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalTokenSupportTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-portal-token-support-isolation-test-record.md`：通过，仅有 LF/CRLF 提示。
- `codegraph sync .`：通过；代码变更同步时输出 `Synced 1 changed files`，记录补充后最终复跑输出 `Already up to date`。

## 未执行事项

- 本轮没有执行 DDL/DML。
- 本轮没有连接远程 MySQL 或 Redis。
- 本轮没有重启后端。
- 本轮没有改动前端。

## 风险与剩余事项

- 本测试不证明真实 HTTP 过滤链已经三端完全独立；它只证明 `PortalTokenSupport` 的 token claim 和 Redis key 隔离行为。
- seller/buyer 登录验证码、失败次数锁定、限流和是否拆独立 token 配置仍需后续单独设计。
- 如果后续拆出 seller/buyer 独立 token secret、header 或 expireTime，本测试需要同步更新为新的配置口径。
