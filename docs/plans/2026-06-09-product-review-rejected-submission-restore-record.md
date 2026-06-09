# 商城商品驳回稿恢复实现记录

## 背景

商品审核被驳回后，卖家或管理端需要在商品编辑页继续沿用上一次提交的内容进行修改，避免重新填写大量商品资料。该能力不放在审核中心，也不放在商品列表“更多”操作里，只在编辑商城商品页底部提供恢复入口。

## 实现

- 后端复用 `product_review_request.review_status` 和 `review_reason`，不新增审核状态表字段；商城商品列表和详情只返回最近一次审核摘要。
- 新增 `GET /product/admin/distribution-products/{spuId}/latest-rejected-submission`，按 SPU 查询最近一次可复用的驳回审核单，读取 `AFTER` 商品快照和 SKU 快照返回给编辑页。
- 新增商品审核提交时改为保存完整 SPU/SKU 快照，保证后续驳回恢复可以覆盖图片、类目属性、详情图文和 SKU 数据。
- 编辑页底部新增 `恢复上次提交内容` 按钮，仅当最近一次审核状态为 `REJECTED` 时显示；点击后弹确认框，确认后只覆盖当前表单，不立即保存正式商品数据。
- 商品列表 SPU 视图新增 `审核状态` 和 `审核反馈` 两列，继续保留销售状态 Tabs 原有含义。

## 验证

- `mvn -pl product clean test "-Dtest=ProductReviewServiceImplTest,ProductDistributionServiceImplTest"`：通过，17 个测试。
- `mvn -pl product -Dtest=ProductModuleBoundaryContractTest test`：通过，3 个测试。
- `mvn -pl ruoyi-system -Dtest=ProductAdminRouteContractTest test`：通过，1 个测试。
- `npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand`：通过，10 个测试。
- `npm exec tsc -- --noEmit`：通过。
- Browser：`http://127.0.0.1:8001/product/distribution` 正常渲染，表头包含 `审核状态`、`审核反馈`；从列表第一行点击 `编辑` 正常进入 `/product/distribution/edit/31`，编辑页基础信息正常显示。控制台仅看到既有 AntD `destroyOnClose` 弃用提示。
