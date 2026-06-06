# 卖家管理 / 买家管理同构程度只读检查

日期：2026-06-06

## 检查范围

- 本次先读取 `AGENTS.md`，按只读检查执行。
- 当前只对比管理端卖家管理与买家管理的同构程度，不修改买家实现。
- 检查对象：
  - `react-ui/src/pages/Seller/index.tsx`
  - `react-ui/src/pages/Buyer/index.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx`
  - `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx`
  - `react-ui/src/services/seller/seller.ts`
  - `react-ui/src/services/buyer/buyer.ts`
  - `react-ui/src/types/seller-buyer/*.d.ts`
  - `RuoYi-Vue/seller/src/main/**`
  - `RuoYi-Vue/buyer/src/main/**`
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - `docs/architecture/reuse-ledger.md`
  - 近期 seller 模板与 buyer 复制执行记录。
- 未连接 MySQL / Redis，未启动服务，未运行 DDL/DML。

## 结论摘要

当前管理端卖家管理和买家管理同构程度高。前端已经收敛为同一个 `PartnerManagementPage` 模板，卖家和买家页面只注入 `PartnerModuleConfig`、权限、字段名和 service。后端 Controller、Service、Mapper、端内菜单/角色/部门/账号、日志、会话、免密票据也基本保持一一对应。

后续复制买家时，推荐继续使用“卖家模板先验收，再按同构规则复制买家”的节奏。可复制的是结构和机械配置；不能复制的是业务语义，尤其是买家充值/余额、商品可见性、端内权限边界、session/log/token/table 归属。

## 同构程度判断

| 层级 | 当前同构程度 | 依据 | 判断 |
|---|---:|---|---|
| 前端页面入口 | 高 | `Seller/index.tsx` 与 `Buyer/index.tsx` 都返回 `<PartnerManagementPage config={...} />` | 后续不应复制整页实现，只复制配置差量。 |
| 前端共享模板 | 高 | `PartnerManagementPage` 通过 `moduleKey`、字段名、权限、service 控制卖家/买家差异 | 可继续作为管理端同构模板，但该文件已 1209 行，新增能力应优先拆子组件。 |
| 前端账号弹窗 | 高 | `PartnerAccountModal` 通过 `config.accountIdField`、`${moduleKey}_account_role`、`${moduleKey}_account_lock_status` 和 `accountPermissions` 区分端 | 字典和权限必须端内独立，不能复用 seller 字典给 buyer。 |
| 前端 service | 高 | seller/buyer service 方法名、HTTP 方法和路径结构一一对应 | 复制时替换 URL 前缀、路径变量名、类型名，不要改共享组件逻辑。 |
| 后端 Admin Controller | 高 | `AdminSellerController` / `AdminBuyerController` 的 CRUD、账号、角色、会话、免密、审计入口一一对应 | 可复制结构；必须替换 `@PreAuthorize` 前缀、`@Log` 标题、路径变量和 service 名。 |
| 后端 Service | 高 | `SellerServiceImpl` / `BuyerServiceImpl` 均为 685 行，方法域基本对应 | 复制时必须替换 terminal、表、Mapper、编号前缀、登录 URL 配置 key、ticket terminal 过滤。 |
| Mapper / SQL | 高 | `seller_*` 与 `buyer_*` 表、账号、部门、菜单、角色、日志、会话分别建模 | 可以复制 SQL 结构，不能共表、不能回退到 `sys_user/sys_role/sys_menu`。 |
| 权限契约测试 | 高 | 已有 `SellerAdminPermissionContractTest` / `BuyerAdminPermissionContractTest` / direct-login / account UI 守卫 | 后续复制买家后必须同步跑对应契约测试，不能只看页面按钮。 |

## 配置差异表

