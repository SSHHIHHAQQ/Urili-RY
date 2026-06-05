# 复用台账

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
  - `react-ui/src/pages/Product/Category/index.tsx`
  - `react-ui/src/pages/Product/Attribute/index.tsx`
  - `react-ui/src/pages/Product/components/ProductImportModal.tsx`
  - `react-ui/src/services/product/product.ts`
  - `react-ui/src/types/product/product.d.ts`
- 当前用途：
  - 管理端维护多级商品分类树。
  - 管理端维护平台级商品属性库和属性自定义选项。
  - 管理端维护类目属性配置，并预览按祖先链继承合并后的发布 schema。
  - 管理端导入商品分类、商品属性和商品属性选项。
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

### 卖家端我的商城商品只读后端模板

- 位置：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/SellerPortalProductDistributionController.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/ISellerPortalProductService.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImpl.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/domain/SellerPortalProduct.java`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/domain/SellerPortalProductSku.java`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImplTest.java`
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
- 当前用途：
  - 作为 seller 端真实业务接口的数据范围控制标准模板。
  - 提供卖家端自己的商城商品列表、详情和 SKU 只读接口。
  - 端入口放在 `seller` 模块，实际只读消费 `product` 共享模块的 `IProductDistributionService`；`product` 模块不承载 `/seller/**` 路由。
  - 当前只完成 seller 模板，buyer 不在本切片复制；待卖家验收通过后，再按同构规则复制买家或单独设计买家浏览口径。
- 复用规则：
  - 列表、详情和 SKU 查询的数据范围必须来自 `PortalSessionContext.requireSession("seller")` 得到的 `PortalLoginSession.subjectId`，不得信任前端传入的 `sellerId`、`subjectId`、`accountId` 或 `terminal`。
  - Service 必须创建新的查询对象并写入当前 `sellerId`；不得直接修改或透传前端提交的 `ProductSpu` 查询对象。
  - 允许复制的列表筛选字段仅限业务筛选字段，例如 `keyword`、`sellerSpuCode`、`sellerSkuCode`、`productName`、`productNameEn`、`categoryId`、`spuStatus`；`systemSpuCode`、`systemSkuCode`、`sourceType` 等管理端或系统字段不得作为 seller 端范围来源。
  - 列表 DTO 转换必须保留 PageHelper 分页元数据，避免 `getDataTable(...)` 只能读到当前页条数。
  - 详情和 SKU 列表必须先读取商品并校验 `product.sellerId == session.subjectId`；不属于当前卖家的商品统一按“商城商品不存在”处理。
  - seller 端响应使用 `SellerPortalProduct` / `SellerPortalProductSku` DTO，不直接返回 `ProductSpu` / `ProductSku`，避免把 `sellerId`、系统 SPU/SKU、`BaseEntity` 审计字段或后台范围字段作为端内 API 标准。
  - 端入口必须使用方法级 `@Anonymous` + `@PortalPreAuthorize(terminal = "seller", hasPermi = "...")` + `@PortalLog(terminal = "seller", ...)`，并继续受 `TerminalRouteOwnershipTest` 和 `TerminalSeedPermissionContractTest` 约束。
  - 当前权限点为 `seller:product:distribution:list` 和 `seller:product:distribution:query`，只读 seed 已写入 `seller_menu` 和 active seller role 授权；本切片没有执行远程数据库 DDL/DML。
  - buyer 端后续如果复制该模板，只能替换 terminal、路径、权限前缀、日志 title、service 名称、DTO 名称和测试名；不能把 seller 商品拥有关系机械改成 buyer 拥有关系，买家浏览商品的可见性规则需要单独确认。

### 端内 Controller 鉴权模板守卫

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalRouteOwnershipTest.java`
- 当前用途：
  - 防止 `product` 共享模块重新暴露 `/seller...` 或 `/buyer...` 端入口。
  - 防止 seller/buyer 受保护 portal handler 漏掉方法级 `@Anonymous`、`@PortalPreAuthorize`、`@PortalLog` 或当前会话派生。
  - 防止 seller/buyer 受保护 portal handler 在方法签名中接收前端传入的 `sellerId`、`buyerId`、`subjectId`、`accountId` 或 `terminal` 作为身份范围边界。
- 复用规则：
  - seller/buyer 受保护端入口新增 controller 时，文件名应使用 `*Portal*Controller.java`，并避免把认证入口之外的真实业务接口放进 `*PortalAuthController.java`。
  - `TerminalRouteOwnershipTest` 会自动发现 seller/buyer 模块 controller 目录下的受保护 portal controller；新增真实业务 controller 后不需要手工维护硬编码清单。
  - seller 受保护端入口新增后先跑 seller 模板守卫；确认通过后，再按同构规则复制 buyer，只替换 terminal、路径、权限前缀和日志 title。
  - 端内业务 handler 可以接收真实业务参数，例如 `categoryId`、筛选对象或分页参数；但不能把 `sellerId`、`buyerId`、`subjectId`、`accountId`、`terminal` 作为请求参数或路径参数接入。
  - 如果某个端内查询 DTO 内含 `subjectId` 或 `accountId`，Controller 或 Service 必须用 `PortalSessionContext` 中的当前会话覆盖这些字段；更稳的模板是提供 session-scoped Service 方法，在 Service 内强制设置范围。
  - 当前账号日志这类带 `subjectId` / `accountId` 的 DTO，标准模板必须使用 session-scoped Service；Controller 覆盖 DTO 只作为历史兼容，不作为后续新增模板。
  - seller/buyer 端登录和免密消费入口属于认证入口例外，不纳入受保护 handler 模板检查；真正业务接口必须纳入。
  - 该测试只证明端入口模板不漏；具体业务权限点、数据范围、字段脱敏和审计内容仍要在对应业务接口测试或接口烟测中验证。

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
  - `PermissionServiceAccountPermissionTest` 覆盖若依实际 `@ss.hasPermi(...)` 运行时判断，证明只有主体权限或端内角色维护权限时不能通过 `*:admin:account:*`。
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
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalSeedPermissionContractTest.java`
- 当前用途：
  - 让综合初始化 seed 覆盖 seller/buyer 端内门户当前最小权限。
  - 初始化 active 卖家、买家的默认 `Owner` 端内角色。
  - 绑定 `OWNER` 账号到默认 `owner` 角色。
  - 绑定 active 端内角色到当前最小端内权限菜单。
- 复用规则：
  - 新环境默认以 `seller_buyer_management_seed.sql` 作为卖家/买家主体、端内账号、端内角色、端内菜单和端内权限初始化入口，不要只依赖零散增量 SQL。
  - 历史增量 SQL 仍可作为已运行环境补丁；新增端内权限时必须同时评估是否需要写入综合 seed 和增量 SQL。
  - 当前最小卖家端权限包括 `seller:account:list`、`seller:dept:list`、`seller:role:list`、`seller:product:category:list`、`seller:product:schema:query`。
  - 当前最小买家端权限包括 `buyer:account:list`、`buyer:dept:list`、`buyer:role:list`、`buyer:product:category:list`、`buyer:product:schema:query`。
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
  - 明文一次性 payload 写入 Redis，默认有效期 30 分钟；审计票据写入 `portal_direct_login_ticket`，只保存 `token_hash`，不保存明文 token。
  - 端地址优先读取若依参数配置 `portal.seller.web.url`、`portal.buyer.web.url`，未配置时使用本地验证占位地址。
- 复用规则：
  - 只复用在卖家端、买家端这类门户入口的免密登录场景。
  - 只负责生成和消费一次性 token；目标端消费后仍由 seller/buyer service 创建端 session，不绕过目标端账号、角色、菜单和权限模型。
  - 后续三端拆分后，卖家端/买家端应各自保留 token 消费入口，并读取自己的端账号、角色、菜单和权限体系。
  - `PortalDirectLoginSupportTest` 已守住 token hash 入库、Redis payload 30 分钟 TTL、一次性消费、跨端拒绝、过期标记和代入原因必填；后续改动免密链路时必须同步跑该测试。

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
  - 管理端审计列表不得返回 `tokenHash`、明文 token、Redis key、Authorization header 或端 session token；该边界由 `PortalDirectLoginTicketTest` 守卫。
  - `AdminDirectLoginPermissionContractTest` 已守住 ticket 列表权限点、service 层 terminal 强制过滤和 mapper XML 的 terminal 条件；后续改 ticket 审计入口时必须同步跑该测试。
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
- 当前用途：
  - 为卖家端、买家端接口提供统一的端内权限注解。
  - 统一解析端 token、校验 terminal、读取端内权限集合。
  - 支持全部权限要求、任一权限要求和若依超级权限 `*:*:*`。
  - 让 `SellerPortalPermissionServiceImpl` / `BuyerPortalPermissionServiceImpl` 以同一接口接入权限校验。
  - `PortalPreAuthorizeAspect` 会把校验通过的 `PortalLoginSession` 写入 `PortalSessionContext`，供当前请求内 Controller、Service 和日志切面复用。
  - 当前已覆盖 `/seller/getInfo`、`/seller/getRouters`、`/buyer/getInfo`、`/buyer/getRouters`。
  - 卖家端已在权限服务中回查 `seller_session` 在线状态，买家端已按同一模板回查 `buyer_session` 在线状态；Redis token 只是入口缓存，DB session 失效后，对应端受保护接口必须拒绝旧 token。
- 复用规则：
  - seller/buyer portal 端受保护接口必须方法级同时声明 `@Anonymous` 和 `@PortalPreAuthorize`：`@Anonymous` 只负责放行若依外层登录过滤，`@PortalPreAuthorize` 负责端 token、terminal 和权限校验。
  - 不要在 seller/buyer portal controller 上使用类级 `@Anonymous`；新增方法如果漏挂 `@PortalPreAuthorize`，类级匿名会让接口无意公开。
  - 后续卖家端真实业务接口优先使用 `@PortalPreAuthorize(terminal = "seller", ...)`。
  - 后续买家端真实业务接口优先使用 `@PortalPreAuthorize(terminal = "buyer", ...)`。
  - seller 端 `@PortalPreAuthorize` 鉴权必须继续经过 `SellerPortalPermissionServiceImpl.assertActiveSellerSession(...)`，并以 `seller_session.status='0'`、`logout_time is null`、`expire_time >= sysdate()` 作为当前会话仍有效的 DB 兜底判断。
  - buyer 端 `@PortalPreAuthorize` 鉴权必须继续经过 `BuyerPortalPermissionServiceImpl.assertActiveBuyerSession(...)`，并以 `buyer_session.status='0'`、`logout_time is null`、`expire_time >= sysdate()` 作为当前会话仍有效的 DB 兜底判断。
  - 不要在 Controller 或 Service 里重复手写 token 解析、权限集合读取、`*:*:*` 判断和端类型判断。
  - 端内接口需要当前主体或账号时，优先从 `PortalSessionContext.requireSession("seller" / "buyer")` 获取 `subjectId`、`accountId` 和 `terminal`。
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
- 复用规则：
  - 后续 seller/buyer 端账号、角色、菜单、部门、日志、会话逻辑必须继续使用各自端内表和端内 domain。
  - 管理端后台能力可以继续使用若依 `sys_*`，因此该测试只扫描 `seller` / `buyer` 模块，不扫描 `ruoyi-system` 或若依后台 mapper。
  - 如果端内账号权限实现移动到新的 terminal 模块，需要同步扩展本测试扫描路径。
  - 该测试只防止控制面回退，不替代端内业务数据范围、权限点、脱敏和接口烟测。

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
  - `getProTablePagination(configOrPageSize?)`
  - `getProTableScroll(x, config?)`
  - `getProTableColumnsState(persistenceKey, config?)`
- 当前用途：
  - 系统管理、系统监控、工具中心、主体管理、财务管理等 ProTable 页面筛选区。
  - 标准 ProTable 分页器，统一开启每页条数切换，并避免使用受控 `pageSize` 导致切换无效。
  - 标准 ProTable 滚动配置，统一传入 `scroll.y` 让 Ant Design 生成 fixed-header 结构，避免表头跟随数据行一起滚动。
  - 标准 ProTable 列设置持久化，使用 localStorage 记住用户调整过的列显示状态。
  - 当前先服务 `react-ui` 管理端；后续拆出 `seller-ui` / `buyer-ui` 后，卖家端和买家端列表页继续复用同一套筛选预设。
- 复用规则：
  - 后续新增 ProTable 页面时，使用 `getPersistedProTableSearch(...)`，不要直接写散落的 `search={{ ... }}`。
  - 后续新增标准分页 ProTable 时，使用 `getProTablePagination(...)`，不要直接写 `pagination={{ pageSize: ... }}`；如果确实需要受控分页，必须同步维护 `current/pageSize/onChange`。
  - 后续新增页面级 ProTable 时，使用 `scroll={getProTableScroll(x)}`；弹窗、抽屉、展开行内的小表格可以只保留横向滚动，但不得再通过页面私有 CSS 覆盖 `.ant-table-container`、`.ant-table-content` 或 `.ant-table-body`。
  - 后续需要列设置持久化时，使用 `columnsState={getProTableColumnsState('稳定页面标识-columns')}`，并给纯 `render` 列、操作列补稳定 `key`。
  - 页面级 ProTable 如果开启 `options.setting` 或 `options.density`，不要使用 `toolBarRender={false}` 关闭 toolbar；无自定义按钮时使用 `toolBarRender={() => []}` 或省略自定义按钮，让 ProTable 自带设置入口正常渲染。
  - 三端前端列表页筛选区默认采用 Ant Design Pro 原生 `vertical` 查询布局，也就是字段名在上、输入框在下，避免标签和输入框横向挤压。
  - 三端前端筛选区必须按内容区宽度响应式降列：宽屏优先一行 5 个筛选字段并给查询动作预留位置，中屏自动降为 4 个或 3 个字段，小屏降为 2 个字段；宁可换行，不允许把输入框压缩成不可用的小块。
  - 当前默认收起态显示 5 个筛选字段，避免 6 个字段刚好占满一行后，查询/重置/展开按钮被挤到第二行并贴近表格。
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

### PartnerAccountModal / PartnerAccountRoleModal

- 位置：
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerAccountRoleModal.tsx`
  - `react-ui/src/services/seller/seller.ts`
  - `react-ui/src/services/buyer/buyer.ts`
  - `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java`
  - `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`
- 当前用途：
  - 管理端卖家账号列表、新增、编辑、部门树绑定、重置默认密码、强制踢出。
  - 管理端买家账号列表、新增、编辑、部门树绑定、重置默认密码、强制踢出。
  - 管理端卖家/买家端账号角色绑定。
  - 管理端卖家/买家账号级免密代入。
- 复用规则：
  - 后续同类主体账号管理 UI 先复用 `PartnerAccountModal`，通过 `PartnerModuleConfig.services` 注入接口，不要在页面内拼接 seller/buyer 路径。
  - 新增、编辑、重置密码和账号角色分配入口通过 `PartnerModuleConfig.accountPermissions` 控制；卖家使用 `seller:admin:account:*`，买家使用 `buyer:admin:account:*`，不得在公共组件内回退混用主体权限。
  - 账号角色绑定先复用 `PartnerAccountRoleModal`，只替换 `getAccountRoles` / `assignAccountRoles` service。
  - 已确认的模式可模板化复制：卖家先做一套，买家只替换字段配置和 service。
  - 账号级免密代入通过 `PartnerModuleConfig.services.directLoginAccount` 注入，seller 使用 `/seller/admin/sellers/{sellerId}/accounts/{accountId}/directLogin`，buyer 使用 `/buyer/admin/buyers/{buyerId}/accounts/{accountId}/directLogin`；不要在公共组件内写 seller/buyer 路径分支。
  - 账号级免密确认框必须填写代入原因；前端不展示或记录免密 token 明文，后端返回链接只用于打开目标端短时入口。
  - `SellerServiceImplTest` / `BuyerServiceImplTest` 已守住账号级免密 service 行为：成功票据必须绑定传入的端内账号，跨主体账号必须拒绝，主体停用时必须拒绝。
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

### PartnerAuditModal

- 位置：
  - `react-ui/src/components/PartnerManagement/PartnerAuditModal.tsx`
  - `react-ui/src/services/seller/seller.ts`
  - `react-ui/src/services/buyer/buyer.ts`
- 当前用途：
  - 管理端查看卖家端审计数据。
  - 管理端查看买家端审计数据。
  - 统一承载登录日志、操作日志、免密票据三个只读 tab。
  - 支持工具栏全局审计入口，也支持主体行内“更多 -> 审计”按当前主体过滤。
- 复用规则：
  - 后续同类主体审计 UI 继续复用 `PartnerAuditModal`，只通过 `PartnerModuleConfig.services` 替换 `listLoginLogs`、`listOperLogs`、`listDirectLoginTickets`。
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
  - `react-ui/src/services/portal/session.ts`
  - `react-ui/src/types/seller-buyer/party.d.ts`
- 当前用途：
  - 管理端继续使用原有 `access_token` / `refresh_token` / `expireTime`，避免影响当前 admin 登录。
  - 卖家端、买家端预留独立 token key：`seller_*` / `buyer_*`。
  - `portal/session.ts` 统一封装卖家端、买家端登录、免密登录、主动退出、修改当前账号密码、`getInfo`、`getRouters`、主体资料、当前账号资料、端内账号只读列表、端内部门只读列表、端内角色只读列表、当前账号日志只读接口和当前账号会话只读接口。
  - `scripts/check-portal-token-isolation.mjs` 作为前端端内 token 静态守卫，已接入 `npm run lint`。
  - `scripts/check-portal-token-isolation.mjs` 同时作为前端 portal 请求身份范围参数静态守卫，防止 portal 页面或 service 绕过端 token，把 `sellerId`、`buyerId`、`subjectId`、`accountId`、`sellerAccountId`、`buyerAccountId` 等客户端身份范围字段作为请求参数发送。
- 复用规则：
  - 后续三端物理拆分时，卖家端、买家端前端优先复用 `setTerminalSessionToken`、`getTerminalAccessToken`、`clearTerminalSessionToken`，不要重新设计 localStorage key。
  - 后续端内页面调用当前账号、主体资料、菜单、权限和退出登录时，优先复用 `sellerPortalSessionService` / `buyerPortalSessionService` 或底层 `portal*` 方法，不要在页面里直接拼 `/seller` / `/buyer` 路径。
  - `portal/session.ts` 的登录、免密登录和端内请求必须显式设置 `isToken: false`，避免全局请求拦截器注入管理端 token。
  - `persistPortalLogin(result, expectedTerminal)` 必须校验后端返回 `terminal` 与当前 URL 端类型一致；不一致时清理相关端内 token 并返回失败，不能把 seller token 写入 buyer key，也不能写入管理端 `access_token`。
  - `src/pages/Portal/**` 和 `src/services/portal/**` 禁止调用管理端 `getAccessToken`、`setSessionToken`、`clearSessionToken`，也不得出现裸 `access_token` / `portal_login_token`；新增端内前端能力后必须运行 `npm run guard:portal-token`。
  - `src/pages/Portal/**` 不得直接调用 `request(...)` 或硬编码 `/api/seller`、`/api/buyer`；页面必须通过 `PORTAL_SERVICE` 或 `@/services/portal/session` 统一出口调用端内接口。
  - 受保护 portal 请求不得把 `sellerId`、`buyerId`、`subjectId`、`accountId`、`sellerAccountId`、`buyerAccountId` 作为 query/body/request params 发送；`terminal` 只允许作为前端本地端类型选择、URL 判断、`persistPortalLogin(..., expectedTerminal)` 和 `getTerminalAccessToken(terminal)` 的本地参数。
  - `portal/session.ts` 的日志和会话查询必须使用 `sanitizePortalQueryParams(params)` 清洗参数；分页、日志安全筛选和商品 `categoryId` 这类业务参数可以保留，但端内数据范围必须由端 token 和后端 `PortalSessionContext` 推导。
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
  - `PartnerSessionModal` 已作为 seller/buyer 管理端主体/账号会话列表 UI 模板。
- 复用规则：
  - 管理端会话列表接口只读，不承担踢出动作；强制踢出继续使用现有 DELETE 接口。
  - seller/buyer 管理端会话列表后端已形成标准模板；前端已以 seller UI 为标准模板复制 buyer，只替换配置和 service，不重新设计两套规则。
  - 管理端会话列表使用管理端若依权限，seller 复用 `seller:admin:forceLogout`，buyer 复用 `buyer:admin:forceLogout`，不要改成端内 `@PortalPreAuthorize`。
  - RuoYi `startPage()` 只作用于 Service 内第一条查询；列表 Service 方法不要先做会消耗分页的前置校验查询，数据范围必须写进最终 session 列表 SQL。
  - 管理端列表查询可以按主体 ID 或账号 ID 收窄范围，但仍不得输出 JWT、Redis key、密码、`directLoginToken` 或 session `tokenId`。
  - 管理端会话弹窗只展示状态、登录账号、登录 IP、登录时间、过期时间、退出时间；不得展示 `tokenId`、JWT、Redis key、Authorization header 或原始 token。
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
  - 直登页必须通过 `persistPortalLogin(result, expectedTerminal)` 校验后端返回的 `terminal` 与 URL 端类型一致，不一致时清理相关端 token 并失败；页面不要自行绕过持久化封装直接写 token。
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
  - 维护仓库、物流渠道、SKU 同步清单和配对关系。
  - 记录外部请求日志和 SKU 配对审计事件。
- 复用规则：
  - 后续 WMS、ERP、物流、支付等外部系统接入优先按独立适配器接入，不要让适配器直接写商品、库存、订单、履约或财务事实表。
  - 上游同步清单只承载外部系统快照和可配对对象，不作为正式业务事实源。
  - 外部请求日志只追加，敏感字段必须脱敏。

### 来源商品库只读视图

- 位置：
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/controller/AdminSourceProductController.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/domain/SourceProductItem.java`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/domain/query/SourceProductQuery.java`
  - `RuoYi-Vue/integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml`
  - `react-ui/src/pages/Product/SourceProductLibrary/index.tsx`
  - `react-ui/src/pages/Product/SourceProductLibrary/SourceProductDetailDrawer.tsx`
  - `react-ui/src/services/integration/sourceProduct.ts`
  - `react-ui/src/types/integration/source-product.d.ts`
  - `react-ui/src/services/integration/constants.ts`
- 当前用途：
  - 管理端「商品管理 / 来源商品库」首版只读列表。
  - 集中展示 `upstream_system_sku_candidate` 中各来源系统同步回来的 SKU 基础资料、识别码、分类、尺寸重量、申报信息、同步状态和配对结果。
  - 详情抽屉展示来源快照摘要，不向前端暴露原始 `source_payload_json`。
- 复用规则：
  - 来源商品库是上游 SKU 同步清单的聚合展示，不是商城商品事实源；不要为了页面再建一张重复来源商品表。
  - 首版接口复用 `product:list:list` 权限；新增导出、批量创建草稿、同步库存等动作前，必须单独补按钮权限和审计日志。
  - SKU 配对仍由「上游系统管理」维护，来源商品库只展示配对结果，不在页面内复制配对规则。
  - integration 相关来源系统、同步状态和配对状态选项复用 `react-ui/src/services/integration/constants.ts`，不要在上游系统页和来源商品库页分别维护。
  - 领星 `dangerousCargo` 保存来源 code；展示时 `1` 表示「普货」，`2-8` 才表示电池、液体、粉末、带磁等特殊属性。

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
  - 统一处理领星 OpenAPI 签名、超时、授权校验、仓库/物流渠道/SKU 拉取。
  - 通过 `LingxingRequestLogger` 写入脱敏请求日志。
- 复用规则：
  - 后续领星相关同步能力继续从该适配器扩展，不要在 Service、Controller 或 Mapper XML 中散落签名和 HTTP 调用逻辑。
  - 如果新增订单、库存、费用等领域能力，适配器只返回外部响应，业务事实落库必须经过对应业务模块。

### UpstreamSystemTask

- 位置：`RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/task/UpstreamSystemTask.java`
- 当前用途：
  - 作为若依 Quartz 的上游系统定时任务入口。
  - `upstreamSystemTask.syncSkus` 每 10 分钟同步已启用领星主仓的 SKU 清单。
  - 复用 `IUpstreamSystemService.syncSkusOnly`，不新增第二套领星签名、请求日志或 SKU 落库逻辑。
- 复用规则：
  - 后续上游系统定时任务优先挂到该 task 或同模块 task，不要在前端、Controller 或独立线程里轮询外部系统。
  - 定时任务登记使用若依 `sys_job`，让“系统监控 / 定时任务”统一启停、手动执行和查看日志。

### 上游系统 React 页面组件

- 位置：
  - `react-ui/src/pages/UpstreamSystem/components/ConnectionModal.tsx`
  - `react-ui/src/pages/UpstreamSystem/components/ConnectionSidebar.tsx`
  - `react-ui/src/pages/UpstreamSystem/components/ConnectionSummary.tsx`
  - `react-ui/src/pages/UpstreamSystem/components/PairingModal.tsx`
  - `react-ui/src/pages/UpstreamSystem/components/SkuSyncPanel.tsx`
  - `react-ui/src/pages/UpstreamSystem/components/SyncTabs.tsx`
  - `react-ui/src/pages/UpstreamSystem/style.module.css`
  - `react-ui/src/pages/UpstreamSystem/style.css`
  - `react-ui/src/services/integration/upstreamSystem.ts`
  - `react-ui/src/types/integration/upstream-system.d.ts`
- 当前用途：
  - 管理端“上游系统管理”菜单页面。
  - 主仓接入新增/编辑/授权、真实同步、仓库/物流渠道/SKU 配对和请求日志查看。
  - 左侧主仓工作台、主仓排序、SKU-only 同步和 SKU 同步状态查看。
- 复用规则：
  - 后续新增外部系统管理页面时，优先复用 modal、类型和 service 分层方式。
  - 左侧主仓列表 + 右侧详情/页签的数据工作台布局，样式集中在 `style.module.css` / `style.css`，不要在页面和组件里继续散写大段撑满高度 inline style。
  - 主数据区必须填满剩余可视高度，表格数据很少时分页器也要压在数据块底部。
  - 页面文案使用“同步清单”，不要在用户界面展示“候选”。
  - 表格列、状态文本、接口类型继续集中维护，不要在页面内复制大段 option 或状态映射。
  - 分页接口进入 `startPage()` 后，Service 不要再先做 `selectOne` 预查，避免污染 PageHelper 上下文。
