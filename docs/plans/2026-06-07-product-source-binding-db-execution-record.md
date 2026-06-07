# 商城商品官方来源 SKU 绑定数据库执行记录

## 执行目标

本次执行用于落地商城商品侧官方来源 SKU 绑定关系。

已确认的第一版规则是：一个官方来源 SKU 组全局只能绑定一个商城 SKU；一个商城 SKU 同时只能存在一个有效来源绑定。

## 目标环境

- 执行日期：2026-06-07
- 数据库配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 激活 `druid`，`application-druid.yml` 从 `.env.local` 读取 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`
- 目标库：`fenxiao`
- 目标地址：`gz-cynosdbmysql-grp-lucf5kyf.sql.tencentcdb.com:28634`
- 是否本地 Docker 库：否
- 敏感信息处理：未在记录中输出数据库密码、Redis 密码或 token secret

## 执行内容

按确认顺序执行：

1. `RuoYi-Vue/sql/20260607_upstream_pairing_role_binding.sql`
2. `RuoYi-Vue/sql/20260607_product_sku_source_binding.sql`

执行方式：

- 本机没有可用 `mysql` 命令行客户端。
- 使用项目本机 Maven 仓库中的 `mysql-connector-j-9.6.0.jar`，通过 JDBC 连接当前运行库执行。
- 执行前在同一数据库 session 中设置确认变量：
  - `@confirm_upstream_pairing_role_binding = 'APPLY_UPSTREAM_PAIRING_ROLE_BINDING'`
  - `@confirm_product_sku_source_binding = 'APPLY_PRODUCT_SKU_SOURCE_BINDING'`

## 执行结果

执行输出摘要：

```text
DB=fenxiao
before.pairing_role=1; before.binding_table=0
executed.upstream_statements=50; executed.product_statements=24
after.warehouse_pairing_role=1; after.logistics_pairing_role=1; after.binding_table=1
after.uk_active_source_parts=1; after.uk_active_sku_parts=1; binding_rows=0
```

结果说明：

- `upstream_system_warehouse_pairing.pairing_role` 已存在。
- `upstream_system_logistics_channel_pairing.pairing_role` 已存在。
- `product_sku_source_binding` 已创建。
- `uk_product_sku_source_active_source` 已存在，用于限制一个有效来源 SKU 组全局只能绑定一个商城 SKU。
- `uk_product_sku_source_active_sku` 已存在，用于限制一个商城 SKU 同时只能存在一个有效来源绑定。
- 当前绑定表为空，未自动回填历史商品绑定关系。

## 后续边界

- 本次只执行数据库迁移，不自动创建历史商品的来源绑定。
- 历史商品如需绑定官方来源 SKU，应通过商城商品编辑页或后续管理端换绑入口逐条建立关系。
- 已提交待审核、待上架、已上架等非草稿商品的绑定锁定规则以后端保存逻辑为准，普通编辑不得换绑。

## 本地后端状态

- 已执行 `mvn -pl ruoyi-admin -am -DskipTests package`，重新打包 `RuoYi-Vue/ruoyi-admin/target/ruoyi-admin.jar`。
- 新 jar 时间：2026-06-07 21:07:42。
- 当前 8080 监听进程使用 `E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-admin\target\ruoyi-admin.jar`，HTTP 检查返回 200。
- 启动检查中曾出现一次并发占用 8080 的失败日志，失败进程已退出；当前有效监听进程为后续核验到的 jar 进程。
