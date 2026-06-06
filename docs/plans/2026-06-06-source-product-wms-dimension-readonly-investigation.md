# 来源商品库 WMS 尺寸只读核查记录

日期：2026-06-06

## 目标

只读核查来源商品库中 `KATGJ-SS-B885` 的 WMS 尺寸重量为什么没有显示，并确认问题是否出在当前拉取数据链路。

## 核查范围

- 当前激活数据源：`application.yml` 激活 `druid`。
- 目标数据库：以后端激活 `application-druid.yml` 指向的远端 MySQL 为准。
- 执行命令类型：只读 `SELECT`。
- 数据影响：无 DDL、无 DML、无同步触发、无数据修改。

## 核查对象

```text
connection_code = LX-CA012
master_sku = KATGJ-SS-B885
```

截图中的 OMS 页面显示：

- WMS尺寸：`4 * 29 * 38 cm`
- WMS重量：`1.229 kg`
- OMS尺寸：`4 * 29 * 38 cm`
- OMS重量：`1.16 kg`

## 来源快照查询结果

当前 `upstream_system_sku_candidate` 中该 SKU 有两条来源记录：

### LX-CA012

```text
master_sku = KATGJ-SS-B885
master_product_name = 5件套砧板
status = ACTIVE

product_length / product_width / product_height / product_weight
= 4.0000 / 29.0000 / 38.0000 / 1.1600

product_length_bs / product_width_bs / product_height_bs / product_weight_bs
= 1.5750 / 11.4180 / 14.9610 / 2.5580

wms_length / wms_width / wms_height / wms_weight
= null / null / null / null

wms_length_bs / wms_width_bs / wms_height_bs / wms_weight_bs
= null / null / null / null

source_payload_json 中：
length / width / height / weight = 4.0 / 29.0 / 38.0 / 1.16
wmsLength / wmsWidth / wmsHeight / wmsWeight = '' / '' / '' / ''
```

### LX-NY013-3275A1E1

该 SKU 在另一个主仓接入中也存在，结果同样是：

```text
product_length / product_width / product_height / product_weight
= 4.0000 / 29.0000 / 38.0000 / 1.1600

wms_length / wms_width / wms_height / wms_weight
= null / null / null / null

source_payload_json 中：
wmsLength / wmsWidth / wmsHeight / wmsWeight = '' / '' / '' / ''
```

## 请求日志核查结果

核查了最近 5 次 `LX-CA012` 的 `SKU_SYNC` 请求日志，`KATGJ-SS-B885` 都在第 1 页响应中。

请求参数形态：

```json
{
  "data": {
    "page": 1,
    "pageSize": 100
  }
}
```

响应中该 SKU 的关键片段：

```json
{
  "sku": "KATGJ-SS-B885",
  "productName": "5件套砧板",
  "length": 4.000,
  "lengthBs": 1.575,
  "width": 29.000,
  "widthBs": 11.418,
  "height": 38.000,
  "heightBs": 14.961,
  "weight": 1.160,
  "weightBs": 2.558,
  "wmsLength": "",
  "wmsLengthBs": "",
  "wmsWidth": "",
  "wmsWidthBs": "",
  "wmsHeight": "",
  "wmsHeightBs": "",
  "wmsWeight": "",
  "wmsWeightBs": ""
}
```

全量来源快照统计：

```text
upstream_system_sku_candidate 总行数 = 10736
LX-CA012 行数 = 5402
source_payload_json 中 wmsLength/wmsWidth/wmsHeight/wmsWeight 任一非空行数 = 0
落库 wms_length/wms_width/wms_height/wms_weight 任一非空行数 = 0
```

## 结论

当前根因不在前端展示，也不在 Java 解析、Mapper 落库或数据库字段。

当前根因是：现有同步链路只调用领星 OpenAPI：

```text
POST /openapi/v1/product/pagelist
```

该接口在当前请求参数下返回了 `wmsLength/wmsWidth/wmsHeight/wmsWeight` 字段，但实际值为空字符串。代码按空字符串转成 `null` 后落库，所以来源商品库的仓库尺寸为空。

截图中的 OMS 页面确实显示了 WMS 尺寸重量，说明 OMS 后台页面使用的数据口径不等同于当前 `/product/pagelist` 响应里的 `wms*` 字段，至少当前同步请求没有拿到该值。

## 判断

问题属于“拉取数据这一步的数据源不完整或接口口径不匹配”，不是显示层丢字段。

当前 `/product/pagelist` 给出的：

- `length/width/height/weight` 对应截图里的 OMS 尺寸和 OMS 重量。
- `wmsLength/wmsWidth/wmsHeight/wmsWeight` 在响应中为空。
- 截图里的 WMS重量 `1.229 kg` 没有出现在当前同步响应和来源快照中。

## 建议下一步

继续只读排查 OMS 页面实际调用的接口：

1. 使用已登录的 Chrome/OMS 页面查看 Network 请求。
2. 搜索 `KATGJ-SS-B885` 对应的产品列表或详情接口响应。
3. 找到包含 `WMS尺寸 = 4 * 29 * 38 cm` 和 `WMS重量 = 1.229 kg` 的接口字段。
4. 再决定同步链路是：
   - 在 `/product/pagelist` 外追加详情接口回填 WMS 尺寸重量；
   - 还是更换 SKU 同步接口；
   - 或者给 `/product/pagelist` 补充必要查询参数。
