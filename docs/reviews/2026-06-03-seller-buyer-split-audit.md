# 2026-06-03 卖家/买家分表草稿检查

## 结论

当前代码层不需要整体重做。旧的单表 `urili_customer + customer_kind` 模型已经从后端 Java、Mapper XML、React 页面、service、types 的有效实现中移除，卖家/买家已经按独立模块落地。

唯一需要产品口径确认的是表名前缀：当前 `AGENTS.md` 和已实施方案采用 `seller`、`buyer`、`seller_account`、`buyer_account`；用户本轮描述里写的是 `seller`、`buyer`、`seller_account`、`buyer_account`。如果最新口径改回带 `urili_` 前缀，则需要做一轮表名、Mapper SQL、迁移脚本和文档重命名；如果继续遵守前面确认的“表不要默认带 urili，直接写 seller”规则，则不需要重做。

## 新增问题

### 1. 表名前缀口径冲突

- 当前实现：`seller`、`buyer`、`seller_account`、`buyer_account`。
- 用户本轮描述：`seller`、`buyer`、`seller_account`、`buyer_account`。
- 当前 `AGENTS.md` 规则：数据库业务表按业务对象直接命名，不默认加 `urili_`。

判断：这是口径冲突，不是代码 bug。需要用户明确最终表名口径。

### 2. 旧命名仍存在于非核心位置

- `RuoYi-Vue/sql/seller_buyer_management_seed.sql` 文件名仍带 `customer`，但内容已经是卖家/买家分表。
- 旧检查时发现 active top menu seed 仍有旧命名；后续已调整为 `RuoYi-Vue/sql/top_menu_seed.sql`，根菜单为 `主体管理`，route name 为 `PartnerManagement`。
- 多个历史架构文档仍保留旧 `urili_customer` 单表方案内容。

判断：不影响当前编译和核心模块，但会误导后续 agent。建议后续做一次“历史草稿归档/重命名”小任务。

### 3. 前端共享组件仍偏大

- `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx` 当前约 928 行。
- 它已经不再是 `CustomerManagementPage(kind)`，但仍包含表格、表单、账号弹窗、附件处理和字典转换。

判断：不需要立即重做，但超过 500 行检查线。后续如果继续加字段或流程，应拆 `components`、`hooks`、表单配置和账号弹窗。

### 4. 同一 `sys_user` 不能跨卖家/买家的约束主要在 Service 层

- `seller_account.user_id` 和 `buyer_account.user_id` 各自有唯一索引。
- `SellerServiceImpl` / `BuyerServiceImpl` 会检查同一 `user_id` 是否已绑定任一端账号。
- 但 MySQL 无跨表唯一约束，手工直接写库仍可能绕过 Service。

判断：应用路径满足当前规则；如果以后对数据一致性要求更高，需要增加迁移校验 SQL 或触发器方案。

## 已修复问题

- 已修正 `docs/architecture/2026-06-03-seller-buyer-split-module-design.md` 底部状态，去掉“未实施”的过期说法，改为 SQL、Java、React 已实施。

## 残留问题

- 远端数据库当前未检查，尚不确认远端是否已经存在旧 `urili_customer` / `urili_customer_account` 或是否已执行分表迁移。
- 旧历史文档仍保留单表方案，只能作为历史记录，不应作为当前实现依据。
- 菜单根目录仍使用“客户管理”口径；如果产品希望彻底弱化 customer 概念，需要单独调整菜单种子和路由名。

## 验证命令

```powershell
rg -n "urili_customer|customer_kind|PartnerManagement|CustomerManagementPage|API\.Urili\.Customer|urili_customer_type|urili_country_region" RuoYi-Vue react-ui -g "!RuoYi-Vue/sql/seller_buyer_management_seed.sql"
```

结果：旧检查时有效代码只发现 active top menu seed 的根菜单旧命名；后续已改为 `RuoYi-Vue/sql/top_menu_seed.sql` 的 `主体管理`。

```powershell
rg -n "create table if not exists seller|create table if not exists buyer|create table if not exists seller_account|create table if not exists buyer_account" RuoYi-Vue\sql\seller_buyer_management_seed.sql
```

结果：SQL 当前创建 `seller`、`buyer`、`seller_account`、`buyer_account`。

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -DskipTests install
```

结果：通过，`BUILD SUCCESS`。

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc
```

结果：通过。

## 未验证原因

- 未查询远端数据库：本次是代码/SQL/文档一致性检查，没有得到执行远端库查询或迁移的明确确认。
- 未做浏览器联调：后端当前连接远端 MySQL/Redis，直接启动会影响真实环境，需要单独确认联调窗口和目标数据。
- 未执行 SQL 到远端：分表 DDL/DML 属于远端数据库变更，必须先确认最终表名口径和迁移方案。

## 权限检查结果

- 卖家后端权限已拆为：
  - `seller:admin:list`
  - `seller:admin:query`
  - `seller:admin:add`
  - `seller:admin:edit`
  - `seller:admin:changeStatus`
  - `seller:admin:resetPwd`
- 买家后端权限已拆为：
  - `buyer:admin:list`
  - `buyer:admin:query`
  - `buyer:admin:add`
  - `buyer:admin:edit`
  - `buyer:admin:changeStatus`
  - `buyer:admin:resetPwd`
- 权限 key 保留 `urili:` 前缀是权限命名空间，不等同于表名前缀。

## 字典/选项复用检查结果

- 主体类型：`subject_type`
- 国家/地区：`country_region`
- 卖家等级：`seller_level`
- 买家等级：`buyer_level`
- 卖家账号角色：`seller_account_role`
- 买家账号角色：`buyer_account_role`
- 前端未继续使用旧 `urili_customer_type` / `urili_country_region`。

## 复用台账检查结果

- `docs/architecture/reuse-ledger.md` 已登记新字典、后端 support、前端 `PartnerManagementPage`。
- 台账明确不要恢复 `CustomerManagementPage(kind)`。

## 大文件合理性判断结果

- `PartnerManagementPage.tsx` 约 928 行，已超过 500 行检查线。
- 当前暂可接受，因为它是卖家/买家共享 UI 组件，不承载“客户单表”业务归属判断。
- 后续继续扩展时应拆分，不建议继续加大这个文件。

## 重复代码检查结果

- 后端公开模块已按 Seller / Buyer 独立命名，未继续复用 `PartnerManagement*`。
- 前端页面、service、types 已按 Seller / Buyer 独立命名。
- 账号创建、编号生成、字段校验等机械逻辑通过内部 support 复用，没有重新包装成客户模块。

## 是否需要重做

- 不需要整体重做。
- 如果最终表名坚持带 `urili_`，需要重做表名和 Mapper SQL 命名。
- 如果继续执行当前已确认规则，即表名不带 `urili_`，只需要做后续清理：重命名 SQL 文件、归档历史单表文档、视情况调整根菜单 `PartnerManagement` 命名、拆分前端大组件。
