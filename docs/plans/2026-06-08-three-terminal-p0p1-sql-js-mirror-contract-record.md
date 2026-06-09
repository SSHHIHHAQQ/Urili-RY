# 2026-06-08 三端隔离 P0/P1 快速推进：SQL 目标锁定与 JS 镜像契约记录

本记录以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 执行情况

- 历史记录（已过期口径）：按最新规则优先尝试 6 个 GPT-5.3 Codex 子 Agent，工具模型为 `gpt-5.3-codex-spark`。
- 平台返回额度限制：`You've hit your usage limit for GPT-5.3-Codex-Spark`，提示到 `2026-06-14 15:12` 后再试；6 个 GPT-5.3 失败 Agent 均已关闭。
- 按 fallback 规则启动 6 个 `gpt-5.4` 只读子 Agent，全部已关闭。
- 6 个 fallback 切片覆盖 seller/buyer 后端、Portal 鉴权和 direct-login、SQL guard、React 运行入口、product/inventory/integration 共享域、验证闸门。
- 采纳 SQL guard 切片发现的 4 个 P1；其余切片未发现确定 P0/P1。

## 采纳的 P1

1. `20260605_product_config_change_log.sql` 只有建表语义，缺少已运行库 schema/index 漂移断言；历史回填 `update` 缺少精确目标数量、目标签名和事务保护。
2. `20260604_currency_rate_sync_job.sql` 对 `sys_job` 使用 `LIMIT 1` 选目标，缺少唯一性、数量和签名断言；同步配置更新也缺少精确目标校验。
3. `20260605_upstream_sku_sync_job.sql` 对 `sys_job` 使用 `LIMIT 1` 选目标，缺少唯一性、数量和签名断言。
4. React 多处 `.js` 镜像是编译副本或漂移文件，可能覆盖 `.ts/.tsx` 真实源码，影响三端 token、权限、路由和 portal 商品页运行入口。
5. `verify-three-terminal.mjs` 在 Windows 下会把同名 `.test.js` 生成镜像当成未登记关键测试，导致 manifest 自检误判。
6. 后端 Java 契约仍要求 `PartnerManagement` 的 `.js` 镜像复制完整 TSX 逻辑，与前端 guard/Jest 的“JS 只做 re-export”规则冲突。

## 已完成

- `20260605_product_config_change_log.sql` 增加 schema、index、回填目标数量和签名断言；4 段历史回填纳入同一事务。
- `20260604_currency_rate_sync_job.sql` 增加 `sys_job` 与 `finance_currency_sync_config` 的 count/signature guard，移除 `@job_id` / `LIMIT 1` 单行选取。
- `20260605_upstream_sku_sync_job.sql` 增加 `sys_job` count/signature guard，移除 `@job_id` / `LIMIT 1` 单行选取。
- `SqlExecutionGuardContractTest` 增加 3 个 SQL 合同测试，固定上述迁移必须 fail-closed。
- 恢复三端运行入口、PartnerManagement、portal 商品页、Product、UpstreamSystem 等 JS 镜像为纯 re-export。
- `PartnerAuditModal.js` 同步导出 `buildAuditParams`，满足 Jest 对审计参数构造的命名导出契约。
- `check-partner-management-template.mjs` 改为验证 TSX 业务源和 JS re-export 契约，避免 JS 复制 TSX 逻辑。
- `verify-three-terminal.mjs` 忽略有同名 TS/TSX 源的 `.test.js` 生成镜像，不再把它们当成 manifest 漏项。
- `AdminAccountPermissionUiContractTest`、`AdminDirectLoginPermissionContractTest` 改为检查 TSX 业务源，JS 只检查 re-export 契约。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node scripts\check-portal-token-isolation.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，60 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests\partner-audit-modal.test.ts tests\product-distribution-permission-guard.test.ts tests\upstream-system-permission-guard.test.ts --runInBand`：通过，3 个 suite / 12 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=AdminAccountPermissionUiContractTest,AdminDirectLoginPermissionContractTest" test`：通过，2 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；4 个前端 guard、React typecheck、13 个 Jest suite / 67 个测试、后端 reactor `test-compile` 和后端三端合同测试全部通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；CodeGraph 返回 `Synced 57 changed files`。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- 历史记录（已过期口径）：当时 `AGENTS.md` 包含 GPT-5.3 Codex 优先、不可用回退 `gpt-5.4`、子 Agent 用完关闭和记录要求；现行规则已改为默认使用 `gpt-5.4`。

## 当前残留项

- P2：工作区仍有较多历史 `.js` 镜像和未跟踪生成文件，当前只修验证点名且影响 P0/P1 的入口。
- P2：`seller.js` / `buyer.js` 仍是完整 service 镜像，当前 guard 只校验关键 URL 和权限串。
- P2：SQL guard 对动态 DDL 变量链路仍可继续加强，本轮只收敛已确认目标脚本。
- P2：`verify-three-terminal` 允许同名 `.test.js` 生成镜像存在但不纳入 manifest，后续可统一清理生成副本。
