# 币种配置阶段总结

日期：2026-06-04

状态：阶段完成。

## 当前目标

在管理端新增“财务管理 / 币种配置”，用于维护平台可用币种、官方汇率、生效汇率、汇率同步设置和同步日志。后续业务币种下拉必须读取启用的币种配置，不直接读取币种字典。

## 已完成事项

- 已确认实际使用汇率字段名为 `effective_rate`，中文名“生效汇率”。
- 已新增 `finance` Maven 模块，并接入 `ruoyi-admin`。
- 已新增币种配置、汇率历史、同步配置、同步日志的 Domain、Mapper、Service、Controller 和 Mapper XML。
- 已将同步凭证加密能力迁移为 `ruoyi-system` 共享支撑类，供 integration 和 finance 复用。
- 已新增 React 管理端页面 `react-ui/src/pages/Finance/Currency/index.tsx`，包含币种列表、汇率历史、同步设置和同步日志。
- 已新增前端 service、types、constants。
- 已新增并执行 `RuoYi-Vue/sql/currency_configuration_seed.sql`。
- 已更新 `docs/architecture/reuse-ledger.md`，登记币种字典、可用币种 options 和 finance 模块复用规则。

## 数据库执行结果

- 执行 SQL：`RuoYi-Vue/sql/currency_configuration_seed.sql`
- 执行方式：一次性 JDBC 执行。
- 执行结果：9 条语句执行成功。
- 校验结果：
  - `currency_code` 字典类型：1 条。
  - `currency_code` 字典数据：10 条。
  - `finance_currency`：3 条，包含 `USD`、`CNY`、`EUR`。
  - “财务管理 / 币种配置”菜单和按钮权限：8 条。

本文档不记录数据库账号、密码、Redis 密码、token secret、汇率 API Key 或 Token。

## 已验证事项

| 类型 | 命令或动作 | 结果 |
| --- | --- | --- |
| 后端编译 | `mvn -DskipTests -pl ruoyi-admin -am compile` | 通过 |
| 前端类型检查 | `npm run tsc` | 通过 |
| 前端构建 | `npm run build` | 通过 |
| 后端重启 | `.\start-backend-local.ps1 -Restart` | 8080 已监听，根路径 HTTP 200 |
| 接口安全链路 | 未登录访问 `/finance/admin/currencies/options` | 返回 401，符合未认证拦截预期 |
| 浏览器渲染 | `http://127.0.0.1:8001/finance/currency` | 菜单、币种列表、同步设置、同步日志已渲染 |

## 权限检查结果

- 菜单权限：`finance:currency:list`
- 查询权限：`finance:currency:query`
- 新增权限：`finance:currency:add`
- 修改权限：`finance:currency:edit`
- 删除权限：`finance:currency:remove`
- 同步配置权限：`finance:currency:syncConfig`
- 汇率同步权限：`finance:currency:sync`
- 同步日志权限：`finance:currency:log`

后端 Controller 的 `@PreAuthorize` 与 `sys_menu.perms` 已保持一致。

## 字典与选项复用检查结果

- `currency_code` 是币种全集，只用于新增/编辑币种配置时选择币种代码。
- `finance_currency` 是业务可用币种来源，后续业务页面应读取 `/finance/admin/currencies/options`。
- 前端页面没有内联大段币种列表。

## 残留问题

- 外部汇率 API 服务商已确定为 ShowAPI 银行汇率查询，当前已按固定适配器接入。
- 自动同步暂未接入 Quartz 定时任务，当前只保存同步计划字段并支持手动同步。
- 真实汇率调整规则还未最终确认；当前已预留并实现 `NONE`、`MANUAL`、`PERCENT_UP`、`PERCENT_DOWN`、`FIXED_DELTA`。

## 下一步建议

1. 确认自动同步是否接入 Quartz，以及具体执行时间、失败告警和补偿策略。
2. 确认生效汇率调整规则是否开放百分比上浮、百分比下调和固定差值。
3. 确认自动同步是否接入 Quartz，以及失败重试和告警策略。
4. 后续订单、结算、余额等业务接入币种时，只读取启用的 `finance_currency` options，并在业务事实表中保存当时实际使用的 `applied_rate`。
