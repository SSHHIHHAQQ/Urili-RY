# 当前 Urili 残留审计报告

日期：2026-06-03

> 更新说明：本报告列出的远端权限/角色、菜单 component/routeName、旧字典、本地缓存 key、历史文档文件名和 Docker compose 命名已经处理。执行记录见 `docs/status/2026-06-03-naming-residual-cleanup.md`。

## 结论

当前已经清理干净的部分：

- 远端业务表名：没有 `urili`。
- 远端字段名：没有 `urili`。
- 远端索引名：没有 `urili`。
- 远端表备注、字段备注：没有 `urili`。
- 远端 `sys_menu.path`：没有 `urili`。
- 后端 Java 实现路径和类名：没有 `Urili/urili`。
- 前端页面、service、types 路径：没有 `Urili/urili`。
- 后端接口路径：当前代码是 `/seller/admin/...`、`/buyer/admin/...`，不是 `/urili/...`。

当前仍残留并且需要处理的部分：

1. 远端权限和角色还停在旧 `urili` 命名空间，但当前代码已经改成 `seller/buyer` 命名空间。这会影响权限校验和创建端账号。
2. 远端菜单 component 和 routeName 还带 `Urili`，其中卖家/买家当前启用菜单会影响前端动态路由加载。
3. 远端旧字典 `urili_customer_type`、`urili_country_region` 仍存在，但当前代码已经使用新字典。
4. 本地少量脚本、全局缓存 key、Docker 环境名和历史文档仍带 `urili`。

## 远端库检查结果

目标库：`fenxiao`

### schema 命名

| 检查项 | 数量 |
| --- | ---: |
| 表名包含 `urili` | 0 |
| 字段名包含 `urili` | 0 |
| 索引名包含 `urili` | 0 |
| 表备注包含 `urili` | 0 |
| 字段备注包含 `urili` | 0 |

判断：数据库结构层面的表、字段、索引已经没有 `urili`。

### 菜单路径

| 检查项 | 数量 |
| --- | ---: |
| `sys_menu.path` 包含 `urili` | 0 |

判断：URL path 层面已经清理干净。

### 菜单 component 和 routeName

| 检查项 | 数量 | 影响 |
| --- | ---: | --- |
| `sys_menu.component` 包含 `Urili` | 10 | 影响动态组件加载 |
| `sys_menu.route_name` 包含 `Urili` | 18 | 影响路由命名一致性 |

其中当前最需要改的是：

| menu_id | 菜单 | 当前 path | 当前 component | 当前 routeName | 建议 |
| ---: | --- | --- | --- | --- | --- |
| 2010 | 客户管理 | `customer` | `NULL` | `UriliCustomer` | 改为 `Customer` 或 `Partner` |
| 2011 | 卖家管理 | `seller` | `Urili/Seller/index` | `UriliSeller` | 改为 `Seller/index`、`Seller` |
| 2012 | 买家管理 | `buyer` | `Urili/Buyer/index` | `UriliBuyer` | 改为 `Buyer/index`、`Buyer` |

其它 `Urili/Warehouse/...`、`Urili/Channel/...`、`Urili/Billing/...` 菜单当前多为禁用或未实现页面，建议后续随对应模块开发或菜单归档一起处理。

### 权限和角色

当前远端角色：

| role_id | role_name | role_key |
| ---: | --- | --- |
| 100 | URILI卖家端账号 | `urili_seller` |
| 101 | URILI买家端账号 | `urili_buyer` |

当前代码需要：

| 模块 | 代码期望 role_key |
| --- | --- |
| 卖家 | `seller` |
| 买家 | `buyer` |

当前远端菜单权限：

- `urili:admin:seller:*`：7 个
- `urili:admin:buyer:*`：7 个
- 其它仓库、上游、渠道、计费相关 `urili:admin:*`：47 个

当前代码需要：

- `seller:admin:*`
- `buyer:admin:*`

判断：

