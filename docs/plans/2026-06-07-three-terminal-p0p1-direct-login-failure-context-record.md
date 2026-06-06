# 2026-06-07 三端 P0/P1 快速推进：免密失败上下文审计记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 本轮边界

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未修改 source-product / integration 旁支脏改。
- 本轮使用 6 个子 Agent 并行只读排查：优先尝试 GPT-5.3 Codex，平台提示额度限制后已关闭失败 Agent，并按用户要求降级 `gpt-5.4`；有效 `gpt-5.4` 子 Agent 已全部关闭。

## 新增问题

- P1：`PortalDirectLoginSupport` 在 DB ticket 已查到但 Redis payload 丢失、过期、端类型不匹配、票据/目标不匹配时，failure auditor 拿不到 ticket 上下文，seller/buyer 只能写普通失败日志，丢失 `ticketId`、acting admin、reason 和目标账号信息。
- P1：token 已消费但目标账号缺失或账号已挪主体时，失败日志此前不会回退使用 token 中的 `accountId`。
- P1：跨端 ticket 失败时，如果不先判断 ticket 端类型，可能把对方端的主体/账号数字写进当前端登录日志表。

## 已修复问题

- `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java`
  - DB ticket 已存在但 payload 校验失败时，从 ticket 恢复 `PortalDirectLoginToken` 审计上下文并调用 failure auditor。
  - 空 token、ticket 不存在、校验器为空仍按无 ticket 上下文场景处理。
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `recordSellerDirectLoginTokenFailure(...)` 先校验 token 端类型，外部端 ticket 不写 seller `subjectId/accountId`。
  - `recordSellerDirectLoginFailure(...)` 在当前端 token 且 account 实体缺失时，回退使用 token 中的 `accountId`。
  - 普通失败日志兜底收窄到真正无 ticket 上下文的场景，避免 direct-login 失败日志重复落库。
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - 按卖家模板机械复制。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java`
  - 增加 payload 缺失和 ticket 端类型不匹配时 failure auditor 能拿到 ticket 上下文的断言。
- `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java`
  - 增加 ticket 上下文已知但 consume 失败时写 direct-login 失败日志的断言。
  - 增加外部端 ticket 不写 seller 主体/账号列的断言。
  - 缺账号和账号挪主体用例改为断言 `accountId` 回退自 token。
- `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`
  - 按卖家模板机械复制。
- `docs/architecture/reuse-ledger.md`
  - 补充免密失败上下文审计模板。

## 残留问题

- P1：高影响 SQL guard 仍是手工枚举，缺少自动发现 20260606/20260607 高影响 SQL 的覆盖。
- P1：管理端卖家/买家页缺静态路由兜底，直达 `/seller`、`/buyer` 或刷新存在 404 风险。
- P1：`portal_direct_login:<tokenHash>` Redis key 仍未在 key 层编码 `seller/buyer` 端类型；运行时有 DB/payload terminal 校验，但观测和按端清理不够清楚。
- P1：部门树跨主体写入/删除、角色菜单 `checkedKeys` 主体隔离、owner 角色禁停用/禁删除仍需补运行时隔离契约测试。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=PortalDirectLoginSupportTest test`：通过，`13` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`48` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`48` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=PortalDirectLoginAuthContractTest test`：通过，`4` 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。

## 未验证原因

- 未做浏览器、截图、DOM 或 UI 细调验收：用户已明确本阶段无需浏览器运行态验收。
- 未执行远程 SQL：本轮没有新增 DDL/DML，也没有修改远程库必要。
- 未启动后端：本轮已用 service 测试和静态契约覆盖修改点。

## 权限检查结果

- 本轮未新增后端接口或权限标识。
- 修改点在免密 token 消费失败审计链路；现有 seller/buyer direct-login 接口权限边界不变。

## 字典/选项复用检查结果

- 本轮未新增业务字典、选项或前端下拉。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，记录免密失败上下文审计模板。

## CodeGraph 更新结果

- `codegraph sync .` 已执行，结果为 `Already up to date`。

## 大文件合理性判断结果

- `SellerServiceImpl` / `BuyerServiceImpl` 是既有同构 service，本轮只收窄免密失败日志分支并补测试，不拆分。
- 新增记录文件只承载阶段审计，不参与运行时。

## 重复代码检查结果

- seller 先实现，buyer 按同构模板机械复制。
- direct-login ticket 到审计上下文的恢复逻辑集中在 `PortalDirectLoginSupport`，没有在 seller/buyer 各自重复实现。
