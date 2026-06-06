# 上游系统同步 staging 差异合并改造计划
生成时间：2026-06-06

## 核心结论

上游系统管理同步改为：

```text
外部接口拉取 -> staging 快照表 -> 数据库按 source_payload_hash 对比 -> 主表差异合并
```

关键原则：

- 全量拉取不等于全量更新主表。
- 主表不再“拉到就 upsert 更新所有字段”。
- 本次快照先完整落 staging。
- 主表只处理三类变化：新增、hash 变化、上游已删除/停用。
- hash 相同的数据不更新主表业务字段，避免弱服务器和 MySQL 做大量无效写入。
- 请求日志继续追加，不参与业务数据差异判断。

## 同步项拆分

| 同步项 | 拉取口径 | 主表合并口径 | 定时策略 |
| --- | --- | --- | --- |
| 仓库 | 全量 | staging 对比后新增、变更、停用 | 每天 23:20 |
| 物流渠道 | 全量 | staging 对比后新增、变更、停用 | 每天 23:30 |
| SKU信息 | 全量分页 | staging 对比后新增、变更、停用 | 每天 23:40 |
| SKU仓库尺寸重量 | 基于本地 ACTIVE SKU 分批查 `skuList` | staging 对比后只更新尺寸重量变化 | 每天 23:59，限速慢跑 |
| 指定SKU仓库尺寸重量 | 指定 SKU 小批量 | 小批量 staging 或临时批次对比后更新 | 人工触发 |
| SKU库存 | 增量窗口 | upsert 库存快照，不做全量停用 | 每 10 分钟 |

说明：

- SKU信息同步不再顺带同步 SKU仓库尺寸重量。
- SKU仓库尺寸重量是重任务，单独低频慢跑。
- SKU库存接口是增量窗口，不适合用“本轮没返回就停用”的全量快照规则。

## 状态语义

主表状态建议统一：

| 状态 | 含义 |
| --- | --- |
| `ACTIVE` | 本地有效，上游本次仍存在 |
| `DISABLED` | 上游全量快照中已不存在，或上游已停用 |

不再把上游删除叫 `MISSING`。`MISSING` 更像临时缺失，业务语义不够清楚。

## 新增 staging 表设计

### 1. 仓库 staging 表

表名：`upstream_system_warehouse_candidate_stage`

| 字段 | 类型 | 注释 |
| --- | --- | --- |
| `stage_id` | bigint | staging记录ID |
| `connection_code` | varchar(64) | 主仓接入编号 |
| `sync_batch_id` | varchar(64) | 本次同步批次号 |
| `warehouse_code` | varchar(100) | 上游仓库代码 |
| `warehouse_name` | varchar(200) | 上游仓库名称 |
| `country_code` | varchar(32) | 国家/地区代码 |
| `source_payload_json` | longtext | 上游原始行JSON |
| `source_payload_hash` | varchar(64) | 上游原始行JSON哈希 |
| `create_time` | datetime | 写入staging时间 |

约束：

- 唯一键：`uk_stage_wh_batch(connection_code, sync_batch_id, warehouse_code)`
- 索引：`idx_stage_wh_batch(connection_code, sync_batch_id)`

主表 `upstream_system_warehouse_candidate` 需要补：

- `source_payload_json`
- `source_payload_hash`

### 2. 物流渠道 staging 表

表名：`upstream_system_logistics_channel_candidate_stage`

| 字段 | 类型 | 注释 |
| --- | --- | --- |
| `stage_id` | bigint | staging记录ID |
| `connection_code` | varchar(64) | 主仓接入编号 |
| `sync_batch_id` | varchar(64) | 本次同步批次号 |
| `warehouse_code` | varchar(100) | 上游仓库代码 |
| `channel_code` | varchar(100) | 上游物流渠道代码 |
| `channel_name` | varchar(200) | 上游物流渠道名称 |
| `source_payload_json` | longtext | 上游原始行JSON |
| `source_payload_hash` | varchar(64) | 上游原始行JSON哈希 |
| `create_time` | datetime | 写入staging时间 |

约束：

- 唯一键：`uk_stage_channel_batch(connection_code, sync_batch_id, warehouse_code, channel_code)`
- 索引：`idx_stage_channel_batch(connection_code, sync_batch_id)`

主表 `upstream_system_logistics_channel_candidate` 需要补：

- `source_payload_json`
- `source_payload_hash`

### 3. SKU信息 staging 表

表名：`upstream_system_sku_candidate_stage`

字段原则：

- 和主表 `upstream_system_sku_candidate` 的业务字段保持一致。
- 必须包含 `connection_code`、`sync_batch_id`、`master_sku`、`source_payload_json`、`source_payload_hash`。
- 不承载配对字段，配对仍在 `upstream_system_sku_pairing`。

约束：

- 唯一键：`uk_stage_sku_batch(connection_code, sync_batch_id, master_sku)`
- 索引：`idx_stage_sku_batch(connection_code, sync_batch_id)`
- 索引：`idx_stage_sku_hash(connection_code, master_sku, source_payload_hash)`

