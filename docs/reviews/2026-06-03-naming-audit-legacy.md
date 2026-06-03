# Urili 命名与路径审计报告

日期：2026-06-03

> 更新说明：本报告包含早先对本地 Docker 库的判断。用户已删除本地数据库，后续以远端库复查报告 `docs/reviews/2026-06-03-naming-audit-remote-only-legacy.md` 为准。

## 审计口径

- `Urili` 是项目/业务域，不是所有路径、菜单、文件、类、变量、表和字段的默认前缀。
- URL 菜单 path、前端页面目录、service/type 目录、数据库业务表名、业务字段名，应优先使用业务对象本身：`seller`、`buyer`、`product`、`order`、`inventory`、`warehouse`、`finance`。
- 权限 key、API 命名空间、已落库兼容对象可以暂时保留 `urili`，但如果要去掉，必须同步改后端注解、菜单权限、角色授权、前端权限判断和接口调用。

## 新增问题

### 1. 数据库仍存在旧业务表 `urili_customer` 和 `urili_customer_account`

严重级别：高

现状：

- 本地 Docker 库 `ry-vue` 存在 `urili_customer`、`urili_customer_account`。
- 当前后端配置指向的库也存在 `urili_customer`、`urili_customer_account`。
- 未发现 `seller`、`buyer`、`seller_account`、`buyer_account` 已落库。
- SQL 种子仍创建旧表：[urili_customer_management_seed.sql](E:/Urili-Ruoyi/RuoYi-Vue/sql/urili_customer_management_seed.sql:6)
- Mapper XML 仍读写旧表：[UriliCustomerMapper.xml](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/resources/mapper/system/UriliCustomerMapper.xml:94)

判断：

- 这不是“必须带 Urili”的合理场景。当前规则已明确业务表应命名为 `seller`、`buyer`、`seller_account`、`buyer_account`。
- 由于已经落库并被后端代码使用，不能直接改名；必须走迁移方案。

建议：

- 废弃 `urili_customer_management_seed.sql` 的旧单表设计。
- 新增确认后的分表 SQL：`seller`、`buyer`、`seller_account`、`buyer_account`。
- 迁移旧表数据后，将旧表改名为 legacy 表保留，不立即删除。

### 2. 数据库菜单仍残留旧二级 path 的 `urili-` 前缀

严重级别：高

本地库和当前配置库都发现以下菜单 path 残留：

| menu_id | parent_id | 当前 path | 状态判断 |
| ---: | ---: | --- | --- |
| 2021 | 2020 | `urili-warehouse-official` | 禁用残留 |
| 2022 | 2020 | `urili-warehouse-third-party` | 仍显示且启用 |
| 2031 | 2020 | `urili-upstream-system` | 禁用残留 |
| 2040 | 0 | `urili-channel` | 隐藏且禁用残留 |
| 2041 | 2040 | `urili-channel-system` | 禁用残留 |
| 2042 | 2040 | `urili-channel-customer` | 禁用残留 |
| 2051 | 2030 | `urili-billing-handling-fee` | 禁用残留 |
| 2052 | 2030 | `urili-billing-freight` | 禁用残留 |
| 2053 | 2030 | `urili-billing-quote-scheme` | 禁用残留 |

判断：

- `sys_menu.path` 会生成前端 URL，这类 path 不应该带 `urili-`。
- 当前源码未找到这些菜单的现行种子，说明它们是历史库数据残留。
- `2022` 仍是 `visible=0,status=0`，属于实际可见可用问题。

建议：

- 对仍保留的菜单改为无前缀 path，例如 `official`、`third-party`、`upstream-system`、`handling-fee`、`freight`、`quote-scheme`。
- 对旧草稿菜单如 `channel` 系列，如果已不属于当前菜单方案，应继续保持隐藏禁用或清理为 legacy 记录。
- 同步调整对应 `component` 和前端页面目录，否则路由 path 虽改，页面加载仍会指向 `pages/Urili/...`。

### 3. 前端页面、service、types 目录使用 `Urili/urili` 作为默认模块路径

严重级别：中

现状：

