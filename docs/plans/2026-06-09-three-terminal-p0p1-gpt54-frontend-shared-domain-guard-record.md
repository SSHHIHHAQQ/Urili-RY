# 三端隔离 P0/P1 快速推进记录：前端 Guard 与共享业务域收口

时间：2026-06-09 00:41，本机 `Asia/Shanghai`。

## 参考方向

- 主参考：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 执行边界：只处理 P0/P1，即编译、guard、接口、权限、串端、service/字段缺失。
- 跳过项：不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

## 子 Agent 使用记录

- 用户最新指定：子 Agent 使用 `gpt-5.4`，不再使用 GPT-5.3 Codex。
- 本轮实际模型：`gpt-5.4`。
- 数量：6 个。
- 状态：全部关闭。
- 切片：
  - 后端编译、test-compile、Maven 本机产物漂移。
  - SQL seed / DDL / DML guard。
  - React 前端 guard、service、字段缺失。
  - 管理端菜单、路由、权限契约。
  - portal 鉴权、会话、direct-login、日志串端。
  - product / inventory / integration / warehouse / finance 共享业务域边界。

## 采纳的 P1

1. `Product/Distribution/index.js` 是编译产物，不是 TSX 纯 re-export，导致 `check-product-upstream-js-mirrors.mjs` 和三端 verifier 被打断。
2. `Inventory/Overview/index.js` 是编译产物；库存总览契约仍停留在共享抽取前，未识别 `InventoryAdjustButton` 已抽到共享组件。
3. 商品分销新增/编辑入口缺少 `warehouse:official:list` 和 `warehouse:thirdParty:list` 依赖权限，导致有新增/编辑权限但无仓库列表权限的角色能进入编辑器，最终因必填仓库无法选择而失败。
4. 领星 SKU 尺寸、重量、申报价等 `BigDecimal` 字段解析把 `Number` 先转成 `double`，存在精度漂移风险。
5. 商品中心仓库明细把 SPU 聚合可用库存文本写入每个仓库，造成仓库级库存事实错配。
6. 本机 8080 后端进程占用 `ruoyi-admin.jar`，`ruoyi-admin clean compile/repackage` 会被阻断；本轮未杀进程，按构建前置条件记录。

## 已完成

- `react-ui/src/pages/Product/Distribution/index.js`
  - 改回 `export { default } from './index.tsx';`。
- `react-ui/src/pages/Inventory/Overview/index.js`
  - 改回 `export { default } from './index.tsx';`。
- `react-ui/tests/inventory-overview-contract.test.ts`
  - 同步共享抽取后的事实：页面内 `InventoryAdjustButton` wrapper 转发到 `@/components/InventoryAdjust/InventoryAdjustButton`，共享组件本体保留批量调整实现。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/InventoryAdminRouteContractTest.java`
  - 同步检查共享 `InventoryAdjustButton` 本体和 wrapper 转发契约。
- `react-ui/config/routes.ts`
  - 商品分销新增/编辑静态路由增加 `warehouse:official:list`、`warehouse:thirdParty:list`。
- `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx`
  - 静态 fallback route guard 同步增加仓库列表权限。
- `react-ui/src/services/session.ts`
  - 静态路由补丁同步增加仓库列表权限。
- `react-ui/src/pages/Product/Distribution/index.tsx`
  - 新增/编辑入口可见性纳入官方仓和三方仓列表权限。
- `react-ui/src/pages/Product/Distribution/EditPage.tsx`
  - 保存按钮和依赖完整性判断纳入官方仓和三方仓列表权限。
- `react-ui/tests/product-distribution-permission-guard.test.ts`
  - 固定路由、入口和编辑页的仓库权限依赖。
- `react-ui/tests/remote-menu-route-guard.test.ts`
  - 固定 fallback route guard 权限清单。
- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/lingxing/LingxingOpenApiClient.java`
  - `firstBigDecimal(...)` 的 `Number` 分支改为基于 `number.toString()` 构造 `BigDecimal`，不再经 `double` 中间态。
- `RuoYi-Vue/integration/src/test/java/com/ruoyi/integration/lingxing/LingxingOpenApiClientTest.java`
  - 新增 `Float` 和大整数精度契约。
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/domain/ProductCenterWarehouse.java`
  - 移除 `stockText` 字段。
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductCenterServiceImpl.java`
  - 不再把 SPU 聚合库存文本写入仓库明细。
