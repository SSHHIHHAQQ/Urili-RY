# 2026-06-07 P0/P1 快速推进：Role-Menu 菜单 ID 区间与免密票据 Scope Guard 记录

## 目标

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 执行情况

- 按最新要求优先启动 `gpt-5.3-codex-spark` 子 Agent。
- 实际启动 6 个 `gpt-5.3-codex-spark` 只读子 Agent；其中：
  - 1 个完成验证入口扫描，结论为 `verify-three-terminal` 未发现 P0/P1 漏测或空跑。
  - 1 个完成后端权限串端扫描，采纳其 `count*MenusByIds` 缺 SQL 层菜单 ID 区间 guard 的 P1。
  - 1 个因上下文失败后回退使用 `gpt-5.4` 做 direct-login 短范围扫描；其自助日志/会话泄漏结论经本地核实为 Controller 已映射 `PortalOwn*Profile`，不作为本轮 P1 修复。
  - 3 个因上下文超限、超时或未产出可采纳结论已关闭。
- 所有本轮已启动子 Agent 均已关闭。

## 已采纳

- P1：seller/buyer role-menu 绑定前的菜单存在性计数只按本端菜单表计数，未在 SQL 层显式限制菜单 ID 区间。
- P1 增强：管理端免密票据列表合同只检查 terminal 强制过滤，未静态固定 `targetSubjectId + targetAccountId` 的 normalizer 顺序和 scoped account mapper。

## 未采纳

- 端内自助日志/会话实际响应泄漏：本地核实 `SellerPortalController` / `BuyerPortalController` 已在响应前映射为 `PortalOwnLoginLogProfile`、`PortalOwnOperLogProfile`、`PortalOwnSessionProfile`，且 `PortalSelfServiceSurfaceContractTest` 与序列化测试已守住可见面。本轮不改 service 返回类型。
- `PortalDirectLoginSupport` legacy Redis key 删除：当前认证链路不读取旧 key，旧 key 仅作为历史残留清理目标，符合 AGENTS 当前规则。本轮不改。

## 已完成

