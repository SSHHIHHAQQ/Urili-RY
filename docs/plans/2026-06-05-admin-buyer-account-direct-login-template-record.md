# 2026-06-05 管理端买家账号级免密模板执行记录

## 目标

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，在卖家账号级免密模板验收通过后，只复制同一类能力到买家：管理端在买家账号弹窗内，对指定买家端账号生成 30 分钟有效的免密登录链接。

## 范围

- 本轮只做买家账号级免密代入。
- 本轮不新增表、不执行 SQL、不改菜单 seed、不改公共 `PartnerAccountModal` 交互。
- 管理端权限继续使用若依 `sys_menu` / `sys_role` 的 `buyer:admin:directLogin`。
- 买家端账号仍读取 `buyer_account`，不写入 `sys_user`。

## 数据源确认

- 后端激活配置为 `spring.profiles.active=druid`。
- MySQL JDBC URL、用户名和密码均来自 `RUOYI_DB_*` 运行变量。
- Redis host、port、database 和密码均来自 `RUOYI_REDIS_*` 运行变量。
- 后端启动通过 `start-backend-local.ps1 -Restart`，脚本读取 `.env.local`；本记录不输出远程连接信息和 token secret。

## 已完成

- `AdminBuyerController` 新增 `POST /buyer/admin/buyers/{buyerId}/accounts/{accountId}/directLogin`。
- `IBuyerService` / `BuyerServiceImpl` 新增 `createBuyerAccountDirectLogin(...)`。
- 后端校验目标账号必须属于当前买家；买家停用时拒绝生成免密链接。
- 账号级免密复用 `PortalDirectLoginSupport`，terminal 固定为 `buyer`，返回结果包含目标 `accountId`。
- `buyer.ts` 增加 `createAdminBuyerAccountDirectLogin(...)`。
- `pages/Buyer/index.tsx` 注入买家账号级免密 service。
- 前端复用卖家已验收的 `PartnerAccountModal`，账号行“更多”自动展示“登录买家端”。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -DskipTests package`：通过，并已用新 jar 重启后端。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/pages/Buyer/index.tsx src/services/buyer/buyer.ts`：通过；当前 Biome 配置只检查了 `src/pages/Buyer/index.tsx`，`src/services/buyer/buyer.ts` 被忽略。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过，覆盖 buyer service 类型检查。
- Playwright / 系统 Chrome 验收 `/partner/buyer`：通过。
- 浏览器验收结果：账号弹窗 `bodyOverflowX=false`、`modalOverflowX=false`、`tableOverflowX=false`。
- 账号级免密请求返回 HTTP `200`、业务 `code=200`，返回 `accountId=2`、有效期 `30` 分钟、登录路径 `/buyer/direct-login`，链接包含 `directLoginToken`。
- 验收时拦截了 `window.open`，只验证生成链接，没有消费免密 token。
- console error / warning 数量为 `0`，page error 数量为 `0`。

## 大文件判断

- `BuyerServiceImpl.java` 当前约 `528` 行，已经超过 500 行自检阈值。
- 本轮只复制卖家已验收的账号级免密方法，未拆分 service，是为了保持同构复制切片稳定。
- 后续继续扩买家账号、免密、会话或权限测试时，应考虑拆出 buyer account/direct-login service。
- `buyer.ts` 当前约 `294` 行，尚未超过 300 行自检阈值。
- `PartnerAccountModal.tsx` 本轮未修改；其大文件拆分判断沿用卖家账号级免密执行记录。

## 当前判断

- 买家账号级免密模板已按卖家模板复制并通过代码层和浏览器验收。
- 管理端卖家/买家账号弹窗现在都具备账号级免密代入能力。
- 本轮没有执行 SQL，也没有改变远程数据库结构。
