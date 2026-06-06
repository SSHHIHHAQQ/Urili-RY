# 来源 SKU 组绑定契约实现记录

## 目标

实现来源商品库侧“来源 SKU 组”稳定契约，使商品列表后续可以绑定 `sourceSkuGroupKey`，而不是绑定单条 `connection_code + master_sku` 原始行。

## 已完成改动

### 后端

1. `SourceProductItem` 新增：
   - `sourceSkuGroupKey`：稳定来源 SKU 组 key，可作为商品侧绑定依据。
   - `sourceDimensionGroupKey`：尺寸拆行展示 key，不作为商品侧绑定依据。
2. `SourceProductQuery` 新增 `sourceSkuGroupKey` 查询条件。
3. `AdminSourceProductController` 新增只读接口：

```text
GET /integration/admin/source-products/group-detail
```

4. `IUpstreamSystemService` / `UpstreamSystemServiceImpl` 新增来源 SKU 组详情查询。
5. `UpstreamSystemMapper` / XML 新增：
   - `selectSourceProductGroupSummary`
   - `selectSourceProductWarehouseDetailList`
6. `selectSourceProductList` 继续保留兼容字段 `sourceGroupKey`，但新增返回：
   - `source_sku_group_key`
   - `source_dimension_group_key`

### 前端

1. 来源商品库类型声明新增：
   - `sourceSkuGroupKey`
   - `sourceDimensionGroupKey`
   - `SourceProductGroupDetail`
2. 来源商品库表格 rowKey 改为优先使用 `sourceDimensionGroupKey`。
3. 详情抽屉打开时按 `sourceSkuGroupKey` 查询官方仓明细。
4. 详情抽屉新增“官方仓明细”表格，展示官方仓、客户尺寸、仓库尺寸、同步状态、配对状态。
5. 仓库尺寸为空继续显示 `-`。

### 文档

1. 商品表设计旧文档已将 `UPSTREAM_SKU` / `connection_code + master_sku` 调整为 `SOURCE_SKU_GROUP` / `sourceSkuGroupKey`。
2. 商品来源绑定计划已补充 `sourceSkuGroupKey` / `sourceDimensionGroupKey` 术语。

## 数据库与权限

- 未新增表。
- 未执行 DDL。
- 未执行 DML。
- 新接口沿用 `product:list:list` 权限。
- 未新增字典项。
- 未新增菜单或按钮权限。

## 只读接口验证

验证对象：

```text
KATGJ-SS-B885
```

验证结果：

| 项 | 结果 |
| --- | --- |
| 列表接口状态 | 200 |
| 列表聚合行数 | 1 |
| `sourceSkuGroupKey` | 有值 |
| `sourceDimensionGroupKey` | 有值 |
| `sourceGroupKey` 与 `sourceDimensionGroupKey` | 相同，用于兼容旧 rowKey |
| `warehouseCount` | 2 |
| `sourceRowCount` | 2 |
| 详情接口状态 | 200 |
| 官方仓明细数量 | 2 |
| 尺寸拆行数量 | 1 |
| 官方仓明细 | `LX-CA012 / CA012`、`LX-NY013-3275A1E1 / NY013` |
| 当前库内 WMS 尺寸 | 仍为空，显示 `-` |

说明：WMS 尺寸为空属于之前确认的补采数据源问题，本次只实现来源 SKU 组契约，不处理 WMS 补采。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl integration -am -DskipTests compile
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-admin -am -DskipTests package
```

结果：第一次因运行中的后端占用 `ruoyi-admin.jar` 导致 repackage rename 失败；停止 8080 Java 后端后重新执行，通过。

```powershell
cd E:\Urili-Ruoyi
.\start-backend-local.ps1 -Restart
```

结果：后端启动成功，`http://127.0.0.1:8080` 返回 HTTP 200，最新错误日志为空。

```text
浏览器验证：http://127.0.0.1:8001/product/list
```

结果：

- 官方主仓列表可渲染数据。
- 多仓摘要可显示 `CA012 / NY013` 和 `2 个仓`。
- 仓库尺寸为空显示 `-`。
- 详情抽屉可显示 `来源 SKU 组` 和“官方仓明细”表格。

## 残留问题

1. 商品列表侧尚未接入 `sourceSkuGroupKey` 绑定。
2. 是否新增商品侧官方仓明细快照表仍需单独确认，当前没有建表。
3. WMS 尺寸补采仍未实现，当前库内 `wms_*` 为空时继续显示 `-`。
4. 官方主仓默认列表在当前数据量下响应较慢，后续可考虑对来源商品库聚合查询做索引或分页查询优化。

## CodeGraph

已执行：

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

结果：Already up to date。
