# 2026-06-07 三端 P0/P1 读模型 Staging 与手机号筛选收口记录

## 背景

- 参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 当前模式：只修 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失。
- 本轮不做浏览器运行态验收、不做截图、不做 DOM 检测、不做 UI 细调。
- 本轮不执行远程 MySQL DDL/DML，不读取或写入 Redis。

## 子 Agent 使用

- 本轮按用户要求使用 `gpt-5.4` 子 Agent。
- 启动 6 个 `gpt-5.4` 只读子 Agent，分别覆盖：
  - 后端权限和串端风险。
  - SQL guard 与读模型 replay-safe 风险。
  - React token/session/route guard。
  - seller/buyer 管理端 UI/service 字段。
  - 菜单 seed、端内菜单和 role-menu 风险。
  - Markdown 目标追踪残留。
- 6 个子 Agent 均已完成并关闭。

## 新增问题

- P0：seller/buyer 管理端列表“手机号”筛选前端发送 `phone`，后端 mapper 实际只识别 `contactPhone`，会导致筛选静默失效。
- P1：`20260607_source_product_read_model.sql` / `20260607_source_warehouse_stock_read_model.sql` 已有源 schema guard，但读模型刷新仍需要进一步降低“正式读模型先被清空、后续回填失败”的窗口。

## 已修复问题

- `RuoYi-Vue/sql/20260607_source_product_read_model.sql`
  - 先写 `tmp_source_product_group` / `tmp_source_product_dimension_group` / `tmp_source_product_warehouse_detail`。
  - staging 全部构建成功后，在事务内删除并替换 `OFFICIAL_MASTER` 正式读模型范围。
  - `source_product_warehouse_detail` 最终回写时显式排除自增 `id`，避免与正式表已有行冲突。
- `RuoYi-Vue/sql/20260607_source_warehouse_stock_read_model.sql`
  - 先写 `tmp_source_warehouse_stock_detail` / `tmp_source_warehouse_stock_group` / `tmp_source_warehouse_stock_filter_metric`。
  - group 和 filter metric 聚合统一读取临时 detail 表。
  - staging 全部构建成功后，在事务内删除并替换 `OFFICIAL_MASTER` 正式读模型范围。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
  - 两个读模型专项合同新增 temporary staging、final transaction copy 和提交顺序断言。
- `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx` / `.js`
  - 查询列改为 `contactPhone`。
  - `buildListParams(...)` 兼容旧浏览器缓存里的 `phone`，提交前转换为 `contactPhone` 并删除 `phone`。
- `react-ui/src/types/seller-buyer/seller.d.ts` / `buyer.d.ts`
  - `SellerListParams` / `BuyerListParams` 查询参数改为 `contactPhone`。
- `docs/architecture/reuse-ledger.md`
  - 补充读模型 temporary staging 规则。
  - 补充 seller/buyer 管理端列表手机号查询参数规则。

## 残留问题

- 历史残留已收口：`integration` fresh bootstrap schema 策略已由 `docs/plans/2026-06-07-three-terminal-p0p1-integration-bootstrap-chain-record.md` 固定为 bootstrap 后必跑 SQL 清单，不再属于未定 P1。
- P1：通用 SQL 自动发现仍主要按 `202606*.sql` 扫描，动态 DDL helper 和未来月份脚本覆盖边界需要后续单独硬化。
- P1：端内 role-menu 仍使用裸 `menuIds` 作为授权输入；由于 `seller_menu` / `buyer_menu` ID 空间可能重叠，跨端提交同数字 ID 可能静默绑定成本端菜单，后续应按稳定业务键或全局不重叠 ID 方案收口。
- P1：管理端 `sys_menu` 的 `2010` 仍存在 `top_menu_seed.sql` 和 `seller_buyer_management_seed.sql` 双 owner 风险，后续应只保留一个最终 owner。
- P1：自定义“重置为指定密码”后端/service 仍在，但 UI 当前只接入默认密码重置；后续需要确认保留指定密码弹窗，或删除未接通的前端 service 导出。
- P2：买家额外保留“充值能力 / 规划中”占位，卖家无对应占位；当前判断为业务有意差异，不阻塞。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn clean -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，`SqlExecutionGuardContractTest` 31 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm exec tsc -- --noEmit`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:portal-token`：通过，输出 `Portal token isolation guard passed.`。
- `cd E:\Urili-Ruoyi\react-ui; node --check src\components\PartnerManagement\PartnerManagementPage.js`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm exec jest -- --config jest.config.ts tests/remote-menu-route-guard.test.ts --runInBand`：通过，1 个 suite、9 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check -- ...`：通过，仅有 LF/CRLF 归一化提示。

## 未验证原因

- 未执行远程 SQL：本轮只收口源码 SQL 和静态合同，不对远程 MySQL 做 DDL/DML。
- 未读取或写入 Redis：本轮未改 Redis 行为。
- 未启动或重启后端：本轮无运行态验收要求。
- 未做浏览器运行态、截图或 DOM 检测：用户已明确本轮不做。

## 权限检查结果

- 后端权限和串端只读子 Agent 未发现 P0/P1。
- React token/session/route guard 只读子 Agent 未发现 P0/P1。
- 本轮改动未新增后端接口、权限标识或按钮权限。

## 字典/选项复用检查结果

- 本轮未新增字典或业务选项。
- 手机号筛选只修正请求参数，不改变展示 code/label。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`：
  - 读模型 temporary staging 规则。
  - seller/buyer 管理端列表 `contactPhone` 查询参数规则。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，`Synced 5 changed files`，`Modified: 5 - 274 nodes in 1.1s`。

## 大文件合理性判断结果

- `SqlExecutionGuardContractTest.java` 已是大型架构合同测试文件，本轮只在既有专项合同中追加断言，不拆分。
- `PartnerManagementPage.tsx/js` 已是同构 seller/buyer 管理端模板文件，本轮只做参数修复，不新增 UI 复杂度。

## 重复代码检查结果

- seller/buyer 手机号筛选通过同一 `PartnerManagementPage` 模板修复，没有分别在 Seller/Buyer 页面重复实现。
- 两个读模型脚本采用同一 temporary staging + final transaction copy 思路，但保留各自读模型字段和聚合逻辑，没有抽成不透明通用 SQL。
