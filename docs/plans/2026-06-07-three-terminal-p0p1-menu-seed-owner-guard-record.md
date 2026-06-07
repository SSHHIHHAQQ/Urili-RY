# 2026-06-07 三端 P0/P1 菜单 Seed Owner Guard 记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按快速推进模式只修 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本轮聚焦管理端 `sys_menu` seed 的回放覆盖风险；不做浏览器运行态验收、不做截图/DOM 检测、不做 UI 细调。

## 新增问题

- P0：`business_menu_seed.sql` 持有 `2440/2441` 的旧占位签名，回放会把 `20260604_product_category_attribute_seed.sql` 已升级的商品分类/属性正式页静默打回占位页和 `basic:*` 权限。
- P0：`business_menu_seed.sql` 持有 `2421`，并且历史上还有尾部 update 会把来源仓库库存真实页打回 `Common/PlannedPage/index`；这与 `20260606_source_warehouse_stock_menu_rename.sql` 的正式页签名冲突。
- P1：`top_menu_seed.sql` 复用若依原生 `menu_id = 108/3`，但原脚本只有执行确认口令，没有 slot/signature guard；已有定制环境中存在静默重写风险。
- P1：旧合同测试还在要求 `business_menu_seed.sql` 保持 `2421` 占位页，方向与当前“专用 seed 单一 owner”冲突。
- P1 追加：`2402` 商城商品列表同时由 `business_menu_seed.sql` 和 `20260605_mall_product_distribution_seed.sql` 持有，虽然当前签名一致，但 owner 不清会造成回放顺序依赖。
- P1 追加：`2412` 售后管理同时由 `business_menu_seed.sql` 和 `20260605_order_after_sale_menu_seed.sql` 持有，虽然当前签名一致，但专用 seed 原先没有 slot/signature guard。
- P1 追加：`2010` 主体管理顶级目录同时出现在 top seed、seller/buyer 全量 seed 和 direct-login 增量 seed 中；当前签名一致，需明确 top 主 owner 与兼容 seed 边界。
- P1 追加：`2485/2486` 商城商品调价和操作日志按钮同时由 `20260605_mall_product_distribution_seed.sql` 与 `20260605_product_distribution_status_price_log.sql` 持有，属于 `2402` 菜单树下的按钮 owner 重复。
- P1 追加：`20260605_source_product_library_menu_component.sql` 使用固定 `menu_id = 2400` 更新来源商品库组件、路由名和权限标识，但原脚本只有确认 token，没有 slot/signature guard；历史库或菜单漂移库补跑时可能静默改错菜单。
- P1 追加：`upstream_system_management_seed.sql` 直接 upsert `2031` 和 `2300-2309` 上游系统管理菜单/按钮权限，但缺少 slot/signature guard；历史库中固定 ID 或权限签名被占用时会静默覆盖管理端真实权限面。

## 已修复问题

- `business_menu_seed.sql`：
  - 增加 `tmp_business_menu_sys_menu_guard` 和 `assert_business_menu_sys_menu_guard()`，在 upsert 前检查自身仍持有的二级菜单签名。
  - 移除 `2440` 商品分类配置和 `2441` 商品属性配置占位菜单，固定由 `20260604_product_category_attribute_seed.sql` 接管。
  - 移除 `2421` 来源仓库库存菜单，固定由 `20260606_source_warehouse_stock_menu_rename.sql` 接管。
  - 删除旧的 `2421 -> Common/PlannedPage/index` 回退 update。
- `20260606_source_warehouse_stock_menu_rename.sql`：
  - 增加 `tmp_source_warehouse_stock_sys_menu_guard` 和 `assert_source_warehouse_stock_sys_menu_guard()`。
  - 允许历史占位签名 `Common/PlannedPage/index` 升级为 `Inventory/SourceWarehouseStock/index`。
  - 在 `2421` 缺失时插入正式菜单，避免从总 seed 迁出后 fresh bootstrap 无法创建该入口。
- `top_menu_seed.sql`：
  - 增加 `tmp_top_menu_sys_menu_guard` 和 `assert_top_menu_sys_menu_guard()`。
  - 对顶级目录和若依原生 `108/3` 复用先做 slot/signature guard，再执行 upsert 和排序调整。
  - 明确允许 `108` 从若依原生 `log` 签名迁移到 `log-center` / `LogCenter` 签名。
- `SqlExecutionGuardContractTest`：
  - 新增 business 总 seed 不得覆盖专用菜单 owner 的合同。
  - 新增 source warehouse seed 独占并 guard `2421` 的合同。
  - 新增 top seed 必须在 upsert/update 前 guard 顶级菜单 slot 的合同。
  - 更新 inventory 权限隔离合同，不再反向要求 `2421` 保持占位页。
