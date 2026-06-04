# 端内部门/角色只读列表权限 SQL 执行记录

日期：2026-06-04

## 目标

为卖家端、买家端补齐以下端内只读权限，并授权给当前启用的端内角色：

- `seller:dept:list`
- `seller:role:list`
- `buyer:dept:list`
- `buyer:role:list`

本次只补权限菜单和角色菜单绑定，不新增表，不修改表结构，不写入业务事实数据。

## 参考方向

- `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`
- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`

## 执行前确认

- SQL 文件：`RuoYi-Vue/sql/20260604_portal_dept_role_list_permission_seed.sql`
- 数据源来源：本机 `.env.local` 中的 `RUOYI_DB_*`
- 目标环境：以后端本机启动脚本读取的远程 MySQL 为准
- 凭证处理：不在文档、日志或对话中输出数据库 URL、用户名、密码
- 影响范围：`seller_menu`、`buyer_menu`、`seller_role_menu`、`buyer_role_menu`

## SQL 执行结果

- 执行方式：使用本机 Maven 依赖中的 MySQL JDBC 驱动执行 SQL 文件。
- 第一次执行：jshell 输入被 BOM 干扰，未执行 SQL，未提交事务。
- 第二次执行：成功提交。
- 执行语句影响行数：
  - `statement_1_rows=1`，新增 `seller:dept:list`。
  - `statement_2_rows=1`，新增 `seller:role:list`。
  - `statement_3_rows=1`，新增 `buyer:dept:list`。
  - `statement_4_rows=1`，新增 `buyer:role:list`。
  - `statement_5_rows=6`，卖家端启用角色绑定部门/角色只读权限。
  - `statement_6_rows=2`，买家端启用角色绑定部门/角色只读权限。

## 执行后数据校验

- `seller_menu` 中 `seller:dept:list` 数量：1。
- `seller_menu` 中 `seller:role:list` 数量：1。
- `buyer_menu` 中 `buyer:dept:list` 数量：1。
- `buyer_menu` 中 `buyer:role:list` 数量：1。
- 启用卖家端角色绑定部门/角色只读权限数量：6。
- 启用买家端角色绑定部门/角色只读权限数量：2。

## 接口烟测

- `mvn -DskipTests install` 首次执行时，Java 编译已通过，但 `ruoyi-admin.jar` 被运行中的 8080 后端进程锁定，Spring Boot repackage 无法重命名 jar。
- 停止 8080 后端进程后执行 `mvn -DskipTests install -rf :ruoyi-admin`：通过。
- 使用 `.\start-backend-local.ps1 -Restart` 启动后端，`http://127.0.0.1:8080` 返回 200。
- 管理端登录：`code=200`。
- 选中烟测主体：
  - `sellerId=9`
  - `buyerId=2`
- 管理端生成免密票据：
  - 卖家端 `ticketId=36`
  - 买家端 `ticketId=37`
- 消费免密票据：
  - 卖家端：`code=200`，`accountId=8`
  - 买家端：`code=200`，`accountId=2`
- 卖家端只读接口：
  - `GET /seller/depts` 返回 `code=200`，数量 0。
  - `GET /seller/roles` 返回 `code=200`，数量 1。
  - 返回字段未发现 `password`、`createBy`、`updateBy`、`delFlag`、`remark`。
- 买家端只读接口：
  - `GET /buyer/depts` 返回 `code=200`，数量 0。
  - `GET /buyer/roles` 返回 `code=200`，数量 1。
  - 返回字段未发现 `password`、`createBy`、`updateBy`、`delFlag`、`remark`。
- 跨端拒绝：
  - 卖家端 token 访问 `GET /buyer/depts` 返回 `code=401`。
  - 卖家端 token 访问 `GET /buyer/roles` 返回 `code=401`。
  - 买家端 token 访问 `GET /seller/depts` 返回 `code=401`。
  - 买家端 token 访问 `GET /seller/roles` 返回 `code=401`。
- 端内操作日志：
  - `seller_oper_log` 可查到 `/seller/depts` 和 `/seller/roles`。
  - `buyer_oper_log` 可查到 `/buyer/depts` 和 `/buyer/roles`。
- 会话清理：
  - 卖家端账号级强制踢出返回 `code=200`，清理 1 条。
  - 买家端账号级强制踢出返回 `code=200`，清理 1 条。

## 验证命令

- `mvn -DskipTests install`
- `mvn -DskipTests install -rf :ruoyi-admin`
- `.\start-backend-local.ps1 -Restart`
- PowerShell HTTP 烟测脚本：管理端登录、免密票据生成和消费、四个端内只读接口、跨端 401、操作日志查询、强制踢出。
- `npm run tsc`：通过。
- `mvn -pl ruoyi-system -DargLine="-Djdk.net.URLClassPath.disableClassPathURLCheck=true" test`：通过，`Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- <本轮触碰文件>`：通过，仅有 Git LF/CRLF 换行提示。

## 未验证事项

- 本轮没有新增或修改表结构。
- 本轮没有启动 React 前端页面做浏览器 UI 验证；本次范围是后端端内只读接口、权限 seed 和前端 service/type 契约。
