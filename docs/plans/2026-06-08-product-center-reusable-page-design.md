# 商品中心可复用页面设计方案

日期：2026-06-08

## 目标

新增管理端菜单 **商品中心**，先在当前 `react-ui` 管理端完成买家视角商品浏览能力，后续可复用到买家端高频入口。

本方案只设计页面、接口、权限和复用边界，不执行 SQL，不改远端数据，不新增业务表。

## 当前代码事实

- 管理端已有 `商城商品列表`，入口是 `Product/Distribution/index`，后端接口是 `/product/admin/distribution-products`，权限为 `product:distribution:*`。它是后台商品运营页，包含新增、编辑、上下架、调价和操作日志，不适合直接作为买家视角页面。
- 商品编辑页已有 `预览买家视图`，入口是 `BuyerProductPreviewModal.tsx`。该组件的页面结构可以复用，包括商品图、规格选择、价格区域、发货仓库、数量、`商品详情 / 商品参数 / 发货仓库` Tabs。
- 现有买家预览数据仍包含假数据，例如样式预览价、模拟库存、默认发货仓等。这些只能用于预览，商品中心必须全部改为真实接口数据。
- 买家端后端已有 `/buyer/product/distribution-products` 只读接口，权限为 `buyer:product:distribution:list/query`，当前只返回上架商品和上架 SKU 的买家 DTO。
- 当前买家 DTO 合同曾禁止返回 `systemSpuCode` / `systemSkuCode`；但本轮需求明确要求商品中心展示系统 SPU、系统 SKU，因此后续实现需要同步调整 DTO 与合同测试。客户 SPU、客户 SKU、供货价、卖家信息仍必须禁止返回。

## 功能定位

**商品中心** 是买家视角商品浏览和未来下单的入口，不是后台商品运营入口。

| 使用位置 | 当前用途 | 复用口径 |
| --- | --- | --- |
| 管理端 | 先开发、先验证、备用查看买家视角 | 使用后台只读接口和 `sys_menu` 权限 |
| 买家端 | 最终高频使用入口 | 复用同一展示组件，改用 buyer portal token 与 buyer 权限 |
| 卖家端或内测入口 | 可选保留，用于内部验证展示效果 | 仅复用展示组件，不暴露买家不可见字段 |

## 数据范围

商品中心只显示买家可见商品：

- SPU 必须是 `ON_SALE`。
- SPU 管控状态必须可见，例如 `control_status = NORMAL`。
- SKU 必须是 `ON_SALE`。
- SKU 管控状态必须可见。
- 至少存在一个可见 SKU 的 SPU 才能进入列表。
- 不允许前端通过参数扩大范围到草稿、待上架、下架、停用、卖家私有或后台管控商品。

## 字段展示边界

### 允许展示

商品中心允许展示以下买家视角字段：

- 商品中文名称。
- 商品英文名称。
- 系统 SPU。
- 系统 SKU。
- 商品主图、SKU 图、详情图。
- 类目名称。
- 商品卖点。
- 销售价、销售价区间、币种。
- SKU 规格信息，例如颜色、尺寸、材质、型号、包装数量、容量。
- 尺寸重量。
- 库存状态、可用库存、可发仓数量、库存更新时间。
- 发货仓库的买家可见摘要，例如仓库类型、仓库名称、仓库编码、币种、发货说明。
- 商品详情图文和商品参数。

### 禁止展示

以下字段不能出现在接口响应、前端类型、列表、详情或浏览器可见数据中：

- 客户 SPU。
- 客户 SKU。
- 卖家 ID、卖家编号、卖家名称。
- 供货价、成本价、后台调价依据。
- 后台管控原因、管控操作人、恢复操作人。
- 来源系统 payload、hash、上游原始字段、来源 SKU 绑定细节。
- 管理端操作日志、审核快照、内部备注。

系统 SPU / 系统 SKU 本轮需求明确允许展示，但必须在合同测试中单独说明，避免误删客户 SKU 和供货价的防泄漏规则。

## 页面结构

### 列表

列表不采用商城商品运营页的大量平铺字段，而采用少量组合列。

建议第一版列表列：

