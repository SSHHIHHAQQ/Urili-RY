# 三端快速推进 P0/P1 管理端登录与 SQL 角色目标收口记录

日期：2026-06-08

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

## 范围

本轮继续按三端独立方向推进，只处理当前 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态、截图、DOM 检测或 UI 细调。

## 子 Agent

- 按本次目标追踪 continuation 中的要求，直接使用 6 个 `gpt-5.4` 只读子 Agent。
- 6 个切片分别覆盖：seller/buyer 账号权限隔离、portal direct-login/session/log、SQL seed/guard、React runtime guard/sidecar、product/inventory/integration/warehouse、验证 manifest/gate。
- 6 个子 Agent 均已关闭；主线程只采纳可复核的 P0/P1 结论。

## 已修复

### P0：ProductCenter 预览属性类型不兼容

- 文件：`react-ui/src/components/ProductCenter/ProductCenterDetailModal.tsx`
- 问题：`API.ProductCenter.Attribute[]` 中 `label` 可为空，直接传给 `BuyerPreviewAttribute[]` 导致 React typecheck 失败。
- 修复：新增 `toPreviewAttributes(...)`，过滤空 label 并收窄成预览组件需要的必填 `label`。

### P1：管理端登录页残留旧 Ant Design Pro mock 登录接口

- 文件：
  - `react-ui/src/pages/User/Login/index.tsx`
  - `react-ui/src/services/ant-design-pro/login.ts`
  - `react-ui/src/services/system/auth.ts`
  - `react-ui/tests/admin-auth-sidecar-contract.test.ts`
- 问题：手机号登录 tab 仍调用 `getFakeCaptcha`，旧 service 仍指向 `/api/login/account`、`/api/login/captcha`、`/api/login/outLogin`，这些不是当前若依后端契约。
- 修复：
  - 移除无真实后端契约的手机号登录 tab。
  - `ant-design-pro/login.ts` 改为兼容导出真实若依 auth：`login` / `logout as outLogin`。
  - 删除 `system/auth.ts` 中不可用的 `getMobileCaptcha(...)`。
  - 增加 Jest 合同，禁止登录相关源码重新引用 legacy mock auth 路径和 helper。

### P1：admin partner role-menu grant 目标不够精确

- 文件：
  - `RuoYi-Vue/sql/20260606_admin_partner_role_menu_grant.sql`
  - `RuoYi-Vue/sql/20260606_legacy_disable_sys_seller_buyer_roles.sql`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminDirectLoginPermissionContractTest.java`
- 问题：
  - `admin` 角色授权目标只过滤 `del_flag='0'`，会把停用 admin 角色纳入授权集合。
  - `role_ids` 中不存在的 ID 会被静默忽略，无法证明输入集合和命中集合完全一致。
- 修复：
  - `admin` grant 只认 `role_key='admin' and status='0' and del_flag='0'`。
  - `admin` grant 和 legacy sys_role cleanup 都增加输入 ID 数量与合法命中数量比对，不一致直接 `45000` fail-closed。
  - 合同测试固定启用态过滤、输入集合完整匹配、错误文案和执行段目标一致性。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/admin-auth-sidecar-contract.test.ts --runInBand`：通过，1 个 suite / 8 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am -DskipTests test-compile`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest,AdminDirectLoginPermissionContractTest" test`：通过，69 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
  - 前端 guard：4 个通过。
  - React typecheck：通过。
  - 前端 Jest：16 个 suite / 108 个测试通过。
  - 后端 reactor test-compile：14 个模块通过。
  - 后端三端合同测试：`ruoyi-system` 192、`ruoyi-framework` 16、`inventory` 1、`integration` 6、`product` 34、`seller` 96、`buyer` 97，均通过。

## 未执行

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图、DOM 检测或 UI 细调验收。

## 残留

- 本轮没有确认新的 P0/P1 残留。
- P2：`npm run verify:three-terminal` 仍有 Umi 提示文本编码显示异常，以及 Jest open handle 提示；命令退出码为 0，三端 gate 已判定通过，本轮不阻塞。
- 流程建议：如果未来再次出现“源码存在但增量编译/合同报旧错误”，优先对相关 Maven 模块补一轮 `clean` 复验；本轮当前总 gate 已通过，未改 verify 脚本。
