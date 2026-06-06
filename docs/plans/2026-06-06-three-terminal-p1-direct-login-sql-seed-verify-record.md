# 2026-06-06 三端 P1：免密 Hash、SQL 隔离和 Seed 一致性收口记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

本轮范围：继续按三端独立方向推进，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、不做截图/DOM 检测、不做 UI 细调。

## 新增问题

- `verify:three-terminal` 前一版未覆盖 `ruoyi-framework` 和 system support 关键测试，存在测试入口漏跑风险。
- 管理端免密登录生成的 hash 路由 URL 在 `#/seller/direct-login?directLoginToken=...` 场景下，前端解析器原先不能稳定读取 hash route query。
- `20260604_three_terminal_isolation_migration.sql` 默认隔离迁移里仍直接更新 legacy `sys_role` 的 seller/buyer 角色，容易把当前三端独立迁移继续绑定到若依后台角色体系。
- 多个 split seed 与 `seller_buyer_management_seed.sql` 复用同一批 seller/buyer 管理端 `sys_menu.menu_id`，如果关键字段不一致，会产生执行顺序覆盖风险。

## 已修复问题

- `react-ui/scripts/verify-three-terminal.mjs` 增加 framework、system support、seller/buyer portal product 测试清单和 report 目录检查，防止关键测试静默漏跑。
- `/Portal/DirectLogin` 同步修复 TS/JS 入口，支持从 hash route query、hash `&` 参数和旧 search query 读取 `directLoginToken`。
- `PortalDirectLoginSupport` 生成免密 URL 时根据 hash route 是否已有 query/参数选择 `?` 或 `&`，避免 token 拼接到错误位置。
- `check-portal-token-isolation.mjs` 增加 direct-login hash route token 解析静态 guard。
- `20260604_three_terminal_isolation_migration.sql` 移除默认 `sys_role` seller/buyer 更新，只保留三端独立表迁移。
- 新增 `20260606_legacy_disable_sys_seller_buyer_roles.sql`，作为可选 legacy 清理脚本；只有确认目标库历史上存在混用 `sys_role` 的 seller/buyer 角色且需要停用时才执行。
- `TerminalSqlIsolationContractTest` 增加默认隔离迁移不得更新 legacy `sys_role` seller/buyer 的合同测试。
- `StandalonePartnerSeedMenuContractTest` 增加 seller/buyer 管理端菜单重复 seed 一致性测试，锁定 `menu_id`、父子关系、路由、权限、类型、显示状态等关键字段。

## 残留问题

- 本轮没有执行远程数据库 DDL/DML；新增 legacy SQL 只是脚本文件，没有回放到任何数据库。
- 三端物理前端拆分仍未开始，当前仍以 `react-ui/` 作为管理端验证入口。
- 本轮没有浏览器运行态验收，符合用户当前要求；UI 细调继续作为 P2 记录，不阻塞。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalSqlIsolationContractTest,StandalonePartnerSeedMenuContractTest,PortalDirectLoginSupportTest" test`
  - 通过：`Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`
  - 通过：`Portal token isolation guard passed.`
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过：前端 guard、`tsc`、system/framework/seller/buyer 后端合同和单测全部通过，最终输出 `three-terminal verification passed.`

## 未验证原因

- 未做浏览器/截图/DOM 验收：用户已明确当前无需浏览器运行态验收和截图/DOM 检测。
- 未执行数据库 DDL/DML：本轮只是脚本与合同测试加固；数据库实际回放需要单独按变更确认流程执行。

## 权限检查结果

- 本轮新增的 direct-login hash guard 与 `PortalDirectLoginSupportTest` 覆盖免密 token 生成和解析边界。
- `verify:three-terminal` 继续覆盖 seller/buyer 管理端权限、端内 portal 权限、菜单 seed、SQL 隔离和路由归属。
- 默认隔离迁移已通过合同测试确认不再更新 legacy `sys_role` seller/buyer。

## 字典/选项复用检查结果

- 本轮没有新增字典或业务选项。

## 复用台账检查结果

- 本轮没有新增可复用业务组件或后端公共 Service；主要是验证入口、SQL seed 和免密登录边界加固。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 7 changed files`，`Modified: 7 - 271 nodes`。

## 大文件合理性判断结果

- 本轮新增/修改文件职责单一，未触发 300 行以上拆分判断。
- `StandalonePartnerSeedMenuContractTest` 增加解析逻辑后仍保持“管理端菜单 seed 合同测试”单一职责。

## 重复代码检查结果

- seller/buyer 同构检查继续走同一套 `verify:three-terminal` 聚合验证。
- direct-login TS/JS 双入口属于当前前端构建产物兼容需要，逻辑保持一致；后续若确认只保留 TS 源，可单独清理 JS 副本。

## 子 Agent 使用

- 本轮按用户要求优先使用 `gpt-5.3-codex-spark` 启动 6 个子 Agent。
- 已采纳 P1：framework/system 测试入口漏跑、direct-login hash route query 解析、默认隔离迁移更新 legacy `sys_role`、terminal 管理端菜单 seed 重复定义一致性。
- 其余返回项未发现 P0/P1，或属于 P2 记录，不阻塞本轮收口。
