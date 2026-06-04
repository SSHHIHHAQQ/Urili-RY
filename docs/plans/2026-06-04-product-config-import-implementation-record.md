# 商品配置批量导入实施记录

日期：2026-06-04

## 1. 实施范围

本次在管理端商品分类配置、商品属性配置中补充第一版批量导入能力。

- 商品分类导入：模板下载、校验预览、确认导入。
- 商品属性导入：模板下载、校验预览、确认导入。
- 商品属性选项导入：模板下载、校验预览、确认导入。

本次不做类目属性绑定导入，不做导入删除，不新增数据库表，也不新增远端菜单 SQL。

## 2. 设计边界

- 导入只支持新增和更新。
- 导入确认前必须先校验预览。
- 分类父级通过“父级分类编码”定位；父级必须已存在，或位于当前文件前面的行。
- 属性选项通过“属性编码 + 选项编码”定位；属性必须已存在且选项来源为 `ATTRIBUTE_OPTION`。
- 确认导入阶段如出现错误，事务回滚，避免半导入。
- 导入权限复用现有管理端权限：
  - 分类导入：`product:category:add`
  - 属性导入：`product:attribute:add`
  - 属性选项导入：`product:attribute:edit`

## 3. 已完成事项

- 新增导入行 DTO：
  - `ProductCategoryImportRow`
  - `ProductAttributeImportRow`
  - `ProductAttributeOptionImportRow`
- 新增导入结果对象：
  - `ProductImportResult`
  - `ProductImportMessage`
- 新增 `ProductConfigImportService`，统一承载校验预览、确认导入和事务回滚。
- 管理端后端接口新增：
  - `/product/admin/categories/importTemplate`
  - `/product/admin/categories/importPreview`
  - `/product/admin/categories/importData`
  - `/product/admin/attributes/importTemplate`
  - `/product/admin/attributes/importPreview`
  - `/product/admin/attributes/importData`
  - `/product/admin/attributes/options/importTemplate`
  - `/product/admin/attributes/options/importPreview`
  - `/product/admin/attributes/options/importData`
- 前端新增 `ProductImportModal`，分类页和属性页复用。
- 前端 `services/product/product.ts` 新增模板下载、预览、确认导入方法。
- 导入模板追加多 sheet 辅助信息：
  - 第一个 sheet 仍为正式导入 sheet，保持空白，避免误导入示例数据。
  - 第二个 sheet 为“填写示例”，按同一字段给出可复制示例行。
  - 第三个 sheet 为“字段说明”，包含字段、是否必填、填写规则、示例和常见错误。
  - 布尔和状态类字段在模板中使用“是/否”“正常/停用”等可读值，导入时由后端转换为内部 code。

## 4. 验证命令

后端 product 模块：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl product -am -DskipTests package
```

结果：通过。

后端 admin 完整打包：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-admin -am -DskipTests package
```

结果：通过，`ruoyi-admin.jar` 已重新 repackage。

前端类型检查：

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc
```

结果：通过。

前端新增范围 lint：

```powershell
cd E:\Urili-Ruoyi\react-ui
npx biome lint src/pages/Product src/services/product src/types/product
```

结果：通过，检查 10 个文件，无警告。

## 5. 接口验证

- 分类导入模板下载成功，空模板预览返回 `code = 601`，错误为“导入文件不能为空”。
- 属性导入模板下载成功，空模板预览返回 `code = 601`，错误为“导入文件不能为空”。
- 属性选项导入模板下载成功，空模板预览返回 `code = 601`，错误为“导入文件不能为空”。
- 临时有效分类 Excel 预览返回 `code = 200`，新增 1 行，错误 0 行。
- 临时有效属性 Excel 预览返回 `code = 200`，新增 1 行，错误 0 行。
- 模板结构验证：
  - 分类、属性、属性选项模板均包含正式导入 sheet、“填写示例”和“字段说明”。
  - 导入解析仍只读取第一个 sheet，示例 sheet 不会被直接导入。
- 中文显示值预览验证：
  - 分类父子两行预览返回 `code = 200`，新增 2 行，错误 0 行；验证“否/是”“正常”等模板显示值可导入。
  - 属性单行预览返回 `code = 200`，新增 1 行，错误 0 行；验证“正常”等模板显示值可导入。
- 属性选项空模板预览返回“商品属性选项导入校验未通过”，已修正此前失败文案过于泛化的问题。

有效 Excel 只做预览，没有执行确认导入，没有写入业务测试数据。

## 6. 浏览器验证

- `http://127.0.0.1:8001/basic-config/product-category` 显示“导入”按钮。
- `http://127.0.0.1:8001/basic-config/product-attribute` 显示“导入属性”和“导入选项”按钮。
- 导入弹窗可打开，包含模板下载、上传、校验、确认导入控件。
- 截图：`logs/screenshots/product-import-modal-20260604-1924.png`

## 7. 大文件合理性判断

- `ProductConfigImportService.java` 当前 519 行，触发 500 行自检。
  - 当前不拆分原因：三类导入共享同一套校验预览、确认导入、事务回滚和结果汇总流程；拆开会复制大量控制逻辑。
  - 后续拆分点：如果新增类目属性绑定导入、导入任务记录、异步导入或错误文件导出，应拆成分类、属性、选项三个导入服务。

## 8. 残留问题

- 类目属性绑定导入未实现。
- 未做导入错误文件下载。
- 未做异步导入任务记录。
- 未用确认导入写入真实业务测试数据；本次只验证预览和接口链路，避免污染当前运行库。
