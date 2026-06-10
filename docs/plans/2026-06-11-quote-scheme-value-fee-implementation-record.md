# 报价方案增值费阶段实现记录

日期：2026-06-11

## 本次范围

- 在报价方案编辑弹窗新增 `增值费` 页签。
- 新增增值费规则配置：物流渠道、触发情况、收费方式、调整方向、调整值、状态、排序、备注。
- 计费方案选择客户物流渠道，成本方案选择系统物流渠道。
- 第一版触发情况只支持 `取消订单`。
- 固定金额币种直接跟随报价方案币种，不单独选择。

## 后端实现

- 新增领域对象：`QuoteSchemeValueFeeRule`。
- 新增表设计脚本：`RuoYi-Vue/sql/20260611_quote_scheme_value_fee_rule.sql`。
- 新增接口：
  - `GET /finance/admin/quote-schemes/{schemeId}/value-fees/list`
  - `POST /finance/admin/quote-schemes/{schemeId}/value-fees`
  - `PUT /finance/admin/quote-schemes/{schemeId}/value-fees/{valueFeeRuleId}`
  - `DELETE /finance/admin/quote-schemes/{schemeId}/value-fees/{valueFeeRuleId}`
- 新增权限点：`finance:quoteScheme:valueFee`。

## 数据库说明

- SQL 文件已生成，包含确认 token：`APPLY_QUOTE_SCHEME_VALUE_FEE_RULE`。
- 已在 2026-06-11 执行到当前激活远端 MySQL，目标库：`fenxiao`。
- 执行前只读 precheck：`quote_scheme_value_fee_rule` 表、增值费字典、`finance:quoteScheme:valueFee` 权限均不存在，`2546` 菜单位无冲突。
- 执行方式：通过 JDBC runner 从 `.env.local` 读取 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`，同一连接内设置 `@confirm_quote_scheme_value_fee_rule = 'APPLY_QUOTE_SCHEME_VALUE_FEE_RULE'` 后执行 SQL。
- 执行后 postcheck：目标库 `fenxiao`，表数量 1，字典类型 3，字典数据 5，权限按钮 1，菜单位冲突 0。

## 不在本次范围

- 不接订单取消事件。
- 不计算订单实际费用。
- 不写订单费用明细或财务流水。
- 不实现分国家、重量段、仓库维度的阶梯增值费。

## 文件大小判断

- `react-ui/src/pages/Finance/QuoteScheme/index.tsx` 已超过 500 行。本次没有拆分，是因为现有报价方案页面已经把基础信息、物流费、操作费放在同一个聚合编辑弹窗中；增值费复用同一套 scheme 上下文、权限和渠道选项，单独拆分会引入额外状态传递，超出本次小步范围。
- `RuoYi-Vue/finance/src/main/java/com/ruoyi/finance/service/impl/QuoteSchemeServiceImpl.java` 已超过 500 行。本次没有拆分，是因为方案、仓库、物流费、增值费共享同一套方案校验、渠道 lookup 和事务边界；后续如果继续增加费用规则维度，再考虑抽出规则子服务。

## 验证记录

- `react-ui`：`npm run tsc` 通过。
- `react-ui`：`npx jest --config jest.config.ts --runTestsByPath tests/finance-quote-scheme-contract.test.ts --runInBand` 通过。
- `RuoYi-Vue`：`mvn -pl finance,logistics,warehouse,buyer,ruoyi-admin -am -DskipTests compile` 通过。
- `RuoYi-Vue`：`mvn -pl ruoyi-system,finance,logistics,warehouse,buyer,ruoyi-admin -am "-Dtest=FinanceAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- `RuoYi-Vue`：`mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- 仓库根目录：`git diff --check` 通过；仅输出既有 LF/CRLF 警告。
- 仓库根目录：`codegraph sync .` 已完成。
- SQL 执行 postcheck 已验证远端库结构和权限 seed 落库成功。
- 后端 jar：先停止 8080 后端进程，再执行 `mvn -pl ruoyi-admin -am -DskipTests package` 通过。
- 后端运行态：通过 `start-backend-local.ps1 -Restart` 启动标准 jar，`http://127.0.0.1:8080` 返回 HTTP 200。
- API smoke：`admin/admin123` 登录成功；`GET /finance/admin/quote-schemes/list?pageNum=1&pageSize=1` 返回 HTTP 200、业务码 200、总数 2；`GET /finance/admin/quote-schemes/{schemeId}/value-fees/list` 返回 HTTP 200、业务码 200、空列表。
