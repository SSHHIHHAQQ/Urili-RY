# 2026-06-08 海外仓服务设置菜单调整记录

## 目标

按用户要求调整管理端菜单：

- 在「海外仓服务设置 / 报价方案」下方新增「物流商管理」菜单。
- 将「系统渠道管理」和「客户渠道管理」从「渠道管理」目录迁入「海外仓服务设置」。
- 删除迁出后空的「渠道管理」顶级目录。

## 范围

- 只调整若依 `sys_menu` 菜单结构。
- 保留系统渠道和客户渠道已有菜单 ID、路径、组件、权限标识。
- 保留客户渠道已有按钮权限 `2260/2261` 挂在 `2042` 下。
- 本次不新增物流商、渠道、报价方案或费用相关业务表。
- 本次不新增后端接口、按钮权限或真实前端页面。

## 数据源确认

- 后端激活配置以 `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 和 `application-druid.yml` 为准。
- 当前 `application-druid.yml` 使用 `${RUOYI_DB_*}` 占位，实际运行连接从本机 `.env.local` 读取。
- 本记录不写入数据库密码、Redis 密码、token secret 或 `.env.local` 内容。

## 运行库调整前只读快照

| menu_id | 菜单 | parent_id | order_num | path | component | route_name | visible | status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 2030 | 海外仓服务设置 | 0 | 30 | `overseas-warehouse-service` |  | `OverseasWarehouseServiceManagement` | `0` | `0` |
| 2051 | 操作费设置 | 2030 | 1 | `billing-handling-fee` | `Billing/HandlingFee/index` | `BillingHandlingFee` | `0` | `0` |
| 2052 | 运费设置 | 2030 | 2 | `billing-freight` | `Billing/Freight/index` | `BillingFreight` | `0` | `0` |
| 2053 | 报价方案 | 2030 | 3 | `billing-quote-scheme` | `Billing/QuoteScheme/index` | `BillingQuoteScheme` | `0` | `0` |
| 2031 | 上游系统管理 | 2030 | 5 | `upstream-system` | `UpstreamSystem/index` | `UpstreamSystem` | `0` | `0` |
| 2040 | 渠道管理 | 0 | 28 | `channel` |  | `Channel` | `0` | `0` |
| 2041 | 系统渠道管理 | 2040 | 1 | `channel-system` | `Channel/System/index` | `ChannelSystem` | `0` | `0` |
| 2042 | 客户渠道管理 | 2040 | 2 | `channel-customer` | `Channel/Customer/index` | `ChannelCustomer` | `0` | `0` |

## 实现内容

- 新增幂等 SQL：`RuoYi-Vue/sql/20260608_overseas_channel_carrier_menu_restructure.sql`。
- 新增菜单：
  - `menu_id = 2054`
  - `menu_name = 物流商管理`
  - `parent_id = 2030`
  - `order_num = 4`
  - `path = logistics-carrier`
  - `component = Common/PlannedPage/index`
  - `route_name = LogisticsCarrier`
  - `perms = logistics:carrier:list`
- 迁移菜单：
  - `2041 系统渠道管理` 改为 `parent_id = 2030`，`order_num = 5`。
  - `2042 客户渠道管理` 改为 `parent_id = 2030`，`order_num = 6`。
  - `2031 上游系统管理` 顺延为 `order_num = 7`。
- 删除历史目录：
  - `2040 渠道管理` 在确认无子菜单后删除。
  - 同步删除 `sys_role_menu.menu_id = 2040` 的角色菜单关联，避免孤儿授权。

## 权限检查结果

- 本次只新增页面型菜单权限标识 `logistics:carrier:list`，未新增后端接口。
- `2041/2042` 保留原权限：`channel:system:list`、`channel:customer:list`。
- `2042` 下已有按钮权限 `2260/2261` 保留，不重建、不改父级。
- 因本轮无后端接口新增，不涉及 `@PreAuthorize` 同步。

## 字典/选项复用检查结果

- 本次未新增物流商类型、渠道类型、报价类型等业务字段或字典。
- 后续落地物流商业务表或页面前，应优先复用若依 `sys_dict` 或已有 integration/warehouse option catalog。

## 复用台账检查结果

- 已检查 `docs/architecture/reuse-ledger.md` 中菜单 slot guard 和 `PlannedPage` 规则。
- 本次新增菜单继续使用统一占位页 `Common/PlannedPage/index`，不新增重复占位页面。

## 大文件合理性判断结果

- 本次新增 SQL 与记录文件均职责单一，没有新增或修改超过 300 行的业务代码文件。

## 重复代码检查结果

- 未创建新的 React 占位页面。
- 未复制渠道或上游系统业务逻辑。

## 验证命令

- 运行库只读查询：查询 `2030/2031/2040/2041/2042/2051/2052/2053/2054/2260/2261` 菜单。
- `mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest#datedHighImpactSqlScriptsMustBeAutoDiscoveredAndGuarded test`
- `RuoYi-Vue/sql/20260608_overseas_channel_carrier_menu_restructure.sql`
- 幂等重放：再次执行 `RuoYi-Vue/sql/20260608_overseas_channel_carrier_menu_restructure.sql`
- `git diff --check -- RuoYi-Vue/sql/20260608_overseas_channel_carrier_menu_restructure.sql docs/plans/2026-06-08-overseas-channel-carrier-menu-restructure-record.md docs/architecture/reuse-ledger.md`
- `codegraph sync .`