- `AGENTS.md`：
  - 增加管理端 `sys_menu` seed 所有权规则：同一 `menu_id` 的最终签名只能由一个 seed 负责，通用 seed 不得回放覆盖专用业务 seed。
- 追加收口：
  - `business_menu_seed.sql` 移除 `2402` 商城商品列表和 `2412` 售后管理，通用 seed 不再回放覆盖这两个专用菜单。
  - `20260605_mall_product_distribution_seed.sql` 增加 `tmp_mall_product_distribution_sys_menu_guard` 和 `assert_mall_product_distribution_sys_menu_guard()`，并从局部 `update menu_id = 2402` 改为完整 upsert `2402` 页面菜单。
  - `20260605_order_after_sale_menu_seed.sql` 增加 `tmp_order_after_sale_sys_menu_guard` 和 `assert_order_after_sale_sys_menu_guard()`，固定专用 seed 持有 `2412`。
  - `SqlExecutionGuardContractTest` 增加 `2402`、`2412` 专用 owner guard 合同，并把 `business_menu_seed.sql` 对 `2402/2412` 的回灌列为禁止项。
  - `SqlExecutionGuardContractTest` 增加 `2010` 合同：`top_menu_seed.sql` 为主 owner；`seller_buyer_management_seed.sql` 和 `20260606_admin_partner_page_direct_login_seed.sql` 只作为已记录的同签名兼容 seed，必须先 guard 再写入。
  - `AGENTS.md` 补充兼容增量 seed 例外边界：只能重复写入已明确记录的同一最终签名，且必须先做 slot/signature guard。
  - `20260605_product_distribution_status_price_log.sql` 删除 `2485/2486` 按钮插入逻辑，保留状态、价格、操作日志表和字典迁移职责。
  - `20260605_mall_product_distribution_seed.sql` 继续统一持有 `2485/2486` 按钮权限。
  - `SqlExecutionGuardContractTest` 增加反向合同：状态/价格日志脚本不得写 `sys_menu`，不得再声明 `2485/2486` 或 `product:distribution:price/log`。
- 后续收口：
  - `top_menu_seed.sql` 对 `2040/2000` 历史草案 cleanup 已增加独立 `tmp_top_menu_legacy_cleanup_guard` 和 `assert_top_menu_legacy_cleanup_guard()`，不再裸更新退役菜单 ID。
  - `SqlExecutionGuardContractTest` 增加 `topMenuSeedLegacyCleanupMustFailClosedBeforeUpdatingLegacyMenus`，固定 legacy cleanup guard 必须先于 `where menu_id = 2040` 和 `where menu_id = 2000` 更新。
  - 详细记录见 `docs/plans/2026-06-07-three-terminal-p0p1-top-menu-legacy-cleanup-guard-record.md`。
- 后续 `2010` single-owner 收口：
  - `seller_buyer_management_seed.sql` 已删除 `2010` 的 guard seed 行和 `insert into sys_menu` upsert 行，只在写 `2011/2012` 及按钮前断言 `top_menu_seed.sql` 已提供正确的 `2010` 根节点。
  - `20260606_admin_partner_page_direct_login_seed.sql` 已删除 `2010` upsert，不再对 `2010` 做 slot/signature 兼容写入，只通过 `assert_partner_root_menu_exists()` 断言根节点存在。
  - `SqlExecutionGuardContractTest` 和 `StandalonePartnerSeedMenuContractTest` 已改为 `2010` top-only 合同。
  - 详细记录见 `docs/plans/2026-06-07-three-terminal-p0p1-menu-2010-single-owner-record.md`。
- 本轮 2400 收口：
  - `20260605_source_product_library_menu_component.sql` 增加 `tmp_source_product_library_menu_component_guard` 和 `assert_source_product_library_menu_component_guard()`，在更新 `menu_id = 2400` 前确认该 ID 仍是来源商品库历史签名或最终签名。
  - guard 允许旧占位签名 `list` + `Common/PlannedPage/index` + `ProductList` + `product:list:list` 迁移到最终签名 `list` + `Product/SourceProductLibrary/index` + `SourceProductLibrary` + `product:list:list`。
  - 因 2026-06-04 改名记录明确名称和组件迁移分步执行，`2400` guard 不把 `menu_name` / `remark` 放入签名白名单，只锁 `path/component/route_name/perms/menu_type`。
  - `SqlExecutionGuardContractTest` 增加 `sourceProductLibraryMenuComponentMigrationMustGuardMenu2400BeforeUpdate`，固定 guard 必须先于 `update sys_menu`。
