# 商品分类与商品属性配置实施目标追踪

日期：2026-06-04

## 1. 目标名称

商品分类与商品属性配置第一阶段落地。

## 2. 总体目标

在 `E:\Urili-Ruoyi` 中实现商品分类与商品属性配置的第一阶段能力：管理端维护平台级商品分类、属性库、属性选项和类目属性配置；卖家端、买家端本阶段不实现页面，只预留后续只读消费边界。实现完成后具备可落库 SQL、后端接口、管理端 React 页面、权限点、复用台账和验证记录，但本轮不直接执行远端数据库变更。

## 3. 已确认边界

- 表和领域基础能力放在 `product` 模块。
- `product` 不是第四个端，只是商品领域基础能力。
- 管理端拥有商品分类、商品属性、类目属性模板的配置权。
- 卖家端后续只读取发布 schema 并提交商品，不维护平台配置。
- 买家端后续只读取分类、筛选项和商品详情，不维护平台配置。
- 第一阶段不做 SKU、多 SKU、库存、价格、条码、商品发布、商品审核、买家筛选、外部平台属性同步。
- 本轮只准备 SQL 文件，不执行数据库 DDL/DML。

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
- 新增前端 `services/product` 和 `types/product`。
- 新增管理端菜单和按钮权限 SQL 文件。
- 更新 `docs/architecture/reuse-ledger.md`。
- 写实施记录 Markdown，记录做了什么、验证了什么、未验证原因。

## 6. 明确不做

- 不执行远端数据库 DDL/DML。
- 不做商品发布完整流程。
- 不做 SKU 规格、多 SKU、库存、价格、条码。
- 不做商品审核。
- 不做买家端筛选和商品详情。
- 不做卖家端发布页面。
- 不做外部平台属性映射或同步。

## 7. 实施清单

| 序号 | 任务 | 状态 | 说明 |
| --- | --- | --- | --- |
| 1 | 创建目标追踪文档 | 已完成 | 当前文件 |
| 2 | 新增 `product` Maven module | 已完成 | 已加入父 `pom` 与 `ruoyi-admin` 依赖 |
| 3 | 新增 4 张配置表 SQL 种子 | 已完成 | 只写 SQL 文件，未执行 |
| 4 | 实现分类后端接口 | 已完成 | 树列表、新增、编辑、删除、叶子可发布校验 |
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

## 8. 验收标准

- 后端 `product` module 能参与 Maven 构建。
- 管理端接口具备分类、属性、选项、类目属性配置的基础 CRUD 和 schema 预览。
- 分类支持多级树，商品发布约束只选择最末级类目的规则在服务层有明确校验入口。
- 属性支持 code/label，属性选项不默认塞进 `sys_dict`。
- 类目属性配置支持 `ADD / OVERRIDE / DISABLE`。
- 前端页面能完成配置操作，不再使用 `Common/PlannedPage` 占位。
- 权限点全部走管理端 `sys_menu`，不写入 `seller_menu` / `buyer_menu`。
- SQL 文件可重复执行或具备幂等设计。
- 所有数据库执行动作留待用户单独确认。

## 9. 数据库边界

本轮不读取、不写入、不迁移远端数据库。

允许创建：

```text
RuoYi-Vue/sql/20260604_product_category_attribute_seed.sql
```

禁止本轮直接执行：

```text
CREATE TABLE
ALTER TABLE
INSERT INTO sys_menu
INSERT INTO sys_dict_*
```

这些 SQL 只能作为文件提交，执行前需要再次确认当前激活数据源、备份和执行记录。

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
- SQL 文件准备完成后，仍需用户确认才能执行到实际数据库。
- 前端远端菜单需要对应 `sys_menu` 种子落库后才能显示新页面。
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

已完成第一阶段实现与静态验证。SQL 文件尚未执行，真实数据库和浏览器接口验证留待用户确认执行窗口、目标数据源、备份和回滚方式后再做。
