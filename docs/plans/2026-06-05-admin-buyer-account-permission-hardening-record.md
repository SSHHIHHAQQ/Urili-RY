# 管理端买家账号权限粒度硬化执行记录

## 背景

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，接在卖家账号权限粒度硬化和远程库执行之后，按同构规则只复制买家账号域权限。

本轮只处理买家管理端账号维护权限粒度，不新增表，不改变免密代入、强制踢出或主体主账号重置密码权限模型。

## 已完成

- `AdminBuyerController` 中买家端账号维护接口切换为独立账号域权限：
  - `GET /buyer/admin/buyers/{buyerId}/accounts`：`buyer:admin:account:list`
  - `POST /buyer/admin/buyers/{buyerId}/accounts`：`buyer:admin:account:add`
  - `PUT /buyer/admin/buyers/{buyerId}/accounts`：`buyer:admin:account:edit`
  - `PUT /buyer/admin/buyers/accounts/resetPwd`：`buyer:admin:account:resetPwd`
  - `PUT /buyer/admin/buyers/accounts/resetDefaultPwd`：`buyer:admin:account:resetPwd`
  - `GET /buyer/admin/buyers/{buyerId}/accounts/{accountId}/roles`：`buyer:admin:account:role:query`
  - `PUT /buyer/admin/buyers/{buyerId}/accounts/{accountId}/roles`：`buyer:admin:account:role:edit`
- 买家主体详情保留 `buyer:admin:query`。
- `seller_buyer_management_seed.sql` 补入 6 个买家管理端账号域 `sys_menu` 权限，ID 使用 `2316-2321`。
- `Buyer/index.tsx` 配置 `PartnerModuleConfig.accountPermissions`，启用 `buyer:admin:account:*`。
- `BuyerAdminPermissionContractTest` 增加账号域权限契约，固定主体详情与账号列表不能错位。
- 新增增量 SQL：`RuoYi-Vue/sql/20260605_admin_buyer_account_permission_seed.sql`。
- 远程运行库已执行该增量 SQL，记录见 `docs/plans/2026-06-05-admin-buyer-account-permission-db-execution-record.md`。

## 验证结果

- `mvn -pl ruoyi-system "-Dtest=BuyerAdminPermissionContractTest,AdminDirectLoginPermissionContractTest" test`：通过，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。
- `mvn -pl ruoyi-system "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminDirectLoginPermissionContractTest" test`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `mvn -pl ruoyi-system test`：通过，`Tests run: 36, Failures: 0, Errors: 0, Skipped: 0`。
- `mvn -pl buyer -am -DskipTests compile`：通过。
- `npm run tsc`：通过。
- `npx biome lint src\pages\Buyer\index.tsx`：通过。
- `git diff --check -- ...` 本轮相关文件：通过，仅有 LF/CRLF 工作区换行提示。
- 远程库执行后核验 6 个买家账号域权限均已写入，父菜单为 `2012`，状态为 `0`。

## 权限检查结果

- 买家端账号维护接口不再复用主体新增、主体编辑、主体查询或端内角色维护权限。
- 买家主体详情仍使用 `buyer:admin:query`。
- 主体级免密和账号级免密仍使用 `buyer:admin:directLogin`。
- 主体级会话和账号级会话、强制踢出仍使用 `buyer:admin:forceLogout`。
- 主账号重置密码仍使用 `buyer:admin:resetPwd`。

## 字典/选项复用检查结果

- 本切片未新增字典或业务选项。
- 账号角色字典仍复用 `buyer_account_role`。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记买家账号域权限已按卖家模板复制完成。

## CodeGraph 更新结果

- `codegraph sync .`：通过，输出 `Already up to date`。

## 大文件合理性判断结果

- `PartnerManagementPage.tsx` 和 `PartnerAccountModal.tsx` 未在本轮继续扩大。
- `BuyerAdminPermissionContractTest.java` 仍是买家管理端权限契约扫描，新增断言属于同一职责。
- `seller_buyer_management_seed.sql` 已超过 500 行，但职责仍是卖家/买家主体和端内基础控制面综合初始化；本轮只补同一初始化边界内的管理端买家账号按钮权限，暂不拆分。

## 残留问题

- 低权限账号运行时负向验证尚未针对账号域权限单独执行。
- 当前完成的是源码契约、编译、前端类型和远程库菜单层验证；浏览器按钮显隐和接口 403 仍可作为后续验收切片补充。
