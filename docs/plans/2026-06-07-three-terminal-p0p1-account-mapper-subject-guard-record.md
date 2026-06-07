# 2026-06-07 三端 P0/P1 快速推进：账号 Mapper 主体 Guard 下推记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本轮范围：只收紧 seller/buyer 账号查询的主体作用域 guard。当前仍按快速推进模式处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 本轮按用户要求先尝试 2 个 GPT-5.3 Codex 子 Agent；平台返回用量限制，失败 Agent 已关闭。
- 随后降级使用并关闭 6 个 `gpt-5.4` 只读子 Agent，切片覆盖 seller、buyer、测试落点、文档同步、Mapper SQL 和剩余 P1 分类。
- 子 Agent 结论与主线程核查一致：裸 `select*AccountById(accountId)` 只应保留在管理端日志筛选中“仅凭 accountId 反推主体”的例外分支；已有 `sellerId` / `buyerId` 上下文的路径应下推到 SQL 层。

## 新增问题

- `SellerMapper.selectSellerAccountById(Long sellerAccountId)` 与 `BuyerMapper.selectBuyerAccountById(Long buyerAccountId)` 只按账号 ID 查询。
- 多个已具备 `sellerId` / `buyerId` 上下文的 service 和 permission guard 先裸查账号，再在 Java 层比对主体 ID。
- 风险路径包括：
  - 管理端账号详情、编辑、锁定、解锁、重置密码。
  - 管理端强制踢出单账号 session。
  - 管理端生成单账号免密登录 token。
  - 端内免密登录消费和 token 校验。
  - 端内自助修改密码。
  - 权限服务校验当前端内账号。
  - 无在线 session 快照时写强制踢出审计日志的用户名补全。

## 已修复问题

- 新增 seller scoped 查询：
  - `SellerMapper.selectSellerAccountByIdAndSellerId(Long sellerId, Long sellerAccountId)`。
  - `SellerMapper.xml` 对应 SQL 增加 `a.seller_id = #{sellerId}`。
- 新增 buyer scoped 查询：
  - `BuyerMapper.selectBuyerAccountByIdAndBuyerId(Long buyerId, Long buyerAccountId)`。
  - `BuyerMapper.xml` 对应 SQL 增加 `a.buyer_id = #{buyerId}`。
- `SellerServiceImpl` / `BuyerServiceImpl` 已将具备主体上下文的账号查询切换到 scoped Mapper：
  - `select*AccountById`。
  - `update*Account` 的 payload 账号查询。
  - `forceLogout*AccountSessions`。
  - `create*AccountDirectLogin`。
  - `directLogin*`。
  - `select*DirectLoginAccount`。
  - `update*OwnPassword`。
  - `record*ForceLogoutAudit` 无 session 快照分支。
- `SellerPortalPermissionServiceImpl` / `BuyerPortalPermissionServiceImpl` 的 `assert*Account(...)` 已改为 scoped Mapper。
- 管理端日志归一化保留 account-only 反查例外：
  - 如果同时有 `subjectId + accountId`，走 scoped Mapper。
  - 如果只有 `accountId`，保留裸查，用于反推出主体 ID 后再查日志。
- seller/buyer 相关单测代理已同步支持新 scoped Mapper 方法。
- `TerminalAccountIsolationTest` 已新增静态契约，扫描 `seller/src/main/java` 与 `buyer/src/main/java`，固定生产代码中裸 `sellerMapper.selectSellerAccountById(...)` / `buyerMapper.selectBuyerAccountById(...)` 只能出现在管理端日志 account-only 反查 helper 内。

## 残留问题

- 2026-06-07 追加收口：裸 `select*AccountById(accountId)` Mapper 接口声明和 XML 已删除，不再保留管理端日志 account-only 反查例外；管理端日志按账号筛选也必须显式提供 `sellerId/buyerId + accountId`。
- direct-login 会话同时结构化保存 issuer/operator 已由后续切片补强，当前不再作为本记录残留。
- 端内 `seller_oper_log` / `buyer_oper_log` direct-login 结构化审计已由后续切片补强，当前不再作为本记录残留。
- `top_menu_seed.sql` 对 `2040/2000` 的 legacy cleanup 已由后续菜单 guard 切片收口，当前不再作为本记录残留。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerServiceImplTest,SellerPortalPermissionServiceImplTest,SellerPortalPermissionServiceImplPortalAccessTest,SellerPortalPermissionServiceImplMenuTreeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerServiceImplTest,BuyerPortalPermissionServiceImplTest,BuyerPortalPermissionServiceImplPortalAccessTest,BuyerPortalPermissionServiceImplMenuTreeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `cd E:\Urili-Ruoyi\RuoYi-Vue\ruoyi-system; mvn -q "-Dtest=TerminalAccountIsolationTest,TerminalSqlIsolationContractTest" test`
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
- `cd E:\Urili-Ruoyi; git diff --check`
- `cd E:\Urili-Ruoyi; codegraph sync .`

## 验证结果

- seller 目标单测：通过；`68` 个测试通过。
- buyer 目标单测：通过；`68` 个测试通过。
- `TerminalAccountIsolationTest,TerminalSqlIsolationContractTest`：通过。
- `npm run verify:three-terminal`：通过。
  - 前端：`4` 个 suite / `18` 个测试通过。
  - 后端：ruoyi-system `115`、ruoyi-framework `15`、product `1`、seller `83`、buyer `84` 个测试通过。
- `git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。

## 未验证原因

- 未做浏览器、截图、DOM 或 UI 细调验收：用户已明确当前快速模式无需浏览器运行态验收。
- 未执行远程 MySQL DDL/DML：本轮只改代码、Mapper SQL 和单测代理，没有运行库数据变更。
- 未读取或写入 Redis：本轮不涉及实际 token/session 运行态。
- 未启动或重启后端：本轮使用编译和单测验证代码级行为。

## 权限检查结果

- 本轮未新增后端接口、菜单或按钮权限。
- 已收紧 seller/buyer 权限服务里的账号 guard，端内权限信息、菜单树和角色绑定路径不再通过裸账号查询确认账号归属。

## 字典/选项复用检查结果

- 本轮未新增字典、枚举或前端选项。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，记录端内账号主体作用域 Mapper 查询模板。

## CodeGraph 更新结果

- `codegraph sync .`：通过；静态契约追加和记录回填前输出 `Synced 1 changed files`，`Modified: 1 - 28 nodes in 756ms`；记录回填后复跑通过。

## 大文件合理性判断结果

- 本轮未新增大代码文件。
- 修改的 service、Mapper、XML 和测试文件都属于既有文件的 P1 guard 收口，未引入新的大文件拆分风险。

## 重复代码检查结果

- seller 先做标准 scoped 查询模板，buyer 按同构命名机械复制。
- seller/buyer 只保留端类型、字段名、错误文案差异；业务 guard 规则保持一致。
