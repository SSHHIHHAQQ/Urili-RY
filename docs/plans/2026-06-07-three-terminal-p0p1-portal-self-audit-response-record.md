# 2026-06-07 三端 P0/P1 Portal 自助日志响应脱敏记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按三端独立快速推进模式只修 P0/P1。本轮聚焦 seller/buyer 端内自助日志接口，修复自助登录日志、操作日志直接返回管理端免密代入审计字段的问题。

## 新增问题

- P1：`/seller/account/login-logs`、`/buyer/account/login-logs` 直接返回 `PortalLoginLog`，会把 `directLoginTicketId`、`actingAdminId`、`actingAdminName`、`directLoginReason` 等管理端免密审计字段回显给端内账号。
- P1：`/seller/account/oper-logs`、`/buyer/account/oper-logs` 直接返回 `PortalOperLog`，除结构化免密审计字段外，还会回显 `operParam` / `jsonResult` 等内部审计载荷。
- P1：免密登录成功日志原先把 `ticketId`、acting admin 和 reason 拼入 `msg`，即使结构化字段脱敏也可能通过文本泄漏。

## 已修复问题

- 新增 `PortalOwnLoginLogProfile` / `PortalOwnOperLogProfile`，作为卖家端、买家端自助日志响应 DTO。
- `SellerPortalController` / `BuyerPortalController` 将自助登录日志和操作日志返回值从内部日志模型映射为端内可见 DTO；分页 total 仍从原 PageHelper 列表计算，只替换 `rows`。
- 自助登录日志 direct-login 文案统一为“免密登录成功/免密登录失败”，不再回显 ticket、acting admin 或 reason。
- `SellerServiceImpl` / `BuyerServiceImpl` 的 direct-login 成功日志 `msg` 改为中性文案“免密登录成功”，结构化审计字段仍通过 `PortalTokenSupport.buildDirectLoginLog(...)` 写入日志，管理端审计不受影响。
- `react-ui/src/services/portal/session.ts` 和 `react-ui/src/types/seller-buyer/party.d.ts` 同步拆分自助日志响应类型。
- `AGENTS.md` 和 `docs/architecture/reuse-ledger.md` 已同步新规则。

## 残留问题

- 本轮不改管理端审计列表，管理端仍可读取完整 direct-login 审计字段。
- 本轮不执行远程 SQL，不改日志表结构，不改 `PortalLogAspect` / `PortalPreAuthorizeAspect` 写入审计前缀。
- bootstrap-only 初始化 SQL 静态合同、旧库存维度同步 SQL 菜单 owner 收敛仍为后续 P1。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=PortalSelfAuditSerializationTest,PortalHomeProfileSerializationTest,PortalAnonymousEndpointContractTest" test`：通过，`8` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`49` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`49` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/portal-session-request.test.ts --runInBand`：通过，`1` 个 suite / `3` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，前端 `4` 个 suite / `18` 个测试通过；后端 ruoyi-system `122`、ruoyi-framework `15`、product `1`、seller `85`、buyer `86` 个测试通过，最终输出 `three-terminal verification passed.`。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。

## 未验证原因

- 未连接远程 MySQL / Redis，未执行 SQL，未写远程数据：本轮只改响应 DTO、controller 映射、service 文案和测试。
- 未做浏览器运行态验收、截图、DOM 检测：用户已明确当前快速推进模式无需浏览器验证。

## 权限检查结果

- 自助日志接口仍要求 `seller:account:loginLog:list` / `seller:account:operLog:list` / `buyer:account:loginLog:list` / `buyer:account:operLog:list`。
- 端内数据范围仍由 `PortalSessionContext.requireSession(...)` 推导，不相信前端传入主体 ID 或账号 ID。
- `PortalAnonymousEndpointContractTest` 已新增合同，固定 self-audit handler 必须映射端内可见 DTO，不能直接返回内部日志模型。

## 字典/选项复用检查结果

- 本轮未新增字典、选项或前端枚举。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记 `PortalOwnLoginLogProfile` / `PortalOwnOperLogProfile`、自助日志 DTO 投影、分页 total 保留和 direct-login 文案脱敏规则。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 14 changed files`，`Added: 3, Modified: 11 - 1,062 nodes`。

## 大文件合理性判断结果

- `SellerPortalController` / `BuyerPortalController` 是端内自助接口入口，本轮只追加同类 profile 映射 helper，不拆分。
- `SellerServiceImpl` / `BuyerServiceImpl` 是既有端内账号、日志、会话 service，本轮只收紧 direct-login 成功日志文本，不拆分。
- `PortalAnonymousEndpointContractTest` 职责仍集中在 portal 匿名/自助接口边界合同，不拆分。

## 重复代码检查结果

- seller/buyer 双端保持同构实现，符合当前“卖家模板通过后机械复制买家”的推进方式。
- 新增 DTO 拆在 `ruoyi-system` 共享域，避免 seller/buyer 各自复制响应类。

## 子 Agent 使用记录

- 本轮按当前目标直接使用 6 个 `gpt-5.4` 只读子 Agent。
- 子 Agent 结论：portal 自助登录日志/操作日志回显 acting admin 审计字段为 P1；写入链路完整，问题在读取面；前端只做后端响应脱敏不会形成 P0/P1 编译风险；测试主落点应是共享 DTO 序列化和轻量 architecture contract。
- SQL 子 Agent 建议后续优先处理 bootstrap-only 初始化 SQL 静态合同；旧库存维度同步 SQL 重复 owner 留作后一片。

## 一句话总结

本轮把 seller/buyer 自助日志接口从“直接返回内部审计模型”收口为“返回端内可见日志 DTO”，管理端审计字段继续写入并保留给管理端，端内用户不再看到 acting admin、ticket、reason 或原始审计载荷。
