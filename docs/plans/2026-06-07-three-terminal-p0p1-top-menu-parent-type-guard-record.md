# 2026-06-07 三端 P0/P1 快速推进：顶级菜单 Parent/Type Guard 记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 本轮边界

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 本轮代码改动仅收紧 `top_menu_seed.sql` 的静态 SQL guard 和对应合同测试。

## 子 Agent 使用情况

- 先按用户要求尝试 6 个 `gpt-5.3-codex-spark` 子 Agent；平台返回额度限制，恢复时间为 `2026-06-13 01:59`，失败 Agent 已关闭。
- 降级启动 6 个 `gpt-5.4` 只读子 Agent，覆盖 direct-login Redis key、seller/buyer 免密链路、测试落点、前端影响、规则文档和残留 P1 排序。
- 子 Agent 结论中，direct-login Redis 新 key 已是 `portal_direct_login:{terminal}:{token_hash}`，主链路不再作为当前 P1 处理；本轮改为收口仍明确存在的 `top_menu_seed.sql` 同 ID guard 维度缺口。

## 新增问题

- P1：`top_menu_seed.sql` 已有 slot/signature guard，但同 ID 校验只比较 `path/component/route_name/perms`。
- 如果历史库中同一 `menu_id` 被挂到错误 `parent_id` 或变成错误 `menu_type`，脚本仍可能把它当成可覆盖菜单继续改写。

## 已修复问题

- `top_menu_seed.sql`
  - `tmp_top_menu_sys_menu_guard` 增加 `parent_id` 和 `menu_type`。
  - 同 ID slot 判断增加 `coalesce(m.parent_id, -1) = seed.parent_id` 和 `coalesce(m.menu_type, '') = seed.menu_type`。
  - 顶级菜单 guard 清单改为显式 `(menu_id, parent_id, menu_type, path, component, route_name, perms)`。
  - 继续允许 `108` 从若依原生 `log` 签名迁移到 `log-center` / `LogCenter` 签名，但二者都必须是顶级目录 `parent_id = 0`、`menu_type = 'M'`。
- `SqlExecutionGuardContractTest`
  - `topMenuSeedMustGuardSysMenuSlotsBeforeUpsert` 增加 parent/type 维度断言，防止后续回退。
- `AGENTS.md` 和 `docs/architecture/reuse-ledger.md`
  - 同步管理端 `sys_menu` seed 同 ID guard 必须覆盖 `parent_id/menu_type` 的规则。

## 残留问题

- direct-login Redis legacy key 读取属于已登记的 30 分钟兼容策略，当前主链路已使用端前缀 key；是否完全移除 legacy fallback 后续可单独硬化。
- 旧 SQL 动态补列 guard 仍需按脚本分批收口。
- direct-login 会话被后台强退时，当前 schema 不能同时结构化保存原 issuer 与本次 operator；是否扩字段需后续方案确认。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`

## 验证结果

- `SqlExecutionGuardContractTest`：通过，`27` 个测试通过。

## 未验证原因

- 未执行 SQL：本轮只改 SQL 文本 guard 和静态合同测试。
- 未做浏览器、截图、DOM 或 UI 细调验收：用户已明确当前快速模式无需浏览器运行态验收。
- 未运行完整三端总验证：本轮是窄范围 SQL guard 合同收口，先跑最小必要测试；收尾如有需要再纳入总入口。

## 权限检查结果

- 本轮不新增接口、菜单或按钮权限。
- 只收紧管理端 `sys_menu` seed 对现有顶级目录的回放保护，不改变 seller/buyer 端内权限模型。

## 字典/选项复用检查结果

- 本轮未新增字典、选项或前端下拉。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记 `top_menu_seed.sql` 同 ID guard 必须覆盖 `parent_id/menu_type`。

## CodeGraph 更新结果

- `codegraph sync .`：通过，返回 `Synced 7 changed files`，`Modified: 7 - 830 nodes`。

## 大文件合理性判断结果

- `SqlExecutionGuardContractTest.java` 已超过 500 行，但职责仍集中在 SQL 执行和 seed guard 合同；本轮只追加同类断言，不拆分。

## 重复代码检查结果

- SQL guard 继续复用脚本内临时表 + procedure 的既有模式。
- 未新增 Java 业务逻辑或 React 业务逻辑重复。
