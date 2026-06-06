# 来源商品库读模型实现记录

## 目标

彻底解决来源商品库列表依赖实时大聚合 SQL 的性能问题，避免后续来源 SKU 明细达到 50000 行后列表加载被 `group by`、`group_concat`、表达式 key 和排序拖垮。

## 根因

旧逻辑在 `/integration/admin/source-products/list` 请求时直接聚合：

- `upstream_system_sku_candidate`
- `upstream_system_connection`
- `upstream_system_sku_pairing`

每次列表请求都现场计算来源 SKU 组、尺寸组、仓库列表、仓库数量、配对状态、total 和排序。数据量增长后，请求路径必然变慢。

## 实现内容

### 1. 新增读模型表

SQL 文件：

```text
RuoYi-Vue/sql/20260607_source_product_read_model.sql
```

新增三张读模型表：

- `source_product_group`
- `source_product_dimension_group`
- `source_product_warehouse_detail`

其中：

- `source_product_group` 承载来源 SKU 组。
- `source_product_dimension_group` 承载来源商品库列表行。
- `source_product_warehouse_detail` 承载来源 SKU 组对应的仓库明细。

### 2. 列表和详情接口切换读模型

涉及：

```text
RuoYi-Vue/integration/src/main/resources/mapper/integration/UpstreamSystemMapper.xml
RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/controller/AdminSourceProductController.java
RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/UpstreamSystemServiceImpl.java
```

接口保持不变：

```text
GET /integration/admin/source-products/list
GET /integration/admin/source-products/group-detail
```

内部改为：

- 列表 count 读 `source_product_dimension_group`。
- 列表 rows 读 `source_product_dimension_group`。
- 详情 group 读 `source_product_group`。
- 详情 dimensionGroups 读 `source_product_dimension_group`。
- 详情 warehouses 读 `source_product_warehouse_detail`。

### 3. 新增读模型构建服务

新增：

```text
RuoYi-Vue/integration/src/main/java/com/ruoyi/integration/service/impl/SourceProductReadModelService.java
```

能力：

- `rebuildOfficialMaster()`
- `rebuildOfficialMasterByConnection(connectionCode)`

事务边界：

- 删除旧读模型行和插入新读模型行在同一个事务内完成。
- 外部 API 同步过程不被包进这个事务。

### 4. 接入维护触发点

已接入：

- SKU 基础资料同步成功后：全量重建官方主仓读模型。
- WMS 尺寸同步成功后：按连接重建受影响来源 SKU 组。
- 选中 SKU WMS 尺寸同步成功后：按连接重建。
- SKU 配对新增后：按连接重建。
- SKU 配对删除后：按连接重建。
- 来源连接仓库名等基础信息变更后：按连接重建。

## 数据库执行

目标环境：

```text
当前后端 .env.local 激活的远端 fenxiao 数据库
```

连接来源：

```text
.env.local 中 RUOYI_DB_URL / RUOYI_DB_USERNAME / RUOYI_DB_PASSWORD
```

执行方式：

```text
本机 JShell + Maven 本地 mysql-connector-j JDBC 驱动
```

执行 SQL：

```text
RuoYi-Vue/sql/20260607_source_product_read_model.sql
```

执行结果：

```text
SQL_EXECUTED_STATEMENTS=10
elapsedMs=2089
```

影响说明：

- 执行了 3 张读模型表的 `create table if not exists`。
- 清空并回填 `OFFICIAL_MASTER` 范围的读模型数据。
- 未修改 `upstream_system_sku_candidate`、`upstream_system_sku_pairing`、`upstream_system_connection` 源表数据。
- 未执行本地 Docker 数据库。

## 数据回填核对

只读查询结果：

```text
source_product_group: 5507
source_product_dimension_group: 5649
source_product_warehouse_detail: 10736
```

目标 SKU：

```text
master_sku = KATGJ-SS-B885
warehouse_count = 2
source_row_count = 2
source_connection_codes = LX-CA012,LX-NY013-3275A1E1
source_warehouse_names = CA012 / NY013
dimension_count = 1
warehouse_detail = CA012:LX-CA012, NY013:LX-NY013-3275A1E1
```

## 接口验证

后端已重新打包并重启。

验证命令类型：

```text
mvn -pl integration -am -DskipTests compile
mvn -pl ruoyi-admin -am -DskipTests package
.\start-backend-local.ps1 -Restart
POST /login
GET /integration/admin/source-products/list
GET /integration/admin/source-products/group-detail
```

结果：

```text
GET /integration/admin/source-products/list?pageNum=1&pageSize=20
code = 200
total = 5649
rowCount = 20
elapsed = 105ms
final restarted jar cold-call elapsed = 359ms

GET /integration/admin/source-products/list?masterSku=KATGJ-SS-B885
code = 200
total = 1
rowCount = 1
warehouseCount = 2
sourceRowCount = 2
elapsed = 72ms
final restarted jar elapsed = 73ms

GET /integration/admin/source-products/group-detail?sourceSkuGroupKey=...
dimensionGroups = 1
warehouses = 2
elapsed = 76ms
final restarted jar elapsed = 73ms
```

对比上一版急救优化后的同口径列表约 553ms，本次读模型接口进一步降到 105ms；相比最初动态聚合和 PageHelper 自动 count 阶段约 27848ms，已移除请求路径上的实时大聚合。

## 权限检查

现有接口仍复用：

```text
product:list:list
```

本次未新增手动重建按钮、导出、快捷创建商品动作，因此未新增菜单权限和按钮权限。

## 字典/选项复用检查

- `repositoryScope` 继续使用前端已有 Tab 选项。
- `systemKind` 继续通过后端 `systemKindLabel` 和前端 integration 常量展示。
- `pairingStatus`、`status` 继续沿用现有同步/配对状态语义。

## 复用台账检查

已更新：

```text
docs/architecture/reuse-ledger.md
```

补充了读模型表、`SourceProductReadModelService`，以及商品列表和快捷创建商品后续必须复用 `sourceSkuGroupKey` 与读模型的规则。

## 大文件合理性判断

- `UpstreamSystemMapper.xml` 是现有 integration 聚合 Mapper，继续承载 SQL 映射；本次新增读模型 SQL 较多，但集中在来源商品库同一业务段。
- 新增 `SourceProductReadModelService.java` 职责单一，只负责读模型重建。
- 前端未改动。

## CodeGraph 更新

已执行：

```text
codegraph sync .
```

结果：

```text
Already up to date
```

## 重复代码检查

读模型 key 规则集中在 Mapper SQL 片段：

- `SourceProductSkuGroupKeyExpression`
- `SourceProductDimensionGroupKeyExpression`

后续商品列表不得重新实现来源 SKU 聚合规则，应直接消费读模型。

## 残留问题

1. 当前没有管理端手动重建读模型按钮；如后续需要，应新增独立权限 `product:source:rebuild` 和审计日志。
2. `source_product_dimension_group.search_text` 仍是 `like` 搜索；50000 行量级读模型表可接受，若后续上百万行再考虑全文索引或专用搜索字段。
3. 第三方主仓仍未实现，本次只回填 `OFFICIAL_MASTER`。

## 回滚方式

读模型不是唯一事实源。必要时可以：

1. 接口临时切回旧动态聚合 Mapper 逻辑。
2. 保留或清空 `source_product_*` 三张读模型表。
3. 不影响 `upstream_system_sku_candidate` 等上游快照源表。
