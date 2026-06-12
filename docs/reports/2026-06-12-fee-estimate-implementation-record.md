# 2026-06-12 费用试算实现记录

## 范围

- 在财务管理下新增 `费用试算` 管理端页面，菜单 seed 使用 `RuoYi-Vue/sql/20260612_fee_estimate_menu_seed.sql`。
- 新增管理端接口 `/finance/admin/fee-estimate/**`：
  - `GET /options`
  - `GET /skus/list`
  - `POST /calculate`
- 新增前端页面 `react-ui/src/pages/Finance/FeeEstimate/index.tsx`，采用左侧条件面板、右上包裹/SKU 面板、右下结果列表的三看板结构。
- 未新增业务表，未保存试算历史。

## 关键规则

- 手工尺寸和选择 SKU 互斥，同一次试算只允许一种包裹输入模式。
- SKU 试算暂时使用来源商品仓库测量尺寸：
  - `measureLengthCm`
  - `measureWidthCm`
  - `measureHeightCm`
  - `measureWeightKg`
- 当前还没有商城确认尺寸字段；未来商城尺寸落地后，需要替换 product 侧 `FinanceFeeEstimateSkuLookupService` 实现。
- SKU 合包规则：
  - 每个 SKU 三边升序。
  - 包裹边 1 = 每个 SKU 最小边 * 数量后相加。
  - 包裹边 2 = 所有 SKU 次长边最大值。
  - 包裹边 3 = 所有 SKU 最长边最大值。
  - 实重 = 每个 SKU 重量 * 数量后相加。
- 渠道支持不选、单选、多选；不选渠道时展示当前报价方案下全部启用渠道。
- 页面不展示住宅地址、签名服务、申报价值。
- 如领星费用试算接口后续要求住宅地址字段，后端对接适配器时默认按住宅地址传递。

## 领星对接状态

- 本次已确认本地 `LingxingOpenApiClient` 没有费用试算方法。
- 公开索引未能确认费用试算 endpoint 和字段。
- 因字段未确认，当前 `POST /calculate` 会计算包裹尺寸、计费重并生成每个渠道的结果行，但费用金额 fail-closed：
  - `LINGXING_ESTIMATE_FIELDS_UNCONFIRMED`
  - 不伪造总费用、基础运费、附加费、操作费、包材费。
- 后续拿到领星费用试算字段后，应新增外部试算适配器，并补 traceId、请求日志、错误映射和脱敏日志。

## 数据库执行状态

- 已新增菜单 seed 文件，但本次未执行 SQL。
- 原因：当前项目规则要求涉及远端数据库 DDL/DML、菜单、权限调整时，先生成记录并得到确认后再执行。
- 如需落库菜单，需要在确认目标数据源后执行：
  - `set @confirm_fee_estimate_menu_seed = 'APPLY_FEE_ESTIMATE_MENU_SEED';`
  - 再执行 `RuoYi-Vue/sql/20260612_fee_estimate_menu_seed.sql`。

## 验证

