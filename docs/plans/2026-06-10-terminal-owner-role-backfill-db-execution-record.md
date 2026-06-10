# 2026-06-10 端内 OWNER 主账号角色补齐远端执行记录

## 记录性质

本文件记录一次远端 MySQL 精确 DML 修复，用于补齐三端独立账号权限改造中的存量卖家 OWNER 主账号控制面缺口。

- 参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`
- 执行模式：快速推进 P0/P1，只处理权限/串端/service/字段缺失类阻塞项
- 浏览器、截图、DOM、UI 细调：本轮跳过

## 用户确认与执行边界

当前 active goal 已包含“远程数据库 DDL/DML 已确认可以执行”。本次只执行本文件列明的精确 DML，不作为后续任意 SQL 自动授权。

## 数据源确认

- 连接来源：本机 `.env.local` 的 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD`
- 目标库名：`fenxiao`
- Redis：不读取、不写入
- 后端服务：不启动、不重启
- 密钥处理：不在记录或输出中写入 host、用户名、密码、Redis 密码、token secret

## 背景

2026-06-10 远端运行库只读聚合发现：

- `seller_owner_account_missing_owner_role_count=1`
- 精确目标：`seller_id=10`、`seller_account_id=9`、`user_name=pengju`
- `seller_id=10` 当前缺 active `owner` 角色，也缺 owner 默认菜单授权
- buyer 侧未发现同类缺口

代码侧已修复未来路径：`SellerServiceImpl` / `BuyerServiceImpl` 在 OWNER 主账号创建或同步时，会确保 owner 角色、默认基础菜单授权和主账号角色绑定闭环。本记录只处理既有运行库存量数据。

## 执行前只读预览

候选集合：

| 项 | 值 |
| --- | --- |
| `seller_id` | `10` |
| `seller_no` | `SAF090001` |
| `seller_name` | `guagua` |
| `seller.status` | `0` |
| `seller_account_id` | `9` |
| `user_name` | `pengju` |
| `account_role` | `OWNER` |
| `seller_account.status` | `0` |
| `lock_status` | `0` |

执行前状态：

- `seller_id=10` 不存在 `role_key='owner'` 的角色记录
- `seller_account_id=9` 不存在任何 account-role 绑定
- seller 默认 owner 基础菜单 7 个均存在并启用：
  - `seller:account:list`
  - `seller:account:loginLog:list`
  - `seller:account:operLog:list`
  - `seller:account:session:list`
  - `seller:dept:list`
  - `seller:role:list`
  - `seller:portal:home`
- `seller_id=10` owner product grant 数量为 `0`

执行前签名：

```text
2c46558ccd06926dcc9ad9b3e3fceae0a780d974f7b1e47406abe23344ac68c7
```

执行脚本必须重新计算同一签名；不匹配则回滚并退出。

## DML 范围

只允许影响以下三张卖家端内权限表：

- `seller_role`
- `seller_role_menu`
- `seller_account_role`

禁止触碰：

- 管理端 `sys_user` / `sys_role` / `sys_menu` / `sys_dept`
- 买家端 `buyer_*`
- 商品、订单、库存、财务、外部系统等业务表
- Redis

## 执行逻辑

事务内执行：

1. 重新只读计算精确候选集合签名。
2. 签名必须等于 `2c46558ccd06926dcc9ad9b3e3fceae0a780d974f7b1e47406abe23344ac68c7`。
3. 插入 `seller_role`：
   - `seller_id=10`
   - `role_name='Owner'`
   - `role_key='owner'`
   - `role_sort=1`
   - `status='0'`
   - `del_flag='0'`
4. 用新 owner role 绑定 7 个默认基础菜单。
5. 将 `seller_account_id=9` 绑定到新 owner role。
6. 事务内复核：owner role 数量、默认菜单授权数量、账号 owner role 绑定、product grant 数量。

## 回滚方式

如需要立即回滚本次补齐，可在确认没有新的业务操作依赖该 owner 角色后，按相反顺序删除本次插入的数据：

1. 删除 `seller_account_role` 中 `seller_account_id=9` 对应的本次 owner role 绑定。
2. 删除 `seller_role_menu` 中本次 owner role 对应的 7 个默认基础菜单绑定。
3. 删除 `seller_role` 中 `seller_id=10`、`role_key='owner'`、`create_by='admin'`、`remark='默认卖家端 Owner 角色'` 的本次新增角色。

不建议在已有端内账号登录、角色调整或后续业务依赖发生后直接回滚；届时需重新生成只读预览和精确回滚方案。

## 执行结果

执行时间：2026-06-10 17:53，本机 `Asia/Shanghai`。

执行方式：

- 使用 Python + `pymysql` 从 `.env.local` 读取当前激活远端 MySQL 配置。
- 未输出 host、用户名、密码或 Redis 配置。
- `autocommit=false`，事务内重新计算签名，签名匹配后执行 DML，事务内复核通过后 `commit`。

签名复核：

```text
2c46558ccd06926dcc9ad9b3e3fceae0a780d974f7b1e47406abe23344ac68c7
```

DML 结果：

| 表 | 动作 | 影响 |
| --- | --- | --- |
| `seller_role` | 新增 `seller_id=10` 的 active `owner` 角色 | 1 行，`seller_role_id=11` |
| `seller_role_menu` | 绑定 owner 默认 7 个基础菜单 | 7 行 |
| `seller_account_role` | 绑定 `seller_account_id=9` 到 `seller_role_id=11` | 1 行 |

事务内复核：

```json
{
  "owner_role_count": 1,
  "owner_menu_count": 7,
  "account_owner_role_count": 1,
  "owner_product_grants": 0
}
```

## 执行后只读复核

全局 seller/buyer 端内 OWNER 控制面复核：

```json
{
  "seller_missing_owner_binding": 0,
  "seller_owner_role_without_default_menu": 0,
  "seller_owner_product_grants": 0,
  "seller_role_menu_missing_menu": 0,
  "seller_role_menu_missing_role": 0,
  "seller_account_role_missing_account": 0,
  "seller_account_role_missing_role": 0,
  "buyer_missing_owner_binding": 0,
  "buyer_owner_role_without_default_menu": 0,
  "buyer_owner_product_grants": 0,
  "buyer_role_menu_missing_menu": 0,
  "buyer_role_menu_missing_role": 0,
  "buyer_account_role_missing_account": 0,
  "buyer_account_role_missing_role": 0
}
```

针对本次目标：

- `seller_id=10` 已有 `seller_role_id=11` / `role_key='owner'`。
- `seller_role_id=11` 已绑定 7 个默认基础菜单。
- `seller_account_id=9` 已绑定到 `seller_role_id=11`。
- `seller_id=10` owner 角色没有 `seller:product:%` 权限授权。

## 收尾验证

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：通过，seller 63 tests、buyer 64 tests。

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest,TerminalSqlIsolationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：通过，93 tests。

```powershell
cd E:\Urili-Ruoyi\react-ui
node scripts\verify-three-terminal.mjs --check-manifest
```

结果：通过，`three-terminal manifest check passed`。

## 结论

- 远端 `fenxiao` 中 `seller_id=10` / `seller_account_id=9` 的 OWNER 主账号控制面缺口已修复。
- 本次只执行 seller 端内三张权限表的精确 DML，不触碰 `sys_*`、`buyer_*`、product、inventory、finance、integration 等表。
- 未读写 Redis，未启动或重启后端。
- 本次修复不包含浏览器、截图、DOM 或 UI 细调验收。
