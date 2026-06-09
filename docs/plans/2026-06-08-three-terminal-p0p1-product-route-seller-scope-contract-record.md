# 2026-06-08 三端 P0/P1 快速推进记录：商品路由权限与卖家作用域合同收敛

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 历史记录（已过期口径）：本轮按当时用户规则优先尝试 6 个 `gpt-5.3-codex-spark` 子 Agent；现行规则为默认使用 `gpt-5.4`。
- 平台返回额度限制，提示需等到 `2026-06-14 15:12` 后再试；失败 Agent 已关闭。
- 按 fallback 规则启动并关闭 6 个 `gpt-5.4` 只读子 Agent。
- 未发现新的确定 P0。
- 采纳的 P1：
  - `react-ui/config/routes.js` 仍是手工镜像，容易与 `routes.ts` 漂移。
  - 商品编辑页静态路由只有 `product:distribution:edit`，但页面详情加载还依赖 `product:distribution:query`。
  - 前端远程菜单权限 matcher 缺少独立合同，容易退回空权限放行或半通配误放行。
  - seller portal 商品详情和 SKU 明细需要把 `sellerId` 下推到 SQL 层，不应只在 service 层做对象后验。
  - seller portal 商品列表 SQL 已有 `seller_id` 过滤，但缺少合同固定。
  - `verify-three-terminal.mjs` 的关键后端测试发现不能只依赖类名正则。

## 已修复问题

- `react-ui/config/routes.js`：
  - 改为纯 re-export `routes.ts`，避免 TS/JS 路由双写漂移。
- `react-ui/config/routes.ts`、`react-ui/src/wrappers/RemoteMenuRouteGuard.tsx`、`react-ui/src/services/session.ts`：
  - `/product/distribution/edit/:spuId` 固定同时要求 `product:distribution:query` 和 `product:distribution:edit`。
  - 增加 `authorityMode: 'all'`，该路由必须两个权限都具备才允许进入。
  - 保持普通路由默认 `any` 语义，避免扩大其它路由的权限要求。
- `react-ui/tests/permission-contract.test.ts`、`remote-menu-route-guard.test.ts`、`product-distribution-permission-guard.test.ts`：
  - 固定空权限 fail-closed、精确权限匹配、半通配拒绝、显式 `*:*:*` 超级权限允许。
  - 固定商品编辑页必须 `query + edit + authorityMode all`。
  - 固定 `routes.js` 只能 re-export，不能重新出现完整路由副本。
- `ProductDistributionServiceImpl` / `IProductDistributionService` / `ProductDistributionMapper` / `ProductDistributionMapper.xml`：
  - 新增 `selectProductById(spuId, sellerId)` 和 `selectProductByIdAndSellerId`。
  - SQL 层以 `p.spu_id + p.seller_id + p.del_flag` 过滤 seller portal 商品详情。
  - seller scoped SKU 查询继续要求先通过 scoped product lookup，再查 scoped SKU。
- `SellerPortalProductServiceImpl` 与相关单测：
  - `requireOwnProduct` 改为使用当前 session sellerId 查询商品详情。
  - 列表和详情 DTO 映射继续按当前 sellerId 查询 SKU，不依赖共享商品对象里的 embedded SKU。
- `ProductDistributionMapperContractTest`、`PortalProductEndpointPermissionContractTest`：
  - 固定 seller portal 商品列表 SQL 必须保留 `p.seller_id = #{sellerId}`。
  - 固定 seller portal 商品详情必须调用 `selectProductById(spuId, session.getSubjectId())`。
  - 禁止 seller portal 退回单参数 `selectProductById(spuId)` 或 `selectSkuList(spuId)`。
- `react-ui/scripts/verify-three-terminal.mjs`：
  - 关键后端测试发现增加路径模式兜底，避免合同类名或显式清单轻微漂移导致漏测。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,product -am "-Dtest=PortalProductEndpointPermissionContractTest,ProductDistributionMapperContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；`PortalProductEndpointPermissionContractTest` 2 个测试，`ProductDistributionMapperContractTest` 8 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；4 个前端 guard、React typecheck、12 个 Jest suite / 65 个测试、后端 reactor `test-compile` 和三端合同测试全部通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅有 LF/CRLF 换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 20 个变更文件，`Added: 1, Modified: 19 - 894 nodes in 1.6s`。

## 权限、数据范围与边界

- 权限检查结果：商品编辑路由现在明确要求查询和编辑两个权限；空权限不再被前端 guard 误放行。
- 数据范围检查结果：seller portal 商品详情和 SKU 明细均从当前 seller session 推导 sellerId，并下推到 product mapper SQL 层。
- 字典/选项检查结果：本轮未新增字典、选项或字段。
- SQL/远程库检查结果：本轮未新增 SQL，未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 运行边界：本轮未启动或重启后端，未做浏览器、截图、DOM 或 UI 细调验收。

## 残留问题

- P2：历史 Markdown 记录中仍有早前只写 `gpt-5.4` 子 Agent 的旧检查点；本轮目标追踪已按最新规则追加，不回改历史上下文。
- P2：`verify-three-terminal.mjs` 仍主要用静态合同和测试清单守门，不等同浏览器运行态回归；按当前快速模式不做浏览器验证。
- P2：未来如果要限制 portal 权限 matcher 不接受 `*:*:*`，需要先确认超级权限业务口径；本轮保持既有兼容语义。
