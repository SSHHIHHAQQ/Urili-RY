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
  - 支持类型 Tabs：全部、新增商品、新增SKU、商品资料变更、SKU资料变更、供货价变更。
  - 支持状态、提交端、提交时间和关键词筛选。
  - 支持按 `review_type` 展示列表审核重点：新增商品看新增范围、供货价、仓库、类目；新增 SKU 看新增 SKU 范围；资料变更看标题、主图和摘要；供货价变更看新旧供货价区间和影响 SKU。
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
- `EDIT_PRICE` 口径已调整为 SKU 供货价变更审核；管理端 SKU 销售价调整直接生效，不再生成商品审核单。
- 审核驳回后正式商品保持审核前状态，驳回稿保留在 `AFTER` 快照；管理端商品审核列表只负责查看、通过和驳回，不承载卖家的 `继续编辑` 入口。
- 非草稿商品审核通过后应用 `AFTER` 快照，商品销售状态保持审核前状态；已上架商品审核通过后新内容立即生效。
- 非草稿商品编辑时禁止提交删除既有 SKU，避免第一版在无删除审核类型的情况下误删正式 SKU。
- 草稿商品仍直接保存；草稿提交审核仍走 `NEW_PRODUCT`。

## 2026-06-08 编辑审核接入补充

- 后端 `PUT /product/admin/distribution-products` 根据当前商品状态分流：
  - `DRAFT`：继续调用商品保存，直接保存草稿。
  - 非 `DRAFT`：调用商品审核服务创建编辑审核单，返回 `已提交审核`。
- 后端销售价调价接口继续直接写入 SKU 销售价；供货价变更由商品审核服务创建 `EDIT_PRICE` 审核单，审批通过后才写入 SKU 供货价。
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
  - 审核类型 Tabs 增加待审核数量统计，按 `reviewStatus=PENDING` 分别显示 `全部(n)`、`新增商品(n)`、`新增SKU(n)`、`商品资料变更(n)`、`SKU资料变更(n)`、`供货价变更(n)`；审核通过或驳回后同步刷新数量。
  - 操作列去掉 `更多` 下拉，待审核行直接展示 `详情 / 通过 / 驳回`，降低审核员操作成本。
  - 修复类型 Tabs 下方大面积空白：根因是全局列表页样式会把直属 `.ant-tabs` 当作主内容容器设置 `flex: 1`，而商品审核的类型 Tabs 只是导航区；现已设置 `flex: none`，避免它吞掉剩余高度并把 ProTable 推到底部。
  - 浏览器 DOM 验证：类型 Tabs 高度 54px、`flex=0 0 auto`，查询区紧贴在 Tabs 下方，`tabsToSearchGap=0`；控制台仅保留既有 Ant Design deprecated prop 提示。
  - 删除管理端商品审核列表的 `继续编辑` 操作，并移除商品编辑页通过 `reviewId` 读取驳回稿的配套深链逻辑；管理端审核菜单只保留 `详情 / 通过 / 驳回`。
  - 浏览器验证：商品审核页已驳回行不再出现 `继续编辑`，页面正文 `hasContinueEdit=false`，该行操作列只保留 `详情`。
