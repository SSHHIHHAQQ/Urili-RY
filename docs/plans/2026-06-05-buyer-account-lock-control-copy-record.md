# 2026-06-05 买家账号锁定解锁模板复制执行记录

## 目标范围

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“卖家模板先定型、买家只复制替换；每个切片只改一类东西”的节奏，本轮只把已验收的卖家账号锁定/解锁模板复制到买家管理。

本轮不做买家充值功能，不扩展余额逻辑，不调整三端物理前端拆分，不改变主体 `status` 启停语义。

## 已修复问题

- `buyer_account` 新增独立锁定字段：`lock_status`、`lock_reason`。
- 新增远程增量 SQL：`RuoYi-Vue/sql/20260605_buyer_account_lock_control.sql`。
- 同步综合初始化 SQL：`RuoYi-Vue/sql/seller_buyer_management_seed.sql`。
- 同步三端迁移 SQL：`RuoYi-Vue/sql/20260604_three_terminal_isolation_migration.sql`。
- 同步买家账号权限 seed：`RuoYi-Vue/sql/20260605_admin_buyer_account_permission_seed.sql`。
- 新增管理端权限点：`buyer:admin:account:lock`。
- 后端新增/接齐买家账号锁定与解锁链路：
  - `PUT /buyer/admin/buyers/{buyerId}/accounts/{accountId}/lock`
  - `PUT /buyer/admin/buyers/{buyerId}/accounts/{accountId}/unlock`
  - `BuyerMapper.xml` 返回并写入 `lock_status` / `lock_reason`。
  - `BuyerPortalPermissionServiceImpl` 在端内权限读取入口拒绝已锁账号。
- Service 规则：
  - 锁定必须填写原因，最长 500 字。
  - 锁定后强制踢出该买家账号在线会话，并删除 buyer 端 token。
  - 解锁只清理锁定字段，不恢复旧会话。
  - 普通账号编辑接口保留当前锁定状态，不允许通过编辑接口绕过锁定/解锁专用权限。
  - 账号登录、免密登录生成、端内改密、端内权限校验均拒绝锁定账号。
- 前端管理端买家页面：
  - `react-ui/src/services/buyer/buyer.ts` 增加 `lockAdminBuyerAccount` / `unlockAdminBuyerAccount`。
  - `react-ui/src/pages/Buyer/index.tsx` 注入 `buyer:admin:account:lock`、`lockAccount`、`unlockAccount`。
  - `react-ui/src/types/seller-buyer/buyer.d.ts` 增加 `lockStatus` / `lockReason`。
  - `react-ui/scripts/check-partner-management-template.mjs` 升级为 seller/buyer 双端都强制检查锁定 service、URL、权限和配置绑定。

## 新增问题

- 浏览器表单登录临时低权限账号时，页面停留在登录页，没有完成跳转；本轮最终 UI 验收改用真实 `/login` 返回的低权限 token 注入管理端 `access_token` 后打开 `/partner/buyer`。该问题不影响本轮权限显隐结论，但后续如要专门验收登录页低权限账号跳转，可另开切片。

## 残留问题

- 本轮没有新增可复跑 smoke 脚本；HTTP smoke 和浏览器验收通过临时 PowerShell / Playwright CLI 执行。
- 管理端买家账号锁定/解锁已复制完成；后续如继续复制其它 seller 模板能力，仍按“一次只复制一类能力”的节奏推进。

## 数据源确认

- 后端运行环境：`http://127.0.0.1:8080`。
- 前端运行环境：`http://127.0.0.1:8001`。
- SQL 目标：当前后端激活配置对应的远程 MySQL。
- 连接来源：本机 `.env.local` 中的 `RUOYI_DB_*` 运行变量。
- 本轮未读取或修改 Redis 数据；锁定成功时运行时会调用后端 token 清理逻辑。
- 执行记录不输出数据库密码、Redis 密码、token secret、JWT 或完整连接串。

## 远程 SQL 执行结果

执行 SQL：`RuoYi-Vue/sql/20260605_buyer_account_lock_control.sql`。

预检：

