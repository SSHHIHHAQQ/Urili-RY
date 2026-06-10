# 物流商渠道命名调整 SQL 执行记录

## 执行目标

- 需求：把页面和后台展示文案中的“候选渠道”统一改名为“物流商渠道”。
- SQL 文件：`RuoYi-Vue/sql/20260610_logistics_carrier_channel_rename.sql`
- 确认变量：`@confirm_logistics_carrier_channel_rename = 'APPLY_LOGISTICS_CARRIER_CHANNEL_RENAME'`

## 用户确认与执行边界

- 用户确认来源：当前三端快速推进目标已明确远程数据库 DDL/DML 已确认可以执行；本次只执行本记录列明的物流商渠道命名调整。
- 本次 SQL 只修改中文展示文案、表注释和列注释；不修改表名、字段名、权限标识、接口路径、业务数据或外部凭据。
- 本次确认变量只适用于 `20260610_logistics_carrier_channel_rename.sql` 当次执行，不得作为后续无确认重放依据。

## 数据源确认

- 后端配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`
- 激活 profile：`druid`
- 主库配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`
- 实际连接变量来源：本机 `.env.local`
- 使用变量名：`RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`
- 敏感信息处理：执行记录不保存数据库密码、外部系统密钥、token secret 或加密密钥。

## 影响范围

- 更新 `logistics_channel_status` 字典类型的中文名称和备注。
- 更新物流商渠道相关按钮菜单备注。
- 更新物流商渠道相关表注释和列注释。
- 不修改表名、字段名、权限标识、接口路径、业务数据或外部凭据。

## 执行结果

- 状态：已执行成功。
- 执行时间：2026-06-10 02:58 +08:00
- 执行方式：通过项目已有 MySQL JDBC 驱动连接当前后端主库执行，执行前设置确认变量。
- 目标环境：当前后端 `.env.local` 指向的远端 MySQL 主库。
- 执行语句数：14。
- 后续脚本加固：仓库脚本已补充 `set names utf8mb4;`，用于后续回放时固定中文字符集；本记录未因此重放远端 SQL。

## 验证结果

- `sys_dict_type` 中 `logistics_channel_status` 的中文名称已更新为 `物流商渠道状态`。
- `sys_dict_type` 中 `logistics_channel_status` 已无 `候选渠道` 残留文案。
- `sys_menu` 中物流商渠道相关按钮菜单名称和备注已无 `候选渠道` 残留文案。
- `logistics_carrier_channel_candidate` 表注释已更新为 `物流商渠道表`。
- 本次记录未保存数据库密码、外部系统密钥、token secret 或加密密钥。

## 回滚方式

- 默认不建议回滚：当前代码和页面文案已按“物流商渠道”口径推进。
- 事务边界说明：本脚本包含 MySQL DDL 注释修改，DDL 会隐式提交，不能通过单个外层事务获得完整原子回滚；风险控制依赖精确目标、完成态断言、执行后校验和单独确认的回滚方案。
- 如必须回滚，只允许针对本次精确目标恢复中文展示文案和注释：
  - `sys_dict_type.dict_type = 'logistics_channel_status'` 的 `dict_name`、`remark`
  - `sys_menu.menu_id in (2514, 2515)` 的 `remark`
  - `logistics_carrier_connection.last_channel_sync_time` 列注释
  - `logistics_carrier_channel_candidate` 表注释和 `status` 列注释
  - `logistics_carrier_channel_mapping` 表注释
- 回滚前必须重新预览上述目标当前值，生成新的确认记录；不得按模糊文案全库替换。
