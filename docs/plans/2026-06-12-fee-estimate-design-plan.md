# 费用试算菜单与功能方案草案

日期：2026-06-12

## 背景

本次需求是在管理端 `财务管理` 的第一个位置新增 `费用试算` 菜单。

费用试算用于让运营选择商品 SKU，或手工输入包裹长宽高和重量，再结合发货地、目的地、报价方案，计算每个收费项。这个页面也用于验证系统计费链路是否按预期工作。

截图只作为功能启发，不复制视觉样式。可以吸收的结构是：

- 左侧集中输入试算条件。
- 中上区域维护 SKU / 包裹明细。
- 结果区展示各渠道或各收费项的试算结果。
- 支持选择商品，也支持手工输入尺寸重量。

## 已确认口径

2026-06-12 沟通确认：

- 第一版只做管理端测试工具，不保存试算历史。
- 渠道支持单选、多选和不选；不选渠道等于展示当前报价方案下全部可试算渠道。
- 目的地第一版页面只展示国家、州、城市、邮编；不展示住宅地址字段。
- 如果领星费用试算接口要求住宅地址字段，后端对接领星时默认按住宅地址传。
- 报价方案支持 `BILLING` 计费方案和 `COST` 成本方案两类。
- 第一版要接真实外部试算来源；如果真实来源未配置或调用失败，必须返回明确失败行，不允许用前端 mock 伪造费用。
- `手工尺寸` 和 `选择 SKU` 两种输入方式互斥，同一次试算只能使用一种。
- 多 SKU 第一版合并成一个包裹试算，包裹尺寸按“最小边累加、次长边取最大、最长边取最大”的规则计算。
- 当前没有商城确认尺寸字段，SKU 试算暂时使用来源商品的仓库测量尺寸；未来商城列表补齐商城尺寸字段后要切换到商城尺寸。

本文里的“手工尺寸”指不从商品 SKU 读取尺寸，而是在费用试算页面直接录入包裹长、宽、高、实重和数量。这个输入只用于本次试算，不回写商品主数据。

## 当前代码逻辑

### 财务菜单

当前顶级 `财务管理` 菜单 ID 是 `2050`，路径是 `finance`。

当前 `business_menu_seed.sql` 已在 `2050` 下放了若干二级占位菜单：

- `2430` 资金账户，order `5`
- `2431` 收款账户，order `10`
- `2432` 分销资金，order `15`
- `2433` 费用管理，order `20`
- `2434` 利润对账，order `25`

`币种配置` 是已实现的真实财务页：

- 菜单 ID：`2442`
- 父菜单：`2050`
- order：`30`
- 页面：`Finance/Currency/index`
- 权限：`finance:currency:*`

`报价方案` 虽然后端归属 `finance` 模块，但当前菜单位是 `2053`，父级仍是 `2030` 海外仓服务设置，页面为 `Finance/QuoteScheme/index`，权限为 `finance:quoteScheme:*`。

结论：`费用试算` 应新增独立财务菜单，不应复用 `2053` 报价方案菜单位。为了成为财务管理第一个位置，建议使用小于 `5` 的 `order_num`，例如 `1`。

### 报价方案

当前报价方案阶段一已经落地：

- 后端接口：`/finance/admin/quote-schemes`
- 主要表：`quote_scheme`、`quote_scheme_scope`、`quote_scheme_warehouse`、`quote_scheme_channel`
- 已有配置：方案类型、费用来源模式、币种、适用买家、适用仓库、客户渠道
- 费用项占位：`operation_fee_code`、`freight_fee_code`
- 增值费表：`quote_scheme_value_fee_rule`

当前边界很明确：报价方案是配置入口，不是完整计费引擎。`quote_scheme_channel` 里的操作费、运费字段目前还是占位引用，不代表本地运费公式和操作费公式已经完成。

### 商品 SKU 与尺寸重量

商品 SKU 已有可复用字段：

- `systemSkuCode`
- `sellerSkuCode`
- `lengthValue`
- `widthValue`
- `heightValue`
- `weight`
- `packageQuantity`
- 来源绑定补充字段：`measureLengthCm`、`measureWidthCm`、`measureHeightCm`、`measureWeightKg`

当前未发现独立命名为“确认尺寸”或“商城尺寸”的字段。第一版费用试算先临时使用来源商品的仓库测量尺寸：`measureLengthCm`、`measureWidthCm`、`measureHeightCm`、`measureWeightKg`。后续商城列表补齐商城尺寸字段后，费用试算的 SKU 尺寸来源必须切换到商城尺寸，不能长期依赖来源仓库测量尺寸。

管理端已有 SKU 列表接口：

- `GET /product/admin/distribution-products/skus/list`
- 权限：`product:distribution:list`

