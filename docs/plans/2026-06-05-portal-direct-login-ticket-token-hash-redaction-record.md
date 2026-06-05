# 免密票据 tokenHash 脱敏执行记录

## 背景

当前开发方向以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为准：管理端保留控制权，卖家端/买家端账号权限独立；管理端免密代入必须短时、一次性、可审计。

前序只读检查发现 `portal_direct_login_ticket` 表只保存 `token_hash` 是正确的，但 `PortalDirectLoginTicket` Domain 有 `tokenHash` 字段，管理端 ticket 列表返回 `PortalDirectLoginTicket` 时理论上可能序列化输出 hash。虽然 hash 不是明文 token，但不应暴露给管理端列表响应。

本轮只处理一类问题：管理端免密票据审计响应不暴露 `tokenHash`。不改 UI、不改接口路径、不改 SQL、不连接远程 MySQL / Redis。

## 本次改动

- `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalDirectLoginTicket.java`
  - 为 `tokenHash` 字段增加 `@JsonIgnore`。
  - 保留 `getTokenHash()` / `setTokenHash(...)`，不影响 Mapper、Support 和一次性消费链路内部使用 hash。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalDirectLoginTicketTest.java`
  - 新增 Jackson 序列化测试。
  - 断言 `ticketId`、`terminal` 等普通字段仍输出。
  - 断言 JSON 中不包含 `tokenHash` 字段名，也不包含具体 hash 值。
- `docs/architecture/reuse-ledger.md`
  - 补充 `PortalDirectLoginTicket.tokenHash` 只允许后端内部 Mapper / Support 使用，管理端响应不得输出。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system -Dtest=PortalDirectLoginTicketTest test
```

验证结果：

- `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

模块回归：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system test
```

- `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

## 未验证原因

- 本切片不启动后端。
- 本切片不连接远程 MySQL / Redis。
- 本切片不执行 SQL。
- 本切片未做浏览器审计弹窗验收；前端类型当前也没有声明 `tokenHash`，但浏览器层后续应在审计详情切片中一并验证。

## 权限检查结果

- 本切片不新增接口和权限点。
- 卖家/买家管理端 ticket 列表仍沿用已有 `seller:admin:ticket:list` / `buyer:admin:ticket:list` 权限。

## 字典/选项复用检查结果

- 本切片未新增字典、字段选项或前端下拉。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md` 的 `PortalDirectLoginTicketMapper` 条目。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；代码变更同步时输出 `Synced 4 changed files`，文档回填后最终复跑输出 `Already up to date`。

## 大文件合理性判断结果

- `PortalDirectLoginTicket.java` 只增加一个注解，职责未扩大。
- 新增测试职责单一，仅验证免密票据序列化脱敏。

## 重复代码检查结果

- 本切片未复制业务逻辑。
- 未引入新 VO；当前用 `@JsonIgnore` 是最小改动，保留内部 Mapper / Support 对 `tokenHash` 的读写能力。

## 残留问题

- 管理端审计弹窗仍需要补齐 `reason`、`usedIp`、`terminal`、`operUrl`、`operIp`、错误信息等详情展示。
- `PortalDirectLoginSupport` 生命周期测试仍需覆盖真实 hash、Redis TTL、一次性消费、过期标记和 terminal mismatch。
- 低权限账号下 ticket 审计入口隐藏和后端拒绝仍需单独验证。
