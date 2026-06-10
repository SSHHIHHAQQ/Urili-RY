# 物流商管理菜单 SQL 执行记录

## 执行目标

- 需求：启用管理端“物流商管理”菜单，并创建物流商管理第一版所需的通用数据表、AGG56 扩展表、字典和按钮权限。
- SQL 文件：`RuoYi-Vue/sql/20260610_logistics_carrier_management.sql`
- 确认变量：`@confirm_logistics_carrier_management = 'APPLY_LOGISTICS_CARRIER_MANAGEMENT'`

## 用户确认与执行边界

- 用户确认来源：当前三端快速推进目标已明确远程数据库 DDL/DML 已确认可以执行；本次只执行本记录列明的物流商管理建表、字典和菜单权限初始化。
- 本次 SQL 新增物流商管理相关表、字典和管理端菜单按钮权限；不写入外部系统账号密码明文，不清理既有业务数据。
- 本次确认变量只适用于 `20260610_logistics_carrier_management.sql` 当次执行，不得作为后续无确认重放依据。

## 数据源确认

- 后端配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`
- 激活 profile：`druid`
- 主库配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`
- 实际连接变量来源：本机 `.env.local`
- 使用变量名：`RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`
- 敏感信息处理：执行记录不保存数据库密码、外部系统密钥、token secret 或加密密钥。

## 影响范围

- 新增物流商管理通用表。
- 新增 AGG56 单系统扩展表。
- 新增物流商渠道、系统标准渠道、渠道映射、面单订单、包裹明细、外部请求日志表。
- 写入物流相关字典类型和初始字典项。
- 更新管理端菜单 `2054` 的 React 组件路径为 `Logistics/Carrier/index`。
- 新增管理端按钮权限菜单 `2510-2517`。

## 执行结果

- 状态：已执行成功。
- 执行时间：2026-06-10 02:00:32 +08:00
- 执行方式：通过项目已有 MySQL JDBC 驱动连接当前后端主库执行，执行前设置确认变量。
- 目标环境：当前后端 `.env.local` 指向的远端 MySQL 主库，库名已脱敏。
- 执行语句数：35。

## 验证结果

- 物流商管理表：`8/8` 已存在。
- 物流相关字典类型：`9/9` 已存在。
- 物流相关字典数据：`43` 条。
- 页面菜单：`menu_id = 2054` 已更新为 `Logistics/Carrier/index`，路由名 `LogisticsCarrier`，权限 `logistics:carrier:list`。
- 按钮权限：`2510-2517` 共 `8/8` 已挂到 `2054` 下。
- 后端接口入口：未登录访问 `/logistics/admin/carriers/list` 返回 `401`，不是 `404`，说明当前 `8080` 进程已识别物流商管理 Controller。
- 本次记录未保存数据库密码、外部系统密钥、token secret 或加密密钥。

## 回滚方式

- 默认不建议回滚：当前管理端菜单、页面和后端接口已经依赖这些物流商管理表和权限点。
- 事务边界说明：本脚本包含 MySQL DDL，DDL 会隐式提交，不能通过单个外层事务获得完整原子回滚；风险控制依赖执行前确认 token、菜单槽位/签名 guard、执行后完成态断言和单独确认的回滚方案。
- 如必须回滚，只允许先生成新的回滚方案并人工确认，至少需要：
  - 预览并确认物流商相关表无需要保留的业务数据。
  - 下线或隐藏管理端菜单 `2054` 及按钮权限 `2510-2517`。
  - 精确删除本次新增表、字典类型、字典数据和按钮权限。
- 不允许直接回放破坏性清理脚本；回滚必须重新生成执行记录并保留验证结果。
