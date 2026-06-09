# 商品审核首版实施记录

日期：2026-06-08

## 本次完成

- 菜单命名与组件：
  - `商品分销审核` 改为 `商品审核`。
  - 菜单组件指向 `Product/Review/index`。
- 数据库脚本：
  - 新增 `RuoYi-Vue/sql/20260608_product_review.sql`。
  - 设计并声明 4 张审核表：`product_review_request`、`product_review_item`、`product_review_snapshot`、`product_review_operation_log`。
  - 增加审核类型、审核状态、风险等级字典 seed。
  - 增加审核页面按钮权限 seed。
  - 脚本带 `@confirm_product_review = 'APPLY_PRODUCT_REVIEW'` 确认 token 和 `45000` fail-closed guard。
- 后端：
  - 新增商品审核领域对象、Mapper、Service、Controller。
  - 新增管理端接口 `/product/admin/reviews/**`。
  - 新增商城商品草稿提交审核接口 `/product/admin/distribution-products/{spuId}/submit-review`。
  - 审核通过 `NEW_PRODUCT` 后，SPU 和审核明细 SKU 从 `DRAFT` 进入 `READY`。
  - 审核驳回保持商品为草稿，并记录驳回原因。
  - 同一 SPU 同时只允许一个待审核单。
  - 有待审核单时，商品编辑、SPU/SKU 销售状态调整、SKU 调价会被拦截，避免审核快照漂移。
  - 商品新增和新增 SKU 只能保存为 `DRAFT`，禁止直接保存为 `READY`。
  - 商品状态接口禁止 `DRAFT -> READY`，必须走商品审核。
- 前端：
  - 新增 `react-ui/src/pages/Product/Review/index.tsx` 审核列表页。
  - 支持类型 Tabs：全部、新增商品、新增SKU、商品资料变更、SKU资料变更、价格变更。
  - 支持状态、提交端、提交时间和关键词筛选。
  - 支持按 `review_type` 展示列表审核重点：新增商品看新增范围、价格、仓库、类目；新增 SKU 看新增 SKU 范围；资料变更看标题、主图和摘要；价格变更看新旧价格区间和影响 SKU。
  - 支持详情抽屉，展示审核单、类型化审核重点、对象明细、快照、操作日志。
  - 详情首个 Tab 为 `审核重点`，按审核类型展示 before/after 快照对比；快照 JSON 前端安全解析，保留原始快照 Tab 作为追溯入口。
  - 支持待审核单通过和驳回。
  - 商城商品列表中草稿 SPU 的动作改为 `提交审核`，不再显示 `提交待上架`。
  - SKU 视图不再开放草稿 SKU 直接提交待上架。
- 契约：
  - `ProductAdminRouteContractTest` 覆盖商品审核管理端路由、权限和前端 service/page 权限 gate。
  - `product-distribution-permission-guard.test.ts` 覆盖审核页 JS 镜像、提交审核入口和前端权限 gate。
  - `reuse-ledger.md` 已登记商品审核管理端模式。

## 本轮未改动

- 未新增或修改数据库表结构。
- 已审核商品删除 SKU 第一版先禁止，尚未设计 `DELETE_SKU` 审核类型。
- seller 端提交审核入口尚未接入。
- 审核详情仍沿用当前抽屉展示，尚未改成和商品编辑页一致的只读弹窗。

## 当前边界

- 首版已跑通 `NEW_PRODUCT` 审核闭环。
- 审核菜单已经按 `review_type` 做类型化列表重点和详情对比展示，后续接入不同审核单后可直接承接展示。
- `ADD_SKU` 审核通过生效逻辑已预留为草稿 SKU 进入 `READY`，但提交入口尚未由商品列表侧接入。
- `EDIT_PRODUCT_INFO` / `EDIT_SKU_INFO` 已接入非草稿商品编辑保存：正式商品不立即覆盖，审核单保存 `BEFORE` 与 `AFTER` 完整商品快照。
- `EDIT_PRICE` 已接入列表调价入口：调价不再直接更新 SKU 销售价，改为生成价格审核单，审核通过后才写入 SKU 销售价。
- 审核驳回后正式商品保持审核前状态，驳回稿保留在 `AFTER` 快照；管理端商品审核列表只负责查看、通过和驳回，不承载卖家的 `继续编辑` 入口。
- 非草稿商品审核通过后应用 `AFTER` 快照，商品销售状态保持审核前状态；已上架商品审核通过后新内容立即生效。
- 非草稿商品编辑时禁止提交删除既有 SKU，避免第一版在无删除审核类型的情况下误删正式 SKU。
- 草稿商品仍直接保存；草稿提交审核仍走 `NEW_PRODUCT`。

## 2026-06-08 编辑审核接入补充

- 后端 `PUT /product/admin/distribution-products` 根据当前商品状态分流：
  - `DRAFT`：继续调用商品保存，直接保存草稿。
  - 非 `DRAFT`：调用商品审核服务创建编辑审核单，返回 `已提交审核`。
