# 上游系统管理 code 口径修正记录

## 目标

修正若依迁移版上游系统管理中偏离旧项目的 code 设计：

- 结算类型回到旧项目：`upstream-payable`、`self-operated-receivable`。
- 主仓新增时显式选择上游系统类型，当前只开放 `lingxing-wms`。
- 新生成的主仓接入编号回到旧项目规则：`LX-{主仓名前缀}-{8位随机后缀}`。

## 已修正

- 后端新增主仓请求支持 `systemKind`，缺省时兼容为 `lingxing-wms`。
- 后端保存 `systemKind` 时不再硬编码旧迁移值 `LINGXING_WMS`。
- 后端保存结算类型时不再转大写，统一保存旧项目 code。
- 后端保留兼容：`LINGXING_WMS`、`UPSTREAM_PAYABLE`、`PLATFORM_ADVANCE` 会归一到新 code。
- 前端新增主仓弹窗增加“上游系统类型”选择。
- 前端结算类型选项改为旧项目 label/code。
- SQL 种子脚本同步改为旧项目 code。
- 新增 `RuoYi-Vue/sql/20260604_upstream_system_code_correction.sql`，用于修正已落库的错误 code。

## 未执行事项

- 未直接执行远端数据库 SQL。
- 已有 `LX-CA012` 接入编号暂不自动改名，避免影响同步清单、配对和请求日志关联。

## 验证记录

- 后端编译：`mvn -pl integration -am -DskipTests package`，通过。
- 前端类型检查：`npm run tsc` 曾通过；补充前端旧值归一化后再次运行时，失败集中在无关文件 `src/components/PartnerManagement/PartnerAuditModal.tsx`，错误为 `hideInSearch` 字段和 `PartnerService` 日志方法类型不匹配，未改动该无关模块。
- 前端构建：`npm run build`，通过；仅有 Browserslist 数据过期提示。
- 前端格式检查：`npx biome check --write src/pages/UpstreamSystem src/types/integration/upstream-system.d.ts`，通过。
- 浏览器验证：打开 `http://127.0.0.1:8001/overseas-warehouse-service/upstream-system`，新增主仓弹窗可见“上游系统类型”，默认值为“领星WMS”；结算类型默认值为“上游仓（应付）”。编辑主仓弹窗中“上游系统类型”不可编辑，旧库值也能归一显示为“领星WMS / 上游仓（应付）”。

## 权限检查结果

本次不新增接口和权限点，继续使用现有：

- `integration:upstream:add`
- `integration:upstream:edit`
- `integration:upstream:credential`

## 字典/选项复用检查结果

`upstream_system_kind` 和 `upstream_settlement_type` 继续走若依字典，前端页面当前保留本地常量并兼容旧错误 code。后续如果全站字典 hook 稳定接入，可再收敛为远端字典。

## 复用台账检查结果

本次没有新增公共组件或公共后端适配器；沿用现有 `UpstreamSystemServiceImpl`、`ConnectionModal`、`constants.ts`。

## 大文件合理性判断结果

本次修改未新增超过 300 行的业务文件；`UpstreamSystemServiceImpl` 当前 699 行，是既有聚合服务。本次只补 code 归一化和编号生成规则，未继续扩大同步、配对、日志等职责边界。后续加入更多外部系统前，应拆出同步任务服务、配对服务和请求日志查询服务。

## 重复代码检查结果

本次没有新增重复表单或重复 Service；系统类型和结算类型选项集中在 `react-ui/src/pages/UpstreamSystem/constants.ts`。