- 后端定向编译与合同测试：
  - `mvn -pl finance,product,ruoyi-system -am "-Dtest=FinanceAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：通过。
- 后端管理端聚合编译：
  - `mvn -pl ruoyi-admin -am -DskipTests test-compile`
  - 结果：通过。
- 前端类型检查：
  - `npm run tsc`
  - 结果：通过。
- 前端费用试算合同测试：
  - `npx jest --config jest.config.ts --runTestsByPath tests/finance-fee-estimate-contract.test.ts --runInBand`
  - 结果：通过，5 tests。
- 财务相关前端合同测试：
  - `npx jest --config jest.config.ts --runTestsByPath tests/finance-currency-contract.test.ts tests/finance-quote-scheme-contract.test.ts tests/finance-fee-estimate-contract.test.ts --runInBand`
  - 结果：通过，16 tests。
- 三端 manifest 检查：
  - `node scripts/verify-three-terminal.mjs --check-manifest`
  - 结果：通过。
- 空白检查：
  - `git diff --check -- <费用试算相关路径>`
  - 结果：通过。
- CodeGraph：
  - `codegraph sync .`
  - 结果：通过，Already up to date。

## 未执行项

- 菜单 seed 已在用户确认后执行到目标库 `fenxiao`，详见下方执行记录。
- 未做真实领星费用试算调用，因为费用试算 endpoint 和字段尚未确认。

## 2026-06-12 16:39 菜单 seed 执行记录

- 用户已确认执行费用试算菜单 seed。
- 数据源确认：
  - 后端激活 profile：`druid`。
  - MySQL 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`。
  - 连接变量来源：本机 `.env.local` 中的 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`，记录中不输出明文。
  - 目标库：`fenxiao`。
  - Redis：本次 SQL 不读取、不写入 Redis。
- 执行命令类型：远端 MySQL 菜单/权限 seed DML；不新增业务表，不写业务数据。
- 执行文件：`RuoYi-Vue/sql/20260612_fee_estimate_menu_seed.sql`。
- 确认 token：`APPLY_FEE_ESTIMATE_MENU_SEED`。
- 执行前只读预检：
  - 财务父菜单 `2050` 存在。
  - `2550`、`2551`、`2552` 费用试算菜单位均为空。
  - `finance:feeEstimate:*` 相关签名无重复占用。
- 第一次执行结果：
  - 失败，错误为 `Can't reopen table: 'seed'`。
  - 原因是 MySQL 临时表不能在同一查询中被重复打开。
  - 失败发生在写入 `sys_menu` 前；随后只读复核确认 `2550`、`2551`、`2552` 仍为空，未产生半写入菜单。
- 修正：
  - 将 `assert_fee_estimate_sys_menu_guard()` 中的重复临时表子查询改为单次 `join tmp_fee_estimate_sys_menu_guard`。
- 第二次执行结果：
  - 成功，runner 输出 `fee estimate menu SQL seed applied with explicit confirmation.`。
  - postcheck：`2550`、`2551`、`2552` 三条菜单/按钮均落库。
  - postcheck：完成态计数为 `3`。
  - postcheck：重复签名计数为 `0`。
  - HEX 校验确认菜单中文名为 UTF-8 正常落库，终端中文乱码仅为控制台显示编码问题。
- 执行后验证：
  - `mvn -pl finance,product,ruoyi-system -am "-Dtest=FinanceAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，3 tests。
  - `npx jest --config jest.config.ts --runTestsByPath tests/finance-fee-estimate-contract.test.ts --runInBand`：通过，5 tests。
  - `git diff --check -- <费用试算相关路径>`：通过，仅有 CRLF 提示。
  - `codegraph sync .`：通过，Already up to date。

## 2026-06-12 17:28 运行态接口排查记录

- 用户截图显示页面请求 `/finance/admin/fee-estimate/options` 与 `/finance/admin/fee-estimate/skus/list` 返回 `No static resource ...`。
- 排查结论：
  - 前端 service 实际请求地址为 `/api/finance/admin/fee-estimate`。
  - 本地开发代理会将 `/api` 转发到 `http://127.0.0.1:8080` 并重写为后端路径 `/finance/admin/fee-estimate`，这是预期行为。
  - 当前 8080 运行态已能命中费用试算 Controller，不再返回静态资源 404。
- 运行态验证：
  - `curl.exe --noproxy "*" -i http://127.0.0.1:8080/finance/admin/fee-estimate/options`：返回业务安全链路 `401`，说明后端映射存在。
  - `curl.exe --noproxy "*" -i http://127.0.0.1:8001/api/finance/admin/fee-estimate/options`：经 8001 代理后返回业务安全链路 `401`，`x-real-url` 指向 `http://127.0.0.1:8080/finance/admin/fee-estimate/options`。
  - `curl.exe --noproxy "*" -i "http://127.0.0.1:8001/api/finance/admin/fee-estimate/skus/list?pageNum=1&pageSize=10"`：经 8001 代理后返回业务安全链路 `401`，`x-real-url` 指向 `http://127.0.0.1:8080/finance/admin/fee-estimate/skus/list?pageNum=1&pageSize=10`。
- 因未携带登录 token，上述验证返回 `401` 是预期结果；关键是已不再出现 `No static resource`。

