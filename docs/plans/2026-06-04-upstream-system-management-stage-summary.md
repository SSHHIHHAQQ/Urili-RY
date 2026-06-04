# 上游系统管理首版落地总结

日期：2026-06-04

## 本次完成

- 新增后端 `integration` Maven 模块，并接入 `ruoyi-admin`。
- 新增“海外仓服务设置 / 上游系统管理”菜单和按钮权限。
- 新增上游系统接入、同步清单、仓库配对、物流渠道配对、SKU 配对、同步状态、请求日志和 SKU 配对审计表。
- 实现领星 OpenAPI 适配器、真实授权校验、仓库/物流渠道/SKU 同步、请求日志脱敏和凭证加密保存。
- 实现 React 管理端页面，包含主仓接入、授权、同步、仓库配对、物流渠道配对、SKU 配对和请求日志查看。
- 真实接入 `LX-CA012` 已保留，凭证加密保存，页面只显示脱敏 Key。

## 新增问题

- 首次真实 SKU 同步按单条 upsert 写入，5400 条数据会导致同步耗时过长并触发前端/脚本超时。
- PageHelper 分页会影响同一 Service 方法内前置 `selectOne` 查询，导致 SKU 列表和请求日志列表出现重复 `LIMIT` 的 SQL 错误。

## 已修复问题

- SKU 同步改为按页批量 upsert，并降低 integration mapper 调试日志量；真实同步通过。
- 分页列表去掉同方法内的前置 `selectOne`，避免 PageHelper 污染非分页查询；SKU 列表和请求日志列表恢复正常。
- React 主页面拆出 `SyncTabs`、`helpers`、`types`，入口文件从 561 行降到 272 行。

## 残留问题

- `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSystemServiceImpl.java` 当前 565 行。首版保留原因：当前职责集中在同一个上游系统聚合内，需要统一处理事务、同步状态、配对约束和审计写入；后续如果扩展订单、库存、费用或更多外部系统，应拆出同步任务服务、配对服务和日志查询服务。
- 全量 `npm run biome:lint` 仍失败，问题来自历史文件 `src/app.tsx`、`src/components/IconSelector/*` 等，与本次新增目录无关。
- 本次系统仓库、系统物流渠道、系统 SKU 仍按第一版手动录入 `code + name`，后续正式主数据表出现后应改为下拉选择和后端校验。

## 验证命令

| 验证项 | 命令或动作 | 结果 |
| --- | --- | --- |
| 后端构建 | `cd RuoYi-Vue; mvn -DskipTests install` | 通过 |
| 前端类型检查 | `cd react-ui; npm run tsc` | 通过 |
| 新增目录 lint | `cd react-ui; npx biome lint src/pages/UpstreamSystem src/services/integration src/types/integration` | 通过 |
| 前端生产构建 | `cd react-ui; npm run build` | 通过 |
| 全量前端 lint | `cd react-ui; npm run biome:lint` | 未通过，历史文件问题 |
| SQL 执行 | JDBC runner 执行 `RuoYi-Vue/sql/upstream_system_management_seed.sql` | 20 条语句执行成功 |
| 真实同步 | 调用 `POST /integration/admin/upstream-systems/LX-CA012/sync` | 仓库 1、物流渠道 7、SKU 5400 |
| 仓库配对 | 临时配对、重复配对、解除配对 | 通过，临时配对已删除 |
| 物流渠道配对 | 同一领星渠道配多个系统渠道、重复系统渠道、解除配对 | 通过，临时配对已删除 |
| SKU 配对 | 临时配对、重复配对、解除配对 | 通过，临时配对已删除 |
| 浏览器联调 | 打开 `http://127.0.0.1:8001/overseas-warehouse-service/upstream-system` | 页面、菜单、Tab、配对弹窗可用 |
| 明文凭证落盘检查 | `rg -n "115433\|dcde49111" .`，排除 `node_modules`、`dist`、`target` | 无命中 |

## 未验证原因

- 未清理全量 `biome:lint` 历史问题：这些问题分布在既有应用入口和图标选择器组件，超出本次上游系统菜单范围，贸然修改会扩大影响面。
- 未验证后续正式主数据下拉：系统仓库、渠道、SKU 主数据表尚未落地，当前按用户确认的第一版手动录入实现。

## 权限检查结果

- 菜单权限：已新增“上游系统管理”菜单权限 `integration:upstream:list`。
- 按钮权限：已新增查询、新增、编辑、授权、同步、配对、日志权限。
- 后端权限：Controller 接口均配置 `@PreAuthorize`。
- 验证结果：`admin` 可访问页面和接口；默认 `ry` 用户调用上游系统列表返回 403。

## 字典/选项复用检查结果

- 上游系统类型、状态、结算类型写入若依字典。
- 前端状态文案集中在 `react-ui/src/pages/UpstreamSystem/constants.ts`。
- API 和数据库保存 code，页面展示 label 或脱敏值。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`。
- 新增登记 `integration` 模块、`SecretCipherSupport`、`LingxingOpenApiClient` 和上游系统 React 组件/service/types。

## 大文件合理性判断结果

- 前端入口文件已拆分，`react-ui/src/pages/UpstreamSystem/index.tsx` 当前 272 行，`SyncTabs.tsx` 当前 289 行。
- 后端 `UpstreamSystemServiceImpl.java` 当前 565 行，首版保留但已列为后续拆分观察点。

## 重复代码检查结果

- 领星签名和 HTTP 调用集中在 `LingxingOpenApiClient`。
- 凭证加密集中在 `SecretCipherSupport`。
- 前端 API 集中在 `react-ui/src/services/integration/upstreamSystem.ts`。
- 前端配对弹窗复用 `PairingModal`，主仓接入弹窗复用 `ConnectionModal`。
