# 商品配置修改记录设计

## 背景

商品分类配置和商品属性配置已经具备新增、编辑、删除、导入、启用/停用等操作。若依 `sys_oper_log` 可以记录接口调用，但不稳定承载业务对象 ID、字段修改前后值和导入逐行结果。因此本次新增商品配置业务级修改记录，用于在具体分类、属性、属性选项和类目属性规则上查看字段级变更。

## 业务目的

- 创建数据时同步写入 `update_by` 和 `update_time`，让“更新时间”表达最后一次变更时间，创建也算一次变更。
- 对商品配置对象记录可查询的业务修改记录。
- 支持前端在行内点击“修改记录”查看某个对象的变更历史。

## 表设计

新增表：`product_config_change_log`

| 字段 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `log_id` | `bigint` | 是 | 自增 | 修改记录 ID |
| `biz_type` | `varchar(32)` | 是 | - | 业务对象类型：`CATEGORY`、`ATTRIBUTE`、`ATTRIBUTE_OPTION`、`CATEGORY_ATTRIBUTE_RULE` |
| `biz_id` | `bigint` | 是 | - | 业务对象主键 |
| `biz_code` | `varchar(128)` | 否 | 空串 | 业务编码，便于辅助检索 |
| `biz_name` | `varchar(256)` | 否 | 空串 | 业务名称，便于展示 |
| `action_type` | `varchar(32)` | 是 | - | 操作类型：`CREATE`、`UPDATE`、`ENABLE`、`DISABLE`、`DELETE` |
| `action_source` | `varchar(32)` | 是 | `PAGE` | 操作来源：`PAGE`、`IMPORT` |
| `operator_name` | `varchar(64)` | 否 | 空串 | 操作人账号 |
| `change_summary` | `varchar(1000)` | 否 | 空串 | 变更摘要 |
| `before_json` | `longtext` | 否 | `null` | 修改前字段快照 JSON |
| `after_json` | `longtext` | 否 | `null` | 修改后字段快照 JSON |
| `diff_json` | `longtext` | 否 | `null` | 字段差异 JSON |
| `change_time` | `datetime` | 是 | 当前时间 | 变更时间 |
| `remark` | `varchar(500)` | 否 | 空串 | 备注 |

索引：

- 主键：`pk_product_config_change_log(log_id)`
- 对象查询：`idx_product_config_change_log_biz(biz_type, biz_id, change_time)`
- 时间查询：`idx_product_config_change_log_time(change_time)`
- 操作人查询：`idx_product_config_change_log_operator(operator_name)`

## 记录范围

- 商品分类：新增、编辑、删除。
- 商品属性：新增、编辑、启用、停用、删除。
- 商品属性选项：新增、编辑、删除。
- 类目属性规则：新增、编辑、删除。
- 导入新增/导入更新：通过服务层复用同一套记录逻辑，来源标记为 `IMPORT`。

## 不承载内容

- 不替代若依 `sys_oper_log`，系统级接口审计仍由若依操作日志承担。
- 不记录正式商城商品、SKU、库存、订单、履约、财务流水。
- 不记录导入文件原始内容，只记录导入落库后的字段变化。

## 权限与接口

新增查询接口：

- `GET /product/admin/change-logs/list`

查询参数：

- `bizType`
- `bizId`
- `actionType`
- `actionSource`
- `operatorName`
- `pageNum`
- `pageSize`

权限：

- 复用管理端商品配置查看权限：`product:category:list`、`product:attribute:list`、`product:categoryAttribute:list` 任一具备即可查询。

## 前端交互

- 商品分类配置行内增加“修改记录”，放入“更多”菜单。
- 商品属性库行内增加“修改记录”；低频操作进入“更多”菜单，避免一行平铺超过两个文字按钮。
- 属性选项弹窗行内增加“修改记录”。
- 类目属性模板的本类目规则行内增加“修改记录”。
- 弹窗展示：变更时间、操作人、操作类型、来源、变更摘要。
- 展开或详情展示：字段名、修改前、修改后。

## 迁移与回滚

迁移：

- 新建 `product_config_change_log`。
- 修改四张商品配置表的新增 SQL，让新增时同时写入 `update_by`、`update_time`。
- 可选补齐历史数据：把空 `update_time` 回填为 `create_time`，把空 `update_by` 回填为 `create_by`。

回滚：

- 删除前端修改记录入口。
- 删除后端查询接口和记录写入调用。
- 保留或删除 `product_config_change_log` 取决于是否需要保留审计历史。
- `update_time` 创建即写入属于字段语义修正，原则上不建议回滚。
