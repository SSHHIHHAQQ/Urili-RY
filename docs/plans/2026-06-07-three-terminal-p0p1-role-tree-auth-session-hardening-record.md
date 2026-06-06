# 三端 P0/P1 角色树、登录与会话守卫收口记录

记录时间：2026-06-07 00:00 +08:00

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

## 本轮范围

当前继续执行三端独立账号权限改造，管理端保留若依 `sys_*`，卖家端和买家端继续按独立账号、角色、菜单、部门、日志、会话体系推进。

本轮只处理 P0/P1：

- 编译失败、验证入口失败。
- guard 缺失或不完整。
- 接口权限串线、端内 service 字段缺失。
- 卖家模板代码级通过后机械复制买家。

本轮明确不做：

- 浏览器运行态验收。
- 截图检查。
- DOM 检测。
- UI 细调和列宽微调。
- 远程 MySQL DDL/DML。
- Redis 读写。
- 后端重启。

## 子 Agent 执行情况

- 用户最新指定：子 Agent 优先使用 GPT-5.3 Codex；不可用时降级使用 `gpt-5.4`。
- 本轮延续并收口上一批 6 个 `gpt-5.4` 子 Agent 的只读审计结论。
- 6 个子 Agent 均已完成并关闭。
- 本轮未新增子 Agent。

## 已完成问题

### 管理端 PartnerManagement 权限闭环

- `PartnerManagementPage` 的编辑按钮增加 `edit + query` 闭环，避免没有查询权限时打开编辑弹窗再调用详情接口。
- `PartnerDeptModal` 拆分部门列表、部门树、添加、编辑权限：
  - 列表只要求 `dept:list`。
  - 添加要求 `dept:add + dept:query`。
  - 编辑要求 `dept:edit + dept:query`。
  - 部门树只在存在 `dept:query` 时加载。
- `PartnerRoleModal` 拆分角色查询、菜单树查询、添加、编辑、状态切换权限：
  - 添加要求 `role:add + menu:query`。
  - 编辑表单要求 `role:edit + role:query`。
  - 状态切换保持要求 `role:edit`。
- `PartnerMenuModal` 的编辑入口增加 `menu:edit + menu:query`。
- `PartnerSessionModal` 支持会话状态 `2` 展示为“已过期”，避免过期状态落成未知展示。
- `check-partner-management-template.mjs` 同步检查 `.tsx` 和 `.js` sidecar，固定上述权限闭环。

### 卖家/买家端角色、菜单、部门树防护

- `SellerPortalPermissionServiceImpl` 和 `BuyerPortalPermissionServiceImpl` 固定 owner 角色保护：
  - owner 角色不可修改。
  - owner 角色不可停用。
  - owner 角色不可删除。
- owner 账号角色绑定保护：
  - owner 账号不能清空角色。
  - owner 账号不能移除启用状态的 owner 角色绑定。
- seller/buyer 菜单新增和编辑增加父级存在性校验。
- seller/buyer 菜单编辑禁止把菜单移动到自己的子孙节点下面。
- `PortalDeptSupport` 增加部门父级子孙环检测。
- `SellerPortalDeptServiceImpl` 和 `BuyerPortalDeptServiceImpl` 在更新部门祖级前调用环检测，防止部门树写成循环结构。

### 端内登录、黑名单与直登票据

- seller/buyer 普通密码登录增加最小登录前置检查：
  - 用户名长度。
  - 密码长度。
  - `sys.login.blackIPList` 黑名单。
- 本轮没有恢复或触碰验证码开关，也没有把 seller/buyer 登录接入验证码，因为用户此前明确关闭过验证码。
- `PortalDirectLoginSupport.consumeToken` 增加 `sys.login.blackIPList` 黑名单检查。
- 当 DB 免密票据存在但 Redis payload 丢失时，系统将 DB 票据置为 `EXPIRED`，删除对应缓存 key 后再抛出异常，避免票据长期停留在可疑未消费状态。

### 会话过期状态和 Product service 缺口

- seller/buyer session profile mapper 对 `status='0'`、未登出且 `expire_time < sysdate()` 的记录派生状态 `2`，避免过期会话仍显示在线。
- `ProductPortalSchemaServiceImpl` 对分类不存在增加受控异常，避免空指针。
- product 模块补 JUnit 测试依赖，允许模块内 service 单测独立运行。

## 新增或增强测试

- `PortalDeptSupportTest`
  - 覆盖部门父级不能移动到自身子孙节点。
