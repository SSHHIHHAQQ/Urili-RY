# 商品分类与属性配置真实数据清空导入执行记录

日期：2026-06-04

## 1. 执行目标

按已确认方案清空当前商品配置测试数据，并导入一批更接近真实服饰电商平台的基础数据，用于验证管理端以下页面在数据量较多时的表现：

- 商品分类配置
- 商品属性配置 / 属性库
- 商品属性配置 / 类目属性模板

## 2. 用户确认

用户已确认执行：

```text
确认清空并导入真实数据
```

本次操作会影响当前运行库中的商品配置数据，因此执行前先做了备份。

## 3. 数据源与执行环境

- 工作区：`E:\Urili-Ruoyi`
- 后端地址：`http://127.0.0.1:8080`
- 前端地址：`http://127.0.0.1:8001`
- 激活配置：`spring.profiles.active = druid`
- 连接来源：`.env.local` 中的 `RUOYI_DB_*` 与 `RUOYI_REDIS_*`
- 数据库操作类型：通过 JDBC 执行备份、事务内清空、只读计数校验
- 接口操作类型：通过后端管理端接口执行 Excel 导入与类目属性规则写入
- 本次未读取或写入本地 Docker MySQL / Redis
- 未在日志、记录或回复中输出数据库地址、账号、密码、Redis 信息或 token

## 4. 清空前备份

备份目录：

```text
logs/import-seed/product-real-seed-20260604/backup/20260604-214118
```

备份文件包括每张表的 JSON 与可回放 SQL：

- `product_category.json` / `product_category.sql`
- `product_attribute.json` / `product_attribute.sql`
- `product_attribute_option.json` / `product_attribute_option.sql`
- `product_category_attribute.json` / `product_category_attribute.sql`

清空前行数：

| 表 | 清空前行数 |
| --- | ---: |
| `product_category` | 262 |
| `product_attribute` | 221 |
| `product_attribute_option` | 300 |
| `product_category_attribute` | 0 |

## 5. 清空范围

已按依赖顺序清空以下 4 张商品配置表：

1. `product_category_attribute`
2. `product_attribute_option`
3. `product_attribute`
4. `product_category`

未清空若依基础表，例如：

- `sys_menu`
- `sys_dict_type`
- `sys_dict_data`
- `sys_oper_log`
- `sys_user`
- `sys_role`

## 6. 导入数据

导入来源目录：

```text
logs/import-seed/product-real-seed-20260604
```

| 文件 | 用途 | 行数 |
| --- | --- | ---: |
| `product-category-real-20260604.xlsx` | 商品分类 | 220 |
| `product-attribute-real-20260604.xlsx` | 商品属性 | 220 |
| `product-attribute-option-real-20260604.xlsx` | 属性选项 | 350 |
| `product-category-attribute-rules-real-20260604.json` | 类目属性模板规则 | 220 |

导入结果：

| 数据 | 预期 | 实际 | 结果 |
| --- | ---: | ---: | --- |
| 商品分类 | 220 | 220 | 通过 |
| 商品属性 | 220 | 220 | 通过 |
| 属性选项 | 350 | 350 | 通过 |
| 类目属性规则 | 220 | 220 | 通过 |

接口导入摘要保存于：

```text
logs/import-seed/product-real-seed-20260604/real-import-api-summary.json
```

## 7. 接口抽查

通过管理端接口登录后抽查：

| 抽查项 | 结果 |
| --- | --- |
| `women_clothing_tops_tshirt` 类目 | `categoryLevel=3`，`childrenCount=0`，`publishEnabled=Y` |
| `gender` 属性 | `attributeType=SINGLE_SELECT`，`optionSource=ATTRIBUTE_OPTION`，选项数 5 |
| `gender` 前 5 个选项 | `FEMALE`、`MALE`、`UNISEX`、`GIRLS`、`BOYS` |
| T 恤类目直接规则 | 1 条 |
| T 恤类目继承预览 | 8 条 |
| T 恤类目前 8 个属性 | `gender`、`season`、`style`、`main_material`、`color_family`、`fit`、`adult_size`、`clothing_length` |

