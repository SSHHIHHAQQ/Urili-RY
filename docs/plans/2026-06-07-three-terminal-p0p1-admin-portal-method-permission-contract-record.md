# 三端 P0/P1 管理端与端内 Portal 方法级权限契约记录

日期：2026-06-07

## 目标

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，在快速推进模式下只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。

本切片专门收口方法级权限契约：

- 管理端 seller/buyer 主体、账号、角色、部门、菜单 Controller 不能只靠路径或 seed 前缀约束，关键方法必须锁定到精确 `*:admin:*` 权限。
- seller/buyer 端内 Portal 的账号、部门、角色列表接口必须要求端内细粒度权限，不能只校验 terminal。
- 只补静态契约和记录，不执行数据库 DDL/DML，不做浏览器、截图、DOM 或 UI 细调。

## 子 Agent

- 先按最新规则尝试启动 6 个 `gpt-5.3-codex-spark` 只读子 Agent；平台返回额度限制，失败 Agent 均已关闭。
- 随后回退启动 6 个 `gpt-5.4` 只读子 Agent，覆盖 seller admin、buyer admin、portal 自助入口、product 端内商品、测试契约、SQL seed。
- 6 个 `gpt-5.4` 子 Agent 均已完成并关闭，均未修改文件。
- 已采纳 P1：seller/buyer 管理端 role/dept/menu Controller 和主体/账号关键方法需要精确方法级权限合同。
- 已采纳 P1：seller/buyer portal `accounts` / `depts` / `roles` 需要端内 `account:list` / `dept:list` / `role:list` 权限合同。
- product 子 Agent 提出 buyer 商品可见性仍取决于业务口径；当前没有已确认的 buyer 可见范围表或规则，本轮记录为后续业务口径，不在 P0/P1 快速切片中实现。
- SQL seed 子 Agent 未发现本切片需新增或执行的 seed 缺口。

## 已完成

- `SellerAdminPermissionContractTest` 补齐 `AdminSellerController` 的 `list/add/edit/changeStatus/resetOwnerPassword` 权限断言。
- `SellerAdminPermissionContractTest` 新增 `sellerRoleDeptMenuHandlersMustUseSpecificPermissions()`，锁定 seller 管理端 role/dept/menu 方法权限。
- `BuyerAdminPermissionContractTest` 按卖家模板机械复制，锁定 buyer 管理端主体、账号、role/dept/menu 方法权限。
- `PortalAnonymousEndpointContractTest` 扩展 seller/buyer portal `accounts` / `depts` / `roles` 权限断言。
- `docs/architecture/reuse-ledger.md` 已登记管理端方法级权限和 portal 自助列表权限复用规则。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,PortalAnonymousEndpointContractTest" test`：通过，9 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
- 前端 guard：portal token、partner management、seller portal product、buyer portal product 均通过。
- React typecheck：通过。
- 前端 Jest：6 个 suite / 30 个测试通过。
- 后端 reactor 编译门：`mvn -pl ruoyi-admin -am -DskipTests test-compile` 通过。
- 后端三端合同测试：`ruoyi-system` 138 个、`ruoyi-framework` 15 个、`product` 1 个、`seller` 91 个、`buyer` 92 个测试通过。
- `three-terminal verification passed.` 已输出。

## CodeGraph

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 3 changed files`、`Modified: 3 - 168 nodes`。

## 未执行

- 未执行数据库 DDL/DML。
- 未读取或写入远程 MySQL / Redis。
- 未做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 结论

本轮把管理端 seller/buyer 的主体、账号、角色、部门、菜单关键方法权限，以及端内 portal 账号/部门/角色列表权限固定成可回归的静态契约。后续复制同构管理端能力时，应继续按“卖家模板先收口，再机械复制买家”的方式推进。
