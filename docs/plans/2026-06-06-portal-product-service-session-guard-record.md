# 2026-06-06 端内商品 Service Session 守卫补强记录

## 目标范围

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。

本轮只处理一类问题：补强 seller/buyer 端商城商品 service 的 session fail-closed 自动化守卫。本轮不改生产代码、不改前端、不执行 SQL、不连接远程 MySQL / Redis。

## 已修复问题

- `SellerPortalProductServiceImplTest` 新增列表入口负向用例：非 seller session 必须先返回“登录状态已失效”，不得调用共享 product service 查询商品。
- `SellerPortalProductServiceImplTest` 新增详情和 SKU 入口负向用例：非 seller session 必须先拒绝，且 `selectProductById(...)` / `selectSkuList(...)` 不得被调用。
- `BuyerPortalProductServiceImplTest` 在已有列表负向用例基础上，新增详情和 SKU 入口负向用例：非 buyer session 必须先拒绝，且 `selectOnSaleProductById(...)` / `selectOnSaleSkuList(...)` 不得被调用。
- seller/buyer 测试 fake 同步当前 `IProductDistributionService.batchUpdateSpuStatus(List<Long>, String, boolean)` 签名。

## 新增问题

- 无新增已知问题。

## 残留问题

- 前端三端物理拆分仍未开始；当前仍按 `react-ui/` 验证 seller/buyer 端入口和工作台模板。
- 管理端 seller/buyer UI 控制面已接入，但后续仍可继续做真实浏览器运行验收，确认远程库菜单权限和按钮显隐。

## 权限检查结果

- 本轮不新增权限点、不新增菜单、不改端内 `seller_menu` / `buyer_menu` seed。
- 生产 Controller 已继续由 `TerminalRouteOwnershipTest` 守住 `@PortalPreAuthorize` / `@PortalLog` / `PortalSessionContext` 模板；本轮补的是 service 层入口在 session 不匹配时不查询共享 product service。

## 字典/选项复用检查结果

- 不涉及字典、选项、前端下拉或 code/label 变更。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记端内商品 service 的 session fail-closed 复用规则。

## 数据源与 SQL 检查

- 本轮未读取 `.env.local`。
- 本轮未连接远程 MySQL / Redis。
- 本轮未执行 DDL/DML。
- 本轮不需要新增 SQL 或 seed。

## 大文件合理性判断结果

- `SellerPortalProductServiceImplTest`：395 行，超过 300 行阈值；职责集中在 seller 端商品 service 范围控制、分页元数据、DTO 脱敏和 session 守卫，暂不拆。
- `BuyerPortalProductServiceImplTest`：425 行，超过 400 行阈值；职责集中在 buyer 端商品 service 可见性、DTO 脱敏、分页元数据和 session 守卫，暂不拆。

## 重复代码检查结果

- seller/buyer 商品 service 测试继续保持镜像模板重复，但 buyer 使用独立商品可见性口径，不机械复制 seller 商品拥有关系。
- 本轮不抽跨端测试基类，避免把 seller/buyer 端内模块重新耦合。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl seller -am "-Dtest=SellerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl buyer -am "-Dtest=BuyerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test

cd E:\Urili-Ruoyi
rg -n "[ \t]+$" RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImplTest.java RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImplTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-06-portal-product-service-session-guard-record.md
git diff --check -- RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerPortalProductServiceImplTest.java RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerPortalProductServiceImplTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md
codegraph sync .
```

## 验证结果

- 首次 seller 定向测试失败，原因是测试 fake 仍实现旧的 `batchUpdateSpuStatus(List<Long>, String)` 签名；已只修测试 fake，不改生产代码。
- `SellerPortalProductServiceImplTest`：通过，`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`。
- `BuyerPortalProductServiceImplTest`：通过，`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。
- 尾随空白检查已执行，未发现匹配。
- `git diff --check -- <本轮触碰的已跟踪测试和文档文件>`：通过，仅有 LF/CRLF 工作区换行提示。

## 未验证原因

- 未做 HTTP smoke：本轮只补 service 单测守卫，未改生产接口行为。
- 未做前端验证：本轮不改前端。
- 未做数据库验证：本轮不改 SQL 或远程数据。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 返回 `Already up to date`。
