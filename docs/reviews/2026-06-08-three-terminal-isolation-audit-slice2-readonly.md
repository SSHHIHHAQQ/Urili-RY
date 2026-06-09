# 2026-06-08 三端隔离 P0/P1 审计切片 2（只读）

## 范围

- 后端
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
- 前端
  - `react-ui/src/pages/Portal/DirectLogin/index.tsx`
  - `react-ui/src/services/portal/session.ts`
  - `react-ui/src/requestErrorConfig.ts`
  - `react-ui/src/app.tsx`
  - `react-ui/src/utils/portalDirectLoginMessage.ts`
  - `react-ui/src/utils/portalRequest.ts`

## 结论

- `P0`：未发现坐实问题。
- `P1`：未发现坐实问题。
- `P2`：发现 1 个可记录的可观测性问题，不影响当前三端隔离边界，但会降低 direct-login 失败排障效率。

## P0

无。

已核实通过的高风险点：

- 免密票据 terminal 串端会在消费前直接拒绝，且不会消费票据：
  - `PortalDirectLoginSupport.java:164-168`
  - `PortalDirectLoginSupport.java:179-190`
  - `PortalDirectLoginSupportTest.java:321-343`
- seller/buyer 端 direct-login 失败审计对 foreign terminal 做匿名失败兜底，不把外端 `ticketId/actingAdmin/reason/accountId` 写入当前端：
  - `SellerServiceImpl.java:733-741`
  - `BuyerServiceImpl.java:733-741`
- 旧 Redis key `portal_direct_login:{token_hash}` 不参与读取回退，只在清理历史残留时删除：
  - `PortalDirectLoginSupport.java:242-244`
  - `PortalDirectLoginSupport.java:350-353`
  - `PortalDirectLoginSupportTest.java:174-190`

## P1

无。

已核实通过的高风险点：

- direct-login 成功提示已经等待 portal 端实际消费确认，不是拿到 popup `READY` 就提示成功：
  - `portalDirectLoginMessage.ts:146-175`
  - `PartnerManagementPage.tsx:617-623`
  - `PartnerAccountModal.tsx:564-569`
  - `portal-direct-login-message.test.ts:30-113`
- portal 401 会先清当前端 token，再跳对应 portal 登录页，并中断原请求，不会继续被业务层当成功结果：
  - `requestErrorConfig.ts:33-42`
  - `requestErrorConfig.ts:89-92`
  - `requestErrorConfig.ts:119-121`
  - `app.tsx:73-82`
  - `app.tsx:352-355`
  - `portal-unauthorized-redirect.test.ts:89-145`
- token 清理按 terminal 隔离，不会因为 seller/buyer 401 误清 admin token，也不会因为 admin 401 误清 portal token：
  - `portalRequest.ts:29-45`
  - `access.ts:87-93`
  - `portal-unauthorized-redirect.test.ts:147-220`
- 强退/后台重置密码审计会把 `actingAdminId/actingAdminName` 覆盖成当前执行控制动作的管理端账号，不会误用旧 direct-login 签发人：
  - `SellerServiceImpl.java:958-997`
  - `BuyerServiceImpl.java:958-997`

## P2

### 1. Portal direct-login 请求级失败被统一折叠成通用文案，后台端拿不到真实失败原因

- 证据：
  - `react-ui/src/pages/Portal/DirectLogin/index.tsx:89-94`
  - `react-ui/src/pages/Portal/DirectLogin/index.tsx:64-72`
  - `react-ui/src/utils/portalDirectLoginMessage.ts:154-163`
- 现象：
  - portal 端 `directLogin(...)` 抛出异常后，`catch` 分支固定回传 `Direct login failed`。
  - 这样管理端弹窗侧虽然确实在等待消费确认，但收到的失败信息会丢失后端原始原因，例如票据过期、目标账号停用、目标账号锁定。
- 影响：
  - 不会破坏 terminal 隔离，也不会导致错误成功提示。
  - 但排查现场时只能看到通用失败提示，审计链路与用户提示之间缺少精确失败上下文。
- 建议：
  - 在 `catch` 中优先透传已规范化的后端错误消息，例如 `error.info.errorMessage`、`error.response?.data?.msg`，取不到时再退回通用文案。
  - 保留现有 terminal 校验和 `persistPortalLogin(...)` fail-closed 逻辑，不要为了透传文案放宽跨端校验。

## 备注

- `react-ui/src/**/*.js` sidecar 当前均为 `export * from './xxx.ts(x)'` 或 `export { default } from './index.tsx'` 的镜像导出，本轮范围内未发现 ts/js 双轨逻辑漂移。
- 本次为只读扫描；未修改业务代码，新增产物仅为本审计 Markdown。