## 8. 数据库只读校验

计数校验输出：

```json
{
  "product_category": 220,
  "product_attribute": 220,
  "product_attribute_option": 350,
  "product_category_attribute": 220,
  "leaf_publish_mismatch": 0,
  "nonleaf_publish_mismatch": 0,
  "boolean_option_rows": 0
}
```

说明：

- `leaf_publish_mismatch = 0`：末级类目的发布判断没有异常。
- `nonleaf_publish_mismatch = 0`：非末级类目的发布判断没有异常。
- `boolean_option_rows = 0`：布尔属性没有导入自定义选项，后续发布端应固定展示 `是 / 否`。

数据库计数文件：

```text
logs/import-seed/product-real-seed-20260604/real-import-db-count-check.json
```

## 9. 验证命令

已执行的主要命令类型：

```powershell
# 确认后端可访问
Invoke-WebRequest -UseBasicParsing -Uri http://127.0.0.1:8080/captchaImage -TimeoutSec 5

# 备份并清空商品配置表
javac -cp <mysql-connector> -encoding UTF-8 logs\import-seed\product-real-seed-20260604\ProductConfigBackupClear.java
java -cp <seed-dir>;<mysql-connector> ProductConfigBackupClear E:/Urili-Ruoyi

# 导入分类、属性、选项、类目属性规则
node logs\import-seed\product-real-seed-20260604\import-product-real-seed.mjs E:/Urili-Ruoyi

# 数据库只读计数校验
javac -cp <mysql-connector> -encoding UTF-8 logs\import-seed\product-real-seed-20260604\ProductConfigCountCheck.java
java -cp <seed-dir>;<mysql-connector> ProductConfigCountCheck E:/Urili-Ruoyi
```

## 10. 权限检查结果

- 使用管理端 `admin / admin123` 登录。
- 商品分类导入、商品属性导入、属性选项导入、类目属性规则保存接口均返回成功。
- 本次未额外创建无权限账号做反向验证；后续如果要验证权限隔离，需要单独准备缺少 `product:*` 权限的角色。

## 11. 字典与选项复用检查

- 本次属性类型不包含 `FILE`。
- `TEXT`、`NUMBER`、`BOOLEAN`、`DATE` 属性不导入属性选项。
- `BOOLEAN` 不维护自定义选项，发布端应固定展示 `是 / 否`，保存建议为 `Y / N`。
- `SINGLE_SELECT`、`MULTI_SELECT` 属性使用 `ATTRIBUTE_OPTION` 来源时才导入自定义选项。
- 本次真实数据没有依赖新的 `sys_dict` 字典数据。

## 12. 复用台账与重复代码检查

- 本次主要是一次性真实测试数据导入，未新增业务公共组件或公共后端服务。
- 没有新增可复用前端组件、后端 Service 抽象或 Mapper 片段，因此未更新 `docs/architecture/reuse-ledger.md`。
- 导入脚本集中放在 `logs/import-seed/product-real-seed-20260604/`，没有散落到业务模块。

## 13. 大文件合理性判断

- `build-product-real-seed.mjs` 用于生成 220 条分类、220 条属性、350 条属性选项和 220 条类目属性规则，文件较长但职责单一，属于一次性测试数据生成脚本。
- `product-category-attribute-rules-real-20260604.json` 较大是因为承载批量规则数据，不属于业务代码文件。
- 本次没有把大批量测试数据写入前端页面或后端业务类。

## 14. CodeGraph 更新结果

已在仓库根目录执行：

```powershell
codegraph sync .
```

结果：

- 同步成功。
- CodeGraph 输出：`Synced 4 changed files`。
- 变更统计：`Added: 3, Modified: 1`。

## 15. 残留问题与下一步

- 当前导入已完成，数量和关键规则校验通过。
- 建议下一步在页面上重点检查：
  - 分类树展开时无子级类目不再显示误导性 `+`。
  - 分类编辑弹窗能正确回填真实数据。
  - 属性库分页、筛选、编辑、选项维护在 220 条属性下是否稳定。
  - 类目属性模板能看懂“本类目直接规则”和“继承预览”的区别。
