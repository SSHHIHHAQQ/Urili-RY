# 复用台账

## ProductCenter 买家可见商品列表模板

- 位置：
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/controller/AdminProductCenterController.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/IProductCenterService.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductCenterServiceImpl.java`
  - `RuoYi-Vue/sql/20260608_product_center_menu_seed.sql`
  - `react-ui/src/services/product/productCenter.ts`
  - `react-ui/src/components/ProductCenter/ProductCenterPage.tsx`
  - `react-ui/src/components/ProductCenter/ProductCenterDetailModal.tsx`
  - `react-ui/src/pages/Product/ProductCenter/index.tsx`
  - `react-ui/tests/product-center-contract.test.ts`
- 当前用途：
  - 固定“商品中心”是买家可见商品列表视角，只读展示已上架 SPU/SKU。
  - 固定后端 DTO 不返回卖家、客户 SKU、供货价、管控原因等卖家/管理端敏感字段，只保留商品名称、系统 SPU/SKU、销售价、库存、仓库、类目、卖点、详情图文和参数。
  - 固定管理端入口使用 `/product/admin/product-center` 和 `product:center:list/query`，当前仅作为内测/备用入口；共享列表组件通过 `fetchList/fetchProduct` 注入数据源，后续买家端可以替换为 buyer portal service。
  - 固定详情弹窗复用买家预览图文流，但商品中心使用 `mode="real"`，不展示预览提示、样式预览价或假库存兜底。
- 复用规则：
  - 后续拆 `buyer-ui` 时优先迁移 `ProductCenterPage` 的布局和 `ProductCenterDetailModal` 的真实模式，不要重新实现一套买家商品列表。
  - 如果新增买家可见字段，先确认不属于卖家敏感信息或成本/管控字段，并同步 `ProductCenterServiceImplTest` 与 `product-center-contract.test.ts`。
  - 如果新增购买、加入采购单等动作，动作入口必须走买家端权限和接口；管理端商品中心保持只读，不把 buyer 权限或下单接口混入 admin 页面。

## product 商品审核管理端模式

- 位置：
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/controller/AdminProductReviewController.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/IProductReviewService.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductReviewServiceImpl.java`
  - `RuoYi-Vue/product/src/main/resources/mapper/product/ProductReviewMapper.xml`
  - `RuoYi-Vue/sql/20260608_product_review.sql`
  - `react-ui/src/services/product/productReview.ts`
  - `react-ui/src/pages/Product/Review/index.tsx`
  - `react-ui/tests/product-distribution-permission-guard.test.ts`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/ProductAdminRouteContractTest.java`
- 当前用途：
  - 固定商品审核管理端接口使用 `/product/admin/reviews`，权限使用 `review:productDistribution:list/query/approve/reject/log`。
  - 固定商城商品草稿提交审核通过 `POST /product/admin/distribution-products/{spuId}/submit-review`，不再走 `DRAFT -> READY` 直改状态。
  - 固定同一 SPU 同时只允许一个待审核单；待审期间商品编辑、SPU/SKU 销售状态和 SKU 调价必须被拦截，避免审核快照漂移。
  - 固定商城商品列表从 `product_review_request` 派生最近一次审核状态和审核反馈，不在商品主表重复新增审核字段。
  - 固定编辑页通过 `GET /product/admin/distribution-products/{spuId}/latest-rejected-submission` 读取最近一次被驳回的完整提交快照；入口只放在编辑页底部“恢复上次提交内容”按钮，不放审核中心或列表更多操作。
  - 固定审核列表以 `review_type` 做类型 Tabs，`全部` 只作为前端筛选，不进入数据库枚举。
  - 固定审核列表和详情必须按 `review_type` 做类型化展示：新增商品、新增 SKU、商品资料变更、SKU 资料变更、价格变更分别展示不同审核重点，详情首个 Tab 保留 before/after 快照对比。
- 复用规则：
  - 后续接入 `ADD_SKU`、`EDIT_PRODUCT_INFO`、`EDIT_SKU_INFO`、`EDIT_PRICE` 时继续复用 `product_review_request/item/snapshot/operation_log`，不要按类型拆新审核主表。
  - 已有商品资料、已有 SKU 资料和价格变更不得先改正式商品表；必须先进入审核快照，审核通过后由 product service 事务化应用。
  - 新增审核类型、状态或按钮权限时，同步更新 SQL seed、Controller 权限、React service/page 权限 gate、`ProductAdminRouteContractTest` 和前端权限合同测试。

## PartnerManagement 管理端同构模板

- 位置：
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.js`
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.js`
  - `react-ui/src/components/PartnerManagement/PartnerDeptModal.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerDeptModal.js`
  - `react-ui/scripts/check-partner-management-template.mjs`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminAccountPermissionUiContractTest.java`
- 当前用途：
  - 固定卖家管理和买家管理共用 `PartnerManagement` 模板；差异只通过端类型、文案、路由、权限标识、字段配置和 service 注入。
  - 固定账号创建和重置密码不能静默回退默认密码 `U12346`；创建账号必须人工填写 5-20 位密码，重置密码必须通过端账号 `resetPwd` 传入人工临时密码。
  - 固定部门弹窗列表加载和部门树加载互不耦合；部门树失败不能阻断部门列表，保存或删除后必须分别刷新列表和树。
- 复用规则：
  - 后续新增同构管理端主体页面时，先复用该模板；不要为卖家、买家分别重写一套表格、账号弹窗或部门弹窗。
  - 修改模板行为时，必须同步 `.tsx` 与 `.js` 镜像，并更新 `check-partner-management-template.mjs` 或对应合同测试，避免后续复制时带回默认密码、权限入口或树加载耦合问题。

## Portal 端内 session 401 模板

- 位置：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImpl.java`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java`
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImplTest.java`
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImplTest.java`
  - `docs/plans/2026-06-08-three-terminal-p0p1-portal-session-unauthorized-record.md`
- 当前用途：
  - 固定卖家端、买家端自助接口的登录态失效必须抛 `ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED)`。
  - 固定端内自助 service 的二次账号校验必须使用 `subjectId + accountId`，不能用只按账号 ID 或管理端语义的查询异常兜底。
  - 固定当前端 session 的 `terminal` 不匹配、`subjectId` 缺失、`accountId` 缺失、`tokenId` 缺失或端内账号绑定不存在时，都按 401 处理，方便前端清理当前端 token 并跳回当前端登录页。
  - 固定主体停用、账号停用、账号锁定、旧密码错误等已识别业务状态仍保留业务异常，不混成登录态失效。
- 复用规则：
  - 后续新增 seller/buyer portal 自助接口时，先复用对应 `assert*SessionAccount(...)` 或同等 401 helper；不要在业务方法里重新手写普通 `new ServiceException("登录状态已失效")`。
  - 后续新增 portal facade 时，只要入口依赖当前端登录态，非当前端 session 必须在调用共享业务 service 或 mapper 前 fail-closed，并带 `HttpStatus.UNAUTHORIZED`。
  - 新增负向测试时必须同时断言 message 和 code；只断言“抛异常”不足以证明前端能进入端内 401 清理流程。

