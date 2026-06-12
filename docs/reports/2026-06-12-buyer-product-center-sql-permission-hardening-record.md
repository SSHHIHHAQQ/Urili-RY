# 2026-06-12 买家端商品中心 SQL 执行与权限架构补强记录

## 范围

本轮按用户确认完成两件事：

1. 执行买家端商品中心远端 SQL。
2. 做一个小的买家端权限架构补强，避免后续订单、售后、财务等业务权限继续混在“自助管理权限”语义里。

本轮不新增业务表，不执行商品可见范围 DDL，不做浏览器 live 页面联调。

## 数据源确认

- 连接来源：`.env.local` 中的 `RUOYI_DB_*`。
- 目标 MySQL：`gz-cynosdbmysql-grp-lucf5kyf.sql.tencentcdb.com:28634/fenxiao`。
- 命令类型：
  - `--precheck`：只读 `SELECT`。
  - `--apply`：执行 `RuoYi-Vue/sql/20260612_buyer_product_center_menu_seed.sql`，写 `buyer_menu` 和 `buyer_role_menu`。
- 密钥处理：未在命令输出和记录中写出数据库用户名、密码、token 或 `.env.local` 明文。

## SQL 执行

新增专用 runner：

```powershell
node .\scripts\buyer-product-center-sql-runner.mjs --precheck
```

写入必须显式确认：

```powershell
$env:BUYER_PRODUCT_CENTER_SQL_CONFIRM = 'APPLY_BUYER_PRODUCT_CENTER_MENU_SEED'
node .\scripts\buyer-product-center-sql-runner.mjs --apply
```

### 执行前只读预检

- 当前库：`fenxiao`
- `buyer_menu = 23`
- `buyer_owner_role = 35`
- `buyer_product_center_page_menu = 0`
- `buyer_product_center_query_permission = 0`
- `buyer_owner_product_center_grants = 0`
- `buyer_invalid_menu_perms = 0`
- `buyer_menu_id_range_violations = 0`
- `buyer_invalid_page_components = 0`
- `buyer_duplicate_menu_perms = 0`

### 执行后 postcheck

- 当前库：`fenxiao`
- `buyer_menu = 25`
- `buyer_owner_role = 35`
- `buyer_product_center_page_menu = 1`
- `buyer_product_center_query_permission = 1`
- `buyer_owner_product_center_grants = 70`
- `buyer_invalid_menu_perms = 0`
- `buyer_menu_id_range_violations = 0`
- `buyer_invalid_page_components = 0`
- `buyer_duplicate_menu_perms = 0`

### 执行后独立只读复核

再次执行：

```powershell
node .\scripts\buyer-product-center-sql-runner.mjs --precheck
```

结果与 postcheck 一致：商品中心页面菜单 1 条、查询权限 1 条、35 个 owner 角色共 70 条授权，异常项均为 0。

## 权限架构补强

新增：

- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/support/BuyerPortalPermissionCatalog.java`

该 catalog 显式拆分：

- `SELF_MANAGEMENT_PERMS`：账号、部门、角色、日志、会话等自助管理权限。
- `BUSINESS_PERMS`：买家端业务权限，目前包含 `buyer:product:center:list/query`。
- `ROLE_ASSIGNABLE_PERMS`：允许 owner 给买家端子账号角色分配的权限全集。
- `NAVIGATION_PERMS`：允许出现在 portal 导航和 getRouters 中的权限全集。

调整：

- `BuyerPortalPermissionServiceImpl` 不再维护 `PORTAL_SELF_MANAGEMENT_PERMS`，改走 `BuyerPortalPermissionCatalog`。
- `BuyerPortalController` 角色菜单模板和角色保存校验改走 `BuyerPortalPermissionCatalog.isRoleAssignable(...)`。
- `BuyerServiceImpl.DEFAULT_OWNER_PERMS` 改为 `BuyerPortalPermissionCatalog.ownerDefaultPerms()`，避免 owner 默认授权和角色可分配权限分叉。
- `PortalSelfServiceSurfaceContractTest` 增加 buyer catalog 合同，seller 端保留原自助权限集合合同。
- `react-ui/tests/buyer-product-center-sql-runner-contract.test.ts` 固定 SQL runner 的确认变量、单一 SQL 文件、postcheck 和密钥不输出边界。

## 验证

- 远端 SQL：
  - `node .\scripts\buyer-product-center-sql-runner.mjs --precheck`：通过，只读。
  - `BUYER_PRODUCT_CENTER_SQL_CONFIRM=APPLY_BUYER_PRODUCT_CENTER_MENU_SEED node .\scripts\buyer-product-center-sql-runner.mjs --apply`：通过，postcheck 精确校验通过。
  - `node .\scripts\buyer-product-center-sql-runner.mjs --precheck`：通过，只读复核。
- 前端目标 Jest：
  - `.\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests\buyer-product-center-sql-runner-contract.test.ts tests\portal-self-management-contract.test.ts tests\product-center-contract.test.ts --runInBand`
  - 结果：3 suites / 18 tests passed。
- 后端目标 Maven：
  - `mvn -pl buyer,ruoyi-system -am "-Dtest=PortalSelfServiceSurfaceContractTest,BuyerServiceImplTest,BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplPortalAccessTest,BuyerPortalPermissionServiceImplMenuTreeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：ruoyi-system 1 test passed，buyer 109 tests passed，reactor build success。

## 未验证项

- 未启动浏览器做 `/buyer/portal/product-center` live 页面截图或 DOM 验证。
- 未新增或执行商品可见范围表设计；第一版仍是公共在售商品可见。