- 本轮上游系统管理收口：
  - `upstream_system_management_seed.sql` 增加 `tmp_upstream_system_management_sys_menu_guard` 和 `assert_upstream_system_management_sys_menu_guard()`。
  - guard 覆盖 `2031` 页面菜单和 `2300-2309` 按钮权限，写 `sys_menu` 前先检查固定 ID 和 `(path, component, route_name, perms)` 签名。
  - `SqlExecutionGuardContractTest` 增加 `upstreamSystemManagementMenuSeedMustGuardSysMenuSlotsBeforeUpsert`，固定上游系统管理 seed 不能裸 upsert 管理端权限菜单。

## 残留问题

- 已收口：`top_menu_seed.sql` 对 `1/2` 的排序调整已纳入现役 top menu guard；`2040/2000` 历史草案状态调整已由后续检查点补独立 legacy cleanup guard。本记录中的旧残留表述不再代表当前状态。
- 已收口：`2010` 主体管理已由后续检查点迁为 `top_menu_seed.sql` 唯一写入 owner；`seller_buyer_management_seed.sql` 和 `20260606_admin_partner_page_direct_login_seed.sql` 只断言根节点存在，不再 upsert `2010`。
- P1：端内 `oper_log` direct-login 结构化审计仍需先出 DDL 方案并确认。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`17` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：追加收口后通过，`20` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=StandalonePartnerSeedMenuContractTest,AdminDirectLoginPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`3` 个测试通过。
- `cd E:\Urili-Ruoyi; rg -n "2402|2412|2010|product:distribution:list|order:afterSale:list|PartnerManagement" RuoYi-Vue\sql\business_menu_seed.sql RuoYi-Vue\sql\20260605_mall_product_distribution_seed.sql RuoYi-Vue\sql\20260605_order_after_sale_menu_seed.sql RuoYi-Vue\sql\top_menu_seed.sql RuoYi-Vue\sql\seller_buyer_management_seed.sql RuoYi-Vue\sql\20260606_admin_partner_page_direct_login_seed.sql`：通过，用于核对 owner 命中分布。
- `cd E:\Urili-Ruoyi; rg -n "\(2402, '商城商品列表'|product:distribution:list|\(2412, '售后管理'|order:afterSale:list" RuoYi-Vue\sql\business_menu_seed.sql`：无命中，确认通用 seed 已退出 `2402/2412` 页面 owner。
- `cd E:\Urili-Ruoyi; rg -n -w "2485|2486" RuoYi-Vue\sql --glob "!warehouse_us_address_seed.sql"; rg -n "product:distribution:(price|log)" RuoYi-Vue\sql`：通过，只命中 `20260605_mall_product_distribution_seed.sql`，确认按钮 owner 已收口。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：legacy cleanup guard 后续收口通过，`21` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：2400 来源商品库组件迁移 guard 后续收口通过，`22` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：上游系统管理菜单 seed guard 收口通过，`24` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：最终收口通过，前端 `4` 个 suite / `18` 个测试通过；后端 ruoyi-system `120`、ruoyi-framework `15`、product `1`、seller `84`、buyer `85` 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：最终收口通过，仅输出既有 LF/CRLF 换行提示，无 whitespace 错误。

## 未验证原因

- 未做浏览器运行态验收、截图、DOM 检测：用户已明确当前快速推进模式无需浏览器验证。
- 未连接远程 MySQL / Redis，未执行 SQL，未写远程数据：本轮只修改 SQL 脚本、静态合同测试和规则记录。
- 旧检查点未完全迁出 `2010` 兼容 seed；已由 `docs/plans/2026-06-07-three-terminal-p0p1-menu-2010-single-owner-record.md` 后续收口覆盖。当前 direct-login 增量 seed 依赖 top-owned `2010`，不再文件内自带根节点。

## 权限检查结果

