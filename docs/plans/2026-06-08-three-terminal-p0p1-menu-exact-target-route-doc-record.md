# 2026-06-08 三端 P0/P1 快速推进：菜单 Exact Target、静态路由与文档口径收口记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。

当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 情况

- 本轮启动 6 个 `gpt-5.4` 只读子 Agent，覆盖 seller/buyer 后端隔离、portal auth/direct-login/session/log、SQL guard、React 管理端路由权限、product/inventory/integration/warehouse/finance 共享域、verifier/Markdown 口径。
- 6 个子 Agent 均已完成并关闭：
  - `019ea75a-bc26-7690-97e9-fa2f5b9a3dd6`
  - `019ea75a-ee6a-74a2-84a9-e058f82319a5`
  - `019ea75b-2dae-72f3-8d51-bd88e69dd3da`
  - `019ea75b-76f0-7c83-b47d-8cc74df5db43`
  - `019ea75b-b66d-71e1-bf51-15de21c090f1`
  - `019ea75b-e4ce-7153-926f-24f532da849d`
- 采纳 P1：
  - 3 个 `sys_menu` seed/update 缺 preview-confirmed exact target count/signature。
  - 3 份旧 Markdown 记录把 GPT-5.3 优先写成“用户最新规则”，容易误导后续 Agent。
  - 主线程发现 `react-ui/config/routes.ts` 的 `*` 404 路由位置会遮挡后续静态路由，补 P1 guard。
- 未采纳为阻塞：
  - read-model 全量刷新脚本当前按 staging/事务原子刷新治理，不等同历史清理脚本的人工 exact target 集合；本轮记录为口径澄清，不作为 P1 阻塞。

## 已完成

- `RuoYi-Vue/sql/20260605_source_product_library_menu_component.sql`
  - 新增 `@source_product_library_menu_component_expected_count` / `@source_product_library_menu_component_expected_signature`。
  - 新增 `assert_source_product_library_menu_component_targets()`，写 `sys_menu` 前校验当前目标行 count/signature。
- `RuoYi-Vue/sql/20260605_order_after_sale_menu_seed.sql`
  - 新增 `@order_after_sale_menu_seed_expected_count` / `@order_after_sale_menu_seed_expected_signature`。
  - 新增 `assert_order_after_sale_menu_seed_targets()`，在 `insert into sys_menu` 前 fail-closed。
- `RuoYi-Vue/sql/20260606_source_warehouse_stock_menu_rename.sql`
  - 新增 `@source_warehouse_stock_menu_rename_expected_count` / `@source_warehouse_stock_menu_rename_expected_signature`。
  - 新增 `assert_source_warehouse_stock_menu_rename_targets()`，在 `update/insert sys_menu` 前 fail-closed。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - 固定上述 3 个脚本的 exact target 变量、错误信息、hash 计算、断言调用和写入前顺序。
- `react-ui/config/routes.ts`
  - 将 `path: '*'` 404 路由移到顶层路由最后，避免遮挡 `/user`、`/account`、`/system`、`/monitor`、`/tool` 等后续静态路由。
- `react-ui/tests/static-route-authority-contract.test.ts`
  - 新增静态路由合同，固定 404 wildcard 必须最后，并固定管理端直达路由必须有 `authority` 和 `RemoteMenuRouteGuard`。
- `react-ui/tests/three-terminal.manifest.json`
  - 纳入新增静态路由合同测试。
- 文档口径修复：
  - `docs/plans/2026-06-08-three-terminal-p0p1-verify-script-owner-role-record.md`
  - `docs/plans/2026-06-08-three-terminal-p0p1-verify-backend-module-gate-record.md`
  - `docs/plans/2026-06-08-three-terminal-p0p1-no-new-blocker-scan-record.md`
  - 将“用户最新规则优先 GPT-5.3”改为“当时旧规则/过期口径”，并明确现行默认 `gpt-5.4`。
- `docs/architecture/reuse-ledger.md`
  - 记录菜单 exact target guard 模板。
  - 澄清读模型全量刷新当前采用 staging/事务原子刷新模板，不和历史清理脚本混用同一验收口径。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; .\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests/static-route-authority-contract.test.ts --runInBand`：通过，1 个 suite / 2 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，75 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
  - 4 个前端 guard 通过。
  - React typecheck 通过。
  - 21 个 Jest suite / 150 个测试通过。
  - 后端 reactor `test-compile` 通过。
  - 后端三端合同测试通过：`ruoyi-system` 199 个、`ruoyi-framework` 16 个、`finance` 9 个、`inventory` 1 个、`integration` 6 个、`product` 35 个、`seller` 96 个、`buyer` 97 个。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅输出工作区 LF/CRLF 换行风格 warning，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