| 配置项 | 卖家管理 | 买家管理 | 复制买家时处理 |
|---|---|---|---|
| `moduleKey` | `seller` | `buyer` | 必须替换。影响权限前缀、字典前缀、端类型和共享模板条件。 |
| 页面标题 | `卖家管理` | `买家管理` | 必须替换，只用于管理端展示。 |
| 主体文案 `label` | `卖家` | `买家` | 必须替换，影响按钮、弹窗、审计标题文案。 |
| 主体 ID 字段 | `sellerId` | `buyerId` | 必须替换，不能用通用 `partnerId` 写入数据库对象。 |
| 主体编号字段 | `sellerNo` | `buyerNo` | 必须替换。 |
| 主体代码字段 | `sellerCode` | `buyerCode` | 必须替换。 |
| 主体名称字段 | `sellerName` / `sellerShortName` | `buyerName` / `buyerShortName` | 必须替换。 |
| 主体类型字段 | `sellerType` | `buyerType` | 必须替换，但字典继续共用 `subject_type`。 |
| 等级字段 | `sellerLevel` | `buyerLevel` | 必须替换。 |
| 等级字典 | `seller_level` | `buyer_level` | 不能共用，buyer 必须用 `buyer_level`。 |
| 账号 ID 字段 | `sellerAccountId` | `buyerAccountId` | 必须替换。 |
| 余额列标题 | `分销账户余额` | `账户余额` | 保留差异。当前均为占位，不得据此做真实财务逻辑。 |
| 充值占位 | 无 | `showRechargePlaceholder: true` | 买家专属占位，不要复制到卖家；也不要把占位升级成充值 CRUD。 |
| 列表模板 | `standard` | `standard` | 可复制。 |
| 搜索状态 key | `admin-seller-management` | `admin-buyer-management` | 必须替换，避免折叠状态串页。 |
| 主体权限前缀 | `seller:admin:*` | `buyer:admin:*` | 必须替换；不能抽成 `partner:admin:*`。 |
| 账号权限前缀 | `seller:admin:account:*` | `buyer:admin:account:*` | 必须替换；账号权限不能并入主体 query/edit/resetPwd。 |
| 管理端 API 前缀 | `/api/seller/admin/sellers` | `/api/buyer/admin/buyers` | 必须替换。 |
| 端菜单管理 API | `/api/seller/admin/menus` | `/api/buyer/admin/menus` | 必须替换。 |
| Controller 路由 | `/seller/admin/sellers` | `/buyer/admin/buyers` | 必须替换。 |
| Controller 类型 | `Seller` / `SellerAccount` | `Buyer` / `BuyerAccount` | 必须替换。 |
| Controller 日志标题 | `卖家管理`、`卖家账号`、`卖家免密登录` | `买家管理`、`买家账号`、`买家免密登录` | 必须替换，审计展示不能串端。 |
| 编号前缀 | `S` | `B` | 必须替换，来自 Service 常量。 |
| terminal 字符串 | `seller` | `buyer` | 必须替换。影响 token、ticket、session、log、权限。 |
| 免密 URL 配置 key | `portal.seller.web.url` | `portal.buyer.web.url` | 必须替换，不能共用一个入口配置。 |
| 免密 fallback URL | `/seller/direct-login` | `/buyer/direct-login` | 必须替换。 |
| 免密票据过滤 | `ticket.setTerminal("seller")` | `ticket.setTerminal("buyer")` | 必须替换，不能相信前端传 terminal。 |
| 主体表 | `seller` | `buyer` | 必须替换，保持独立主体表。 |
| 账号表 | `seller_account` | `buyer_account` | 必须替换，保持端内账号隔离。 |
| 部门/角色/菜单表 | `seller_dept` / `seller_role` / `seller_menu` | `buyer_dept` / `buyer_role` / `buyer_menu` | 必须替换，不能回退到若依 `sys_*`。 |
| 登录/操作日志表 | `seller_login_log` / `seller_oper_log` | `buyer_login_log` / `buyer_oper_log` | 必须替换，管理端才继续用若依日志。 |
| 会话表 | `seller_session` | `buyer_session` | 必须替换。 |
| 账号角色字典 | `seller_account_role` | `buyer_account_role` | 必须替换。 |
| 账号锁定字典 | `seller_account_lock_status` | `buyer_account_lock_status` | 必须替换，不能共用 seller 字典。 |
| seed 菜单 ID / 权限集合 | seller 独立 ID 与 `seller:admin:*` | buyer 独立 ID 与 `buyer:admin:*` | 复制 SQL 时必须保持 ID 唯一、权限唯一、父级正确。 |