- 页面目录：[react-ui/src/pages/Urili](E:/Urili-Ruoyi/react-ui/src/pages/Urili)
- service 目录：[react-ui/src/services/urili](E:/Urili-Ruoyi/react-ui/src/services/urili)
- types 目录：[react-ui/src/types/urili](E:/Urili-Ruoyi/react-ui/src/types/urili)
- 菜单组件路径仍为 `Urili/Customer/Seller/index`、`Urili/Customer/Buyer/index`：[urili_customer_management_seed.sql](E:/Urili-Ruoyi/RuoYi-Vue/sql/urili_customer_management_seed.sql:277)

判断：

- 这类前端目录和菜单 component path 是项目内部路径，不需要默认加 `Urili`。
- 更自然的目录应按业务模块拆分，例如 `pages/Seller`、`pages/Buyer`、`services/seller.ts`、`services/buyer.ts`、`types/seller.d.ts`、`types/buyer.d.ts`。

建议：

- 配合卖家/买家分表重构一起改，避免先改目录再改业务模型造成两次大迁移。
- 同步改 `sys_menu.component`，否则菜单仍会加载旧目录。

### 4. 旧客户单表代码与新分表规则并存

严重级别：中

现状：

- 旧单表代码仍存在：`UriliCustomer`、`UriliCustomerAccount`、`UriliCustomerMapper`、`IUriliCustomerService`、`UriliCustomerServiceImpl`。
- 新分表方向的类也已出现：`UriliSeller`、`UriliBuyer`、`UriliSellerAccount`、`UriliBuyerAccount`、`UriliSellerMapper`、`UriliBuyerMapper`、`IUriliSellerService`、`IUriliBuyerService`。
- 分表规则文档明确写着旧表应迁移：[2026-06-03-seller-buyer-split-module-design.md](E:/Urili-Ruoyi/docs/architecture/2026-06-03-seller-buyer-split-module-design.md:31)

判断：

- 当前主要问题不是类名前缀，而是“旧 customer 单表模式”和“seller/buyer 分表模式”并存。
- 继续保留旧 `UriliCustomer*` 会把命名问题和模块边界问题叠加。

建议：

- 先确认分表迁移方案，再统一删除或降级旧 `Customer` 单表实现。
- Java 类名是否去掉 `Urili` 可作为同一轮重构的第二层决策；当前更关键的是先消除旧单表模型。

### 5. `proTableSearch` 的 localStorage key 使用 `urili:` 前缀

严重级别：低

位置：[proTableSearch.ts](E:/Urili-Ruoyi/react-ui/src/utils/proTableSearch.ts:5)

现状：

```ts
const SEARCH_COLLAPSED_STORAGE_PREFIX = 'urili:proTableSearch:collapsed:';
```

判断：

- 这是一个全局 ProTable 工具，已经用于系统管理、系统监控、工具中心等页面，不是 URILI 专属。
- 这里的 `urili:` 没有必要。

建议：

- 改为 `ruoyi-react:proTableSearch:collapsed:` 或 `proTableSearch:collapsed:`。
- 影响仅为旧浏览器折叠状态缓存失效，可以接受。

### 6. 字典类型仍使用 `urili_customer_type`、`urili_country_region`

严重级别：中

现状：

- SQL 种子创建 `urili_customer_type`、`urili_country_region`：[urili_customer_management_seed.sql](E:/Urili-Ruoyi/RuoYi-Vue/sql/urili_customer_management_seed.sql:140)
- 前端读取这两个字典：[CustomerManagementPage.tsx](E:/Urili-Ruoyi/react-ui/src/pages/Urili/Customer/components/CustomerManagementPage.tsx:290)
- 复用台账也记录了旧字典：[reuse-ledger.md](E:/Urili-Ruoyi/docs/architecture/reuse-ledger.md:7)

判断：

- `customer_type` 应随旧 `customer` 单表一起废弃，分成 `subject_type`、`seller_level`、`buyer_level` 等更清晰字典。
- `country_region` 是通用基础字典，不应天然带 `urili_`。但当前已经被页面和台账引用，迁移要同步处理。

建议：

- 分表实施时新增无前缀字典类型。
- 对 `urili_country_region` 是否改成 `country_region`，按现有设计文档仍是待确认项。

