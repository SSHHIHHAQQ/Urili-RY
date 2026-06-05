# 2026-06-05 管理端卖家标准列表模板执行记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理管理端卖家管理列表模板。

## 本轮范围

- 只改 `react-ui` 管理端前端。
- 只启用卖家管理标准列表模板。
- 不复制买家。
- 不修改后端接口、菜单、权限 seed、DDL/DML 或远程数据。

## 已完成

- `PartnerModuleConfig` 新增 `listTemplate` 和 `searchStorageKey` 配置。
- `PartnerManagementPage` 在 `listTemplate: 'standard'` 时使用紧凑列表：
  - 卖家编号和卖家代码合并为一列，上下展示。
  - 卖家名称和卖家简称合并为一列，上下展示。
  - 登录账号和卖家等级合并为一列，上下展示。
  - 创建时间和最后登录时间继续在“时间”列上下展示，空值显示 `-`。
  - 状态、余额、联系人、操作列维持固定宽度，操作仍为“编辑 / 账号 / 更多”。
- `Seller/index.tsx` 启用 `listTemplate: 'standard'`，并设置稳定筛选折叠状态 key：`admin-seller-management`。
- 买家入口未启用该模板，等待卖家验收通过后再按配置复制。

## 验证

```powershell
cd E:\Urili-Ruoyi\react-ui
npx biome lint src/components/PartnerManagement/PartnerManagementPage.tsx src/pages/Seller/index.tsx
npm run tsc

cd E:\Urili-Ruoyi
git diff --check -- react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx react-ui/src/pages/Seller/index.tsx
```

- `biome lint`：通过。
- `tsc --noEmit`：通过。
- `git diff --check`：通过，仅提示 LF/CRLF 工作区换行警告。
- Playwright / Chrome 验收 `/partner/seller`：通过。
  - 1366x768：`bodyOverflowX=false`，`tableOverflowX=false`。
  - 1366x900：搜索区默认展开且与表格不重叠。
  - 表头为 8 列：卖家编号/代码、卖家名称、登录账号/等级、分销账户余额、联系人、状态、时间、操作。
  - `GET /api/seller/admin/sellers/list`：HTTP 200，业务 `code=200`，返回 3 行。
  - console error 数量为 0，page error 数量为 0。
  - 点击筛选区“收起”后，浏览器写入 `proTableSearch:collapsed:admin-seller-management=true`。
  - 截图：`output/playwright/admin-seller-standard-template-expanded.png`。

## 未验证

- 未做新增、编辑、状态切换、账号、菜单、审计等操作流验收；本轮只验收列表模板。
- 本轮不涉及接口契约变化。
- 未执行 SQL；本轮不涉及数据库变化。

## 权限检查

- 管理端权限前缀仍由 `moduleKey='seller'` 推导为 `seller:admin:*`。
- 本轮未新增按钮权限、菜单权限或端内权限。

## 字典与选项复用

- 卖家等级继续读取 `seller_level`。
- 主体类型继续读取 `subject_type`。
- 国家/地区继续读取 `country_region`。
- 下拉选择继续复用 `SEARCHABLE_SELECT_PROPS`。

## 复用台账

- 已同步更新 `docs/architecture/reuse-ledger.md` 中 `PartnerManagementPage` 规则。

## 大文件判断

- `PartnerManagementPage.tsx` 是既有大文件，本轮只加配置化列表模板，不拆文件。
- 后续如果继续扩账号、部门、角色、审计或列表配置，应优先拆子组件，避免继续加重该文件。

## 重复代码检查

- 没有复制买家代码。
- 没有新增独立卖家页面实现；继续复用 `PartnerManagementPage`。
- 买家复制时只启用配置和替换文案/service，不重新设计列表逻辑。
