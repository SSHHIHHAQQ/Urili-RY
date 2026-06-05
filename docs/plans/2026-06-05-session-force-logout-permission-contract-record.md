# 2026-06-05 会话与强制踢出权限契约补强记录

## 目标范围

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。

本切片只补管理端会话列表 / 强制踢出的权限契约测试，不改后端业务实现、不改前端页面布局、不执行 SQL、不连接远程 MySQL / Redis。

## 已修复问题

- `SellerAdminPermissionContractTest` 增加卖家管理端 `sessions`、`accountSessions`、`forceLogoutSeller`、`forceLogoutSellerAccount` 必须使用 `seller:admin:forceLogout` 的断言。
- `BuyerAdminPermissionContractTest` 按卖家模板增加买家管理端 `sessions`、`accountSessions`、`forceLogoutBuyer`、`forceLogoutBuyerAccount` 必须使用 `buyer:admin:forceLogout` 的断言。
- `AdminAccountPermissionUiContractTest` 增加 seller/buyer 页面必须注入主体级会话、账号级会话、主体级强踢、账号级强踢 service 的断言。
- `AdminAccountPermissionUiContractTest` 增加共享主体列表和账号弹窗必须通过 `access.hasPerms(\`${permPrefix}:forceLogout\`)` 控制“会话 / 强制踢出”入口的断言。
- `PermissionServiceAccountPermissionTest` 增加 `seller:admin:forceLogout` / `buyer:admin:forceLogout` 权限矩阵，防止主体权限、角色权限、账号权限或跨端权限误授权会话强踢。
- 更新 `docs/architecture/reuse-ledger.md` 和 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`。

## 新增问题

- 无。

## 残留问题

- 本切片没有新增运行时低权限验收；后续可单独用低权限角色验证主体行和账号行均不显示“会话 / 强制踢出”，且后端接口返回拒绝。
- seller/buyer 会话列表和强踢 service 已有间接测试和历史 HTTP smoke；后续可单独补 service 级直接单测，覆盖主体级 list、账号级 list、主体级强踢、账号级强踢和 Redis token terminal scope 删除。
- 当前会话列表查看和强制踢出共用 `*:admin:forceLogout`；如果后续需要“只能查看会话，不能踢出”的管理员角色，必须另起权限、seed、前后端显隐和运行库 SQL 切片。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,AdminAccountPermissionUiContractTest" test
mvn -pl ruoyi-framework -Dtest=PermissionServiceAccountPermissionTest test

cd E:\Urili-Ruoyi\react-ui
npm run guard:partner-management
```

## 验证结果

- `ruoyi-system` 定向契约测试：`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `ruoyi-framework` 权限服务测试：`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `npm run guard:partner-management`：通过，`Partner management template guard passed.`。

## 未验证原因

- 本切片不启动后端，因为只补源码级契约测试。
- 本切片不连接远程 MySQL / Redis，因为未新增或修改 DDL / DML。
- 本切片不做浏览器 smoke，因为会话 UI 的 seller/buyer 浏览器验收已有前序记录；本轮只把权限契约固化进可复跑测试。

## 权限检查结果

- 卖家管理端会话列表和强制踢出必须绑定 `seller:admin:forceLogout`。
- 买家管理端会话列表和强制踢出必须绑定 `buyer:admin:forceLogout`。
- 管理端 UI 必须通过 `access.hasPerms(\`${permPrefix}:forceLogout\`)` 控制主体行和账号行的“会话 / 强制踢出”入口。
- 若只拥有 `*:admin:query`、`*:admin:role:*`、`*:admin:account:*` 等非强踢权限，不能通过 `PermissionService.hasPermi("*:admin:forceLogout")`。

## 字典/选项复用检查结果

- 本切片未新增字典、字段选项或前端下拉。

## 复用台账检查结果

- 已在 `docs/architecture/reuse-ledger.md` 的“管理端端内会话列表查询接口”段落登记会话与强制踢出权限契约守卫。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 16 changed files`，退出码 `0`。
- 文档回填后复跑 `codegraph sync .`：通过，输出 `Synced 1 changed files`，退出码 `0`。

## 大文件合理性判断结果

- `SellerAdminPermissionContractTest.java`：`247` 行，职责单一。
- `BuyerAdminPermissionContractTest.java`：`247` 行，职责单一。
- `AdminAccountPermissionUiContractTest.java`：`99` 行，职责单一。
- `PermissionServiceAccountPermissionTest.java`：`191` 行，职责单一。
- 本切片没有触发 300 行以上新增或修改文件拆分判断。

## 重复代码检查结果

- seller 和 buyer 后端契约测试保持同构结构，是为了明确双端独立权限前缀，不在本切片抽公共基类。
- 前端 UI 契约仍通过共享 `AdminAccountPermissionUiContractTest` 同时覆盖 seller/buyer，未复制页面逻辑。

## 当前判断

- 管理端会话列表和强制踢出已经从“代码已接线”升级为“源码级契约受保护”。
- 后续修改 `AdminSellerController` / `AdminBuyerController`、Seller/Buyer 页面配置、`PartnerManagementPage` 或 `PartnerAccountModal` 时，`*:admin:forceLogout` 漏配、串端或误复用账号权限会被定向测试发现。
