# 2026-06-09 portal auth/session/direct-login/401/self-service DTO 只读 P0/P1 审查

## 范围

- 前端
  - `react-ui/src/services/portal/session.ts`
  - `react-ui/src/requestErrorConfig.ts`
  - `react-ui/src/app.tsx`
- 后端
  - `RuoYi-Vue/seller/.../SellerPortalAuthController.java`
  - `RuoYi-Vue/buyer/.../BuyerPortalAuthController.java`
  - `RuoYi-Vue/seller/.../SellerPortalController.java`
  - `RuoYi-Vue/buyer/.../BuyerPortalController.java`
  - `RuoYi-Vue/seller/.../SellerServiceImpl.java`
  - `RuoYi-Vue/buyer/.../BuyerServiceImpl.java`
- 合同/测试
  - `react-ui/tests/portal-unauthorized-redirect.test.ts`
  - `react-ui/tests/portal-session-request.test.ts`
  - `react-ui/tests/portal-direct-login-message.test.ts`
  - `react-ui/tests/terminal-session-token.test.ts`
  - `RuoYi-Vue/ruoyi-system/.../PortalSelfServiceSurfaceContractTest.java`
  - `RuoYi-Vue/ruoyi-system/.../PortalDirectLoginAuthContractTest.java`
  - `RuoYi-Vue/ruoyi-system/.../PortalAnonymousEndpointContractTest.java`

## 结论

- **P0/P1：未发现当前可坐实问题。**
- **P2：1 项。**

本次结论基于静态审查；未实际执行 Jest/Maven 命令。

## P0/P1

### 未发现当前开放 P0/P1

已核对的关键边界：

1. **portal 401 按 terminal 分流，direct-login 401 不会误清已有 portal token**
   - `react-ui/src/requestErrorConfig.ts:33-45`
   - `react-ui/src/app.tsx:73-85`
   - `react-ui/src/app.tsx:353-360`
   - `react-ui/tests/portal-unauthorized-redirect.test.ts:89-179`

2. **portal request 只使用选定 terminal token，不回退 admin token，并剥离调用方可控 scope 参数**
   - `react-ui/src/services/portal/session.ts:28-31`
   - `react-ui/src/services/portal/session.ts:33-54`
   - `react-ui/src/services/portal/session.ts:142-173`
   - `react-ui/tests/portal-session-request.test.ts:265-345`

3. **seller/buyer direct-login 只从 POST body 取 `directLoginToken`**
   - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalAuthController.java:33-44`
   - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalAuthController.java:33-44`
   - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalDirectLoginAuthContractTest.java:16-32`

4. **portal controller 不接受前端传入 sellerId/buyerId/accountId 等身份范围，统一从 session 推导**
   - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java:58-194`
   - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java:58-194`
   - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalAnonymousEndpointContractTest.java:128-183`

5. **自助日志/会话接口返回 `PortalOwn*Profile`，未直接泄露内部审计字段**
   - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:331-460`
   - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:331-460`
   - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalOwnLoginLogProfile.java:10-109`
   - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalOwnOperLogProfile.java:10-146`
   - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalOwnSessionProfile.java:10-100`
   - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalSelfServiceSurfaceContractTest.java:54-214`

6. **跨端 direct-login 失败时不会把外端 token 的 accountId 回填到当前端日志**
   - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:1117-1125`
   - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:1117-1125`

## P2

### 1. admin 401 清理逻辑双处维护，且远程菜单缓存清理口径已出现轻微漂移

- 文件：
  - `react-ui/src/app.tsx:43-46`
  - `react-ui/src/app.tsx:73-85`
  - `react-ui/src/requestErrorConfig.ts:33-45`
- 现象：
  - `app.tsx` 的 admin 未授权清理走 `clearAdminSession()`，会同时执行 `clearSessionToken()` 和 `setRemoteMenu(null)`。
  - `requestErrorConfig.ts` 的 `handleUnauthorized()` 在 admin 401 分支只执行 `clearSessionToken()`，不清 `remoteMenu`。
- 风险判断：
  - **当前不构成 P0/P1**。portal/admin 401 分流和 token 清理边界仍然正确，没有看到 seller/buyer token 串到 admin，或 admin 401 误清 seller/buyer token。
  - 但这是实际的行为漂移点。后续如果有人只改其中一处，可能把 admin 401 后的菜单缓存、跳转或 direct-login 例外逻辑改裂，先表现为 UI 残留/状态不一致，再升级成回归缺陷。
- 建议：
  - 后续把 admin/portal 401 清理和跳转抽到一个共享 helper，至少统一 `remoteMenu` 清理口径。

## 建议验证命令

### 前端

```powershell
cd E:\Urili-Ruoyi\react-ui
npx jest --config jest.config.ts --runInBand tests/portal-unauthorized-redirect.test.ts tests/portal-session-request.test.ts tests/portal-direct-login-message.test.ts tests/terminal-session-token.test.ts
```

### 后端

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalSelfServiceSurfaceContractTest,PortalDirectLoginAuthContractTest,PortalAnonymousEndpointContractTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

### 定位命令

```powershell
rg -n "handleUnauthorized|clearTerminalSessionToken|clearSessionToken|setRemoteMenu|direct-login|PortalOwn|PortalSessionContext.requireSession" E:\Urili-Ruoyi\react-ui E:\Urili-Ruoyi\RuoYi-Vue
```
