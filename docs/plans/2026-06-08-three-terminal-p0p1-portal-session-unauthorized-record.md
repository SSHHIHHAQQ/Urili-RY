# 2026-06-08 三端 P0/P1 Portal Session 401 收口记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用

- 本轮按用户最新要求使用 6 个 `gpt-5.4` 子 Agent，不再使用 GPT-5.3 Codex。
- 覆盖范围：seller/buyer 后端隔离、portal auth/direct-login/session/log、SQL guard、React 管理端路由权限、共享业务域、verifier/manifest/docs。
- 6 个子 Agent 均已完成并关闭。

## 已处理 P1

- 问题：卖家/买家端自助接口的 portal session 二次校验在端不匹配、token 缺失或账号绑定不存在时抛普通 `ServiceException`，前端不会按 401 清理当前端 token，也不会跳回对应 seller/buyer 登录页。
- 修复：`SellerServiceImpl` / `BuyerServiceImpl` 的 `assert*SessionAccount(...)` 改为用 `subjectId + accountId` 查询端内账号，缺失时统一抛 `ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED)`。
- 修复：端内修改密码中主体或账号在二次读取时消失，也按登录态失效返回 401；主体停用、账号停用、账号锁定和旧密码错误仍保留业务异常。
- 修复：`SellerPortalProductServiceImpl` / `BuyerPortalProductServiceImpl` 的失效 session 统一返回 401 code，保持商品 portal fail-closed 语义。
- 测试：`SellerServiceImplTest` / `BuyerServiceImplTest` 固定端不匹配、缺 token、账号绑定不存在时均为 401，且不会继续查询端内会话列表或执行密码重置。
- 测试：`SellerPortalProductServiceImplTest` / `BuyerPortalProductServiceImplTest` 固定商品 portal 非当前端 session 的 fail-closed 异常 code。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest,SellerPortalProductServiceImplTest,BuyerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
  - seller：63 个测试通过。
  - buyer：64 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅输出工作区 LF/CRLF 换行风格 warning，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过。

## 未执行

- 未执行远端 MySQL DDL/DML，未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图、DOM 或 UI 细调验收。

## P2 留存

- 部分账号角色/菜单只读接口缺少管理端 `@Log`，属于审计增强，不阻塞本轮 P0/P1。
- 状态切换 DTO 校验可继续加强，当前未确认会导致串端或权限失效。
- SQL guard 可继续增强递归发现、`replace into` 识别和端内菜单 ID range trigger。
- direct-login 目标窗口超时提示和 401 helper 复用可后续优化。
- 部分共享域 service 仍可补更细的语义测试。
