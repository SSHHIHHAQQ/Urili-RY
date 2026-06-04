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

说明：本节是当前权威状态。下方历史检查点按发生时间保留，若早期“未完成”事项与本节或最新检查点冲突，以本节和最新检查点为准。

| 项目 | 状态 | 说明 |
| --- | --- | --- |
| 三端隔离方案 | 已完成 | 已写入 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` |
| AGENTS 规则更新 | 已完成 | 已将新方向写入 `AGENTS.md` |
| 账号表字段设计 | 第一批已落地 | `seller_account` / `buyer_account` 已改为端内账号字段；端内角色、菜单、部门、日志、会话表已进入 SQL |
| 数据库 DDL/DML | 已执行 | 远程库已执行 `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql` |
| 后端管理端账号改造 | 第一批已完成 | 卖家/买家账号创建、列表、重置密码、主账号重置、免密登录不再依赖 `sys_user` |
| 后端端内认证改造 | 第一批已完成 | 已新增卖家端/买家端登录入口、独立 token、免密 token 消费、登录日志、会话写入；端内菜单/角色权限读取已接入，端内操作日志第一批写入链路已接入 |
| 后端端内权限基础改造 | 第四批已完成 | 管理端已可维护端内菜单、角色、账号角色绑定、部门和端账号部门绑定；卖家端/买家端登录后已可读取端内角色、权限和菜单；端内接口级权限注解和统一校验器已落地 |
| 管理端前端字段改造 | 第八批已完成 | 卖家/买家管理已接入公共端账号弹窗、端内部门弹窗、端内角色弹窗和端内菜单配置弹窗；支持端账号列表、新增、编辑、部门树绑定、重置默认密码、强制踢出、账号角色绑定、端内部门维护、端内角色维护和端内菜单维护 |
| 管理端审计 UI 与查询 | 已完成 | 卖家/买家管理已按同构模板接入审计弹窗，可查询登录日志、操作日志和免密票据；端内操作日志第一批写入链路已覆盖 `getInfo` / `getRouters` |
| 免密代入审计原因 | 已完成 | 管理端生成卖家/买家端免密代入票据时必须填写代入原因，并写入 `portal_direct_login_ticket.reason` |
| 前端三端拆分 | 未开始 | 需等账号和权限模型稳定 |
| 旧实现迁移 | 第二批已完成 | 旧 `PortalAccountSupport` / `PortalAccountMapper` 已移除；迁移脚本已删除账号表旧 `user_id` 列；早期复用 `sys_user` 的历史方案文档已标记过期 |

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
- 强制踢出已在后续管理端强制踢出检查点完成；端内操作日志写入链路已接入 `@PortalLog`，当前覆盖卖家/买家端 `getInfo` 和 `getRouters`，后续真实业务接口继续复用。

## 阶段目标

### 阶段 0：冻结旧方向

目标：停止继续扩展卖家/买家复用 `sys_user` 的实现。

任务：

- [x] 写明新方向参考方案。
- [x] 更新 `AGENTS.md`。
- [x] 审计当前文档，标记旧方向文档为过期或待迁移。
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
- [x] 增加端内接口级权限注解、切面和统一校验器。
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
- [x] 管理端可查看卖家端登录/操作日志。
- [x] 管理端可查看买家端登录/操作日志。
- [x] 管理端可强制踢出卖家/买家主体或账号的在线会话。

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
| 管理端同构 UI 模板已形成 | 卖家侧先做标准样板，买家侧只替换端类型、文案、路由、权限、字段配置和 service；账号、部门、角色、菜单、审计弹窗已按此方式接入 | 后续已确定模式的管理端 UI 直接套模板推进，不再逐页重新设计 |
| 端内真实业务接口范围控制仍需逐步接入 | 管理端审计弹窗已可查看登录日志、操作日志和 ticket；`seller_oper_log` / `buyer_oper_log` 第一批写入链路已接入端内 `getInfo` / `getRouters` | 后续真实端内业务接口必须从 token 推导主体范围，并继续使用 `@PortalLog` 写入端内操作日志 |
| 前端三端物理拆分仍未开始 | 当前仍以 `react-ui/` 作为管理端验证入口；真正 `admin-ui` / `seller-ui` / `buyer-ui` 物理拆分尚未落地 | 等账号、端入口、菜单域、权限模型和管理端控制权继续稳定后再拆目录 |

## 下一步

下一步不再重复设计已确认的管理端同构 UI。后续同构页面按“卖家一套做好，再复制成买家，只替换配置和 service”的方式提速推进。

建议顺序：

- 继续把后续真实卖家端/买家端业务接口接入端 token 主体推导，不信任前端传入的 `sellerId` / `buyerId`。
- 真实端内接口继续接入 `@PortalPreAuthorize` 和 `@PortalLog`，让权限校验、数据范围和操作日志形成默认模板。
- 新增管理端同构 UI 时，优先复用当前 `PartnerManagement` 模板和 service 配置注入方式。
- 前端三端物理拆分暂不提前做，等端入口、菜单域、权限模型和管理端控制权继续稳定后再拆目录。

## 2026-06-04 目标追踪状态清理检查点

本检查点用于清理目标追踪里的陈旧状态，不做代码改动。

已完成：

- `AGENTS.md` 已记录同构管理端 UI 的模板化推进规则：卖家侧先形成样板，买家侧替换配置和 service。
- 本文件顶部“当前状态”补齐“管理端审计 UI 与查询”已完成。
- 本文件顶部“当前残留点”移除“日志与审计页面仍未接入”的陈旧表述。
- 历史检查点保留原始时间线；若早期“未完成”与顶部当前状态冲突，以顶部当前状态和最新检查点为准。

验证结果：

- 本轮仅文档清理。
- `git diff --check -- docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md AGENTS.md`：通过，仅有 LF/CRLF 提示。

## 2026-06-04 端内权限校验器自动化验证检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，补强后续真实端内业务接口会复用的 `PortalPermissionChecker` 自动化验证。

已完成：

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalPermissionCheckerTest.java`。
- 覆盖无显式权限要求时只校验端会话即可通过。
- 覆盖 `requiredPermissions` 与 `anyPermissions` 同时存在时的组合校验。
- 覆盖缺少 required 权限时返回 `403` 和“没有操作权限”。
- 覆盖缺少 any 权限时返回 `403` 和“没有操作权限”。
- 覆盖端类型未注册权限服务时拒绝访问，避免卖家/买家端权限服务串用。

