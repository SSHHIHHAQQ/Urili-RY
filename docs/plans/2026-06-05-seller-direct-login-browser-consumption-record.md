# 2026-06-05 卖家端免密直登浏览器消费执行记录

## 背景

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，并按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏执行。

本切片只处理卖家端免密直登浏览器消费验收，以及验收过程中发现的直登页 Ant Design 加载态警告。不复制买家、不改 SQL、不改后端接口、不改权限点。

## 已完成

- 修改 `react-ui/src/pages/Portal/DirectLogin/index.tsx`。
- 将直登页加载态 `Spin` 的 `tip` 改为 `description`，消除 Ant Design 已废弃属性警告。
- 使用浏览器生成并消费卖家端账号级免密票据。
- 验证 `/seller/direct-login` 消费后落到 `/seller/portal`。
- 验证直登后写入 seller 端 token，不写入 buyer 端 token。
- 验证 seller token 可访问 seller `getInfo` / `getRouters`，但调用 buyer `getInfo` 被业务拒绝。
- 验证免密票据消费后状态变为 `USED`，并保留本次代入原因。
- 验证后主动调用 `/api/seller/logout`，清理浏览器内 seller 端 token 和会话缓存。

## 数据源与影响

- 数据源确认：后端激活 `druid`；MySQL / Redis 均通过 `RUOYI_*` 运行变量读取；本轮未输出 `.env.local` 凭证。
- 本轮未执行人工 SQL、DDL 或 DML。
- 本轮通过现有管理端接口生成并消费免密票据，会产生正常运行时数据：`portal_direct_login_ticket` 票据、seller 端会话、seller 端登录日志和相关审计信息。
- 本记录不写入、不展示明文 `directLoginToken`。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/pages/Portal/DirectLogin/index.tsx src/components/PartnerManagement/PartnerAuditModal.tsx`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。
- Playwright / Chrome 生成卖家直登票据：通过，目标 `sellerId=9`、`sellerNo=SAF030002`、`accountId=8`、`userName=1234`、`ticketId=106`、有效期 `30` 分钟、直登路径 `/seller/direct-login`。
- Playwright / Chrome 消费卖家直登票据：通过，最终路径 `/seller/portal`。
- 端 token 隔离验证：`sellerTokenPresent=true`，`buyerTokenPresent=false`，管理端 token 未被直登页覆盖。
- seller 端接口验证：seller `getInfo` HTTP `200`、业务 `code=200`，返回 `subjectId=9`、`accountId=8`；seller `getRouters` HTTP `200`、业务 `code=200`。
- 跨端拒绝验证：使用 seller token 调 buyer `getInfo`，HTTP `200`、业务 `code=401`。
- 票据消费验证：`ticketStatus=USED`、`usedTime` 已写入、代入原因匹配。
- 浏览器控制台检查：`npx --yes --package @playwright/cli playwright-cli console error` 返回 `Errors: 0, Warnings: 0`。
- 会话清理验证：`/api/seller/logout` HTTP `200`、业务 `code=200`；清理后 `hasSellerTokenAfterCleanup=false`、`hasSellerUserAfterCleanup=false`。
- `cd E:\Urili-Ruoyi; git diff --check -- react-ui/src/pages/Portal/DirectLogin/index.tsx docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-seller-direct-login-browser-consumption-record.md`：通过，仅有 LF/CRLF 归一化提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 1 changed files`。

## 权限检查

- 票据生成仍通过管理端卖家控制面接口，权限边界属于管理端 `seller:admin:directLogin`。
- 票据消费后端内身份由 seller 端 token 建立，后续 seller 端接口继续由 `PortalSessionContext` 和端内权限控制。
- 本轮没有新增菜单、按钮或权限标识。

## 字典与选项复用检查

- 本轮不涉及字典、国家/地区、状态选项或字段映射新增。

## 复用台账检查

- 已更新 `docs/architecture/reuse-ledger.md` 的“三端前端直登入口与端内工作台模板”条目，补充直登页 `Spin.description` 规则和卖家直登浏览器消费验收约束。

## 大文件与重复代码检查

- `react-ui/src/pages/Portal/DirectLogin/index.tsx` 职责单一，未达到拆分阈值。
- 本轮没有复制买家代码，没有新增同构重复实现。

## 未验证与后续

- 买家端直登浏览器消费未在本切片执行；等卖家模板验收稳定后，再按同一规则复制/验收买家。
- 低权限管理端账号生成免密票据的负向验收未在本切片执行。
