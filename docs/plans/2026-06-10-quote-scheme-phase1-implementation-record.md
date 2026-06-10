# 报价方案阶段一实施记录

日期：2026-06-10

## 本次范围

本次实现报价方案阶段一：基础信息、适用买家范围、仓库范围、客户渠道明细、操作费/运费占位字段。

本次不做自动最优、不调用外部费用试算、不做手工费率公式、不接入订单下单链路。

## 模块归属

报价方案已归入 `finance` 模块：

- 后端接口：`/finance/admin/quote-schemes`
- 权限前缀：`finance:quoteScheme:*`
- 前端页面：`react-ui/src/pages/Finance/QuoteScheme/index.tsx`
- 兼容入口：`react-ui/src/pages/Billing/QuoteScheme/index.tsx`

保留 `2053` 报价方案菜单位；增量 SQL 将菜单最终组件指向 `Finance/QuoteScheme/index`，同时保留旧 `Billing/QuoteScheme/index` 兼容入口。

## 数据库变更

已新增增量脚本：

- `RuoYi-Vue/sql/20260610_quote_scheme_phase1.sql`

脚本内容：

- 新增 `quote_scheme`
- 新增 `quote_scheme_scope`
- 新增 `quote_scheme_warehouse`
- 新增 `quote_scheme_channel`
- 新增报价方案相关字典
- 更新 `2053` 菜单签名
- 新增 `2540-2545` 按钮权限

脚本带确认 token：`@confirm_quote_scheme_phase1 = 'APPLY_QUOTE_SCHEME_PHASE1'`。

## 数据库执行记录

执行日期：2026-06-11

确认来源：用户回复“确认执行”。

执行前数据源确认：

- 配置来源：`application.yml` / `application-druid.yml` 使用 `RUOYI_DB_*`、`RUOYI_REDIS_*` 环境变量，本机实际值来自 `.env.local`。
- MySQL：`gz-cynosdbmysql-grp-lucf5kyf.sql.tencentcdb.com:28634/fenxiao`，远端/非本机。
- Redis：`114.132.156.75:6379`，database `1`，远端/非本机。
- 本次只执行 MySQL 增量 SQL，没有写 Redis。

执行过程：

- 本机没有 `mysql` 命令行客户端，改用本机 Maven 缓存中的 MySQL JDBC 驱动执行 SQL。
- 第一次执行连接到 `fenxiao` 后，在 `call assert_quote_scheme_menu_guard()` 处 fail-closed；原因是远端 `2053` 报价方案菜单仍保留历史权限 `billing:quoteScheme:list`。
- 只读核对确认 `2053` 菜单确实是报价方案页：`path=billing-quote-scheme`、`component=Billing/QuoteScheme/index`、`route_name=BillingQuoteScheme`、`perms=billing:quoteScheme:list`。
- 已将 SQL guard 补充为允许历史签名 `billing:quoteScheme:list`，再重新执行同一脚本。
- 第二次执行成功，输出 `CONNECTED_DATABASE=fenxiao`、`EXECUTED_STATEMENTS=30`。

执行后只读验证：

- `quote_scheme`、`quote_scheme_scope`、`quote_scheme_warehouse`、`quote_scheme_channel` 四张表已存在。
- 表字段数量分别为：`quote_scheme=17`、`quote_scheme_scope=13`、`quote_scheme_warehouse=8`、`quote_scheme_channel=15`。
- 6 个报价方案字典类型已存在且状态正常：`quote_scheme_type`、`quote_scheme_fee_source_mode`、`quote_scheme_scope_type`、`quote_scheme_warehouse_scope_mode`、`quote_scheme_status`、`quote_scheme_channel_status`。
- 字典项数量分别为：方案类型 2、费用来源模式 2、适用对象类型 3、仓库范围模式 2、方案状态 2、渠道状态 2。
- `2053` 菜单已更新为 `component=Finance/QuoteScheme/index`、`route_name=FinanceQuoteScheme`、`perms=finance:quoteScheme:list`。
- `2540-2545` 按钮权限已写入：`query`、`add`、`edit`、`status`、`warehouse`、`channel`。
- 脚本临时断言过程已清理，`routine_left_count=0`。

## 运行态验证记录

验证日期：2026-06-11

执行前状态：

- `8080` 后端进程正在运行，旧进程占用 `ruoyi-admin.jar`。
- `8001` React 前端已在监听。

执行过程：

