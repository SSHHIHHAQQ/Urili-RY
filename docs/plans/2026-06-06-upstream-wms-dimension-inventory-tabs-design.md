# 上游系统管理 WMS 尺寸重量与 SKU 库存同步设计审核稿

日期：2026-06-06

## 目标

本设计用于扩展管理端「上游系统管理」页面和 integration 模块能力：

1. 拉取每个来源 SKU 的 WMS 尺寸重量，也就是仓库实测尺寸重量。
2. 拉取每个来源 SKU 在主仓/仓库下的库存快照。
3. 页面增加对应 Tabs，方便运营查看同步状态、异常和配对情况。

本稿只做设计，不执行 SQL，不修改代码。

## 已确认事实

- 当前若依项目已经有 `integration` 模块和「上游系统管理」页面。
- 当前 SKU 同步走领星 OMS v1 `POST /openapi/v1/product/pagelist`，只传 `page/pageSize`。
- 真实验证结果显示：普通分页列表返回 `wmsLength/wmsWidth/wmsHeight/wmsWeight` 为空；按 `skuList` 精准查询同一 SKU 时会返回 WMS 尺寸重量。
- 当前 `upstream_system_sku_candidate` 已有 WMS 字段：
  - `wms_height`
  - `wms_length`
  - `wms_width`
  - `wms_weight`
  - 对应英制字段也已存在。
- 当前 `LingxingOpenApiClient` 已统一封装签名、超时、重试、traceId、脱敏请求日志，可以继续扩展，不应另写一套领星请求逻辑。
- 公开 OMS v1 文档能确认产品列表接口和 `reqTime/authcode` 签名规则；库存 API 路径需要领星工作人员确认。
- 公开 WMS v2 智能设备文档里有库存接口，但该文档使用 `timestamp/sign`，且 appKey 来源是智能设备 API，不是当前 OMS v1 的 `reqTime/authcode` 体系；本设计不直接采用 WMS v2。

## 总体原则

1. 上游系统管理只保存「上游快照」和「配对关系」。
2. 不把领星库存直接写成 URILI 可售库存事实源。
3. 真正可售库存、锁定库存、库存流水仍归后续 `inventory` 模块。
4. 外部请求日志继续只追加，敏感信息继续脱敏。
5. WMS 尺寸重量优先补齐到现有 `upstream_system_sku_candidate`，不重复建一张 SKU 尺寸表。
6. SKU 库存单独建上游库存快照表，避免把库存数量塞进 SKU 同步清单。

## 页面 Tabs 设计

建议调整为 6 个 Tabs：

| Tab | 用途 | 数据来源 | 是否新增表 |
| --- | --- | --- | --- |
| 领星仓库同步清单 | 查看上游仓库与系统仓库配对 | `upstream_system_warehouse_candidate` / pairing | 否 |
| 领星物流渠道同步清单 | 查看上游渠道与系统渠道配对 | `upstream_system_logistics_channel_candidate` / pairing | 否 |
| 领星 SKU 同步清单 | 查看 SKU 基础资料和 SKU 配对 | `upstream_system_sku_candidate` / pairing | 否 |
| 仓库尺寸重量 | 专门查看 WMS 尺寸重量补全情况 | `upstream_system_sku_candidate` 的 WMS 字段 | 否 |
| SKU 库存同步清单 | 查看上游 SKU 库存快照 | 新增上游库存快照表 | 是 |
| 请求日志 | 查看外部请求日志 | `upstream_system_request_log` | 否 |

### 为什么把「仓库尺寸重量」单独成 Tab

WMS 尺寸重量和 SKU 基础资料虽然都来自产品接口，但同步口径不同：

- SKU 基础资料：分页列表，适合全量拉取。
- WMS 尺寸重量：需要按 SKU 精准补查，否则列表口径可能为空。

如果混在 SKU 同步清单里，运营无法快速看到哪些 SKU 已有仓库实测尺寸，哪些还缺失。单独 Tab 更清楚。

### 为什么把「SKU 库存同步清单」单独成 Tab

库存不是 SKU 主数据字段，而是按仓库、库存类型、库存属性变化的快照。它后续还会和库存模块、订单锁定、履约扣减发生关系，不能塞进 SKU 同步清单。

## WMS 尺寸重量同步设计

### 接口策略

当前建议两步走：

1. 继续用普通分页拉取 SKU 基础资料。
2. 对本批次返回的 SKU 按批次调用产品列表接口的精准查询口径补齐 WMS 字段。

建议新增 client 方法：

```text
LingxingOpenApiClient.listProductSkuPageBySkuList(List<String> skuList, traceId)
```

请求仍走：

```text
POST /openapi/v1/product/pagelist?authcode=...
```

data 暂定：

```json
{
  "page": 1,
  "pageSize": 100,
  "skuList": ["SKU1", "SKU2"]
}
```

