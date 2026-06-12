# 2026-06-12 买家端商品中心空列表排查记录

## 最终结论

买家端商品中心空列表不是因为远端商品数据为空，也不是因为商品没有上架。

当前运行库 `fenxiao` 中，满足买家端首版可见条件的商品有 8 个 SPU；买家 owner 账号也已经拿到 `buyer:product:center:list` 和 `buyer:product:center:query` 权限。

真正根因是后端列表接口在分页上下文中先执行了端账号绑定校验：

1. `BuyerPortalProductCenterController#list` 先调用 `startPage()`。
2. `BuyerPortalProductServiceImpl#assertBuyerSession(...)` 随后查询 `buyer_account` 校验当前 token 绑定账号。
3. 账号查询 XML 本身已经带 `limit 1`。
4. PageHelper 仍处于当前线程上下文，又追加了分页 `LIMIT ?`。
5. 实际 SQL 变成 `... limit 1 LIMIT ?`，MySQL 语法错误，接口返回 `code=500`。

因此前端表格表现为无商品，本质是接口 500 后没有可渲染数据。

## 修复内容

- `BuyerPortalProductServiceImpl`：端账号绑定校验前临时清理 PageHelper，上下文恢复后再执行真正商品列表查询。
- `SellerPortalProductServiceImpl`：同步补同类风险，避免卖家端商品列表也出现 `limit 1 LIMIT ?`。
- `BuyerPortalProductServiceImplTest` / `SellerPortalProductServiceImplTest`：新增回归测试，固定账号绑定校验不继承分页上下文，同时商品列表查询仍保留原分页上下文。

本次没有改变商品可见范围规则：买家端第一版仍按当前规则看全部上架且存在上架 SKU 的商城商品。未来如果要按买家、客户等级、渠道、合同价或区域限制商品范围，应该在商品分发/可见性查询层新增策略条件，不能让前端传 `buyerId` 来控制数据范围。

## 数据源与影响范围

- MySQL 目标：来自本机 `.env.local` 注入的当前后端运行数据源，库名确认是 `fenxiao`。
- Redis：本轮没有直接读写 Redis。
- 命令类型：只读 SQL 核验、本地 HTTP/API 验证、一次买家端登录/登出验证。
- 数据影响：没有执行 DDL/DML；登录验证会产生正常买家端登录日志和短时会话，验证后已尝试登出。
- 敏感信息：报告不记录数据库密码、完整连接串、portal 密码或 token。

## 只读核验结果

| 项目 | 数量 |
| --- | ---: |
| `product_spu` 总数 | 47 |
| active SPU | 47 |
| `ON_SALE/NORMAL` SPU | 9 |
| `product_sku` 总数 | 135 |
| active SKU | 135 |
| `ON_SALE/NORMAL` SKU | 24 |
| 买家商品中心可见 SPU | 8 |

买家 owner 权限核验：

| 项目 | 结果 |
| --- | ---: |
| `data.permissions` 权限数 | 21 |
| 包含 `buyer:product:center:list` | 是 |
| 包含 `buyer:product:center:query` | 是 |
| `/buyer/getRouters` 包含商品中心路由 | 是 |

## 验证记录

修复前实登接口结果：

- `/buyer/login`：`code=200`
- `/buyer/getInfo`：登录有效
- `/buyer/getRouters`：包含商品中心路由
- `/buyer/product/center/list?pageNum=1&pageSize=20`：`code=500`
- 后端错误：`buyer_account ... limit 1 LIMIT ?`

修复后验证：

| 验证项 | 结果 |
| --- | --- |
| `mvn -pl buyer,seller,product -am "-Dtest=BuyerPortalProductServiceImplTest,SellerPortalProductServiceImplTest,ProductDistributionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` | 通过，37 个测试 |
| `mvn -pl ruoyi-admin -am -DskipTests package` | 通过 |
| `.\start-backend-local.ps1 -Restart` | 后端 8080 启动成功 |
| `http://127.0.0.1:8080/buyer/product/center/list?pageNum=1&pageSize=20` | `code=200,total=8,rows=8` |
| `http://127.0.0.1:8001/api/buyer/product/center/list?pageNum=1&pageSize=20` | `code=200,total=8,rows=8` |
| 浏览器进入 `http://127.0.0.1:8001/buyer/portal/product-center` | 页面显示商品行 |

页面首屏样例：

- `SPU202606050016`：亲肤磨毛被套，3 个可见 SKU。
- `SPU202606050012`：城市缓震跑步鞋，3 个可见 SKU。

## 后续计划

1. 继续保留后端 `@PortalPreAuthorize`，不能只靠前端隐藏菜单。
2. 商品中心首版先保持“所有买家可见全部可售商城商品”。
3. 后续做商品范围限制时，在 `product` 分发读模型或可见性策略中扩展，不从前端接收买家范围参数。
4. 如果要把商品详情、SKU、价格、库存进一步拆分权限，按 `buyer:product:center:*` 命名继续扩展，并同步端内角色可分配菜单。
