# 2026-06-04 页面规划中占位与动态路由检查

## 背景

用户反馈：所有尚未实现的菜单页面都应展示统一的“功能规划中”页面，不能出现 Umi 开发错误页，例如 `Cannot find module './Channel/System/index.tsx'`。

## 新增问题

- 当前运行菜单中存在 7 个页面型菜单指向不存在的前端页面文件：
  - 官方仓库：`Warehouse/Official/index`
  - 第三方仓库：`Warehouse/ThirdParty/index`
  - 系统渠道管理：`Channel/System/index`
  - 客户渠道管理：`Channel/Customer/index`
  - 操作费设置：`Billing/HandlingFee/index`
  - 运费设置：`Billing/Freight/index`
  - 报价方案：`Billing/QuoteScheme/index`
- 这类菜单此前会由 `react-ui/src/services/session.ts` 动态导入为 `@/pages/...`，缺失文件时直接进入 “Something went wrong / Cannot find module” 错误页。

## 已修复问题

- 已在 `react-ui/src/services/session.ts` 的动态菜单路由加载处增加统一兜底：
  - 真实页面存在时继续加载真实页面。
  - 菜单页面文件缺失时，自动降级到 `Common/PlannedPage/index.tsx`。
  - 缺失页面只在开发控制台输出警告，不再让用户看到红色错误页。
- 空组件路径也会落到 `Common/PlannedPage/index.tsx`，避免后续菜单配置不完整时直接报错。

## 残留问题

- 当前菜单表中仍有 7 个旧菜单保留了不存在的组件路径。由于本次未执行数据库 DML，不直接改远端菜单数据。
- 全量 `biome lint` 仍失败，失败项集中在既有文件：
  - `src/components/DictTag/index.tsx`
  - `src/components/RightContent/AvatarDropdown.tsx`
  - `src/components/IconSelector/*`
  - `src/pages/Monitor/Druid/index.tsx`
  - `src/utils/*`
- `npx biome lint src/services/session.ts` 未处理目标文件，原因是当前 Biome 配置忽略该路径。

## 验证命令

- `npm run tsc`
  - 结果：通过。
- `npm run build`
  - 结果：通过，Webpack 编译成功。
- `npm run biome:lint`
  - 结果：失败；失败为既有 lint 问题，不是本次 `session.ts` 改动引入。
- 通过本地后端只读接口检查当前菜单：
  - 目标环境：`http://127.0.0.1:8080`
  - 连接来源：后端当前激活配置，未直接连接数据库或 Redis。
  - 命令类型：HTTP 登录后读取 `/getRouters` 菜单数据。
  - 影响范围：只读菜单接口；未执行数据库或 Redis 写入。
  - 结果：49 个页面型菜单中，23 个有真实页面，19 个显式指向 `Common/PlannedPage`，7 个缺失真实页面。
- 浏览器抽查：
  - `/channel/channel-system`
  - `/warehouse/warehouse-official`
  - `/overseas-warehouse-service/billing-freight`
  - `/finance/fund-account`
  - 结果：均显示“功能规划中”，未出现 `Something went wrong` 或 `Cannot find module`。

## 未验证原因

- 未逐个手工点击全部 49 个页面型菜单；已用后端菜单数据做组件存在性比对，并用浏览器抽查覆盖缺失页面、显式占位页面两类路径。
- 未修复全量 `biome lint` 的既有问题，避免扩大本次修改范围。

## 权限检查结果

- 本次没有新增后端接口、菜单权限、按钮权限或前端权限标识。
- 浏览器验证使用 `admin / admin123` 登录态，只验证页面渲染兜底，不改变权限模型。

## 字典/选项复用检查结果

- 本次未新增业务字段、状态、类型、币种、国家、仓库类型等字典或 option catalog。
- 不涉及 code/label 映射变更。

## 复用台账检查结果

- 已确认 `docs/architecture/reuse-ledger.md` 中已有 `PlannedPage` 复用规则。
- 本次继续复用 `react-ui/src/pages/Common/PlannedPage/index.tsx`，未新增重复占位页面。

## 大文件合理性判断结果

- `react-ui/src/services/session.ts` 当前 198 行，低于 300 行自检阈值。
- 新增逻辑职责集中在动态菜单页面路径转换和缺失页面兜底，暂不需要拆分文件。

## 重复代码检查结果

- 未为 7 个缺失菜单分别创建重复占位页面文件。
- 使用统一动态路由兜底处理当前和后续同类缺失页面。
