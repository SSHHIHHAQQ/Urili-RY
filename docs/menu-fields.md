# URILI 顶级菜单字段记录

日期：2026-06-03

## 当前口径

本文件记录当前若依验证工程的顶级菜单基线。当前阶段只维护 `sys_menu` 顶级目录字段，不新增页面、接口、按钮权限、业务表或字典。

可重复执行的 SQL 文件：

- `RuoYi-Vue/sql/urili_top_menu_seed.sql`

旧文件 `RuoYi-Vue/sql/urili_menu_seed.sql` 已改为兼容说明文件，不再写入旧版“URILI运营后台”包装根目录。

## 顶级菜单顺序

| 顺序 | menu_id | 菜单名 | path | route_name | 说明 |
| --- | ---: | --- | --- | --- | --- |
| 1 | 2010 | 客户管理 | `customer` | `UriliCustomer` | 复用原客户管理菜单位 |
| 2 | 2060 | 商品管理 | `product` | `UriliProduct` | 新增顶级目录 |
| 3 | 2070 | 订单管理 | `order` | `UriliOrder` | 新增顶级目录 |
| 4 | 2080 | 库存管理 | `inventory` | `UriliInventory` | 新增顶级目录 |
| 5 | 2020 | 仓库管理 | `warehouse` | `UriliWarehouse` | 复用原仓储管理菜单位 |
| 6 | 2030 | 海外仓服务设置 | `overseas-warehouse-service` | `UriliOverseasWarehouseService` | 复用原上游系统菜单位 |
| 7 | 2050 | 财务管理 | `finance` | `UriliFinance` | 复用原计费管理菜单位 |
| 8 | 108 | 日志中心 | `log-center` | `LogCenter` | 复用若依原日志管理目录 |
| 9 | 3 | 工具中心 | `tool` | 空 | 复用若依原系统工具目录 |

若依原生 `系统管理` 和 `系统监控` 保留在顶级菜单后面：

| menu_id | 菜单名 | order_num |
| ---: | --- | ---: |
| 1 | 系统管理 | 90 |
| 2 | 系统监控 | 91 |

旧口径的 `2040` 渠道管理保留但停用：

| menu_id | 菜单名 | order_num | visible | status |
| ---: | --- | ---: | --- | --- |
| 2040 | 渠道管理 | 80 | `1` | `1` |

## 边界说明

- 本次没有新增 `perms`，因为只维护 `M` 类型顶级目录。
- 商品、订单、库存当前只有顶级目录，没有二级菜单、页面、接口或按钮权限。
- 日志中心和工具中心复用若依已有子菜单和权限点。
- 后续新增业务表、二级菜单、按钮权限或页面前，仍需按工程规则先提交 Markdown 设计方案并确认。

## 验证记录

详细验证记录见：

- `docs/status/2026-06-03-top-menu-adjustment.md`
- `docs/status/2026-06-03-menu-path-prefix-fix.md`
