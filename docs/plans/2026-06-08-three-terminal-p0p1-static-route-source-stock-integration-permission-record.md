# 三端快速推进 P0/P1 静态路由、来源仓库库存与 Integration 权限收口记录

日期：2026-06-08

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

## 范围

本轮继续按三端独立方向推进，只处理当前 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态、截图、DOM 检测或 UI 细调。

## 子 Agent

- 本轮收口的是已经启动并返回的 6 个只读子 Agent 切片，覆盖 seller/buyer 账号权限隔离、React runtime guard、SQL guard、product/inventory/integration/warehouse、admin 控制权、verify manifest/gate。
- 6 个子 Agent 均已关闭；主线程只采纳可复核的 P0/P1 结论。
- 历史记录（已过期口径）：当时用户曾更新后续子 Agent 规则为优先 GPT-5.3 Codex（`gpt-5.3-codex-spark`），不可用或额度限制时再回退 `gpt-5.4`。当前现行规则已改为默认使用 `gpt-5.4`；本轮未再新增子 Agent。

## 已修复

### P1：admin partner 非 admin button grant cleanup 缺事务收口

- 文件：
  - `RuoYi-Vue/sql/20260606_admin_partner_non_admin_button_grant_cleanup.sql`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
- 问题：脚本已有确认 token、精确目标数量和签名校验，但删除段缺少 `start transaction` / 删除后 completion assert / `commit`。
- 修复：
  - 删除前保持 `assert_admin_partner_button_cleanup_targets()` 预检。
  - 删除段包进事务。
  - 增加 `assert_admin_partner_button_cleanup_completed()`，删除后仍有目标 child grant 时直接 `45000` fail-closed。
  - 合同测试固定确认、事务、删除、completion assert、commit 和 drop procedure 的顺序。

### P1：静态后台详情/编辑路由缺少 route-level guard

- 文件：
  - `react-ui/config/routes.ts`
  - `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx`
  - `react-ui/tests/remote-menu-route-guard.test.ts`
- 问题：以下静态路由可直链进入页面壳子，未挂 `authority` 和 `RemoteMenuRouteGuard`：
  - `/system/dict-data/index/:id`
  - `/system/role-auth/user/:id`
  - `/monitor/job-log/index/:id`
  - `/tool/gen/import`
  - `/tool/gen/edit`
- 修复：
  - 路由配置增加后端/入口对齐的权限和 wrapper。
  - wrapper 增加静态兜底权限，避免 route prop 漂移时放行。
  - Jest 合同固定静态路由权限、`authorityMode='all'` 和 JS route mirror。

### P1：SourceWarehouseStock 前端未进三端 gate

- 文件：
  - `react-ui/src/pages/Inventory/SourceWarehouseStock/index.tsx`
  - `react-ui/src/pages/Inventory/SourceWarehouseStock/index.js`
  - `react-ui/src/services/integration/sourceWarehouseStock.js`
  - `react-ui/tests/source-warehouse-stock-contract.test.ts`
  - `react-ui/tests/three-terminal.manifest.json`
  - `react-ui/scripts/verify-three-terminal.mjs`
- 问题：来源仓库库存页面和 service 已存在，但前端未做 `inventory:sourceWarehouse:list` fail-close 短路，也未登记进三端 manifest；verifier 关键测试识别也漏掉 `source-warehouse` 命名空间。
- 修复：
  - 页面接入 `useAccess()`，无 `inventory:sourceWarehouse:list` 时筛选 options、主表 request 和展开明细都不请求后端。
  - 对应 `.js` 镜像改成纯 TS/TSX re-export，避免旧编译产物分叉。
  - 新增前端合同测试，固定 admin API、权限短路、JS mirror 和 manifest/verifier gate。
  - manifest 增加 `tests/source-warehouse-stock-contract.test.ts`，verifier 关键测试 regex 增加 `source-warehouse`。

### P1：来源商品查询混用 product 权限作为 integration 查询权限

- 文件：
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/controller/AdminSourceProductController.java`
  - `RuoYi-Vue/integration/src/test/java/com/ruoyi/integration/architecture/IntegrationAdminPermissionContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/IntegrationAdminRouteContractTest.java`
  - `react-ui/src/pages/Product/Distribution/EditPage.tsx`
  - `react-ui/tests/product-distribution-permission-guard.test.ts`
- 问题：来源商品库属于 integration 管理端读模型，旧实现把 `product:list:list` 当成 `integration:upstream:query` 的替代权限，弱化 product/integration 模块边界。
- 修复：
  - 后端 `AdminSourceProductController` 的 `list` 和 `groupDetail` 统一收口到 `@ss.hasPermi('integration:upstream:query')`。
  - 前端商品分销编辑页来源 SKU 查询只认 `integration:upstream:query`。
  - 后端和前端合同测试禁止把 `product:list:list` 作为 integration 查询替代权限。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal -- --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runInBand tests/remote-menu-route-guard.test.ts tests/source-warehouse-stock-contract.test.ts tests/product-distribution-permission-guard.test.ts`：通过，3 个 suite / 23 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,integration -am "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=SqlExecutionGuardContractTest,IntegrationAdminRouteContractTest,IntegrationAdminPermissionContractTest" test`：通过，`ruoyi-system` 69 个测试、`integration` 6 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
  - 前端 guard：4 个通过。
  - React typecheck：通过。
  - 前端 Jest：17 个 suite / 113 个测试通过。
  - 后端 reactor test-compile：14 个模块通过。
  - 后端三端合同测试：`ruoyi-system` 192、`ruoyi-framework` 16、`inventory` 1、`integration` 6、`product` 34、`seller` 96、`buyer` 97，均通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅输出既有 LF/CRLF 换行风格 warning。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 14 个变更文件，新增 1 个、修改 13 个索引节点。

## 验证备注

- 首次尝试 `npm test -- --runInBand ...` 时被项目脚本转入 `verify-three-terminal`，该脚本不接受直接传测试路径；随后改用 `npx jest --config jest.config.ts ...` 通过。
- 首次尝试 Maven 聚合定向测试时依赖模块没有同名测试触发 surefire fail；随后补 `-Dsurefire.failIfNoSpecifiedTests=false` 通过。

## 未执行

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图、DOM 检测或 UI 细调验收。

## 残留

- 本轮没有确认新的 P0/P1 残留。
- P2：若依 React 原页面里仍有部分旧权限命名不完全贴合官方后端，例如局部按钮使用 `monitor:job-log:*`、`system:dictType:*`；本轮只处理静态 route-level guard，不扩大到历史页面按钮权限梳理。
