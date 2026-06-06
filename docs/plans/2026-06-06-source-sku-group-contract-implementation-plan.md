# 来源 SKU 组绑定契约实现计划

## 目标

商品列表后续不再绑定单条 `connection_code + master_sku` 来源原始行，而是绑定“来源 SKU 组”，并通过来源商品库统一查询该来源 SKU 组对应的官方仓明细。

本计划只定义实现顺序和边界，不直接执行建表、DDL、DML 或菜单权限调整。

## 当前事实

1. 来源商品库当前已经从 `upstream_system_sku_candidate` 聚合展示官方主仓数据。
2. 当前聚合字段包括：
   - `sourceGroupKey`
   - `sourceConnectionCodes`
   - `sourceWarehouseNames`
   - `warehouseCount`
   - `sourceRowCount`
3. 当前 `sourceGroupKey` 是列表聚合行 key，包含来源 SKU、来源商品名、客户尺寸和 WMS 尺寸签名。
4. 商品设计旧文档仍保留 `source_ref_type = UPSTREAM_SKU`、`source_ref_id = connection_code + master_sku` 的预留说明。
5. 大量真实来源 SKU 同时存在于 CA012、NY013 等官方主仓；商品侧如果绑定单仓原始行，会拆碎同一个来源 SKU 的可发货仓库能力。

## 核心决策

### 1. 拆分两个 key

来源商品库需要同时提供两个不同含义的 key：

| 字段 | 含义 | 是否可落库为商品绑定依据 |
| --- | --- | --- |
| `sourceSkuGroupKey` | 稳定来源 SKU 组 key，只表达同一个真实来源 SKU 组 | 是 |
| `sourceDimensionGroupKey` | 尺寸拆行展示 key，表达同一来源 SKU 组下某一组客户尺寸 + 仓库尺寸 | 否 |

当前 `sourceGroupKey` 不建议直接作为商品绑定 key，因为它包含尺寸签名。WMS 尺寸补采或尺寸修正后，该 key 可能变化。

### 2. 稳定来源 SKU 组的判定规则

第一版只处理“官方主仓”：

```text
repositoryScope + trim(master_sku) + trim(master_product_name)
```

规则说明：

- 只做 `trim`。
- 不做大小写模糊。
- 不做相似商品名匹配。
- `repositoryScope` 必须进入 key，避免未来三方主仓与官方主仓 key 冲突。
- 建议 API 返回人类可读字段 `masterSku`、`masterProductName`，稳定 key 本身可使用 hash 形式，避免 `source_ref_id` 长度膨胀。

建议格式：

```text
OFFICIAL_MASTER:{sha256(trim(master_sku) + separator + trim(master_product_name))}
```

### 3. 商品侧绑定来源 SKU 组

商品侧后续绑定建议改为：

```text
source_type = SOURCE_PRODUCT
source_ref_type = SOURCE_SKU_GROUP
source_ref_id = sourceSkuGroupKey
```

旧方案中的 `UPSTREAM_SKU` 和 `connection_code + master_sku` 仅适合表达“单条上游 SKU 原始行”，不适合作为商品列表主绑定对象。

### 4. 仓库明细必须结构化

`sourceConnectionCodes`、`sourceWarehouseNames`、`warehouseCount`、`sourceRowCount` 可以继续作为列表摘要字段，但商品侧不能只靠这些逗号字符串保存官方仓能力。

来源商品库需要提供结构化官方仓明细，例如：

| 字段 | 说明 |
| --- | --- |
| `sourceSkuGroupKey` | 稳定来源 SKU 组 key |
| `connectionCode` | 官方仓接入编码 |
| `masterWarehouseName` | 官方仓名称 |
| `masterSku` | 来源 SKU |
| `masterProductName` | 来源商品名 |
| `productLength` / `productWidth` / `productHeight` / `productWeight` | 客户申报尺寸重量 |
| `wmsLength` / `wmsWidth` / `wmsHeight` / `wmsWeight` | WMS 尺寸重量 |
| `syncStatus` | 同步状态 |
| `pairingStatus` | 配对状态 |
| `lastSyncedAt` | 最近同步时间 |