## 运行库 DML 执行结果

- 执行时间：2026-06-08 01:06:56 +08:00。
- 执行目标：当前激活配置指向的远端 MySQL。
- 执行脚本：`RuoYi-Vue/sql/20260608_overseas_channel_carrier_menu_restructure.sql`。
- 执行方式：本机 `jshell` 加载 `mysql-connector-j`，从 `.env.local` 读取 `RUOYI_DB_*` 运行变量，只执行该菜单脚本。
- 数据影响：只写 `sys_menu` 和 `sys_role_menu` 中的菜单结构与历史顶级目录关联；不写业务表，不改 Redis。
- 失败重试记录：
  - 首次执行在 guard 阶段失败，错误为 `Can't reopen table: 'seed'`。
  - 失败发生在菜单 DML 前；修正 guard 写法为单次 join 临时表后重跑。
- 成功执行语句数：26 条。
- 幂等重放结果：再次执行成功，`replay_check=carrier2054:1|movedChannels:2|menu2040:0|role2040:0`。

## 运行库调整后只读快照

| menu_id | 菜单 | parent_id | order_num | path | component | route_name | visible | status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 2030 | 海外仓服务设置 | 0 | 30 | `overseas-warehouse-service` |  | `OverseasWarehouseServiceManagement` | `0` | `0` |
| 2051 | 操作费设置 | 2030 | 1 | `billing-handling-fee` | `Billing/HandlingFee/index` | `BillingHandlingFee` | `0` | `0` |
| 2052 | 运费设置 | 2030 | 2 | `billing-freight` | `Billing/Freight/index` | `BillingFreight` | `0` | `0` |
| 2053 | 报价方案 | 2030 | 3 | `billing-quote-scheme` | `Billing/QuoteScheme/index` | `BillingQuoteScheme` | `0` | `0` |
| 2054 | 物流商管理 | 2030 | 4 | `logistics-carrier` | `Common/PlannedPage/index` | `LogisticsCarrier` | `0` | `0` |
| 2041 | 系统渠道管理 | 2030 | 5 | `channel-system` | `Channel/System/index` | `ChannelSystem` | `0` | `0` |
| 2042 | 客户渠道管理 | 2030 | 6 | `channel-customer` | `Channel/Customer/index` | `ChannelCustomer` | `0` | `0` |
| 2031 | 上游系统管理 | 2030 | 7 | `upstream-system` | `UpstreamSystem/index` | `UpstreamSystem` | `0` | `0` |
| 2260 | 客户渠道查询 | 2042 | 1 |  |  |  | `0` | `0` |
| 2261 | 客户渠道新增 | 2042 | 2 |  |  |  | `0` | `0` |

清理复查：

- `sys_menu.menu_id = 2040`：0 条。
- `sys_role_menu.menu_id = 2040`：0 条。
- `sys_menu.parent_id = 2040`：0 条。

## 未验证原因

- 未做浏览器点击验证；本次已通过运行库 `sys_menu` 只读复查和幂等重放确认菜单结构生效。
- 浏览器侧边栏会缓存远程菜单；已登录会话需要退出重登或清理对应 remote menu sessionStorage key。

## CodeGraph 更新结果

- 已执行 `codegraph sync .`，结果为 `Already up to date`。

## 残留问题

- 「物流商管理」当前是占位入口，不是已落地物流商业务页面。
- 「系统渠道管理」「客户渠道管理」「操作费设置」「运费设置」「报价方案」仍指向尚未实现的页面组件，当前依赖动态路由缺失页面兜底显示「功能规划中」。