### 4. SKU仓库尺寸重量 staging 表

表名：`upstream_system_sku_dimension_stage`

| 字段 | 类型 | 注释 |
| --- | --- | --- |
| `stage_id` | bigint | staging记录ID |
| `connection_code` | varchar(64) | 主仓接入编号 |
| `sync_batch_id` | varchar(64) | 本次同步批次号 |
| `master_sku` | varchar(128) | 上游masterSku |
| `wms_height` | decimal(18,4) | WMS高(cm) |
| `wms_height_bs` | decimal(18,4) | WMS英制高(in) |
| `wms_length` | decimal(18,4) | WMS长(cm) |
| `wms_length_bs` | decimal(18,4) | WMS英制长(in) |
| `wms_weight` | decimal(18,4) | WMS重量(kg) |
| `wms_weight_bs` | decimal(18,4) | WMS英制重量(lb) |
| `wms_width` | decimal(18,4) | WMS宽(cm) |
| `wms_width_bs` | decimal(18,4) | WMS英制宽(in) |
| `source_payload_json` | longtext | 上游原始行JSON |
| `source_payload_hash` | varchar(64) | 上游原始行JSON哈希 |
| `create_time` | datetime | 写入staging时间 |

约束：

- 唯一键：`uk_stage_sku_dimension_batch(connection_code, sync_batch_id, master_sku)`
- 索引：`idx_stage_sku_dimension_batch(connection_code, sync_batch_id)`

## 差异合并规则

### 新增

staging 有、主表没有：

```text
insert into 主表(...)
select ... from staging
left join 主表 on 自然键
where 主表.自然键 is null
```

结果：

- 插入 `ACTIVE`
- 写入 `first_seen_time`
- 写入 `last_seen_time`
- 写入 `source_payload_hash`
- 写入 `sync_batch_id`

### 变更

staging 有、主表有、hash 不同：

```text
update 主表
join staging on 自然键
set 主表业务字段 = staging字段,
    主表.source_payload_hash = staging.source_payload_hash,
    主表.source_payload_json = staging.source_payload_json,
    主表.status = 'ACTIVE',
    主表.last_seen_time = now(),
    主表.update_time = now()
where 主表.source_payload_hash <> staging.source_payload_hash
```

结果：

- 只更新真正变化的数据。
- `update_time` 只代表业务内容变化。

### 无变化

staging 有、主表有、hash 相同：

```text
update 主表
join staging on 自然键
set 主表.sync_batch_id = staging.sync_batch_id,
    主表.last_seen_time = now(),
    主表.status = 'ACTIVE'
where 主表.source_payload_hash = staging.source_payload_hash
```

结果：

- 不更新业务字段。
- 不更新 `update_time`，避免误导“内容变了”。
- 只更新 `last_seen_time` 和批次，用于本轮存在性判断。

### 上游删除或停用

主表有、staging 本批次没有：

```text
update 主表
left join staging on 自然键 and sync_batch_id = 本批次
set 主表.status = 'DISABLED',
    主表.update_time = now()
where staging.自然键 is null
  and 主表.connection_code = 当前主仓
  and 主表.status <> 'DISABLED'
```

结果：

- 不物理删除。
- 标记 `DISABLED`。
- 配对关系不自动删除，后续业务判断上游对象状态时阻断使用。

## 统计结果

每个同步批次需要统计：

| 指标 | 含义 |
| --- | --- |
| `pulled_count` | 本次从上游拉到的行数 |
| `inserted_count` | 新增主表行数 |
| `changed_count` | hash变化并更新的行数 |
| `unchanged_count` | hash相同，仅刷新本轮见过的行数 |
| `disabled_count` | 主表有但本次快照没有，被标记停用的行数 |
| `failed_count` | 失败行数 |

`upstream_system_sync_state` 里第一版可先存：

- `total_count = pulled_count`
- `success_count = inserted_count + changed_count`
- `failed_count = failed_count`

更完整的统计字段后续可扩展，或者把详细统计放到同步批次表。

## 是否新增同步批次表

建议第一版新增：

表名：`upstream_system_sync_batch`

目的：

- 保存每次同步的汇总结果。
- 避免把所有统计都塞进状态表。
- 后续页面可以展示最近几次同步记录。

关键字段：

| 字段 | 类型 | 注释 |
| --- | --- | --- |
| `sync_batch_id` | varchar(64) | 同步批次号 |
| `connection_code` | varchar(64) | 主仓接入编号 |
| `sync_type` | varchar(32) | 同步类型 |
| `mode` | varchar(32) | SCHEDULED、MANUAL、SELECTED |
| `status` | varchar(16) | SYNCING、SUCCESS、FAILED、SKIPPED |
| `pulled_count` | int | 拉取行数 |
| `inserted_count` | int | 新增行数 |
| `changed_count` | int | 变更行数 |
| `unchanged_count` | int | 未变化行数 |
| `disabled_count` | int | 停用行数 |
| `failed_count` | int | 失败行数 |
| `started_time` | datetime | 开始时间 |
| `finished_time` | datetime | 结束时间 |
| `error_message` | varchar(500) | 失败原因 |

