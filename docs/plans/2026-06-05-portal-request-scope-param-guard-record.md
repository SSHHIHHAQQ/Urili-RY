# 2026-06-05 前端 portal 请求身份范围参数守卫记录

## 目标

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理一个切片：前端 portal 页面和 service 不得把客户端身份范围字段作为请求参数发送给 seller/buyer 端接口。

本切片不新增 SQL，不执行远程数据库 DDL/DML，不改变后端权限模型，不启动 `seller-ui` / `buyer-ui` 物理拆分，也不替代后端 `PortalSessionContext` 和 session-scoped Service 的数据范围收敛。

## 已完成

- 核对 `react-ui/src/services/portal/session.ts`：当前日志和会话查询已经统一通过 `sanitizePortalQueryParams(params)` 清洗参数，过滤 `sellerId`、`buyerId`、`subjectId`、`accountId`、`sellerAccountId`、`buyerAccountId` 和 `terminal`。
- 扩展 `react-ui/scripts/check-portal-token-isolation.mjs`：
  - 禁止 `src/pages/Portal/**` 和 `src/services/portal/**` 中出现 `sellerId:`、`buyerId:`、`subjectId:`、`accountId:`、`sellerAccountId:`、`buyerAccountId:` 这类身份范围对象键。
  - 继续保留已有守卫：portal 页面不得直接调用 `request(...)`，不得硬编码 `/api/seller` / `/api/buyer`，portal 请求必须显式 `isToken:false`，日志和会话查询必须使用 `sanitizePortalQueryParams(params)`。
- 更新 `docs/architecture/reuse-ledger.md`，登记前端 portal 请求身份范围参数守卫规则。
- 更新 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 顶部当前状态，并追加本检查点。

## 子 agent 结论

- 6 个只读子 agent 已完成审计；关闭调用时工具侧已无可关闭句柄。
- 当前 `Portal/Home` 中 `row.accountId` 只作为表格行 key 使用，不属于请求身份范围参数。
- 当前 portal service 的请求出口集中在 `react-ui/src/services/portal/session.ts`；本轮不重新设计页面和 service，只加强静态守卫。
- `categoryId` 属于商品 schema 业务路径参数，允许保留。
- 验证建议采用 `npm run guard:portal-token`、定向 `biome lint` 和 `npm run tsc`。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint scripts\check-portal-token-isolation.mjs src\services\portal\session.ts src\pages\Portal\Home\index.tsx`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `git diff --check -- react-ui\scripts\check-portal-token-isolation.mjs react-ui\src\services\portal\session.ts docs\architecture\reuse-ledger.md docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\plans\2026-06-05-portal-request-scope-param-guard-record.md`：通过，仅有 LF/CRLF 工作区换行提示。
- 相关文件尾随空白检查：通过。
- 相关文件冲突标记检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，首次同步输出 `Synced 11 changed files`；记录回填后最终复跑输出 `Already up to date`。

## 当前判断

- 前端 portal 目录现在有静态守卫防止把客户端身份范围对象键带进请求构造。
- 该守卫只能减少前端误传和回归风险，真实数据范围仍必须由端 token、后端 `PortalSessionContext` 和 seller/buyer Service 内的 session-scoped 查询决定。
- 本轮没有扩大到三端前端物理拆分；后续管理端 UI 接入仍按“卖家模板验收通过后复制买家，只替换配置和 service”的方式推进。
