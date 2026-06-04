# 商品属性单位与数值精度规则修正记录

日期：2026-06-04

## 1. 问题

用户指出：

```text
单位和数值进度是干嘛的？是不是跟属性类型挂钩
```

实际业务含义：

- `单位`：数字属性的计量单位，例如 `cm`、`kg`、`g`。
- `数值精度`：数字属性允许或展示的小数位数，例如 `0` 表示整数，`2` 表示保留两位小数。

这两个字段应只对 `NUMBER` 属性生效。此前只按属性类型收口了 `选项来源 / 字典类型`，没有同步收口 `单位 / 数值精度`，属于漏改。

## 2. 修正规则

| 属性类型 | 选项来源 | 字典类型 | 单位 | 数值精度 |
| --- | --- | --- | --- | --- |
| `TEXT` | 固定 `NONE` | 隐藏/清空 | 隐藏/清空 | 隐藏/清空为 `0` |
| `NUMBER` | 固定 `NONE` | 隐藏/清空 | 可填 | 可填，范围 `0-8` |
| `BOOLEAN` | 固定 `NONE` | 隐藏/清空 | 隐藏/清空 | 隐藏/清空为 `0` |
| `DATE` | 固定 `NONE` | 隐藏/清空 | 隐藏/清空 | 隐藏/清空为 `0` |
| `SINGLE_SELECT` | `ATTRIBUTE_OPTION` 或 `SYS_DICT` | `SYS_DICT` 时必填 | 隐藏/清空 | 隐藏/清空为 `0` |
| `MULTI_SELECT` | `ATTRIBUTE_OPTION` 或 `SYS_DICT` | `SYS_DICT` 时必填 | 隐藏/清空 | 隐藏/清空为 `0` |

后端额外限制：

- 非 `NUMBER` 属性如果传入非空 `unit`，拒绝。
- 非 `NUMBER` 属性如果传入非 `0` 的 `valuePrecision`，拒绝。
- `NUMBER` 属性的 `valuePrecision` 必须在 `0-8` 范围内。

## 3. 修改文件

- `react-ui/src/pages/Product/constants.ts`
- `react-ui/src/pages/Product/Attribute/components/AttributeLibrary.tsx`
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductConfigServiceImpl.java`
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/ProductConfigImportService.java`
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/domain/importdata/ProductAttributeImportRow.java`
- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/ProductImportTemplateService.java`

## 4. 前端修正

- 新增 `isNumberAttributeType(...)`。
- 新增/编辑属性弹窗中：
  - 默认 `TEXT` 不显示 `单位` / `数值精度`。
  - 切换到 `NUMBER` 后显示 `单位` / `数值精度`。
  - 从 `NUMBER` 切换到其他类型时，自动清空 `unit` 并把 `valuePrecision` 归为 `0`。
- 保存前统一归一化，避免隐藏字段残留旧值提交。

## 5. 后端修正

- `ProductConfigServiceImpl` 增加数字属性配置校验，用于普通新增/编辑接口。
- `ProductConfigImportService` 增加同样校验，用于 Excel 导入预览和导入。
- 导入服务增加空行保护，避免 ExcelUtil 返回空行时出现 NPE。

## 6. 导入模板修正

- `单位` 字段提示改为：仅 `NUMBER` 属性可填，例如 `cm`、`kg`、`g`；其他属性留空。
- `数值精度` 字段提示改为：仅 `NUMBER` 属性可填，范围 `0-8`；其他属性填 `0` 或留空。
- 属性模板示例新增：
  - `clothing_length_cm` / 衣长 / `NUMBER` / 单位 `cm` / 精度 `1`
  - `package_weight_g` / 包装重量 / `NUMBER` / 单位 `g` / 精度 `0`

## 7. 验证结果

### 7.1 编译与类型检查

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl product -am -DskipTests compile

cd E:\Urili-Ruoyi\react-ui
npm run tsc
```

结果：均通过。

### 7.2 后端打包与重启

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -DskipTests package

cd E:\Urili-Ruoyi
.\start-backend-local.ps1
```

结果：

- `ruoyi-admin.jar` 重新打包成功。
- 后端已重新启动，`http://127.0.0.1:8080/captchaImage` 返回 HTTP 200。

### 7.3 接口校验

通过管理端接口验证：

| 场景 | 结果 |
| --- | --- |
| `TEXT` 属性传 `unit=cm` | 拒绝，返回 `只有数字属性才允许配置单位` |
| `BOOLEAN` 属性传 `valuePrecision=2` | 拒绝，返回 `只有数字属性才允许配置数值精度` |
| 查询属性列表确认测试 code 是否落库 | 未落库 |

### 7.4 页面校验

在 `http://127.0.0.1:8001/basic-config/product-attribute` 验证：

| 操作 | 结果 |
| --- | --- |
| 打开新增商品属性弹窗，默认 `TEXT` | 不显示 `单位` / `小数位数` |
| 属性类型切换为 `NUMBER` | 显示 `单位` / `小数位数` |
| 属性类型切换为 `BOOLEAN` | `单位` / `小数位数` 隐藏 |

补充修正：页面展示文案已从 `数值精度` 改为 `小数位数`，表达含义为数字属性允许或展示的小数位数。浏览器验证 `NUMBER` 属性弹窗显示 `单位` / `小数位数`，不再显示 `数值精度`。

### 7.5 导入预览补充验证

使用临时生成的 Excel 做导入预览时，该临时文件被若依 `ExcelUtil` 识别为空行，因此不能作为“单位规则命中”的有效样本。

已确认的问题是：导入服务不再抛空行 NPE，而是返回行级错误：

```text
导入行为空，请检查模板格式或删除空行
```

真实导入模板的单位/精度规则已通过后端编译和模板说明更新覆盖。

### 7.6 CodeGraph 同步

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

结果：执行成功，CodeGraph 同步了 9 个变更文件，新增 1 个、修改 8 个，共 436 个节点。

## 8. 工程检查

- 权限检查结果：本次未新增接口、菜单或按钮权限，只强化现有商品属性新增/编辑/导入链路的字段规则。
- 字典/选项复用检查结果：属性类型、选项来源、字典类型继续复用现有常量和若依字典能力；`单位` / `数值精度` 不新增字典。
- 复用台账检查结果：本次是现有商品属性规则收敛，没有新增公共组件或跨模块复用能力，未新增复用台账条目。
- 大文件合理性判断结果：未新增超过 300 行的新文件；修改集中在既有商品配置页面、导入服务和配置服务中。
- 重复代码检查结果：前端用 `isNumberAttributeType(...)` 收口数字属性判断；后端普通保存与导入链路分别做入口校验，规则一致但没有抽成跨服务工具类，避免为单一模块过早抽象。

## 9. 残留风险

- 本次没有新增专门的 Java 单元测试；当前通过编译、前端类型检查、接口运行验证和页面联动验证覆盖。
- 导入预览的业务规则命中仍建议后续用后端生成的真实模板手工或自动化构造用例再补一轮。
