# 商城商品创建更新时间修复记录

## 目标

修复管理端「商品管理 / 商城商品列表」中新建商品缺少更新时间的问题。创建商品本身属于一次业务变更，因此新建 SPU 和新建 SKU 时应同时写入 `create_time` 与 `update_time`，并让 `update_by` 与 `create_by` 保持一致。

## 影响范围

- 后端模块：`RuoYi-Vue/product`
- 涉及表：
  - `product_spu`
  - `product_sku`
- 涉及字段：
  - `update_by`
  - `update_time`
- 不涉及：
  - 不新增表
  - 不修改表结构
  - 不直接执行 SQL DML
  - 不回填历史数据

## 实现内容

1. `ProductDistributionServiceImpl.insertProduct(...)`
   - 新建 SPU 时同时设置 `createBy` 和 `updateBy`。

2. `ProductDistributionServiceImpl.saveSkus(...)`
   - 新建 SKU 时同时设置 `createBy` 和 `updateBy`。

3. `ProductDistributionMapper.xml`
   - `insertSpu` 新增写入 `update_by`、`update_time`。
   - `insertSku` 新增写入 `update_by`、`update_time`。
   - `create_time` 和 `update_time` 均使用数据库 `sysdate()`，保持同一数据库时间口径。

## 业务口径

- 创建时：
  - `create_by = 当前操作人`
  - `update_by = 当前操作人`
  - `create_time = sysdate()`
  - `update_time = sysdate()`
- 后续编辑或状态变更仍沿用原有更新逻辑，继续刷新 `update_by` 和 `update_time`。

## 验证记录

已执行：

- `mvn -pl product -am -DskipTests compile`
  - 结果：通过。
  - 说明：编译输出包含既有 Java 17 模块路径提示和弃用 API 提示，未出现编译失败。
- `codegraph sync .`
  - 结果：通过。
  - 输出摘要：`Already up to date`。

## 备注

本次只修复后续新建商品的更新时间写入逻辑。当前远端库中已存在且 `update_time` 为空的历史商品，如需补齐，需要按远端数据库 DML 规则单独生成回填执行记录并确认后再执行。
