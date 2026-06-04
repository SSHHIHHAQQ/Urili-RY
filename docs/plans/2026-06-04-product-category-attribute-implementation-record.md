# 商品分类与商品属性配置实施记录

日期：2026-06-04

## 1. 实施范围

本次完成商品分类与商品属性配置第一阶段落地：

- 后端新增 `RuoYi-Vue/product` Maven module。
- 管理端新增 `/product/admin/**` 配置接口。
- SQL 文件新增 4 张配置表、相关字典和管理端菜单权限种子。
- React 管理端新增商品分类页面、商品属性页面、属性库、属性选项、类目属性模板和 schema 预览。
- React 管理端新增商品分类、商品属性、商品属性选项导入入口。
- 更新复用台账、目标追踪和 SQL 执行记录。

本记录已追加执行阶段结果：用户确认后已执行 SQL、重启后端，并完成接口与浏览器页面验证。SQL 执行细节见 `docs/plans/2026-06-04-product-category-attribute-sql-execution-record.md`。

## 2. 已完成事项

- 新增父 `pom.xml` 的 `product` module 声明，并让 `ruoyi-admin` 依赖 `product`。
- 新增 `product_category`、`product_attribute`、`product_attribute_option`、`product_category_attribute` SQL。
- 新增管理端权限点：
  - `product:category:*`
  - `product:attribute:*`
  - `product:categoryAttribute:*`
- 新增后端领域对象、Mapper、Mapper XML、Service、Controller。
- 分类支持多级树，第一版不开放移动分类。
- 分类发布约束：
  - 可发布分类不能再新增子分类。
  - 有子分类的分类不能设置为可发布。
- 属性库支持属性类型、选项来源、若依字典类型、自定义选项。
- 类目属性配置支持 `ADD`、`OVERRIDE`、`DISABLE`。
- schema 预览按分类祖先链合并本类目规则和继承规则。
- 前端页面按管理端归属放在 `react-ui/src/pages/Product/**`。
- 前端服务集中在 `react-ui/src/services/product/product.ts`。
- 前端类型集中在 `react-ui/src/types/product/product.d.ts`。
- 导入能力：
  - 分类、属性、属性选项均支持模板下载、校验预览、确认导入。
  - 导入只支持新增和更新，不支持导入删除。
  - 分类导入用父级分类编码定位父级；属性选项导入用属性编码定位属性。
  - 导入确认阶段如出现错误会回滚，避免半导入。
  - 导入模板包含空白正式导入 sheet、可复制示例 sheet 和字段说明 sheet，降低新人员误填成本。
  - 导入模板面向操作人员展示“是/否”“正常/停用”等可读值，不要求新人员直接填写内部 code。

## 3. 未做事项

- 未做卖家端商品发布、SKU、多 SKU、库存、价格、商品审核、买家筛选、外部平台属性映射。
- 未做真实分类、属性、选项、类目模板的业务初始化数据。
- 未开放分类拖拽移动或跨父级迁移。

## 4. 验证命令

后端 product 模块链路：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl product -am -DskipTests package
```

结果：通过，`product` module 编译和打包成功。

后端 admin 依赖链路：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-admin -am -DskipTests package
```

结果：最终通过，`ruoyi-admin` 已完成重新打包。早前曾因运行中的 jar 锁文件导致 repackage rename 失败，停止占用进程后重新构建成功。

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

## 5. 权限检查结果

- 后端接口全部配置 `@PreAuthorize`。
- 新增、编辑、删除类接口配置 `@Log`。
- 确认导入接口配置 `@Log(... BusinessType.IMPORT)`，校验预览和模板下载不记录写操作日志。
- SQL 文件包含菜单和按钮权限种子。
- 前端按钮使用 `access.hasPerms(...)` 控制展示。
- 导入权限复用现有管理端权限，不新增远端菜单 SQL：
  - 分类导入：`product:category:add`
  - 属性导入：`product:attribute:add`
  - 属性选项导入：`product:attribute:edit`
- 本次只写管理端 `sys_menu` 权限，不写 `seller_menu` / `buyer_menu`。

