# 2026-06-08 三端 P0/P1：库存明细权限、Legacy SQL 清理与商品类目语义记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

本轮继续执行快速推进模式：只处理 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用

- 按用户最新规则先尝试 6 个 `gpt-5.3-codex-spark` 子 Agent。
- 平台返回额度限制：`You've hit your usage limit for GPT-5.3-Codex-Spark. Switch to another model now, or try again at Jun 14th, 2026 3:12 PM.`
- 已关闭失败的 GPT-5.3 子 Agent，并按 fallback 规则启动 6 个 `gpt-5.4` 只读子 Agent。
- 6 个 `gpt-5.4` 子 Agent 均已完成并关闭。

## 采纳的 P1

- 库存总览新增仓库视图暴露 SKU + 仓库粒度明细，但后端 `warehouse/list` 使用 `inventory:overview:list`，前端视图入口也未按 `inventory:overview:query` 隐藏。
- `20260608_overseas_channel_carrier_menu_restructure.sql` 删除 legacy `2040` 菜单和 `sys_role_menu` 时仅按 `menu_id = 2040` 删除，缺少预览后的 expected count/signature。
- 商品分销类目改动把 `categoryName` 从叶子类目快照变成全路径展示值，并通过 live `product_category` join 动态生成 `categoryPath`，会让历史商品展示随类目树变化漂移。

## 已修复

### 库存仓库视图权限

- `AdminInventoryOverviewController#warehouseList` 改为 `inventory:overview:query`。
- `Inventory/Overview/index.tsx`：
  - 无 `inventory:overview:query` 时隐藏仓库视图入口。
  - 若当前状态停在 `WAREHOUSE`，但权限缺失，则回退到 `SPU` 视图。
- `InventoryAdminRouteContractTest` 固定仓库视图和后端仓库明细查询必须走 `query` 权限。

### Legacy SQL 精确清理

- `20260608_overseas_channel_carrier_menu_restructure.sql` 增加：
  - `@overseas_channel_legacy_role_menu_expected_delete_count`
  - `@overseas_channel_legacy_menu_expected_delete_count`
  - `@overseas_channel_legacy_role_menu_expected_signature`
  - `@overseas_channel_legacy_menu_expected_signature`
  - `assert_legacy_channel_cleanup_targets()`
- 删除前重新计算 `sys_role_menu(menu_id=2040)` 和 `sys_menu(menu_id=2040)` 的 count/signature，签名不匹配时 fail-closed。
- `sys_menu` 删除语句同时约束 legacy 菜单签名，不再裸删。
- `SqlExecutionGuardContractTest` 新增合同固定上述 guard。

### 商品类目字段语义

- `ProductDistributionServiceImpl#fillCategorySnapshot` 恢复为保存 `category.getCategoryName()`，即叶子类目快照。
- `ProductDistributionMapper.xml` 移除 `categoryPathSelect` / `categoryPathJoin` 和 `category_path` 映射，不再从 live `product_category` 拼展示路径。
- React 商品分销 TS 页面恢复使用 `categoryName`，与当前 JS 镜像语义一致。
- `ProductDistributionMapperContractTest` 新增合同：商品分销不得引入 `categoryPath/category_path` 或 live `product_category` path join。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=InventoryAdminRouteContractTest" test`
  - 通过：1 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl inventory -am -DskipTests compile`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`
  - 通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,InventoryAdminRouteContractTest" test`
  - 通过：46 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dtest=ProductDistributionMapperContractTest,ProductDistributionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过：product 模块 7 个测试，reactor 依赖模块无匹配测试时不失败。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`
  - 通过。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过；仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 前端：4 个 guard 通过，React typecheck 通过，10 个 Jest suite / 43 个测试通过。
  - 后端：reactor `test-compile` 通过，三端合同测试通过；后端合同总计 390 个测试通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过；输出 `Already up to date`。

## 边界

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 残留

- `verify:three-terminal` 是三端隔离和关键合同守门，不是完整运行时业务回归；本轮按用户要求不做浏览器运行态验证。
- `mvn -pl product -am -Dtest=... test` 在 reactor 依赖模块无匹配测试时需要带 `-Dsurefire.failIfNoSpecifiedTests=false`，否则 Maven 会在依赖模块提前失败。
