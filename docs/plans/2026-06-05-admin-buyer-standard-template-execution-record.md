# 2026-06-05 管理端买家标准列表模板执行记录

## 目标

在卖家标准列表模板浏览器验收通过后，按同一模板复制到买家管理，只替换端类型、文案、字段配置、权限标识和 service，不重新设计。

## 本轮范围

- 只改 `react-ui` 管理端前端。
- 只启用买家管理标准列表模板。
- 保留买家充值占位列，不接入充值功能、不新增弹窗。
- 不修改后端接口、菜单、权限 seed、DDL/DML 或远程数据。

## 已完成

- `Buyer/index.tsx` 启用 `listTemplate: 'standard'`。
- `Buyer/index.tsx` 设置稳定筛选折叠状态 key：`admin-buyer-management`。
- `PartnerManagementPage` 标准模板下的操作列使用不换行 `Space`，避免“更多”在买家多一列时竖排。
- `proTableSearch.ts` 将 `xl` 断点调整为 6 列展示，符合宽屏优先 6 个筛选字段的规则，避免默认展开时日期筛选行被表格压住。

## 验证

```powershell
cd E:\Urili-Ruoyi\react-ui
npx biome lint src/utils/proTableSearch.ts src/components/PartnerManagement/PartnerManagementPage.tsx src/pages/Seller/index.tsx src/pages/Buyer/index.tsx
npm run tsc
```

- `biome lint`：通过。
- `tsc --noEmit`：通过。
- Playwright / Chrome 验收 `/partner/buyer`：通过。
  - 1366x900：`bodyOverflowX=false`，`tableOverflowX=false`。
  - 搜索区默认展开，包含买家编号/代码、买家代码、买家名称、买家简称、登录账号/等级、公司名称、手机号、买家等级、账户余额、状态、创建时间、最后登录时间。
  - 搜索区与表格不重叠：`searchBottom=378`，`tableTop=386`。
  - 表头为 9 列：买家编号/代码、买家名称、登录账号/等级、账户余额、充值、联系人、状态、时间、操作。
  - `GET /api/buyer/admin/buyers/list`：HTTP 200，业务 `code=200`，返回 1 行。
  - console error 数量为 0，page error 数量为 0。
  - 截图：`output/playwright/admin-buyer-standard-template-expanded.png`。
- 卖家回归验收 `/partner/seller`：通过。
  - 1366x900：`bodyOverflowX=false`，`tableOverflowX=false`。
  - 搜索区默认展开且与表格不重叠。
  - `GET /api/seller/admin/sellers/list`：HTTP 200，业务 `code=200`，返回 3 行。
  - console error 数量为 0，page error 数量为 0。
  - 截图：`output/playwright/admin-seller-standard-template-expanded.png`。

## 未验证

- 未做新增、编辑、状态切换、账号、菜单、审计等操作流验收；本轮只验收列表模板复制。
- 本轮不涉及接口契约变化。
- 未执行 SQL；本轮不涉及数据库变化。

## 权限检查

- 管理端权限前缀仍由 `moduleKey='buyer'` 推导为 `buyer:admin:*`。
- 本轮未新增按钮权限、菜单权限或端内权限。

## 字典与选项复用

- 买家等级继续读取 `buyer_level`。
- 主体类型继续读取 `subject_type`。
- 国家/地区继续读取 `country_region`。
- 下拉选择继续复用 `SEARCHABLE_SELECT_PROPS`。

## 复用台账

- 已同步更新 `docs/architecture/reuse-ledger.md` 中 `PartnerManagementPage` 和 ProTable 筛选区规则。

## 大文件判断

- `PartnerManagementPage.tsx` 是既有大文件，本轮只补标准模板排版约束，不拆文件。
- 后续如果继续扩账号、部门、角色、审计或列表配置，应优先拆子组件。

## 重复代码检查

- 没有复制整份买家页面。
- 买家继续复用 `PartnerManagementPage`，只启用配置并沿用 buyer service。
