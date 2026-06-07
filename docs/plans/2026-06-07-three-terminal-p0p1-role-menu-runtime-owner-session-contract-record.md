# 2026-06-07 三端 P0/P1：Role-Menu 运行时复核、Owner 角色双向约束与会话校验合同

## 目标

参考 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`，继续按三端独立方向推进 P0/P1。当前只处理编译、guard、接口、权限、串端、service/字段缺失，不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 执行情况

- 先按用户最新要求尝试 `gpt-5.3-codex-spark` 子 Agent；平台返回用量限制，失败 Agent 已关闭。
- 随后回退使用 `gpt-5.4`，共 6 个只读子 Agent 完成扫描。
- 采纳的 P1：
  - role-menu 绑定只校验菜单 ID 存在，未在运行时重新复核端内菜单 fail-closed 不变量。
  - owner 角色约束只保证 owner 账号不能丢 owner 角色，未禁止非 owner 账号绑定 owner 角色。
  - `PortalPermissionChecker` 的生产逻辑已会在无权限串时调用端内权限服务，但测试未固定这个会话活性校验路径。
- 未采纳为本轮改动：
  - 新建账号默认 `U12346`：这是此前用户明确指定的默认登录密码，本轮不擅自改成强制输入；仅作为后续安全复议项记录。
  - live smoke / 浏览器链路 / production build：用户已明确当前无需浏览器运行态验收，故不纳入本轮阻塞验证。
  - `country_region` 后端字典权威校验：属于后续 P1，需单独切片处理。

## 已完成

- `PortalPermissionSupport` 新增 `assertTerminalMenuId(...)`，固定 seller 菜单 ID 必须在 `100000-199999`，buyer 菜单 ID 必须在 `200000-299999`。
- `SellerPortalPermissionServiceImpl` / `BuyerPortalPermissionServiceImpl` 在 role-menu 写入前保留原 count 校验，并逐条读取菜单行后复核：
  - 菜单 ID 区间；
  - 页面/按钮权限不能为空；
  - 权限必须使用当前端前缀，不能使用 `*` 通配或 `seller:admin:` / `buyer:admin:` 管理端命名空间；
  - `C` 页面菜单 component 必须位于当前端页面根路径。
- seller/buyer `assignAccountRoles(...)` 改为 owner 角色双向约束：
  - owner 账号必须保留 owner 角色；
  - 非 owner 账号禁止绑定 owner 角色。
- `PortalPermissionCheckerTest` 固定无权限串的 portal 接口也必须调用当前端 `IPortalPermissionCheckService.selectPermissions(...)`，从而触发端内 DB session 活性校验。
- `SellerAdminPermissionContractTest` / `BuyerAdminPermissionContractTest` 补充 role/dept/menu 管理控制器的路由形态断言，防止同构复制时权限正确但路由串端。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest" test`：通过，8 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 7 个 Jest suite / 33 个测试通过，后端 ruoyi-system 143、ruoyi-framework 15、integration 4、product 3、seller 91、buyer 92 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalPermissionCheckerTest,SellerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；`PortalPermissionCheckerTest` 5 个、seller permission service 16 个、buyer permission service 16 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 8 个变更文件，Modified 8，共 556 个节点。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- `SellerAdminPermissionContractTest` / `BuyerAdminPermissionContractTest` 当前各约 540 行，`SellerPortalPermissionServiceImplTest` / `BuyerPortalPermissionServiceImplTest` 当前各约 718 行。它们是同一职责的架构/服务合同测试，本轮不拆分；拆分会复制大量反射/代理 helper，短期增加维护成本。后续若继续增加测试，应优先抽公共测试工具再拆文件。

## 残留 P1

- `country_region` 当前仍主要靠前端候选项和后端 2 位长度校验，后续应把 `country_region` 提升为服务层字典权威校验。
- ProductDistributionMapper 仍直接读写来源商品、来源仓、`upstream_system_sku_pairing` 等 integration/source 表，需先定 facade、事实归属和投影同步方式。
- 商品库存聚合字段 `available_stock`、`warehouse_count`、`inventory_status` 仍需按库存事实源和 SPU 汇总规则设计后接通。
