# 2026-06-08 三端 P0/P1 快速推进记录：上游配对与库存总览权限分流

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 历史记录（已过期口径）：按当时用户规则优先尝试 6 个 `gpt-5.3-codex-spark` 子 Agent；现行规则为默认使用 `gpt-5.4`。
- 平台返回额度限制，提示需等到 `2026-06-14 15:12` 后再试；失败 Agent 已关闭。
- 按 fallback 规则启动并关闭 6 个 `gpt-5.4` 只读子 Agent。
- 采纳的 P1：
  - 上游系统页打开仓库配对时会无条件请求官方仓列表，未按 `warehouse:official:list` 做依赖接口权限 gate。
  - 上游系统页物流渠道“已配对”标签对无 `integration:upstream:pair` 用户仍可点击解除配对。
  - 库存总览页未拆 `inventory:overview:list` 和 `inventory:overview:query`，无 list 权限时仍会请求 SPU/SKU 列表。
  - 卖家/买家管理页和上游系统页 JS 镜像仍保留完整实现，存在与 TSX 源分叉风险。
- 未发现新的确定 P0。

## 已修复问题

- `react-ui/src/pages/UpstreamSystem/index.tsx`：
  - 增加 `canQueryOfficialWarehouses = access.hasPerms('warehouse:official:list')`。
  - 无官方仓查询权限时，仓库配对候选加载 fail-closed，不再请求 `getOfficialWarehouseList`。
- `react-ui/src/pages/UpstreamSystem/components/SyncTabs.tsx`：
  - 增加 `canPairUpstream` 和 `canQueryOfficialWarehouses`。
  - 仓库配对入口同时要求 `integration:upstream:pair` 和 `warehouse:official:list`。
  - 物流渠道已配对标签在无 `integration:upstream:pair` 时只读展示，不再包 `Popconfirm` 或触发解除配对请求。
- `react-ui/src/pages/Inventory/Overview/index.tsx`：
  - 增加 `canListInventoryOverview = access.hasPerms('inventory:overview:list')`。
  - 视图切换按 `list`/`query` 分流：SPU/SKU 视图依赖 `list`，仓库视图依赖 `query`。
  - SPU/SKU request 在无 `list` 权限时返回空数据，不再请求后端列表接口。
  - 无任何库存总览权限时显示空态，不触发列表请求。
- JS 镜像收敛：
  - `react-ui/src/pages/Seller/index.js` 改为纯 re-export。
  - `react-ui/src/pages/Buyer/index.js` 改为纯 re-export。
  - `react-ui/src/pages/UpstreamSystem/index.js` 改为纯 re-export。
- 合同同步：
  - `react-ui/scripts/check-partner-management-template.mjs` 要求 seller/buyer 页面 JS 为纯 re-export。
  - `react-ui/tests/upstream-system-permission-guard.test.ts` 固定上游配对依赖权限和 JS 纯 re-export。
  - `InventoryAdminRouteContractTest` 固定库存总览 `list`/`query` 前端分流。
  - `AdminAccountPermissionUiContractTest` 改为检查 TSX 真实源，并要求 seller/buyer 页面 JS 纯 re-export。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- 历史记录（已过期命令口径）：`cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/upstream-system-permission-guard.test.ts --runInBand`：当时通过，1 个 suite / 4 个测试；当前公开 `npm run test:unit` 入口已收口为 `verify-three-terminal`，复核请使用 `npm run verify:three-terminal` 或直接调用 Jest 二进制。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=InventoryAdminRouteContractTest" test`：通过，1 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=AdminAccountPermissionUiContractTest" test`：通过，1 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：
  - 首次失败原因：`AdminAccountPermissionUiContractTest` 仍要求 seller/buyer `index.js` 复制完整配置。
  - 修复后复跑通过；4 个前端 guard、React typecheck、11 个 Jest suite / 56 个测试、后端 reactor `test-compile` 和三端合同测试全部通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 10 个变更文件，`Modified: 10 - 168 nodes in 911ms`。

## 权限、字典、复用与边界

- 权限检查结果：本轮未新增后端接口；只补齐前端对已有后端权限的依赖 gate，避免无权限用户触发 403 请求或可点击无权操作。
- 字典/选项复用检查结果：本轮未新增字典或选项字段。
- 复用台账检查结果：本轮未新增公共业务组件或公共后端服务；JS 镜像改纯 re-export 后减少重复实现，不需要追加复用台账条目。
- 重复代码检查结果：已消除 seller/buyer 管理页和 UpstreamSystem 页面的 JS/TSX 双实现分叉风险。
- 大文件合理性判断结果：本轮主要修改现有页面 guard、镜像文件和合同测试；未新增需要拆分的单一职责业务大文件。
- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮按用户要求未做浏览器、截图、DOM 或 UI 细调验收。

## 残留问题

- P2：`verify-three-terminal.mjs` 的关键前端测试自动发现正则偏窄，后续新增非关键词命名测试时可能只靠人工补 manifest。
- P2：`warehouse`、`inventory`、`ruoyi-admin` 等模块目前没有本模块 `src/test/java`，主要靠编译和 `ruoyi-system` 跨模块合同兜底。
- P2：部分 deprecated seed 文件可补合同，锁死“只能 abort，不得重新长回 DML”。
- P2：单跑 `seller` / `buyer` 测试应带 `-am`，否则可能因未带最新 `ruoyi-system` reactor 依赖而误判。
