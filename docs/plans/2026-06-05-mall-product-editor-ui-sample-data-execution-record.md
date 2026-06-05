# 商城商品编辑页与样例数据执行记录

日期：2026-06-05

状态：已执行到当前远端 `fenxiao` 运行库，并完成后端、前端和浏览器路径验证。

## 1. 执行目标

- 将新增商品从抽屉改为独立页面 `/product/distribution/create`。
- 新增页不展示系统 SPU 输入项；编辑页只读展示系统 SPU。
- SPU 标题拆分为商品中文标题 `product_name` 和商品英文标题 `product_name_en`，英文标题必填。
- SPU 增加详情文字 `detail_content`。
- 详情图片继续复用 `product_image`，使用 `image_role='DETAIL'`。
- 写入真实感演示商品，方便页面视觉验收。
- 商品分类 TreeSelect 搜索改为只按节点标题匹配。

## 2. 目标环境

- 目标 MySQL：远端腾讯云 MySQL，主机 `gz-cynosdbmysql-grp-lucf5kyf.sql.tencentcdb.com`，端口 `28634`，数据库 `fenxiao`。
- 目标 Redis：远端 Redis，主机 `114.132.156.75`，端口 `6379`，数据库索引 `0`。
- 连接来源：后端激活配置读取 `.env.local` 中的 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD`；执行记录不保存密码或密钥。
- 本次未读取或写入本地 Docker MySQL / Redis。

## 3. 执行内容

SQL 文件：`RuoYi-Vue/sql/20260605_mall_product_editor_ui_sample_data.sql`

执行类型：

- DDL：给 `product_spu` 新增 `product_name_en`、`detail_content`。
- DML：回填存量 `product_name_en`。
- DML：插入 4 个演示 SPU、7 个演示 SKU。
- DML：插入演示商品主图、轮播图、详情图、SKU 图。
- DML：给 T 恤演示商品插入必要类目属性值。

执行结果：

```text
OK statement 1/27
...
OK statement 27/27
DONE statements=27
```

## 4. 执行后验证

数据库验证：

```text
NEW_COLUMNS=2
DEMO_SPU=4
DEMO_SKU=7
IMAGE_ROLE=DETAIL count=4
IMAGE_ROLE=GALLERY count=4
IMAGE_ROLE=MAIN count=4
IMAGE_ROLE=SKU_MAIN count=7
```

接口验证：

```text
LOGIN_CODE=200
LIST_CODE=200
LIST_TOTAL=4
DETAIL_CODE=200
```

演示商品：

- `SPUDEMO202606050001`：轻量透气棒球帽 / Lightweight Breathable Baseball Cap
- `SPUDEMO202606050002`：城市缓震跑步鞋 / Urban Cushion Running Shoes
- `SPUDEMO202606050003`：防泼水通勤双肩包 / Water-Resistant Commuter Backpack
- `SPUDEMO202606050004`：纯棉宽松短袖 T 恤 / Cotton Relaxed Fit Short Sleeve T-Shirt

## 5. 浏览器验证

使用 Playwright CLI 从根路径登录后验证：

- 菜单进入 `商品管理 / 商城商品列表` 成功。
- 列表展示 4 条演示 SPU，中文标题和英文标题同时展示。
- 点击“新增商品”后进入 `/product/distribution/create` 独立页面，不再打开抽屉。
- 新增页没有系统 SPU 输入项。
- 新增页展示商品中文标题、商品英文标题、客户 SPU、卖家、分类、图片槽位、详情图文和 SKU 矩阵。
- 商品分类搜索“帽”只展示命中帽类节点及必要父级路径。
- 点击列表第一行“编辑”进入 `/product/distribution/edit/4`。
- 编辑页顶部只读展示系统 SPU、来源和 SKU 数。
- 编辑页可见 SPU 主图、轮播图、详情图、SKU 图和详情文字。
- 新浏览器会话进入新增页和编辑页后，控制台 `Errors: 0, Warnings: 0`。

截图：

- `output/playwright/mall-product-create-page.png`

## 6. 构建与服务验证

已执行：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-admin -am -DskipTests package

cd E:\Urili-Ruoyi
.\start-backend-local.ps1 -Restart

cd E:\Urili-Ruoyi\react-ui
npm run tsc
npm run build
```

结果：

- 后端 `ruoyi-admin` 打包成功。
- 后端 8080 启动成功，`/captchaImage` 返回 `code=200`。
- 前端 TypeScript 检查通过。
- 前端生产构建通过，构建产物包含 `product\distribution\create\index.html`。

## 7. 已修复问题

- 修复商品分类 TreeSelect 搜索误匹配整棵子树文本的问题，改为节点标题匹配。
- 修复商品详情抽屉 Antd 6 `Drawer.width` 废弃提示。
- 修复图片预览 Antd 6 `visible/onVisibleChange` 废弃提示。
- 修复动态类目属性 `key` 被 spread 到 `Form.Item` 的 React 警告。
- 修复 SKU 销售价单元格 Antd 6 `Space.direction` 废弃提示。

## 8. 残留问题

- 当前 Umi dev server 直接访问任意业务深链都会 404，包括既有 `/user/login`、`/account/center`，不只是 `/product/distribution/create`；应用内菜单和按钮跳转正常，生产构建已生成新增页静态入口。
- 演示商品使用外部图片 URL，只用于视觉验收；后续正式商品仍应走本系统上传和审核流程。
- 库存仍只展示读取边界，等待库存模块按 `sku_id + warehouse_id` 提供读取接口。

## 9. CodeGraph

已在仓库根目录执行：

```powershell
codegraph sync .
```

结果：

```text
Synced 3 changed files
Added: 1, Modified: 2 - 125 nodes in 516ms
```
