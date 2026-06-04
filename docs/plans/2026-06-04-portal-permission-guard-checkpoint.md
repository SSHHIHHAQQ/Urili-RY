# 端内权限校验基础设施检查点

日期：2026-06-04

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

## 目标

在卖家端、买家端已经具备独立登录、token、角色、菜单和权限读取能力之后，补齐后端接口级权限校验基础设施，避免后续真实端内业务接口只做登录校验、不做端内权限校验。

## 已完成

- 新增 `@PortalPreAuthorize` 注解，用于标记卖家端、买家端接口的端类型和权限要求。
- 新增 `PortalPreAuthorizeAspect` 切面，在接口执行前统一进入端内权限校验。
- 新增 `PortalPermissionChecker`，统一完成：
  - 从请求 token 解析端内会话。
  - 校验请求端类型必须匹配 `seller` 或 `buyer`。
  - 调用对应端的权限服务读取权限集合。
  - 支持全部权限要求和任一权限要求。
  - 即使没有声明具体权限，也会触发主体和账号启用状态校验。
- 新增 `IPortalPermissionCheckService`，让卖家端、买家端权限服务以同一接口接入统一校验器。
- `SellerPortalPermissionServiceImpl` 已接入 `IPortalPermissionCheckService`，返回卖家端权限集合。
- `BuyerPortalPermissionServiceImpl` 已接入 `IPortalPermissionCheckService`，返回买家端权限集合。
- `PortalPermissionSupport` 补充权限匹配方法，支持若依超级权限 `*:*:*`。
- 新增 `PortalPermissionSupportTest`，覆盖空权限要求、全部权限、任一权限和超级权限匹配。
- `/seller/getInfo`、`/seller/getRouters` 已接入 `@PortalPreAuthorize(terminal = "seller")`。
- `/buyer/getInfo`、`/buyer/getRouters` 已接入 `@PortalPreAuthorize(terminal = "buyer")`。
- `PortalTokenSupport.requireSession` 登录失效返回码已明确为 `401`。
- `PortalPermissionChecker` 权限不足返回码已明确为 `403`。
- 新增 `PortalSessionContext`，用于在当前请求内复用已校验通过的 `PortalLoginSession`。
- `PortalPreAuthorizeAspect` 已把校验通过的端内会话写入 `PortalSessionContext`，并在请求结束时恢复或清理。
- `PortalLogAspect` 优先读取 `PortalSessionContext`，减少已挂权限注解接口的重复 token 解析。
- `/seller/getInfo`、`/seller/getRouters`、`/buyer/getInfo`、`/buyer/getRouters` 已改为从 `PortalSessionContext.requireSession(...)` 读取当前端内会话。

## 验证结果

- `mvn -pl ruoyi-system -DargLine="-Djdk.net.URLClassPath.disableClassPathURLCheck=true" test`：通过。
- `mvn -DskipTests compile`：通过。
- `mvn -DskipTests install`：通过。
- `.\start-backend-local.ps1 -Restart`：后端已通过本机脚本重启。
- `http://127.0.0.1:8080`：返回 HTTP 200。
- 管理端 `admin / admin123` 登录：成功。
- 卖家端免密登录后访问 `/seller/getInfo`：返回 `code=200`。
- 买家端免密登录后访问 `/buyer/getInfo`：返回 `code=200`。
- 无 token 访问 `/seller/getInfo`：返回 `code=401`。
- 卖家端 token 访问 `/seller/getRouters`：返回 `code=200`。
- 买家端 token 访问 `/buyer/getRouters`：返回 `code=200`。
- 卖家端 token 访问 `/buyer/getInfo`：返回 `code=401`。
- `PortalSessionContextTest`：3 条通过。

说明：Windows 当前 Maven Surefire classpath 需要追加 `-Djdk.net.URLClassPath.disableClassPathURLCheck=true`，否则 `ruoyi-system` 单测会出现 classpath URL 检查导致的类加载失败；这不是业务代码失败。

## 权限检查结果

- 管理端仍继续使用若依 `@PreAuthorize("@ss.hasPermi(...)")` 和 `sys_menu` 权限点。
- 卖家端、买家端后续真实业务接口应改用 `@PortalPreAuthorize`，不直接依赖 `sys_menu` / `sys_role` 判断端内权限。
- 当前 `getInfo` / `getRouters` 已读取端内角色、权限和菜单，并已接入 `@PortalPreAuthorize`；真实业务接口仍需逐接口接入该注解和 token 派生的数据范围。

## 复用台账检查结果

- 已同步更新 `docs/architecture/reuse-ledger.md` 中的端内权限校验复用规则。
- 后续不要在 Controller 或 Service 中重复手写 token 解析、权限列表读取和 `*:*:*` 判断。

## 大文件合理性判断

- 本轮新增的是权限注解、切面、统一校验器和接口适配，没有继续扩大卖家/买家主体 Service。
- `PortalPermissionChecker` 只承担接口执行前的身份和权限检查，不承载菜单、角色、部门维护逻辑。
- `PortalPermissionSupport` 仍保持无状态工具职责，没有直接读写 `seller_*` / `buyer_*` 表。

## 未完成

- 卖家端、买家端会话接口已接入 `@PortalPreAuthorize`；真实业务接口尚未批量接入。
- 真实业务接口的数据范围仍需继续按端 token 推导 `sellerId` / `buyerId`，不能相信前端传入主体 ID。
- 本轮未做管理端 UI 改动，因此未运行 `npm run tsc`。

## 下一步

- 管理端 UI 接入继续按已确认模板推进：卖家侧先做标准样板，买家侧只替换字段配置、权限标识和 service。
- 后续新增或改造真实端内业务接口时，同时接入：
  - `@PortalPreAuthorize`
  - `@PortalLog`
  - 从端 token 推导主体范围
