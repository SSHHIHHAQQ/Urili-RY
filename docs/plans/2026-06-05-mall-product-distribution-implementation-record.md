# 商城商品列表首版实现记录

日期：2026-06-05

状态：代码已实现，SQL 已执行到远端 `fenxiao` 运行库，后端、前端基础运行态和新增页 UI 已验证。

## 1. 目标

落地管理端“商品管理 / 商城商品列表”首版正式能力，用于管理商城正式商品主数据，而不是来源商品库或占位页面。

首版支持：

- 管理端手工创建 SPU。
- 一个 SPU 下维护多个 SKU。
- 商品必须绑定卖家。
- SPU 绑定末级可发布类目，并按类目属性模板填写 SPU 属性值。
- SKU 维护固定规格、客户 SKU、系统 SKU、供货价、销售价、币种和状态。
- SPU 主图、SPU 轮播图、SKU 图通过现有上传能力保存资源路径。
- SPU / SKU 双层状态查看和切换。
- 库存只展示读取边界，不在商品模块维护库存事实。

## 2. 代码改动

### 2.1 后端

- 新增 `product_spu`、`product_sku`、`product_attribute_value`、`product_image` 对应领域对象。
- 新增管理端接口 `AdminProductDistributionController`，路径 `/product/admin/distribution-products`。
- 新增 `IProductDistributionService` 和 `ProductDistributionServiceImpl`。
- 新增 `ProductDistributionMapper` 和 MyBatis XML。
- 新增 `ProductSellerLookupService`，由 seller 模块提供卖家快照实现，product 模块不直接读取 seller Mapper。
- product 模块新增 finance 依赖，用于校验业务可用币种。

### 2.2 前端

- 新增 `react-ui/src/pages/Product/Distribution/index.tsx`，替换原商城商品列表占位入口。
- 新增 SPU 列表、展开 SKU 子表、详情抽屉和新增/编辑独立页面。
- 新增 `ProductImageSection`、`ImageUploadField`、`SkuMatrixEditor`、`DetailContentBuilder`、`DetailContentPreview` 等录入组件。
- 新增 `distributionProduct.ts` service 和 `distribution-product.d.ts` 类型。
- 列表页复用 `getPersistedProTableSearch(...)`，选择器复用可搜索下拉配置。

### 2.3 SQL

- 新增 SQL 执行稿：`RuoYi-Vue/sql/20260605_mall_product_distribution_seed.sql`。
- 该脚本包含 4 张业务表、商品销售状态字典、商品来源字典、菜单组件更新和按钮权限。
- 已同步 `RuoYi-Vue/sql/business_menu_seed.sql` 中菜单 `2402` 的组件路径。
- SQL 已执行到远端 `fenxiao` 运行库，执行记录见 `docs/plans/2026-06-05-mall-product-distribution-db-execution-record.md`。

## 3. 关键规则

- SPU 系统编码和 SKU 系统编码由后端生成。
- 客户 SPU / 客户 SKU 在同一卖家下校验唯一。
- SPU 只能选择启用且可发布的末级类目。
- SPU 上架前必须有卖家、类目、SPU 主图、至少一个 `READY` 或 `ON_SALE` 的可上架 SKU。
- SKU 上架前必须有系统 SKU、供货价、销售价、启用币种，且 SKU 不能是停用状态。
- SPU 已上架后新增 SKU 时，新 SKU 不能自动成为 `ON_SALE`。
- 销售价低于供货价首版不硬阻断，后续可在前端补强风险提示。
- 商品模块不维护库存数量；未来库存模块按 `sku_id + warehouse_id` 提供读取。

## 4. 权限检查结果

- 后端列表、详情、SKU 查询使用 `product:distribution:query`。
- 后端新增使用 `product:distribution:add`。
- 后端编辑使用 `product:distribution:edit`。
- 后端 SPU / SKU 状态切换使用 `product:distribution:status`。
- 前端新增、编辑、状态切换按钮已接入对应权限判断。

## 5. 字典/选项复用检查结果

- 商品销售状态 SQL 初始化为 `product_sales_status`。
- 商品创建来源 SQL 初始化为 `product_source_type`。
- 前端首版集中维护状态和来源展示映射，后续可切换为字典接口读取。
- 后端仍通过 `finance_currency` 校验业务可用币种。
- 新增/编辑页不允许用户自由选择 SKU 币种；当前先通过“发货仓库（预留）”的同国家选择推导币种，仓库真实模块落地后替换临时仓库选项并补保存字段。

## 6. 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，新增“product 商城商品 SPU/SKU 模块”。
- 已记录 seller 快照、类目 schema、finance 币种、上传路径和库存边界的复用规则。

## 7. 新增问题

- 前端直接访问部分历史 SPA 动态路由可能返回 404；当前新增页 `/product/distribution/create` 已可直接打开并在登录后回跳。
- 前端销售价低于供货价的风险提示还未做成显式弹窗或标记。
- 库存列首版只展示 `--`，等待库存模块提供读取接口后接入。
- 发货仓库当前只做 UI 预留和币种推导，不保存仓库绑定；后续仓库模块落地后需要替换临时选项并把 SPU 仓库绑定持久化。

## 8. 已修复问题

- 修正前端币种 option 类型误用，统一使用 `label/value`。
- 修正 Ant Design v6 `Divider` 类型不兼容问题。
- 修正 ProTable `valueEnum` 宽类型直接读取 `.text` 的 TypeScript 报错。
- 收紧 SPU 上架校验，草稿 SKU 不再计入可上架 SKU。
- 补充币种不存在时的后端明确错误提示。

## 9. 残留问题

- 已在浏览器中完成新增页视觉和交互验收；截图见 `output/playwright/mall-product-create-page-v2.png`。
- 若后续接入卖家上传审核流程，需要新增审核中心表和来源转换规则，不复用本菜单状态字段硬凑审核。

