# 2026-06-09 三端 P0/P1 Portal Endpoint 合同加固记录

## 参考方向

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向推进。

执行口径：

- 只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。
- 不做浏览器运行态验收、截图、DOM 检测或 UI 细调。
- 子 Agent 固定使用 `gpt-5.4`，不再使用 GPT-5.3 Codex 作为首选。
- P2 只记录，不阻塞本轮收口。

## 本轮目标

收敛 seller/buyer 端 portal endpoint 的账号权限边界，避免未来新增端内接口时绕过三端隔离合同。

完成标准：

- seller/buyer 非 auth portal controller 的 handler method 必须声明端内权限、端内日志和 portal token filter 边界。
- portal endpoint 不得从前端参数读取 `sellerId` / `buyerId` / `subjectId` / `accountId` 等身份范围字段。
- portal endpoint 不得读取若依管理端登录上下文。
- 最小后端合同测试、前端窄测和三端总闸门通过。

## 子 Agent 执行情况

本轮使用并关闭 6 个 `gpt-5.4` 子 Agent：

- seller/buyer 后端账号、角色、菜单、部门、日志、会话隔离扫描。
- portal auth/session/direct-login/log/session 扫描。
- React route/request/service/proxy/sidecar guard 扫描。
- SQL/seed guard 扫描。
- product/inventory/integration/warehouse/finance 共享域边界扫描。
- verify gate / manifest / sidecar 覆盖扫描。

子 Agent 结论：

- 未发现新的可坐实 P0/P1。
- P2 风险已记录到本文件，不阻塞本轮。

## 已完成的 P1 加固

修改文件：

- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalAnonymousEndpointContractTest.java`

具体变化：

- 增加 `isPortalController(...)` 判断，对 `SellerPortal*Controller` / `BuyerPortal*Controller` 的非 auth handler 统一执行 portal endpoint 合同。
- 非 auth portal handler 必须同时声明：
  - `@Anonymous`
  - `@PortalPreAuthorize(terminal = "seller" / "buyer")`
  - `@PortalLog(terminal = "seller" / "buyer")`
- auth handler 继续禁止 `@Anonymous` 和 `@PortalPreAuthorize`，但必须使用 `@PortalLog` 且 `allowAnonymous = true`、`isSaveResponseData = false`。
- auth handler 和非 auth portal handler 都禁止读取前端传入的身份范围字段：
  - `sellerId`
  - `buyerId`
  - `subjectId`
  - `accountId`
  - `sellerAccountId`
  - `buyerAccountId`
- auth handler 和非 auth portal handler 都禁止读取若依管理端登录上下文：
  - `SecurityUtils.getLoginUser(...)`
  - `SecurityUtils.getUserId(...)`
  - `SecurityUtils.getUsername(...)`
  - 未限定调用的 `getLoginUser(...)` / `getUserId(...)` / `getUsername(...)`
  - `LoginUser`
  - `SysUser`

修复原因：

- 旧合同会检查已经带 `@Anonymous` 的 portal handler，但未来如果新增非 auth portal controller 方法时漏掉 `@Anonymous`，该方法有机会绕过深度 portal 合同检查。
- 本轮把检查入口前移到 controller 文件名和 handler method 层，避免漏注解接口静默通过。

## 验证结果

已通过：

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=PortalAnonymousEndpointContractTest" test`
  - 1 个测试，通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests/portal-unauthorized-redirect.test.ts tests/remote-menu-route-guard.test.ts tests/getrouters-authority-contract.test.ts tests/admin-auth-sidecar-contract.test.ts tests/terminal-session-token.test.ts --runInBand`
  - 5 个 suite，通过。
  - 63 个测试，通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs`
  - portal token guard、partner management guard、seller/buyer portal product guard、product upstream mirrors guard 通过。
  - React typecheck 通过。
  - 前端三端 Jest：22 个 suite，通过；161 个测试，通过。
  - 后端 reactor test-compile 通过。
  - 后端三端合同测试通过。
  - 总闸门输出：`three-terminal verification passed`。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过，退出码 0。
  - 仅输出当前工作区 LF/CRLF 换行提示，无空白错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过，输出 `Already up to date`。

## P2 记录

本轮不处理以下 P2：

- `react-ui` 的 targeted Jest 入口仍有摩擦：`npm test` / `npm run jest` 走总闸门，直接切片调试需要显式 `npx jest --config jest.config.ts ...`。
- `app.tsx` 与 `requestErrorConfig.ts` 都维护 portal/admin 401 分流，当前行为一致，但后续存在漂移风险。
- `20260608_product_center_menu_seed.sql` 的执行后完成态校验字段偏窄，后续可补齐 `order_num` / `visible` / `status` / `icon` / `remark`。
- `top_menu_seed.sql` 为兼容历史 `108` 放了 `log-center` 和旧 `log` 两条 guard 签名，可读性较弱。
- `assert_terminal_menu_range_ready` 在多份 terminal permission seed 中重复，后续规则再收紧时有同步漂移风险。
- integration 域 SKU 配对有领域审计事件，仓库配对和物流渠道配对目前主要依赖业务写入与 controller `@Log`，后续可补同等级领域审计快照。
- 来源商品/来源仓库库存读模型重建仍是事务内 `delete + rebuild`，当前单库事务下不构成 P0/P1，但大批量或跨库扩展时建议改为 staging/swap。
- `verify-three-terminal` 的后端关键测试识别仍是规则加显式清单混合，未来 `ruoyi-framework` 新增中性命名关键合同测试时仍需维护 manifest。
- 前端 JS/TS sidecar 覆盖是按三端关键面定点测试，不是仓库级全量镜像审计。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮只改了一处后端合同测试和 Markdown 记录。
- CodeGraph 已同步。
