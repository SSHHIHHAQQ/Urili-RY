# 三端隔离 P0/P1 完成审计切片 3（只读）

> 历史记录（已过期口径）：本文记录的是当时 SQL guard 只读扫描发现的候选 P1。后续检查点已收口 terminal menu ID 重排精确签名、`product_distribution_status_price_log` exact target 与 `SqlExecutionGuardContractTest` 泛匹配盲区；当前状态以 `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 较新检查点和现行代码为准，不要把本文 SQL guard P1 作为现存阻塞项。

## 范围

- `RuoYi-Vue/sql` 全部增量 SQL
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
- 目标：只查不改，重点核对高影响 DDL/DML confirm guard、confirm 调用顺序、target count/signature 与实际 DML 范围、dynamic DDL/prepare 绕过、端内菜单 ID/perms/component fail-open

## 结论

- `P0`：未发现
- `P1`：3 个
- `P2`：1 个
- 现状补充：`SqlExecutionGuardContractTest` 当前 63 个用例本地执行通过，但存在契约盲区，不能据此判定本次范围已经完全 fail-closed。

## P1

### 1. `terminal_menu_id_range_isolation` 的签名预览范围小于实际 `parent_id` 更新范围

- 文件：
  - [20260607_terminal_menu_id_range_isolation.sql](/E:/Urili-Ruoyi/RuoYi-Vue/sql/20260607_terminal_menu_id_range_isolation.sql:244)
  - [20260607_terminal_menu_id_range_isolation.sql](/E:/Urili-Ruoyi/RuoYi-Vue/sql/20260607_terminal_menu_id_range_isolation.sql:417)
  - [SqlExecutionGuardContractTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java:1867)
- 问题：
  - 预览签名只覆盖 `seller_menu_id/buyer_menu_id < 100000` 的低位菜单行。
  - 但实际 DML 里的 `update seller_menu set parent_id = parent_id + 100000 where parent_id > 0 and parent_id < 100000` 与 `update buyer_menu set parent_id = parent_id + 200000 where parent_id > 0 and parent_id < 100000` 会命中“自身已经在最终区间、但仍引用低位父菜单”的高位行。
  - 这部分行不在 `@terminal_menu_range_*_expected_signature` 里，属于“guard 预览集合 < 实际 DML 集合”。
- 风险：
  - 在部分迁移、半修复库、手工补数据环境下，脚本可能改到未被 preview-confirm 的高位菜单记录。
  - 当前契约测试只检查“存在 expected count/signature”和语句顺序，没有校验 `parent_id` 更新的目标集合是否被单独 preview。
- 建议：
  - 给 `seller_menu.parent_id`、`buyer_menu.parent_id` 单独做 expected count/signature；
  - 或把菜单预览集合改成“所有将被任一 update 命中的行”，而不是仅按主键低位过滤；
  - 同步补一个契约测试，明确拒绝“只 preview 低位 menu_id、却更新任意低位 parent_id 引用”的脚本形态。

### 2. `product_distribution_status_price_log` 有全表业务状态迁移，但没有 exact target count/signature

- 文件：
  - [20260605_product_distribution_status_price_log.sql](/E:/Urili-Ruoyi/RuoYi-Vue/sql/20260605_product_distribution_status_price_log.sql:6)
  - [20260605_product_distribution_status_price_log.sql](/E:/Urili-Ruoyi/RuoYi-Vue/sql/20260605_product_distribution_status_price_log.sql:116)
  - [SqlExecutionGuardContractTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java:3417)
- 问题：
  - 该脚本只有 `@confirm_product_distribution_status_price_log`，没有 `expected_count / expected_signature`。
  - 但它会批量改写：
    - `product_spu` 中全部 `spu_status = 'DISABLED'` 的行
    - `product_sku` 中全部 `sku_status = 'DISABLED'` 的行
  - 这是典型高影响业务 DML，不是单行 seed，也不是纯结构性 DDL。
- 风险：
  - 远端库如果存在历史脏值、枚举漂移、演示数据混入，执行前无法证明命中的就是预览过的那一批行。
  - 当前契约测试只校验 replay-safe helper 和 confirm 调用顺序，没有对这两段 update 加 preview-confirm 合同。
- 建议：
  - 为 `product_spu`、`product_sku` 两段迁移各自增加 exact target count/signature；
  - signature 至少覆盖主键 + 原状态 + 关键目标字段；
  - 测试层补“业务表批量状态迁移必须带 preview-confirm”规则，避免这类脚本继续只靠 confirm token。

### 3. `SqlExecutionGuardContractTest` 没有校验“调用的 confirmed procedure 是否与当前脚本匹配”

- 文件：
  - [SqlExecutionGuardContractTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java:34)
  - [SqlExecutionGuardContractTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java:3645)
  - [SqlExecutionGuardContractTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java:3896)
  - [SqlExecutionGuardContractTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java:3920)
- 问题：
  - `CONFIRM_CALL` 只匹配任意 `call assert_*_confirmed();`。
  - `assertAutoDiscoveredGuard(...)` 与 `assertGuard(...)` 只要求：
    - 文件里有 `set @confirm_*`
    - 文件里有 token 字面量
    - 文件里有任意一个 `call assert_*_confirmed();`
  - 没有证明“被调用的 confirmed procedure”就是当前脚本对应的那个 procedure，也没有证明这个 procedure 内部校验的就是当前脚本的 confirm 变量和 token。
- 风险：
  - 复制脚本时即使误调用了别的 `assert_xxx_confirmed()`，当前测试也可能继续通过。
  - 这会把“guard 存在”降级成“guard 字样存在”，属于高影响 SQL 合同的假阳性。
- 建议：
  - `assertGuard(...)` 应显式校验具体 procedure 名称，而不是泛匹配；
  - `assertConfirmationCallBeforeDml(...)` 也应接收并校验目标 procedure 名；
  - auto-discovered 分支至少要求同文件内 `@confirm_*`、token 常量、`create procedure assert_*_confirmed`、`call assert_*_confirmed();` 四者能解析成同一 guard。

## P2

### 4. auto-discovery 对 `prepare` 的识别偏向 `@ddl`，对动态 DML 基本依赖专项测试兜底

- 文件：
  - [SqlExecutionGuardContractTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java:23)
  - [SqlExecutionGuardContractTest.java](/E:/Urili-Ruoyi/RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java:3627)
  - [20260604_three_terminal_legacy_sys_user_account_backfill.sql](/E:/Urili-Ruoyi/RuoYi-Vue/sql/20260604_three_terminal_legacy_sys_user_account_backfill.sql:265)
- 问题：
  - `DYNAMIC_HIGH_IMPACT_SQL_HINT` 只盯 `set @ddl = concat('alter/create/drop...')` 这一类动态 DDL。
  - 但仓库里已经存在 `set @dml = 'update ...'; prepare stmt from @dml; execute stmt;` 这种动态 DML 形态。
  - 当前 legacy backfill 之所以没漏，是因为有专项测试，不是 auto-discovery 本身识别到了它。
- 风险：
  - 后续如果新增一个日期前缀 SQL，使用 `@dml` / `@sql` 动态拼装 `update/delete/insert`，而又没有补专项测试，generic auto-discovery 可能漏报。
- 建议：
  - 把 dynamic 检测从“只看 `@ddl` + DDL 关键词”扩成“任意 prepared SQL 变量 + DDL/DML 关键词”；
  - 或新增一条规则：凡是 `prepare stmt from @...` 的日期前缀 SQL，都必须有专项测试明确绑定目标集合和 guard。

## 已核对但未报问题

- 当前 seller/buyer 端内菜单 seed 已普遍带有：
  - ID 区间 guard
  - `perms` 前缀 guard
  - 页面菜单 `component` 非空 guard
- 本次范围内未发现新的 seller/buyer `menu_id` 范围 fail-open、`perms` 通配/跨端前缀 fail-open、页面菜单空 `component` fail-open 证据。

## 验证

- 已执行：`mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test`
- 结果：`BUILD SUCCESS`，`Tests run: 63, Failures: 0, Errors: 0`
- 说明：测试全绿不代表上述盲区不存在；其中第 1、3、4 项本质上就是“测试当前能通过，但合同仍可被绕开/覆盖不足”。
