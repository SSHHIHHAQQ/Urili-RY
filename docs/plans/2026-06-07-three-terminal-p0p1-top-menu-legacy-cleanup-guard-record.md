# 2026-06-07 三端 P0/P1 顶级菜单 Legacy Cleanup Guard 记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按快速推进模式只修 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本轮只收口 `top_menu_seed.sql` 中 `2040/2000` 历史菜单状态清理的脚本安全性，不执行远程库 SQL，不做浏览器、截图、DOM 或 UI 细调。

## 新增问题

- P1：`top_menu_seed.sql` 对 `menu_id = 2040` 的旧渠道草案 cleanup 是裸 `update sys_menu`，没有先确认该 ID 仍是旧渠道草案。
- P1：`top_menu_seed.sql` 对 `menu_id = 2000` 的旧 `URILI运营后台` wrapper root cleanup 是裸 `update sys_menu`，没有先确认该 ID 仍是旧包装根菜单。
- 历史备注：`20260605_source_product_library_menu_component.sql` 也存在固定 `menu_id = 2400` 的菜单迁移更新；该项当时不属于本轮 legacy cleanup 切片，后续已在菜单 seed owner guard 记录中单独收口。

## 已修复问题

- `top_menu_seed.sql` 新增独立 `tmp_top_menu_legacy_cleanup_guard` 和 `assert_top_menu_legacy_cleanup_guard()`。
- `2040` cleanup 只允许旧渠道草案签名：
  - `menu_id = 2040`
  - `parent_id = 0`
  - `menu_name = '渠道管理'`
  - `path in ('urili-channel', 'channel')`
  - `menu_type = 'M'`
- `2000` cleanup 只允许旧 wrapper root 语义：
  - `menu_id = 2000`
  - `parent_id = 0`
  - `menu_name = 'URILI运营后台'`
  - `menu_type = 'M'`
- legacy cleanup guard 与现役 `tmp_top_menu_sys_menu_guard` 分离，避免把退役菜单签名混入当前顶级菜单 owner 集合。
- `SqlExecutionGuardContractTest` 新增 `topMenuSeedLegacyCleanupMustFailClosedBeforeUpdatingLegacyMenus`，固定 legacy cleanup guard 必须先于 `where menu_id = 2040` 和 `where menu_id = 2000` 的更新。
- 已更新复用台账：`docs/architecture/reuse-ledger.md`。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`21` 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅输出既有 LF/CRLF 换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 1 changed files`，`Modified: 1 - 50 nodes in 1.1s`。

## 未验证原因

- 未执行远程 MySQL SQL：本轮目标是脚本 guard 和静态合同收口；执行 `top_menu_seed.sql` 会实际更新远程 `sys_menu.visible/status/order_num/remark`，不能增加本轮 guard 证据。
- 未读取或写入 Redis：本切片不涉及会话或缓存。
- 未启动或重启后端：本切片不涉及运行态接口。
- 未做浏览器、截图、DOM 或 UI 细调：用户已明确当前快速推进模式无需浏览器验收。

## 权限检查结果

- 本轮只处理管理端若依 `sys_menu` 顶级菜单历史草案 cleanup 的脚本安全性。
- 未新增、删除或修改 seller/buyer 端内权限。
- 未新增管理端按钮权限。

## 字典/选项复用检查结果

- 本轮未新增字典类型、字典数据或前端选项。

## 复用台账检查结果

- 已在 `docs/architecture/reuse-ledger.md` 记录：`2040/2000` legacy cleanup 必须走独立 allowlist guard，禁止裸 `update sys_menu where menu_id in (2040, 2000)`。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 1 changed files`，`Modified: 1 - 50 nodes in 1.1s`。

## 大文件合理性判断结果

- `SqlExecutionGuardContractTest.java` 已超过 500 行，但职责仍集中在 SQL 执行和菜单 seed guard 合同；本轮只追加同类静态合同，不拆分。
- `top_menu_seed.sql` 仍是顶级菜单 seed 单文件；本轮新增的 legacy cleanup guard 与现役顶级菜单 owner guard 职责分离，未引入跨模块业务逻辑。

## 重复代码检查结果

- SQL guard 继续沿用脚本内临时表 + procedure 的局部 helper 模式，符合现有 MySQL seed 无 include 机制的写法。
- 没有复制 Java 业务逻辑或 React 业务逻辑。

## 子 Agent 使用记录

- 先尝试 2 个 `gpt-5.3-codex-spark` 只读子 Agent，平台返回额度限制，恢复时间为 `2026-06-13 01:59`，失败 Agent 已关闭。
- 回退使用并关闭 6 个 `gpt-5.4` 只读子 Agent，分别复核 legacy cleanup 缺口、合同测试落点、Markdown 回填、相邻 SQL 风险、远程库执行必要性和 SQL 语法/副作用。

## 残留问题

- 已收口：`20260605_source_product_library_menu_component.sql` 的 `menu_id = 2400` 固定更新已在后续检查点补 `tmp_source_product_library_menu_component_guard` / `assert_source_product_library_menu_component_guard()`，不再作为本记录残留风险。
- 如果后续要真实重放 `top_menu_seed.sql` 到远程库，必须先确认当前激活数据源，并只读预检 `sys_menu` 中 `2000/2040/2041/2042` 的现状，再决定是否执行。

## 一句话总结

本轮把 `top_menu_seed.sql` 的 `2040/2000` 历史菜单 cleanup 从裸固定 ID 更新改为 fail-closed allowlist guard，并用 `SqlExecutionGuardContractTest` 锁住回归。