- 这是当前最大的功能风险。
- 如果不迁移权限，卖家/买家接口的 `@PreAuthorize("@ss.hasPermi('seller:admin:*')")`、`@PreAuthorize("@ss.hasPermi('buyer:admin:*')")` 会和远端菜单权限不匹配。
- 如果不迁移角色，创建卖家/买家端账号时，代码按 `seller` / `buyer` 查角色，会找不到角色。

### 旧字典

远端仍有：

| dict_type | 数据条数 | 当前判断 |
| --- | ---: | --- |
| `urili_customer_type` | 3 | 旧 customer 单表字典，可清理 |
| `urili_country_region` | 25 | 旧国家/地区字典，已被 `country_region` 取代 |

当前代码和复用台账使用：

- `subject_type`
- `seller_level`
- `buyer_level`
- `seller_account_role`
- `buyer_account_role`
- `country_region`

判断：旧字典已经没有必要继续保留，但删除前应备份并确认没有历史页面引用。

## 本地文件路径检查

当前全仓文件路径仍带 `urili` 的只有：

```text
docs/menu-fields.md
docs/reviews/2026-06-03-naming-audit-legacy.md
docs/reviews/2026-06-03-naming-audit-remote-only-legacy.md
```

判断：

- 这些是历史文档或审计报告，不是当前业务实现文件。
- 如果要求文档文件名也完全不带 `urili`，可以后续统一改名并更新引用。

当前后端和前端实现路径没有 `Urili/urili`：

```text
react-ui/src/pages/Seller/index.tsx
react-ui/src/pages/Buyer/index.tsx
react-ui/src/services/seller/seller.ts
react-ui/src/services/buyer/buyer.ts
react-ui/src/types/seller-buyer/seller.d.ts
react-ui/src/types/seller-buyer/buyer.d.ts
```

## 本地实现文本检查

当前实现文件中仍带 `urili` 的主要位置：

### `react-ui/src/utils/proTableSearch.ts`

```ts
const SEARCH_COLLAPSED_STORAGE_PREFIX = 'urili:proTableSearch:collapsed:';
```

判断：

- 这是全局 ProTable 工具，不是 URILI 专属。
- 建议改成 `proTableSearch:collapsed:`。
- 影响只是旧浏览器 localStorage 中的筛选区折叠状态失效，风险低。

### `RuoYi-Vue/sql/seller_buyer_management_seed.sql`

仍包含：

- 旧表兼容判断：`urili_customer`、`urili_customer_account`
- 旧角色迁移：`urili_seller`、`urili_buyer`

判断：

- 这是迁移兼容脚本，不是当前业务表。
- 远端旧表已经删除后，旧表兼容段会跳过。
- 但当前远端角色仍是 `urili_seller`、`urili_buyer`，所以该脚本里的角色迁移段还有实际价值。

### `RuoYi-Vue/sql/legacy_menu_seed.sql`

仅为旧菜单 seed 的兼容说明，属于历史说明文件。

### `docker-compose.yml`

仍有：

```text
urili-ruoyi-mysql
urili-ruoyi-redis
urili-ruoyi-mysql-data
urili-ruoyi-redis-data
```

判断：

- 这是本地隔离环境容器和 volume 名，不是业务路径、字段或权限。
- 如果要求连本地容器名也不要带 `urili`，可以改，但会影响已有 Docker volume 名称。

### `README.md` / `AGENTS.md`

仍有 URILI 项目说明文字。

判断：这是项目说明，不属于需要清理的业务字段、路径或文件问题。

## 新增问题

1. 远端 `sys_menu.perms` 仍是 `urili:admin:*`，但当前代码已经改为 `seller:admin:*`、`buyer:admin:*`。
2. 远端 `sys_role.role_key` 仍是 `urili_seller`、`urili_buyer`，但当前代码创建端账号时查 `seller`、`buyer`。
3. 远端卖家/买家菜单 component 仍是 `Urili/Seller/index`、`Urili/Buyer/index`，但当前前端页面路径是 `Seller/index`、`Buyer/index`。
4. 远端旧字典 `urili_customer_type`、`urili_country_region` 仍存在。
5. `react-ui/src/utils/proTableSearch.ts` 的 localStorage key 仍带 `urili:`。