- `SellerPortalPermissionMapper.xml` 的 `countSellerMenusByIds` 增加 `seller_menu_id >= 100000 and seller_menu_id < 200000`。
- `BuyerPortalPermissionMapper.xml` 的 `countBuyerMenusByIds` 增加 `buyer_menu_id >= 200000 and buyer_menu_id < 300000`。
- `TerminalRoleMenuMapperIsolationContractTest` 增加 `count*MenusByIds` 菜单 ID 区间静态合同，除表隔离外同步守住 seller/buyer 数字空间。
- `AdminDirectLoginPermissionContractTest` 增强免密票据列表合同：必须先 normalizer，再强制 terminal，再调用共享 ticket mapper；normalizer 必须使用 `targetSubjectId + targetAccountId` 和 `select*AccountByIdAnd*Id(...)`，不得回退到裸账号 mapper。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=AdminDirectLoginPermissionContractTest,TerminalRoleMenuMapperIsolationContractTest,PortalSelfServiceSurfaceContractTest" test`：通过，3 个测试通过，0 skipped。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am "-Dtest=SellerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，seller 16 个、buyer 16 个测试通过，0 skipped。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 7 个 Jest suite / 33 个测试通过；后端 ruoyi-system 147、ruoyi-framework 15、integration 4、product 5、seller 92、buyer 93 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; rg -n "[ \t]+$" ...`：无输出，当前切片新增/变更文件未发现尾随空白。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；代码变更后同步 4 个变更文件，Modified 4，共 125 个节点；文档补写后再次同步显示 `Already up to date`。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 历史记录（已过期口径）：`AGENTS.md` 已包含“子 Agent 优先 GPT-5.3 Codex，失败再回退 gpt-5.4”的规则，本轮未改 AGENTS。

## 残留项

- `verify-three-terminal` 清单策略仍可后续增强：新增关键三端测试时应显式纳入清单，避免命名偏离导致人为漏纳入。
- 端内自助 service 返回内部模型虽未形成实际响应泄漏，但如后续要进一步收窄内部接口，可单独把 service 返回类型改为 `PortalOwn*Profile`。

## 2026-06-07 追加检查点：端内自助 Service DTO 边界与菜单状态 Guard

### 目标

本追加检查点仍以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

### 子 Agent 执行情况

- 按用户最新要求优先使用 `gpt-5.3-codex-spark`。
- 本追加检查点实际启动并关闭 4 个 `gpt-5.3-codex-spark` 只读子 Agent，未回退 `gpt-5.4`。
- 子 Agent 结论处理：
  - 验证入口扫描：`verify-three-terminal` 未发现新增 P0/P1 漏测或空跑。
  - 自助审计可见面扫描：当前 API 未实际泄漏，但 service 仍返回内部模型，采纳为 P1 防回归切片。
  - direct-login/session/log 扫描：未发现新增 P0/P1。
  - role-menu 权限扫描：采纳 `count*MenusByIds` 缺少菜单可用状态闭环校验的 P1。

### 已完成

- `ISellerService` / `IBuyerService` 的端内自助日志和会话方法返回类型收窄为 `PortalOwnLoginLogProfile`、`PortalOwnOperLogProfile`、`PortalOwnSessionProfile`。
- `SellerServiceImpl` / `BuyerServiceImpl` 将自助审计 DTO 映射下沉到 service 层，并复用 `PageHelper Page` 元数据复制，避免分页总数丢失。
- `SellerPortalController` / `BuyerPortalController` 不再接触自助审计内部模型，直接 `getDataTable(service.select*Own...)`。
- `PortalSelfServiceSurfaceContractTest` 更新为 service 边界合同：接口和实现必须返回 `PortalOwn*Profile`，service DTO 构造不能设置内部审计字段，Page 元数据必须复制。
- `PortalAnonymousEndpointContractTest` 更新为 controller 只返回 service 可见结果的合同。
- `SellerServiceImplTest` / `BuyerServiceImplTest` 增加自助日志和会话返回 own-profile 且保留 PageHelper 元数据的断言。
- `SellerPortalPermissionMapper.xml` / `BuyerPortalPermissionMapper.xml` 的 `count*MenusByIds` 增加 `status = '0'`，`batch*RoleMenu` 的菜单 join 增加 `m.status = '0'`。
- `TerminalRoleMenuMapperIsolationContractTest` 增加菜单状态过滤合同。

### 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，seller 51 个、buyer 51 个测试通过，0 skipped。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=PortalSelfServiceSurfaceContractTest,PortalAnonymousEndpointContractTest,PortalSelfAuditSerializationTest" test`：通过，4 个测试通过，0 skipped。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am "-Dtest=SellerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，seller 16 个、buyer 16 个测试通过，0 skipped。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalRoleMenuMapperIsolationContractTest,PortalSelfServiceSurfaceContractTest,PortalAnonymousEndpointContractTest" test`：通过，3 个测试通过，0 skipped。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 7 个 Jest suite / 33 个测试通过；后端 ruoyi-system 147、ruoyi-framework 15、integration 4、product 5、seller 92、buyer 93 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; rg -n "[ \t]+$" ...`：无输出，当前追加检查点新增/变更文件未发现尾随空白。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 13 个变更文件，Modified 13，共 1091 个节点。

### 边界说明

- 本追加检查点未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本追加检查点未启动或重启后端。
- 本追加检查点未做浏览器、截图、DOM 或 UI 细调验收。
- `seller_menu` / `buyer_menu` 当前 schema 没有 `del_flag`，所以本轮只按现有字段补 `status = '0'`；如果未来为菜单表增加软删字段，需要再把 `del_flag = '0'` 纳入同一合同。

### 残留项

