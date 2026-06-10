# 系统物流渠道管理 SQL 执行记录

## 执行时间

- 2026-06-10

## 目标环境

- 连接来源：`E:\Urili-Ruoyi\.env.local`
- 应用激活配置：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 中 `spring.profiles.active=druid`
- JDBC 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml` 的 `RUOYI_DB_*` 环境变量
- 目标数据库：远端 MySQL，库名 `fenxiao`
- 本次未使用本地 Docker MySQL。
- 本记录不写入数据库账号、密码和完整连接串。

## 执行脚本

- `RuoYi-Vue/sql/20260610_system_logistics_channel_management.sql`
- 确认变量：`@confirm_system_logistics_channel_management = 'APPLY_SYSTEM_LOGISTICS_CHANNEL_MANAGEMENT'`

## 执行方式

本机未安装 `mysql` CLI。本次使用本机 Maven 缓存中的 `mysql-connector-j`，通过 JShell/JDBC 读取 `.env.local` 后连接目标 MySQL，在同一连接内先设置确认变量，再逐条执行 SQL 脚本。

执行命令类型：远端 MySQL DDL/DML 增量迁移。

## 预检结果

- 目标菜单 `sys_menu.menu_id = 2041` 存在，原权限为 `channel:system:list`，符合脚本允许的旧占位签名。
- 按钮菜单位 `2520-2526` 未被占用。
- 目标库已存在 `logistics_system_channel`，其余系统渠道扩展表由本次脚本补齐。

## 执行结果

- SQL 执行完成。
- JDBC 执行语句数：36。
- 目标库：`fenxiao`。

执行后校验：

| 校验项 | 结果 |
| --- | --- |
| 系统渠道目标表数量 | 5 |
| `logistics_system_channel.buyer_scope_mode` | 已存在 |
| 菜单 2041 | `2041:2030:channel-system:Channel/System/index:logistics:systemChannel:list:C` |
| 按钮权限菜单 | 7 个 |
| 相关字典类型 | 6 个 |

## 影响范围

- 新增或补齐系统物流渠道管理相关表结构。
- 更新管理端物流渠道菜单 `2041` 的最终权限标识。
- 新增系统物流渠道管理按钮权限。
- 新增系统物流渠道状态、绑定状态、买家范围模式、发货地址模式、下单规则校验模式、平台类型字典。

## 追加执行记录

### 2026-06-10 签名服务字段补齐

原因：系统渠道签名服务应属于基础信息，不属于下单规则；本次将签名服务上移到 `logistics_system_channel.signature_services`。

执行方式：

- 继续使用受保护脚本 `RuoYi-Vue/sql/20260610_system_logistics_channel_management.sql`。
- 在同一连接内设置 `@confirm_system_logistics_channel_management = 'APPLY_SYSTEM_LOGISTICS_CHANNEL_MANAGEMENT'`。
- 脚本通过 `add_system_logistics_column_if_missing` 幂等补齐字段。

执行前后校验：

| 校验项 | 结果 |
| --- | --- |
| 执行前 `signature_services` 字段 | 0 |
| JDBC 执行语句数 | 37 |
| 执行后 `signature_services` 字段 | 1 |
| 菜单 2041 | `2041:2030:channel-system:Channel/System/index:logistics:systemChannel:list:C` |

## 后续验证结果

### 后端打包

首次执行 `mvn -pl ruoyi-admin -am -DskipTests package` 时，正在运行的 8080 后端占用 `ruoyi-admin.jar`，Spring Boot repackage 无法重命名 jar。

处理方式：

- 确认 8080 监听进程为本机 Java 后端。
- 停止该进程释放 jar 文件。
- 重新执行打包。

结果：重新打包通过，`BUILD SUCCESS`。

### 后端启动

```powershell
cd E:\Urili-Ruoyi
.\start-backend-local.ps1
```

结果：

- 8080 已监听。
- `http://127.0.0.1:8080` 返回 HTTP 200。

### 接口验证

使用 `POST /login` 获取管理端 token 后验证：

| 接口 | 结果 |
| --- | --- |
| `GET /logistics/admin/system-channels/list?pageNum=1&pageSize=10` | `code=200`，当前 `total=0` |
| `GET /getRouters` | 已包含 `Channel/System/index` |
| `GET /getRouters` | 已包含 `logistics:systemChannel:list` |

追加签名服务字段后已重新打包并启动本机后端，列表接口和 `/getRouters` 仍验证通过。

说明：当前系统渠道表已建好但没有初始化业务渠道数据，所以列表为空是预期状态。
