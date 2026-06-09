# 来源 SKU 管理端权限修复记录

日期：2026-06-08

## 问题

管理端账号在商城商品编辑页配对/选择来源 SKU 时，前端使用 `product:list:list` 判断是否允许查询来源 SKU；后端来源商品库接口也只接受 `product:list:list`。这会导致拥有上游主仓/来源 SKU 查询权限的管理端账号，在商品编辑页仍被拦截，表现为无法编辑或配对来源 SKU。

## 根因

来源 SKU 属于 `integration` 模块的上游读模型能力，不应该只绑定到“来源商品库”菜单权限 `product:list:list`。商品编辑页使用来源 SKU 时，本质需要的是管理端上游查询能力 `integration:upstream:query`。

## 修复

- `AdminSourceProductController` 的 `/integration/admin/source-products/list` 和 `/group-detail` 改为接受 `integration:upstream:query` 或 `product:list:list` 任一权限。
- 商品编辑页 `canQuerySourceProducts` 改为 `integration:upstream:query || product:list:list`。
- 更新前端权限 guard、后端 integration 权限合同和系统路由合同。

## 验证

- `mvn -pl integration -am -DskipTests compile`：通过。
- `mvn -pl integration -am -Dtest=IntegrationAdminPermissionContractTest "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过。
- `mvn -pl ruoyi-system -Dtest=IntegrationAdminRouteContractTest test`：通过。
- `npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand`：通过。
- `npm run tsc`：通过。
- `mvn -pl ruoyi-admin -am "-Dmaven.test.skip=true" package`：通过，已重新打包后端 jar。
- `.\start-backend-local.ps1 -Restart`：通过，`http://127.0.0.1:8080` 返回 HTTP 200。

## 备注

第一次执行 `mvn -pl ruoyi-admin -am -DskipTests package` 时失败，原因是 `-DskipTests` 仍会编译测试代码，现有 `seller` 测试桩尚未覆盖 `IProductDistributionService` 新增审核方法。后续若要完整打包并编译测试，应补齐该测试桩。