## 已修复问题

- 顶级菜单 path 已不再带 `urili-`：
  - `customer`
  - `product`
  - `order`
  - `inventory`
  - `warehouse`
  - `overseas-warehouse-service`
  - `finance`
- 本地库和当前配置库都已回读确认 7 个顶级 path 正确。

## 残留问题

- 旧二级菜单 path 仍残留 `urili-`，其中 `2022` 仍可见启用。
- 旧业务表 `urili_customer`、`urili_customer_account` 已落库且仍被代码引用。
- 前端 `pages/Urili`、`services/urili`、`types/urili` 仍是默认模块路径。
- 旧架构文档仍有多处 `urili_customer` 方案，和当前分表规则冲突，应标记为历史方案或补充“已废弃”说明。
- 当前没有执行任何重命名或迁移，避免破坏已落库数据和未提交代码。

## 可保留项

- `urili:admin:*` 权限 key：当前仍作为若依权限命名空间使用，若去掉需要同步后端注解、菜单权限、角色授权和前端权限判断。
- `/api/urili/admin/...` API 前缀：当前作为后端接口命名空间使用，若去掉需要同步前后端接口和兼容策略。
- Docker 容器和 volume 名 `urili-ruoyi-*`：这是本工程运行环境命名，不会出现在业务路由或数据模型里。
- README、历史报告里描述项目名 `URILI`：属于项目说明，不是业务路径或表字段命名问题。

## 验证命令

```powershell
rg --files . | rg -i "(^|[\\/])urili|urili"
rg -n "\burili[_-]|\bUrili\b|Urili[A-Z]|/urili\b|API\.Urili|namespace API\.Urili" -S RuoYi-Vue react-ui docs README.md docker-compose.yml --glob "!**/node_modules/**" --glob "!**/target/**"
docker exec urili-ruoyi-mysql mysql -u root -p<password> -D ry-vue -N -e "<information_schema table/column/index audit>"
docker exec urili-ruoyi-mysql mysql -h <configured-host> -P <configured-port> -u root -p<password> -D <configured-db> -N -e "<information_schema table/column/index audit>"
docker exec urili-ruoyi-mysql mysql -u root -p<password> -D ry-vue -N -e "<sys_menu/sys_role/sys_dict_type audit>"
docker exec urili-ruoyi-mysql mysql -h <configured-host> -P <configured-port> -u root -p<password> -D <configured-db> -N -e "<sys_menu/sys_role/sys_dict_type audit>"
```

## 未验证原因

- 未运行编译、测试、前端构建或浏览器截图；本轮是命名审计，没有改实现。
- 未执行数据库重命名或菜单清理 SQL；涉及表迁移、接口兼容、权限同步，需要先确认方案。

## 权限检查结果

- 已检查 `sys_menu.perms` 和后端 `@PreAuthorize`。
- 权限 key 大量使用 `urili:admin:*`，当前判断为可保留命名空间。
- 如果后续决定去掉 `urili:`，必须同步菜单权限、角色授权、后端注解、前端 `access.hasPerms` 和已有用户权限缓存。

## 字典/选项复用检查结果

- 已发现旧字典 `urili_customer_type`、`urili_country_region` 被 SQL、前端和复用台账引用。
- `urili_customer_type` 应随旧 customer 单表废弃或迁移。
- `urili_country_region` 建议改为 `country_region`，但需要确认并同步迁移台账。

## 复用台账检查结果

- `docs/architecture/reuse-ledger.md` 仍登记旧 `urili_customer_type` 和 `urili_country_region`。
- 如果字典类型迁移，必须同步更新复用台账，避免后续继续复用旧 key。

## 大文件合理性判断结果

- 本轮未修改实现文件。
- 现有 `CustomerManagementPage.tsx` 已超过 800 行，后续拆卖家/买家页面时必须一并拆分组件、hooks、services、types，不能继续在单文件里堆叠。

## 重复代码检查结果

- 当前存在旧 `Customer` 单表实现和新 `Seller/Buyer` 分表实现并行的迹象。
- 后续不应在两套模式上继续扩展；应选择 `seller/buyer` 分表模式，旧 `customer` 模式走迁移或废弃。
