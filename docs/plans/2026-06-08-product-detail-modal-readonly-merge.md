# 商城商品查看弹窗合并实现记录

## 背景

商品列表原“查看”使用侧边 `Drawer`，展示结构与编辑页差异较大。用户要求改成类似编辑页的只读效果，并以弹窗承载，避免查看、编辑两套信息结构割裂。

## 实现

- 保留列表页 `openDetail` 和 `getDistributionProduct` 数据加载逻辑，不新增后端接口。
- 将 `ProductDetailDrawer` 内部由 Ant `Drawer` 改为 Ant `Modal`，文件名暂不改，减少调用侧改动。
- 查看弹窗按编辑页结构分区展示：
  - 顶部只读摘要
  - 基础信息
  - 商品图片
  - 类目属性
  - 详情图文
  - SKU 信息
  - 发货仓库
- 商品图片使用只读图片槽，不出现上传、删除、遮罩或点击放大的编辑态行为。
- SKU 表补充库存状态中文展示，继续复用 `inventoryStatusText` / `inventoryStatusColor`。
- `ProductDetailDrawer.js` 改为 re-export 到 TSX，避免旧 Drawer 实现被解析到。

## 验证

- `npm run tsc` 通过。
- `codegraph sync .` 已执行。
- 浏览器验证 `http://127.0.0.1:8001/product/distribution`：
  - 点击列表第一行“查看”后出现 `.ant-modal`。
  - 页面不存在 `.ant-drawer`。
  - 弹窗包含“基础信息 / 商品图片 / SKU 信息 / 发货仓库”区域。