- 2026-06-09 审核详情直观化改造：
  - 新增 `ProductReviewBusinessPreview` 业务审核预览组件，把详情第一个 Tab 从 `审核重点` 改为 `变更预览`。
  - `NEW_PRODUCT` 作为完整商品审核上限形态：直接展示商品基础信息、商品图片、SKU 和价格、发货仓库、商品属性、商品详情图文；不展示系统说明备注或“已完整”类检查清单。
  - `ADD_SKU` 专注本次新增 SKU，详情直接展示商品上下文和新增 SKU 表。
  - `EDIT_PRODUCT_INFO` 默认展示商品资料变更字段、主图对比和修改后的详情图文。
  - `EDIT_SKU_INFO` 默认只展示发生变化的 SKU 资料字段。
  - 优化 `EDIT_SKU_INFO` 的详情显示：默认表格只展示真正发生字段变化的 SKU，规格从整段 before/after 文本改为短标签摘要，变化内容改为 `变化项` 标签；需要细看时展开当前行，按 `字段 / 修改前 / 修改后` 查看差异。
  - `EDIT_PRICE` 独立展示 SKU 供货价左右对比，不再在详情中间插入供货价汇总卡片。
  - 原 `对象明细` / `审核快照` 改为 `审计对象` / `审计快照`，保留追溯能力但不再作为主审核入口。
  - 浏览器验证：`新增商品 / 新增SKU / 商品资料变更 / SKU资料变更 / 供货价变更` 五个 type 均进入各自 `变更预览`，且保留 `审计快照`；干净会话控制台 error/warning 为 0。
  - 按审核员视角精简新增商品详情：删除 `新增商品审核需要看完整商品` 这类系统说明和 `审核检查清单 / 已完整` 自检内容，新增商品详情直接展示真实商品资料区块。
  - 按同一原则精简新增 SKU 详情：删除顶部说明提示，详情直接展示商品上下文和新增 SKU 表。
  - 继续清理所有 type 的说明型 Alert：商品资料变更、SKU 资料变更、供货价变更和兜底预览都不再展示系统备注，主审核区只保留真实资料、变化表和价格对比。
  - 浏览器验证：5 个 type 的主预览均无说明型文案残留，且各自真实内容区仍可见。
  - 将审核单号、状态、提交人、审核人、价格、仓库、变化摘要等低频核对信息从详情首屏移入 `审核基础信息` Tab；详情打开后默认首屏直接进入 `变更预览`。
  - 浏览器验证：详情默认选中 `变更预览`，存在 `审核基础信息` Tab，当前激活面板不展示 `审核单号` / `提交人` 等基础字段，直接展示商品基础信息和 SKU 价格。
  - 修正审核详情商品属性值展示：问题原因是审核预览直接展示快照里的 `valueCode` / `valueJson`，单选和多选属性会显示英文 code；现改为加载当前类目属性 schema，复用商品详情页同一套属性名和选项名映射，优先展示 `optionLabel` 中文值，布尔值展示“是/否”，历史配置缺失时才回退原 code。
  - 浏览器验证：打开 `新增商品` 审核详情，`/api/product/admin/category-attributes/schema/632` 返回 200；`商品属性` 表显示 `适用性别：男`、`适用季节：夏季`、`版型：常规`、`风格：通勤`，不再直接显示英文选项 code；控制台 error/warning 为 0。
  - 调整新增商品审核的仓库展示：主列表新增 `仓库类型` 列，将 `official / third_party / MIXED` 映射为 `官方仓 / 三方仓 / 混合`；新增商品详情改为始终展示仓库类型，只有非官方仓时才展示 `发货仓库` 指标和具体发货仓库表，官方仓新增商品不再展示发货仓库区块。
  - 兼容历史审核单旧仓库文本：`product_review_request.warehouse_summary` 若已是仓库类型 code 则直接返回；若可从 `product_spu_warehouse` 聚合出仓库类型则使用聚合结果；否则将非空历史仓库名/编号归一为 `third_party`，避免主列表继续显示 `123213` 一类仓库文本。
  - 浏览器验证：`新增商品` 类型中官方仓待审核行显示 `仓库类型：官方仓`，审核重点不再出现 `发货仓库`；打开该官方仓详情后，变更预览首屏只显示 `SKU数量 / 供货价区间 / 仓库类型：官方仓`，不显示发货仓库指标和发货仓库表。
  - 审核详情主预览升级为左右双栏对比：左侧固定为 `修改前`，右侧固定为 `修改后`，按商品详情阅读顺序展示商品基础信息、商品图片、SKU、商品属性和商品详情图文；旧值使用浅红色高亮，新值使用浅绿色高亮。
  - `ADD_SKU` 改为左侧空态、右侧新增 SKU 绿色高亮；`EDIT_SKU_INFO` 改为按 SKU 展示左右字段对比，不再要求审核员展开表格才能看修改前后。
  - 自查并修正商品审核相关中文展示：商品状态、SKU 状态、仓库类型、库存状态、审计对象类型、变化类型、快照角色、载荷类型、图片角色、锁定/绑定状态、操作日志前后状态均不再直接展示 `DRAFT / READY / BEFORE / AFTER / SPU / NO_WAREHOUSE` 等英文 code；审计快照 JSON 预览会对常见状态、仓库类型、属性类型等值做中文化展示。
  - 补强商品属性值兜底：当历史快照或属性值缺少明确属性类型时，仍会优先通过类目属性选项映射展示中文选项名，避免单选/多选属性回退显示英文 option code。
  - 修正 `EDIT_PRICE` 业务口径：供货价变更审核特指 SKU 供货价变更，管理端 SKU 销售价调整直接生效，不进入商品审核。
  - 后端新增 `ProductSkuSupplyPriceUpdateRequest` 和 `submitSkuSupplyPriceReview`，审批通过时只调用 `updateSkuSupplyPrice` 写入 `product_sku.supply_price`，不再通过审核链路写 `sale_price`。
  - 非草稿商品编辑如果只改 SKU 供货价，会被归类为 `EDIT_PRICE`；审核单主表的 `priceBeforeMin/Max`、`priceAfterMin/Max` 在该类型下保存供货价区间。
  - 前端供货价变更详情改为供货价口径和左右双栏：展示 `SKU 供货价左右对比 / 原供货价 / 新供货价`，不再展示或对比销售价。
  - 商品列表 `调整售价` 成功提示改为直接生效语义，低于供货价确认文案不再描述审核风险。
  - `npm run tsc`：通过。
  - `npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand`：通过。
  - `mvn -pl product -am -Dtest=ProductReviewServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，11 tests，0 failures/errors。
  - 浏览器运行态验证：用拦截数据构造 `EDIT_PRICE` 供货价审核单，登录 `http://127.0.0.1:8001/review-center/product-distribution` 后打开供货价变更详情，详情抽屉存在 `修改前 / 修改后 / SKU 供货价左右对比 / 原供货价 / 新供货价`，且不再出现 `原销售价 / 新销售价 / SKU 价格对比`。

