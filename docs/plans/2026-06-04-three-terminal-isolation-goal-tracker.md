# 三端独立改造目标追踪

日期：2026-06-04

## 参考方向

本目标追踪以以下方案为当前唯一参考方向：

- `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

该方案明确替代此前“卖家/买家账号继续复用若依 `sys_user`”的旧方向。

后续如果旧文档、旧代码或旧 SQL 仍然体现以下思路，均应视为待迁移或待清理项：

- 卖家端账号写入 `sys_user`
- 买家端账号写入 `sys_user`
- 卖家端角色写入 `sys_role`
- 买家端角色写入 `sys_role`
- 卖家端菜单写入 `sys_menu`
- 买家端菜单写入 `sys_menu`
- 卖家端部门写入 `sys_dept`
- 买家端部门写入 `sys_dept`
- 卖家端登录/操作日志只写若依系统日志
- 买家端登录/操作日志只写若依系统日志

## 总目标

形成三端独立的账号权限控制面：

| 端 | 目标 |
| --- | --- |
| 管理端 | 保留若依 `sys_*` 后台能力，作为平台控制面 |
| 卖家端 | 独立账号、密码、角色、菜单、权限、部门、日志、会话 |
| 买家端 | 独立账号、密码、角色、菜单、权限、部门、日志、会话 |

管理端仍保留对卖家和买家的控制权，但控制权来自：

- 平台管理接口
- 主体状态
- 账号状态
- 菜单/角色配置
- 免密代入
- 强制踢出
- 审计日志

不再来自账号体系混用。

## 当前状态

| 项目 | 状态 | 说明 |
| --- | --- | --- |
| 三端隔离方案 | 已完成 | 已写入 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` |
| AGENTS 规则更新 | 已完成 | 已将新方向写入 `AGENTS.md` |
| 账号表字段设计 | 第一批已落地 | `seller_account` / `buyer_account` 已改为端内账号字段；端内角色、菜单、部门、日志、会话表已进入 SQL |
| 数据库 DDL/DML | 已执行 | 远程库已执行 `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql` |
| 后端管理端账号改造 | 第一批已完成 | 卖家/买家账号创建、列表、重置密码、主账号重置、免密登录不再依赖 `sys_user` |
| 后端端内认证改造 | 第一批已完成 | 已新增卖家端/买家端登录入口、独立 token、免密 token 消费、登录日志、会话写入；端内菜单/角色权限校验仍待做 |
| 后端端内权限基础改造 | 第四批已完成 | 管理端已可维护端内菜单、角色、账号角色绑定、部门和端账号部门绑定；卖家端/买家端登录后已可读取端内角色、权限和菜单 |
| 管理端前端字段改造 | 第三批已完成 | 卖家/买家管理已接入公共端账号弹窗；支持端账号列表、新增、编辑、部门树绑定和重置默认密码 |
| 前端三端拆分 | 未开始 | 需等账号和权限模型稳定 |
| 旧实现迁移 | 第一批已完成 | 旧 `PortalAccountSupport` / `PortalAccountMapper` 已移除；迁移脚本已删除账号表旧 `user_id` 列 |

## 2026-06-04 实施检查点

本次实施以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，已从方案阶段进入代码和远程库落地。

已完成：

- 后端卖家/买家账号 Mapper 与 Service 改造：账号字段来自 `seller_account` / `buyer_account`，不再 join `sys_user`。
- 删除旧的 `PortalAccountSupport` 和 `PortalAccountMapper`，避免后续继续把端账号写回 `sys_user`。
- 免密登录返回和 Redis payload 改为 `accountId`，有效期保持 30 分钟。
- 前端账号重置默认密码改为发送端账号 ID。
- 初始化脚本 `RuoYi-Vue/sql/seller_buyer_management_seed.sql` 已更新为三端独立表结构。
- 新增并执行远程库迁移脚本 `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`。

远程库执行与校验：

- 执行来源：本机 `.env.local` 中的 `RUOYI_DB_*`，未输出凭证。
- 执行结果：迁移脚本 82 条语句成功。
- 表结构校验：14 张端内核心表存在。
- 账号字段校验：`seller_account` / `buyer_account` 旧 `user_id` 列数量为 0。
- 数据量校验：`seller_account = 3`，`buyer_account = 1`。

