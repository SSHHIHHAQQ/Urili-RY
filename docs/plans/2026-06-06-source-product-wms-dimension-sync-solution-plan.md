# 来源商品库 WMS 尺寸同步补采方案

日期：2026-06-06

## 背景

来源商品库当前展示的仓库尺寸、仓库重量来自 `upstream_system_sku_candidate` 的 `wms_length / wms_width / wms_height / wms_weight` 字段。

这些字段已经存在，前端、后端查询和字段映射也已经覆盖。问题不是展示层漏字段，而是当前同步请求没有拿到 OMS 页面展示的 WMS 字段值。

## 当前事实

当前代码路径：

- `LingxingOpenApiClient.listProductSkuPage(...)`
- 请求：`POST https://api.xlwms.com/openapi/v1/product/pagelist`
- 当前请求 `data` 只包含：

```json
{
  "page": 1,
  "pageSize": 100
}
```

当前同步日志和运行库数据表现：

- `length / width / height / weight` 有值，对应 OMS 尺寸和 OMS 重量。
- `wmsLength / wmsWidth / wmsHeight / wmsWeight` 在分页全量请求中返回空字符串。
- 本地解析逻辑把空字符串转为 `null` 后落库，所以来源商品库看不到仓库尺寸。

## 官方文档依据

官方 OMS v1 产品分页文档显示，`/v1/product/pagelist` 请求参数除 `page / pageSize` 外，还支持：

- `skuList`
- `approveStatus`
- `type`

响应 `ProductVO` 中明确包含：

- `length / width / height / weight`
- `wmsLength / wmsWidth / wmsHeight / wmsWeight`
- 对应英制字段 `*Bs`

官方 WMS v2 文档中：

- `/v2/product/page` 当前公开响应只包含 `sku / productName / approveStatus / createTime`。
- `/v2/inventory/productPage` 当前公开响应只包含库存位置和库存数量。

所以从公开文档看，优先解法仍应在 OMS v1 `/product/pagelist` 上补齐请求口径，而不是直接切到 WMS v2。

## 只读探测结果

目标 SKU：`KATGJ-SS-B885`

目标接入：`LX-CA012`

数据源确认：

- 连接来源：本机 `.env.local` 中的 `RUOYI_DB_*` 和 `URILI_SECRET_ENCRYPTION_KEY`。
- 查询类型：只读查询 `upstream_system_connection` 凭证密文，并在内存中解密后发起领星 OpenAPI 只读请求。
- 数据影响：没有执行 DDL / DML，没有写本地库，也没有写远端领星数据。
- 输出处理：未输出 appKey、appSecret、签名、authcode 或原始响应。

探测口径：

| 请求口径 | 结果 |
| --- | --- |
| `page/pageSize` | 能命中 SKU，但 `wmsLength / wmsWidth / wmsHeight / wmsWeight` 为空 |
| `page/pageSize + skuList=["KATGJ-SS-B885"]` | 返回 `wmsLength=4.000`、`wmsWidth=29.000`、`wmsHeight=38.000`、`wmsWeight=1.229` |
| `page/pageSize + sku="KATGJ-SS-B885"` | `sku` 参数不符合当前官方文档口径，表现等同未精确筛选，WMS 字段仍为空 |

结论：

当前根因是同步口径不完整。官方 `skuList` 精确查询可以拿到 OMS 页面展示的 WMS 尺寸重量。

## 推荐方案

不改表结构，复用现有 `wms_*` 字段。

### 1. 扩展领星客户端

在 `LingxingOpenApiClient` 中新增支持 `skuList` 的产品分页方法，例如：

- `listProductSkuPage(int current, int size, String traceId)` 保持当前全量发现 SKU 的行为。
- 新增 `listProductSkuPageBySkuList(List<String> skuList, String traceId)` 或内部参数对象，使用官方 `skuList`。

注意点：

- 使用官方字段名 `skuList`，不要使用旧代码里出现过的 `sku`。
- 分批请求，建议每批最多 50 个 SKU，降低接口兼容和响应体风险。
- 复用当前签名、重试、超时、脱敏日志逻辑。
- 新操作日志类型建议用 `SKU_WMS_SUPPLEMENT`，便于和全量 `SKU_SYNC` 区分。

### 2. 调整 SKU 同步流程

当前全量分页仍作为 SKU 清单来源：

1. 调用 `page/pageSize` 分页获取完整 SKU 集合。
2. 对当前页或当前批次的 SKU 再用 `skuList` 做精确补采。
3. 如果补采响应中存在同 SKU 记录，用补采记录覆盖同 SKU 的产品快照字段，尤其是 `wms_*` 和 `source_payload_json`。
4. 再执行现有 `upsertSkuCandidates(...)`。
5. `markMissingSkus(...)` 仍只基于全量分页批次判断，不由补采接口决定缺失。

这样可以保证：

- 仍能发现全量 SKU。
- `wms_*` 能拿到 OMS 页面一致的值。
- 不引入新表。
- 不把 OMS 页面内部接口写进后端。

### 3. 错误处理建议

补采失败时建议让本次 SKU 同步失败并记录错误，而不是静默落库半成品。

原因：

- 当前来源商品库后续要用于客户尺寸和仓库尺寸对比。
- 如果 WMS 字段缺失但同步状态显示成功，会继续造成误判。

如果担心接口压力，可以后续再增加“只补缺失 WMS 字段”的增量任务，但首版应先保证数据正确。

### 4. 来源商品库合并逻辑的衔接

WMS 字段补齐后，再做官方主仓来源商品合并会更稳：

- 同一来源 SKU + 来源商品名完全一致：先视为同一产品组。
- 客户尺寸一致、仓库尺寸一致：合并展示一条。
- 客户尺寸一致、仓库尺寸不同：按仓库尺寸拆开展示。
- 客户尺寸不同：不应强行合并，需要单独标识差异。

没有 WMS 字段前，不建议先做这层合并，否则会把真实的仓库尺寸差异隐藏掉。

## 验证标准

实现后至少验证：

1. 对 `LX-CA012 / KATGJ-SS-B885` 触发同步后，数据库中：
   - `wms_length = 4.0000`
   - `wms_width = 29.0000`
   - `wms_height = 38.0000`
   - `wms_weight = 1.2290`
2. 来源商品库接口返回同样的 `wms*` 值。
3. 前端来源商品库展示仓库尺寸 `4 x 29 x 38 cm`、仓库重量 `1.229 kg`。
4. 请求日志能区分全量 `SKU_SYNC` 和补采 `SKU_WMS_SUPPLEMENT`。
5. 全量 SKU 数量不因补采接口变化而减少。

## 风险

- 外部接口调用量会增加。按每页 100 个 SKU、补采批量 50 个估算，每 5400 个 SKU 约增加 108 次只读请求。
- 需要确认领星接口限流。如果遇到限流，应增加批次间隔或把补采拆成后台任务。
- 当前文档没有说明 `skuList` 数量上限，首版应保守使用 50。

## 结论

推荐先按 OMS v1 `skuList` 精确补采方案落地。

这条路径已经通过只读请求验证能拿到 OMS 页面一致的 WMS 字段值，改动范围小，不需要新增表，也不依赖不稳定的 OMS 页面内部接口。
