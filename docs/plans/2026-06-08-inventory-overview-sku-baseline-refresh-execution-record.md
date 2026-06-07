# 库存总览 SKU 基线刷新 SQL 执行记录

日期：2026-06-08

## 执行前确认

- 用户确认：用户在对话中回复“执行”。
- 配置来源：
  - 后端激活配置：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`
  - 激活 profile：`druid`
  - 数据源配置：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`
  - 实际 MySQL 连接变量来源：`.env.local` 中的 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`
- 目标环境分类：`REMOTE_OR_NON_LOOPBACK`
- 明文连接信息：未写入本记录。
- 执行工具：本机没有 `mysql` PATH 命令；使用 Maven 缓存中的 MySQL JDBC 驱动执行。
- SQL 文件：`RuoYi-Vue/sql/20260608_inventory_overview_sku_baseline_refresh.sql`
- 确认 token：`APPLY_INVENTORY_OVERVIEW_SKU_BASELINE_REFRESH`

## 执行结果

- 执行语句数：30
- 执行结果：成功。

## 数据验证

执行后库存总览读模型数据：

| 表 | 行数 |
| --- | ---: |
| `inventory_sku_warehouse_stock` | 69 |
| `inventory_overview_sku_read_model` | 69 |
| `inventory_overview_spu_read_model` | 24 |

状态分布：

| 范围 | 状态 | 行数 |
| --- | --- | ---: |
| SKU + 仓库明细 | `NO_WAREHOUSE` | 69 |
| SKU 读模型 | `NO_WAREHOUSE` | 69 |
| SPU 读模型 | `NO_WAREHOUSE` | 24 |

说明：当前运行库 `product_sku_source_binding` 和 `product_spu_warehouse` 仍为空，因此本次生成的是商城 SKU 基线占位行，状态为 `仓库未配置`。后续商品绑定来源 SKU 或配置发货仓后，读模型刷新逻辑会转为来源主仓行或三方仓行。

## 后端验证

- 第一次 `mvn -pl ruoyi-admin -am -DskipTests package` 在 `spring-boot:repackage` 阶段失败，原因是旧 8080 后端进程锁定 `ruoyi-admin.jar`，不是编译错误。
- 已停止旧后端进程。
- 第二次 `mvn -pl ruoyi-admin -am -DskipTests package` 成功。
- 已执行 `.\start-backend-local.ps1 -Restart`。
- 8080 已监听，`http://127.0.0.1:8080` 返回 HTTP 200。
- 登录后接口验证：
  - `GET /inventory/admin/overview/spu/list?pageNum=1&pageSize=10` 返回 `code=200,total=24`，首行状态 `NO_WAREHOUSE`。
  - `GET /inventory/admin/overview/sku/list?pageNum=1&pageSize=10` 返回 `code=200,total=69`，首行状态 `NO_WAREHOUSE`。
  - `GET /inventory/admin/overview/sku/{skuId}/warehouses` 返回 `code=200`，首个 SKU 有 1 条 `NO_WAREHOUSE` 明细行。

## 前端验证

- 前端 dev server：`http://127.0.0.1:8001`。
- 浏览器打开 `http://127.0.0.1:8001/inventory/overview`。
- 登录后进入库存总览成功。
- SPU 视图已显示 24 个 SPU 聚合行，状态为 `仓库未配置`。
- SKU 视图已显示 69 个 SKU 行，状态为 `仓库未配置`。
- 浏览器控制台未发现错误日志。

## 回滚说明

本脚本未新增表结构，但已经写入字典和库存占位行，并重建读模型。若需回滚：

1. 删除本次新增的 `NO_WAREHOUSE` / `SOURCE_UNBOUND` 字典数据。
2. 删除 `inventory_sku_warehouse_stock` 中 `warehouse_ref_type in ('NO_WAREHOUSE', 'SOURCE_UNBOUND')` 且尚无业务流水依赖的占位行。
3. 清空并按上一版逻辑重建 `inventory_overview_sku_read_model` / `inventory_overview_spu_read_model`。

如占位行已经产生库存流水，不建议物理删除，应改为状态作废并保留流水追溯。