## 2026-06-12 17:40 页面布局调整记录

- 调整左侧条件面板字段顺序：
  - `发货仓`
  - `报价方案`
  - `物流渠道`
  - `包裹方式`
  - 目的地与收件地址字段
- `包裹方式` 放到左侧物流渠道下方，选项为 `选择 SKU` / `手工尺寸`。
- 新增 `收件地址2`，并贯通到前端类型和后端 `FeeEstimateRequest.destinationAddress2`。
- SKU 模式：
  - 右侧仍保持上下两块：`包裹信息` + `试算结果`。
  - `包裹信息` 中保留 SKU 搜索、SKU 列表和数量编辑。
- 手工尺寸模式：
  - 左侧在 `包裹方式` 下方显示 `长度 cm`、`宽度 cm`、`高度 cm`、`重量 kg`。
  - 右侧不再渲染 `包裹信息` 面板。
  - `试算结果` 面板铺满右侧工作区。
- 验证：
  - `npm run tsc`：通过。
  - `npx jest --config jest.config.ts --runTestsByPath tests/finance-fee-estimate-contract.test.ts --runInBand`：通过，5 tests。
  - `mvn -pl finance,product,ruoyi-system -am "-Dtest=FinanceAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，3 tests。
  - 浏览器运行态验证：
    - SKU 模式左侧字段顺序为 `发货仓 / 报价方案 / 物流渠道 / 包裹方式 / 到货国家/地区 ... / 收件地址2`，右侧为 2 个 section。
    - 手工尺寸模式显示 `长度 cm / 宽度 cm / 高度 cm / 重量 kg`，右侧只有 1 个 `试算结果` section，且不再显示 SKU 搜索或 `包裹信息`。

## 2026-06-12 17:43 后端运行 jar 重启记录

- 用户截图仍显示 `/finance/admin/fee-estimate/options` 和 `/finance/admin/fee-estimate/skus/list` 返回 `No static resource ...`。
- 现场复核：
  - 直连 `http://127.0.0.1:8080/finance/admin/fee-estimate/options` 已返回业务安全链路 `401`，不是静态资源 404。
  - 经 8001 代理 `http://127.0.0.1:8001/api/finance/admin/fee-estimate/options` 同样返回业务安全链路 `401`，`x-real-url` 指向 8080 目标接口。
- 为避免浏览器或运行 jar 状态不一致，重新打包并重启后端：
  - 第一次 `mvn -pl ruoyi-admin -am -DskipTests package` 失败，原因是 8080 Java 进程占用 `ruoyi-admin.jar`，Spring Boot repackage 无法 rename。
  - 停止 8080 后端进程后，重新执行同一打包命令成功。
  - 使用 `.\start-backend-local.ps1 -Restart` 启动新 jar。
- 重启后复核：
  - 新后端进程已监听 8080。
  - 直连 8080 和经 8001 代理访问费用试算 options 接口均返回业务安全链路 `401`，不再出现 `No static resource`。
  - 浏览器刷新 `http://127.0.0.1:8001/finance/fee-estimate` 后未出现 `No static resource` toast，页面能加载报价方案选项。

## 2026-06-12 17:57 自动最优候选解析 V0 实现记录

- 本次目标：先把“买家 + SKU + 目的地 + 可选指定仓库 -> 候选仓库 -> 计费报价方案 -> 客户渠道 -> 系统渠道”的链路跑通；不做缓存、队列和外部费用真实金额。
- 后端新增能力：
  - `FeeEstimateRequest.selectionMode`：`MANUAL` 保留原手动模式，`AUTO_BEST` 进入自动最优候选解析。
  - `FeeEstimateRequest.buyerId`：自动模式必填，用于匹配买家等级、报价方案适用范围和客户渠道可见性。
  - `FinanceFeeEstimateSkuLookupService.selectSkuWarehouseCandidatesByIds`：由 product 模块提供 SKU/SPU 可发仓库候选。
  - `FinanceFeeEstimateLogisticsLookupService`：由 logistics 模块提供客户渠道到系统渠道的候选解析，不让 finance 直接读物流 mapper。
  - `FeeEstimateResponse.resolveSummary` 与 `routeCandidates`：返回候选数量、可执行候选数量、失败候选数量和每条候选失败原因。
