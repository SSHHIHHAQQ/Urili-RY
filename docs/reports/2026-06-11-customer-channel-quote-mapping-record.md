# 客户渠道报价主仓渠道映射实施记录

时间：2026-06-11

## 目标

在客户渠道管理编辑弹窗中新增“主仓渠道映射”页签，复用系统渠道管理的主仓渠道映射交互，但客户渠道只允许映射报价仓的主仓渠道。

## 实现范围

- 新增客户渠道报价主仓渠道映射表：`logistics_customer_channel_quote_mapping`。
- 新增客户渠道接口：
  - `GET /logistics/admin/customer-channels/{customerChannelCode}/quote-channel-mappings/list`
  - `POST /logistics/admin/customer-channels/{customerChannelCode}/quote-channel-mappings`
  - `DELETE /logistics/admin/customer-channels/{customerChannelCode}/quote-channel-mappings/{mappingId}`
- 后端 Service 校验：
  - 只能选择 `settlementType = self-operated-receivable` 的报价仓上游系统。
  - 主仓渠道必须来自该报价仓的 `ACTIVE` 物流渠道同步清单。
  - 同一个客户渠道只保留一条 `QUOTE` 用途映射，重新配置时事务内覆盖旧映射。
- 前端客户渠道编辑弹窗新增“主仓渠道映射”页签，配置弹窗只展示报价仓连接，并从选中报价仓加载主仓渠道。

## SQL 状态

已新增增量脚本：

- `RuoYi-Vue/sql/20260611_customer_channel_quote_mapping.sql`

脚本带确认 token：

- `@confirm_customer_channel_quote_mapping`
- `APPLY_CUSTOMER_CHANNEL_QUOTE_MAPPING`

当前激活配置摘要：

- profile：`druid`
- JDBC：`jdbc:mysql://gz-cynosdbmysql-grp-lucf5kyf.sql.tencentcdb.com:28634/fenxiao`

2026-06-11 02:56 已按用户确认执行该 DDL，执行后校验结果：

- `logistics_customer_channel_quote_mapping` 表存在。
- `uk_logistics_customer_quote_channel` 索引存在，字段为 `customer_channel_code,pairing_role`。
- `idx_logistics_customer_quote_upstream` 索引存在，字段为 `connection_code,upstream_channel_code,pairing_role`。

2026-06-11 02:57 已重新打包 `ruoyi-admin.jar` 并重启本机后端，进程监听 `8080`，根路径 HTTP 200。

## 验证

- `npx jest --config jest.config.js tests/logistics-customer-channel-contract.test.ts --runInBand`：通过
- `mvn -pl logistics -am "-Dtest=LogisticsAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过
- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过
- `mvn -pl ruoyi-admin -am "-DskipTests" compile`：通过
- `mvn -pl ruoyi-admin -am "-DskipTests" package`：通过
- `npm run tsc`：通过
- `GET http://127.0.0.1:8080/logistics/admin/customer-channels/TEST/quote-channel-mappings/list`：返回业务鉴权 `code=401`，确认新路径由后端安全链路接管。
