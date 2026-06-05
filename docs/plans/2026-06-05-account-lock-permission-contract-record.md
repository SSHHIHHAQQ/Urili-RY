# 2026-06-05 账号锁定权限契约补强记录

## 目标范围

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。

本切片只补账号锁定/解锁权限契约测试，不改后端业务实现、不改前端页面布局、不执行 SQL、不连接远程 MySQL / Redis。

## 已修复问题

- `SellerAdminPermissionContractTest` 增加卖家账号 `lockAccount` / `unlockAccount` 必须使用 `seller:admin:account:lock` 的断言。
- `BuyerAdminPermissionContractTest` 按卖家模板复制买家账号 `lockAccount` / `unlockAccount` 必须使用 `buyer:admin:account:lock` 的断言。
- `AdminAccountPermissionUiContractTest` 增加 seller/buyer 页面配置必须声明 `accountPermissions.lock`。
- `AdminAccountPermissionUiContractTest` 增加共享账号弹窗必须通过 `access.hasPerms(accountPermissions.lock...)` 控制锁定/解锁入口。
- `PermissionServiceAccountPermissionTest` 增加 `seller:admin:account:lock` / `buyer:admin:account:lock` 权限矩阵，防止主体权限、角色权限或跨端账号权限误授权锁定/解锁。
- 更新 `docs/architecture/reuse-ledger.md` 和 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`。

## 新增问题

- 无。

## 残留问题

- 本切片没有新增浏览器验收；seller/buyer 锁定低权限浏览器负向验收已由前序记录覆盖。
- 本切片没有做远程 SQL 差异审计；账号锁定权限点和字段已在前序 seller/buyer 锁定执行记录中验证。

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
- `ruoyi-framework` 权限服务测试：`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- `npm run guard:partner-management`：通过，`Partner management template guard passed.`。
- 首次运行 `ruoyi-system` 命令时未给 `-Dtest` 参数加引号，PowerShell 将逗号解析为参数分隔并报错；已用引号修正后通过，属于命令写法问题，不是代码失败。

## 未验证原因

- 本切片不启动后端，因为只补源码级契约测试。
- 本切片不连接远程 MySQL / Redis，因为未新增或修改 DDL / DML。
- 本切片不做浏览器 smoke，因为前序 seller/buyer 低权限锁定验收已经覆盖运行时显隐和后端拒绝；本轮只把该能力固化进可复跑契约测试。

## 权限检查结果

- 卖家 `lockAccount` / `unlockAccount` 必须绑定 `seller:admin:account:lock`。
- 买家 `lockAccount` / `unlockAccount` 必须绑定 `buyer:admin:account:lock`。
- 管理端 UI 必须通过 `accountPermissions.lock` 和 `access.hasPerms(...)` 控制锁定/解锁入口。
- 若只拥有 `*:admin:query`、`*:admin:role:*` 或其它非账号锁定权限，不能通过 `PermissionService.hasPermi("*:admin:account:lock")`。

## 字典/选项复用检查结果

- 本切片未新增字典、字段选项或前端下拉。

## 复用台账检查结果

- 已在 `docs/architecture/reuse-ledger.md` 的 `PartnerAccountModal / PartnerAccountRoleModal` 段落登记账号锁定权限契约守卫。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 4 changed files`，退出码 `0`。
- 文档回填后复跑 `codegraph sync .`：通过，输出 `Already up to date`，退出码 `0`。

## 大文件合理性判断结果

- `SellerAdminPermissionContractTest.java`：`243` 行，职责单一。
- `BuyerAdminPermissionContractTest.java`：`243` 行，职责单一。
- `AdminAccountPermissionUiContractTest.java`：`86` 行，职责单一。
- `PermissionServiceAccountPermissionTest.java`：`176` 行，职责单一。
- 本切片没有触发 300 行以上新增或修改文件拆分判断。

## 重复代码检查结果

- seller 和 buyer 后端契约测试保持同构结构，是为了明确双端独立权限前缀，不在本切片抽公共基类。
- 前端 UI 契约仍通过共享 `AdminAccountPermissionUiContractTest` 同时覆盖 seller/buyer，未复制页面逻辑。

## 当前判断

- 卖家账号锁定模板和买家复制模板现在不仅有运行时验收，也有源码级契约测试兜底。
- 后续修改账号弹窗、页面配置、controller 权限或权限服务矩阵时，`*:admin:account:lock` 漏配、串端或误复用主体权限会被定向测试发现。
