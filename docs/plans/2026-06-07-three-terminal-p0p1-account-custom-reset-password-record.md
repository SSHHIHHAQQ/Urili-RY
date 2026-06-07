# 2026-06-07 三端 P0/P1 账号自定义重置密码收口记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本轮继续快速推进模式：只处理 P0/P1 编译、guard、接口、权限、串端、service/字段缺失；不做浏览器、截图、DOM 或 UI 细调。

## 子 Agent 情况

- 按本轮目标要求使用并关闭 6 个 `gpt-5.4` 只读扫描子 Agent。
- 本切片采纳密码扫描结论：后端已有 `resetPwd` 自定义密码接口，但管理端账号弹窗仍只调用默认密码重置，且 guard 未固定自定义密码接线。
- 其他子 Agent 返回的 P1 已记录为残留：免密消费 ack、账号级审计入口、三端迁移 SQL preflight、综合 seed bootstrap/patch 边界、portal 权限/菜单/前端请求 guard 补强。

## 已完成

- `PartnerService` 增加 `resetAccountPassword(id, accountId, password)` 契约。
- seller/buyer 管理端页面配置接入：
  - `resetAdminSellerAccountPassword`
  - `resetAdminBuyerAccountPassword`
- `PartnerAccountModal` 的账号行“重置密码”改为输入临时密码和确认密码，提交调用自定义 `resetPwd` 接口，不再静默重置为默认密码 `U12346`。
- 同步维护当前并存的 `.tsx` / `.js` 镜像文件。
- 后端 `PartnerSupport` 新增 `normalizeTemporaryPassword(...)`，集中校验临时密码非空且长度为 5-20 位。
- seller/buyer 自定义重置密码 service 改为复用 `normalizeTemporaryPassword(...)`，防止前端绕过校验。
- seller/buyer service 单测新增非法临时密码 fail-closed 覆盖，确认 mapper 写入和强踢会话发生前即拒绝。
- `check-partner-management-template.mjs` 和 `AdminAccountPermissionUiContractTest` 增加自定义重置密码接线守卫。
- 已更新 `AGENTS.md` 和 `docs/architecture/reuse-ledger.md`。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer,ruoyi-system -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest,AdminAccountPermissionUiContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；`SellerServiceImplTest` 53 个测试通过，`BuyerServiceImplTest` 53 个测试通过，`AdminAccountPermissionUiContractTest` 1 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 5 个 suite / 21 个测试通过，后端 ruoyi-system 132、ruoyi-framework 15、product 1、seller 91、buyer 92 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；结果为 `Already up to date`。

## 边界

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器、截图、DOM 或 UI 细调验收。
- 后端 controller 当前仍接收 `SellerAccount` / `BuyerAccount` 请求体，本轮通过 service 层白名单读取 `password` 并校验收口；后续如要进一步收窄入参，可另起 DTO 切片。

## 残留 P1

- 免密登录管理端提示仍未等待 portal 端真正消费 token 后 ack。
- 账号行缺少账号级审计入口；后端管理端审计接口已有按 `subjectId + accountId` 过滤能力。
- `20260604_three_terminal_isolation_migration.sql` 仍需把 legacy blocker 前移为真正 preflight，并用合同固定非事务化说明。
- `seller_buyer_management_seed.sql` 仍同时承担 fresh bootstrap 和全局增量修补职责，后续应拆分或增加 profile/freshness guard。
- portal `product:distribution:*`、`accounts/depts/roles` 细粒度权限、端内菜单后端 fail-closed、portal 主链路请求仍有 guard/test 补强空间。

## 交付清单

- 新增问题：已确认账号重置密码入口此前走默认密码，管理端提示语义与真实接口能力不一致。
- 已修复问题：账号重置密码改为人工临时密码，前后端校验和 guard 已补。
- 残留问题：见“残留 P1”。
- 验证命令：见“验证”。
- 未验证原因：按快速模式未做浏览器、截图、DOM、远程 DB 或 Redis 验收。
- 权限检查结果：仍使用 `*:admin:account:resetPwd`，并由前端和后端合同固定。
- 字典/选项复用检查结果：本切片不新增字典或选项。
- 复用台账检查结果：已更新 `docs/architecture/reuse-ledger.md`。
- CodeGraph 更新结果：`codegraph sync .` 通过，结果为 `Already up to date`。
- 大文件合理性判断结果：`PartnerAccountModal.tsx` 已超过 500 行，本轮按 P0/P1 小切片保留原结构；后续继续扩账号能力时应优先拆分账号表格、表单或操作区。
- 重复代码检查结果：seller/buyer 仍通过共享 `PartnerAccountModal` 和 `PartnerService` 契约复用；仅同步维护现有 `.js` 镜像。
