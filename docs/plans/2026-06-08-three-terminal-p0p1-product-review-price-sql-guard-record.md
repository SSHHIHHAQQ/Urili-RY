# 三端独立 P0/P1 ProductReview 调价审核与 SQL Guard 收口记录

日期：2026-06-08

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

## 范围

本轮继续按快速推进模式处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

未执行远程 MySQL DDL/DML，未读取或写入 Redis。

## 子 Agent 使用

- 先按用户要求尝试 6 个 GPT-5.3 Codex 子 Agent（`gpt-5.3-codex-spark`）。
- 平台返回额度限制，提示需要等到 2026-06-14 15:12 后再试；该批 6 个子 Agent 已关闭。
- 回退使用 6 个 `gpt-5.4` 只读子 Agent，覆盖：
  - seller/buyer 后端账号隔离
  - portal auth/session/direct-login/log
  - SQL guard
  - React runtime guard/sidecar
  - product/inventory/integration/warehouse
  - `verify-three-terminal` 门禁覆盖
- 6 个 `gpt-5.4` 子 Agent 均已关闭。

## 结论处理

- seller/buyer 后端隔离、portal auth/session/direct-login/log、React runtime guard/sidecar、`verify-three-terminal` 门禁：未发现 P0/P1。
- ProductReview 子 Agent 报告的编译 P0 在当前工作区已不存在；当前复核确认 `validateReviewPrice(...)` 已存在，`ProductReviewServiceImplTest` 可编译通过。
- ProductReview 调价审核并发覆盖 P1 属实：审批时原逻辑只读取 `AFTER` SKU 快照，未校验 `BEFORE` 快照与当前正式 SKU 是否一致。
- SQL guard 子 Agent 报告的 3 个 P1 属实：历史增量脚本中真实 DML 的 count/signature 和事务边界不足。

## 已修复

### ProductReview 调价审核

- `approveSkuSalePriceEdit(...)` 现在必须同时存在 `BEFORE` 和 `AFTER` SKU 快照。
- 审批写入新销售价前，使用 `BEFORE` 快照 hash 与当前正式 SKU `snapshotSkuFull(...)` hash 比对。
- 当前正式 SKU 已变化时，直接拒绝审批并提示“SKU 正式数据已变化，请重新提交审核”。
- `IProductDistributionService.prepareReviewedProductUpdate(...)` 和 `applyReviewedProductUpdate(...)` 从默认抛 `UnsupportedOperationException` 改为抽象接口方法，避免实现遗漏被运行时才发现。
- `ProductReviewServiceImplTest` 新增调价审核并发变化拒绝用例，并让成功用例覆盖 `BEFORE` 快照。

### SQL Guard

- `20260607_inventory_overview_platform_stock.sql`
  - 增加字典 seed、菜单 seed、库存初始化动态目标的 expected count/signature。
  - 增加 `tmp_inventory_overview_platform_stock_write_targets` 和 `inventory_overview_platform_stock_assert_count_signature(...)`。
  - 将字典、菜单、库存初始化和 read model DML 放入事务段。

- `20260605_mall_product_distribution_seed.sql`
  - 增加字典 seed、历史 `product_sales_status/DISABLED` 行、菜单 seed 的 expected count/signature。
  - 将 `2481-2486` 按钮菜单纳入 `tmp_mall_product_distribution_sys_menu_guard`，冲突时 fail-closed。
  - 将字典和菜单 DML 放入事务段。

- `20260605_mall_product_editor_ui_sample_data.sql`
  - 增加 `product_name_en = ''` 宽条件回填目标 count/signature。
  - 增加现有 `SPUDEMO20260605%` / `SKUDEMO20260605%` 演示数据命名空间 count/signature。
  - 将回填和演示数据 DML 放入事务段。

- `SqlExecutionGuardContractTest` 新增/扩展静态合同，固定上述 guard、错误文案、事务边界和 DML 前置顺序。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dtest=ProductReviewServiceImplTest" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，`ProductReviewServiceImplTest` 9 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`
  - 通过，68 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product,inventory,integration,warehouse,ruoyi-system,seller,buyer -am -DskipITs -DskipTests compile`
  - 通过，10 个 reactor 模块编译成功。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 前端：15 个 Jest suite / 101 个测试通过，React typecheck 通过，4 个前端 guard 通过。
  - 后端：reactor test-compile 通过；ruoyi-system 192、ruoyi-framework 16、inventory 1、integration 6、product 27、seller 96、buyer 97 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过，退出码 0；仅有 Git LF/CRLF 工作区提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过，`Synced 4 changed files`，`Modified: 4 - 214 nodes in 1.1s`。

## 未执行

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图、DOM 或 UI 细调验收。

## 残留

- P2：`npm run verify:three-terminal` 末尾仍有 Jest 异步句柄提示；命令退出码为 0，三端门禁已判定通过，本轮不作为 P0/P1 处理。