接口验证：

- `mvn -DskipTests install`：通过。
- `npm run tsc`：通过。
- 后端启动：`start-backend-local.ps1 -Restart` 后 8080 正常监听。
- `/captchaImage`：200。
- 管理端登录：成功。
- `/seller/admin/sellers/list`：200，返回 3 条。
- `/buyer/admin/buyers/list`：200，返回 1 条。
- 卖家账号列表：返回 1 条。
- 买家账号列表：返回 1 条。
- 卖家免密登录：200，`expireMinutes = 30`，返回 `accountId`。
- 买家免密登录：200，`expireMinutes = 30`，返回 `accountId`。

未完成：

- `seller_role` / `buyer_role`、`seller_menu` / `buyer_menu`、`seller_dept` / `buyer_dept` 的管理端后端配置接口已完成；管理端配置页面和账号部门绑定仍未做。
- 免密登录目前仍是 Redis 一次性 token，尚未落 `portal_direct_login_ticket` 审计票据表。
- 强制踢出和端内操作日志写入仍未接入业务代码。

## 阶段目标

### 阶段 0：冻结旧方向

目标：停止继续扩展卖家/买家复用 `sys_user` 的实现。

任务：

- [x] 写明新方向参考方案。
- [x] 更新 `AGENTS.md`。
- [ ] 审计当前文档，标记旧方向文档为过期或待迁移。
- [x] 审计当前代码，列出并迁移第一批依赖 `sys_user` 的卖家/买家端账号逻辑。

完成标准：

- 新任务不再基于 `seller_account.user_id` / `buyer_account.user_id` 扩展功能。
- 后续代码实现引用三端隔离方案。

### 阶段 1：表结构设计确认

目标：确认三端独立账号权限基础表。

任务：

- [x] 输出并落地 `seller_account` 新字段方案。
- [x] 输出并落地 `buyer_account` 新字段方案。
- [x] 输出并落地 `seller_role` / `seller_menu` / `seller_dept` / `seller_account_role` / `seller_role_menu` 表方案。
- [x] 输出并落地 `buyer_role` / `buyer_menu` / `buyer_dept` / `buyer_account_role` / `buyer_role_menu` 表方案。
- [x] 输出并落地 `seller_login_log` / `seller_oper_log` 表方案。
- [x] 输出并落地 `buyer_login_log` / `buyer_oper_log` 表方案。
- [x] 输出并落地管理端免密代入票据表方案。
- [x] 输出并执行旧数据迁移方案。

完成标准：

- 用户确认 Markdown 表结构设计。
- 明确哪些表新增、哪些字段废弃、哪些旧逻辑迁移。
- 明确远程数据库执行计划。

### 阶段 2：数据库迁移

目标：让数据库具备三端独立账号权限基础。

前置条件：

- 表结构设计已确认。
- 已读取当前激活 MySQL/Redis 配置。
- 已生成远程数据库执行记录。

任务：

- [x] 只读确认远程库当前激活配置。
- [x] 只读确认远程库当前卖家/买家账号数据量。
- [x] 执行已确认 DDL。
- [x] 执行已确认迁移 DML。
- [x] 校验迁移后数量、账号字段和旧列移除状态。

完成标准：

- 卖家端账号可独立存在于 `seller_account`。
- 买家端账号可独立存在于 `buyer_account`。
- 新增卖家/买家端账号不再写入 `sys_user`。

### 阶段 3：后端认证和权限改造

目标：管理端、卖家端、买家端认证分开。

任务：

- [x] 保留管理端若依登录。
- [x] 新增卖家端登录服务。
- [x] 新增买家端登录服务。
- [x] token/session 增加 `terminal`、`accountId`、`subjectId`。
- [x] 改造管理端卖家账号密码重置。
- [x] 改造管理端买家账号密码重置。
- [x] 改造卖家端最后登录记录。
- [x] 改造买家端最后登录记录。
- [x] 改造免密代入生成和消费。
- [x] 增加端内权限读取、端类型校验和菜单数据范围校验。
- [ ] 端内业务接口逐步接入数据范围校验。

完成标准：