仓库尺寸为空时，前端展示 `-`，不再额外显示“一致/不一致”文案。

## 实施阶段

## 阶段 1：来源商品库契约补齐

### 后端

1. 在 `SourceProductItem` 增加：
   - `sourceSkuGroupKey`
   - `sourceDimensionGroupKey`
2. 保留当前 `sourceGroupKey`，短期兼容前端当前 rowKey。
3. `selectSourceProductList` 同时返回：
   - `source_sku_group_key`
   - `source_dimension_group_key`
   - 兼容字段 `source_group_key`
4. `source_sku_group_key` 只基于 `repositoryScope + master_sku + master_product_name` 计算。
5. `source_dimension_group_key` 继续基于来源 SKU 组 + 客户尺寸 + WMS 尺寸计算。
6. 官方主仓仍按 `repositoryScope = OFFICIAL_MASTER` 过滤；三方主仓第一版继续保留空结果或占位逻辑。

### 前端

1. 类型声明增加：
   - `sourceSkuGroupKey`
   - `sourceDimensionGroupKey`
2. 来源商品库表格 rowKey 改为优先使用 `sourceDimensionGroupKey`，兼容回退到 `sourceGroupKey`。
3. 列表展示继续使用 `sourceWarehouseNames`、`warehouseCount`、`sourceRowCount`。
4. 不在列表中展示“一致/不一致”；仓库尺寸为空展示 `-`。

### 验证

1. 用 `KATGJ-SS-B885` 验证 CA012 / NY013 返回同一个 `sourceSkuGroupKey`。
2. 如果尺寸不同，允许拆成多个 `sourceDimensionGroupKey`。
3. `sourceSkuGroupKey` 不随 WMS 尺寸补采变化。

## 阶段 2：来源 SKU 组官方仓明细接口

### 后端

新增只读查询能力，建议接口：

```text
GET /integration/admin/source-products/group-detail
```

查询参数：

```text
repositoryScope
sourceSkuGroupKey
```

返回结构：

```text
group: 来源 SKU 组摘要
dimensionGroups: 尺寸拆行分组
warehouses: 官方仓明细列表
```

说明：

- 商品列表侧只能消费该接口或后端 service，不允许自己复制来源商品库 SQL 聚合规则。
- 该接口不写 `upstream_system_sku_candidate`，只读取当前同步快照。
- 如果未来需要开放给外部系统查询，可以在此契约稳定后再抽独立 DTO。

### 前端

1. 来源商品库详情抽屉改为优先展示结构化 `warehouses`。
2. `sourceConnectionCodes`、`sourceWarehouseNames` 保留为摘要，不作为明细数据源。

### 验证

1. 同一个 `sourceSkuGroupKey` 能查出多个官方仓。
2. 官方仓明细数量与 `warehouseCount` 对得上。
3. 原始明细数量与 `sourceRowCount` 对得上。

## 阶段 3：商品侧绑定契约改造

### 文档更新

更新商品设计文档中过期说明：

- `source_ref_type = UPSTREAM_SKU` 改为 `SOURCE_SKU_GROUP`。
- `source_ref_id = connection_code + master_sku` 改为 `sourceSkuGroupKey`。
- 明确 `connection_code` 只属于来源 SKU 组下的官方仓明细，不是商品主绑定 key。

### 后端

1. 商品创建或快捷创建时，接收 `sourceSkuGroupKey`。
2. 后端通过来源商品库 service 查询该来源 SKU 组摘要和官方仓明细。
3. 商品侧保存主绑定：
   - `source_type = SOURCE_PRODUCT`
   - `source_ref_type = SOURCE_SKU_GROUP`
   - `source_ref_id = sourceSkuGroupKey`
