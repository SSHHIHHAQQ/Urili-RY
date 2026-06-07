# 2026-06-07 三端 P0/P1 Admin 前缀 401 分流记录

## 背景

本记录以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按三端独立账号权限改造推进。

当前模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 问题

`/api/seller/**`、`/api/buyer/**` 中的普通端内 portal 请求 401 应清理对应 seller/buyer 端 token 并跳 `/seller/login` 或 `/buyer/login`。

但 `/api/seller/admin/**`、`/api/buyer/admin/**` 是管理端后台接口，仍应走 admin 登录失效流程，清理管理端 session 并跳 `/user/login`。此前实现已有分流逻辑，但测试只覆盖普通 portal API、普通 admin API 和分类函数，缺少 admin-prefixed portal API 的行为回归。

## 已完成

- `react-ui/tests/portal-unauthorized-redirect.test.ts`：
  - 新增 `/api/seller/admin/menus/list` 401 行为测试。
  - 新增 `/api/buyer/admin/menus/list` 401 行为测试。
  - 新增响应体 `code=401` 场景下 `/api/seller/admin/sellers/list` 仍走 admin 登录流且 reject 原 response 的测试。
- 已更新 `docs/architecture/reuse-ledger.md`，固定该测试必须同时覆盖普通 portal API、普通 admin API 和 admin-prefixed seller/buyer API。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runInBand tests/portal-unauthorized-redirect.test.ts`：通过，`1` 个 suite / `6` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过：
  - 前端 `5` 个 suite / `26` 个测试通过。
  - 后端 `ruoyi-system` `132`、`ruoyi-framework` `15`、`product` `1`、`seller` `91`、`buyer` `92` 个测试通过。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 残留 P1

- 账号行缺少账号级审计入口；后端管理端审计接口已有按 `subjectId + accountId` 过滤能力。
- `20260604_three_terminal_isolation_migration.sql` 仍需把 legacy blocker 前移为真正 preflight，并用合同固定非事务化说明。
- `seller_buyer_management_seed.sql` 仍同时承担 fresh bootstrap 和全局增量修补职责，后续应拆分或增加 profile/freshness guard。
- portal `accounts/depts/roles`、商城商品读接口和 admin dept/role controller 可补更精确方法级权限合同。
