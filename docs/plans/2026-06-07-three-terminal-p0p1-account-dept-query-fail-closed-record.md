# 2026-06-07 三端 P0/P1 快速推进：账号弹窗部门权限 Fail-Closed 记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按当前快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 历史记录（已过期口径）：本轮按用户最新要求优先尝试 GPT-5.3 Codex；实际命中额度限制，提示可用时间为 `2026-06-13 01:59`，因此降级使用 `gpt-5.4`。
- 已开启并关闭 6 个 `gpt-5.4` 只读扫描子 Agent，用于并行扫描 SQL 授权、静态路由 guard、账号弹窗权限、旧 DDL 可重放、登录日志 actor、操作日志结构化审计。
- 本切片采纳账号弹窗权限扫描结论：账号新增/编辑不能因为缺少部门查询权限而加载空部门树或形成半可用状态；修复方式选择字段级 fail-closed，不扩大到隐藏新增/编辑入口。

## 新增问题

- `PartnerAccountModal` 的账号列表、新增和编辑权限与部门树查询权限没有完全解耦。
- 当用户具备账号新增或编辑权限，但缺少 `seller:admin:dept:query` / `buyer:admin:dept:query` 时，弹窗仍可能进入可编辑态，部门字段缺少明确的 fail-closed 表达。

## 已修复问题

- 账号弹窗新增 `canQueryDept = access.hasPerms(`${permPrefix}:dept:query`)` 作为部门树专属权限判断。
- 部门树请求保持 `if (!partnerId || !canQueryDept) return;`，缺少部门查询权限时不请求部门树接口。
- 部门 `TreeSelect` 在缺少部门查询权限时禁用，并显示 `无部门查询权限`。
- 新增和编辑按钮继续只由账号新增/编辑权限控制，不把部门查询权限错误提升为账号操作入口权限。
- `react-ui/scripts/check-partner-management-template.mjs` 已补账号弹窗部门权限 fail-closed guard。
- `AdminAccountPermissionUiContractTest` 已补前端权限契约断言，覆盖 `canQueryDept`、部门树请求 guard、字段禁用和提示文案。
- 已更新复用台账：`docs/architecture/reuse-ledger.md`。

## 验证命令

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\check-partner-management-template.mjs`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=AdminAccountPermissionUiContractTest" test`：通过，`1` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 `3` 个 suite / `9` 个测试通过，后端 ruoyi-system `100`、ruoyi-framework `15`、product `1`、seller `80`、buyer `81` 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；结果为 `Synced 4 changed files`，`Modified: 4 - 138 nodes in 616ms`。

## 未验证原因

- 未做浏览器运行态验收、截图、DOM 检测或 UI 细调；这是用户明确要求的快速推进边界。
- 未执行远程 MySQL DDL/DML，未读取或写入 Redis；本切片只涉及前端权限 guard、前端脚本 guard 和后端契约测试。
- 未启动或重启后端；本切片无需运行态后端验证。

## 权限检查结果

- 本轮未新增后端接口权限标识。
- 管理端账号弹窗中，账号列表、账号新增、账号编辑仍分别由账号权限控制。
- 部门字段只在具备 `dept:query` 时加载部门树；缺少权限时字段禁用并提示原因。
- 该规则适用于 seller/buyer 同构管理页面，权限前缀仍按 `seller:admin:*` / `buyer:admin:*` 隔离。

## 字典/选项复用检查结果

- 本轮未新增字典、选项或状态枚举。
- 部门树仍复用当前 `getDeptTree(partnerId)` service 和既有 `TreeSelect` 数据结构。

## 复用台账检查结果

- 已在 `docs/architecture/reuse-ledger.md` 增加“账号弹窗部门权限 Fail-Closed 模板”。
- 后续新增同构弹窗字段时，额外查询权限应落在字段级 guard，不应错误扩大为整个新增/编辑入口的权限条件。

## CodeGraph 更新结果

- `codegraph sync .` 已执行并通过；结果为 `Synced 4 changed files`，`Modified: 4 - 138 nodes in 616ms`。

## 大文件合理性判断结果

- `PartnerAccountModal.tsx` 属于既有共享账号弹窗模板，文件较大但本轮只补字段级权限 guard 和提示文案；未新增新的职责块。
- 本轮不拆分该文件，避免在 P0/P1 快速模式中引入 UI 结构性重构。

## 重复代码检查结果

- seller/buyer 管理端账号弹窗共用 `PartnerAccountModal` 模板，本轮只改共享组件。
- `PartnerAccountModal.js` 是现有同目录 JS 镜像文件，本轮按当前项目双文件现状同步，避免 TS/JS 行为分叉。

## 残留问题

- 静态 `/seller` / `/buyer` 路由 fallback guard 已由 `docs/plans/2026-06-07-three-terminal-p0p1-static-partner-route-guard-record.md` 的回归保护补强收口。
- `20260606_admin_partner_role_menu_grant.sql` wildcard 授权 P1 已由后续白名单/合同检查点收口为明确白名单。
- 旧 DDL 裸 `ALTER TABLE ADD COLUMN` 可重放风险仍需分批收口。
- 管理端强制踢出、重置密码等登录日志 actor 结构化字段仍需后续补齐。
- 端内操作日志直接登录的结构化审计字段涉及 DDL，必须先走 Markdown 方案确认。
