# 2026-06-08 三端隔离 P0/P1 快速推进：商品依赖权限与 Portal 审计参数过滤记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本轮只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 历史记录（已过期口径）：当时先按旧规则尝试启动 6 个 GPT-5.3 Codex 子 Agent，工具模型为 `gpt-5.3-codex-spark`；当前现行规则已改为默认使用 `gpt-5.4`。
- 平台返回额度限制：`You've hit your usage limit for GPT-5.3-Codex-Spark. Switch to another model now, or try again at Jun 14th, 2026 3:12 PM.`
- 6 个 5.3 失败 Agent 已关闭。
- 按 fallback 规则启动并关闭 6 个 `gpt-5.4` 只读子 Agent。

子 Agent 收敛结果：

- 后端 seller/buyer 管理端 Controller/Service/Mapper：未发现确定 P0/P1；子 Agent 自跑 seller/buyer service 单测和管理端权限合同通过。
- React 卖家/买家管理页与 service：未发现确定 P0/P1；模板 guard 通过。
- SQL/seed/权限菜单/SqlExecutionGuard：未发现确定 P0/P1；`SqlExecutionGuardContractTest` 等通过；留下 P2：免密前端 URL 配错时运行不可用但不串端。
- 验证入口和 guard 脚本：未发现确定 P0/P1；`verify-three-terminal --check-manifest` 通过。
- Portal 自助接口、认证、日志：确认 1 个 P1，端内自助接口请求参数可能污染 `oper_param` 审计文本。
- 商品/库存/仓库/集成相邻业务：确认 2 个 P1，商品分销编辑页缺依赖权限时可能卡死或清空属性。

## 已修 P1

### 1. 商品分销新增/编辑页依赖权限 fail-closed

问题：

- 新增路由只要求 `product:distribution:add`。
- 编辑路由只要求商品查询/编辑权限。
- 页面实际依赖 `seller:admin:list`、`product:category:list`、`product:categoryAttribute:preview` 才能维护卖家、类目和类目属性。
- 缺 `product:categoryAttribute:preview` 时，前端把 schema 置空，提交会把 `attributeValues` 组装成空数组，后端保存时会先删旧属性再重建，存在属性被清空或编辑卡死风险。

处理：

- `react-ui/config/routes.ts`：商品分销新增/编辑静态路由改为 `authorityMode: 'all'`。
- 新增路由要求 `product:distribution:add + seller:admin:list + product:category:list + product:categoryAttribute:preview`。
- 编辑路由要求 `product:distribution:query + product:distribution:edit + seller:admin:list + product:category:list + product:categoryAttribute:preview`。
- `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx`：同步静态 fallback 权限，避免动态路由缺失时放宽。
- `react-ui/src/pages/Product/Distribution/EditPage.tsx`：保存前检查商品维护依赖权限；类目属性必须完成当前类目 schema 加载后才允许保存。
- 保存按钮在缺商品保存权限或依赖权限时禁用。

### 2. PortalLog 端内自助接口审计参数过滤

问题：

- Seller/Buyer 自助日志与会话接口不会信任前端 `subjectId/accountId` 做数据查询。
- 但 `PortalLogAspect` 对 GET 参数默认写入 `oper_param`，恶意调用方可把 `subjectId/accountId/actingAdminId/actingAdminName/directLoginReason/directLoginTicketId` 等字段塞入后台审计文本。
- 这不是读越权，但会污染管理端审计阅读面。

处理：

- `RuoYi-Vue/ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/PortalLogAspect.java`
  - 扩展端内敏感字段过滤列表：`subjectId`、`accountId`、`sellerId`、`buyerId`、`sellerAccountId`、`buyerAccountId`、`directLoginTicketId`、`actingAdminId`、`actingAdminName`、`directLoginReason`、`terminal`、`tokenId`、`operParam`、`jsonResult`。
  - GET 参数写入 `oper_param` 前先做 map key 过滤，避免仅依赖 JSON property filter。
- `PortalLogAspectContractTest`：固定请求参数 map 必须先过滤再序列化。
- `LogAspectSensitiveFieldFilterTest`：固定 PortalLog 的 scope/audit 字段过滤清单。

## 验证结果

- 历史记录（已过期命令口径）：`cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/product-distribution-permission-guard.test.ts tests/remote-menu-route-guard.test.ts --runInBand`：当时通过，2 个 suite / 17 个测试；当前公开 `npm run test:unit` 入口已收口为 `verify-three-terminal`，复核请使用 `npm run verify:three-terminal` 或直接调用 Jest 二进制。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-framework "-Dtest=PortalLogAspectContractTest,LogAspectSensitiveFieldFilterTest" test`：通过，5 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；4 个前端 guard、React typecheck、12 个 Jest suite / 66 个测试、后端 reactor `test-compile` 和后端三端合同测试全部通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅有 LF/CRLF 换行提示。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 当前残留项

- P2：免密登录 `portal.seller.web.url` / `portal.buyer.web.url` 配错时会导致免密窗口不可用；当前不会串端落库，后续可补 URL terminal/path 合同。
- P2：商品分销编辑页存在异步 schema 请求竞态时，当前处理选择 fail-closed 阻止保存，不做 UI 级加载态细调。
- P2：本轮只按源码/合同验证，未直连远端库核对 live `sys_menu`、`sys_role_menu`、`seller_menu`、`buyer_menu`、`sys_config`。
