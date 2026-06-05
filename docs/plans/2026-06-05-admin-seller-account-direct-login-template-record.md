# 2026-06-05 管理端卖家账号级免密模板执行记录

## 目标

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理一类能力：管理端在卖家账号弹窗内，对指定卖家端账号生成 30 分钟有效的免密登录链接。

## 范围

- 本轮只改卖家账号级免密代入，不复制买家。
- 本轮不新增表、不执行 SQL、不改菜单 seed、不改买家接口。
- 管理端权限继续使用若依 `sys_menu` / `sys_role` 的 `seller:admin:directLogin`。
- 卖家端账号仍读取 `seller_account`，不写入 `sys_user`。

## 数据源确认

- 后端激活配置为 `spring.profiles.active=druid`。
- MySQL JDBC URL、用户名和密码均来自 `RUOYI_DB_*` 运行变量。
- Redis host、port、database 和密码均来自 `RUOYI_REDIS_*` 运行变量。
- 后端启动通过 `start-backend-local.ps1 -Restart`，脚本读取 `.env.local`；本记录不输出远程连接信息和 token secret。

## 已完成

- `AdminSellerController` 新增 `POST /seller/admin/sellers/{sellerId}/accounts/{accountId}/directLogin`。
- `ISellerService` / `SellerServiceImpl` 新增 `createSellerAccountDirectLogin(...)`。
- 后端校验目标账号必须属于当前卖家；卖家停用时拒绝生成免密链接。
- 账号级免密仍复用 `PortalDirectLoginSupport`，terminal 固定为 `seller`，返回结果包含目标 `accountId`。
- `PartnerService` 增加可选 `directLoginAccount` service，不影响尚未接入的买家。
- `seller.ts` 增加 `createAdminSellerAccountDirectLogin(...)`。
- `pages/Seller/index.tsx` 注入卖家账号级免密 service。
- `PartnerAccountModal` 在账号行“更多”里新增“登录卖家端”，要求填写代入原因后生成并打开免密链接。
- 账号级免密确认框未使用外部 `Form.useForm`，避免隐藏弹窗表单未挂载警告。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -DskipTests package`：通过，并已用新 jar 重启后端。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/components/PartnerManagement/PartnerAccountModal.tsx src/components/PartnerManagement/PartnerManagementPage.tsx src/pages/Seller/index.tsx`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- Playwright / 系统 Chrome 验收 `/partner/seller`：通过。
- 浏览器验收结果：账号弹窗 `bodyOverflowX=false`、`modalOverflowX=false`、`tableOverflowX=false`。
- 账号级免密请求返回 HTTP `200`、业务 `code=200`，返回 `accountId=8`、有效期 `30` 分钟、登录路径 `/seller/direct-login`，链接包含 `directLoginToken`。
- 验收时拦截了 `window.open`，只验证生成链接，没有消费免密 token。
- console error / warning 数量为 `0`，page error 数量为 `0`。

## 大文件判断

- `PartnerAccountModal.tsx` 当前约 `580` 行，已经超过 500 行自检阈值。
- 本轮仅补卖家账号级免密入口，未拆分文件，是为了保持卖家模板切片稳定，避免把重构和功能验收混在一起。
- 后续继续扩账号管理时，优先拆 `AccountTable`、`AccountFormModal` 或账号行操作区，不再继续往该文件塞独立配置页。
- `PartnerManagementPage.tsx` 当前约 `1189` 行，属于既有大型共享模板；本轮只通过配置接入 seller service，不扩大列表页职责。
- `SellerServiceImpl.java` 当前约 `528` 行，属于 seller 主服务既有集中实现；后续如果继续扩展会话、免密、权限和账号管理测试，应考虑拆出专门的 account/direct-login service。

## 当前判断

- 卖家账号级免密模板已通过代码层和浏览器验收。
- 买家侧暂不复制；下一步如果复制买家，只替换端类型、文案、路径、权限标识和 buyer service，不重新设计 UI 和流程。
- 本轮没有执行 SQL，也没有改变远程数据库结构。
