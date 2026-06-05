# 买家商品浏览复制前边界方案

日期：2026-06-05

## 参考方向

本方案继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，并承接 `docs/plans/2026-06-05-seller-portal-product-template-acceptance-record.md` 的 seller 模板验收结论。

当前执行节奏固定为：

- 先做一套标准 seller 模板。
- seller 模板验收通过后，再复制 buyer。
- 每个切片只改一类东西，避免后端、前端、SQL、UI 验收混在一起造成返工。

本切片只做 buyer 复制前的业务边界和实现边界记录，不落地 buyer 商品接口，不改前端，不执行数据库 DDL/DML。

## 当前事实

- seller portal “我的商城商品”模板已完成列表、详情、SKU 三个只读入口，并完成后端单测、HTTP smoke、前端 guard、TypeScript 检查和浏览器 smoke 验收。
- buyer 模块当前已有商品分类和商品 schema 只读入口，用于买家端商品浏览准备；尚未发现 buyer 商品列表、详情、SKU 浏览入口。
- `product_spu` / `product_sku` 是商城商品事实源，`product` 模块是共享商品域，不是第四个端。
- 管理端商品接口和实体会包含 seller 归属、系统 SPU/SKU、供货价、审计字段等后台字段，不能直接作为 buyer 端响应标准。
- seller 商品模板的数据范围是“当前 seller 拥有的商品”，由 `PortalLoginSession.subjectId` 收敛为 `sellerId`。
- buyer 商品浏览的数据范围不是“当前 buyer 拥有的商品”。当前阶段没有 buyer-specific 商品可见性表、客户专属价格表、库存承诺表或上架渠道表。
- buyer 当前端内商品权限只有分类和 schema；商城商品列表、详情、SKU 权限尚未落地。
- 旧方向中“seller/buyer 账号、角色、菜单、部门继续混用若依 `sys_*`”的结论已过期；buyer 商品浏览复制不得借机回退到 `sys_menu` / `sys_role` / `sys_user`。

## 不能照抄 seller 的部分

1. 不能把 `buyerId` 当成商品归属字段。
   - seller 端查商品时用当前 seller 的 `subjectId` 过滤 `sellerId` 是正确的。
   - buyer 端查商品时，当前 buyer 的 `subjectId` 只代表登录主体，不代表商品归属。

2. 不能暴露 seller 侧内部字段。
   - buyer 响应不应包含 `sellerId`、`systemSpuCode`、`systemSkuCode`、后台审计字段。
   - `sellerSpuCode`、`sellerSkuCode`、供货价等字段首版不向 buyer 展示。

3. 不能把管理端 `ProductSpu` / `ProductSku` 当作 buyer DTO。
   - buyer 端需要独立 `BuyerPortalProduct` / `BuyerPortalProductSku`。
   - DTO 只保留买家浏览需要的商品标题、类目、图片、销售状态、销售价、币种、SKU 规格等字段。

4. 不能让前端决定数据范围。
   - buyer 商品接口不得相信前端传入的 `buyerId`、`subjectId`、`accountId`、`terminal`。
   - sellerId、系统 SPU/SKU、sourceType 等后台过滤字段也不应作为 buyer 浏览查询条件。

## 推荐的 buyer 首版浏览口径

首版 buyer 商品浏览建议先做“平台已上架商品只读浏览”，不引入客户专属价格、库存承诺、下单、购物车或审核链路。

推荐规则：

- SPU 可见条件：`spuStatus = ON_SALE`。
- SKU 可见条件：`skuStatus = ON_SALE`。
- 价格口径：只展示销售价和币种，不展示供货价；列表价格聚合必须基于 `ON_SALE` SKU，避免草稿、待上架、已下架 SKU 的价格混入 buyer 列表。
- 列表可见性：首版建议要求 SPU 至少存在一个 `ON_SALE` SKU，否则该 SPU 对 buyer 视为不可浏览。
- 库存口径：首版不展示库存可售承诺；库存模块落地后单独接入。
- seller 信息：首版不展示 seller 身份；如后续需要展示品牌、店铺或供应商，需要单独确认字段和脱敏规则。
- 查询条件：先保留 `keyword`、`categoryId`、商品中英文标题等业务筛选；不开放 sellerId、系统编码、sourceType 这类后台筛选。
- 详情规则：只有 `ON_SALE` SPU 可访问详情，SKU 列表只返回 `ON_SALE` SKU。

## 建议实现方式

后续进入 buyer 后端切片时，建议按 seller 已验收模板复制结构，但替换业务口径：

