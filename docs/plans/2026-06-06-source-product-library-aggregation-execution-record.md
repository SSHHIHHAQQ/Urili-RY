# 来源商品库官方主仓聚合展示执行记录

日期：2026-06-06

## 目标

来源商品库不再按 `connection_code + master_sku` 原始明细直接展示官方主仓数据，而是按来源商品聚合后展示。

本次只做官方主仓聚合和三方主仓 Tab 预留，不新增三方主仓数据源。

## 已完成

- 前端来源商品库新增 Tab：
  - 官方主仓
  - 三方主仓
- 官方主仓列表查询改为聚合结果。
- 三方主仓当前返回空结果，作为后续三方主仓接入预留。
- 聚合 key：
  - `master_sku`
  - `master_product_name`
  - 客户尺寸签名：`product_length / product_width / product_height / product_weight`
  - 仓库尺寸签名：`wms_length / wms_width / wms_height / wms_weight`
- 前端不展示“尺寸一致 / 不一致”文案。
- 仓库尺寸为空时继续显示 `-`。
- 聚合行新增只读展示字段：
  - `sourceGroupKey`
  - `sourceConnectionCodes`
  - `sourceWarehouseNames`
  - `warehouseCount`
  - `sourceRowCount`

## 外部 SKU 查询口径

当前不需要新增表。

现有 `upstream_system_sku_candidate` 已经是来源 SKU 快照明细表，外部只按来源 SKU 查询“有几个主仓/仓库”时，可以直接基于它聚合：

```sql
select master_sku,
       master_product_name,
       count(distinct connection_code) as warehouse_count,
       group_concat(distinct connection_code order by connection_code separator ',') as source_connection_codes
from upstream_system_sku_candidate
where master_sku = ?
group by master_sku, master_product_name;
```

如果后续其他模块需要稳定调用，不建议新建物化表；建议在后端补一个只读查询 DTO/API，仍然读 `upstream_system_sku_candidate` 聚合结果。

## 数据库影响

- 未新增表。
- 未执行 DDL。
- 未执行 DML。
- 只读验证使用当前 `.env.local` 的 `RUOYI_DB_*` 连接当前运行库。
- 只读验证目标：`upstream_system_sku_candidate`、`upstream_system_connection`、`upstream_system_sku_pairing`。

## 只读验证结果

目标 SKU：`KATGJ-SS-B885`

聚合查询结果：

- 聚合行数：1
- 覆盖主仓数：2
- 覆盖原始明细数：2
- 当前运行库 WMS 尺寸仍为空，所以页面仓库尺寸应显示 `-`

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl integration -am -DskipTests compile
mvn -pl ruoyi-admin -am -DskipTests package

cd E:\Urili-Ruoyi\react-ui
npm run tsc

cd E:\Urili-Ruoyi
.\start-backend-local.ps1 -Restart
codegraph sync .
```

结果：

- 后端编译通过。
- 后端打包通过，并已用新 jar 重启，`http://127.0.0.1:8080` 返回 HTTP 200。
- 前端 TypeScript 检查通过。
- 前端 `http://127.0.0.1:8001` 返回 HTTP 200。
- 浏览器验证 `/product/list`：
  - 官方主仓 Tab 加载聚合数据。
  - 页面展示 `CA012 / NY013`、`2 个仓`。
  - 仓库尺寸为空时显示 `-`。
  - 三方主仓 Tab 当前显示空数据，无控制台错误。
- CodeGraph 同步通过，结果为 `Synced 26 changed files`。

## 检查结果

- 权限检查：接口仍复用 `product:list:list`，未新增权限点。
- 字典/选项复用检查：来源系统、同步状态、配对状态继续复用 `react-ui/src/services/integration/constants.ts`。
- 复用台账检查：本次未新增公共组件或公共方法；仍复用既有来源商品库只读视图记录。
- 大文件合理性判断：本次修改文件未新增超过 300 行的大文件。
- 重复代码检查：未新增第二套来源商品表或重复同步逻辑；聚合仍基于 `upstream_system_sku_candidate`。

## 残留问题

- WMS 字段补采尚未落地；当前聚合后的仓库尺寸仍依赖已有 `wms_*` 数据。
- 三方主仓当前只是 Tab 和查询口径预留，没有实际数据源。
- 若后续外部模块大量按 SKU 查询聚合结果，应新增只读后端接口，而不是让前端或其他模块直接拼 SQL。
