# 仓库编辑回填与操作列配对实现记录

## 背景

用户反馈：

- 点击仓库列表“编辑”没有效果或没有回填保存内容。
- 官方仓库操作列需要增加“配对”按钮。
- 配对流程要求先选择要配对的仓库类型，再选择具体配对哪个主仓的仓库。

## 根因判断

1. 编辑弹窗原先依赖 `formRef.current?.setFieldsValue(...)` 在 `ModalForm` 打开时回填。首次打开时表单实例可能尚未挂载，导致回填不稳定，表现为点编辑后无内容或像没反应。
2. 编辑入口直接使用列表行数据作为表单数据，没有重新拉取详情；当列表数据与详情字段不完全一致时，表单回填存在隐患。
3. 官方仓库列表已拆分履约仓/报价仓展示，但操作列还没有从仓库页直接发起配对的入口。
4. 配对弹窗“履约仓”场景原先走新增的 `/warehouse/official/pairing-connections`，没有复用已经在“同步官方仓库”里跑通的主仓接入接口；在当前运行态未部署新接口或接口过滤不兼容时，会表现为主仓接入下拉为空。
5. 运行库 `fenxiao` 中存在历史结算类型 code，例如 `UPSTREAM-PAYABLE`；仓库模块新写的过滤只按小写 `upstream-payable` 精确匹配，没有复用上游系统模块对旧值的兼容规则，导致真实主仓可能被误过滤。

## 修复内容

### 编辑弹窗

- `react-ui/src/pages/Warehouse/components/WarehouseFormModal.tsx`
  - 去掉不稳定的 `formRef.setFieldsValue` 回填。
  - 改为 `ModalForm` 的 `initialValues`。
  - 使用 `key={current?.warehouseId || 'warehouse-create'}` 确保编辑不同仓库时表单重新初始化。

- `react-ui/src/pages/Warehouse/WarehouseManagementPage.tsx`
  - 点击编辑时先调用详情接口：
    - 官方仓：`GET /warehouse/official/{warehouseId}`
    - 第三方仓：`GET /warehouse/third-party/{warehouseId}`
  - 详情返回成功后再打开编辑弹窗。

### 操作列配对

- 新增前端组件：
  - `react-ui/src/pages/Warehouse/components/WarehousePairingModal.tsx`
- 履约仓下拉复用既有官方仓同步接口：
  - 主仓接入：`GET /warehouse/official/sync-connections`
  - 主仓仓库：`GET /warehouse/official/sync-candidates`
- 报价仓继续走配对专用接口，避免混用履约仓和报价仓语义。
- 前端补充结算类型兼容过滤，识别 `upstream-payable` / `UPSTREAM_PAYABLE` / `UPSTREAM-PAYABLE` 和 `self-operated-receivable` / `PLATFORM_ADVANCE`。
- 交互顺序：
  1. 选择配对仓库类型：`履约仓` / `报价仓`
  2. 选择主仓接入
  3. 选择主仓仓库
- 操作列官方仓显示：
  - `编辑`
  - `配对`
  - `更多` 中放入启停操作，符合表格操作列超过两个动作时收进 Dropdown 的规则。

### 后端接口

- 新增请求对象：
  - `OfficialWarehousePairingRequest`
- 新增接口：
  - `GET /warehouse/official/pairing-connections`
  - `GET /warehouse/official/pairing-candidates`
  - `POST /warehouse/official/{warehouseId}/pairing`
- 权限复用：
  - `warehouse:official:sync`
- 后端规则：
  - `FULFILLMENT` 履约仓只能选择 `upstream-payable` 上游仓接入。
  - `QUOTE` 报价仓只能选择 `self-operated-receivable` 自营仓接入。
  - 兼容旧结算类型 code：`UPSTREAM_PAYABLE`、`UPSTREAM-PAYABLE`、`PLATFORM_ADVANCE`。
  - 同一个官方仓同一配对类型已有绑定时，在同一事务内替换旧绑定。
  - 继续复用 `upstream_system_warehouse_pairing` 作为事实源，不新增表。

## 只读数据核验

目标库按当前激活配置确认为 `fenxiao`。只读查询确认：

- `upstream_system_connection` 有 3 条启用主仓接入：
  - `LX-CA012` / `CA012` / `UPSTREAM-PAYABLE`
  - `LX-NY013-3275A1E1` / `NY013` / `upstream-payable`
  - `LX-KAT-91B1E277` / `KAT` / `self-operated-receivable`