- 第一次执行 `mvn -pl ruoyi-admin -am -DskipTests package` 失败，原因是旧后端进程占用 `ruoyi-admin.jar`，Spring Boot repackage 无法把 jar 改名为 `.original`。
- 已停止旧 `8080` 进程，再重新执行 `mvn -pl ruoyi-admin -am -DskipTests package`，打包成功。
- 已使用 `start-backend-local.ps1` 启动新 `ruoyi-admin.jar`。
- 已确认 `http://127.0.0.1:8080` 返回 HTTP 200。

接口只读验证：

- 登录接口可用，验证码开关返回 `captchaEnabled=false`。
- `GET /finance/admin/quote-schemes/list?pageNum=1&pageSize=10` 返回 `code=200`，当前 `total=0`。
- `GET /finance/admin/quote-schemes/options/buyers` 返回 `code=200`，当前 35 条。
- `GET /finance/admin/quote-schemes/options/warehouses` 返回 `code=200`，当前 5 条。
- `GET /finance/admin/quote-schemes/options/customer-channels` 返回 `code=200`，当前 3 条。
- `GET /finance/admin/quote-schemes/options/fee-placeholders` 返回 `code=200`，当前 0 条，符合阶段一仅占位。
- `GET /getRouters` 返回 `code=200`，动态路由包含 `Finance/QuoteScheme/index`。

浏览器验证：

- 使用 Playwright CLI 打开 `http://127.0.0.1:8001/overseas-warehouse-service/billing-quote-scheme`。
- 页面标题为 `报价方案 - Ant Design Pro`。
- 页面正文包含报价方案筛选区、`新增` 按钮、`客户渠道` 列和空表格 `暂无数据`。
- 网络请求中，报价方案列表、报价方案字典、买家下拉、仓库下拉、客户渠道下拉、费用占位接口均返回 HTTP 200。
- 截图已保存：`output/playwright/quote-scheme-phase1-runtime.png`。

## 边界处理

`finance` 不直接依赖 `warehouse`、`buyer`、`logistics` 的 Mapper。阶段一通过 lookup port 获取只读快照：

- `QuoteSchemeBuyerLookupService`
- `QuoteSchemeWarehouseLookupService`
- `QuoteSchemeCustomerChannelLookupService`

对应实现分别放在 `buyer`、`warehouse`、`logistics` 模块。

## 验证记录

已执行：

- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl finance,buyer,warehouse,logistics,ruoyi-admin -am -DskipTests compile`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,finance,buyer,warehouse,logistics,ruoyi-admin -am "-Dtest=FinanceAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/finance-quote-scheme-contract.test.ts --runInBand`
- `cd E:\Urili-Ruoyi; git diff --check`
- `cd E:\Urili-Ruoyi; codegraph sync .`
- SQL 执行后重新执行 `FinanceAdminRouteContractTest`
- SQL 执行后重新执行 `finance-quote-scheme-contract.test.ts`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-admin -am -DskipTests package`
- `cd E:\Urili-Ruoyi; .\start-backend-local.ps1`
- Playwright CLI 页面验证和截图

结果：

- 后端 reactor 编译通过，新增 `finance` 报价方案类、`buyer`/`warehouse`/`logistics` lookup 实现已进入编译产物。
- `FinanceAdminRouteContractTest` 通过，固定报价方案必须归属 `finance` 模块、路由、权限、SQL guard 和前端入口。
- `finance-quote-scheme-contract.test.ts` 通过，固定前端页面、service、菜单权限、SQL 和 manifest 约束。
- `git diff --check` 通过，仅输出当前工作区 LF/CRLF 提示，没有空白错误。
- CodeGraph 同步完成。

## 2026-06-11 表单交互调整记录

本次只调整报价方案新增/编辑入口的表单表达，不新增远端表结构，不删除远端字段。

调整内容：

- 前端列表、查询区、新增弹窗、编辑弹窗不再展示“方案编码”。
- 后端新增报价方案时，如果前端不传 `schemeCode`，由服务端生成内部编码，避免当前数据库 `scheme_code` 非空约束阻塞保存；真实业务界面不再把该字段交给用户维护。
- 新增/编辑弹窗改为上方“基础信息”多列布局。新增时先只展示基础信息，主按钮为“保存并下一步”；保存成功拿到 `schemeId` 后，再展开下方 `Tabs`。
- 已保存或编辑已有方案时，下方 `Tabs` 分成“物流费”和“操作费”。
- “物流费”页签按方案类型预留渠道口径：计费方案对应客户物流渠道，成本方案对应系统物流渠道。当前阶段成本方案的系统物流渠道先做占位，不允许直接新增绑定。
- “物流费”页签按费用来源模式切换字段文案：外部试算显示试算运费项，系统费率显示系统运费规则。由于运费设置菜单尚未实现，当前下拉保持空选项。
- “操作费”页签不绑定物流渠道，先作为方案级配置占位；外部试算显示试算操作费项，系统费率显示系统操作费规则。
- 已删除新增弹窗里的说明型提示块，不再显示“计费方案按客户物流渠道绑定物流费”“保存基础信息后再配置物流费”“操作费不跟物流渠道绑定”等长期占位文案。
- 新增/编辑弹窗移除“状态”表单项；新增默认按启用提交，编辑保存不改动原状态。
- 列表状态列改为 Ant Design `Switch`，按 `finance:quoteScheme:status` 权限控制是否可操作，替代原来的状态标签和“更多”菜单启停项。
- 后端编辑保存时，如果前端未传状态，保留当前数据库状态，避免普通编辑把停用方案意外改回启用。
- 列表操作列删除“更多/费用配置”入口，报价方案编辑和费用配置统一从“编辑”进入。
- 列表字段顺序调整为“优先级、状态、生效时间”，状态位置贴近生效判断字段。
- 编辑弹窗补强表单回填：日期字段转为表单控件需要的 `dayjs` 值，并在弹窗挂载后重新写入表单，避免编辑时基础字段看起来为空。
- 生效优先级输入框增加占位说明“数字越大越优先”，与 SQL 注释和列表排序 `effective_priority desc` 保持一致。
- 客户物流渠道新增/编辑弹窗的状态从下拉框改为 Ant Design `Switch`，默认启用；渠道列表状态也改为 `Switch`，可直接启停。
- 成本方案的“新增系统物流渠道”从占位禁用改为可用：finance 增加系统物流渠道 lookup port，由 logistics 模块实现系统渠道只读选项；前端按方案类型在客户物流渠道和系统物流渠道之间切换选项。
- 前端合同测试同步固定：报价方案查询字段数改为 6，并断言页面不再含 `label="方案编码"`、必须存在“保存并下一步”、不得再引入 `<Alert`。

本次没有执行数据库 `ALTER TABLE DROP COLUMN scheme_code`。原因是远端 DDL 删除字段属于高影响变更，需要单独提交 SQL 方案并确认；当前用“界面隐藏 + 后端内部生成”满足第一版交互诉求。

补充验证：

- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl finance,buyer,warehouse,logistics,ruoyi-admin -am -DskipTests compile`
- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/finance-quote-scheme-contract.test.ts --runInBand`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,finance,buyer,warehouse,logistics,ruoyi-admin -am "-Dtest=FinanceAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-admin -am "-Dmaven.test.skip=true" package`
- `cd E:\Urili-Ruoyi; .\start-backend-local.ps1`
- 只读接口验证：`GET /finance/admin/quote-schemes/list?pageNum=1&pageSize=10` 返回 `code=200`。
- 浏览器验证：打开 `http://127.0.0.1:8001/overseas-warehouse-service/billing-quote-scheme`，新增弹窗初始状态只包含“基础信息”，不包含“物流费”“操作费”“方案编码”和说明型提示文案，主按钮显示“保存并下一步”。
- 浏览器验证：列表状态列渲染为 Ant Design `Switch`；新增弹窗基础信息表单项为“方案名称、方案类型、费用来源模式、币种、适用对象、仓库范围、生效时间、失效时间、生效优先级、备注”，不再包含“状态”。
- 浏览器验证：列表不再显示“更多/费用配置”；表头顺序为“优先级、状态、生效时间”；点击编辑后方案名称、方案类型、费用来源、币种、适用对象、仓库范围、生效时间和优先级均能回填；新增客户物流渠道弹窗的状态项为 `Switch`，不再是下拉框。
- 运行态验证：`GET /finance/admin/quote-schemes/options/system-channels` 带管理端 token 返回 `code=200`，当前返回 3 条启用系统物流渠道；打开成本方案“测试成本”后，“新增系统物流渠道”按钮为可点击状态，弹窗内系统物流渠道选择框不再禁用，状态项仍为 `Switch`。
