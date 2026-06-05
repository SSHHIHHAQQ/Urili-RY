# 2026-06-05 卖家端商品列表前端查询范围守卫补强记录

## 参考方向

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，并遵守当前节奏：

- 先做一套标准卖家模板，验收通过后再复制买家。
- 每个切片只改一类东西，减少返工。
- 当前切片只加固前端 portal 静态守卫，不复制 buyer，不改后端，不执行数据库 DDL/DML。

## 本轮范围

本轮只处理 `react-ui/scripts/check-portal-token-isolation.mjs`：

- 将 `getSellerPortalDistributionProducts` 纳入必须调用 `sanitizePortalQueryParams(params)` 的查询函数清单。
- 固定 `portal/session.ts` 的 `PORTAL_SCOPE_PARAM_KEYS` 必须包含 `terminal`。
- 保持已有守卫：portal 页面不得直接调用 `request(...)`，不得硬编码 `/api/seller` / `/api/buyer`，portal 请求必须显式 `isToken:false`。

本轮不做：

- 不复制 buyer。
- 不改 seller 商品 UI 或后端接口。
- 不执行远程数据库 DDL/DML。
- 不启动三端前端物理拆分。

## 已完成

- 更新 `react-ui/scripts/check-portal-token-isolation.mjs`：
  - `portalQueryFunctions` 新增 `getSellerPortalDistributionProducts`。
  - 新增 `PORTAL_SCOPE_PARAM_KEYS` 包含 `terminal` 的静态检查。
- 调整实现方式：
  - 不把 `terminal:` 加入全局禁止对象键正则，避免误伤 TypeScript 函数参数和端配置对象。
  - 通过检查 sanitizer 清单来保证 `terminal` 作为 query 参数会被过滤。

## 验证结果

- 首次把 `terminal:` 加入全局禁止对象键正则后，`npm run guard:portal-token` 抓到了 `Portal/terminal.ts` 和 `services/portal/session.ts` 的合法端配置/函数参数，说明该正则过粗。
- 已改为检查 `PORTAL_SCOPE_PARAM_KEYS` 必须包含 `terminal`，并保留原有禁止 `sellerId:`、`buyerId:`、`subjectId:`、`accountId:`、`sellerAccountId:`、`buyerAccountId:` 对象键。
- `cd E:\Urili-Ruoyi\react-ui; npx biome check --write scripts/check-portal-token-isolation.mjs`：通过并格式化脚本。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过，`Portal token isolation guard passed.`。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npx biome check scripts/check-portal-token-isolation.mjs`：通过。
- `git diff --check -- <本切片相关文件>`：通过，仅有 LF/CRLF 工作区换行提示。
- 相关文件尾随空白和冲突标记检查：通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 当前判断

seller portal 商品列表前端 service 现在被静态守卫纳入查询参数清洗契约。后续如果有人把 `getSellerPortalDistributionProducts` 改成直接透传 `params`，或从 `PORTAL_SCOPE_PARAM_KEYS` 移除 `terminal`，`npm run guard:portal-token` 会失败。真实数据范围仍以后端 `PortalLoginSession.subjectId` 为准。