该接口可作为第一版选择 SKU 的数据来源，但 `finance` 后端不能直接依赖 product Mapper。若后端试算需要读取 SKU 尺寸，建议新增 finance 侧 lookup port，由 product 模块实现只读查询。

SKU 模式尺寸口径：

1. 当前临时使用来源商品的仓库测量尺寸：`measureLengthCm`、`measureWidthCm`、`measureHeightCm`、`measureWeightKg`。
2. 如果来源仓库测量尺寸完整且可解析为正数，SKU 可参与试算。
3. 如果来源仓库测量尺寸缺失，页面提示该 SKU 缺少可试算尺寸；用户需要先同步/补齐来源尺寸，或切换到手工尺寸模式。
4. 后续商城尺寸字段落地后，优先级改为只使用商城尺寸；来源仓库测量尺寸最多作为参考展示。
5. SKU 模式下不手工改 SKU 尺寸；如果本次不想使用 SKU 尺寸，应切换到手工尺寸模式。

### 合并包裹尺寸口径

多 SKU 第一版合并成一个包裹。每个 SKU 先把三边从小到大排序为 `minSide`、`midSide`、`maxSide`，再按下面规则计算合并包裹三边：

1. 包裹边 1：所有 SKU 的最小边按数量相加，公式为 `sum(minSide * quantity)`。
2. 包裹边 2：所有 SKU 的次长边取最大值，公式为 `max(midSide)`。
3. 包裹边 3：所有 SKU 的最长边取最大值，公式为 `max(maxSide)`。
4. 包裹实重：所有 SKU 重量按数量相加，公式为 `sum(weightKg * quantity)`。

示例：SKU A 为 `26 * 15 * 12`、数量 1；SKU B 为 `21 * 15 * 9`、数量 2。排序后：

- A：`12 / 15 / 26`
- B：`9 / 15 / 21`

合并包裹为：

- 边 1：`12 * 1 + 9 * 2 = 30`
- 边 2：`max(15, 15) = 15`
- 边 3：`max(26, 21) = 26`

所以合并尺寸是 `30 * 15 * 26 cm`。最终展示时可以按从大到小显示为 `26 * 15 * 30 cm`，但计算来源要保留这三条规则。

### 外部费用试算边界

旧 `E:\Urili` 架构里，费用试算通过 `FreightQuotePort` 一类端口接入外部报价能力。领星费用试算、物流商 API 或其它外部报价只能形成预估或候选报价，不能直接成为平台财务流水、卖家应收或实际结算事实源。

这个边界建议继续沿用到当前若依工程：

- 费用试算结果是临时结果或测试结果。
- 不写财务流水。
- 不写订单费用明细。
- 不改变报价方案配置。
- 如未来要保存试算记录或报价快照，需要单独确认表设计。

### 现有领星与物流报价封装

当前工程已经有可复用的外部系统和物流报价封装：

- `integration` 模块已有 `LingxingOpenApiClient` 和 `UpstreamLingxingClientFactory`，负责领星 OpenAPI client 创建、凭证解密、请求日志、硬超时和本机时钟健康检查。
- `integration` 模块已有领星仓库、物流渠道、SKU、尺寸、库存等同步组件。
- 当前 `LingxingOpenApiClient` 没有费用试算/运费试算方法。
- `logistics` 模块已有通用物流报价入口：`ILogisticsCarrierService.quote(LogisticsQuoteRequest request)`。
- `ruoyi-admin` 已暴露物流商报价接口：`POST /logistics/admin/carriers/quote`，当前权限绑定 `logistics:carrier:label`。
- 当前报价实现主要走 AGG56：`LogisticsCarrierServiceImpl.quote(...)` 调用 `Agg56OpenApiClient.rates(...)`，并把 provider 原始结果存入 `providerResultJson` 返回。
- 已查领星公开文档索引 `https://apidoc-omp.xlwms.com/llms.txt`，当前公开索引里未发现费用试算/运费试算 endpoint；OpenAPI JSON 直连需要登录，不能仅凭猜测确定字段。

结论：

- 费用试算不应在 finance 页面或 finance Controller 里直接拼领星 HTTP。
- 如果第一版使用现有物流商报价能力，应由 finance fee estimate service 编排报价方案、仓库、渠道和包裹参数，再调用 logistics 暴露的报价 service/port。
- 如果要新增领星“费用试算”专用 API，应补在 `integration/logistics` 适配层内，复用 `LingxingOpenApiClient`、请求日志、脱敏和超时保护，再由 finance 编排调用。
- 实现前必须拿到领星费用试算接口文档或后台接口字段，确认必填字段、字段名、枚举值和返回费用项。没有确认前，不落死领星请求字段。
- 如果领星费用试算要求住宅地址字段，页面不展示该字段，后端默认传“住宅地址/是”。
- 费用试算结果需要把 provider 原始响应转换成统一费用列；原始 JSON 最多作为管理端排障信息或请求日志入口，不作为页面主要展示结构。

