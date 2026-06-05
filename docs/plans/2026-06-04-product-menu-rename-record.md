# 商品管理菜单重命名记录

## 目标

将商品管理下两个占位菜单的展示名调整为当前确认口径：

- 原「商品列表」改为「来源商品库」
- 原「分销商品」改为「商城商品列表」

## 调整范围

- 修改文件：`RuoYi-Vue/sql/business_menu_seed.sql`
- 菜单 ID 保持不变：`2400`、`2402`
- 路由、组件、权限标识、排序、图标保持不变
- 未新增表、接口、权限点、字典或前端页面

## 数据库影响

用户已在 2026-06-04 明确授权同步运行库菜单。

执行前配置确认：

- 激活配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`，`spring.profiles.active=druid`
- MySQL 连接来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml` 的 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD`
- 目标环境：远端 MySQL
- 目标库：`fenxiao`
- 本地 Docker 检查：未发现本地 MySQL 或 Redis 容器参与本次验证
- 执行命令类型：定点 DML，仅更新 `sys_menu.menu_id in (2400, 2402)` 的 `menu_name`、`remark`、`update_by`、`update_time`
- 影响范围：只影响管理端菜单展示名和菜单备注，不修改路由、组件、权限标识、排序、图标、角色授权或业务数据

计划执行 SQL：

```sql
update sys_menu
set menu_name = '来源商品库',
    remark = '商品管理菜单：来源商品库，占位入口',
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2400;

update sys_menu
set menu_name = '商城商品列表',
    remark = '商品管理菜单：商城商品列表，占位入口',
    update_by = 'admin',
    update_time = sysdate()
where menu_id = 2402;
```

## 验证记录

- 已搜索当前仓库旧菜单名出现位置：仅命中 `RuoYi-Vue/sql/business_menu_seed.sql`
- CodeGraph 更新：已执行 `codegraph sync .`，结果为 `Already up to date`
- 运行库 DML：已执行，更新 `2` 行
- 运行库执行前：
  - `2400`：`商品列表`
  - `2402`：`分销商品`
- 运行库执行后：
  - `2400`：`来源商品库`
  - `2402`：`商城商品列表`
- 运行库校验：已通过，两个菜单 ID 的 `menu_name` 与 `remark` 均已同步为新名称