- `buyer_account` 锁定字段数：`0`。
- `idx_buyer_account_buyer_lock` 索引数：`0`。
- `buyer:admin:account:lock` 权限数：`0`。
- `buyer_account_lock_status` 字典类型数：`0`。
- `buyer_account_lock_status` 字典数据数：`0`。

执行：

- 首次执行语句数：`14`。
- 幂等复跑语句数：`14`。

执行后：

- `buyer_account` 锁定字段数：`2`。
- `idx_buyer_account_buyer_lock` distinct 索引数：`1`。
- `menu_id=2323` 且 `perms=buyer:admin:account:lock`：`1`。
- `buyer:admin:account:lock` 权限数：`1`。
- `buyer_account_lock_status` 字典类型数：`1`。
- `buyer_account_lock_status` 字典数据数：`2`。

## 后端接口验收

管理员 token：

- `GET /buyer/admin/buyers/list?pageNum=1&pageSize=10`：业务 `code=200`，`total=1`。
- `GET /buyer/admin/buyers/{buyerId}/accounts`：业务 `code=200`，账号数 `1`。
- 空锁定原因请求：业务 `code=500`，账号 `lockStatus` / `lockReason` 未变化。
- 有效锁定请求：业务 `code=200`，锁定后 `lockStatus=1`，原因匹配。
- 锁定后账号级免密登录：业务 `code=500`，提示买家账号已锁定不能免密登录。
- 解锁请求：业务 `code=200`，解锁后 `lockStatus=0`，`lockReason=''`。

低权限 token：

- 临时角色：`codex_buyer_lock_negative`。
- 临时账号：`codex_b_lock_ltd`。
- 允许权限：`buyer:admin:list`、`buyer:admin:query`、`buyer:admin:account:list`。
- 禁止权限：`buyer:admin:account:lock`。
- `/getInfo`：`buyer:admin:account:list=true`，`buyer:admin:account:lock=false`，权限数量 `3`。
- `GET /buyer/admin/buyers/list?pageNum=1&pageSize=1`：业务 `code=200`。
- `GET /buyer/admin/buyers/{buyerId}/accounts`：业务 `code=200`，账号数 `1`。
- `PUT /buyer/admin/buyers/{buyerId}/accounts/{accountId}/lock`：业务 `code=403`。
- `PUT /buyer/admin/buyers/{buyerId}/accounts/{accountId}/unlock`：业务 `code=403`。
- 低权限锁定/解锁请求后，账号 `lockStatus` / `lockReason` 未变化。
- 清理后临时账号剩余 `0`，临时角色剩余 `0`。

## 浏览器验收

使用 Playwright CLI 打开 `http://127.0.0.1:8001/partner/buyer`。本轮使用真实 `/login` 返回的低权限管理端 token 注入 `access_token` / `refresh_token` / `expireTime` 后进入页面；未读取 console 和 network 响应正文，避免输出 token。

结果：

- `buyer:admin:account:list=true`。
- `buyer:admin:account:lock=false`。
- 买家账号弹窗可打开。
- 锁定状态列数量：`1`。
- “锁定账号 / 解锁账号”操作数量：`0`。
- “更多”账号操作按钮数量：`0`。
- 管理端 token 存在：`true`。
- `seller_access_token=false`。
- `buyer_access_token=false`。
- 截图：`react-ui/output/playwright/buyer-lock-lowperm-negative.png`，文件大小 `51957` bytes。
- 清理后临时账号剩余 `0`，临时角色剩余 `0`。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl buyer -am "-Dtest=BuyerServiceImplTest,BuyerPortalPermissionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl ruoyi-system -am "-Dtest=PartnerSupportTest,BuyerAdminPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl buyer -am test
mvn -DskipTests install
mvn -DskipTests install -rf :ruoyi-admin

cd E:\Urili-Ruoyi
.\start-backend-local.ps1 -Restart

