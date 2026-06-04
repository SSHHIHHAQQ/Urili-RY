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
  - 密码仍保存在若依 `sys_user.password`，账号绑定表不保存密码。

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

### PortalAccountSupport

- 位置：`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalAccountSupport.java`
- 当前用途：
  - 创建若依端账号
  - 分配 `seller` / `buyer` 角色
  - 重置密码
  - 同步主账号资料和启停状态
  - 校验同一个 `sys_user` 不能同时绑定卖家账号和买家账号
- 复用规则：
  - 卖家和买家的公开 Service 仍然位于 `RuoYi-Vue/seller`、`RuoYi-Vue/buyer` 两个 Maven 子模块。
  - 支撑类只处理若依账号公共操作，不直接决定业务模块归属。

### PortalDirectLoginSupport

- 位置：`RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java`
- 当前用途：
  - 管理端为卖家端、买家端生成免密登录 token。
  - token 写入 Redis，默认有效期 30 分钟。
  - 端地址优先读取若依参数配置 `portal.seller.web.url`、`portal.buyer.web.url`，未配置时使用本地验证占位地址。
- 复用规则：
  - 只复用在卖家端、买家端这类门户入口的免密登录场景。
  - 只负责生成 token 和跳转地址，不负责消费 token、创建 session 或绕过目标端权限模型。
  - 后续三端拆分后，卖家端/买家端应各自实现 token 消费入口，并继续复用若依账号、角色和权限体系。

## 前端表格与表单

### ProTable 筛选区展开状态

- 位置：`react-ui/src/utils/proTableSearch.ts`
- 方法：`getPersistedProTableSearch(config, storageKey?)`
- 当前用途：
  - 系统管理、系统监控、工具中心、主体管理等 ProTable 页面筛选区。
- 复用规则：
  - 后续新增 ProTable 页面时，使用 `getPersistedProTableSearch({ labelWidth: ... })`，不要直接写散落的 `search={{ ... }}`。

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

- 位置：`RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/support/SecretCipherSupport.java`
- 当前用途：
  - 使用环境配置中的主密钥加密保存领星 Key/Secret。
  - 保存和展示时只暴露脱敏结果，不返回明文凭证。
- 复用规则：
  - 后续新增外部系统凭证时复用该加密支撑类或同等安全层，不要在业务表、SQL、日志、前端响应中保存明文密钥。
  - 缺少 `URILI_SECRET_ENCRYPTION_KEY` 时不允许保存或调用需要凭证的接口。

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
