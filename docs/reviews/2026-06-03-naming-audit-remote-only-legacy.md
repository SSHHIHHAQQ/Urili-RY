# Urili 命名与路径远端库复查报告

日期：2026-06-03

## 审计口径

- 本轮只以当前后端激活配置指向的远端库为准。
- 已确认 `application.yml` 激活 `druid`，数据源来自 `application-druid.yml`。
- 本地 Docker MySQL 当前未运行，本轮未读取、未写入本地数据库。
- 本轮所有数据库操作都是只读查询，没有执行 DDL/DML。

## 远端数据库结论

目标库：`fenxiao`

### 1. 远端仍只有旧业务表

远端存在：

| 表名 | 行数 | 判断 |
| --- | ---: | --- |
| `urili_customer` | 1 | 旧单表设计，命名不符合当前规则 |
| `urili_customer_account` | 1 | 旧账号绑定表，命名不符合当前规则 |

远端不存在：

```text
seller
buyer
seller_account
buyer_account
```

判断：

- 当前规则要求业务表按业务对象命名，不默认加 `urili_`。
- 远端库仍停留在旧 `urili_customer` 单表状态。
- 远端已有 1 条客户数据和 1 条账号绑定数据，不能直接删除或粗暴改名，必须做迁移。

### 2. 当前代码已经切到 seller/buyer 表，但远端库还没跟上

代码 Mapper 已经读写新表：

- [UriliSellerMapper.xml](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/resources/mapper/system/UriliSellerMapper.xml:92) 读取 `seller`
- [UriliSellerMapper.xml](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/resources/mapper/system/UriliSellerMapper.xml:242) 写入 `seller_account`
- [UriliBuyerMapper.xml](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/resources/mapper/system/UriliBuyerMapper.xml:92) 读取 `buyer`
- [UriliBuyerMapper.xml](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/main/resources/mapper/system/UriliBuyerMapper.xml:242) 写入 `buyer_account`

但远端库没有这些表。

判断：

- 这是当前最危险的不一致：卖家/买家接口会在远端库上查不到表。
- 下一步应优先补远端迁移方案，而不是先做前端或类名微调。

### 3. 远端菜单仍有旧二级 `urili-` path

远端 `sys_menu.path` 中仍有 9 条 `urili-*` path，其中 1 条仍可见且启用。

| menu_id | 菜单 | 当前 path | visible | status | 判断 |
| ---: | --- | --- | --- | --- | --- |
| 2021 | 官方仓库 | `urili-warehouse-official` | `0` | `1` | 禁用残留 |
| 2022 | 第三方仓库 | `urili-warehouse-third-party` | `0` | `0` | 可见启用，应修 |
| 2031 | 上游系统管理 | `urili-upstream-system` | `0` | `1` | 禁用残留 |
| 2040 | 渠道管理 | `urili-channel` | `1` | `1` | 隐藏禁用残留 |
| 2041 | 系统渠道管理 | `urili-channel-system` | `0` | `1` | 禁用残留 |
| 2042 | 客户渠道管理 | `urili-channel-customer` | `0` | `1` | 禁用残留 |
| 2051 | 操作费设置 | `urili-billing-handling-fee` | `0` | `1` | 禁用残留 |
| 2052 | 运费设置 | `urili-billing-freight` | `0` | `1` | 禁用残留 |
| 2053 | 报价方案 | `urili-billing-quote-scheme` | `0` | `1` | 禁用残留 |

判断：

- `sys_menu.path` 会影响前端 URL，不应该带 `urili-`。
- 其中 `2022` 是实际可见启用问题。
- 其余虽然禁用，也会污染菜单管理和后续迁移判断，建议清理或转 legacy。

### 4. 远端角色和权限仍使用 `urili` 命名空间

远端角色：

| role_id | role_name | role_key |
| ---: | --- | --- |
| 100 | URILI卖家端账号 | `urili_seller` |
| 101 | URILI买家端账号 | `urili_buyer` |

远端权限：

