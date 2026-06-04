# 商品分类与商品属性配置实施目标追踪

日期：2026-06-04

## 1. 目标名称

商品分类与商品属性配置第一阶段落地。

## 2. 总体目标

在 `E:\Urili-Ruoyi` 中实现商品分类与商品属性配置的第一阶段能力：管理端维护平台级商品分类、属性库、属性选项和类目属性配置；卖家端、买家端本阶段不实现页面，只预留后续只读消费边界。实现完成后具备可落库 SQL、后端接口、管理端 React 页面、权限点、复用台账和验证记录。用户确认后，已继续执行 SQL、重启后端，并完成接口与浏览器页面验证。

追加目标：在管理端商品分类配置和商品属性配置中补充批量导入能力，第一版支持分类、属性库、属性选项三类模板；类目属性绑定导入留到第二阶段。

追加目标：增强三类导入模板的可读性，正式导入 sheet 保持空白，额外增加“填写示例”和“字段说明”sheet，避免新人员不知道如何填写，同时避免示例数据被误导入。

## 3. 已确认边界

- 表和领域基础能力放在 `product` 模块。
- `product` 不是第四个端，只是商品领域基础能力。
- 管理端拥有商品分类、商品属性、类目属性模板的配置权。
- 卖家端后续只读取发布 schema 并提交商品，不维护平台配置。
- 买家端后续只读取分类、筛选项和商品详情，不维护平台配置。
- 第一阶段不做 SKU、多 SKU、库存、价格、条码、商品发布、商品审核、买家筛选、外部平台属性同步。
- SQL 文件已在用户确认后执行到当前激活数据源，并已留下执行记录、备份路径和验证结果。
- 导入只支持新增和更新，不支持导入删除。
- 导入前先做校验预览，确认导入阶段如果出错必须整体回滚。

## 4. 端归属设计

```text
RuoYi-Vue/product
  商品分类、属性库、属性选项、类目属性配置、发布 schema 预览

RuoYi-Vue/seller
  后续卖家商品发布、草稿、提交；只读调用 product schema

RuoYi-Vue/buyer
  后续买家商品浏览、筛选、详情；只读调用 product 分类和筛选项

react-ui
  当前阶段只实现管理端配置页面
```

管理端接口：

```text
/product/admin/categories/**
/product/admin/attributes/**
/product/admin/category-attributes/**
```

端归属口径：

- `admin` 就是管理端。
- 属于 admin / seller / buyer 某个端的页面、Controller、权限和端内业务能力必须放在对应端。
- `product` 只承载商品共享基础域；不作为第四个端。
- 后续 seller 发布入口放 `seller`，buyer 浏览入口放 `buyer`。

## 5. 本阶段实现范围

- 新增 `RuoYi-Vue/product` Maven module。
- 接入父 `pom.xml` 和 `ruoyi-admin` 依赖。
- 新增 4 张配置表 SQL 文件：
  - `product_category`
  - `product_attribute`
  - `product_attribute_option`
  - `product_category_attribute`
- 后端实现管理端接口：
  - `/product/admin/categories/**`
  - `/product/admin/attributes/**`
  - `/product/admin/category-attributes/**`
- 前端实现管理端页面：
  - 商品分类
  - 商品属性，包含属性库和类目属性模板
- 前端实现导入弹窗：
  - 商品分类导入
  - 商品属性导入
  - 商品属性选项导入
- 新增前端 `services/product` 和 `types/product`。
- 新增管理端菜单和按钮权限 SQL 文件。
- 更新 `docs/architecture/reuse-ledger.md`。
- 写实施记录 Markdown，记录做了什么、验证了什么、未验证原因和后续执行结果。

## 6. 明确不做

- 不做商品发布完整流程。
- 不做 SKU 规格、多 SKU、库存、价格、条码。
- 不做商品审核。
- 不做买家端筛选和商品详情。
- 不做卖家端发布页面。
- 不做外部平台属性映射或同步。
- 不做类目属性绑定导入。
- 不做导入删除。

## 7. 实施清单

