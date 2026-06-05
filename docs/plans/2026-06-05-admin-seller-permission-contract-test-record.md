# 管理端卖家权限契约守卫执行记录

## 背景

当前开发方向以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为准：管理端保留若依 `sys_*` 控制面，卖家端和买家端账号、权限、菜单、部门、日志、会话独立。

本轮按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进，只补卖家管理端权限契约守卫，不复制买家、不改 UI、不改接口行为、不执行 SQL。

## 本次改动

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java`。
- 测试扫描 `seller` 模块下所有 `AdminSeller*Controller.java`。
- 校验卖家管理端 controller 必须使用 `/seller/admin` 路由前缀。
- 校验卖家管理端 handler 必须声明 `@PreAuthorize("@ss.hasPermi('seller:admin:...')")`。
- 校验 controller 中声明的 `seller:admin:*` 权限必须存在于 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`。
- 校验 `POST` / `PUT` / `DELETE` / `PATCH` 等变更类管理端操作必须声明 `@Log`。
- 校验卖家管理端 controller 不得使用端内 `@Anonymous` / `@PortalPreAuthorize` / `@PortalLog`。
- 更新 `docs/architecture/reuse-ledger.md`，登记该测试作为卖家管理端权限契约守卫。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system -Dtest=SellerAdminPermissionContractTest test
```

验证结果：

- `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

补充回归：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system test
```

- `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

空白检查：

```powershell
cd E:\Urili-Ruoyi
git diff --check -- RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-admin-seller-permission-contract-test-record.md
```

- 通过；仅提示 `docs/architecture/reuse-ledger.md` 和 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 后续 Git 触碰时 LF/CRLF 会归一化。

## 未验证原因

- 本切片是源码级契约测试，不启动后端。
- 本切片不连接远程 MySQL / Redis。
- 本切片不做浏览器 UI 验收。
- 本切片不做低权限账号的按钮隐藏和接口拒绝验证，后续需要单独切片处理。

## 权限检查结果

- 当前卖家管理端 Admin Controller 的权限前缀符合 `seller:admin:*`。
- 当前卖家管理端 controller 中使用到的权限点均能在 `seller_buyer_management_seed.sql` 中找到。
- 当前卖家管理端变更类操作具备 `@Log`。
- 当前测试只覆盖卖家管理端，不覆盖买家；买家需要在卖家守卫验收后按同构规则复制。

## 字典/选项复用检查结果

- 本切片未新增字典、字段选项或前端下拉。

## 复用台账检查结果

- 已在 `docs/architecture/reuse-ledger.md` 新增“卖家管理端权限契约守卫”条目。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；代码变更同步时输出 `Synced 2 changed files`，文档回填后最终复跑输出 `Already up to date`。

## 大文件合理性判断结果

- 新增测试文件职责单一，只做卖家管理端权限契约扫描，未触发大文件拆分风险。

## 重复代码检查结果

- 测试写法沿用现有 `TerminalRouteOwnershipTest` 的源码扫描风格。
- 暂未抽公共测试基类；等买家守卫复制后，如重复明显，再按同构规则抽取 helper。

## 残留问题

- 买家管理端权限契约守卫尚未复制。
- 管理端低权限账号负向验证尚未做。
- 免密票据列表不返回 `tokenHash` 的脱敏守卫尚未做。
- `PortalPreAuthorizeAspect` / `PortalLogAspect` 执行级测试尚未做。
