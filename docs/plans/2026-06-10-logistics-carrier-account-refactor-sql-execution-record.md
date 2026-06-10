# 物流商账号模型改造 SQL 执行记录

## 执行目标

- 需求：把物流商管理从“接入编号/单系统接入”改成“物流商系统 + 多个物流商账号”。
- SQL 文件：`RuoYi-Vue/sql/20260610_logistics_carrier_account_refactor.sql`
- 确认变量：`@confirm_logistics_carrier_account_refactor = 'APPLY_LOGISTICS_CARRIER_ACCOUNT_REFACTOR'`

## 用户确认与执行边界

- 用户确认来源：当前三端快速推进目标已明确远程数据库 DDL/DML 已确认可以执行；本次只执行本记录列明的物流商账号模型改造。
- 本次 SQL 只新增 `carrier_account_id`、回填历史关联、补充索引并保留旧 `connection_code` 兼容列；不删除旧列、不清空业务数据、不写入外部凭据。
- 本次确认变量只适用于 `20260610_logistics_carrier_account_refactor.sql` 当次执行，不得作为后续无确认重放依据。

## 数据源确认

- 后端配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`
- 激活 profile：`druid`
- 主库配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`
- 实际连接变量来源：本机 `.env.local`
- 使用变量名：`RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`
- 敏感信息处理：执行记录不保存数据库密码、外部系统密钥、token secret 或加密密钥。

## 影响范围

- 给物流商账号主表新增内部自增账号 ID：`carrier_account_id`。
- 给 AGG56 凭据、物流商渠道、渠道映射、面单订单、外部请求日志补充 `carrier_account_id`。
- 按旧内部 `connection_code` 回填新账号 ID。
- 补充按账号 ID 查询所需索引。
- 保留旧 `connection_code` 作为内部兼容列，不再作为管理端用户可见字段。

## 执行结果

- 状态：已执行成功。
- 执行时间：2026-06-10 02:31:42 +08:00
- 执行方式：通过项目已有 MySQL JDBC 驱动连接当前后端主库执行，执行前设置确认变量。
- 目标环境：当前后端 `.env.local` 指向的远端 MySQL 主库，库名已脱敏。
- 执行语句数：37。

## 验证结果

- `carrier_account_id` 字段：`6/6` 张目标表已存在。
- 主表空值：`logistics_carrier_connection.carrier_account_id` 空值数为 `0`。
- 当前物流商账号数量：`0`，本轮没有历史账号数据需要回填。
- 账号 ID 查询索引：`6/6` 个目标索引名已存在。
- 本次记录未保存数据库密码、外部系统密钥、token secret 或加密密钥。

## 页面验证

- 验证时间：2026-06-10 02:45 +08:00
- 验证入口：管理端远程菜单 `/overseas-warehouse-service/logistics-carrier`。
- `/getRouters` 下发结果：物流商管理菜单挂在 `/overseas-warehouse-service` 下，页面组件为 `Logistics/Carrier/index`，权限为 `logistics:carrier:list`。
- 页面结果：物流商管理列表可正常打开，表格展示 `物流商名称`、`物流商系统`、`API地址`、`状态`、`凭据` 等字段。
- 新增弹窗结果：展示 `物流商名称`、`物流商系统`、`APP Token`、`APP Key`、`API地址`、`备注`。
- 页面未展示旧的 `接入编号` 输入项或列表列。

## 回滚方式

- 默认不建议回滚：当前代码已经按 `carrier_account_id` 作为运行期物流商账号主键推进。
- 事务边界说明：本脚本包含 MySQL DDL，DDL 会隐式提交，不能通过单个外层事务获得完整原子回滚；风险控制依赖执行前确认 token、执行中完成态断言、执行后校验和单独确认的回滚方案。
- 如必须回滚，只允许先生成新的回滚方案并人工确认，至少需要：
  - 预览并确认 `logistics_carrier_connection` 与各子表 `connection_code` 仍可完整关联。
  - 停止依赖 `carrier_account_id` 的后端写入和页面操作。
  - 精确移除本次新增索引和 `carrier_account_id` 列。
- 不允许直接按旧脚本反向模糊删除字段或索引；回滚必须重新生成执行记录并保留验证结果。
