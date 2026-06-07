# 2026-06-07 三端隔离 P0/P1 综合 Seed 执行画像 Guard 记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

执行模式：快速推进模式，只修 P0/P1（编译、guard、接口、权限、串端、service/字段缺失）；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 本轮目标

- 收口 `RuoYi-Vue/sql/seller_buyer_management_seed.sql` 同时承担 fresh bootstrap 和已有环境 patch 的执行边界问题。
- 不拆分脚本，先增加显式执行画像 guard，降低误用脚本造成半初始化或误回放的风险。
- 用合同测试固定 `FRESH_BOOTSTRAP` / `PATCH_EXISTING` 的存在和调用顺序。

## 子 Agent 使用记录

- 本轮先按用户偏好尝试 6 个 `gpt-5.3-codex-spark` 只读子 Agent，但工具侧提示该模型已达到用量限制，需要等到 2026-06-13 01:59 后再试。
- 随后按用户 fallback 规则尝试 6 个 `gpt-5.4` 子 Agent；其中部分也遇到额度限制。
- 已收到的有效只读结论：`seller_buyer_management_seed.sql` 的 fresh bootstrap 主要覆盖端内表、字典、管理端菜单和缺失插入配置；PATCH_EXISTING 主要覆盖已有 seller/buyer 数据的 Owner 角色、端内菜单和账号角色绑定回填。

## 新增问题

- `seller_buyer_management_seed.sql` 原先只要求确认 token，没有要求操作者声明当前是在全新环境初始化，还是在已有环境做补授权/收敛。
- 该脚本包含 `CREATE TABLE IF NOT EXISTS`、字典/菜单初始化、portal URL 缺失插入，以及基于现有 seller/buyer/account 数据的 Owner 角色和权限绑定回填；没有 profile guard 时，执行语义不清。

## 已修复问题

- `seller_buyer_management_seed.sql` 新增执行画像说明：
  - `FRESH_BOOTSTRAP`：用于 RuoYi 初始化和 `top_menu_seed.sql` 之后的新数据库。
  - `PATCH_EXISTING`：用于已有 seller/buyer 环境的收敛。
- 新增 `@seller_buyer_management_seed_profile` 变量，必须显式设置为 `FRESH_BOOTSTRAP` 或 `PATCH_EXISTING`。
- 新增 `assert_seller_buyer_management_seed_profile()`：
  - 要求当前会话已选中目标 database。
  - 要求 `sys_menu` 表存在。
  - 要求 `top_menu_seed.sql` 已提供 `2010` 主体管理根菜单。
  - `FRESH_BOOTSTRAP` 下，如果 seller/buyer 端内核心表已存在，则 fail-closed。
  - `PATCH_EXISTING` 下，如果 seller/buyer 端内核心表都不存在，则 fail-closed。
- profile guard 在确认 token 之后、首条 `CREATE TABLE` 之前执行。
- `TerminalSeedPermissionContractTest` 增加 profile seed 存在性检查。
- `SqlExecutionGuardContractTest` 增加 profile guard 顺序合同，固定它必须早于建表、portal URL 配置缺失插入和 Owner 角色回填。
- `docs/architecture/reuse-ledger.md` 增补综合 seed 执行画像复用规则。

## 残留问题

- 本轮选择“不拆文件，先加 profile/freshness guard”；如果后续要更彻底治理，可继续把 `seller_buyer_management_seed.sql` 拆成 fresh baseline 和 patch helper。
- 历史残留已收口：`integration` fresh bootstrap schema 缺口已由 `docs/plans/2026-06-07-three-terminal-p0p1-integration-bootstrap-chain-record.md` 固定为 bootstrap 后必跑 SQL 清单。
- 本轮未执行远程数据库 SQL，因此没有验证真实库 profile 执行结果。

## 权限检查结果

- 本轮不新增端内或管理端权限点，只固定综合 seed 的执行边界。
- 既有管理端菜单、端内 OWNER 权限回填逻辑保持不变。

## 字典/选项复用检查结果

- 本轮不新增字典项、不修改字典 code/label。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记综合 seed 同时承担 fresh DDL 和 patch 回填时必须要求显式执行画像。

## 大文件合理性判断结果

- `seller_buyer_management_seed.sql` 已是综合 seed 大文件，本轮不拆分，原因是当前目标是 P1 执行边界收口，拆分会扩大影响面。
- `SqlExecutionGuardContractTest.java` 已是集中式 SQL 合同测试文件，本轮新增一个针对 seed profile guard 的合同方法，保持在既有职责内。

## 重复代码检查结果

- 没有新增 Java 业务逻辑或前端重复代码。
- SQL guard 是脚本内局部 procedure，不引入跨脚本运行依赖。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,TerminalSeedPermissionContractTest,TerminalSqlIsolationContractTest" test

cd E:\Urili-Ruoyi
git diff --check
codegraph sync .
```

验证结果：
- `mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,TerminalSeedPermissionContractTest,TerminalSqlIsolationContractTest" test`：通过；`SqlExecutionGuardContractTest` 33 个测试通过，`TerminalSeedPermissionContractTest` 1 个测试通过，`TerminalSqlIsolationContractTest` 11 个测试通过。
- `git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `codegraph sync .`：通过；同步 2 个变更文件，92 个节点。

## 未验证原因

- 本轮按用户要求不做浏览器运行态验收、截图、DOM 检测或 UI 细调。
- 本轮不执行远程 MySQL DDL/DML，不读取或写入 Redis。
- 本轮未启动或重启后端。

## CodeGraph 更新结果

- `codegraph sync .` 已通过；同步 2 个变更文件，92 个节点。
