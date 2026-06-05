# 端内权限综合 Seed 对齐执行记录

## 背景

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理 SQL 初始化一致性问题。

前一轮只读审计发现：卖家端、买家端的端内账号、部门、角色、商品分类、商品 schema 权限已经分别存在于增量 SQL 中，但综合初始化脚本 `RuoYi-Vue/sql/seller_buyer_management_seed.sql` 尚未包含这些端内权限和默认角色绑定。该状态容易导致新环境只执行综合 seed 后，端内门户基础权限缺失。

## 本轮范围

- 修改 `RuoYi-Vue/sql/seller_buyer_management_seed.sql`。
- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/TerminalSeedPermissionContractTest.java`。
- 更新目标追踪和复用台账。
- 不执行远端 MySQL DDL / DML。
- 不修改管理端 UI。
- 不修改 seller/buyer 接口业务逻辑。

## 已完成

- 在综合 seed 中补入 active 卖家、买家的默认 `Owner` 端内角色初始化。
- 在综合 seed 中补入卖家端端内权限：
  - `seller:account:list`
  - `seller:dept:list`
  - `seller:role:list`
  - `seller:product:category:list`
  - `seller:product:schema:query`
- 在综合 seed 中补入买家端端内权限：
  - `buyer:account:list`
  - `buyer:dept:list`
  - `buyer:role:list`
  - `buyer:product:category:list`
  - `buyer:product:schema:query`
- 在综合 seed 中补入 `OWNER` 账号到默认 `owner` 角色的绑定。
- 在综合 seed 中补入 active 端内角色到上述端内权限菜单的绑定。
- 新增契约测试，防止后续综合 seed 漏掉端内门户最小权限和绑定结构。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=TerminalSeedPermissionContractTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 34, Failures: 0, Errors: 0, Skipped: 0`。
- `git diff --check -- RuoYi-Vue\sql\seller_buyer_management_seed.sql docs\plans\2026-06-04-three-terminal-isolation-goal-tracker.md docs\architecture\reuse-ledger.md react-ui\src\pages\Product\categoryTree.ts`：通过；Git 仅提示 LF/CRLF 转换警告。
- 新增未跟踪文件空白检查：无尾随空白输出。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，结果为 `Already up to date`。
- UTF-8 读取 `seller_buyer_management_seed.sql`：新增中文备注显示正常；PowerShell 默认编码下出现的乱码为控制台显示问题。

## 当前判断

- 综合 seed 现在覆盖端内门户当前最小权限和默认角色绑定。
- 历史增量 SQL 仍保留，用于已运行环境按批次补丁执行；综合 seed 作为新环境初始化入口，不再遗漏这些端内权限。
- 本轮没有触碰远端数据库，运行环境是否已经应用这些权限仍需在需要执行 SQL 时按 AGENTS 数据源确认规则单独记录和确认。
- `seller_buyer_management_seed.sql` 已超过 500 行，但职责仍是卖家/买家主体和端内基础控制面综合初始化；本轮只在同一初始化边界内补权限和绑定，不单独拆分。

## 后续建议

- 后续新增 seller/buyer 端真实业务权限时，先按卖家端模板落地并验收，再复制买家端；新增权限必须同步更新综合 seed、必要的增量 SQL、复用台账和契约测试。
- 若准备对远端库执行综合 seed 或增量 SQL，应先生成单独 SQL 执行记录，写明数据源、影响对象、回滚方式和验证命令。