- 管理端账号不能登录卖家端/买家端。
- 卖家端账号不能登录管理端/买家端。
- 买家端账号不能登录管理端/卖家端。
- 停用主体后，该主体下账号不可登录。
- 停用账号后，该账号不可登录。

### 阶段 4：管理端控制能力

目标：管理端对卖家/买家保持平台控制权。

任务：

- [x] 管理端可管理卖家主体状态。
- [x] 管理端可管理买家主体状态。
- [x] 管理端可管理卖家端账号。
- [x] 管理端可管理买家端账号。
- [x] 管理端可通过后端接口配置卖家端菜单和角色。
- [x] 管理端可通过后端接口配置买家端菜单和角色。
- [x] 管理端可通过后端接口绑定卖家端账号与端内角色。
- [x] 管理端可通过后端接口绑定买家端账号与端内角色。
- [ ] 管理端可查看卖家端登录/操作日志。
- [ ] 管理端可查看买家端登录/操作日志。
- [ ] 管理端可强制踢出卖家/买家主体或账号的在线会话。

完成标准：

- 管理端不混用账号体系也能停用、重置、代入、踢出、审计卖家/买家端账号。

### 阶段 5：前端三端物理拆分

目标：在账号权限模型稳定后拆分前端。

任务：

- [ ] 确认最终目录命名。
- [ ] 拆出管理端前端。
- [ ] 拆出卖家端前端。
- [ ] 拆出买家端前端。
- [ ] 三端使用不同登录入口。
- [ ] 三端使用不同 token storage key。
- [ ] 三端使用不同菜单接口。

完成标准：

- 三个前端独立运行、独立构建、独立登录。
- 卖家端和买家端不携带管理端菜单和权限逻辑。

## 当前残留点

| 残留点 | 说明 | 处理方式 |
| --- | --- | --- |
| 端内权限业务鉴权未全面接入 | `getInfo` / `getRouters` 已读取端内角色、权限和菜单；后续真实业务接口仍需逐步使用端 token 推导主体范围 | 后续业务接口开发时逐接口接入 |
| 端账号页面已接入，端内配置页面未接入 | 卖家/买家管理已可在主体行进入账号弹窗，并维护端账号部门绑定；端内部门、菜单、角色和账号角色绑定仍缺独立配置页面 | 后续继续按同一模板接入配置页面 |
| 强制踢出未实现 | 登录已写入 `seller_session` / `buyer_session`，但尚未做管理端踢出接口 | 下一步接入 session 作废和 Redis token 删除 |
| 强制踢出未实现 | 登录已写入 `seller_session` / `buyer_session`，但尚未做管理端踢出接口 | 下一步接入 session 作废和 Redis token 删除 |

## 下一步

下一步进入管理端前端页面接入和管理端审计控制能力改造。

建议顺序：

- 先做管理端前端接入端内菜单、角色、部门和账号角色绑定。
- 再做端账号 `dept_id` 页面绑定，让卖家/买家员工账号可以在管理端弹窗中选择端内部门。
- 端内业务接口从端 token 推导 `sellerId` / `buyerId`，不相信前端传入主体 ID。
- 设计并落地 `portal_direct_login_ticket`，把免密代入从 Redis-only 补齐为可审计票据。
- 接入强制踢出：作废 `seller_session` / `buyer_session` 并删除对应 Redis token。

## 2026-06-04 端登录实现检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成三端独立认证的第一批后端落地。

已完成：

- 新增 `PortalTokenSupport`，卖家端/买家端 token 使用 `portal_login_tokens:` Redis 前缀和端内 claim，不复用管理端 `login_tokens:`。
- 新增 `PortalLoginResult`、`PortalLoginSession`、`PortalLoginIssue`、`PortalLoginLog`，统一承载端登录返回、会话和日志。
- 新增 `/seller/login`、`/buyer/login`，账号密码只读取 `seller_account` / `buyer_account`。
- 新增 `/seller/direct-login`、`/buyer/direct-login`，消费管理端生成的免密 token；token 30 分钟有效，消费后立即删除。
- 登录成功后更新 `seller_account.last_login_time` / `buyer_account.last_login_time`，写入 `seller_login_log` / `buyer_login_log` 和 `seller_session` / `buyer_session`。
- Spring Security 仅匿名放行 `/seller/login`、`/buyer/login`、`/seller/direct-login`、`/buyer/direct-login`，管理端接口保持认证要求。

