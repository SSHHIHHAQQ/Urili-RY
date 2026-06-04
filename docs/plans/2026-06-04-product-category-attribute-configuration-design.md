# 商品分类与商品属性配置设计方案

日期：2026-06-04

## 1. 目标

设计管理端“商品分类”和“商品属性”配置能力，用于控制卖家发布商品时需要填写哪些资料字段。

本方案只做设计确认：不执行数据库变更，不新增菜单 SQL，不实现后端接口，不实现前端页面。

## 2. 已确认口径

- 商品只能选择一个主分类。
- 商品分类不固定三级，支持多级树。
- 卖家发布商品时只能选择最末级类目。
- 商品属性允许从上级分类继承到下级分类。
- 子分类可以为继承属性调整“本类目规则”。
- 本期不做 SKU 规格属性、多 SKU、库存、价格、条码。

## 3. 范围边界

本期做：分类树、属性库、属性选项、分类属性模板、本类目规则调整、发布表单 schema 预览。

本期不做：商品发布完整流程、商品审核、买家筛选搜索、SKU 规格组合、SKU 库存价格条码、外部平台属性同步。

原因：当前讨论的是“卖家上传商品时需要填哪些资料字段”，不是 SKU、库存或价格系统。SKU 后续应单独设计“规格模板 / SKU 规则”，不要混进当前属性配置。

## 4. 菜单建议

当前菜单建议：

```text
商品管理
  商品分类
  商品属性
```

如果后续采用新版菜单分组，可以迁移为：

```text
商品运营
  商品配置
    商品分类
    商品属性
```

“商品属性”页面建议两个 Tab：`属性库`、`类目属性模板`。运营人员能区分“维护字段本身”和“给某个类目配置发布表单”。

## 5. 核心模型

采用四个核心对象：

```text
商品分类树
属性库
属性选项
分类属性规则
```

卖家发布表单不是写死页面，而是根据最末级类目动态计算最终属性清单。

## 6. 商品分类设计

分类支持任意层级，例如：

```text
服装
  上衣
    T恤
    外套
电器
  小家电
    吹风机
```

规则：

- 每个分类只有一个父分类，根分类 `parent_id = 0`。
- 商品只能挂一个主分类，且只能选择最末级分类。
- 一个分类只要存在未删除子分类，就不是最末级。
- 停用分类不可被卖家选择；停用父分类后，子分类也不应对卖家可选。
- 已被商品使用的分类，后续不要直接新增子分类；必须先迁移既有商品，否则会破坏“商品只能挂最末级类目”的规则。

建议表：`product_category`。

字段建议：`category_id` 主键、`parent_id` 父分类、`ancestors` 祖先链、`category_code` 稳定编码、`category_name` 名称、`category_level` 层级、`sort_order` 排序、`status` 状态、`del_flag` 删除标记、`remark` 备注、`create_by/create_time/update_by/update_time` 审计字段。

约束建议：

- `category_code` 全局唯一。
- 同一父分类下 `category_name` 不重复。
- 不允许循环父子关系。
- 有子分类、已绑定属性规则、已被商品使用时，不允许直接删除。

## 7. 属性库设计

属性库维护字段本身，例如：是否可水洗、是否带电池、材质、产地、认证信息、是否液体、是否易碎。

属性定义全局唯一，重点是稳定 code：

```text
attribute_code = washable
attribute_name = 是否可水洗
attribute_type = SINGLE_SELECT
options = YES / NO
```

属性定义不绑定某个分类。分类只是引用属性，并配置当前类目下的填写规则。

建议表：`product_attribute`。

字段建议：`attribute_id` 主键、`attribute_code` 稳定编码、`attribute_name` 名称、`attribute_type` 类型、`option_source` 选项来源、`dict_type` 若依字典类型、`unit` 单位、`status` 状态、`del_flag` 删除标记、`remark` 备注、`create_by/create_time/update_by/update_time` 审计字段。

属性类型建议：`TEXT`、`NUMBER`、`BOOLEAN`、`SINGLE_SELECT`、`MULTI_SELECT`、`DATE`、`FILE`。

约束建议：

- `attribute_code` 全局唯一，建议英文小写下划线。
- 属性被分类引用后，不允许修改 `attribute_code` 和 `attribute_type`。
- 属性被商品使用后，不允许物理删除，只能停用。

## 8. 属性选项设计

有选项的属性保存 code，展示 label。

示例：

```text
是否可水洗：YES -> 可以，NO -> 不可以
是否带电池：YES -> 带电池，NO -> 不带电池
```

不要把所有商品属性选项都塞进若依 `sys_dict`。

建议规则：