- 菜单和按钮权限大量使用 `urili:admin:*`。
- 后端控制器也使用同一权限命名空间，例如 [UriliAdminSellerController.java](E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-admin/src/main/java/com/ruoyi/web/controller/urili/UriliAdminSellerController.java:33)。

判断：

- 这类权限命名空间可以暂时保留，因为它是安全边界的一部分。
- 如果决定去掉 `urili:`，必须同步改远端 `sys_menu.perms`、角色授权、后端 `@PreAuthorize`、前端 `access.hasPerms`，不建议和表迁移混在同一步做。

### 5. 远端字典仍有旧 `urili_` 类型

远端字典类型：

| dict_id | dict_name | dict_type | 判断 |
| ---: | --- | --- | --- |
| 100 | URILI客户类型 | `urili_customer_type` | 应随旧 customer 单表迁移或废弃 |
| 101 | 国家/地区 | `urili_country_region` | 通用字典，建议迁移到 `country_region` |

代码侧已经有新方向：

- 卖家页面使用 `seller_account_role`
- 买家页面使用 `buyer_account_role`
- 设计文档也已写明国家/地区使用 `country_region`

判断：

- 远端字典落后于当前代码和设计。
- 字典迁移要同步 `sys_dict_type`、`sys_dict_data`、前端读取 key、复用台账。

## 文件命名与路径结论

### 1. 前端目录仍默认带 `Urili/urili`

当前仍存在：

- `react-ui/src/pages/Urili/`
- `react-ui/src/services/urili/`
- `react-ui/src/types/urili/`

判断：

- 这些是项目内部路径，不是安全命名空间，不应默认带 `Urili`。
- 建议后续改成：

```text
react-ui/src/pages/Seller/
react-ui/src/pages/Buyer/
react-ui/src/pages/shared/partner/
react-ui/src/services/seller.ts
react-ui/src/services/buyer.ts
react-ui/src/types/seller.d.ts
react-ui/src/types/buyer.d.ts
```

### 2. 菜单 component 仍指向 `Urili/...`

远端菜单 component 仍有：

```text
Urili/Customer/Seller/index
Urili/Customer/Buyer/index
Urili/Warehouse/...
Urili/Upstream/...
Urili/Billing/...
```

判断：

- 菜单 `component` 会决定前端动态加载路径。
- 如果前端目录去掉 `Urili`，必须同步更新远端 `sys_menu.component`。

### 3. Java 类名仍带 `Urili`

当前 Java 类名如：

```text
UriliAdminSellerController
UriliSeller
UriliBuyer
UriliSellerMapper
IUriliSellerService
```

判断：

- Java 类名带 `Urili` 可以暂时保留，属于项目业务命名空间。
- 但如果目标是彻底普通化模块命名，可以后续改成 `AdminSellerController`、`Seller`、`SellerMapper`、`ISellerService`。
- 这个不应优先于远端表迁移。

### 4. 全局 ProTable 缓存 key 仍带 `urili:`

位置：[proTableSearch.ts](E:/Urili-Ruoyi/react-ui/src/utils/proTableSearch.ts:5)

```ts
const SEARCH_COLLAPSED_STORAGE_PREFIX = 'urili:proTableSearch:collapsed:';
```

判断：

- 这是全局表格工具，不是 URILI 专属。
- 建议改成 `proTableSearch:collapsed:` 或 `ruoyi-react:proTableSearch:collapsed:`。
- 影响只是旧浏览器 localStorage 折叠状态失效，风险低。

## 新增问题

1. 远端库和当前代码已经不匹配：代码查 `seller/buyer`，远端只有 `urili_customer/urili_customer_account`。
2. 远端 `sys_menu` 仍有 1 条可见启用的 `urili-*` URL path：`urili-warehouse-third-party`。
3. 远端仍缺少新字典类型，如 `country_region`、`seller_account_role`、`buyer_account_role`。
4. 旧报告 [2026-06-03-naming-audit-legacy.md](E:/Urili-Ruoyi/docs/reviews/2026-06-03-naming-audit-legacy.md:1) 中关于本地库的结论已经不再适用；以后以本报告为准。

