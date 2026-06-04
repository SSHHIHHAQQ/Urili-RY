# 卖家端商品 Schema 权限 SQL 执行记录

日期：2026-06-04

## 目标

按 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 的三端隔离方向，先做一套卖家端标准模板：

- `GET /seller/product/categories/{categoryId}/schema` 只允许卖家端 token 访问。
- 接口权限点为 `seller:product:schema:query`。
- 本轮只做卖家端，不复制买家端。
- 本轮只做端内商品 Schema 只读样板，不做商品发布保存、审核、SKU、库存、价格、买家端接口或前端页面。

## 数据源确认

- 连接来源：本机 `.env.local` 中的 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`。
- 配置基线：`application.yml` 激活 `druid`；`application-druid.yml` 使用 `RUOYI_DB_URL` 环境变量。
- 本记录不输出数据库地址、账号或密码。
- 当前目标追踪已确认允许对远程数据库执行 DDL/DML；本脚本只做卖家端权限 DML，不做 DDL。

## 执行脚本

- `RuoYi-Vue/sql/20260604_seller_product_schema_permission_seed.sql`

脚本行为：

- 幂等创建当前启用卖家主体的默认端内 Owner 角色。
- 幂等绑定当前 `account_role = OWNER` 的卖家账号到默认端内 Owner 角色。
- 幂等插入 `seller_menu.perms = seller:product:schema:query` 隐藏按钮权限。
- 将该权限授予当前启用且未删除的 `seller_role`。
- 菜单名称和备注使用 ASCII，避免数据库字符集或客户端编码导致菜单名称显示为问号。

## 执行结果

已执行。

执行方式：

- 本机未安装 `mysql` CLI。
- 最终使用临时 Java 执行器读取 `.env.local` 和本 SQL 文件，通过项目已缓存的 MySQL JDBC 驱动执行。
- 临时 Java 执行器和编译产物已在执行后删除。
- 输出和记录均不包含数据库地址、账号、密码、token、免密登录 URL 或 Redis key。

执行后计数：

| 项目 | 结果 |
| --- | ---: |
| 执行语句数 | 4 |
| `seller_menu` 中 `seller:product:schema:query` | 1 |
| `seller_role_menu` 中该权限绑定 | 3 |
| 启用 `seller_role` | 3 |
| OWNER 卖家账号绑定 owner 角色 | 3 |

## 验证计划

- `seller_menu` 中 `seller:product:schema:query` 数量为 `1`。
- `seller_role_menu` 中该权限绑定数量大于 `0`。
- 管理端登录后生成卖家端免密 token，并消费为 seller portal token。
- seller token 调 `GET /seller/product/categories/{categoryId}/schema` 返回 `code=200`。
- 响应不包含 `password`、`token`、`tokenId`、`createBy`、`updateBy`、`remark`、Redis key。
- admin token 不能代替 seller token 访问该接口。
- 无 token 不能访问该接口。
- 伪造 `sellerId`、`buyerId`、`accountId`、`terminal` 查询参数不能改变返回范围。
- 本轮不验证 buyer 端；buyer 端等卖家模板验收通过后复制。

## 验证结果

- 数据源确认：tracked YAML 通过 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD` 和 `RUOYI_REDIS_*` 环境变量读取 MySQL/Redis 配置；`.env.local` 存在且包含所需 key。
- `mvn -DskipTests compile`：通过。
- 停止旧 8080 后端进程后执行 `mvn -DskipTests package`：通过，已重新打包 `ruoyi-admin.jar`。
- `.\start-backend-local.ps1`：返回成功；8080 正常监听。
- `/captchaImage`：`code=200`，`captchaEnabled=false`。
- 管理端登录：`code=200`。
- 验证卖家主体：`sellerId=9`。
- 验证商品类目：`categoryId=446`。
- 当前远程库商品 Schema 配置状态：本轮抽查可发布类目后，选中类目的管理端 schema 字段数为 `0`；因此卖家端接口正向响应为 `data=[]`。这说明接口模板和权限边界可验证，但商品配置内容还需要后续类目属性绑定数据支撑。
- 管理端生成卖家端免密票据：`code=200`。
- 消费免密票据建立卖家端会话：`code=200`，`terminal=seller`。
- seller token 调 `GET /seller/product/categories/446/schema`：HTTP 200，业务 `code=200`。
- schema 返回字段数：`0`。
- schema 返回必填字段数：`0`。
- schema 返回选项数：`0`。
- 响应敏感 key 检查：未发现 `password`、`token`、`tokenId`、`createBy`、`updateBy`、`remark`、Redis key。
- admin token 调同一接口：HTTP 200，业务 `code=401`，不能代替 seller token。
- 无 token 调同一接口：HTTP 200，业务 `code=401`。
- seller token 携带伪造 `sellerId=999999&buyerId=999999&accountId=999999&terminal=buyer` 调同一接口：业务 `code=200`，返回数量仍为 `0`；接口未读取这些参数扩大范围。
- POST 探测同一路径：HTTP 200，业务 `code=500`；没有写入入口。后续如统一异常体验，可把无映射方法整理成更明确的 405/404，但本切片不扩大范围。
- 验证后已调用 `POST /seller/logout` 清理本轮 seller portal token。

## 当前判断

- 卖家端商品 Schema 只读模板已经具备标准形态：端内路径、seller token 鉴权、端内权限点、`PortalSessionContext`、`PortalLog`、product schema 复用、端内 DTO 脱敏。
- 本切片只落卖家端，未新增 buyer endpoint、buyer 权限、buyer 前端 service 或买家端验证。
- 后续 buyer 端复制时，只替换 terminal、路径、权限点、日志 title 和 seed 表名，不重新设计 schema 逻辑。
