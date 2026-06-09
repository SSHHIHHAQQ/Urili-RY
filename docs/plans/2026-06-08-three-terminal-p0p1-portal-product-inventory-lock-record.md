# 2026-06-08 三端 P0/P1 快速推进记录：Portal 商品权限与库存乐观锁

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 历史记录（已过期口径）：按当时用户规则优先尝试 6 个 `gpt-5.3-codex-spark` 子 Agent；现行规则为默认使用 `gpt-5.4`。
- 平台返回额度限制，提示需等到 `2026-06-14 15:12` 后再试；失败 Agent 已关闭。
- 按 fallback 规则启动并关闭 6 个 `gpt-5.4` 只读子 Agent。
- 采纳的 P1：
  - seller/buyer portal 商品列表只按 `product:distribution:list` 展示区块，却无条件展示详情动作；后端详情和 SKU 明细接口要求 `product:distribution:query`。
  - seller portal SKU 明细只在 SPU 层校验归属，SKU SQL 没有下推 `seller_id` 约束。
  - 库存调整有 `version` 字段但 update 只按 `stock_id`，并发调整可能静默覆盖且仍写入 ledger。
  - portal 商品子页面 JS 镜像仍保留完整实现，存在与 TSX 源分叉风险。
- 未发现新的确定 P0。

## 已修复问题

- `react-ui/src/pages/Portal/Home/index.tsx`：
  - 增加 `canQueryDistributionProducts`，按 `product:distribution:query` 判断详情动作权限。
  - seller/buyer 商品列表仍可由 `product:distribution:list` 展示，但详情动作必须额外有 `query` 权限。
- `react-ui/src/pages/Portal/Home/SellerOwnDistributionProductList.tsx`、`BuyerDistributionProductList.tsx`：
  - 增加 `canQuery` fail-closed 参数。
  - 无 `query` 权限时不渲染详情操作列，也不触发详情/SKU 请求。
- Portal 商品 JS 镜像收敛：
  - `SellerOwnDistributionProductList.js`、`BuyerDistributionProductList.js`、`SellerProductSchemaPreview.js`、`BuyerProductSchemaPreview.js` 改为纯 re-export。
  - `check-seller-portal-product-template.mjs`、`check-buyer-portal-product-template.mjs` 和 `portal-product-schema-preview.test.ts` 固定上述合同。
- seller portal SKU 明细：
  - `IProductDistributionService` 新增 `selectSkuList(spuId, sellerId)`。
  - `ProductDistributionMapper` 新增 `selectSkuListBySpuIdAndSellerId`，SQL 同时带 `sk.spu_id` 和 `sk.seller_id`。
  - `SellerPortalProductServiceImpl` 改为用 session sellerId 调用带作用域的 SKU 查询。
  - `ProductDistributionMapperContractTest`、`ProductDistributionServiceImplTest`、`SellerPortalProductServiceImplTest` 固定服务与 SQL 合同。
- 库存调整乐观锁：
  - `InventoryOverviewMapper.xml#updateWarehouseStock` 的 `where` 增加 `version = #{version}`。
  - `InventoryOverviewServiceImpl#confirmAdjust` 校验 update 返回行数，冲突时抛出“库存数据已变更，请刷新后重试”，不继续写 ledger 或刷新读模型。
  - `InventoryAdminRouteContractTest` 固定乐观锁 SQL 和 Service 行数校验。

## 验证结果

- 历史记录（已过期命令口径）：`cd E:\Urili-Ruoyi\react-ui; npm run guard:seller-portal-product; npm run guard:buyer-portal-product; npm run test:unit -- --runTestsByPath tests/portal-product-schema-preview.test.ts tests/portal-session-request.test.ts tests/portal-home-error-handling.test.ts --runInBand`：当时通过，3 个 suite / 11 个测试；当前公开 `npm run test:unit` 入口已收口为 `verify-three-terminal`，复核请使用 `npm run verify:three-terminal` 或直接调用 Jest 二进制。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=InventoryAdminRouteContractTest,PortalProductEndpointPermissionContractTest" test`：通过，2 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl inventory -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dtest=ProductDistributionMapperContractTest,ProductDistributionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，10 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，8 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，9 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；4 个前端 guard、React typecheck、11 个 Jest suite / 58 个测试、后端 reactor `test-compile` 和三端合同测试全部通过。

## 权限、字典、复用与边界

- 权限检查结果：本轮未新增后端接口；补齐前端 `list/query` 依赖 gate，避免无 `query` 权限用户看到详情动作或触发 403。
- 数据范围检查结果：seller portal SKU 明细从服务和 SQL 两层下推 `sellerId`，不依赖“SPU/SKU 数据永远一致”的隐含假设。
- 库存数据底线：库存调整写路径改为乐观锁，避免并发覆盖后仍写 ledger。
- 字典/选项复用检查结果：本轮未新增字典或选项字段。
- 复用台账检查结果：本轮未新增公共业务组件或公共后端服务；JS 镜像改纯 re-export 后减少重复实现，不需要追加复用台账条目。
- 大文件合理性判断结果：本轮主要修改权限 gate、Mapper/Service 合同和测试；未新增需要拆分的单一职责业务大文件。
- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮按用户要求未做浏览器、截图、DOM 或 UI 细调验收。

## 残留问题

- P2：仓库里同时存在 `jest.config.js` 和 `jest.config.ts`，直接手工执行 `npx jest ...` 时需要显式 `--config jest.config.ts`。
- P2：历史 Markdown 记录可能仍描述旧的 JS/TS 镜像同步方式，后续可集中整理，不阻塞当前 P0/P1。
- P2：`verify:three-terminal` 是三端隔离与关键合同守门，不等同完整浏览器运行态回归；按当前快速模式不做浏览器运行态验证。
