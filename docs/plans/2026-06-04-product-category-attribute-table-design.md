# 商品分类与属性配置表设计

日期：2026-06-04

## 1. 设计结论

第一阶段建议只设计 4 张配置表：

```text
product_category
product_attribute
product_attribute_option
product_category_attribute
```

这 4 张表只解决“卖家发布商品时，某个最末级类目需要填写哪些资料字段”。不承载商品事实、不承载 SKU、不承载库存、不承载价格。

## 2. 表关系

```text
product_category 1 - n product_category_attribute
product_attribute 1 - n product_attribute_option
product_attribute 1 - n product_category_attribute
```

含义：

- 分类是多级树。
- 属性是全局字段库。
- 属性选项挂在属性下。
- 分类属性规则把属性挂到某个分类，并配置必填、展示、排序、分组、校验等规则。

卖家发布商品时，选择一个最末级分类，系统沿分类祖先链计算最终属性清单。

## 3. `product_category`

业务目的：维护商品分类树，决定商品发布时可以选择哪个最末级类目。

不承载：商品属性值、SKU、库存、价格、订单、履约、财务流水。

字段建议：

| 字段 | 类型建议 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `category_id` | `bigint` | 是 | 自增 | 分类主键 |
| `parent_id` | `bigint` | 是 | `0` | 父分类 ID，根分类为 `0` |
| `ancestors` | `varchar(500)` | 是 | 空字符串 | 祖先链，参考若依部门表 |
| `category_code` | `varchar(64)` | 是 | 无 | 稳定分类编码，全局唯一 |
| `category_name` | `varchar(128)` | 是 | 无 | 分类名称 |
| `category_level` | `int` | 是 | `1` | 分类层级，冗余字段 |
| `publish_enabled` | `char(1)` | 是 | `N` | 是否允许卖家发布选择 |
| `sort_order` | `int` | 是 | `0` | 同父级排序 |
| `schema_version` | `int` | 是 | `1` | 分类属性规则版本，规则变更时递增 |
| `status` | `char(1)` | 是 | `0` | `0` 正常，`1` 停用 |
| `del_flag` | `char(1)` | 是 | `0` | `0` 存在，`2` 删除 |
| `remark` | `varchar(500)` | 否 | 空 | 备注 |
| `create_by` | `varchar(64)` | 否 | 空 | 创建人 |
| `create_time` | `datetime` | 否 | 空 | 创建时间 |
| `update_by` | `varchar(64)` | 否 | 空 | 更新人 |
| `update_time` | `datetime` | 否 | 空 | 更新时间 |

约束建议：

- 主键：`category_id`。
- 唯一：`category_code`。
- 唯一：同一父分类下 `category_name` 不重复。
- 索引：`parent_id`、`ancestors`、`status`。
- 不建议第一版保存 `leaf_flag`；是否最末级由“是否存在未删除子分类”实时判断，避免数据不一致。
- `publish_enabled = Y` 只代表运营允许发布选择；实际可选还必须同时满足最末级、正常状态、父级未停用。

关键业务规则：

- 商品只能挂一个主分类。
- 商品只能选择最末级分类。
- 有子分类、已绑定属性规则、已被商品使用时，不允许直接删除。
- 已被商品使用的最末级分类，不建议再新增子分类；需要先迁移商品。
- 分类移动会改变属性继承，第一版建议不开放自由拖拽移动。

## 4. `product_attribute`

业务目的：维护商品发布字段本身，例如“是否可水洗”“是否带电池”“材质”。

不承载：某个分类下是否必填、排序、分组、提示；这些放到规则表。

字段建议：

| 字段 | 类型建议 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `attribute_id` | `bigint` | 是 | 自增 | 属性主键 |
| `attribute_code` | `varchar(64)` | 是 | 无 | 稳定属性编码，全局唯一 |
| `attribute_name` | `varchar(128)` | 是 | 无 | 属性名称 |
| `attribute_type` | `varchar(32)` | 是 | 无 | 字段类型 |
| `option_source` | `varchar(32)` | 是 | `NONE` | 选项来源 |
| `dict_type` | `varchar(100)` | 否 | 空 | 若依字典类型 |
| `unit` | `varchar(32)` | 否 | 空 | 单位，例如 `cm`、`kg`、`W` |
| `value_precision` | `int` | 否 | 空 | 数值小数位，数值属性使用 |
| `status` | `char(1)` | 是 | `0` | `0` 正常，`1` 停用 |
| `del_flag` | `char(1)` | 是 | `0` | `0` 存在，`2` 删除 |
| `remark` | `varchar(500)` | 否 | 空 | 备注 |
| `create_by` | `varchar(64)` | 否 | 空 | 创建人 |
| `create_time` | `datetime` | 否 | 空 | 创建时间 |
| `update_by` | `varchar(64)` | 否 | 空 | 更新人 |
| `update_time` | `datetime` | 否 | 空 | 更新时间 |

`attribute_type` 建议值：

```text
TEXT
NUMBER
BOOLEAN
SINGLE_SELECT
MULTI_SELECT
DATE
FILE
```

