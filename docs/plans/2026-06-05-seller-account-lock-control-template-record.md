# 2026-06-05 卖家账号锁定解锁模板执行记录

## 目标范围

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收后复制买家；每个切片只改一类东西”的节奏，本轮只落地卖家端账号 `lock_status` / `lock_reason` 锁定解锁控制。

本轮不复制买家，不做三端物理前端拆分，不调整主体 `status` 启停语义。

## 已修复问题

- `seller_account` 新增独立锁定字段：`lock_status`、`lock_reason`。
- 新增远程增量 SQL：`RuoYi-Vue/sql/20260605_seller_account_lock_control.sql`。
- 同步综合初始化 SQL：`RuoYi-Vue/sql/seller_buyer_management_seed.sql`。
- 同步三端迁移 SQL：`RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`。
- 同步卖家账号权限 seed：`RuoYi-Vue/sql/20260605_admin_seller_account_permission_seed.sql`。
- 新增管理端权限点：`seller:admin:account:lock`。
- 后端新增 seller 账号锁定/解锁接口：
  - `PUT /seller/admin/sellers/{sellerId}/accounts/{accountId}/lock`
  - `PUT /seller/admin/sellers/{sellerId}/accounts/{accountId}/unlock`
- Service 规则：
  - 锁定必须填写原因，最长 500 字。
  - 锁定后立即强制踢出该卖家账号在线会话，并删除 seller 端 token。
  - 解锁只清理 `lock_status` / `lock_reason`，不恢复旧会话。
  - 普通账号编辑接口保留当前锁定状态，不允许通过编辑接口绕过锁定/解锁专用权限。
  - 账号登录、免密登录生成、免密登录消费、端内改密、端内权限校验均拒绝锁定账号。
  - 对卖家端登录用户不回显管理员填写的锁定原因。
- 前端管理端账号弹窗：
  - 仅 seller 注入 `lockAccount` / `unlockAccount` 可选 service。
  - buyer 未注入，所以买家账号弹窗不展示锁定列或锁定/解锁操作。
  - seller 锁定/解锁操作收进“更多”，未增加第三个行内按钮。
  - 锁定状态复用 `seller_account_lock_status` 字典，缺失时 fallback。
  - 锁定账号不展示“登录卖家端”免密操作。

## 新增问题

- 无新增已知阻断问题。

## 残留问题

- 买家账号锁定/解锁尚未复制，等待卖家模板验收后按同构规则复制。
- 低权限管理端真实账号对 `seller:admin:account:lock` 的接口和浏览器负向验收，已在后续独立切片 `docs/plans/2026-06-05-seller-account-lock-low-permission-negative-record.md` 补齐。
- 本轮没有新建可复跑 smoke 脚本；HTTP smoke 通过临时 PowerShell 执行。

## 数据源确认

- 数据源来源：本机 `.env.local` 的 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD`。
- 目标类型：远程 MySQL。
- 本轮未读取或修改 Redis 数据；锁定成功时运行时会调用后端 token 清理逻辑。
- 执行记录未输出数据库密码、Redis 密码、token secret 或完整连接串。

## 远程 SQL 执行结果

执行 SQL：`RuoYi-Vue/sql/20260605_seller_account_lock_control.sql`。

预检：

- `seller_account` 锁定字段数：`0`。
- `buyer_account` 锁定字段数：`0`。
- `seller_account` 锁定索引行数：`0`。
- `seller:admin:account:lock` 权限数：`0`。
- `seller_account_lock_status` 字典类型数：`0`。
- `seller_account_lock_status` 字典数据数：`0`。

执行：

- 首次执行语句数：`14`。
- 幂等复跑语句数：`14`。

执行后：

- `seller_account` 锁定字段数：`2`。
- `buyer_account` 锁定字段数：`0`。
- `idx_seller_account_seller_lock` 索引数：`1`，索引列数：`2`。
- `seller:admin:account:lock` 权限数：`1`。
- `seller_account_lock_status` 字典类型数：`1`。
- `seller_account_lock_status` 字典数据数：`2`。

## HTTP Smoke

- 后端已通过 `start-backend-local.ps1 -Restart` 使用新 jar 启动。
- `GET http://127.0.0.1:8080/captchaImage`：HTTP `200`，业务 `code=200`，验证码仍为关闭状态。
- 管理端登录：`admin / admin123`，业务 `code=200`。
- 卖家列表接口：业务 `code=200`。
- 卖家账号列表接口：业务 `code=200`。
- 空锁定原因验证：`PUT /seller/admin/sellers/{sellerId}/accounts/{accountId}/lock` 传空白原因返回业务 `code=500`，锁定状态和锁定原因保持不变。
- 成功锁定/解锁验证：选择未锁定、锁定原因为空、无在线会话的卖家账号；锁定后 `lock_status=1`、`lock_reason=codex smoke verify`；解锁后 `lock_status=0`、`lock_reason=''`。