## 不应该复制的差异

| 差异 | 不应复制的原因 | 建议边界 |
|---|---|---|
| 买家充值占位 | 充值属于财务/余额模块，不是主体资料 CRUD | 保留买家“待接入”占位；正式充值前先做财务表、流水、权限和审计方案。 |
| 余额字段与余额筛选 | 当前 Mapper 固定返回 `0.00 USD`，只是占位聚合 | 不把 `accountBalance` 当事实源；后续接钱包/余额聚合服务。 |
| 商品归属逻辑 | seller 商品是“当前卖家拥有”；buyer 商品应是“当前买家可见/可购买” | 复制商品类模板时只复制结构，不复制 `sellerId == subjectId` 业务谓词。 |
| 端内账号权限表 | 卖家、买家必须独立账号、部门、角色、菜单、日志、会话 | 绝不复制成共用 `customer_*` 或回退若依 `sys_*`。 |
| 免密票据审计 | seller/buyer 共用 `portal_direct_login_ticket`，但查询必须按 terminal 强制过滤 | 不为 buyer 另建一张 ticket 表，也不能让前端 terminal 决定查询范围。 |
| 管理端敏感权限 | directLogin、ticket:list、forceLogout、account:* 都是敏感能力 | 不能合并到 query/edit/list；低权限验收必须检查后端 403 和前端隐藏。 |
| 字典类型 | 等级、账号角色、锁定状态有端内字典 | 不能让 buyer 页面读取 seller 字典。 |
| 搜索持久化 key | 搜索折叠状态写入 localStorage | seller/buyer 必须不同 key，否则页面体验串扰。 |
| 业务提示文案 | 审计、弹窗、日志标题会影响操作记录理解 | 机械替换时必须检查中文文案，不只替换代码标识。 |

## 风险

| 风险 | 严重度 | 说明 | 建议 |
|---|---|---|---|
| 机械复制导致权限串端 | 高 | `seller:admin:*` / `buyer:admin:*` 只差前缀，漏替换会导致按钮显隐或后端鉴权错误 | 复制后跑 `SellerAdminPermissionContractTest`、`BuyerAdminPermissionContractTest`、`AdminAccountPermissionUiContractTest`。 |
| directLogin / ticket terminal 漏替换 | 高 | 免密代入涉及明文一次性 token 和审计票据，terminal 错误会造成跨端查询或登录失败 | 必须检查 `createToken(...)` terminal、URL key、fallback URL、`ticket.setTerminal(...)`。 |
| session/log 表串端 | 高 | 强制踢出、端内登录、端内操作日志都依赖独立表和 Redis token terminal | 复制时检查 Mapper 表名、Service terminal、`PortalTokenSupport.deleteLoginTokens(...)`。 |
| 把买家充值做成主体字段 | 高 | 会破坏财务流水、金额精度和审计要求 | 充值/余额单独进入 finance 方案，不放在 buyer 表简单更新。 |
| buyer 商品浏览照搬 seller 拥有关系 | 高 | 买家不是商品拥有者，不能用 `buyerId` 当商品归属字段 | buyer 商品浏览先确认可见性、上架状态、价格和库存口径。 |
| 大共享组件继续膨胀 | 中 | `PartnerManagementPage.tsx` 1209 行、`PartnerAccountModal.tsx` 688 行 | 新增复杂能力时拆 columns、form、account actions、audit/session 子模块。 |
| Mapper 占位余额误导筛选 | 中 | 余额筛选现在只对常量 0 生效 | 在正式钱包模块前，报告和页面都标注占位，不扩展为真实财务规则。 |
| seed 复制 ID 或字典错误 | 中 | SQL 中 seller/buyer 菜单、字典、权限都是成对但 ID 不同 | 后续 SQL 复制前先列替换表，再做只读预检和幂等复跑。 |
| 只看前端按钮导致误判 | 中 | 权限需后端注解、seed、前端显隐和真实接口共同验证 | 敏感权限变更后必须做低权限接口和浏览器验收。 |

