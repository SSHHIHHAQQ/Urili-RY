# 币种汇率同步设置简化执行记录

日期：2026-06-04

状态：已完成

## 本次目标

将币种配置里的“同步设置”从通用 API 配置简化为固定 ShowAPI 银行汇率查询接入。页面只开放接入密钥、汇率基准时间、启用状态和备注；后端固定服务商、接口地址、认证方式、超时、重试、同步类型和基准币种。

## 已确认规则

- 官方汇率服务：ShowAPI 银行汇率查询。
- 基准币种：固定人民币 `CNY`。
- 汇率基准时间字段：`rate_anchor_time`。
- 默认汇率基准时间：`09:30:00`。
- 时间口径：北京时间 `Asia/Shanghai`。
- 如果外部返回时间刚好等于 `09:30:00`，允许选中。
- 如果当天没有基准时间之后的官方汇率，本次同步失败且不更新；每天同步，不跨到下一天取值。
- ShowAPI `appKey` 按敏感凭证处理，只加密保存，只脱敏展示，不写入 Markdown、SQL、日志或前端响应明文。

## 代码变更

- 后端新增并使用 `rate_anchor_time`：
  - `RuoYi-Vue/finance/src/main/java/com/ruoyi/finance/domain/FinanceCurrencySyncConfig.java`
  - `RuoYi-Vue/finance/src/main/resources/mapper/finance/FinanceCurrencyMapper.xml`
- 后端固定 ShowAPI 适配：
  - `RuoYi-Vue/finance/src/main/java/com/ruoyi/finance/support/CurrencyRateSyncClient.java`
  - `RuoYi-Vue/finance/src/main/java/com/ruoyi/finance/support/CurrencyRateCandidate.java`
  - `RuoYi-Vue/finance/src/main/java/com/ruoyi/finance/support/CurrencyRateSyncResponse.java`
  - `RuoYi-Vue/finance/src/main/java/com/ruoyi/finance/service/impl/FinanceCurrencyServiceImpl.java`
- 前端同步设置表单简化：
  - `react-ui/src/pages/Finance/Currency/index.tsx`
  - `react-ui/src/pages/Finance/Currency/constants.ts`
  - `react-ui/src/types/finance/currency.d.ts`

## SQL 与数据执行

- 新增迁移脚本：`RuoYi-Vue/sql/20260604_currency_showapi_sync_migration.sql`
- 更新种子脚本：`RuoYi-Vue/sql/currency_configuration_seed.sql`
- 已在当前激活远端 MySQL 执行迁移：
  - `finance_currency_sync_config.rate_anchor_time` 已存在。
  - 同步配置已收敛为 `SHOWAPI_BANK_RATE`。
  - `base_currency_code` 已固定为 `CNY`。
  - `finance_currency` 默认币种已修正为 `CNY`，`USD` 不再是默认币种。
  - `sys_dict_data.currency_code` 默认项已修正为 `CNY`。
- ShowAPI 接入密钥已通过后端加密保存；接口查询不会返回密文或明文。

## 同步验证

- 后端重启：`.\start-backend-local.ps1 -Restart`
- 同步配置保存：通过，返回 `providerCode=SHOWAPI_BANK_RATE`、`baseCurrencyCode=CNY`、`rateAnchorTime=09:30:00`。
- 测试连接：通过，返回可用现汇卖出价候选汇率 29 条，不更新业务汇率。
- 立即同步：通过，返回可用现汇卖出价候选汇率 29 条，更新启用币种 3 条。
- 当前启用币种同步结果：
  - `CNY`：官方汇率和生效汇率为 `1.00000000`。
  - `USD`：基准币种 `CNY`，官方汇率和生效汇率已更新。
  - `EUR`：基准币种 `CNY`，官方汇率和生效汇率已更新。
- 同步日志记录成功状态、返回币种数、更新币种数和耗时；请求 URL 只保留脱敏密钥。

## 页面验证

- 前端地址：`http://127.0.0.1:8001`
- 通过菜单“财务管理 / 币种配置”进入页面成功。
- 币种列表显示 `CNY` 为默认币种，`USD`、`EUR` 为非默认币种，三者基准币种均为 `CNY`。
- 同步设置页签显示：
  - 官方汇率服务：ShowAPI 银行汇率查询
  - 基准币种：人民币 `CNY`
  - 应用名称：`fenxiao`
  - 应用 ID：`2080411`
  - 接入密钥：仅脱敏占位展示
  - 汇率基准时间：`09:30:00`
  - 启用状态：正常
  - 最近同步状态：`SUCCESS`
- 直接打开动态菜单路由 `/finance/currency` 仍会出现 404；从菜单进入正常。这属于当前动态菜单路由加载方式的既有行为，本次未扩大修复。

## 验证命令

| 类型 | 命令或动作 | 结果 |
| --- | --- | --- |
| 后端编译 | `mvn -DskipTests -pl finance -am compile` | 通过 |
| 前端类型检查 | `npm run tsc` | 通过 |
| 后端打包 | `mvn -DskipTests install` | 通过 |
| 前端构建 | `npm run build` | 通过 |
| 后端重启 | `.\start-backend-local.ps1 -Restart` | 通过 |
| 接口验证 | 登录后保存同步配置、测试连接、立即同步 | 通过 |
| 浏览器验证 | Playwright 从菜单进入币种配置和同步设置页签 | 通过 |

## 权限检查

- 同步配置读取/保存：`finance:currency:syncConfig`
- 测试连接/立即同步：`finance:currency:sync`
- 同步日志：`finance:currency:log`
- 后端 Controller `@PreAuthorize` 与菜单按钮权限保持一致。

## 字典与复用检查

- 币种全集仍由若依字典 `currency_code` 维护。
- 业务可用币种仍以 `finance_currency` 启用记录为准。
- 后续业务下拉必须调用 `/finance/admin/currencies/options`，不得直接读取 `currency_code` 字典。
- 敏感凭证继续复用 `SecretCipherSupport`。

## 残留问题

- 自动定时同步尚未接入 Quartz；当前已具备手动同步和测试连接能力。
- ShowAPI `105-30` 文档为实时汇率查询，返回 `day`、`time`、`code`、`hui_out`、`zhesuan` 等字段；当前实现以 `hui_out` 现汇卖出价作为官方汇率来源，并按返回候选记录中的当天时间筛选。如果后续需要历史日线或分钟级历史记录，需要另行确认接口能力并扩展适配器。