- `PortalDirectLoginSupportTest`
  - 覆盖 Redis payload 丢失时 DB 票据置为 `EXPIRED`。
  - 覆盖黑名单 IP 在消费前拒绝免密票据。
- `SellerPortalPermissionServiceImplTest`
  - 覆盖 owner 账号必须保留 owner 角色。
  - 覆盖 owner 角色不可修改。
- `BuyerPortalPermissionServiceImplTest`
  - 覆盖 owner 账号必须保留 owner 角色。
  - 覆盖 owner 角色不可修改。
- `SellerPortalPermissionServiceImplMenuTreeTest`
  - 覆盖菜单不能移动到自己的子孙节点。
- `BuyerPortalPermissionServiceImplMenuTreeTest`
  - 覆盖菜单不能移动到自己的子孙节点。
- `ProductPortalSchemaServiceImplTest`
  - 覆盖分类不存在时返回受控异常。
- `verify-three-terminal.mjs`
  - 纳入 `PortalDeptSupportTest`。
  - 保持三端验证入口收口。

## 验证结果

已通过：

- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\check-partner-management-template.mjs`
- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=PortalDirectLoginSupportTest,PortalDeptSupportTest" test`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product "-Dtest=ProductPortalSchemaServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product "-Dmaven.test.skip=true" install`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller "-Dtest=SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplMenuTreeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer "-Dtest=BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplMenuTreeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
- `cd E:\Urili-Ruoyi; git diff --check`
- `cd E:\Urili-Ruoyi; codegraph sync .`

`npm run verify:three-terminal` 最终通过，覆盖：

- portal token guard。
- partner management guard。
- seller/buyer portal product guards。
- React TypeScript 编译。
- Jest：3 个 test suite、9 个测试通过。
- 后端 reactor：`ruoyi-common`、`ruoyi-system`、`ruoyi-framework`、`finance`、`integration`、`warehouse`、`product`、`seller`、`buyer` 均通过。
- 后端测试计数：`ruoyi-system` 93 个、`ruoyi-framework` 15 个、seller 68 个、buyer 69 个。
- 最终输出：`three-terminal verification passed.`

## 权限检查结果

- 管理端 PartnerManagement 的编辑、添加、树查询、状态切换入口已按对应权限闭环。
- seller/buyer owner 角色和 owner 账号绑定已做 service 层强制保护。
- seller/buyer 菜单树和部门树已补父子循环防护。
- 免密直登消费已加入 IP 黑名单检查。

## 字典/选项复用检查结果

- 本轮未新增字典。
- 本轮未新增业务下拉选项。
- 会话状态 `2` 只作为既有 session 状态派生展示补齐，不新增数据库字典项。

## 复用台账检查结果

- 已补充 `docs/architecture/reuse-ledger.md`：
  - PartnerManagement RBAC 闭环模板。
  - owner 角色和 owner 账号保护模板。
  - PortalDeptSupport 树环检测模板。
  - PortalDirectLoginSupport 票据异常收口模板。
  - session profile 过期状态派生模板。
  - Product portal schema 受控异常模板。

## 大文件合理性判断

- 本轮只在既有 service、mapper、modal 和 guard 文件上做局部增强。
- 新增测试文件职责单一。
- 未新增超过 300 行的大文件。

## 重复代码检查结果

- seller/buyer 仍是端隔离下的同构实现，本轮按用户指定的“卖家模板通过后机械复制买家”推进。
- 跨端共同逻辑优先收口到 `PortalDeptSupport`、`PortalDirectLoginSupport`、`verify-three-terminal.mjs` 和 `check-partner-management-template.mjs`。
- 未新增第三套端内权限实现。

## 残留问题

P2 记录，不阻塞本轮：

- seller/buyer session 的 `login_location`、`browser`、`os` 等设备字段后续需要 DDL 和写入链路补齐。
- 强制踢出如果要记录原因和执行人，需要后续补 DDL 和审计字段。
- SQL seed 的自包含授权、配置污染、菜单自动展开等仍需要后续单独切片治理。
- seller/buyer 密码登录暂未接入验证码；本轮只做黑名单和长度前置检查，未修改验证码开关。

## CodeGraph

- `codegraph sync .`：通过。
- 结果：`Synced 48 changed files`，`Added: 7, Modified: 41 - 2,276 nodes in 1.7s`。
- 回填记录后最终复跑：`Already up to date`。

## git diff 检查

- `git diff --check`：通过。
- 仅有 Git 工作区 LF/CRLF 替换提示，无 whitespace 错误。
