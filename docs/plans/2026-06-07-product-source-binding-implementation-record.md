# 商城商品官方来源 SKU 绑定实现记录

## 目标

本轮实现商品列表侧的官方来源 SKU 配对关系。第一版确认规则是：一个官方来源 SKU 组全局只能绑定一个商城 SKU。商品侧以 `product_sku_source_binding` 为主事实；来源商品库和来源库存继续通过 `upstream_system_sku_pairing` 投影显示配对摘要。

## 已落地

- 新增商品侧 SQL：`RuoYi-Vue/sql/20260607_product_sku_source_binding.sql`。
  - 新增 `product_sku_source_binding`。
  - 使用 `active_sku_key` 限制一个商城 SKU 同时只有一个 ACTIVE 绑定。
  - 使用 `active_source_key` 限制一个官方来源 SKU 组全局同时只能绑定一个商城 SKU。
  - 显式依赖 `upstream_system_warehouse_pairing.pairing_role`，避免报价仓和履约仓混用。
- 后端商品保存链路接入官方仓来源绑定。
  - 官方仓商品不再要求前端传发货仓库 ID。
  - 官方仓 SKU 必须传 `sourceDimensionGroupKey`。
  - 后端从来源尺寸组读取尺寸重量，从来源仓明细 + 履约仓配对反推系统官方仓。
  - 商品离开草稿或 SKU 离开草稿时锁定来源绑定。
  - 草稿内换绑会把旧绑定标记为 `REPLACED`；未锁定绑定删除/切换非官方仓时标记 `RELEASED`。
  - 同步维护 `upstream_system_sku_pairing` 投影，并触发来源商品/来源库存读模型按连接重建。
- 前端商品编辑页接入官方仓来源 SKU 选择。
  - 仓库类型为官方仓时，发货仓库显示为自动派生，不再展示仓库多选。
  - 增加来源 SKU 选择弹窗，默认筛选官方来源、ACTIVE、未配对。
  - 官方仓 SKU 行显示来源 SKU、来源商品、来源仓，尺寸重量只读。
- 商品列表 SKU 视图、SPU 展开明细和详情抽屉增加来源 SKU 展示。
- `SqlExecutionGuardContractTest` 已加入商品侧来源绑定 SQL 和上游 pairing role SQL 的显式确认 token 检查。
- 复用台账已追加“商城 SKU 官方来源绑定模板”。

## 验证结果

- 后端编译：`mvn -pl product -am -DskipTests compile`，通过。
- SQL guard：`mvn -pl ruoyi-system -Dtest=SqlExecutionGuardContractTest test`，通过，34 个测试通过。
- 前端类型检查：`npm run tsc`，通过。
- CodeGraph：`codegraph sync .`，完成，结果为 already up to date。
- 前端页面冒烟：使用本地 `http://127.0.0.1:8001/product/distribution`，默认账号登录后商城商品列表可打开；SPU/SKU 视图切换可用，SKU 视图出现“来源SKU”列，未发现前端运行错误。

## 数据库执行状态

截至本记录创建时，仅完成代码和 SQL 文件落地，尚未在当前运行库执行 DDL/DML。实际执行顺序必须是：

1. `20260607_upstream_pairing_role_binding.sql`
2. `20260607_product_sku_source_binding.sql`

两者都必须先设置各自确认 token。