验证结果：

- `mvn -DskipTests install`：通过。
- 后端通过 `.\start-backend-local.ps1 -Restart` 重启，8080 正常监听。
- `/captchaImage`：200，验证码开关仍为关闭状态。
- 管理端 `admin / admin123` 登录成功。
- `/seller/login`：返回 `code=200`，`terminal=seller`，`expireMinutes=30`。
- `/buyer/login`：返回 `code=200`，`terminal=buyer`，`expireMinutes=30`。
- `/seller/direct-login`：第一次消费返回 `code=200`，第二次复用返回失败。
- 卖家端 token 访问管理端 `/getInfo`：业务返回 `code=401`。
- 买家端 token 访问管理端 `/getInfo`：业务返回 `code=401`。
- 远程库近 10 分钟新增：`seller_login_log=9`、`buyer_login_log=3`、`seller_session=9`、`buyer_session=3`。
- 远程库账号最后登录：`seller_account` 中已有最后登录账号 1 个，`buyer_account` 中已有最后登录账号 1 个。

未完成：

- `seller_dept` / `buyer_dept` 管理端后端配置已在后续检查点完成；管理端前端页面和账号部门绑定仍未做。
- 免密代入仍未落 `portal_direct_login_ticket` 审计票据表。
- 强制踢出尚未实现。

## 2026-06-04 端内菜单角色管理接口检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成管理端控制卖家端/买家端菜单和角色的第一批后端接口。

已完成：

- 新增 `PortalMenu`、`PortalRole`、`PortalTreeSelect`，统一卖家端/买家端菜单和角色领域对象。
- 新增 `PortalPermissionSupport`，统一端内菜单默认值、角色校验、ID 去重和树结构构建。
- 新增卖家端管理接口：
  - `/seller/admin/menus/**`
  - `/seller/admin/sellers/{sellerId}/roles/**`
- 新增买家端管理接口：
  - `/buyer/admin/menus/**`
  - `/buyer/admin/buyers/{buyerId}/roles/**`
