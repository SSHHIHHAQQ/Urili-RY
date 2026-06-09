# 库存调整审核真实联调测试执行记录

日期：2026-06-09

## 执行背景

- 用户已授权在“库存总览”中真实调整平台总库存。
- 用户已授权真实创建、修改库存调整审核策略与策略绑定。
- 本次目标是验证库存调整审核配置中策略组合、卖家绑定、库存总览调整、审核处理、等待期数量变化、到期自动生效与批量调整是否真实生效。

## 数据源与运行态

- 后端配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`，激活 profile 为 `druid`。
- MySQL 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`，通过 `RUOYI_DB_*` 环境变量注入。
- Redis 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`，通过 `RUOYI_REDIS_*` 环境变量注入。
- 后端地址：`http://127.0.0.1:8080`，测试前 HTTP 200。
- 前端地址：`http://127.0.0.1:8001`，测试前 HTTP 200。
- 后端 jar：`RuoYi-Vue/ruoyi-admin/target/ruoyi-admin.jar`，测试前确认运行进程使用该 jar。

## 影响范围

- 会临时调整选定测试库存行的平台总库存。
- 会临时写入或修改以 `TEST-库存调整审核-20260609-` 为前缀的策略。
- 会临时切换全局绑定或测试卖家的卖家绑定，并在测试结束恢复原绑定状态。
- 会临时写入选定 SKU 最近 30 天的 `inventory_sku_sales_daily` 数据，并在测试结束恢复。
- 会产生真实审核单、库存流水和审核操作日志；这些属于测试留痕，不删除。

## 恢复策略

- 测试开始前备份测试库存行、全局绑定、卖家绑定和销量数据。
- 每一阶段结束后核对库存状态。
- 全部测试结束后恢复测试库存行平台库存、恢复原全局绑定、恢复原卖家绑定、恢复原销量数据。
- 测试新增策略保留但改为 `DISABLED`，避免继续影响业务。

## 执行进度

- [x] 确认数据源和运行态
- [x] 获取管理端 API token
- [x] 选择测试库存行并建立备份
- [x] 创建测试策略并验证非法配置拒绝
- [x] 执行单条库存调整策略矩阵
- [x] 执行审核处理与等待期数量变化
- [x] 执行到期自动生效
- [x] 执行批量调整
- [x] 恢复现场
- [x] 回归验证

## 执行日志

### 2026-06-09 初始确认

- 已确认运行态使用远端 MySQL/Redis 连接变量，不读取本地 Docker 数据库。
- 已确认后端和前端服务在线。

### 2026-06-09 真实联调执行结果

- 最终通过 Run ID：`20260609140121`。
- 证据目录：`logs/inventory-adjustment-review-real-test/20260609140121/`。
- 断言数量：203 项，全部 `PASS`。
- 测试卖家：`seller_id=5`。
- 测试库存行：`stock_id=132`、`stock_id=133`。
- 真实测试覆盖：
  - 策略非法配置拒绝：空销量窗口拒绝、卖家绑定缺少对象拒绝。
  - 审核模式：`DISABLED`、`ALWAYS`、`CONDITIONAL`。
  - 调整方向：`DECREASE`、`INCREASE`、`BOTH`。
  - 字段范围：`PLATFORM_TOTAL`、`ALL` 对平台总库存调整的效果。
  - 销量窗口与阈值：`[7,30]` 取大值、`[30]`、不同 `reserveDays`、最低退回数量、最低退回比例。
  - 策略绑定：卖家强制审核覆盖全局不审核、卖家不审核覆盖全局强制审核。
  - 审核处理：驳回、立即生效、修改计划生效时间。
  - 到期自动生效：`autoEffectEnabled=Y` 自动生效、`autoEffectEnabled=N` 保持等待。
  - 人工提前生效：`manualEffectAllowed=N` 时未来计划时间拒绝人工提前生效，到期后仍可由自动任务生效。
  - 等待期库存变化：申请退回 850，等待期内库存降到 300 后实际只生效 300，未满足 550。
  - 批量调整：一条生成审核单、一条直接更新。

### 发现并修复的问题

- 真实测试发现 `InventoryAdjustmentReviewServiceImpl.copyStock` 未复制 `syncMode`、`syncPolicyId`、`syncPolicyScope`、`syncPolicyKey`、`syncStatus`、`lastAutoSyncTime`，审核单生效时会把 `sync_mode` 写成 `null`，触发数据库非空约束失败。
- 已修复为与库存总览 `copyStock` 保持一致，并在 `syncMode` 为空时兜底为 `MANUAL`。
- 修复后重新打包 `ruoyi-admin.jar`，并通过 `start-backend-local.ps1 -Restart` 启动当前 jar。

### 恢复与现场核对

- 样本库存恢复：
  - `stock_id=132`：平台总库存 `0`，平台可售 `0`，平台锁定 `0`。
  - `stock_id=133`：平台总库存 `0`，平台可售 `0`，平台锁定 `0`。
- 测试策略恢复：`TEST-库存调整审核-20260609-20260609140121-*` 没有启用策略。
- 测试审核单恢复：`submit_reason` 包含 `20260609140121` 的审核单没有 `WAITING` 状态。
- 绑定恢复：全局绑定恢复为原策略 `policy_id=2` 且 `ENABLED`；测试卖家绑定已清理。
- 销量数据恢复：测试前最近 30 天备份为空，测试结束已恢复为空。

### 回归验证

- 后端窄测试：`mvn -pl inventory -am "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - `ruoyi-system`：203 tests passed。
  - `inventory`：13 tests passed。
- 前端类型检查：`npm run tsc -- --pretty false`，通过。
- 前端库存合同测试：`npx jest --config jest.config.ts tests/inventory-adjustment-review-contract.test.ts tests/inventory-overview-contract.test.ts --runInBand`
  - 2 suites passed，8 tests passed。
- 三端 manifest guard：`node scripts/verify-three-terminal.mjs --check-manifest`，通过。
- CodeGraph：`codegraph sync .`，完成，同步 3 个变更文件。
