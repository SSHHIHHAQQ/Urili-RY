# 币种配置数据库执行记录

日期：2026-06-04

状态：已执行。

## 数据源确认

- 后端激活配置：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`
- 数据源配置：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`
- 执行 SQL、查询数据或启动后端前，必须按当前激活配置确认 MySQL 与 Redis 目标。
- 如使用本机启动脚本，连接来源应为 `.env.local` 中的 `RUOYI_*` 环境变量。
- 本地 Docker MySQL/Redis 只作为隔离验证用途，本轮默认不读取、不写入。
- 敏感信息处理：本文档不记录数据库地址全文、账号、密码、Redis 密码、汇率 API Key 或 Token。

## 用户授权

- 用户已确认开始实现币种配置。
- 该确认视为允许按 `docs/plans/2026-06-04-currency-configuration-plan.md` 的已确认方案编写代码和 SQL。
- 已按当前激活数据源完成目标确认，并在本记录中补充执行时间和结果。

## 计划执行类型

| 类型 | 是否影响远端数据 | 说明 |
| --- | --- | --- |
| DDL | 是 | 新增 `finance_currency`、`finance_currency_rate_history`、`finance_currency_sync_config`、`finance_currency_sync_log` |
| DML | 是 | 新增/更新 `currency_code` 字典、第一批可用币种、财务管理下的币种配置菜单和按钮权限 |
| 查询 | 否 | 验证字典、表结构、菜单权限、数据量和同步日志 |
| 外部接口调用 | 可能 | 手动同步时会请求配置的汇率 API，并追加同步日志 |

## 待执行 SQL

- `RuoYi-Vue/sql/currency_configuration_seed.sql`

## 执行明细

| 时间 | 动作 | 命令类型 | 目标 | 结果 |
| --- | --- | --- | --- | --- |
| 2026-06-04 13:30:41 +08:00 | 执行 `RuoYi-Vue/sql/currency_configuration_seed.sql` | DDL/DML | 当前激活远端 MySQL | 通过 JDBC 执行 9 条语句；未在命令输出或本文档记录密码、Token 或汇率 API 凭证 |
| 2026-06-04 13:30:41 +08:00 | 校验初始化结果 | 查询 | 当前激活远端 MySQL | `currency_code` 字典类型 1 条，字典数据 10 条，`finance_currency` 3 条，币种菜单/按钮权限 8 条 |
| 2026-06-04 13:31:28 +08:00 | 重启后端服务 | 服务重启 | `http://127.0.0.1:8080` | 8080 已由新 Java 进程监听；根路径 HTTP 200；未登录访问币种接口返回 401 |

## 验证项

| 验证项 | 预期 |
| --- | --- |
| `sys_dict_type` | 已验证存在 `currency_code` |
| `sys_dict_data` | 已验证存在 10 条币种 code，包含 `USD`、`CNY`、`EUR` |
| `finance_currency` | 已验证存在默认可用币种 `USD`、`CNY`、`EUR` |
| `finance_currency_rate_history` | 已随 SQL 创建，历史只追加 |
| `finance_currency_sync_config` | 已随 SQL 创建，凭证字段为密文/脱敏展示 |
| `finance_currency_sync_log` | 已随 SQL 创建，同步请求日志只追加 |
| `sys_menu` | 已验证存在“财务管理 / 币种配置”和 7 个按钮权限 |
| 权限 | 已静态检查后端 `@PreAuthorize` 与菜单 `perms` 一致 |

## 未验证项

- 未配置真实外部汇率 API，因此未执行真实“测试连接”和“立即同步”。
- 未接入 Quartz 自动调度；当前只保存同步计划字段，手动同步接口可用。

## 回滚思路

- 菜单和权限：禁用或删除本次新增 `sys_menu` 记录。
- 字典：禁用 `currency_code` 字典数据；如已有业务引用，不建议物理删除。
- 业务表：如尚无业务引用，可删除本次新增 finance currency 表；如已有同步历史，优先保留历史并停用币种或同步配置。
- 外部凭证：优先清空加密凭证字段并停用同步配置，不在日志或报告中输出明文。
