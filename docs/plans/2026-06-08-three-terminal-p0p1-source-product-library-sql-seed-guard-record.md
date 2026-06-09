# 三端快速推进 P0/P1 来源商品库与 SQL Seed Guard 收口记录

日期：2026-06-08

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

## 范围

本轮继续按三端独立方向推进，只处理当前 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态、截图、DOM 检测或 UI 细调。

## 子 Agent

- 历史记录（已过期口径）：当时按用户要求先尝试 6 个 GPT-5.3 Codex 子 Agent（工具模型 `gpt-5.3-codex-spark`）；平台返回 usage limit，提示需等到 2026-06-14 15:12 后再试，失败子 Agent 已关闭。现行规则为默认使用 `gpt-5.4`，除非用户在当前任务重新明确要求。
- 回退使用 6 个 `gpt-5.4` 只读子 Agent，覆盖 seller/buyer 后端隔离、React 三端 guard/session/direct-login、SQL seed/guard、来源商品/库存/商品中心权限、verify manifest/gate、admin 控制权六个切片。
- 6 个回退子 Agent 均已关闭；主线程复核后只采纳确认的 P0/P1：
  - 来源商品库菜单权限与后端接口权限漂移。
  - 来源商品库存在未实现的 `THIRD_PARTY_MASTER` tab/scope。
  - `product-center` / `source-product` 未进入三端 verifier 关键前端合同识别。
  - direct-login ticket 表缺少 `ticket_id` identity/主键合同。
  - split terminal permission seed 缺少端内菜单非法/重复权限预检。
  - `seller_buyer_management_seed.sql` 缺少 role-menu 孤儿/越界预检和最终状态断言。

## 已修复

### P1：来源商品库权限、菜单和未实现 scope 收口

- 文件：
  - `react-ui/src/pages/Product/SourceProductLibrary/index.tsx`
  - `react-ui/src/pages/Product/SourceProductLibrary/index.js`
  - `react-ui/src/pages/Product/SourceProductLibrary/SourceProductDetailDrawer.js`
  - `react-ui/src/pages/Product/SourceProductLibrary/constants.js`
  - `react-ui/src/services/integration/sourceProduct.js`
  - `RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSystemServiceImpl.java`
  - `RuoYi-Vue/sql/business_menu_seed.sql`
  - `RuoYi-Vue/sql/20260605_source_product_library_menu_component.sql`
  - `react-ui/tests/source-product-library-contract.test.ts`
  - `react-ui/tests/three-terminal.manifest.json`
  - `react-ui/scripts/verify-three-terminal.mjs`
- 问题：
  - 来源商品库是 integration 管理端读模型，菜单 seed 仍使用 `product:list:list`，与后端 `integration:upstream:query` 不一致。
  - 页面存在 `THIRD_PARTY_MASTER` tab，但当前后端读模型只支持官方来源商品库，第三方 scope 不应作为可操作入口。
  - 页面和 service 的 `.js` 镜像不是纯 re-export，存在运行入口分叉风险。
- 修复：
  - 来源商品库页面统一使用 `integration:upstream:query` 作为查询和详情权限；无权限时主表 request 和详情入口 fail-closed，不请求后端。
  - 移除未实现的 `THIRD_PARTY_MASTER` tab，只保留 `OFFICIAL_MASTER`。
  - 后端 `normalizeSourceProductQuery(...)` 对非官方来源商品库 scope 直接 `ServiceException` fail-closed。
  - 菜单 seed 最终权限改为 `integration:upstream:query`，guard 允许历史 `product:list:list` 占位签名迁移到新最终签名，但不允许静默扩散。
  - 相关 `.js` 镜像改为纯 TS/TSX re-export。
  - 新增前端合同测试，固定权限、scope、JS mirror、seed 权限、后端 unsupported scope fail-closed，以及 manifest/verifier 覆盖。

### P1：三端 verifier 关键前端合同识别不完整

- 文件：
  - `react-ui/scripts/verify-three-terminal.mjs`
  - `react-ui/tests/three-terminal.manifest.json`
- 问题：`product-center`、`source-product` 这类已进入三端 gate 的合同测试未被关键前端正则识别，后续 manifest 漂移时可能漏检。
- 修复：关键前端测试识别增加 `product-center` 和 `source-product` 命名空间；manifest 增加 `tests/source-product-library-contract.test.ts`。

### P1：SQL seed fail-closed 合同补齐

- 文件：
  - `RuoYi-Vue/sql/20260604_portal_direct_login_ticket.sql`
  - `RuoYi-Vue/sql/20260604_portal_account_list_permission_seed.sql`
  - `RuoYi-Vue/sql/20260604_portal_dept_role_list_permission_seed.sql`
  - `RuoYi-Vue/sql/20260607_portal_self_audit_permission_seed.sql`
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
- 问题：
  - direct-login ticket seed 只检查字段存在，未固定 `ticket_id` 必须是 `bigint not null auto_increment` 且为唯一主键。
  - split terminal permission seed 在写 role-menu 前缺少端内菜单非法 perms、跨端 perms、管理端命名空间、重复 perms 和 C 菜单 component 空值预检。
  - `seller_buyer_management_seed.sql` 在历史库 PATCH 分支缺少 role-menu 孤儿/越界预检，最终状态也缺少统一 completion assert。
- 修复：
  - 增加 `assert_portal_direct_login_ticket_identity_contract()` 并在字段存在检查后和字段合同检查后执行。
  - split seed 在写 role-menu 前分别校验 seller/buyer 菜单权限前缀、禁止 `*`、禁止跨端和 `seller:admin:` / `buyer:admin:` 管理端命名空间、禁止重复 perms，C 菜单 component 必填。
  - `seller_buyer_management_seed.sql` 的 range-ready 预检增加 seller/buyer role-menu 孤儿和越界检查。
  - 增加 `assert_seller_buyer_management_seed_completed()`，固定 sys_menu 最终签名、portal URL 配置存在、seller/buyer baseline 菜单存在且结构正确、role-menu 无孤儿/越界。
  - `SqlExecutionGuardContractTest` 增加上述合同，确保 guard 顺序在写入 role-menu 前。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; .\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests/source-product-library-contract.test.ts tests/source-warehouse-stock-contract.test.ts tests/product-center-contract.test.ts --runInBand`：通过，3 个 suite / 11 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal -- --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,integration -am "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=SqlExecutionGuardContractTest,IntegrationAdminRouteContractTest,IntegrationAdminPermissionContractTest" test`：通过，`ruoyi-system` 69 个测试、`integration` 6 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -am "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=SqlExecutionGuardContractTest" test`：通过，70 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
  - 前端 guard：4 个通过。
  - React typecheck：通过。
  - 前端 Jest：18 个 suite / 117 个测试通过。
  - 后端 reactor test-compile：14 个模块通过。
  - 后端三端合同测试：`ruoyi-system` 194、`ruoyi-framework` 16、`inventory` 1、`integration` 6、`product` 34、`seller` 96、`buyer` 97，均通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅输出既有 LF/CRLF 换行风格 warning。

## 验证备注

- 其中一次 SQL guard 定向测试因合同测试查找顺序过严失败，已按实际 SQL 顺序改为相邻片段断言后重新通过。
- 本轮未执行远程 MySQL DDL/DML，SQL 改动只做脚本和静态合同测试。

## 未执行

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图、DOM 检测或 UI 细调验收。

## 残留

- 本轮没有确认新的 P0/P1 残留。
- P2：历史若依 React 页面仍可能存在旧按钮权限命名与后端不完全贴合的问题；本轮仅按快速推进模式处理来源商品库、SQL seed guard 和 verifier 漏检。
