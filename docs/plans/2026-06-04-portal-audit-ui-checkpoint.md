# 2026-06-04 管理端审计接口与 UI 检查点

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按已确认的模板化方式推进：seller 先落一套，buyer 只替换字段、权限、接口前缀和 service。

## 已完成

- 后端新增卖家/买家只读审计列表接口：
  - `GET /seller/admin/sellers/loginLogs/list`
  - `GET /seller/admin/sellers/operLogs/list`
  - `GET /seller/admin/sellers/directLoginTickets/list`
  - `GET /buyer/admin/buyers/loginLogs/list`
  - `GET /buyer/admin/buyers/operLogs/list`
  - `GET /buyer/admin/buyers/directLoginTickets/list`
- 后端新增/扩展审计查询对象：
  - `PortalLoginLog`
  - `PortalOperLog`
  - `PortalDirectLoginTicket`
- 后端 mapper 已接入独立表只读查询：
  - `seller_login_log`
  - `seller_oper_log`
  - `buyer_login_log`
  - `buyer_oper_log`
  - `portal_direct_login_ticket`
- 免密票据查询在 service 层强制 terminal：
  - seller 只查 `terminal = seller`
  - buyer 只查 `terminal = buyer`
- 新增管理端共享审计弹窗 `PartnerAuditModal`，包含登录日志、操作日志、免密票据三个 tab。
- `PartnerManagementPage` 已新增工具栏“审计”入口和行内“更多 -> 审计”入口。
- 卖家/买家 service 已按模板接入：
  - `getAdminSellerLoginLogs`
  - `getAdminSellerOperLogs`
  - `getAdminSellerDirectLoginTickets`
  - `getAdminBuyerLoginLogs`
  - `getAdminBuyerOperLogs`
  - `getAdminBuyerDirectLoginTickets`
- 权限 SQL 已新增并执行：
  - `RuoYi-Vue/sql/20260604_portal_audit_admin_menu_seed.sql`
  - `docs/plans/2026-06-04-portal-audit-permission-sql-record.md`
- `seller_buyer_management_seed.sql` 已同步补齐 2250-2255 六个审计权限按钮。

## 验证结果

- 数据源确认：
  - MySQL：远端 `fenxiao` 库。
  - Redis：远端 Redis。
  - 本次没有使用本地 Docker MySQL/Redis。
- 远端 SQL 执行：
  - 执行语句数：2。
  - 远端 `sys_menu` 已验证存在 6 条权限记录：
    - `2250 seller:admin:loginLog:list`
    - `2251 seller:admin:operLog:list`
    - `2252 seller:admin:ticket:list`
    - `2253 buyer:admin:loginLog:list`
    - `2254 buyer:admin:operLog:list`
    - `2255 buyer:admin:ticket:list`
- 后端：
  - 第一次 `mvn -DskipTests install`：Java 编译已通过，最终 repackage 因运行中的 `ruoyi-admin.jar` 被锁失败。
  - 停止 8080 后端进程后重跑 `mvn -DskipTests install`：通过。
  - `start-backend-local.ps1 -Restart`：后端已启动，`http://127.0.0.1:8080` 返回 HTTP 200。
- 接口：
  - 管理端登录接口返回 token。
  - 6 个审计列表接口均返回 200。
  - 卖家登录日志 total=20，卖家操作日志 total=0，卖家免密票据 total=7。
  - 买家登录日志 total=9，买家操作日志 total=0，买家免密票据 total=6。
- 前端：
  - `npm run tsc`：通过。
  - Playwright 验证卖家管理：
    - 工具栏展示“审计”。
    - 全局卖家审计弹窗可打开。
    - 登录日志 tab 有数据。
    - 操作日志 tab 查询正常，当前暂无数据。
    - 免密票据 tab 有数据。
    - 行内“更多 -> 审计”可打开，并显示当前卖家标题和主体标识。
  - Playwright 验证买家管理：
    - 工具栏展示“审计”。
    - 全局买家审计弹窗可打开。
    - 登录日志 tab 有数据。
    - 免密票据 tab 有数据。

## 大文件合理性判断

- `PartnerAuditModal.tsx` 单独承载审计弹窗，避免继续扩大 `PartnerManagementPage.tsx`。
- `PartnerManagementPage.tsx` 本轮只增加审计入口、状态和 service 契约；审计表格和筛选逻辑已拆到 `PartnerAuditModal.tsx`。
- 后续如要做独立审计页面、导出或详情能力，不应继续堆进 `PartnerManagementPage.tsx`，应继续拆独立页面或独立公共组件。

## 当前剩余

- `seller_oper_log` / `buyer_oper_log` 当前只有查询入口，真实端内操作日志写入链路还没有接入。
- 审计列表当前为只读查询；导出、清理、详情展开暂不做。
- 真正 seller-ui / buyer-ui 物理拆分仍在后续阶段，不在本轮完成。