- 后端调价接口 `batch-update-sku-sale-price` 改为创建 `EDIT_PRICE` 审核单；审批通过后才批量写入 SKU 销售价。
- 审批通过编辑类审核时，后端会校验当前正式商品快照 hash 与 `BEFORE` hash 是否一致；不一致则拒绝审批，避免覆盖其他已生效变更。
- 前端商品编辑页非草稿状态底部主按钮显示 `提交审核`，提交后使用后端返回提示。
- 管理端商品审核列表不提供 `继续编辑` 操作；驳回后的卖家继续编辑入口应由后续 seller 端或商品列表侧单独承载。
- 管理端商品编辑页不再识别 `reviewId` 参数读取审核单 `AFTER` 快照，避免把卖家继续编辑流程挂在管理端审核菜单上。

## 验证结果

- 2026-06-08 编辑审核接入补充：
  - `mvn -pl product -am -DskipTests clean compile`：通过。
  - `mvn -pl product -am -Dtest=ProductReviewServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，8 tests，0 failures/errors。
  - `npm run tsc`：通过。
  - `npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand`：通过，1 suite，6 tests。
  - `codegraph sync .`：通过，已同步 6 个变更文件。
  - 浏览器运行态验证：
    - `http://127.0.0.1:8001/review-center/product-distribution` 可登录后加载商品审核页。
    - `http://127.0.0.1:8001/product/distribution` 可登录后加载商城商品列表。
    - 非草稿商品编辑页底部主按钮显示 `提交审核`。
    - 控制台仅出现 Ant Design `Modal.destroyOnClose` / `Drawer.width` 废弃提示，未发现本轮审核链路运行错误。
- 2026-06-08 本轮类型化展示补充：
  - 修复审核类型 Tabs 不显示的问题：原实现将 Tabs 放在 ProTable `headerTitle`，但页面同时关闭了 `toolBarRender`，导致 Tabs 被隐藏；现已移到表格上方作为显式类型切换入口。
  - 审核类型 Tabs 增加待审核数量统计，按 `reviewStatus=PENDING` 分别显示 `全部(n)`、`新增商品(n)`、`新增SKU(n)`、`商品资料变更(n)`、`SKU资料变更(n)`、`价格变更(n)`；审核通过或驳回后同步刷新数量。
  - 操作列去掉 `更多` 下拉，待审核行直接展示 `详情 / 通过 / 驳回`，降低审核员操作成本。
  - 修复类型 Tabs 下方大面积空白：根因是全局列表页样式会把直属 `.ant-tabs` 当作主内容容器设置 `flex: 1`，而商品审核的类型 Tabs 只是导航区；现已设置 `flex: none`，避免它吞掉剩余高度并把 ProTable 推到底部。
  - 浏览器 DOM 验证：类型 Tabs 高度 54px、`flex=0 0 auto`，查询区紧贴在 Tabs 下方，`tabsToSearchGap=0`；控制台仅保留既有 Ant Design deprecated prop 提示。
  - 删除管理端商品审核列表的 `继续编辑` 操作，并移除商品编辑页通过 `reviewId` 读取驳回稿的配套深链逻辑；管理端审核菜单只保留 `详情 / 通过 / 驳回`。
  - 浏览器验证：商品审核页已驳回行不再出现 `继续编辑`，页面正文 `hasContinueEdit=false`，该行操作列只保留 `详情`。
  - `npm run tsc`：通过。
  - `npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand`：通过。
  - `codegraph sync .`：通过，已同步 CodeGraph 索引。
  - 浏览器运行态验证：登录管理端后打开 `http://127.0.0.1:8001/review-center/product-distribution`，商品审核页可加载，页面可见 `全部(1) / 新增商品(1) / 新增SKU(0) / 商品资料变更(0) / SKU资料变更(0) / 价格变更(0)` 类型 Tabs；页面标题、筛选区、类型化表头和列表数据正常出现。控制台仅有 Ant Design deprecated prop 警告，未出现页面运行错误。
- `mvn -pl product -am -DskipTests compile`：通过。
- `mvn -pl ruoyi-system -Dtest=ProductAdminRouteContractTest test`：通过。
- `mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test`：通过。
- `mvn -pl product -am -Dtest=ProductDistributionServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `npm run tsc`：通过。
- `npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand`：通过。
- `npx playwright screenshot --timeout=15000 http://127.0.0.1:8001/user/login E:\Urili-Ruoyi\logs\product-review-login-check.png`：通过，仅验证当前前端服务可加载；商品审核业务页等待 SQL 迁移后验证。

## 后续建议

1. 单独设计已审核商品删除 SKU 的审核类型和生效规则，当前第一版先禁止删除既有 SKU。
2. 接 seller 端提交审核、待审锁定、驳回原因查看和驳回稿继续编辑入口。
3. 商品审核详情从当前抽屉展示合并为“商品编辑页同款只读弹窗”，和商品列表查看能力统一。
4. 商品列表补充“存在待审变更”的标识和查看审核单入口，避免卖家重复提交时只看到后端拦截。
5. 后续统一清理 Ant Design `destroyOnClose` / `Drawer.width` 废弃属性，降低浏览器控制台噪音。
