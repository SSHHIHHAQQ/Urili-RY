# 2026-06-08 三端 P0/P1 快速推进记录：Seller SKU 作用域与路由守门

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 本轮按目标要求启动 6 个 `gpt-5.4` 子 Agent 做只读分面扫描。
- 6 个子 Agent 已全部关闭。
- 未发现新的确定 P0。
- 采纳的 P1：
  - seller portal 列表/详情 DTO 的 embedded SKU 直接使用 `ProductSpu#getSkus()`，该集合来自共享商品服务的裸 `spu_id` SKU 查询，缺少 `seller_id` 作用域。
  - 上述 seller embedded SKU 作用域风险缺少合同覆盖。
  - `Seller/index.js`、`Buyer/index.js` 运行入口 sidecar 未纳入 guard，可能漂移后仍误报通过。
  - `config/routes.ts/js` 的关键三端静态路由只做散落字符串检查，缺少 path、authority、wrapper、component 成组合同。
  - `RouterVoPermissionContractTest` 是关键后端权限合同，但未进入 `criticalBackendExplicitTestClasses`，被误删 manifest 时自检不会 fail-closed。

## 已修复问题

- `SellerPortalProductServiceImpl`：
  - `selectOwnProductList` 和 `selectOwnProductById` 映射 DTO 时传入当前 session 的 `sellerId`。
  - `toPortalProduct` 不再使用 `product.getSkus()`，改为调用 `productDistributionService.selectSkuList(product.getSpuId(), sellerId)`。
  - 保持 `selectOwnSkuList` 现有 owner 校验和 scoped SKU 查询。
- `SellerPortalProductServiceImplTest`：
  - 固定列表和详情即使收到 `ProductSpu#getSkus()` 中的异主体脏 SKU，也只返回 scoped SKU 查询结果。
  - 固定列表和详情都会向 `selectSkuList(spuId, sellerId)` 下推当前 sellerId。
- `PortalProductEndpointPermissionContractTest`：
  - 新增 seller embedded SKU 静态合同，禁止退回 `result.setSkus(toPortalSkus(product.getSkus()))`。
  - 固定 seller portal embedded SKU 必须走 `selectSkuList(product.getSpuId(), sellerId)`。
- `check-portal-token-isolation.mjs`：
  - 增加 `src/pages/Seller/index.js`、`src/pages/Buyer/index.js` pure re-export 断言。
- `remote-menu-route-guard.test.ts`：
  - 增加 `config/routes.ts` 和 `config/routes.js` 静态路由成组合同。
  - 固定 `/seller`、`/buyer` 必须绑定对应 admin 权限、`RemoteMenuRouteGuard` 和对应组件。
  - 固定 `/seller|buyer/login`、`/seller|buyer/direct-login`、`/seller|buyer/portal` 继续绑定 Portal 登录、免密登录和首页组件。
- `three-terminal.manifest.json`：
  - 将 `RouterVoPermissionContractTest` 加入 `criticalBackendExplicitTestClasses`。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- 历史记录（已过期命令口径）：`cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/remote-menu-route-guard.test.ts --runInBand`：当时通过，1 个 suite / 10 个测试；当前公开 `npm run test:unit` 入口已收口为 `verify-three-terminal`，复核请使用 `npm run verify:three-terminal` 或直接调用 `.\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath ... --runInBand`。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller -am "-Dtest=PortalProductEndpointPermissionContractTest,SellerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；`PortalProductEndpointPermissionContractTest` 2 个测试，`SellerPortalProductServiceImplTest` 8 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；4 个前端 guard、React typecheck、11 个 Jest suite / 59 个测试、后端 reactor `test-compile` 和三端合同测试全部通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅有 LF/CRLF 换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 5 个变更文件，`Modified: 5 - 270 nodes in 931ms`。

## 权限、数据范围与边界

- 权限检查结果：本轮未新增后端接口或权限点；补强现有三端路由和权限合同。
- 数据范围检查结果：seller portal embedded SKU 现在从 facade 层重新按 `sellerId` 查询，不依赖共享商品对象里已挂载的 SKU 集合。
- 字典/选项检查结果：本轮未新增字典、选项或字段。
- SQL/远程库检查结果：本轮未新增 SQL，未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 运行边界：本轮未启动或重启后端，未做浏览器、截图、DOM 或 UI 细调验收。

## 残留问题

- P2：direct-login popup 超时/关闭时，管理端提示仍偏通用，后续可补更细错误提示。
- P2：运行时代码仍有 legacy direct-login key 删除分支；当前不读取旧 key，不构成本轮 P0/P1。
- P2：后续如果新增关键路由测试文件，仍需同步 `three-terminal.manifest.json`；当前通过 manifest 自检兜底。
