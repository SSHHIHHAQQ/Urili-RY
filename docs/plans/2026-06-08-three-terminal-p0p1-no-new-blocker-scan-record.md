# 2026-06-08 三端 P0/P1 快速推进：未发现新阻塞项扫描记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。

当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 情况

- 历史记录（已过期口径）：按当时旧规则曾优先尝试 GPT-5.3 Codex，工具模型为 `gpt-5.3-codex-spark`；本轮 6 个已启动 Agent 均返回额度限制，提示需等到 `2026-06-14 15:12` 后再试，均已关闭。现行规则见 `AGENTS.md` 和目标追踪现行口径索引，默认使用 `gpt-5.4`。
- 随后按当时 fallback 规则启动并关闭 6 个 `gpt-5.4` 只读子 Agent。
- 6 个 `gpt-5.4` 切片覆盖：
  - seller/buyer 后端 admin 与 portal 权限链路。
  - SQL seeds/migrations。
  - React 三端请求、401、路由权限、JS/TS 镜像和 direct-login。
  - product/inventory/warehouse/integration 近期开口。
  - seller/buyer 端内 role/menu/dept 运行时隔离。
  - 文档、AGENTS、复用台账和当前代码规则冲突。
- 子 Agent 与主 Agent 均未发现新的确定 P0/P1。

## 本轮结论

- 没有发现需要本轮修复的确定 P0/P1。
- 不做业务代码改动。
- 当前三端快速验证入口仍通过。

## 已核查范围

- 后端 seller/buyer：
  - 管理端 controller 权限、session/list 与 forceLogout 分权。
  - portal 自助日志/session DTO 脱敏边界。
  - `sellerId/buyerId + accountId` 账号查询边界。
  - 管理端日志按账号筛选时必须带主体 ID。
  - portal 权限前缀 fail-closed。
- SQL：
  - confirm token、`45000` fail-closed、target guard。
  - seller/buyer 菜单 ID range。
  - role-menu 授权范围。
  - owner role 与终端菜单 seed。
- React：
  - 空 `authority` 不 fail-open。
  - admin / portal 401 不串端。
  - seller/buyer service baseUrl 不串端。
  - portal direct-login 和 portal token guard。
- product/inventory/integration：
  - seller portal 商品 scope。
  - buyer portal 公共在售目录口径。
  - buyer portal product schema 端口绑定。
  - 库存总览、来源商品、来源仓库库存和上游系统管理的 admin route / 权限合同。
- 文档与规则：
  - AGENTS 当前三端独立方向与子 Agent 模型规则未发现 P0/P1 级冲突。
  - 卖家/买家管理页仍复用同一 `PartnerManagementPage` 模板配置。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
  - 4 个前端 guard 通过。
  - React typecheck 通过。
  - 12 个 Jest suite / 66 个测试通过。
  - 后端 reactor `test-compile` 通过。
  - 后端三端合同测试通过：`ruoyi-system` 181 个、`ruoyi-framework` 16 个、`integration` 5 个、`product` 17 个、`seller` 96 个、`buyer` 97 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；CodeGraph 返回 `Already up to date`。

子 Agent 补充验证也均通过，包括 seller/buyer 权限与部门服务测试、SQL guard 相关合同测试、React 401/portal 请求测试、product/inventory/integration 编译与前端定向 Jest。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮没有核对远端运行库 live 数据是否存在历史脏值。

## P2 记录

- SQL 自动发现目前只扫描 `RuoYi-Vue/sql` 顶层 dated 文件；如果未来 dated/bootstrap SQL 放入子目录会漏网。
- 非 dated 高影响 SQL 仍依赖 `SqlExecutionGuardContractTest` 手工枚举 `assertGuard(...)`。
- `app.tsx` 与 `requestErrorConfig.ts` 的 401 分流逻辑重复维护，当前行为一致，但后续存在漂移风险。
- `react-ui/src/services/seller/seller.js` 和 `react-ui/src/services/buyer/buyer.js` 仍是完整 JS 镜像，不是纯 re-export，当前未发现错端 URL。
- `Inventory/Overview`、`SourceProductLibrary`、`SourceWarehouseStock` 的前端专属 guard/测试粒度偏粗，当前主要依赖跨层合同测试兜底。
- 少量历史 Markdown 仍残留旧 `sys_user` / `PortalAccountSupport` / 本地数据源口径；当前代码和 AGENTS 已以三端独立方向为准。
- seller/buyer service 中仍有少量英文 `ServiceException` 文案，属于提示文案一致性问题。
