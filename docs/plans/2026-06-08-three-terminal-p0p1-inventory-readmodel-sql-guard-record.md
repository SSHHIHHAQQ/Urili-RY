# 2026-06-08 三端隔离 P0/P1 快速推进记录：商品库存读模型与旧 SQL guard

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 历史记录（已过期口径）：优先尝试 6 个 GPT-5.3 Codex 子 Agent，工具模型为 `gpt-5.3-codex-spark`。
- 平台返回额度限制：需要等到 `2026-06-14 15:12` 后再试；6 个 GPT-5.3 Codex 子 Agent 均已关闭。
- 按回退规则启动 6 个 `gpt-5.4` 只读子 Agent，覆盖 seller/buyer 后端、Portal 鉴权和 direct-login、SQL guard、React 运行入口、product/inventory/integration 共享域、验证闸门。
- 6 个 `gpt-5.4` 子 Agent 均已返回并关闭。

## 采纳的 P1

- 商品分销列表和详情接口已经暴露 `availableStock`、`warehouseCount`、`inventoryStatus`、`stockUpdateTime` 字段，但 `ProductDistributionMapper.xml` 仍输出 `null as available_stock` 等占位；当前库存 overview read model 已存在，应接入读模型而不是继续返回占位。
- `20260606_upstream_inventory_dimension_sync.sql` 仍用 `LIMIT 1` 选 `sys_job`，并且按既有 upstream 权限派生写 `sys_role_menu` 时缺少 preview-confirmed count/signature guard。
- `20260606_upstream_sync_staging_diff.sql` 的多段 `sys_job` upsert 仍保留 `LIMIT 1` 和 `@job_id` 单行选取，缺少 exact target guard。
- `react-ui/src/app.js` 不是纯 re-export，可能绕过受保护的 `app.tsx` 运行入口；`verify:three-terminal` 已将其拦截为 P1。

## 已完成

- `ProductDistributionMapper.xml` 新增 SPU/SKU 库存汇总 SQL 片段，商品 SPU 查询接 `inventory_overview_spu_read_model`，SKU 查询接 `inventory_overview_sku_read_model`。
- `ProductDistributionMapperContractTest` 从“库存字段必须是 null 占位”调整为“库存字段必须来自 overview read model，且禁止直接读取库存事实源表”。
- `20260606_upstream_inventory_dimension_sync.sql` 增加 `sys_role_menu` 目标 count/signature guard、`sys_job` 唯一性和 count/signature guard，并把 role-menu/job DML 放入事务；移除 `LIMIT 1` 和 `@job_id`。
- `20260606_upstream_sync_staging_diff.sql` 增加 `sys_job` count/signature guard、分组唯一性 guard、组件拆分后禁止回放 guard，并把 job DML 放入事务；移除 `LIMIT 1` 和 `@job_id`。
- `SqlExecutionGuardContractTest` 增加两条合同，固定上述两个旧 SQL 的 fail-closed 规则。
- `react-ui/src/app.js` 改回 `export * from './app.tsx';`，避免 JS 镜像覆盖 TSX 入口。

## 验证结果

- 历史记录（已过期命令口径）：`cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product "-Dtest=ProductDistributionMapperContractTest" test`：当时通过，8 个测试；当前按复用台账的窄范围 Maven 模板，`product` / `integration` / `seller` / `buyer` 这类存在 reactor 内部依赖的模块复核应带 `-am`，例如 `mvn -pl product -am "-Dtest=ProductDistributionMapperContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，62 个测试。
- 历史记录（已过期命令口径）：`cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product "-Dtest=ProductDistributionServiceImplTest,ProductPortalSchemaServiceImplTest,ProductDistributionMapperContractTest" test`：当时通过，16 个测试；当前复核应使用带 `-am` 的 reactor 命令并按需加 `-Dsurefire.failIfNoSpecifiedTests=false`。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；4 个前端 guard、React typecheck、13 个 Jest suite / 67 个测试、后端 reactor `test-compile` 和后端三端合同测试全部通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，仅有 LF/CRLF 工作区换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，首次返回 `Synced 4 changed files`。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 当前残留项

- P2：`ProductDistributionMapper` 现在读取库存 overview read model，但 read model 的刷新时机仍依赖库存模块既有刷新链路；后续如果要做到强实时库存，需要单独设计刷新触发。
- P2：两份旧 20260606 SQL 现在会在组件拆分后 fail-closed，避免回放重新写入旧 job；如需在已拆分库上补跑旧脚本，必须重新出专门迁移方案。
- P2：工作区仍有较多历史 `.js` 镜像和未跟踪生成文件；当前只收敛验证点名且影响 P0/P1 的入口。
