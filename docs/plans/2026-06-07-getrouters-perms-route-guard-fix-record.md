# 2026-06-07 getRouters 权限字段与路由 Guard 修复记录

## 背景

前端三端路由 guard 收紧后，远程菜单页面必须带非空 `authority` 并命中当前用户权限才允许访问。登录后访问 `/inventory/source-warehouse-stock` 出现 `403 / Forbidden`。

## 新增问题

- `/getRouters` 返回的 `RouterVo` 没有输出 `sys_menu.perms`。
- `react-ui/src/services/session.ts` 会从远程菜单项读取 `item.perms` 并转换为 `authority`。
- 当前 guard 已按安全方向改为“空 authority fail-closed”，导致旧的后端路由 payload 缺口被暴露，页面被前端拒绝。

## 已修复问题

- `RouterVo` 增加 `perms` 字段和 getter/setter。
- `SysMenuServiceImpl.buildMenus(...)` 构建路由时写入 `menu.getPerms()`。
- 对菜单 frame 和内链子路由同步写入 `perms`，避免特殊路由丢权限。
- 新增 `SysMenuServiceImplTest.buildMenusCopiesMenuPermsToNestedRoutePayload`，固定嵌套页面菜单权限会进入路由 payload。
- 已重新打包 `ruoyi-admin.jar` 并重启本机 8080 后端。

## 残留问题

- 旧浏览器会话如果还缓存了不带 `authority` 的 `admin_remote_menu:admin`，需要退出重登或清空 sessionStorage 后重新拉菜单。
- 本次只修复若依管理端 `/getRouters` 权限字段输出，不改 seller/buyer 端内菜单权限模型。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SysMenuServiceImplTest" test`：通过，`1` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/remote-menu-route-guard.test.ts --runInBand`：通过，`1` 个 suite、`9` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -DskipTests package`：通过，完整后端 jar 重新打包成功。
- `cd E:\Urili-Ruoyi; .\start-backend-local.ps1 -Restart`：已启动新 jar，8080 监听进程为 `ruoyi-admin.jar`。
- Playwright 重新登录 `admin/admin123`：`/api/login`、`/api/getRouters`、`/api/getInfo` 均返回 200。
- Playwright 访问 `http://127.0.0.1:8001/inventory/source-warehouse-stock`：页面不再显示 403，来源仓库库存表格加载成功。
- Playwright 网络验证：`/api/integration/admin/source-warehouse-stocks/groups/list?pageNum=1&pageSize=20&inventoryScope=COMPREHENSIVE` 返回 200。

## 未验证原因

- 未执行远端 SQL、DDL 或 DML：本次修复仅涉及后端路由 DTO 和构建逻辑。
- 未验证无权限用户访问：本轮目标是恢复 admin 远程菜单权限 payload；无权限拒绝逻辑由既有前端 guard 测试覆盖。

## 权限检查结果

- 保留前端“空 authority 拒绝”的 fail-closed 行为。
- 通过后端 `/getRouters` 输出 `perms`，让前端 `authority` 能与 `/getInfo` 返回的权限集合匹配。
- 没有放宽 `RemoteMenuRouteGuard`，也没有新增通配权限。

## 字典/选项复用检查结果

- 本次未新增或修改字典、选项、状态映射。

## 复用台账检查结果

- 本次未新增公共组件、公共方法或 option catalog。
- 复用现有若依 `sys_menu.perms` 作为管理端菜单权限来源。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 3 changed files`，新增 `1`、修改 `2`，`117` nodes。

## 大文件合理性判断结果

- `RouterVo.java`、`SysMenuServiceImpl.java` 和新增测试均未达到拆分阈值。

## 重复代码检查结果

- 未复制业务逻辑。
- `perms` 透传沿用现有 `SysMenu` 到 `RouterVo` 的构建链路。
