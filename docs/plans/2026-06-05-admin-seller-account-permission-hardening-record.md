# 管理端卖家账号权限粒度硬化执行记录

## 背景

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。

只读核对确认：卖家管理端端账号列表、新增、编辑、账号角色绑定和账号密码重置此前复用了主体管理权限或端内角色管理权限，例如 `seller:admin:query`、`seller:admin:add`、`seller:admin:edit`、`seller:admin:role:*`。这会导致“能改卖家主体”或“能维护端内角色”的管理员同时获得端账号维护能力，权限边界过粗。

## 本轮范围

- 只处理卖家管理端账号维护权限粒度。
- 不复制买家。
- 不新增表、不改表结构。
- 不执行远程 MySQL / Redis DDL 或 DML。
- 不改变免密代入和强制踢出权限模型。
- 不改变主体级新增、编辑、详情、主账号重置密码权限模型。

## 已完成

- 使用 3 个只读子 agent 并行核对后端权限、前端共享组件、seed/test 契约；子 agent 均已关闭。
- `AdminSellerController` 中卖家端账号维护接口切换为独立账号域权限：
  - `GET /seller/admin/sellers/{sellerId}/accounts`：`seller:admin:account:list`
  - `POST /seller/admin/sellers/{sellerId}/accounts`：`seller:admin:account:add`
  - `PUT /seller/admin/sellers/{sellerId}/accounts`：`seller:admin:account:edit`
  - `PUT /seller/admin/sellers/accounts/resetPwd`：`seller:admin:account:resetPwd`
  - `PUT /seller/admin/sellers/accounts/resetDefaultPwd`：`seller:admin:account:resetPwd`
  - `GET /seller/admin/sellers/{sellerId}/accounts/{accountId}/roles`：`seller:admin:account:role:query`
  - `PUT /seller/admin/sellers/{sellerId}/accounts/{accountId}/roles`：`seller:admin:account:role:edit`
- 卖家主体详情保留 `seller:admin:query`；修正过一次误把主体详情换成账号权限的错位。
- `seller_buyer_management_seed.sql` 补入 6 个卖家管理端账号域 `sys_menu` 权限。
- `PartnerModuleConfig` 增加可选 `accountPermissions` 配置。
- `Seller/index.tsx` 配置卖家账号域权限；`Buyer/index.tsx` 未配置，买家仍保持旧权限模型。
- `PartnerManagementPage` 的主体行“账号”入口改为读取 `accountPermissions.list`。
- `PartnerAccountModal` 的新增、编辑、重置密码、分配角色入口改为读取 `accountPermissions`。
- `SellerAdminPermissionContractTest` 增加账号域权限契约，固定主体详情与账号列表不能再错位。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system "-Dtest=SellerAdminPermissionContractTest,AdminDirectLoginPermissionContractTest" test
```

- 通过，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system test
```

- 通过，`Tests run: 35, Failures: 0, Errors: 0, Skipped: 0`。

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl seller -am -DskipTests compile
```

- 通过，`seller` 及依赖模块编译成功。
- 补充说明：`mvn -pl seller -DskipTests compile` 未通过，原因是当前工作区已有 seller 依赖的 `ruoyi-system` / `product` 新增类，需要 `-am` 一起构建依赖模块；这不是本轮权限注解导致的编译错误。

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc
```

- 通过。

```powershell
cd E:\Urili-Ruoyi\react-ui
npx biome lint src\components\PartnerManagement\PartnerManagementPage.tsx src\components\PartnerManagement\PartnerAccountModal.tsx src\pages\Seller\index.tsx
```

- 通过。

```powershell
cd E:\Urili-Ruoyi
git diff --check -- RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\controller\AdminSellerController.java RuoYi-Vue\sql\seller_buyer_management_seed.sql RuoYi-Vue\ruoyi-system\src\test\java\com\ruoyi\system\architecture\SellerAdminPermissionContractTest.java react-ui\src\components\PartnerManagement\PartnerManagementPage.tsx react-ui\src\components\PartnerManagement\PartnerAccountModal.tsx react-ui\src\pages\Seller\index.tsx docs\architecture\reuse-ledger.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md
```

- 通过，仅有 LF/CRLF 工作区换行提示。
- 新增记录文件尾随空白检查：通过。

## 权限检查结果

- 卖家端账号维护接口不再复用主体新增、主体编辑、主体查询或端内角色维护权限。
- 卖家主体详情仍使用 `seller:admin:query`。
- 主体级免密和账号级免密仍使用 `seller:admin:directLogin`，未并入账号 CRUD 权限。
- 主体级会话和账号级会话、强制踢出仍使用 `seller:admin:forceLogout`，未并入账号 CRUD 权限。
- 主账号重置密码仍使用 `seller:admin:resetPwd`，未并入账号 CRUD 权限。

## 字典/选项复用检查结果

- 本切片未新增字典或业务选项。
- 账号角色字典仍复用 `seller_account_role`。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记卖家账号域权限和 `PartnerModuleConfig.accountPermissions` 复用规则。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 10 changed files`。

## 大文件合理性判断结果

- `PartnerManagementPage.tsx` 和 `PartnerAccountModal.tsx` 已是既有大文件，本轮只加入账号权限配置读取，没有继续追加新的复杂业务组件。
- `SellerAdminPermissionContractTest.java` 职责仍是卖家管理端权限契约扫描，新增断言属于同一职责。
- `seller_buyer_management_seed.sql` 已超过 500 行，但职责仍是卖家/买家主体和端内基础控制面综合初始化；本轮只补同一初始化边界内的管理端卖家账号按钮权限，暂不拆分。

## 重复代码检查结果

- 前端没有复制 seller/buyer 页面逻辑；通过 `accountPermissions` 让卖家先启用账号域权限，买家继续走默认权限。
- 后端只修改卖家 controller 权限注解，没有复制买家。

## 残留问题

- 买家管理端账号域权限尚未复制，需等卖家模板验收后再按同构规则替换 terminal、权限前缀、seed 和前端配置。
- 远程运行库执行已在后续独立切片完成，记录见 `docs/plans/2026-06-05-admin-seller-account-permission-db-execution-record.md`。
- 低权限账号运行时负向验证尚未做；当前完成的是源码契约、编译和前端类型层验证。
