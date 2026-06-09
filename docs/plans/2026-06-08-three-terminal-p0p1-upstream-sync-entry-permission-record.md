# 2026-06-08 三端 P0/P1 快速推进：上游同步入口权限收口记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。

当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 情况

- 历史记录（已过期口径）：按用户最新规则优先尝试 6 个 GPT-5.3 Codex 子 Agent，工具模型为 `gpt-5.3-codex-spark`；平台返回额度限制，提示需等到 `2026-06-14 15:12` 后再试，6 个失败 Agent 已关闭。
- 按 fallback 规则启动并关闭 6 个 `gpt-5.4` 只读子 Agent。
- 6 个 fallback 切片覆盖：
  - seller/buyer 账号、角色、部门、菜单后端边界。
  - Portal 鉴权、token/session、direct-login、登录/操作日志。
  - SQL seeds/migrations 和 SQL guard。
  - React 运行入口、路由、request/proxy/access/session、JS/TS 镜像。
  - product/inventory/integration/shared domain 三端边界。
  - 验证入口与测试覆盖。
- 采纳的 P1：上游系统“同步”入口按钮只检查 `integration:upstream:sync`，导致仅具备 `integration:upstream:dimensionSync` 或 `integration:upstream:inventorySync` 的合法角色无法打开同步弹窗；后端 `/sync` 与页面内部同步项过滤已经允许三类同步权限任一。

## 已完成

- `react-ui/src/pages/UpstreamSystem/components/ConnectionSummary.tsx` 新增 `manualSyncEntryPermissions`，同步入口按 `integration:upstream:sync`、`integration:upstream:dimensionSync`、`integration:upstream:inventorySync` 任一权限可见。
- 同步更新 `react-ui/src/pages/UpstreamSystem/components/ConnectionSummary.js` 镜像，避免运行入口漂移。
- `react-ui/tests/upstream-system-permission-guard.test.ts` 增加契约断言，固定同步弹窗入口必须覆盖三类手动同步权限，同时继续检查同步项内部按 `syncTypeOptions` 权限过滤。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts tests/upstream-system-permission-guard.test.ts --runInBand`：通过，1 个 suite / 4 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node --check src\pages\UpstreamSystem\components\ConnectionSummary.js`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
  - 4 个前端 guard 通过。
  - React typecheck 通过。
  - 12 个 Jest suite / 66 个测试通过。
  - 后端 reactor `test-compile` 通过。
  - 后端三端合同测试通过：`ruoyi-system` 181 个、`ruoyi-framework` 16 个、`integration` 5 个、`product` 17 个、`seller` 96 个、`buyer` 97 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮没有核对远端运行库 live 数据是否存在历史脏值。

## P2 记录

- direct-login 前端桥接消费页 5 秒超时、opener 侧 15 秒超时，可能出现慢启动时的短暂假失败；建议后续抽共用超时常量。
- `app.tsx` 与 `requestErrorConfig.ts` 仍重复维护 401 分流和登录跳转逻辑，当前行为一致但后续有漂移风险。
- `react-ui/src/services/seller/seller.js` 和 `react-ui/src/services/buyer/buyer.js` 仍是完整 JS 镜像，不是纯 re-export；当前 guard 已校验关键 URL。
- `verify-three-terminal` 后端 reactor 会纳入存在 `src/test/java` 的模块，但收尾只验证 manifest 中的 surefire XML；`finance` 这类模块可能显示 `No tests to run` 后整体通过。
- SQL guard 对动态 DDL 的自动发现模式偏窄，当前未发现绕过脚本；后续可把 `prepare stmt from @...` 变量链路纳入识别。
- `20260606_upstream_inventory_dimension_sync.sql` 的 `sys_role_menu` 授权仍按已有权限继承，没有像专用 grant SQL 那样做精确 role/grant count 与 signature 预确认。
- terminal permission seed 的 owner-role 约束仍依赖显式清单，通用 auto-discovery 尚未强制所有新增 terminal permission seed 都只能授 owner。
- 商品分销编辑页进入/保存门槛未完整覆盖仓库与来源 SKU 依赖权限；当前表现为缺权限时流程不可用，不属于本轮串端或接口 P1。
- `ruoyi-system` 的 `IntegrationAdminRouteContractTest` 未覆盖 `integration:upstream:credential`；integration 模块自身 `IntegrationAdminPermissionContractTest` 已覆盖。
