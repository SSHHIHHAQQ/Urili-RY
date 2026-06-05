# 2026-06-06 端内权限菜单树测试补强记录

## 目标范围

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。

本轮只处理一类问题：补强 seller/buyer 端 `selectPortalMenuTree(...)` 的 service 层自动化守卫。本轮不改生产代码、不改前端、不执行 SQL、不连接远程 MySQL / Redis。

## 已修复问题

- 新增 `SellerPortalPermissionServiceImplMenuTreeTest`，从 PortalAccess 测试中迁入 seller 离线菜单树拒绝用例。
- 按 seller 标准模板新增在线菜单树正向用例：必须先校验 DB session 在线，再用当前 `sellerId/accountId` 查询 `selectSellerAccountMenuList(...)`，并返回 `PortalPermissionSupport.buildMenuTree(...)` 形成的父子树。
- 新增 `BuyerPortalPermissionServiceImplMenuTreeTest`，按 seller 模板同构复制，只替换 buyer 领域对象、mapper、service、terminal、token 和文案。
- `SellerPortalPermissionServiceImplPortalAccessTest` / `BuyerPortalPermissionServiceImplPortalAccessTest` 只保留权限信息和 `selectPermissions(...)` 访问测试，不再混入菜单树 fake。

## 新增问题

- 无新增已知问题。

## 残留问题

- `SellerPortalPermissionServiceImplPortalAccessTest` / `BuyerPortalPermissionServiceImplPortalAccessTest` 当前各 456 行，超过 400 行自检阈值但低于 500 行；职责仍集中在端内访问、会话守卫和权限信息返回，暂不继续拆。
- `SellerPortalPermissionServiceImplMenuTreeTest` / `BuyerPortalPermissionServiceImplMenuTreeTest` 当前各 338 行，超过 300 行自检阈值；职责集中在菜单树读取和在线会话校验，暂不继续拆。

## 权限检查结果

- 本轮不新增权限点、不新增菜单、不改 `sys_menu` 或端内 `seller_menu` / `buyer_menu` seed。
- 本轮固定的是 service 层读取契约：菜单树读取必须经过端内在线会话校验，并从 `seller_*` / `buyer_*` 端内 mapper 查询，不得走管理端 `sys_menu`。

## 字典/选项复用检查结果

- 不涉及字典、选项、前端下拉或 code/label 变更。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记菜单树测试分文件规则和 `selectPortalMenuTree(...)` 的端内 mapper 读取契约。

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

- seller/buyer 菜单树测试继续保持镜像模板重复，符合三端隔离和“卖家先成模板，买家同构复制”的推进规则。
- 本轮不抽跨端测试基类，避免把 seller/buyer 端内模块重新耦合。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl seller -am "-Dtest=SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplPortalAccessTest,SellerPortalPermissionServiceImplMenuTreeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl buyer -am "-Dtest=BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplPortalAccessTest,BuyerPortalPermissionServiceImplMenuTreeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test

cd E:\Urili-Ruoyi
rg -n "[ \t]+$" RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplPortalAccessTest.java RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplMenuTreeTest.java RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplPortalAccessTest.java RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplMenuTreeTest.java
git diff --check -- RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplPortalAccessTest.java RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplPortalAccessTest.java
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
- 尾随空白检查通过：`rg -n "[ \t]+$" ...` 返回码 `1`，表示未发现匹配。
- `git diff --check -- <本轮触碰 tracked 测试文件>`：通过。

## 未验证原因

- 未做 HTTP smoke：本轮只调整 service 单测结构，单测已经覆盖目标行为。
- 未做前端验证：本轮不改前端。
- 未做数据库验证：本轮不改 SQL 或远程数据。

## CodeGraph 更新结果

- 已执行 `cd E:\Urili-Ruoyi; codegraph sync .`。
- 输出：`Already up to date`，命令退出码 `0`。
