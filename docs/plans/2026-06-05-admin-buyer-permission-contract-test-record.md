# 管理端买家权限契约守卫执行记录

## 背景

当前开发方向以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为准：管理端保留若依 `sys_*` 控制面，卖家端和买家端账号、权限、菜单、部门、日志、会话独立。

本轮延续“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏。在卖家管理端权限契约守卫通过后，只复制同构守卫到买家管理端，不改 UI、不改接口行为、不执行 SQL。

## 本次改动

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java`。
- 测试扫描 `buyer` 模块下所有 `AdminBuyer*Controller.java`。
- 校验买家管理端 controller 必须使用 `/buyer/admin` 路由前缀。
- 校验买家管理端 handler 必须声明 `@PreAuthorize("@ss.hasPermi('buyer:admin:...')")`。
- 校验 controller 中声明的 `buyer:admin:*` 权限必须存在于 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`。
- 校验 `POST` / `PUT` / `DELETE` / `PATCH` 等变更类管理端操作必须声明 `@Log`。
- 校验买家管理端 controller 不得使用端内 `@Anonymous` / `@PortalPreAuthorize` / `@PortalLog`。
- 更新 `docs/architecture/reuse-ledger.md`，将“卖家管理端权限契约守卫”扩展为“卖家/买家管理端权限契约守卫”。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system -Dtest=BuyerAdminPermissionContractTest test
```

验证结果：

- `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

卖家/买家组合验证：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system "-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest" test
```

- `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

模块回归：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system test
```

- `Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

说明：曾执行未加引号的 `-Dtest=SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest`，PowerShell 将逗号解析为参数分隔导致命令解析失败；已用引号重跑并通过，非代码问题。

## 未验证原因

- 本切片是源码级契约测试，不启动后端。
- 本切片不连接远程 MySQL / Redis。
- 本切片不做浏览器 UI 验收。
- 本切片不做低权限账号的按钮隐藏和接口拒绝验证，后续需要单独切片处理。

## 权限检查结果

- 当前买家管理端 Admin Controller 的权限前缀符合 `buyer:admin:*`。
- 当前买家管理端 controller 中使用到的权限点均能在 `seller_buyer_management_seed.sql` 中找到。
- 当前买家管理端变更类操作具备 `@Log`。
- 卖家/买家管理端权限契约现在均有自动化守卫。

## 字典/选项复用检查结果

- 本切片未新增字典、字段选项或前端下拉。

## 复用台账检查结果

- 已在 `docs/architecture/reuse-ledger.md` 将守卫条目扩展为“卖家/买家管理端权限契约守卫”。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；代码变更同步时输出 `Synced 1 changed files`，文档回填后最终复跑输出 `Already up to date`。

## 大文件合理性判断结果

- 新增测试文件职责单一，只做买家管理端权限契约扫描，未触发大文件拆分风险。
- 当前卖家/买家守卫存在同构测试代码，符合本轮“卖家验收后复制买家”的推进方式；后续若继续增加第三类同构管理端守卫，再评估是否抽公共 helper。

## 重复代码检查结果

- 买家测试按卖家已验收模板复制，只替换 terminal、controller 路径、权限前缀和 seed 权限集合。
- 本轮未抽公共基类，避免把买家复制切片扩大成测试框架重构。

## 残留问题

- 管理端低权限账号负向验证尚未做。
- 免密票据 `tokenHash` 不应返回给管理端列表的脱敏守卫尚未做。
- `PortalPreAuthorizeAspect` / `PortalLogAspect` 执行级测试尚未做。
- 审计详情 UI 字段补齐尚未做。