- 新增 `SellerPortalPermissionMapper` / `BuyerPortalPermissionMapper`，数据只读写 `seller_menu` / `buyer_menu`、`seller_role` / `buyer_role`、`seller_role_menu` / `buyer_role_menu`。
- 更新 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`，补齐管理端按钮权限。
- 新增并执行 `RuoYi-Vue/sql/20260604_portal_permission_admin_menu_seed.sql`，只 upsert 管理端 `sys_menu` 中用于控制端内菜单/角色的 20 个按钮权限。

远程库执行与校验：

- 执行来源：本机 `.env.local` 中的 `RUOYI_DB_*`，未输出凭证。
- 执行脚本：`RuoYi-Vue/sql/20260604_portal_permission_admin_menu_seed.sql`。
- 执行结果：2 条语句成功。
- 权限校验：`sys_menu` 中 20 个 `seller:admin:menu:*`、`seller:admin:role:*`、`buyer:admin:menu:*`、`buyer:admin:role:*` 权限存在。

接口验证：

- 首次 `mvn -DskipTests install`：Java 编译通过，最终 repackage 因旧后端进程占用 jar 失败。
- 停止 8080 旧后端进程后重新执行 `mvn -DskipTests install`：通过。
- 后端通过 `.\start-backend-local.ps1 -Restart` 启动，8080 正常监听。
- `/captchaImage`：200。
- 管理端 `admin / admin123` 登录成功。
- 卖家端闭环验证通过：
  - 新增临时 `seller_menu`
  - 新增临时 `seller_role`
  - `seller_role_menu` 绑定菜单
  - `roleMenuTreeselect` 返回绑定菜单
  - 删除临时角色和菜单
- 买家端闭环验证通过：
  - 新增临时 `buyer_menu`
  - 新增临时 `buyer_role`
  - `buyer_role_menu` 绑定菜单
  - `roleMenuTreeselect` 返回绑定菜单
  - 删除临时角色和菜单
- 无 token 访问 `/seller/admin/menus/list`：返回业务 `code=401`。
- 无 token 访问 `/buyer/admin/menus/list`：返回业务 `code=401`。

未完成：

- 管理端前端页面尚未接入这些菜单/角色配置接口。

## 2026-06-04 端账号角色绑定与端内权限读取检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成端账号绑定端内角色，以及卖家端/买家端登录后读取端内角色、权限和菜单的第一批后端闭环。

已完成：

- 新增 `PortalAccountRoleAssign` 和 `PortalPermissionInfo`，分别承载端账号角色绑定请求和端内权限返回对象。
- `PortalTokenSupport` 增加端 token 解析能力，可按 `seller` / `buyer` 校验 terminal 并读取 `portal_login_tokens:` Redis 会话。
- 卖家管理端新增账号角色接口：
  - `GET /seller/admin/sellers/{sellerId}/accounts/{accountId}/roles`
  - `PUT /seller/admin/sellers/{sellerId}/accounts/{accountId}/roles`
- 买家管理端新增账号角色接口：
  - `GET /buyer/admin/buyers/{buyerId}/accounts/{accountId}/roles`
  - `PUT /buyer/admin/buyers/{buyerId}/accounts/{accountId}/roles`
- 卖家端新增端内会话接口：
  - `GET /seller/getInfo`
  - `GET /seller/getRouters`
- 买家端新增端内会话接口：
  - `GET /buyer/getInfo`
  - `GET /buyer/getRouters`
- `SellerPortalPermissionMapper` / `BuyerPortalPermissionMapper` 增加端账号角色、权限 code 和菜单树查询，数据只读取 `seller_*` / `buyer_*` 表。
- 修复端内权限读取 SQL：去掉 `select distinct role_key/perms` 查询中按未选出字段排序的问题，避免 MySQL `DISTINCT + ORDER BY` 报错。

接口验证：

- `mvn -DskipTests install`：通过。
- 后端通过 `.\start-backend-local.ps1 -Restart` 重启，8080 正常监听。
- `/captchaImage`：200。
- 管理端 `admin / admin123` 登录成功。
- 卖家端闭环验证通过：
  - 选择卖家主体 `sellerId=9`、端账号 `accountId=8`。
  - 新增临时 `seller_menu` 和 `seller_role`。
  - 通过管理端接口把临时角色绑定到端账号。
  - 管理端免密代入后，`/seller/getInfo` 返回临时 `roleKey` 和 `perms`。
  - `/seller/getRouters` 返回临时菜单。
  - 无 token 访问 `/seller/getInfo` 返回业务 `code=401`。
  - 验证完成后恢复端账号原角色并删除临时角色和菜单。
- 买家端闭环验证通过：
  - 选择买家主体 `buyerId=2`、端账号 `accountId=2`。
  - 新增临时 `buyer_menu` 和 `buyer_role`。
  - 通过管理端接口把临时角色绑定到端账号。
  - 管理端免密代入后，`/buyer/getInfo` 返回临时 `roleKey` 和 `perms`。
  - `/buyer/getRouters` 返回临时菜单。
  - 无 token 访问 `/buyer/getInfo` 返回业务 `code=401`。
  - 验证完成后恢复端账号原角色并删除临时角色和菜单。

未完成：

- 管理端前端页面尚未接入端内菜单、角色和账号角色绑定接口。
- `seller_dept` / `buyer_dept` 管理端后端配置已完成；管理端前端页面和账号部门绑定仍未做。
- 端内业务接口还未逐步接入 token 推导主体范围。
- 免密代入仍未落 `portal_direct_login_ticket` 审计票据表。
- 强制踢出尚未实现。

## 2026-06-04 端内部门管理接口检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成卖家端/买家端独立部门的第一批管理端后端接口。

已完成：

- 新增 `PortalDept`，统一承载卖家端/买家端部门字段。
- 新增 `PortalDeptSupport`，统一部门默认值、状态校验、父级校验、祖级路径和树结构构建。
- 卖家端新增管理端部门接口：
  - `GET /seller/admin/sellers/{sellerId}/depts/list`
  - `GET /seller/admin/sellers/{sellerId}/depts/{deptId}`
  - `GET /seller/admin/sellers/{sellerId}/depts/treeselect`
  - `POST /seller/admin/sellers/{sellerId}/depts`
  - `PUT /seller/admin/sellers/{sellerId}/depts`
  - `DELETE /seller/admin/sellers/{sellerId}/depts/{deptId}`
- 买家端新增管理端部门接口：
  - `GET /buyer/admin/buyers/{buyerId}/depts/list`
  - `GET /buyer/admin/buyers/{buyerId}/depts/{deptId}`
  - `GET /buyer/admin/buyers/{buyerId}/depts/treeselect`
  - `POST /buyer/admin/buyers/{buyerId}/depts`
  - `PUT /buyer/admin/buyers/{buyerId}/depts`
  - `DELETE /buyer/admin/buyers/{buyerId}/depts/{deptId}`
- 部门数据分别读写 `seller_dept` / `buyer_dept`，不复用 `sys_dept`。
- 删除部门前会检查同端子部门和端账号 `dept_id` 占用，避免账号挂到已删除部门。
- 更新 `RuoYi-Vue/sql/20260604_portal_permission_admin_menu_seed.sql` 和 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`，补齐 10 个管理端部门按钮权限。