## 建议第一版范围

第一版先做 `试算工具页 + 后端试算接口 + 真实外部试算编排 + 菜单权限`，不做持久化试算记录。

### 菜单与权限

新增菜单：

- 菜单名：`费用试算`
- 父级：`财务管理`，`menu_id = 2050`
- 建议 path：`fee-estimate`
- 建议 component：`Finance/FeeEstimate/index`
- 建议 routeName：`FinanceFeeEstimate`
- 建议权限：`finance:feeEstimate:list`
- 建议排序：`order_num = 1`

建议按钮权限：

- `finance:feeEstimate:query`：读取试算所需选项和 SKU 快照
- `finance:feeEstimate:calculate`：执行试算

是否给普通管理角色授权，需要按现有角色菜单授权策略另行确认。新增 sys_menu 本身不会自动让普通角色看到菜单。

### 页面结构

建议页面使用当前管理端 Ant Design Pro 风格，不复制截图样式。采用“三看板”布局：左侧一个试算条件面板，右侧上下两个数据面板。布局学习截图的信息组织方式，但控件、间距、表格、按钮、颜色全部按本项目现有组件和规范实现。

建议布局：

1. 左侧试算条件面板
   - 面板宽度建议 `320-360px`，高度跟随页面可视区域，内部滚动
   - 发货仓 / 发货地
   - 目的国家 / 地区
   - 省州、城市、邮编
   - 报价方案
   - 物流渠道范围：不选、单选或多选；不选表示当前方案下全部渠道
   - `BILLING` 方案展示客户物流渠道，`COST` 方案展示系统物流渠道
   - 底部固定试算按钮，避免条件较多时操作按钮被滚出

2. 右上 SKU / 包裹面板
   - 输入方式：选择 SKU / 手工输入，二者互斥
   - SKU 模式支持按系统 SKU、客户 SKU、商品名搜索
   - 手工模式输入一个包裹的长、宽、高、重量、数量
   - SKU 模式按行维护 SKU 和数量，系统按合包规则计算一个包裹尺寸
   - 使用项目现有 Table / ProTable / Form 组件展示 SKU、商品名称、数量、尺寸、重量、操作
   - 面板高度保持紧凑，右下结果面板占主要剩余高度

3. 右下试算结果面板
   - 学习截图里的列表方式：上方显示本次请求编号，下方用一张结果表按物流渠道逐行展示
   - 建议列：物流渠道、总费用、基础运费、附加费、操作费、包材费、币种、计费重、体积重、包裹数量
   - 成功渠道展示金额和重量；失败或未配置渠道保留行，用状态图标和 `--` 展示缺失费用
   - 不默认展开原始明细；如后续需要，可在行内增加“查看明细”弹窗
   - 结果表占满剩余高度，空状态也要撑开，不让页面下半屏露出大面积空白

### 后端接口

建议归属 `finance` 模块：

- `GET /finance/admin/fee-estimate/options`
- `GET /finance/admin/fee-estimate/skus`
- `POST /finance/admin/fee-estimate/calculate`

第一版页面请求建议包含：

- `warehouseCode`
- `destinationCountryCode`
- `destinationState`
- `destinationCity`
- `destinationPostalCode`
- `quoteSchemeId`
- `channelCodes`：可空；空数组或不传表示全部渠道。`BILLING` 方案语义为客户物流渠道，`COST` 方案语义为系统物流渠道
- `packageInputMode`：`SKU` / `MANUAL`，二者互斥
- `packageLines`
  - SKU 模式：`skuId`、`quantity`
  - 手工模式：`lengthCm`、`widthCm`、`heightCm`、`weightKg`、`quantity`

对接领星出站请求时，字段以领星费用试算接口文档为准。当前已确认的映射原则：

- 页面不展示住宅地址字段。
- 如果领星必填住宅地址字段，后端出站时默认传住宅地址为 `true` / `是` / 对应领星枚举值。
- 页面字段不足以满足领星必填项时，不在前端临时加字段；先补充方案并确认字段来源。

第一版响应建议包含：

- `requestSummary`
  - `requestNo`
- `packageSummary`
  - `sourceMode`：`SKU` / `MANUAL`
  - `actualWeightKg`
  - `volumeWeightKg`
  - `chargeableWeightKg`
  - `lengthCm`
  - `widthCm`
  - `heightCm`
- `results`
  - `schemeType`
  - `channelType`
  - `channelCode`
  - `channelName`
  - `currencyCode`
  - `sourceMode`
  - `success`
  - `totalAmount`
  - `baseFreightAmount`
  - `surchargeAmount`
  - `operationFeeAmount`
  - `packingFeeAmount`
  - `chargeableWeightKg`
  - `volumeWeightKg`
  - `packageCount`
  - `traceId`
  - `requestLogId`
  - `errorCode`
  - `errorMessage`

