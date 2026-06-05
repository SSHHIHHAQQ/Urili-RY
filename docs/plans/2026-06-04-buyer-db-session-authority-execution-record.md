# 买家端 DB 会话权威鉴权执行记录

日期：2026-06-04

## 背景

本次切片以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按“卖家标准模板验收通过后复制买家”的节奏推进。

目标是让买家端 `@PortalPreAuthorize` 鉴权不只依赖 Redis 中的 portal session，还要回查 `buyer_session`，以 DB 中的 `status/logout_time/expire_time` 作为会话是否仍有效的兜底权威。

## 变更范围

代码变更：

- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/mapper/BuyerMapper.java`
- `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml`
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerPortalPermissionServiceImpl.java`

文档变更：

- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md`
- `docs/architecture/reuse-ledger.md`
- `docs/plans/2026-06-04-buyer-db-session-authority-execution-record.md`

本轮未改：

- 不改 seller 代码。
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

为了证明“DB session 已失效但 Redis token 仍存在时，buyer 端接口也会拒绝旧 token”，执行了受控 DML，只作用于本轮新建的测试 `buyer_session` 行：

```sql
update buyer_session
set status = '1',
    logout_time = sysdate()
where token_id = ?
  and buyer_id = ?
  and buyer_account_id = ?;
```

验证拒绝后，为了正常调用 `/buyer/logout` 清理 Redis token 和 DB session，又临时恢复同一测试 session：

```sql
update buyer_session
set status = '0',
    logout_time = null
where token_id = ?
  and buyer_id = ?
  and buyer_account_id = ?;
```

随后调用 `/buyer/logout`，最终仍由业务接口把该测试 session 标记为退出状态。

本轮早期尝试使用 JShell 执行 SQL 时被 PowerShell 原生命令错误流打断；后续改用系统临时目录中的 Java 单文件执行受控 DML，执行后已删除临时文件。最后额外检查最近本地测试窗口内的 `buyer_session` 在线遗留，影响行数为 `0`。

## 验证结果

- `mvn -DskipTests compile`：通过。
- `mvn -DskipTests package`：通过。
- `.\start-backend-local.ps1 -Restart`：成功。
- `/captchaImage`：`code=200`，`captchaEnabled=false`。
- 管理端登录：`code=200`。
- 管理端买家列表：`code=200`。
- 管理端生成买家端免密票据：`code=200`。
- 买家端消费免密票据：`code=200`，`terminal=buyer`。
- buyer token 初次调用 `/buyer/getInfo`：`code=200`。
- 将本轮测试 `buyer_session` 更新为失效：影响行数 `1`。
- Redis token 保留时，旧 buyer token 调 `/buyer/getInfo`：`code=401`，`msg=登录状态已失效`。
- 将同一测试 `buyer_session` 临时恢复：影响行数 `1`。
- 调用 `/buyer/logout` 清理测试 token：`code=200`。
- logout 后旧 buyer token 调 `/buyer/getInfo`：`code=401`。
- 最近本地测试窗口内在线 `buyer_session` 遗留清理检查：影响行数 `0`。

## 检查项

- 权限检查结果：本轮复用 `@PortalPreAuthorize(terminal = "buyer", ...)` 鉴权链，不新增权限点；验证覆盖 `/buyer/getInfo`。
- 字典/选项复用检查结果：本轮不涉及字典或前端选项。
- 复用台账检查结果：已更新 `docs/architecture/reuse-ledger.md` 的 `PortalPreAuthorize` 复用规则。
- CodeGraph 更新结果：已执行 `codegraph sync .`，结果为 `Already up to date`。
- 大文件合理性判断结果：本轮只在既有 Mapper、XML、Service 中增加小段会话校验逻辑，未引入新增大文件。
- 重复代码检查结果：按卖家模板复制到买家，只替换 mapper、表名、字段名和 terminal；保持两端同构，不重新设计。

## 当前判断

- 买家端 DB 会话权威鉴权模板已落地。
- `buyer_session` 现有字段足够承载该规则，不需要新增 DDL。
- seller/buyer 两端已形成一致的 DB 会话权威鉴权模板。
- 管理端强制踢出已有执行接口，但管理端 session 列表查询仍需后续补齐。