| 列名 | 内容 |
| --- | --- |
| 商品基础信息 | 主图、商品中文名称、商品英文名称、系统 SPU、前 2-3 个系统 SKU、SKU 数量 |
| 销售与库存 | 销售价区间、币种、库存状态、可用库存、可发仓数量 |
| 类目与卖点 | 类目名称、卖点摘要 |
| 发货信息 | 仓库类型、可发仓摘要、最新库存同步时间 |
| 操作 | 查看详情；未来接入采购单或下单 |

搜索区保持克制：

- 关键词：匹配中文名、英文名、系统 SPU、系统 SKU、类目、卖点。
- 类目：可选。
- 库存状态：可选，第一版如果后端已有稳定枚举再开放。

不提供销售状态筛选，因为商品中心固定只看已上架可见商品。

### 详情

详情复用 `预览买家视图` 的信息架构，但改成真实数据模式：

- 标题从 `买家商品详情预览` 调整为 `商品详情`。
- 移除 `预览模式` 标签。
- 移除 `数据仅用于预览，未发布到买家端。` 提示。
- 价格使用真实 SKU 销售价。
- 库存使用真实库存读模型或真实可见库存摘要。
- 发货仓库使用真实发货仓库数据。
- 不再使用 `样式预览价`、模拟 `现货 N 件`、`默认发货仓`。
- `商品详情 / 商品参数 / 发货仓库` Tabs 保留。
- 未接订单前，不提供可点击的真实下单动作；可以隐藏或禁用购买按钮，避免形成假交易入口。

## 前端复用设计

推荐拆成展示组件和数据适配层。

```text
react-ui/src/components/ProductCenter/
  ProductCenterPage.tsx
  ProductCenterDetailModal.tsx
  productCenterTypes.ts
  productCenterFormatters.ts
  style.module.css

react-ui/src/pages/Product/ProductCenter/index.tsx
  管理端商品中心入口

react-ui/src/services/product/productCenter.ts
  管理端商品中心只读 service

react-ui/src/services/portal/session.ts
  后续 buyer portal 复用商品中心 service adapter
```

核心规则：

- `ProductCenterPage` 只接收 `listProducts`、`getProductDetail`、`getProductSkus` 等函数，不直接 import `request`。
- 组件不关心 admin / buyer / seller token。
- 组件只依赖 `ProductCenterProduct` / `ProductCenterSku` / `ProductCenterWarehouse` 这套买家视角 DTO。
- 管理端页面负责注入后台只读 service。
- 买家端页面负责注入 buyer portal service。
- 如果卖家端内测也复用，必须通过独立 adapter 映射到同一 DTO，不允许把卖家字段传入通用组件。

## 后端接口设计

### 管理端临时承载接口

建议新增 product 模块后台只读 Controller：

```text
GET /product/admin/product-center/list
GET /product/admin/product-center/{spuId}
GET /product/admin/product-center/{spuId}/skus
```

权限建议：

```text
product:center:list
product:center:query
```

接口只返回商品中心 DTO，不返回 `ProductSpu` / `ProductSku` 全量后台模型。

### 买家端复用接口

买家端已有：

```text
GET /buyer/product/distribution-products/list
GET /buyer/product/distribution-products/{spuId}
GET /buyer/product/distribution-products/{spuId}/skus
```

第一版可继续复用现有 buyer 权限：

```text
buyer:product:distribution:list
buyer:product:distribution:query
```

后续如果要把权限语义改名为 `buyer:product:center:*`，应单独做权限迁移方案，不在本轮混入。

## DTO 设计

建议抽象为买家视角 DTO：

```text
ProductCenterProduct
- spuId
- systemSpuCode
- productName
- productNameEn
- categoryId
- categoryName
- sellingPoint
- mainImageUrl
- detailContent
- skuCount
- visibleSystemSkuCodes
- salePriceMin
- salePriceMax
- currencySummary
- availableStock
- warehouseCount
- inventoryStatus
- stockUpdateTime
- warehouseKindSummary
- skus
- warehouses

ProductCenterSku
- skuId
- spuId
- systemSkuCode
- color
- size
- lengthValue
- widthValue
- heightValue
- weight
- material
- style
- model
- packageQuantity
- capacity
- skuImageUrl
- salePrice
- currencyCode
- availableStock
- warehouseCount
- inventoryStatus
- stockUpdateTime
- sortOrder

ProductCenterWarehouse
- warehouseId
- warehouseCode
- warehouseName
- warehouseKind
- warehouseKindLabel
- settlementCurrency
- stockText
- deliveryText
```

