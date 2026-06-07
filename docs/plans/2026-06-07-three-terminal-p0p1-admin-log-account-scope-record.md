# 2026-06-07 三端 P0/P1 管理端日志账号筛选主体约束记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按快速推进模式只修 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本轮聚焦 seller/buyer 管理端登录日志、操作日志按账号筛选时的主体约束，移除 account-only 反查主体例外。

## 新增问题

- P1：seller/buyer 管理端日志查询原先允许只传 `accountId`，再通过裸 `select*AccountById(accountId)` 反查主体 ID。该例外和“三端账号查询必须显式主体约束”的主模式冲突。
- P1：`TerminalAccountIsolationTest` 原合同把该例外白名单化，导致后续生产代码仍可能继续扩散裸 accountId 查询。

## 已修复问题

- `SellerServiceImpl.resolveSellerAdminLogSubjectId(...)`：传 `sellerAccountId` 时必须同时传 `sellerId`，否则 fail-closed，不再调用裸 `sellerMapper.selectSellerAccountById(...)`。
- `BuyerServiceImpl.resolveBuyerAdminLogSubjectId(...)`：传 `buyerAccountId` 时必须同时传 `buyerId`，否则 fail-closed，不再调用裸 `buyerMapper.selectBuyerAccountById(...)`。
- `SellerServiceImplTest` / `BuyerServiceImplTest`：旧的“只传账号 ID 自动推导主体”用例改为缺主体拒绝，并新增显式主体 + 账号筛选成功用例。
- `TerminalAccountIsolationTest`：从“允许管理端日志反查例外”改为“生产代码禁止裸 accountId mapper 调用”。
- `AGENTS.md` 和 `docs/architecture/reuse-ledger.md` 已同步新规则。

## 残留问题

- 端内接口仍需在后续真实业务接入时继续保持“从 token 推导主体范围，不相信前端传入 sellerId/buyerId”的规则。
- P1 候选：portal 自助日志接口不应把管理端免密代入的 `actingAdminId`、`actingAdminName`、`directLoginReason`、`directLoginTicketId` 等审计字段回显给卖家/买家端；后续应补 portal 自助日志脱敏 DTO 或 controller 映射。
- P1 候选：`20260606_upstream_inventory_dimension_sync.sql` 仍可能和 `upstream_system_management_seed.sql` 重复 owning `2307/2308/2309` 管理端菜单 slot；后续应收敛 owner 并补 SQL guard。
- P1 候选：`ry_20260417.sql` / `quartz.sql` 属 bootstrap-only 初始化脚本，后续应补静态合同，避免被当成普通增量 SQL 回放。
- runtime smoke 入口已有脚本但未纳入快速验证；按用户当前“不做浏览器运行态验收”要求，本轮只记录不阻塞。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`49` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`49` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalAccountIsolationTest" test`：通过，`3` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，前端 `4` 个 suite / `18` 个测试通过；后端 ruoyi-system `120`、ruoyi-framework `15`、product `1`、seller `85`、buyer `86` 个测试通过，最终输出 `three-terminal verification passed.`。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：记录补充后复跑通过，输出 `Already up to date`。

## 未验证原因

- 未连接远程 MySQL / Redis，未执行 SQL，未写远程数据：本轮只改 service、测试和规则记录。
- 未做浏览器运行态验收、截图、DOM 检测：用户已明确当前快速推进模式无需浏览器验证。

## 权限检查结果

- 管理端日志列表仍走若依后台权限体系，不改 `sys_*` 管理端控制面。
- seller/buyer 端内日志查询不受本轮影响，仍从端内 session 推导主体和账号。
- 管理端日志按账号筛选时，必须显式绑定当前主体，避免 accountId-only 查询成为绕过主体上下文的例外入口。

## 字典/选项复用检查结果

- 本轮未新增字典、选项或前端枚举。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记生产代码禁止裸 accountId mapper 调用，管理端日志筛选也必须使用显式主体 + 账号范围。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 5 changed files`，`Modified: 5 - 639 nodes`。
- 记录补充后再次执行 `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 大文件合理性判断结果

- `SellerServiceImpl` / `BuyerServiceImpl` 是既有端内账号、日志、会话 service，本轮只收紧私有日志查询归一化 helper，不拆分。
- `SellerServiceImplTest` / `BuyerServiceImplTest` 已超过 500 行，但当前承担同一 service 的端内账号、会话、日志行为回归；本轮只追加同类小用例，不在快速 P0/P1 切片中做测试文件拆分。
- `TerminalAccountIsolationTest` 职责仍集中在三端账号隔离静态合同，不拆分。

## 重复代码检查结果

- seller/buyer 双端保持同构实现，符合当前“卖家模板通过后机械复制买家”的推进方式。
- 本轮没有复制前端或 SQL 业务逻辑。

## 子 Agent 使用记录

- 本轮先尝试 6 个 `gpt-5.3-codex-spark` 只读子 Agent；平台返回额度限制，恢复时间为 `2026-06-13 01:59`，失败 Agent 已全部关闭。
- 按规则回退启动 6 个 `gpt-5.4` 只读子 Agent。account-only 日志反查主题的子 Agent 结论为 P1，并建议移除例外、同步 seller/buyer service 测试和 `TerminalAccountIsolationTest`。
- 其余 `gpt-5.4` 子 Agent 结论处理：React 管理端未发现新的 P0/P1；seller/buyer 验证入口后续应坚持 `-am`；bootstrap-only SQL、旧库存维度同步 SQL 重复 owner、portal 自助日志回显 acting admin 审计字段均记录为后续 P1，不扩大本切片。
- 本轮有效子 Agent 已全部关闭。

## 一句话总结

本轮把 seller/buyer 管理端日志账号筛选从“accountId-only 自动反查主体”收紧为“必须显式传主体 + 账号”，并用静态合同禁止生产代码恢复裸 accountId mapper 调用。
