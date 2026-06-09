# 2026-06-08 三端隔离 P0/P1 快速推进记录：商品审核审计与日志权限 Guard

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本轮仍按快速推进模式执行，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 执行情况

- 历史记录（已过期口径）：按最新规则优先尝试 6 个 GPT-5.3 Codex 子 Agent，工具模型 `gpt-5.3-codex-spark`；平台返回额度限制，提示到 `2026-06-14 15:12` 后再试，失败 Agent 已关闭。
- 按 fallback 规则启动并关闭 6 个 `gpt-5.4` 只读子 Agent。
- fallback 切片覆盖 SQL guard、product review 后端、product review 前端、seller/buyer 端隔离、验证闸门、product/inventory/integration/warehouse 共享域。

## 采纳的 P0/P1

- P0 观察项：子 Agent 和本地一度复现 `product` 编译断口，报 `refreshInventoryOverviewAfterCommit(...)` 无法解析。随后当前工作树已出现一致的方法定义和调用，按当前权威状态重跑 product reactor 编译通过，未留下残余编译断口。
- P1：`20260608_product_review.sql` 中 `assert_product_review_sys_menu_guard()` 调用过晚，菜单父级或按钮 slot 冲突时，表和字典已经持久写入，存在半执行状态。
- P1：商品审核 service 定义并持久化 `submitSubjectId`、`submitAccountId`、`reviewerId`、`operatorId`，但 service 未赋值，导致审核追责只能依赖用户名。
- P1：商品审核详情接口只需要 `review:productDistribution:query`，但 service 无条件返回 `logs`，无 `review:productDistribution:log` 的用户仍能在网络响应中拿到日志。

## 已完成

- `20260608_product_review.sql` 将 `tmp_product_review_sys_menu_guard` 准备和 `call assert_product_review_sys_menu_guard()` 前移到 confirm token 后、持久表和字典写入前。
- `SqlExecutionGuardContractTest` 增加顺序断言，固定商品审核菜单 guard 必须早于 `product_review_request` 建表、字典写入、菜单 update 和按钮 insert。
- `ProductReviewServiceImpl` 在管理端提交审核时写入当前 `sys_user.userId` 到 `submitSubjectId` 和 `submitAccountId`。
- `ProductReviewServiceImpl` 在审批/驳回时写入 `reviewerId` 和操作日志 `operatorId`。
- `ProductReviewServiceImpl.selectReviewById(...)` 不再返回操作日志；日志继续通过独立 `logs` 接口返回。
- `react-ui/src/pages/Product/Review/index.tsx` 改为有 `review:productDistribution:log` 权限时才调用 `getProductReviewLogs(...)`。
- `ProductModuleBoundaryContractTest` 增加商品审核审计 ID 和日志权限边界合同。
- `product-distribution-permission-guard.test.ts` 增加前端日志接口权限调用合同。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product,inventory,integration,warehouse -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dtest=ProductModuleBoundaryContractTest,ProductDistributionMapperContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，10 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,ProductAdminRouteContractTest" test`：通过，65 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests\product-distribution-permission-guard.test.ts --runInBand`：通过，1 个 suite / 6 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，退出码 0；仅输出当前工作区 LF/CRLF 换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 返回 `Already up to date`。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