## 10. 验证命令

已执行：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl product -am -DskipTests compile

cd E:\Urili-Ruoyi\react-ui
npm run tsc

cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-admin -am -DskipTests compile

cd E:\Urili-Ruoyi\react-ui
npm run build

cd E:\Urili-Ruoyi
npx --package @playwright/cli playwright-cli -s=mall_product_ui open http://127.0.0.1:8001/product/distribution/create --browser=chrome
npx --package @playwright/cli playwright-cli -s=mall_product_ui snapshot
npx --package @playwright/cli playwright-cli -s=mall_product_ui console warning
npx --package @playwright/cli playwright-cli -s=mall_product_ui screenshot --filename output/playwright/mall-product-create-page-v2.png --full-page

cd E:\Urili-Ruoyi
codegraph sync .

cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-admin -am -DskipTests package

cd E:\Urili-Ruoyi
.\start-backend-local.ps1 -Restart
```

结果：

- product 模块编译通过。
- React TypeScript 检查通过。
- ruoyi-admin 聚合编译通过。
- React 生产构建通过。
- 浏览器登录后进入新增页成功。
- 新增页底部保存栏、8 个固定图片槽、主图必填标识、尺寸图提示槽、详情模块按钮、SKU 表格和仓库币种推导均已通过快照验证。
- 新增/编辑 SKU 表不展示系统 SKU；详情抽屉仍展示系统 SKU。
- 未选仓库时 SKU 币种列显示 `-`；选择中国仓库后 SPU 仓库国家显示中国、币种显示人民币 (CNY)，SKU 币种列同步显示人民币 (CNY)。
- 跨国家仓库混选已被前端拦截，页面保持原同国家仓库选择。
- 新增/编辑页底部保存操作区已改为 Ant Design `Affix + Card + Space + Button`，浏览器验证 `bottomGap=0`，确认为冻结在视口底部。
- Playwright 控制台检查结果为 `Errors: 0, Warnings: 0`。
- SKU 尺寸重量展示已改为紧凑格式，例如 `28.00 x 20.00 x 3.00 cm   180 g`，不再展示 `长 28cm / 宽 20cm / 高 3cm / 重 180g`。
- SKU 批量填充区已改为 Ant Design `Space + Space.Compact` 紧凑工具条，尺寸三边使用组合输入，重量、供货价、销售价和“应用到全部 SKU”按钮保持同一操作行，避免输入框纵向铺满页面。
- CodeGraph 同步通过。
- ruoyi-admin jar 打包通过。
- 后端 8080 启动成功。
- 管理端登录成功，`/product/admin/distribution-products/list?pageNum=1&pageSize=10` 返回 `code=200`。
- 管理端详情接口已返回 SKU 三边尺寸字段，演示 SKU 返回 `lengthValue=28cm`、`widthValue=20cm`、`heightValue=3cm`、`weight=180g`。
- 动态路由中已返回 `Product/Distribution/index`。
- 新增表单依赖接口验证通过：启用卖家 3 条、启用分类 220 条、启用币种 4 条。
- 新增接口已进入后端业务校验；缺少卖家时返回“请选择卖家”，列表仍为 0，未插入临时数据。
- 前端 8001 启动成功，首页返回 200，`/api/captchaImage` 代理返回 `code=200`。

## 11. 未验证原因

- SQL 已执行并完成基础库内验证。
- 后端已重启并完成登录、列表接口、动态路由验证。
- 新增商品完整成功保存路径本轮未再造新临时业务数据；当前已验证新增页 UI、前端校验边界和后端业务校验入口。
- 前端已启动并完成首页、`/api` 代理、TypeScript、生产构建和浏览器新增页视觉验收。
- 仓库绑定真实保存未验证，原因是仓库模块尚未落地；本轮只按确认范围预留 UI 和币种推导。
- SKU 三边尺寸字段已实施；已按 AGENTS 先生成并确认字段方案 `docs/plans/2026-06-05-mall-product-sku-dimensions-field-plan.md`，随后同步后端、前端、SQL 脚本和运行库。

## 12. CodeGraph 更新结果

已执行：

```powershell
cd E:\Urili-Ruoyi
codegraph sync .
```

结果：

- 首次同步成功，输出 `Synced 13 changed files`，其中 Added 12、Modified 1。
- SQL 执行与运行态记录更新后再次同步成功，输出 `Synced 1 changed files`。
- 本轮新增页 UI 反馈修正完成后再次执行 `codegraph sync .`，输出 `Already up to date`。
- SKU 三边尺寸字段和文档记录补齐后再次执行 `codegraph sync .`，输出 `Already up to date`。

## 13. 大文件合理性判断结果

- `ProductDistributionServiceImpl` 已超过 500 行，但职责仍集中在商城商品 SPU/SKU 首版保存、状态、图片、属性值的事务边界内。后续如果继续增加导入、批量审核、库存读取或来源转换，应拆分为独立 service/support。
- `EditPage.tsx` 已承载新增/编辑页主体流程，但 SKU 矩阵、图片上传和详情内容已拆到独立组件；后续如果继续增加价格策略、库存读取或审核状态，应继续拆分页面内的基础信息、仓库绑定和类目属性区块。

## 14. 重复代码检查结果

- 卖家选择、类目选择、币种选择均复用现有 service。
- 类目 schema 计算复用后端 `IProductConfigService.previewCategorySchema(...)`。
- 图片上传复用现有通用上传接口。
- 状态和来源展示首版在商品列表目录集中维护，未散落到多个页面。
- 详情图文模块统一通过 `detailContent.ts` 做解析和序列化，新增/编辑页与详情预览不各自拼 JSON。
