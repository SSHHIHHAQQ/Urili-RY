# 客户渠道管理实现记录

日期：2026-06-10

## 实现范围

本次实现管理端“客户渠道管理”菜单，用于维护买家实际可见和可下单选择的物流渠道。

当前口径：

- 客户渠道映射系统渠道，不直接映射物流商。
- 系统渠道继续负责物流商账号、物流商渠道、仓库和发货地址。
- 买家范围归客户渠道管理，不放在系统渠道管理。
- 平台渠道映射确认归属客户渠道管理，但本轮先不实现平台映射 Tab。

## 后端

新增管理端接口：

- `/logistics/admin/customer-channels`

权限点：

- `logistics:customerChannel:list`
- `logistics:customerChannel:query`
- `logistics:customerChannel:add`
- `logistics:customerChannel:edit`
- `logistics:customerChannel:status`
- `logistics:customerChannel:binding`
- `logistics:customerChannel:buyer`

新增后端文件：

- `RuoYi-Vue/logistics/src/main/java/com/ruoyi/logistics/domain/LogisticsCustomerChannel.java`
- `RuoYi-Vue/logistics/src/main/java/com/ruoyi/logistics/domain/LogisticsCustomerChannelSystemMapping.java`
- `RuoYi-Vue/logistics/src/main/java/com/ruoyi/logistics/domain/LogisticsCustomerChannelBuyerScope.java`
- `RuoYi-Vue/logistics/src/main/java/com/ruoyi/logistics/domain/request/LogisticsCustomerChannelRequest.java`
- `RuoYi-Vue/logistics/src/main/java/com/ruoyi/logistics/domain/request/LogisticsCustomerChannelSystemMappingRequest.java`
- `RuoYi-Vue/logistics/src/main/java/com/ruoyi/logistics/domain/request/LogisticsCustomerChannelBuyerScopeRequest.java`
- `RuoYi-Vue/logistics/src/main/java/com/ruoyi/logistics/mapper/LogisticsCustomerChannelMapper.java`
- `RuoYi-Vue/logistics/src/main/java/com/ruoyi/logistics/service/ILogisticsCustomerChannelService.java`
- `RuoYi-Vue/logistics/src/main/java/com/ruoyi/logistics/service/impl/LogisticsCustomerChannelServiceImpl.java`
- `RuoYi-Vue/logistics/src/main/resources/mapper/logistics/LogisticsCustomerChannelMapper.xml`
- `RuoYi-Vue/ruoyi-admin/src/main/java/com/ruoyi/web/controller/logistics/AdminLogisticsCustomerChannelController.java`

关键校验：

- 仓库面单自动保存 `NOT_REQUIRED` / `NOT_FETCH` / `UNSUPPORTED`，不要求用户填写第三方面单字段。
- 第三方面单选择 `REQUIRED` 时，平台面单获取和客户上传面单必须至少开启一个。
- 绑定系统渠道时，只允许绑定启用状态的系统渠道。
- 绑定买家只绑定 `buyer` 主体，不绑定买家子账号。
- 买家范围 `ALL` 不写明细；`INCLUDE` / `EXCLUDE` 必须选择至少一个买家。

## SQL

新增脚本：

- `RuoYi-Vue/sql/20260610_customer_logistics_channel_management.sql`

新增表：

- `logistics_customer_channel`
- `logistics_customer_channel_system_mapping`
- `logistics_customer_channel_buyer_scope`

新增字典：

- `logistics_customer_channel_type`
- `logistics_label_upload_required`
- `logistics_platform_label_fetch`
- `logistics_customer_label_upload_support`
- `logistics_customer_channel_scope_mode`
- `logistics_customer_channel_status`
- `logistics_customer_channel_binding_status`

菜单：

