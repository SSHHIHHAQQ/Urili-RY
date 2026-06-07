# 2026-06-07 P0/P1 快速推进：账号级审计入口记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 背景

- 管理端主体级审计入口已存在，账号行此前没有直接进入该账号审计视图的入口。
- 后端 seller/buyer 管理端日志接口已支持账号级查询：
  - 登录日志、操作日志：`subjectId + accountId`
  - 免密票据：`targetSubjectId + targetAccountId`
- 后端 service 已要求账号筛选必须同时带主体 ID，不允许裸账号 ID 反查主体。

## 子 Agent 结论

- 本轮使用并关闭 6 个 `gpt-5.4` 只读子 Agent。
- 前端结论：账号级审计入口应放在 `PartnerAccountModal` 账号行“更多”下拉，不新增独立页面或新权限点。
- 后端结论：现有接口和 mapper 已支持账号级日志和票据筛选，不需要新增后端接口。
- 权限结论：账号行“审计”入口用任一审计权限可见；登录日志、操作日志、免密票据 tab 继续按各自权限独立显隐。
- 验证结论：建议补静态 guard、Java UI 契约测试和轻量参数编排单测。

## 已完成

- `PartnerAccountModal.tsx` / `.js`
  - 账号行“更多”新增“审计”入口。
  - 新增 `auditModalOpen` / `auditAccount` 状态。
  - 入口显隐复用 `loginLog:list || operLog:list || ticket:list`，不新增 `accountPermissions.audit`。
  - 打开 `PartnerAuditModal` 时同时传当前主体和当前账号。
- `PartnerAuditModal.tsx` / `.js`
  - 新增可选 `account` 上下文。
  - 登录日志、操作日志请求固定追加 `subjectId + accountId`。
  - 免密票据请求固定追加 `targetSubjectId + targetAccountId`。
  - `buildAuditParams(...)` 在缺少主体 ID 时不会发送裸 `accountId` / `targetAccountId`。
  - 账号级审计时隐藏账号名、操作人、目标主体编号等身份类搜索项，只保留状态、时间等过滤。
  - 审计搜索缓存 key 按主体级/账号级作用域分开，避免浏览器旧筛选状态串作用域。
- `react-ui/tests/partner-audit-modal.test.ts`
  - 新增参数编排测试，覆盖日志账号级、票据账号级和裸账号 ID fail-safe。
- `check-partner-management-template.mjs`
  - 固定账号行审计入口、`PartnerAuditModal` 账号传参、审计列、敏感字段不渲染和 deny-by-default 文案。
- `AdminAccountPermissionUiContractTest`
  - 固定 TS/JS 双镜像中的账号审计入口和账号级审计参数/列/敏感字段契约。
- `verify-three-terminal.mjs`
  - 将 `partner-audit-modal.test.ts` 纳入三端验证清单。
- `docs/architecture/reuse-ledger.md`
  - 补充 `PartnerAuditModal` 账号级入口和固定作用域复用规则。
- `AGENTS.md`
  - 已检查，现有规则已包含“管理端登录日志、操作日志和免密票据审计列表按账号筛选时必须显式提供对应主体 ID”，本轮无需修改。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/partner-audit-modal.test.ts --runInBand`：通过，`1` 个 suite / `3` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=AdminAccountPermissionUiContractTest test`：通过，`1` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 `6` 个 suite / `29` 个测试通过，后端 ruoyi-system `132`、ruoyi-framework `15`、product `1`、seller `91`、buyer `92` 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；结果为 `Already up to date`。

注意：`verify:three-terminal` 结束后 Jest 仍提示存在 open handle，但测试和命令退出码均为通过；本轮未把它作为 P0/P1 阻塞项。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 残留 P1

- `20260604_three_terminal_isolation_migration.sql` 仍需把 legacy blocker 前移为真正 preflight，并用合同固定非事务化/半执行说明。
- `seller_buyer_management_seed.sql` 仍同时承担 fresh bootstrap 和全局增量修补职责，后续应拆分或增加 profile/freshness guard。
- portal `accounts/depts/roles`、商城商品读接口和 admin dept/role controller 可补更精确方法级权限合同。
- 后端可补 `operLog` 正向账号级查询测试和 admin 审计接口参数绑定测试，当前不阻塞本轮前端入口落地。
