# 费用试算买家模拟视图实现记录

日期：2026-06-12

## 实现范围

本次在管理端 `费用试算` 页面增加 `买家模拟` 视图，并保留原有 `运营试算` 视图。

前端改动：

- 左侧条件面板顶部增加 `运营试算 / 买家模拟` Tabs。
- `运营试算` 保留原有发货仓、报价方案、物流渠道、包裹方式、目的地和结果区。
- `买家模拟` 左侧新增买家、商品/SKU、`仓库/渠道选择方式`。
- `仓库/渠道选择方式` 支持：
  - `手动指定`：展示 `选择仓库` 和 `选择客户渠道`。
  - `自动最优`：只展示 `限制仓库` 多选；不选表示全部候选仓库参与计算。
- `买家模拟` 右上增加 `候选解析` 看板。
- 结果表新增 `包裹尺寸`、`实重`、`体积重`、`计费重`。
- 结果表不再展示 `包裹数量` 主列。

后端改动：

- `FeeEstimateRequest` 增加：
  - `estimateView`
  - `warehouseCodes`
  - `customerChannelCode`
- `FeeEstimateResponse` 增加 `estimateView`。
- `FeeEstimateOptions` 增加 `customerChannels`，供买家模拟手动指定客户渠道使用。
- `FeeEstimateServiceImpl` 支持买家模拟视图：
  - 自动最优可接收多选限制仓库。
  - 手动指定按 `买家 + SKU + 指定仓库 + 指定客户渠道` 解析候选。
  - 手动指定和自动最优共用候选解析链路。
  - 外部费用字段未确认时继续 fail-closed，不伪造金额。

## 数据表影响

本次不新增表，不新增 SQL，不保存试算历史，不写订单费用快照，不写财务流水。

## 验证记录

已执行：

- `npm run tsc`
- `npx jest --config jest.config.js tests/finance-fee-estimate-contract.test.ts --runInBand`
- `mvn -pl ruoyi-system -am "-Dtest=FinanceAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl finance -am "-DskipTests" compile`
- `git diff --check`
- 浏览器运行态验证 `http://127.0.0.1:8001/finance/fee-estimate`

验证结论：

- 前端 TypeScript 通过。
- 费用试算前端契约测试通过。
- 管理端财务路由合同测试通过。
- finance 模块编译通过。
- `git diff --check` 未发现空白错误；当前工作区输出了既有 CRLF 提示。
- 浏览器中 `运营试算` 可见，结果表包含新增尺寸和重量列。
- 浏览器中 `买家模拟` 可见，左侧字段、候选解析看板和结果表可见。
- 浏览器中切换到 `自动最优` 后，只显示 `限制仓库`，不显示 `选择仓库` 和 `选择客户渠道`。

## 后续边界

- 真实买家费用和系统成本仍依赖外部费用试算适配器字段确认。
- 如果后续需要保存买家模拟历史、订单报价快照或下单锁价记录，需要另行提交数据表设计方案并确认。

## 2026-06-12 18:40 交互优化记录

本次按买家模拟视图优化商品选择方式：

- `买家模拟` 左侧 `商品/SKU` 不再使用大下拉选择。
- 改为 `选择商品` 按钮打开弹窗。
- 弹窗复用管理端 `ProTable + Modal + rowSelection` 的批量选择交互，参考商城商品 `批量选择来源 SKU` 的操作方式。
- 弹窗支持关键词分页查询 SKU，跨页勾选会保留在已选区。
- 点击 `确认选择` 后回填到左侧商品清单，数量仍在左侧清单中维护。
- `运营试算` 的右侧包裹 SKU 快速选择暂不改动。

数据影响：

- 不新增数据表。
- 不新增接口。
- 继续复用 `/api/finance/admin/fee-estimate/skus/list` 查询 SKU。

补充验证：

