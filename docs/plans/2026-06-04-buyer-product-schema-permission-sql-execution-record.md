# 买家端商品 Schema 权限 SQL 执行记录

日期：2026-06-04

## 目标

按 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 的三端隔离方向，在卖家端标准模板验收后同构复制买家端商品 Schema 只读入口：

- `GET /buyer/product/categories/{categoryId}/schema` 只允许买家端 token 访问。
- 接口权限点为 `buyer:product:schema:query`。
- 本轮只复制买家端后端只读入口和买家端权限 DML，不做前端页面。
- 本轮不做商品详情、搜索筛选、购物车、订单、价格、库存或商品发布。

## 数据源确认

- 连接来源：本机 `.env.local` 中的 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`。
- 配置基线：`application.yml` 激活 `druid`；`application-druid.yml` 使用 `RUOYI_DB_URL` 环境变量。
- 本记录不输出数据库地址、账号或密码。
- 当前目标追踪已确认允许对远程数据库执行 DDL/DML；本脚本只做买家端权限 DML，不做 DDL。

## 执行脚本

- `RuoYi-Vue/sql/20260604_buyer_product_schema_permission_seed.sql`

脚本行为：

- 幂等创建当前启用买家主体的默认端内 Owner 角色。
- 幂等绑定当前 `account_role = OWNER` 的买家账号到默认端内 Owner 角色。
- 幂等插入 `buyer_menu.perms = buyer:product:schema:query` 隐藏按钮权限。
- 将该权限授予当前启用且未删除的 `buyer_role`。
- 菜单名称和备注使用 ASCII，避免数据库字符集或客户端编码导致菜单名称显示为问号。

## 执行结果

已执行。

执行方式：

- 本机未安装 `mysql` CLI。
- 使用临时 Java 执行器读取 `.env.local` 和本 SQL 文件，通过项目已缓存的 MySQL JDBC 驱动执行。
- 临时 Java 执行器和编译产物已在执行后删除。
- 输出和记录均不包含数据库地址、账号、密码、token、免密登录 URL 或 Redis key。

执行后计数：

| 项目 | 结果 |
| --- | ---: |
| 执行语句数 | 4 |
| `buyer_menu` 中 `buyer:product:schema:query` | 1 |
| `buyer_role_menu` 中该权限绑定 | 1 |
| 启用 `buyer_role` | 1 |
| OWNER 买家账号绑定 owner 角色 | 1 |

## 验证计划

- `buyer_menu` 中 `buyer:product:schema:query` 数量为 `1`。
- `buyer_role_menu` 中该权限绑定数量大于 `0`。
- 管理端登录后生成买家端免密 token，并消费为 buyer portal token。
- buyer token 调 `GET /buyer/product/categories/{categoryId}/schema` 返回 `code=200`。
- 响应不包含 `password`、`token`、`tokenId`、`createBy`、`updateBy`、`remark`、Redis key。
- admin token 不能代替 buyer token 访问该接口。
- 无 token 不能访问该接口。
- seller token 不能访问 buyer 端 schema 接口。
- 伪造 `sellerId`、`buyerId`、`accountId`、`terminal` 查询参数不能改变返回范围。

## 验证结果

- 数据源确认：tracked YAML 通过 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD` 和 `RUOYI_REDIS_*` 环境变量读取 MySQL/Redis 配置；`.env.local` 存在且包含所需 key。
- `mvn -DskipTests compile`：通过。
- 停止旧 8080 后端进程后执行 `mvn -DskipTests package`：通过，已重新打包 `ruoyi-admin.jar`。
- `.\start-backend-local.ps1`：返回成功；8080 正常监听。
- `/captchaImage`：`code=200`，`captchaEnabled=false`。
- 管理端登录：`code=200`。
- 当前远程库商品 Schema 配置状态：本轮选中的启用且可发布类目，管理端 schema 字段数为 `8`。
- 管理端生成买家端免密票据：`code=200`。
- 消费免密票据建立买家端会话：`code=200`，`terminal=buyer`。
- buyer token 调 `GET /buyer/product/categories/{categoryId}/schema`：HTTP 200，业务 `code=200`。
- schema 返回字段数：`8`。
- schema 返回必填字段数：`5`。
- schema 返回选项数：`40`。
- 响应敏感 key 检查：未发现 `password`、`token`、`tokenId`、`createBy`、`updateBy`、`remark`、Redis key。
- admin token 调同一接口：业务 `code=401`，不能代替 buyer token。
- 无 token 调同一接口：业务 `code=401`。
- seller token 调同一接口：业务 `code=401`，不能跨端访问 buyer schema。
- buyer token 携带伪造 `sellerId=999999&buyerId=999999&accountId=999999&terminal=seller` 调同一接口：业务 `code=200`，返回数量差异为 `0`；接口未读取这些参数扩大范围。
- 验证后已调用 `POST /buyer/logout` 和 `POST /seller/logout` 清理本轮 buyer/seller portal token。
- seller 回归验证：seller token 调 `GET /seller/product/categories/{categoryId}/schema` 返回业务 `code=200`，字段数 `8`，敏感 key 命中数 `0`。

## 当前判断

- 买家端商品 Schema 只读入口已按卖家模板复制完成：端内路径、buyer token 鉴权、端内权限点、`PortalSessionContext`、`PortalLog`、product schema 复用、端内 DTO 脱敏。
- `ProductPortalSchemaController` 已把 `@Anonymous` 下沉到具体方法，避免类级别匿名放行导致未来新增方法漏挂 `@PortalPreAuthorize`。
- 端内 schema 现在同时过滤 `status='0'` 和 `visibleFlag='Y'`，不把不可见字段交给前端隐藏。
- 当前 buyer 权限 seed 运行库执行成功；旁路检查提醒如果历史上存在软删除的同名 owner 角色，类似 seed 可能撞唯一键，后续同类 seed 应统一设计“恢复软删角色或显式处理冲突”的策略。
