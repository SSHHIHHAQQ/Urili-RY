# 真实商品样板数据执行记录

## 目标

为商城商品列表补充一批更接近真实业务的样板商品，用于检查列表、SKU 展开、商品图片、买家视图预览和详情图文展示效果。

## 数据源与边界

- 当前工程：`E:\Urili-Ruoyi`
- 后端运行库：来自 `.env.local` 的远端 MySQL，库名 `fenxiao`
- 文件存储：当前后端配置为 COS；本次样板图不走上传接口，放在管理端前端静态目录 `react-ui/public/demo-products/20260608/`
- 商品图片 URL：使用 `http://127.0.0.1:8001/demo-products/20260608/...`，避免被前端 `resolveResourceUrl` 误转为 `/api/profile`
- 样板商品前缀：`SPUREAL20260608%`、`SKUREAL20260608%`
- 卖家：`seller_id=5`，`seller_name=跨新科技`
- 发货仓：`warehouse_id=4`，三方仓，币种 `USD`
- 来源：管理端手工创建，不绑定官方来源 SKU，不写 `product_sku_source_binding`

## 商品与 SKU 设计

| 系统 SPU | 类目 | 商品中文标题 | 商品英文标题 | SKU 设计 |
| --- | --- | --- | --- | --- |
| SPUREAL202606080001 | 衬衫 | 男士免烫牛津长袖衬衫 | Non-Iron Oxford Long Sleeve Shirt | 3 色 x 2 尺码，共 6 个 SKU |
| SPUREAL202606080002 | 牛仔裤 | 高腰微弹直筒女士牛仔裤 | High Rise Stretch Straight Jeans | 3 色 x 2 尺码，共 6 个 SKU |
| SPUREAL202606080003 | 冲锋衣 | 三合一防风防水户外冲锋衣 | 3-in-1 Waterproof Outdoor Jacket | 3 色 x 2 尺码，共 6 个 SKU |
| SPUREAL202606080004 | 跑步鞋 | 城市缓震透气跑步鞋 | Urban Cushion Breathable Running Shoes | 2 色 + 5 个鞋码，共 5 个 SKU |
| SPUREAL202606080005 | 通勤双肩包 | 防泼水多隔层通勤双肩包 | Water-Resistant Multi-Pocket Commuter Backpack | 4 色 / 2 容量，共 4 个 SKU |
| SPUREAL202606080006 | 床品套件 | 全棉磨毛床单四件套 | Brushed Cotton Sheet Set 4 Pieces | 4 色 / 2 床型，共 4 个 SKU |
| SPUREAL202606080007 | 浴巾 | A 类加厚吸水纯棉浴巾 | Thick Absorbent Cotton Bath Towel | 4 色，共 4 个 SKU |

## 图片设计

每个 SPU 生成一张 4x2 电商摄影分镜图，并裁切为 8 张 800x800 JPG：

1. 主图：清晰展示商品主体。
2. 尺寸图：商品平铺或正侧视，带无文字尺寸示意线。
3. 材质图：面料、纹理或材料微距。
4. 细节图：拉链、纽扣、口袋、缝线、收边等结构。
5. 场景图：真实使用场景，不出现品牌。
6. 颜色图：展示颜色或规格变体。
7. 包装图：展示入仓/发货可理解的包装状态。
8. 陈列补充图：商品与吊牌、面料样、配件等组合。

## 详情图文设计

每个 SPU 的 `detail_content` 都使用四类模块：

- 文本段落：解释商品定位、适用场景和采购价值。
- 图片模块：展示主场景或核心卖点图。
- 图文模块：配一张细节图，说明材料、结构或使用体验。
- 参数表模块：列出材质、规格、包装数量、建议人群/场景、单位说明等。

## 回滚方式

如需回滚本批样板数据：

1. 删除 `system_spu_code like 'SPUREAL20260608%'` 的 SPU 相关 `product_image`、`product_spu_warehouse`、`product_sku`、`product_attribute_value`、`product_spu` 数据。
2. 删除 `react-ui/public/demo-products/20260608/` 下的静态样板图。

## 执行结果

已执行。

- 静态图片：生成并裁切 7 个商品目录，共 56 张独立商品图，最终保留为 JPG，目录体积约 4.7MB。
- 写入 SPU：7 条。
- 写入 SKU：35 条。
- 写入商品图片：91 条，其中 SPU 主图/轮播图 56 条，SKU 主图 35 条。
- 写入 SPU 仓库绑定：7 条。
- 详情图文：7 个 SPU 均包含文本段落、图片模块、图文模块和参数表模块。

## 验证结果

数据库验证：

```text
SPU: 7
SKU: 35
商品图片: 91
SPU 仓库绑定: 7
包含四类详情模块的 SPU: 7
```

SKU 价格与币种抽样：

```text
SPUREAL202606080001 | 男士免烫牛津长袖衬衫 | sku=6 | sale=18.9000-21.9000 | currency=USD/USD
SPUREAL202606080002 | 高腰微弹直筒女士牛仔裤 | sku=6 | sale=39.9000-43.5000 | currency=USD/USD
SPUREAL202606080003 | 三合一防风防水户外冲锋衣 | sku=6 | sale=79.9000-88.9000 | currency=USD/USD
SPUREAL202606080004 | 城市缓震透气跑步鞋 | sku=5 | sale=54.9000-59.9000 | currency=USD/USD
SPUREAL202606080005 | 防泼水多隔层通勤双肩包 | sku=4 | sale=44.9000-54.9000 | currency=USD/USD
SPUREAL202606080006 | 全棉磨毛床单四件套 | sku=4 | sale=69.9000-79.9000 | currency=USD/USD
SPUREAL202606080007 | A 类加厚吸水纯棉浴巾 | sku=4 | sale=14.9000-15.9000 | currency=USD/USD
```

浏览器验证：

- `http://127.0.0.1:8001/product/distribution` 待上架 Tab 可看到本批 7 个 SPU。
- 列表主图均能加载，图片自然尺寸为 `800x800`。
- 展开 SKU 行后，SKU 图可加载，SKU 规格显示为 `颜色：象牙白` 这类带规格属性名的文本。
- SKU 尺寸重量显示为 `36.00 x 28.00 x 8.00 cm  520 g` 这类格式。
- 编辑页打开 `SPUREAL202606080005` 后，买家预览弹窗可正常显示。
- 买家预览内商品图、详情文本、图文模块和参数表均能渲染，预览按钮为 `填写下单信息 / 加入采购单 / 提交订单`。