远程库执行与校验：

- 执行来源：本机 `.env.local` 中的 `RUOYI_DB_*`，未输出凭证。
- 执行脚本：`RuoYi-Vue/sql/20260604_portal_permission_admin_menu_seed.sql`。
- 执行方式：使用本机 Maven 依赖中的 MySQL JDBC 驱动执行 SQL。
- 执行结果：1 条 upsert 语句成功。
- 权限校验：`sys_menu` 中 10 个 `seller:admin:dept:*`、`buyer:admin:dept:*` 权限存在。

接口验证：

- `mvn -DskipTests install`：通过。
- 后端通过 `.\start-backend-local.ps1 -Restart` 重启，8080 正常监听。
- `/captchaImage`：200。
- 管理端 `admin / admin123` 登录成功。
- 卖家端部门闭环验证通过：
  - 选择卖家主体 `sellerId=9`。
  - 新增临时 `seller_dept`。
  - 查询列表、详情和树选择均返回临时部门。
  - 修改临时部门名称和排序后可查询到更新结果。
  - 删除临时部门后列表不可见。
  - 无 token 访问部门列表返回业务 `code=401`。
- 买家端部门闭环验证通过：
  - 选择买家主体 `buyerId=2`。
  - 新增临时 `buyer_dept`。
  - 查询列表、详情和树选择均返回临时部门。
  - 修改临时部门名称和排序后可查询到更新结果。
  - 删除临时部门后列表不可见。
  - 无 token 访问部门列表返回业务 `code=401`。

未完成：

- 管理端前端页面尚未接入端内菜单、角色、部门和账号角色绑定接口。
- 端账号 `dept_id` 字段已接入后端新增/编辑账号流程和前端 service/type 契约；管理端页面弹窗仍未接入。
- 免密代入仍未落 `portal_direct_login_ticket` 审计票据表。
- 强制踢出尚未实现。

## 2026-06-04 端账号部门绑定检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成卖家端/买家端员工账号归属端内部门的第一批后端闭环和前端契约。

已完成：

- `PortalAccount` 增加 `deptId` / `deptName`，作为卖家端和买家端账号共用的端内部门字段。
- 卖家端账号列表、详情、登录查询和主账号查询已映射 `seller_account.dept_id`，并通过 `seller_dept` 返回 `deptName`。
- 买家端账号列表、详情、登录查询和主账号查询已映射 `buyer_account.dept_id`，并通过 `buyer_dept` 返回 `deptName`。
- 新增卖家端账号时可写入 `dept_id`，并校验部门必须属于同一个 `seller_id`。
- 新增买家端账号时可写入 `dept_id`，并校验部门必须属于同一个 `buyer_id`。
- 新增管理端编辑端账号接口：
  - `PUT /seller/admin/sellers/{sellerId}/accounts`
  - `PUT /buyer/admin/buyers/{buyerId}/accounts`