- `RuoYi-Vue/product/src/test/java/com/ruoyi/product/service/impl/ProductCenterServiceImplTest.java`
  - 固定商品中心不暴露仓库级 `stockText`。
- `react-ui/src/components/ProductCenter/ProductCenterDetailModal.tsx`
  - 商品中心真实详情不再从仓库读取库存文本。
- `react-ui/src/pages/Product/Distribution/components/BuyerProductPreviewModal.tsx`
  - `stockText` 改为可选；real 模式缺省时显示“按SKU库存展示”。
- `react-ui/src/types/product/product-center.d.ts`
  - 移除商品中心仓库类型的 `stockText`。
- `react-ui/tests/product-center-contract.test.ts`
  - 固定商品中心仓库明细不再使用仓库级库存文本。
- `react-ui/tests/three-terminal.manifest.json`
  - 新增 `LingxingOpenApiClientTest` 到后端关键测试清单。

## 数据源和远端影响

- 本轮没有执行 DDL。
- 本轮没有执行 DML。
- 本轮没有读取或写入远端 MySQL。
- 本轮没有读取或写入远端 Redis。
- 本轮没有启动、重启或停止后端服务。
- 本轮只运行本地编译、测试和 CodeGraph；未触达远端业务数据。

## 验证结果

- `mvn -pl seller,buyer,product,inventory,integration,warehouse,ruoyi-system -am -DskipTests test-compile`
  - 通过。
- `mvn -pl product -am -DskipTests install`
  - 初次不带上游依赖失败，说明本机 `.m2` 依赖缓存存在漂移风险。
- `mvn -pl product -am -DskipTests install`
  - 通过，刷新本机上游模块产物。
- `mvn -pl seller "-Dtest=SellerPortalProductServiceImplTest" test`
  - 通过，8 个测试。
- `mvn -pl integration,product -am "-Dtest=LingxingOpenApiClientTest,ProductCenterServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，`LingxingOpenApiClientTest` 1 个测试，`ProductCenterServiceImplTest` 5 个测试。
- `mvn -pl ruoyi-system "-Dtest=InventoryAdminRouteContractTest" test`
  - 通过，1 个测试。
- `node scripts/check-product-upstream-js-mirrors.mjs`
  - 通过。
- `npx jest --config jest.config.ts tests\inventory-overview-contract.test.ts tests\product-distribution-permission-guard.test.ts tests\remote-menu-route-guard.test.ts --runInBand`
  - 3 个 suite，27 个测试通过。
- `npx jest --config jest.config.ts tests\product-center-contract.test.ts tests\product-distribution-permission-guard.test.ts tests\inventory-overview-contract.test.ts tests\remote-menu-route-guard.test.ts --runInBand`
  - 4 个 suite，31 个测试通过。
- `node scripts\verify-three-terminal.mjs`
  - portal token、partner、seller/buyer portal product、product/upstream mirror guard 通过。
  - React typecheck 通过。
  - 前端 21 个 suite、160 个测试通过。
  - 后端 reactor test-compile 通过。
  - 后端三端契约通过。
  - 总结果：`three-terminal verification passed`。

## 残留 P2

- direct-login opener 与 portal 页超时时长不一致，当前仍能由 opener 超时收敛。
- portal 页仍有少量 console 输出和英文失败文案。
- SQL guard 对动态 DML 的识别可增强；本轮未发现现有动态 DML。
- `warehouse` 和 `ruoyi-admin` 当前测试源码覆盖较少，现阶段主要依赖架构契约和 reactor compile。
- 来源商品/来源仓库读模型仍是删除后重建策略，后续可评估 staging/swap。

## 当前判断

- 本轮未引入 seller/buyer 账号体系混用。
- 管理端仍保持若依 `sys_*` 权限体系。
- seller/buyer portal 请求仍从 token/session 推导主体，不依赖前端传入 `sellerId` / `buyerId`。
- 商品分销新增/编辑入口与必填仓库依赖接口权限已对齐。
- 商品中心真实详情不再展示伪仓库库存事实。
- 领星数值解析不再通过 `double` 中间态破坏精度。
