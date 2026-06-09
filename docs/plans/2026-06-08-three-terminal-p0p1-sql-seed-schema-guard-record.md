# 2026-06-08 三端快速推进 P0/P1 SQL Seed 与 Schema Guard 收口记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续执行三端独立快速推进模式。本轮只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态、截图、DOM 或 UI 细调。

## 子 Agent

- 按用户最新要求，本轮直接使用 6 个 `gpt-5.4` 子 Agent，不再把 GPT-5.3 Codex 作为首选。
- 6 个子 Agent 均已关闭。
- 采纳的 P1：
  - `top_menu_seed.sql`、`business_menu_seed.sql`、`20260606_admin_partner_page_direct_login_seed.sql` 缺少事务和最终完成断言。
  - `20260608_product_review.sql` 的 schema guard 只校验部分列名，不足以 fail-closed 拦截同名表结构不兼容。
  - 历史 Markdown 中 GPT-5.3 优先的旧口径容易被误读为现行规则。

## 已完成

- `RuoYi-Vue/sql/top_menu_seed.sql`
  - 新增 `tmp_top_menu_seed_expected`。
  - 新增 `assert_top_menu_seed_completed()`。
  - 将菜单 upsert、若依原生菜单排序和旧菜单禁用清理放入事务。
- `RuoYi-Vue/sql/business_menu_seed.sql`
  - 新增 `tmp_business_menu_seed_expected`。
  - 新增 `assert_business_menu_seed_completed()`。
  - 将业务二级菜单 upsert 放入事务。
- `RuoYi-Vue/sql/20260606_admin_partner_page_direct_login_seed.sql`
  - 新增 `assert_admin_partner_page_direct_login_seed_completed()`。
  - 将卖家/买家管理页与免密按钮 upsert 放入事务。
- `RuoYi-Vue/sql/20260608_product_review.sql`
  - 新增 `tmp_product_review_column_contract`，覆盖 4 张商品审核表的列顺序、类型、nullable、default 和 extra。
  - 新增 `tmp_product_review_index_contract`，覆盖主键、`uk_product_review_no` 和关键二级索引。
  - `assert_product_review_schema_ready()` 改为按 `information_schema.columns` 与 `information_schema.statistics` 做精确合同校验。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - 补菜单 seed 事务/完成断言顺序合同。
  - 补商品审核 schema/index 合同关键字符串与顺序合同。
- `AGENTS.md` 与目标追踪顶部维持当前口径：子 Agent 默认 `gpt-5.4`。
- 历史 Markdown 中 GPT-5.3 优先的旧口径已标记为“历史记录（已过期口径）”。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,StandalonePartnerSeedMenuContractTest,AdminDirectLoginPermissionContractTest" test`
  - 通过，75 个测试，0 失败，0 跳过。

## 未执行

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器、截图、DOM 或 UI 细调验收。

## 残留

- P2：历史 Markdown 中仍会出现 GPT-5.3 相关执行事实，但已标记为过期历史口径，不再作为现行规则。
- P2：商品审核 schema guard 是文本合同与 SQL 结构合同，未在 live MySQL 上实际回放。
