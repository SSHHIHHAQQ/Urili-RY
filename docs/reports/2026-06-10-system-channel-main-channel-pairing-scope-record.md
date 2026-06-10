# 系统渠道主仓渠道配对粒度调整记录

时间：2026-06-10 23:55

## 背景

系统渠道与主仓渠道的配对不应受主仓仓库影响。正确规则是：

- 一个系统渠道在同一配对用途下只能配对一个主仓渠道。
- 一个主仓渠道可以被多个系统渠道复用。
- 主仓仓库、履约仓库只影响仓库自身配对，不参与系统渠道与主仓渠道的配对过滤。

## 本次调整

- 前端 `系统渠道管理` 增加 `主仓渠道映射` 页签，配置时只选择上游系统和主仓渠道。
- 前端不再在系统渠道主仓渠道配对中提交 `systemWarehouseCode` / `upstreamWarehouseCode`。
- 后端 `LogisticsChannelPairingRequest` 移除仓库字段。
- 后端保存物流渠道配对时：
  - 不再要求先存在仓库配对。
  - 不再按上游仓库筛选候选物流渠道。
  - 按 `system_channel_code + pairing_role` 判断系统渠道是否已经配对。
  - 历史兼容列 `system_warehouse_code` / `upstream_warehouse_code` 写入空串。
- 新增增量脚本：
  - `RuoYi-Vue/sql/20260610_system_channel_main_channel_pairing_scope.sql`
  - 已纳入 `SqlExecutionGuardContractTest` 确认 token 检查。

## 数据库执行

目标环境：后端当前激活 `druid` 数据源，数据库为 `fenxiao`。连接来源为 `.env.local` 环境变量，未输出任何敏感值。

执行前预检：

- `upstream_system_logistics_channel_pairing` 中 `(system_channel_code, pairing_role)` 无重复行。
- 旧索引仍包含仓库维度：
  - `uk_upstream_channel_pairing_system_role`: `system_warehouse_code,system_channel_code,pairing_role`
  - `idx_upstream_channel_pairing_upstream_role`: `connection_code,upstream_warehouse_code,upstream_channel_code,pairing_role`

执行后结果：

- `uk_upstream_channel_pairing_system_role`: `system_channel_code,pairing_role`
- `idx_upstream_channel_pairing_upstream_role`: `connection_code,upstream_channel_code,pairing_role`

## 验证

- `npm run tsc`：通过。
- `npx jest --config jest.config.js tests/logistics-system-channel-contract.test.ts --runInBand`：通过，5 个用例通过。
- `mvn -pl integration -am "-Dtest=IntegrationAdminPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，7 个用例通过。
- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，81 个用例通过。
- `mvn -pl ruoyi-admin -am "-DskipTests" package`：第一次因旧后端进程锁定 jar 失败；停止 8080 旧进程后重新打包通过。
- `.\start-backend-local.ps1 -Restart`：已重启后端。
- `http://127.0.0.1:8080`：返回 HTTP 200。

## 未完成验证

本次未做浏览器自动化截图验证。原因是当前环境未暴露 in-app Browser 控制工具，Playwright CLI 缺少默认浏览器，且项目未安装可直接调用的 Playwright 包；为避免额外下载浏览器，本次未引入新的浏览器依赖。
