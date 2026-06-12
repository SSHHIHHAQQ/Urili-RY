# 买家端商品中心实施方案

## 背景

- 管理端已有 `商品中心`，定位是买家可见商品的只读列表和详情。
- 当前买家端已经有 `/buyer/product/distribution-products` 只读接口，权限为 `buyer:product:distribution:list` 和 `buyer:product:distribution:query`。
- 当前第一版业务口径是不限制买家可见范围：买家可查看所有已上架且存在可售 SKU 的商品。
- 后续可能按买家主体、渠道、报价、合同、黑白名单或区域调整商品可见范围，因此本轮不能把“全量可见”写死到前端。

## 实施口径

1. 买家端新增顶级菜单 `商品中心`，路径 `/buyer/portal/product-center`。
2. 权限使用新的买家端商品中心命名，避免和历史商品浏览/预览权限混用：
   - `buyer:product:center:list`
   - `buyer:product:center:query`
3. `buyer owner` 默认开通商品中心，并允许通过买家端角色授权给子账号。
4. 第一版仅支持浏览商品列表和商品详情，不新增采购单、下单、询价、购物车或专属价格能力。
5. 商品数据通过买家端接口读取，前端不得传 `buyerId`、`subjectId`、`accountId` 等身份范围参数；后端从当前 buyer portal session 派生身份。
6. 商品可见范围的未来变化预留在后端 `BuyerPortalProductServiceImpl.buildVisibleProductQuery(...)` 和共享商品域 `IProductPortalDistributionService` 查询口，不在 React 页面里做业务判断。

## 代码改动范围

- 前端：
  - 新增 `react-ui/src/pages/Portal/Home/BuyerProductCenter.tsx`，适配买家端商品接口到共享 `ProductCenterPage`。
  - 更新 `react-ui/src/pages/Portal/Home/index.tsx`，增加买家端顶级菜单和视图分发。
  - 更新 `react-ui/config/routes.ts`，增加 `/buyer/portal/product-center` shell 路由。
  - 更新相关 Jest 合同测试，固定 buyer 商品中心是受控业务例外。
- 后端：
  - 更新 `BuyerPortalPermissionServiceImpl` 的可分配权限集合，允许买家商品中心权限进入端内角色菜单。
  - 增加或更新测试，固定商品权限不再被过滤，但仍拒绝 seller/admin/通配权限污染。
- SQL：
  - 新增 guarded seed，写入买家端顶级 `商品中心` 页面菜单和查询按钮，并给 owner 角色默认授权。
  - 本轮只提交 SQL 文件，不直接执行远端 DML。

## 验证计划

- 前端合同测试：买家 portal shell 路由、菜单、商品中心 adapter、禁止身份参数。
- 后端窄测试：`BuyerPortalPermissionServiceImplPortalAccessTest`、买家菜单树/角色权限相关测试。
- 三端最小 manifest gate：从 `react-ui` 目录运行 `node .\scripts\verify-three-terminal.mjs --check-manifest`。
- 完成后运行 `codegraph sync .`，并在实施记录写明结果。

## 不做事项

- 不新增业务表。
- 不执行远端 SQL。
- 不调整管理端商品中心语义。
- 不开放下单、采购单、询价或专属价格。
- 不把卖家、供货价、管控原因等敏感字段暴露给买家端。
