# 商城商品列表 SQL 执行记录

日期：2026-06-05

## 1. 执行目标

- 目标功能：管理端“商品管理 / 商城商品列表”首版正式能力。
- SQL 文件：`RuoYi-Vue/sql/20260605_mall_product_distribution_seed.sql`
- 目标 MySQL：远端运行库，连接来源为本机 `.env.local`，地址已脱敏。
- 目标 Redis：远端 Redis，连接来源为本机 `.env.local`，地址已脱敏。
- 连接来源：后端激活配置读取 `.env.local` 中的 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`；执行记录不保存密码或密钥。

## 2. 执行前确认

- `application.yml` 激活 profile 为 `druid`。
- `application-druid.yml` 使用环境变量 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`。
- 本地 Docker daemon 未运行，未读取或写入本地 Docker MySQL/Redis。
- 本机没有 `mysql` 命令行客户端，因此使用 Maven 缓存中的 MySQL JDBC 驱动执行 SQL。
- 用户确认来源：用户已在当前任务链路中确认允许把商城商品列表首版 SQL 执行到当前远端运行库。
- 确认 token：该早期建表脚本执行时未设置独立 SQL session token；后续新增或重放同类远端 DDL/DML 必须补独立确认 token 后再执行。
- 影响范围：创建商品基础表、初始化商品字典和管理端菜单/按钮权限，不清空任何已有表，不删除业务数据。
- 回滚方式：未自动执行回滚；如需回滚，需先确认没有正式商品数据依赖，再人工删除新增表、字典项、菜单按钮并恢复菜单组件。

## 3. 执行内容

执行 SQL 类型：

- DDL：创建 `product_spu`、`product_sku`、`product_attribute_value`、`product_image`。
- DML：初始化 `product_sales_status`、`product_source_type` 字典。
- DML：更新菜单 `2402` 的组件为 `Product/Distribution/index`。
- DML：新增 `product:distribution:query/add/edit/status` 按钮权限。

不包含：

- 不清空任何已有表。
- 不删除任何业务数据。
- 不修改卖家、买家、订单、库存、财务、上游系统业务事实数据。

## 4. 执行命令类型

使用 PowerShell 从 `.env.local` 读取连接变量，通过 `jshell` 加载 MySQL JDBC 驱动执行 SQL 文件。

执行结果：

```text
OK statement 1/11
OK statement 2/11
OK statement 3/11
OK statement 4/11
OK statement 5/11
OK statement 6/11
OK statement 7/11
OK statement 8/11
OK statement 9/11
OK statement 10/11
OK statement 11/11
DONE statements=11
```

## 5. 执行后验证

验证结果：

```text
TABLE product_spu exists=1
TABLE product_sku exists=1
TABLE product_attribute_value exists=1
TABLE product_image exists=1
MENU2402 component=Product/Distribution/index, perms=product:distribution:list
PERM_COUNT product:distribution=5
DICT product_sales_status=5
DICT product_source_type=3
```

## 6. 风险与后续

- SQL 已执行到当前远端运行库。
- 仍需重启后端，使新编译代码进入运行态。
- 仍需用管理端账号验证新接口和页面入口。
- 如果后续需要给非 admin 角色授权，需要按角色补 `sys_role_menu` 分配；本脚本只创建菜单和按钮权限点。