## 6. 字典和选项复用检查结果

- 属性类型、选项来源、类目属性规则模式、属性分组通过 SQL 初始化为若依字典。
- 前端同类 code/label 集中在 `react-ui/src/pages/Product/constants.ts`。
- 自定义商品属性选项进入 `product_attribute_option`，没有把所有业务属性选项塞进 `sys_dict`。
- 若选择 `SYS_DICT` 作为选项来源，属性只保存 `dict_type`，具体字典项仍复用若依字典。

## 7. 复用台账检查结果

已更新 `docs/architecture/reuse-ledger.md`：

- 登记 `RuoYi-Vue/product`。
- 登记 `react-ui/src/pages/Product/**`、`services/product`、`types/product`。
- 登记 `ProductConfigImportService` 和 `ProductImportModal`，后续同类商品配置导入复用同一流程。
- 明确后续 seller/buyer 只读消费 product schema，不维护平台配置。
- 明确后续不要在前端或其他模块重复实现类目属性继承合并逻辑。

## 8. 大文件合理性判断结果

- `ProductConfigServiceImpl.java` 当前 527 行，触发 500 行自检。
  - 当前不拆分原因：第一阶段分类、属性、类目属性规则共用一个事务边界和一个 Mapper，schema 预览需要同时读取三类配置；先保持一个 Service 能减少跨服务绕行。
  - 后续拆分点：一旦进入商品发布、买家筛选或外部平台属性映射，应拆成 `ProductCategoryConfigService`、`ProductAttributeConfigService`、`ProductCategorySchemaService`。
- `CategoryAttributeTemplate.tsx` 当前 422 行，触发 400 行自检。
  - 当前不拆分原因：该文件只承载“选择类目、本类目规则、继承预览、规则表单”一个配置工作台，职责仍围绕类目属性模板。
  - 后续拆分点：如果规则表单字段继续增加，应拆出 `CategoryAttributeRuleForm` 和列配置文件。
- `ProductConfigImportService.java` 当前 519 行，触发 500 行自检。
  - 当前不拆分原因：该文件只承载商品配置导入的同一条流程，分类、属性、选项共享行级校验、预览、确认导入和事务回滚策略；拆开会复制大量导入控制逻辑。
  - 后续拆分点：如果继续加入类目属性绑定导入、导入任务记录或异步导入，应拆为 `ProductCategoryImportService`、`ProductAttributeImportService`、`ProductAttributeOptionImportService`。

## 9. 重复代码检查结果

- 分类树组装和 Tree/TreeSelect 转换抽到 `react-ui/src/pages/Product/categoryTree.ts`。
- 商品选项管理从属性库页面拆到 `AttributeOptionManager.tsx`。
- 商品导入弹窗抽到 `ProductImportModal.tsx`，分类页和属性页复用。
- 商品导入模板生成抽到 `ProductImportTemplateService`，分类、属性、属性选项复用同一套“正式导入 + 示例 + 字段说明”结构。
- 商品状态、是否、属性类型、选项来源、规则模式集中到 `react-ui/src/pages/Product/constants.ts`。
- 后端 schema 继承合并只在 `IProductConfigService.previewCategorySchema` 暴露，不在前端重复实现。

## 10. 残留问题

- 当前运行库已有 4 张商品配置表、商品配置字典和管理端菜单权限；尚未录入真实商品分类和属性配置数据。
- `product_category` 的逻辑删除与唯一索引后续如果要支持删除后同名重建，需要在执行 SQL 前再次确认索引策略。
- 类目移动第一版未开放，后续如要拖拽排序或移动类目，需要单独设计继承规则重算和商品历史 schema 版本处理。
- 类目属性绑定导入暂未实现，等分类和属性库稳定后再做第二阶段。

## 11. 下一步建议

1. 使用导入模板录入少量测试分类、属性、属性选项。
2. 再手工配置少量类目属性模板，验证 schema 预览。
3. 真实配置稳定后，再进入类目属性绑定导入或卖家端商品发布表单设计。