需要向领星确认：

- `skuList` 是否是正式支持字段。
- `skuList` 单次最大数量。
- `skuList` 传 masterSku 还是产品 SKU 字段。
- 普通分页 WMS 为空、`skuList` 有值是否符合预期。

### 同步流程

1. `syncSkusOnly(connectionCode)` 开始同步。
2. 分页拉取 SKU 基础清单，写入 `upstream_system_sku_candidate`。
3. 收集本页 SKU，按 `skuList` 批量补查 WMS 尺寸重量。
4. 只更新 WMS 字段、WMS 相关原始快照和 `update_time`。
5. 如果补查失败，不回滚已同步的 SKU 基础资料；记录请求日志和 SKU 同步状态错误摘要。
6. 定时任务也走同一套逻辑，避免手动同步和定时同步结果不一致。

### 关键防坑

普通分页返回 WMS 空字符串，不能在后续定时同步时把已经补齐的 WMS 字段覆盖成空。

建议 Mapper upsert 调整为：

- 基础 SKU 同步时：如果本次 `wms_*` 为 null，不覆盖原有非 null WMS 字段。
- WMS 精准补查时：只有接口返回有效数值时才覆盖 WMS 字段。

### 页面展示字段

「仓库尺寸重量」Tab 建议列：

| 字段 | 说明 |
| --- | --- |
| 领星 masterSku | 来源 SKU |
| 领星产品名 | 来源产品名称 |
| 产品尺寸 | `product_length * product_width * product_height cm` |
| 产品重量 | `product_weight kg` |
| WMS 尺寸 | `wms_length * wms_width * wms_height cm` |
| WMS 重量 | `wms_weight kg` |
| 尺寸状态 | 已补齐 / 缺失 / 部分缺失 |
| 系统 SKU | 已配对的系统 SKU |
| 系统 SKU 名称 | 已配对的系统 SKU 名称 |
| 最近发现 | `last_seen_time` |
| 更新时间 | `update_time` |

筛选：

- 关键词：masterSku / 产品名 / 系统 SKU。
- 尺寸状态：全部、已补齐、缺失、部分缺失。
- 配对状态：全部、已配对、未配对。

操作：

- 同步仓库尺寸重量。
- 可选：只补齐缺失 WMS 的 SKU。

## SKU 库存同步设计

### API 前置确认

库存 API 当前不能硬编码。需要领星确认 OMS v1 可用接口：

1. 产品库存分页查询的接口路径。
2. 是否仍使用 OMS v1 `appKey + reqTime + data + authcode`。
3. 是否支持按仓库编码查询。
4. 是否支持按 SKU 列表查询。
5. 返回字段里的总库存、可用库存、锁定库存、在途库存、库存属性、批次、库位分别叫什么。
6. 库存是否按产品库存、箱库存、退货库存、综合库存分接口或分字段返回。

在接口未确认前，后端设计可以先预留适配层和表，不直接实现真实调用。

### 新增表：`upstream_system_sku_inventory_snapshot`

业务目的：保存每个主仓接入下，来源 SKU 在上游仓库中的库存快照。

业务逻辑：

- 这是上游库存快照，不是 URILI 库存事实源。
- 同一同步维度只保留最新快照，可更新。
- 请求日志仍只追加，原始响应行保存在快照表用于排查。
- 后续如要入账到库存模块，必须由 `inventory` 服务根据 SKU 配对、仓库配对生成库存流水，不能由 integration 直接写库存事实表。

建议字段：

| 字段 | 类型 | 必填 | 注释 |
| --- | --- | --- | --- |
| `inventory_snapshot_id` | bigint | 是 | 主键，自增 |
| `connection_code` | varchar(64) | 是 | 主仓接入编号 |
| `upstream_warehouse_code` | varchar(100) | 是 | 领星仓库代码 |
| `upstream_warehouse_name` | varchar(200) | 否 | 领星仓库名称快照 |
| `master_sku` | varchar(128) | 是 | 领星 masterSku / SKU |
| `master_product_name` | varchar(255) | 否 | 领星产品名称快照 |
| `inventory_scope` | varchar(32) | 是 | 库存口径，例如 PRODUCT / BOX / RETURN / COMPREHENSIVE |
| `inventory_attribute` | varchar(32) | 否 | 库存属性，例如正品、次品；保存上游 code |
| `batch_no` | varchar(100) | 否 | 批次号；如果接口返回聚合库存可为空 |
| `location_code` | varchar(100) | 否 | 库位编码；如果接口返回聚合库存可为空 |
| `total_quantity` | bigint | 是 | 总库存 |
| `available_quantity` | bigint | 是 | 可用库存 |
| `locked_quantity` | bigint | 是 | 锁定库存 |
| `in_transit_quantity` | bigint | 是 | 在途库存 |
| `boxed_quantity` | bigint | 否 | 已装箱库存；接口无该字段时为空 |
| `unboxed_quantity` | bigint | 否 | 未装箱库存；接口无该字段时为空 |
| `system_warehouse_code` | varchar(64) | 否 | 已配对系统仓库代码快照 |
| `system_warehouse_name` | varchar(200) | 否 | 已配对系统仓库名称快照 |
| `system_sku` | varchar(128) | 否 | 已配对系统 SKU 快照 |
| `system_sku_name` | varchar(255) | 否 | 已配对系统 SKU 名称快照 |
| `customer_name` | varchar(200) | 否 | 客户名称快照 |
| `status` | varchar(16) | 是 | 同步清单状态，ACTIVE / MISSING |
| `sync_batch_id` | varchar(64) | 是 | 同步批次号 |
| `source_payload_json` | longtext | 否 | 上游库存原始行 JSON |
| `source_payload_hash` | varchar(64) | 否 | 上游库存原始行哈希 |
| `first_seen_time` | datetime | 是 | 首次发现时间 |
| `last_seen_time` | datetime | 是 | 最近发现时间 |
| `update_time` | datetime | 是 | 更新时间 |