`stockText` 和 `deliveryText` 可以由后端根据真实字段生成，也可以前端用真实数值格式化生成；不得用假库存兜底。

## 菜单与权限

管理端新增 `sys_menu` 菜单：

```text
菜单名：商品中心
父级：商品管理
组件：Product/ProductCenter/index
权限：product:center:list
```

建议单独新增菜单 seed，例如：

```text
RuoYi-Vue/sql/20260608_product_center_menu_seed.sql
```

要求：

- 不改写现有 `2402 商城商品列表`。
- 不复用 `product:distribution:*` 权限作为商品中心后台权限。
- seed 必须带确认 token。
- seed 必须带 `sys_menu` slot/signature guard。
- 执行远端 SQL 前必须先确认激活数据源，并生成执行记录。

具体 `menu_id` 不在方案阶段固定，实施前先核对 live `sys_menu` 和现有 seed ID 段，再确定可用 ID。

## 实施步骤

1. 新增商品中心 Markdown 方案并确认。
2. 后端新增商品中心 DTO、管理端只读 Controller、Service facade 或映射方法。
3. 调整 buyer portal 商品 DTO，允许系统 SPU / 系统 SKU，但继续禁止客户 SKU、卖家信息、供货价等敏感字段。
4. 新增前端 `ProductCenter` 可复用组件和管理端 `Product/ProductCenter/index` 页面。
5. 将现有 `BuyerProductPreviewModal` 的假数据依赖拆出，形成真实数据详情模式；预览模式继续服务商品编辑页。
6. 新增管理端 service `productCenter.ts`，buyer portal 后续通过 adapter 复用组件。
7. 新增菜单 seed 和权限 seed。
8. 补合同测试：
   - 后端商品中心接口必须是 admin namespace。
   - 商品中心 DTO 禁止敏感字段。
   - buyer portal DTO 允许系统编码但禁止客户编码和供货价。
   - 前端商品中心组件不得直接调用 `request`。
   - 管理端商品中心 service 只能调用 `/api/product/admin/product-center`。
   - buyer portal adapter 只能调用 `/api/buyer/product/distribution-products`。
9. 运行验证：
   - 后端相关模块测试。
   - `npm run tsc -- --pretty false`。
   - 商品中心相关 Jest/guard。
   - 必要时执行 `npm run verify:three-terminal`。
   - 完成代码更新后运行 `codegraph sync .`。

## 不在第一版范围

- 不新增商品、库存、订单或采购单业务表。
- 不实现真实下单、支付、履约创建。
- 不在商品中心开放商品维护、调价、上下架。
- 不展示客户 SKU、客户 SPU、供货价、卖家资料。
- 不把商品中心做成后台商城商品列表的皮肤版本。
- 不把假库存、假价格、默认发货仓带入商品中心。

## 风险与处理

| 风险 | 处理 |
| --- | --- |
| 系统 SPU/SKU 与旧买家 DTO 合同冲突 | 明确调整合同：系统编码允许展示，客户编码仍禁止 |
| 复用现有后台 `ProductSpu` 导致字段泄漏 | 后端必须新建商品中心 DTO 映射，不返回后台模型 |
| 预览组件含假数据 | 拆分预览模式和真实模式，真实模式无假数据 fallback |
| 管理端和买家端数据源不同 | 组件只接数据源函数，service adapter 分端实现 |
| 未来买家端迁移成本高 | 第一版就把展示组件放到可复用组件目录，页面只做薄封装 |
| 菜单 seed 覆盖历史菜单 | 新 seed 独立持有商品中心菜单，并使用 slot/signature guard |

## 待确认事项

1. 管理端商品中心是否放在 `商品管理` 下，与 `商城商品列表` 并列。
2. 商品中心第一版详情页是否隐藏购买按钮，等采购单/订单接口确认后再开放。
3. 列表中系统 SKU 是否只展示前 2-3 个，详情里展示全部 SKU。
4. 如果卖家端也保留内测入口，是否只作为隐藏菜单或内部角色可见菜单。
