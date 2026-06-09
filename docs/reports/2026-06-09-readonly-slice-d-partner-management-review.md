# 只读切片 D 复核报告

## 范围

- 目标文档：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`
- 复核对象：
  - `react-ui/src/pages/Seller/index.tsx`
  - `react-ui/src/pages/Buyer/index.tsx`
  - `react-ui/src/components/PartnerManagement/*`
  - `react-ui/src/services/seller/seller.ts`
  - `react-ui/src/services/buyer/buyer.ts`
  - `react-ui/src/utils/portalDirectLoginMessage.ts`
- 模式：只读
- 口径：快速推进模式，仅看 P0/P1

## 结论

本次复核范围内，未发现当前 P0/P1。

## 逐项复核

### 1. service URL

结论：未发现 P0/P1。

证据：

- Seller 页面配置全部绑定到 seller admin service：`react-ui/src/pages/Seller/index.tsx:72-113`
- Buyer 页面配置全部绑定到 buyer admin service：`react-ui/src/pages/Buyer/index.tsx:73-114`
- Seller service URL 使用 `/api/seller/admin/...`：
  - 列表 `react-ui/src/services/seller/seller.ts:3-10`
  - 账号重置密码 `react-ui/src/services/seller/seller.ts:251-262`
  - 主体会话列表 `react-ui/src/services/seller/seller.ts:271-279`
  - 账号会话列表 `react-ui/src/services/seller/seller.ts:287-295`
  - 主体免密代入 `react-ui/src/services/seller/seller.ts:297-305`
  - 账号免密代入 `react-ui/src/services/seller/seller.ts:307-318`
- Buyer service URL 使用 `/api/buyer/admin/...`：
  - 列表 `react-ui/src/services/buyer/buyer.ts:3-10`
  - 账号重置密码 `react-ui/src/services/buyer/buyer.ts:251-262`
  - 主体会话列表 `react-ui/src/services/buyer/buyer.ts:271-279`
  - 账号会话列表 `react-ui/src/services/buyer/buyer.ts:287-295`
  - 主体免密代入 `react-ui/src/services/buyer/buyer.ts:297-305`
  - 账号免密代入 `react-ui/src/services/buyer/buyer.ts:307-318`

最小修复建议：无。

### 2. 权限前缀

结论：未发现 P0/P1。

证据：

- Seller 账号权限前缀为 `seller:admin:*`：`react-ui/src/pages/Seller/index.tsx:63-71`
- Buyer 账号权限前缀为 `buyer:admin:*`：`react-ui/src/pages/Buyer/index.tsx:64-72`
- 共用模板内部权限前缀统一从 `config.moduleKey` 生成：
  - 主体页 `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:455-458` 与 `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:824-825,863-887`
  - 账号页 `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:242-257`
  - 角色页 `react-ui/src/components/PartnerManagement/PartnerRoleModal.tsx:120-128`

最小修复建议：无。

### 3. 字段配置

结论：未发现当前 P0/P1。

证据：

- Seller 字段映射独立且与 seller service 配套：`react-ui/src/pages/Seller/index.tsx:45-62`
- Buyer 字段映射独立且与 buyer service 配套：`react-ui/src/pages/Buyer/index.tsx:45-63`
- 共用模板通过 `config.idField/noField/codeField/nameField/shortNameField/typeField/levelField/accountIdField` 做字段投影，而不是写死 seller/buyer 字段：`react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:107-136,401-452,672-781`
- 账号弹窗取账号主键也走 `config.accountIdField`：`react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:151-153`

说明：

- 这里看到的是模板级字段隔离正确，没有发现 seller/buyer 串字段导致当前流程不可用的 P0/P1。

最小修复建议：无。

### 4. resetPwd

结论：未发现 P0/P1。

证据：

- 计划/规则要求“人工输入 5-20 位临时密码并调用 `resetPwd`，不得退回默认密码”：
  - `AGENTS.md:174`
- Seller 页面权限点使用 `seller:admin:account:resetPwd`：`react-ui/src/pages/Seller/index.tsx:68`
- Buyer 页面权限点使用 `buyer:admin:account:resetPwd`：`react-ui/src/pages/Buyer/index.tsx:68`
- 账号弹窗重置密码要求人工输入密码并校验 5-20 位：
  - 密码规则 `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:69-91`
  - 重置弹窗 `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:377-442`
- 实际调用的是 `.../resetPwd` 接口，提交体为 `{ password }`，未见默认密码回退：
  - Seller `react-ui/src/services/seller/seller.ts:251-262`
  - Buyer `react-ui/src/services/buyer/buyer.ts:251-262`

最小修复建议：无。

### 5. direct-login 成功回传等待

结论：未发现 P0/P1。

证据：

- 规则要求前端成功提示必须等待 portal 端通过 `PORTAL_DIRECT_LOGIN_RESULT_MESSAGE` 回传成功：
  - `AGENTS.md:170`
- bridge 常量和判定逻辑明确要求等待 `PORTAL_DIRECT_LOGIN_RESULT_MESSAGE`，不能只靠 READY：
  - 常量 `react-ui/src/utils/portalDirectLoginMessage.ts:1-4`
  - result 判定 `react-ui/src/utils/portalDirectLoginMessage.ts:80-96`
  - READY 仅触发发 token，真正 resolve 在 result success：`react-ui/src/utils/portalDirectLoginMessage.ts:146-175`
- 主体免密代入成功提示在 `await openPortalDirectLoginWindow(...)` 之后：
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:613-635`
- 账号免密代入成功提示同样在 `await openPortalDirectLoginWindow(...)` 之后：
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:551-581`

最小修复建议：无。

### 6. session list / force logout 权限拆分

结论：未发现 P0/P1。

证据：

- 规则要求会话查看走 `*:admin:session:list`，强退走 `*:admin:forceLogout`，前端“会话”入口不得继续绑定强退权限：
  - `AGENTS.md:173`
- 主体页“会话”入口和“强制踢出”入口已拆分为两个权限判断：
  - 会话 `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:881-885`
  - 强退 `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:887-891`
- 账号页“会话”入口和“强制踢出”入口也已拆分：
  - 会话 `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:655-657`
  - 强退 `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:661-663`
- 会话弹窗只调用 `.../sessions/list` 查询接口，不夹带强退动作：
  - `react-ui/src/components/PartnerManagement/PartnerSessionModal.tsx:167-205`
- 强退动作单独调用 delete session 接口：
  - 主体强退 `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:639-655`
  - 账号强退 `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:445-462`

最小修复建议：无。

### 7. 角色分配入口可见性

结论：未发现 P0/P1。

证据：

- 方案要求管理端账号“分配角色”入口必须同时具备：
  - `*:admin:role:query`
  - `*:admin:account:role:query`
  - `*:admin:account:role:edit`
  - 依据：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md:226`
- 共用账号模板显隐条件正是三者同时满足：
  - `canQueryRole` 定义 `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:253`
  - `canAssignAccountRoles` 组合判断 `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:254-256`
  - “分配角色”按钮显隐 `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:676-683`
- 角色分配弹窗读取的是账号角色回显接口并提交角色分配接口，和上述权限语义一致：
  - 加载 `react-ui/src/components/PartnerManagement/PartnerAccountRoleModal.tsx:46-66`
  - 提交 `react-ui/src/components/PartnerManagement/PartnerAccountRoleModal.tsx:77-95`

最小修复建议：无。

## 建议

当前这块在快速推进模式下可继续按“无 P0/P1”处理。

如果要补一层防回归，优先把以下两类契约测固定住：

1. `PartnerAccountModal` 的“分配角色”按钮显隐必须同时依赖三种权限。
2. `PartnerManagementPage` / `PartnerAccountModal` 的“会话”与“强制踢出”必须继续拆成 `session:list` 与 `forceLogout` 两组权限。

## 本次只读命令

```powershell
rg -n "resetPwd|direct-login|session:list|forceLogout|account:role:query|account:role:edit|role:query|成功|READY|PORTAL_DIRECT_LOGIN_RESULT_MESSAGE" docs\plans\2026-06-04-three-terminal-isolation-control-plan.md react-ui\src\services\seller react-ui\src\services\buyer react-ui\src\services\portal react-ui\src\pages\Seller react-ui\src\pages\Buyer
rg -n "session:list|forceLogout|会话.*只读权限|前端.*会话.*入口" AGENTS.md docs\plans\2026-06-04-three-terminal-isolation-control-plan.md react-ui\src\components\PartnerManagement react-ui\src\pages\Seller react-ui\src\pages\Buyer
Get-Content react-ui\src\pages\Seller\index.tsx -Encoding UTF8
Get-Content react-ui\src\pages\Buyer\index.tsx -Encoding UTF8
Get-Content react-ui\src\components\PartnerManagement\PartnerManagementPage.tsx -Encoding UTF8
Get-Content react-ui\src\components\PartnerManagement\PartnerAccountModal.tsx -Encoding UTF8
Get-Content react-ui\src\components\PartnerManagement\PartnerAccountRoleModal.tsx -Encoding UTF8
Get-Content react-ui\src\components\PartnerManagement\PartnerSessionModal.tsx -Encoding UTF8
Get-Content react-ui\src\components\PartnerManagement\PartnerRoleModal.tsx -Encoding UTF8
Get-Content react-ui\src\services\seller\seller.ts -Encoding UTF8
Get-Content react-ui\src\services\buyer\buyer.ts -Encoding UTF8
Get-Content react-ui\src\utils\portalDirectLoginMessage.ts -Encoding UTF8
```