cd E:\Urili-Ruoyi\react-ui
node --check scripts/check-partner-management-template.mjs
npm run guard:partner-management
npx biome lint src/components/PartnerManagement/PartnerAccountModal.tsx src/components/PartnerManagement/PartnerManagementPage.tsx src/pages/Buyer/index.tsx src/services/buyer/buyer.ts src/types/seller-buyer/buyer.d.ts scripts/check-partner-management-template.mjs
npm run tsc -- --pretty false
npm run guard:portal-token
npx --yes --package @playwright/cli playwright-cli --raw -s=buyer-lock-neg run-code --filename <temp-js-file>

cd E:\Urili-Ruoyi
codegraph sync .
```

## 验证结果

- buyer 定向单测：`Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`。
- system 定向契约测试：`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。
- buyer 完整模块测试：`ruoyi-system` 44 个测试、`finance` 9 个测试、`buyer` 33 个测试均通过。
- `node --check scripts/check-partner-management-template.mjs`：通过。
- `npm run guard:partner-management`：通过。
- 定向 `biome lint`：通过，`Checked 5 files`。
- `npm run tsc -- --pretty false`：通过。
- `npm run guard:portal-token`：通过。
- `mvn -DskipTests install`：第一次在 `ruoyi-admin` repackage 处因旧 8080 Java 进程锁住 `ruoyi-admin.jar` 失败；已停止旧进程后执行 `mvn -DskipTests install -rf :ruoyi-admin`，通过。
- `start-backend-local.ps1 -Restart` 后，`http://127.0.0.1:8080` HTTP `200`。

## 权限检查结果

- 管理端接口 `lock` / `unlock` 均使用 `@PreAuthorize("@ss.hasPermi('buyer:admin:account:lock')")`。
- 低权限角色未绑定 `buyer:admin:account:lock`，接口返回 `403` 且状态不变。
- 前端 `Buyer/index.tsx` 通过 `accountPermissions.lock` 控制锁定/解锁操作展示。
- 模板守卫已要求 buyer 和 seller 都配置各自 `*:admin:account:lock`、`lockAccount`、`unlockAccount`。

## 字典/选项复用检查结果

- 新增 buyer 独立字典：`buyer_account_lock_status`。
- 字典值沿用 seller 模板 code：`0=未锁定`、`1=已锁定`。
- 未复用 `seller_account_lock_status`，避免 buyer 弹窗按 `${moduleKey}_account_lock_status` 取字典时串端。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，把 seller 锁定模板复制到 buyer 的规则和验收结果写入 `PartnerAccountModal / PartnerAccountRoleModal` 复用段。

## CodeGraph 更新结果

- 已执行 `cd E:\Urili-Ruoyi; codegraph sync .`。
- 首次同步结果：`Already up to date`，命令退出码 `0`。
- 回填记录后复跑结果：`Already up to date`，命令退出码 `0`。

## 大文件合理性判断结果

- `BuyerServiceImpl.java` 当前约 `721` 行，属于既有 Service 聚合文件；本轮只复制 seller 锁定/解锁路径，没有在本切片拆分，避免引入额外结构变化。
- `BuyerServiceImplTest.java` 当前约 `1075` 行，属于既有端账号 service 行为测试集合；本轮按 seller 模板补充锁定相关断言，后续若继续扩展账号测试，建议按行为域拆分测试类。
- `PartnerAccountModal.tsx` 当前约 `733` 行，本轮未修改共享弹窗；后续如继续扩账号能力，应优先拆账号表格、账号表单或操作区。

## 重复代码检查结果

- 前端没有复制买家账号弹窗组件，只通过 `PartnerModuleConfig` 注入 buyer service 和权限。
- 后端 buyer 复制 seller 行为模板，但仍保持 buyer 独立接口、独立 Mapper、独立表字段、独立权限点和独立字典。
- `check-partner-management-template.mjs` 已升级为回归守卫，防止 seller/buyer 模板后续漏接锁定能力。

## 当前判断

- 买家账号锁定/解锁已按卖家标准模板复制，并完成 SQL、后端、前端、低权限接口和低权限浏览器验收。
- 该切片证明的是“低权限账号能看买家账号列表，但不能锁定/解锁”，不是通过隐藏整个账号入口规避权限验证。