- `verify-three-terminal` 清单策略仍可后续增强：新增关键三端测试时应显式纳入清单，避免命名偏离导致人为漏纳入。
- role-menu 权限字符串格式仍是 P2 级别的统一正规化问题，当前不阻塞 P0/P1 快速推进。

## 2026-06-07 追加检查点：Portal 菜单读路径运行时 Fail-Closed

### 目标

本追加检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1。当前切片聚焦 seller/buyer 登录后菜单下发路径：历史脏 `role_menu/menu` 关系不能绕过写路径校验，被读成跨端菜单、空权限菜单或静默占位路由。

### 子 Agent 执行情况

- 先按用户最新要求尝试 6 个 `gpt-5.3-codex-spark` 只读子 Agent；平台返回用量限制，全部已关闭。
- 随后按 fallback 规则启动并关闭 6 个 `gpt-5.4` 只读子 Agent，覆盖 seller/buyer 控制面、direct-login/session/log、SQL/seed、React guard、端内菜单权限模型和文档残留。
- 采纳的 P1：seller/buyer `selectPortalMenuTree(...)` 读路径没有复用端内菜单 ID 区间、`component` 根路径和 `perms` 前缀校验。
- 未在本切片采纳：
  - `ProductDistributionMapper` 跨模块表访问和库存聚合占位仍需业务边界/库存事实源设计，不能在本轮直接改 SQL 接通。
  - `verify-three-terminal` manifest 化属于后续验证体系增强，本轮记录为残留，不阻塞当前 P1 修复。

### 已完成

- `PortalPermissionSupport` 新增 `assertReadableTerminalMenu(...)`，统一复核端内菜单非空、菜单 ID 区间、菜单类型、页面组件根路径和权限前缀。
- `SellerPortalPermissionServiceImpl.selectPortalMenuTree(...)` 在构建树之前逐条复核 `selectSellerAccountMenuList(...)` 返回的菜单，发现脏菜单直接 fail-closed。
- `BuyerPortalPermissionServiceImpl.selectPortalMenuTree(...)` 对称补齐同一读路径复核。
- `SellerPortalPermissionServiceImplMenuTreeTest` / `BuyerPortalPermissionServiceImplMenuTreeTest` 改用当前端内菜单 ID 区间，并新增脏菜单读路径拒绝用例，覆盖跨端 ID、跨端权限、admin 命名空间、通配权限、空权限、跨端/公共/空组件和空菜单类型。
- 已更新 `docs/architecture/reuse-ledger.md`。

### 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalPermissionSupportTest,SellerPortalPermissionServiceImplMenuTreeTest,BuyerPortalPermissionServiceImplMenuTreeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`PortalPermissionSupportTest` 4 个、seller 菜单树 8 个、buyer 菜单树 8 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am clean "-Dtest=PortalPermissionSupportTest,SellerPortalPermissionServiceImplMenuTreeTest,BuyerPortalPermissionServiceImplMenuTreeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，clean 后重新编译并通过同一组 20 个测试，排除旧 `ruoyi-system` / seller / buyer 产物误判。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; rg -n "[ \t]+$" ...`：无输出，当前切片新增/变更文件未发现尾随空白。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 1 个变更文件，Modified 1，共 70 个节点。

### 边界说明

- 本追加检查点未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本追加检查点未启动或重启后端。
- 本追加检查点未做浏览器、截图、DOM 或 UI 细调验收。
- 本追加检查点只处理 seller/buyer portal 菜单下发读路径；管理端菜单编辑查询路径如需更严格复核，应单独切片处理。

### 残留项

- `verify-three-terminal` 清单策略可后续 manifest 化，降低人工白名单漏纳入风险。
- `ProductDistributionMapper` 跨模块表访问仍需按来源快照、SKU pairing 投影和事实归属方案继续收口。
- 商品库存聚合字段仍需库存事实源、SPU 汇总、去重规则、仓库范围和状态枚举设计确认后再接通。
