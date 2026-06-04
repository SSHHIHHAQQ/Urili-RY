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
  - 后续卖家端商品发布入口必须放在 `seller` 端模块，只读消费 product schema，不维护平台配置。
  - 后续买家端分类浏览、筛选项和商品详情入口必须放在 `buyer` 端模块，只读消费 product 分类和筛选 schema。
  - 商品属性类型、选项来源、类目属性规则模式等 code 由 SQL 初始化字典或 `pages/Product/constants.ts` 集中维护，不要在页面里复制状态映射。
  - 类目属性规则统一通过 `IProductConfigService.previewCategorySchema(categoryId)` 计算，不要在前端或其他模块手写继承合并逻辑。
  - 商品配置导入统一使用 `ProductConfigImportService` 和 `ProductImportModal` 的“下载模板、校验、确认导入”流程，不要在分类页、属性页分别复制上传解析和结果表格。
  - 商品配置导入模板统一使用 `ProductImportTemplateService` 输出“正式导入 sheet + 填写示例 sheet + 字段说明 sheet”；第一个 sheet 保持空白，示例只供复制参考，避免误导入。
  - 导入模板只使用业务 code 定位，不让用户填写数据库主键；分类父级使用父级分类编码，属性选项使用属性编码。
  - 导入只支持新增和更新，不支持导入删除；删除仍必须走页面操作和后端业务校验。
  - 本阶段明确不包含 SKU、多 SKU、库存、价格、商品发布、商品审核和外部平台属性同步。

### PartnerSupport

- 位置：`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PartnerSupport.java`
- 当前用途：
  - 卖家/买家编号生成
  - 主体类型、等级、联系地址、附件字段校验
  - 主账号昵称生成
- 复用规则：
  - 只放卖家/买家共用的机械逻辑。
  - 不要把它扩成新的“客户模块”。

### 卖家/买家端账号表

- 位置：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml`
- 当前用途：
  - 管理端创建卖家/买家端主账号和子账号。
  - 管理端重置卖家/买家端账号密码。
  - 卖家/买家列表展示主账号、账号数和最后登录时间。
  - 免密登录生成时读取端内主账号 `accountId`。
  - 卖家端/买家端登录成功后更新最后登录 IP 和最后登录时间。
- 复用规则：
  - 卖家端账号只读写 `seller_account`，买家端账号只读写 `buyer_account`。
  - 不允许重新引入 `PortalAccountSupport` 或 `PortalAccountMapper` 把端账号绑定回 `sys_user`。
  - 端内角色、菜单、部门、日志和会话分别使用 `seller_*` / `buyer_*` 表。

### PortalDirectLoginSupport

- 位置：`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java`
- 当前用途：
  - 管理端为卖家端、买家端生成免密登录 token。
  - 卖家端、买家端登录入口消费免密登录 token。
  - token 写入 Redis，默认有效期 30 分钟。
  - 端地址优先读取若依参数配置 `portal.seller.web.url`、`portal.buyer.web.url`，未配置时使用本地验证占位地址。
- 复用规则：
  - 只复用在卖家端、买家端这类门户入口的免密登录场景。
  - 只负责生成和消费一次性 token，不负责创建端 session 或绕过目标端权限模型。
  - 后续三端拆分后，卖家端/买家端应各自保留 token 消费入口，并读取自己的端账号、角色、菜单和权限体系。

### PortalDirectLoginTicketMapper

- 位置：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalDirectLoginTicket.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/mapper/PortalDirectLoginTicketMapper.java`
  - `RuoYi-Vue/ruoyi-system/src/main/resources/mapper/system/PortalDirectLoginTicketMapper.xml`
- 当前用途：
  - 记录管理端免密代入卖家端、买家端的审计票据。
  - 保存 acting admin、目标端、目标主体、目标端账号、过期时间、使用时间、使用 IP 和状态。
  - 通过 `token_hash` 唯一索引校验一次性 token，不保存明文 token。
