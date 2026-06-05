# 仓库管理业务表补库执行记录
## 执行时间

- 2026-06-06

## 背景

新增/保存仓库时报错：

```text
Table 'fenxiao.warehouse' doesn't exist
```

后端当前已加载仓库模块，执行仓库编码唯一性校验时会查询 `warehouse` 表，但运行库中尚未执行仓库管理业务表 seed。

## 目标环境

- 项目：`E:\Urili-Ruoyi`
- 后端激活配置来源：
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`
- 数据源类型：远端 MySQL
- 目标库：`fenxiao`
- Redis：本次未读写 Redis

## 执行内容

- SQL 文件：`RuoYi-Vue/sql/warehouse_management_seed.sql`
- 执行方式：本机 JShell + Maven 缓存中的 `mysql-connector-j` JDBC 驱动，读取 SQL 文件后逐条执行。
- 执行性质：
  - DDL：创建 `warehouse`、`official_warehouse`、`third_party_warehouse`
  - DML：初始化 `warehouse_kind` 字典数据
  - DML：初始化仓库管理菜单和按钮权限
- 幂等性：
  - 表结构使用 `create table if not exists`
  - 菜单使用 `on duplicate key update`
  - 字典使用 `not exists` 防重复写入

## 执行前检查

- 当前数据库：`fenxiao`
- 已存在仓库业务表：空

## 执行结果

- 执行 SQL 语句数：7
- `warehouse`：已创建，当前记录数 `0`
- `official_warehouse`：已创建，当前记录数 `0`
- `third_party_warehouse`：已创建，当前记录数 `0`
- `warehouse_kind` 字典数据：`2`
- 仓库菜单与按钮权限：`11`
- 仓库管理父菜单 `2020`：存在

## 针对报错的验证

已执行与报错同类的查询：

```sql
select count(1) from warehouse where warehouse_code = ?
```

结果正常返回 `0`，不再出现 `Table 'fenxiao.warehouse' doesn't exist`。

## 未验证项

- 本次未新增真实仓库业务数据。
- 本次未重启后端；DDL/DML 已在当前运行库生效，表查询不依赖后端重启。
