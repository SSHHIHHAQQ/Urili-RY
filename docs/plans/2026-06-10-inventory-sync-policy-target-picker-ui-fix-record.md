# 库存总览自动同步 WMS 库存设置目标选择 UI 修复记录

## 背景

库存总览的“自动同步WMS库存设置”弹窗中，设置范围切换样式与库存总览 `SPU视图 / SKU视图 / 仓库视图` 不一致；同时 `SPU设置`、`SKU设置`、`明细行设置` 使用下拉选择目标，不适合库存目标数据量较大的场景。

## 调整内容

- 将设置范围、库存同步方式从 `Segmented` 改为 `Radio.Group buttonStyle="solid"`，与库存总览视图切换按钮风格保持一致。
- 新增 `InventorySyncPolicyTargetPicker`：
  - `SPU设置` 使用带搜索、分页、单选的 `ProTable` 选择 SPU。
  - `SKU设置` 使用带搜索、分页、单选的 `ProTable` 选择 SKU。
  - `明细行设置` 使用带搜索、分页、单选的 `ProTable` 选择 SKU + 仓库库存明细行。
- 保留 `卖家维度` 和 `仓库设置` 的下拉选择：
  - 卖家和官方仓库属于受控选项，数量相对有限。
  - 仓库设置继续支持多选官方仓库。
- 同步更新前端 Jest 契约和后端架构契约，确保目标选择器继续使用真实库存总览接口。

## 当前边界

- 本次仍保持后端当前单目标合同：`spuId`、`skuId`、`stockId` 单值保存。
- 前端表格选择器当前使用单选 `radio`，不做伪批量。
- 后续若需要一次选择多个 SPU/SKU/明细行，需要先扩展后端 `SyncPolicyRequest` 和预览/确认逻辑。

## 验证

- 前端类型检查：`npm run tsc` 通过。
- 前端契约测试：`jest tests/inventory-overview-contract.test.ts --runInBand` 通过。
- 后端架构契约：`mvn -pl ruoyi-system -am "-Dtest=InventoryAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- 浏览器验证：
  - 打开 `库存管理 / 库存总览 / 自动同步WMS库存设置`。
  - 设置范围和同步方式均显示为 solid radio button 风格。
  - `SPU设置` 显示 SPU 小表格，支持搜索、分页、单选，选择后“已选择SPU”同步更新。
  - 选择 SPU 后点击“预览影响”，后端预览接口返回影响明细和预览表格，未点击“确认应用”。
  - `SKU设置` 和 `明细行设置` 均显示为小表格，切换范围后已生成的预览结果会清除。

## 数据影响

- 未执行 DDL/DML。
- 浏览器只执行了同步策略预览，没有保存策略，没有改库存数据。
