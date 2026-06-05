# 2026-06-05 订单管理售后管理菜单记录

## 目标

在管理端「订单管理」下新增「售后管理」二级菜单入口。

## 范围

- 新增菜单入口：`订单管理 / 售后管理`。
- 菜单路径：`/order/after-sale`。
- 组件：`Common/PlannedPage/index`。
- 权限标识：`order:afterSale:list`。
- 本次不新增订单、售后、履约、费用、库存或财务业务表。
- 本次不新增后端接口、按钮权限或真实前端页面。

## 数据源确认

- 已只读确认后端激活配置为 `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 的 `druid` profile。
- 已只读确认 MySQL 配置来源为 `RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`，目标为远端 MySQL，不是本地 Docker MySQL。
- 本记录不写入数据库密码、Redis 密码、token secret 或 `.env.local` 内容。

## 实现内容

- 更新 `RuoYi-Vue/sql/business_menu_seed.sql`，在 `menu_id = 2070` 的「订单管理」下补入：
  - `menu_id = 2412`
  - `menu_name = 售后管理`
  - `order_num = 15`
  - `path = after-sale`
  - `component = Common/PlannedPage/index`
  - `route_name = AfterSaleManagement`
  - `perms = order:afterSale:list`
- 新增可单独执行的幂等 SQL：`RuoYi-Vue/sql/20260605_order_after_sale_menu_seed.sql`。

## 权限检查结果

- 本次只新增页面型菜单权限标识 `order:afterSale:list`，未新增后端接口。
- 因没有后端接口，本轮不存在 `@PreAuthorize` 与按钮权限同步项。
- 后续售后管理进入真实实现时，需要重新设计售后单、订单状态、履约异常、费用影响和按钮权限。

## 字典/选项复用检查结果

- 本次未新增状态、类型、原因、渠道等业务字段或字典。
- 后续售后原因、售后状态、责任方、费用类型等字段进入正式实现前，应优先复用或新增若依 `sys_dict`。

## 复用台账检查结果

- 已复用 `docs/architecture/reuse-ledger.md` 中登记的 `PlannedPage` 占位入口规则。
- 未新增重复占位页面。

## 大文件合理性判断结果

- 本次只改 SQL seed 和记录文件，没有新增或修改超过 300 行的业务代码文件。

## 重复代码检查结果

- 没有新增 React 页面重复实现「功能规划中」。
- 菜单占位继续统一指向 `Common/PlannedPage/index`。

## 验证命令

- `rg -n "2412|售后管理|after-sale|AfterSaleManagement|order:afterSale:list" RuoYi-Vue/sql/20260605_order_after_sale_menu_seed.sql RuoYi-Vue/sql/business_menu_seed.sql docs/plans/2026-06-05-order-after-sale-menu-record.md`
- `jshell --class-path <mysql-connector-j> -q`
- `codegraph sync .`

## 运行库菜单 DML 执行结果

- 执行时间：2026-06-05 23:22:09 +08:00。
- 执行目标：当前激活配置指向的远端 MySQL。
- 执行脚本：`RuoYi-Vue/sql/20260605_order_after_sale_menu_seed.sql`。
- 执行方式：本机 `jshell` 加载 `mysql-connector-j`，从 `application-druid.yml` 读取 master datasource，只执行该菜单 seed。
- 执行语句数：2 条，包含 `set names utf8mb4` 和幂等 `insert into sys_menu ... on duplicate key update`。
- 数据影响：仅新增或更新 `sys_menu.menu_id = 2412` 的页面型菜单记录，不写业务表，不改 Redis。
- 只读复查结果：

| 字段 | 值 |
| --- | --- |
| menu_id | `2412` |
| menu_name | `售后管理` |
| parent_id | `2070` |
| order_num | `15` |
| path | `after-sale` |
| component | `Common/PlannedPage/index` |
| route_name | `AfterSaleManagement` |
| perms | `order:afterSale:list` |
| visible | `0` |
| status | `0` |

## 前端生效说明

- 后端菜单数据已写入运行库。
- 管理端前端会缓存远程菜单到 `sessionStorage.admin_remote_menu`；已登录会话如果仍看不到新菜单，需要退出重登，或清理该 sessionStorage key 后重新加载。
- 本次菜单组件指向统一占位页，因此点击后显示「功能规划中」属于预期。

## 未验证原因

- 未做浏览器点击验证；本次已通过运行库 `sys_menu` 只读复查确认菜单记录生效。
- 若当前浏览器仍持有旧菜单缓存，需要按「前端生效说明」刷新菜单缓存后再看侧边栏。

## CodeGraph 更新结果

- 已执行 `codegraph sync .`。
- 菜单 seed 新增后同步结果为 `Synced 5 changed files`。
- 记录文件补充后再次同步结果为 `Synced 1 changed files`。

## 残留问题

- 「售后管理」当前是占位入口，不是已落地售后业务页面。
- 售后业务正式实现前必须先出 Markdown 设计方案，明确订单状态机、售后单、退件、履约异常、费用/财务影响、权限点和回滚策略。