- `npm run tsc` 通过。
- `npx jest --config jest.config.js tests/finance-fee-estimate-contract.test.ts --runInBand` 通过。
- `mvn -pl ruoyi-system -am "-Dtest=FinanceAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- 浏览器运行态验证：买家模拟中点击 `选择商品` 可打开 `选择商品 SKU` 弹窗；表格加载 SKU；勾选后 `已选择 SKU` 和 `确认选择` 数量更新；确认后 SKU 回填左侧商品清单。

## 2026-06-12 18:50 来源仓交集限制记录

本次按买家模拟包裹约束补充 SKU 选择限制：

- 费用试算 SKU 快照新增 `sourceWarehouseCodes`，用于前端判断 SKU 可用来源仓 code。
- `买家模拟` 的 `选择商品 SKU` 弹窗会计算已选 SKU 的共同来源仓交集。
- 新勾选 SKU 时，必须和当前已选 SKU 保持至少一个共同来源仓。
- 没有共同来源仓的 SKU 行会在表格中禁选。
- 确认选择前和点击 `试算` 前都会二次校验共同来源仓，避免绕过 UI 产生不可试算组合。
- 弹窗已选区和左侧已选商品清单都会展示当前共同来源仓。

数据影响：

- 不新增数据表。
- 不新增接口。
- `/api/finance/admin/fee-estimate/skus/list` 的 SKU 行新增 `sourceWarehouseCodes` 返回字段。

补充验证：

- `npm run tsc` 通过。
- `npx jest --config jest.config.js tests/finance-fee-estimate-contract.test.ts --runInBand` 通过。
- `mvn -pl ruoyi-system -am "-Dtest=FinanceAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- `mvn -pl finance,product -am "-DskipTests" compile` 通过。
- `mvn -pl ruoyi-admin -am "-DskipTests" package` 通过；第一次打包因旧 8080 进程锁定 jar 失败，停止旧进程后重跑成功。
- 已通过 `start-backend-local.ps1 -Restart` 启动新版后端。
- 浏览器运行态验证：弹窗中来源仓 code 可见；选择 `US014` 来源仓 SKU 后，`CA333333`、`NY013` 等无交集 SKU 自动禁选；确认后左侧显示 `共同来源仓：US014`。

## 2026-06-12 19:05 SKU 弹窗筛选优化记录

本次按商品数量增长后的性能风险调整买家模拟商品选择弹窗：

- 删除 `选择商品 SKU` 弹窗里的宽泛 `关键词` 搜索字段。
- 弹窗查询改为三个明确筛选项：
  - `来源仓`
  - `SKU`
  - `商品名称`
- 前端请求 `/api/finance/admin/fee-estimate/skus/list` 时只提交 `sourceWarehouseCode`、`skuCode`、`productName`，不再提交 `keyword`。
- 后端费用试算 SKU 查询新增 `FeeEstimateSkuQuery`，由 Controller、Service、Product lookup port 传递结构化筛选条件。
- 商品 SKU Mapper 继续保留原有通用 `keyword` 能力给商品管理等页面使用；费用试算 SKU lookup 不再写入该字段。

数据影响：

- 不新增数据表。
- 不新增接口。
- 不改变已选 SKU 的共同来源仓交集限制。

补充验证：

- `npm run tsc` 通过。
- `npx jest --config jest.config.js tests/finance-fee-estimate-contract.test.ts --runInBand` 通过。
- `mvn -pl ruoyi-system -am "-Dtest=FinanceAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- `mvn -pl finance,product -am "-DskipTests" compile` 通过。

## 2026-06-12 19:15 SKU 弹窗可用库存列记录

本次按商品选择弹窗展示需求补充库存字段：

- `选择商品 SKU` 弹窗新增 `可用库存` 列。
- 库存口径使用平台库存的可用库存，即商品 SKU 列表已接入的 `availableStock`。
- 后端费用试算 SKU 快照新增 `availableStock` 字段。
- 商品 lookup 继续复用 `ProductDistributionMapper.selectSkuPageList` / `selectSkuById`，其库存来源是 `inventory_overview_sku_read_model.platform_available_qty as available_stock`。
- 只展示库存数量，不新增库存校验，不改变共同来源仓交集限制，也不改变包裹尺寸和费用试算计算逻辑。

数据影响：

- 不新增数据表。
- 不新增接口。
- 不写库存流水，不修改库存数量。

补充验证：

- `npx jest --config jest.config.js tests/finance-fee-estimate-contract.test.ts --runInBand` 通过。
- `mvn -pl ruoyi-system -am "-Dtest=FinanceAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- `mvn -pl finance,product -am "-DskipTests" compile` 通过。
- `mvn -pl ruoyi-admin -am "-DskipTests" package` 通过。
- 已通过 `start-backend-local.ps1 -Restart` 启动新版后端，`http://127.0.0.1:8080` 返回 200。
- 浏览器运行态验证：`http://127.0.0.1:8001/finance/fee-estimate` 的 `买家模拟` 中可打开 `选择商品 SKU` 弹窗；弹窗表头包含 `可用库存`；行内可展示 `0`、`1000` 等平台可用库存数值；未出现 `关键词` 筛选项。
- `git diff --check` 通过；仅输出当前工作区已有 LF/CRLF 转换提示。
- `codegraph sync .` 通过，最终同步 4 个变更文件。
- `npm run tsc` 当前被 `src/pages/Buyer/index.tsx`、`src/pages/Seller/index.tsx` 中直登 reason 参数类型阻塞；该阻塞不在本次费用试算改动范围内。
