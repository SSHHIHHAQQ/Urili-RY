# 库存总览平台库存基础版实施记录

日期：2026-06-07

状态：代码、基础增量 SQL、后端重启和浏览器基础验证已完成。2026-06-08 已执行 SKU 基线读模型刷新，当前商城 SKU 已进入库存总览。

## 本次目标

落地库存总览基础版，以“平台总库存”为平台当前库存池字段，支持 SPU / SKU 两种视图，并在 SKU + 仓库明细行上双击调整平台总库存和平台在途库存。

## 已完成

- 新增后端 `inventory` Maven 模块，并接入 `ruoyi-admin` 聚合编译。
- 新增库存总览增量 SQL：`RuoYi-Vue/sql/20260607_inventory_overview_platform_stock.sql`。
- 新增库存总览读模型修正 SQL：`RuoYi-Vue/sql/20260608_inventory_overview_sku_baseline_refresh.sql`。
- 新增库存总览核心表、流水表、锁定表、来源扣减待抵消表、在途跟踪表，以及 SKU/SPU 永久读模型表。
- 新增后端接口：`/inventory/admin/overview/**`。
- 新增前端页面：`react-ui/src/pages/Inventory/Overview`。
- 通用 `business_menu_seed.sql` 不再拥有 `2420` 库存总览菜单，避免通用 seed 覆盖专用菜单。
- 库存总览页面使用 Ant Design Pro 原生 ProTable 搜索区，不额外做独立筛选区。
- 前端已拆为主页面、SKU 仓库展开表、双击编辑单元格和展示 helper，避免单文件继续膨胀。
- 读模型刷新已调整为从商城 SKU 起步，来源绑定和仓库绑定缺失时生成占位库存行并显示状态。
- 新增库存状态：`仓库未配置`、`来源SKU未绑定`。
- 复用台账已追加库存总览平台库存模板。
- 设计文档状态已更新为“已确认并进入基础版实现”。

## 核心规则

- 官方仓库存来自来源仓库库存正品综合库存，按来源主仓展示，例如 `CA012`。
- 页面不展示上游系统名或外部接入编码作为官方仓明细主标识。
- 官方仓平台可售库存按 `min(平台总库存, 来源可用库存 - 待抵消来源扣减) - 平台锁定库存` 计算。
- 三方仓平台可售库存按 `平台总库存 - 平台锁定库存` 计算。
- 库存总览不再以来源 SKU 绑定或仓库绑定作为展示前提；有效商城 SKU 必须进入读模型。
- SKU + 仓库明细仍是最小库存事实维度；多个官方来源主仓或多个三方仓都会生成多条明细行。
- 未配置仓库的占位行不允许手工调整平台库存。
- 平台总库存不能调整到小于当前平台锁定库存。
- 三方仓不支持设置平台在途库存。
- 库存调整先 preview，再 confirm，落 `inventory_stock_ledger`。

## 权限检查

- 后端接口已加权限：
  - `inventory:overview:list`
  - `inventory:overview:query`
  - `inventory:overview:adjust`
  - `inventory:overview:ledger`
- 增量 SQL 专用维护 `2420` 菜单和按钮权限。
- 前端双击编辑会读取 `access.hasPerms('inventory:overview:adjust')`，无权限时不可编辑。
- `SqlExecutionGuardContractTest` 已增加库存总览 SQL guard 和通用菜单 seed 不抢占库存总览菜单的合同检查。

## 字典与选项

- 新增 `inventory_status` 字典。
- 追加 `NO_WAREHOUSE`、`SOURCE_UNBOUND` 库存状态。
- 新增 `inventory_operation_type` 字典。
- 前端状态展示使用集中 helper，未在页面内散落多份状态映射。

## 复用与重复代码