- 国家、地区、币种、是否等平台公共选项可以复用 `sys_dict`。
- 材质、电池类型、洗涤方式、认证类型等商品属性选项优先放商品属性选项表。
- 数据库存 code，前端展示 label。
- label 可以调整，code 被使用后不应修改。

建议表：`product_attribute_option`。

字段建议：`option_id` 主键、`attribute_id` 属性 ID、`option_code` 选项编码、`option_label` 选项名称、`sort_order` 排序、`is_default` 是否默认、`status` 状态、`remark` 备注、`create_by/create_time/update_by/update_time` 审计字段。

约束建议：

- 同一属性下 `option_code` 唯一。
- 选项被商品使用后不允许物理删除，只能停用。
- 停用选项后，已有商品仍可回显，新发布或编辑时不可再选择。

## 9. 分类属性规则设计

分类属性规则说明“这个分类下这个属性怎么展示和校验”。

规则来源：

- 当前分类直接新增属性。
- 上级分类继承属性。
- 当前分类对继承属性做本类目调整。
- 当前分类停用继承属性。

可调整内容：是否必填、是否展示、是否允许卖家编辑、是否用于后续筛选、分组、排序、输入提示、帮助文案、长度范围正则等校验规则。

不可调整内容：属性 code、属性类型、属性选项 code 集合。这些内容必须回到属性库统一维护。

建议表：`product_category_attribute`。

字段建议：`category_attribute_id` 主键、`category_id` 分类 ID、`attribute_id` 属性 ID、`rule_mode` 规则模式、`required_flag` 是否必填、`visible_flag` 是否展示、`editable_flag` 卖家是否可编辑、`filterable_flag` 是否用于筛选、`group_code` 分组、`sort_order` 排序、`placeholder` 输入提示、`help_text` 帮助文案、`validation_rule` 校验 JSON、`status` 状态、`remark` 备注、`create_by/create_time/update_by/update_time` 审计字段。

规则模式：

- `ADD`：当前分类直接新增属性。
- `OVERRIDE`：当前分类调整继承属性的本类目规则。
- `DISABLE`：当前分类停用继承属性。

约束建议：同一分类下同一属性只能有一条有效规则。

## 10. 继承属性的运营操作

页面建议：

```text
左侧：商品分类树
右侧：当前分类属性配置
```

右侧分区：

```text
继承上级属性
本类目新增属性
本类目停用属性
```

每条继承属性展示来源和操作：

```text
是否可水洗
来源：服装
当前规则：选填
操作：调整本类目规则 / 在本类目停用 / 恢复继承
```

示例：

```text
服装
  是否可水洗：选填
  材质：必填

服装 > 外套
  继承：是否可水洗、材质
```

运营认为外套必须填写“是否可水洗”：

```text
是否可水洗 -> 调整本类目规则 -> 必填 = 是 -> 保存
```

保存后：

```text
服装
  是否可水洗：选填

服装 > 外套
  是否可水洗：必填，本类目已调整
```

这不会修改父分类，也不会复制一条新的属性。系统只保存 `服装 > 外套` 的本类目规则。

其他操作：

- 不需要继承属性：`是否可水洗 -> 在本类目停用`。
- 不想保留本类目调整：`是否可水洗 -> 恢复继承`。

## 11. 最终属性清单计算

卖家选择最末级分类后，系统按以下步骤生成发布表单：

1. 查询分类祖先链，例如 `服装 -> 上衣 -> T恤`。
2. 从根分类到最末级分类依次读取分类属性规则。
3. 上级 `ADD` 规则进入候选清单。
4. 子级 `ADD` 规则新增属性。
5. 子级 `OVERRIDE` 规则调整必填、排序、分组、提示等。
6. 子级 `DISABLE` 规则停用该属性。
7. 过滤停用分类、停用属性、停用选项。
8. 按分组和排序输出最终表单 schema。

同一个属性最终只出现一次，以最靠近当前分类的规则为准。

## 12. 管理端页面

商品分类页：左侧分类树，右侧分类详情和子分类列表。支持新增根分类、新增子分类、编辑名称编码排序状态备注、停用、删除、查看属性概览。限制为有子分类、已绑定属性规则、已被商品使用时不能直接删除。第一版不建议开放自由移动分类，因为移动会改变属性继承结果。

属性库 Tab：支持新增属性、编辑名称选项状态备注、停用属性、查看属性被哪些分类引用。属性被引用后不允许修改属性类型，属性被商品使用后不允许删除，选项 code 被使用后不允许修改。

类目属性模板 Tab：左侧分类树，右侧当前分类最终属性清单。支持给当前分类新增属性、调整继承属性的本类目规则、在当前分类停用继承属性、恢复继承、调整分组排序、预览卖家发布表单。

