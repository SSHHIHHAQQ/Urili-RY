# 2026-06-07 三端 P0/P1 SQL DDL 可重放 Guard 记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按快速推进模式只修 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本轮聚焦旧商品、来源商品、币种相关增量 SQL 的 DDL 可重放风险；不做浏览器运行态验收、不做截图/DOM 检测、不做 UI 细调。

## 新增问题

- P1：`20260604_source_product_library_sku_candidate_fields.sql` 使用裸多列 `ALTER TABLE upstream_system_sku_candidate ADD COLUMN`，历史库补跑、执行中断后重放或部分列已存在时会直接失败，后续索引和来源商品读模型依赖列无法补齐。
- P0：`20260605_product_distribution_status_price_log.sql` 对 `product_spu`、`product_sku` 使用裸多列 `ADD COLUMN`，同一脚本重放或当前 seed 已吸收字段后会出现重复列失败。
- P1：`20260605_product_distribution_status_price_log.sql` 对 `product_sku.sale_price` 使用无条件 `MODIFY COLUMN`，重放时会强制覆盖后续 schema 漂移，并可能带来不必要的表结构变更成本。
- P0：`20260604_currency_showapi_sync_migration.sql` 的 `rate_anchor_time`、`20260605_mall_product_sku_dimension_fields.sql` 的 `length_value/width_value/height_value`、`20260605_mall_product_editor_ui_sample_data.sql` 的 `product_name_en/detail_content` 已被当前 seed 基线吸收，裸补列会让 fresh bootstrap 后再跑增量脚本直接失败。
- P1：`20260604_currency_showapi_sync_migration.sql` 在 `GENERIC_RATES` 与 `SHOWAPI_BANK_RATE` 并存的半迁移状态下，直接 `UPDATE provider_code` 会撞唯一键，错误信息不可控。
- P1 追加：`20260606_upstream_sync_staging_diff.sql` 仍使用 6 段手写 `@column_exists` + 动态 `ALTER TABLE ... ADD COLUMN` 补列；虽然具备幂等判断，但没有统一 helper 和锚点列 fail-closed，前置 seed 或前置迁移缺失时错误定位不清。

## 已修复问题

- `20260604_source_product_library_sku_candidate_fields.sql`：
  - 增加 `add_column_if_missing(...)`，把整段裸多列 `ALTER TABLE ... ADD COLUMN` 拆为逐列幂等补齐。
  - 增加 `assert_column_exists(...)`，在使用 `after master_product_name` 前先确认锚点列存在，避免模糊 DDL 失败。
  - 保留原有确认 token 和 `create_index_if_missing(...)` 索引 helper。
- `20260605_product_distribution_status_price_log.sql`：
  - 增加 `add_column_if_missing(...)`，分别补齐 `product_spu` / `product_sku` 管控字段。
  - 增加 `modify_product_sku_sale_price_if_needed()`，只有 `sale_price` 当前定义不满足 `decimal(18,4) null comment '销售价'` 时才执行修改；缺列时先 fail-closed。
  - 原有历史 `DISABLED` 状态迁移、索引 helper、字典和操作日志表逻辑保持不变。
- `20260604_currency_showapi_sync_migration.sql`：
  - 增加 `add_column_if_missing(...)`，幂等补齐 `rate_anchor_time`。
  - 增加 `assert_showapi_provider_conflict_absent()`，在 `GENERIC_RATES` 与 `SHOWAPI_BANK_RATE` 并存时主动 fail-closed，避免唯一键原生异常。
- `20260605_mall_product_sku_dimension_fields.sql`：
  - 增加 `add_column_if_missing(...)`，幂等补齐 SKU 长宽高展示字段。
- `20260605_mall_product_editor_ui_sample_data.sql`：
  - 增加 `add_column_if_missing(...)`，幂等补齐 SPU 英文标题和详情字段。
- `20260606_upstream_sync_staging_diff.sql`：
  - 将仓库、物流渠道、SKU 的 6 段手写动态补列收敛为 `add_column_if_missing(...)` helper 调用。
  - 增加 `assert_column_exists(...)`，在补仓库/物流渠道 `source_payload_json` 前断言 `status` 锚点列存在，在补 SKU `wms_payload_json` 前断言 `source_payload_hash` 锚点列存在。
  - 保留后续 `create table if not exists` staging/state/batch 表和 `sys_job` DML 顺序，不在本切片改动调度语义。
- `SqlExecutionGuardContractTest` 新增 3 组合同：
  - 来源商品候选字段脚本必须使用 replay-safe column helper 和锚点 guard。
  - 商品销售状态/价格日志脚本必须使用 replay-safe column helper 和条件 modify helper。
  - 已被 seed 吸收的币种、商城短脚本必须使用 replay-safe column helper，并对 ShowAPI provider 冲突 fail-closed。
- `SqlExecutionGuardContractTest` 追加合同：
  - `upstreamSyncStagingDiffMustUseReplaySafeColumnHelper` 固定 `20260606_upstream_sync_staging_diff.sql` 必须使用 `add_column_if_missing(...)` 和锚点列断言，禁止回退到表名级裸 `ALTER TABLE ... ADD COLUMN` 字符串，并要求补列先于 `upstream_system_sync_state` 建表。

## 残留问题

