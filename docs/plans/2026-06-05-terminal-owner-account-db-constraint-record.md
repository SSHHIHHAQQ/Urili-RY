# 2026-06-05 端内 OWNER 主账号数据库唯一约束执行记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，在前一轮 Service 级 OWNER 主账号唯一性兜底之后，补齐数据库层兜底：同一卖家主体只能有一个 `seller_account.account_role = OWNER`，同一买家主体只能有一个 `buyer_account.account_role = OWNER`。

## 本轮范围

- 新增增量 SQL：`RuoYi-Vue/sql/20260605_terminal_owner_account_unique_constraint.sql`。
- 更新初始化 SQL：`RuoYi-Vue/sql/seller_buyer_management_seed.sql`。
- 更新三端迁移 SQL：`RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`。
- 执行远程 MySQL DDL 前置预检。
- 执行远程 MySQL DDL，并做幂等复跑验证。
- 不改 Java 生产逻辑。
- 不改前端 UI。
- 不处理 `lock_status` / `lock_reason` / 解锁账号。

## 设计说明

MySQL 没有直接的局部唯一索引语法，因此本轮使用“生成列 + 唯一索引”实现 OWNER 局部唯一：

- `seller_account.owner_unique_seller_id`：当 `account_role = 'OWNER'` 时生成 `seller_id`，否则为 `NULL`。
- `uk_seller_account_owner(owner_unique_seller_id)`：唯一索引只会限制 OWNER 账号；多个非 OWNER 账号生成 `NULL`，MySQL 唯一索引允许多个 `NULL`。
- `buyer_account.owner_unique_buyer_id` / `uk_buyer_account_owner` 同理。

该方案不限制同一主体下多个 `ADMIN` 或 `STAFF` 子账号。

## 数据源确认

- 配置来源：本机 `.env.local` 的 `RUOYI_DB_*` 变量。
- 目标类型：远程 MySQL。
- 预检方式：临时 JDBC 程序读取 `.env.local`，只输出连接状态、MySQL 版本、重复 OWNER 组数和目标列/索引存在性；未输出数据库密码或连接串。
- MySQL 版本：`8.0.30-cynos-3.1.16.003`。
- Redis：本轮不连接 Redis，不影响 Redis 数据。

## 执行前预检

- `seller_account` 重复 OWNER 主体组数：`0`。
- `buyer_account` 重复 OWNER 主体组数：`0`。
- `seller_account.owner_unique_seller_id` 存在数：`0`。
- `buyer_account.owner_unique_buyer_id` 存在数：`0`。
- `uk_seller_account_owner` 存在数：`0`。
- `uk_buyer_account_owner` 存在数：`0`。

## 执行命令

```powershell
cd E:\Urili-Ruoyi
# 通过临时 JDBC SQL runner 读取 .env.local 后执行：
RuoYi-Vue/sql/20260605_terminal_owner_account_unique_constraint.sql
```

执行结果：

- 远程 DDL 执行成功。
- 临时 JDBC SQL runner 执行语句数：`16`。
- 执行后 `seller_account` 重复 OWNER 主体组数：`0`。
- 执行后 `buyer_account` 重复 OWNER 主体组数：`0`。
- 执行后 `seller_account.owner_unique_seller_id` 存在数：`1`。
- 执行后 `buyer_account.owner_unique_buyer_id` 存在数：`1`。
- 执行后 `uk_seller_account_owner` 存在数：`1`。
- 执行后 `uk_buyer_account_owner` 存在数：`1`。
- 幂等复跑：通过，同一 SQL 再执行一次后，上述列/索引存在数仍为 `1`，重复 OWNER 主体组数仍为 `0`。

## 未验证原因

- 本轮未做 HTTP smoke；这是数据库约束 DDL，不改变接口路径或前端交互。
- 本轮未做浏览器验收；不改变可见 UI。

## 权限检查结果

- 本轮不新增后端接口，不改变 `@PreAuthorize`。
- 数据库约束是账号模型底线，不替代接口权限和菜单按钮权限。

## 字典/选项复用检查结果

- 本轮未新增字典。
- 继续使用端账号角色 code：`OWNER` / `ADMIN` / `STAFF`。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记端内 OWNER 数据库唯一约束。

## CodeGraph 更新结果

- 已执行 `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 1 changed files`，`Modified: 1 - 22 nodes in 516ms`。
- 收尾复跑 `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 大文件合理性判断结果

- 本轮新增 SQL 文件职责单一，只处理端内 OWNER 主账号数据库唯一约束。
- 修改既有大 SQL 文件是为了保持初始化基线和迁移基线一致，没有混入其他 DDL。

## 重复代码检查结果

- seller/buyer 使用同一生成列方案，差异仅为主体字段和索引名称。

## 回滚方式

如需回滚本轮数据库 DDL：

```sql
alter table seller_account drop index uk_seller_account_owner;
alter table seller_account drop column owner_unique_seller_id;
alter table buyer_account drop index uk_buyer_account_owner;
alter table buyer_account drop column owner_unique_buyer_id;
```

## 残留问题

- `lock_status` / `lock_reason` / 解锁账号仍未设计和落地。
