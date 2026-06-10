# 客户渠道管理 SQL 执行记录

## 执行时间

- 2026-06-10 19:06:36 +08:00

## 目标环境

- 连接来源：`E:\Urili-Ruoyi\.env.local`
- 应用激活配置：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 中 `spring.profiles.active=druid`
- JDBC 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml` 的 `RUOYI_DB_*` 环境变量
- 目标数据库：远端 MySQL，库名 `fenxiao`
- 本次未使用本地 Docker MySQL。
- 本记录不写入数据库账号、密码和完整连接串。

## 执行脚本

- `RuoYi-Vue/sql/20260610_customer_logistics_channel_management.sql`
- 确认变量：`@confirm_customer_logistics_channel_management = 'APPLY_CUSTOMER_LOGISTICS_CHANNEL_MANAGEMENT'`

## 执行方式

本机未安装 `mysql` CLI。本次使用本机 Maven 缓存中的 `mysql-connector-j`，通过 JShell/JDBC 读取 `.env.local` 后连接目标 MySQL，在同一连接内先设置确认变量，再逐条执行 SQL 脚本。

执行命令类型：远端 MySQL DDL/DML 增量迁移。

## 预检结果

- 目标数据库确认为 `fenxiao`。
- 执行前客户渠道三张目标表均不存在：
  - `logistics_customer_channel`
  - `logistics_customer_channel_system_mapping`
  - `logistics_customer_channel_buyer_scope`
- 目标菜单 `sys_menu.menu_id = 2042` 存在，原权限为 `channel:customer:list`，符合脚本允许的旧占位签名。
- 历史按钮 `2260/2261` 存在，原权限为 `channel:customer:query`、`channel:customer:add`，符合脚本允许升级的历史签名。
- 客户渠道相关字典类型执行前数量为 `0`。

## 执行结果

- SQL 执行完成。
- JDBC 执行语句数：31。
- 目标库：`fenxiao`。

执行后校验：

| 校验项 | 结果 |
| --- | --- |
| 客户渠道目标表数量 | 3 |
| 菜单 2042 | `2042:2030:channel-customer:Channel/Customer/index:logistics:customerChannel:list:C` |
| 客户渠道按钮权限菜单 | 7 个 |
| 客户渠道相关字典类型 | 7 个 |
| 客户渠道相关字典明细 | `2/2/2/2/2/3/2` |

已创建或确认存在的表：

- `logistics_customer_channel`
- `logistics_customer_channel_system_mapping`
- `logistics_customer_channel_buyer_scope`

已创建或确认存在的权限：

- `logistics:customerChannel:list`
- `logistics:customerChannel:query`
- `logistics:customerChannel:add`
- `logistics:customerChannel:edit`
- `logistics:customerChannel:status`
- `logistics:customerChannel:binding`
- `logistics:customerChannel:buyer`

## 影响范围

- 新增客户渠道管理相关表结构。
- 更新管理端客户渠道菜单 `2042` 的最终权限标识。
- 新增或升级客户渠道管理按钮权限。
- 新增客户渠道类型、面单上传、平台面单获取、客户上传面单支持、买家范围模式、渠道状态、绑定状态字典。

## 后端部署

执行 SQL 后已重新部署本地后端：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-admin -am -DskipTests package

cd E:\Urili-Ruoyi
.\start-backend-local.ps1 -Restart
```

部署结果：

- `mvn -pl ruoyi-admin -am -DskipTests package` 通过，`BUILD SUCCESS`。
- 后端启动后 8080 已监听，监听进程 PID 为 `34312`。
- `http://127.0.0.1:8080` 返回 HTTP 200。

## 接口验证

使用 `POST /login` 获取管理端 token 后验证：

| 接口 | 结果 |
| --- | --- |
| `GET /logistics/admin/customer-channels/list?pageNum=1&pageSize=10` | `code=200`，当前 `total=0` |
| `GET /logistics/admin/customer-channels?pageNum=1&pageSize=10` | `code=200`，当前 `total=0` |
| `GET /getRouters` | 已包含 `Channel/Customer/index` |
| `GET /getRouters` | 已包含 `logistics:customerChannel:list` |

说明：当前客户渠道表已建好但没有初始化业务渠道数据，所以列表为空是预期状态。
