# 端内商品分类权限 SQL 执行记录

日期：2026-06-04

## 目标

按 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 的三端隔离方向，继续推进真实端内业务接口模板：

- 卖家端新增可发布商品分类只读入口：`GET /seller/product/categories`。
- 买家端新增可发布商品分类只读入口：`GET /buyer/product/categories`。
- 接口只返回启用且可发布的商品分类，用于后续选择 `categoryId` 后读取商品 Schema。
- 接口权限分别为 `seller:product:category:list` 和 `buyer:product:category:list`。

## 数据源确认

- 后端激活配置：`spring.profiles.active=druid`。
- MySQL 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml` 通过 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD` 读取。
- Redis 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 通过 `RUOYI_REDIS_*` 读取。
- `.env.local` 存在并包含本机运行所需 key；本记录不写入、不展示任何凭证。

## 执行脚本

- `RuoYi-Vue/sql/20260604_portal_product_category_permission_seed.sql`

脚本内容：

- 幂等插入 `seller_menu.perms = seller:product:category:list` 隐藏按钮权限。
- 幂等插入 `buyer_menu.perms = buyer:product:category:list` 隐藏按钮权限。
- 将该权限绑定到当前启用的卖家端角色。
- 将该权限绑定到当前启用的买家端角色。

## 执行结果

首次临时 Java SQL 执行器因临时源文件 BOM 编码导致 `javac` 失败，未连接数据库、未执行 SQL。随后使用无 BOM 临时执行器重新执行成功。

| 项目 | 结果 |
| --- | ---: |
| 执行语句数 | 4 |
| `seller_menu` 中 `seller:product:category:list` | 1 |
| `seller_role_menu` 中该权限绑定 | 3 |
| 启用 `seller_role` | 3 |
| `buyer_menu` 中 `buyer:product:category:list` | 1 |
| `buyer_role_menu` 中该权限绑定 | 1 |
| 启用 `buyer_role` | 1 |
| 当前启用且可发布商品分类 | 160 |

## 验证结果

- `mvn -DskipTests compile`：通过。
- 停止 8080 旧后端进程后执行 `mvn -DskipTests package`：通过。
- 中途曾因 8080 新 Java 进程占用 `ruoyi-admin.jar` 导致一次 `ruoyi-admin.jar.original` 重命名失败；停止占用进程后重新 package 通过。
- `.\start-backend-local.ps1`：返回成功；8080 正常监听。
- `/captchaImage`：`code=200`，`captchaEnabled=false`。
- 管理端登录：`code=200`。
- 管理端生成卖家端免密票据：`code=200`。
- 管理端生成买家端免密票据：`code=200`。
- 卖家端消费免密票据：`code=200`，`terminal=seller`。
- 买家端消费免密票据：`code=200`，`terminal=buyer`。
- seller token 调 `GET /seller/product/categories`：业务 `code=200`，返回 `160` 个分类。
- buyer token 调 `GET /buyer/product/categories`：业务 `code=200`，返回 `160` 个分类。
- seller/buyer 返回分类中 `publishEnabled != Y` 的数量均为 `0`。
- 响应敏感 key 检查：未发现 `password`、`token`、`tokenId`、`createBy`、`updateBy`、`remark`、`delFlag`。
- admin token 调卖家/买家端分类接口：业务 `code=401`。
- 无 token 调卖家/买家端分类接口：业务 `code=401`。
- seller token 调买家端分类接口：业务 `code=401`。
- buyer token 调卖家端分类接口：业务 `code=401`。
- 伪造 `sellerId`、`buyerId`、`accountId`、`subjectId`、`terminal` 查询参数不能扩大范围，返回数量差异为 `0`。
- 验证后已调用 `POST /seller/logout` 和 `POST /buyer/logout` 清理本轮 portal token。

## 免密登录日志脱敏修复

本轮同时修复子 agent 审计发现的高风险点：

- `POST /seller/admin/sellers/{sellerId}/directLogin` 的 `@Log` 增加 `isSaveResponseData=false`。
- `POST /buyer/admin/buyers/{buyerId}/directLogin` 的 `@Log` 增加 `isSaveResponseData=false`。

验证结果：

- 本轮生成 seller/buyer 免密票据后，`sys_oper_log` 中卖家免密登录日志行数为 `1`。
- 本轮生成 seller/buyer 免密票据后，`sys_oper_log` 中买家免密登录日志行数为 `1`。
- 本轮 directLogin 操作日志中命中 `token`、`loginUrl`、`directLoginToken` 的行数为 `0`。

## 当前判断

- 端内商品分类只读接口已形成商品 Schema 前置入口：端 token 鉴权、端内权限点、端内 DTO 脱敏、可发布分类过滤、跨端拒绝和伪造参数不扩大范围均已验证。
- 端内商品接口继续复用 `product` 模块配置，不新增 seller/buyer 业务表，不在前端或端模块重复计算商品分类规则。
- 免密登录仍会返回明文一次性 token 给管理端页面用于打开端入口，但不会再把响应体写入若依 `sys_oper_log.json_result`。