- `upstream_system_warehouse_candidate` 中 3 个主仓均有候选仓库：
  - `LX-CA012`：1 条，ACTIVE 1 条
  - `LX-NY013-3275A1E1`：1 条，ACTIVE 1 条
  - `LX-KAT-91B1E277`：9 条，ACTIVE 9 条

结论：主仓接入和主仓仓库不是缺数据，空白由配对弹窗取值路径和结算类型兼容过滤导致。

配对表结构只读核验：

- `upstream_system_warehouse_pairing` 当前存在 `system_warehouse_code`、`upstream_warehouse_code`。
- 当前未发现 `pairing_role`。

结论：当前运行库还未具备履约仓/报价仓双角色配对所需结构。下拉加载可以通过复用既有同步接口修复；保存接口和新后端 Mapper 要完整启用，仍需先执行 `20260607_upstream_pairing_role_binding.sql`。

## 远端 SQL 执行记录

执行时间：2026-06-07

目标环境：

- 连接来源：`.env.local` 中的 `RUOYI_DB_*`
- 目标库：`fenxiao`
- SQL 文件：`RuoYi-Vue/sql/20260607_upstream_pairing_role_binding.sql`
- 确认 token：`APPLY_UPSTREAM_PAIRING_ROLE_BINDING`

执行结果：

- 执行语句数：49
- `upstream_system_warehouse_pairing` 已存在：
  - `pairing_role`
  - `system_warehouse_code`
  - `upstream_warehouse_code`
- `upstream_system_logistics_channel_pairing` 已存在：
  - `pairing_role`
  - `system_warehouse_code`
  - `upstream_warehouse_code`
- 已复核索引：
  - `uk_upstream_wh_pairing_system_role`
  - `uk_upstream_wh_pairing_upstream_role`
  - `uk_upstream_channel_pairing_system_role`
  - `idx_upstream_channel_pairing_upstream_role`

后端运行态更新：

- 已停止旧 8080 后端进程。
- 已执行 `mvn -pl ruoyi-admin -am -DskipTests package`，`ruoyi-admin.jar` 打包成功。
- 已通过 `start-backend-local.ps1` 启动新后端。
- `http://127.0.0.1:8080` 返回 HTTP 200。

接口验证：

- `GET /warehouse/official/pairing-connections?pairingRole=FULFILLMENT`
  - 返回 2 条：`LX-CA012`、`LX-NY013-3275A1E1`
- `GET /warehouse/official/pairing-candidates?pairingRole=FULFILLMENT&connectionCode=LX-CA012`
  - 返回 1 条：`CA91244744 / MEISU`
- `GET /warehouse/official/pairing-connections?pairingRole=QUOTE`
  - 返回 1 条：`LX-KAT-91B1E277`

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl warehouse -am -DskipTests compile
mvn -pl warehouse -am -DskipTests clean compile

cd E:\Urili-Ruoyi\react-ui
npm run tsc -- --pretty false
npm run build

npx --package @playwright/cli playwright-cli open http://127.0.0.1:8001/warehouse/official
```

## 验证结果

- 后端 `warehouse` 模块编译通过。
- 后端 `warehouse` 模块 clean compile 通过。
- 前端类型检查通过。
- 前端生产构建通过。
- 浏览器验证通过：
  - 官方仓库页能打开配对弹窗。
  - “主仓接入”在履约仓下可显示 `LX-CA012 - CA012`、`LX-NY013-3275A1E1 - NY013`。
  - 选择 `LX-CA012` 后，“主仓仓库”可显示 `CA91244744 - MEISU（已配对：CA012）`，且已配对项保持禁用。

## 未验证项

- 当前代码依赖已有增量 SQL `RuoYi-Vue/sql/20260607_upstream_pairing_role_binding.sql` 中的 `pairing_role` 等字段。
- 按项目规则，远端 DDL/DML 需要明确确认后才能执行；本记录未执行该远端 SQL。
- 本次只验证了下拉加载和已配对禁用，未提交新的配对保存动作，避免修改远端业务数据。
- 因运行库尚未具备 `pairing_role`，本次未重启后端到最新仓库模块代码，避免新 Mapper 直接访问缺失字段。

## 下一步

确认执行 `20260607_upstream_pairing_role_binding.sql` 后：

1. 执行远端 SQL。
2. 重新打包并重启后端。
3. 浏览器验证：
   - 编辑按钮能打开并回填详情。
   - 官方仓操作列显示“配对”。
   - 配对弹窗能按类型加载主仓接入和主仓仓库。
   - 配对保存后列表刷新并展示履约仓/报价仓。
