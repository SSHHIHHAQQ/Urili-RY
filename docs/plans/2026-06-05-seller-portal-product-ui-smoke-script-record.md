# 2026-06-05 卖家端商品前端浏览器烟测脚本记录

## 参考方向

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，并遵守当前节奏：

- 先做一套标准卖家模板，验收通过后再复制买家。
- 每个切片只改一类东西，减少返工。
- 当前切片只补 seller portal 商品前端可复跑浏览器验收脚本，不复制 buyer，不改后端接口，不执行数据库 DDL/DML。

## 本轮范围

本轮新增：

- `scripts/smoke/seller-portal-product-ui-smoke.ps1`
- `scripts/smoke/seller-portal-product-ui-smoke.mjs`

本轮不做：

- 不复制 buyer 商品页面或 buyer 商品 service。
- 不修改 seller 商品 UI、后端接口、权限 seed 或数据库数据。
- 不启动三端前端物理拆分。

## 脚本行为

`seller-portal-product-ui-smoke.ps1` 是 PowerShell 启动器：

- 默认使用 `http://127.0.0.1:8080` 和 `http://127.0.0.1:8001`。
- 默认使用管理端 `admin / admin123` 创建 seller 免密票据。
- 默认使用 `SellerId=5`。
- 优先复用全局 `@playwright/cli` 里的 `playwright` 包。
- 如果本机没有可用 Playwright runtime，则在临时目录 `%TEMP%\urili-playwright-smoke-runtime` 准备 runtime，且设置 `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1`，继续使用本机 Chrome/Edge 通道，不往项目里加 npm 依赖。

`seller-portal-product-ui-smoke.mjs` 执行真实浏览器验收：

- 读取 `/captchaImage`，如果验证码开启则直接失败，不修改验证码配置。
- 管理端登录后调用 `POST /seller/admin/sellers/{sellerId}/directLogin` 创建一次性免密票据。
- 使用 Playwright 打开 `/seller/direct-login` 并进入 `/seller/portal`。
- 验证 fresh browser context 下只写入 `seller_access_token`，不写入 `access_token` 或 `buyer_access_token`。
- 验证页面出现“卖家端”“商品发布准备”“我的商城商品”“客户SPU”。
- 点击商品“详情”，验证弹窗出现“商品详情”“客户SKU”“SKU规格”“商品状态”。
- 检查页面可见文本不包含 `sellerId`、`systemSpuCode`、`systemSkuCode`、`tokenId`、`Authorization`。
- 检查浏览器控制台没有 `error` / `warning` / `warn`。
- 关闭详情弹窗并点击“退出”，验证回到 `/user/login`。

脚本不会输出 `admin token`、`seller token`、`directLoginToken`、免密 URL、Redis key 或 `.env.local` 内容；异常输出会对常见 token/URL 片段做脱敏。

## 验证结果

- 后端连通性：`GET http://127.0.0.1:8080/captchaImage` 返回 `code=200`，`captchaEnabled=False`。
- 前端连通性：`GET http://127.0.0.1:8001` 返回 HTTP 200。
- Playwright runtime：全局 `@playwright/cli` 内存在 `playwright` 包；本机 Chrome 和 Edge 通道均可 headless launch。
- `cd E:\Urili-Ruoyi\react-ui; npx biome check ..\scripts\smoke\seller-portal-product-ui-smoke.mjs`：通过。
- `cd E:\Urili-Ruoyi; node --check scripts/smoke/seller-portal-product-ui-smoke.mjs`：通过。
- PowerShell 解析检查：通过。
- `cd E:\Urili-Ruoyi; .\scripts\smoke\seller-portal-product-ui-smoke.ps1 -SellerId 5`：通过，输出：

```text
[ok] admin login
[ok] seller direct-login ticket created
[ok] seller portal loaded
[ok] seller token storage isolated
[ok] seller product card rendered
[ok] seller product detail modal rendered
[ok] seller portal logout cleanup
[pass] seller portal product UI smoke completed.
```

## 当前判断

seller portal “我的商城商品”现在具备可复跑的浏览器验收脚本。后续复制 buyer 前，应先用该脚本和现有后端 HTTP smoke 完成 seller 模板验收；buyer 商品浏览口径仍需单独确认，不能直接把 seller 商品拥有关系机械替换成 buyer。