## 建议的复制边界

### 可以复制的结构

- `PartnerModuleConfig` 配置模式。
- `PartnerManagementPage` 标准列表模板。
- `PartnerAccountModal` 账号列表、新增、编辑、重置默认密码、锁定/解锁、账号角色、账号级免密、会话操作结构。
- Admin Controller 的 CRUD、账号、部门、菜单、角色、日志、会话、免密和票据审计入口结构。
- Service 中主体校验、主账号创建/同步、账号唯一性、状态启停、锁定/解锁、重置密码、强制踢出、免密生成/消费结构。
- Mapper 中主体表、账号表、日志表、会话表的 SQL 结构。
- seed 中管理端菜单权限、端内最小权限、端内 Owner 角色初始化结构。
- 契约测试和前端 guard 的检查思路。

### 必须替换的配置

- terminal：`seller` -> `buyer`。
- URL：`/seller/admin/sellers` -> `/buyer/admin/buyers`，`/seller/direct-login` -> `/buyer/direct-login`。
- 权限：`seller:admin:*` -> `buyer:admin:*`，`seller:*` 端内权限 -> `buyer:*`。
- 数据表：`seller_*` -> `buyer_*`。
- Java 类型、Mapper、Service、Controller、DTO、测试类名：`Seller*` -> `Buyer*`。
- 字段名：`sellerId/sellerNo/sellerCode/sellerName/sellerLevel/sellerAccountId` -> `buyerId/buyerNo/buyerCode/buyerName/buyerLevel/buyerAccountId`。
- 字典：`seller_level`、`seller_account_role`、`seller_account_lock_status` -> 对应 buyer 字典。
- 配置 key：`portal.seller.web.url` -> `portal.buyer.web.url`。
- 前端配置：`moduleKey`、`label`、`title`、`balanceTitle`、`showRechargePlaceholder`、`searchStorageKey`、service import。
- seed：菜单 ID、父级菜单、显示名称、权限标识、端内 `seller_menu/seller_role_menu` 或 `buyer_menu/buyer_role_menu`。

### 不进入复制范围的内容

- 不新增或改动买家充值、钱包、余额流水、财务账户表。
- 不新增或改动商品可见性、客户价、库存承诺、上架渠道等业务规则。
- 不把卖家/买家账号绑定回 `sys_user`。
- 不把卖家/买家菜单和角色复用 `sys_menu` / `sys_role` 作为端内权限体系。
- 不把 seller/buyer 合并为通用 `partner` 或 `customer` 业务表长期混放。
- 不修改远程 MySQL / Redis，除非先生成 Markdown 方案或执行记录并得到确认。

## 建议后续流程

1. 每次先在 seller 侧定型一个能力切片，并完成静态契约测试、前端类型检查、必要的低权限接口和浏览器验收。
2. buyer 复制时只替换配置表中的机械项，不重新设计 UI，不扩展新业务。
3. 复制完成后至少检查：
   - 前端 `Buyer/index.tsx` 配置。
   - `buyer.ts` service URL。
   - `AdminBuyerController` 权限和日志。
   - `BuyerServiceImpl` terminal、URL key、ticket terminal、session/log 写入。
   - `BuyerMapper.xml` 表名和字段。
   - `seller_buyer_management_seed.sql` 权限、字典、端内菜单。
   - seller/buyer 管理端权限契约测试。
4. 遇到充值、余额、商品浏览、价格、库存、财务、外部系统时停止机械复制，先写业务边界方案。

## 新增问题

- 当前没有发现“买家管理明显漏复制卖家管理某个管理端结构”的问题。
- 风险主要来自未来复制时漏替换 terminal/权限/表名/字典，或把买家充值、余额、商品可见性当成普通同构功能照搬。

## 已修复问题

- 本次为只读检查，未修复代码。

## 残留问题

- 买家充值和余额仍为占位，未进入财务模块设计。
- 商品浏览等非管理端主体资料能力仍需按业务口径单独确认。
- 本次未做运行时接口或浏览器验收，结论基于静态代码和既有执行记录。

## 验证命令

