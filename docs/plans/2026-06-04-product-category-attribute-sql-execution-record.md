# 商品分类与商品属性配置 SQL 执行记录

日期：2026-06-04

## 1. 执行目的

执行商品分类与商品属性配置第一阶段 SQL，使管理端“基础配置 / 商品分类配置、商品属性配置”从占位页切换到正式页面，并创建后端接口需要的商品配置表、字典和按钮权限。

## 2. 用户确认

用户已确认执行全部操作：执行 SQL、重启后端、验证页面。

## 3. 目标环境

- 后端配置来源：
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`
  - `.env.local`
- 激活 profile：`druid`
- MySQL URL 来源：`.env.local` 的 `RUOYI_DB_URL`
- 目标数据库：远端 MySQL，库名 `fenxiao`
- Redis 来源：`.env.local` 的 `RUOYI_REDIS_*`
- 本次不输出、不记录数据库密码、Redis 密码、token secret 或加密密钥明文。

## 4. SQL 文件

```text
RuoYi-Vue/sql/20260604_product_category_attribute_seed.sql
```

## 5. 影响范围

新增或更新：

- `product_category`
- `product_attribute`
- `product_attribute_option`
- `product_category_attribute`
- `sys_dict_type`
- `sys_dict_data`
- `sys_menu`

菜单位置保持在：

- `基础配置 / 商品分类配置`
- `基础配置 / 商品属性配置`

菜单组件更新为：

- `Product/Category/index`
- `Product/Attribute/index`

## 6. 执行方式

使用 JDBC 读取 `.env.local` 的连接环境变量执行 SQL 文件，避免在命令行输出明文凭证。

## 7. 回滚口径

如果 SQL 执行后未产生业务配置数据，可按备份或手工回滚：

- 将 `sys_menu` 中 `2440`、`2441` 的 `component` 恢复为 `Common/PlannedPage/index`。
- 删除本次新增的商品配置按钮权限。
- 删除本次新增的商品配置字典项。
- 如确认表内无业务数据，可删除 4 张商品配置表。

如果已经开始维护分类或属性配置，不建议直接删除表，应先备份并评估配置数据影响。

## 8. 执行结果

已执行完成。

- 执行时间：2026-06-04 18:47 - 18:56
- 执行前备份：`logs/db-backups/product-category-attribute-before-20260604-184722.sql`
- 备份范围：本次相关 `sys_menu` 记录、商品配置表、商品配置字典类型和字典项。
- SQL 执行结果：最终成功执行 19 个 SQL 语句。
- 中途修正：
  - 第一次执行器未过滤 SQL 注释，未产生有效业务执行结果，已修正执行器后重试。
  - 第二次执行发现 3 组字典初始化子查询缺少字段别名，已修正 SQL 文件并重试。
- 最终建表结果：`product_category`、`product_attribute`、`product_attribute_option`、`product_category_attribute` 已存在。
- 最终菜单结果：
  - `基础配置 / 商品分类配置`：`Product/Category/index`
  - `基础配置 / 商品属性配置`：`Product/Attribute/index`
  - 商品配置按钮权限使用 `2470` - `2480`，避免与现有菜单 ID 冲突。
- 后端重启结果：`RuoYi-Vue/ruoyi-admin/target/ruoyi-admin.jar` 已重新构建并启动，当前监听 `8080` 的进程使用 2026-06-04 18:52:43 构建的 jar。
- 接口验证结果：
  - `/getRouters` 返回 `基础配置 / 商品分类配置` 组件 `Product/Category/index`。
  - `/getRouters` 返回 `基础配置 / 商品属性配置` 组件 `Product/Attribute/index`。
  - `/product/admin/categories/list` 返回 `code = 200`。
  - `/product/admin/attributes/list` 返回 `code = 200`。
- 前端页面验证结果：
  - `http://127.0.0.1:8001/basic-config/product-category` 不再显示“功能规划中”，已显示商品分类配置页面。
  - `http://127.0.0.1:8001/basic-config/product-attribute` 不再显示“功能规划中”，已显示属性库和类目属性模板。
- 页面截图：
  - `logs/screenshots/product-category-20260604-1855.png`
  - `logs/screenshots/product-attribute-20260604-1855.png`

备注：如果浏览器仍看到旧的“功能规划中”，通常是旧登录态或前端运行时缓存导致；重新登录或硬刷新后会重新拉取 `/getRouters`。
