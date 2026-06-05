# 商品属性导入模板导出联动执行记录

## 目标

- 商品属性导入模板导出时体现“属性类型”和“选项来源”的关联规则。
- 模板文案统一使用“小数位数”，不再出现“数值精度”。
- 表格对新人员更友好，能通过示例和规则说明理解每种属性类型怎么填。
- 支持 200 行以上测试导入时仍有可用下拉，不只覆盖默认前 100 行。

## 实现内容

- `商品属性导入` sheet：
  - `选项来源` 列改为按 `属性类型` 列动态联动。
  - `TEXT` / `NUMBER` / `BOOLEAN` / `DATE` 只能选择 `NONE`。
  - `SINGLE_SELECT` / `MULTI_SELECT` 只能选择 `ATTRIBUTE_OPTION` 或 `SYS_DICT`。
  - `属性类型` 与 `状态` 下拉扩展到 1000 行，避免超过 100 行后没有下拉。
  - `数值精度` 列名改为 `小数位数`。
- `填写示例` sheet：
  - 示例增加到覆盖 `TEXT`、`NUMBER`、`BOOLEAN`、`DATE`、`SINGLE_SELECT`、`MULTI_SELECT`。
  - 同时覆盖 `ATTRIBUTE_OPTION` 和 `SYS_DICT` 两种选择型来源。
- `字段说明` sheet：
  - 更新选项来源规则，明确与属性类型联动。
  - 更新小数位数字段说明。
- 新增 `类型来源规则` sheet：
  - 逐行说明每种属性类型允许的选项来源。
  - 说明字典类型、单位、小数位数、属性选项导入方式的填写规则。
- 新增隐藏 sheet `_attribute_option_source`：
  - 存放 Excel 依赖下拉需要引用的选项来源列表。

## 数据库影响

- 未新增表。
- 未新增字段。
- 未执行 DDL。
- 未写入业务数据。

## 验证记录

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am -DskipTests compile`：通过。
- 首次 `mvn -pl ruoyi-admin -am -DskipTests package` 因运行中的后端锁住 `ruoyi-admin.jar` 失败。
- 停止 8080 Java 进程后重新执行 `mvn -pl ruoyi-admin -am -DskipTests package`：通过。
- `cd E:\Urili-Ruoyi; .\start-backend-local.ps1 -Restart`：后端已重启。
- `Invoke-WebRequest http://127.0.0.1:8080`：返回 HTTP 200。
- 最新错误日志 `logs/ruoyi-admin-8080-20260605-180921.err.log`：长度 0。
- 调用真实接口 `POST /product/admin/attributes/importTemplate` 下载 xlsx：成功。
- 解包检查真实 xlsx：
  - 存在 `类型来源规则` sheet。
  - 存在隐藏 sheet `_attribute_option_source`。
  - `商品属性导入` sheet 存在 `小数位数`，不存在 `数值精度`。
  - `类型来源规则` sheet 包含 `TEXT` 和 `ATTRIBUTE_OPTION / SYS_DICT` 规则说明。
  - `选项来源` 联动公式包含 `INDIRECT("TYPE_"&$C$2)` 和 `INDIRECT("TYPE_"&$C$1001)`。
  - 数据校验数量为 1009。
  - `属性类型` 下拉扩展范围包含 `C102:C1001`。
  - `状态` 下拉扩展范围包含 `H102:H1001`。

## 复用与边界检查

- 继续复用现有 `ExcelUtil` / `ExcelSheet` 生成表头、示例和说明 sheet。
- 未修改通用 `ExcelUtil`，避免影响其他模块模板导出。
- 依赖下拉只在商品属性模板服务内补充，规则局限在 `product` 模块。
- 未新增前端逻辑，本次变化由后端模板导出接口生效。
- 新增 `ProductAttributeTypeSourceRuleRow` 只服务商品属性模板规则 sheet，不作为跨模块公共 DTO。

## 大文件判断

- `ProductImportTemplateService.java` 当前约 360 行，超过 300 行自检线。
- 本文件职责仍集中在“商品配置导入模板生成”，没有混入导入执行、数据库写入或页面逻辑。
- 本次新增的 Excel 依赖下拉逻辑与模板导出强相关，拆成独立公共工具会扩大影响面；当前先不拆。
- 如果后续分类模板、属性选项模板也需要复杂 workbook 后处理，再考虑抽出 product 内部模板 workbook helper。

## CodeGraph

- 已执行 `codegraph sync .`，结果：Already up to date。
