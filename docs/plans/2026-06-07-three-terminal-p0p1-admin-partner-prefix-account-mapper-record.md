# 2026-06-07 三端 P0/P1 快速推进：管理端按钮端前缀与裸账号 Mapper 删除记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本轮范围：只处理 P0/P1 的权限、guard 和账号查询边界。不做浏览器运行态验收、截图、DOM 检测或 UI 细调；不执行远程数据库 DDL/DML。

## 子 Agent 使用情况

- 本轮按当前目标使用 6 个 `gpt-5.4` 子 Agent 并行只读扫描。
- 覆盖范围包括端内 portal 身份链路、Mapper/XML 主体约束、SQL seed、React guard、日志审计和验证入口。
- 6 个子 Agent 已全部关闭。

## 新增问题

- 管理端 partner 子按钮授权和非 admin cleanup 虽已改为明确 `menu_id + perms` 白名单，但仍缺少“父页端别与子按钮端别一致”的 SQL 约束。
- `SellerMapper` / `BuyerMapper` 接口和 XML 仍保留裸 `select*AccountById(accountId)` 入口，后续新代码可能误用它绕过 `sellerId/buyerId + accountId` 的 SQL 层主体约束。

## 已修复问题

- `20260606_admin_partner_role_menu_grant.sql` 子按钮授权增加端前缀一致性约束。
- `20260606_admin_partner_non_admin_button_grant_cleanup.sql` 的预期删除计数、预览查询和实际 delete 均增加端前缀一致性约束。
- `AdminDirectLoginPermissionContractTest` 固定子按钮白名单和端前缀一致性。
- 删除 seller/buyer Mapper 接口和 XML 中裸 accountId 查询入口。
- `TerminalAccountIsolationTest` 固定 Mapper 接口/XML 不得恢复裸 accountId 查询声明。

## 残留问题

- `business_menu_seed.sql` 和 `upstream_system_management_seed.sql` 的父级菜单 guard 可继续分批收口。
- React portal session `.js` 镜像深度 guard 和 401 行为级单测属于前端 guard 补强候选。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=TerminalAccountIsolationTest,AdminDirectLoginPermissionContractTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=AdminDirectLoginPermissionContractTest,TerminalAccountIsolationTest" test`
- `cd E:\Urili-Ruoyi; rg -n 'public .*selectSellerAccountById\(Long sellerAccountId\)|<select id="selectSellerAccountById"|sellerMapper\.selectSellerAccountById\(' RuoYi-Vue/seller/src/main`
- `cd E:\Urili-Ruoyi; rg -n 'public .*selectBuyerAccountById\(Long buyerAccountId\)|<select id="selectBuyerAccountById"|buyerMapper\.selectBuyerAccountById\(' RuoYi-Vue/buyer/src/main`
- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs`
- `cd E:\Urili-Ruoyi; git diff --check`

## 验证结果

- Maven 三模块目标验证：通过，`102` 个测试通过。
- ruoyi-system 合同测试：通过，`4` 个测试通过。
- seller/buyer main 目录裸 accountId Mapper 查询声明/XML/调用扫描：无命中。
- 三端总验证入口：通过，React `tsc`、Jest `4` 个 suite / `18` 个测试、后端合同测试均通过。
- `git diff --check`：通过，仅有工作区 LF/CRLF 换行提示，无 whitespace 错误。

## 未验证原因

- 未做浏览器、截图、DOM 或 UI 细调验收：当前快速模式明确不需要。
- 未执行远程 MySQL DDL/DML：本轮只改代码、SQL 文件和合同测试，没有运行库数据变更。
- 未读取或写入 Redis：本轮不涉及真实 token/session 运行态。

## 权限检查结果

- 管理端 partner 按钮授权仍只面向 `role_key='admin'`。
- 子按钮授权和 cleanup 必须同时满足明确 `menu_id` 白名单、明确 `perms` 白名单、父页端前缀一致性。

## 字典/选项复用检查结果

- 本轮未新增字典、枚举或前端选项。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记管理端 partner 按钮端前缀一致性和裸 accountId Mapper 声明禁止规则。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 返回 `Synced 6 changed files`，`Modified: 6 - 200 nodes`。

## 大文件合理性判断结果

- 本轮未新增大代码文件。
- 修改均为既有 SQL、Mapper、合同测试和 Markdown 记录的 P1 guard 收口。

## 重复代码检查结果

- seller 先收掉裸 Mapper，buyer 按同构规则机械同步。
- 管理端授权脚本和 cleanup 使用相同端前缀一致性条件。