建议唯一约束：

```text
uk_upstream_inventory_snapshot
(connection_code, upstream_warehouse_code, master_sku, inventory_scope, inventory_attribute, batch_no, location_code)
```

说明：

- 如果领星库存接口只返回 SKU + 仓库聚合，不返回批次/库位，则 `batch_no/location_code` 统一为空，唯一键等同于 SKU 仓库库存快照。
- 如果领星返回批次/库位明细，表结构也能承接。

建议索引：

| 索引 | 字段 | 用途 |
| --- | --- | --- |
| `idx_upstream_inventory_connection_status` | `connection_code, status` | 按主仓和状态筛选 |
| `idx_upstream_inventory_sku` | `connection_code, master_sku` | 查 SKU 库存 |
| `idx_upstream_inventory_warehouse` | `connection_code, upstream_warehouse_code` | 查仓库库存 |
| `idx_upstream_inventory_system_sku` | `connection_code, system_sku` | 查已配对系统 SKU |
| `idx_upstream_inventory_update` | `connection_code, update_time` | 最近同步排序 |

### 新增表：`upstream_system_inventory_sync_state`

业务目的：记录每个主仓的库存同步状态，供页面展示和定时任务判断。

建议字段：

| 字段 | 类型 | 必填 | 注释 |
| --- | --- | --- | --- |
| `connection_code` | varchar(64) | 是 | 主仓接入编号，主键 |
| `status` | varchar(16) | 是 | 同步状态：NEVER / SYNCING / FRESH / FAILED |
| `sync_batch_id` | varchar(64) | 否 | 最近同步批次号 |
| `last_started_time` | datetime | 否 | 最近开始同步时间 |
| `last_finished_time` | datetime | 否 | 最近结束同步时间 |
| `last_success_time` | datetime | 否 | 最近成功同步时间 |
| `next_sync_time` | datetime | 否 | 下次计划同步时间 |
| `total_count` | int | 是 | 最近同步总行数 |
| `active_count` | int | 是 | 最近同步有效行数 |
| `missing_count` | int | 是 | 最近标记上游缺失行数 |
| `last_error_message` | varchar(500) | 否 | 最近失败原因 |
| `update_time` | datetime | 否 | 更新时间 |

### 同步流程

1. 校验主仓启用、凭证可用。
2. 加内存并发锁，防止同一主仓重复库存同步。
3. 创建 `syncBatchId`，写 `SYNCING` 状态。
4. 按仓库或分页拉取库存。
5. 映射仓库配对和 SKU 配对快照。
6. upsert `upstream_system_sku_inventory_snapshot`。
7. 本批次未出现的旧库存行标记为 `MISSING`。
8. 更新 `FRESH / FAILED` 状态和统计字段。
9. 全部请求写入 `upstream_system_request_log`，operation 建议为 `INVENTORY_SYNC`。

### 页面展示字段

「SKU 库存同步清单」Tab 建议列：

| 字段 | 说明 |
| --- | --- |
| 领星仓库 | 上游仓库代码 / 名称 |
| 系统仓库 | 配对后的系统仓库 |
| 领星 masterSku | 来源 SKU |
| 领星产品名 | 来源产品名称 |
| 库存口径 | 产品库存 / 箱库存 / 退货库存 / 综合库存 |
| 库存属性 | 正品 / 次品等上游属性 |
| 总库存 | 上游总库存 |
| 可用库存 | 上游可用库存 |
| 锁定库存 | 上游锁定库存 |
| 在途库存 | 上游在途库存 |
| 系统 SKU | 配对后的系统 SKU |
| 客户名称 | SKU 配对快照 |
| 同步状态 | ACTIVE / MISSING |
| 最近同步 | `last_seen_time` |