## inventory 管理端路由权限合同

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/InventoryAdminRouteContractTest.java`
  - `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/controller/AdminInventoryOverviewController.java`
  - `react-ui/src/services/inventory/overview.ts`
  - `react-ui/src/pages/Inventory/Overview/index.tsx`
  - `react-ui/tests/three-terminal.manifest.json`
- 当前用途：
  - 固定库存概览管理端接口必须使用 `/inventory/admin/overview` 后台入口，不能回退到裸 `/inventory`，也不能误接入 anonymous、portal、seller 或 buyer 端权限面。
  - 固定后台权限点为 `inventory:overview:list/query/adjust/ledger`，前端库存调整入口必须继续由 `inventory:overview:adjust` 控制。
  - 固定 React service 使用 `/api/inventory/admin/overview`，避免管理端页面误调 portal 或普通 inventory 路由。
  - 固定 `verify:three-terminal` 必须实际执行 `InventoryAdminRouteContractTest`，不能只依赖 `ruoyi-admin -am -DskipTests test-compile` 的编译覆盖。
- 复用规则：
  - 后续新增 inventory 管理端接口时，先判断是否属于管理端 sys 权限面；若属于，必须使用 `@PreAuthorize("@ss.hasPermi('inventory:...')")`，不能改成 seller/buyer portal 权限。
  - 新增 inventory 管理端页面或 service 时，同步更新 route/permission/service/frontend 合同测试和 `three-terminal.manifest.json`。

## InventoryAdjust 管理端库存调整组件

- 位置：
  - `react-ui/src/components/InventoryAdjust/InventoryAdjustButton.tsx`
  - `react-ui/src/pages/Inventory/Overview/index.tsx`
  - `react-ui/src/pages/Product/Distribution/index.tsx`
  - `react-ui/src/services/inventory/overview.ts`
  - `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/controller/AdminInventoryOverviewController.java`
  - `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryOverviewServiceImpl.java`
- 当前用途：
  - 固定管理端库存调整入口复用同一个 React 组件，库存总览和商城商品列表都只提供入口，不复制库存校验和保存规则。
  - 固定库存调整仍调用 `/inventory/admin/overview` 的预览与确认接口，由库存模块统一校验官方仓来源可用、来源在途、三方仓在途限制、锁定库存下限，并统一写库存流水。
  - 固定商品列表的库存调整不写商品操作日志；库存变更审计以 `inventory_stock_ledger` 为准，避免商品日志与库存流水重复记录。
- 复用规则：
  - 后续需要在商品详情、审核详情或其他管理端页面加库存快速调整时，优先复用 `InventoryAdjustModal` 或 `InventoryAdjustButton`，不要在业务页面里手写库存调整表格。
  - 前端入口必须同时检查 `inventory:overview:query` 和 `inventory:overview:adjust`；后端仍以库存模块权限为最终权限边界。
  - 商品状态、管控状态不应在前端额外拦截库存调整；是否允许调整由库存模块的库存行规则决定。

## integration 管理端权限合同与 surefire 覆盖

- 位置：
  - `RuoYi-Vue/integration/src/test/java/com/ruoyi/integration/architecture/IntegrationAdminPermissionContractTest.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/controller/AdminUpstreamSystemController.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/controller/AdminSourceProductController.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/controller/AdminSourceWarehouseStockController.java`
  - `RuoYi-Vue/sql/upstream_system_management_seed.sql`
  - `RuoYi-Vue/sql/business_menu_seed.sql`
  - `RuoYi-Vue/sql/20260605_source_product_library_menu_component.sql`
  - `RuoYi-Vue/sql/20260606_source_warehouse_stock_menu_rename.sql`
  - `react-ui/scripts/verify-three-terminal.mjs`
  - `react-ui/tests/three-terminal.manifest.json`
- 当前用途：
  - 固定 integration 管理端 controller 只能使用 `/integration/admin/**` 后台入口，不能误接入 anonymous、portal、seller 或 buyer 端权限面。
  - 固定上游系统管理接口的精确权限：`integration:upstream:list/query/add/edit/credential/sync/dimensionSync/inventoryQuery/inventorySync/pair/log`。
  - 固定来源商品库只读接口继续使用 `product:list:list`，来源仓库库存只读接口继续使用 `inventory:sourceWarehouse:list`。
  - 固定这些后台权限必须在现有 seed SQL 中存在，并且 `verify:three-terminal` 必须实际生成 integration 模块 surefire 报告。
- 复用规则：
  - 后续新增或改造 integration 管理端接口时，先判断是否属于管理端 sys 权限面；若属于，必须使用 `@PreAuthorize("@ss.hasPermi('...')")` 或明确的 `hasAnyPermi`，不能改成 seller/buyer portal 权限。
  - mutating 接口必须保留 `@Log`，不能只靠菜单权限或前端按钮隐藏。
  - 如果新增 `integration:upstream:*` 或其他 integration 管理端权限，必须同步 seed SQL、controller 权限、前端权限 gate 和 `IntegrationAdminPermissionContractTest`。
  - 上游仓库、物流渠道、SKU 配对删除必须使用 `connectionCode + pairingId` 双作用域；controller 路由、service、mapper SQL 和前端 service 都不得回退到裸 pairingId 删除。
  - 如果新增新的 critical integration 合同测试，必须加入 `react-ui/tests/three-terminal.manifest.json` 的 `backendTestClasses`；如果类名不匹配当前 critical pattern，还必须加入 `criticalBackendExplicitTestClasses`，确保 surefire XML 由总验证入口检查，不能只依赖 reactor compile。

## 上游履约/报价仓绑定角色

- 位置：
  - `RuoYi-Vue/sql/upstream_system_management_seed.sql`
  - `RuoYi-Vue/sql/20260607_upstream_pairing_role_binding.sql`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/support/UpstreamSystemConstants.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSystemServiceImpl.java`
  - `RuoYi-Vue/warehouse/src/main/resources/mapper/warehouse/WarehouseMapper.xml`
  - `react-ui/src/pages/UpstreamSystem/components/SyncTabs.tsx`
  - `react-ui/src/pages/Warehouse/WarehouseManagementPage.tsx`
- 当前用途：
  - 固定 `upstream_system_warehouse_pairing.pairing_role` 承载官方仓的两类绑定：`FULFILLMENT` 表示履约仓，`QUOTE` 表示报价仓。
  - 固定接入结算类型决定绑定用途：`upstream-payable` 只能配履约，`self-operated-receivable` 只能配报价；前端传入用途后，后端仍按结算类型二次校验。
  - 固定物流渠道配对必须带 `system_warehouse_code + upstream_warehouse_code + pairing_role`，避免同一渠道码跨仓库误配。
  - 固定库存、来源仓库库存和出库履约口径只读取 `FULFILLMENT` 仓库配对；`QUOTE` 只服务报价/试算链路，不进入库存来源。
- 复用规则：
  - 后续凡是需要把上游仓库翻译为系统仓库的库存、尺寸重量、出库、来源仓库库存读模型，只能使用 `FULFILLMENT` 配对。
  - 后续凡是需要调用自营仓报价或运费试算，只能使用 `QUOTE` 配对，不得复用履约仓字段临时判断。
  - 新增系统渠道与上游渠道绑定时，必须先确认对应用途的仓库绑定已经存在，再写渠道绑定。
  - 不要把履约仓、报价仓字段复制到 `warehouse` 或 `official_warehouse` 表；事实源仍是上游配对表，仓库列表通过 mapper 分角色 join 展示。

## SQL DDL 可重放 Helper 与条件 Modify Guard

- 位置：
  - `RuoYi-Vue/sql/20260604_source_product_library_sku_candidate_fields.sql`
  - `RuoYi-Vue/sql/20260605_product_distribution_status_price_log.sql`
  - `RuoYi-Vue/sql/20260604_currency_showapi_sync_migration.sql`
  - `RuoYi-Vue/sql/20260605_mall_product_sku_dimension_fields.sql`
  - `RuoYi-Vue/sql/20260605_mall_product_editor_ui_sample_data.sql`
  - `RuoYi-Vue/sql/20260606_upstream_sync_staging_diff.sql`
  - `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`
  - `RuoYi-Vue/sql/20260604_portal_direct_login_ticket.sql`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalDirectLoginTicketSqlContractTest.java`
  - `docs/plans/2026-06-07-three-terminal-p0p1-sql-ddl-replay-guard-record.md`
  - `docs/plans/2026-06-07-three-terminal-p0p1-account-identity-modify-replay-guard-record.md`
  - `docs/plans/2026-06-07-three-terminal-p0p1-direct-login-ticket-modify-replay-guard-record.md`
  - `docs/plans/2026-06-07-three-terminal-p0p1-password-default-inventory-contract-record.md`
- 当前用途：
  - 固定增量 SQL 中 `ADD COLUMN` 不直接裸写多列 `ALTER TABLE`，而是通过脚本内局部 `add_column_if_missing(...)` 按列幂等补齐。
  - 固定依赖 `AFTER xxx` 的补列脚本，在关键锚点列缺失时先用 `assert_column_exists(...)` fail-closed。
  - 固定 `MODIFY COLUMN` 不无条件执行；对 `product_sku.sale_price` 这类定义收敛，先检查 precision、scale、nullable、comment，只有不满足目标定义时才修改。
  - 固定 seller/buyer 账号身份列 `user_name` / `nick_name` / `password` 的定义收敛也必须通过 `modify_terminal_account_identity_columns_if_needed(...)`：先确认三列存在，再检查类型、长度、nullable 和 default，不满足目标定义时才执行 `MODIFY`。
  - 固定 `seller_account.password` / `buyer_account.password` 只能是 `varchar(100) not null` 且无空串默认值；fresh seed、三端隔离迁移和存量库修正都不得使用 `default ''` 给遗漏密码的写入兜底。
  - 固定 `portal_direct_login_ticket` 的票据审计列定义收敛也必须通过 `modify_portal_direct_login_ticket_columns_if_needed(...)`：先确认目标列完整，再检查类型、长度、nullable 和 default，不满足目标定义时才执行 `MODIFY`。
  - 固定唯一键冲突窗口不交给 MySQL 原生异常；`GENERIC_RATES` 与 `SHOWAPI_BANK_RATE` 并存时先显式 signal，要求人工处理半迁移状态。
  - 固定 `20260606_upstream_sync_staging_diff.sql` 的仓库/物流渠道 payload 列和 SKU `wms_payload_*` 列也使用 `add_column_if_missing(...)`；在依赖 `after status` 或 `after source_payload_hash` 前，必须先用 `assert_column_exists(...)` 明确锚点列。
  - 固定三端隔离主迁移的 `assert_three_terminal_isolation_preflight(...)` 模式：混合 DDL/DML 的大脚本必须在首条业务 DDL/DML 前完成只读 preflight，先检查目标 database、既有表关键列和 legacy `user_id` 绑定，不在 preflight 内修数据。
- 复用规则：
  - 后续新增或修改业务增量 SQL 时，凡是补列，优先复用 `information_schema.columns` + dynamic DDL 的 `add_column_if_missing(...)` 模式；不要回退到裸多列 `ALTER TABLE ... ADD COLUMN`。
  - 如果补列依赖物理列顺序，必须先断言锚点列存在；不要让脚本在 dynamic DDL 里报模糊错误。
  - 如果必须收敛已有列定义，先写条件 helper 和明确失败信息，再执行 `MODIFY COLUMN`；不要在可重放脚本中无条件修改列定义。
  - 收敛端账号密码列前必须先确认没有 null 或空白密码；如果存在，先走 legacy backfill、人工重置或明确修复，不得由 DDL 默认值吞掉。
  - 如果脚本混合 `CREATE/ALTER/UPDATE` 等非事务动作，必须在脚本头部说明 MySQL DDL 隐式提交、可能半执行，以及失败后修复原因并从头重放同一幂等脚本的策略。
  - 已被当前 seed 基线吸收的短脚本，仍应保持幂等 helper，以兼容历史库、半迁移库和 fresh bootstrap 后补跑。
  - 综合 seed 如果同时包含 fresh DDL 和已有环境补授权/回填，例如 `seller_buyer_management_seed.sql`，必须要求显式执行画像；`FRESH_BOOTSTRAP` 只能用于没有 seller/buyer 端内核心表的新环境，`PATCH_EXISTING` 只能用于已有端内表的环境。
  - 同类脚本需要在 `SqlExecutionGuardContractTest` 增加静态合同，至少锁住 helper 存在、裸 DDL 不回归、确认 token 在第一个高影响动作前执行。
  - 日期前缀增量 SQL 统一由 `DATED_SQL_FILE` 自动发现，不再只扫描 `202606*.sql`；后续新增 `202607...sql`、`202701...sql` 等日期命名脚本时，同样必须 fail-closed。
  - dynamic DDL helper 必须纳入 high-impact hint；`set @ddl = concat('alter table ...')`、`create index`、`drop table/view`、`truncate table`、`rename table` 等动态执行形态不得绕过确认 token 合同。
  - integration Mapper 已硬依赖的 staging/state/batch 表、库存快照、payload/hash 列和读模型已固定为 bootstrap 后必跑 SQL 清单，见 `docs/architecture/integration-bootstrap-required-sql.md`；不要把带 `OFFICIAL_MASTER` 清理回填的 read model 脚本直接吸收到基础 seed。

## 菜单 Slot Guard、Portal 精确权限与远程菜单缓存 Key

- 位置：
  - `RuoYi-Vue/sql/20260604_product_category_attribute_seed.sql`
  - `RuoYi-Vue/sql/20260606_source_warehouse_stock_menu_rename.sql`
  - `RuoYi-Vue/sql/20260605_mall_product_distribution_seed.sql`
  - `RuoYi-Vue/sql/20260605_order_after_sale_menu_seed.sql`
  - `RuoYi-Vue/sql/20260608_overseas_channel_carrier_menu_restructure.sql`
  - `RuoYi-Vue/sql/20260605_source_product_library_menu_component.sql`
  - `RuoYi-Vue/sql/20260605_seller_account_lock_control.sql`
  - `RuoYi-Vue/sql/20260605_buyer_account_lock_control.sql`
  - `RuoYi-Vue/sql/top_menu_seed.sql`
  - `RuoYi-Vue/sql/business_menu_seed.sql`
  - `RuoYi-Vue/sql/currency_configuration_seed.sql`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalPermissionSupport.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalAnonymousEndpointContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalPermissionSupportTest.java`
  - `react-ui/src/utils/remoteMenuStorage.ts`
  - `react-ui/src/utils/remoteMenuStorage.js`
  - `react-ui/src/services/session.ts`
  - `react-ui/src/access.ts`
  - `react-ui/src/access.js`
  - `react-ui/tests/remote-menu-route-guard.test.ts`
  - `react-ui/scripts/check-partner-management-template.mjs`
  - `docs/plans/2026-06-07-three-terminal-p0p1-menu-portal-cache-guard-record.md`
- 当前用途：
  - 固定 `sys_menu` 增量 seed 在 upsert 前先做 slot/signature guard，允许明确的旧占位签名迁移到正式页，拒绝其他菜单占用。
  - 固定 `2442` 币种菜单只由财务币种脚本持有，权限使用 `finance:currency:*`，业务基础配置 seed 不再声明 `basic:currency:list`。
  - 固定 `currency_configuration_seed.sql` 的 `tmp_currency_configuration_sys_menu_guard` 同 ID 校验必须纳入 `parent_id/menu_type`，允许历史 `2442` 旧占位签名迁移到正式页，但拒绝挂错父级或类型错误的币种菜单和按钮菜单。
  - 固定 `2440/2441` 商品分类/属性菜单只由商品分类属性脚本持有，`business_menu_seed.sql` 不再声明 `basic:productCategory:list` / `basic:productAttribute:list`。
  - 固定 `20260604_product_category_attribute_seed.sql` 的 `tmp_product_category_attribute_sys_menu_guard` 同 ID 校验必须纳入 `parent_id/menu_type`；`2440/2441` 允许历史占位签名迁移到正式页，但页面菜单必须保持 `parent_id=2090`、`menu_type=C`，按钮菜单必须保持对应页面父级和 `menu_type=F`。
  - 固定 `2421` 来源仓库库存菜单只由 `20260606_source_warehouse_stock_menu_rename.sql` 持有；该脚本允许历史占位签名升级到 `Inventory/SourceWarehouseStock/index`，并在缺失时插入菜单。
  - 固定 `20260606_source_warehouse_stock_menu_rename.sql` 的 `tmp_source_warehouse_stock_sys_menu_guard` 同 ID 校验必须纳入 `parent_id/menu_type`；旧占位和正式组件两条允许签名都必须保持 `parent_id=2080`、`menu_type=C`。
  - 固定 `2400` 来源商品库最终页面主 owner 为 `business_menu_seed.sql`；`20260605_source_product_library_menu_component.sql` 只作为历史组件迁移脚本保留，必须先用 `tmp_source_product_library_menu_component_guard` / `assert_source_product_library_menu_component_guard()` 允许旧占位签名 `Common/PlannedPage/index` + `ProductList` + `product:list:list` 迁移到最终签名 `Product/SourceProductLibrary/index` + `SourceProductLibrary` + `product:list:list`。
  - 固定 `2402` 商城商品列表菜单只由 `20260605_mall_product_distribution_seed.sql` 持有，`business_menu_seed.sql` 不再声明 `product:distribution:list` 页面菜单。
  - 固定 `20260605_mall_product_distribution_seed.sql` 的 `tmp_mall_product_distribution_sys_menu_guard` 同 ID 校验必须纳入 `parent_id/menu_type`，避免历史 `2402` 菜单挂错商品父级或类型错误时被静默改写。
  - 固定 `2402` 下 `2485/2486` 商城商品调价和操作日志按钮也只由 `20260605_mall_product_distribution_seed.sql` 持有；`20260605_product_distribution_status_price_log.sql` 只负责状态、价格、日志表和字典迁移，不再写 `sys_menu`。
  - 固定 `2412` 售后管理菜单只由 `20260605_order_after_sale_menu_seed.sql` 持有，`business_menu_seed.sql` 不再声明 `order:afterSale:list` 页面菜单。
  - 固定 `20260605_order_after_sale_menu_seed.sql` 的 `tmp_order_after_sale_sys_menu_guard` 同 ID 校验必须纳入 `parent_id/menu_type`，避免历史 `2412` 菜单挂错订单父级或类型错误时被静默改写。
  - 固定 `2054` 物流商管理菜单只由 `20260608_overseas_channel_carrier_menu_restructure.sql` 持有；该脚本同时负责把 `2041/2042` 系统渠道/客户渠道从历史 `2040` 渠道管理目录迁入 `2030` 海外仓服务设置，并删除迁空后的 `2040` 顶级目录。
  - 固定 `20260608_overseas_channel_carrier_menu_restructure.sql` 的 `tmp_overseas_channel_menu_guard` 同 ID 校验必须纳入 `parent_id/menu_type`；`2041/2042` 只允许历史父级 `2040` 或最终父级 `2030` 两种签名，`2054` 必须保持 `parent_id=2030`、`menu_type=C`。
  - 固定 `2010` 主体管理顶级目录唯一写入 owner 为 `top_menu_seed.sql`；`seller_buyer_management_seed.sql` 和 `20260606_admin_partner_page_direct_login_seed.sql` 只能在写入 `2011/2012` 及按钮前断言 `2010` 已存在且保持 `parent_id=0/menu_type=M/path=partner/route_name=PartnerManagement/perms=''`，不得再 upsert `2010`。
  - 固定 `20260605_seller_account_lock_control.sql` / `20260605_buyer_account_lock_control.sql` 的账号锁定按钮菜单 slot guard 必须纳入 `parent_id/menu_type`；`2322` 必须保持 `parent_id=2011`、`menu_type=F`，`2323` 必须保持 `parent_id=2012`、`menu_type=F`。
  - 固定 `top_menu_seed.sql` 在复用若依原生 `108/3` 以及调整顶级目录前先做 slot/signature guard；其同 ID 校验必须纳入 `parent_id/menu_type`，允许 `108` 从若依原生日志目录迁移到日志中心，但不允许历史菜单挂错父级或菜单类型时被静默改写。
  - 固定 `top_menu_seed.sql` 对 `2040/2000` 历史草案 cleanup 必须先走独立 `tmp_top_menu_legacy_cleanup_guard` / `assert_top_menu_legacy_cleanup_guard()`；`2040` 只允许 `urili-channel` / `channel` 的旧渠道草案签名，`2000` 只允许旧 `URILI运营后台` 顶级 wrapper root 语义，禁止裸 `update sys_menu where menu_id in (2040, 2000)`。
  - 固定 `business_menu_seed.sql` 写入二级业务菜单前，必须先确认父菜单 `2050/2060/2070/2080/2100` 已由 `top_menu_seed.sql` 提供；其 `tmp_business_menu_sys_menu_guard` 的同 ID 菜单校验必须纳入 `parent_id/menu_type`，避免历史菜单同签名但挂错父级时被静默 re-parent。
  - 固定 `upstream_system_management_seed.sql` 写入 `2031/2300-2309` 前，必须先确认父菜单 `2030` 已由 `top_menu_seed.sql` 提供；其 `tmp_upstream_system_management_sys_menu_guard` 的同 ID 菜单校验必须纳入 `parent_id/menu_type`。
  - 固定 `20260605_source_product_library_menu_component.sql`、`20260605_order_after_sale_menu_seed.sql`、`20260606_source_warehouse_stock_menu_rename.sql` 在写 `sys_menu` 前，除 slot/signature guard 外，还必须要求预览确认的 exact target count/signature；目标签名覆盖 `menu_id/menu_name/parent_id/order_num/path/component/query/route_name/is_frame/is_cache/menu_type/visible/status/perms/icon/remark`。
  - 固定 seller/buyer portal 运行时权限只做精确匹配，不继承若依后台 `*:*:*` 全权限语义。
  - 固定 seller/buyer 管理端主体、账号、角色、部门、菜单 Controller 必须使用精确 `seller:admin:*` / `buyer:admin:*` 方法级权限；主体 `list/add/edit/changeStatus`、账号级人工临时密码 `resetPwd`、role/dept/menu 的列表、详情、树、增删改、状态变更、角色菜单树等入口必须由 `SellerAdminPermissionContractTest` / `BuyerAdminPermissionContractTest` 锁定，并禁止恢复主体级 `resetOwnerPwd` 和账号级 `resetDefaultPwd` 默认密码重置入口，不允许只靠路径前缀或菜单 seed 兜底。
  - 固定 seller/buyer portal `accounts` / `depts` / `roles` 自助列表入口必须分别要求 `seller:account:list` / `seller:dept:list` / `seller:role:list` 和 `buyer:account:list` / `buyer:dept:list` / `buyer:role:list`；这些入口仍必须从当前端 token 推导主体，不接受前端传入主体或账号范围参数。
  - 固定远程菜单缓存 key 使用 `admin_remote_menu:admin|seller|buyer` scoped key，不再使用全局 `admin_remote_menu`。
- 复用规则：
  - 新增或改造 `sys_menu` seed 时，先声明期望的 `menu_id/path/component/route_name/perms` guard，再执行 `insert/update/on duplicate key update`。
  - 已接管业务菜单的增量 seed 如果会 `update/insert sys_menu`，还要在首个写入前计算当前目标行 count/signature，并通过 `@...expected_count` / `@...expected_signature` fail-closed；slot guard 只证明菜单槽位可写，不能替代预览确认的精确目标。
  - 如果 seed 是把旧占位菜单升级为正式页，guard 必须同时列出“允许迁移的旧签名”和“最终签名”；不要把正常升级路径误判为冲突。
  - 如果菜单名称迁移和组件迁移历史上是分步执行，guard 优先锁 `path/component/route_name/perms/menu_type` 这类菜单技术签名；没有明确必要时不要把 `menu_name` / `remark` 放入签名白名单。
  - 同一个 `menu_id` 的业务所有权默认必须只有一处脚本负责。后续如果币种、商品分类/属性、来源仓库库存、商城商品列表、商城商品按钮或售后管理菜单继续调整，只改各自专用 seed 和对应合同测试，不要放回 `business_menu_seed.sql` 或相邻迁移脚本。
  - 兼容增量 seed 可以通过 `parent_id` 引用页面菜单，或在明确 allowlist 内同签名补齐历史菜单；但不能再次 upsert 已由专用 seed 接管的页面最终签名，也不能用按钮 seed 改写父级页面菜单。依赖 `2010` 的增量 seed 必须先 fail-closed 校验 top seed 根菜单，不得自行补根节点。
  - seller/buyer portal 端内权限不得使用 `Constants.ALL_PERMISSION` 或 `*:*:*` 放行；管理端后台权限继续走若依原有 `PermissionService`。
  - 前端远程菜单缓存、登出清理和后续三端菜单域拆分，优先复用 `getRemoteMenuStorageKey(...)` 和 `REMOTE_MENU_SCOPES`，不要散写 storage key。

## SQL Guard、Legacy 回填目标白名单与管理端按钮授权白名单

- 位置：
  - `RuoYi-Vue/sql/business_menu_seed.sql`
  - `RuoYi-Vue/sql/currency_configuration_seed.sql`
  - `RuoYi-Vue/sql/upstream_system_management_seed.sql`
  - `RuoYi-Vue/sql/warehouse_us_address_seed.sql`
  - `RuoYi-Vue/sql/20260604_three_terminal_legacy_sys_user_account_backfill.sql`
  - `RuoYi-Vue/sql/20260606_admin_partner_role_menu_grant.sql`
  - `RuoYi-Vue/sql/20260606_admin_partner_non_admin_button_grant_cleanup.sql`
  - `RuoYi-Vue/sql/20260606_legacy_disable_sys_seller_buyer_roles.sql`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminDirectLoginPermissionContractTest.java`
  - `docs/plans/2026-06-07-three-terminal-p0p1-sql-guard-whitelist-record.md`
- 当前用途：
  - 固定高影响 SQL seed 即使不是 `202606*.sql` 命名，也必须显式 `@confirm_*`、`APPLY_*` token 和确认 procedure。
  - 固定 legacy `sys_user` 端账号回填必须先由人工预览出 seller/buyer account ID 白名单和 expected_count，再限定 DML 范围。
  - 固定 legacy `sys_role` seller/buyer cleanup 必须先由人工预览 role ID 白名单和 expected_count。
  - 固定管理端 partner 菜单按钮授权只使用明确 `menu_id` + `perms` 白名单，不使用 `seller:admin:%` / `buyer:admin:%` 通配符。
  - 固定管理端 partner 子按钮授权和非 admin cleanup 必须要求 `child.perms` 的端前缀与 `page_menu.perms` 的端前缀一致，避免历史错误挂载的 seller/buyer admin 按钮被跨端授权或跨端清理。
- 复用规则：
  - 新增或改动业务 seed 只要包含 DDL/DML，就优先复用 `set @confirm_*`、`assert_*_confirmed()`、`signal sqlstate '45000'` 和“确认调用在第一个高影响语句前”的模式。
  - SQL helper 如果执行历史清理、批量回填、批量授权或批量删除，除确认 token 外还必须有人工预览后的 ID 白名单和 expected_count；不要只靠类型、前缀或状态条件定位目标。
  - 管理端给 seller/buyer 管理页面补按钮权限时，只能新增明确按钮 ID 与权限标识，并同步更新 `AdminDirectLoginPermissionContractTest` 的白名单哨兵。
  - 不允许把 seller/buyer 端内权限、菜单、角色或账号通过 `sys_*` 进行端内控制；这些 SQL helper 只服务管理端控制面和 legacy 迁移/清理。
  - `SqlExecutionGuardContractTest` 的高影响语句识别应覆盖 insert、insert ignore、replace、update、delete alias/from、alter table、create/drop view、create/drop/truncate/rename table 和 create index 变体。

## 三端认证入口审计、免密 acting admin 链路与 Guard 覆盖规则

- 位置：
  - `RuoYi-Vue/ruoyi-common/src/main/java/com/ruoyi/common/annotation/PortalLog.java`
  - `RuoYi-Vue/ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/PortalLogAspect.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalAuthController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalAuthController.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalDirectLoginToken.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalLoginLog.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalLoginSession.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml`
  - `RuoYi-Vue/sql/20260607_terminal_login_log_direct_login_audit.sql`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalAnonymousEndpointContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalDirectLoginAuthContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalSqlIsolationContractTest.java`
  - `react-ui/scripts/check-portal-token-isolation.mjs`
  - `react-ui/scripts/check-partner-management-template.mjs`
  - `react-ui/scripts/check-seller-portal-product-template.mjs`
  - `react-ui/scripts/check-buyer-portal-product-template.mjs`
  - `docs/plans/2026-06-06-three-terminal-p0p1-auth-audit-guard-hardening-record.md`
- 当前用途：
  - 固定 seller/buyer 登录、免密登录这类认证入口的匿名例外和审计边界。
  - 固定管理端免密代入后 acting admin 从 ticket payload 进入 portal session、端内登录日志结构化字段，并进入后续端内操作日志审计前缀。
  - 固定 React portal、partner 管理和 seller/buyer 商品模板 guard 同时覆盖 TypeScript 源与当前并存的 JavaScript 副本。
  - 固定三端身份相关高影响 SQL helper 必须在第一个高影响 DDL/DML 前完成显式确认。
- 复用规则：
  - seller/buyer 登录和免密登录接口不得加 `@PortalPreAuthorize`；必须使用 `@PortalLog(... allowAnonymous = true, isSaveResponseData = false)`，并显式写入正确 terminal。
  - 普通端内业务接口不得设置 `allowAnonymous = true`；缺少 portal session 时继续 fail-closed。
  - 免密 ticket payload 必须携带 `actingAdminId`、`actingAdminName` 和 `reason`；`PortalTokenSupport.createLogin(..., PortalDirectLoginToken)` 必须把这些字段复制到 `PortalLoginSession`。
  - `PortalTokenSupport.buildLoginLog(...)` 必须对普通登录日志显式写 `directLogin=false`，避免 `seller_login_log.direct_login` / `buyer_login_log.direct_login` 的 `not null default 0` 在 MyBatis 显式插入 null 时失效。
  - 免密登录成功和已解析 ticket 后的业务失败日志，必须通过 `buildDirectLoginLog(...)` 写入 `directLogin`、`directLoginTicketId`、`actingAdminId`、`actingAdminName`、`directLoginReason`；不要只把这些信息拼进 `msg` 文本。
  - `seller_login_log` / `buyer_login_log` 必须保留上述 direct-login 审计字段，当前由 `20260604_three_terminal_isolation_migration.sql`、`seller_buyer_management_seed.sql` 和 `20260607_terminal_login_log_direct_login_audit.sql` 共同覆盖，且新增补丁必须纳入 SQL guard。
  - 端内操作日志应复用 `PortalLogAspect` / `PortalPreAuthorizeAspect` 写入 `PortalOperLog` 的结构化 direct-login 字段：`directLogin`、`directLoginTicketId`、`actingAdminId`、`actingAdminName`、`directLoginReason`；`directLoginAudit{...}` 的 `oper_param` 前缀只保留兼容/回放用途，不得作为唯一审计来源。
  - 如果需要继续扩展 acting admin 到新的 session 设备字段、强退执行人或原因字段，必须先走单独 DDL 方案确认；当前快速模式只保证 Redis session、端内登录日志和操作日志链路。
  - `PortalLoginResultData` 等端内登录响应必须保持白名单字段，不得回流 `sellerId`、`buyerId`、`sellerAccountId`、`buyerAccountId`、`subjectId` 或 `accountId`。
  - partner 管理读模型不得暴露 `password`、`token`、`refreshToken`、`directLoginToken`、`loginUrl`、`tokenHash`、`authorization`、`accessToken`。
  - seller/buyer portal 商品和认证相关 guard 必须检查 `.ts/.tsx` 与 `.js`；只有明确 `export { default } from './index.tsx';` 这类纯 re-export 才可视为跟随 TypeScript 源。
  - 新增影响三端身份、权限、日志、会话、legacy `sys_user` / `sys_role` 清理的 SQL helper 时，必须纳入 `SqlExecutionGuardContractTest` 或相邻 SQL 合同测试，且确认调用必须位于执行区第一个高影响 DDL/DML 前。

## 三端免密登录与 SQL Guard 收口规则

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalDirectLoginAuthContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - `react-ui/src/utils/portalDirectLoginMessage.ts`
  - `react-ui/src/utils/portalDirectLoginMessage.js`
  - `react-ui/scripts/check-portal-token-isolation.mjs`
  - `react-ui/scripts/verify-three-terminal.mjs`
  - `react-ui/tests/portal-direct-login-message.test.ts`
  - `docs/plans/2026-06-06-three-terminal-p0p1-direct-login-guard-followup-record.md`
- 当前用途：
  - 固定管理端免密代入 seller/buyer 端时的 token 生成、投递、消费和审计边界。
  - 固定三端身份相关远程 DDL/DML 脚本必须 fail-closed，需要显式 `@confirm_*` 和确认 token。
- 复用规则：
  - `PortalDirectLoginSupport.consumeToken(...)` 必须使用带 validator 的重载；不得新增无 validator 的消费入口。
  - 创建免密票据前必须解析并校验 acting admin；缺失时不得写 `portal_direct_login_ticket`，不得写 Redis。
  - 免密登录前端桥接只能在目标 popup、目标 origin、目标 terminal 的 READY 消息通过后发送 token；不得通过 timer 在 READY 前主动发送。
  - 管理端免密代入成功提示必须等待目标 portal 端完成 `/direct-login` 消费并回传 `PORTAL_DIRECT_LOGIN_RESULT_MESSAGE` success；READY 只表示目标窗口可接收 token，不能作为消费成功 ack。
  - `Portal/DirectLogin` 消费成功后才能回传 RESULT success；消费失败、terminal mismatch 或持久化失败只能回传 RESULT error 或保持失败状态，不得让管理端误报成功。
  - `portalDirectLoginMessage.ts` 和当前并存的 `.js` 副本必须同步维护；`check-portal-token-isolation.mjs` 必须同时检查两者。
  - 端内权限、日志、会话、OWNER 约束等会影响三端身份或远程库的 SQL 脚本必须带显式确认 guard，并纳入 `SqlExecutionGuardContractTest` 或相邻 SQL 契约测试。
  - `verify-three-terminal` 必须包含 portal token/session 相关 Jest 单测，避免前端 bridge 回归在只跑 typecheck 和后端合约时漏过。

## 买家端商城商品浏览只读后端模板

- 位置：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalProductDistributionController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/IBuyerPortalProductService.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/domain/BuyerPortalProduct.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/domain/BuyerPortalProductSku.java`
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImplTest.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/IProductDistributionService.java`
  - `RuoYi-Vue/product/src/main/resources/mapper/product/ProductDistributionMapper.xml`
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - `react-ui/src/pages/Portal/Home/BuyerDistributionProductList.tsx`
  - `react-ui/src/pages/Portal/Home/index.tsx`
  - `react-ui/src/services/portal/session.ts`
  - `react-ui/src/types/seller-buyer/party.d.ts`
  - `react-ui/scripts/check-buyer-portal-product-template.mjs`
  - `react-ui/scripts/check-portal-token-isolation.mjs`
  - `scripts/smoke/buyer-distribution-product-read-template-smoke.ps1`
  - `scripts/smoke/buyer-portal-product-ui-smoke.ps1`
  - `scripts/smoke/buyer-portal-product-ui-smoke.mjs`
  - `docs/plans/2026-06-05-buyer-distribution-product-read-template-record.md`
  - `docs/plans/2026-06-05-buyer-product-permission-dml-smoke-record.md`
  - `docs/plans/2026-06-05-buyer-portal-product-ui-template-record.md`
  - `docs/plans/2026-06-05-buyer-portal-product-protable-copy-record.md`
- 当前用途：
  - 作为 buyer 端商城商品浏览的第一套后端只读模板。
  - 作为 buyer portal 工作台商城商品卡片的标准 ProTable 前端模板。
  - buyer 端只读浏览平台已上架商品，不承载商品归属、下单、购物车、库存承诺或客户专属价格。
  - product 共享查询提供 `ON_SALE` SPU / `ON_SALE` SKU 的只读口径，buyer facade 负责端入口、端鉴权、DTO 脱敏和业务筛选。
  - buyer 真实运行库权限 DML 与 HTTP smoke 已形成可复跑验收模板。
  - buyer 前端契约守卫和浏览器 smoke 已形成可复跑验收模板。
- 复用规则：
  - buyer 商品浏览只展示 `ON_SALE` SPU 和 `ON_SALE` SKU；列表价格、币种和 SKU 数量必须基于 `ON_SALE` SKU。
  - buyer 请求不能传 `buyerId`、`subjectId`、`accountId`、`terminal` 决定数据范围，也不能使用 `sellerId`、系统编码、seller 客户编码、sourceType 或 spuStatus 作为 buyer 浏览筛选。
  - buyer DTO 不暴露 `sellerId`、`sellerNo`、`sellerName`、`sellerSpuCode`、`sellerSkuCode`、`systemSpuCode`、`systemSkuCode`、供货价、后台审计字段、token 或 Redis key。
  - product 管理端商品查询继续保持原行为；不要为了 buyer 浏览直接改管理端 `selectProductList(...)` 的含义。
  - buyer 端权限写入 `buyer_menu` / `buyer_role_menu`，不写入若依 `sys_menu` / `sys_role`。
  - 真实运行库补权限和 HTTP smoke 已单独成切片；后续 buyer 前端复制时不得重复执行 DML，除非权限 seed 或运行库权限发生新变更。
  - HTTP smoke 必须覆盖无 token 拒绝、buyer 登录、`getInfo` 权限集合、列表、伪造范围参数不生效、详情、SKU、固定不存在商品拒绝和 logout 后旧 token 失效。
  - buyer 前端工作台复制已按 seller portal 商品卡片模板替换 terminal、service、路由、DTO 和断言文本，不重新设计页面结构。
  - buyer portal 商品主列表必须使用标准 `ProTable`、`getPersistedProTableSearch(...)`、`getProTablePagination(...)` 和 `getProTableScroll(...)`，并固定 `current -> pageNum`、`pageSize -> pageSize` 的若依分页映射。
  - buyer 前端卡片标题使用“商城商品”，不使用 seller 的“我的商城商品”；buyer 页面不展示客户 SPU/SKU、seller 内部编码、系统编码、供货价或后台审计字段。
  - buyer 商品浏览筛选只暴露 buyer 可用口径；当前前端只保留关键词和分页，不提供 `spuStatus`、seller 客户编码、系统编码、供货价或 sourceType 这类后台/卖家字段筛选。
  - buyer 商品前端模板改动后至少运行 `npm run guard:buyer-portal-product`、`npm run guard:portal-token`、`npm run tsc -- --pretty false` 和 `scripts/smoke/buyer-portal-product-ui-smoke.ps1`。
  - buyer 商品 service 的列表、详情和 SKU 入口都必须先校验 `PortalLoginSession.terminal == "buyer"`、`subjectId` 和 `accountId`；session 不匹配时必须先返回带 `HttpStatus.UNAUTHORIZED` 的“登录状态已失效”，且不得调用共享 product service 查询商品。
  - `BuyerPortalProductServiceImplTest` 已固定非 buyer session 的 fail-closed 契约：列表、详情和 SKU 均不得查询 product service；后续新增 buyer 商品只读入口时先补同类 service 层负向测试，再接 Controller。

## 买家商品浏览复制前边界

- 位置：
  - `docs/plans/2026-06-05-buyer-product-browse-copy-boundary-plan.md`
  - `docs/plans/2026-06-05-seller-portal-product-template-acceptance-record.md`
- 当前用途：
  - 作为 seller 商品模板验收后复制 buyer 商品浏览前的业务边界。
  - 固定 buyer 商品浏览不能机械复制 seller 商品拥有关系。
  - 固定首版 buyer 浏览口径为平台已上架 SPU/SKU 只读浏览。
- 复用规则：
  - 后续复制 buyer 时只复用 seller 的端入口结构、DTO 收敛方式、guard 和 smoke 验收方法，不复用 seller 的 `sellerId = 当前 subjectId` 商品归属谓词。
  - buyer 首版列表只浏览 `ON_SALE` SPU 和 `ON_SALE` SKU；列表价格聚合必须基于 `ON_SALE` SKU，不能把草稿、待上架或已下架 SKU 的价格带进 buyer 列表。
  - buyer 端商品 DTO 不直接复用管理端 `ProductSpu` / `ProductSku`，也不复用 seller DTO。
  - buyer 首版只展示销售价和币种；供货价、seller 内部编码、系统 SPU/SKU、后台审计字段、token、Redis key 均不得进入 buyer 响应或页面。
  - buyer 端商品权限写入 `buyer_menu` / `buyer_role_menu`；不得写回若依 `sys_menu` / `sys_role`。
  - buyer 商品路径和权限命名暂定沿用 seller 模板的 `distribution-products` / `buyer:product:distribution:*`；如果改为 `browse-products` / `buyer:product:browse:*`，必须在 buyer 后端切片开始前一次选定。
  - buyer 商品浏览已按后端只读模板、权限 DML 与 HTTP smoke、前端工作台复制、ProTable 差量复制、浏览器 smoke 与模板验收分切片推进完成；后续只按维护/演进口径更新。
  - portal 401 清理 admin session 的前端隔离问题已作为独立切片完成，不再作为 buyer 商品浏览复制阻塞项。

## 卖家端商品模板验收基线

- 位置：
  - `docs/plans/2026-06-05-seller-portal-product-template-acceptance-record.md`
  - `scripts/smoke/seller-own-distribution-product-read-template-smoke.ps1`
  - `scripts/smoke/seller-portal-product-ui-smoke.ps1`
  - `react-ui/scripts/check-seller-portal-product-template.mjs`
  - `react-ui/scripts/check-portal-token-isolation.mjs`
- 当前用途：
  - 作为 seller portal “我的商城商品”模板是否可以进入复制评估的验收基线。
  - 同时覆盖后端 service 范围控制、端权限 seed 契约、前端模板契约、portal token/query 隔离、真实 HTTP 链路和真实浏览器链路。
- 复用规则：
  - buyer 商品浏览业务口径已确认并完成 ProTable 差量复制；后续 seller 商品模板新增结构变化时，仍先验收 seller，再按 buyer 浏览口径替换 terminal、路由、service、权限、DTO、前端断言和 smoke 脚本。
  - 不允许把 seller 商品拥有关系机械替换成 buyer；buyer 可见性、上架状态、价格口径和库存可见边界需要单独确认。
  - seller 商品模板改动后，至少重新运行 seller service 单测、seller 模块测试、前端两个 guard、`npm run tsc`、后端 HTTP smoke 和前端浏览器 smoke。

## 卖家端商品前端浏览器烟测脚本

- 位置：
  - `scripts/smoke/seller-portal-product-ui-smoke.ps1`
  - `scripts/smoke/seller-portal-product-ui-smoke.mjs`
- 当前用途：
  - 可复跑验收 seller portal “我的商城商品”前端模板。
  - 覆盖管理端生成 seller 免密票据、seller direct-login、seller portal 加载、token storage 隔离、商品列表、详情弹窗、可见字段脱敏和退出清理。
  - 默认使用本机 Chrome/Edge 通道，不往 `react-ui/package.json` 增加 Playwright 依赖。
- 复用规则：
  - seller 商品模板验收时先运行后端 HTTP smoke，再运行该浏览器 smoke。
  - 脚本不得输出 `admin token`、`seller token`、`directLoginToken`、免密 URL、Redis key 或 `.env.local` 内容。
  - 验证码开启时脚本只能失败并提示，不得自行修改验证码开关。
  - buyer 版本已按独立买家浏览口径另行完成；后续 seller 模板新增结构性变化时，先验收 seller，再按 buyer 业务口径复制差量。

## 卖家端商品前端模板契约守卫

- 位置：
  - `react-ui/scripts/check-seller-portal-product-template.mjs`
  - `react-ui/src/pages/Portal/Home/SellerOwnDistributionProductList.tsx`
  - `react-ui/src/pages/Portal/Home/index.tsx`
  - `react-ui/src/services/portal/session.ts`
  - `react-ui/package.json`
- 当前用途：
  - 固定 seller portal “我的商城商品”前端模板契约。
  - 防止 seller 商品卡片被放入 buyer 分支。
  - 防止 seller portal 商品页面直接调用 `request(...)`、导入管理端 product service、硬编码 API 路径或复用管理端 `API.ProductDistribution` 类型。
  - 防止 seller 商品列表 service 透传客户端身份范围参数。
  - 固定 seller portal 商品列表必须使用标准 ProTable、统一筛选展开持久化、统一分页和统一滚动配置。
- 复用规则：
  - buyer 商品浏览 ProTable 差量已按 seller 模板复制完成；后续 seller 模板新增结构变化时，仍先确认 buyer 浏览可见性、上架状态、价格口径和库存可见边界，再复制差量。
  - seller portal 商品页面必须通过 `@/services/portal/session` 调用端内 service，不要在页面中手写 Authorization、API 路径或管理端 service。
  - seller portal 商品响应类型必须使用 `API.Partner.SellerPortalProduct` / `API.Partner.SellerPortalProductSku`，不要把管理端商品 DTO 当作端内 API 标准。
  - seller portal 商品主列表必须使用 `ProTable`、`getPersistedProTableSearch(...)`、`getProTablePagination(...)` 和 `getProTableScroll(...)`；详情弹窗内 SKU 小明细表可以保留 Ant Design `Table`。
  - 修改 seller portal 商品模板后必须运行 `npm run guard:seller-portal-product` 和 `npm run tsc -- --pretty false`。

## 若依字典

### 主体类型

- 字典类型：`subject_type`
- 初始化位置：`RuoYi-Vue/sql/seller_buyer_management_seed.sql`
- 当前选项：
  - `COMPANY`：公司
  - `PERSON`：个人
  - `OTHER`：其他
- 当前用途：
  - 管理端卖家管理新增/编辑弹窗
  - 管理端买家管理新增/编辑弹窗
- 复用规则：
  - 后续主体、账号、导入模板或筛选条件需要主体类型时，优先读取该字典。
  - 不要在 React 页面、Java Service 或导入逻辑中另写一套主体类型选项。

### 卖家/买家等级

- 字典类型：
  - `seller_level`
  - `buyer_level`
- 初始化位置：`RuoYi-Vue/sql/seller_buyer_management_seed.sql`
- 当前选项：`L1`、`L2`、`L3`
- 当前用途：
  - 管理端卖家管理等级字段
  - 管理端买家管理等级字段
- 复用规则：
  - 卖家等级只读 `seller_level`，买家等级只读 `buyer_level`。
  - 不要把卖家等级和买家等级重新合并回一个客户等级字段。

### 卖家/买家账号角色

- 字典类型：
  - `seller_account_role`
  - `buyer_account_role`
- 初始化位置：`RuoYi-Vue/sql/seller_buyer_management_seed.sql`
- 当前选项：
  - `OWNER`：负责人
  - `ADMIN`：管理员
  - `STAFF`：普通账号
- 当前用途：
  - 管理端卖家账号新增弹窗
  - 管理端买家账号新增弹窗
- 复用规则：
  - 账号绑定表保存 code，不保存中文 label。
  - 卖家端账号密码保存在 `seller_account.password`，买家端账号密码保存在 `buyer_account.password`，均为 BCrypt 密文。
  - 不再把卖家/买家端账号写入 `sys_user`，也不再通过 `sys_role` 分配端内账号角色。

### 国家/地区

- 字典类型：`country_region`
- 初始化位置：`RuoYi-Vue/sql/seller_buyer_management_seed.sql`
- 保存规则：业务表保存 2 位国家/地区代码，例如 `CN`、`US`。
- 当前用途：
  - 管理端卖家管理新增/编辑弹窗的可搜索国家/地区下拉
  - 管理端买家管理新增/编辑弹窗的可搜索国家/地区下拉
- 复用规则：
  - 后续仓库、地址、渠道、导入模板需要国家/地区选项时，优先读取该字典。
  - 页面展示 label，接口和数据库保存 code。
  - 需要新增国家/地区时维护 `sys_dict_data`，不要在页面内硬编码大段国家列表。

### 币种全集

- 字典类型：`currency_code`
- 初始化位置：`RuoYi-Vue/sql/currency_configuration_seed.sql`
- 当前选项：`USD`、`CNY`、`EUR`、`GBP`、`CAD`、`AUD`、`JPY`、`HKD`、`MXN`、`BRL`
- 当前用途：
  - 管理端币种配置新增/编辑弹窗的币种代码选择。
- 复用规则：
  - `currency_code` 只代表系统认识的币种全集，不代表业务可用币种。
  - 后续业务页面的币种下拉必须读取 `finance_currency` 启用币种 options，不直接读取该字典。
  - 数据库存储和 API 传输保存 code，不保存展示 label。

## 后端内部复用

### product 商品配置模块

- 位置：
  - `RuoYi-Vue/product`
  - `RuoYi-Vue/sql/20260604_product_category_attribute_seed.sql`
  - `RuoYi-Vue/sql/20260605_product_config_change_log.sql`
  - `react-ui/src/pages/Product/Category/index.tsx`
  - `react-ui/src/pages/Product/Attribute/index.tsx`
  - `react-ui/src/pages/Product/components/ProductImportModal.tsx`
  - `react-ui/src/pages/Product/components/ProductConfigChangeLogDrawer.tsx`
  - `react-ui/src/services/product/product.ts`
  - `react-ui/src/types/product/product.d.ts`
- 当前用途：
  - 管理端维护多级商品分类树。
  - 管理端维护平台级商品属性库和属性自定义选项。
  - 管理端维护类目属性配置，并预览按祖先链继承合并后的发布 schema。
  - 管理端导入商品分类、商品属性和商品属性选项。
  - 管理端按具体分类、属性、属性选项和类目属性规则查看业务级修改记录。
- 复用规则：
  - `product` 是商品共享基础域，不是 admin / seller / buyer 之外的第四个端。
  - 管理端配置入口使用 `/product/admin/**`，权限走若依 `sys_menu` / `sys_role`。
  - 卖家端商品分类与 schema 入口已迁入 `seller` 端模块 facade，只读委托 product schema 服务，不维护平台配置。
  - 买家端商品分类与 schema 入口已迁入 `buyer` 端模块 facade，只读委托 product schema 服务，不维护平台配置。
  - 后续买家端分类浏览、筛选项和商品详情入口必须继续放在 `buyer` 端模块，只读消费 product 分类和筛选 schema。
  - 商品属性类型、选项来源、类目属性规则模式等 code 由 SQL 初始化字典或 `pages/Product/constants.ts` 集中维护，不要在页面里复制状态映射；选择型属性判断统一复用 `isOptionAttributeType(...)`。
  - 类目属性规则统一通过 `IProductConfigService.previewCategorySchema(categoryId)` 计算，不要在前端或其他模块手写继承合并逻辑。
  - 商品配置导入统一使用 `ProductConfigImportService` 和 `ProductImportModal` 的“下载模板、校验、确认导入”流程，不要在分类页、属性页分别复制上传解析和结果表格。
  - 商品配置导入模板统一使用 `ProductImportTemplateService` 输出“正式导入 sheet + 填写示例 sheet + 字段说明 sheet”；第一个 sheet 保持空白，示例只供复制参考，避免误导入。
  - 商品配置修改记录统一使用 `ProductConfigChangeLogService`、`ProductConfigChangeLogMapper` 和 `ProductConfigChangeLogDrawer`；页面只传 `bizType` 和 `bizId`，不要在各页面重复实现 JSON diff 展示。
  - `sys_oper_log` 继续作为若依系统级接口审计；`product_config_change_log` 只记录商品配置业务对象的字段级变更。
  - 导入模板只使用业务 code 定位，不让用户填写数据库主键；分类父级使用父级分类编码，属性选项使用属性编码。
  - 导入只支持新增和更新，不支持导入删除；删除仍必须走页面操作和后端业务校验。
  - 商品分类、属性库和类目属性模板仍只负责配置，不承载正式商城商品、SKU、库存、价格、商品审核和外部平台属性同步。

### product 商城商品 SPU/SKU 模块

- 位置：
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/controller/AdminProductDistributionController.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/IProductDistributionService.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java`
  - `RuoYi-Vue/product/src/main/resources/mapper/product/ProductDistributionMapper.xml`
  - `RuoYi-Vue/sql/20260605_mall_product_distribution_seed.sql`
  - `react-ui/src/pages/Product/Distribution/index.tsx`
  - `react-ui/src/pages/Product/Distribution/EditPage.tsx`
  - `react-ui/src/pages/Product/Distribution/components/ProductImageSection.tsx`
  - `react-ui/src/pages/Product/Distribution/components/ImageUploadField.tsx`
  - `react-ui/src/pages/Product/Distribution/components/SkuMatrixEditor.tsx`
  - `react-ui/src/pages/Product/Distribution/components/ProductDetailDrawer.tsx`
  - `react-ui/src/pages/Product/Distribution/components/DetailContentBuilder.tsx`
  - `react-ui/src/pages/Product/Distribution/components/DetailContentPreview.tsx`
  - `react-ui/src/pages/Product/Distribution/detailContent.ts`
  - `react-ui/src/services/product/distributionProduct.ts`
  - `react-ui/src/types/product/distribution-product.d.ts`
- 当前用途：
  - 管理端“商品管理 / 商城商品列表”正式商品主数据。
  - 支持管理端手工创建 SPU，一个 SPU 下维护多个 SKU。
  - SPU 绑定卖家、末级可发布类目、商品中文标题、商品英文标题、主图、轮播图、模块化详情内容和类目属性值。
  - SKU 维护系统 SKU、客户 SKU、固定规格、SKU 图、供货价、销售价、币种和 SKU 状态；新增/编辑页隐藏系统 SKU，查看详情保留系统 SKU。
  - SPU / SKU 共用 `DRAFT`、`READY`、`ON_SALE`、`OFF_SALE`、`DISABLED` 销售状态。
- 复用规则：
  - 商城商品正式事实源使用 `product_spu` / `product_sku`，来源商品库仍只是上游同步快照展示，不要把上游快照直接当正式商品。
  - 商品创建来源首版只落 `ADMIN_MANUAL`，后续卖家提交或来源商品库生成时只扩展来源字段和审核链路，不复制一套商品主表。
  - `product_spu.product_name` 表示商品中文标题，`product_spu.product_name_en` 表示商品英文标题；二者在管理端保存时均必填。
  - 系统 SPU / 系统 SKU 由后端生成；新增页不得作为输入项展示系统 SPU，编辑页只读展示系统 SPU；新增/编辑 SKU 表内不展示系统 SKU，列表/详情查看场景可以只读展示系统 SKU。
  - 卖家快照通过 `ProductSellerLookupService` 读取 seller 模块，不在 product 模块直接读 seller Mapper。
  - 商品类目和类目属性 schema 继续复用 `IProductConfigService.previewCategorySchema(categoryId)`，前端不要自己合并祖先属性模板。
  - 商城商品新增/编辑页渲染类目属性时按属性类型统一处理：`TEXT` 用输入框，`NUMBER` 用数字输入并显示 `unit`，`BOOLEAN` 固定用 `是 / 否` 选项，`SINGLE_SELECT` / `MULTI_SELECT` 使用 schema options，`DATE` 用日期选择器；多选保存为 JSON 数组、编辑回显时必须解析回数组。
  - 商品分类 TreeSelect 搜索复用 `SEARCHABLE_TREE_SELECT_PROPS`，只按当前节点标题匹配，避免搜索一个词时把整棵无关子树带出来。
  - 业务可用币种最终仍由 `finance_currency` 和 `IFinanceCurrencyService` 做可用性校验；新增/编辑页不允许自由选择 SKU 币种，当前先通过“发货仓库（预留）”同国家选择推导币种，仓库模块落地后替换临时仓库选项。
  - 图片上传复用 `/common/upload` 和 `FileStorageService`，商品表只保存 `/profile/...` 等资源路径。
  - SPU 图片固定为最多 8 个槽位：第 1 张为必填主图，第 2 张仅作为尺寸图提示，其余槽位空白占位；不要把轮播图做成动态追加后才出现的交互。
  - SPU 详情内容统一通过 `detailContent.ts` 序列化为 `product_spu.detail_content` JSON，当前支持文本段落、图片模块、图文模块和参数表模块；新增/编辑页和详情预览必须复用同一套解析逻辑。
  - 详情模块里的图片 URL 随 `detail_content` JSON 保存；后续若需要详情图独立审核、排序或素材复用，再单独设计详情图片存储，不要临时拆出并行逻辑。
  - SKU 录入优先使用“固定规格字段选择 -> 规格值 -> 生成 SKU 矩阵 -> 表格补充价格/图片/状态”的页面模式；固定规格字段仍落 `product_sku` 固定字段，不另建规格模板表。
  - SKU 三边尺寸和重量属于 SKU 级物流/包装属性，固定展示在 SKU 表格中；`length_value`、`width_value`、`height_value`、`weight` 均为可填写单位的字符串，不进入“规格属性”勾选区，也不参与 SKU 矩阵组合生成。
  - SKU 尺寸重量在列表和详情中按紧凑格式展示，例如 `42.00 x 42.00 x 17.00 cm   920 g`；不要展示成 `长 42cm / 宽 42cm / 高 17cm / 重 920g` 这类标签串。
  - SKU 表批量供货价、销售价输入框保持足够宽度，避免为了压缩表格把价格录入框做成不可用的小块。
  - 发货仓库当前只做 SPU 维度 UI 预留和币种推导，不保存仓库绑定；未选仓库时 SKU 币种列只显示 `-`，不要在表格内反复提示。
  - 库存首版只保留读取边界；后续库存数量、仓库拆分和库存流水必须进入 `inventory` 模块，不写入 `product_sku`。
  - SPU 上架是商品展示边界，SKU 上架是具体可售边界；SPU `ON_SALE` 下新增 SKU 不允许自动进入 `ON_SALE`。
  - 管理端权限使用 `product:distribution:*` 若依权限点；seller/buyer 端商品发布和浏览后续必须放各自端模块，不把 product 当第四个端。

### 端内商品分类与 Schema 只读模板

- 位置：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductSchemaController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalProductSchemaController.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/domain/PortalProductCategory.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/domain/PortalProductCategorySchemaItem.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/domain/PortalProductAttributeOption.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/IProductPortalSchemaService.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductPortalSchemaServiceImpl.java`
  - `RuoYi-Vue/sql/20260604_portal_product_category_permission_seed.sql`
  - `RuoYi-Vue/sql/20260604_seller_product_schema_permission_seed.sql`
  - `RuoYi-Vue/sql/20260604_buyer_product_schema_permission_seed.sql`
  - `react-ui/src/pages/Portal/Home/SellerProductSchemaPreview.tsx`
  - `react-ui/src/pages/Portal/Home/BuyerProductSchemaPreview.tsx`
  - `react-ui/src/services/portal/session.ts`
  - `react-ui/src/types/seller-buyer/party.d.ts`
- 当前用途：
  - 作为 seller/buyer 端真实业务接口的第一套标准模板。
  - 卖家端、买家端只读获取启用且可发布的商品分类，用于选择 `categoryId`。
  - 卖家端只读获取商品发布所需的类目属性 schema。
  - 买家端只读获取商品浏览、筛选或商品详情所需的类目属性 schema。
  - seller 商品分类和 schema 端入口由 `SellerPortalProductSchemaController` 承载；buyer 商品分类和 schema 端入口由 `BuyerPortalProductSchemaController` 承载。
  - 当前 `react-ui` 的 `/seller/portal` 已接入“商品发布准备”只读卡片，`/buyer/portal` 已接入“商品浏览准备”只读卡片，分别真实消费对应端商品分类和 schema 接口。
  - 只消费 `product` 模块配置，不维护平台商品分类、属性库或类目属性规则。
- 复用规则：
  - 已按“先验收卖家模板，再复制买家”的节奏落地；后续同构端内商品接口继续按模板化推进。
  - 商品分类列表统一复用 `IProductConfigService.selectCategoryList(...)`，后端强制 `status=0` 和 `publishEnabled=Y`，不要让前端筛掉不可发布分类。
  - schema 计算统一收敛在 `IProductPortalSchemaService`，内部复用 `IProductConfigService.previewCategorySchema(categoryId)`；seller/buyer facade 只做端入口、端鉴权、日志和委托，不复制继承合并逻辑。
  - 端内接口必须使用 `@PortalPreAuthorize` 校验 terminal 和端内权限；分类列表权限为 `seller:product:category:list` / `buyer:product:category:list`，schema 权限为 `seller:product:schema:query` / `buyer:product:schema:query`。
  - 端内接口需要匿名放行给若依外层登录过滤，再由 `@PortalPreAuthorize` 做端 token 鉴权；`@Anonymous` 应下沉到明确的方法级入口，不做类级匿名。
  - 端内接口必须从 `PortalSessionContext.requireSession(...)` 获取当前端身份；不得信任前端传入的 `sellerId`、`buyerId`、`accountId`、`subjectId` 或 `terminal`。
  - 端内 schema 只返回启用类目、可发布类目、启用属性规则和可见属性规则；属性选项只返回启用选项。
  - 响应 DTO 只返回端内表单需要的字段，不返回 `createBy`、`updateBy`、`remark`、密码、token、Redis key 或后台审计字段。
  - 前端消费必须通过 `portal/session.ts` 注入对应端 token，并显式 `isToken: false`，不要复用管理端 `access_token`。
  - 前端不得传 `sellerId`、`buyerId`、`subjectId`、`accountId` 决定分类或 schema 数据范围。
  - 卖家端和买家端商品 Schema 前端消费模板均已落地；后续同构端内商品页面只替换 terminal、路径、权限点、文案和 service，不提前混合两端逻辑。
  - 前端展示属性类型、规则模式、分组、选项来源、是否标识时，优先复用 `react-ui/src/pages/Product/constants.ts`，不要在端内页面另写一套 code/label。
  - 当前权限 seed 会授予 active 端内角色；如果后续改成明确角色清单，必须同步更新 SQL 执行记录和本台账。
  - 后续维护 seller/buyer 端同构接口时，只替换 terminal、路径、权限点、日志 title、seed 表名和验证主体，不重新设计。
  - seller/buyer facade 已按各自 terminal 模块收口；后续维护只复制端入口配置，不复制 product schema 计算。

### product 商城商品操作日志辅助模板

- 位置：
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/domain/ProductDistributionOperationLog.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/mapper/ProductDistributionOperationLogMapper.java`
  - `RuoYi-Vue/product/src/main/resources/mapper/product/ProductDistributionOperationLogMapper.xml`
  - `RuoYi-Vue/sql/20260605_product_distribution_status_price_log.sql`
  - `docs/plans/2026-06-05-product-distribution-operation-log-compile-unblock-record.md`
- 当前用途：
  - 记录管理端商城商品 SPU / SKU 状态、控制状态和 SKU 销售价变更的业务操作日志。
  - `sys_oper_log` 仍作为若依接口级审计；`product_distribution_operation_log` 只承载商城商品业务对象字段级变化。
- 复用规则：
  - 同一次批量操作必须共用同一个 `batchNo`，不要为每行商品重新生成批次号。
  - SPU / SKU 状态、控制状态和价格变更应复用现有日志对象构造与字段差异摘要，不要在 controller、mapper XML 或前端重复拼日志。
  - 日志只记录业务字段变化和操作上下文，不记录 token、Redis key、密码、`.env.local`、数据库连接串或其他敏感信息。
  - 后续补真实 HTTP / 数据库验收时，需要单独核验操作前后数据、日志行数、字段差异和操作者，不把编译通过当作业务验收完成。

### 卖家端我的商城商品只读后端模板

- 位置：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductDistributionController.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/ISellerPortalProductService.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/domain/SellerPortalProduct.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/domain/SellerPortalProductSku.java`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImplTest.java`
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - `scripts/smoke/seller-own-distribution-product-read-template-smoke.ps1`
- 当前用途：
  - 作为 seller 端真实业务接口的数据范围控制标准模板。
  - 提供卖家端自己的商城商品列表、详情和 SKU 只读接口。
  - 端入口放在 `seller` 模块，实际只读消费 `product` 共享模块的 `IProductDistributionService`；`product` 模块不承载 `/seller/**` 路由。
  - seller 模板已形成后端只读范围控制基线；buyer 已按独立买家浏览口径另行完成复制，后续新增 seller 结构性变化仍需先验收 seller，再按 buyer 口径复制差量。
- 复用规则：
  - 列表、详情和 SKU 查询的数据范围必须来自 `PortalSessionContext.requireSession("seller")` 得到的 `PortalLoginSession.subjectId`，不得信任前端传入的 `sellerId`、`subjectId`、`accountId` 或 `terminal`。
  - Service 必须创建新的查询对象并写入当前 `sellerId`；不得直接修改或透传前端提交的 `ProductSpu` 查询对象。
  - 允许复制的列表筛选字段仅限业务筛选字段，例如 `keyword`、`sellerSpuCode`、`sellerSkuCode`、`productName`、`productNameEn`、`categoryId`、`spuStatus`；`systemSpuCode`、`systemSkuCode`、`sourceType` 等管理端或系统字段不得作为 seller 端范围来源。
  - 列表 DTO 转换必须保留 PageHelper 分页元数据，避免 `getDataTable(...)` 只能读到当前页条数。
  - 详情和 SKU 列表必须先读取商品并校验 `product.sellerId == session.subjectId`；不属于当前卖家的商品统一按“商城商品不存在”处理。
  - 列表、详情和 SKU 查询都必须先校验 `PortalLoginSession.terminal == "seller"`、`subjectId` 和 `accountId`；session 不匹配时必须先返回带 `HttpStatus.UNAUTHORIZED` 的“登录状态已失效”，且不得调用共享 product service 查询商品。
  - seller 端响应使用 `SellerPortalProduct` / `SellerPortalProductSku` DTO，不直接返回 `ProductSpu` / `ProductSku`，避免把 `sellerId`、系统 SPU/SKU、`BaseEntity` 审计字段或后台范围字段作为端内 API 标准。
  - 端入口必须使用方法级 `@Anonymous` + `@PortalPreAuthorize(terminal = "seller", hasPermi = "...")` + `@PortalLog(terminal = "seller", ...)`，并继续受 `TerminalRouteOwnershipTest` 和 `TerminalSeedPermissionContractTest` 约束。
  - 当前权限点为 `seller:product:distribution:list` 和 `seller:product:distribution:query`，只读 seed 已写入 `seller_menu` 和 active seller role 授权；远程运行库已补 seller 端权限 DML。buyer 端商品浏览权限已按独立买家口径在 buyer 侧另行完成。
  - 脚本化烟测必须覆盖 seller 登录、列表、伪造客户端范围参数、详情、SKU、响应字段脱敏、跨卖家详情/SKU 负向访问和 logout 清理；烟测不得输出 token、JWT、Redis key、`.env.local` 或数据库连接明文。
  - buyer 端后续如果复制该模板，只能替换 terminal、路径、权限前缀、日志 title、service 名称、DTO 名称和测试名；不能把 seller 商品拥有关系机械改成 buyer 拥有关系，买家浏览商品的可见性规则需要单独确认。
  - `SellerPortalProductServiceImplTest` 已固定非 seller session 的 fail-closed 契约：列表、详情和 SKU 均不得查询 product service；后续修改 `IProductDistributionService` 接口时，需要同步维护 seller/buyer portal service fake，避免测试编译漂移。
  - 直接运行 seller/buyer portal 商品服务测试时必须带 `-am`，例如 `mvn -pl seller -am "-Dtest=SellerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`；不带 `-am` 的局部命令可能读取本机 Maven 仓库里的旧 `product` 构件并出现 `NoSuchMethodError` 假红。

### 卖家端我的商城商品前端工作台模板

- 位置：
  - `react-ui/src/types/seller-buyer/party.d.ts`
  - `react-ui/src/services/portal/session.ts`
  - `react-ui/src/pages/Portal/Home/SellerOwnDistributionProductList.tsx`
  - `react-ui/src/pages/Portal/Home/index.tsx`
- 当前用途：
  - 在 seller portal 工作台只读展示当前 seller 的商城商品列表、详情和 SKU。
  - 作为 seller 端真实业务列表接入的前端模板，配合后端 `SellerPortalProductDistributionController` 使用。
  - seller 主列表已升级为标准 ProTable 模板；buyer 已按 buyer 浏览口径完成 ProTable 差量复制。
- 复用规则：
  - seller portal 商品请求必须通过 `services/portal/session.ts`，使用 seller token，并显式 `isToken: false`；不要回退复用管理端 `access_token`。
  - 列表参数必须经过 `sanitizePortalQueryParams(...)`，不得把 `sellerId`、`buyerId`、`subjectId`、`accountId`、`sellerAccountId`、`buyerAccountId` 或 `terminal` 作为端内数据范围传给后端。
  - seller portal 商品类型使用 `API.Partner.SellerPortalProduct` / `SellerPortalProductSku`，不要直接复用管理端 `API.ProductDistribution.Spu` 作为端内响应标准。
  - seller portal 商品主列表必须复用 `getPersistedProTableSearch(...)` 保存筛选区展开/收起状态，使用 `current -> pageNum` 和 `pageSize -> pageSize` 映射调用若依分页接口。
  - 商品详情和 SKU 均从 seller portal 接口读取；前端只负责展示，不做 seller 归属判断。
  - buyer 后续不能机械复制 seller 商品拥有关系；必须先确认买家商品浏览可见性、上架状态、价格口径和库存边界。

### 端内 Controller 鉴权模板守卫

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java`
- 当前用途：
  - 防止 `product` 共享模块重新暴露 `/seller...` 或 `/buyer...` 端入口。
  - 防止 seller/buyer 受保护 portal handler 漏掉方法级 `@Anonymous`、`@PortalPreAuthorize`、`@PortalLog` 或当前会话派生。
  - 防止 seller/buyer 受保护 portal handler 在方法签名中接收前端传入的 `sellerId`、`buyerId`、`subjectId`、`accountId` 或 `terminal` 作为身份范围边界。
  - 防止 seller/buyer 受保护 portal handler 使用若依后台 `SecurityUtils.getLoginUser/getUserId/getUsername`、`LoginUser` 或 `SysUser` 作为端内身份兜底。
- 复用规则：
  - seller/buyer 受保护端入口新增 controller 时，文件名应使用 `*Portal*Controller.java`，并避免把认证入口之外的真实业务接口放进 `*PortalAuthController.java`。
  - `TerminalRouteOwnershipTest` 会自动发现 seller/buyer 模块 controller 目录下的受保护 portal controller；新增真实业务 controller 后不需要手工维护硬编码清单。
  - seller 受保护端入口新增后先跑 seller 模板守卫；确认通过后，再按同构规则复制 buyer，只替换 terminal、路径、权限前缀和日志 title。
  - 端内业务 handler 可以接收真实业务参数，例如 `categoryId`、筛选对象或分页参数；但不能把 `sellerId`、`buyerId`、`subjectId`、`accountId`、`terminal` 作为请求参数或路径参数接入。
  - 受 `@PortalPreAuthorize` 保护的端内 handler 不得在缺失端内上下文时回退到若依后台登录上下文；缺少 `PortalSessionContext` 或 terminal 不匹配时必须直接拒绝。
  - 如果某个端内查询 DTO 内含 `subjectId` 或 `accountId`，Controller 或 Service 必须用 `PortalSessionContext` 中的当前会话覆盖这些字段；更稳的模板是提供 session-scoped Service 方法，在 Service 内强制设置范围。
  - 当前账号日志这类带 `subjectId` / `accountId` 的 DTO，标准模板必须使用 session-scoped Service；Controller 覆盖 DTO 只作为历史兼容，不作为后续新增模板。
  - seller/buyer 端登录和免密消费入口属于认证入口例外，不纳入受保护 handler 模板检查；真正业务接口必须纳入。
  - 该测试只证明端入口模板不漏；具体业务权限点、数据范围、字段脱敏和审计内容仍要在对应业务接口测试或接口烟测中验证。

### 端内日志 SQL 独立 DDL 守卫

- 位置：
  - `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`
  - `RuoYi-Vue/sql/20260604_three_terminal_legacy_sys_user_account_backfill.sql`
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalSqlIsolationContractTest.java`
  - `docs/plans/2026-06-05-terminal-log-sql-explicit-ddl-record.md`
  - `docs/plans/2026-06-05-terminal-legacy-sys-user-backfill-isolation-record.md`
- 当前用途：
  - 固定 seller/buyer 端内登录日志和操作日志表必须使用显式独立 DDL。
  - 防止 `seller_login_log` / `buyer_login_log` 通过 `LIKE sys_logininfor` 派生。
  - 防止 `seller_oper_log` / `buyer_oper_log` 通过 `LIKE sys_oper_log` 派生。
  - 固定当前主三端隔离迁移脚本不得再从 `sys_user` 回填 seller/buyer 端账号；历史混用账号库回填只能放在明确标记的 legacy helper 中。
- 复用规则：
  - 管理端继续使用若依 `sys_logininfor` / `sys_oper_log`；seller/buyer 端内日志只写各自端内日志表。
  - 新增或调整端内日志表时，应显式列出端主体 ID、端账号 ID、业务日志字段和索引，不从若依 `sys_*` 日志表复制结构。
  - 当前主三端隔离 SQL 不能包含 `migrate_*_account_from_sys_user`、`join sys_user` 或类似账号回填逻辑。
  - `20260604_three_terminal_legacy_sys_user_account_backfill.sql` 只用于历史库仍有 `seller_account.user_id` / `buyer_account.user_id` 指向 `sys_user` 的迁出场景；如需使用，必须在主三端隔离迁移脚本之前执行。
  - 新环境、当前远程验证库和后续业务增量不得执行 legacy helper，也不得重新把端账号绑定回 `sys_user`。
  - 调整三端隔离 SQL 或综合 seed 后，至少运行 `mvn -pl ruoyi-system "-Dtest=TerminalSqlIsolationContractTest,TerminalSeedPermissionContractTest" test`。

### PortalOperLogService 端内写入路由

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/impl/PortalOperLogServiceImpl.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/mapper/PortalOperLogMapper.java`
  - `RuoYi-Vue/ruoyi-system/src/main/resources/mapper/system/PortalOperLogMapper.xml`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/impl/PortalOperLogServiceImplTest.java`
- 当前用途：
  - 固定 `@PortalLog` 生成的 `PortalOperLog` 按 terminal 写入对应端内操作日志表。
  - seller terminal 只能调用 `insertSellerOperLog(...)`，最终落到 `seller_oper_log`。
  - buyer terminal 只能调用 `insertBuyerOperLog(...)`，最终落到 `buyer_oper_log`。
  - 未知 terminal 必须 fail loud，不允许静默回落到管理端 `sys_oper_log`。
  - 免密代入上下文必须由 Aspect 复制到 `PortalOperLog` 结构化字段，并由 `PortalOperLogMapper.xml` 写入 `seller_oper_log` / `buyer_oper_log`；不得只拼接 `oper_param`。
- 复用规则：
  - 端内业务接口继续使用 `@PortalLog` + `AsyncFactory.recordPortalOper(...)` + `IPortalOperLogService` 写入端内操作日志，不得改用 `ISysOperLogService`。
  - 管理端接口继续使用若依 `@Log` / `sys_oper_log`；seller/buyer 端内接口继续使用 `@PortalLog` / `seller_oper_log` / `buyer_oper_log`。
  - 查询端内操作日志时，seller/buyer Mapper 的 `PortalOperLog` resultMap 和 `select*OperLogList` 必须同步投影 direct-login 结构化字段，避免写入后不可查。
  - 后续如果增加新的端类型，必须先扩展 `PortalOperLogServiceImplTest` 和 mapper XML，再接入 controller。
  - 调整端内操作日志写入路由后，至少运行 `mvn -pl ruoyi-system -am "-Dtest=PortalOperLogServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。

### 卖家/买家管理端权限契约守卫

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminDirectLoginPermissionContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalSeedPermissionContractTest.java`
- 当前用途：
  - 固定卖家/买家管理端 Admin Controller 的标准模板，防止平台后台控制面误用端内 `@PortalPreAuthorize` / `@PortalLog` / `@Anonymous`。
  - 校验卖家管理端 handler 必须使用 `seller:admin:*` 权限前缀，买家管理端 handler 必须使用 `buyer:admin:*` 权限前缀。
  - 校验 controller 中声明的卖家/买家管理端权限必须存在于 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`。
  - 校验 `POST` / `PUT` / `DELETE` / `PATCH` 等变更类管理端操作必须声明 `@Log`。
  - 固定免密代入和免密票据审计的专门权限：主体级/账号级免密代入必须使用 `*:admin:directLogin`，免密票据列表必须使用 `*:admin:ticket:list`。
  - 校验共享前端模板必须按 `directLogin` 和 `ticket:list` 权限显隐主体行入口、账号行入口、全局审计入口和免密票据 tab。
  - 卖家/买家账号维护均已按卖家标准模板细化权限：账号列表、新增、编辑、密码重置、账号角色查询和账号角色分配分别使用 `seller:admin:account:*` / `buyer:admin:account:*`，不得继续复用主体 `query/add/edit/resetPwd` 或端内角色维护权限。
  - 卖家/买家主体详情仍分别使用 `seller:admin:query` / `buyer:admin:query`；主体/账号免密代入仍使用 `*:admin:directLogin`；主体/账号会话和强制踢出仍使用 `*:admin:forceLogout`；这些敏感控制不并入账号 CRUD 权限。
  - `PermissionServiceAccountPermissionTest` 覆盖若依实际 `@ss.hasPermi(...)` 运行时判断，证明只有主体权限或端内角色维护权限时不能通过 `*:admin:account:*`，账号 reset/lock/forceLogout 权限不能通过 `*:admin:directLogin` / `*:admin:ticket:list`，directLogin/ticket 权限也不能误授权账号操作或强制踢出。
  - `AdminAccountPermissionUiContractTest` 覆盖前端显隐契约，证明 seller/buyer 页面必须配置 `accountPermissions`，公共账号入口和账号弹窗按钮必须用账号域权限控制。
- 复用规则：
  - 后续新增 `AdminSeller*Controller` 时先跑卖家测试；确认卖家模板通过后，再按同构规则复制到买家测试。
  - 买家复制时只替换 terminal、controller 路径、权限前缀和 seed 权限集合，不重新设计规则。
  - seller 模板验收通过后复制 buyer 时，不允许把免密代入权限并入 `query` / `edit` / `list`，也不允许让没有 `ticket:list` 的账号看到免密票据审计 tab。
  - 复制买家账号域权限前，先以卖家模板验收结果为准；买家只替换为 `buyer:admin:account:*`、buyer controller、buyer seed 和 buyer 前端配置。
  - 卖家低权限运行时负向验收已固定样板：管理端测试角色只给 `seller:admin:list`、`seller:admin:query`、`seller:admin:loginLog:list`、`seller:admin:operLog:list`，不得给 `seller:admin:directLogin` 或 `seller:admin:ticket:list`；验收时必须同时检查后端接口 403 和前端按钮/tab 隐藏。
  - 买家低权限运行时负向验收已按卖家样板复制：管理端测试角色只给 `buyer:admin:list`、`buyer:admin:query`、`buyer:admin:loginLog:list`、`buyer:admin:operLog:list`，不得给 `buyer:admin:directLogin` 或 `buyer:admin:ticket:list`。
  - 账号域真实低权限验收已固定样板：创建临时管理端角色和账号时，只授予主体管理、卖家/买家管理和主体查询权限，不授予任何 `seller:admin:account:*` 或 `buyer:admin:account:*`；验收必须同时检查 `/seller/admin/sellers/{sellerId}/accounts` 与 `/buyer/admin/buyers/{buyerId}/accounts` 返回 `code=403`，并用浏览器确认卖家/买家管理数据行不显示“账号”入口。
  - 真实低权限账号验收后必须强制踢出在线 token，并删除临时 `sys_user_role`、`sys_role_menu`、`sys_user`、`sys_role` 数据；不把测试账号和测试角色长期留在运行库。
  - 权限注解或后端鉴权改动后，真实接口验收前必须确认 8080 运行的是重新打包后的新 jar；否则可能因为旧 jar 未加载新注解而误判。
  - 静态契约测试、`PermissionService` 单元测试、真实接口 403 和浏览器按钮隐藏分别覆盖不同层；后续账号域或敏感控制权限变更时，不能用其中一层替代全部验收。

### 端内权限综合 Seed 初始化守卫

- 位置：
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - `RuoYi-Vue/sql/20260604_portal_account_list_permission_seed.sql`
  - `RuoYi-Vue/sql/20260604_portal_dept_role_list_permission_seed.sql`
  - `RuoYi-Vue/sql/20260604_portal_product_category_permission_seed.sql`
  - `RuoYi-Vue/sql/20260604_seller_product_schema_permission_seed.sql`
  - `RuoYi-Vue/sql/20260604_buyer_product_schema_permission_seed.sql`
  - `RuoYi-Vue/sql/20260607_portal_self_audit_permission_seed.sql`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalSeedPermissionContractTest.java`
- 当前用途：
  - 让综合初始化 seed 覆盖 seller/buyer 端内门户当前最小权限。
  - 初始化 active 卖家、买家的默认 `Owner` 端内角色。
  - 绑定 `OWNER` 账号到默认 `owner` 角色。
  - 绑定 active 端内角色到当前最小端内权限菜单。
- 复用规则：
  - 新环境默认以 `seller_buyer_management_seed.sql` 作为卖家/买家主体、端内账号、端内角色、端内菜单和端内权限初始化入口，不要只依赖零散增量 SQL。
  - 历史增量 SQL 仍可作为已运行环境补丁；新增端内权限时必须同时评估是否需要写入综合 seed 和增量 SQL。
  - 综合初始化 seed 写入 `seller_menu` / `buyer_menu` 前，也必须通过 `assert_seller_menu_permission_slot(...)` / `assert_buyer_menu_permission_slot(...)` 断言同 `perms` 菜单签名；不得因为它是 fresh/patch 综合入口就只用 `where not exists (select 1 ... where perms = ...)`。
  - 综合初始化 seed 给 `owner` 角色写 `seller_role_menu` / `buyer_role_menu` 时，`join seller_menu` / `join buyer_menu` 必须带 `parent_id=0`、`menu_type=F`、空 `path/component/route_name` 条件，避免历史脏菜单被默认 Owner 角色绑定。
  - 独立增量权限 seed 写入 `seller_menu` / `buyer_menu` 前，必须先通过 `assert_seller_menu_permission_slot(...)` / `assert_buyer_menu_permission_slot(...)` 断言已有同 `perms` 菜单的 `parent_id/menu_type/path/component/route_name` 与当前 seed 期望一致；不得只用 `where not exists (select 1 ... where perms = ...)` 静默复用历史脏菜单。
  - 独立增量权限 seed 写 `seller_role_menu` / `buyer_role_menu` 时，`join seller_menu` / `join buyer_menu` 也必须带同一签名条件，避免历史脏菜单绕过 insert guard 后被角色授权绑定。
  - 当前最小卖家端权限包括 `seller:account:list`、`seller:account:loginLog:list`、`seller:account:operLog:list`、`seller:account:session:list`、`seller:dept:list`、`seller:role:list`、`seller:product:category:list`、`seller:product:schema:query`、`seller:product:distribution:list`、`seller:product:distribution:query`。
  - 当前最小买家端权限包括 `buyer:account:list`、`buyer:account:loginLog:list`、`buyer:account:operLog:list`、`buyer:account:session:list`、`buyer:dept:list`、`buyer:role:list`、`buyer:product:category:list`、`buyer:product:schema:query`、`buyer:product:distribution:list`、`buyer:product:distribution:query`。
  - `TerminalSeedPermissionContractTest` 会自动扫描 seller/buyer 源码中的 `@PortalPreAuthorize(hasPermi = "...")`；新增端内权限时，controller 和综合 seed 必须一起更新。
  - 新增 seller/buyer 端真实业务权限时，先按卖家模板落地并验收，再复制买家；复制时只替换 terminal、权限前缀、表名和验证主体，不重新设计权限结构。
  - 每次调整端内门户权限 seed 后必须运行 `mvn -pl ruoyi-system -Dtest=TerminalSeedPermissionContractTest test`；涉及管理端控制面权限时同时运行 seller/buyer 管理端权限契约测试。

### PartnerSupport

- 位置：`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PartnerSupport.java`
- 当前用途：
  - 卖家/买家编号生成
  - 主体类型、等级、联系地址、附件字段校验
  - 主账号昵称生成
  - 卖家端/买家端当前账号修改密码字段校验
- 复用规则：
  - 只放卖家/买家共用的机械逻辑。
  - 不要把它扩成新的“客户模块”。
  - 后续端内密码修改继续复用 `normalizePasswordChange(...)`，不要在 seller/buyer service 或 React 页面里重复写旧密码、新密码、确认密码校验规则。

### 卖家/买家端账号表

- 位置：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml`
- 当前用途：
  - 管理端创建卖家/买家端主账号和子账号。
  - 管理端重置卖家/买家端账号密码。
  - 卖家端/买家端当前账号自行修改密码。
  - 卖家/买家列表展示主账号、账号数和最后登录时间。
  - 免密登录生成时读取端内主账号 `accountId`。
  - 卖家端/买家端登录成功后更新最后登录 IP 和最后登录时间。
- 复用规则：
  - 卖家端账号只读写 `seller_account`，买家端账号只读写 `buyer_account`。
  - 不允许重新引入 `PortalAccountSupport` 或 `PortalAccountMapper` 把端账号绑定回 `sys_user`。
  - 端内角色、菜单、部门、日志和会话分别使用 `seller_*` / `buyer_*` 表。
  - 端内当前账号修改密码必须从 `PortalSessionContext` 推导主体 ID 和账号 ID，不允许前端传 `sellerId` / `buyerId` / `accountId` 决定修改对象。
  - 按账号 ID 查询可以读取 `password` 供 Service 内部旧密码校验；管理端列表、端内资料接口和端内账号列表不得返回 `password` 字段。

### PortalDirectLoginSupport

- 位置：`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java`
- 当前用途：
  - 管理端为卖家端、买家端生成免密登录 token。
  - 卖家端、买家端登录入口消费免密登录 token。
  - 明文一次性 token 只返回给管理端用于生成短时登录链接；审计票据写入 `portal_direct_login_ticket`，只保存 `token_hash`，不保存明文 token。
  - Redis payload 默认有效期 30 分钟，key 使用 `portal_direct_login:{terminal}:{token_hash}`；认证链路只读取端隔离 key，不再读取旧 `portal_direct_login:{token_hash}`，成功、失败收口和过期清理时仍删除旧 key 作为历史残留清理；payload 不保存明文 token 字段。
  - 票据 `terminal` 与当前消费端不匹配时，共享 support 不得把外端票据上下文交给当前端失败审计器；seller/buyer service 只能按当前端普通失败记录或直接拒绝，不得写入外端 `ticketId`、`actingAdmin*`、`reason`、目标主体或目标账号。
  - 端地址优先读取若依参数配置 `portal.seller.web.url`、`portal.buyer.web.url`，未配置时使用本地验证占位地址；seed 只允许缺失插入，不得覆盖已有环境配置。
- 复用规则：
  - 只复用在卖家端、买家端这类门户入口的免密登录场景。
  - 只负责生成和消费一次性 token；目标端消费后仍由 seller/buyer service 创建端 session，不绕过目标端账号、角色、菜单和权限模型。
  - 目标端消费免密 token 时，必须在票据标记 `USED` 和删除 Redis payload 之前完成当前主体/端账号状态校验；主体停用、账号停用、账号锁定、目标账号不存在或目标账号已改绑到其他主体时，不得消耗票据。seller/buyer service 通过 `consumeToken(..., validator)` 注入当前状态校验，`PortalDirectLoginSupportTest` 固定 validator 失败时不 mark used、不删 Redis。
  - 后续三端拆分后，卖家端/买家端应各自保留 token 消费入口，并读取自己的端账号、角色、菜单和权限体系。
  - `PortalDirectLoginSupportTest` 已守住 token hash 入库、Redis 端前缀 hash key、旧 Redis key 不可被认证链路消费、Redis payload 不含 token 字段、30 分钟 TTL、一次性消费、跨端拒绝不写外端审计上下文、过期标记和代入原因必填；后续改动免密链路时必须同步跑该测试。

### PortalDirectLoginTicketMapper

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalDirectLoginTicket.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/mapper/PortalDirectLoginTicketMapper.java`
  - `RuoYi-Vue/ruoyi-system/src/main/resources/mapper/system/PortalDirectLoginTicketMapper.xml`
- 当前用途：
  - 记录管理端免密代入卖家端、买家端的审计票据。
  - 保存 acting admin、目标端、目标主体、目标端账号、过期时间、使用时间、使用 IP 和状态。
  - 通过 `token_hash` 唯一索引校验一次性 token，不保存明文 token。
  - `PortalDirectLoginTicket.tokenHash` 只允许后端内部 Mapper / Support 使用，必须通过 `@JsonIgnore` 阻止管理端列表响应序列化输出。
- 复用规则：
  - 免密代入必须经过 `PortalDirectLoginSupport` 和该 ticket mapper，不要在 seller/buyer service 或前端里临时生成直登 token。
  - 卖家端和买家端共用这一张平台审计票据表，不分别复制 `seller_direct_login_ticket` / `buyer_direct_login_ticket`。
  - 管理端审计列表已读取 `portal_direct_login_ticket`，后续继续复用该表，不要读取 Redis payload。
  - 管理端卖家/买家免密票据列表必须在各自 service 层强制设置 `terminal='seller'` / `terminal='buyer'`，再调用共享 mapper；不能相信前端传入的 `terminal` 参数决定查询范围。
  - 管理端卖家/买家免密票据列表按 `targetAccountId` 筛选时必须同时提供 `targetSubjectId`，并由 `SellerServiceImpl` / `BuyerServiceImpl` 通过 scoped account mapper 校验账号确实属于该主体；不得让裸账号 ID 进入共享 ticket mapper。
  - 管理端审计列表不得返回 `tokenHash`、明文 token、Redis key、Authorization header 或端 session token；该边界由 `PortalDirectLoginTicketTest` 守卫。
  - `AdminDirectLoginPermissionContractTest`、`SellerServiceImplTest`、`BuyerServiceImplTest` 已守住 ticket 列表权限点、service 层 terminal 强制过滤、账号筛选主体约束、normalizer 执行顺序、scoped account mapper 和 mapper XML 的 terminal 条件；后续改 ticket 审计入口时必须同步跑这些测试。
  - 管理端生成免密代入票据时必须填写“代入原因”，并通过 `PortalDirectLoginSupport` 写入 `reason` 字段，不要另开临时备注字段或绕过公共支撑。

### PortalTokenSupport

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalTokenSupportTest.java`
- 当前用途：
  - 为卖家端、买家端签发独立登录 token。
  - 使用 `portal_login_tokens:` Redis 前缀，避免与管理端 `login_tokens:` 混用。
  - token claim 包含 `terminal`、端登录 key 和用户名；登录返回包含 `terminal`、`subjectId`、`subjectNo`、`accountId`。
  - 构建 `PortalLoginSession` 和 `PortalLoginLog`，由 seller/buyer 模块分别写入自己的会话表和登录日志表。
  - 解析卖家端/买家端请求头中的端 token，并按期望端类型读取 Redis 会话。
  - 批量删除端内 Redis token，供管理端强制踢出卖家/买家主体或账号在线会话。
  - 删除当前端内登录 token，供卖家端/买家端主动退出当前会话。
  - 自动化测试已覆盖端内 Redis key 前缀、JWT terminal claim、Redis session terminal 校验和端内 token 删除 key 拼接。
- 复用规则：
  - 只用于卖家端、买家端这类端内登录身份建立。
  - 不要把它用于管理端若依登录；管理端继续使用若依 `TokenService`。
  - 新增 seller/buyer 端内接口时，应从端 token 解析出的身份推导数据范围，不要相信前端传入的 `sellerId` / `buyerId`。
  - 它只提供端内会话身份；端内角色、权限和菜单读取由 seller/buyer 模块自己的 PermissionService 完成，不在 token 工具里直接查业务权限表。
  - 强制踢出必须复用 `PortalTokenSupport.deleteLoginTokens` 删除 Redis token，不要在 seller/buyer service 中拼接 `portal_login_tokens:` key。
  - 当前短期配置仍共享 `token.secret`、`token.header` 和 `token.expireTime`；端隔离依赖 JWT terminal claim 与 `portal_login_tokens:{terminal}:{tokenId}`。如果后续拆成三端独立 token 配置，必须同步更新 `PortalTokenSupportTest`。

### PortalPermissionSupport

- 位置：`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalPermissionSupport.java`
- 当前用途：
  - 统一卖家端/买家端菜单默认值和合法性校验。
  - 统一卖家端/买家端角色字段校验和菜单 ID 去重。
  - 统一 `PortalMenu` 树构建和 `PortalTreeSelect` 输出。
- 复用规则：
  - 后续新增卖家端/买家端菜单、角色、端内权限读取时优先复用该支撑类。
  - 该类只放两端共用的无状态校验和树处理，不直接读写 `seller_*` / `buyer_*` 表。
  - 端内权限数据访问仍必须留在 seller/buyer 模块自己的 Mapper 和 Service 中。

### PortalPreAuthorize / PortalPermissionChecker / PortalSessionContext

- 位置：
  - `RuoYi-Vue/ruoyi-common/src/main/java/com/ruoyi/common/annotation/PortalPreAuthorize.java`
  - `RuoYi-Vue/ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/PortalPreAuthorizeAspect.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalPermissionChecker.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalSessionContext.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/IPortalPermissionCheckService.java`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplPortalAccessTest.java`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplMenuTreeTest.java`
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplPortalAccessTest.java`
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplMenuTreeTest.java`
- 当前用途：
  - 为卖家端、买家端接口提供统一的端内权限注解。
  - 统一解析端 token、校验 terminal、读取端内权限集合。
  - 支持全部权限要求、任一权限要求和若依超级权限 `*:*:*`。
  - 让 `SellerPortalPermissionServiceImpl` / `BuyerPortalPermissionServiceImpl` 以同一接口接入权限校验。
  - `PortalPreAuthorizeAspect` 会把校验通过的 `PortalLoginSession` 写入 `PortalSessionContext`，供当前请求内 Controller、Service 和日志切面复用。
  - 当前已覆盖 `/seller/getInfo`、`/seller/getRouters`、`/buyer/getInfo`、`/buyer/getRouters`。
  - 卖家端已在权限服务中回查 `seller_session` 在线状态，买家端已按同一模板回查 `buyer_session` 在线状态；Redis token 只是入口缓存，DB session 失效后，对应端受保护接口必须拒绝旧 token。
  - `SellerPortalPermissionServiceImplPortalAccessTest` / `BuyerPortalPermissionServiceImplPortalAccessTest` 已固定端内权限 service 会话 fail-closed 契约：`selectPortalPermissionInfo(...)`、`selectPermissions(...)` 必须复用同一会话守卫；畸形 session 不得继续查询主体、账号或 DB session；DB session 缺失、已退出、已过期或状态非在线时，权限 service 必须返回登录失效。
  - `SellerPortalPermissionServiceImplMenuTreeTest` / `BuyerPortalPermissionServiceImplMenuTreeTest` 已固定 `selectPortalMenuTree(...)` 的独立菜单树读取契约：离线 DB session 不得查询端内菜单；在线 session 必须用当前 `subjectId/accountId` 查询端内 mapper，并通过 `PortalPermissionSupport.buildMenuTree(...)` 返回父子树。
- 复用规则：
  - seller/buyer portal 端受保护接口必须方法级同时声明 `@Anonymous` 和 `@PortalPreAuthorize`：`@Anonymous` 只负责放行若依外层登录过滤，`@PortalPreAuthorize` 负责端 token、terminal 和权限校验。
  - 不要在 seller/buyer portal controller 上使用类级 `@Anonymous`；新增方法如果漏挂 `@PortalPreAuthorize`，类级匿名会让接口无意公开。
  - 后续卖家端真实业务接口优先使用 `@PortalPreAuthorize(terminal = "seller", ...)`。
  - 后续买家端真实业务接口优先使用 `@PortalPreAuthorize(terminal = "buyer", ...)`。
  - seller/buyer 端 `selectPortalPermissionInfo(...)`、`selectPermissions(...)`、`selectPortalMenuTree(...)` 都必须先经过 `assertActiveSellerSession(...)` / `assertActiveBuyerSession(...)`；守卫必须先校验 session 形态：`session` 非空、terminal 匹配、`subjectId` 非空、`accountId` 非空、`tokenId` 非空白；任一不满足都按 401 登录失效处理，且不得继续查询主体、账号、角色、菜单或 DB session。
  - seller/buyer 端 `selectPortalPermissionInfo(...)` 正向返回必须以当前 session 的 `subjectId/accountId` 查询端内 mapper：seller 读取 `seller_account_role/seller_role/seller_role_menu/seller_menu`，buyer 读取 `buyer_account_role/buyer_role/buyer_role_menu/buyer_menu`；返回的 `roles` / `permissions` 不得来自管理端 `sys_role` / `sys_menu`。
  - `SellerPortalPermissionServiceImplTest` / `BuyerPortalPermissionServiceImplTest` 只保留 `assignAccountRoles...` 角色绑定写操作测试；`SellerPortalPermissionServiceImplPortalAccessTest` / `BuyerPortalPermissionServiceImplPortalAccessTest` 承载端内访问、会话守卫和权限信息返回；`SellerPortalPermissionServiceImplMenuTreeTest` / `BuyerPortalPermissionServiceImplMenuTreeTest` 承载菜单树读取测试。后续不要把三类 fake 重新混回一个 500 行以上测试文件。
  - seller 端 `@PortalPreAuthorize` 鉴权必须继续经过 `SellerPortalPermissionServiceImpl.assertActiveSellerSession(...)`，并以 `seller_session.status='0'`、`logout_time is null`、`expire_time >= sysdate()` 作为当前会话仍有效的 DB 兜底判断。
  - buyer 端 `@PortalPreAuthorize` 鉴权必须继续经过 `BuyerPortalPermissionServiceImpl.assertActiveBuyerSession(...)`，并以 `buyer_session.status='0'`、`logout_time is null`、`expire_time >= sysdate()` 作为当前会话仍有效的 DB 兜底判断。
  - 不要在 Controller 或 Service 里重复手写 token 解析、权限集合读取、`*:*:*` 判断和端类型判断。
  - 端内接口需要当前主体或账号时，优先从 `PortalSessionContext.requireSession("seller" / "buyer")` 获取 `subjectId`、`accountId` 和 `terminal`。
  - 受保护端内接口不得用若依后台 `SecurityUtils`、`LoginUser`、`SysUser` 或 `@ss` 权限上下文推导 seller/buyer 主体、端账号或数据范围；管理端 `/seller/admin/**`、`/buyer/admin/**` 继续走若依后台上下文。
  - 端内主动退出也必须先经过 `@PortalPreAuthorize`，再用 `PortalSessionContext` 中的当前会话删除对应 Redis token 和更新当前 session 行；不要让前端传 `tokenId`、`sellerId`、`buyerId` 或 `accountId` 决定退出范围。
  - 端内当前账号修改密码也必须先经过 `@PortalPreAuthorize`，再从 `PortalSessionContext` 读取当前会话；请求体只允许包含旧密码、新密码和确认密码，不允许传目标账号 ID。
  - 涉及密码、token、密钥的端内接口必须关闭响应日志或保证脱敏；`oldPassword`、`newPassword`、`confirmPassword` 不得写入 Markdown、日志或前端状态持久化。
  - `PortalPreAuthorizeAspect` 不依赖 `argNames` 绑定注解参数，统一从 `MethodSignature` 读取 `@PortalPreAuthorize`，避免 Spring AOP 参数名绑定差异导致端内接口 500。
  - 管理端接口继续使用若依 `@PreAuthorize("@ss.hasPermi(...)")`，不要把管理端后台权限迁移到 `@PortalPreAuthorize`。
  - 接口涉及端内业务数据时，权限注解只负责“能不能访问该操作”；数据范围仍必须从端 token 推导 `sellerId` / `buyerId`，不能相信前端传入主体 ID。

### TerminalAccountIsolationTest

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalAccountIsolationTest.java`
- 当前用途：
  - 自动扫描 `seller` / `buyer` 模块，防止端内账号权限控制面重新依赖管理端 `sys_*`。
  - 当前守卫关键词包括 `sys_user`、`sys_role`、`sys_menu`、`sys_dept`、`sys_user_role`、`sys_role_menu`、`SysUser`、`SysRole`、`SysMenu`、`SysDept`、`PortalAccountSupport`、`PortalAccountMapper`、`seller_account.user_id`、`buyer_account.user_id`。
  - 固定 seller/buyer 账号 Mapper 和 Service 的主体作用域：Mapper 接口和 XML 不得保留裸 `selectSellerAccountById(accountId)` / `selectBuyerAccountById(accountId)` 声明，Service 接口和实现不得新增单参数账号查询入口；生产代码不得调用裸 `sellerMapper.selectSellerAccountById(accountId)` / `buyerMapper.selectBuyerAccountById(accountId)` 或裸账号 service helper；管理端登录日志、操作日志和免密票据审计列表按账号筛选也必须显式提供 `sellerId/buyerId + accountId`，并走 `select*AccountByIdAnd*Id(...)` 校验。
- 复用规则：
  - 后续 seller/buyer 端账号、角色、菜单、部门、日志、会话逻辑必须继续使用各自端内表和端内 domain。
  - 管理端后台能力可以继续使用若依 `sys_*`，因此该测试只扫描 `seller` / `buyer` 模块，不扫描 `ruoyi-system` 或若依后台 mapper。
  - 如果端内账号权限实现移动到新的 terminal 模块，需要同步扩展本测试扫描路径。
  - 该测试只防止控制面回退和账号裸查扩散，不替代端内业务数据范围、权限点、脱敏和接口烟测。

### PortalDeptSupport

- 位置：`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDeptSupport.java`
- 当前用途：
  - 统一卖家端/买家端部门默认值和合法性校验。
  - 统一部门父级校验、祖级路径生成和树结构构建。
  - 统一 `PortalDept` 到 `PortalTreeSelect` 的树选择输出。
- 复用规则：
  - 后续新增卖家端/买家端部门管理、账号部门绑定、端内组织选择时优先复用该支撑类。
  - 该类只放两端共用的无状态校验和树处理，不直接读写 `seller_*` / `buyer_*` 表。
  - 部门数据访问仍必须留在 seller/buyer 模块自己的 Dept Mapper 和 Service 中。

### 卖家/买家端菜单角色管理接口

- 位置：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerMenuController.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerRoleController.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerMenuController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerRoleController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml`
- 当前用途：
  - 管理端维护卖家端全局菜单 `seller_menu`。
  - 管理端维护买家端全局菜单 `buyer_menu`。
  - 管理端维护某个卖家主体下的端内角色 `seller_role`。
  - 管理端维护某个买家主体下的端内角色 `buyer_role`。
  - 角色菜单关系分别写入 `seller_role_menu` / `buyer_role_menu`。
  - 管理端绑定端账号和端内角色，分别写入 `seller_account_role` / `buyer_account_role`。
  - 卖家端/买家端通过 `/seller/getInfo`、`/seller/getRouters`、`/buyer/getInfo`、`/buyer/getRouters` 读取自己的端内角色、权限和菜单。
- 复用规则：
  - 管理端 `sys_menu` 只保存这些后台接口的按钮权限，不保存卖家端/买家端真实菜单。
  - 卖家端角色不得写入 `sys_role`，买家端角色不得写入 `sys_role`。
  - 卖家端菜单不得写入 `sys_menu`，买家端菜单不得写入 `sys_menu`。
  - 管理端账号弹窗已接入账号角色绑定接口；后续菜单、角色独立配置页面接入时，应继续调用这些接口，不要复用系统菜单 `/system/menu` 或系统角色 `/system/role` 页面直接改端内权限。
  - 端内 `getInfo` / `getRouters` 必须从端 token 推导 `sellerId` / `buyerId` 和 `accountId`，不能相信前端传入主体 ID。

### 卖家/买家端部门管理接口

- 位置：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerDeptController.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/ISellerPortalDeptService.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalDeptServiceImpl.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/mapper/SellerPortalDeptMapper.java`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalDeptMapper.xml`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerDeptController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/IBuyerPortalDeptService.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalDeptServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/mapper/BuyerPortalDeptMapper.java`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerPortalDeptMapper.xml`
- 当前用途：
  - 管理端维护某个卖家主体下的端内部门 `seller_dept`。
  - 管理端维护某个买家主体下的端内部门 `buyer_dept`。
  - 提供端内部门列表、详情、树选择、新增、修改和删除接口。
  - 删除部门前检查同端子部门和端账号 `dept_id` 占用。
- 复用规则：
  - 管理端 `sys_dept` 只代表平台后台组织，不代表卖家端或买家端内部部门。
  - 卖家端部门不得写入 `sys_dept`，买家端部门不得写入 `sys_dept`。
  - 后续账号新增/编辑需要部门归属时，应分别读取 `seller_dept` / `buyer_dept`，不要复用系统部门 `/system/dept`。
  - 端内业务接口需要组织范围时，必须从端 token 推导主体，再读取对应端内部门树和账号 `dept_id`。

### 卖家/买家端账号部门绑定

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalAccount.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml`
  - `react-ui/src/types/seller-buyer/party.d.ts`
  - `react-ui/src/services/seller/seller.ts`
  - `react-ui/src/services/buyer/buyer.ts`
- 当前用途：
  - `PortalAccount.deptId` / `deptName` 统一承载卖家端和买家端员工账号的端内部门归属。
  - 卖家账号新增/编辑时写入 `seller_account.dept_id`，并校验部门必须属于同一个 `seller_id`。
  - 买家账号新增/编辑时写入 `buyer_account.dept_id`，并校验部门必须属于同一个 `buyer_id`。
  - 管理端前端 service/type 已具备端账号编辑和端内部门列表/树查询契约。
- 复用规则：
  - 后续账号管理 UI 应复用 `PortalAccountBase.deptId` / `deptName` 和现有 seller/buyer service，不要在页面内临时拼接口路径。
  - 不允许使用 `sys_dept.dept_id` 绑定卖家端或买家端员工账号。
  - 账号编辑应保留原登录账号；登录名变更如需支持，必须单独设计审计和唯一性规则。

## 前端表格与表单

### 管理端同构 UI 模板化推进

- 当前用途：
  - 管理端卖家管理和买家管理。
  - 后续同构的卖家/买家控制弹窗、只读审计列表、配置页和相邻管理入口。
- 复用规则：
  - 已确认过的同构模式不再每次重新设计。
  - 先把卖家侧做成标准样板并完成验证，再复制成买家侧。
  - 买家侧只替换端类型、页面文案、路由、权限标识、字段配置和 service。
  - 通用表格、表单、弹窗、审计 tab 优先抽到 `react-ui/src/components/PartnerManagement/`。
  - seller/buyer 差异必须进入 `PartnerModuleConfig` 或独立 service，不要在公共组件里写大量 `if seller else buyer` 分支。
  - 模板化不等于跳过验证；复制后仍至少运行 TypeScript 检查，并做卖家、买家两个入口的冒烟验证。

### ProTable 筛选区与分页配置

- 位置：`react-ui/src/utils/proTableSearch.ts`
- 方法：
  - `getPersistedProTableSearch(config, storageKey?)`
  - `getActionSafeProTableSearchSpan(fieldCount)`
  - `getProTablePagination(configOrPageSize?)`
  - `getProTableScroll(x, config?)`
  - `getProTableColumnsState(persistenceKey, config?)`
- 当前用途：
  - 系统管理、系统监控、工具中心、主体管理、财务管理等 ProTable 页面筛选区。
  - 标准 ProTable 分页器，统一开启每页条数切换，并避免使用受控 `pageSize` 导致切换无效。
  - 标准 ProTable 滚动配置，统一传入 `scroll.y` 让 Ant Design 生成 fixed-header 结构，避免表头跟随数据行一起滚动。
  - 标准 ProTable 列设置持久化，使用 localStorage 记住用户调整过的列显示状态。
  - 页面级 ProTable 查询动作区由 `react-ui/src/global.css` 统一作为搜索行中的弹性动作列：空间足够时与筛选字段同一行靠右，空间不足时自然换到下一行靠右。
  - 字段数量已知的页面仍可在 `getPersistedProTableSearch({ fieldCount })` 中声明查询字段数来微调字段列数，但该配置不再承担防止动作区悬浮/压表格的职责。
  - 当前先服务 `react-ui` 管理端；后续拆出 `seller-ui` / `buyer-ui` 后，卖家端和买家端列表页继续复用同一套筛选预设。
- 复用规则：
  - 后续新增 ProTable 页面时，使用 `getPersistedProTableSearch(...)`，不要直接写散落的 `search={{ ... }}`。
  - 后续新增标准分页 ProTable 时，使用 `getProTablePagination(...)`，不要直接写 `pagination={{ pageSize: ... }}`；如果确实需要受控分页，必须同步维护 `current/pageSize/onChange`。
  - 后续新增页面级 ProTable 时，使用 `scroll={getProTableScroll(x)}`；弹窗、抽屉、展开行内的小表格可以只保留横向滚动，但不得再通过页面私有 CSS 覆盖 `.ant-table-container`、`.ant-table-content` 或 `.ant-table-body`。
  - 后续需要列设置持久化时，使用 `columnsState={getProTableColumnsState('稳定页面标识-columns')}`，并给纯 `render` 列、操作列补稳定 `key`。
  - 页面级 ProTable 如果开启 `options.setting` 或 `options.density`，不要使用 `toolBarRender={false}` 关闭 toolbar；无自定义按钮时使用 `toolBarRender={() => []}` 或省略自定义按钮，让 ProTable 自带设置入口正常渲染。
  - 三端前端列表页筛选区默认采用 Ant Design Pro 原生 `vertical` 查询布局，也就是字段名在上、输入框在下，避免标签和输入框横向挤压。
  - 三端前端筛选区必须按内容区宽度响应式降列：默认宽屏优先一行 5 个筛选字段并给查询动作预留位置，已知字段数的页面可通过 `fieldCount` 在 `xl/xxl` 断点优化为 8 列；中屏默认 4 个字段，小屏降为 2 个字段；宁可换行，不允许把输入框压缩成不可用的小块。
  - 当前默认收起态显示 5 个筛选字段；`.ant-pro-query-filter-actions` 的父级 `ant-col` 必须占用剩余空间、靠右、底部对齐输入框，不能强制占满整行导致少字段页面也多出一行按钮。
  - `fieldCount` 只作为字段列数优化，不作为布局兜底；固定 7 字段页面在缩放后的 2K 视口下应优先让 7 个字段占满首行、动作区自然落到搜索卡片右下角，不要让字段和动作按钮挤在同一小列里。
  - 查询控件的最小宽度只能在当前栅格宽度内生效，不能用硬 `min-width` 撑破 Ant Design 栅格；否则 1K 或浏览器缩放后的 2K 会把动作区挤到表格上方。
  - 不允许在页面私有 CSS 中移动 `.ant-pro-query-filter-actions`；动作区位置只能通过全局页面级 ProTable 样式统一维护。
  - 日期范围、金额区间、余额区间、库存区间等长控件默认占 2 个筛选格；普通输入框也要保留最小可用宽度。
  - 弹窗内小表格、纯明细表、无查询条件表格等确有理由的场景可显式 `search={false}`，但不要另起一套页面内筛选布局。
  - 同一业务指标的最小/最大查询条件统一做成一个区间字段，例如余额、金额、库存数量；优先使用 Ant Design 原生组合控件和默认输入框样式，不自定义特殊容器，不使用假的禁用输入框，前端提交时再拆成后端需要的最小/最大参数。
  - 三端前端表格操作列同一行超过 2 个操作时，最多保留 2 个高频操作按钮直接展示，其余操作使用 Ant Design `Dropdown` 收进“更多”下拉菜单，不要横向平铺 3 个及以上文字按钮。
  - 三端前端表格操作列的行内操作和“更多”下拉菜单项默认只展示文字，不加操作图标；“更多”作为下拉触发器必须使用 Ant Design 小下箭头提示可展开。

### 三端可搜索选择器

- 位置：`react-ui/src/utils/selectSearch.ts`
- 导出：
  - `SEARCHABLE_SELECT_PROPS`
  - `SEARCHABLE_TREE_SELECT_PROPS`
  - `filterSelectOption`
  - `filterTreeSelectNode`
- 当前用途：
  - 管理端普通 `Select`、`ProFormSelect`、`TreeSelect`、`ProFormTreeSelect`。
  - ProTable 中 `valueType: 'select'` 或 `valueEnum` 生成的查询下拉。
  - 当前先服务 `react-ui` 管理端；后续拆出 `seller-ui` / `buyer-ui` 后沿用同一套模糊搜索规则。
- 复用规则：
  - 三端前端所有业务选择器默认必须可模糊搜索，支持按 label、value/code、title、text、name 等文本匹配。
  - 新增选择器优先使用 `fieldProps={SEARCHABLE_SELECT_PROPS}` 或 `fieldProps={SEARCHABLE_TREE_SELECT_PROPS}`；普通 Ant Design 组件使用 `<Select {...SEARCHABLE_SELECT_PROPS} />` 或 `<TreeSelect {...SEARCHABLE_TREE_SELECT_PROPS} />`。
  - 如果已有 `fieldProps` 包含 `onChange`、`options`、`treeData`、`defaultValue` 等配置，必须先展开公共搜索配置，再保留原有配置。
  - 操作列“更多”这种命令 `Dropdown` 不按可搜索选择器处理。

### 页面级标题

- 位置：`react-ui/src/global.css`
- 当前用途：
  - 全局隐藏 `PageContainer` 自动生成的页面级标题行。
  - 全局让 `PageContainer` 内容区、直接承载的 `ProTable`、卡片和 Tabs 按剩余视口高度撑满。
  - 全局用高度预算控制列表页：表格主数据区至少保留 60% 的可用高度，上半部分根据视口高度自动缩小 padding、行距和区块间距。
  - 全局压缩 ProTable 查询按钮行，筛选区高度随字段行数自适应，避免按钮行撑出大块空白。
  - 全局固定 ProTable 表头，表格数据滚动时表头不跟随数据行一起滚动。
  - 全局让 ProTable 分页器压在主数据块底部，并在分页器上方显示浅色横向分隔线。
  - `urili-fill-table` 只提供页面级 ProTable 撑满布局，必须保留 Ant Design Pro 默认白色卡片。
  - `upstream-fill-table` 只用于上游系统详情内部嵌套清单，允许移除内部 ProTable 卡片边框、阴影、toolbar 和内边距。
- 复用规则：
  - 三端前端页面顶部不再展示独立页面标题，也不要在筛选区、表格区上方手写页面级标题。
  - 后续新增页面继续使用该全局规则；需要表达局部区域时，使用表格、卡片、抽屉、弹窗自身标题。
  - 三端前端页面的主数据块、表单块或占位块必须填满可视区域；内容不足时由分块高度补齐，不要让页面下半屏暴露空白背景。
  - 三端前端列表页表格主数据区至少保留 60% 的可用高度；滚动只发生在数据体区域，表头必须固定在表格顶部；筛选区不能因为查询按钮或固定间距单独撑出空白行。
  - 新增页面优先使用 `PageContainer` 直接承载 `ProTable`、`Card`、`ProCard` 或 `Tabs`；如果确实需要额外包裹层，包裹层也要保持纵向 flex 撑满。
  - 页面级列表不要用私有 CSS 去掉 ProTable 卡片；来源商品库、商城商品列表、主体管理、财务列表等主列表应保留默认白色卡片。
  - 需要复用“领星 SKU 同步清单”这类嵌在详情面板里的无边框列表时，才在 ProTable 上加 `className="upstream-fill-table"`；不要继续全局覆盖所有 `.ant-table-container`、`.ant-table-content` 或 `.ant-table-body`。
  - 表格分页器必须留在主数据块底部，上方保留分隔线；不要在单页里把分页器改回跟随数据行的位置。
  - 全局 Tabs 撑满规则必须保留 `.ant-tabs-tabpane-hidden { display: none; }`，避免未激活页签内容和当前页签叠加显示。

### PartnerManagementPage

- 位置：`react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx`
- 当前用途：
  - 管理端卖家管理页面
  - 管理端买家管理页面
- 复用规则：
  - 只复用主体资料管理的 UI 结构、附件控件、账号弹窗和字典 option 转换。
  - 卖家入口必须在 `react-ui/src/pages/Seller/`。
  - 买家入口必须在 `react-ui/src/pages/Buyer/`。
  - 不要恢复 `CustomerManagementPage(kind)` 作为主入口。
  - 管理端标准列表模板通过 `PartnerModuleConfig.listTemplate='standard'` 启用；当前已在卖家管理和买家管理启用并完成浏览器验收。
  - 标准列表把编号/代码、名称/简称、登录账号/等级合并为上下两行展示，搜索字段仍保留原独立字段，不把后端查询参数合并。
  - 标准模板操作列使用不换行操作组，保持“编辑 / 账号 / 更多”一行显示；买家可额外保留“充值”占位列。
  - 筛选折叠状态可通过 `PartnerModuleConfig.searchStorageKey` 固定到业务 key，避免路由变化后丢失用户的展开/收起状态。
  - 账号入口权限通过 `PartnerModuleConfig.accountPermissions.list` 控制；卖家和买家均已配置各自的 `*:admin:account:list`，未配置时只作为历史兼容回退到旧的 `${moduleKey}:admin:query`。
  - 复制到买家时只替换端类型、文案、字段配置、权限标识和 service；不要把买家充值占位或买家字段反向带回卖家。
  - `react-ui/scripts/check-partner-management-template.mjs` 已作为管理端 seller/buyer 同构模板守卫接入 `npm run lint`；后续改 `Seller/index.tsx`、`Buyer/index.tsx`、`react-ui/src/services/seller/seller.ts`、`react-ui/src/services/buyer/buyer.ts` 或 `PartnerManagement` 公共组件时，必须保持页面只通过共享模板配置接入，service 不串端、不调用 `/system/*`，账号、角色、菜单、部门、日志、会话和免密能力配置齐全。

### PartnerAccountModal / PartnerAccountRoleModal

- 位置：
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerAccountRoleModal.tsx`
  - `react-ui/src/services/seller/seller.ts`
  - `react-ui/src/services/buyer/buyer.ts`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java`
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`
- 当前用途：
  - 管理端卖家账号列表、新增、编辑、部门树绑定、人工临时密码重置、强制踢出。
  - 管理端买家账号列表、新增、编辑、部门树绑定、人工临时密码重置、强制踢出。
  - 管理端卖家/买家端账号角色绑定。
  - 管理端卖家/买家账号级免密代入。
- 复用规则：
  - 后续同类主体账号管理 UI 先复用 `PartnerAccountModal`，通过 `PartnerModuleConfig.services` 注入接口，不要在页面内拼接 seller/buyer 路径。
  - 当前管理端 UI 只接入人工输入 5-20 位临时密码的 `resetPwd`；前端 seller/buyer service 不再导出 `resetDefaultPwd` 默认密码重置函数，后端 `AdminSellerController` / `AdminBuyerController` 也不得恢复该路由。创建新账号时默认密码 `U12346` 的规则仍保留，不等同于重置已有账号为默认密码。
  - 新增、编辑、重置密码和账号角色分配入口通过 `PartnerModuleConfig.accountPermissions` 控制；卖家使用 `seller:admin:account:*`，买家使用 `buyer:admin:account:*`，不得在公共组件内回退混用主体权限。
  - 账号“分配角色”入口必须同时要求 `*:admin:role:query`、`*:admin:account:role:query` 和 `*:admin:account:role:edit`；因为账号角色回显接口会读取端内角色列表，前端按钮显隐必须和后端 `accountRoles` 接口权限一致。`AdminAccountPermissionUiContractTest` 与 `check-partner-management-template.mjs` 已固定该组合。
  - 账号角色绑定先复用 `PartnerAccountRoleModal`，只替换 `getAccountRoles` / `assignAccountRoles` service。
  - 已确认的模式可模板化复制：卖家先做一套，买家只替换字段配置和 service。
  - 账号级免密代入通过 `PartnerModuleConfig.services.directLoginAccount` 注入，seller 使用 `/seller/admin/sellers/{sellerId}/accounts/{accountId}/directLogin`，buyer 使用 `/buyer/admin/buyers/{buyerId}/accounts/{accountId}/directLogin`；不要在公共组件内写 seller/buyer 路径分支。
  - 账号级免密确认框必须填写代入原因；前端不展示或记录免密 token 明文，后端返回链接只用于打开目标端短时入口。
  - `SellerServiceImplTest` / `BuyerServiceImplTest` 已守住账号级免密 service 行为：成功票据必须绑定传入的端内账号，跨主体账号必须拒绝，主体停用时必须拒绝。
  - `SellerServiceImplTest` / `BuyerServiceImplTest` 已守住免密票据消费前状态复验：票据生成后如果主体停用、账号停用、账号锁定、账号不存在或账号已改绑到其他主体，direct-login 入口必须在 `PortalDirectLoginSupport` 标记票据 `USED` 之前拒绝，且不得创建端登录 session。
  - `SellerServiceImplTest` / `BuyerServiceImplTest` 已按同一模板补充端账号生命周期守卫：账号新增密码加密和默认 `STAFF`、部门归属校验、人工临时密码重置、停用账号强踢、登录成功最后登录/会话/日志写入、停用账号登录拒绝、当前账号会话范围和 current 标记。后续维护必须先改 seller 模板并验收，再复制 buyer，只替换 terminal、字段、service 和文案。
  - 管理端重置 seller/buyer 端账号密码后必须强制踢出该端账号既有会话，并通过 `PortalTokenSupport.deleteLoginTokens` 删除对应 terminal 的 Redis token；当前只允许人工临时密码 `resetPwd` 入口，默认密码 `resetDefaultPwd` 和主体主账号默认重置入口都不得恢复。`SellerServiceImplTest` / `BuyerServiceImplTest` 已按“seller 模板先验收、buyer 同构复制”覆盖该规则。
  - 管理端账号弹窗“重置密码”入口必须使用人工输入的临时密码并调用 `resetAccountPassword` / 后端 `resetPwd`，不得静默调用 `resetAccountDefaultPassword`。临时密码长度为 5-20 位，前端输入框和后端 `PartnerSupport.normalizeTemporaryPassword(...)` 都必须校验；成功提示不得回显明文密码。
  - `SellerServiceImpl` / `BuyerServiceImpl` 已补 Service 级主账号唯一性兜底：手工新增第二个 `OWNER` 必须拒绝；账号编辑时不采纳前端 payload 的 `accountRole`，始终保留当前账号角色。`SellerServiceImplTest` / `BuyerServiceImplTest` 已按同一模板覆盖该规则；数据库层 OWNER 唯一约束也已通过独立 DDL 落地。
  - `PartnerSupport.normalizeAccountRole(...)` 集中维护端账号角色白名单，当前合法值为 `OWNER` / `ADMIN` / `STAFF`；seller/buyer service 必须调用该 helper，不要在两端各自散写角色判断。
  - `RuoYi-Vue/sql/20260605_terminal_owner_account_unique_constraint.sql` 已补数据库层 OWNER 唯一约束：`seller_account.owner_unique_seller_id` / `buyer_account.owner_unique_buyer_id` 为生成列，分别通过 `uk_seller_account_owner` / `uk_buyer_account_owner` 限制每个主体只能有一个 `OWNER`。初始化 SQL 和三端迁移 SQL 已同步该约束。
  - seller 账号锁定/解锁已作为标准模板落地，buyer 已按同一模板复制完成：`seller_account.lock_status` / `buyer_account.lock_status` 与 `lock_reason` 均独立于账号 `status`；管理端权限点分别为 `seller:admin:account:lock` 和 `buyer:admin:account:lock`。锁定必须填写原因并强踢该端账号会话，解锁只清锁定字段且不恢复旧会话。前端通过 `PartnerModuleConfig.services.lockAccount` / `unlockAccount` 可选能力展示锁定列和“更多”操作，不在公共组件内写 seller/buyer 路径分支。
  - seller 与 buyer 账号锁定低权限负向验收均已跑通：临时管理端角色只给对应 `*:admin:account:list`，不给 `*:admin:account:lock`，必须能打开账号弹窗、看到锁定状态列，但不能看到锁定/解锁动作，后端锁定/解锁接口必须返回 `403` 且账号锁定字段不变。后续复制其它同构账号能力时，继续先验收一端模板，再只替换 terminal、权限、字段和 service。
  - `SellerAdminPermissionContractTest` / `BuyerAdminPermissionContractTest` / `AdminAccountPermissionUiContractTest` / `PermissionServiceAccountPermissionTest` 已共同守住账号锁定权限契约：controller 必须使用 `*:admin:account:lock`，前端必须声明 `accountPermissions.lock` 并通过 `access.hasPerms(accountPermissions.lock...)` 控制入口，权限服务不能把主体权限或角色权限误当成账号锁定权限。
  - 该测试只守 service 数据范围，不替代管理端接口 `@PreAuthorize`、菜单按钮权限、审计日志和浏览器烟测。
  - `SellerPortalPermissionServiceImplTest` / `BuyerPortalPermissionServiceImplTest` 已守住账号角色绑定 service 行为：账号必须属于当前主体，角色必须属于当前主体；清空角色只删除当前端账号绑定，不批量插入空角色。
  - 后续改造 `assignAccountRoles(...)` 时继续复用 `PortalPermissionSupport.sanitizeIds(...)`，不要在 seller/buyer service 中各自重写 roleId 过滤、去重规则。
  - 账号弹窗表格列宽和 Modal 宽度已按 1366 桌面视口验收；后续复制买家或增加列时，必须继续验证 `bodyOverflowX=false`、`modalOverflowX=false`、`tableOverflowX=false`，不要通过隐藏横向滚动条掩盖列宽问题。
  - 账号新增/编辑表单 Modal 使用 `forceRender` 保证 `useForm` 已挂载；后续拆分账号表单时需保留这一约束或改成子组件内部创建 form。
  - 账号行直接展示最多两个高频操作；更多低频操作继续收进 Ant Design `Dropdown`。
  - `PartnerAccountModal.tsx` 已接近 500 行，后续继续扩账号管理时优先拆账号表格、账号表单或操作区，不要继续把独立配置页塞进该文件。

### PartnerDeptModal

- 位置：
  - `react-ui/src/components/PartnerManagement/PartnerDeptModal.tsx`
  - `react-ui/src/services/seller/seller.ts`
  - `react-ui/src/services/buyer/buyer.ts`
- 当前用途：
  - 管理端维护卖家端部门 `seller_dept`。
  - 管理端维护买家端部门 `buyer_dept`。
  - 支持端内部门列表、新增、编辑、删除和上级部门选择。
- 复用规则：
  - 后续端内部门维护继续复用 `PartnerDeptModal`，不要在卖家页、买家页分别复制部门表格和表单。
  - 卖家/买家只通过 `PartnerModuleConfig.services` 替换 `listDepts`、`getDeptTree`、`addDept`、`updateDept`、`removeDept`。
  - 主体行“部门”入口放在“更多”菜单内，不直接扩宽主体列表操作列。
  - 不允许使用系统部门 `/system/dept` 或 `sys_dept` 维护卖家端、买家端内部部门。

### PartnerMenuModal

- 位置：
  - `react-ui/src/components/PartnerManagement/PartnerMenuModal.tsx`
  - `react-ui/src/services/seller/seller.ts`
  - `react-ui/src/services/buyer/buyer.ts`
- 当前用途：
  - 管理端维护卖家端全局菜单 `seller_menu`。
  - 管理端维护买家端全局菜单 `buyer_menu`。
  - 支持端内菜单列表、新增、编辑、删除和上级菜单选择。
  - 维护菜单名称、菜单类型、路由、组件、权限标识、显示状态、菜单状态和缓存/外链字段。
- 复用规则：
  - 后续端内菜单维护继续复用 `PartnerMenuModal`，不要复制系统菜单 `/system/menu` 页面。
  - 卖家/买家只通过 `PartnerModuleConfig.services` 替换 `listMenus`、`getMenu`、`addMenu`、`updateMenu`、`removeMenu` 和 `getMenuTree`。
  - 管理端工具栏“菜单配置”入口按 `seller:admin:menu:list` / `buyer:admin:menu:list` 权限展示。
  - 端内菜单不允许写入 `sys_menu`，也不允许调用 `/system/menu` 接口维护卖家端、买家端菜单。
  - 当前菜单配置是端维度全局配置，不挂在单个卖家/买家主体下；角色弹窗再按主体绑定角色菜单权限。

### PartnerRoleModal

- 位置：
  - `react-ui/src/components/PartnerManagement/PartnerRoleModal.tsx`
  - `react-ui/src/services/seller/seller.ts`
  - `react-ui/src/services/buyer/buyer.ts`
- 当前用途：
  - 管理端维护某个卖家主体下的端内角色 `seller_role`。
  - 管理端维护某个买家主体下的端内角色 `buyer_role`。
  - 维护角色名称、权限字符、显示顺序、状态、备注和菜单权限树。
  - 角色菜单关系分别写入 `seller_role_menu` / `buyer_role_menu`。
- 复用规则：
  - 后续端内角色维护继续复用 `PartnerRoleModal`，不要在卖家页、买家页分别复制角色表格和表单。
  - 卖家/买家只通过 `PartnerModuleConfig.services` 替换 `listRoles`、`getRole`、`addRole`、`updateRole`、`changeRoleStatus`、`removeRoles`、`getMenuTree` 和 `getRoleMenuTree`。
  - 主体行“角色”入口放在“更多”菜单内，按 `seller:admin:role:list` / `buyer:admin:role:list` 权限展示。
  - 端内角色不允许写入 `sys_role`，端内菜单权限树不允许读取 `/system/menu`；必须分别读取 `seller_menu` / `buyer_menu`。
  - 当前角色维护是主体维度的角色弹窗；端维度菜单配置复用 `PartnerMenuModal`。
  - 角色新增或编辑提交 `menuIds` 时，后端必须先全量校验所有 ID 均存在于当前端对应菜单表；不得依赖 `insert into ... select ... join seller_menu/buyer_menu` 静默丢弃不存在或跨端菜单 ID。

### PartnerAuditModal

- 位置：
  - `react-ui/src/components/PartnerManagement/PartnerAuditModal.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx`
  - `react-ui/src/services/seller/seller.ts`
  - `react-ui/src/services/buyer/buyer.ts`
- 当前用途：
  - 管理端查看卖家端审计数据。
  - 管理端查看买家端审计数据。
  - 统一承载登录日志、操作日志、免密票据三个只读 tab。
  - 支持工具栏全局审计入口，也支持主体行内“更多 -> 审计”按当前主体过滤。
  - 支持账号行“更多 -> 审计”入口，按当前主体和当前账号固定过滤。
- 复用规则：
  - 后续同类主体审计 UI 继续复用 `PartnerAuditModal`，只通过 `PartnerModuleConfig.services` 替换 `listLoginLogs`、`listOperLogs`、`listDirectLoginTickets`。
  - 账号行审计入口不得新增第二套权限点；入口显隐复用登录日志、操作日志、免密票据任一审计权限，三个 tab 继续按各自权限独立显隐。
  - 账号级登录日志和操作日志查询必须同时传 `subjectId + accountId`；账号级免密票据查询必须同时传 `targetSubjectId + targetAccountId`。前端 helper 不得在缺少主体 ID 时发裸 `accountId` / `targetAccountId`。
  - 审计列表默认只读，不在该弹窗内增加删除、清空、导出等写操作；如后续需要导出，应先补权限点和审计记录。
  - 审计详情字段使用表格展开行展示，不继续挤压主表列宽；登录日志展示登录地点、浏览器、操作系统和提示，操作日志展示请求地址、操作 IP、操作地点、方法名和异常信息，免密票据展示目标端、签发人 ID、使用 IP、更新信息、代入原因和备注。
  - 审计详情字段已按“先 seller 模板、后 buyer 验收”完成两端浏览器验证；后续新增字段必须同时验证 seller/buyer 入口，而不是只看共用组件代码。
  - 免密票据 tab 必须按 `seller:admin:ticket:list` / `buyer:admin:ticket:list` 显隐；全局审计入口只有在登录日志、操作日志或免密票据任一审计权限存在时展示。
  - seller/buyer 低权限账号如果只有登录日志/操作日志权限，没有免密票据权限，审计弹窗必须只展示登录日志和操作日志 tab；行内“更多”菜单不得展示免密代入入口。
  - 免密票据查询必须在后端 service 层强制 terminal，不能只靠前端传参区分 seller/buyer。
  - `seller_oper_log` / `buyer_oper_log` 已接入第一批真实写入链路；后续端内业务接口继续使用 `@PortalLog` 写入端内日志，并复用 `PortalOperLog` 查询对象和该弹窗。
  - 已确定的 seller/buyer 同构 UI 模式直接模板化复制：seller 做好一套后，buyer 只替换字段配置、权限标识、接口前缀和 service。

### PortalSubjectProfile

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalSubjectProfile.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java`
- 当前用途：
  - 卖家端 `/seller/profile` 返回当前 token 绑定卖家主体的只读资料。
  - 买家端 `/buyer/profile` 返回当前 token 绑定买家主体的只读资料。
  - 端内 profile 接口从 `PortalSessionContext.requireSession(...)` 推导主体 ID，不接收前端传入 `sellerId` / `buyerId`。
- 复用规则：
  - 后续端内真实业务接口继续按该模式从端 token 推导主体范围。
  - 端内 profile 返回 `PortalSubjectProfile`，不要直接返回管理端 `Seller` / `Buyer` 全对象，避免暴露 `createBy`、`updateBy`、`remark` 等后台字段。
  - 端内真实业务接口继续接入 `@PortalPreAuthorize` 和 `@PortalLog`，不要只在前端隐藏入口。

### PortalAccountProfile

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalAccountProfile.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java`
- 当前用途：
  - 卖家端 `/seller/account/profile` 返回当前 token 绑定卖家端账号的只读资料。
  - 买家端 `/buyer/account/profile` 返回当前 token 绑定买家端账号的只读资料。
  - 卖家端 `/seller/accounts` 返回当前 token 绑定卖家主体下的端账号只读列表。
  - 买家端 `/buyer/accounts` 返回当前 token 绑定买家主体下的端账号只读列表。
  - 端内账号资料接口从 `PortalSessionContext.requireSession(...)` 推导主体 ID 和账号 ID，不接收前端传入 `sellerId` / `buyerId` / `accountId`。
- 复用规则：
  - 后续端内“当前账号”类接口继续复用该 DTO，不要直接返回 `SellerAccount` / `BuyerAccount` 全对象。
  - 端内账号资料响应不得包含 `password`、`createBy`、`updateBy`、`remark` 等后台或敏感字段。
  - 账号查询必须在 Service 层校验账号归属当前主体，不能只按 `accountId` 单点查询后返回。
  - 端内账号列表必须从端 token 推导主体范围，不允许前端传入 `sellerId` / `buyerId` 查询其他主体账号。
  - 端内账号列表必须通过 `@PortalPreAuthorize` 校验 `seller:account:list` / `buyer:account:list`，不要只做登录态校验。

### PortalDeptProfile / PortalRoleProfile

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalDeptProfile.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalRoleProfile.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java`
- 当前用途：
  - 卖家端 `/seller/depts` 返回当前 token 绑定卖家主体下的端内部门只读列表。
  - 卖家端 `/seller/roles` 返回当前 token 绑定卖家主体下的端内角色只读列表。
  - 买家端 `/buyer/depts` 返回当前 token 绑定买家主体下的端内部门只读列表。
  - 买家端 `/buyer/roles` 返回当前 token 绑定买家主体下的端内角色只读列表。
  - 四个接口都从 `PortalSessionContext.requireSession(...)` 推导主体 ID，不接收前端传入 `sellerId` / `buyerId`。
- 复用规则：
  - 后续端内部门、角色下拉或只读选择器优先复用这两个 DTO，不要直接返回 `PortalDept` / `PortalRole` 原始对象。
  - 端内部门/角色只读列表必须通过 `@PortalPreAuthorize` 校验 `seller:dept:list` / `seller:role:list` / `buyer:dept:list` / `buyer:role:list`。
  - 响应不得包含 `password`、`createBy`、`updateBy`、`delFlag`、`remark` 等后台或敏感字段。
  - 同构端内接口继续按 seller 样板复制到 buyer，只替换 terminal、权限标识、service 和文案。

### 端内当前账号日志只读接口

- 位置：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/ISellerService.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/IBuyerService.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`
  - `react-ui/src/services/portal/session.ts`
  - `react-ui/src/types/seller-buyer/party.d.ts`
- 当前用途：
  - 卖家端 `/seller/account/login-logs` 返回当前卖家端账号自己的登录日志。
  - 卖家端 `/seller/account/oper-logs` 返回当前卖家端账号自己的操作日志。
  - 买家端 `/buyer/account/login-logs` 返回当前买家端账号自己的登录日志。
  - 买家端 `/buyer/account/oper-logs` 返回当前买家端账号自己的操作日志。
  - 前端统一通过 `sellerPortalSessionService.getLoginLogs` / `getOperLogs` 和 `buyerPortalSessionService.getLoginLogs` / `getOperLogs` 调用。
- 复用规则：
  - 端内当前账号日志接口必须优先使用 session-scoped Service 方法，例如卖家模板 `selectSellerOwnLoginLogList(PortalLoginSession, PortalLoginLog)` / `selectSellerOwnOperLogList(PortalLoginSession, PortalOperLog)` 和买家模板 `selectBuyerOwnLoginLogList(PortalLoginSession, PortalLoginLog)` / `selectBuyerOwnOperLogList(PortalLoginSession, PortalOperLog)`。
  - Service 内基于 `PortalSessionContext.requireSession(...)` 传入的 `PortalLoginSession` 强制设置 `subjectId` 和 `accountId`；Controller 不承担数据范围安全边界，只负责鉴权、取 session、分页和返回表格。
  - Controller 返回前必须把内部日志模型投影为端内可见 DTO：登录日志使用 `PortalOwnLoginLogProfile`，操作日志使用 `PortalOwnOperLogProfile`；不得直接 `getDataTable(select*Own*LogList(...))` 返回 `PortalLoginLog` / `PortalOperLog`。
  - 端内当前账号日志响应不得返回 `subjectId`、`accountId`、`tokenId`、`directLoginTicketId`、`actingAdminId`、`actingAdminName`、`directLoginReason`、`operParam`、`jsonResult`、Java `method` 或 Redis key；管理端审计接口仍保留完整结构化 direct-login 审计字段。
  - 端内 direct-login 登录日志可显示中性文案“免密登录成功/免密登录失败”，不得把 `ticketId`、acting admin 或 reason 拼进 `msg` 返回给卖家/买家端。
  - 端内日志响应投影时必须保留原 PageHelper 分页 total：先对内部日志列表调用 `getDataTable(rawLogs)`，再替换 `rows` 为端内可见 DTO 列表。
  - 前端传入的 `subjectId`、`accountId` 只能作为无效输入处理，不能扩大查询范围。
  - 端内日志筛选只保留当前 mapper 明确支持的内容筛选：登录日志保留 `userName`、`ipaddr`、`status`、`params.beginTime`、`params.endTime`；操作日志保留 `title`、`operName`、`status`、`params.beginTime`、`params.endTime`。
  - 当前账号日志接口分页最大 `pageSize` 为 100；后续如抽公共端内分页工具，应保留该上限。
  - 查看操作日志本身会写入端内操作日志；端内日志页面不要做高频自动轮询。
  - 管理端审计列表继续使用 `/seller/admin/sellers/*` 和 `/buyer/admin/buyers/*` 下的日志接口；不要把端内当前账号日志接口当成管理端全量审计接口。
  - 后续端内安全中心或个人中心日志页只读取当前账号日志，不允许筛选其他主体或其他账号。
  - 当前 seller/buyer 双端均已完成 session-scoped Service 模板；后续同构复制只替换 terminal、service、controller、mapper、测试名和文案，不重新设计接口、权限、SQL 或前端入口。

### 端内当前账号会话只读接口

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalSessionProfile.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java`
  - `react-ui/src/services/portal/session.ts`
  - `react-ui/src/types/seller-buyer/party.d.ts`
- 当前用途：
  - 卖家端 `/seller/account/sessions` 返回当前卖家端账号自己的登录会话。
  - 买家端 `/buyer/account/sessions` 返回当前买家端账号自己的登录会话。
  - 前端统一通过 `sellerPortalSessionService.getSessions` / `buyerPortalSessionService.getSessions` 调用。
- 复用规则：
  - 端内当前账号会话接口必须从 `PortalSessionContext.requireSession(...)` 推导 `subjectId` 和 `accountId`，不允许前端传主体 ID 或账号 ID 扩大查询范围。
  - 响应不得返回 `tokenId`、JWT、Redis key、密码密文或其他敏感字段。
  - `PortalSessionProfile.tokenId` 只允许后端内部用于判断 `current`，必须保持不输出给前端。
  - `PortalSessionProfileTest` 固定 `tokenId` JSON 脱敏契约；后续调整会话 DTO 或复用该响应对象时必须保持该测试通过。
  - 当前账号会话接口分页最大 `pageSize` 为 100；后续如抽公共端内分页工具，应保留该上限。
  - 管理端强制踢出继续使用管理端 session 接口；端内当前账号会话接口只读，不承担踢出能力。

### 三端前端 session 基础层

- 位置：
  - `react-ui/src/access.ts`
  - `react-ui/src/app.tsx`
  - `react-ui/src/requestErrorConfig.ts`
  - `react-ui/src/services/session.ts`
  - `react-ui/src/services/session.js`
  - `react-ui/src/services/portal/session.ts`
  - `react-ui/src/services/portal/session.js`
  - `react-ui/src/types/seller-buyer/party.d.ts`
  - `react-ui/src/utils/remoteMenuStorage.ts`
  - `react-ui/src/utils/remoteMenuStorage.js`
  - `react-ui/src/utils/portalPaths.ts`
  - `react-ui/src/utils/portalPaths.js`
  - `react-ui/src/utils/portalRequest.ts`
  - `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx`
  - `react-ui/src/wrappers/RemoteMenuRouteGuard.js`
- 当前用途：
  - 管理端继续使用原有 `access_token` / `refresh_token` / `expireTime`，避免影响当前 admin 登录。
  - 卖家端、买家端预留独立 token key：`seller_*` / `buyer_*`。
  - `portal/session.ts` 统一封装卖家端、买家端登录、免密登录、主动退出、修改当前账号密码、`getInfo`、`getRouters`、主体资料、当前账号资料、端内账号只读列表、端内部门只读列表、端内角色只读列表、当前账号日志只读接口和当前账号会话只读接口。
  - `portalPaths.ts` / `portalPaths.js` 统一维护 `/seller` / `/buyer` 的端登录页、直登入口和 portal 路由识别。
  - `RemoteMenuRouteGuard.tsx` / `RemoteMenuRouteGuard.js` 负责管理端静态 seller/buyer 路由兜底权限和 portal 公开路径豁免；`services/session.ts` 中的 `RemoteMenuRouteGuard` 负责空权限 fail-closed。
  - `remoteMenuStorage.ts` / `remoteMenuStorage.js` 负责管理端远程菜单按 `admin` / `seller` / `buyer` scope 写入浏览器 sessionStorage。
  - `scripts/check-portal-token-isolation.mjs` 作为前端端内 token 静态守卫，已接入 `npm run lint`。
  - `scripts/check-portal-token-isolation.mjs` 同时作为前端 portal 请求身份范围参数静态守卫，防止 portal 页面或 service 绕过端 token，把 `sellerId`、`buyerId`、`subjectId`、`accountId`、`sellerAccountId`、`buyerAccountId`、`terminal` 等客户端身份范围字段作为请求参数发送。
  - `app.tsx` 和 `requestErrorConfig.ts` 的 401 处理通过 `getPortalTerminalFromApiUrl(...)` 区分 portal 请求和管理端请求：portal 401 只清对应 seller/buyer 端 token，不清管理端 `access_token` / `admin_remote_menu`，也不跳管理端登录页。
  - portal 401 会跳到对应端登录页并携带当前 portal 路由 `redirect` 参数；`app.tsx` 响应拦截器发现响应体 `code/errorCode = 401` 后必须 reject 原 response，`requestErrorConfig.ts` 的 BizError 401 和 HTTP 401 errorHandler 分支也必须 redirect 后继续 throw 原 error，避免业务页面继续按成功结果读取数据。
  - `portal-unauthorized-redirect.test.ts` 必须同时覆盖普通 portal API、普通 admin API 和 `/api/seller/admin/**` / `/api/buyer/admin/**`，防止后台 partner 管理接口被误归类为端内 portal 请求。
- 复用规则：
  - 后续三端物理拆分时，卖家端、买家端前端优先复用 `setTerminalSessionToken`、`getTerminalAccessToken`、`clearTerminalSessionToken`，不要重新设计 localStorage key。
  - 后续端内页面调用当前账号、主体资料、菜单、权限和退出登录时，优先复用 `sellerPortalSessionService` / `buyerPortalSessionService` 或底层 `portal*` 方法，不要在页面里直接拼 `/seller` / `/buyer` 路径。
  - `portal/session.ts` 的登录、免密登录和端内请求必须显式设置 `isToken: false`，避免全局请求拦截器注入管理端 token。
  - `/api/seller/**`、`/api/buyer/**` 的非 admin portal 请求发生 401 时，只能调用 `clearTerminalSessionToken('seller'|'buyer')`；`/api/seller/admin/**`、`/api/buyer/admin/**` 仍属于管理端后台接口，继续按 admin session 过期处理。
  - portal 401 跳转必须保留当前 portal 页面为 `redirect` 参数，且 `redirect` 后续仍由 portal 登录页校验必须属于当前 terminal；不要回退为裸 `history.replace(getPortalLoginPath(portalTerminal))`。
  - `request.responseInterceptors` 对响应体 401 只能在处理跳转后 reject/throw，`requestErrorConfig.ts` / `.js` 的 errorHandler 对 BizError 401 和 HTTP 401 也只能在处理跳转后 throw 原 error；不能 `return response` 或空 `return`，否则端内页面会继续执行成功分支。
  - `persistPortalLogin(result, expectedTerminal)` 必须校验后端返回 `terminal` 与当前 URL 端类型一致；响应缺 token、跨端或无效时只返回失败，不清理任何已有端内 token，不能把 seller token 写入 buyer key，也不能写入管理端 `access_token`，也不能清理响应声明的另一端 token。
  - 管理端免密登录响应 `DirectLoginResult` 只能对前端序列化 `token`、`ticketId`、`loginUrl`、`expireMinutes`、`expireTime`；目标账号内部字段如 `accountId`、`username` 只能供后端内部测试和链路使用，不得进入 JSON 响应。
  - `src/pages/Portal/**` 和 `src/services/portal/**` 禁止调用管理端 `getAccessToken`、`setSessionToken`、`clearSessionToken`，也不得出现裸 `access_token` / `portal_login_token`；新增端内前端能力后必须运行 `npm run guard:portal-token`。
  - `src/pages/Portal/**` 不得直接调用 `request(...)` 或硬编码 `/api/seller`、`/api/buyer`；页面必须通过 `PORTAL_SERVICE` 或 `@/services/portal/session` 统一出口调用端内接口。
  - 只要 `react-ui/src` 下仍存在 `.tsx` 和同名 `.js` sidecar，`scripts/check-portal-token-isolation.mjs` 必须同时检查 TS/TSX 与 JS 文件；不能只固定源码文件。
  - `scripts/check-portal-token-isolation.mjs` 必须同时检查 `portal/session.ts` / `portal/session.js` 的 `isToken:false`、参数清洗和端内请求出口规则，也必须同时检查 `portalPaths.ts` / `portalPaths.js` 的 seller/buyer 登录路由与 portal 路由识别。
  - `scripts/check-portal-token-isolation.mjs` 必须显式检查 `RemoteMenuRouteGuard.tsx/js`、`services/session.ts/js`、`remoteMenuStorage.ts/js`：seller/buyer 静态路由必须分别兜底到 `seller:admin:list` / `buyer:admin:list`，portal 登录/直登/工作台公开路径不得被后台菜单兜底权限误拦截，远程菜单缓存 key 必须按端 scope 区分，空 authority 必须返回 403。
  - 管理端发起免密登录时必须把 `config.moduleKey` 传给 `openPortalDirectLoginWindow(...)`；卖家/买家页面配置的 `moduleKey` 必须分别固定为 `seller` / `buyer`。
  - 受保护 portal 请求不得把 `sellerId`、`buyerId`、`subjectId`、`accountId`、`sellerAccountId`、`buyerAccountId` 作为 query/body/request params 发送；`terminal` 只允许作为前端本地端类型选择、URL 判断、`persistPortalLogin(..., expectedTerminal)` 和 `getTerminalAccessToken(terminal)` 的本地参数。
  - `portal/session.ts` 的日志、会话和 seller 商品列表查询必须使用 `sanitizePortalQueryParams(params)` 清洗参数；分页、日志安全筛选和商品 `categoryId` 这类业务参数可以保留，但端内数据范围必须由端 token 和后端 `PortalSessionContext` 推导。
  - `PORTAL_SCOPE_PARAM_KEYS` 必须包含 `terminal`；`terminal` 只允许作为前端本地端类型参数，不允许作为 portal query 参数传给后端决定数据范围。
  - 后续端内安全设置或个人中心修改密码时，优先调用 `sellerPortalSessionService.updatePassword` / `buyerPortalSessionService.updatePassword`；页面不要传主体 ID、账号 ID，也不要持久化旧密码、新密码或确认密码。
  - 后续端内安全中心或个人中心展示当前账号会话时，优先调用 `sellerPortalSessionService.getSessions` / `buyerPortalSessionService.getSessions`；页面不要传主体 ID、账号 ID、`tokenId` 或 Redis key。

### 管理端端内会话列表查询接口

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalSessionProfile.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/ISellerService.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/IBuyerService.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml`
  - `react-ui/src/components/PartnerManagement/PartnerSessionModal.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx`
  - `react-ui/src/services/seller/seller.ts`
  - `react-ui/src/pages/Seller/index.tsx`
  - `react-ui/src/services/buyer/buyer.ts`
  - `react-ui/src/pages/Buyer/index.tsx`
- 当前用途：
  - 管理端按卖家主体查看 seller 端 session 列表：`GET /seller/admin/sellers/{sellerId}/sessions/list`。
  - 管理端按卖家账号查看 seller 端 session 列表：`GET /seller/admin/sellers/{sellerId}/accounts/{accountId}/sessions/list`。
  - 管理端按买家主体查看 buyer 端 session 列表：`GET /buyer/admin/buyers/{buyerId}/sessions/list`。
  - 管理端按买家账号查看 buyer 端 session 列表：`GET /buyer/admin/buyers/{buyerId}/accounts/{accountId}/sessions/list`。
  - 响应继续复用 `PortalSessionProfile`，`tokenId` 只供后端内部使用，不输出给前端。
  - 管理端会话列表允许返回免密审计字段：`directLogin`、`directLoginTicketId`、`actingAdminId`、`actingAdminName`、`directLoginReason`；这些字段只用于管理端识别免密代入来源，不代表端内账号权限。
  - `PartnerSessionModal` 已作为 seller/buyer 管理端主体/账号会话列表 UI 模板。
- 复用规则：
  - 管理端会话列表接口只读，不承担踢出动作；强制踢出继续使用现有 DELETE 接口。
  - seller/buyer 管理端会话列表后端已形成标准模板；前端已以 seller UI 为标准模板复制 buyer，只替换配置和 service，不重新设计两套规则。
  - 管理端会话列表使用管理端若依权限，seller 复用 `seller:admin:forceLogout`，buyer 复用 `buyer:admin:forceLogout`，不要改成端内 `@PortalPreAuthorize`。
  - RuoYi `startPage()` 只作用于 Service 内第一条查询；列表 Service 方法不要先做会消耗分页的前置校验查询，数据范围必须写进最终 session 列表 SQL。
  - 管理端列表查询可以按主体 ID 或账号 ID 收窄范围，但仍不得输出 JWT、Redis key、密码、`directLoginToken` 或 session `tokenId`。
  - 管理端会话弹窗只展示状态、免密代入标识、acting admin、登录账号、登录 IP、登录时间、过期时间、退出时间；不得展示 `tokenId`、JWT、Redis key、Authorization header 或原始 token。
  - `PartnerSessionModal` 的免密代入标识必须复用原状态列承载，避免为了审计字段额外拉宽表格；后续 seller/buyer 同构页面继续复制该模板。
  - `SellerAdminPermissionContractTest` / `BuyerAdminPermissionContractTest` / `AdminAccountPermissionUiContractTest` / `PermissionServiceAccountPermissionTest` 已共同守住会话与强制踢出权限契约：controller 会话列表和强踢方法必须使用 `*:admin:forceLogout`，前端主体行和账号行必须通过 `access.hasPerms(\`${permPrefix}:forceLogout\`)` 控制“会话 / 强制踢出”，账号权限或主体编辑权限不得误授权强踢。
  - `AdminAccountPermissionUiContractTest`、`AdminDirectLoginPermissionContractTest` 和 `scripts/check-partner-management-template.mjs` 必须同时覆盖 `.tsx` 与 `.js` sidecar；新增管理端共享模板能力时，两类文件都要保持权限和 service 配置一致。
  - 旧目标追踪中“管理端前端 session 列表 UI 尚未接入 / buyer UI 仍未复制”的表述已过期；当前会话列表 UI 以 `PartnerSessionModal`、Seller/Buyer 配置和本台账为准。
  - 当前 `react-ui/` 仍是管理端验证入口；该基础层只为后续物理拆分降低重复改造，不代表现在立即复制 `seller-ui` / `buyer-ui`。

### 三端前端直登入口与端内工作台模板

- 位置：
  - `react-ui/src/pages/Portal/terminal.ts`
  - `react-ui/src/pages/Portal/DirectLogin/index.tsx`
  - `react-ui/src/pages/Portal/Home/index.tsx`
  - `react-ui/src/pages/Portal/Home/SellerProductSchemaPreview.tsx`
  - `react-ui/src/pages/Portal/Home/BuyerProductSchemaPreview.tsx`
  - `react-ui/config/routes.ts`
- 当前用途：
  - `/seller/direct-login` 和 `/buyer/direct-login` 共用同一页面消费管理端生成的一次性免密票据。
  - `/seller/portal` 和 `/buyer/portal` 共用同一工作台页面读取当前端主体、当前账号、端内账号、端内部门、端内角色和权限信息。
  - `/seller/portal` 展示卖家端商品发布准备卡片，`/buyer/portal` 展示买家端商品浏览准备卡片；两者均通过对应端 service 消费商品分类和 Schema 接口。
  - `PORTAL_META` 和 `PORTAL_SERVICE` 统一承载 terminal 到文案、首页路径和 service 的映射。
- 复用规则：
  - 已确认的 seller/buyer 同构前端按模板复制：卖家侧做成样板后，买家侧只替换 terminal、文案、路由、权限标识、字段配置和 service。
  - 直登页只能消费 `directLoginToken` 并写入对应端 token key；不能写入或覆盖管理端 `access_token`。
  - `react-ui/src/app.tsx` 必须把 portal 路由排除在管理端 `getUserInfo()`、动态菜单加载和管理端登录态重定向之外。
  - 管理端动态菜单页面未登录直达时，统一通过 `redirectToLogin()` 跳转 `/user/login?redirect=...`；不要让 `/partner/seller`、`/partner/buyer` 这类后端动态菜单路由在无管理端 token 时先落入静态 404。
  - 管理端登录成功后必须先把 `/getRouters` 返回的远程菜单写入 `setRemoteMenu(...)`；`setRemoteMenu(...)` 同步维护运行时内存和 `sessionStorage.admin_remote_menu`，避免首次登录后动态菜单和动态路由依赖刷新才出现。
  - 管理端清理 admin token 时必须同步清理 `admin_remote_menu`；不要让退出登录、token 过期或异常重定向后继续复用旧菜单。
  - 动态路由 patch 必须保持幂等，按 path 更新已有 `routes` / `children`，不要每次重新追加重复路由；也不要用 `history.go(0)` 或整页刷新作为动态菜单恢复手段。
  - 登录兜底只作用于非 portal 管理端路由；`/seller/direct-login`、`/buyer/direct-login`、`/seller/portal`、`/buyer/portal` 继续由端内 session 逻辑自行处理。
  - 直登页必须通过 `persistPortalLogin(result, expectedTerminal)` 校验后端返回的 `terminal` 与 URL 端类型一致，响应缺 token、跨端或无效时只返回失败，不清理任何已有端内 token；页面不要自行绕过持久化封装直接写 token，也不要清理响应声明的另一端 token。
  - 直登页加载态使用 Ant Design `Spin.description`，不要再使用已废弃的 `tip`，避免浏览器控制台警告干扰验收。
  - 卖家直登浏览器消费模板已按 `/seller/direct-login` 到 `/seller/portal` 验收：消费后必须只写入 seller 端 token，不写入 buyer token；seller token 调 seller `getInfo` / `getRouters` 应成功，调 buyer `getInfo` 应被业务拒绝；票据必须转为 `USED` 并保留代入原因。
  - 买家直登浏览器消费模板已按 `/buyer/direct-login` 到 `/buyer/portal` 验收：消费后必须只写入 buyer 端 token，不写入 seller token；buyer token 调 buyer `getInfo` / `getRouters` 应成功，调 seller `getInfo` 应被业务拒绝；票据必须转为 `USED` 并保留代入原因。
  - 工作台或后续端内页面必须通过 `getTerminalAccessToken(terminal)` 读取端 token，不要复用管理端 `getAccessToken()`。
  - 工作台内端内业务卡片必须从端 service 读取数据，不要在页面内手写 Authorization 或读取管理端 token。
  - 当前工作台是验证型入口；后续正式卖家端/买家端页面可以替换 UI，但必须保留端 token、端 service 和后端 `PortalSessionContext` 权限边界。

### PlannedPage

- 位置：`react-ui/src/pages/Common/PlannedPage/index.tsx`
- 当前用途：
  - 新业务菜单的临时占位入口。
  - 菜单种子已落地但业务表、接口、权限按钮和正式页面尚未确认时，避免前端动态路由导入失败。
  - `react-ui/src/services/session.ts` 动态路由导入目标页面缺失时，回退到该占位页。
- 复用规则：
  - 只允许作为短期占位页使用，不承载真实业务规则。
  - 后续某个菜单进入正式实现时，应替换为对应业务模块页面，不要在 `PlannedPage` 内按菜单名堆分支逻辑。

### 财务币种配置页面

- 位置：
  - `react-ui/src/pages/Finance/Currency/index.tsx`
  - `react-ui/src/pages/Finance/Currency/constants.ts`
  - `react-ui/src/services/finance/currency.ts`
  - `react-ui/src/types/finance/currency.d.ts`
- 当前用途：
  - 管理端“财务管理 / 币种配置”页面。
  - 维护可用币种、官方汇率、生效汇率、同步配置和同步日志。
- 复用规则：
  - 后续业务币种下拉统一调用 `getCurrencyOptions()`，不要直接读取 `currency_code` 字典。
  - 状态、调整方式、舍入方式、认证方式和同步状态继续集中维护在 `constants.ts`。
  - 业务模块只读取 `effectiveRate` / `effective_rate` 作为当前生效汇率；如果需要交易当时快照，业务流水表使用独立快照字段。

## 文件存储

### FileStorageService

- 位置：`RuoYi-Vue/ruoyi-framework/src/main/java/com/ruoyi/framework/file/FileStorageService.java`
- 默认实现：`RuoYi-Vue/ruoyi-framework/src/main/java/com/ruoyi/framework/file/LocalFileStorageService.java`
- COS 实现：`RuoYi-Vue/ruoyi-framework/src/main/java/com/ruoyi/framework/file/CosFileStorageService.java`
- COS 资源访问：`RuoYi-Vue/ruoyi-framework/src/main/java/com/ruoyi/framework/file/CosFileResourceController.java`
- 结果对象：`RuoYi-Vue/ruoyi-framework/src/main/java/com/ruoyi/framework/file/StoredFile.java`
- 当前配置：
  - 默认 `ruoyi.file-storage.type=local`
  - 本机或部署环境可通过 `RUOYI_FILE_STORAGE_TYPE=cos` 切换腾讯云 COS
- 当前用途：
  - `/common/upload`
  - `/common/uploads`
  - `/system/user/profile/avatar`
- 复用规则：
  - 后续业务上传图片、附件、凭证、面单、导入文件时，优先通过 `FileStorageService`。
  - COS 凭证只能来自环境变量或本机 `.env.local`，不要提交 `SecretId`、`SecretKey` 明文。
  - 持久化路径继续使用 `/profile/...`，COS 访问地址由后端生成或重定向，避免调用方直接拼接对象存储地址。
  - 卖家/买家附件新增或替换时必须先通过 `/common/upload` 进入 `FileStorageService`，业务表只保存 `/profile/...` 资源路径和附件元数据。
  - 旧 data URL 只允许作为存量兼容读取，不能作为新增或替换附件继续写入。

## 外部系统接入

### integration 上游系统管理模块

- 位置：`RuoYi-Vue/integration`
- 当前用途：
  - 管理领星主仓接入、授权状态、启停状态和真实同步。
  - 维护仓库、物流渠道、SKU 信息、SKU 仓库尺寸重量、SKU 库存同步清单和配对关系。
  - 记录外部请求日志、分项同步状态、同步批次统计和 SKU 配对审计事件。
- 复用规则：
  - 后续 WMS、ERP、物流、支付等外部系统接入优先按独立适配器接入，不要让适配器直接写商品、库存、订单、履约或财务事实表。
  - 上游同步清单只承载外部系统快照和可配对对象，不作为正式业务事实源。
  - 仓库、物流渠道、SKU 基础信息、SKU 仓库尺寸重量这类全量同步必须先写 staging 表，再通过 `source_payload_hash` / `wms_payload_hash` 做数据库差异合并；hash 未变化时不得更新主表业务字段或 `update_time`。
  - 上游全量快照中消失的数据标记为 `DISABLED`，不要直接删除；配对关系不自动删除。
  - SKU 库存是 10 分钟增量快照，只作为上游库存源头，不直接写平台可售库存。
  - 外部请求日志只追加，敏感字段必须脱敏。

### 来源商品库只读视图

- 位置：
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/controller/AdminSourceProductController.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/SourceProductReadModelService.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/domain/SourceProductItem.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/domain/query/SourceProductQuery.java`
  - `RuoYi-Vue/integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml`
  - `RuoYi-Vue/sql/20260607_source_product_read_model.sql`
  - `react-ui/src/pages/Product/SourceProductLibrary/index.tsx`
  - `react-ui/src/pages/Product/SourceProductLibrary/SourceProductDetailDrawer.tsx`
  - `react-ui/src/services/integration/sourceProduct.ts`
  - `react-ui/src/types/integration/source-product.d.ts`
  - `react-ui/src/services/integration/constants.ts`
- 当前用途：
  - 管理端「商品管理 / 来源商品库」首版只读列表。
  - 集中展示 `upstream_system_sku_candidate` 中各来源系统同步回来的 SKU 基础资料、识别码、分类、尺寸重量、申报信息、同步状态和配对结果。
  - 详情抽屉展示来源快照摘要，不向前端暴露原始 `source_payload_json`。
  - 列表和详情读取 `source_product_group` / `source_product_dimension_group` / `source_product_warehouse_detail` 读模型；`upstream_system_sku_candidate` 仍是上游快照明细源。
- 复用规则：
  - 来源商品库是上游 SKU 同步清单的聚合展示，不是商城商品事实源；不要为了页面再建一张重复来源商品表。
  - 来源商品库、商品列表来源绑定、快捷创建商品和外部按来源 SKU 查仓库能力必须复用 `sourceSkuGroupKey` 和来源商品读模型，不要在各自模块重新拼 `connection_code + master_sku` 聚合规则。
  - 读模型由 SKU 基础资料同步、WMS 尺寸同步、SKU 配对变更、来源连接仓库名变更后重建；前端列表请求不得回退到实时大聚合 SQL。
  - 首版接口复用 `product:list:list` 权限；新增导出、批量创建草稿、同步库存等动作前，必须单独补按钮权限和审计日志。
  - SKU 配对仍由「上游系统管理」维护，来源商品库只展示配对结果，不在页面内复制配对规则。
  - integration 相关来源系统、同步状态和配对状态选项复用 `react-ui/src/services/integration/constants.ts`，不要在上游系统页和来源商品库页分别维护。
  - 领星 `dangerousCargo` 保存来源 code；展示时 `1` 表示「普货」，`2-8` 才表示电池、液体、粉末、带磁等特殊属性。
  - `RuoYi-Vue/sql/20260607_source_product_read_model.sql` 是高影响增量读模型建表/回填脚本，不是 bootstrap baseline；必须保留显式 confirm-token guard、源表/源列前置校验、`repository_scope = 'OFFICIAL_MASTER'` 定向刷新和 `SqlExecutionGuardContractTest` 专项合同。
  - 读模型脚本不得先删除 `source_product_*` 再校验上游源表；必须在删除和回填前确认 `upstream_system_sku_candidate` / `upstream_system_connection` / `upstream_system_sku_pairing` 及关键列存在。
  - 来源商品读模型刷新必须先写 `tmp_source_product_*` 临时 staging 表；只有 staging 全部构建成功后，才允许在事务内删除并替换 `OFFICIAL_MASTER` 正式读模型范围。
  - `source_product_warehouse_detail` 的唯一键必须包含 `source_dimension_group_key`，避免同一连接和来源 SKU 的多尺寸候选行互相覆盖。
  - `source_product_group.search_text` / `source_product_dimension_group.search_text` 的聚合摘要必须带 `order by`，避免同批源数据重放后搜索摘要字符串顺序抖动。

### 来源仓库库存快照

- 位置：
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/controller/AdminSourceWarehouseStockController.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/domain/SourceWarehouseStockItem.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/domain/query/SourceWarehouseStockQuery.java`
  - `RuoYi-Vue/integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml`
  - `react-ui/src/pages/Inventory/SourceWarehouseStock/index.tsx`
  - `react-ui/src/services/integration/sourceWarehouseStock.ts`
  - `react-ui/src/types/integration/upstream-system.d.ts`
  - `react-ui/src/services/integration/constants.ts`
  - `RuoYi-Vue/sql/20260607_source_warehouse_stock_read_model.sql`
- 当前用途：
  - 上游系统管理内的「SKU库存同步清单」读取 `upstream_system_sku_inventory_snapshot`，作为来源仓库库存源头。
  - `UpstreamInventorySyncTask.sync()` 通过若依 Quartz 每 10 分钟同步已启用领星主仓库存，调用领星 `/integratedInventory/pageOpen`。
  - `business_menu_seed.sql` 中独立「来源仓库库存」菜单仍可后续承接跨模块库存查询，不等同于上游系统 Tab 内的源头快照。
  - 独立「来源仓库库存」列表读取 `source_warehouse_stock_group` / `source_warehouse_stock_detail` / `source_warehouse_stock_filter_metric` 永久读模型，按同一来源 SKU + 商品名合并展示，并展开结构化明细。
- 复用规则：
  - 来源仓库库存快照只代表上游返回的库存口径，不直接覆盖平台可售库存、订单占用库存或财务库存。
  - 恢复独立「来源仓库库存」菜单、导出、详情或批量操作前，必须复用该快照源，并补齐权限点、审计日志和数据边界。
  - integration 相关来源系统、同步状态、配对状态和库存口径选项复用 `react-ui/src/services/integration/constants.ts`，不要在页面内复制状态映射。
  - 来源仓库库存列表不得回退到每次请求实时聚合 `upstream_system_sku_inventory_snapshot`；查询走读模型，库存同步或连接变更后由读模型重建能力维护。
  - 来源仓库库存「来源主仓」「来源仓库」筛选必须复用 `source_warehouse_stock_filter_metric` 读模型选项接口，保持可搜索下拉；不要在页面内手写选项，也不要退回成模糊文本框。
  - 来源仓库库存数量区间筛选作用在 `source_warehouse_stock_group` 父级聚合库存字段上；总库存、可用库存、锁定库存、在途库存使用一个区间字段转后端 min/max 参数，不要拆成两个独立筛选项。
  - `source_warehouse_stock_*` 仅为可覆盖读模型，不是平台真实库存、库存总览、订单占用库存或库存流水事实表。
  - `RuoYi-Vue/sql/20260607_source_warehouse_stock_read_model.sql` 是高影响增量读模型建表/回填脚本，不是 bootstrap baseline；必须保留显式 confirm-token guard、源表/源列前置校验、`repository_scope = 'OFFICIAL_MASTER'` 定向刷新和 `SqlExecutionGuardContractTest` 专项合同。
  - 读模型脚本不得先删除 `source_warehouse_stock_*` 再校验上游源表；必须在删除和回填前确认 `upstream_system_sku_inventory_snapshot` / `upstream_system_connection` 及关键列存在。
  - 来源仓库库存读模型刷新必须先写 `tmp_source_warehouse_stock_*` 临时 staging 表；只有 detail/group/filter_metric staging 全部构建成功后，才允许在事务内删除并替换 `OFFICIAL_MASTER` 正式读模型范围。
  - `source_warehouse_stock_group.search_text` 的聚合摘要必须带 `order by`，避免同批源数据重放后搜索摘要字符串顺序抖动。

### SecretCipherSupport

- 位置：`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/SecretCipherSupport.java`
- 当前用途：
  - 使用环境配置中的主密钥加密保存领星 Key/Secret。
  - 加密保存汇率 API Key / Token。
  - 保存和展示时只暴露脱敏结果，不返回明文凭证。
- 复用规则：
  - 后续新增外部系统凭证时复用该加密支撑类或同等安全层，不要在业务表、SQL、日志、前端响应中保存明文密钥。
  - 缺少 `URILI_SECRET_ENCRYPTION_KEY` 时不允许保存或调用需要凭证的接口。

## 财务模块

### finance 币种配置模块

- 位置：
  - `RuoYi-Vue/finance`
  - `RuoYi-Vue/sql/currency_configuration_seed.sql`
  - `RuoYi-Vue/sql/20260604_currency_showapi_sync_migration.sql`
  - `RuoYi-Vue/sql/20260604_currency_rate_sync_job.sql`
  - `RuoYi-Vue/finance/src/main/java/com/ruoyi/finance/task/CurrencyRateSyncTask.java`
  - `RuoYi-Vue/finance/src/main/java/com/ruoyi/finance/task/CurrencyRateSyncSchedulePolicy.java`
- 当前用途：
  - 维护 `finance_currency` 平台可用币种。
  - 追加记录 `finance_currency_rate_history` 汇率历史。
  - 维护 `finance_currency_sync_config` 汇率同步设置。
  - 追加记录 `finance_currency_sync_log` 同步日志。
  - 固定接入 ShowAPI 银行汇率查询作为官方汇率来源。
  - ShowAPI 官方汇率取 `hui_out` 现汇卖出价，不取 `zhesuan` 中行折算价。
  - 使用 `rate_anchor_time` 按北京时间选择当天基准时间之后的第一条官方汇率。
  - 若依 Quartz 每分钟轻量检查币种汇率同步计划，到 `rate_anchor_time + 1 分钟` 才实际调用外部接口；无基准后数据时每 15 分钟重试一次，最多重试 4 次。
  - 简化后的同步设置固定 `sync_enabled = 1`，定时同步是否停用由配置 `status` 控制。
- 复用规则：
  - 后续金额、余额、账单、结算、订单折算等业务需要当前可用币种时，优先读取 `finance_currency` 中启用币种。
  - 官方汇率保存到 `official_rate`，业务当前使用 `effective_rate`。
  - 汇率历史和同步日志只追加，不覆盖。
  - 外部 API 适配层只返回官方汇率候选和官方汇率时间，不能绕过财务模块直接写订单、余额、结算等业务事实表。
  - ShowAPI `appKey` 只能通过后端配置接口加密保存，SQL、Markdown、日志和前端响应不得保存或展示明文。
  - 后续如果扩展汇率来源或任务调度策略，优先复用 `CurrencyRateSyncTask` 作为 Quartz 入口，不要在前端页面或 Controller 内做轮询重试。

### LingxingOpenApiClient

- 位置：`RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/lingxing/LingxingOpenApiClient.java`
- 当前用途：
  - 统一处理领星 OpenAPI 签名、超时、授权校验、仓库/物流渠道/SKU、SKU 仓库尺寸重量和 SKU 库存拉取。
  - 通过 `LingxingRequestLogger` 写入脱敏请求日志。
- 复用规则：
  - 后续领星相关同步能力继续从该适配器扩展，不要在 Service、Controller 或 Mapper XML 中散落签名和 HTTP 调用逻辑。
  - 如果新增订单、库存、费用等领域能力，适配器只返回外部响应，业务事实落库必须经过对应业务模块。

### 上游系统定时同步任务

- 位置：`RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/task/UpstreamSystemTask.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/task/UpstreamWarehouseSyncTask.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/task/UpstreamLogisticsChannelSyncTask.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/task/UpstreamSkuInfoSyncTask.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/task/UpstreamSkuDimensionSyncTask.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/task/UpstreamInventorySyncTask.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/task/UpstreamScheduledSyncExecutor.java`
- 当前用途：
  - 每个上游同步 job 使用独立 `@Component` 作为若依 Quartz 调用入口，避免所有任务挤在 `upstreamSystemTask` 一个 Bean 下。
  - `UpstreamWarehouseSyncTask.sync` / `UpstreamLogisticsChannelSyncTask.sync` / `UpstreamSkuInfoSyncTask.sync` 分别在每日 23:20、23:30、23:40 同步低频基础资料。
  - `UpstreamSkuDimensionSyncTask.sync` 每日 23:59 限速同步 SKU 仓库尺寸重量。
  - `UpstreamInventorySyncTask.sync` 每 10 分钟同步上游 SKU 库存快照。
  - `UpstreamScheduledSyncExecutor` 只承载公共调度边界：筛选启用的领星主仓、串行执行、汇总成功失败和抛出受控异常。
  - `UpstreamSystemTask` 只保留历史方法名兼容转发，新的 `sys_job.invoke_target` 不再指向它。
  - 复用 `IUpstreamSystemService` 分项同步方法，不新增第二套领星签名、请求日志或落库逻辑。
  - 定时任务入口统一调用 `IUpstreamSystemService.syncScheduled(...)`，同步状态和批次记录 `SCHEDULED`，并向 `upstream_system_request_log` 写入一条同步任务摘要日志。
- 复用规则：
  - 后续新增上游同步任务时，新建独立 task 组件，并复用 `UpstreamScheduledSyncExecutor`；不要继续把新方法塞回 `UpstreamSystemTask`。
  - 新增同步任务时必须通过 `syncScheduled(...)` 或同等的 service 调度入口写同步摘要日志，不要只写应用日志。
  - 不要在前端、Controller 或独立线程里轮询外部系统。
  - 定时任务登记使用若依 `sys_job`，让“系统监控 / 定时任务”统一启停、手动执行和查看日志。

### 上游系统 React 页面组件

- 位置：
  - `react-ui/src/pages/UpstreamSystem/components/ConnectionModal.tsx`
  - `react-ui/src/pages/UpstreamSystem/components/ConnectionSidebar.tsx`
  - `react-ui/src/pages/UpstreamSystem/components/ConnectionSummary.tsx`
  - `react-ui/src/pages/UpstreamSystem/components/PairingModal.tsx`
  - `react-ui/src/pages/UpstreamSystem/components/SkuDimensionPanel.tsx`
  - `react-ui/src/pages/UpstreamSystem/components/SkuInventoryPanel.tsx`
  - `react-ui/src/pages/UpstreamSystem/components/SkuSyncPanel.tsx`
  - `react-ui/src/pages/UpstreamSystem/components/SyncTabs.tsx`
  - `react-ui/src/pages/UpstreamSystem/style.module.css`
  - `react-ui/src/pages/UpstreamSystem/style.css`
  - `react-ui/src/services/integration/upstreamSystem.ts`
  - `react-ui/src/types/integration/upstream-system.d.ts`
- 当前用途：
  - 管理端“上游系统管理”菜单页面。
  - 主仓接入新增/编辑/授权、分项同步、仓库/物流渠道/SKU 配对、SKU 仓库尺寸重量、SKU 库存和请求日志查看。
  - 左侧主仓工作台、主仓排序、顶部同步内容选择、指定 SKU 快速获取尺寸重量和分项同步状态查看。
- 复用规则：
  - 后续新增外部系统管理页面时，优先复用 modal、类型和 service 分层方式。
  - 左侧主仓列表 + 右侧详情/页签的数据工作台布局，样式集中在 `style.module.css` / `style.css`，不要在页面和组件里继续散写大段撑满高度 inline style。
  - 主数据区必须填满剩余可视高度，表格数据很少时分页器也要压在数据块底部。
  - 页面文案使用“同步清单”，不要在用户界面展示“候选”。
  - 表格列、状态文本、接口类型继续集中维护，不要在页面内复制大段 option 或状态映射。
  - 分页接口进入 `startPage()` 后，Service 不要再先做 `selectOne` 预查，避免污染 PageHelper 上下文。

## 仓库模块

### warehouse 仓库主数据模块

- 位置：
  - `RuoYi-Vue/warehouse`
  - `RuoYi-Vue/sql/warehouse_management_seed.sql`
  - `RuoYi-Vue/sql/warehouse_us_address_seed.sql`
  - `react-ui/src/pages/Warehouse`
  - `react-ui/src/services/warehouse/warehouse.ts`
  - `react-ui/src/types/warehouse/warehouse.d.ts`
- 当前用途：
  - 管理端“仓库管理 / 官方仓库”和“仓库管理 / 第三方仓库”首版。
  - `warehouse` 作为系统仓库主数据事实源，`official_warehouse` / `third_party_warehouse` 作为 1:1 类型扩展表。
  - 第三方仓只保存 `seller_id`，展示时关联 seller 主体信息。
  - 官方仓同步复用现有 `upstream_system_warehouse_pairing`，不在官方仓表复制上游配对字段。
  - 官方仓同步清单直接读取现有 `upstream_system_warehouse_candidate`，页面文案使用“同步清单”，不要重新维护一份上游仓库数据。
  - 结算币种复用 `finance_currency` 启用币种 options。
  - 国家/地区复用 `country_region`，仓库地址保存国家 code。
  - 美国州/城市使用 `us_state` / `us_city`，仅在 `countryCode = US` 时启用联动；其他国家保持普通文本输入。
- 复用规则：
  - 后续商品、库存、订单、履约需要选择系统仓库时，优先读取 `warehouse` 主数据，不要重新维护仓库选项。
  - 官方仓与上游主仓仓库配对继续以 `upstream_system_warehouse_pairing` 为事实源，不要把 `externalWarehouseCode` 等字段重复放回官方仓扩展表。
  - 官方仓列表展示主仓接入名称时通过 `upstream_system_warehouse_pairing.connection_code` 联查 `upstream_system_connection`，不要在 `warehouse` 或 `official_warehouse` 冗余主仓名称。
  - 商品发布页等旧页面若仍有静态仓库选项，应优先替换成 `warehouse` 正常仓库 options，不再新增页面内硬编码仓库。
  - 第三方仓归属卖家校验必须通过 seller 模块或稳定 facade，不要直接信任前端传入的 seller 展示字段。
  - 地址组件继续复用 `WarehouseFields` 的国家/美国州城市联动口径，不要在官方仓和第三方仓页面分别维护两套逻辑。
  - 美国完整城市数据来自 U.S. Census Gazetteer Places seed，后续更新必须记录来源 URL、数据年份和导入脚本。
  - `warehouse_management_seed.sql` 写入 `2021/2022/202101-202105/202201-202204` 管理端仓库菜单前，必须先确认父菜单 `2020` 已由 `top_menu_seed.sql` 提供，并通过 `tmp_warehouse_management_sys_menu_guard` / `assert_warehouse_management_sys_menu_guard()` 做 slot/signature guard；同 ID slot guard 必须覆盖 `parent_id` 和 `menu_type`，避免历史菜单挂错父级或类型时被静默覆盖；后续新增仓库管理按钮也必须先进入该 guard 清单。
  - `AdminWarehouseController` 必须使用 `/warehouse/admin` 管理端命名空间，`react-ui/src/services/warehouse/warehouse.ts` 和 JS 镜像必须使用 `/api/warehouse/admin`；不得把后台仓库接口回退到裸 `/warehouse`。
### 来源仓库库存占位历史约束（2026-06-06）

- 位置：
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/controller/AdminSourceWarehouseStockController.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/task/UpstreamSystemTask.java`
  - `RuoYi-Vue/sql/upstream_system_management_seed.sql`
  - `RuoYi-Vue/sql/20260606_upstream_inventory_dimension_sync.sql`
  - `react-ui/src/pages/UpstreamSystem/components/SkuInventoryPanel.tsx`
  - `react-ui/src/services/integration/sourceWarehouseStock.ts`
- 历史用途：
  - 该条记录的是 2026-06-06 库存能力未开放时的回滚约束，已经被 2026-06-07 的真实库存同步实现取代。
  - 当前库存同步事实源以 `upstream_system_sku_inventory_snapshot` 和后续读模型为准；不要再按 2026-06-06 的纯占位口径理解库存能力。
  - `20260606_upstream_inventory_dimension_sync.sql` 当前仍承载库存快照 schema、同步状态、`sys_job` 注册和管理端角色授权继承，不是纯 cleanup/disable 脚本。
  - `2307/2308/2309` 上游系统管理按钮的最终 `sys_menu` owner 只能是 `upstream_system_management_seed.sql`；`20260606_upstream_inventory_dimension_sync.sql` 只能断言这些菜单已由 owner seed 提供，再做 `sys_role_menu` 授权继承，不得再 `insert into sys_menu`。
- 复用规则：
  - 继续扩展来源仓库库存前，必须先确认 schema、同步落库方案、权限点、审计日志和数据边界。
  - 不允许只扩展前端 tab、Controller、service 请求函数或 Quartz job，必须保持事实源和权限同步。
  - 后续库存同步脚本需要新增管理端按钮时，先放进 `upstream_system_management_seed.sql` 的 owner guard，再由业务迁移脚本断言菜单存在；不要在业务迁移脚本中直接写 `sys_menu`。
  - 库存相关状态、口径、配对状态选项如需恢复，优先复用 `react-ui/src/services/integration/constants.ts`，不要在页面内复制映射。
## 三端 P0/P1 验证入口与 SQL Guard 模板

- 位置：
  - `react-ui/scripts/verify-three-terminal.mjs`
  - `react-ui/tests/three-terminal.manifest.json`
  - `react-ui/package.json`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - `react-ui/tests/portal-session-request.test.ts`
  - `react-ui/tests/portal-direct-login-message.test.ts`
  - `react-ui/tests/portal-unauthorized-redirect.test.ts`
  - `docs/plans/2026-06-06-three-terminal-p0p1-sql-guard-jest-final-record.md`
  - `docs/plans/2026-06-07-three-terminal-p0p1-verify-list-drift-integration-coverage-record.md`
- 当前用途：
  - 固定三端验证入口：公开总入口使用 `npm run verify:three-terminal`，入口内部先做前端 guard、React typecheck 和三端关键 Jest，再做后端全 reactor 测试编译门和显式三端合同测试。
  - 固定后端编译闭环：`verify-three-terminal.mjs` 必须先运行 `mvn -pl ruoyi-admin -am -DskipTests test-compile`，确保 `ruoyi-admin` 依赖链上的 `ruoyi-quartz`、`ruoyi-generator`、`finance`、`warehouse`、`integration`、`product`、`seller`、`buyer` 等模块至少通过主代码和测试代码编译。
  - 固定后端三端测试清单：`three-terminal.manifest.json` 的 `backendTestClasses` 承载显式清单，`verify-three-terminal.mjs` 只对三端、Portal、权限、DirectLogin、SQL Guard、菜单/日志/会话等关键测试家族做自动发现，另用 `criticalBackendExplicitTestClasses` 保留当前 seller/buyer/product 服务类关键测试；不得再用裸 `Seller|Buyer` 作为发现条件误拦普通业务单测。列入 `backendTestClasses` 的测试仍必须产出 surefire report。
  - 固定前端三端测试清单：`three-terminal.manifest.json` 的 `frontendTestPaths` 承载前端关键 Jest 清单，`verify-three-terminal.mjs` 在 `react-ui/tests` 和 `react-ui/src` 中发现 terminal、portal、partner、remote-menu、direct-login、unauthorized、redirect、three-terminal 等关键测试文件；关键测试必须进入 `frontendTestPaths`，普通非三端 Jest 不再自动阻断该快速入口。
  - 固定 `integration` 模块至少进入 `ruoyi-admin -am` 编译闭环，避免来源商品、来源仓库库存、上游同步等已接入管理端能力只在旁路 compile 中验证。
  - 固定高影响 SQL 默认拒绝执行：会改远端结构、菜单、权限、任务或端内账号控制面的脚本，必须设置显式 `@confirm_*` 令牌后才能运行。
  - 固定官方初始化基线脚本边界：`ry_20260417.sql` / `quartz.sql` 是 bootstrap-only 建库基线，允许包含破坏性 `DROP TABLE`，但必须保留 `URILI_BOOTSTRAP_ONLY_SQL` 哨兵注释，且不得被当作普通增量 SQL 回放。
  - 固定 portal 前端请求边界：seller/buyer 请求必须使用对应端 token，并过滤 `sellerId`、`buyerId`、`subjectId`、`accountId`、`sellerAccountId`、`buyerAccountId`、`terminal` 等前端可控范围参数。
  - 固定 direct-login 消息边界：一次性 token 只允许发送给匹配 popup、origin 和 terminal 的 ready 消息。
  - 固定上游系统管理菜单 seed 边界：`upstream_system_management_seed.sql` 写 `2031` 和 `2300-2309` 前必须先通过 `tmp_upstream_system_management_sys_menu_guard` / `assert_upstream_system_management_sys_menu_guard()` 做 slot/signature guard。
  - 固定上游库存迁移脚本边界：`20260606_upstream_inventory_dimension_sync.sql` 只能依赖并断言 `2307/2308/2309` 已由上游系统管理 seed 提供，不得重复写 `sys_menu`。
- 复用规则：
  - 后续新增三端合约测试时，必须同步加入 `react-ui/tests/three-terminal.manifest.json` 的 `backendTestClasses`；如果关键测试类未加入，`verify-three-terminal.mjs --check-manifest` 和完整 `verify:three-terminal` 都应失败。finance 等非三端普通测试只要求进入 `ruoyi-admin -am` 编译闭环，不强制纳入三端快速测试清单。
  - 后续新增前端三端关键 Jest 时，必须同步加入 `react-ui/tests/three-terminal.manifest.json` 的 `frontendTestPaths`；普通非三端 Jest 可以留在自身模块测试入口，不应被 `verify:three-terminal` 全量误拦。
  - 后续新增高影响 SQL 脚本时，必须补 `set @confirm_*`、确认过程、`signal sqlstate '45000'` 和执行前 `call assert_*_confirmed();`；`SqlExecutionGuardContractTest` 通过 `DATED_SQL_FILE` 自动发现日期前缀高影响脚本，dynamic DDL helper 也必须纳入 high-impact hint，不能只靠手工登记或 `202606*.sql` 白名单。
  - `ry_20260417.sql` / `quartz.sql` 不得改造成普通增量迁移，也不得塞进 confirm-token 增量脚本清单；如果新增 bootstrap baseline 脚本，必须先登记 bootstrap-only 哨兵和静态合同。
  - 会写管理端 `sys_role_menu` 授权的业务迁移脚本，如果依赖按钮权限，必须先断言对应 `sys_menu` 已由主 owner seed 提供；不能为了授权方便在迁移脚本里补写菜单 owner。
  - `test`、`test:coverage`、`test:update`、`test:unit`、`jest` 等公开入口不得绕过三端验证；`verify-three-terminal.mjs` 内部直接调用本地 Jest 并校验 JSON 结果，公开脚本不再提供 raw Jest 入口。
  - `verify-three-terminal.mjs` 的公开脚本自检必须同时在 `--check-manifest` 和正常完整验证路径执行，避免公开 npm script 后续被改回 raw Jest 后只在旁路检查中暴露。
  - SQL 自动发现顺序校验必须绑定到真正的 `call assert_*_confirmed();`，不能用无关 `call assert_*` 替代确认过程位置。
  - seller/buyer split permission seed 给端内基础菜单或按钮授权时，只能授给启用且未删除的 `owner` 角色；不得把新权限扩散给所有启用角色。
  - portal 页面或 service 不得自行拼接端 token、端内 API URL 或主体范围参数；继续通过 `services/portal/session.ts` 统一发起请求。

## Portal 登录会话一致性与 SQL Seed Guard 模板

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java`
  - `RuoYi-Vue/ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/PortalLogAspect.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalLoginSessionConsistencyContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - `react-ui/scripts/check-partner-management-template.mjs`
  - `react-ui/src/components/PartnerManagement/PartnerSessionModal.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerSessionModal.js`
- 当前用途：
  - 端内登录 token/session 一致性模板：登录 token 先写 Redis 后，DB 登录日志和会话表记录失败时必须删除刚签发的 Redis token。
  - 登录失败日志保留模板：普通业务失败使用 `ServiceException`，端内登录事务必须配置 `noRollbackFor = ServiceException.class`，避免失败日志被回滚。
  - 登录/免密登录操作日志模板：`PortalLogAspect` 可从成功返回的 `PortalLoginResult.token` 回查同端 session，补齐账号、主体和 direct-login acting admin 审计。
  - 高影响 SQL seed 模板：写 `sys_menu` 前必须先做 slot/signature guard；动态 DDL 前必须先做 baseline 表/字段 guard。
  - JS sidecar guard 模板：管理端模板检查必须同时覆盖 `.tsx` 和 `.js` 副本，避免构建实际使用文件绕过契约。
- 复用规则：
  - 后续 seller/buyer 同构登录、免密登录或刷新 token 逻辑，必须复用 `PortalTokenSupport`，不得自行生成 JWT 或自行写 Redis key。
  - 任何端内登录成功链路新增 DB 记录步骤时，都必须保持“DB 记录失败 -> 删除已签发 Redis token -> 抛出异常”的补偿语义。
  - 任何端内匿名允许的认证接口，如果成功响应中携带 `PortalLoginResult`，操作日志应回查同端 session，不应长期记为 `anonymous`。
  - seller/buyer portal 权限读路径必须逐项 trim 逗号分隔权限，并按 terminal prefix fail-closed；seller 端不得接受 buyer 权限、`seller:admin:*` 或 `*:*:*`，buyer 端不得接受 seller 权限、`buyer:admin:*` 或 `*:*:*`。
  - 新增管理端菜单 seed、按钮 seed、端内账号控制 SQL 或动态 DDL SQL 时，必须同步补 `SqlExecutionGuardContractTest`。
  - 同构 UI 文件如果存在 `.tsx` / `.js` sidecar，guard 必须同时检查两份文件。

## 三端角色树、登录与会话守卫模板

- 位置：
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerDeptModal.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerRoleModal.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerMenuModal.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerSessionModal.tsx`
  - `react-ui/scripts/check-partner-management-template.mjs`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDeptSupport.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductPortalSchemaServiceImpl.java`
- 当前用途：
  - 管理端 PartnerManagement 权限闭环：打开编辑弹窗前必须同时具备写权限和详情查询权限；树形选择或菜单树依赖查询权限，不允许只有新增/编辑权限时隐式调用查询接口。
  - seller/buyer owner 角色保护：owner 角色不可修改、停用或删除；owner 账号不能清空角色，也不能移除启用状态的 owner 角色绑定。
  - seller/buyer 菜单树保护：新增/更新菜单时校验父级存在，更新时禁止把菜单移动到自己的子孙节点。
  - seller/buyer 菜单组件路径保护：管理端端内菜单 `component` 校验必须先去掉前导 `./` / `/` 并按小写首段比较，防止 `./Buyer/...`、`./Seller/...` 或大小写变体绕过跨端校验。
  - seller/buyer 部门树保护：统一通过 `PortalDeptSupport.assertDeptParentNotDescendant` 防止部门父级形成循环。
  - seller/buyer 登录前置检查：普通密码登录先检查用户名、密码和 `sys.login.blackIPList`；不在该模板里隐式恢复验证码开关。
  - direct-login 票据消费模板：消费前检查黑名单；DB 票据存在但 Redis payload 丢失时，将 DB 票据置为 `EXPIRED` 后再抛出异常。
  - seller/buyer 会话状态模板：未登出但 `expire_time` 已过期的在线会话，在列表 SQL 中派生为状态 `2`，前端展示“已过期”。
  - product portal schema 模板：缺少分类等上游输入时返回受控 `ServiceException`，不让空指针穿透到接口层。
- 复用规则：
  - 后续新增端内角色、菜单、部门管理能力时，必须先复用当前 owner 保护、父级存在校验和父子循环检测，不要在 seller/buyer 内各自散写不同规则。
  - 后续扩展端内菜单维护表单时，组件路径、路由路径和权限标识都必须保留静态 guard；组件路径不能只比较原始大小写或只去掉 `/`，必须覆盖 `./` 前缀。
  - 后续扩展管理端同构弹窗时，按钮可见性必须覆盖“操作权限 + 被调用接口权限”，并同步加入 `check-partner-management-template.mjs`。
  - 后续接入 seller/buyer 验证码前必须单独确认，不允许因为密码登录前置检查而恢复管理端验证码开关。
  - 后续 direct-login 票据异常场景必须保留可审计状态收口，不允许 DB 票据和 Redis payload 不一致后继续保持可疑可用状态。
  - 后续新增 session 列表字段时，优先在 mapper SQL 和统一前端 modal 中补齐，不要新增平行会话列表实现。

## 三端免密一次性、角色绑定和验证清单模板

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/mapper/SellerMapper.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/mapper/BuyerMapper.java`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java`
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalLoginSessionConsistencyContractTest.java`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerPortalPermissionMapper.xml`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerPortalPermissionMapper.xml`
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerRoleModal.tsx`
  - `react-ui/scripts/check-partner-management-template.mjs`
  - `react-ui/scripts/verify-three-terminal.mjs`
  - `RuoYi-Vue/sql/top_menu_seed.sql`
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - `RuoYi-Vue/sql/20260606_admin_partner_role_menu_grant.sql`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminDirectLoginPermissionContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
- 当前用途：
  - 免密 token 一次性模板：拿到 DB ticket 和 Redis payload 后，首次提交无论业务校验成功还是失败，都必须删除 Redis payload，并将 DB ticket 收口为 `USED` 或 `EXPIRED`。
  - 免密失败上下文模板：DB ticket 已经查到但 Redis payload 丢失、过期、端类型不匹配、票据/目标不匹配时，`PortalDirectLoginSupport` 必须先从 DB ticket 恢复 `PortalDirectLoginToken` 审计上下文，再调用 seller/buyer failure auditor；只有空 token、ticket 不存在、校验器为空这类确实无 ticket 的场景才写普通失败日志。
  - 免密退出日志模板：端内 session 若带 `directLogin=true`，主动退出日志必须用 `PortalTokenSupport.buildDirectLoginLog(..., session)` 保留 `ticketId`、acting admin 和 reason；普通登录仍写 `directLogin=false`。
  - 强制踢出逐 session 审计模板：seller/buyer 强制踢出前必须先读取在线 `PortalLoginSession` 列表，按每个 session 写登录日志，再按同一批 tokenId 删除 Redis token；direct-login session 必须保留 `directLogin`、`ticketId` 和 reason，但 `actingAdmin*` 必须覆盖为本次执行强退的后台管理员。
  - 角色绑定模板：seller/buyer 账号角色回显和绑定合法性校验都必须过滤停用角色，不能让停用角色静默挂到账户上等待后续重新启用。
  - 管理端弹窗权限模板：弹窗按钮可见性必须覆盖实际会调用的查询接口权限；例如角色编辑必须有 `role:edit + role:query + menu:query`，账号部门树必须有 `dept:query` 才请求。
  - 管理端授权脚本模板：`sys_role_menu` 首段授权只能授经过签名确认的菜单树入口，子按钮授权必须从已授页面向下扩展，不能对裸 `seller:admin:%` / `buyer:admin:%` 做全局通配授权。
  - SQL seed 模板：会写 `sys_menu`、`sys_config`、端内角色菜单或管理端授权的 seed 必须 fail-closed；已有配置需要收敛时，使用先 `update` 再缺失 `insert` 的可回放模式。
  - 高影响 SQL 自动发现模板：`SqlExecutionGuardContractTest` 必须通过 `DATED_SQL_FILE` 自动扫描日期前缀 SQL 中含 `insert/update/delete/alter table/create table` 或 dynamic DDL high-impact hint 的脚本；发现高影响脚本时，必须要求 `@confirm_*`、`signal sqlstate '45000'` 和 `call assert_*_confirmed();`，不能只靠手工枚举或按日期白名单漏扫。
  - 三端验证清单模板：`verify-three-terminal.mjs` 必须先跑 `ruoyi-admin -am -DskipTests test-compile` 覆盖后端 reactor 编译链，再读取 `react-ui/tests/three-terminal.manifest.json` 运行三端关键后端测试并校验 surefire report；后端显式合同类必须有 XML report、`tests > 0` 且 `skipped = 0`；新增前端关键 Jest 在 `react-ui/tests` 和 `react-ui/src/**` 的三端关键词范围内 fail-closed，普通前端单测不再让总入口直接失败。
  - Maven 窄范围验证模板：`product`、`integration`、`seller`、`buyer` 等已存在 reactor 内部依赖的模块，定向测试命令必须使用 `-am`，例如 `mvn -pl product -am ... test` 或 `mvn -pl seller,buyer -am ... test`；不要把裸 `mvn -pl product ... test`、`mvn -pl integration ... test` 或不带 `-am` 的 seller/buyer portal 商品测试命令作为当前门禁。
- 复用规则：
  - 后续新增 direct-login 失败分支时，不允许保留同一个 token 可在 30 分钟窗口内失败重放。
  - 后续新增 direct-login 失败日志时，若已有 ticket 或 payload 上下文，必须写 `buildDirectLoginLog(...)` 并保留 `ticketId`、acting admin、reason 和目标账号；若 ticket 端类型不是当前端，当前端日志只能写 direct-login 审计字段，不能把对方端的 `subjectId/accountId` 写入当前端日志表。
  - 后续新增端内退出、踢出或 session 收口日志时，必须优先从 `PortalLoginSession` 复制 direct-login 审计字段；批量踢出不能只保留一条无法定位具体 session 的汇总日志。管理端强制踢出、账号锁定或密码重置导致的强退必须同时用运行时测试固定 `directLogin/ticketId/reason` 保留、`actingAdmin*` 归当前后台管理员、Redis token 只按当前 terminal 删除。
  - 后续新增账号角色绑定能力时，必须同时考虑 role `del_flag` 和 `status`，并补 seller/buyer 对称测试。
  - 后续新增同构管理端弹窗时，先列出按钮触发后会调用的 service，再按 service 对应查询/写入权限闭合，不只看按钮本身的写权限。
  - 后续新增管理端授权 SQL 时，不允许用权限前缀全局授权；必须先确认菜单 ID、父子关系、`menu_type` 和 `perms` 签名，再授受控树。
  - 后续新增高影响 seed 时，自动扫描发现的新高影响 SQL 必须自带 fail-closed guard，已有 config 需要修正时不能只写 `where not exists`；DDL 可重放性和菜单 slot/signature guard 仍按相邻 SQL 契约逐步收紧。
  - 后续新增 Java 三端关键测试或 `react-ui/tests` 下三端关键 Jest 时，必须同时更新验证清单；清单不收录应当让验证失败，而不是静默跳过。普通非三端测试仍按各自模块测试入口维护。

## Portal 自助审计权限模板

- 位置：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java`
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - `RuoYi-Vue/sql/20260607_portal_self_audit_permission_seed.sql`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalAnonymousEndpointContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalSeedPermissionContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - `docs/plans/2026-06-07-three-terminal-p0p1-portal-self-audit-permission-record.md`
- 当前用途：
  - 固定 seller/buyer 端内自助登录日志、操作日志和会话列表接口必须同时满足当前 terminal 会话和端内细粒度权限。
  - 固定自助审计权限写入 `seller_menu` / `buyer_menu`，并授权给端内 OWNER 角色，不写入或依赖若依 `sys_menu` / `sys_role`。
  - 固定权限补丁脚本必须通过 `@confirm_portal_self_audit_permission_seed` fail-closed guard 后才允许执行。
- 复用规则：
  - 后续新增端内自助审计、端内安全中心或账号自助管理接口时，不得只写 `@PortalPreAuthorize(terminal = "...")`；凡涉及日志、会话、权限、账号资料等敏感自助读写，都必须补 `hasPermi`。
  - seller/buyer 同构权限命名使用端前缀，例如 `seller:account:loginLog:list` 和 `buyer:account:loginLog:list`，不要复用管理端 `seller:admin:*` / `buyer:admin:*`。
  - 新增端内自助权限时，同步更新综合 seed、独立增量 seed、OWNER 授权、`TerminalSeedPermissionContractTest` 和 `PortalAnonymousEndpointContractTest`。
  - 独立权限补丁脚本执行前必须确认当前激活数据源，执行记录必须写明是否影响远程库和只读核验结果，不能输出 `.env.local` 明文。

## 余额/充值占位边界

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PartnerProfile.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalSubjectProfile.java`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml`
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.js`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminAccountPermissionUiContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/domain/PortalHomeProfileSerializationTest.java`
  - `docs/plans/2026-06-07-three-terminal-p0p1-balance-placeholder-boundary-record.md`
- 当前用途：
  - 固定 seller/buyer 主体资料、管理端列表/详情和端内 profile 不承载真实余额、币种、充值或结算事实。
  - 固定管理端余额列只是占位展示，不从后端读取 `USD 0.00` 这类伪真实余额。
  - 固定买家充值只保留“充值能力 / 规划中”占位，不提供动作按钮、弹窗、写接口或资金语义。
- 复用规则：
  - 在 finance 真实余额读模型落地前，seller/buyer mapper 不得投影 `account_balance` / `balance_currency`，也不得使用 `balanceMin` / `balanceMax` 查询条件。
  - `PartnerProfile`、`PortalSubjectProfile` 等主体资料 DTO 不得加入 `accountBalance` / `balanceCurrency`；真实余额必须通过单独 finance read model 或资金账户 DTO 暴露。
  - 前端可以保留占位列，但必须明确显示“待接入 / 占位”或“规划中”，不得格式化为 `USD 0.00` 这类金额事实。
  - 后续恢复余额筛选前，必须先完成 finance 账户、流水、充值/结算事实源设计，并重新定义权限、审计和查询口径。

## 端内角色与部门隔离测试模板

- 位置：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalDeptServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalDeptServiceImpl.java`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplTest.java`
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplTest.java`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalDeptServiceImplTest.java`
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalDeptServiceImplTest.java`
  - `react-ui/scripts/verify-three-terminal.mjs`
  - `docs/plans/2026-06-07-three-terminal-p0p1-role-dept-isolation-test-record.md`
- 当前用途：
  - 固定 seller/buyer 端内角色 checkedKeys 查询必须先校验当前主体拥有该角色，再带 `subjectId + roleId` 查询菜单勾选项。
  - 固定 seller/buyer OWNER 角色不允许编辑、停用或删除；禁停用和禁删除必须在状态更新、账号角色计数、角色菜单删除、角色删除之前 fail-closed。
  - 固定 seller/buyer 部门自身的新增、更新、父部门选择和删除都必须按当前主体查询旧部门或父部门；查不到时不能继续写入、更新 ancestors、检查子部门、检查账号或删除。
  - 固定新增 seller/buyer 关键测试必须进入 `react-ui/tests/three-terminal.manifest.json` 的 `backendTestClasses`；清单不收录应让总验证失败。
- 复用规则：
  - 后续新增卖家端同构权限或组织结构能力时，先在 seller 测试中补完整负向断言，再机械复制到 buyer，只替换端类型、service、mapper、字段和中文错误语义。
  - 后续新增角色状态、删除、授权、菜单勾选或账号角色绑定时，必须断言错主体或 OWNER 保护发生在任何写入 mapper 调用之前。
  - 后续新增或改造角色菜单授权时，必须断言 `menuIds` 中只要存在不属于当前端菜单表的 ID，service 就在任何角色写入、角色菜单删除或角色菜单插入前 fail-closed；seller/buyer 两端必须保持对称负向测试。
  - `seller_menu` / `buyer_menu` 数字 ID 空间必须保持不重叠：seller 使用 `100000-199999`，buyer 使用 `200000-299999`；fresh seed 和三端隔离迁移必须从 `100000` / `200000` 起步，运行库重排只能使用 guarded 迁移脚本同步更新 role-menu 与 `parent_id`。
  - 后续新增部门树写入或删除能力时，必须断言错主体部门和错主体父部门不会触发 insert/update/delete/ancestor/account/child mapper 调用。
  - 如果后续改为主体私有菜单模板，必须先重写 `seller_menu` / `buyer_menu` schema 和对应合同测试；当前只保证同端角色菜单绑定按主体隔离，菜单模板仍是端级全局模板。

## 卖家/买家管理端列表查询参数模板

- 位置：
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.js`
  - `react-ui/src/types/seller-buyer/seller.d.ts`
  - `react-ui/src/types/seller-buyer/buyer.d.ts`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml`
- 当前用途：
  - 固定 seller/buyer 管理端主体列表的同构筛选参数。
  - 前端手机号筛选字段使用后端 mapper 实际识别的 `contactPhone`。
- 复用规则：
  - `PartnerManagementPage.buildListParams(...)` 必须兼容旧浏览器缓存里的 `phone`，提交接口前转换为 `contactPhone` 并删除 `phone`。
  - `SellerListParams` / `BuyerListParams` 查询类型只暴露 `contactPhone`，不要重新引入 `phone` 查询参数。
  - 主体表单和联系人展示可以继续兼容实体上的 `phone` / `contactPhone`，但列表查询必须以 mapper 条件为准。

## 端内账号主体作用域 Mapper 查询模板

- 位置：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/mapper/SellerMapper.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/mapper/BuyerMapper.java`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java`
  - `docs/plans/2026-06-07-three-terminal-p0p1-account-mapper-subject-guard-record.md`
- 当前用途：
  - 固定 seller/buyer 账号查询在已有主体上下文时必须使用 `selectSellerAccountByIdAndSellerId(...)` / `selectBuyerAccountByIdAndBuyerId(...)`。
  - 固定 SQL 层同时带 `subjectId + accountId`，不要默认先按裸账号 ID 查询再在 Java 层比较主体。
  - 固定管理端日志归一化不得再保留 account-only 反查：如果请求传 `accountId`，必须同时传 `sellerId` / `buyerId`；缺少主体 ID 时 service 必须 fail-closed，不得从裸账号 ID 反推出主体。
- 复用规则：
  - 管理端 seller/buyer 新增和编辑端账号接口都必须对请求体使用 `@Validated @RequestBody`；不要只校验新增账号而让编辑路径绕过 `PortalAccount` 字段长度、必填和格式约束。
  - 后续新增端内账号详情、账号编辑、锁定、解锁、重置密码、强制踢出、免密登录、端内改密、权限信息和菜单树校验时，必须优先走 scoped Mapper。
  - seller/buyer 同构实现时，先做 seller 标准模板，再机械复制 buyer，只替换端类型、字段名、Mapper 方法名和错误文案。
  - `TerminalAccountIsolationTest.terminalAccountMappersMustKeepSubjectScopeWithoutAccountOnlyLookup` 已固定生产代码禁止裸 accountId mapper 调用，并禁止 Mapper 接口/XML 恢复裸 accountId 查询声明；如果后续确实需要新增账号查询能力，必须新增显式主体约束方法，不得恢复 account-only 反查例外。

## 账号弹窗部门权限 Fail-Closed 模板

- 位置：
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.js`
  - `react-ui/scripts/check-partner-management-template.mjs`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminAccountPermissionUiContractTest.java`
  - `docs/plans/2026-06-07-three-terminal-p0p1-account-dept-query-fail-closed-record.md`
- 当前用途：
  - 固定 seller/buyer 管理端账号弹窗中，账号列表、新增、编辑权限与部门树查询权限分开判断。
  - 固定 `dept:query` 缺失时不请求部门树接口，部门字段禁用并显示 `无部门查询权限`。
  - 固定新增和编辑入口仍由账号新增/编辑权限控制，不把部门查询权限扩大为账号操作入口权限。
- 复用规则：
  - 后续同构账号、员工、组织弹窗如果某个字段依赖额外查询权限，应在字段级 fail-closed，不应隐藏整个弹窗或整个新增/编辑按钮，除非该字段本身是业务必填且没有安全默认值。
  - 不要用 `Promise.all` 把账号列表和部门树查询强耦合；账号列表应能在具备账号列表权限时独立加载。
  - seller/buyer 复制时只替换权限前缀、service 和文案，不重新设计弹窗权限结构。
  - 新增字段级权限 guard 时，同步更新 `check-partner-management-template.mjs` 和 `AdminAccountPermissionUiContractTest`，避免 TS/JS 镜像或同构页面分叉。

## 管理端强退登录日志 Actor 模板

- 位置：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java`
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`
  - `docs/plans/2026-06-07-three-terminal-p0p1-login-log-admin-actor-record.md`
- 当前用途：
  - 固定管理端触发的 seller/buyer 锁定、停用、强制踢出、重置密码强退普通会话时，登录日志写入当前后台 admin 的 `actingAdminId` / `actingAdminName`。
  - 固定端内自助改密触发强退时不读取若依后台登录态，避免端内账号和管理端 `sys_user` 串用。
  - 固定 direct-login 会话的 `actingAdmin*` 表示原免密票据签发人；强退时不覆盖该 issuer。
  - `seller_oper_log` / `buyer_oper_log` 中的 `actingAdmin*` 同样表示原免密票据签发人，不表示当前端内账号，也不表示后续管理端强退/重置密码的执行人。
- 复用规则：
  - 后续新增管理端会话失效类动作时，必须显式区分管理端动作与端内自助动作，不要无条件调用 `SecurityUtils.getUserId()`。
  - 后续如果需要在 direct-login 强退日志中同时结构化保存“原 issuer”和“本次 operator”，必须先设计新字段或独立审计事件；不要把两个语义塞进同一组 `actingAdmin*` 字段。
  - seller/buyer 同构实现先做一端标准模板，再复制另一端，只替换端类型、方法名、错误文案和 Mapper 名称。
  - 管理端后台动作仍应保留若依 `@Log` / `sys_oper_log` 审计；端内登录日志补 actor 只是便于按端内账号追踪会话失效原因。

## 管理端静态 Partner 路由 Guard 模板

- 位置：
  - `react-ui/config/routes.ts`
  - `react-ui/config/routes.js`
  - `react-ui/src/services/session.ts`
  - `react-ui/src/services/session.js`
  - `react-ui/src/wrappers/RemoteMenuRouteGuard.tsx`
  - `react-ui/src/wrappers/RemoteMenuRouteGuard.js`
  - `react-ui/scripts/check-partner-management-template.mjs`
  - `react-ui/scripts/verify-three-terminal.mjs`
  - `react-ui/tests/remote-menu-route-guard.test.ts`
  - `docs/plans/2026-06-07-three-terminal-p0p1-static-partner-route-guard-record.md`
- 当前用途：
  - 固定管理端静态 `/seller`、`/buyer` fallback 路由必须和远程菜单动态路由共用同一个 `RemoteMenuRouteGuard` 权限判断。
  - 固定 `/seller` 需要 `seller:admin:list`，`/buyer` 需要 `buyer:admin:list`；静态 route 必须声明 `authority` 和 `@/wrappers/RemoteMenuRouteGuard`。
  - 固定 wrapper 的 `getStaticRouteAuthority(...)` 必须归一化尾斜杠：`/seller/` 等同 `/seller`，`/buyer/` 等同 `/buyer`。
  - 固定 `/seller/login`、`/seller/direct-login`、`/seller/portal` 及其子路径不得被静态管理端 seller fallback 捕获；buyer 同理。
  - 固定 `check-partner-management-template.mjs` 同时校验路由、wrapper、guard 导出、remote menu storage scoped key 和 TS/JS 镜像，避免静态路径成为无权限直达入口。
- 复用规则：
  - 后续新增管理端静态 fallback 路由时，必须显式声明 `authority` 并挂 `RemoteMenuRouteGuard` wrapper；不能只依赖远程菜单 patch。
  - 显式 `route.authority` 必须优先于静态 fallback authority；静态 fallback 只作为管理端 `/seller`、`/buyer` 直达兜底。
  - 不要用宽泛 `/seller/*`、`/buyer/*` 管理端路由吞掉端内 login、direct-login 或 portal 入口；端内入口继续由 Portal 路由和端 session 逻辑负责。
  - 后续新增 `react-ui/tests` 下的 React 三端关键 Jest 测试时，必须同步更新 `react-ui/tests/three-terminal.manifest.json` 的 `frontendTestPaths`；普通非三端 Jest 不进入该快速验证入口的强制发现范围。
  - 拆出 `seller-ui` / `buyer-ui` 后，端内前端不得复用该管理端 wrapper；端内页面应读取 seller/buyer 自己的端内权限模型。

## 端内菜单 Fail-Closed 与远程菜单空权限 Guard 模板

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalPermissionSupport.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplMenuTreeTest.java`
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplMenuTreeTest.java`
  - `react-ui/src/components/PartnerManagement/PartnerMenuModal.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerMenuModal.js`
  - `react-ui/src/services/session.ts`
  - `react-ui/src/services/session.js`
  - `react-ui/scripts/check-partner-management-template.mjs`
  - `react-ui/scripts/check-portal-token-isolation.mjs`
  - `react-ui/tests/remote-menu-route-guard.test.ts`
  - `docs/plans/2026-06-07-three-terminal-p0p1-terminal-menu-fail-closed-record.md`
- 当前用途：
  - 固定 seller/buyer 端内页面菜单 `C` 和按钮菜单 `F` 的 `perms` 必填，必须使用当前端前缀 `seller:` / `buyer:`。
  - 固定端内菜单权限禁止 `*` 通配和 `seller:admin:` / `buyer:admin:` 管理端命名空间。
  - 固定端内页面菜单 `C` 的 `component` 必填，且必须使用当前端页面根路径；不能为空回退到共享占位页。
  - 固定 React 远程菜单 route guard 对空 `authority` 返回 403，不把空权限视为允许访问。
  - 固定三端 token / proxy 运行入口镜像必须同时检查 `.ts/.tsx` 和 `.js`，尤其是 `access.js`、`proxy.js`。
- 复用规则：
  - 后续新增 seller/buyer 端内菜单写入、导入、批量同步或 seed 生成时，必须复用 `PortalPermissionSupport.normalizeTerminalMenu(...)` 或等价的同一规则，不要在 service/mapper/XML 中复制散落校验。
  - seller/buyer 同构实现先做 seller 标准模板，再机械复制 buyer，只替换 terminal、权限前缀、组件根和测试文案。
  - 后续新增远程菜单或静态 fallback 路由时，必须显式提供 authority；空 authority 只能被视为配置错误，不允许回退为可访问。
  - 后续修改三端 token、proxy、菜单、权限相关 TS 源时，必须同步检查 JS 镜像和 guard 脚本，避免开发入口和运行入口漂移。

## Portal 商品入口、Role-Menu 隔离与自助可见面合同模板

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalProductEndpointPermissionContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRoleMenuMapperIsolationContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalSelfServiceSurfaceContractTest.java`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplMenuTreeTest.java`
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplMenuTreeTest.java`
  - `react-ui/scripts/verify-three-terminal.mjs`
  - `docs/plans/2026-06-07-three-terminal-p0p1-portal-product-menu-self-service-contract-record.md`
- 当前用途：
  - 固定 seller/buyer 商品分类、商品 schema、商城商品 portal 接口必须声明当前端 `@PortalPreAuthorize` 权限，必须从 `PortalSessionContext.requireSession(...)` 推导主体和账号 scope，不能接受前端传入的主体/账号 ID。
  - 固定 seller/buyer role-menu Mapper XML 只允许使用当前端 `*_role`、`*_menu`、`*_role_menu` 表；禁止回退到 `sys_menu` 或误连对端表。
  - 固定 seller/buyer `count*MenusByIds` 必须在 SQL 层同时限制当前端菜单 ID 区间：seller `100000-199999`，buyer `200000-299999`；不能只依赖“查询的是本端菜单表”。
  - 固定 Portal 自助 `profile`、`accountProfile`、`accounts`、`depts`、`roles` 必须映射为 `Portal*Profile` 可见 DTO，不直接返回 seller/buyer/domain 内部模型。
  - 固定 Portal 自助登录/操作日志查询只能复制白名单筛选字段，并强制覆盖当前 session 的 `subjectId + accountId`；返回行必须映射为 `PortalOwnLoginLogProfile` / `PortalOwnOperLogProfile`。
  - 固定端内菜单 `updateMenu` 与 `insertMenu` 一样，在写 Mapper 前拒绝空 `perms`、非法 `component`、通配权限、管理端命名空间和跨端权限。
- 复用规则：
  - 后续新增 seller/buyer portal 商品、订单、库存、财务等端内接口时，优先复制 `PortalProductEndpointPermissionContractTest` 的合同结构：端权限、当前端 session scope、端 service 调用、禁止前端传 scope。
  - 后续新增端内 role-menu、dept-menu、account-role 等绑定 Mapper 时，必须新增静态 XML 合同，显式列出当前端表、当前端菜单 ID 区间和禁止表；不能只靠 service mock 调用次数。
  - 后续新增端内自助接口时，返回对象必须先设计 Portal 可见 DTO；如果直接返回 domain，需要先说明字段安全性并补序列化合同，不允许默认返回内部模型。
  - 后续修改自助日志查询条件时，先更新白名单合同；不得让 `directLogin*`、`actingAdmin*`、`operParam`、`jsonResult`、`subjectId`、`accountId` 等内部审计字段从请求侧透传。

## 商城 SKU 官方来源绑定模板

- 位置：
  - `RuoYi-Vue/sql/20260607_product_sku_source_binding.sql`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/domain/ProductSkuSourceBinding.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java`
  - `RuoYi-Vue/product/src/main/resources/mapper/product/ProductDistributionMapper.xml`
  - `react-ui/src/pages/Product/Distribution/EditPage.tsx`
  - `react-ui/src/pages/Product/Distribution/components/SkuMatrixEditor.tsx`
  - `docs/plans/2026-06-07-product-source-pairing-relation-plan.md`
- 当前用途：
  - 固定商品侧用 `product_sku_source_binding` 保存商城 SKU 与官方来源 SKU 的主事实关系。
  - 固定第一版官方来源 SKU 按 `source_sku_group_key` 全局只能绑定一个商城 SKU；ACTIVE 约束由 `active_source_key` 唯一键兜底。
  - 固定官方仓商品不由前端手工选择发货仓库，而是通过来源 SKU 尺寸组、`source_product_warehouse_detail`、`upstream_system_warehouse_pairing(pairing_role='FULFILLMENT')` 和 `warehouse` 推导。
  - 固定 `upstream_system_sku_pairing` 只是来源商品库和来源库存读模型的配对投影，不作为商品侧主事实。
  - 固定离开草稿后锁定来源绑定；普通新增/编辑只能释放未锁定绑定，已锁定绑定后续必须走管理端换绑/释放能力。
- 复用规则：
  - 后续新增官方仓商品保存、审核通过、卖家端提报接入时，必须写 `product_sku_source_binding`，不要直接把 upstream 原始 SKU 行塞进商品表。
  - 后续来源商品库/库存模块只消费 `upstream_system_sku_pairing` 投影和读模型，不反向写商品绑定主事实。
  - 后续如果业务允许同一个官方来源 SKU 被多个商城 SKU 或多个卖家共享，必须先重做 `active_source_key` 约束和来源/库存配对展示口径，不能只删除唯一键。
  - 官方仓绑定的仓库范围必须继续使用履约用途 `pairing_role='FULFILLMENT'`；报价仓、物流渠道或手工仓库选择不能混入发货库存读取边界。

## 商品 Mapper 与三端菜单 SQL Guard 模板

- 位置：
  - `RuoYi-Vue/product/src/main/resources/mapper/product/ProductDistributionMapper.xml`
  - `RuoYi-Vue/product/src/test/java/com/ruoyi/product/architecture/ProductDistributionMapperContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalSqlIsolationContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminDirectLoginPermissionContractTest.java`
  - `RuoYi-Vue/sql/20260604_*permission_seed.sql`
  - `RuoYi-Vue/sql/20260606_admin_partner_role_menu_grant.sql`
  - `RuoYi-Vue/sql/20260606_admin_partner_non_admin_button_grant_cleanup.sql`
  - `RuoYi-Vue/sql/20260607_portal_self_audit_permission_seed.sql`
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - `docs/plans/2026-06-07-three-terminal-p0p1-product-mapper-terminal-menu-range-admin-signature-record.md`
- 当前用途：
  - 固定商品 SPU resultMap 只能映射 `ProductSpu` 字段；SKU 来源绑定字段必须留在 SKU/绑定读模型，不得塞回 `ProductSpuResult`。
  - 固定 `ProductDistributionMapper.xml` 当前跨模块表 debt 必须保持 statement 级 allowlist；新增来源商品、来源仓、上游配对或仓库表访问时，必须先更新 `ProductDistributionMapperContractTest` 并说明事实归属。
  - 固定商品分销库存聚合字段在库存事实源方案确认前只能是显式 `null as available_stock / warehouse_count / inventory_status / stock_update_time` 占位；不得在 product mapper 内直接引用 `source_warehouse_stock_*` 或 `upstream_system_sku_inventory_snapshot` 伪接通库存。
  - 固定所有会写 `seller_menu` / `buyer_menu` 的 terminal 权限 seed 必须先调用 `assert_terminal_menu_range_ready()`，在插入前校验 ID 区间和 `AUTO_INCREMENT` 起点。
  - 固定 seller 端菜单 ID 使用 `100000-199999`，buyer 端菜单 ID 使用 `200000-299999`；旧库未跑区间迁移时，后续权限 seed 必须 fail-closed。
  - 固定 admin partner 授权和清理脚本必须先校验 `2010/2011/2012` 页面 `path/component/route/perms` 签名，再对按钮做白名单授权或清理。
  - 固定 admin partner 按钮行必须满足 `child.path='#'`、`child.component=''`、`child.route_name=''`，避免脏按钮菜单被误授权或误清理。
- 复用规则：
  - 后续新增 terminal 权限 seed 时，如果出现 `insert into seller_menu` 或 `insert into buyer_menu`，必须复制同类 `assert_terminal_menu_range_ready()` guard，并确保调用点位于第一条 terminal 菜单插入之前。
  - 后续新增商品 mapper resultMap 时，先确认 Java domain 是否真的有对应字段；跨 DTO 的字段不能靠 MyBatis resultMap 临时塞入。
  - 后续真正接通商品库存聚合前，必须先确认 inventory 事实源、SPU 汇总/去重规则、仓库口径和状态枚举；不能用 `master_sku`、来源仓数量或临时 SQL join 冒充真实库存。
  - 后续新增 admin partner 按钮权限时，只能扩展明确按钮 ID 和 `perms` 白名单，并同步更新 `AdminDirectLoginPermissionContractTest`，不得回退到 `seller:admin:%` / `buyer:admin:%` 通配授权。
  - 后续修改三端后端验证清单时，新增的 P0/P1 合同测试必须加入 `react-ui/tests/three-terminal.manifest.json` 的 `backendTestClasses`；若测试类名不匹配现有 critical pattern，也要加入 `criticalBackendExplicitTestClasses`。

## 管理端商品分销权限 Guard 模板

- 位置：
  - `react-ui/config/routes.ts`
  - `react-ui/config/routes.js`
  - `react-ui/src/pages/Product/Distribution/EditPage.tsx`
  - `react-ui/src/pages/Product/Distribution/EditPage.js`
  - `react-ui/tests/product-distribution-permission-guard.test.ts`
  - `react-ui/scripts/verify-three-terminal.mjs`
  - `react-ui/tests/three-terminal.manifest.json`
  - `docs/plans/2026-06-07-three-terminal-p0p1-product-distribution-permission-guard-record.md`
- 当前用途：
  - 固定管理端商品分销 create/edit 直达路由必须配置 `authority`，并使用 `@/wrappers/RemoteMenuRouteGuard`；不能只靠列表页按钮隐藏。
  - 固定 `/product/distribution/create` 使用 `product:distribution:add`，`/product/distribution/edit/:spuId` 使用 `product:distribution:edit`。
  - 固定商品分销编辑页依赖来源商品库列表前必须检查 `product:list:list`。
  - 固定商品分销编辑页依赖官方仓、三方仓列表前必须分别检查 `warehouse:official:list`、`warehouse:thirdParty:list`。
  - 固定无依赖列表权限时不主动调用对应 admin list 接口；编辑态可以继续合并后端详情返回的已绑定仓库用于回显。
  - 固定 TS/TSX 源和 JS 镜像都必须纳入静态测试；JS 镜像如果没有来源 SKU 弹窗，不强行添加不存在的 UI，但已有仓库列表请求必须同样 guard。
- 复用规则：
  - 后续新增管理端页面的 create/edit/detail 直达路由时，必须同步配置 route `authority` 和 `RemoteMenuRouteGuard`，并补静态测试；不能只隐藏入口按钮。
  - 页面内部如果调用其他管理域的 list/option/select 接口，必须先检查对应接口权限；如果该依赖是页面必需能力，应在页面权限方案里显式说明组合权限。
  - 后续如果新增 product-scoped 选择接口替代 integration/warehouse admin list 接口，需要先同步权限点、service 和静态测试，再删除本模板中的跨域依赖 guard。
  - 后续修改三端前端验证清单时，新增的 P0/P1 前端权限 guard 测试必须加入 `react-ui/tests/three-terminal.manifest.json` 的 `frontendTestPaths`；若文件名不匹配现有 critical pattern，也要扩展 `criticalFrontendTestPathPattern`。

## Product 到 Integration 公开 Facade 边界模板

- 位置：
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/ISourceReadModelRefreshService.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/SourceReadModelRefreshServiceImpl.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java`
  - `RuoYi-Vue/product/src/test/java/com/ruoyi/product/architecture/ProductModuleBoundaryContractTest.java`
  - `react-ui/scripts/verify-three-terminal.mjs`
  - `docs/plans/2026-06-07-three-terminal-p0p1-product-integration-public-facade-record.md`
- 当前用途：
  - 固定 product 模块不得 import `com.ruoyi.integration.service.impl.*`。
  - 固定来源商品库和来源库存读模型刷新由 integration 内部实现，product 只依赖 `ISourceReadModelRefreshService` 公开合同。
  - 固定商品来源绑定释放、投影同步后，只通过 facade 通知读模型刷新，不直接知道 `SourceProductReadModelService` / `SourceWarehouseStockReadModelService` 两个内部实现类。
  - 固定商品来源绑定释放、投影同步后，调用 `refreshOfficialMasterSkuPairingByConnection(...)` 刷新 SKU pairing 派生链路：来源商品读模型、库存 SKU pairing 快照、来源仓库存读模型和来源库存总览。
- 复用规则：
  - 后续 product、warehouse、finance、seller、buyer 等模块如需触发 integration 内部读模型刷新或同步投影，必须先在 `com.ruoyi.integration.service` 暴露公开 service/facade；不得跨模块 import `integration.service.impl`。
  - 后续如果跨模块写入或替换 `upstream_system_sku_pairing` 投影，必须通过同一 facade 刷新受影响 `connectionCode`，不能只刷新商品侧 `spuId` 库存总览。
  - 后续新增跨模块 facade 时，同步补架构合同测试，扫描调用方主代码是否依赖被调用方 `impl` 包。
  - `ProductModuleBoundaryContractTest` 当前只固定 product -> integration impl 边界；如后续发现 warehouse/finance 等模块重复问题，应新增各自模块边界测试，而不是扩大 product 测试范围。
  - mapper 直接读写 integration/source 表不属于本模板已解决范围；迁移前必须先定来源快照、SKU pairing 投影和官方仓派生的事实归属。

## 端内 Role-Menu 运行时复核与 Owner 角色约束模板

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalPermissionSupport.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalPermissionCheckerTest.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplTest.java`
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java`
  - `docs/plans/2026-06-07-three-terminal-p0p1-role-menu-runtime-owner-session-contract-record.md`
- 当前用途：
  - 固定 role-menu 绑定不能只校验菜单 ID 存在；写入前必须逐条读取端内菜单并复核菜单 ID 区间、权限前缀、通配符、管理端命名空间和页面 component 根路径。
  - 固定 seller 菜单 ID 使用 `100000-199999`，buyer 菜单 ID 使用 `200000-299999`。
  - 固定 role-menu `count*MenusByIds` 存在性计数也必须带同一 ID 区间，避免历史脏数据或未来脚本回退时让跨端 ID 在计数阶段被误判为合法。
  - 固定 role-menu `count*MenusByIds` 和 `batch*RoleMenu` 必须过滤 `status = '0'` 的可用菜单；当前 seller/buyer 菜单表没有 `del_flag`，未来若新增软删字段，应把 `del_flag = '0'` 纳入同一 guard。
  - 固定 owner 角色绑定为双向约束：owner 账号必须保留 owner 角色，非 owner 账号禁止绑定 owner 角色。
  - 固定无权限串的 portal 接口也必须经过端内 `IPortalPermissionCheckService.selectPermissions(...)`，以触发 DB session 活性校验。
  - 固定 seller/buyer role、dept、menu 管理控制器的路由形态，防止权限名正确但路由串端。
- 复用规则：
  - 后续新增 seller/buyer 端内 role-menu、account-role 或批量授权功能时，必须复用 `PortalPermissionSupport.assertTerminalMenuId(...)`、`assertTerminalMenuComponent(...)`、`assertTerminalMenuPerms(...)`；`count(*)` 只能作为 fail-closed 的前置存在性检查，且必须带当前端 ID 区间。
  - role-menu 写入 SQL 不能只依赖 service 层校验；批量 insert/select join 菜单表时也必须过滤当前端可用菜单。
  - 后续扩展 owner/admin/staff 角色模型时，必须先定义账号类型与角色 key 的允许矩阵，再修改 `assignAccountRoles(...)` 双向约束；不能让员工账号获得 owner 权限。
  - 后续新增无权限串的 portal 自助接口时，仍必须经过 `PortalPermissionChecker.requireAuthorized(...)` 或等价的端内 permission service 路径，不能只依赖 Redis/JWT token 存活。
  - 后续修改 seller/buyer 管理端 role、dept、menu 控制器时，必须同步更新 `SellerAdminPermissionContractTest` / `BuyerAdminPermissionContractTest`，保证权限点、路由形态和端前缀一起变更。

## 国家/地区字典权威校验模板

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PartnerSupport.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PartnerSupportTest.java`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java`
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`
  - `docs/plans/2026-06-07-three-terminal-p0p1-country-region-dict-authority-record.md`
- 当前用途：
  - 固定 seller/buyer 主体资料 `countryCode` 不能只靠前端下拉或 2 位长度校验。
  - 固定 `countryCode` 先归一化为 `[A-Z]{2}`，再基于若依 `country_region` 字典启用项做 membership 校验。
  - 固定 `country_region` 字典缺失时 fail-closed，不能静默接受任意两位代码。
  - 固定 seller/buyer 主体新增在国家/地区 code 不在字典范围内时，必须在 mapper 写入前失败。
- 复用规则：
  - 后续任何主体资料、仓库资料或外部系统映射如果保存国家/地区 code，应优先复用若依 `country_region` 字典作为后端服务层权威来源；不能只依赖前端下拉。
  - 新增共享字典校验时，应优先放在 `PartnerSupport` 或同级 support/helper 中，并由具体 service 注入若依字典服务读取启用项。
  - 如果未来将 `country_region` 拆成独立 seed 或更完整的国家主数据，必须保留 `COUNTRY_REGION_DICT_TYPE` 和现有服务层 membership 语义，避免回退到长度校验。

## 端内自助审计可见 DTO 边界模板

- 位置：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/ISellerService.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/IBuyerService.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/BuyerPortalController.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalOwnLoginLogProfile.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalOwnOperLogProfile.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalOwnSessionProfile.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalSelfServiceSurfaceContractTest.java`
  - `docs/plans/2026-06-07-three-terminal-p0p1-role-menu-ticket-scope-guard-record.md`
- 当前用途：
  - 固定 seller/buyer 端内自助登录日志、操作日志、会话列表的 service 返回类型必须是 `PortalOwn*Profile`，不能把 `PortalLoginLog`、`PortalOperLog`、`PortalSessionProfile` 内部审计模型交给 controller。
  - 固定 controller 只返回 service 的可见 DTO 列表，不负责剥离 `directLoginTicketId`、`actingAdmin*`、`directLoginReason`、`operParam`、`jsonResult`、`subjectId`、`accountId`、`tokenId` 等内部字段。
  - 固定 service 做 DTO 映射时必须保留 PageHelper `Page` 元数据，避免分页总数、页码、页大小在映射后丢失。
- 复用规则：
  - 后续新增 seller/buyer 端内自助审计、日志、会话或安全记录接口时，service 边界就应返回端内可见 DTO，不要把内部审计 domain 传到 controller 后再过滤。
  - 如果端内可见字段要增加，优先扩展 `PortalOwn*Profile` 并同步序列化/架构合同；不得直接复用管理端审计模型。
  - 涉及分页列表 DTO 映射时，优先复用现有 PageHelper 元数据复制模式，不能把 `Page` 转成普通 `ArrayList` 后再交给 `getDataTable(...)`。

## Portal 菜单读路径运行时复核模板

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalPermissionSupport.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImplMenuTreeTest.java`
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImplMenuTreeTest.java`
  - `docs/plans/2026-06-07-three-terminal-p0p1-role-menu-ticket-scope-guard-record.md`
- 当前用途：
  - 固定 seller/buyer 登录后菜单树下发不能只相信 role-menu 写路径和 SQL join；读出菜单后也必须逐条 fail-closed 复核。
  - 固定 seller 菜单 ID 只能位于 `100000-199999`，buyer 菜单 ID 只能位于 `200000-299999`。
  - 固定页面菜单必须使用当前端页面根路径，权限必须使用当前端前缀，不能使用通配权限或 `seller:admin:` / `buyer:admin:` 管理端命名空间。
  - 固定历史脏菜单不能被跳过、降级或回退到前端占位页；后端应在菜单树构建前拒绝。
- 复用规则：
  - 后续新增 seller/buyer portal 菜单、工作台导航、快捷入口或端内 role-menu 派生读模型时，优先复用 `PortalPermissionSupport.assertReadableTerminalMenu(...)`。
  - 写路径的 `normalizeTerminalMenu(...)` 不能替代读路径复核；任何从数据库、缓存、投影表或批量 join 读出的菜单，只要会下发给 portal 前端，就要在返回前重新复核。
  - 如果未来新增 `seller_menu` / `buyer_menu` 软删字段或扩展菜单类型，需要同步扩展 `assertReadableTerminalMenu(...)` 和 seller/buyer 菜单树测试。

## 上游 SKU 配对投影删除作用域模板

- 位置：
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductDistributionServiceImpl.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/mapper/ProductDistributionMapper.java`
  - `RuoYi-Vue/product/src/main/resources/mapper/product/ProductDistributionMapper.xml`
  - `RuoYi-Vue/product/src/test/java/com/ruoyi/product/architecture/ProductDistributionMapperContractTest.java`
  - `docs/plans/2026-06-07-three-terminal-p0p1-product-upstream-sku-pairing-scope-record.md`
- 当前用途：
  - 固定 `product` 侧临时维护 `upstream_system_sku_pairing` 投影时，不允许再按裸 `system_sku` 删除。
  - 删除上游 SKU 配对投影必须先按 `sourceDimensionGroupKey` 解析 connection 集合，再使用 `connection_code in (...) + system_sku` 作为删除作用域。
  - 解析删除作用域的 connection 集合不得只过滤 `status = 'ACTIVE'`；否则旧来源维度失效后，换绑或解绑时会留下旧 connection 下的投影。
  - SKU 来源换绑或系统 SKU 变化时，必须先按旧绑定的来源维度删除旧投影，再按新绑定来源维度重新 upsert 投影，避免旧 connection 残留或新 connection 缺失。
  - `ProductDistributionMapperContractTest` 必须固定该删除作用域，防止回退到 `deleteUpstreamSkuPairingsBySystemSku` 这类裸删除。
- 复用规则：
  - 后续如果继续保留 product -> integration/source 表的 mapper debt，新增任何写 `upstream_system_sku_pairing` 的语句都必须进入 statement 级 allowlist，并说明 connection 作用域。
  - 更完整的长期方向仍是把 `upstream_system_sku_pairing` 写入下沉到 integration 公开 facade；在完成 facade 迁移前，product 侧只能做受合同约束的窄投影维护。

## 三端前端 Guard Manifest 与 Service URL 合同模板

- 位置：
  - `react-ui/tests/three-terminal.manifest.json`
  - `react-ui/scripts/verify-three-terminal.mjs`
  - `react-ui/scripts/check-partner-management-template.mjs`
  - `react-ui/src/services/seller/seller.ts`
  - `react-ui/src/services/buyer/buyer.ts`
  - `docs/plans/2026-06-07-three-terminal-p0p1-frontend-guard-terminal-menu-range-record.md`
- 当前用途：
  - 固定三端前端 guard 入口必须登记在 `frontendGuardScripts`，不能只硬编码在 `verify-three-terminal.mjs`。
  - 固定 `verify-three-terminal` 必须从 manifest 读取 guard、前端测试和后端合同测试，并校验 package script 存在且命令文本等于 manifest 的 `expectedCommand`。
  - 固定 `package.json` 中所有 `guard:*` 脚本都必须反向登记到 manifest，不能新增关键守门脚本后被 `verify-three-terminal` 静默跳过。
  - 固定 seller/buyer 管理端 service 函数必须按函数名绑定对应 `/api/seller/admin/**` 或 `/api/buyer/admin/**` URL，不能只靠文件级路径扫描。
  - 固定 seller/buyer 管理端页面不得直接 `request(...)`，不得导入对端 service 或 `/api/system/**`。
- 复用规则：
  - 后续新增三端关键前端 guard 时，必须先写入 `frontendGuardScripts`，同时填写 `name` 和 `expectedCommand`，再由 `verify-three-terminal` 统一执行。
  - 后续修改 `guard:*` 的 package script 命令时，必须同步更新 manifest；如果命令漂移，manifest 自检必须失败。
  - 后续新增 seller/buyer admin service 函数时，必须同步扩展 `check-partner-management-template.mjs` 的函数级 URL 合同，避免函数名和 URL 串端。
  - 如果未来拆出 `seller-ui` / `buyer-ui`，管理端保留该函数级 URL 合同；portal 端应建立同等清单化 guard，而不是复制散落脚本。

## 端内菜单 ID 与自增段位 Guard 模板

- 位置：
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - `RuoYi-Vue/sql/20260604_portal_account_list_permission_seed.sql`
  - `RuoYi-Vue/sql/20260604_portal_dept_role_list_permission_seed.sql`
  - `RuoYi-Vue/sql/20260604_portal_product_category_permission_seed.sql`
  - `RuoYi-Vue/sql/20260607_portal_self_audit_permission_seed.sql`
  - `RuoYi-Vue/sql/20260604_seller_product_schema_permission_seed.sql`
  - `RuoYi-Vue/sql/20260604_buyer_product_schema_permission_seed.sql`
  - `RuoYi-Vue/sql/20260607_terminal_menu_id_range_isolation.sql`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalSqlIsolationContractTest.java`
  - `docs/plans/2026-06-07-three-terminal-p0p1-frontend-guard-terminal-menu-range-record.md`
- 当前用途：
  - 固定 `seller_menu` 数字 ID 和 `AUTO_INCREMENT` 只能位于 `100000-199999`。
  - 固定 `buyer_menu` 数字 ID 和 `AUTO_INCREMENT` 只能位于 `200000-299999`。
  - 固定端内菜单 seed 在插入前必须调用 `assert_terminal_menu_range_ready()`，并同时校验已有 ID 与下一次自增 ID 的上下界。
  - 固定菜单 ID 重排脚本迁移后必须校验主键、`parent_id` 和 role-menu 绑定都进入最终端内 ID 段，再重置自增并复核。
- 复用规则：
  - 后续任何写入 `seller_menu` / `buyer_menu` 的 seed 都必须保留上下界 guard，不允许只写 `AUTO_INCREMENT >= floor`。
  - 端内菜单 ID 重排、补 seed 或历史库修复脚本必须纳入 `TerminalSqlIsolationContractTest`，避免菜单 ID 段污染另一端。
  - MySQL `ALTER TABLE ... AUTO_INCREMENT` 会隐式提交；涉及自增修复的脚本必须在记录和注释中说明事务边界，不能把普通事务当成 DDL 回滚保证。

## Portal 登录与免密消费 Token 保留模板

- 位置：
  - `react-ui/src/pages/Portal/Login/index.tsx`
  - `react-ui/src/pages/Portal/DirectLogin/index.tsx`
  - `react-ui/src/pages/Portal/DirectLogin/index.js`
  - `react-ui/scripts/check-portal-token-isolation.mjs`
  - `react-ui/tests/terminal-session-token.test.ts`
  - `docs/plans/2026-06-07-three-terminal-p0p1-token-preclear-verify-sql-guard-record.md`
- 当前用途：
  - 固定 seller/buyer 普通登录页提交前不得先清理当前端已有 token。
  - 固定 seller/buyer `/direct-login` 页面打开、等待 token 超时或消费失败时不得清理当前端已有 token，避免免密失败误踢已有端内会话。
  - 成功路径继续通过 `persistPortalLogin(...)` 覆盖写入新 token；跨端或无效响应仍由 `persistPortalLogin(...)` fail-closed，但不得清理任何已有端内 token，也不得清理响应声明的另一端 token。
- 复用规则：
  - 后续改登录页、直登页、扫码登录、SSO 或三端拆分后的 portal 登录入口时，不要在拿到有效 portal login result 之前调用 `clearPortalLogin(...)`。
  - 如果新增 JS 镜像入口，必须与 TS/TSX 一起纳入 `terminal-session-token.test.ts` 和 `check-portal-token-isolation.mjs`，避免运行入口漂移。

## 三端验证入口发现范围模板

- 位置：
  - `react-ui/scripts/verify-three-terminal.mjs`
  - `react-ui/tests/three-terminal.manifest.json`
  - `RuoYi-Vue/pom.xml`
  - `docs/plans/2026-06-07-three-terminal-p0p1-token-preclear-verify-sql-guard-record.md`
- 当前用途：
  - 固定 `verify-three-terminal` 从 manifest 读取后端测试、前端测试和前端 guard。
  - 固定前端关键测试自动发现从 `react-ui` 仓库级根目录开始，沿用 ignored dirs 和关键路径过滤，不只扫描 `tests` / `src`。
  - 固定后端合同测试执行模块从 `RuoYi-Vue/pom.xml` reactor 动态派生存在 `src/test/java` 的模块，不再长期硬编码少数模块。
- 复用规则：
  - 后续新增三端 P0/P1 前端测试时，只要文件名命中关键模式，就必须登记 manifest，否则 `--check-manifest` fail-closed。
  - 后续在 `inventory`、`warehouse`、`finance` 或新 Maven 模块新增三端关键测试时，不需要手工扩展 Maven `-pl` 清单，但测试类仍必须进入 manifest。

## 三端隔离 SQL 空密码与菜单段位 Preflight 模板

- 位置：
  - `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - `docs/plans/2026-06-07-three-terminal-p0p1-token-preclear-verify-sql-guard-record.md`
- 当前用途：
  - 固定三端隔离迁移不能把存量 `seller_account` / `buyer_account` 的 null 或空白密码规范成 `''`。
  - 固定 `assert_no_blank_terminal_passwords(...)` 必须在 `update seller_account` / `update buyer_account` 前 fail-closed，要求先 backfill、重置或人工修复密码。
  - 固定综合 seed 的 `assert_terminal_menu_range_ready()` 必须在 `sys_config`、`seller_role`、`buyer_role` 和端内菜单权限写入前执行，避免跨端菜单 ID 污染时产生部分副作用。
- 复用规则：
  - 后续端账号、密码或 owner 迁移脚本不得出现 `password = coalesce(password, '')` 这类静默修复；应先 preflight，再让显式修复脚本处理数据。
  - 后续综合 seed 或补权限脚本只要依赖 `seller_menu` / `buyer_menu` ID 段，就必须在任何端内角色、菜单、权限授权写入前执行区间 guard。

## 库存总览平台库存读模型与操作模板

- 位置：
  - `RuoYi-Vue/inventory`
  - `RuoYi-Vue/sql/20260607_inventory_overview_platform_stock.sql`
  - `RuoYi-Vue/sql/20260608_inventory_overview_sku_baseline_refresh.sql`
  - `react-ui/src/pages/Inventory/Overview`
  - `react-ui/src/services/inventory/overview.ts`
  - `react-ui/src/types/inventory/overview.d.ts`
  - `docs/plans/2026-06-07-inventory-overview-platform-stock-design.md`
- 当前用途：
  - 固定库存总览以平台可售库存为核心，不复用来源仓库库存页面作为事实操作页。
  - 固定库存总览读模型从商城 SKU 起步，来源 SKU 绑定和仓库绑定缺失时必须展示问题状态，不能让列表空表隐藏配置问题。
  - 固定官方仓按来源主仓库存派生，页面只展示来源主仓名，例如 `CA012`，不展示上游系统名或外部仓库编码前缀。
  - 固定平台库存字段命名为 `platform_total_qty` / “平台总库存”，并用 SKU + 仓库来源维度行承载调整；多个官方来源主仓或多个三方仓都按明细行拆开，SPU/SKU 主行只做聚合。
  - 固定 SPU 展开层按一个平铺表展示 `SKU + 仓库` 明细，SKU 信息作为第一列重复展示；不得回退为“每个 SKU 一个卡片/小表/重复表头”的结构。
  - 固定库存总览仓库筛选选项从 `inventory_sku_warehouse_stock` 当前明细读模型提取，不能直接复用仓库管理选项而漏掉官方来源主仓或占位仓库。
  - 固定库存调整必须先 preview 再 confirm，写入 `inventory_stock_ledger`，不能直接覆盖库存状态。
  - 固定库存调整原因是选填备注，前端不得拦截空原因，后端只做 trim 和空白归一，不得把原因作为保存前置条件。
  - 固定 SPU / SKU 聚合行的“调整库存”只作为入口，最终必须展开为 `inventory_sku_warehouse_stock.stock_id` 明细行批量调整，不允许直接写 SPU/SKU 聚合读模型。
  - 固定批量调整必须整批校验、整批成功；任一明细行不允许保存时，后端必须拒绝整批请求，不能部分保存合法行。
  - 固定通用 `business_menu_seed.sql` 不再拥有 `2420` 库存总览菜单，专用库存 SQL 负责菜单和按钮权限。
- 复用规则：
  - 后续订单占用、出库扣减、在途转可售和库存流水页面应复用 `inventory` 模块的库存行、锁定表和流水表，不要在订单或商品模块里重复计算平台可售库存。
  - 后续新增库存调整入口必须走后端 preview/confirm 合同，并写库存流水；前端只能展示后端返回的风险提示，不能自行模拟库存业务规则。
  - 后续新增库存调整入口可以传调整原因，但只能作为选填审计备注；如原因为空，应继续执行 preview/confirm 业务校验。
  - 后续新增聚合维度调整、批量导入调整或自动化释放动作时，都必须复用“明细行预览 -> 二次确认 -> 明细行落库 -> 刷新 SKU/SPU 读模型”的链路。
  - 后续调整 SPU 展开层展示时，优先在 `WarehouseStockTable` 上扩展列或行样式，不要重新引入多层嵌套表格。
  - 后续库存总览新增数量区间或时间区间筛选时，应复用 `buildInventoryOverviewListParams(...)` 展开为后端 min/max 或 start/end 参数，不要在页面和仓库视图里各写一套转换逻辑。
  - 后续来源库存同步落库完成后，应通过读模型刷新逻辑更新 `inventory_sku_warehouse_stock` 和 SKU/SPU 汇总表，不要让库存总览直接做大聚合查询。

## 管理端端内会话查看与强退权限拆分模板

- 位置：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java`
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx`
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SellerAdminPermissionContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/BuyerAdminPermissionContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminAccountPermissionUiContractTest.java`
- 当前用途：
  - 固定管理端查看 seller/buyer 端在线会话使用只读权限 `seller:admin:session:list` / `buyer:admin:session:list`。
  - 固定真正强制踢出会话才使用 `seller:admin:forceLogout` / `buyer:admin:forceLogout`。
  - 固定共享 PartnerManagement 模板中主体级和账号级“会话”入口不得继续绑定强退权限。
- 复用规则：
  - 后续新增端内 session 页面、账号弹窗、批量操作或三端拆分后的管理端 UI 时，查看和强退必须分成两个权限点。
  - seller 侧先做标准样板后，buyer 侧只能替换端类型、文案、路由、权限前缀和 service，不要重新设计权限口径。

## 高影响 SQL 精确目标与读模型原子刷新模板

- 位置：
  - `RuoYi-Vue/sql/20260606_legacy_disable_sys_seller_buyer_roles.sql`
  - `RuoYi-Vue/sql/20260608_inventory_overview_sku_baseline_refresh.sql`
  - `RuoYi-Vue/sql/20260607_source_warehouse_stock_read_model.sql`
  - `RuoYi-Vue/sql/20260607_source_product_read_model.sql`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - `docs/plans/2026-06-08-three-terminal-p0p1-session-permission-sql-portal-inventory-contract-record.md`
- 当前用途：
  - 固定 legacy `sys_role` 清理必须按预览确认的 role_id 集合和精确签名执行，不能只靠 `role_key=seller/buyer`。
  - 固定库存总览读模型清空和重建必须有事务边界，避免删除后中断提交空表。
  - 固定来源商品和来源仓库库存读模型刷新属于全量重建读模型脚本，当前治理要求是 staging/临时表加事务保护，并由 `SqlExecutionGuardContractTest` 固定删除和重建的原子边界；它不等同于历史库清理脚本的“人工确认精确目标集合”。
- 复用规则：
  - 后续历史库清理脚本必须要求确认 token、精确目标集合、目标签名/hash 和 `45000` fail-closed。
  - 后续全量删除再重建的读模型刷新脚本，优先 staging/swap；若使用事务，必须把删除和重建 insert 放入同一事务，并在记录中说明事务边界。
  - 如果未来把读模型全量刷新也升级为 preview-confirmed exact target 标准，必须先统一修改本模板和合同测试；在此之前，不要把读模型刷新和历史清理脚本混用同一验收口径。

## Portal 改密与库存总览核心交互契约模板

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalPasswordChangeContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/InventoryAdminRouteContractTest.java`
  - `react-ui/tests/three-terminal.manifest.json`
  - `react-ui/src/services/portal/session.ts`
  - `react-ui/src/services/inventory/overview.ts`
  - `react-ui/src/pages/Inventory/Overview/components/WarehouseViewTable.tsx`
  - `react-ui/src/pages/Inventory/Overview/components/SpuSkuWarehouseTable.tsx`
  - `react-ui/src/pages/Inventory/Overview/components/SkuWarehouseTable.tsx`
  - `react-ui/src/pages/Inventory/Overview/components/QuantityCell.tsx`
- 当前用途：
  - 固定 seller/buyer portal 改密必须走 `/api/{terminal}/account/password`、`PUT`、端 token header 和当前端 session 推导。
  - 固定库存总览 `warehouse/list`、`warehouse/options`、`spu/{spuId}/sku-warehouses`、`warehouses`、`adjust preview`、`adjust confirm`、`ledger` 的后端权限、service URL 和前端组件绑定。
  - 固定库存总览配对状态筛选复用 `react-ui/src/services/integration/constants.ts` 的 SKU 配对选项；未配对只包含 `NO_WAREHOUSE` 和 `SOURCE_UNBOUND`。
- 复用规则：
  - 后续 portal 自助改密、换绑邮箱、换绑手机等账号自助入口必须进入 three-terminal manifest，不允许只靠编译覆盖。
  - 后续库存总览新增核心交互时，controller 权限、service URL、组件调用和 manifest 合同必须一起更新。
  - 后续库存总览新增配对相关筛选或标签时，不要在库存页面重新维护一套 `已配对/未配对` 文案映射。

## Portal Redirect 白名单与 Integration Admin Route 合同模板

- 位置：
  - `react-ui/src/utils/portalPaths.ts`
  - `react-ui/src/utils/portalPaths.js`
  - `react-ui/src/pages/Portal/Login/index.tsx`
  - `react-ui/scripts/check-portal-token-isolation.mjs`
  - `react-ui/tests/portal-session-request.test.ts`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/IntegrationAdminRouteContractTest.java`
  - `react-ui/tests/three-terminal.manifest.json`
- 当前用途：
  - 固定 portal 登录 redirect 只接受当前端登录页、直登页和 `/{terminal}/portal` 子树，不再用 `/seller/*`、`/buyer/*` 粗略判断。
  - 固定 `/seller/accounts`、`/buyer/admin/menus` 等管理端路径不得被当成 portal path。
  - 固定 integration admin controller 与 React service 只能走 `/integration/admin/**` 和 `/api/integration/admin/**`。
- 复用规则：
  - 后续新增 portal 页面时，必须先进入 `PORTAL_ROUTE_PREFIXES` / `isPortalTerminalPath(...)` 白名单，再允许登录 redirect。
  - 后续新增 integration 管理端 service、页面或 controller 时，必须补入 `IntegrationAdminRouteContractTest` 和 three-terminal manifest，不能只依赖 compile 覆盖。

## 端内菜单 Perms 唯一性与 Cleanup 目标签名模板

- 位置：
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - `RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`
  - `RuoYi-Vue/sql/20260606_admin_partner_non_admin_button_grant_cleanup.sql`
  - `RuoYi-Vue/sql/20260607_admin_partner_owner_reset_permission_cleanup.sql`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
- 当前用途：
  - 固定 `seller_menu` / `buyer_menu` 的 `perms` 必须唯一，角色菜单授权按 `perms` join 前必须先 fail-closed。
  - 固定端内菜单不得出现空权限、`*`、外端前缀或 `seller:admin:` / `buyer:admin:` 管理端命名空间。
  - 固定高影响 partner cleanup 不只确认数量，还要确认 SHA-256 精确目标签名。
- 复用规则：
  - 后续新增端内菜单 seed 时，必须依赖唯一 `perms`，不得用可能返回多行的宽松 join 绑定角色。
  - 后续 cleanup 删除/修改权限授权、菜单、角色或高影响业务数据时，必须同时具备确认 token、预览数量和精确目标签名。

## 来源库存预捕获刷新模板

- 位置：
  - `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/IInventoryOverviewService.java`
  - `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryOverviewServiceImpl.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSystemServiceImpl.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSyncServiceImpl.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/SourceReadModelRefreshServiceImpl.java`
  - `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventoryOverviewRefreshContractTest.java`
- 当前用途：
  - 在按 connection 删除并重建 `source_warehouse_stock_detail` 前，先捕获当前受影响 SPU。
  - 重建后用重建前集合和重建后集合的并集刷新库存总览，避免某个来源库存组整组消失后旧读模型残留。
- 复用规则：
  - 后续任何会删除/重建来源库存明细或改变来源 SKU/仓库配对投影的流程，都必须先捕获受影响 SPU，再重建，再带预捕获集合刷新库存总览。
  - 不要只在重建后从新明细反查受影响 SPU；整组消失时新明细为空，会漏刷旧库存快照。

## Integration 到 Warehouse 事实源端口模板

- 位置：
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/IWarehouseFactLookupService.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/domain/WarehouseFact.java`
  - `RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/service/impl/WarehouseFactLookupServiceImpl.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSystemServiceImpl.java`
  - `RuoYi-Vue/integration/src/test/java/com/ruoyi/integration/architecture/IntegrationAdminPermissionContractTest.java`
- 当前用途：
  - integration 上游仓配对保存时，通过端口回查 warehouse 模块事实源，只接受正常官方仓，并使用事实源返回的仓库编码和名称落库。
  - 避免 integration 直接依赖 warehouse Maven 模块，防止与 warehouse 已依赖 integration 的方向形成循环依赖。
- 复用规则：
  - 后续 integration 需要读取仓库事实时，优先复用 `IWarehouseFactLookupService` 端口，不要直接读 warehouse 表，也不要把请求里的仓库名称、状态或类型当成可信事实。
  - 如果需要更多仓库事实字段，扩展 `WarehouseFact` 和 warehouse 端实现，并同步补合同测试。

## Product/Upstream JS 镜像 Guard 与库存批量调整合同模板

- 位置：
  - `react-ui/scripts/check-product-upstream-js-mirrors.mjs`
  - `react-ui/tests/three-terminal.manifest.json`
  - `react-ui/package.json`
  - `react-ui/tests/upstream-system-permission-guard.test.ts`
  - `react-ui/tests/inventory-overview-contract.test.ts`
  - `react-ui/src/services/inventory/overview.ts`
  - `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventoryOverviewRefreshContractTest.java`
- 当前用途：
  - 固定 Product、UpstreamSystem、product service、integration service 下存在 TS/TSX 同名源文件的 JS 镜像必须是纯 re-export，避免运行入口绕过 TS/TSX 权限、路由和 service 合同。
  - 固定 `guard:product-upstream-mirrors` 必须登记到 `three-terminal.manifest.json`，由 `verify:three-terminal` 统一执行。
  - 固定库存总览批量调整前端 service 必须暴露 `previewInventoryOverviewBatchAdjust` / `confirmInventoryOverviewBatchAdjust`，并使用 `/api/inventory/admin/overview/adjust/batch-preview` 和 `/adjust/batch-confirm`。
  - 固定库存总览运行时刷新合同只检查 `refreshProductInventoryOverview` 方法体内的重建/刷新顺序，避免其它操作方法中的同名刷新调用造成全文件静态误判。
- 复用规则：
  - 后续 Product/UpstreamSystem 新增 TS/TSX 同名 JS 入口时，只允许纯 re-export；如果确需保留 JS 实现，必须先说明运行入口差异并扩展 guard，而不是绕过合同。
  - 后续新增三端关键 JS 镜像 guard 时，必须同时写入 package script、manifest 和 `verify-three-terminal` 入口，不允许只靠单独脚本本地运行。
  - 后续库存总览新增批量动作时，必须同时补后端权限/Controller、前端 service URL、页面调用和合同测试；不能只在页面里临时拼 URL。
  - 后续静态合同检查方法内部顺序时，优先切出目标方法或目标源码块再断言，不要用全文件 `indexOf` 判断跨方法调用顺序。

## InventorySyncPolicy 管理端库存同步方式组件

- 位置：
  - `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/controller/AdminInventoryOverviewController.java`
  - `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/IInventoryStockSyncPolicyService.java`
  - `RuoYi-Vue/inventory/src/main/java/com/ruoyi/inventory/service/impl/InventoryStockSyncPolicyServiceImpl.java`
  - `RuoYi-Vue/inventory/src/main/resources/mapper/inventory/InventoryStockSyncPolicyMapper.xml`
  - `RuoYi-Vue/sql/20260609_inventory_auto_wms_stock_sync_policy.sql`
  - `react-ui/src/services/inventory/overview.ts`
  - `react-ui/src/components/InventorySyncPolicy/InventorySyncPolicyButton.tsx`
  - `react-ui/src/pages/Inventory/Overview/index.tsx`
  - `react-ui/tests/inventory-overview-contract.test.ts`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/InventoryAdminRouteContractTest.java`
  - `RuoYi-Vue/inventory/src/test/java/com/ruoyi/inventory/architecture/InventoryOverviewRefreshContractTest.java`
- 当前用途：
  - 固定库存总览的“自动同步WMS库存”设置入口复用同一个 React 组件，支持卖家默认、仓库设置、SPU设置、SKU设置和 SKU+仓库明细行设置。
  - 固定后端保存的同步方式 code 为 `AUTO_SOURCE_AVAILABLE`，业务语义是按来源可用库存自动同步平台总库存；前端展示文案才使用“自动同步WMS库存”。
  - 固定来源库存刷新后先按策略重算 SKU+仓库明细行，再刷新 SKU/SPU 永久读模型，避免读模型展示旧的平台库存。
  - 固定自动同步模式下不允许手动调整平台总库存；平台在途库存仍走已有库存调整入口。
- 复用规则：
  - 后续在商品列表、卖家端或其它库存页面增加库存同步方式入口时，优先复用 `InventorySyncPolicyButton` 或抽取其稳定子组件，不要复制预览/确认表单逻辑。
  - 后续新增同步方式 code 时，必须同时更新 SQL 字典、后端策略服务、前端展示映射、合同测试和库存流水操作类型。
  - 后续来源库存刷新、SKU配对变化、仓库配对变化影响平台库存时，必须通过 `IInventoryStockSyncPolicyService` 的公开入口触发策略重算，不要在其它模块直接写库存策略字段。
