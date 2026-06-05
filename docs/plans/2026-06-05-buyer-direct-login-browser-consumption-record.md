# 2026-06-05 买家端免密直登浏览器消费执行记录

## 背景

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，并按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏执行。

卖家端免密直登浏览器消费已在 `docs/plans/2026-06-05-seller-direct-login-browser-consumption-record.md` 验收通过。本切片只按同一清单验收买家端直登消费链路，不改 SQL、不改后端接口、不改权限点、不重新设计直登页。

## 已完成

- 使用浏览器生成并消费买家端账号级免密票据。
- 验证 `/buyer/direct-login` 消费后落到 `/buyer/portal`。
- 验证直登后写入 buyer 端 token，不写入 seller 端 token。
- 验证 buyer token 可访问 buyer `getInfo` / `getRouters`，但调用 seller `getInfo` 被业务拒绝。
- 验证免密票据消费后状态变为 `USED`，并保留本次代入原因。
- 验证后主动调用 `/api/buyer/logout`，清理浏览器内 buyer 端 token 和会话缓存。
- 本轮未新增代码变更；买家复用上一片已修正的共用直登页。

## 数据源与影响

- 数据源确认：后端激活 `druid`；MySQL / Redis 均通过 `RUOYI_*` 运行变量读取；本轮未输出 `.env.local` 凭证。
- 本轮未执行人工 SQL、DDL 或 DML。
- 本轮通过现有管理端接口生成并消费免密票据，会产生正常运行时数据：`portal_direct_login_ticket` 票据、buyer 端会话、buyer 端登录日志和相关审计信息。
- 本记录不写入、不展示明文 `directLoginToken`。

## 验证结果

- Playwright / Chrome 生成买家直登票据：通过，目标 `buyerId=2`、`buyerNo=BAF030001`、`accountId=2`、`ticketId=107`、有效期 `30` 分钟、直登路径 `/buyer/direct-login`。
- Playwright / Chrome 消费买家直登票据：通过，最终路径 `/buyer/portal`。
- 端 token 隔离验证：`buyerTokenPresent=true`，`sellerTokenPresent=false`，管理端 token 未被直登页覆盖。
- buyer 端接口验证：buyer `getInfo` HTTP `200`、业务 `code=200`，返回 `subjectId=2`、`accountId=2`；buyer `getRouters` HTTP `200`、业务 `code=200`。
- 跨端拒绝验证：使用 buyer token 调 seller `getInfo`，HTTP `200`、业务 `code=401`。
- 票据消费验证：`ticketStatus=USED`、`usedTime` 已写入、代入原因匹配。
- 浏览器控制台检查：`npx --yes --package @playwright/cli playwright-cli console error` 返回 `Errors: 0, Warnings: 0`。
- 会话清理验证：`/api/buyer/logout` HTTP `200`、业务 `code=200`；清理后 `hasBuyerTokenAfterCleanup=false`、`hasBuyerUserAfterCleanup=false`。
- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/pages/Portal/DirectLogin/index.tsx src/pages/Buyer/index.tsx src/services/buyer/buyer.ts`：通过，Biome 输出 `Checked 2 files`；`src/services/buyer/buyer.ts` 按当前 Biome 配置被忽略，service 类型由 `tsc` 覆盖。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- `cd E:\Urili-Ruoyi; git diff --check -- docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-buyer-direct-login-browser-consumption-record.md`：通过，仅有 LF/CRLF 归一化提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 权限检查

- 票据生成仍通过管理端买家控制面接口，权限边界属于管理端 `buyer:admin:directLogin`。
- 票据消费后端内身份由 buyer 端 token 建立，后续 buyer 端接口继续由 `PortalSessionContext` 和端内权限控制。
- 本轮没有新增菜单、按钮或权限标识。

## 字典与选项复用检查

- 本轮不涉及字典、国家/地区、状态选项或字段映射新增。

## 复用台账检查

- 已更新 `docs/architecture/reuse-ledger.md` 的“三端前端直登入口与端内工作台模板”条目，补充买家直登浏览器消费验收约束。

## 大文件与重复代码检查

- 本轮未修改代码文件，不涉及新的大文件或重复代码。
- 买家端继续复用共用 `DirectLoginPage` 和 `buyerPortalSessionService`，没有复制新的直登页面。

## 未验证与后续

- 低权限管理端账号生成买家免密票据的负向验收未在本切片执行。
- 买家端正式物理拆分到 `buyer-ui` 后，还需要复用本验收清单做独立前端入口验证。
