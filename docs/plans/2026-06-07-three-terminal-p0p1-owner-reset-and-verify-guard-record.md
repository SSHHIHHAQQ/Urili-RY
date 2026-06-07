# 2026-06-07 三端 P0/P1 主账号默认重置入口与验证 Guard 收口记录

## 背景

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 已按最新规则先尝试 6 个 `gpt-5.3-codex-spark` 子 Agent；平台返回额度限制，失败 Agent 已关闭。
- 随后回退使用 6 个 `gpt-5.4` 只读子 Agent，覆盖 SQL seed、后端 runtime 隔离、React guard/service、Portal 业务接口 scope、三端验证清单、Markdown/复用台账一致性。
- 6 个回退子 Agent 均已完成并关闭。

## 本轮判断

- P1：管理端卖家/买家列表页仍存在主体级“重置主账号”入口，调用 `/{sellerId}/resetOwnerPwd` / `/{buyerId}/resetOwnerPwd` 并恢复默认密码 `U12346`。这与当前规则“重置密码默认语义为账号级人工输入 5-20 位临时密码；恢复默认密码必须是账号级单独入口”冲突。
- P1：`verify-three-terminal.mjs` 后端 report 校验只检查 surefire 文件存在，未校验 `tests > 0` 和 `skipped = 0`；前端关键 Jest 自动发现只扫 `react-ui/tests`，对 `react-ui/src/**` 下新增关键 test 文件不 fail-closed。
- P1：`docs/architecture/reuse-ledger.md` 仍有两处旧口径写成自动发现 `202606*.sql`，与当前 `DATED_SQL_FILE` 口径冲突。

## 已完成

- 移除管理端 `PartnerManagementPage` 顶层 `resetOwnerPwd` 操作，只保留账号弹窗内的账号级 `resetPwd` 人工临时密码入口。
- 移除 `Seller` / `Buyer` 管理页对 `resetAdmin*OwnerPassword` 的注入。
- 移除 `react-ui/src/services/seller/seller.*` 与 `react-ui/src/services/buyer/buyer.*` 中的 `resetAdmin*OwnerPassword` service。
- 移除 `AdminSellerController` / `AdminBuyerController` 中主体级 `resetOwnerPwd` 路由。
- 移除 `ISellerService` / `IBuyerService` 与对应实现中的主体级 owner 默认密码重置方法。
- 删除 seller/buyer service 单测里锁定旧主体级默认密码重置语义的用例；账号级 `resetDefaultPwd` 覆盖保留。
- 更新 `AdminAccountPermissionUiContractTest`，禁止管理页、页面配置和 service 重新出现 `resetOwnerPwd` / `resetOwnerPassword`。
- 更新 `SellerAdminPermissionContractTest` / `BuyerAdminPermissionContractTest`，禁止 controller 暴露主体级 owner 默认密码重置路由和 `*:admin:resetPwd` controller 权限。
- 更新 `verify-three-terminal.mjs`：
  - 前端关键 Jest 自动发现扩展到 `react-ui/tests` + `react-ui/src`。
  - 后端显式合同测试必须产出 XML surefire report，并要求 `tests > 0`、`skipped = 0`。
- 更新 `docs/architecture/reuse-ledger.md`，将旧 `202606*.sql` 自动发现口径统一为 `DATED_SQL_FILE` + dynamic DDL high-impact hint。

## 未执行 / 未处理

- 未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图或 DOM 验收。
- SQL seed 和历史授权脚本里仍可见 `seller:admin:resetPwd` / `buyer:admin:resetPwd` 历史权限。当前生产 controller 与 React 管理端已不再使用该主体级权限；后续如要清理远端菜单/角色授权，应另开 SQL 清理方案并按远端 DDL/DML 确认流程执行。
- `integration` 模块当前仍是 `verify-three-terminal` 的 reactor 编译覆盖，不是 integration 模块 surefire report 覆盖；本轮未新增 integration 合同测试。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\check-partner-management-template.mjs; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=AdminAccountPermissionUiContractTest,SellerAdminPermissionContractTest,BuyerAdminPermissionContractTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，113 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 guard、TypeScript、6 个 Jest suite / 30 个测试、后端 reactor test-compile 和后端三端合同链路均通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 同步 23 个变更文件。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 Git 的 LF/CRLF 工作区提示。
