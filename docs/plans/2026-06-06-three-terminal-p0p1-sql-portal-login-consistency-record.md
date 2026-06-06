# 2026-06-06 三端 P0/P1 SQL Guard 与 Portal 登录一致性收口记录

本记录以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，执行范围限定为当前快速推进模式的 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 用户最新指定：子 Agent 优先使用 GPT-5.3 Codex；不可用时使用 `gpt-5.4`。
- 本轮先尝试 GPT-5.3 Codex，平台返回用量/可用性限制后关闭失败 Agent。
- 实际降级使用 6 个 `gpt-5.4` 子 Agent，切片覆盖 portal token/session/direct-login/log、seller 后端、buyer 后端、React 管理端模板、SQL/seed、验证脚本。
- 6 个有效子 Agent 均已返回结论；本轮采纳 SQL guard、JS sidecar guard、Portal 登录会话一致性和登录审计链路 P1；其余 P2 记录但不阻塞。

## 新增问题

- SQL 账号锁定 seed 对 `sys_menu` 的 slot/signature 检查不完整，存在菜单 ID 或权限点被占用时仍尝试 upsert 的风险。
- 端内 OWNER 唯一约束脚本在动态 DDL 前缺少 baseline 表/字段存在性检查。
- `PartnerSessionModal.js` sidecar 的会话表 rowKey 未与 `.tsx` 主文件保持同一 tokenId 优先规则。
- Portal 登录 token 先写 Redis，后续登录日志和会话表再写数据库；如果数据库记录失败，会留下可用 Redis token 但没有完整 DB 会话/审计记录。
- Portal 登录/免密登录接口成功返回时，`PortalLogAspect` 原先无法从本次返回值回查刚生成的端内 session，操作日志可能记录为 `anonymous`。

## 已修复问题

- `20260605_seller_account_lock_control.sql` 和 `20260605_buyer_account_lock_control.sql` 增加 `assert_sys_menu_slot`、`assert_sys_menu_signature_available`，在 upsert 前确认菜单 ID 和权限签名安全。
- `20260605_terminal_owner_account_unique_constraint.sql` 增加 `assert_table_exists`、`assert_column_exists`，在动态 OWNER 唯一约束 DDL 前确认 seller/buyer 账号表和关键字段存在。
- `SqlExecutionGuardContractTest` 增加 SQL guard 契约，防止上述脚本丢失运行时防护。
- `PartnerSessionModal.js` rowKey 改为优先使用 `record.tokenId`，并把 JS sidecar 纳入 `guard:partner-management`。
- `PortalTokenSupport` 增加显式 token 回查 session 的重载，用于登录响应后的同端会话解析。
- `PortalLogAspect` 在匿名允许的登录/免密登录接口成功返回时，从 `AjaxResult.data` 中的 `PortalLoginResult.token` 回查同端 session，补齐账号、主体和 direct-login acting admin 审计。
- `SellerServiceImpl`、`BuyerServiceImpl` 的普通登录和免密登录加事务边界与 Redis token 删除补偿；业务 `ServiceException` 不回滚，保留登录失败日志。
- 新增 `PortalLoginSessionConsistencyContractTest` 和 `PortalTokenSupportTest` 覆盖登录 token/session 一致性与显式 token 回查能力。
- `AGENTS.md` 增加子 Agent 模型优先级规则：优先 GPT-5.3 Codex，不可用再降级 `gpt-5.4`。

## 残留问题

- 子 Agent 标出的 `seller_menu` / `buyer_menu` 当前仍是端级菜单，不是每个主体独立菜单，属于后续权限产品形态 P2，不阻塞当前 P0/P1。
- `seller_account.user_name` / `buyer_account.user_name` 当前是端内全局唯一，不是 `(subject_id, user_name)` 复合唯一；按当前“账号层面必须注册两个账号”的方向可接受，若未来要求同名员工账号跨主体复用，需要另行设计。
- React 401 处理仍有重复入口，属于前端结构治理 P2。
- `PartnerManagement` 主组件体量较大，当前按快速推进保留；后续 UI 稳定后再拆分。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system,ruoyi-framework,seller,buyer -am "-Dtest=PortalTokenSupportTest,PortalLoginSessionConsistencyContractTest,SqlExecutionGuardContractTest,PortalLogAspectContractTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test

cd E:\Urili-Ruoyi\react-ui
npm run guard:partner-management

cd E:\Urili-Ruoyi
git diff --check
```

验证结果：
- Maven 目标测试通过：`PortalTokenSupportTest` 7 个测试、`PortalLoginSessionConsistencyContractTest` 1 个测试、`SqlExecutionGuardContractTest` 5 个测试、`PortalLogAspectContractTest` 1 个测试、`SellerServiceImplTest` 45 个测试、`BuyerServiceImplTest` 45 个测试均通过；reactor build success。
- `npm run guard:partner-management` 通过，输出 `Partner management template guard passed.`。
- `git diff --check` 通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。

## 未验证原因

- 未启动后端服务；本轮只处理代码级 P0/P1 和契约验证。
- 未执行远程 MySQL DDL/DML；SQL 只做脚本和 contract 收口。
- 未读取或写入 Redis；Redis token 行为通过单元测试和源码 contract 验证。
- 未做浏览器、截图、DOM 或 UI 细调验收；符合当前快速推进边界。

## 权限检查结果

- 账号锁定 seed 新增 `sys_menu` slot/signature 运行时 guard，避免覆盖或混用管理端权限菜单。
- Portal 登录日志回查仍按 `terminal` 校验，seller token 不会被 buyer 日志解析，buyer token 不会被 seller 日志解析。

## 字典/选项复用检查结果

- 本轮未新增字典、状态选项或业务枚举。

## 复用台账检查结果

- 已补充复用台账中的 Portal 登录会话一致性、SQL guard 和 JS sidecar guard 复用规则。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；返回 `Synced 10 changed files`，`Added: 1, Modified: 9 - 474 nodes`。

## 大文件合理性判断结果

- `SellerServiceImpl`、`BuyerServiceImpl` 为既有大文件；本轮仅在登录/免密登录方法内做同构补偿，不扩大职责。
- `PartnerManagement` 相关大文件保持既有边界，本轮只修 JS sidecar rowKey 与 guard，不做 P2 拆分。

## 重复代码检查结果

- 卖家/买家登录补偿按当前已确认的同构模板机械复制，字段、terminal、mapper 保持端隔离。
- SQL guard helper 在对应脚本内局部定义和清理，避免跨脚本隐式依赖。
