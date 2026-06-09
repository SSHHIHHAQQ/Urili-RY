# 2026-06-08 seller/buyer 管理入口只读扫描

## 范围

- `react-ui/config/routes.ts`
- `react-ui/config/routes.js`
- `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx`
- `react-ui/src/services/session.ts`
- `react-ui/src/access.ts`
- `react-ui/src/pages/Seller/index.tsx`
- `react-ui/src/pages/Buyer/index.tsx`
- `react-ui/src/components/PartnerManagement/*`
- `react-ui/src/services/seller/seller.ts`
- `react-ui/src/services/buyer/buyer.ts`
- `react-ui/tests/*` 中与 route / authority / permission / session 相关的契约测试

## 扫描目标

只看 P0/P1：

- 编译
- guard
- 接口
- 权限
- 串端
- service / 字段缺失
- `current -> pageNum`

不看：

- 浏览器
- 截图
- DOM / UI 细调

## 结论

本次范围内 **未发现 seller/buyer 管理入口 route / service / permission 的 P0/P1 问题**。

已核对的关键点：

- `/seller`、`/buyer` 静态入口同时具备 `authority` 和 `RemoteMenuRouteGuard`
- 空 `authority` 在 `RemoteMenuRouteGuard` 中 fail-closed，返回 403
- seller / buyer service URL 未发现串端
- `.ts` / `.js` 镜像在本次范围内均为纯 re-export，未发现漂移
- `PartnerManagementPage` 与 `PartnerAuditModal` 的 ProTable 请求都已把 `current` 转成 `pageNum`
- “分配角色”按钮已同时覆盖 `*:admin:role:query`、`*:admin:account:role:query`、`*:admin:account:role:edit`
- `tsc --noEmit` 通过
- 指定 `--config jest.config.ts` 后，目标 guard / authority / session 契约测试通过

## P0 / P1

无。

## P2

### P2-1 Jest 直接执行会因双配置文件歧义失败

- 文件：
  - `react-ui/jest.config.js:1`
  - `react-ui/jest.config.ts:1`
- 现象：
  - 直接执行 `npx jest ...` 时，Jest 会先报 `Multiple configurations found`，要求显式 `--config`
- 影响：
  - 不影响当前业务代码，也不影响 `npm test` / `verify-three-terminal` 这类仓库脚本
  - 但会增加针对 seller/buyer 管理入口做临时定向测试时的摩擦
- 最小修复建议：
  - 保留单一 Jest 配置文件；或者把其中一个改成明确注释的唯一入口，避免默认解析歧义

## 关键证据

### 1. 静态 seller / buyer 入口已显式加 authority + wrapper

- `react-ui/config/routes.ts:48-58`

### 2. 空 authority fail-closed

- `react-ui/src/services/session.ts:121-133`
- `react-ui/tests/remote-menu-route-guard.test.ts:150-165`

### 3. 静态 fallback authority 正确映射 seller / buyer

- `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx:12-18`
- `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx:102-110`
- `react-ui/tests/remote-menu-route-guard.test.ts:167-209`

### 4. seller / buyer service 未发现串端

- seller:
  - `react-ui/src/services/seller/seller.ts:3-339`
- buyer:
  - `react-ui/src/services/buyer/buyer.ts:3-339`

### 5. account role 按钮权限覆盖后端查询/编辑接口

- `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:245-256`

### 6. ProTable 分页参数已做 `current -> pageNum`

- `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:261-289`
- `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:1008-1016`
- `react-ui/src/components/PartnerManagement/PartnerAuditModal.tsx:135-145`
- `react-ui/src/components/PartnerManagement/PartnerAuditModal.tsx:424-425`

### 7. `.js` 镜像未漂移

- `react-ui/config/routes.js:1`
- `react-ui/src/access.js:1-2`
- `react-ui/src/pages/Seller/index.js:1`
- `react-ui/src/pages/Buyer/index.js:1`
- `react-ui/src/services/seller/seller.js:1`
- `react-ui/src/services/buyer/buyer.js:1`
- `react-ui/tests/admin-auth-sidecar-contract.test.ts:15-17`
- `react-ui/tests/admin-auth-sidecar-contract.test.ts:55-57`

## 本次验证命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc
npx jest --config jest.config.ts tests/remote-menu-route-guard.test.ts tests/static-route-authority-contract.test.ts tests/permission-contract.test.ts tests/portal-session-request.test.ts tests/getrouters-authority-contract.test.ts --runInBand
```

## 验证结果

- `npm run tsc`: 通过
- 指定 `--config jest.config.ts` 的 5 组目标测试：`47 passed`
