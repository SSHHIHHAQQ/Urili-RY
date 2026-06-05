# 管理端卖家会话列表后端模板执行记录

日期：2026-06-04

## 背景

本次切片以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按“卖家先做标准模板，验收通过后复制买家”的节奏推进。

目标是补齐管理端对卖家端 session 的只读查看能力，让管理端在强制踢出前后可以按卖家主体或卖家端账号查看会话列表。

## 变更范围

代码变更：

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/ISellerService.java`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
- `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml`

文档变更：

- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`
- `docs/architecture/reuse-ledger.md`
- `docs/plans/2026-06-04-admin-seller-session-list-execution-record.md`

本轮未改：

- 不改 buyer 代码。
- 不新增 DDL/DML。
- 不新增菜单权限。
- 不改前端。

## 接口

新增管理端只读接口：

- `GET /seller/admin/sellers/{sellerId}/sessions/list`
- `GET /seller/admin/sellers/{sellerId}/accounts/{accountId}/sessions/list`

权限：

- 两个接口复用 `seller:admin:forceLogout`。
- 理由：会话列表包含登录 IP、登录时间、过期时间、退出时间等控制面信息；能强制踢出的管理员才允许查看。

响应：

- 复用 `PortalSessionProfile`。
- `tokenId` 保持 `@JsonIgnore`，不输出给前端。
- 不返回 JWT、Redis key、密码、免密 token 或 directLogin URL。

## 数据源确认

连接来源：

- 后端配置激活 `druid`。
- MySQL/Redis 均通过 `.env.local` 中的 `RUOYI_*` 运行变量注入。
- 本记录不输出远程 MySQL/Redis 地址、账号、密码、Redis 密码或 token secret。

执行环境：

- 后端地址：`http://127.0.0.1:8080`
- 登录账号：`admin / admin123`

## 远程 DDL/DML

本轮没有新增业务 DDL。

本轮没有直接执行远程 SQL。

接口烟测为了创建可查询的 seller session，执行了正常业务链路：

- 管理端生成卖家端免密票据。
- 卖家端消费免密票据生成 seller token 和 `seller_session`。
- 验证后调用 `/seller/logout` 清理本轮 seller token。
- 最后调用管理端主体级强制清理接口确认没有额外在线测试会话遗留。

## 实现说明

- `AdminSellerController` 增加主体级和账号级 session 列表 GET 接口。
- `ISellerService` / `SellerServiceImpl` 增加只读查询方法。
- `SellerMapper.xml` 的 `selectSellerSessionProfileList` 支持 `sellerAccountId` 可选：
  - 不传 `sellerAccountId`：查询某个卖家主体下的 session。
  - 传 `sellerAccountId`：查询某个卖家端账号的 session。
- RuoYi `startPage()` 只作用于 Service 内第一条查询；本轮修正了先做前置账号校验会消耗分页的问题。列表 Service 方法不做前置校验查询，数据范围由最终 SQL 中的 `seller_id` / `seller_account_id` 限定。

## 验证结果

- `mvn -DskipTests compile`：通过。
- 停止旧 8080 后端进程后执行 `mvn -DskipTests package`：通过。
- `.\start-backend-local.ps1 -Restart`：成功。
- `/captchaImage`：`code=200`，`captchaEnabled=false`。
- 管理端登录：`code=200`。
- 管理端卖家列表：`code=200`。
- 管理端生成卖家端免密票据：`code=200`。
- 卖家端消费免密票据：`code=200`。
- `GET /seller/admin/sellers/{sellerId}/sessions/list?pageNum=1&pageSize=10`：`code=200`。
- `GET /seller/admin/sellers/{sellerId}/accounts/{accountId}/sessions/list?pageNum=1&pageSize=10`：`code=200`。
- 主体级会话列表未输出 `tokenId`。
- 账号级会话列表未输出 `tokenId`。
- `/seller/logout`：`code=200`。
- 管理端主体级强制清理：`code=200`，`data=0`。

## 检查项

- 权限检查结果：复用管理端 `seller:admin:forceLogout`，未新增权限点；接口没有使用端内 `@PortalPreAuthorize`。
- 字典/选项复用检查结果：本轮不涉及字典或前端选项。
- 复用台账检查结果：已更新 `docs/architecture/reuse-ledger.md` 的管理端端内会话列表查询接口规则。
- CodeGraph 更新结果：已执行 `codegraph sync .`，结果为 `Already up to date`。
- 大文件合理性判断结果：本轮只在既有 Controller、Service、Mapper XML 中增加小段只读查询逻辑，未引入新增大文件。
- 重复代码检查结果：当前只做 seller 标准模板；buyer 尚未复制，后续按该模板替换端类型和字段名，不重新设计。

## 当前判断

- 管理端卖家 session 列表后端模板已落地。
- 该能力是只读查询，不改变现有强制踢出行为。
- buyer 端已在后续执行记录中按同一模板复制完成。
- 管理端前端 session 列表 UI 尚未接入。
