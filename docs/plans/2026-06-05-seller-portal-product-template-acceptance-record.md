# 2026-06-05 卖家端商品模板验收记录

## 参考方向

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，并遵守当前节奏：

- 先做一套标准卖家模板，验收通过后再复制买家。
- 每个切片只改一类东西，减少返工。
- 当前切片只做 seller portal “我的商城商品”模板验收，不复制 buyer，不改后端接口，不执行数据库 DDL/DML。

## 验收范围

本次验收覆盖 seller portal “我的商城商品”模板的后端、前端和真实运行链路：

- 后端 seller 商品只读 service 范围控制。
- seller 端权限 seed 契约。
- 前端 seller 商品模板契约守卫。
- 前端 portal token / query 参数隔离守卫。
- TypeScript 类型检查。
- 后端真实 HTTP smoke。
- 前端真实浏览器 smoke。

本次验收不覆盖：

- buyer 商品浏览口径。
- buyer 商品页面或 buyer 商品 service 复制。
- 三端前端物理拆分。
- 远程数据库 DDL/DML 新变更。

## 验收命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller "-Dtest=SellerPortalProductServiceImplTest" test`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest" test`
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:seller-portal-product`
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller test`
- `cd E:\Urili-Ruoyi; .\scripts\smoke\seller-own-distribution-product-read-template-smoke.ps1 -SellerUsername '594165649@qq.com' -OtherSellerUsername '1234'`
- `cd E:\Urili-Ruoyi; .\scripts\smoke\seller-portal-product-ui-smoke.ps1 -SellerId 5`

## 验证结果

- `SellerPortalProductServiceImplTest`：通过，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- `TerminalRouteOwnershipTest,TerminalSeedPermissionContractTest`：通过，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- `npm run guard:seller-portal-product`：通过，`Seller portal product template guard passed.`。
- `npm run guard:portal-token`：通过，`Portal token isolation guard passed.`。
- `npm run tsc`：通过。
- `mvn -pl seller test`：通过，`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`。
- 后端 HTTP smoke：通过，覆盖 seller 登录、商品列表、伪造客户端范围参数、详情、SKU、字段脱敏、跨 seller 详情/SKU 拒绝和 logout 清理。
- 前端浏览器 smoke：通过，覆盖 admin 创建 seller 免密票据、seller direct-login、seller token storage 隔离、商品卡片、详情弹窗和退出清理。

## 当前判断

seller portal “我的商城商品”模板已完成后端契约、前端契约、真实 HTTP 链路和真实浏览器链路验收，可以作为 seller 端真实业务接口范围控制模板。

buyer 仍未复制。后续若进入 buyer 商品浏览，应先确认 buyer 商品可见性、上架状态、价格口径和库存可见边界，再按已验收的 seller 模板替换 terminal、路由、service、权限、DTO、前端断言和 smoke 脚本；不能把 seller 商品拥有关系机械替换成 buyer。
