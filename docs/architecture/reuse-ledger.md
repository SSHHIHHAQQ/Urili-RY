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
  - 后续如增加“代入原因”，应写入 `reason` 字段，不要另开临时备注字段。

### PortalTokenSupport

- 位置：`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalTokenSupport.java`
- 当前用途：
  - 为卖家端、买家端签发独立登录 token。
  - 使用 `portal_login_tokens:` Redis 前缀，避免与管理端 `login_tokens:` 混用。
  - token claim 包含 `terminal`、端登录 key 和用户名；登录返回包含 `terminal`、`subjectId`、`subjectNo`、`accountId`。
  - 构建 `PortalLoginSession` 和 `PortalLoginLog`，由 seller/buyer 模块分别写入自己的会话表和登录日志表。
  - 解析卖家端/买家端请求头中的端 token，并按期望端类型读取 Redis 会话。
- 复用规则：
  - 只用于卖家端、买家端这类端内登录身份建立。
  - 不要把它用于管理端若依登录；管理端继续使用若依 `TokenService`。
  - 新增 seller/buyer 端内接口时，应从端 token 解析出的身份推导数据范围，不要相信前端传入的 `sellerId` / `buyerId`。
  - 它只提供端内会话身份；端内角色、权限和菜单读取由 seller/buyer 模块自己的 PermissionService 完成，不在 token 工具里直接查业务权限表。

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
  - 后续前端页面接入时，应调用这些接口，不要复用系统菜单 `/system/menu` 或系统角色 `/system/role` 页面直接改端内权限。
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

### PlannedPage

- 位置：`react-ui/src/pages/Common/PlannedPage/index.tsx`
- 当前用途：
  - 新业务菜单的临时占位入口。
  - 菜单种子已落地但业务表、接口、权限按钮和正式页面尚未确认时，避免前端动态路由导入失败。
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
- 当前用途：
  - 维护 `finance_currency` 平台可用币种。
  - 追加记录 `finance_currency_rate_history` 汇率历史。
  - 维护 `finance_currency_sync_config` 汇率同步设置。
  - 追加记录 `finance_currency_sync_log` 同步日志。
  - 固定接入 ShowAPI 银行汇率查询作为官方汇率来源。
  - ShowAPI 官方汇率取 `hui_out` 现汇卖出价，不取 `zhesuan` 中行折算价。
  - 使用 `rate_anchor_time` 按北京时间选择当天基准时间之后的第一条官方汇率。
- 复用规则：
  - 后续金额、余额、账单、结算、订单折算等业务需要当前可用币种时，优先读取 `finance_currency` 中启用币种。
  - 官方汇率保存到 `official_rate`，业务当前使用 `effective_rate`。
  - 汇率历史和同步日志只追加，不覆盖。
  - 外部 API 适配层只返回官方汇率候选和官方汇率时间，不能绕过财务模块直接写订单、余额、结算等业务事实表。
  - ShowAPI `appKey` 只能通过后端配置接口加密保存，SQL、Markdown、日志和前端响应不得保存或展示明文。

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
  - `react-ui/src/services/integration/upstreamSystem.ts`
  - `react-ui/src/types/integration/upstream-system.d.ts`
- 当前用途：
  - 管理端“上游系统管理”菜单页面。
  - 主仓接入新增/编辑/授权、真实同步、仓库/物流渠道/SKU 配对和请求日志查看。
  - 左侧主仓工作台、主仓排序、SKU-only 同步和 SKU 同步状态查看。
- 复用规则：
  - 后续新增外部系统管理页面时，优先复用 modal、类型和 service 分层方式。
  - 页面文案使用“同步清单”，不要在用户界面展示“候选”。
  - 表格列、状态文本、接口类型继续集中维护，不要在页面内复制大段 option 或状态映射。
  - 分页接口进入 `startPage()` 后，Service 不要再先做 `selectOne` 预查，避免污染 PageHelper 上下文。
