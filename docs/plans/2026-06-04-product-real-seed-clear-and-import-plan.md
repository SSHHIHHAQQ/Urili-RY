# 商品分类与属性配置真实测试数据清空导入方案

日期：2026-06-04

## 1. 目标

清空当前商品配置数据，并导入一批更接近真实电商平台的数据，用于验证：

- `商品分类配置` 在约 200 条真实类目下的树形展示、编辑、删除、叶子类目判断。
- `商品属性配置 / 属性库` 在约 200 条真实属性下的查询、分页、编辑、选项维护。
- `商品属性配置 / 类目属性模板` 在约 200 条类目属性规则下的本类目规则和继承预览。

## 2. 当前数据源确认

- 后端配置文件：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`
- 服务端口：`8080`
- 激活配置：`spring.profiles.active = druid`
- MySQL：通过 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD` 运行变量注入。
- Redis：通过 `RUOYI_REDIS_*` 运行变量注入。
- 本方案不读取本地 Docker MySQL / Redis。

## 3. 本次会清空的表

按依赖顺序清空以下 4 张商品配置表：

1. `product_category_attribute`
2. `product_attribute_option`
3. `product_attribute`
4. `product_category`

不清空 `sys_menu`、`sys_dict_type`、`sys_dict_data`、`sys_oper_log` 等若依基础表。

## 4. 清空前备份

执行清空前必须先备份当前 4 张表：

- `product_category`
- `product_attribute`
- `product_attribute_option`
- `product_category_attribute`

备份计划输出到：

```text
logs/import-seed/product-real-seed-20260604/backup/
```

备份内容：

- 每张表当前行数汇总。
- 每张表当前数据 JSON。
- 可回放的 SQL insert 备份文件。

如果清空或导入失败，按备份 SQL 恢复。

## 5. 已生成真实测试数据

输出目录：

```text
logs/import-seed/product-real-seed-20260604/
```

| 文件 | 行数 | 说明 |
| --- | ---: | --- |
| `product-category-real-20260604.xlsx` | 220 | 10 个一级类目、50 个二级类目、160 个叶子类目 |
| `product-attribute-real-20260604.xlsx` | 220 | 文本、数字、布尔、单选、多选属性 |
| `product-attribute-option-real-20260604.xlsx` | 350 | 单选/多选属性的真实选项 |
| `product-category-attribute-rules-real-20260604.json` | 220 | 类目属性模板规则，执行时通过接口写入 |

### 5.1 分类数据范围

一级类目包括：

- 女装
- 男装
- 童装
- 内衣家居服
- 运动服饰
- 鞋靴
- 箱包
- 配饰
- 家纺
- 孕婴服饰

示例类目链路：

- 女装 / 上装 / T恤
- 女装 / 上装 / 衬衫
- 男装 / 上装 / Polo衫
- 运动服饰 / 跑步 / 跑步T恤
- 鞋靴 / 运动鞋 / 跑步鞋
- 家纺 / 床品 / 四件套

### 5.2 属性数据范围

属性共 220 条：

| 类型 | 行数 | 示例 |
| --- | ---: | --- |
| 文本 `TEXT` | 50 | 品牌系列、设计主题、商品货号、面料描述 |
| 数字 `NUMBER` | 60 | 衣长、胸围、肩宽、商品重量、鞋跟高 |
| 布尔 `BOOLEAN` | 40 | 是否可水洗、是否可机洗、是否防晒、是否有内衬 |
| 单选 `SINGLE_SELECT` | 50 | 适用性别、适用季节、版型、主材质、尺码 |
| 多选 `MULTI_SELECT` | 20 | 适用场景、功能特性、洗护方式、图案元素 |

布尔属性不维护自定义选项，后续发布端固定展示 `是 / 否`，保存建议为 `Y / N`。

### 5.3 类目属性模板规则

计划导入约 220 条规则：

- 一级类目挂通用属性：适用性别、适用季节、风格、主材质、颜色分类。
- 二级类目挂分组属性：版型、厚薄、成人尺码、洗护方式、适用场景等。
- 部分叶子类目挂细分属性：衣长类型、图案、面料特性、销售单位、包装类型等。

这样 `类目属性模板` 页面不会是空的，可以看到父级继承和叶子类目的最终字段。

## 6. 执行顺序

确认后按以下顺序执行：

1. 读取当前激活数据源配置，不输出密钥。
2. 登录后端，获取临时 token，不输出 token。
3. 备份 4 张商品配置表。
4. 清空 4 张商品配置表。
5. 通过导入预览接口验证分类 Excel。
6. 通过导入接口写入分类。
7. 通过导入预览接口验证属性 Excel。
8. 通过导入接口写入属性。
9. 通过导入预览接口验证属性选项 Excel。
10. 通过导入接口写入属性选项。
11. 读取分类和属性列表，按 JSON 规则解析 code 到 id。
12. 调用 `/product/admin/category-attributes` 写入类目属性模板规则。
13. 验证最终行数和页面接口：
    - 分类 220
    - 属性 220
    - 属性选项 350
    - 类目属性规则 220

## 7. 验证记录

已完成的非写库验证：

| 验证项 | 结果 |
| --- | --- |
| Excel 文件生成 | 通过 |
| manifest 行数检查 | 分类 220、属性 220、属性选项 350、类目属性规则 220 |
| Excel 预览图 | 已生成 `category-preview.png`、`attribute-preview.png`、`attributeOption-preview.png` |
| 公式错误扫描 | 未发现 `#REF!`、`#DIV/0!`、`#VALUE!`、`#NAME?`、`#N/A` |

## 8. 风险与回滚

风险：

- 本次会清空当前商品配置数据，包括之前导入的压测数据和手工配置数据。
- 如果后续已经有人基于当前分类或属性做了手工调整，也会被清空。

回滚：

- 使用清空前备份 SQL 回放 4 张表。
- 回放顺序为：`product_category`、`product_attribute`、`product_attribute_option`、`product_category_attribute`。

## 9. 待确认

请确认是否执行：

```text
清空当前商品分类配置和商品属性配置，并导入本方案的真实测试数据。
```

确认后我再执行备份、清空、导入和接口验证。