- 已更新 `docs/architecture/reuse-ledger.md`。
- 前端通用展示方法已拆到 `helpers.tsx`。
- 双击编辑单元格独立为 `components/QuantityCell.tsx`。
- SKU 仓库展开表独立为 `components/SkuWarehouseTable.tsx`。

## 大文件检查

- `react-ui/src/pages/Inventory/Overview/index.tsx`：291 行，已拆分到可接受范围。
- `InventoryOverviewServiceImpl.java`：新增少量状态判断后仍职责集中在库存总览查询、预览、确认和重算，暂不拆分。
- `20260607_inventory_overview_platform_stock.sql`：701 行，包含同一批库存表、菜单权限、字典、回填和 guard。作为一次性增量脚本保持单文件，避免拆分后执行顺序和确认 token 分散。
- `20260608_inventory_overview_sku_baseline_refresh.sql`：只做字典补充、库存承载行补齐和读模型刷新，不新增表结构。
- `2026-06-07-inventory-overview-platform-stock-design.md`：618 行，是完整设计记录，不按源码拆分。

## 验证命令

| 命令 | 结果 |
| --- | --- |
| `mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test` | 通过，38 个测试成功 |
| `java ... UriliSqlRunner .env.local RuoYi-Vue/sql/20260607_inventory_overview_platform_stock.sql` | 通过，执行 54 条 SQL 语句 |
| `jshell ... RuoYi-Vue/sql/20260608_inventory_overview_sku_baseline_refresh.sql` | 通过，执行 30 条 SQL 语句 |
| `mvn -pl ruoyi-admin -am -DskipTests '-Dspring-boot.repackage.skip=true' package` | 通过 |
| `mvn -pl ruoyi-admin -am -DskipTests package` | 第二次通过，第一次因旧后端进程锁定 jar 在 repackage 阶段失败 |
| `npm run tsc -- --pretty false` | 通过 |
| `npx biome lint src\pages\Inventory\Overview\index.tsx src\pages\Inventory\Overview\helpers.tsx src\pages\Inventory\Overview\components\QuantityCell.tsx src\pages\Inventory\Overview\components\SkuWarehouseTable.tsx src\services\inventory\overview.ts src\types\inventory\overview.d.ts` | 通过 |
| `git diff --check` | 无空白错误；仅有既有 CRLF/LF 提示 |
| `codegraph sync .` | 通过；代码同步时 `Synced 8 changed files`，补写记录后 `Synced 1 changed files`，最终复查 `Already up to date` |
| `.\start-backend-local.ps1 -Restart` | 通过，8080 已启动 |
| `GET /inventory/admin/overview/spu/list` | 登录后返回 `code=200,total=24`，首行状态 `NO_WAREHOUSE` |
| `GET /inventory/admin/overview/sku/list` | 登录后返回 `code=200,total=69`，首行状态 `NO_WAREHOUSE` |
| `GET /inventory/admin/overview/sku/{skuId}/warehouses` | 登录后返回 `code=200`，首个 SKU 有 1 条 `NO_WAREHOUSE` 明细 |
| 浏览器打开 `http://127.0.0.1:8001/inventory/overview` | 通过，SPU/SKU 视图均显示数据，控制台无错误 |

## 未验证项

- 当前库存读模型已生成 24 个 SPU、69 个 SKU，状态均为 `仓库未配置`。这是当前运行库尚无 `product_sku_source_binding` 和 `product_spu_warehouse` 数据的预期结果。
- 前端 dev server 使用 `DISABLE_MFSU=1` 启动验证；默认 MFSU 缓存启动存在同路径输出冲突，需要后续单独清理前端缓存策略。

## 下一步

1. 商品侧后续需要补齐发货仓/来源 SKU 配置流程，或在商品创建时明确官方仓与三方仓归属。
2. 后续接入订单占用和出库扣减逻辑，落地 `inventory_reservation` 和来源扣减待抵消流程。
3. 单独处理前端 MFSU 缓存冲突，恢复默认 dev server 启动方式。
