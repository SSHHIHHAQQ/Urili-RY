# 2026-06-07 三端 P0/P1 快速推进：管理端商品分销权限 Guard 记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 1. 本轮目标

- 收口管理端商城商品分销新增/编辑直达路由缺少权限 guard 的问题。
- 收口商品分销编辑页依赖来源商品库、官方仓、三方仓列表接口时未先检查对应权限的问题。
- 给该问题补前端静态合同测试，并纳入当前 `verify-three-terminal` 快速验证入口。

## 2. 子 Agent 使用情况

- 先按用户最新要求尝试 6 个 `gpt-5.3-codex-spark` 只读子 Agent。
- 平台返回 GPT-5.3 Codex Spark 用量限制，失败 Agent 已关闭。
- 随后回退使用并关闭 6 个 `gpt-5.4` 只读子 Agent。
- 采纳结论：
  - 商品分销编辑页存在来源商品库、官方仓、三方仓列表依赖权限 guard 缺口。
  - 商品分销 create/edit 直达路由缺少 `authority` 和 `RemoteMenuRouteGuard`。
  - 商品库存聚合字段不能在当前 fast 模式里靠 `master_sku` 快速 join，需要先定库存口径。
  - `product` 直接依赖 `integration.impl` 可以作为后续 P1 小切片；mapper 直接读写来源表需要先定 facade/事实归属。
  - seller/buyer 裸 accountId、direct-login Redis key、菜单 ID 区间、portal 401 串端未发现新的明显 P0/P1。

## 3. 已完成

- `react-ui/config/routes.ts` 和 `routes.js`：
  - `/product/distribution/create` 增加 `authority: ['product:distribution:add']`。
  - `/product/distribution/edit/:spuId` 增加 `authority: ['product:distribution:edit']`。
  - 两个直达路由都增加 `wrappers: ['@/wrappers/RemoteMenuRouteGuard']`。
- `react-ui/src/pages/Product/Distribution/EditPage.tsx`：
  - 增加 `useAccess()`。
  - 官方仓列表请求先检查 `warehouse:official:list`。
  - 三方仓列表请求先检查 `warehouse:thirdParty:list`。
  - 来源 SKU 选择入口和 `getSourceProductList(...)` 请求先检查 `product:list:list`。
  - 无权限时不发依赖列表请求，返回空列表；编辑态仍保留已绑定仓库回显合并逻辑。
- `react-ui/src/pages/Product/Distribution/EditPage.js`：
  - 同步官方仓、三方仓列表权限 guard。
  - 当前 JS 镜像没有来源 SKU 弹窗和 `getSourceProductList(...)` 调用，本轮确认后未强行补不存在的 UI。
- 新增 `react-ui/tests/product-distribution-permission-guard.test.ts`：
  - 固定 create/edit 直达路由必须带权限和 `RemoteMenuRouteGuard`。
  - 固定编辑页依赖来源商品库、官方仓、三方仓列表前必须检查对应权限。
  - 固定管理端商品编辑页不得混入 `seller:admin:` / `buyer:admin:` 权限。
- `react-ui/scripts/verify-three-terminal.mjs`：
  - 将新增测试加入前端白名单。
  - 将 `product-distribution-permission` 纳入关键前端测试发现规则。

## 4. 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/product-distribution-permission-guard.test.ts --runInBand`：通过，`1` 个 suite / `3` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 `7` 个 Jest suite / `33` 个测试通过；后端 ruoyi-system `143`、ruoyi-framework `15`、integration `4`、product `2`、seller `89`、buyer `90` 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 `6` 个变更文件，Added `1`、Modified `5`，共 `117` 个节点。

## 5. 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮只处理管理端商品分销权限 guard，不改变卖家端、买家端 portal 商品接口。

## 6. 残留 P1

- 商品库存聚合字段 `available_stock`、`warehouse_count`、`inventory_status` 仍是占位，不能在当前 fast 模式用 `master_sku` 快速 join；需要先明确 SKU 级库存事实源、SPU 汇总/去重规则和状态枚举生成规则。
- `ProductDistributionServiceImpl` 直接依赖 `integration.service.impl.*`，后续可作为 P1 小切片收敛到 integration 公开 service/facade。
- `ProductDistributionMapper.xml` 直接读写来源商品、来源仓、`upstream_system_sku_pairing` 等 integration/source 表，需先定 facade、事实归属和投影同步方式，再迁移 SQL 边界。
