# 2026-06-05 管理端卖家/买家共享模板守卫执行记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家先形成标准样板，买家只替换配置和 service；每个切片只改一类东西”的节奏，本轮只固定管理端卖家/买家共享模板契约。

## 本轮范围

- 只改 `react-ui` 前端守卫脚本、`package.json` 和文档记录。
- 不改后端接口。
- 不改 Seller / Buyer 页面实现。
- 不执行远程 MySQL / Redis DDL 或 DML。
- 不启动三端前端物理拆分。

## 已完成

- 新增 `react-ui/scripts/check-partner-management-template.mjs`。
- 新增 `react-ui` 脚本 `guard:partner-management`。
- `npm run lint` 已接入 `guard:partner-management`，在 `guard:portal-token` 后执行。
- 守卫固定 `Seller/index.tsx` / `Buyer/index.tsx` 必须通过共享 `PartnerManagementPage` 配置接入。
- 守卫固定 seller / buyer 页面只能导入各自 service，不允许串端或直接调用 `request(...)`。
- 守卫固定 seller / buyer 配置必须包含标准列表模板、稳定搜索状态 key、账号域权限、账号/部门/角色/菜单/会话/日志/免密等 service 映射。
- 守卫固定 `react-ui/src/services/seller/seller.ts` / `react-ui/src/services/buyer/buyer.ts` 只能调用各自 `/api/seller/admin/**` / `/api/buyer/admin/**` 路径，不允许回退 `/api/system/**` 或串端调用。
- 守卫固定 `PartnerManagementPage`、`PartnerAccountModal`、`PartnerAuditModal` 保留共享模板能力，公共组件内不硬编码 seller/buyer/system API 路径。
- 更新 `docs/architecture/reuse-ledger.md`，登记管理端共享模板守卫规则。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run guard:partner-management
npx biome lint scripts/check-partner-management-template.mjs package.json
node --check scripts/check-partner-management-template.mjs
npm run guard:portal-token
npm run guard:seller-portal-product
npm run guard:buyer-portal-product
npm run tsc -- --pretty false
```

验证结果：

- `npm run guard:partner-management`：通过。
- `npx biome lint scripts/check-partner-management-template.mjs package.json`：通过。
- `node --check scripts/check-partner-management-template.mjs`：通过。
- `npm run guard:portal-token`：通过。
- `npm run guard:seller-portal-product`：通过。
- `npm run guard:buyer-portal-product`：通过。
- `npm run tsc -- --pretty false`：通过。

## 未验证原因

- 本轮没有跑完整 `npm run lint`；当前仓库全量 `biome:lint` 仍有历史无关文件问题，上一切片已记录，不在本轮扩大处理。
- 本轮没有做浏览器验收；该切片是静态模板契约守卫，不改变可见 UI。
- 本轮没有执行 SQL；不涉及数据库变化。

## 权限检查结果

- 守卫固定 seller 使用 `seller:admin:account:*` 账号域权限。
- 守卫固定 buyer 使用 `buyer:admin:account:*` 账号域权限。
- 守卫固定 seller/buyer 管理端 service 不调用 `/api/system/**` 管理卖家/买家端账号、菜单、角色或部门。

## 字典/选项复用检查结果

- 本轮未新增字典或选项。
- 守卫不改变 `subject_type`、`country_region`、`seller_level`、`buyer_level` 和端账号角色字典的现有复用方式。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md` 的 `PartnerManagementPage` 条目。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 大文件合理性判断结果

- 新增守卫脚本超过 300 行，职责单一：只检查管理端 seller/buyer 共享模板、service 隔离和公共组件不得硬编码 API。
- 本轮没有继续向 `PartnerManagementPage.tsx`、`PartnerAccountModal.tsx` 等既有大文件追加业务逻辑。
- 选择独立脚本而不是把规则写入页面，是为了减少公共组件继续膨胀。

## 重复代码检查结果

- 没有复制 Seller / Buyer 页面逻辑。
- 没有新增第二套管理端页面组件。
- 守卫要求后续仍通过 `PartnerModuleConfig` 替换端类型、文案、字段、权限和 service。

## 残留问题

- 子 agent 盘点指出卖家账号生命周期自动化守卫仍不足，建议下一片只补 seller 账号生命周期 service 测试，不与本轮静态模板守卫混合。
- `lock_status` / `lock_reason` / 解锁账号仍属于后续独立设计和实现范围，本轮未处理。