- P0：`business_menu_seed.sql` 回放会把 `2440/2441` 从商品分类/属性正式页静默打回占位页，也会把 `2421` 从来源仓库存量正式页打回 `Common/PlannedPage/index`，下一批应优先做 fail-closed 或拆出总 seed 中已被专用 seed 接管的菜单。
- P1：`top_menu_seed.sql` 复用若依原生 `menu_id = 108/3`，但还没有 slot/signature guard；已有定制环境中存在静默重写风险。
- P1：seller/buyer 端内 `oper_log` 的 direct-login 审计仍未结构化落库，当前主要依赖 `oper_param` 文本前缀。该项需要新增 `seller_oper_log` / `buyer_oper_log` 字段，按 AGENTS 规则必须先出 DDL 方案并确认，本轮未直接改表。
- P1：`ry_20260417.sql` / `quartz.sql` 是 bootstrap 初始化脚本，误执行后果是 P0，但当前可达性主要是人工误执行和文档治理缺口；后续适合补 bootstrap-only 静态合同，不直接改初始化 SQL 本体。
- P1：`20260604_portal_direct_login_ticket.sql`、`20260604_three_terminal_isolation_migration.sql` 仍有裸 `ALTER TABLE ... MODIFY` 类型的定义收敛语句。本轮只处理裸 `ADD COLUMN` 重放失败和最明确的 `sale_price` 条件 modify，广义 DDL 收敛 guard 后续单独排队。
- P1：`20260606_upstream_sync_staging_diff.sql` 当前承载 integration Mapper 必需的仓库/渠道 payload 列、SKU `wms_payload_*`、sync state/batch 和 staging 表；fresh bootstrap 如果只跑初始化基线和 `upstream_system_management_seed.sql` 仍会缺这些 schema。后续应把这些表/列吸收到 seed 基线，或在 bootstrap 文档中明确为强制第二步。
- 已核对：`20260607_terminal_login_log_direct_login_audit.sql` 和 `20260607_terminal_oper_log_direct_login_audit.sql` 已经使用 `add_column_if_missing(...)`，并有显式 guard、业务字段契约覆盖；本轮不改代码，只在同类补列脚本扫描中记录为已覆盖。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`14` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：upstream sync staging diff 后续收口通过，`23` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，前端 `4` 个 suite、`14` 个测试通过；后端 ruoyi-system `108`、ruoyi-framework `15`、product `1`、seller `83`、buyer `84` 测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅输出既有 LF/CRLF 换行提示。

## 未验证原因

- 未做浏览器运行态验收、截图、DOM 检测：用户已明确当前快速推进模式无需浏览器验证。
- 未连接远程 MySQL / Redis，未执行 SQL，未写远程数据：本轮只修改 SQL 脚本和静态合同测试；虽然用户此前已确认远程 DDL/DML 可以执行，但本切片不需要真实执行迁移。
- 未直接修复 oper_log 结构化审计：涉及新增端内日志表字段，需要先提交 DDL 方案并确认。

## 权限检查结果

- 本轮未改管理端、卖家端、买家端权限判断逻辑。
- SQL 合同只加强迁移脚本 fail-closed 和可重放性，不改变 `sys_menu`、`seller_menu`、`buyer_menu` 的权限语义。
- 子 Agent 发现的 `business_menu_seed.sql` / `top_menu_seed.sql` 菜单 guard 残留已记录为后续 P0/P1。

## 字典/选项复用检查结果

- 本轮未新增字典类型、字典数据或前端选项。
- `product_distribution_status_price_log` 中既有字典更新逻辑保持原样，仅把字段 DDL 改为可重放。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记 SQL DDL 可重放 helper、条件 modify helper、provider 冲突 fail-closed 的复用规则。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：本检查点收尾同步通过，输出 `Synced 1 changed files`，`Modified: 1 - 52 nodes in 903ms`。

## 大文件合理性判断结果

- `SqlExecutionGuardContractTest.java` 已超过 500 行，但职责仍集中在 SQL 执行 guard 合同；本轮只追加同类合同，不拆分。
- 5 份 SQL 文件均保持单脚本单职责：来源商品候选字段、商品销售状态价格日志、币种 ShowAPI 迁移、商城 SKU 尺寸字段、商城编辑页演示数据。没有为了本轮 helper 化新增跨文件抽象。
- `20260606_upstream_sync_staging_diff.sql` 是既有大型 integration schema + job 迁移脚本，本轮只替换前置补列段，不继续抽离 staging 建表或 sys_job 逻辑。

## 重复代码检查结果

- SQL `add_column_if_missing(...)` 在多份脚本中重复出现，这是 MySQL 独立脚本缺少 include 机制下的局部 helper 模式，符合当前仓库已有写法。
- 没有复制 Java 业务逻辑或 React 业务逻辑。
- `20260606_upstream_sync_staging_diff.sql` 复用相同局部 helper 模式，减少脚本内 6 段重复手写 `@column_exists` 逻辑。

## 子 Agent 使用记录

- 按用户最新要求，本轮先尝试 6 个 `gpt-5.3-codex-spark` 子 Agent；平台返回用量/可用性限制，提示恢复时间为 `2026-06-13 01:59`，失败 Agent 已全部关闭。
- 回退使用 6 个 `gpt-5.4` 只读子 Agent，分别复核来源商品 SQL、商品销售状态/价格日志 SQL、币种/商城短脚本 SQL、业务/顶部菜单 seed、端内 oper_log direct-login 审计、bootstrap SQL 隔离风险；完成后已全部关闭。
- 本轮追加按当前规则回退使用并关闭 6 个 `gpt-5.4` 只读子 Agent，复核 `20260606_upstream_sync_staging_diff.sql` 的锚点列、合同测试、Markdown 更新位置、integration Mapper 依赖、同类补列脚本覆盖情况和 MySQL 语法注意点。

## 一句话总结

本轮把 5 份高风险增量 SQL 从裸 DDL 改为可重放、可 fail-closed 的脚本，并后续把 `20260606_upstream_sync_staging_diff.sql` 的手写补列段收敛到同一 helper 模板；未执行数据库迁移，下一批优先处理 fresh bootstrap schema 吸收和剩余菜单 slot/signature guard 残留。