- 本轮只处理管理端若依 `sys_menu` seed 的菜单签名和权限标识归属，不改变 seller/buyer 端内权限模型。
- `2440/2441` 权限归属固定为 `product:category:*` / `product:attribute:*`，不再由总 seed 写 `basic:*`。
- `2421` 权限归属固定为 `inventory:sourceWarehouse:list`，由 source warehouse 专用 seed 持有。
- `2400` 权限归属固定为 `product:list:list`，来源商品库最终页面主 owner 为 `business_menu_seed.sql`；历史组件迁移脚本只允许同一旧签名迁移到同一最终签名。
- `2402` 权限归属固定为 `product:distribution:list`，由商城商品专用 seed 持有。
- `2485/2486` 权限归属固定为 `product:distribution:price` / `product:distribution:log`，由商城商品专用 seed 持有。
- `2412` 权限归属固定为 `order:afterSale:list`，由售后菜单专用 seed 持有。
- `2010` 顶级目录无权限标识，唯一写入 owner 为 `top_menu_seed.sql`；依赖 seed 只能断言该根节点存在且签名正确，不得再补齐或改写。
- `top_menu_seed.sql` 顶级目录不新增按钮权限。
- `2031` 权限归属固定为 `integration:upstream:list`，由 `upstream_system_management_seed.sql` 持有。
- `2300-2309` 上游系统管理按钮权限固定由 `upstream_system_management_seed.sql` 持有，包含 `integration:upstream:query/add/edit/credential/sync/pair/log/dimensionSync/inventoryQuery/inventorySync`。

## 字典/选项复用检查结果

- 本轮未新增字典类型、字典数据或前端选项。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记 `2421/2440/2441` 单 owner、`2400` 来源商品库历史组件迁移 guard、`2402/2412` 专用 seed owner、`2485/2486` 按钮 owner、`2010` top 主 owner + 兼容 seed guard 边界，以及 top seed slot/signature guard 和 source warehouse 旧占位迁移规则。
- 已追加登记 `2031` / `2300-2309` 上游系统管理菜单和按钮 owner guard。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：追加收口后先输出 `Synced 1 changed files`，`Modified: 1 - 49 nodes in 782ms`；记录更新后复跑通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：2400 来源商品库组件迁移 guard 收口后输出 `Synced 1 changed files`，`Modified: 1 - 51 nodes in 810ms`；记录更新后复跑确认。
- `cd E:\Urili-Ruoyi; codegraph sync .`：上游系统管理 seed guard 和 portal 权限读路径收口后最终执行通过，输出 `Synced 5 changed files`，`Modified: 5 - 325 nodes`。

## 大文件合理性判断结果

- `SqlExecutionGuardContractTest.java` 已超过 500 行，但职责仍集中在 SQL 执行和菜单 seed guard 合同；本轮只追加同类合同，不拆分。
- `business_menu_seed.sql` 和 `top_menu_seed.sql` 仍是单一 seed 文件，职责分别为通用二级菜单和顶级菜单；本轮没有拆分。

## 重复代码检查结果

- SQL slot/signature guard 继续使用脚本内临时表 + procedure 的局部 helper 模式，符合现有 MySQL seed 无 include 机制的写法。
- 没有复制 Java 业务逻辑或 React 业务逻辑。

## 子 Agent 使用记录

- 首轮先尝试 4 个 `gpt-5.3-codex-spark` 只读子 Agent；平台返回额度限制，恢复时间为 `2026-06-13 01:59`，失败 Agent 已全部关闭。
- 追加收口时按用户最新要求尝试 6 个 `gpt-5.3-codex-spark` 只读子 Agent；平台同样返回额度限制，失败 Agent 已全部关闭。
- 回退使用并关闭 6 个 `gpt-5.4` 只读子 Agent，复核 `2402/2412/2010` owner、`2040/2000` legacy 边界、合同测试模板和 Markdown 更新位置。
- 按当前目标要求追加启动 6 个 `gpt-5.4` 只读子 Agent，复核 `2485/2486` 按钮 owner、测试改法、状态/价格日志职责、文档更新位置和三端影响；使用完毕后关闭。
- 本轮继续按“GPT-5.3 Codex 优先，不可用则回退 gpt-5.4”执行；因前序已确认 `gpt-5.3-codex-spark` 额度不可用，本轮回退启动并关闭 6 个 `gpt-5.4` 只读子 Agent，复核 `2400` 历史签名、SQL guard、合同测试、Markdown 更新位置、是否保留脚本以及同级固定菜单 ID 风险。
- 本轮上游系统管理收口同样按“GPT-5.3 Codex 优先，不可用则回退 gpt-5.4”执行；当前回退使用并关闭 6 个 `gpt-5.4` 只读子 Agent，采纳 SQL/seed 子 Agent 对 `2031`、`2300-2309` 缺少 slot/signature guard 的 P1 结论。

## 一句话总结

本轮把 `2421/2440/2441` 从通用 `business_menu_seed.sql` 中迁出，补齐 business/top/source-warehouse/source-product-library 等菜单 seed 的 slot/signature guard，并用 `SqlExecutionGuardContractTest` 锁住回放覆盖风险。