| 序号 | 任务 | 状态 | 说明 |
| --- | --- | --- | --- |
| 1 | 创建目标追踪文档 | 已完成 | 当前文件 |
| 2 | 新增 `product` Maven module | 已完成 | 已加入父 `pom` 与 `ruoyi-admin` 依赖 |
| 3 | 新增 4 张配置表 SQL 种子 | 已完成 | 已按用户确认执行到当前激活数据源 |
| 4 | 实现分类后端接口 | 已完成 | 树列表、新增、编辑、删除、按是否末级自动判断可发布 |
| 5 | 实现属性库和属性选项后端接口 | 已完成 | 属性 CRUD、选项维护 |
| 6 | 实现类目属性配置后端接口 | 已完成 | ADD / OVERRIDE / DISABLE、schema 预览 |
| 7 | 实现管理端商品分类页面 | 已完成 | 多级分类树与新增/编辑/删除 |
| 8 | 实现管理端商品属性页面 | 已完成 | 属性库 Tab、类目属性模板 Tab |
| 9 | 新增前端 service 和类型定义 | 已完成 | `services/product`、`types/product` |
| 10 | 新增菜单权限 SQL | 已完成 | 管理端 `sys_menu`，不写 seller/buyer menu |
| 11 | 更新复用台账 | 已完成 | 已记录 product 模块、schema service、前端页面规则 |
| 12 | 运行后端构建验证 | 已完成 | product 链路通过；admin 链路跳过 repackage 后通过 |
| 13 | 运行前端静态检查 | 已完成 | `npm run tsc` 与新增范围 `biome lint` 通过 |
| 14 | 更新实施记录 | 已完成 | 已写实施记录 Markdown |
| 15 | 执行 SQL 并备份 | 已完成 | 备份见 `logs/db-backups/product-category-attribute-before-20260604-184722.sql` |
| 16 | 重启后端 | 已完成 | 当前 `8080` 使用新构建的 `ruoyi-admin.jar` |
| 17 | 浏览器页面验证 | 已完成 | 分类页、属性页均不再显示规划页 |
| 18 | 新增分类/属性/选项导入后端 | 已完成 | 模板下载、校验预览、确认导入 |
| 19 | 新增管理端导入弹窗 | 已完成 | 分类页、属性页复用 `ProductImportModal` |
| 20 | 验证导入接口和页面入口 | 已完成 | 模板下载、空模板预览、有效预览、浏览器入口 |
| 21 | 增强导入模板示例和字段说明 | 已完成 | 第一 sheet 仍为空白正式导入区，后续 sheet 给示例和填写规则 |

## 8. 验收标准

- 后端 `product` module 能参与 Maven 构建。
- 管理端接口具备分类、属性、选项、类目属性配置的基础 CRUD 和 schema 预览。
- 分类支持多级树，商品发布约束只选择最末级类目的规则在服务层有明确校验入口。
- 属性支持 code/label，属性选项不默认塞进 `sys_dict`。
- 类目属性配置支持 `ADD / OVERRIDE / DISABLE`。
- 前端页面能完成配置操作，不再使用 `Common/PlannedPage` 占位。
- 前端页面能下载导入模板、上传 Excel、执行校验预览并确认导入。
- 导入模板应包含可复制示例和字段说明，但导入解析仍只读取第一个正式导入 sheet。
- 导入接口只通过业务 code 定位，不要求用户填写数据库主键。
- 权限点全部走管理端 `sys_menu`，不写入 `seller_menu` / `buyer_menu`。
- SQL 文件可重复执行或具备幂等设计。
- 数据库执行动作已在用户确认后完成，执行记录已落 Markdown。

## 9. 数据库边界

初始实现阶段不直接读取、不写入、不迁移远端数据库；随后用户确认“都执行”后，已按项目规则确认激活数据源、写 SQL 执行记录、备份相关数据，并执行 SQL。

允许创建：

```text
RuoYi-Vue/sql/20260604_product_category_attribute_seed.sql
```

已执行内容：

```text
CREATE TABLE
INSERT INTO sys_menu
INSERT INTO sys_dict_*
```

本次没有执行大规模 `ALTER TABLE`，没有写入卖家端或买家端权限体系。

## 10. 预期验证

后端：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -DskipTests package
```

前端：

```powershell
cd E:\Urili-Ruoyi\react-ui
npx biome lint src/pages/Product src/services/product src/types/product
```

如果完整验证受已有工作区未完成改动影响，需要记录具体失败原因和本轮新增文件是否可定位通过。

## 11. 风险

- 当前工作区已有大量未提交改动，本轮不能回滚或覆盖无关变更。
- 如果后端 jar 正在运行，完整 `mvn package` 可能因 jar 被占用失败，需要记录并使用既有启动脚本流程处理。
- 前端远端菜单已由 `sys_menu` 种子落库后显示新页面；如果浏览器仍显示旧页面，需要重新登录或硬刷新以重新拉取 `/getRouters`。
- 旧 `business_menu_seed.sql` 已有商品分类/属性占位菜单，正式实现时需要用新菜单 SQL 指向正式页面，避免继续落到 `Common/PlannedPage`。
- 分类移动会影响属性继承，第一版不建议开放自由拖拽移动。
- 属性类型和选项 code 一旦被引用，不应随意修改。

## 12. 执行顺序

1. 后端 `product` module 和 SQL 文件。
2. 后端 Controller / Service / Mapper / XML。
3. 前端 services / types。
4. 前端商品分类页面。
5. 前端商品属性页面。
6. 菜单权限 SQL。
7. 复用台账和实施记录。
8. 构建、lint、变更复查。

## 13. 当前状态

已完成第一阶段实现、SQL 执行、后端重启、接口验证和浏览器页面验证。当前分类与属性配置页面已替换原规划页，运行库已有商品配置表、字典和管理端菜单权限；下一步是录入少量真实测试配置，验证新增、编辑、删除和 schema 预览写入链路。
