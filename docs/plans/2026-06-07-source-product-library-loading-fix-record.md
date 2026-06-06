# 来源商品库加载过慢问题修复记录

## 问题

来源商品库页面进入后长时间处于加载状态，用户感知为“加载不出数据”。

## 根因

后端 `/integration/admin/source-products/list` 使用 `startPage()` 自动分页。

`startPage()` 会让 PageHelper 对来源商品库的大聚合 SQL 自动生成 count 查询。当前来源商品库 SQL 包含：

- `group by` 来源 SKU、来源商品名、客户尺寸、WMS 尺寸
- `group_concat`
- 多个 `max`
- `left join upstream_system_sku_pairing`
- 稳定 `sourceSkuGroupKey` / `sourceDimensionGroupKey` 表达式

PageHelper 的自动 count 会包裹整条聚合查询，导致默认列表接口耗时约 27.8 秒，前端 ProTable 一直显示 loading。

## 修复方式

1. 来源商品库列表 Controller 不再使用 `startPage()` 自动 count。
2. 新增 `countSourceProductList` 轻量 count：
   - 只保留过滤条件和聚合分组。
   - 不 select 大量字段。
   - 不做 `group_concat`。
   - 不做排序。
3. 当前页数据查询使用：

```java
PageHelper.startPage(pageNum, pageSize, false)
```

即关闭 PageHelper 自动 count，但保留当前页 limit。

## 数据库与权限

- 未新增表。
- 未执行 DDL。
- 未执行 DML。
- 未新增权限。
- 未新增菜单。
- 新增逻辑只读现有来源 SKU 同步快照。

## 验证结果

### 修复前

接口：

```text
GET /integration/admin/source-products/list?repositoryScope=OFFICIAL_MASTER&pageNum=1&pageSize=20
```

结果：

| 项 | 值 |
| --- | --- |
| 状态 | 200 |
| 总数 | 5649 |
| 返回行数 | 20 |
| 耗时 | 27848 ms |

### 修复后

同一接口结果：

| 项 | 值 |
| --- | --- |
| 状态 | 200 |
| 总数 | 5649 |
| 返回行数 | 20 |
| 耗时 | 553 ms |

### 目标 SKU 回归验证

验证 SKU：

```text
KATGJ-SS-B885
```

结果：

| 项 | 值 |
| --- | --- |
| 列表状态 | 200 |
| 列表总数 | 1 |
| `warehouseCount` | 2 |
| `sourceRowCount` | 2 |
| 详情状态 | 200 |
| 官方仓明细数量 | 2 |
| 官方仓 | `CA012:LX-CA012`、`NY013:LX-NY013-3275A1E1` |

### 前端验证

页面：

```text
http://127.0.0.1:8001/product/list
```

结果：

- 登录后来源商品库 5 秒内渲染出数据。
- 页面不再停留在 loading。
- 官方主仓列表显示 `2 个仓`。
- 仓库尺寸为空继续显示 `-`。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl integration -am -DskipTests compile
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-admin -am -DskipTests package
```

结果：通过。

```powershell
cd E:\Urili-Ruoyi
.\start-backend-local.ps1 -Restart
```

结果：后端启动成功，`http://127.0.0.1:8080` 返回 HTTP 200。

## 后续建议

1. 后续如果来源商品库数据量继续增长，需要补专门索引或考虑物化聚合结果。
2. 商品列表接入 `sourceSkuGroupKey` 时，应复用当前来源商品库查询契约，不要重新写聚合 SQL。