### 2026-06-09 商品审核价格口径统一

- 商品审核主表的 `priceBeforeMin/priceBeforeMax/priceAfterMin/priceAfterMax` 在审核工作台中统一作为供货价区间使用。
- 新增商品、新增 SKU、商品资料变更、SKU 资料变更和供货价变更的列表重点、详情摘要和首屏指标不再强调销售价区间；供货价变更主审核视图不再展示或对比销售价，销售价只保留在非供货价变更场景的 SKU 资料列表中作为普通字段。
- 后端 `ProductReviewServiceImpl` 新增商品和商品编辑审核单均从 SKU 供货价聚合区间，避免商品列表接入时把审核摘要重新带回销售价口径。
- 后端列表和详情读取审核单时，会按审核快照重新计算供货价区间并覆盖返回值：
  - `NEW_PRODUCT`：取 `AFTER` SKU 快照的 `supplyPrice` 区间。
  - `ADD_SKU`：只取本次 `CREATE` SKU 对应 `AFTER` 快照的 `supplyPrice` 区间。
  - `EDIT_PRODUCT_INFO` / `EDIT_SKU_INFO` / `EDIT_PRICE`：取 `BEFORE` 与 `AFTER` SKU 快照的 `supplyPrice` 区间。
  - 当缺少独立 SKU 快照时，回退到 SPU 快照中的 SKU 供货价和商品供货价区间。
  - 这样可以修正历史审核单主表 `price_*` 字段曾保存销售价的情况，避免前端虽然显示“供货价区间”但实际取到销售价。
- 商品审核详情不再展示已废弃的 `卖点/sellingPoint` 字段；商品基础信息、商品图片、商品属性、详情图文和 SKU 左右对比统一改为同一字段一行的对齐式网格，避免修改前/修改后因为内容高度不同而错位。
  - 浏览器运行态验证：用拦截数据构造 `EDIT_SKU_INFO` SKU 资料变更审核单，详情抽屉存在 `修改前 / 修改后 / SKU 资料左右对比 / 客户SKU / OLD-SKU-A / NEW-SKU-A / 型号 / OLD-MODEL / NEW-MODEL`，审核员无需展开表格即可看到字段变化。
  - `codegraph sync .`：通过，已同步 CodeGraph 索引。
  - 浏览器运行态验证：登录管理端后打开 `http://127.0.0.1:8001/review-center/product-distribution`，商品审核页可加载，页面可见 `全部(1) / 新增商品(1) / 新增SKU(0) / 商品资料变更(0) / SKU资料变更(0) / 供货价变更(0)` 类型 Tabs；页面标题、筛选区、类型化表头和列表数据正常出现。控制台仅有 Ant Design deprecated prop 警告，未出现页面运行错误。
