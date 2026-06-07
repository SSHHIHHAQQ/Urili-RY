# 2026-06-07 三端 P0/P1 Portal 权限读路径前缀 Guard 记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按快速推进模式只修 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本轮聚焦 seller/buyer portal 权限读路径，确保历史脏数据、手工 SQL 或 seed 偏差不会把跨端权限、管理端权限或全权限污染进端内 session。

## 新增问题

- P1：seller/buyer `splitPermissions(...)` 对逗号分隔权限只 trim 整串，不 trim 每个分段；`"buyer:account:list, buyer:account:edit"` 会把第二项保留前导空格，导致端内权限误判缺失。
- P1：seller/buyer portal 权限读路径直接信任 DB 返回的 `perms`，没有运行时复核本端前缀；历史脏数据可能把对端权限、管理端权限或 `*:*:*` 带入端内 permission set。

## 已修复问题

- `SellerPortalPermissionServiceImpl`：
  - `splitPermissions(...)` 改为逐项 trim，过滤空段。
  - 运行时只允许 `seller:` 开头的端内权限。
  - 显式拒绝 `seller:admin:`、非 seller 前缀和 `*:*:*`。
- `BuyerPortalPermissionServiceImpl`：
  - `splitPermissions(...)` 改为逐项 trim，过滤空段。
  - 运行时只允许 `buyer:` 开头的端内权限。
  - 显式拒绝 `buyer:admin:`、非 buyer 前缀和 `*:*:*`。
- `SellerPortalPermissionServiceImplPortalAccessTest` / `BuyerPortalPermissionServiceImplPortalAccessTest`：
  - 增加分段 trim 和空段过滤断言。
  - 增加跨端、管理端命名空间、全权限污染 fail-closed 断言。

## 残留问题

- 管理端日志查询仍存在 account-only 反查主体例外，后续应评估是否改为必须显式传主体 ID。
- `PortalPermissionChecker` 仍只做集合匹配；本轮把 terminal prefix guard 放在 seller/buyer 权限读路径，避免扩大 framework 改动面。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerPortalPermissionServiceImplPortalAccessTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：首次发现 `Arrays` import 误删导致 seller 编译失败；补回后通过，`6` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerPortalPermissionServiceImplPortalAccessTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：首次发现 `Arrays` import 误删导致 buyer 编译失败；补回后通过，`6` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：最终收口通过，前端 `4` 个 suite / `18` 个测试通过；后端 ruoyi-system `120`、ruoyi-framework `15`、product `1`、seller `84`、buyer `85` 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：最终收口通过，仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。

## 未验证原因

- 未连接远程 MySQL / Redis，未执行 SQL，未写远程数据：本轮只改 seller/buyer service 和单元测试。
- 未做浏览器运行态验收、截图、DOM 检测：用户已明确当前快速推进模式无需浏览器验证。

## 权限检查结果

- seller 端运行时 permission set 现在只能包含 seller 端内权限，不再接受 buyer 权限、seller 管理端权限或全权限。
- buyer 端运行时 permission set 现在只能包含 buyer 端内权限，不再接受 seller 权限、buyer 管理端权限或全权限。
- 本轮不改变管理端若依 `sys_*` 权限体系，不改变端内菜单写入逻辑。

## 字典/选项复用检查结果

- 本轮未新增字典、选项或前端枚举。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记 portal 权限读路径必须逐项 trim 并按 terminal prefix fail-closed。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：最终收口执行通过，输出 `Synced 5 changed files`，`Modified: 5 - 325 nodes`。

## 大文件合理性判断结果

- `SellerPortalPermissionServiceImpl` / `BuyerPortalPermissionServiceImpl` 是既有端内权限 service，本轮只补私有解析/校验方法和对应测试，不新增抽象。
- 两个 portal access 测试类职责仍集中在登录态、在线会话和端内权限读取，不拆分。

## 重复代码检查结果

- seller/buyer 双端保持同构实现，符合当前“卖家模板通过后机械复制买家”的推进方式。
- 本轮没有复制前端或 SQL 业务逻辑。

## 子 Agent 使用记录

- 本问题来自本轮 6 个只读子 Agent 的 buyer/system 扫描结论；本轮遵守“GPT-5.3 Codex 优先，不可用则回退 gpt-5.4”的要求，因当前可用模型回退为 `gpt-5.4`。

## 一句话总结

本轮把 seller/buyer portal 权限读路径从“信任 DB 返回字符串”收紧为“逐项 trim + 本端前缀 fail-closed”，避免脏权限进入端内 session。