4. 商品侧展示仓库数量、可发货仓库等信息时，优先从绑定明细或来源查询契约获得，不再按 `connection_code + master_sku` 自己聚合。

### 前端

1. 商品列表或快捷创建选择来源商品时，选择对象是来源 SKU 组。
2. UI 展示来源 SKU、来源商品名、官方仓数量、官方仓名称摘要。
3. 不允许把 CA012 和 NY013 两条同 SKU 记录当成两个可创建商品对象。

## 阶段 4：是否新增商品侧快照表

来源商品库侧不建议为了 `warehouseCount`、`sourceWarehouseNames` 这类派生字段新增聚合表。

如果商品侧需要保存“创建商品当时该来源 SKU 组的官方仓能力”，建议新增商品模块内的绑定明细表。该表需要单独确认后才能执行 DDL。

第一版建议方向：

```text
product_source_warehouse_binding
```

候选字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `product_id` | 商品 SPU ID |
| `sku_id` | 商品 SKU ID，是否必填需确认绑定粒度 |
| `source_sku_group_key` | 来源 SKU 组 key |
| `repository_scope` | 官方主仓 / 三方主仓 |
| `master_sku` | 来源 SKU 快照 |
| `master_product_name` | 来源商品名快照 |
| `connection_code` | 官方仓接入编码 |
| `master_warehouse_name` | 官方仓名称快照 |
| `product_length` / `product_width` / `product_height` / `product_weight` | 客户尺寸重量快照 |
| `wms_length` / `wms_width` / `wms_height` / `wms_weight` | WMS 尺寸重量快照 |
| `bind_status` | 绑定状态 |
| `snapshot_time` | 快照时间 |
| `create_by` / `create_time` / `update_by` / `update_time` | 若依审计字段 |

是否落这张表取决于商品侧业务是否要求历史追溯：

- 只需要当前实时仓库能力：不建表，实时查来源商品库明细接口。
- 需要创建时快照、下架释放、换绑审计、仓库能力变化追踪：建商品侧绑定明细表。

## 阶段 5：验证与记录

### 必跑验证

1. 后端编译：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl integration -am -DskipTests compile
mvn -pl product -am -DskipTests compile
```

2. 来源商品库只读数据验证：

```text
KATGJ-SS-B885
```

验证点：

- CA012 / NY013 同一个来源商品返回同一个 `sourceSkuGroupKey`。
- 多仓明细能完整返回。
- 仓库尺寸为空展示 `-`。
- WMS 尺寸补采后 `sourceSkuGroupKey` 不变化。

3. 前端验证：

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run typecheck
```

如果项目当前没有独立 typecheck 脚本，则记录未执行原因，并至少通过浏览器验证来源商品库页面。

4. CodeGraph：

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

### 记录要求

实现完成后补一份 Markdown 执行记录，至少包含：

- 改动文件
- 接口契约变化
- 数据库是否变更
- 权限是否变更
- 字典是否变更
- 验证命令
- 未验证原因
- 残留问题

## 待确认问题

1. 商品侧绑定粒度是 SPU 级，还是 SKU 级？
2. 快捷创建商品时，同一个来源 SKU 组是否默认绑定全部官方仓？
3. 商品侧是否需要保存官方仓明细快照表，还是实时查询来源商品库即可？
4. `source_ref_type` 是否正式采用 `SOURCE_SKU_GROUP` 这个 code？
5. 当前 WMS 尺寸补采未完成前，商品侧是否允许先用客户申报尺寸作为默认尺寸，并标记尺寸来源？

## 推荐推进顺序

1. 先做阶段 1 和阶段 2，补齐来源商品库稳定契约。
2. 再更新商品侧文档和绑定逻辑，避免商品侧复制聚合规则。
3. 最后根据是否需要追溯，再决定是否新增商品侧绑定明细表。

推荐不要先建来源商品库聚合表。当前问题本质是查询契约和绑定粒度不清楚，不是缺少一张来源库聚合缓存表。