`option_source` 建议值：

```text
NONE
ATTRIBUTE_OPTION
SYS_DICT
```

约束建议：

- 主键：`attribute_id`。
- 唯一：`attribute_code`。
- 索引：`attribute_name`、`attribute_type`、`status`。
- 被分类规则引用后，不允许修改 `attribute_code` 和 `attribute_type`。
- 被商品使用后，不允许物理删除，只能停用。

code/label 规则：

- API 和数据库保存 `attribute_code`。
- 页面展示 `attribute_name`。
- `attribute_code` 使用英文小写下划线，例如 `washable`、`battery_included`。

## 5. `product_attribute_option`

业务目的：维护属性自己的选项，例如“是否可水洗”的 `YES/NO`。

字段建议：

| 字段 | 类型建议 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `option_id` | `bigint` | 是 | 自增 | 选项主键 |
| `attribute_id` | `bigint` | 是 | 无 | 属性 ID |
| `option_code` | `varchar(64)` | 是 | 无 | 选项编码 |
| `option_label` | `varchar(128)` | 是 | 无 | 选项展示名称 |
| `sort_order` | `int` | 是 | `0` | 同属性下排序 |
| `default_flag` | `char(1)` | 是 | `N` | 是否默认选项 |
| `status` | `char(1)` | 是 | `0` | `0` 正常，`1` 停用 |
| `remark` | `varchar(500)` | 否 | 空 | 备注 |
| `create_by` | `varchar(64)` | 否 | 空 | 创建人 |
| `create_time` | `datetime` | 否 | 空 | 创建时间 |
| `update_by` | `varchar(64)` | 否 | 空 | 更新人 |
| `update_time` | `datetime` | 否 | 空 | 更新时间 |

约束建议：

- 主键：`option_id`。
- 唯一：同一 `attribute_id` 下 `option_code` 唯一。
- 索引：`attribute_id`、`status`。
- 选项被商品使用后不允许物理删除，只能停用。
- 停用选项后，已有商品仍可回显，新发布或编辑时不可再选择。

是否进 `sys_dict`：

- `YES/NO`、国家、币种等平台公共枚举可以复用 `sys_dict`。
- 材质、电池类型、洗涤方式、认证类型等商品属性选项优先放本表。
- 不建议把所有商品属性选项塞进 `sys_dict`，否则字典会膨胀，分类维度和选项版本也不好管。

## 6. `product_category_attribute`

业务目的：维护分类与属性的绑定关系，以及当前分类下这个属性的填写规则。

字段建议：

| 字段 | 类型建议 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `category_attribute_id` | `bigint` | 是 | 自增 | 类目属性配置主键 |
| `category_id` | `bigint` | 是 | 无 | 分类 ID |
| `attribute_id` | `bigint` | 是 | 无 | 属性 ID |
| `rule_mode` | `varchar(32)` | 是 | `ADD` | 规则模式 |
| `required_flag` | `char(1)` | 是 | `N` | 是否必填 |
| `visible_flag` | `char(1)` | 是 | `Y` | 是否展示 |
| `editable_flag` | `char(1)` | 是 | `Y` | 卖家是否可编辑 |
| `filterable_flag` | `char(1)` | 是 | `N` | 是否预留买家筛选 |
| `group_code` | `varchar(64)` | 否 | 空 | 表单分组 |
| `sort_order` | `int` | 是 | `0` | 最终表单排序 |
| `placeholder` | `varchar(255)` | 否 | 空 | 输入提示 |
| `help_text` | `varchar(500)` | 否 | 空 | 帮助说明 |
| `validation_rule` | `text` | 否 | 空 | 校验规则 JSON 文本 |
| `status` | `char(1)` | 是 | `0` | `0` 正常，`1` 停用 |
| `remark` | `varchar(500)` | 否 | 空 | 备注 |
| `create_by` | `varchar(64)` | 否 | 空 | 创建人 |
| `create_time` | `datetime` | 否 | 空 | 创建时间 |
| `update_by` | `varchar(64)` | 否 | 空 | 更新人 |
| `update_time` | `datetime` | 否 | 空 | 更新时间 |

`rule_mode` 建议值：

```text
ADD       当前分类直接新增属性
OVERRIDE  当前分类调整继承属性的本类目规则
DISABLE   当前分类停用继承属性
```

约束建议：

- 主键：`category_attribute_id`。
- 唯一：同一 `category_id` + `attribute_id` 只能有一条有效规则。
- 索引：`category_id`、`attribute_id`、`rule_mode`、`status`。
- `DISABLE` 规则只表达当前分类停用该继承属性，必填、排序、提示等字段可忽略。
- 修改任意有效规则后，建议递增对应分类的 `schema_version`。

可调整项：

- 是否必填。
- 是否展示。
- 是否允许卖家编辑。
- 是否用于后续筛选。
- 分组。
- 排序。
- 输入提示。
- 帮助文案。
- 校验规则。

不可调整项：

- 属性 code。
- 属性类型。
- 属性选项 code 集合。

