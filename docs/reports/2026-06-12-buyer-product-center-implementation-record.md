# 2026-06-12 买家端商品中心实施记录

## 背景

- 本次在买家端第一版开放顶级菜单“商品中心”。
- 第一版不限制买家可见商品范围，继续读取当前买家可见商品读模型中的已上架商品；未来可在 buyer service / product read model 层增加买家、渠道、合同、区域等可见范围规则。
- 本次不新增业务表；后续已按确认执行远端菜单/权限 seed，不调整管理端商品中心语义。

## 实现内容

- 前端：
  - 新增 `react-ui/src/pages/Portal/Home/BuyerProductCenter.tsx`，复用共享 `ProductCenterPage`，通过 buyer portal service 注入列表和详情数据源。
  - 新增 `/buyer/portal/product-center` 静态 shell 路由，并在 `Portal/Home` 内部增加 buyer-only 顶级菜单“商品中心”。
  - 新增 `react-ui/src/pages/Buyer/ProductCenter/index.tsx` 作为 `buyer_menu.component = Buyer/ProductCenter/index` 的真实前端落点，实际复用 portal shell。
  - buyer portal 请求统一走当前 buyer token，继续剔除前端传入的 `buyerId`、`subjectId`、`accountId` 等身份范围参数。
- 后端：
  - 新增 `BuyerPortalProductCenterController`，提供 `/buyer/product/center/list`、`/{spuId}`、`/{spuId}/skus`。
  - 接口使用 `buyer:product:center:list/query`，通过 `PortalSessionContext.requireSession("buyer")` 派生当前买家端 session，不接受前端身份范围参数。
  - `BuyerPortalProductServiceImpl` 的买家可见查询保留 `keyword`、中英文名、类目、系统 SPU/SKU 等查询条件，仍清理 seller scope 和管理端状态字段。
  - `BuyerPortalController`、`BuyerPortalPermissionServiceImpl`、`BuyerServiceImpl.DEFAULT_OWNER_PERMS` 均放行 `buyer:product:center:list/query`，确保 owner 默认授权且子账号角色可分配。
- SQL：
  - 新增 `RuoYi-Vue/sql/20260612_buyer_product_center_menu_seed.sql`，带确认 token、ID 区间、component、perms 唯一、slot signature、owner grant exact count 等 fail-closed guard。
  - `2026-06-12` 已通过 `scripts/buyer-product-center-sql-runner.mjs` 执行到远端 `fenxiao`，并通过 postcheck 与独立只读 precheck 复核。
- 记录与合同：
  - 更新 `docs/architecture/reuse-ledger.md`，登记 buyer 商品中心复用 `ProductCenterPage` 的约定。
  - 更新前端 live verifier 白名单，允许 buyer 商品中心权限，同时继续拦截 seller product 和其他冻结业务面。

## 验证

- 前端定向 Jest：
  - `.\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests\portal-self-management-contract.test.ts tests\portal-session-request.test.ts tests\product-center-contract.test.ts tests\portal-self-management-live-contract.test.ts tests\portal-self-management-live-write-contract.test.ts tests\portal-direct-login-live-contract.test.ts --runInBand`
  - 结果：6 suites / 106 tests passed。
- 后端定向 Maven：
  - `mvn -pl buyer,ruoyi-system -am "-Dtest=BuyerPortalPermissionServiceImplPortalAccessTest,BuyerPortalPermissionServiceImplTest,BuyerPortalProductServiceImplTest,PortalProductEndpointPermissionContractTest,PortalSelfServiceSurfaceContractTest,SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：ruoyi-system 86 tests passed，buyer 47 tests passed，reactor build success。
- 三端 manifest：
  - `node .\scripts\verify-three-terminal.mjs --check-manifest`
  - 结果：passed。
- 通用 diff 检查：
  - `git diff --check`
  - 结果：无 whitespace error；仅输出 Windows CRLF 提示。
- CodeGraph：
  - `codegraph sync .`
  - 结果：成功，Added 3 / Modified 10，597 nodes。
- 远端 SQL：
  - 预检：目标库 `fenxiao`，`buyer_menu = 23`，商品中心页面/查询权限/owner 授权均为 `0`，异常项均为 `0`。
  - 执行：`BUYER_PRODUCT_CENTER_SQL_CONFIRM=APPLY_BUYER_PRODUCT_CENTER_MENU_SEED node .\scripts\buyer-product-center-sql-runner.mjs --apply`。
  - postcheck：`buyer_menu = 25`，`buyer_product_center_page_menu = 1`，`buyer_product_center_query_permission = 1`，`buyer_owner_product_center_grants = 70`，异常项均为 `0`。
  - 独立只读复核：`node .\scripts\buyer-product-center-sql-runner.mjs --precheck` 通过，状态与 postcheck 一致。

## 未验证项

- 未启动浏览器进行 live 页面联调；当前完成到代码合同、后端合同、SQL guard 和索引同步。