### 计费策略建议

推荐先按“外部试算优先”的方式实现，和当前报价方案阶段一一致：

1. 用户选择报价方案。
2. 系统筛出该方案可用仓库和客户渠道。
3. 如果用户选择了渠道，则只试算选中渠道；如果未选择渠道，则试算方案下全部可用渠道。
4. `BILLING` 方案按客户物流渠道展开，再解析到对应系统渠道或外部试算来源。
5. `COST` 方案按系统物流渠道展开，用于内部成本试算。
6. 调用统一的费用试算端口。
7. 把结果转换成统一的费用列返回，例如总费用、基础运费、附加费、操作费、包材费。

如果某个渠道当前没有可调用的外部试算适配器，第一版对该渠道返回 `未配置试算来源` 失败结果，同时继续展示其它可试算渠道。不要用前端 mock 冒充真实计费。

## 暂不建议第一版做的内容

- 不新增试算记录表。
- 不保存报价快照。
- 不写订单费用明细。
- 不写财务流水。
- 不做自动最低价决策。
- 不做利润底线或价格兜底。
- 不直接在前端计算真实费用。
- 不让 finance 直接依赖 product、warehouse、logistics 的 Mapper。

## 已确认与待确认问题

已确认：

1. 第一版只做“管理端测试工具”，不保存试算历史。
2. 渠道可单选、多选或不选；不选渠道等于全部展示。
3. 多 SKU 第一版合并成一个包裹。
4. 当前要接真实外部试算来源。
5. 目的地第一版页面按美国地址字段处理，不展示住宅地址字段。
6. 报价方案支持 `BILLING` 和 `COST` 两类。
7. `手工尺寸` 和 `选择 SKU` 互斥，同一次试算只能使用一种。
8. 合并包裹尺寸按“最小边按数量相加、次长边取最大、最长边取最大”计算。
9. 结果区学习截图列表方式，按物流渠道逐行展示费用列。
10. 当前没有商城尺寸字段，SKU 试算暂时使用来源商品的仓库测量尺寸。
11. 未来商城尺寸字段落地后，SKU 试算要切换到商城尺寸，不能长期依赖来源仓库测量尺寸。
12. 如果领星费用试算接口必填住宅地址字段，后端出站时默认按住宅地址传，页面不让用户填写。
13. 页面采用左侧条件面板、右上包裹/SKU 面板、右下结果面板的三看板布局，用本项目组件实现，不复刻截图视觉样式。

建议按本文口径默认执行，但仍建议确认：

1. 外部请求排障信息是否展示 `requestNo/traceId/requestLogId`，默认不展示原始响应 JSON。
2. 领星费用试算接口字段目前未在公开索引中找到，实施前需要拿到领星接口文档、后台接口字段或可验证的请求样例。

## 推荐实施切片

### 切片一：菜单与页面骨架

- 新增 `Finance/FeeEstimate/index.tsx`
- 新增 service 和类型定义
- 复用现有报价方案、仓库、渠道、SKU 选择相关 service 和模糊搜索下拉工具
- 新增 SQL seed，放到财务管理第一个位置
- 新增前端合同测试和后端路由权限合同测试

### 切片二：选项与 SKU 快照

- 新增 finance fee estimate Controller / Service
- 新增 product SKU lookup port
- 支持 SKU 搜索和尺寸重量归一化
- 复用现有 `QuoteScheme*LookupService` 模式，避免 finance 直接依赖 product、warehouse、logistics Mapper

### 切片三：试算接口

- 先根据报价方案筛出可用仓库、客户渠道和费用来源
- 根据 `BILLING/COST` 方案类型解析客户渠道或系统渠道
- 对未选择渠道的请求展开全部可用渠道
- 调用 logistics/integration 侧统一报价端口，返回统一费用列
- 如果单个渠道试算来源未配置或调用失败，返回该渠道明确失败结果，不影响其它渠道展示

### 切片四：运行态验证

- `react-ui` 定向 Jest 合同测试
- `RuoYi-Vue` finance/product/logistics/warehouse reactor compile
- `FinanceAdminRouteContractTest`
- API smoke
- 浏览器打开 `http://127.0.0.1:8001/finance/fee-estimate` 验证菜单、表单、SKU 选择和结果区
- `codegraph sync .`

## 成功标准

- 管理端 `财务管理` 下第一个菜单是 `费用试算`。
- 有权限用户可见菜单并可执行试算；无权限用户接口被后端拒绝。
- SKU 模式和手工模式都能形成一致的包裹参数。
- 试算结果能展示每个渠道、每个收费项、计费重、币种和失败原因。
- 未配置真实计费来源时，系统返回清晰的未配置结果，不伪造费用。
- 不产生财务流水、订单费用或持久化试算记录。
