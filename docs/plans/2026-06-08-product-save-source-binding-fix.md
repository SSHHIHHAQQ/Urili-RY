# 商城商品保存来源 SKU 绑定修复记录

## 背景

编辑商城商品时，用户在官方仓模式下重新配对来源 SKU，点击保存后页面没有返回商品列表；再次进入编辑页时，来源 SKU 绑定没有按预期回显，表现为“保存像是假保存”。

## 根因判断

前端编辑保存成功后只提示保存成功，没有跳回 `/product/distribution`，用户无法明确感知保存流程结束。

后端官方仓商品的核心事实分两层保存：

- `product_sku` 保存商城 SKU 基础信息。
- `product_sku_source_binding` 保存商城 SKU 与官方来源 SKU 的 active 绑定事实，详情和列表再通过该表回显来源 SKU。

如果来源绑定写入没有真正生效，原实现缺少保存后反查校验，可能返回 200 后仍形成“官方仓商品已保存，但 SKU 无 active 来源绑定”的不一致状态。

## 修复内容

1. 编辑页保存成功后返回商城商品列表。
2. 官方仓 SKU 来源绑定插入、更新时检查影响行数，写入失败立即抛错。
3. 官方仓 SKU 保存完成后按 SKU 逐个反查 active 来源绑定，校验 `source_dimension_group_key` 与本次提交一致；不一致时回滚并提示重新配对来源 SKU。
4. 增加后端单元测试覆盖“官方仓保存后绑定未落表必须失败”和“绑定一致才允许通过”。
5. 增加前端合同测试固定编辑保存成功后的列表返回行为。

## 验证

- `git diff --check` 通过。
- `mvn -pl product -am -Dtest=ProductDistributionServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过，7 个测试全部通过。
- `npx jest --config jest.config.ts tests/product-distribution-permission-guard.test.ts --runInBand` 通过。
- `npm run tsc` 通过。
- `mvn -pl ruoyi-admin -am "-Dmaven.test.skip=true" package` 通过。
- 后端通过 `start-backend-local.ps1 -Restart` 重启，`http://127.0.0.1:8080` 返回 200。
- 浏览器只读打开 `http://127.0.0.1:8001/product/distribution`，商品列表路由可正常进入，未执行真实保存写库。
- `codegraph sync .` 通过，已同步 CodeGraph 索引。

## 注意

当前修复会阻止新的“官方仓无 active 来源绑定”继续被保存成功。已经存在的历史异常商品不会在本次代码修复中自动改数据；如需修复存量异常商品，应另行确认数据修复方案后再执行。
