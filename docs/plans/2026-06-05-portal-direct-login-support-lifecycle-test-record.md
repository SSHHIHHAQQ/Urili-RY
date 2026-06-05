# 免密登录 Support 生命周期测试执行记录

## 背景

当前开发方向以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为准：管理端保留控制权，卖家端/买家端账号权限独立；管理端免密代入必须短时、一次性、可审计。

前序切片已经完成免密票据列表 `tokenHash` 脱敏。本轮只补 `PortalDirectLoginSupport` 生命周期自动化守卫，不改生产逻辑、不改 UI、不改接口路径、不执行 SQL。

## 本次改动

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java`。
- 测试使用 fake Redis、fake ticket mapper、fake config service 和测试 SecurityContext，直接验证真实 `PortalDirectLoginSupport`。
- 覆盖生成免密 token 时：
  - 入库票据只保存 SHA-256 hash，不保存明文 token。
  - acting admin、目标端、目标主体、目标账号、代入原因和 30 分钟过期时间写入票据。
  - Redis payload 写入 `portal_direct_login:{token}`，TTL 为 30 分钟。
  - 返回 URL 使用配置的端地址和 `directLoginToken`。
- 覆盖消费免密 token 时：
  - 标记 ticket 为 used，记录使用 IP 和 updateBy=`system`。
  - 删除 Redis payload。
  - 二次消费被拒绝。
- 覆盖跨端消费：
  - seller token 用 buyer 端消费会被拒绝。
  - 不标记 used、不标记 expired、不删除 Redis payload。
- 覆盖过期 ticket：
  - 标记 ticket 为 expired。
  - 删除 Redis payload。
- 覆盖空代入原因：
  - 直接拒绝，不写 ticket，不写 Redis。
- 更新 `docs/architecture/reuse-ledger.md`，登记 `PortalDirectLoginSupportTest` 的守卫范围。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system -Dtest=PortalDirectLoginSupportTest test
```

验证结果：

- `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

模块回归：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system test
```

- `Tests run: 32, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

说明：

- 首次定向测试暴露测试环境没有 RequestContext，`IpUtils.getIpAddr()` 无法取请求；已在测试中补最小 `HttpServletRequest` 代理，生产逻辑未改。

## 未验证原因

- 本切片不启动后端。
- 本切片不连接远程 MySQL / Redis。
- 本切片不执行 SQL。
- 本切片未做浏览器直登流程验收；浏览器层应在后续端内登录入口或审计 UI 切片中单独验证。

## 权限检查结果

- 本切片不新增接口和权限点。
- 管理端生成免密链接仍由 `seller:admin:directLogin` / `buyer:admin:directLogin` 控制。

## 字典/选项复用检查结果

- 本切片未新增字典、字段选项或前端下拉。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md` 的 `PortalDirectLoginSupport` 条目。

## 格式检查结果

- `cd E:\Urili-Ruoyi; git diff --check -- RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-portal-direct-login-support-lifecycle-test-record.md`：通过，仅有 Markdown LF/CRLF 归一化提示。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 大文件合理性判断结果

- 新增测试文件约 422 行，已超过 400 行自检阈值，因此明确判断是否拆分。
- 当前内容虽然包含 fake Redis、fake Mapper 和 request/security 上下文工具，但职责仍集中在 `PortalDirectLoginSupport` 生命周期，夹具只服务这一组测试。
- 本轮暂不拆分，避免在单一 support 测试上额外引入测试基类；后续若继续增加 direct-login 支撑测试，再优先抽出 fake fixture。

## 重复代码检查结果

- 本切片未复制生产逻辑。
- 测试中的 hash helper 只用于校验真实 `PortalDirectLoginSupport` 的输出，不替代生产实现。

## 残留问题

- 管理端审计弹窗仍需要补齐 `reason`、`usedIp`、`terminal`、`operUrl`、`operIp`、错误信息等详情展示。
- 低权限账号下 direct-login / ticket 审计入口隐藏和后端拒绝仍需单独验证。
- 端内直登入口的浏览器消费流程仍需单独验收。