- `mvn -pl product -am -DskipTests compile`：通过。
- `mvn -pl product -am -Dtest=ProductReviewServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，13 tests，0 failures/errors；覆盖 `NEW_PRODUCT`、`ADD_SKU`、`EDIT_PRODUCT_INFO`、`EDIT_SKU_INFO`、`EDIT_PRICE` 五类审核从快照 `supplyPrice` 重算供货价区间，验证历史主表销售价旧值不会继续透出到审核工作台。
- `EDIT_PRICE` 主审核视图再次收口为只审核供货价变化：删除 `当前销售价` 左右对比行、删除 `高于销售价` 风险判断、删除未使用的价格变更表格旧实现；销售价不再进入供货价变更审核主视图。
- `mvn -pl ruoyi-system -Dtest=ProductAdminRouteContractTest test`：通过。
- `mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test`：通过。
- `mvn -pl product -am -Dtest=ProductDistributionServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `npm run tsc`：通过。
- `npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand`：通过。
- 2026-06-09 本次补充验证：
  - `npm run tsc -- --pretty false`：通过。
  - `npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand`：通过，10 tests，0 failures/errors；契约固定 `ProductReviewBusinessPreview` 不得包含 `当前销售价` / `高于销售价`。
  - `rg "当前销售价|高于销售价|原销售价|新销售价|SKU 价格对比|renderPriceChangeTable"`：商品审核源码中无命中，仅保留测试的禁止断言和文档说明。
  - 浏览器运行态验证：`http://127.0.0.1:8001/review-center/product-distribution` 可加载，当前运行数据中 `全部(0)` 且 `供货价变更(0)`，因此未打开真实详情单。
  - `codegraph sync .`：通过，已同步 2 个变更文件。
  - 删除供货价模块中间汇总卡片：`影响 SKU / 涨价 SKU / 降价 SKU / 原供货价区间 / 新供货价区间` 不再出现在详情主预览，只保留逐 SKU 的供货价左右对比。
  - 浏览器补充验证：打开 `商品资料变更` 第一条详情，详情抽屉中未出现 `影响 SKU / 涨价 SKU / 降价 SKU / 原供货价区间 / 新供货价区间` 汇总卡片标题。
- `npx playwright screenshot --timeout=15000 http://127.0.0.1:8001/user/login E:\Urili-Ruoyi\logs\product-review-login-check.png`：通过，仅验证当前前端服务可加载；商品审核业务页等待 SQL 迁移后验证。

### 2026-06-09 商品审核综合变更补充

- 审核类型仍由后端商品审核服务判断，商城商品列表只提交修改后的商品数据或供货价调整数据，不允许前端直接决定 `reviewType`。
- 后端新增 `EDIT_MIXED` / `综合变更` 审核类型，用于一次提交同时命中多类变更的场景。
- 后端编辑审核的变更范围拆为四类：
  - 商品资料：SPU 基础资料、图片、属性、详情图文、仓库等变化。
  - 新增 SKU：本次新增的 SKU。
  - SKU 资料：既有 SKU 的图片、规格、尺寸重量、仓库、状态等非供货价资料变化。
  - 供货价：既有 SKU 的供货价变化。
- 后端归类规则：
  - 只命中一类变更时，继续进入对应 type：`ADD_SKU`、`EDIT_PRODUCT_INFO`、`EDIT_SKU_INFO`、`EDIT_PRICE`。
  - 同时命中两类及以上时，统一进入 `EDIT_MIXED`。
  - `EDIT_MIXED` 审批通过时仍按完整 `AFTER` 商品快照生效。
- 前端商品审核 Tabs 增加 `综合变更`，待审核数量统计会包含该类型。
- 审核详情首屏改为实际变更范围驱动：
  - `EDIT_PRODUCT_INFO` / `EDIT_SKU_INFO` / `EDIT_PRICE` / `EDIT_MIXED` 都进入统一 `EditChangeOverviewView`。
  - 详情从 before/after 快照重新识别实际变更模块，命中的模块都展示，未命中的模块不展示。
  - 供货价变化独立展示为 `SKU 供货价左右对比`，不再混入 `SKU 资料左右对比`。
  - `SKU 资料左右对比` 明确排除供货价字段，避免供货价变化被误归类为 SKU 资料。