类目属性模板列表建议显示：属性名称、属性 code、属性类型、来源分类、规则来源、是否必填、分组、排序、状态。

## 13. 后续商品发布使用方式

后续卖家发布商品时：

1. 卖家选择一个最末级商品分类。
2. 后端根据分类计算最终属性清单。
3. 前端按 schema 渲染表单。
4. 卖家填写属性值。
5. 后端再次按 schema 校验必填、类型、选项合法性。
6. 后端保存商品和属性值。

关键规则：

- 前端展示只是辅助，后端必须做最终校验。
- 卖家端接口不能相信前端传入的 `sellerId`，必须从卖家端 token 身份推导。
- 商品只保存一个主分类 ID。
- 属性值保存 code，不保存 label。

后续属性值表方向，当前不实施：

```text
product
  category_id

product_attribute_value
  product_id
  category_id
  attribute_id
  attribute_code
  value_code
  value_text
  value_number
  value_date
  value_json
```

## 14. 字典与 code 规则

建议使用若依 `sys_dict` 的平台枚举：

```text
product_attribute_type
product_attribute_option_source
product_attribute_rule_mode
product_attribute_group
```

商品属性业务选项不默认进入 `sys_dict`，例如材质、电池类型、洗涤方式、认证类型等，应由商品模块自管。

## 15. 权限点建议

管理端接口挂若依 `sys_menu` 权限，建议：

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

这些是管理端配置权限，不写入 `seller_menu` 或 `buyer_menu`。卖家端未来只读取发布 schema 和提交商品，不维护平台属性配置。

## 16. 审计日志

以下操作需要写操作日志：新增、编辑、停用、删除分类；新增、编辑、停用属性；新增、编辑、停用属性选项；绑定分类属性；调整本类目规则；停用继承属性；恢复继承。

日志至少记录操作人、操作时间、目标分类、目标属性、变更前后关键字段。

## 17. 实施阶段

阶段 1：配置基础能力。新增 `product` Maven module；新增分类、属性、选项、类目属性配置表；新增管理端菜单和权限；实现商品分类管理、属性库、类目属性模板和表单预览。

阶段 2：商品发布接入。设计商品主表和商品属性值表；卖家端发布商品时读取分类属性 schema；后端校验属性值；商品保存一个主分类。

阶段 3：扩展能力。商品审核、买家筛选、商品导入导出、外部平台属性映射、SKU 规格模板。SKU 规格模板必须单独设计。

## 18. 初始化、迁移和回滚

当前工程还没有商品业务表，本方案确认前不要执行 DDL 或菜单 SQL。

确认后：

- 先备份目标数据库。
- 先生成 DDL 和菜单权限 SQL，再单独确认。
- 执行记录必须写明目标环境、连接来源、执行命令类型和影响范围。
- 如果仅完成配置能力且尚无商品发布数据，可以按备份回滚配置表。
- 如果配置已被商品使用，不能直接删除配置表，只能停用菜单和接口，保留历史配置供商品回显。

## 19. 风险

- 分类移动会改变属性继承结果，第一版不建议开放自由拖拽移动。
- 属性类型被引用后修改会导致历史数据不可解释，应禁止。
- 选项 code 被使用后不能修改，只能停用旧 code、新增新 code。
- SKU 字段容易和普通属性混淆，当前方案明确排除 SKU 规格。
- 商品属性选项全部进入 `sys_dict` 会导致字典膨胀，建议商品模块自管业务选项。

## 20. 参考平台

主流平台共同点是：分类或商品类型决定一套属性 schema，而不是把字段写死在页面里。

- [Shopify Standard Product Taxonomy](https://shopify.github.io/product-taxonomy/)
- [Shopify Category Metafields](https://help.shopify.com/en/manual/custom-data/metafields/category-metafields)
- [WooCommerce Product Categories and Attributes](https://woocommerce.com/document/managing-product-taxonomies/)
- [Amazon SP-API Product Listings Guide](https://developer-docs.amazon.com/sp-api/lang-es_ES/docs/manage-product-listings-guide)
- [Google Merchant Product Data Specification](https://support.google.com/merchants/answer/7052112/product-data-specification)

## 21. 推荐结论

建议第一版确认以下落地口径：

```text
多级商品分类树
商品只能挂一个最末级主分类
属性库全局维护
类目属性规则绑定到任意层级分类
子分类继承上级属性
子分类可调整本类目规则
卖家发布表单由最末级分类动态生成
本期不做 SKU 规格和多 SKU
```

这个方案比固定三级分类更灵活，比每个分类复制属性更可维护，也为后续商品发布、审核、买家筛选和外部平台映射保留扩展空间。
