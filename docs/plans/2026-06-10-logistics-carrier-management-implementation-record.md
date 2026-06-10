# 物流商管理实施记录

日期：2026-06-10

## 范围

本次落地管理端物流商管理第一版，接入方先支持 `AGG56`。

已实现：

- 通用物流商接入表、物流商渠道、系统渠道、渠道映射、面单订单、面单包裹、外部请求日志。
- AGG56 私有凭证扩展表，凭证通过后端 API 加密保存，不进入通用表。
- AGG56 授权、物流商渠道同步、费用试算、创建面单、获取面单、取消面单后端能力。
- 管理端接口：`/logistics/admin/carriers/**`。
- 管理端页面：`Logistics/Carrier/index`，覆盖接入列表、授权、同步、物流商渠道、系统渠道、映射和请求日志。
- SQL 增量脚本：`RuoYi-Vue/sql/20260610_logistics_carrier_management.sql`，带确认 token 和菜单 slot guard。

## 边界

- `logistics_carrier_connection` 只保存通用连接字段。
- `logistics_agg56_connection` 只保存 AGG56 私有字段，例如 `app_token` / `app_key` 密文、AGG56 用户信息和客户代码。
- 报价结果只返回给调用方并进入脱敏请求日志，不写费用业务表。
- 创建面单时使用调用方传入的 `businessOrderNo` 作为全局唯一业务单号。
- 取消面单只更新旧面单状态；重新创建新面单是后续单独的创建动作。
- 本次未执行远端数据库 DDL/DML，只提交 guarded SQL 文件。

## 验证

| 验证项 | 结果 |
| --- | --- |
| 后端 reactor 编译 | 通过：`mvn -pl logistics,ruoyi-admin -am -DskipTests compile` |
| 前端 TypeScript | 通过：`npm run tsc` |
| SQL guard | 通过：`mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，80 项断言 |
| CodeGraph | 通过：`codegraph sync .`，索引已是最新状态 |
| 浏览器路由 | 前端 8001 已编译并可登录；当前运行库未执行菜单 SQL，`/logistics-carrier` 登录后仍为 404，需执行 `20260610_logistics_carrier_management.sql` 后复验 |

## 已知后续

- `logistics_final_carrier` 本次 SQL 先放常用与兜底项，完整用户清单仍需继续生成完整 seed。
- 未对远端库执行 `20260610_logistics_carrier_management.sql`；执行前需按项目规则确认目标数据源和确认 token。
- 前端页面第一版集中在一个工作台文件中，后续如果继续扩展面单管理 UI，应拆出表格列、弹窗和抽屉组件。
