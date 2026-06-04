# 币种配置目标追踪

日期：2026-06-04

状态：阶段完成；ShowAPI 固定汇率同步已按现汇卖出价校正完成。

## 总目标

在 `E:\Urili-Ruoyi` 管理端新增“财务管理 / 币种配置”能力，用于维护平台可用币种、官方汇率、生效汇率、汇率同步设置和同步日志。

## 已确认输入

- 币种字典作为平台币种全集。
- 币种配置用于限制后续业务可用币种。
- 后续业务币种下拉读取币种配置里的启用币种，不直接读取币种字典。
- 外部同步汇率定义为官方汇率。
- 官方汇率采用 ShowAPI `hui_out` 现汇卖出价，不采用 `zhesuan` 中行折算价。
- 官方汇率不直接给业务使用。
- 实际使用汇率字段采用 `effective_rate`，中文名“生效汇率”。
- 第一版调整规则先支持 `NONE` 和 `MANUAL`。
- 默认基准币种固定为人民币 `CNY`。
- 第一版初始化可用币种按 `USD`、`CNY`、`EUR` 处理。

## 范围内工作

| 序号 | 事项 | 状态 | 当前结果 |
| --- | --- | --- | --- |
| 1 | 方案确认 | 已完成 | 已生成 `docs/plans/2026-06-04-currency-configuration-plan.md`，用户确认开始实现 |
| 2 | Codex 目标追踪 | 已完成 | 已创建本轮 Codex goal，并生成本文件 |
| 3 | 数据库执行记录 | 已完成 | 已生成并更新 `docs/plans/2026-06-04-currency-configuration-db-execution-record.md` |
| 4 | 后端 `finance` 模块 | 已完成 | 已新增 Maven 模块、Domain、Mapper、Service、Controller，并接入 `ruoyi-admin` |
| 5 | 数据表与 SQL | 已完成 | 已新增币种字典、币种配置、汇率历史、同步配置、同步日志和菜单权限 SQL，并已执行 |
| 6 | 汇率同步 | 已完成 | 已改为固定 ShowAPI 银行汇率查询接入；官方汇率取 `hui_out` 现汇卖出价，按北京时间 `rate_anchor_time` 之后第一条官方汇率同步，支持手动同步、测试连接、同步日志和汇率历史追加 |
| 7 | React 页面 | 已完成 | 已新增币种列表、同步设置、同步日志入口 |
| 8 | 复用台账 | 已完成 | 已登记币种字典、可用币种 options、finance 前后端复用点 |
| 9 | 构建与验证 | 已完成 | 后端 compile、前端 tsc/build、SQL 执行校验和浏览器渲染检查已完成 |
| 10 | 阶段总结 | 已完成 | 已生成 `docs/plans/2026-06-04-currency-configuration-stage-summary.md` |

## 范围外工作

- 不做真实财务余额、充值、结算或订单汇率锁定。
- 不把汇率写入卖家、买家主体资料表。
- 不直接从旧项目复制代码。
- 不把官方汇率作为业务直接使用的值。
- 不保存明文 API Key、Token 或 Secret。
- 不新增其他外部汇率服务商，也不开放自定义 API URL。

## 当前默认设计

| 项 | 当前默认 |
| --- | --- |
| 字典类型 | `currency_code` |
| 可用币种表 | `finance_currency` |
| 汇率历史表 | `finance_currency_rate_history` |
| 同步配置表 | `finance_currency_sync_config` |
| 同步日志表 | `finance_currency_sync_log` |
| 官方汇率字段 | `official_rate` |
| 生效汇率字段 | `effective_rate` |
| 默认基准币种 | `CNY` |
| 默认币种 | `CNY` |
| 汇率精度 | 8 |
| 金额精度 | 2 |
| 舍入方式 | `HALF_UP` |

## 验证记录

| 类型 | 命令或动作 | 状态 | 结果 |
| --- | --- | --- | --- |
| 后端构建 | `mvn -DskipTests -pl ruoyi-admin -am compile` | 通过 | 全量 compile 通过；`finance`、`seller`、`buyer`、`integration` 和 `ruoyi-admin` 均成功 |
| 后端打包 | `mvn -DskipTests install` | 通过 | 已产出包含 `finance-3.9.2.jar` 的 `ruoyi-admin.jar`；一次执行曾因运行 jar 被占用导致 repackage rename 失败，停止 8080 后重跑通过 |
| 后端重启 | 停止 8080 后执行 `.\start-backend-local.ps1` | 通过 | 8080 已启动；根路径 HTTP 200；未登录访问币种接口返回 401，说明安全链路生效 |
| ShowAPI 同步接口 | 登录后调用测试连接和立即同步 | 通过 | 测试连接返回可用现汇卖出价候选 29 条；立即同步更新启用币种 3 条 |
| 前端构建 | `npm run build` | 通过 | Webpack 编译通过；仅有 Browserslist 数据过期提示 |
| 前端类型检查 | `npm run tsc` | 通过 | TypeScript 检查通过 |
| SQL 执行 | `RuoYi-Vue/sql/currency_configuration_seed.sql` | 通过 | JDBC 执行 9 条语句；`currency_code` 字典 1 条类型、10 条数据；`finance_currency` 3 条；币种菜单/按钮 8 条 |
| 权限检查 | 后端 `@PreAuthorize` 与菜单权限一致性 | 通过 | 后端权限点与 `sys_menu.perms` 一致 |
| 凭证脱敏 | 保存和查询同步配置 | 通过静态检查 | 后端写入 `credential_ciphertext`，返回 `credential_masked`，`credential` 仅写入不回显 |
| 浏览器渲染 | `http://127.0.0.1:8001/finance/currency` | 通过 | 菜单展示在“财务管理 / 币种配置”；列表展示 USD/CNY/EUR；同步设置和同步日志可渲染 |

## 残留问题

- 外部汇率 API 服务商已确定为 ShowAPI 银行汇率查询，配置页只开放接入密钥、汇率基准时间和启用状态。
- 自动定时同步尚未接入 Quartz 定时任务；需要等具体运行策略确认后再打开。
- 第一版已支持 `NONE`、`MANUAL`、`PERCENT_UP`、`PERCENT_DOWN`、`FIXED_DELTA` 调整模式；后续还需要按业务规则确认哪些模式开放给运营使用。