## 权限检查结果

- 后端锁定/解锁接口均使用 `@PreAuthorize("@ss.hasPermi('seller:admin:account:lock')")`。
- 管理端前端 seller config 配置 `lock: 'seller:admin:account:lock'`。
- 买家端未配置锁定权限、未注入锁定 service。
- 普通账号编辑权限 `seller:admin:account:edit` 不再承担锁定/解锁动作。

## 字典/选项复用检查结果

- 新增并复用 `seller_account_lock_status`：
  - `0`：未锁定
  - `1`：已锁定
- 前端通过 `getDictSelectOption('seller_account_lock_status')` 读取；读取失败时使用同值 fallback。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记 seller 账号锁定/解锁模板和 buyer 暂不复制边界。

## CodeGraph 更新结果

- 已执行 `cd E:\Urili-Ruoyi; codegraph sync .`。
- 同步结果：`Already up to date`，命令退出码 `0`。

## 大文件合理性判断结果

- `PartnerAccountModal.tsx` 已超过 500 行，本轮只追加账号锁定操作的必要入口。后续继续扩账号管理时，应优先拆分账号表格、账号表单或操作区。
- 新增 SQL 文件职责单一，只处理 seller 账号锁定字段、字典和权限。

## 重复代码检查结果

- 后端只实现 seller 标准模板，未复制 buyer。
- 前端通过共享 `PartnerAccountModal` 的可选 service 接入，不在 seller 页面复制账号弹窗逻辑。
- 静态 guard 已补 seller 锁定 service 和路径检查，防止 seller 模板退回手工散写。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system -am "-Dtest=PartnerSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl seller -am "-Dtest=SellerPortalPermissionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl seller -am test
mvn -DskipTests install
mvn -DskipTests install -rf :ruoyi-admin

cd E:\Urili-Ruoyi\react-ui
node --check scripts/check-partner-management-template.mjs
npm run guard:partner-management
npx biome lint src/components/PartnerManagement/PartnerAccountModal.tsx src/components/PartnerManagement/PartnerManagementPage.tsx src/pages/Seller/index.tsx src/services/seller/seller.ts src/types/seller-buyer/seller.d.ts scripts/check-partner-management-template.mjs
npm run tsc -- --pretty false
npm run guard:portal-token

cd E:\Urili-Ruoyi
.\start-backend-local.ps1 -Restart
```

## 验证结果

- `PartnerSupportTest`：`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。
- `SellerServiceImplTest`：`Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`。
- `SellerPortalPermissionServiceImplTest`：`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
- `mvn -pl seller -am test`：通过；`ruoyi-system` 44 个测试、`finance` 9 个测试、`seller` 31 个测试均通过。
- `node --check scripts/check-partner-management-template.mjs`：通过。
- `npm run guard:partner-management`：通过。
- `npx biome lint ...`：通过。
- `npm run tsc -- --pretty false`：通过。
- `npm run guard:portal-token`：通过。
- `mvn -DskipTests install`：前 12 个模块和 `ruoyi-admin` 编译成功，在 `ruoyi-admin` repackage 阶段因旧后端 Java 进程锁住 jar 失败；停止 8080 后端进程后，`mvn -DskipTests install -rf :ruoyi-admin` 通过。

## 未验证原因

- 本轮未做买家复制，因当前切片明确只验收卖家模板。
- 本轮模板落地时尚未做低权限真实管理端账号浏览器负向验收；该项已由后续独立切片 `docs/plans/2026-06-05-seller-account-lock-low-permission-negative-record.md` 补齐。