- 编辑端账号时保留原登录账号，允许更新昵称、邮箱、手机号、状态、备注和 `dept_id`。
- 普通新增端账号如果未传 `accountRole`，默认从 `OWNER` 调整为 `STAFF`；主体创建主账号时仍显式使用 `OWNER`。
- 前端补齐账号部门契约：
  - `PortalAccountBase.deptId`
  - `PortalAccountBase.deptName`
  - `PortalDept`
  - `PortalTreeNode`
  - `getAdminSellerDepts`
  - `getAdminSellerDeptTree`
  - `updateAdminSellerAccount`
  - `getAdminBuyerDepts`
  - `getAdminBuyerDeptTree`
  - `updateAdminBuyerAccount`

验证结果：

- `mvn -DskipTests install`：通过；首次因旧 8080 进程占用 `ruoyi-admin.jar` 导致 repackage 失败，停止旧进程后重新执行通过。
- `.\start-backend-local.ps1 -Restart`：已重启后端。
- `GET /captchaImage`：HTTP 200。
- `npm run tsc`：通过。
- 卖家端账号部门绑定闭环通过：
  - 使用管理端 `admin / admin123` 登录。
  - 选择卖家主体 `sellerId=9` 和端账号。
  - 创建临时 `seller_dept`。
  - 调用 `PUT /seller/admin/sellers/{sellerId}/accounts` 绑定临时部门。
  - 再查账号列表，返回的 `deptId` 和 `deptName` 与临时部门一致。
  - 恢复原 `deptId`，删除临时部门。
- 买家端账号部门绑定闭环通过：
  - 使用管理端 `admin / admin123` 登录。
  - 选择买家主体 `buyerId=2` 和端账号。
  - 创建临时 `buyer_dept`。
  - 调用 `PUT /buyer/admin/buyers/{buyerId}/accounts` 绑定临时部门。
  - 再查账号列表，返回的 `deptId` 和 `deptName` 与临时部门一致。
  - 恢复原 `deptId`，删除临时部门。
- 临时数据清理校验：`sellerCodexDeptCount=0; buyerCodexDeptCount=0`。

大文件合理性判断：

- `SellerServiceImpl.java` 和 `BuyerServiceImpl.java` 当前均约 403 行，已触发 400 行判断阈值。本轮新增逻辑只是在既有账号新增/更新链路中补 `dept_id` 校验和写入，职责仍属于当前服务的主体账号管理范围；此时拆分会把既有主体资料、登录、账号逻辑一起牵动，改动面超过本轮目标。
- `SellerMapper.xml` 和 `BuyerMapper.xml` 当前均约 337 行，已触发 300 行判断阈值。本轮只补账号查询字段、部门 join 和 `dept_id` 写入，仍在同一个 Mapper 表范围内。
- 后续继续接入免密审计票据、强制踢出、端内操作日志时，应优先考虑拆分账号服务、登录服务和主体资料服务，避免继续扩大 `SellerServiceImpl` / `BuyerServiceImpl`。

未完成：

- 管理端页面尚未提供端账号新增/编辑弹窗和部门树选择控件。
- 管理端页面尚未接入端内菜单、角色、部门和账号角色绑定接口。
- 强制踢出尚未实现。

## 2026-06-04 免密代入审计票据检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成管理端免密代入卖家端、买家端的审计票据落地。

已完成：

- 新增 `portal_direct_login_ticket` 表，用于记录管理端免密代入票据。
- 新增 `PortalDirectLoginTicket`、`PortalDirectLoginTicketMapper` 和 `PortalDirectLoginTicketMapper.xml`。
- `PortalDirectLoginSupport` 从 Redis-only 改为 DB ticket + Redis payload：
  - 生成免密 token 时写入 `portal_direct_login_ticket`。
  - DB 只保存 `token_hash`，不保存 token 明文。
  - 票据记录 `terminal`、目标主体、目标账号、acting admin、过期时间、使用时间、使用 IP 和状态。
  - 消费前校验 ticket 仍为 `ISSUED` 且未过期。
  - 消费成功后原子更新 ticket 为 `USED`，再删除 Redis token。
  - 同一 token 第二次消费会失败。
