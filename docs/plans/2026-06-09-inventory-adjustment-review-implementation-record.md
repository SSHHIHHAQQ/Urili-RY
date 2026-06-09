# 库存调整审核实现记录

日期：2026-06-09

## 实现范围

- 后端新增库存调整审核策略、绑定、审核单、操作日志、销量日聚合模型。
- 库存总览单条/批量调整平台总库存时，按“申请退回数量”判断是否进入审核。
- 默认门槛：近 7 日与近 30 日日均销量取大值，再乘以策略保留天数；申请退回数量大于可立即退回数量时进入审核。
- 审核生效时按当前库存重新计算实际可退回数量，实际生效数量不超过当前平台总库存扣减锁定库存后的可用上限。
- 支持人工立即生效、调整计划生效时间、驳回、操作日志、策略组配置、卖家/全局策略绑定。
- 新增 Quartz 任务入口 `inventoryAdjustmentReviewTask.effectDueReviews`，到期自动处理允许自动生效的等待审核单。
- 前端新增 `Inventory/AdjustmentReview/index` 页面，菜单 2452 从占位页切换到真实页面。

## 关键口径

- 主口径是“申请退回多少库存”，不是“把平台库存改到多少”。
- 进入审核后，平台库存不立即扣减。
- 到期或人工生效时，重新读取当前库存：
  - `actualEffectQty = min(requestedAdjustQty, max(0, currentPlatformTotalQty - currentReservedQty))`
  - `effectiveAfterPlatformTotalQty = currentPlatformTotalQty - actualEffectQty`
  - `unfulfilledQty = requestedAdjustQty - actualEffectQty`

## 新增/调整文件

- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/controller/AdminInventoryAdjustmentReviewController.java`
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryAdjustmentReviewServiceImpl.java`
- `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/task/InventoryAdjustmentReviewTask.java`
- `RuoYi-Vue/inventory/src/main/resources/mapper/inventory/InventoryAdjustmentReviewMapper.xml`
- `RuoYi-Vue/sql/20260609_inventory_adjustment_review.sql`
- `react-ui/src/pages/Inventory/AdjustmentReview/index.tsx`
- `react-ui/src/services/inventory/adjustmentReview.ts`
- `react-ui/src/types/inventory/adjustment-review.d.ts`
- `react-ui/tests/inventory-adjustment-review-contract.test.ts`

## 验证

- `mvn -pl inventory -am -DskipTests compile`
- `mvn -pl ruoyi-system -am "-Dtest=InventoryAdminRouteContractTest,SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `npm run tsc -- --pretty false`
- `npx jest --config jest.config.ts tests/inventory-adjustment-review-contract.test.ts tests/inventory-overview-contract.test.ts --runInBand`
- `node scripts/verify-three-terminal.mjs --check-manifest`
- `codegraph sync .`
- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest#inventoryAdjustmentReviewMustUseExactTargetsAndCompletionAssert" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 未执行事项

- 已连接远端数据库执行 `20260609_inventory_adjustment_review.sql`，详见 `docs/plans/2026-06-09-inventory-adjustment-review-sql-execution-record.md`。
- 未启动后端和浏览器做菜单实机点击验证。