这些必须回到 `product_attribute` 或 `product_attribute_option` 统一维护。

## 7. 校验规则 JSON 建议

`validation_rule` 不建议第一版拆很多细表，先用 JSON 文本承载轻量校验。

示例：

```json
{
  "min": 0,
  "max": 100,
  "minLength": 1,
  "maxLength": 50,
  "pattern": "^[A-Za-z0-9_-]+$"
}
```

说明：

- 后端必须解析并校验。
- 前端可按规则做辅助校验。
- JSON 文本字段比数据库 JSON 类型兼容性更稳，后续确认 MySQL 版本后再决定是否使用 JSON 类型。

## 8. 最终属性清单计算

卖家选择最末级分类后：

1. 查询分类祖先链，例如 `服装 -> 上衣 -> T恤`。
2. 从根分类到最末级分类读取 `product_category_attribute`。
3. 上级 `ADD` 规则进入候选属性清单。
4. 子级 `ADD` 规则新增属性。
5. 子级 `OVERRIDE` 规则覆盖必填、排序、分组、提示等本类目规则。
6. 子级 `DISABLE` 规则停用该属性。
7. 过滤停用分类、停用属性、停用选项。
8. 按分组和排序输出最终表单 schema。

同一个属性最终只出现一次，以最靠近当前分类的规则为准。

## 9. 预留商品发布表

当前不实施商品发布，但后续应至少再设计：

```text
product
product_attribute_value
```

方向建议：

- `product` 保存商品主数据和唯一主分类 `category_id`。
- `product_attribute_value` 保存不同类目下的动态属性值。
- 属性值保存 code，不保存 label。
- 商品保存时可记录 `category_schema_version`，用于追踪当时使用的类目属性规则版本。

`product_attribute_value` 后续字段方向：

| 字段 | 说明 |
| --- | --- |
| `product_id` | 商品 ID |
| `category_id` | 发布时的主分类 ID |
| `category_schema_version` | 发布时的分类规则版本 |
| `attribute_id` | 属性 ID |
| `attribute_code` | 属性 code 快照 |
| `value_code` | 单选值 code |
| `value_text` | 文本值 |
| `value_number` | 数值 |
| `value_date` | 日期 |
| `value_json` | 多选、文件等复杂值 |

这张表不是当前阶段建表范围。

## 10. 建议字典

可以进入若依 `sys_dict` 的是配置枚举：

```text
product_attribute_type
product_attribute_option_source
product_attribute_rule_mode
product_attribute_group
```

商品属性业务选项不默认进入 `sys_dict`，由 `product_attribute_option` 管理。

## 11. 权限和审计影响

建议管理端权限：

```text
product:category:list
product:category:query
product:category:add
product:category:edit
product:category:remove

product:attribute:list
product:attribute:query
product:attribute:add
product:attribute:edit
product:attribute:remove

product:categoryAttribute:list
product:categoryAttribute:edit
product:categoryAttribute:preview
```

这些权限写管理端 `sys_menu`，不写 `seller_menu` 或 `buyer_menu`。

需要写操作日志的动作：

- 分类新增、编辑、停用、删除。
- 属性新增、编辑、停用。
- 属性选项新增、编辑、停用。
- 分类属性绑定。
- 本类目规则调整。
- 继承属性停用。
- 恢复继承。

## 12. 推荐索引汇总

```text
product_category
- pk(category_id)
- uk(category_code)
- uk(parent_id, category_name)
- idx(parent_id)
- idx(ancestors)
- idx(status)

product_attribute
- pk(attribute_id)
- uk(attribute_code)
- idx(attribute_name)
- idx(attribute_type)
- idx(status)

product_attribute_option
- pk(option_id)
- uk(attribute_id, option_code)
- idx(attribute_id)
- idx(status)

product_category_attribute
- pk(category_attribute_id)
- uk(category_id, attribute_id)
- idx(category_id)
- idx(attribute_id)
- idx(rule_mode)
- idx(status)
```

唯一索引和逻辑删除的兼容方式需要在生成 DDL 时再定。第一版建议对配置表少做“删除再重建”，更多使用停用、恢复和修改，避免唯一索引与历史删除记录冲突。

## 13. 初始化和回滚

本设计确认前不要执行 DDL 或菜单 SQL。

确认后再单独输出：

- 建表 DDL。
- 字典初始化 SQL。
- 管理端菜单权限 SQL。
- 执行记录 Markdown。

执行前必须确认当前激活数据源，备份目标数据库，并写清楚目标环境、连接来源、执行命令类型和影响范围。

如果配置还没有被商品使用，可按备份回滚。如果配置已被商品使用，不应直接删除配置表，应停用菜单和接口并保留历史配置供商品回显。

## 14. 推荐结论

第一阶段只建 4 张配置表最稳：

```text
product_category
product_attribute
product_attribute_option
product_category_attribute
```

不要现在建 SKU 表，也不要把动态属性塞进商品主表。等商品发布流程确认后，再设计 `product` 和 `product_attribute_value`。
