# 2026-06-05 端内权限 Service 会话守卫与权限信息返回执行记录

## 目标范围

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。

本轮只处理 seller/buyer 端 `@PortalPreAuthorize` 背后的权限 service 会话守卫与权限信息返回测试：session 形态异常、terminal 错误、`subjectId/accountId/tokenId` 缺失或 DB session 不在线时必须 fail-closed；在线 session 的 `roles` / `permissions` 必须来自各自端内权限 mapper。本轮不新增接口、不改前端、不执行 SQL、不连接远程 MySQL / Redis。

## 已修复问题

- `SellerPortalPermissionServiceImpl.assertActiveSellerSession(...)` 增加入口 session 形态守卫。
- `BuyerPortalPermissionServiceImpl.assertActiveBuyerSession(...)` 按 seller 模板同构复制。
- `tokenId` 校验从非空收紧为非空白，空白 token 直接按登录失效处理。
- seller/buyer 权限 service 在账号锁定时拒绝继续读取端内权限。
- seller/buyer 单测补齐畸形 session、空白 token、DB session 离线和账号锁定负向场景。
- 畸形 session 测试使用“查了就失败”的 fake，固定 `BeforeLookup` 契约：不允许继续查询主体、账号或 DB session。
- seller/buyer 单测补齐 `selectPermissions(...)` 和 `selectPortalMenuTree(...)` 入口的 fail-closed 回归测试，固定三个 public 权限 service 入口都复用同一会话守卫。
- seller/buyer mapper fake 记录 `countOnline*Session` 的 subject/account/token 入参，固定 DB session 查询不能漏传或错传 token。
- seller 先补齐 `selectPortalPermissionInfo(...)` 正向权限信息返回模板，固定在线 session 会返回 terminal、主体、账号、用户名、昵称、角色和权限。
- buyer 按 seller 模板同构复制，只替换 terminal、领域对象、mapper、service、权限 code 和文案。
- seller/buyer recording permission mapper 支持返回端内 `roleKeys` 与 `permissions`，并记录权限查询入参；权限字符串支持逗号拆分后进入返回集合。

## 新增问题

- 无新增已知问题。

## 残留问题

- 当前只补权限 service 的 session 守卫和 `selectPortalPermissionInfo(...)` 正向权限信息返回；未扩展到更多端内业务 service。
- `selectPortalMenuTree(...)` 目前只覆盖离线拒绝，未补菜单树正向返回断言。
- 两个权限 service 测试文件已超过 500 行；本轮为避免混入测试结构重排，暂不拆分，下一刀继续扩展前应优先做测试文件拆分。
- 后续已在 `docs/plans/2026-06-06-terminal-permission-service-test-split-record.md` 完成测试拆分：角色绑定测试保留在 `SellerPortalPermissionServiceImplTest` / `BuyerPortalPermissionServiceImplTest`，端内访问和会话守卫测试迁入 `SellerPortalPermissionServiceImplPortalAccessTest` / `BuyerPortalPermissionServiceImplPortalAccessTest`。

## 权限检查结果

- 本轮不新增权限点、不新增菜单、不改 `sys_menu` 或端内 `seller_menu` / `buyer_menu` seed。
- seller/buyer 端受保护接口仍通过 `@PortalPreAuthorize` 进入 `PortalPermissionChecker`，再调用各自端内 `IPortalPermissionCheckService`。
- 权限 service 继续使用各自端内主体、账号、角色、菜单和 session 表，不复用管理端 `sys_user` / `sys_role` / `sys_menu`。

## 字典/选项复用检查结果

- 不涉及字典、选项、前端下拉或 code/label 变更。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md` 的 `PortalPreAuthorize / PortalPermissionChecker / PortalSessionContext` 段落。
- 已登记规则：seller/buyer 端权限 service 的三个 public 权限入口必须先校验 session 形态，再查主体、账号和 DB session；畸形 session 必须 401 且不得继续查库。
- 已登记规则：在线 session 的 `PortalPermissionInfo.roles` / `permissions` 必须来自各自端内 `seller_role/seller_menu` 或 `buyer_role/buyer_menu` 链路，不得回退到管理端 `sys_*` 权限。

## 数据源与 SQL 检查

- 本轮未读取 `.env.local`。
- 本轮未连接远程 MySQL / Redis。
- 本轮未执行 DDL/DML。
- 本轮不需要新增 SQL 或 seed。

## 大文件合理性判断结果

- `SellerPortalPermissionServiceImplTest` / `BuyerPortalPermissionServiceImplTest` 均为 529 行，超过 500 行自检阈值。当前文件已经同时覆盖角色绑定、会话守卫和权限信息返回，后续继续扩展前应拆成角色绑定测试与端内访问测试；本轮不拆的理由是当前切片只固定 seller 标准模板并同构复制 buyer，避免把行为覆盖和文件结构重排混成一刀。
- 后续拆分后的当前状态见 `docs/plans/2026-06-06-terminal-permission-service-test-split-record.md`：角色绑定测试类各 288 行，portal access 测试类各 423 行。
- seller/buyer 权限 service 均为端内权限读取模板文件，本轮只新增入口守卫方法，不引入新职责。

## 重复代码检查结果

- seller/buyer 本轮保持镜像模板重复，符合三端隔离和“卖家先成模板，买家同构复制”的当前推进规则。
- 本轮不抽跨端测试基类，避免把 seller/buyer 端内模块重新耦合在一起。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl seller -am "-Dtest=SellerPortalPermissionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl buyer -am "-Dtest=BuyerPortalPermissionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test

cd E:\Urili-Ruoyi
git diff --check -- RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplTest.java RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-terminal-permission-service-session-shape-guard-record.md
codegraph sync .
```

## 验证结果

- `SellerPortalPermissionServiceImplTest`：通过，`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。
- `BuyerPortalPermissionServiceImplTest`：通过，`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- <本轮触碰文件>`：通过，仅有 LF/CRLF 工作区换行提示。

## 未验证原因

- 未做 HTTP smoke：本轮是 service 级 fail-closed 守卫，单测已经覆盖目标行为；不需要启动后端或写远程运行库。
- 未做前端验证：本轮不改前端。

## CodeGraph 更新结果

- 已执行 `cd E:\Urili-Ruoyi; codegraph sync .`。
- 首次同步结果：`Synced 5 changed files`；文档回填后复跑结果：`Already up to date`。
- 本轮继续补齐 `selectPermissions(...)` / `selectPortalMenuTree(...)` 入口守卫后再次执行，结果：`Synced 2 changed files`，命令退出码 `0`。
- 本轮继续补齐 `selectPortalPermissionInfo(...)` 正向权限信息返回后再次执行，结果：`Synced 3 changed files`，命令退出码 `0`。
