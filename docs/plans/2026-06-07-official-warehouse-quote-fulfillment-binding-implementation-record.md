# 官方仓履约/报价绑定实现记录

## 目标

- 官方仓支持两条固定上游绑定：
  - 履约仓：使用上游仓（应付），用于库存、尺寸重量、出库、成本。
  - 报价仓：使用自营仓（应收），用于后续运费试算和销售报价。
- 系统仓库在同一用途下只能绑定一次；同一个系统仓库可以各绑定一个履约仓和一个报价仓。
- 系统渠道在同一用途和系统仓库下只能绑定一次；物流渠道配对必须带具体仓库上下文，避免跨仓同渠道误配。

## 已完成

- 新增配对用途常量：`FULFILLMENT`、`QUOTE`。
- 扩展仓库配对实体、请求对象、Mapper 和 Service，写入 `pairing_role`。
- 扩展物流渠道配对实体、请求对象、Mapper 和 Service，写入 `system_warehouse_code`、`upstream_warehouse_code`、`pairing_role`。
- 后端按接入结算类型强制校验用途：
  - `upstream-payable` 只能配履约。
  - `self-operated-receivable` 只能配报价。
- 官方仓同步入口只展示并使用上游仓（应付）接入，只创建履约绑定。
- 库存同步的仓库翻译只读取 `FULFILLMENT` 配对，避免报价仓污染库存来源。
- 官方仓列表拆分展示“履约仓”和“报价仓”。
- 上游系统管理仓库/物流渠道配对按钮按当前接入显示履约/报价用途。
- 物流渠道配对时要求选择具体领星仓库，并把对应系统仓库带给后端。

## SQL

- 更新 fresh seed：`RuoYi-Vue/sql/upstream_system_management_seed.sql`。
- 新增增量脚本：`RuoYi-Vue/sql/20260607_upstream_pairing_role_binding.sql`。
- 增量脚本包含确认 token：
  - `set @confirm_upstream_pairing_role_binding = 'APPLY_UPSTREAM_PAIRING_ROLE_BINDING';`
- 本次未直接执行远端数据库 DDL/DML。

## 权限检查

- 未新增后端接口权限点。
- 复用现有上游管理权限：
  - `integration:upstream:pair`
  - `integration:upstream:sync`
- 官方仓同步继续复用：
  - `warehouse:official:sync`

## 字典/选项复用

- 新增 `upstream_pairing_role` 字典 seed：
  - `FULFILLMENT`：履约
  - `QUOTE`：报价
- 前端当前使用统一 constants 暴露中文文案，后续可切换为若依字典加载。

## 复用台账

- 已更新 `docs/architecture/reuse-ledger.md`，记录履约/报价绑定角色的事实源、使用边界和后续复用规则。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl integration,warehouse -am -DskipTests compile

cd E:\Urili-Ruoyi\react-ui
npm run tsc -- --pretty false

cd E:\Urili-Ruoyi
git diff --check -- <本次相关文件>

cd E:\Urili-Ruoyi
codegraph sync .
```

## 验证结果

- 后端编译：通过。
- 前端类型检查：通过。
- diff 空白检查：通过，仅有 Windows 换行提示。
- CodeGraph 更新：通过，同步 29 个变更文件。

## 未验证原因

- 未执行远端 SQL，因此当前运行库如果尚未应用 `20260607_upstream_pairing_role_binding.sql`，运行时查询新增列会失败。
- 未做浏览器联调截图；本次先完成模型、后端约束、前端入参和类型检查。

## 残留事项

- 按项目规则确认后执行增量 SQL。
- SQL 应用后重启后端，并在页面验证：
  - 上游仓（应付）只能绑定履约仓。
  - 自营仓（应收）只能绑定报价仓。
  - 官方仓列表能同时显示履约仓和报价仓。
  - 物流渠道配对必须选择具体仓库。

## 2026-06-07 仓库配对下拉修正

### 背景

用户确认仓库配对应沿用每行领星仓库后的“配对”操作：点击后弹窗固定展示当前领星仓库，系统仓库应通过下拉选择，不应要求用户手输系统仓库代码和名称。

### 调整

- `PairingModal` 增加 `customPairingItems`，保留 SKU 和物流渠道的原有手工输入能力，同时允许仓库配对替换为自定义下拉项。
- 上游系统管理仓库配对弹窗改为“系统仓库”可搜索下拉。
- 下拉数据复用现有官方仓库列表接口 `/api/warehouse/official/list`，只取启用仓库。
- 当前主仓同一配对用途下已经绑定过的系统仓库在下拉中标记为“已配对”并禁用，避免重复配对。
- 保存仓库配对时，前端从下拉选项反查 `systemWarehouseCode` 和 `systemWarehouseName` 后提交给后端。

### 验证

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc -- --pretty false

cd E:\Urili-Ruoyi
git diff --check -- react-ui/src/pages/UpstreamSystem/components/PairingModal.tsx react-ui/src/pages/UpstreamSystem/index.tsx docs/plans/2026-06-07-official-warehouse-quote-fulfillment-binding-implementation-record.md

cd E:\Urili-Ruoyi
codegraph sync .
```

