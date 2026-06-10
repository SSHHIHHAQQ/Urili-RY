# 物流渠道管理运行库结构修复记录

## 背景

系统渠道编辑保存时报错：

```text
Unknown column 'fulfillment_mode' in 'field list'
```

原因是后端 `LogisticsSystemChannelMapper` 已按新版系统渠道模型读写 `fulfillment_mode`，但当前运行库 `fenxiao.logistics_system_channel` 仍是旧版结构。

## 目标环境

- 连接来源：`E:\Urili-Ruoyi\.env.local`
- 后端配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`、`application-druid.yml`
- 目标库：当前 JDBC URL 指向的运行库，已只读确认 `database() = fenxiao`

## 执行范围

本次只做幂等结构补齐和字典补齐：

- 若 `logistics_system_channel.fulfillment_mode` 缺失，则新增该列，默认 `CARRIER_LABELING`
- 若 `logistics_system_channel.signature_services` 缺失，则补齐该列
- 若 `idx_logistics_system_channel_fulfillment` 缺失，则新增 `(fulfillment_mode, status)` 索引
- 补齐 `logistics_system_channel_fulfillment_mode` 字典类型与 `CARRIER_LABELING`、`DIRECT_FULFILLMENT_WAREHOUSE` 字典项

## 不执行范围

- 不删除旧字段 `service_level`
- 不删除旧字段 `buyer_scope_mode`
- 不清理历史业务数据
- 不修改客户渠道表数据

## 确认来源

用户要求“彻底解决这些问题”，本次按现有增量 SQL 的确认流程执行最小幂等修复。

## 执行结果

已在当前运行库 `fenxiao` 完成幂等修复和复核：

- `logistics_system_channel.fulfillment_mode`：已存在
- `logistics_system_channel.signature_services`：已存在
- `idx_logistics_system_channel_fulfillment`：已存在
- `logistics_customer_channel`：已存在
- `logistics_customer_channel_system_mapping`：已存在
- `logistics_customer_channel_buyer_scope`：已存在
- `logistics_system_channel_fulfillment_mode` 字典：已补齐并复核中文显示
  - `CARRIER_LABELING`：物流商打单
  - `DIRECT_FULFILLMENT_WAREHOUSE`：直推履约仓

本次没有删除旧字段 `service_level`、`buyer_scope_mode`，也没有清理历史业务数据。旧字段只作为历史兼容残留保留，避免扩大本次修复影响面。

## 验证结果

- 运行库结构对照：系统渠道、客户渠道、仓库绑定、下单规则、上游物流渠道候选、上游物流渠道配对、上游仓库配对相关表和关键索引均无缺口
- 前端类型检查：`npm run tsc` 通过
- 前端契约测试：`npx jest --config jest.config.js tests/logistics-system-channel-contract.test.ts tests/logistics-customer-channel-contract.test.ts --runInBand` 通过，2 个套件、10 个用例通过
- 后端编译：`mvn -pl logistics,integration,ruoyi-admin -am "-DskipTests" compile` 通过
- 后端契约测试：`mvn -pl logistics,integration -am "-Dtest=LogisticsAdminRouteContractTest,IntegrationAdminPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过，19 个用例通过
- 后端打包：先停止占用 `ruoyi-admin.jar` 的 8080 后端进程，再执行 `mvn -pl ruoyi-admin -am "-DskipTests" package` 通过
- 后端重启：通过 `start-backend-local.ps1 -Restart` 启动新 jar，`http://127.0.0.1:8080` 返回 HTTP 200，最新错误日志为空
- CodeGraph：`codegraph sync .` 已执行，索引已是最新
