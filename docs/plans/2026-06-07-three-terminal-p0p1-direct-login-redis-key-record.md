# 2026-06-07 三端 P0/P1 快速推进：免密 Redis Key 端隔离记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本轮范围：只处理免密登录 Redis key 端隔离这一 P1 切片；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 新增问题

- `portal_direct_login:<tokenHash>` Redis key 未在 key 层编码 `seller/buyer`，不利于三端独立后的观测、清理和误用排查。

## 已修复问题

- `PortalDirectLoginSupport` 创建免密 payload 时改为写入 `portal_direct_login:{terminal}:{tokenHash}`。
- `consumeToken(...)` 读取时优先查端前缀 key，并兼容 30 分钟窗口内旧 `portal_direct_login:{tokenHash}` key。
- 成功消费、失败消费、过期和不匹配等收口路径统一删除新旧两种 Redis key。
- `PortalDirectLoginSupportTest` 补齐端前缀 key 写入断言和旧 key 兼容消费断言。
- 复用台账已同步更新 `PortalDirectLoginSupport` 的 Redis key 规则。

## 残留问题

- 管理端卖家/买家页缺静态路由兜底，直达 `/seller`、`/buyer` 或刷新存在 404 风险。
- 部门树跨主体写入/删除、角色菜单 `checkedKeys` 主体隔离、owner 角色禁停用/禁删除仍需补运行时隔离契约测试。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=PortalDirectLoginSupportTest test`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=PortalDirectLoginAuthContractTest test`

## 验证结果

- `PortalDirectLoginSupportTest`：通过，`14` 个测试通过。
- `PortalDirectLoginAuthContractTest`：通过，`4` 个测试通过。

## 未验证原因

- 未做浏览器、截图、DOM 或 UI 细调验收：用户已明确当前快速模式无需浏览器运行态验收。
- 未执行远程 MySQL DDL/DML，未读取或写入 Redis：本轮是代码与单元契约切片，不需要动运行库。

## 权限检查结果

- 本轮未新增接口、菜单或按钮权限。
- 免密消费仍沿用既有 seller/buyer direct-login 入口和 `PortalDirectLoginAuthContractTest` 的匿名例外契约。

## 字典/选项复用检查结果

- 本轮未新增字典、选项或字段枚举。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，明确免密 Redis payload key 使用 `portal_direct_login:{terminal}:{tokenHash}`，并记录旧 key 30 分钟兼容消费策略。

## CodeGraph 更新结果

- `codegraph sync .`：通过；首次结果为 `Synced 1 changed files`，`Modified: 1 - 101 nodes in 954ms`；回填记录后最终复跑结果为 `Already up to date`。

## 大文件合理性判断结果

- 本轮仅在既有 `PortalDirectLoginSupport` 和对应测试内做小范围修改，不新增大文件。
- 记录文件属于阶段留痕，不承载业务逻辑。

## 重复代码检查结果

- Redis key 拼接仍集中在 `PortalDirectLoginSupport` 内部私有方法，未在 seller/buyer service 或前端重复拼接。

## 子 Agent 说明

- 按用户要求优先尝试 `gpt-5.3-codex-spark`，平台返回额度限制；失败 Agent 已关闭。
- 已降级启动 6 个 `gpt-5.4` 只读扫描 Agent，用于并行检查剩余 P0/P1 风险；本记录只覆盖当前已落地的免密 Redis key 切片。