```powershell
cd E:\Urili-Ruoyi
Get-Content -Raw -Encoding UTF8 AGENTS.md
rg -n "Seller|Buyer|seller|buyer|卖家|买家" docs react-ui RuoYi-Vue -g "*.md" -g "*.tsx" -g "*.ts" -g "*.java" -g "*.xml" -g "*.sql"
Get-Content -Encoding UTF8 docs\architecture\reuse-ledger.md
Get-Content -Encoding UTF8 docs\status\2026-06-04-seller-buyer-operations-implementation.md
Get-Content -Encoding UTF8 docs\reviews\2026-06-04-seller-buyer-management-gap-audit.md
Get-Content -Encoding UTF8 react-ui\src\pages\Seller\index.tsx
Get-Content -Encoding UTF8 react-ui\src\pages\Buyer\index.tsx
Get-Content -Encoding UTF8 react-ui\src\services\seller\seller.ts
Get-Content -Encoding UTF8 react-ui\src\services\buyer\buyer.ts
rg -n "@RequestMapping|@PreAuthorize|@Log|directLogin|accounts|sessions|loginLogs|operLogs|directLoginTickets" RuoYi-Vue\seller\src\main\java\com\ruoyi\seller\controller\AdminSellerController.java RuoYi-Vue\buyer\src\main\java\com\ruoyi\buyer\controller\AdminBuyerController.java
rg -n "seller:admin|buyer:admin|seller_level|buyer_level|portal\.seller|portal\.buyer|seller_menu|buyer_menu" RuoYi-Vue\sql\seller_buyer_management_seed.sql
git status --short
```

## 未验证原因

- 未运行 Maven、前端构建或浏览器验收：本次任务是只读静态检查，未改功能代码。
- 未连接 MySQL / Redis：本次不需要验证运行库数据，且按规则不得猜测数据源。
- 未执行 DDL/DML：本次没有数据库变更。

## 权限检查结果

- 管理端主体权限前缀已按 seller/buyer 分离：`seller:admin:*`、`buyer:admin:*`。
- 账号域权限已独立为 `*:admin:account:*`，未并入主体 `query/edit/resetPwd`。
- directLogin、ticket:list、forceLogout 均为独立敏感权限。
- 已存在管理端权限契约测试和前端权限显隐契约测试，后续复制应继续运行。

## 字典/选项复用检查结果

- `subject_type`、`country_region` 可继续共用。
- `seller_level` / `buyer_level` 必须独立。
- `seller_account_role` / `buyer_account_role` 必须独立。
- `seller_account_lock_status` / `buyer_account_lock_status` 必须独立。
- 前端选择器继续复用 `SEARCHABLE_SELECT_PROPS`，不在页面内散写下拉搜索逻辑。

## 复用台账检查结果

- 已读取 `docs/architecture/reuse-ledger.md`。
- 台账已记录管理端权限契约、PartnerManagementPage 规则、账号表、免密登录、端内日志/会话等复用边界。
- 本次未新增代码抽象，因此未修改台账。

## CodeGraph 更新结果

- 本次未执行 `codegraph sync .`。
- 原因：只读检查未改功能代码；仅生成本审查报告，避免在当前已有未跟踪 `.codegraph/` 目录的情况下修改本机索引状态。

## 大文件合理性判断结果

- `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx`：1209 行，既有共享模板大文件。本次只读检查不拆分；后续新增复杂能力应优先拆子组件。
- `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx`：688 行，既有账号弹窗大文件。继续扩展账号能力前建议拆账号表格、表单、操作区。
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`：685 行。
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`：685 行。
- 上述 Service 当前结构一一对应，本次不做拆分；未来若新增财务、商品或外部系统能力，不应继续塞入主体管理 Service。

## 重复代码检查结果

- 前端没有两套页面实现，已通过 `PartnerManagementPage` 和配置复用。
- 后端 seller/buyer 仍是独立模块、独立表、独立权限，属于端隔离下的同构实现，不是错误共用。
- 后续复制应继续用模板和守卫控制差异，不建议创建第三套 `partner/customer` 通用模块承载卖家/买家主体事实。
