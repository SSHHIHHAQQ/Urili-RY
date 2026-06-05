# 2026-06-06 端内权限 Service 测试拆分执行记录

## 目标范围

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。

本轮只处理一类问题：把 seller/buyer 端 `PortalPermissionServiceImpl` 测试从 500 行以上的大文件拆成“角色绑定写操作”“端内访问/权限读取”和“端内菜单树读取”三个测试职责。本轮不改生产代码、不改前端、不执行 SQL、不连接远程 MySQL / Redis。

## 已修复问题

- 新增 `SellerPortalPermissionServiceImplPortalAccessTest`，承载 seller 端 `selectPortalPermissionInfo(...)`、`selectPermissions(...)` 相关端内访问测试。
- 新增 `SellerPortalPermissionServiceImplMenuTreeTest`，承载 seller 端 `selectPortalMenuTree(...)` 离线拒绝和在线菜单树返回测试。
- `SellerPortalPermissionServiceImplTest` 收缩为角色绑定测试，只保留 4 个 `assignAccountRoles...` 用例和角色绑定 fake。
- 新增 `BuyerPortalPermissionServiceImplPortalAccessTest`，按 seller 模板同构复制，只替换 terminal、领域对象、mapper、service、token 和权限 code。
- 新增 `BuyerPortalPermissionServiceImplMenuTreeTest`，按 seller 菜单树模板同构复制，只替换 terminal、领域对象、mapper、service、token 和文案。
- `BuyerPortalPermissionServiceImplTest` 收缩为角色绑定测试，只保留 4 个 `assignAccountRoles...` 用例和角色绑定 fake。
- 原有 10 个 seller 测试行为和 10 个 buyer 测试行为均保留，并新增 seller/buyer 菜单树在线正向返回断言。
- 锁定账号访问测试继续断言具体错误文案，避免从“账号已锁定”退化成只要抛 `ServiceException` 即可。

## 新增问题

- 无新增已知问题。

## 残留问题

- `SellerPortalPermissionServiceImplPortalAccessTest` / `BuyerPortalPermissionServiceImplPortalAccessTest` 当前各 456 行，超过 400 行自检阈值但低于 500 行；职责集中在端内访问、会话守卫和权限信息返回，暂不继续拆。
- `SellerPortalPermissionServiceImplMenuTreeTest` / `BuyerPortalPermissionServiceImplMenuTreeTest` 当前各 338 行，只承载菜单树读取和在线会话校验，暂不继续拆。

## 权限检查结果

- 本轮不新增权限点、不新增菜单、不改 `sys_menu` 或端内 `seller_menu` / `buyer_menu` seed。
- 测试拆分不改变 `@PortalPreAuthorize`、`PortalPermissionChecker`、`SellerPortalPermissionServiceImpl` 或 `BuyerPortalPermissionServiceImpl` 的生产权限链路。

## 字典/选项复用检查结果

- 不涉及字典、选项、前端下拉或 code/label 变更。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记权限 service 测试结构规则：角色绑定写操作、端内访问/权限读取、端内菜单树读取测试分文件维护。

## 数据源与 SQL 检查

- 本轮未读取 `.env.local`。
- 本轮未连接远程 MySQL / Redis。
- 本轮未执行 DDL/DML。
- 本轮不需要新增 SQL 或 seed。

## 大文件合理性判断结果

- `SellerPortalPermissionServiceImplTest`：321 行，超过 300 行阈值；职责集中在角色绑定写操作，暂不继续拆。
- `SellerPortalPermissionServiceImplPortalAccessTest`：456 行，超过 400 行阈值；职责集中在端内访问、会话守卫和权限信息返回，暂不继续拆。
- `SellerPortalPermissionServiceImplMenuTreeTest`：338 行，超过 300 行阈值；职责集中在菜单树读取和在线会话校验，暂不继续拆。
- `BuyerPortalPermissionServiceImplTest`：321 行，超过 300 行阈值；职责集中在角色绑定写操作，暂不继续拆。
- `BuyerPortalPermissionServiceImplPortalAccessTest`：456 行，超过 400 行阈值；职责集中在端内访问、会话守卫和权限信息返回，暂不继续拆。
- `BuyerPortalPermissionServiceImplMenuTreeTest`：338 行，超过 300 行阈值；职责集中在菜单树读取和在线会话校验，暂不继续拆。

## 重复代码检查结果

- seller/buyer 继续保持镜像测试模板重复，符合三端隔离和“卖家先成模板，买家同构复制”的推进规则。
- 本轮不抽跨端测试基类，避免把 seller/buyer 端内模块重新耦合。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl seller -am "-Dtest=SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplPortalAccessTest,SellerPortalPermissionServiceImplMenuTreeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl buyer -am "-Dtest=BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplPortalAccessTest,BuyerPortalPermissionServiceImplMenuTreeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test

cd E:\Urili-Ruoyi
git diff --check -- RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplPortalAccessTest.java RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplPortalAccessTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-06-terminal-permission-service-test-split-record.md
rg -n "[ \t]+$" RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplPortalAccessTest.java RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplMenuTreeTest.java RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplPortalAccessTest.java RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplMenuTreeTest.java
codegraph sync .
```

## 验证结果

- `SellerPortalPermissionServiceImplMenuTreeTest`：通过，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。
- `SellerPortalPermissionServiceImplPortalAccessTest`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `SellerPortalPermissionServiceImplTest`：通过，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- seller 合计：通过，`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`。
- `BuyerPortalPermissionServiceImplMenuTreeTest`：通过，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。
- `BuyerPortalPermissionServiceImplPortalAccessTest`：通过，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `BuyerPortalPermissionServiceImplTest`：通过，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。
- buyer 合计：通过，`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`。

## 未验证原因

- 未做 HTTP smoke：本轮只调整 service 单测结构，单测已经覆盖目标行为。
- 未做前端验证：本轮不改前端。
- 未做数据库验证：本轮不改 SQL 或远程数据。

## CodeGraph 更新结果

- 已执行 `cd E:\Urili-Ruoyi; codegraph sync .`。
- 测试拆分后输出：`Synced 2 changed files`，命令退出码 `0`。
- 复用台账修正后最近一次输出：`Synced 1 changed files`，命令退出码 `0`。