- 复用规则：
  - 免密代入必须经过 `PortalDirectLoginSupport` 和该 ticket mapper，不要在 seller/buyer service 或前端里临时生成直登 token。
  - 卖家端和买家端共用这一张平台审计票据表，不分别复制 `seller_direct_login_ticket` / `buyer_direct_login_ticket`。
  - 后续管理端审计列表应读取 `portal_direct_login_ticket`，而不是读取 Redis payload。
  - 管理端生成免密代入票据时必须填写“代入原因”，并通过 `PortalDirectLoginSupport` 写入 `reason` 字段，不要另开临时备注字段或绕过公共支撑。

### PortalTokenSupport

- 位置：`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java`
- 当前用途：
  - 为卖家端、买家端签发独立登录 token。
  - 使用 `portal_login_tokens:` Redis 前缀，避免与管理端 `login_tokens:` 混用。
  - token claim 包含 `terminal`、端登录 key 和用户名；登录返回包含 `terminal`、`subjectId`、`subjectNo`、`accountId`。
  - 构建 `PortalLoginSession` 和 `PortalLoginLog`，由 seller/buyer 模块分别写入自己的会话表和登录日志表。
  - 解析卖家端/买家端请求头中的端 token，并按期望端类型读取 Redis 会话。
  - 批量删除端内 Redis token，供管理端强制踢出卖家/买家主体或账号在线会话。
  - 删除当前端内登录 token，供卖家端/买家端主动退出当前会话。
- 复用规则：
  - 只用于卖家端、买家端这类端内登录身份建立。
  - 不要把它用于管理端若依登录；管理端继续使用若依 `TokenService`。
  - 新增 seller/buyer 端内接口时，应从端 token 解析出的身份推导数据范围，不要相信前端传入的 `sellerId` / `buyerId`。
  - 它只提供端内会话身份；端内角色、权限和菜单读取由 seller/buyer 模块自己的 PermissionService 完成，不在 token 工具里直接查业务权限表。
  - 强制踢出必须复用 `PortalTokenSupport.deleteLoginTokens` 删除 Redis token，不要在 seller/buyer service 中拼接 `portal_login_tokens:` key。

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
- 复用规则：
  - 后续卖家端真实业务接口优先使用 `@PortalPreAuthorize(terminal = "seller", ...)`。
  - 后续买家端真实业务接口优先使用 `@PortalPreAuthorize(terminal = "buyer", ...)`。
  - 不要在 Controller 或 Service 里重复手写 token 解析、权限集合读取、`*:*:*` 判断和端类型判断。
  - 端内接口需要当前主体或账号时，优先从 `PortalSessionContext.requireSession("seller" / "buyer")` 获取 `subjectId`、`accountId` 和 `terminal`。
  - 端内主动退出也必须先经过 `@PortalPreAuthorize`，再用 `PortalSessionContext` 中的当前会话删除对应 Redis token 和更新当前 session 行；不要让前端传 `tokenId`、`sellerId`、`buyerId` 或 `accountId` 决定退出范围。
  - `PortalPreAuthorizeAspect` 不依赖 `argNames` 绑定注解参数，统一从 `MethodSignature` 读取 `@PortalPreAuthorize`，避免 Spring AOP 参数名绑定差异导致端内接口 500。
  - 管理端接口继续使用若依 `@PreAuthorize("@ss.hasPermi(...)")`，不要把管理端后台权限迁移到 `@PortalPreAuthorize`。
  - 接口涉及端内业务数据时，权限注解只负责“能不能访问该操作”；数据范围仍必须从端 token 推导 `sellerId` / `buyerId`，不能相信前端传入主体 ID。

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

### ProTable 筛选区展开状态

- 位置：`react-ui/src/utils/proTableSearch.ts`
- 方法：`getPersistedProTableSearch(config, storageKey?)`
- 当前用途：
  - 系统管理、系统监控、工具中心、主体管理等 ProTable 页面筛选区。
