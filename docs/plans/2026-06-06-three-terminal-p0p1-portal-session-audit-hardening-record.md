# 2026-06-06 三端 P0/P1 快速推进：Portal 会话、审计与前端请求收口记录

本记录以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本轮不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 已完成

- seller/buyer 管理端会话列表服务层增加主体存在和账号归属前置校验，避免只凭路径上的 `sellerId` / `buyerId` / `accountId` 直接查会话。
- seller/buyer portal 免密登录后端接口收口为 `POST /direct-login` 且只从 request body 读取 `directLoginToken`，不再提供 GET/query token 入口。
- seller/buyer 免密登录在票据或 token 解析失败时补写失败登录日志；票据解析后已有主体/账号上下文的失败路径继续写原有明细日志，避免重复。
- `PortalLogAspect` 在端内会话缺失时不再静默跳过，改为写入失败操作日志，记录 `端内会话不存在`。
- 新增 `PortalAnonymousEndpointContractTest`，固定端内 `@Anonymous` 接口必须同时声明对应 terminal 的 `@PortalPreAuthorize`。
- 新增 `PortalLogAspectContractTest`，固定端内操作日志不能因会话缺失静默 return。
- `verify-three-terminal.mjs` 补入 `PortalDirectLoginAuthContractTest`、`PortalAnonymousEndpointContractTest`、`PortalLogAspectContractTest`、`PortalOperLogServiceImplTest`、`PortalAccountTest`。
- React 全局请求拦截器删除调试用 `?token=123` 拼接。
- React 代理配置改为支持 `API_PROXY_TARGET`，默认值为当前验证后端 `http://127.0.0.1:8080`。
- `check-portal-token-isolation.mjs` 增加 request debug token 和 proxy hardcode guard。

## 子 Agent 结果

- 本轮继续使用 6 个子 Agent 并已全部关闭。
- 已采纳 P0/P1：管理端会话查询缺服务层归属校验；关键契约测试未进入三端 verify；端内匿名放行需要契约防漏；会话缺失审计静默跳过；前端全局请求拼接 `?token=123`；代理硬编码 `localhost:8080`。
- 未采纳为本轮阻塞项：
  - buyer 商品列表是否应按 buyer 主体限定可见范围：当前业务规则未确认，记录为待确认边界，不擅自新增 buyer 可见关系表或过滤规则。
  - portal 路由统一 wrapper：当前 token key、portal request 和页面内跳转已有 guard 覆盖，记录为后续结构优化，不阻塞 P0/P1。
  - buyer 账号 lock 权限缺失：主线程复核后发现当前前端和 SQL seed 均已有 `buyer:admin:account:lock`。
  - SQL 子 Agent 因上下文耗尽失败，无有效结论；主线程已用最小搜索补位。

## 验证结果

- `mvn -pl ruoyi-system,ruoyi-framework,seller,buyer -am "-Dtest=PortalDirectLoginAuthContractTest,PortalAnonymousEndpointContractTest,PortalLogAspectContractTest,SellerServiceImplTest,BuyerServiceImplTest,PortalOperLogServiceImplTest,PortalAccountTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `npm run guard:portal-token`：通过，输出 `Portal token isolation guard passed.`。
- `npm run verify:three-terminal`：通过，最终输出 `three-terminal verification passed.`。
- `git diff --check`：通过；只有 LF/CRLF 工作区换行提示，无空白错误。
- `codegraph sync .`：通过，输出 `Synced 16 changed files`，`Added: 3, Modified: 13 - 759 nodes`。

## 数据库与运行边界

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 残留记录

- buyer portal 商品可见范围需要业务确认：如果买家端只能看分配给自己的商品，需要先设计 buyer 商品可见关系或价格/授权模型；如果买家端允许公开查看全部在售商品，需要写入架构文档作为明确规则。
- portal 路由 wrapper 可作为后续 P2 结构治理项，在三前端物理拆分前统一端入口守卫。
