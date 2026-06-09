# 2026-06-08 SQL seeds/DDL/guard 只读 P0/P1 扫描

## 结论

本轮按指定切片对以下对象做了只读 P0/P1 扫描：

- `RuoYi-Vue/sql/20260608*`
- `RuoYi-Vue/sql/business_menu_seed.sql`
- `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
- `RuoYi-Vue/sql/top_menu_seed.sql`
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`

结论：**本轮未发现明确的 P0/P1 问题**。已核对的重点项包括：

- 高影响 SQL 的确认 token、`45000` fail-closed、确认调用顺序
- `20xxxxxx*.sql` 自动发现合同
- `URILI_BOOTSTRAP_ONLY_SQL` bootstrap-only 边界
- dynamic DDL / `set @ddl` 合同
- `2010` 顶级菜单 owner 边界
- `seller_menu` / `buyer_menu` ID 区间、`role_menu` 关联范围与跨端污染防护
- `portal.seller.web.url` / `portal.buyer.web.url` 占位地址只在缺失时插入，不覆盖既有值

补充验证：

- 已执行 `mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test`
- 结果：`Tests run: 70, Failures: 0, Errors: 0, Skipped: 0`

## P0/P1 Findings

无。

## 关键证据

1. `SqlExecutionGuardContractTest` 已把本次目标文件全部纳入显式 guard 合同：
   - `20260608_inventory_overview_sku_baseline_refresh.sql`：`156-158`
   - `20260608_terminal_menu_auto_increment_reset.sql`：`159-161`
   - `20260608_product_review.sql`：`162-164`
   - `20260608_product_center_menu_seed.sql`：`165-167`
   - `20260608_overseas_channel_carrier_menu_restructure.sql`：`168-170`
   - `seller_buyer_management_seed.sql`：`171-173`
   - `top_menu_seed.sql`：`177-179`
   - `business_menu_seed.sql`：`180-182`

2. 日期前缀 SQL 与全量增量 SQL 都有自动发现合同，且 bootstrap-only 脚本被显式排除在普通增量 guard 之外：
   - 日期前缀自动发现：`201-226`
   - 全量增量自动发现：`228-258`
   - bootstrap-only 跳过条件：`241-245`
   - bootstrap-only 断言：`4186-4201`

3. `top_menu_seed.sql` 仍保持 `2010` 只由顶级 seed 写入，依赖 seed 只断言不重写：
   - `top_menu_seed.sql` 写入 `2010`：`130-144`, `169-224`
   - `seller_buyer_management_seed.sql` 只断言 `2010` 已存在且签名正确：`43-55`, `94-102`
   - 配套契约测试：`2678-2743`

4. `seller_buyer_management_seed.sql` 已对 terminal 菜单 ID 区间、非法 perms、空 component、唯一索引、`role_menu` 越界/孤儿引用做 fail-closed：
   - seller guard：`191-338`
   - seller/buyer 端菜单自增起点：`734-785`
   - `portal.*.web.url` 仅在缺失时插入：`1435-1445`
   - 契约测试：`892-949`, `1675-1683`, `2040-2067`

5. `20260608_terminal_menu_auto_increment_reset.sql` 保持为独立确认步骤，并对 seller/buyer 菜单与 role-menu 范围、目标快照、动态 `alter table ... auto_increment` 做前后置断言：
   - SQL：`7-38`, `104-170`, `172-320`
   - 契约测试：`2185-2253`

6. `20260608_overseas_channel_carrier_menu_restructure.sql` 已对删除目标做 expected count/signature 双确认，并在删除前后做 legacy root/child/role-menu 完整性校验：
   - SQL：`11-58`, `140-224`, `367-395`
   - 契约测试：`2827-2891`

7. `20260608_product_review.sql` 已对父菜单签名、按钮 perms、字典 seed、表结构存在性与收尾一致性做 fail-closed：
   - SQL：`19-179`, `185-249`
   - 契约测试：`2895-2963`

## 最小修复建议

无必须修复项。

如需继续降低未来回归风险，建议保持当前做法：

- 所有新增 `20xxxxxx*.sql` 持续依赖 `SqlExecutionGuardContractTest` 自动发现，不要只加手工白名单。
- 任何新增 dynamic DDL helper 都保持“确认 token -> 目标快照断言 -> DDL/DML -> 完成态断言”的顺序。
- 任何涉及 `2010`、`seller_menu` / `buyer_menu`、`portal.*.web.url` 的兼容 seed，都继续只做 slot/signature assert，不扩散 owner。