- 前端类型检查：通过。
- diff 空白检查：通过，仅有 Windows 换行提示。
- CodeGraph 更新：通过，同步 18 个变更文件。

## 2026-06-07 仓库配对下拉空数据修正

### 根因

- 官方仓库模块的状态 code 使用若依标准：
  - `0`：正常
  - `1`：停用
- 上游系统管理仓库配对下拉误传 `status=ENABLED`，后端 `warehouse` 查询按 `w.status = #{status}` 精确过滤，导致官方仓库全部被过滤掉。
- 第一版下拉过滤只读取当前主仓接入的仓库配对关系，不能覆盖“同一系统仓同一角色只能配一次”的全局约束。

### 调整

- 官方仓库下拉改为 `status=0`。
- 下拉数据仍复用 `/api/warehouse/official/list`。
- 按当前主仓接入结算类型推导配对角色：
  - 履约仓：过滤掉已有 `warehousePairingId` 的官方仓库。
  - 报价仓：过滤掉已有 `quoteWarehousePairingId` 的官方仓库。
- 下拉只保留当前角色仍可选的官方仓库，不再展示已占用仓库。

### 验证

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc -- --pretty false
```

- 前端类型检查：通过。

## 2026-06-07 报价仓误报重复诊断

### 现象

在 KAT 报价仓接入下，把领星仓库 CA012 配对到系统仓库 CA012 时，页面提示“仓库配对重复：系统仓库或领星仓库已经配对”。但 CA012 仅存在履约仓配对，报价仓应允许再次绑定。

### 根因

- 截图中的报错文案已经不在当前源码中，当前源码的重复提示会按角色输出“履约仓/报价仓已经绑定”。
- 本机 8080 当时运行的是旧 `ruoyi-admin.jar` 进程，旧后端仍按不区分 `pairing_role` 的逻辑返回重复配对错误。
- 只读 schema 检查确认当前运行库 `fenxiao` 已具备正确结构：
  - `upstream_system_warehouse_pairing.pairing_role` 已存在。
  - 唯一索引为 `uk_upstream_wh_pairing_system_role(system_warehouse_code,pairing_role)`。
  - 唯一索引为 `uk_upstream_wh_pairing_upstream_role(connection_code,upstream_warehouse_code,pairing_role)`。
- 只读数据检查确认 CA012 当前只有履约配对：
  - `LX-CA012 / CA91244744 / CA012 / FULFILLMENT / ACTIVE`

### 处理

- 停止占用 `ruoyi-admin.jar` 的旧 8080 Java 进程。
- 重新执行后端打包：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-admin -am -DskipTests package
```

- 使用 `start-backend-local.ps1 -Restart` 启动新 jar。
- 当前 8080 已由新 jar 监听，根路径 HTTP 200。

### 结论

该问题不是 CA012 真的报价仓重复，而是运行时后端未更新。新 jar + 当前数据库角色索引组合下，同一系统仓库允许分别绑定一次履约仓和一次报价仓。
