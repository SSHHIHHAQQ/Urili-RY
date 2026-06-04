# 商品分类与商品属性配置实施记录

日期：2026-06-04

## 1. 实施范围

本次完成商品分类与商品属性配置第一阶段落地：

- 后端新增 `RuoYi-Vue/product` Maven module。
- 管理端新增 `/product/admin/**` 配置接口。
- SQL 文件新增 4 张配置表、相关字典和管理端菜单权限种子。
- React 管理端新增商品分类页面、商品属性页面、属性库、属性选项、类目属性模板和 schema 预览。
- 更新复用台账和目标追踪。

本次没有执行数据库 DDL/DML，没有读取或写入远端数据库。

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

## 3. 未做事项

- 未执行 `RuoYi-Vue/sql/20260604_product_category_attribute_seed.sql`。
- 未启动或重启后端服务。
- 未做浏览器 UI 操作验证，因为菜单和表结构还没有落到实际数据库。
- 未做卖家端商品发布、SKU、多 SKU、库存、价格、商品审核、买家筛选、外部平台属性映射。

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

结果：编译阶段通过，最终 `spring-boot:repackage` 因 `ruoyi-admin.jar` 无法重命名为 `ruoyi-admin.jar.original` 失败。该失败符合本项目已知的运行 jar 锁文件场景。

随后使用跳过 repackage 的验证命令：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-admin -am -DskipTests "-Dspring-boot.repackage.skip=true" package
```

结果：通过，`ruoyi-admin` 依赖链路编译和普通 jar 打包成功。

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

结果：通过，检查 9 个文件，无警告。

## 5. 权限检查结果

- 后端接口全部配置 `@PreAuthorize`。
- 新增、编辑、删除类接口配置 `@Log`。
- SQL 文件包含菜单和按钮权限种子。
- 前端按钮使用 `access.hasPerms(...)` 控制展示。
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
- 明确后续 seller/buyer 只读消费 product schema，不维护平台配置。
- 明确后续不要在前端或其他模块重复实现类目属性继承合并逻辑。

## 8. 大文件合理性判断结果

- `ProductConfigServiceImpl.java` 当前 527 行，触发 500 行自检。
  - 当前不拆分原因：第一阶段分类、属性、类目属性规则共用一个事务边界和一个 Mapper，schema 预览需要同时读取三类配置；先保持一个 Service 能减少跨服务绕行。
  - 后续拆分点：一旦进入商品发布、买家筛选或外部平台属性映射，应拆成 `ProductCategoryConfigService`、`ProductAttributeConfigService`、`ProductCategorySchemaService`。
- `CategoryAttributeTemplate.tsx` 当前 422 行，触发 400 行自检。
  - 当前不拆分原因：该文件只承载“选择类目、本类目规则、继承预览、规则表单”一个配置工作台，职责仍围绕类目属性模板。
  - 后续拆分点：如果规则表单字段继续增加，应拆出 `CategoryAttributeRuleForm` 和列配置文件。

## 9. 重复代码检查结果

- 分类树组装和 Tree/TreeSelect 转换抽到 `react-ui/src/pages/Product/categoryTree.ts`。
- 商品选项管理从属性库页面拆到 `AttributeOptionManager.tsx`。
- 商品状态、是否、属性类型、选项来源、规则模式集中到 `react-ui/src/pages/Product/constants.ts`。
- 后端 schema 继承合并只在 `IProductConfigService.previewCategorySchema` 暴露，不在前端重复实现。

## 10. 残留问题

- SQL 尚未执行，真实菜单、字典和业务表不存在于当前运行库。
- UI 只能完成静态检查，不能进行浏览器端真实接口验证。
- `product_category` 的逻辑删除与唯一索引后续如果要支持删除后同名重建，需要在执行 SQL 前再次确认索引策略。
- 类目移动第一版未开放，后续如要拖拽排序或移动类目，需要单独设计继承规则重算和商品历史 schema 版本处理。

## 11. 下一步建议

1. 单独确认 SQL 执行窗口、目标数据源、备份和回滚方式。
2. 执行 `20260604_product_category_attribute_seed.sql` 后重启后端。
3. 使用 admin 账号验证菜单、权限、分类新增、属性新增、属性选项维护、类目属性 schema 预览。
4. 真实配置稳定后，再进入卖家端商品发布表单设计。