- `PortalDirectLoginResult` 和前端 `DirectLoginResult` 类型补充 `ticketId`，方便管理端后续审计展示或跳转。
- 新增远程库执行记录：`docs/plans/2026-06-04-portal-direct-login-ticket-db-execution-record.md`。
- 更新初始化脚本 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`，后续初始化也会包含该票据表。

远程库执行与验证：

- 执行来源：本机 `.env.local` 中的 `RUOYI_DB_*`，未输出凭证。
- 执行脚本：`RuoYi-Vue/sql/20260604_portal_direct_login_ticket.sql`。
- 执行结果：成功。
- 表结构校验：`portal_direct_login_ticket` 存在，字段数 19，索引数 5。
- 明文 token 检查：表结构不包含明文 token 字段。

接口闭环验证：

- `mvn -DskipTests install`：通过；首次因旧 8080 进程占用 jar 导致 repackage 失败，停止旧进程后重新执行通过。
- `npm run tsc`：通过。
- `.\start-backend-local.ps1 -Restart`：已启动后端。
- `GET /captchaImage`：HTTP 200。
- 管理端 `admin / admin123` 登录：成功。
- 卖家免密代入：
  - `POST /seller/admin/sellers/{sellerId}/directLogin` 返回 `code=200`，返回 `ticketId`。
  - `GET /seller/direct-login?directLoginToken=...` 第一次返回 `code=200`。
  - 同一 token 第二次消费返回 `code=500`。
  - 远程库对应 ticket：`status=USED`，`used_time` 已写入，`used_ip` 已写入，`token_hash` 长度 64，hash 匹配，未保存 token 明文。
- 买家免密代入：
  - `POST /buyer/admin/buyers/{buyerId}/directLogin` 返回 `code=200`，返回 `ticketId`。
  - `GET /buyer/direct-login?directLoginToken=...` 第一次返回 `code=200`。
  - 同一 token 第二次消费返回 `code=500`。
  - 远程库对应 ticket：`status=USED`，`used_time` 已写入，`used_ip` 已写入，`token_hash` 长度 64，hash 匹配，未保存 token 明文。

大文件合理性判断：

- 本轮没有继续扩大 `SellerServiceImpl` / `BuyerServiceImpl` 主体服务；免密审计能力集中在 `PortalDirectLoginSupport` 与独立 ticket mapper。
- `PortalDirectLoginSupport` 当前约 210 行，职责仍单一：端免密 token 生成、票据审计、消费校验。
- 新增 ticket mapper/xml 只负责 `portal_direct_login_ticket`，没有混入卖家/买家业务查询。

未完成：

- 管理端页面尚未提供 ticket 审计列表入口。
- 强制踢出尚未实现。

## 2026-06-04 管理端账号 UI 接入检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家模板做好，买家替换配置和 service”的方式接入管理端端账号维护 UI。

已完成：

- 新增公共组件 `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx`。
- 卖家管理、买家管理主体行新增“账号”入口。
- 卖家/买家共用同一个账号弹窗，按 `PartnerModuleConfig` 注入：
  - 主体 ID 字段
  - 端账号 ID 字段
  - 账号列表 service
  - 新增账号 service
  - 编辑账号 service
  - 部门树 service
  - 重置默认密码 service
- 账号弹窗支持：
  - 查看端账号列表
  - 新增端账号，默认初始密码 `U12346`
  - 编辑端账号
  - 绑定端内部门树
  - 维护账号角色字段
  - 维护账号状态、手机、邮箱、备注
  - 重置端账号默认密码
- 新增账号默认角色为 `STAFF`；已有 `OWNER` 可展示，但新增时前端不主动创建第二个负责人账号。
- 主体列表去掉强制横向 `scroll.x`，继续使用紧凑单元格和 `tableLayout="fixed"`，避免页面主动生成横向滚动条。

验证结果：

- `npm run tsc`：通过。
- `git diff --check -- react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx react-ui/src/pages/Seller/index.tsx react-ui/src/pages/Buyer/index.tsx`：通过，仅出现 Git CRLF 提示。
- 浏览器验收：管理端登录成功；卖家管理可打开账号弹窗和新增账号表单；买家管理可打开账号弹窗。
- 浏览器 console error：0。
- 截图证据：`logs/screenshots/2026-06-04-buyer-account-modal.png`。

未完成：

- 管理端端内部门、菜单、角色和账号角色绑定独立配置页面仍未接入。
- 管理端 ticket 审计列表入口尚未接入。
- 强制踢出尚未实现。
