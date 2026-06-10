# 商品审核列表性能只读排查记录

## 排查范围

- 接口：`GET /product/admin/reviews/list`
- 目标：确认慢点是否来自连表缺索引，或来自服务层重复查询。
- 本次只做只读排查，未执行 DDL/DML，未修改远端数据。

## 数据源确认

- 连接来源：`E:\Urili-Ruoyi\.env.local`
- MySQL：远端运行库，地址已脱敏
- 账号：使用 `.env.local` 中的 `RUOYI_DB_USERNAME`
- 敏感信息：未输出密码、token 或 Redis 密码。

## 代码链路

- Controller：`AdminProductReviewController.list`
- Service：`ProductReviewServiceImpl.selectReviewList`
- Mapper：`ProductReviewMapper.selectReviewList`

当前服务层逻辑：

1. 分页查询审核单列表。
2. 对每个审核单分别查询 `product_review_item`。
3. 对每个审核单分别查询 `product_review_snapshot`。
4. 根据明细和快照在 Java 内补算供货价区间。

因此页面 20 条审核单时，会产生：

- 1 次审核单主列表查询
- 20 次审核明细查询
- 20 次审核快照查询

## 只读验证结果

当前远端表数据量：

| 表 | 行数 |
| --- | ---: |
| `product_review_request` | 34 |
| `product_review_item` | 122 |
| `product_review_snapshot` | 203 |
| `product_spu_warehouse` | 26 |

当前远端索引已存在：

- `product_review_request`
  - `uk_product_review_no(review_no)`
  - `idx_product_review_type_status(review_type, review_status, submit_time)`
  - `idx_product_review_status_time(review_status, submit_time)`
  - `idx_product_review_spu(spu_id, submit_time)`
  - `idx_product_review_seller(seller_id, submit_time)`
  - `idx_product_review_submit(submit_terminal, submit_time)`
  - `idx_product_review_pending_key(active_pending_key, review_status)`
- `product_review_item`
  - `idx_product_review_item_review(review_id, sort_order)`
  - `idx_product_review_item_spu(spu_id, review_id)`
  - `idx_product_review_item_sku(sku_id, review_id)`
- `product_review_snapshot`
  - `idx_product_review_snapshot_review(review_id, snapshot_role)`
  - `idx_product_review_snapshot_item(item_id, snapshot_role)`
- `product_spu_warehouse`
  - `uk_product_spu_warehouse(spu_id, warehouse_id)`

EXPLAIN 观察：

- `review_status = PENDING` 场景会使用 `idx_product_review_status_time`。
- 全部列表因为 `order by case when r.review_status = 'PENDING' then 0 else 1 end, r.submit_time asc, r.review_id desc`，仍会 `Using filesort`。
- 仓库类型汇总子查询会按 `product_spu_warehouse` 的 `uk_product_spu_warehouse` 扫描并物化。

耗时拆分：

| 环节 | 只读测得耗时 |
| --- | ---: |
| 主列表 SQL，20 行 | 约 15ms |
| 逐条查询 items/snapshots，40 次 SQL | 约 587ms |
| 批量查询 items/snapshots，2 次 SQL | 约 59ms |

进一步拆分：

| 环节 | 多次测得均值 |
| --- | ---: |
| PageHelper count SQL | 约 16.0ms |
| 主列表 SQL | 约 16.4ms |
| 批量查 item | 约 18.2ms |
| 批量查 snapshot | 约 32.0ms |
| 逐条查 item，20 次 SQL | 约 327.4ms |
| 逐条查 snapshot，20 次 SQL | 约 332.3ms |

当前页 20 条审核单对应：

- `product_review_item` 返回 83 行。
- `product_review_snapshot` 返回 149 行。
- snapshot JSON 总大小约 435KB，单条最大约 13KB。
- Python 侧 JSON 解析参考耗时约 3.7ms，仅作体积判断参考；主因不是 JSON 体积本身。

接口计时：

| 请求 | 耗时 |
| --- | ---: |
| `pageSize=20` 全部 | 约 709-760ms |
| `pageSize=20&reviewStatus=PENDING` | 约 721-792ms |
| `pageSize=20&reviewStatus=PENDING&reviewType=NEW_PRODUCT` | 约 229-267ms |
| `pageSize=1&reviewStatus=PENDING` | 约 83-87ms |

## 初步结论

这次主要慢点不是“完全没加索引”，而是服务层 N+1 查询。

索引侧也有优化空间，尤其是全部列表的排序表达式导致 filesort，以及仓库类型汇总 join 每次都要参与列表查询。但当前数据量下，主列表 SQL 本身不是主耗时。

更精确地说，700ms 的主体由以下部分组成：

1. PageHelper count 和主列表查询合计约 30-35ms。
2. 20 条审核单逐条读取 `product_review_item`，约 327ms。
3. 20 条审核单逐条读取 `product_review_snapshot`，约 332ms。
4. Java 对快照做供货价区间补算、JSON 序列化响应、HTTP 框架开销补齐剩余几十毫秒。