## 已修复问题

1. 远端旧业务表 `urili_customer`、`urili_customer_account` 已删除。
2. 远端业务表、字段、索引已无 `urili`。
3. 远端 `sys_menu.path` 已无 `urili`。
4. 当前后端 Java 实现路径、类名、接口路径已无 `Urili/urili`。
5. 当前前端页面、service、types 路径已无 `Urili/urili`。

## 残留问题

需要优先处理：

1. 远端角色和权限从 `urili_*` / `urili:admin:*` 迁移到 `seller`、`buyer`、`seller:admin:*`、`buyer:admin:*`。
2. 远端卖家/买家菜单 component 和 routeName 改成当前前端路径。
3. 删除远端旧字典 `urili_customer_type`、`urili_country_region`。
4. 修改 `proTableSearch` 缓存 key。

可后续处理：

1. 历史文档文件名和内容中的 `Urili/urili`。
2. Docker 容器和 volume 名。
3. 禁用或未实现菜单上的 `Urili/Warehouse/...`、`Urili/Channel/...`、`Urili/Billing/...` component。

## 权限检查结果

权限当前不一致。后端代码已经使用：

```text
seller:admin:list
seller:admin:query
seller:admin:add
seller:admin:edit
seller:admin:changeStatus
seller:admin:resetPwd

buyer:admin:list
buyer:admin:query
buyer:admin:add
buyer:admin:edit
buyer:admin:changeStatus
buyer:admin:resetPwd
```

远端仍使用：

```text
urili:admin:seller:*
urili:admin:buyer:*
```

`directLogin` 两个权限点仍存在，但当前代码没有实现。

## 字典/选项复用检查结果

新字典已存在并应作为当前实现来源：

- `subject_type`
- `seller_level`
- `buyer_level`
- `seller_account_role`
- `buyer_account_role`
- `country_region`

旧字典仍存在，应在备份后清理：

- `urili_customer_type`
- `urili_country_region`

## 复用台账检查结果

复用台账应同步复查：

- 当前前端路径已经是 `react-ui/src/pages/Seller/`、`react-ui/src/pages/Buyer/`。
- 如果台账仍写旧 `Urili` 路径，需要改成当前路径。
- `PartnerManagementPage` 当前已移动到 `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx`，也应同步台账。

## 大文件合理性判断结果

本轮只做审计报告，没有修改实现代码，不触发大文件拆分判断。

## 重复代码检查结果

当前实现已经收敛到 `seller/buyer` 模块和共享 `PartnerManagementPage`，没有发现新的 `UriliCustomer` 单表实现路径。

## 验证命令

```powershell
rg --files . -g "!**/node_modules/**" -g "!**/target/**" -g "!**/.git/**" | rg -i "urili"
rg -l -i "urili" RuoYi-Vue react-ui README.md docker-compose.yml AGENTS.md -g "!**/target/**" -g "!**/node_modules/**"
rg -n -i "urili" react-ui\src\pages react-ui\src\services react-ui\src\types react-ui\src\components react-ui\src\utils -g "!**/node_modules/**"
rg -n -i "urili" RuoYi-Vue\ruoyi-admin\src\main\java RuoYi-Vue\ruoyi-system\src\main\java RuoYi-Vue\ruoyi-system\src\main\resources\mapper -g "!**/target/**"
docker run --rm --env "MYSQL_PWD=<masked>" mysql:8.4 mysql --host="<remote-host>" --port="<remote-port>" --user="<user>" --database="fenxiao" -N --default-character-set=utf8mb4 --execute="<urili residual scan sql>"
```

## 未验证原因

- 未运行后端编译、前端构建、接口请求或浏览器验证。
- 本轮是命名残留审计，没有改代码或远端数据。