## 已修复问题

- 顶级菜单 path 在远端已是无前缀：
  - `customer`
  - `product`
  - `order`
  - `inventory`
  - `warehouse`
  - `overseas-warehouse-service`
  - `finance`
- 本轮未执行写操作，因此没有新增修复落库。

## 残留问题

- 远端旧业务表和当前代码不匹配。
- 远端旧二级菜单 path 仍残留 `urili-`。
- 前端目录、service、types 仍带 `Urili/urili`。
- 远端字典类型仍带旧 `urili_` 命名。
- 权限/API 是否去掉 `urili` 需要单独决策，目前建议保留。

## 建议执行顺序

1. 先写远端数据库迁移方案：创建 `seller`、`buyer`、`seller_account`、`buyer_account`，迁移旧 1 条客户和 1 条账号绑定数据。
2. 同步远端字典：补 `country_region`、`subject_type`、`seller_level`、`buyer_level`、`seller_account_role`、`buyer_account_role`。
3. 同步远端菜单：修可见启用的 `urili-warehouse-third-party`，并处理其余禁用残留菜单。
4. 再改前端目录和菜单 component：去掉 `pages/Urili`、`services/urili`、`types/urili`。
5. 最后再评估是否要改 Java 类名、API `/api/urili/...` 和权限 `urili:admin:*`。

## 验证命令

```powershell
docker compose ps
where.exe mysql
docker run --rm -e MYSQL_PWD=<masked> mysql:8.4 mysql -h <remote-host> -P <remote-port> -u <user> -D fenxiao -N --default-character-set=utf8mb4 -e "<information_schema audit>"
docker run --rm -e MYSQL_PWD=<masked> mysql:8.4 mysql -h <remote-host> -P <remote-port> -u <user> -D fenxiao -N --default-character-set=utf8mb4 -e "<sys_menu/sys_role/sys_dict_type audit>"
rg --files . | rg -i "(^|[\\/])urili|urili"
rg -n "\burili[_-]|\bUrili\b|Urili[A-Z]|/urili\b|API\.Urili|namespace API\.Urili|pages/Urili|services/urili|types/urili" -S RuoYi-Vue react-ui docs README.md docker-compose.yml --glob "!**/node_modules/**" --glob "!**/target/**"
```

## 未验证原因

- 未运行后端编译、前端构建、接口请求或浏览器截图。
- 本轮是远端只读命名审计，没有改代码，也没有改远端数据。
- 远端迁移涉及 DDL/DML，按工程规则必须先确认 Markdown 方案后执行。

## 权限检查结果

- 已检查远端 `sys_menu.perms`、`sys_role.role_key` 和后端控制器权限注解。
- `urili:admin:*` 与 `urili_seller`、`urili_buyer` 目前保持一致。
- 这些属于安全和授权命名空间，短期建议保留。

## 字典/选项复用检查结果

- 远端仍只有旧 `urili_customer_type`、`urili_country_region`。
- 代码和设计已经倾向无前缀字典，如 `country_region`、`seller_account_role`、`buyer_account_role`。
- 需要迁移远端字典并同步复用台账。

## 复用台账检查结果

- `docs/architecture/reuse-ledger.md` 已经记录了一些新字典和公共组件，但仍包含 `react-ui/src/pages/Urili/...` 这类路径。
- 如果目录去掉 `Urili`，复用台账必须同步更新。

## 大文件合理性判断结果

- 本轮未修改实现文件。
- `PartnerManagementPage.tsx` 是跨卖家/买家共享大组件，后续改目录时要评估是否继续共享，避免重新堆成单一大文件。

## 重复代码检查结果

- 当前代码已进入 seller/buyer 分表模式，但远端库仍是 customer 单表模式。
- 这是模式冲突，不是简单重复代码；应通过迁移收敛到 seller/buyer 模式。
