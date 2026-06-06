# ProTable 查询动作区全局稳定行修复记录

## 背景

用户反馈「来源仓库库存」在部分分辨率下再次出现查询区按钮悬浮、压到表格上方的问题。此前同类问题也在「第三方仓库」「来源商品库」等页面出现过。

本次目标不是继续逐页调整筛选字段数量，而是把页面级 ProTable 的查询动作区作为全局弹性动作列处理：空间足够时与筛选字段同一行靠右，空间不足时自然换到下一行靠右，避免字段数量、断点列数或页面漏传 `fieldCount` 后再次复发。

## 根因

- Ant Design Pro 的查询动作区 `.ant-pro-query-filter-actions` 本身在一个 `.ant-col` 父级栅格列中。
- 之前的全局工具通过 `fieldCount` 避免筛选字段在某些断点刚好占满整行，但这是字段层面的规避，不是布局兜底。
- 当页面字段数变化、常量漏改，或新页面未传 `fieldCount` 时，动作区父级 `.ant-col` 仍可能被 Ant Design 栅格限制成很窄的一列，视觉上就会悬浮到表格上方。

## 修复内容

- `react-ui/src/global.css`
  - 页面级 ProTable 搜索区规则匹配 `.ant-pro-query-filter-row > .ant-col:has(> .ant-pro-query-filter-actions)`。
  - 动作区父级列设置为 `order: 999`、`flex: 1 0 180px`、`max-width: none`、`margin-inline-start: auto`。
  - 动作区父级列使用 `display: flex`、`align-items: flex-end`、`justify-content: flex-end`，让按钮与输入框底部对齐。
  - 动作区本体不再强制占满整行；少字段页面可以和筛选字段同一行，多字段页面才自然换行。

- `docs/architecture/reuse-ledger.md`
  - 更新 ProTable 复用规则。
  - 明确 `fieldCount` 只用于字段列数优化，不再承担防止动作区悬浮的职责。
  - 明确不允许在页面私有 CSS 中移动 `.ant-pro-query-filter-actions`。

## 反复修正说明

第一次全局修复把动作区强制固定为搜索卡片内部底部整行，虽然解决了“压表格”的问题，但造成字段较少的页面也多出一整行按钮，视觉上仍像悬在筛选区和表格区之间。

最终修正为弹性动作列：

- 5 个筛选字段的页面，例如属性库、币种配置，按钮回到同一行右侧。
- 7 个筛选字段的页面，例如官方仓库、来源仓库库存，按钮同样回到同一行右侧。
- 8 个或更多筛选字段的页面，例如第三方仓库、来源商品库、卖家管理、买家管理，按钮自然换到最后一行右侧。

## 浏览器验证

验证环境：

- 前端：`http://127.0.0.1:8001`
- 后端：`http://127.0.0.1:8080`
- 登录账号：项目本地默认管理端账号

验证结果：

- `/inventory/source-warehouse-stock`
  - `2048 x 1024`：7 字段收起态保持 1 行，动作区同一行靠右，搜索卡片到表格卡片间距 `10px`。
  - `1180 x 1024`：搜索区 2 行，动作区最后一行靠右，搜索卡片到表格卡片间距 `10px`。

- `/warehouse/official`
  - `2048 x 1024`：7 字段收起态保持 1 行，动作区同一行靠右，搜索卡片到表格卡片间距 `10px`。
  - `1180 x 1024`：搜索区 2 行，动作区最后一行靠右，搜索卡片到表格卡片间距 `10px`。

- `/warehouse/third-party`
  - `2048 x 1024`：8 字段展开态搜索区 2 行，动作区最后一行靠右，搜索卡片到表格卡片间距 `10px`。
  - `1180 x 1024`：搜索区 3 行，动作区最后一行靠右，搜索卡片到表格卡片间距 `10px`。

- `/product/list`
  - `2048 x 1024`：来源商品库搜索区 2 行，动作区最后一行靠右，搜索卡片到表格卡片间距 `10px`。
  - `1180 x 1024`：搜索区 3 行，动作区最后一行靠右，搜索卡片到表格卡片间距 `10px`。

- `/basic-config/product-attribute`
  - `2048 x 1024`：属性库 5 字段收起态保持 1 行，动作区同一行靠右，搜索卡片到表格卡片间距 `10px`。
  - `1180 x 1024`：搜索区 2 行，动作区最后一行靠右，搜索卡片到表格卡片间距 `10px`。

- `/product/distribution`
  - `2048 x 1024`：商城商品列表 5 字段收起态保持 1 行，动作区同一行靠右，搜索卡片到表格卡片间距 `10px`。
  - `1180 x 1024`：搜索区 2 行，动作区最后一行靠右，搜索卡片到表格卡片间距 `10px`。

- `/partner/seller`、`/partner/buyer`
  - `2048 x 1024`：搜索区 4 行，动作区最后一行靠右，搜索卡片到表格卡片间距 `10px`。
  - `1180 x 1024`：搜索区 6 行，动作区最后一行靠右，搜索卡片到表格卡片间距 `10px`。

- `/finance/currency`
  - `2048 x 1024`：币种配置 5 字段收起态保持 1 行，动作区同一行靠右，搜索卡片到表格卡片间距 `10px`。

补充说明：

- 本次浏览器验证中，`/product/source-product-library` 不是当前菜单实际路径；实际「来源商品库」路径为 `/product/list`。
- 为了完成本地浏览器验证，后端通过 `.\start-backend-local.ps1 -Restart` 重启；未执行数据库 DDL/DML。
- 前端开发服务因 Umi MFSU 缓存出现重复输出路径错误，已用 `DISABLE_MFSU=1` 重新启动在 `8001` 端口用于验证。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npx biome lint src/global.css
npm run tsc -- --pretty false

cd E:\Urili-Ruoyi
git diff --check -- react-ui/src/global.css docs/architecture/reuse-ledger.md
```

结果：

- `npx biome lint src/global.css`：通过。
- `npm run tsc -- --pretty false`：通过。
- `git diff --check`：通过；仅提示 Git 后续可能将 LF 转 CRLF。

## CodeGraph

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

结果：

- 通过。
- 同步 7 个变更文件。
- Modified: 6，Removed: 1。

## 复用结论

后续新增页面级 ProTable 时：

- 继续使用 `getPersistedProTableSearch(...)`。
- 不再为了查询按钮错位去逐页调整 `fieldCount` 或写页面私有 CSS。
- 页面级查询按钮位置只由 `react-ui/src/global.css` 的 ProTable 搜索区全局规则维护。
