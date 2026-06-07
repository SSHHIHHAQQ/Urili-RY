# 2026-06-07 三端 P0/P1 菜单、Portal 权限与远程菜单缓存 Guard 记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按快速推进模式只修 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本轮不做浏览器运行态验收、不做截图/DOM 检测、不做 UI 细调。

## 新增问题

- P1：`business_menu_seed.sql` 和 `currency_configuration_seed.sql` 同时声明 `sys_menu.menu_id = 2442`，但一个是 `basic:currency:list` 基础配置占位，一个是 `finance:currency:list` 财务币种正式页，存在重放顺序变化时静默覆盖菜单签名的风险。
- P1：`20260604_product_category_attribute_seed.sql` 已补的菜单 guard 只允许最终正式页签名，会误拦截从旧占位菜单升级到正式页的正常迁移路径。
- P1：Portal 端内运行时权限匹配仍承认 `*:*:*`，会让历史脏权限或手工 SQL 中的通配符绕过 seller/buyer 端内 fail-closed。
- P1：React 远程菜单缓存使用全局 `admin_remote_menu` key，后续三端菜单域拆分时存在缓存串端风险，且 partner-management guard 未覆盖该约束。

## 已修复问题

- `business_menu_seed.sql` 移除 `2442/basic:currency:list`，币种配置由财务币种脚本单一持有。
- `currency_configuration_seed.sql` 增加 `tmp_currency_configuration_sys_menu_guard` 和 `assert_currency_configuration_sys_menu_guard()`，允许旧基础配置占位迁移到财务正式页，但其他 `menu_id/path/component/route_name/perms` 冲突会 fail-closed。
- `20260604_product_category_attribute_seed.sql` 的菜单 guard 改为允许 `2440/2441` 旧占位签名或最终正式页签名，其他签名占用会失败。
- `SqlExecutionGuardContractTest` 增加币种菜单单一 owner 与产品分类/属性旧占位迁移 guard 合同。
- `PortalPermissionSupport` 的 portal 专用权限匹配改为精确匹配，不再使用若依后台 `Constants.ALL_PERMISSION` 放行逻辑；后台 `PermissionService` 未改。
- `PortalPermissionSupportTest` 改为断言 `*:*:*` 不能通过 portal seller/buyer 权限检查。
- 新增 `react-ui/src/utils/remoteMenuStorage.ts` / `.js`，统一生成 `admin_remote_menu:admin|seller|buyer` scoped key。
- `session.ts` 的远程菜单读写改为 scoped key，`access.ts` / `.js` 的登出清理改为清理三端 scoped key。
- `remote-menu-route-guard.test.ts` 增加远程菜单 scoped key 和缓存写入/清理断言。
- `check-partner-management-template.mjs` 增加远程菜单缓存 key guard，防止回退到全局 `admin_remote_menu` 常量。

## 残留问题

- P1：旧商品/来源商品 SQL 仍存在裸 `ALTER TABLE ... ADD COLUMN` 可重放性问题，典型文件包括：
  - `RuoYi-Vue/sql/20260604_source_product_library_sku_candidate_fields.sql`
  - `RuoYi-Vue/sql/20260604_currency_showapi_sync_migration.sql`
  - `RuoYi-Vue/sql/20260605_mall_product_sku_dimension_fields.sql`
  - `RuoYi-Vue/sql/20260605_mall_product_editor_ui_sample_data.sql`
  - `RuoYi-Vue/sql/20260605_product_distribution_status_price_log.sql`
- P0 风险待单独治理：`RuoYi-Vue/sql/ry_20260417.sql` 和 `RuoYi-Vue/sql/quartz.sql` 是 bootstrap 初始化脚本，误执行会删表。由于 AGENTS 当前将它们作为初始化基线记录，本轮未移动、未改名、未执行，后续应单独确认 bootstrap-only 隔离策略。
- P1：`top_menu_seed.sql`、`business_menu_seed.sql` 以及部分旧 `sys_menu` 增量 seed 仍缺完整 slot/signature guard。本轮只收口最确定的 `2442` 冲突和产品分类/属性升级路径。
- P1：端内 `oper_log` 的 direct-login 审计仍未完全结构化，当前主要依赖操作日志参数前缀，后续需要单独补审计字段或明确不扩展。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`11` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=PortalPermissionSupportTest,PortalPermissionCheckerTest" test`：通过，`9` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm exec jest -- --config jest.config.ts --runTestsByPath tests/remote-menu-route-guard.test.ts tests/terminal-session-token.test.ts --runInBand`：通过，`2` 个 suite、`9` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，前端 `4` 个 suite、`14` 个测试通过；后端 ruoyi-system `105`、ruoyi-framework `15`、product `1`、seller `83`、buyer `84` 测试通过。

## 未验证原因

- 未做浏览器运行态验收、截图、DOM 检测：用户已明确当前快速推进模式无需浏览器验证。
- 未连接远程 MySQL / Redis，未执行 SQL，未写远程数据：本轮只修改 SQL 脚本和静态/单元测试，不进行远程库变更。
- 未修复全部裸 DDL：涉及多个旧商品/来源商品迁移脚本，适合后续作为单独 SQL 重放治理切片处理。

## 权限检查结果

- 管理端若依 `sys_*` 权限逻辑未改，`Constants.ALL_PERMISSION` 仍只保留在后台权限体系。
- seller/buyer portal 端内权限运行时不再承认 `*:*:*`，需要精确权限命中。
- 静态 `/seller` / `/buyer` 和远程菜单 route guard 继续按非空 authority fail-closed。
- 币种菜单权限归属固定为 `finance:currency:*`，业务基础配置 seed 不再持有 `basic:currency:list`。

## 字典/选项复用检查结果

- 本轮未新增字典、选项或业务字段。
- 币种菜单归属继续复用既有 `finance:currency:*` 权限和财务币种页面。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记菜单 slot/signature guard、portal 精确权限匹配和远程菜单 scoped cache key 规则。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 10 changed files`，新增 `2`、修改 `8`，`201` nodes。

## 大文件合理性判断结果

- `SqlExecutionGuardContractTest.java` 已超过 500 行，但职责仍集中在 SQL 执行 guard 合同；本轮只追加同类合同，不拆分。
- `check-partner-management-template.mjs` 已超过 500 行，但它是现有 partner 管理模板集中 guard；本轮只增加同类缓存键断言，不拆分。

## 重复代码检查结果

- 未复制业务逻辑。
- 远程菜单缓存 key 抽到 `react-ui/src/utils/remoteMenuStorage.ts` / `.js`，避免 `session.ts` 和 `access.ts` 分别散写 key。
- SQL 菜单 guard 仍按脚本内局部 procedure 落地，后续可考虑抽统一模板，但当前 MySQL 脚本没有公共 include 机制。

## 子 Agent 使用记录

- 本轮收口并关闭上一批 6 个只读子 Agent。
- 该批子 Agent 是上一轮已启动的 `gpt-5.4`；用户最新要求“GPT-5.3 Codex 优先，不可用再用 gpt-5.4”将在后续新开子 Agent 时执行。
