# 商品审核 SQL 执行记录

日期：2026-06-08

## 执行前确认

- 用户确认：已在当前对话中确认执行。
- SQL 文件：`RuoYi-Vue/sql/20260608_product_review.sql`
- 激活配置来源：
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`
  - 本机 `.env.local` 的 `RUOYI_*` 变量
- 激活 profile：`druid`
- 目标 MySQL：
  - 远端运行库，地址已脱敏
- 目标 Redis：
  - 远端 Redis，地址已脱敏
- 执行方式：
  - 本机没有可用 `mysql` CLI。
  - 使用本机 Maven 缓存中的 MySQL JDBC 驱动执行 SQL。
  - 仅读取 `.env.local` 注入连接，不在记录或聊天中输出密码、token。
- SQL 确认 token：
  - 执行前设置 `@confirm_product_review = 'APPLY_PRODUCT_REVIEW'`。
- 影响范围：
  - 新增商品审核表。
  - 新增商品审核字典。
  - 更新 `sys_menu.menu_id=2451` 为 `商品审核` 并指向 `Product/Review/index`。
  - 新增商品审核按钮权限菜单。
- 回滚方式：
  - 未自动执行回滚。
  - 如需回滚，需按执行结果人工确认后删除新增审核表、字典项和菜单按钮，并恢复 `2451` 菜单名称及组件。

## 执行结果

- 第一次执行：
  - 前置建表和字典语句已执行到菜单 slot guard 前。
  - 在 `call assert_product_review_sys_menu_guard()` 处失败。
  - 失败原因：MySQL 临时表在同一 guard 查询中被重复打开，报错 `Can't reopen table: 'seed'`。
- 修复：
  - 将 `20260608_product_review.sql` 中的菜单 slot guard 从重复子查询改为单次 `join tmp_product_review_sys_menu_guard seed`。
- 第二次执行：
  - 重放同一个幂等脚本。
  - 执行成功。
  - JDBC 执行器统计 `executedStatements=26`。
- 后端部署：
  - 第一次 `mvn -pl ruoyi-admin -am -DskipTests package` 因旧 8080 Java 进程锁定 `ruoyi-admin.jar`，在 repackage 阶段失败。
  - 停止旧后端进程后重新打包成功。
  - 已通过 `start-backend-local.ps1` 启动新 jar。
  - 8080 已监听，`http://127.0.0.1:8080` 返回 HTTP 200。

## 验证结果

- 当前 database：远端运行库，名称已脱敏
- 审核表：
  - `product_review_request`
  - `product_review_item`
  - `product_review_snapshot`
  - `product_review_operation_log`
- 字典：
  - `product_review_type`：5 项
  - `product_review_status`：4 项
  - `product_review_risk_level`：3 项
- 菜单：
  - `menu_id=2451`
  - `menu_name=商品审核`
  - `parent_id=2100`
  - `path=product-distribution`
  - `component=Product/Review/index`
  - `perms=review:productDistribution:list`
- 按钮权限：
  - `2491`：`review:productDistribution:query`
  - `2492`：`review:productDistribution:approve`
  - `2493`：`review:productDistribution:reject`
  - `2494`：`review:productDistribution:log`
- 运行态 API：
  - admin 登录成功。
  - `GET /product/admin/reviews/list?pageNum=1&pageSize=5` 返回 `code=200`，当前 `total=0`。
  - `GET /getRouters` 能找到 `ProductDistributionReview`，组件为 `Product/Review/index`。
- 追加验证：
  - `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，64 个测试通过。
  - `mvn -pl product -am -DskipTests compile`：通过。
  - `mvn -pl ruoyi-admin -am -DskipTests package`：停止旧进程后通过。