- Controller：
  - 新增 buyer 端只读入口，建议延续商品域命名。为了复用 seller 模板，路径暂定：
    - `GET /buyer/product/distribution-products/list`
    - `GET /buyer/product/distribution-products/{spuId}`
    - `GET /buyer/product/distribution-products/{spuId}/skus`
  - 如果后续用户希望 buyer API 语义更接近“浏览商品”，也可以在后端切片开始前改为 `/buyer/product/browse-products/**`；但同一个切片内不要同时保留两套路由。
  - 页面文案可以显示“商城商品”，不向用户暴露 `distribution-products` 这类内部路径词。
  - 使用 `@Anonymous`、`@PortalPreAuthorize`、`@PortalLog` 和 `PortalSessionContext.requireSession(...)`。

- Service：
  - 新增 `IBuyerPortalProductService` / `BuyerPortalProductServiceImpl`。
  - 只通过 product 共享 Service 获取商品数据，不直接从 buyer 模块跨模块读写 product Mapper。
  - Service 内强制设置 buyer 浏览可见性规则，前端参数只能作为业务筛选，不作为身份范围。

- DTO：
  - 新增 `BuyerPortalProduct` / `BuyerPortalProductSku`。
  - 不复用管理端 `ProductSpu` / `ProductSku`，不复用 seller DTO。

- 权限：
  - 新增端内权限暂定为：
    - `buyer:product:distribution:list`
    - `buyer:product:distribution:query`
  - 如果 buyer API 路径改为 `browse-products`，权限也应同步改为 `buyer:product:browse:list/query`；路径和权限必须一次选定，不混用两套命名。
  - 只写入 `buyer_menu` / `buyer_role_menu`，不得写入 `sys_menu` / `sys_role`。
  - 远程 DML 单独作为一个切片执行，并写清目标数据源、执行类型和影响范围。

- 验证：
  - 先做 buyer backend 单测和端路由/seed 契约测试。
  - 后补 buyer HTTP smoke。
  - 再复制 buyer 前端工作台卡片、前端 guard 和浏览器 smoke。

## 后续切片顺序

1. buyer 后端只读模板切片。
   - 只新增 buyer Controller / Service / DTO / 单测 / seed 文件更新。
   - 不做前端，不执行远程 DML。

2. buyer 权限 DML 与 HTTP smoke 切片。
   - 只补远程 `buyer_menu` / `buyer_role_menu` 权限和真实 HTTP 验收。
   - 不改前端 UI。

3. buyer 前端工作台复制切片。
   - 只复制 seller 已验收的前端卡片模式，替换 terminal、service、DTO、文案和断言。
   - 不改后端接口。

4. buyer 浏览器 smoke 与模板验收切片。
   - 只做可复跑 browser smoke、guard 和验收记录。
   - buyer 验收通过后，再进入下一类业务功能。

5. portal 401 隔离加固切片。
   - 当前前端全局 401 处理可能在 portal 请求失败时清理 admin session。
   - 这不是 buyer 商品后端复制的阻塞项，但应作为后续独立 token/session 隔离加固处理，不混进商品浏览切片。

## 风险和待确认

- 当前没有 buyer-specific 商品可见性表；首版只能按平台上架状态做全局浏览。
- 当前没有客户专属价格；首版 buyer 只看销售价。
- 当前没有库存承诺；首版不展示可售库存。
- 如果后续要按买家等级、国家地区、合同价、白名单商品或供应商可见性做限制，需要单独设计表和权限，不应塞进本次复制切片。
- 如果发现历史数据中存在 `ON_SALE` SPU 但没有 `ON_SALE` SKU，需要单独做 product 共享查询收口，不在前端临时过滤。

## 检查清单

- 新增问题：buyer 商品浏览不能机械复制 seller 商品拥有关系。
- 已修复问题：本切片未修改代码，不涉及修复。
- 残留问题：buyer 商品浏览接口、权限 DML、前端卡片和 smoke 尚未落地。
- 验证命令：
  - `git diff --check -- docs/plans/2026-06-05-buyer-product-browse-copy-boundary-plan.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/architecture/reuse-ledger.md`
  - `rg -n "[ \t]+$|^(<<<<<<<|=======|>>>>>>>)" docs/plans/2026-06-05-buyer-product-browse-copy-boundary-plan.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/architecture/reuse-ledger.md`
  - `codegraph sync .`
- 未验证原因：本切片是方案记录，不包含后端、前端或数据库变更。
- 权限检查结果：规划使用 `buyer_menu` / `buyer_role_menu`，不使用 `sys_menu` / `sys_role`。
- 字典/选项复用检查结果：本切片未新增字典；商品状态继续沿用 product 既有状态 code。
- 复用台账检查结果：已登记 buyer 商品浏览复制前边界。
- CodeGraph 更新结果：已执行 `codegraph sync .`，结果为 `Already up to date`。
- 大文件合理性判断结果：本方案文件职责单一，不需要拆分。
- 重复代码检查结果：后续实现应复制模板结构，但不能复制 seller 业务谓词。