- 菜单 `2042` 仍为“客户渠道管理”，组件为 `Channel/Customer/index`。
- 历史按钮 `2260/2261` 继续作为客户渠道查询/新增按钮位，并升级为 `logistics:customerChannel:query`、`logistics:customerChannel:add`。
- 新增按钮位 `2530-2533` 对应编辑、启停、绑定系统渠道、绑定买家。

说明：数据库 SQL 已在用户确认后执行，执行记录见 `docs/plans/2026-06-10-customer-logistics-channel-management-sql-execution-record.md`。

## 前端

新增：

- `react-ui/src/services/logistics/customerChannel.ts`
- `react-ui/src/pages/Channel/Customer/index.tsx`
- `react-ui/tests/logistics-customer-channel-contract.test.ts`

页面行为：

- 主列表展示客户渠道代码、名称、渠道类型、承运商、签名服务、面单相关配置、状态、绑定系统渠道数量、买家范围和更新时间。
- 新增客户渠道采用两步交互：先保存基础信息，再显示配置区 Tabs。
- 编辑客户渠道直接显示基础信息和配置区 Tabs。
- 配置区第一版包含两个 Tab：
  - 绑定系统渠道
  - 绑定买家
- 绑定买家弹窗支持可用名单和不可用名单，并按买家代码、买家名称、买家简称搜索。

## 契约测试

已新增或更新：

- `RuoYi-Vue/logistics/src/test/java/com/ruoyi/logistics/architecture/LogisticsAdminRouteContractTest.java`
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/SqlExecutionGuardContractTest.java`
- `react-ui/tests/logistics-customer-channel-contract.test.ts`
- `react-ui/tests/three-terminal.manifest.json`

## 验证记录

### TypeScript

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc
```

结果：通过。

### 前端契约测试

```powershell
cd E:\Urili-Ruoyi\react-ui
.\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests\logistics-customer-channel-contract.test.ts --runInBand
.\node_modules\.bin\jest.cmd --config jest.config.ts --runTestsByPath tests\logistics-system-channel-contract.test.ts tests\logistics-customer-channel-contract.test.ts --runInBand
```

结果：

- 客户渠道契约测试：`5` tests passed。
- 系统渠道 + 客户渠道契约测试：`10` tests passed。

### 后端契约测试

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl logistics,ruoyi-admin -am "-Dtest=LogisticsAdminRouteContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：通过，`10` tests passed。

### SQL Guard

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：通过，`81` tests passed。

### Manifest

```powershell
cd E:\Urili-Ruoyi\react-ui
node -e "JSON.parse(require('fs').readFileSync('tests/three-terminal.manifest.json','utf8')); console.log('manifest ok')"
```

结果：通过，`manifest ok`。

### 浏览器 Smoke

页面地址：

- `http://127.0.0.1:8001/overseas-warehouse-service/channel-customer`

已验证：

- 页面标题为“客户渠道管理”。
- 主列表列为：客户渠道代码、客户渠道名称、渠道类型、承运商、签名服务、上传物流面单、平台面单获取、客户上传面单支持、状态、绑定系统渠道数量、买家范围、最后更新人、最后更新时间、操作。
- 页面没有出现“保险”和“平台渠道映射”。
- 新增客户渠道默认渠道类型为仓库面单，弹窗内不显示上传物流面单、平台面单获取、客户上传面单支持，也不提前显示配置 Tabs。
- 切换到第三方面单后显示上传物流面单。
- 选择需要上传物流面单后显示平台面单获取和客户上传面单支持。
- 浏览器错误日志为空。

说明：后续已按确认流程执行数据库 SQL，执行记录见 `docs/plans/2026-06-10-customer-logistics-channel-management-sql-execution-record.md`；浏览器 Smoke 仍未在页面点击保存写入业务数据。

### 静态检查和 CodeGraph

```powershell
cd E:\Urili-Ruoyi
git diff --check
codegraph sync .
```

结果：

- `git diff --check` 通过；仅输出工作区既有 LF/CRLF 提示。
- `codegraph sync .` 通过，结果为 `Already up to date`。
