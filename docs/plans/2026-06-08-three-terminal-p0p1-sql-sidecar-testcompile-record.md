# 三端独立 P0/P1 SQL 授权集合、登录 Sidecar 与 TestCompile 收口记录

日期：2026-06-08

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

## 范围

本轮继续按快速推进模式处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

未执行远程 MySQL DDL/DML，未读取或写入 Redis，未启动或重启后端。

## 子 Agent 使用

> 历史记录（已过期口径）：以下 GPT-5.3 优先尝试记录只描述当时执行情况；现行规则已按用户最新要求改为子 Agent 默认使用 `gpt-5.4`，不再优先使用 GPT-5.3 Codex。

- 按当时用户要求先尝试 6 个 GPT-5.3 Codex 子 Agent（`gpt-5.3-codex-spark`）。
- 平台返回额度限制，提示需要等到 2026-06-14 15:12 后再试；该批 6 个子 Agent 已关闭。
- 回退使用 6 个 `gpt-5.4` 只读子 Agent，覆盖：
  - seller/buyer 后端隔离
  - portal auth/session/direct-login/log
  - React runtime guard/sidecar
  - SQL guard/菜单授权
  - product/inventory/integration/warehouse
  - `verify-three-terminal` 门禁覆盖
- 6 个 `gpt-5.4` 子 Agent 均已关闭。

## 结论处理

- seller/buyer 后端隔离、portal auth/session/direct-login/log、`verify-three-terminal` 门禁覆盖：未发现新的 P0/P1。
- SQL guard 切片发现 1 个 P1 属实：`20260606_admin_partner_role_menu_grant.sql` 的预览集合按已有页面授权计算，但执行时会先补页面授权再补按钮授权，导致 expected count/signature 可能少算最终写入集合。
- React runtime 切片发现 1 个 P1 属实：管理端登录相关 `auth.js` / `login.js` 是独立请求实现，未被 admin sidecar 合同覆盖，存在与 TS 主实现漂移风险。
- product/inventory/integration/warehouse 切片发现 1 个 P1 属实：seller/buyer portal 商品单测替身未实现 `IProductDistributionService` 新增接口，clean test source 编译会失败。
- 本地复核额外发现 1 个 P1：修 SQL 授权集合后执行段临时出现重复 `join sys_menu child`，已删除并补合同约束。

## 已修复

### 管理端 Partner 菜单授权 SQL

- `20260606_admin_partner_role_menu_grant.sql`
  - 子按钮授权预览集合改为基于最终签名页面 `2011/2012` 计算，而不是依赖当前已存在的 `page_grant`。
  - 实际插入段同样基于 `2011/2012` 页面派生按钮授权，避免预览集合和执行集合漂移。
  - 删除重复 `join sys_menu child`。

- `SqlExecutionGuardContractTest`
  - 固定 role/menu grant 必须使用最终签名 partner 页面计算授权集合。
  - 禁止回退到 `join sys_role_menu page_grant`。
  - 固定 `page_menu` 和 `child` join 的出现次数，防止重复 join 或遗漏执行段。

- `AdminDirectLoginPermissionContractTest`
  - 将旧的 `page_grant` 断言更新为“只从签名页 `2011/2012` 派生按钮授权”。
  - 继续保持免密登录、票据审计权限与通用 admin 权限分离。

### 管理端登录 Sidecar

- `react-ui/src/services/system/auth.js` 改为纯 re-export：`export * from './auth.ts';`
- `react-ui/src/services/ant-design-pro/login.js` 改为纯 re-export：`export * from './login.ts';`
- `admin-auth-sidecar-contract.test.ts` 增加上述两个登录 request sidecar 的合同覆盖。

### Seller/Buyer Portal Product TestCompile

- `SellerPortalProductServiceImplTest` 和 `BuyerPortalProductServiceImplTest` 的 `RecordingProductDistributionService` 补齐：
  - `prepareReviewedProductUpdate(ProductSpu product)`
  - `applyReviewedProductUpdate(ProductSpu product)`
- 修复 clean test source 编译时接口漂移导致的 testCompile 失败。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=AdminDirectLoginPermissionContractTest,SqlExecutionGuardContractTest" test`
  - 通过，69 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am "-Dtest=SellerPortalProductServiceImplTest,BuyerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，seller 8 个测试，buyer 9 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/admin-auth-sidecar-contract.test.ts --runInBand`
  - 通过，1 个 suite / 7 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 前端：15 个 Jest suite / 103 个测试通过，React typecheck 通过，4 个前端 guard 通过。
  - 后端：reactor test-compile 通过；ruoyi-system 192、ruoyi-framework 16、inventory 1、integration 6、product 27、seller 96、buyer 97 个测试通过。

## 未执行

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图、DOM 或 UI 细调验收。

## AGENTS 规则复核

- 本轮没有新增架构方向或协作规则，只是按既有三端隔离、快速推进、子 Agent 优先级和 Markdown 记录规则执行。
- 因此未修改 `AGENTS.md`。

## 残留

- 本轮未发现新的 P0/P1 残留。
- `npm run verify:three-terminal` 输出中仍有 Umi 提示文本编码显示异常，这是终端输出显示问题，不影响命令退出码和验证结果，本轮不作为 P0/P1 处理。
