# 库存调整审核配置效果测试记录

日期：2026-06-09

## 目标

对库存调整审核配置做可用性验收，重点验证策略组和卖家绑定不是只保存字段，而是真正影响库存调整审核判定、审核单生效和定时任务筛选。

## 覆盖范围

### 策略组字段

- 策略状态：`ENABLED` / `DISABLED`
- 审核模式：`DISABLED` / `CONDITIONAL` / `ALWAYS`
- 方向范围：`DECREASE` / `INCREASE` / `BOTH`
- 字段范围：`PLATFORM_TOTAL` / `ALL` / 非支持字段
- 销量窗口：`[7]` / `[30]` / `[7,30]` / 无效窗口
- 保留天数：`1` / `7` / `30` / 负数
- 冷却小时：默认值 / 自定义值 / 负数
- 最低退回数量：低于门槛 / 等于门槛 / 负数
- 最低退回比例：低于门槛 / 等于门槛 / 超出范围
- 到期自动生效：确认 Mapper 只捞取 `auto_effect_enabled = Y` 的到期审核单
- 允许人工生效：未来计划生效时间下，`manual_effect_allowed = N` 会阻止人工提前生效

### 卖家绑定字段

- 全局绑定
- 卖家绑定
- 卖家绑定优先于全局绑定
- 绑定停用后回退到全局绑定
- 策略停用后不再命中该策略
- 无启用匹配策略时不再硬回退到默认审核
- 全局绑定对象 ID 自动归零
- 卖家绑定必须提供正数卖家 ID
- 优先级必须大于 0
- 绑定状态只允许 `ENABLED` / `DISABLED`

## 发现并修复的问题

1. 无匹配启用策略时，服务层原本会硬回退到默认条件审核。
   - 风险：停用策略或停用绑定后，仍可能触发库存调整审核，配置没有真实效果。
   - 修复：无匹配启用策略时回退为 `DISABLED` 的“无匹配策略”，不再触发审核。

2. 后端策略/绑定保存缺少枚举和数值边界校验。
   - 风险：API 可以保存页面之外的无效值，后续判定表现不可控。
   - 修复：补齐策略状态、审核模式、方向范围、字段范围、销量聚合方式、销量窗口、天数、小时、门槛、Y/N 开关、绑定类型、绑定状态、卖家 ID 和优先级校验。

3. 前端卖家绑定表单没有按绑定类型动态要求卖家 ID。
   - 风险：新增卖家绑定时可能提交空对象或 0。
   - 修复：`SELLER` 类型要求正数卖家 ID，`GLOBAL` 类型禁用对象 ID，由后端归零。

4. 前端销量窗口输入缺少格式校验。
   - 风险：填入无效窗口后才由后端拒绝，交互不清晰。
   - 修复：前端校验至少包含一个正整数天数。

## 新增测试

- `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/service/impl/InventoryAdjustmentReviewPolicyEffectTest.java`
  - 覆盖 3 种审核模式 × 3 种方向范围 × 2 个调整方向的矩阵。
  - 覆盖销量窗口、保留天数、锁定库存、最低退回数量、最低退回比例。
  - 覆盖卖家绑定优先级、绑定停用、策略停用和无匹配策略。
  - 覆盖冷却小时、人工提前生效开关、到期自动生效 SQL 筛选。
  - 覆盖策略组和绑定保存校验。
- `react-ui/tests/inventory-adjustment-review-contract.test.ts`
  - 固定审核配置页面字段、销量窗口校验和卖家绑定动态校验。
- `react-ui/tests/three-terminal.manifest.json`
  - 已登记 `InventoryAdjustmentReviewPolicyEffectTest`，防止后续验证漏跑。

## 验证命令

- `mvn -pl inventory -am "-Dtest=InventoryAdjustmentReviewPolicyEffectTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：通过，`8` 个测试。
- `mvn -pl inventory -am "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：通过，`ruoyi-system` 203 个测试、`inventory` 13 个测试。
- `npm run tsc -- --pretty false`
  - 结果：通过。
- `npx jest --config jest.config.ts tests/inventory-adjustment-review-contract.test.ts tests/inventory-overview-contract.test.ts --runInBand`
  - 结果：通过，`2` 个测试套件、`8` 个测试。
- `node scripts/verify-three-terminal.mjs --check-manifest`
  - 结果：通过。

## 未执行事项

- 未修改远端数据库策略数据。
- 未启动浏览器做页面手工点击；本轮以服务级自动化、前端合同和 manifest 校验为准。
