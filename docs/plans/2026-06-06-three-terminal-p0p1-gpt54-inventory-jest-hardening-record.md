# 2026-06-06 三端 P0/P1 快速推进：GPT-5.4 子 Agent、库存收口与 Jest 防空跑记录

## 目标

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。

本轮不做浏览器运行态验收、不做截图、不做 DOM 检测、不做 UI 细调。

## 子 Agent 使用情况

- 按用户最新要求，后续子 Agent 直接使用 `gpt-5.4`，不再尝试 GPT-5.3。
- 本轮已使用并关闭 6 个 `gpt-5.4` 子 Agent。
- 子 Agent 覆盖范围：
  - portal Controller、`@Anonymous`、`@PortalPreAuthorize`、`@PortalLog` 边界
  - seller/buyer service 与 mapper 主体隔离
  - SQL seed / migration
  - React 路由、登录、token、权限
  - 验证脚本与 contract 测试
  - 管理端控制权、免密代入、强退、审计

## 新增问题

- P0：`upstream_system_management_seed.sql` 和 `20260606_upstream_inventory_dimension_sync.sql` 仍保留未确认库存能力的表 DDL、库存权限和库存 job 启用路径。
- P0：管理端仍存在可触达或可恢复的库存接口痕迹，包括上游库存同步、库存列表、库存状态和来源仓库库存列表。
- P1：`npm test` / `npm run jest` 允许 0 个 Jest 测试静默通过，前端测试入口可能被误判为已有业务测试覆盖。

## 已修复问题

- `upstream_system_management_seed.sql` 已移除库存快照表 DDL、库存状态表 DDL、库存权限 seed、库存自动扩权和库存 job 启用块，仅保留“库存 schema 未确认前省略”的说明。
- `20260606_upstream_inventory_dimension_sync.sql` 已改为 cleanup/disable 脚本：保留已确认的 `integration:upstream:dimensionSync` 权限，清理/隐藏库存权限，禁用旧 `upstreamSystemTask.syncInventory` job，不建库存表、不启库存 job。
- `AdminSourceWarehouseStockController` 已降级为不可路由占位类。
- `AdminUpstreamSystemController` 中库存相关方法取消 HTTP mapping，不再暴露 `inventory/sync`、`inventory/list`、`inventory-sync-state`。
- `UpstreamSystemTask.syncInventory()` 保留旧 Quartz 方法名但立即抛出禁用错误，防止历史 job 命中后继续落库存数据。
- `UpstreamSystemServiceImpl.syncWarehouseStocksOnly()` 增加库存 schema 未确认守卫，当前直接失败；库存列表返回空占位，库存状态返回 `NEVER` 占位。
- `SkuInventoryPanel.tsx` 改为静态占位组件；`upstreamSystem.ts` 移除真实库存请求函数；`sourceWarehouseStock.ts` 改为本地空结果占位。
- `package.json` 去掉 Jest 的 `--passWithNoTests`，并新增 `tests/terminal-session-token.test.ts`，验证 admin/seller/buyer token storage key 互相独立。

## 残留问题

- 来源仓库库存的 domain、mapper XML 和部分 service 内部实现仍保留历史草稿；当前前端 Tab 仅为占位，不发真实请求，没有 HTTP 映射、权限 seed 或启用 job 入口。后续恢复必须先确认库存 schema、同步落库方案、权限点和审计边界。
- `20260606_upstream_inventory_dimension_sync.sql` 中库存权限字符串仅用于 cleanup/disable，不代表重新开放库存能力。
- 未处理 P2：portal 异常 terminal fallback、JS/TS twin 长期维护风险、direct-login 成功链路未在端内 session/log 上直接写 ticketId。

## 验证命令

- `cd E:\Urili-Ruoyi\react-ui; npm run jest -- --listTests`：通过，列出 `tests/terminal-session-token.test.ts`。
- `cd E:\Urili-Ruoyi\react-ui; npm run jest -- --runInBand`：通过，`2 passed`。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-admin -am -DskipTests compile`：通过，`BUILD SUCCESS`。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，`three-terminal verification passed.`。
- 静态搜索确认：当前只在 cleanup/disable SQL 中保留 `integration:upstream:inventory*` 字符串；没有 `inventory/sync`、`inventory/list`、`inventory-sync-state`、库存建表语句或可触达来源仓库库存 Controller mapping。

## 未验证原因

- 未执行远程 MySQL DDL/DML；本轮只修改 SQL 文件，不回放到远程库。
- 未读取或写入 Redis。
- 未启动或重启后端服务。
- 未做浏览器、截图、DOM 或 UI 细调验收。

## 权限检查结果

- 未确认库存权限不再被 seed 授权，也不再被自动继承扩权。
- 已确认尺寸重量同步权限 `integration:upstream:dimensionSync` 保留。
- 三端管理权限、portal 权限和端内 token 隔离继续由 `verify:three-terminal` 覆盖。

## 字典/选项复用检查结果

- 本轮未新增字典。
- 库存相关选项仅作为占位历史保留，不恢复真实页面前不得新增页面内重复映射。

## 复用台账检查结果

- 复用台账已追加本轮“来源仓库库存占位”最新约束：库存恢复必须从 schema 和同步方案开始，不能只恢复 Controller 或前端入口。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；本轮最终代码同步返回 `Synced 2 changed files`，`Modified: 2 - 140 nodes`。

## 大文件合理性判断结果

- 本轮新增 Jest 测试和记录文件职责单一。
- 未新增超过治理阈值的复杂业务文件。

## 重复代码检查结果

- 未新增重复业务逻辑。
- 库存前端降级为占位，避免保留一套未确认库存真实请求逻辑。
