# 库存自动同步 WMS 库存策略 SQL 执行记录

## 执行背景

库存总览“自动同步WMS库存设置”需要后端策略表、库存行同步字段、库存流水策略快照字段、SPU/SKU 读模型摘要字段、菜单按钮权限和字典数据。本次按用户“执行”授权，将迁移脚本应用到当前运行环境。

## 目标环境

- 执行日期：2026-06-09
- 连接来源：仓库根目录 `.env.local`
- MySQL：远端运行库，连接来源为本机 `.env.local`，地址已脱敏
- Redis：本次未写入 Redis，仅确认后端运行变量来自 `.env.local`
- 执行方式：本机 Python `pymysql` 读取 `.env.local` 后连接 MySQL，未在命令和记录中输出数据库密码

## 用户确认与执行边界

- 用户确认来源：用户在当前任务中下达“执行”授权，并且三端快速推进目标已明确远程数据库 DDL/DML 可以执行。
- 本确认仅适用于 `RuoYi-Vue/sql/20260609_inventory_auto_wms_stock_sync_policy.sql` 在本记录列明范围内的一次执行；不得作为后续无确认重放依据。
- 本次不写 Redis，不改写已有库存数量、订单、履约或财务事实数据。
- 本次包含远端运行库 DDL 和菜单/字典 DML，执行前已预览目标 count/signature，脚本内不匹配会 `45000` fail-closed。

## 执行脚本

- `RuoYi-Vue/sql/20260609_inventory_auto_wms_stock_sync_policy.sql`

执行前预览脚本精确目标：

```text
menu_count=0
menu_signature=e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
dict_type_count=0
dict_type_signature=e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
dict_data_count=0
dict_data_signature=e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
```

执行确认变量：

```sql
set @confirm_inventory_auto_wms_stock_sync_policy = 'APPLY_INVENTORY_AUTO_WMS_STOCK_SYNC_POLICY';
set @inventory_auto_wms_menu_expected_count = '0';
set @inventory_auto_wms_menu_expected_signature = 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855';
set @inventory_auto_wms_dict_type_expected_count = '0';
set @inventory_auto_wms_dict_type_expected_signature = 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855';
set @inventory_auto_wms_dict_data_expected_count = '0';
set @inventory_auto_wms_dict_data_expected_signature = 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855';
```

## 影响范围

- 新增或确认存在 `inventory_stock_sync_policy`。
- 扩展库存行同步策略字段、库存流水策略快照字段、SKU/SPU 读模型同步方式摘要字段。
- 新增按钮权限 `inventory:overview:syncPolicy`。
- 新增字典 `inventory_stock_sync_mode` 和库存流水类型 `AUTO_SOURCE_SYNC`。
- 不直接调整库存数量，不触发 WMS 外部库存动作，不写 Redis 编码池或会话数据。

## 回滚方式

- 默认不自动回滚：页面、接口和后端策略已依赖新增表/字段/字典。
- 如需回滚，必须先停止使用自动同步策略入口并部署不再读写新增字段的代码版本。
- 受控回滚顺序应为：禁用/移除按钮权限和字典项，确认无策略行被业务引用后处理 `inventory_stock_sync_policy`，最后再按字段级方案移除新增列；每一步都需要新的只读预览、确认 token 或精确目标签名。
- 已产生的库存流水或业务事实不得直接删除；如未来策略已被使用，应按业务冲销/作废方案处理。

## 脚本修复

- 首次执行在 `set session group_concat_max_len = greatest(...)` 失败，原因是目标 MySQL 对 session 变量赋值类型要求更严格；已改为显式 `cast(... as unsigned)`。
- 第二次执行在最终完成校验时报 `Can't reopen table: 'seed'`，原因是 MySQL 临时表不能在同一语句中重复打开；已改为先把临时 seed 表数量读取到变量，再做完成校验。
- 两处修复后重新运行 `SqlExecutionGuardContractTest`，结果通过。

## 执行结果

最终迁移执行成功：

```text
statements=89
policy_table=1
menu_242005=1
sync_dict_type=1
sync_mode_data=3
operation_type_data=1
```

落库内容：

- 新增或确认存在 `inventory_stock_sync_policy`
- 扩展库存行同步策略字段
- 扩展库存流水策略快照字段
- 扩展 SKU/SPU 读模型同步方式摘要字段
- 新增按钮权限 `inventory:overview:syncPolicy`
- 新增字典 `inventory_stock_sync_mode`
- 新增库存流水类型 `AUTO_SOURCE_SYNC`

## 后端重启

- 打包命令：`mvn -pl ruoyi-admin -am -DskipTests package`
- 首次打包失败原因：旧 `ruoyi-admin.jar` 被 8080 进程占用
- 处理方式：停止 8080 旧后端进程后重新打包
- 最终打包结果：通过
- 启动方式：`.\start-backend-local.ps1 -Restart`
- 新后端 PID：`37240`
- 启动日志：`logs/ruoyi-admin-8080-20260609-130611.out.log`
- HTTP 检查：`http://127.0.0.1:8080` 返回 200

## 运行态验证

直接接口验证：

```text
GET /inventory/admin/overview/seller/options -> code 200, sellerCount=3
GET /inventory/admin/overview/official-warehouse/options -> code 200, officialCount=2
GET /inventory/admin/overview/warehouse/options -> code 200
```

浏览器验证：

- 页面：`http://127.0.0.1:8001/inventory/overview`
- 自动同步弹窗可打开
- 设置范围显示“卖家维度”
- 卖家下拉显示真实卖家选项
- 仓库设置中仓库字段是多选
- 官方仓选项显示 `美东NY013 / NY013`、`美西-CA012 / CA012`
- 官方仓多选未包含“来源SKU未绑定”或“发货仓库未配置”
- SPU、SKU、明细行设置均为可搜索选择器，不再显示手输 ID 输入框

## 验证命令

```powershell
mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl ruoyi-admin -am -DskipTests package
```

补充验证来自接口调用和浏览器实测。
