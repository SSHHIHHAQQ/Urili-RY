# 库存总览平台库存增量 SQL 执行记录

日期：2026-06-07

## 执行前确认

- 用户确认：用户在对话中回复“执行”。
- 配置来源：
  - 后端激活配置：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`
  - 激活 profile：`druid`
  - 数据源配置：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`
  - 实际 MySQL 连接变量来源：`.env.local` 中的 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`
- 目标环境分类：`REMOTE_OR_NON_LOOPBACK`
- 明文连接信息：未写入本记录，未在聊天输出。
- 执行工具：本机没有 `mysql` PATH 命令；使用 Maven 缓存中的 MySQL JDBC 驱动执行。
- SQL 文件：`RuoYi-Vue/sql/20260607_inventory_overview_platform_stock.sql`
- 确认 token：`APPLY_INVENTORY_OVERVIEW_PLATFORM_STOCK`

## 影响范围

- 新增库存总览业务表：
  - `inventory_sku_warehouse_stock`
  - `inventory_stock_ledger`
  - `inventory_reservation`
  - `inventory_source_deduction_pending`
  - `inventory_in_transit_tracking`
  - `inventory_overview_sku_read_model`
  - `inventory_overview_spu_read_model`
- 写入或更新字典：
  - `inventory_status`
  - `inventory_operation_type`
- 写入或更新菜单：
  - `2420` 库存总览
  - `242001` 库存总览查询
  - `242002` 库存调整
  - `242003` 库存流水
  - `242004` 库存导出
- 回填库存读模型：
  - 官方仓：基于来源仓库库存正品综合库存和 SKU 来源绑定生成 SKU + 来源主仓库存行。
  - 三方仓：基于 `product_spu_warehouse` 生成 SKU + 三方仓库存行。
  - SKU/SPU 汇总：基于库存行生成永久读模型。

## 执行结果

- 第一次执行：
  - 目标环境分类：`REMOTE_OR_NON_LOOPBACK`
  - 预检结果：必要基础表存在。
  - 结果：失败于 `call assert_inventory_overview_sys_menu_guard()`。
  - 错误：`Can't reopen table: 'seed'`。
  - 影响：失败发生在库存业务表 DDL 前，未创建库存总览业务表。
- 修复动作：
  - 修正 `RuoYi-Vue/sql/20260607_inventory_overview_platform_stock.sql` 中的菜单 guard。
  - 将同一临时表的重复引用改为 `tmp_inventory_overview_sys_menu_guard_ids` + `tmp_inventory_overview_sys_menu_guard` 各引用一次，避免 MySQL 临时表重开限制。
  - 重新执行 `mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test`，37 个测试通过。
- 第二次执行：
  - 目标环境分类：`REMOTE_OR_NON_LOOPBACK`
  - 预检结果：必要基础表存在。
  - 执行语句数：`54`
  - 执行结果：成功。

## 验证结果

- 新表与数据行数：
  - `inventory_sku_warehouse_stock`：0
  - `inventory_stock_ledger`：0
  - `inventory_reservation`：0
  - `inventory_source_deduction_pending`：0
  - `inventory_in_transit_tracking`：0
  - `inventory_overview_sku_read_model`：0
  - `inventory_overview_spu_read_model`：0
- 菜单和字典：
  - `sys_menu` 中 `2420/242001/242002/242003/242004` 共 5 条。
  - `2420` 已指向 `Inventory/Overview/index`，权限为 `inventory:overview:list`。
  - `inventory_status` 字典数据 5 条。
  - `inventory_operation_type` 字典数据 8 条。
- 当前基础数据：
  - `product_spu` 有效数据 24 条。
  - `product_sku` 有效数据 69 条。
  - `product_sku_source_binding` 0 条。
  - `product_spu_warehouse` 0 条。
  - `source_warehouse_stock_detail` 官方仓正品综合库存 3096 条。
  - 官方仓 join 候选 0 条，三方仓候选 0 条。
- 读模型为空原因：
  - 当前运行库尚无商城 SKU 来源绑定，也无商城 SPU 发货仓绑定，因此库存总览没有可回填的 SKU + 仓库库存行。
- 后端验证：
  - 已停止旧 8080 后端。
  - 已执行 `mvn -pl ruoyi-admin -am -DskipTests package`，完整 Spring Boot jar 重新打包成功。
  - 已执行 `.\start-backend-local.ps1 -Restart`，8080 后端启动成功。
  - 未登录访问库存总览接口返回业务 `401`，说明安全链路接管请求。
  - 登录后 `GET /inventory/admin/overview/spu/list?pageNum=1&pageSize=10` 返回 `code=200,total=0`。
  - 登录后 `GET /inventory/admin/overview/sku/list?pageNum=1&pageSize=10` 返回 `code=200,total=0`。
  - 登录后 `/getRouters` 返回库存总览路由：`InventoryOverview / overview / Inventory/Overview/index / inventory:overview:list`。
- 前端验证：
  - 默认前端 dev server 启动遇到 Umi MFSU 缓存冲突。
  - 使用 `DISABLE_MFSU=1` 启动 `http://127.0.0.1:8001` 成功。
  - 浏览器打开 `http://127.0.0.1:8001/inventory/overview` 成功。
  - SPU 视图可渲染，筛选区保留在 ProTable 原查询区，表格为空数据。
  - SKU 视图可切换并渲染，表格为空数据。
  - 浏览器控制台未发现错误日志。

## 回滚说明

本脚本包含 DDL、字典/菜单 DML 和读模型回填，MySQL DDL 存在隐式提交，不承诺普通事务回滚。若需回滚，应单独编写反向 SQL，按表、菜单、字典和回填数据影响范围逐项确认后执行。
