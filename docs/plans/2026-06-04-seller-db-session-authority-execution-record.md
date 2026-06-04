# 卖家端 DB 会话权威鉴权执行记录

日期：2026-06-04

## 背景

本次切片以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按“先做卖家标准模板，验收通过后再复制买家”的节奏推进。

目标是让卖家端 `@PortalPreAuthorize` 鉴权不只依赖 Redis 中的 portal session，还要回查 `seller_session`，以 DB 中的 `status/logout_time/expire_time` 作为会话是否仍有效的兜底权威。

## 变更范围

代码变更：

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/mapper/SellerMapper.java`
- `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml`
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerPortalPermissionServiceImpl.java`

文档变更：

- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`
- `docs/architecture/reuse-ledger.md`
- `docs/plans/2026-06-04-seller-db-session-authority-execution-record.md`

本轮未改：

- 不改 buyer 代码。
- 不新增 DDL。
- 不新增权限点。
- 不改前端。

## 数据源确认

连接来源：

- 后端配置激活 `druid`。
- MySQL/Redis 均通过 `.env.local` 中的 `RUOYI_*` 运行变量注入。
- 本记录不输出远程 MySQL/Redis 地址、账号、密码、Redis 密码或 token secret。

执行环境：

- 后端地址：`http://127.0.0.1:8080`
- 登录账号：`admin / admin123`

## 远程 DML 说明

本轮没有新增业务 DDL。

为了证明“DB session 已失效但 Redis token 仍存在时，seller 端接口也会拒绝旧 token”，执行了受控 DML，只作用于本轮新建的测试 `seller_session` 行：

```sql
update seller_session
set status = '1',
    logout_time = sysdate()
where token_id = ?
  and seller_id = ?
  and seller_account_id = ?;
```

验证拒绝后，为了正常调用 `/seller/logout` 清理 Redis token 和 DB session，又临时恢复同一测试 session：

```sql
update seller_session
set status = '0',
    logout_time = null
where token_id = ?
  and seller_id = ?
  and seller_account_id = ?;
```

随后调用 `/seller/logout`，最终仍由业务接口把该测试 session 标记为退出状态。

本轮还通过管理端 `DELETE /seller/admin/sellers/{sellerId}/sessions` 清理早期脚本失败遗留的同卖家测试 session。

## 验证结果

- `mvn -DskipTests compile`：通过。
- `mvn -DskipTests package`：通过。
- `.\start-backend-local.ps1 -Restart`：成功。
- `/captchaImage`：`code=200`，`captchaEnabled=false`。
- 管理端登录：`code=200`。
- 管理端卖家列表：`code=200`。
- 管理端生成卖家端免密票据：`code=200`。
- 卖家端消费免密票据：`code=200`，`terminal=seller`。
- seller token 初次调用 `/seller/getInfo`：`code=200`。
- 将本轮测试 `seller_session` 更新为失效：影响行数 `1`。
- Redis token 保留时，旧 seller token 调 `/seller/getInfo`：`code=401`，`msg=登录状态已失效`。
- 将同一测试 `seller_session` 临时恢复：影响行数 `1`。
- 调用 `/seller/logout` 清理测试 token：`code=200`。
- logout 后旧 seller token 调 `/seller/getInfo`：`code=401`。
- 管理端清理同卖家遗留测试 session：`code=200`，影响行数 `1`。

## 当前判断

- 卖家端 DB 会话权威鉴权模板已落地。
- `seller_session` 现有字段足够承载该规则，不需要新增 DDL。
- 买家端尚未复制，后续应按本模板只替换 `buyer` mapper、表名、字段名和 terminal。
- 管理端强制踢出已有执行接口，但管理端 session 列表查询仍需后续补齐。
