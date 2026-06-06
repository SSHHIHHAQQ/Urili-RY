# 2026-06-06 三端 P0/P1 库存入口与管理端授权 Seed 收口记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续执行当前快速推进模式：只修 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 按用户要求优先尝试 `gpt-5.3-codex-spark`，但 6 个 GPT-5.3 子 Agent 因平台用量限制不可用并已关闭。
- 已降级使用 6 个 `gpt-5.4` 子 Agent 并行审计 SQL/seed、React、seller/buyer 后端、portal 安全、integration/product/inventory 和验证脚本。
- 本轮采纳的 P0/P1：未确认的来源仓库库存后端/前端/SQL/job 仍可达，基础卖家/买家管理 seed 不应自动给 admin 写 `sys_role_menu`。
- 本轮未直接采纳为阻塞项：buyer portal 商品可见范围。当前复用台账已记录首版 buyer 浏览是公共 `ON_SALE` 商品目录；若后续改成 buyer 主体授权可见，需要另起表设计方案。

## 新增问题

- `AdminUpstreamSystemController` 暴露了未确认的 `inventory/sync`、`inventory/list`、`inventory-sync-state` 接口。
- `AdminSourceWarehouseStockController` 暴露了 `/integration/admin/source-warehouse-stocks/list`，且允许旧占位权限访问真实库存快照查询。
- `UpstreamSystemTask.syncInventory()` 仍会执行真实库存同步写入。
- `react-ui` 中上游系统库存 tab、库存 service 和来源仓库库存 service 仍可能触达真实库存接口。
- `20260606_upstream_inventory_dimension_sync.sql` 原先会建库存快照表、授权库存按钮并启用库存同步 job。
- `seller_buyer_management_seed.sql` 原先存在基础 seed 直接补 `sys_role_menu` 的风险；管理端授权应使用显式确认脚本。

## 已修复问题

- 移除 `AdminUpstreamSystemController` 中未确认库存 HTTP 映射。
- 将 `AdminSourceWarehouseStockController` 降级为不可路由占位类。
- 将 `UpstreamSystemTask.syncInventory()` 改为立即抛出禁用错误，避免旧 Quartz target 被触发后继续落库存数据。
- 移除 `SyncTabs.tsx` 中的 SKU 库存 tab 入口。
- 将 `SkuInventoryPanel.tsx` 改为静态占位组件。
- 移除 `upstreamSystem.ts` 中未确认库存请求函数。
- 将 `sourceWarehouseStock.ts` 改为本地空结果占位，不再请求后端。
- 将 `20260606_upstream_inventory_dimension_sync.sql` 改为只保留已确认的尺寸重量同步权限，并清理库存权限授权、隐藏库存权限、禁用旧库存 job；不再建表、不再授权库存同步、不再启 job。
- `seller_buyer_management_seed.sql` 不再作为基础 seed 自动写管理端 `sys_role_menu`；`AdminDirectLoginPermissionContractTest` 增加静态契约，要求基础 seed 不做 admin 角色授权，授权改走确认脚本。

## 残留问题

- 来源仓库库存 service/mapper/DTO 内部实现仍有残留，但当前无 HTTP、tab、真实菜单页面、权限 seed 或激活 job 入口；后续恢复必须先确认 schema、同步落库方案、权限点、审计日志和数据边界。
- buyer portal 商品浏览当前按公共 `ON_SALE` 商品目录保留，不按 buyer 主体授权范围过滤；如果业务确认需要 buyer 专属可见范围，必须先走表设计确认流程。
- 前端 Jest 当前没有业务用例，`npm test` 通过主要依赖 `verify:three-terminal` 和 `--passWithNoTests`；这是 P2 测试覆盖风险，不阻塞本轮。

## 验证命令

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:seller-portal-product`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:buyer-portal-product`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm test -- --runInBand`：通过；包含 `verify:three-terminal`、前端 typecheck、后端三端 contract/service 测试；Jest 当前无业务测试并以 code 0 退出。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-admin -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；只有 LF/CRLF 工作区换行提示，无空白错误。
- 静态搜索确认：库存 HTTP 映射、库存 tab、库存请求 URL、库存 DDL、基础 seed admin 授权块均无活入口命中；`20260606_upstream_inventory_dimension_sync.sql` 中库存权限仅作为 cleanup/disable 文本保留。

## 未验证原因

- 未做浏览器运行态、截图、DOM 或 UI 细调验收，因为用户明确要求本轮不做。
- 未执行远端数据库 DDL/DML；本轮只修改 SQL 文件，未回放到远端库。
- 未读写 Redis。
- 未启动或重启后端服务。

## 权限检查结果

- 未确认库存权限 `integration:upstream:inventoryQuery` / `integration:upstream:inventorySync` 不再被本次 SQL 授权，只用于清理已有授权和隐藏旧权限。
- 尺寸重量同步权限 `integration:upstream:dimensionSync` 保留，因为该能力已确认并仍挂在上游系统同步域。
- 卖家/买家管理基础 seed 不再直接写 `sys_role_menu`；管理端授权必须走 `20260606_admin_partner_role_menu_grant.sql` 的显式确认流程。

## 字典/选项复用检查结果

- 本轮未新增业务字典。
- 来源仓库库存相关状态选项继续登记为占位方向；恢复前不得在页面内复制状态映射或硬编码真实库存口径。

## 复用台账检查结果

- `docs/architecture/reuse-ledger.md` 已存在“来源仓库库存占位”条目，记录当前不开放真实 HTTP Controller、上游系统库存 Tab、前端路由或 Quartz 任务入口。
- 本轮补充确认该条目仍适用，后续恢复必须从 schema 和同步方案开始，不允许只恢复前端或 Controller。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 6 changed files`，`Modified: 6 - 187 nodes`。

## 大文件合理性判断结果

- 本轮新增记录文件为阶段记录，不属于业务大文件风险。
- 本轮代码修改集中在现有 Controller、Task、前端 service 和占位组件，没有新增超过阈值的复杂实现文件。

## 重复代码检查结果

- 未新增重复业务逻辑。
- `SkuInventoryPanel.tsx` 和 `sourceWarehouseStock.ts` 均降级为占位，避免保留一套未确认库存真实逻辑。