- 已补测试：
  - `商品资料 + 供货价` 会归类为 `EDIT_MIXED`，摘要包含 `SPU资料` 和 `供货价`。
  - `SKU资料 + 供货价` 会归类为 `EDIT_MIXED`，摘要包含 `SKU资料` 和 `供货价`。
  - `EDIT_MIXED` 列表/详情供货价区间仍从快照 `supplyPrice` 重算。
- 验证结果：
  - `mvn -pl product -am -Dtest=ProductReviewServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，15 tests，0 failures/errors。
  - `mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test`：通过，79 tests，0 failures/errors。
  - `mvn -pl ruoyi-system -Dtest=ProductAdminRouteContractTest test`：通过，1 test，0 failures/errors。
  - `npm run tsc -- --pretty false`：通过。
  - `npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand`：通过，10 tests，0 failures/errors。
  - 浏览器运行态验证：`http://127.0.0.1:8001/review-center/product-distribution` 可加载，类型 Tabs 显示 `全部(26) / 新增商品(5) / 新增SKU(5) / 商品资料变更(6) / SKU资料变更(5) / 供货价变更(5) / 综合变更(0)`；当前运行数据没有综合变更单，因此未打开真实综合详情。

### 2026-06-09 快速推进 P1 追补

- 当前仍按三端快速推进模式处理，只修 P0/P1，不做浏览器、截图、DOM 或 UI 细调。
- 子 Agent：本轮确认并关闭 6 个子 Agent，全部使用 `gpt-5.4`，未使用 GPT-5.3 Codex。
- SQL seed：
  - `product_review_type` 的 `EDIT_MIXED` 字典项已增加同类型 `dict_sort` 槽位冲突 guard。
  - 如果远端已有同类型同排序位但不同 `dict_value` 的脏字典项，脚本会 `45000` fail-closed，不会静默产生重复排序。
- 前端审核预览：
  - `EDIT_MIXED` / `EDIT_SKU_INFO` 的 SKU 资料识别补齐后端审核口径中的包装数量、币种、来源维度组、来源 SKU 组、Master SKU、手工尺寸重量和来源尺寸重量。
  - 供货价仍独立进入 `SKU 供货价左右对比`，不会重新混进 SKU 资料变化。
- 测试补齐：
  - `ProductReviewServiceImplTest` 新增 `EDIT_MIXED` 审批生效路径，确认综合变更审批后按完整 `AFTER` 商品快照应用。
  - `product-distribution-permission-guard.test.ts` 固定前端 SKU 审核字段集。
  - `SqlExecutionGuardContractTest` 固定商品审核 SQL seed 的字典排序槽位 guard。
- 验证结果：
  - `mvn -pl ruoyi-system,product -am "-Dtest=SqlExecutionGuardContractTest,ProductReviewServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`SqlExecutionGuardContractTest` 79 tests，`ProductReviewServiceImplTest` 16 tests。
  - `.\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests\product-distribution-permission-guard.test.ts --runInBand`：通过，1 suite / 10 tests；Jest 仍有既有 open handle 提示。
  - `npm run tsc -- --pretty false`：通过。
  - `git diff --check`：通过，仅输出 LF/CRLF 换行提示。
  - `codegraph sync .`：通过，输出 `Already up to date`。
- 远端影响：本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis，SQL 脚本未回放到远端库。

## 后续建议

1. 单独设计已审核商品删除 SKU 的审核类型和生效规则，当前第一版先禁止删除既有 SKU。
2. 接 seller 端提交审核、待审锁定、驳回原因查看和驳回稿继续编辑入口。
3. 商品审核详情从当前抽屉展示合并为“商品编辑页同款只读弹窗”，和商品列表查看能力统一。
4. 商品列表补充“存在待审变更”的标识和查看审核单入口，避免卖家重复提交时只看到后端拦截。
5. 后续统一清理 Ant Design `destroyOnClose` / `Drawer.width` 废弃属性，降低浏览器控制台噪音。