- 自动模式规则：
  - 只支持 SKU 模式，不支持手工尺寸模式。
  - 多 SKU 先取共同可发仓库；如果客户指定仓库，指定仓库必须在共同可发仓库内。
  - 候选仓库必须同国家、同币种，否则直接视为配置异常。
  - 报价方案只匹配 `BILLING`、已启用、当前时间有效、买家范围命中、仓库命中的方案。
  - 同一仓库命中多套方案时，按现有报价方案列表顺序取第一套；当前 mapper 顺序为 `effective_priority desc, effective_time desc, scheme_id desc`，也就是优先级数字越大越优先。
  - 自动模式排除客户上传面单渠道。
  - 系统渠道必须启用且绑定候选仓库；物流商打单模式还必须存在启用的物流商渠道映射。
- 前端新增最小入口：
  - `渠道选择`：`手动选择` / `自动最优`。
  - 自动最优显示 `买家`，隐藏 `报价方案` 和 `物流渠道`。
  - 自动最优下 `发货仓` 可选；不选则在 SKU 共同可发仓库内自动解析。
  - 自动最优固定使用 SKU 模式，避免手工尺寸缺少商品仓库信息。
  - 结果表增加 `发货仓`、`系统渠道`，便于看到解析出来的内部链路。
- 当前仍然 fail-closed 的部分：
  - 买家侧真实费用金额未接外部试算，仍返回 `LINGXING_ESTIMATE_FIELDS_UNCONFIRMED` 或 `INTERNAL_RATE_NOT_IMPLEMENTED`。
  - 系统成本择优未接履约仓成本试算和物流商面单价格试算。
  - 当前 V0 只做同步候选解析，没有队列、缓存、候选读模型或外部 API 并发限制。
- 验证：
  - `mvn -pl finance,product,logistics -am -DskipTests compile`：通过。
  - `mvn -pl ruoyi-system -Dtest=FinanceAdminRouteContractTest test`：通过，3 tests。
  - `npx jest --config jest.config.ts tests/finance-fee-estimate-contract.test.ts --runInBand`：通过，5 tests。
  - `npm run tsc`：通过。

## 2026-06-12 18:04 自动最优运行态 API 验证记录

- 为让本地 8080 使用最新 Java 代码，先停止旧后端进程，随后在 `RuoYi-Vue` 目录执行：
  - `mvn -pl ruoyi-admin -am -DskipTests package`：通过。
  - `.\start-backend-local.ps1 -Restart`：启动新 jar，8080 已监听。
- 运行态接口验证：
  - `GET http://127.0.0.1:8080/finance/admin/fee-estimate/options` 返回 `200`。
  - 浏览器刷新 `http://127.0.0.1:8001/finance/fee-estimate` 后页面正常，自动最优模式下能看到买家选项。
- 自动最优样本请求：
  - 登录账号：`admin/admin123`，仅用于本机验证 token，不输出 token。
  - 请求：`selectionMode=AUTO_BEST`、`buyerId=3`、`skuId=49`、`destinationCountryCode=US`、`destinationPostalCode=91144`。
  - 返回：`code=200`。
  - 解析结果：
    - 买家：`BUY-US-CRG-001 / L1`。
    - SKU：`SKU202606050042`。
    - 候选仓库：`NY013 / US`。
    - 包裹合并：`28 * 20 * 3 cm`，实重 `0.420 kg`，体积重 `0.336 kg`，计费重 `0.420 kg`。
    - `resolveSummary.warehouseCandidateCount=1`。
    - `resolveSummary.quoteSchemeCandidateCount=0`。
    - `resolveSummary.routeCandidateCount=1`。
    - `routeCandidates[0].failureCode=QUOTE_SCHEME_MISSING`。
- 结论：
  - 新代码已经能从买家、SKU 推到候选仓库，并返回明确失败原因。
  - 当前真实运行库仍卡在报价方案仓库绑定：`quote_scheme_warehouse` 未补齐，所以严格新规则下无法继续展开客户渠道和系统渠道。