## 同步流程

### 仓库 / 物流渠道 / SKU信息

```text
1. 创建 sync_batch_id
2. 写 upstream_system_sync_batch = SYNCING
3. 分页/分批请求领星
4. 每行生成 source_payload_json 和 source_payload_hash
5. 批量插入 staging
6. SQL 插入新增
7. SQL 更新 hash 变化
8. SQL 刷新 hash 相同数据的 last_seen_time
9. SQL 标记本批次不存在的数据为 DISABLED
10. 写 sync_state 和 sync_batch 汇总
11. 清理历史 staging
```

### SKU仓库尺寸重量全量

```text
1. 从主表读取 ACTIVE SKU
2. 每批 50 个 SKU 调领星 skuList 查询
3. 每批间隔 2000ms
4. 查询结果写入 sku_dimension_stage
5. 用 hash 对比，只更新 WMS 尺寸重量变化的 SKU
6. 未返回的 SKU 不停用 SKU 主资料，只保留原尺寸状态
7. 写同步状态和批次统计
```

### 指定SKU仓库尺寸重量

```text
1. 用户输入最多 100 个 SKU
2. 去重后生成 sync_batch_id
3. 调领星 skuList 查询
4. 写 sku_dimension_stage
5. 只合并这些 SKU 的 WMS 尺寸重量变化
6. 未返回的 SKU 给出未命中数量，不停用 SKU
```

### SKU库存

库存不同：

- 领星库存接口按时间窗口增量返回。
- 不代表完整快照。
- 不用本轮缺失停用规则。

流程保持：

```text
1. 读取上次成功时间
2. 带时间窗口调用 integratedInventory/pageOpen
3. upsert 库存快照
4. 记录请求日志和库存同步状态
```

## 前端同步入口

顶部“同步”按钮改为 Modal：

复选框：

- 仓库
- 物流渠道
- SKU信息
- SKU仓库尺寸重量
- SKU库存

默认勾选：

- 仓库
- 物流渠道

说明：

- SKU信息、SKU仓库尺寸重量、SKU库存必须用户主动勾选。
- SKU仓库尺寸重量勾选时显示“任务较慢，会限速执行”的提示。

## 请求日志

请求日志继续走 `upstream_system_request_log` 追加。

页面不再只显示 operation code，改成中文类型：

| operation | 显示 |
| --- | --- |
| `AUTH_CHECK` | 授权校验 |
| `WAREHOUSE_SYNC` | 仓库 |
| `LOGISTICS_CHANNEL_SYNC` | 物流渠道 |
| `SKU_SYNC` | SKU信息 |
| `SKU_DIMENSION_FULL_SYNC` | SKU仓库尺寸重量 |
| `SKU_DIMENSION_SELECTED_SYNC` | 指定SKU仓库尺寸重量 |
| `INVENTORY_SYNC` | SKU库存 |

## 定时任务

继续使用若依 `sys_job`：

| 任务 | cron | 说明 |
| --- | --- | --- |
| 领星仓库每日同步 | `0 20 23 * * ?` | 低频 |
| 领星物流渠道每日同步 | `0 30 23 * * ?` | 低频 |
| 领星SKU信息每日同步 | `0 40 23 * * ?` | 不带尺寸重量 |
| 领星SKU仓库尺寸重量每日限速同步 | `0 59 23 * * ?` | 分批慢跑 |
| 领星SKU库存每10分钟同步 | `0 0/10 * * * ?` | 增量库存 |

策略：

- 禁止并发。
- misfire 不立即补跑。
- 同一主仓同一时间只允许一个同步任务执行。
- 库存任务遇到同主仓正在跑尺寸重量全量任务时跳过本轮。

## 实施顺序

1. 暂停旧 `领星SKU每10分钟同步`。
2. 新增 staging 表和同步批次表。
3. 给仓库、物流渠道主表补 `source_payload_json` / `source_payload_hash`。
4. 把仓库、物流渠道、SKU信息改为 staging 差异合并。
5. SKU信息同步去掉尺寸重量补拉。
6. SKU仓库尺寸重量改为单独 staging 差异合并和限速。
7. 指定SKU尺寸重量走小批量 staging 合并。
8. 修 `HttpClient` 复用和同步锁。
9. 改若依 `sys_job`。
10. 改前端同步 Modal、指定 SKU 入口、请求日志展示。
11. 编译、重启、验证 CPU 和线程。

## 验证重点

- hash 相同的数据不更新主表业务字段和 `update_time`。
- hash 不同的数据才更新主表业务字段。
- staging 本批次没有的全量对象标记 `DISABLED`。
- SKU信息同步不会产生 SKU仓库尺寸重量请求日志。
- SKU仓库尺寸重量全量任务请求类型显示为“SKU仓库尺寸重量”。
- 指定SKU任务请求类型显示为“指定SKU仓库尺寸重量”。
- 10分钟库存任务仍在若依定时任务菜单可见。
- Java `HttpClient-*` 线程不再持续增长。