筛选：

- 关键词：领星 SKU、产品名、系统 SKU、客户名称。
- 仓库：上游仓库或系统仓库。
- 库存口径。
- 库存属性。
- 同步状态。
- 配对状态：系统仓库已配/未配，系统 SKU 已配/未配。

操作：

- 同步库存。
- 查看原始快照摘要。
- 不提供库存编辑、覆盖、删除。

## 前端交互建议

1. 顶部主仓摘要继续默认收起。
2. 每个同步类 Tab 顶部只保留一行简洁状态：
   - 同步状态
   - 上次成功
   - 下次同步
   - 最近数量
   - 手动同步按钮
3. 全局右上「同步」按钮可以改成下拉：
   - 同步全部
   - 同步仓库/渠道
   - 同步 SKU 基础资料
   - 同步仓库尺寸重量
   - 同步 SKU 库存
4. 表格继续铺满页面，分页固定底部。
5. 不在页面上展示“候选”字样，继续使用“同步清单”。

## 权限建议

当前可继续复用：

- `integration:upstream:list`
- `integration:upstream:query`
- `integration:upstream:sync`
- `integration:upstream:pair`
- `integration:upstream:log`

如果要更细，建议新增按钮权限：

| 权限 | 用途 |
| --- | --- |
| `integration:upstream:dimensionSync` | 手动同步 WMS 尺寸重量 |
| `integration:upstream:inventorySync` | 手动同步 SKU 库存 |
| `integration:upstream:inventoryQuery` | 查看 SKU 库存同步清单 |

首版也可以先复用 `integration:upstream:sync`，避免权限 seed 过早膨胀。

## 定时任务建议

当前已有若依 Quartz：

```text
upstreamSystemTask.syncSkus
```

建议改成拆分任务：

```text
upstreamSystemTask.syncSkus
upstreamSystemTask.syncWmsDimensions
upstreamSystemTask.syncInventory
```

首版默认：

- SKU 基础 + WMS 尺寸：10 分钟一次。
- 库存：如果接口性能允许，10 分钟一次；如果领星限流明显，先 15 或 30 分钟一次。

不建议在一个任务里无条件同步所有能力，否则库存接口失败会影响 SKU 基础清单刷新。

## 与库存模块的边界

上游库存快照不是库存事实源。后续要把领星库存纳入平台库存时，必须走独立库存模块：

1. integration 拉取并保存上游库存快照。
2. inventory 服务读取快照、校验系统仓库配对、SKU 配对。
3. inventory 服务写 `inventory_sku_warehouse_stock` 当前库存。
4. inventory 服务追加 `inventory_stock_movement` 流水。
5. 商品列表只读 inventory 聚合，不直接读上游库存快照作为可售库存。

## 我建议的实施顺序

### 第一阶段：修 WMS 尺寸重量

1. 扩展 `LingxingOpenApiClient` 支持 `skuList` 精准查询。
2. SKU 同步后补查 WMS 尺寸重量。
3. 防止分页同步把已补齐 WMS 字段覆盖成空。
4. 增加「仓库尺寸重量」Tab。
5. 请求日志 operation 增加 `SKU_DIMENSION_SYNC`。

原因：这是当前已经实测能解决的问题，且不需要新建库存事实表。

### 第二阶段：库存接口确认和上游库存快照

1. 向领星确认 OMS v1 库存接口。
2. 新增上游库存快照表和库存同步状态表。
3. 扩展 client 和 service。
4. 增加「SKU 库存同步清单」Tab。
5. 加定时任务。

原因：库存接口路径和字段未确认，不能先硬编码。

### 第三阶段：库存模块入账

1. 等库存模块表和业务规则确认。
2. 根据上游快照生成库存流水。
3. 商品列表接入真实库存聚合。

原因：这一步会影响交易事实源，必须和库存模块规则一起做。

## 需要你审核确认的问题

1. Tabs 是否按「仓库 / 物流渠道 / SKU / 仓库尺寸重量 / SKU库存 / 请求日志」这个顺序。
2. WMS 尺寸重量是否允许先用 `skuList` 精准查询方案落地，同时让领星确认它是正式支持口径。
3. SKU 库存 Tab 首版是否只做「上游库存快照」，不直接写平台可售库存。
4. 库存同步频率首版是否和 SKU 一样 10 分钟，还是先保守 15/30 分钟。
5. 权限首版是否复用 `integration:upstream:sync`，还是新增 `dimensionSync` / `inventorySync`。

## 本轮未执行

- 未执行 SQL。
- 未修改 Java/React 代码。
- 未连接数据库。
- 未启动服务或跑测试。

原因：本轮只做设计审核稿，等待确认后再实现。
