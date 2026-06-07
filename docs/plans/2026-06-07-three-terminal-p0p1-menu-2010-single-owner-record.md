# 2026-06-07 三端 P0/P1 2010 菜单 Single Owner 收口记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按快速推进模式只修 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本轮聚焦管理端 `sys_menu.menu_id = 2010` 的 seed owner 重复问题；不做浏览器运行态验收、不做截图/DOM 检测、不做 UI 细调。

## 新增问题

- P1：`2010` 主体管理顶级目录此前同时由 `top_menu_seed.sql`、`seller_buyer_management_seed.sql` 和 `20260606_admin_partner_page_direct_login_seed.sql` 写入，虽然签名一致，但 owner 不清会造成回放顺序依赖和后续排查歧义。
- P1：`seller_buyer_management_seed.sql` 原 `tmp_seller_buyer_sys_menu_guard` 只校验 path/component/route/perms，未把 `parent_id/menu_type` 纳入同 ID guard。
- P1：`20260606_admin_partner_page_direct_login_seed.sql` 原 `assert_sys_menu_slot(...)` 只校验 path/component/route/perms，未把 `parent_id/menu_type` 纳入同 ID guard。

## 已修复问题

- `top_menu_seed.sql` 保持 `2010` 主体管理顶级目录的唯一写入 owner。
- `seller_buyer_management_seed.sql`：
  - 删除 `2010` 的 guard seed 行和 `insert into sys_menu` upsert 行。
  - 在写 `2011/2012` 及按钮前断言 `2010` 已由 `top_menu_seed.sql` 提供，且签名为 `parent_id=0/menu_type=M/path=partner/route_name=PartnerManagement/perms=''`。
  - `tmp_seller_buyer_sys_menu_guard` 增加 `parent_id/menu_type`，同 ID guard 覆盖父级和菜单类型。
- `20260606_admin_partner_page_direct_login_seed.sql`：
  - 删除 `2010` upsert，不再对 `2010` 做 slot/signature 兼容写入。
  - 新增 `assert_partner_root_menu_exists()`，在写 `2011/2012/2205/2215` 前断言 `2010` 已存在且签名正确。
  - `assert_sys_menu_slot(...)` 增加 `parent_id/menu_type` 参数，避免历史同 ID 菜单挂错父级或类型时被静默改写。
- `SqlExecutionGuardContractTest` 更新为：`2010` 必须由 top seed 写入；seller/buyer 全量 seed 和 direct-login 增量 seed 不得再写 `2010`，只能先断言它。
- `StandalonePartnerSeedMenuContractTest` 更新为：direct-login seed 的菜单树依赖 top-owned `2010`，而不是文件内自带根节点。
- `AGENTS.md` 和 `docs/architecture/reuse-ledger.md` 已同步 `2010` single-owner 规则。

## 残留问题

- P1：端内 role-menu 当前已有本端存在性校验，但 `seller_menu` / `buyer_menu` ID 空间仍可能重叠；跨端提交同数字 ID 仍可能绑定成本端同号菜单，后续应做端内菜单 ID 段隔离或稳定 `businessKey` 方案。
- P1：SQL guard 自动发现仍需把日期模式从 `202606*.sql` 泛化到日期前缀增量 SQL，并补动态 DDL helper 的 high-impact hint。
- P1：seller/buyer 前端仍有未接通的“指定密码重置” service 导出；当前 UI 口径是默认重置为 `U12346`，后续建议删除未接通前端导出，不新增 UI 弹窗。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,TerminalSeedPermissionContractTest,StandalonePartnerSeedMenuContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`34` 个测试通过。

## 未验证原因

- 未做浏览器运行态验收、截图、DOM 检测：用户已明确当前快速推进模式无需浏览器验证。
- 未连接远程 MySQL / Redis，未执行 SQL，未写远程数据：本轮只修改 SQL 脚本、静态合同测试和 Markdown 记录。

## 权限检查结果

- `2010` 顶级目录无权限标识，唯一写入 owner 为 `top_menu_seed.sql`。
- `2011` 卖家管理、`2012` 买家管理和 `2200-2323` 管理端按钮权限仍由 `seller_buyer_management_seed.sql` 写入。
- `20260606_admin_partner_page_direct_login_seed.sql` 仅作为 split 环境的增量补丁写 `2011/2012/2205/2215`，不再写顶级根节点 `2010`。

## 字典/选项复用检查结果

- 本轮未新增字典类型、字典数据或前端选项。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记 `2010` 只能由 `top_menu_seed.sql` 写入；依赖 `2010` 的增量 seed 必须 fail-closed 校验根菜单，不得自行补根节点。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；首次同步输出 `Synced 2 changed files`，`Modified: 2 - 114 nodes in 1.1s`。
- 本轮最终同步输出 `Synced 5 changed files`，`Modified: 5 - 239 nodes in 996ms`；记录更新后复跑输出 `Already up to date`。

## 大文件合理性判断结果

- `SqlExecutionGuardContractTest.java` 已超过 500 行，但本轮只追加同类 SQL seed owner 合同，职责仍集中在 SQL 执行与菜单 seed guard；暂不拆分。
- `StandalonePartnerSeedMenuContractTest.java` 未超过治理阈值。
- 两个 SQL seed 文件职责未扩大，只减少 `2010` owner 重复并补 fail-closed guard；暂不拆分。

## 重复代码检查结果

- SQL guard 继续使用脚本内 procedure 模式，符合当前 MySQL seed 无 include 机制的写法。
- 没有复制 Java 业务逻辑或 React 业务逻辑。

## 子 Agent 使用记录

- 本轮按用户要求回退使用 4 个 `gpt-5.4` 只读子 Agent，分别复核 role-menu `menuIds` 串端风险、`2010` 菜单 owner、SQL guard 自动发现和前端重置密码 service 残留。
- 已采纳 `2010` 菜单 owner 子 Agent 结论，完成 `2010` single-owner 收口。
- 其他三个子 Agent 的有效结论已记录为后续 P1，不在本切片混做。
- 4 个子 Agent 均已关闭。

## 一句话总结

`2010` 主体管理顶级目录已经从历史三处 upsert 收敛为 `top_menu_seed.sql` 唯一写入，seller/buyer 全量 seed 和 direct-login 增量 seed 只允许先断言该根节点存在。