验证结果：

- 首次执行 `mvn -pl ruoyi-system -DargLine="-Djdk.net.URLClassPath.disableClassPathURLCheck=true" test` 失败，原因是 JUnit `assertEquals` 在 `int` 与 `Integer` 间存在重载歧义。
- 修正断言为 `Integer.valueOf(HttpStatus.FORBIDDEN)` 后重跑通过。
- 最终结果：`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。

本轮未执行事项：

- 本轮没有执行 DDL/DML。
- 本轮没有连接远程 MySQL/Redis。
- 本轮没有重启后端。
- 本轮没有改动前端。

## 2026-06-04 免密代入原因必填检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，补齐管理端免密代入必须可审计的“代入原因”链路。

已完成：

- 新增 `PortalDirectLoginRequest`，承载管理端生成免密代入票据时提交的 `reason`。
- `PortalDirectLoginSupport.createToken(...)` 新增 `reason` 参数，并统一校验：
  - 代入原因不能为空。
  - 代入原因不能超过 `portal_direct_login_ticket.reason` 字段长度 255 字符。
  - 生成票据时写入 `portal_direct_login_ticket.reason`。
- 卖家管理端接口 `POST /seller/admin/sellers/{sellerId}/directLogin` 改为接收 request body，并把 `reason` 传入公共支撑。
- 买家管理端接口 `POST /buyer/admin/buyers/{buyerId}/directLogin` 按同一模板改造。
- 卖家/买家 service 的 `create*DirectLogin` 签名同步补充 `reason`。
- 前端 `PartnerManagementPage` 点击“登录卖家端/买家端”时先弹出“代入原因”输入框，校验通过后才生成并打开免密链接。
- 卖家/买家前端 service 同步改为 `POST` JSON body：`{ reason }`。
- `docs/architecture/reuse-ledger.md` 已更新：免密代入必须通过公共支撑写入 `reason`，不能另开临时备注字段或绕过公共支撑。

验证结果：

- `mvn -DskipTests compile`：通过。
- `npm run tsc`：通过。
- `mvn -pl ruoyi-system -DargLine="-Djdk.net.URLClassPath.disableClassPathURLCheck=true" test`：通过，`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check`：通过，仅有 LF/CRLF 提示。

大文件合理性判断：

- `PartnerManagementPage.tsx` 是既有公共主体管理大文件。本轮只在已有免密登录操作中加入原因弹窗和 service 参数，没有继续扩展审计表格、账号表格或端内配置表单；暂不为了这一处交互拆新组件。
- `PortalDirectLoginSupport.java` 仍只负责免密 token 生成、票据审计和消费校验；本轮增加 `reason` 校验属于同一职责。

本轮未执行事项：

- 本轮没有新增或修改表结构，未执行 DDL。
- 本轮没有执行远程 DML。
- 本轮没有重启后端。
- 本轮没有生成真实免密代入票据；如需验证远程库 `reason` 落库，需要重启后端后做一次真实免密代入烟测。

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
- 强制踢出已在后续管理端强制踢出检查点完成。

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
- 强制踢出已在后续管理端强制踢出检查点完成。

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

- 管理端前端页面已在后续 UI 检查点接入端账号、账号角色绑定、端内部门、端内角色和端内菜单配置；登录/操作日志与 ticket 审计页面仍未接入。
- 端账号 `dept_id` 字段已接入后端新增/编辑账号流程和前端 service/type 契约；管理端页面弹窗仍未接入。
- 免密代入仍未落 `portal_direct_login_ticket` 审计票据表。
- 强制踢出已在后续管理端强制踢出检查点完成。

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
- 后续继续接入端内操作日志或更多端内业务接口时，应优先考虑拆分账号服务、登录服务和主体资料服务，避免继续扩大 `SellerServiceImpl` / `BuyerServiceImpl`。

未完成：

- 管理端页面尚未提供端账号新增/编辑弹窗和部门树选择控件。
- 管理端页面已在后续 UI 检查点接入端账号、账号角色绑定、端内部门、端内角色和端内菜单配置；登录/操作日志与 ticket 审计页面仍未接入。
- 强制踢出已在后续管理端强制踢出检查点完成。

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
- 强制踢出已在后续管理端强制踢出检查点完成。

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

- 管理端端内部门、菜单、角色独立配置页面仍未接入；账号角色绑定已在账号弹窗中接入。
- 管理端 ticket 审计列表入口尚未接入。
- 强制踢出已在后续管理端强制踢出检查点完成。

## 2026-06-04 管理端强制踢出检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成管理端对卖家端、买家端在线会话的强制踢出能力。

已完成：

- `PortalTokenSupport` 新增端内 token 批量删除方法，删除范围限定在 `portal_login_tokens:{terminal}:{tokenId}`。
- 卖家端新增主体级和账号级强制踢出：
  - `DELETE /seller/admin/sellers/{sellerId}/sessions`
  - `DELETE /seller/admin/sellers/{sellerId}/accounts/{accountId}/sessions`
- 买家端新增主体级和账号级强制踢出：
  - `DELETE /buyer/admin/buyers/{buyerId}/sessions`
  - `DELETE /buyer/admin/buyers/{buyerId}/accounts/{accountId}/sessions`
- 强制踢出会：
  - 从 `seller_session` / `buyer_session` 查询在线 token。
  - 更新 session `status = '1'`。
  - 写入 `logout_time`。
  - 删除 Redis 中对应端内 token。
- 主体或端账号被停用时，Service 会同步调用强制踢出逻辑。
- 强制踢出接口改为幂等返回：即使当前没有在线会话，也返回 `code=200`，`data=0`。
- 前端卖家/买家主体行“更多”菜单新增“强制踢出”。
- 前端卖家/买家账号弹窗账号行新增“强制踢出”。
- 新增远程库执行记录：`docs/plans/2026-06-04-portal-force-logout-menu-db-execution-record.md`。

远程库执行与验证：

- 执行来源：本机 `.env.local` 中的 `RUOYI_DB_*`，未输出凭证。
- 执行脚本：`RuoYi-Vue/sql/20260604_portal_force_logout_menu_seed.sql`。
- 执行结果：`executedStatements=1`。
- 权限校验：`seller:admin:forceLogout` / `buyer:admin:forceLogout` 共 2 个权限点存在。

接口闭环验证：

- `mvn -DskipTests install`：通过；首次因旧 8080 进程占用 jar 导致 repackage 失败，停止旧进程后重新执行通过。
- `npm run tsc`：通过。
- `.\start-backend-local.ps1 -Restart`：已启动后端。
- 管理端 `admin / admin123` 登录：成功。
- 卖家账号级强退：
  - 强退前 `/seller/getInfo` 返回 `code=200`。
  - `DELETE /seller/admin/sellers/{sellerId}/accounts/{accountId}/sessions` 返回 `code=200`，`data=1`。
  - 强退后同 token 调 `/seller/getInfo` 返回 `code=401`。
  - 重复强退返回 `code=200`，`data=0`。
  - `seller_session` 对应 token：`status=1`，`logout_time` 已写入。
- 买家账号级强退：
  - 强退前 `/buyer/getInfo` 返回 `code=200`。
  - `DELETE /buyer/admin/buyers/{buyerId}/accounts/{accountId}/sessions` 返回 `code=200`，`data=1`。
  - 强退后同 token 调 `/buyer/getInfo` 返回 `code=401`。
  - 重复强退返回 `code=200`，`data=0`。
  - `buyer_session` 对应 token：`status=1`，`logout_time` 已写入。
- 卖家主体级强退：
  - 强退前 `/seller/getInfo` 返回 `code=200`。
  - `DELETE /seller/admin/sellers/{sellerId}/sessions` 返回 `code=200`，`data=1`。
  - 强退后同 token 调 `/seller/getInfo` 返回 `code=401`。
  - 重复强退返回 `code=200`，`data=0`。
- 买家主体级强退：
  - 强退前 `/buyer/getInfo` 返回 `code=200`。
  - `DELETE /buyer/admin/buyers/{buyerId}/sessions` 返回 `code=200`，`data=1`。
  - 强退后同 token 调 `/buyer/getInfo` 返回 `code=401`。
  - 重复强退返回 `code=200`，`data=0`。

浏览器验证：

- 卖家管理主体行“更多”菜单已展示“强制踢出”。
- 卖家账号弹窗账号行已展示“强制踢出”。
- 浏览器 console error：0。
- 截图证据：`logs/screenshots/2026-06-04-seller-force-logout-account-modal.png`。

大文件合理性判断：

- `SellerServiceImpl.java` / `BuyerServiceImpl.java` 继续超过 400 行。本轮新增的是账号/主体控制流里的会话作废逻辑，和当前服务已有账号状态、登录、免密代入职责相关；为避免一次性拆动主体、账号、登录三类历史逻辑，本轮保持在原服务内。
- 后续接入登录/操作日志页面或更多端内业务接口时，应优先拆分登录会话控制服务，避免继续扩大主体服务类。

未完成：

- 管理端端内部门、菜单、角色独立配置页面仍未接入；账号角色绑定已在账号弹窗中接入。
- 管理端 ticket 审计列表入口尚未接入。
- 管理端卖家/买家登录日志、操作日志页面尚未接入。

## 2026-06-04 管理端账号角色绑定 UI 检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按已确认的“卖家一套做好，再复制成买家，只替换配置和 service”方式接入管理端端账号角色绑定 UI。

已完成：

- 新增公共组件 `react-ui/src/components/PartnerManagement/PartnerAccountRoleModal.tsx`，单独承载端账号角色分配弹窗。
- `PartnerManagementPage` 的公共 service 契约补充：
  - `getAccountRoles`
  - `assignAccountRoles`
- 卖家管理接入：
  - `getAdminSellerAccountRoles`
  - `assignAdminSellerAccountRoles`
- 买家管理接入：
  - `getAdminBuyerAccountRoles`
  - `assignAdminBuyerAccountRoles`
- `PartnerAccountModal` 账号行新增“分配角色”入口，按 `seller:admin:role:edit` / `buyer:admin:role:edit` 权限展示。
- 账号行操作调整为：高频“编辑”“分配角色”直接展示，低频“重置密码”“强制踢出”收进“更多”，继续遵守复用台账中的表格操作规则。
- 主体管理公共组件中的 Ant Design `Space direction` 旧写法已替换为 `Flex vertical`，避免浏览器验证时出现弃用告警。

验证结果：

- `npm run tsc`：通过。
- Playwright 浏览器验证：
  - 管理端登录成功。
  - 卖家管理从实际菜单进入 `/partner/seller`。
  - 卖家账号弹窗可打开，账号行展示“分配角色”，角色弹窗可打开。
  - 买家管理进入 `/partner/buyer`。
  - 买家账号弹窗可打开，账号行展示“分配角色”，角色弹窗可打开。
  - 卖家和买家的浏览器 console 均为 `0 errors / 0 warnings`。
- 截图证据：
  - `logs/screenshots/2026-06-04-seller-account-role-modal.png`
  - `logs/screenshots/2026-06-04-buyer-account-role-modal.png`

大文件合理性判断：

- `PartnerAccountRoleModal.tsx` 独立拆出，避免继续把角色分配逻辑堆进账号弹窗。
- `PartnerAccountModal.tsx` 当前约 507 行，触发 500 行判断阈值。本轮只新增角色弹窗入口和操作列收敛，真实角色分配表单已拆到独立组件；为保持本轮模板化提速，不在本批拆账号表格和账号表单。
- `PartnerManagementPage.tsx` 属于既有公共主体管理大文件，本轮只补 service 契约并替换旧 UI API。后续如果继续扩展日志或审计页面，不应再扩大该文件，应新建独立公共配置组件或按菜单拆页。

未完成：

- 管理端端内菜单配置已在后续 UI 检查点完成；日志和审计页面仍未接入。
- 管理端 ticket 审计列表入口尚未接入。
- 管理端卖家/买家登录日志、操作日志页面尚未接入。

## 2026-06-04 管理端端内菜单 UI 检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按已确认的“卖家一套做好，买家只替换配置和 service”的模板化方式接入管理端卖家端/买家端菜单维护 UI。

已完成：

- 新增公共组件 `react-ui/src/components/PartnerManagement/PartnerMenuModal.tsx`，单独承载端维度菜单列表、新增/编辑表单和删除入口。
- `react-ui/src/types/seller-buyer/party.d.ts` 补齐端内菜单列表和菜单详情结果类型，并补充 `PortalMenu.remark` 字段。
- 卖家 service 补齐端内菜单维护接口：
  - `getAdminSellerMenus`
  - `getAdminSellerMenu`
  - `addAdminSellerMenu`
  - `updateAdminSellerMenu`
  - `removeAdminSellerMenu`
- 买家 service 按同一模板补齐端内菜单维护接口：
  - `getAdminBuyerMenus`
  - `getAdminBuyerMenu`
  - `addAdminBuyerMenu`
  - `updateAdminBuyerMenu`
  - `removeAdminBuyerMenu`
- `PartnerManagementPage` 公共 service 契约补充：
  - `listMenus`
  - `getMenu`
  - `addMenu`
  - `updateMenu`
  - `removeMenu`
- 卖家/买家管理页工具栏新增“菜单配置”，通过 `seller:admin:menu:list` / `buyer:admin:menu:list` 权限展示。
- 菜单配置弹窗支持：
  - 查看端内菜单树。
  - 新增菜单。
  - 编辑菜单。
  - 删除菜单。
  - 维护上级菜单、菜单类型、菜单名称、显示顺序、图标、外链、路由地址、组件路径、路由参数、路由名称、权限标识、是否缓存、显示状态、菜单状态和备注。
- 本轮没有提交新增/编辑/删除表单，没有写入远程业务数据；浏览器验证只打开弹窗和新增表单。

验证结果：

- `npm run tsc`：通过。
- 浏览器验证：
  - 卖家管理 `/partner/seller` 工具栏展示“菜单配置”。
  - 卖家端菜单配置弹窗可打开。
  - 卖家新增菜单表单可打开，包含上级菜单、菜单类型、菜单名称和权限标识等字段。
  - 卖家浏览器 console 为 `0 errors / 0 warnings`。
  - 买家管理 `/partner/buyer` 工具栏展示“菜单配置”。
  - 买家端菜单配置弹窗可打开。
  - 买家新增菜单表单可打开，包含上级菜单、菜单类型、菜单名称和权限标识等字段。
  - 买家浏览器 console 为 `0 errors / 0 warnings`。
- 截图证据：
  - `logs/screenshots/2026-06-04-seller-menu-modal.png`
  - `logs/screenshots/2026-06-04-buyer-menu-modal.png`

大文件合理性判断：

- `PartnerMenuModal.tsx` 当前约 499 行，触发 400 行判断阈值并接近 500 行阈值；它只承载端内菜单表格、菜单树和菜单表单，职责仍然单一，暂不拆分。
- `PartnerManagementPage.tsx` 当前约 1122 行，属于既有公共主体管理大文件。本轮只增加“菜单配置”入口、状态和 service 契约，具体菜单表格和表单已拆到 `PartnerMenuModal.tsx`。
- 后续日志和审计页面不应继续堆进 `PartnerManagementPage.tsx`，应继续新建独立公共组件或独立页面。

未完成：

- 管理端 ticket 审计列表入口尚未接入。
- 管理端卖家/买家登录日志、操作日志页面尚未接入。

## 2026-06-04 管理端端内部门 UI 检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家一套做好，买家只替换配置和 service”的模板化方式接入管理端卖家端/买家端部门维护 UI。

已完成：

- 新增公共组件 `react-ui/src/components/PartnerManagement/PartnerDeptModal.tsx`，单独承载端内部门列表和新增/编辑表单。
- 卖家 service 补齐端内部门维护接口：
  - `getAdminSellerDept`
  - `addAdminSellerDept`
  - `updateAdminSellerDept`
  - `removeAdminSellerDept`
- 买家 service 补齐端内部门维护接口：
  - `getAdminBuyerDept`
  - `addAdminBuyerDept`
  - `updateAdminBuyerDept`
  - `removeAdminBuyerDept`
- `PartnerManagementPage` 公共 service 契约补充：
  - `listDepts`
  - `addDept`
  - `updateDept`
  - `removeDept`
- 卖家/买家主体行“更多”菜单新增“部门”，通过 `seller:admin:dept:list` / `buyer:admin:dept:list` 权限展示。
- 部门弹窗支持：
  - 查看端内部门列表。
  - 新增部门。
  - 编辑部门。
  - 删除部门。
  - 维护上级部门、部门名称、排序、负责人、电话、邮箱和状态。
- 表格操作继续保持最多两个直接文字操作；部门入口放入主体行“更多”，避免主体列表操作列变宽。

验证结果：

- `npm run tsc`：通过。
- Playwright 浏览器验证：
  - 买家管理 `/partner/buyer` 主体行“更多”展示“部门”。
  - 买家部门弹窗可打开。
  - 买家新增部门表单可打开。
  - 修复新增部门表单 `useForm` 挂载警告后，买家浏览器 console 为 `0 errors / 0 warnings`。
  - 卖家管理 `/partner/seller` 主体行“更多”展示“部门”。
  - 卖家部门弹窗可打开。
  - 卖家浏览器 console 为 `0 errors / 0 warnings`。
- 截图证据：
  - `logs/screenshots/2026-06-04-buyer-dept-modal.png`
  - `logs/screenshots/2026-06-04-seller-dept-modal.png`

大文件合理性判断：

- `PartnerDeptModal.tsx` 独立拆出，避免继续扩大 `PartnerManagementPage.tsx`。
- `PartnerManagementPage.tsx` 本轮只增加部门弹窗入口、状态和 service 契约，未承载部门表格或表单细节。
- 后续日志和审计页面应继续新建独立公共组件或独立页面，不应继续堆进 `PartnerManagementPage.tsx`。

未完成：

- 管理端端内菜单配置和端内角色维护已在后续 UI 检查点完成。
- 管理端 ticket 审计列表入口尚未接入。
- 管理端卖家/买家登录日志、操作日志页面尚未接入。

## 2026-06-04 管理端端内角色 UI 检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按已确认的“卖家一套做好，买家只替换配置和 service”的模板化方式接入管理端卖家端/买家端角色维护 UI。

已完成：

- 新增公共组件 `react-ui/src/components/PartnerManagement/PartnerRoleModal.tsx`，单独承载某个卖家或买家主体下的端内角色列表、新增/编辑表单、状态切换和删除入口。
- `react-ui/src/types/seller-buyer/party.d.ts` 补齐端内角色、端内菜单、角色分页、角色详情、菜单树和角色菜单树类型。
- 卖家 service 补齐端内菜单和角色维护接口：
  - `getAdminSellerMenuTree`
  - `getAdminSellerRoleMenuTree`
  - `getAdminSellerRoles`
  - `getAdminSellerRole`
  - `addAdminSellerRole`
  - `updateAdminSellerRole`
  - `changeAdminSellerRoleStatus`
  - `removeAdminSellerRoles`
- 买家 service 按同一模板补齐端内菜单和角色维护接口：
  - `getAdminBuyerMenuTree`
  - `getAdminBuyerRoleMenuTree`
  - `getAdminBuyerRoles`
  - `getAdminBuyerRole`
  - `addAdminBuyerRole`
  - `updateAdminBuyerRole`
  - `changeAdminBuyerRoleStatus`
  - `removeAdminBuyerRoles`
- `PartnerManagementPage` 公共 service 契约补充：
  - `getMenuTree`
  - `getRoleMenuTree`
  - `listRoles`
  - `getRole`
  - `addRole`
  - `updateRole`
  - `changeRoleStatus`
  - `removeRoles`
- 卖家/买家主体行“更多”菜单新增“角色”，通过 `seller:admin:role:list` / `buyer:admin:role:list` 权限展示。
- 角色弹窗支持：
  - 查看当前主体下的端内角色列表。
  - 新增角色。
  - 编辑角色。
  - 删除角色。
  - 维护角色名称、权限字符、显示顺序、状态、备注和菜单权限树。
- 本轮没有提交新增/编辑/删除表单，没有写入远程业务数据；浏览器验证只打开弹窗和新增表单。

验证结果：

- `npm run tsc`：通过。
- 浏览器验证：
  - 卖家管理 `/partner/seller` 主体行“更多”展示“角色”。
  - 卖家角色弹窗可打开。
  - 卖家新增角色表单可打开，包含角色名称、权限字符、状态和菜单权限。
  - 卖家浏览器 console 为 `0 errors / 0 warnings`。
  - 买家管理 `/partner/buyer` 主体行“更多”展示“角色”。
  - 买家角色弹窗可打开。
  - 买家新增角色表单可打开，包含角色名称、权限字符、状态和菜单权限。
  - 买家浏览器 console 为 `0 errors / 0 warnings`。
- 截图证据：
  - `logs/screenshots/2026-06-04-seller-role-modal.png`
  - `logs/screenshots/2026-06-04-buyer-role-modal.png`

大文件合理性判断：

- `PartnerRoleModal.tsx` 当前约 409 行，触发 400 行判断阈值；它只承载端内角色表格、角色表单和菜单树勾选，职责仍然单一，暂不拆分。
- `PartnerManagementPage.tsx` 当前约 1100 行，属于既有公共主体管理大文件。本轮只增加角色弹窗入口、状态和 service 契约，具体角色表格和表单已拆到 `PartnerRoleModal.tsx`。
- 后续日志和审计页面不应继续堆进 `PartnerManagementPage.tsx`，应继续新建独立公共组件或独立页面。

未完成：

- 管理端端内菜单配置已在后续 UI 检查点完成；日志和审计页面仍未接入。
- 管理端 ticket 审计列表入口尚未接入。
- 管理端卖家/买家登录日志、操作日志页面尚未接入。

## 2026-06-04 管理端 UI 第八批收口检查点

当前管理端卖家/买家页面已按模板化方式完成第一批端内控制 UI：

- 端账号维护：已接入 `PartnerAccountModal`。
- 端账号角色绑定：已接入 `PartnerAccountRoleModal`。
- 端内部门维护：已接入 `PartnerDeptModal`。
- 端内角色维护：已接入 `PartnerRoleModal`。
- 端内菜单维护：已接入 `PartnerMenuModal`。

当前剩余前端控制项：

- 管理端 ticket 审计列表入口尚未接入。
- 管理端卖家/买家登录日志、操作日志页面尚未接入。

验证汇总：

- `npm run tsc`：通过。
- `git diff --check`：通过，仅出现 Git CRLF 提示。
- 浏览器验证已覆盖卖家/买家端账号角色、端内部门、端内角色、端内菜单弹窗；本轮验证未提交新增/编辑/删除表单。

## 2026-06-04 端内权限校验基础设施检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成卖家端、买家端接口级权限校验的基础设施。

已完成：

- 新增 `@PortalPreAuthorize`，用于声明端类型、全部权限要求和任一权限要求。
- 新增 `PortalPreAuthorizeAspect`，在端内接口执行前统一进入权限校验。
- 新增 `PortalPermissionChecker`，统一解析端 token、校验端类型、读取端内权限集合，并支持若依超级权限 `*:*:*`。
- 新增 `IPortalPermissionCheckService`，让卖家端、买家端权限服务以同一契约接入统一校验器。
- `SellerPortalPermissionServiceImpl`、`BuyerPortalPermissionServiceImpl` 已实现该契约。
- `PortalPermissionSupport` 已补齐权限匹配方法。
- 新增 `PortalPermissionSupportTest` 覆盖端内权限匹配规则。
- `/seller/getInfo`、`/seller/getRouters`、`/buyer/getInfo`、`/buyer/getRouters` 已接入 `@PortalPreAuthorize`。
- 登录失效通过端内权限守卫返回 `code=401`，权限不足返回 `code=403`。
- `AGENTS.md` 已补充：已确认的同构管理端 UI 模式按模板化推进，卖家侧做好后买家侧只替换端类型、文案、路由、权限标识、字段配置和 service。

验证结果：

- `mvn -pl ruoyi-system -DargLine="-Djdk.net.URLClassPath.disableClassPathURLCheck=true" test`：通过。
- `mvn -DskipTests compile`：通过。
- `mvn -DskipTests install`：通过。
- `.\start-backend-local.ps1 -Restart`：后端已重启。
- 管理端登录成功。
- 卖家端免密登录后 `/seller/getInfo` 返回 `code=200`。
- 买家端免密登录后 `/buyer/getInfo` 返回 `code=200`。
- 无 token 访问 `/seller/getInfo` 返回 `code=401`。
- 卖家端 token 访问 `/seller/getRouters` 返回 `code=200`。
- 买家端 token 访问 `/buyer/getRouters` 返回 `code=200`。
- 卖家端 token 访问 `/buyer/getInfo` 返回 `code=401`。

未完成：

- 卖家端、买家端会话接口已接入 `@PortalPreAuthorize`；真实端内业务接口尚未批量接入。
- 后续真实端内业务接口仍必须从端 token 推导 `sellerId` / `buyerId`，不能相信前端传入主体 ID。
- 本轮未改管理端前端代码，未运行 `npm run tsc`。

## 2026-06-04 旧方向文档审计检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，完成阶段 0 中“审计当前文档，标记旧方向文档为过期或待迁移”的收口。

已标记过期的历史方案：

- `docs/architecture/2026-06-03-three-portal-buyer-seller-plan.md`
- `docs/architecture/2026-06-03-seller-buyer-split-module-design.md`
- `docs/architecture/2026-06-03-admin-portal-start-plan.md`
- `docs/architecture/2026-06-03-admin-customer-field-alignment.md`
- `docs/architecture/2026-06-03-remote-seller-buyer-table-migration-plan.md`
- `docs/architecture/2026-06-03-multi-user-marketplace-best-architecture.md`
- `docs/plans/2026-06-04-seller-buyer-operations-implementation-plan.md`
- `docs/plans/2026-06-04-seller-buyer-full-remediation-plan.md`

说明：

- 上述文档作为历史记录保留，正文不删除。
- 其中“卖家/买家账号继续复用若依 `sys_user` / `sys_role` / `sys_menu`”的设计已废弃。
- 后续三端独立账号权限改造只以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 和本目标追踪为准。

代码同步修复：

- 修复 `AdminSellerController` 中卖家账号编辑操作日志标题乱码。
- 修复 `AdminBuyerController` 中买家账号编辑操作日志标题乱码。

验证结果：

- `rg -n -F "鍗"`：卖家、买家管理相关代码无匹配。
- `rg -n -F "涔"`：卖家、买家管理相关代码无匹配。
- `rg -n -F "璐"`：卖家、买家管理相关代码无匹配。

## 2026-06-04 端内会话上下文检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，推进“端内业务接口从端 token 推导主体范围，不相信前端传入 `sellerId` / `buyerId`”的基础能力。

已完成：

- 新增 `PortalSessionContext`，用 ThreadLocal 保存当前请求内已校验通过的 `PortalLoginSession`。
- `PortalPreAuthorizeAspect` 在权限校验通过后写入 `PortalSessionContext`，并在请求结束时恢复或清理。
- `PortalLogAspect` 优先从 `PortalSessionContext` 读取会话，只有缺少上下文时才回退到 token 解析。
- `/seller/getInfo`、`/seller/getRouters` 已改为从 `PortalSessionContext.requireSession("seller")` 获取端内会话。
- `/buyer/getInfo`、`/buyer/getRouters` 已改为从 `PortalSessionContext.requireSession("buyer")` 获取端内会话。
- 新增 `PortalSessionContextTest`，覆盖同端读取、跨端拒绝和清理行为。

验证结果：

- `mvn -DskipTests compile`：通过。
- `mvn -pl ruoyi-system -DargLine="-Djdk.net.URLClassPath.disableClassPathURLCheck=true" test`：通过，`PortalPermissionSupportTest` 4 条、`PortalSessionContextTest` 3 条。
- `mvn -DskipTests install`：通过。
- `.\start-backend-local.ps1 -Restart`：后端已重启，8080 正常监听。
- 管理端登录：`code=200`。
- 卖家端免密登录后 `/seller/getInfo`、`/seller/getRouters` 均返回 `code=200`。
- 买家端免密登录后 `/buyer/getInfo`、`/buyer/getRouters` 均返回 `code=200`。
- 卖家端 token 访问 `/buyer/getInfo` 返回 `code=401`。

未完成：

- 当前只是给真实业务接口准备统一会话入口；后续真实 seller/buyer 业务接口仍需逐个使用 `PortalSessionContext.requireSession(...)` 派生主体范围。
