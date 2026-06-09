# 2026-06-08 三端 P0/P1 SQL 与 Portal 商品字段契约收口记录

## 参考方向

- 继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向。
- 当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。
- 不做浏览器运行态验收、截图、DOM 检测或 UI 细调。
- 子 Agent 按用户最新要求使用 `gpt-5.4`，不再把 GPT-5.3 Codex 作为首选。

## 子 Agent 使用记录

- 本轮收敛并关闭 6 个 `gpt-5.4` 子 Agent。
- 覆盖切片：seller/buyer 后端隔离、SQL/seed guard、React route/access/proxy/request/token、product/integration/inventory/warehouse、portal 商品字段契约、verify gate。
- 已采纳 P1：SQL direct-login ticket 遗留行预检、历史 sys_role 清理事务/完成断言、buyer portal 商品仓库字段契约、seller/buyer portal 商品字段展示契约。
- 未采纳为阻塞项：若干 P2，如测试夹具仍使用 `SysUser` 搭 admin 安全上下文、401 helper 重复等，按快速推进模式记录不阻塞。
- 关闭状态：6 个子 Agent 均已关闭。

## 已修复 P1

1. `20260604_portal_direct_login_ticket.sql`
   - `assert_no_invalid_direct_login_ticket_rows()` 显式拒绝 `terminal`、目标主体、目标账号、目标用户名、acting admin、token hash、status 和 used_time 状态中的 `NULL` / 空串 / 非法值。
   - `SqlExecutionGuardContractTest` 增加这些 fail-closed 条件的合同断言。

2. `20260606_legacy_disable_sys_seller_buyer_roles.sql`
   - 增加 `assert_legacy_sys_role_cleanup_completed()` 完成断言。
   - 将目标角色禁用清理包进 `start transaction` / `commit`。
   - `SqlExecutionGuardContractTest` 增加顺序合同：`assert_targets -> start transaction -> update sys_role -> assert_completed -> commit`。

3. buyer portal 商品仓库字段契约
   - `BuyerPortalProduct` / `BuyerPortalProductSku` 增加 `warehouseCount`。
   - `BuyerPortalProductServiceImpl` 从 `ProductSpu` / `ProductSku` 映射 `warehouseCount`。
   - `BuyerPortalProductServiceImplTest` 增加 buyer DTO 仓库字段正向断言，同时保留 seller/supply 隐藏字段负向断言。
   - `react-ui/src/types/seller-buyer/party.d.ts` 增加 buyer portal 商品和 SKU 的 `warehouseCount` 类型。

4. seller/buyer portal 商品页面字段契约
   - seller portal 商品列表和详情展示供货价范围与发货仓数。
   - buyer portal 商品列表和详情展示发货仓数。
   - `check-seller-portal-product-template.mjs` / `check-buyer-portal-product-template.mjs` 增加关键字段正向 guard。
   - `portal-product-schema-preview.test.ts` 增加 seller/buyer portal 商品字段可见性与 buyer 隐藏字段负向合同。

5. AGENTS 子 Agent 规则
   - `AGENTS.md` 已更新为默认使用 `gpt-5.4`。
   - 未经用户当前任务重新明确要求，不再把 GPT-5.3 Codex 作为首选。

## 权限与三端隔离判断

- 本轮未新增后端接口、菜单、按钮权限或端内角色菜单。
- 子 Agent 与本地验证未发现新的 seller/buyer 账号、菜单、权限、日志、会话、token/Redis key 串端 P0/P1。
- buyer portal 商品仍不暴露 seller/supply 字段；本轮只补仓库汇总这种 buyer 可见业务字段。
- seller/buyer 商品详情动作继续由 `canQuery` fail-closed 控制。

## 数据源与远端影响

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 本轮 SQL 修改只更新脚本和合同测试，未对当前运行库回放。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am "-Dtest=SellerPortalProductServiceImplTest,BuyerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，seller 8 个测试、buyer 9 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/check-seller-portal-product-template.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/check-buyer-portal-product-template.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/portal-product-schema-preview.test.ts --runInBand`：通过，1 个 suite / 6 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，72 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，退出码 0；仅输出 LF/CRLF 换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 同步 11 个变更文件，修改 11 个、519 个节点。

## 未验证原因

- 未做浏览器运行态、截图或 DOM 检测：用户已明确当前阶段无需。
- 未执行完整 `npm run verify:three-terminal`：当前快速推进模式只跑最小必要测试和 manifest 自检。
- 未回放 SQL：本轮目标是脚本 guard 与合同收口，不触达远程数据库。

## 当前残留

- P2：测试夹具里仍有 `SysUser` 用于 admin 安全上下文，不代表生产 seller/buyer 端内账号继续复用 `sys_user`。
- P2：`app.tsx` 与 `requestErrorConfig.ts` 的 401 分流逻辑仍有重复，当前行为一致，后续可抽共享 helper。
- P2：本轮只做字段级展示补齐，未做 UI 细调。