- 复用规则：
  - 后续新增 ProTable 页面时，使用 `getPersistedProTableSearch({ labelWidth: ... })`，不要直接写散落的 `search={{ ... }}`。
  - 管理端 ProTable 筛选区默认桌面宽度一行展示 6 个筛选字段；字段超过 6 个时按统一网格自动换行。
  - 弹窗内小表格、纯明细表、无查询条件表格等确有理由的场景可显式 `search={false}`，但不要另起一套页面内筛选布局。
  - 同一业务指标的最小/最大查询条件统一做成一个区间字段，例如余额、金额、库存数量；优先使用 Ant Design 原生组合控件和默认输入框样式，不自定义特殊容器，不使用假的禁用输入框，前端提交时再拆成后端需要的最小/最大参数。
  - 表格操作列同一行超过 2 个操作时，最多保留 2 个高频操作按钮直接展示，其余操作使用 Ant Design `Dropdown` 收进“更多”下拉菜单，不要横向平铺 3 个及以上文字按钮。
  - 表格操作列的行内操作和“更多”下拉菜单项默认只展示文字，不加操作图标；“更多”作为下拉触发器必须使用 Ant Design 小下箭头提示可展开。

### 页面级标题

- 位置：`react-ui/src/global.css`
- 当前用途：
  - 全局隐藏 `PageContainer` 自动生成的页面级标题行。
  - 全局让 `PageContainer` 内容区、直接承载的 `ProTable`、卡片和 Tabs 按剩余视口高度撑满。
  - 全局让 ProTable 分页器压在主数据块底部，并在分页器上方显示浅色横向分隔线。
- 复用规则：
  - 管理端页面顶部不再展示独立页面标题，也不要在筛选区、表格区上方手写页面级标题。
  - 后续新增页面继续使用该全局规则；需要表达局部区域时，使用表格、卡片、抽屉、弹窗自身标题。
  - 管理端页面的主数据块、表单块或占位块必须填满可视区域；内容不足时由分块高度补齐，不要让页面下半屏暴露空白背景。
  - 新增页面优先使用 `PageContainer` 直接承载 `ProTable`、`Card`、`ProCard` 或 `Tabs`；如果确实需要额外包裹层，包裹层也要保持纵向 flex 撑满。
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

### PartnerAccountModal / PartnerAccountRoleModal

- 位置：
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerAccountRoleModal.tsx`
  - `react-ui/src/services/seller/seller.ts`
  - `react-ui/src/services/buyer/buyer.ts`
- 当前用途：
  - 管理端卖家账号列表、新增、编辑、部门树绑定、重置默认密码、强制踢出。
  - 管理端买家账号列表、新增、编辑、部门树绑定、重置默认密码、强制踢出。
  - 管理端卖家/买家端账号角色绑定。
- 复用规则：
  - 后续同类主体账号管理 UI 先复用 `PartnerAccountModal`，通过 `PartnerModuleConfig.services` 注入接口，不要在页面内拼接 seller/buyer 路径。
  - 账号角色绑定先复用 `PartnerAccountRoleModal`，只替换 `getAccountRoles` / `assignAccountRoles` service。
  - 已确认的模式可模板化复制：卖家先做一套，买家只替换字段配置和 service。
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

### 三端前端 session 基础层

- 位置：
  - `react-ui/src/access.ts`
  - `react-ui/src/services/portal/session.ts`
  - `react-ui/src/types/seller-buyer/party.d.ts`
- 当前用途：
  - 管理端继续使用原有 `access_token` / `refresh_token` / `expireTime`，避免影响当前 admin 登录。
  - 卖家端、买家端预留独立 token key：`seller_*` / `buyer_*`。
  - `portal/session.ts` 统一封装卖家端、买家端登录、免密登录、主动退出、`getInfo`、`getRouters`、主体资料、当前账号资料、端内账号只读列表、端内部门只读列表和端内角色只读列表接口。
- 复用规则：
  - 后续三端物理拆分时，卖家端、买家端前端优先复用 `setTerminalSessionToken`、`getTerminalAccessToken`、`clearTerminalSessionToken`，不要重新设计 localStorage key。
  - 后续端内页面调用当前账号、主体资料、菜单、权限和退出登录时，优先复用 `sellerPortalSessionService` / `buyerPortalSessionService` 或底层 `portal*` 方法，不要在页面里直接拼 `/seller` / `/buyer` 路径。
  - 当前 `react-ui/` 仍是管理端验证入口；该基础层只为后续物理拆分降低重复改造，不代表现在立即复制 `seller-ui` / `buyer-ui`。

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