## 2026-06-12 18:25 报价方案仓库绑定与外部费用试算接入记录

- 只读检查结论：
  - 当前 `测试1` 计费方案和 `测试成本` 成本方案都绑定了 `KAT-F-CA012 / KAT测试仓-CA012`。
  - `KAT-F-CA012` 是有效系统仓库，并且已经有报价仓仓库配对：`LX-KAT-91B1E277 / CA012 / 美西-CA-012仓`。
  - 但当前商品仓库绑定没有使用 `KAT-F-CA012`；真实商品可发仓库里有 `CA012`、`NY013`、`CAC002`、`CACA333333`、`US014`。
  - 本次样本 `buyerId=3 + skuId=49` 解析出的候选仓库是 `NY013 / 美东NY013`，所以当前绑定 `KAT-F-CA012` 对这个样本不生效，接口仍返回 `QUOTE_SCHEME_MISSING`。
- 补齐外部试算接入边界：
  - finance 新增 `FinanceFeeEstimateExternalService` 端口，不直接依赖领星客户端。
  - integration 新增 `LingxingFinanceFeeEstimateExternalServiceImpl`，负责校验报价仓仓库配对、报价渠道配对、上游连接状态和凭证状态。
  - `LingxingOpenApiClient` 新增 `estimateFee(...)`，复用现有签名、超时、重试、请求日志和脱敏链路。
  - 配置项新增 `urili.integration.lingxing.fee-estimate-path`，环境变量为 `URILI_LINGXING_FEE_ESTIMATE_PATH`。
- 当前仍需配置/补齐的数据：
  - 领星费用试算 endpoint 未在公开官方 API 索引中确认，当前必须通过 `URILI_LINGXING_FEE_ESTIMATE_PATH` 显式配置。
  - 当前系统渠道还缺少 `QUOTE` 用途的报价仓渠道配对；即使仓库配对正确，也会返回 `QUOTE_CHANNEL_PAIRING_MISSING`，不会伪造金额。
- 验证：
  - `mvn -pl finance,integration,ruoyi-system -am -DskipTests compile`：通过。
  - `mvn -pl ruoyi-system,integration -am "-Dtest=FinanceAdminRouteContractTest,IntegrationModuleBoundaryContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，10 tests。
  - `npx jest --config jest.config.js tests/finance-fee-estimate-contract.test.ts --runInBand`：通过，5 tests。
  - `node scripts/verify-three-terminal.mjs --check-manifest`：通过。

## 2026-06-12 18:32 外部试算运行态复核记录

- 重新打包并重启：
  - 停止 8080 当前后端进程。
  - `mvn -pl ruoyi-admin -am -DskipTests package`：通过。
  - `.\start-backend-local.ps1 -Restart`：返回成功，8080 已监听。
- 自动最优样本：
  - 请求：`buyerId=3`、`skuId=49`、`destinationCountryCode=US`、`destinationPostalCode=91144`、`selectionMode=AUTO_BEST`。
  - 结果：候选仓库为 `NY013 / 美东NY013`。
  - 失败原因：`QUOTE_SCHEME_MISSING`，说明当前绑定 `KAT-F-CA012` 对该 SKU 样本不生效。
- 手动指定 `KAT-F-CA012`：
  - `UPS-S`：映射到系统渠道 `UPS`，失败于 `SYSTEM_CHANNEL_WAREHOUSE_MISSING`。
  - `USPS`：映射到系统渠道 `USPS` / `USPS-Z`，均失败于 `SYSTEM_CHANNEL_WAREHOUSE_MISSING`。
  - `USPS-SF`：映射到系统渠道 `USPS-S`，失败于 `CARRIER_MAPPING_MISSING`。
  - 结论：仓库本身有效，但当前报价方案渠道、系统渠道仓库绑定、物流商打单映射还没有形成可执行路线，所以尚未触发正式外部费用试算 HTTP 调用。
- CodeGraph：
  - `codegraph sync .`：通过，已同步 1 个变更文件。