因此实际瓶颈是“远端 MySQL 上 40 次按主键/索引小查询的往返成本”，不是单条 SQL 计划很差。

## 推荐修复顺序

1. 优先修复 `ProductReviewServiceImpl.selectReviewList` 的 N+1。
   - 新增批量 mapper：
     - `selectReviewItemsByReviewIds(List<Long> reviewIds)`
     - `selectReviewSnapshotsByReviewIds(List<Long> reviewIds)`
   - Service 当前页审核单只批量查 2 次，再按 `reviewId` 分组回填。
   - 保持详情页 `selectReviewById` 仍使用单审核单查询。

2. 列表页不要再用快照实时补算供货价区间。
   - `product_review_request` 已有 `price_before_min/max`、`price_after_min/max` 字段。
   - 如果提交审核时已经写入这些字段，列表应直接使用审核单主表快照字段。
   - 快照解析应保留在详情页或兜底场景，不应成为列表页常规路径。

3. 前端 Tab 待审数量改成专用聚合接口。
   - 当前页面加载会并发调用多次 `/reviews/list?pageSize=1&reviewStatus=PENDING&reviewType=...` 来拿各类型待审数。
   - 建议后端新增一个只读统计接口，一次返回各审核类型的待审数量，避免用列表接口做计数。

4. SQL 排序与索引后续优化。
   - 如需彻底避免全部列表 filesort，可新增排序辅助字段，例如 `review_status_rank`，或把“待审核优先”拆成默认 Tab 查询。
   - 可考虑索引：
     - `(del_flag, review_status, submit_time, review_id)`
     - `(del_flag, review_type, review_status, submit_time, review_id)`
   - 但这些属于第二阶段，不是当前 700ms 的主因。

## 已实施优化

实施时间：2026-06-09

代码调整：

1. `ProductReviewServiceImpl.selectReviewList` 不再对每条审核单常规读取明细和快照。
2. 列表优先使用 `product_review_request` 主表已保存的 `price_before_min/max`、`price_after_min/max`。
3. 仅当历史审核单缺少供货价区间字段时，才触发兼容回补。
4. 兼容回补从原来的逐条 `selectReviewItems` / `selectReviewSnapshots`，改为当前页审核单批量查询：
   - `selectReviewItemsByReviewIds`
   - `selectReviewSnapshotsByReviewIds`
5. 审核详情 `selectReviewById` 保持原逻辑，仍读取单个审核单的明细和快照，避免影响详情展示和审核生效逻辑。

验证：

1. 已执行 `mvn -pl product -am test`，通过。
2. 已执行 `mvn -pl ruoyi-admin -am -DskipTests package`，通过。
3. 已重启本地后端并对 `GET /product/admin/reviews/list` 做真实接口只读计时。

实测结果：

| 请求 | 优化后耗时 |
| --- | ---: |
| `pageSize=20` 全部 | 首次 293ms，后续约 79-95ms |
| `pageSize=20&reviewStatus=PENDING` | 约 61-86ms |
| `pageSize=20&reviewStatus=PENDING&reviewType=NEW_PRODUCT` | 约 58-87ms |

结论：

- 原 700ms 主因已确认并修复：服务层 N+1 查询已经从普通列表路径移除。
- 当前剩余耗时主要来自分页主查询、HTTP 框架、远端数据库往返和首次 JVM/连接池预热。

## 待审数量聚合接口优化

实施时间：2026-06-09

问题：

- 前端审核页原先为了显示审核类型 Tab 数量，会调用 7 次列表接口：
  - `ALL`
  - `NEW_PRODUCT`
  - `ADD_SKU`
  - `EDIT_PRODUCT_INFO`
  - `EDIT_SKU_INFO`
  - `EDIT_PRICE`
  - `EDIT_MIXED`
- 这些请求只需要 `total`，但仍触发列表接口、分页和响应包装，不适合作为数量统计入口。

已实施：

1. 新增后端只读接口：`GET /product/admin/reviews/pending-counts`。
2. 权限沿用列表权限：`review:productDistribution:list`。
3. SQL 只查询 `product_review_request` 待审核记录，并按 `review_type` 聚合。
4. 返回结构包含 `ALL` 总数和各审核类型数量。
5. 前端 `Product/Review` 页面改为一次请求 `pending-counts`，不再并发调用多次列表接口获取 Tab 数量。

实测结果：

| 请求 | 优化后耗时 |
| --- | ---: |
| `GET /product/admin/reviews/pending-counts` | 首次 152ms，后续约 36-53ms |
| `GET /product/admin/reviews/list?pageNum=1&pageSize=20` | 首次 188ms，后续约 72-91ms |

最终结论：

- 审核页首屏从“1 次列表 + 7 次计数列表”收敛为“1 次列表 + 1 次聚合统计”。
- 后端列表 N+1 和前端多次列表计数两个主要性能浪费点都已处理。
