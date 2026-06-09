# 商城商品保存耗时优化执行记录

日期：2026-06-08

## 背景

商城商品编辑保存时，用户反馈点击保存需要等待约 5 秒，卖家体验较差。此前库存概览刷新和来源读模型刷新已经调整为事务提交后的后台任务，本次继续排查同步核心保存链路。

## 本次结论

本次重点优化官方仓商品的来源 SKU 保存链路。原保存流程在每个 SKU 上重复读取：

- 官方来源 SKU 快照
- 来源 SKU 派生的官方履约仓
- 当前商城 SKU 已有来源绑定
- 来源 SKU 是否已被其他商城 SKU 占用
- 保存后逐 SKU 校验绑定是否落库

该模式在 SKU 数量增加时会形成明显的 N+1 查询。现在改为在一次保存请求内批量读取并缓存复用。

## 改动范围

- `integration` 模块新增批量读取端口：
  - `selectOfficialSourceBindingSnapshots`
  - `selectOfficialWarehousesBySourceDimensionGroups`
- `product` 模块新增商品侧批量绑定查询：
  - `selectActiveSourceBindingsBySkuIds`
  - `selectActiveSourceBindingsBySourceSkuGroupKeys`
- `ProductDistributionServiceImpl` 新增并复用官方来源保存上下文：
  - SPU 仓库派生阶段读取来源快照和官方仓时使用同一批量上下文
  - SKU 保存阶段批量读取已有绑定和来源 SKU 占用关系
  - 保存后绑定落库校验由逐 SKU 单查改成批量查询

## 保留边界

- 核心事实仍然同步保存：SPU、SKU、发货仓库、类目属性、图片元数据、来源 SKU 绑定。
- 库存概览刷新和来源读模型刷新仍然后台执行，不阻塞卖家保存。
- 商品模块仍不直接读取来源商品库表，来源 SKU 和官方仓派生数据继续通过 integration port 访问。
- 本次没有新增表、没有执行 SQL、没有修改前端页面。

## 验证记录

后端定向测试：

```powershell
mvn -pl product -am "-Dtest=ProductDistributionServiceImplTest,ProductDistributionMapperContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：16 个测试通过。

后端 product reactor 测试：

```powershell
mvn -pl product -am test
```

结果：249 个测试通过。

后端打包：

```powershell
mvn -pl ruoyi-admin -am -DskipTests package
```

结果：打包成功。

后端启动验证：

- 8080 重新启动成功。
- `http://127.0.0.1:8080` 返回 200。

真实保存验证：

- 登录账号：管理端默认账号。
- 验证商品：`spuId=18`，官方仓，草稿，2 个 SKU。
- 操作：读取商品详情后原样调用 `PUT /product/admin/distribution-products/18`。
- HTTP 总耗时：约 1479ms。
- 后端核心保存日志：

```text
商城商品核心保存完成 action=update spuId=18 total=966ms spu=32ms warehouses=59ms skusAndSource=283ms attributes=421ms images=170ms asyncRegister=1ms
商城商品后台读模型刷新完成 task=product-inventory-overview-refresh cost=433ms
```

说明：本次真实验证中，核心同步保存耗时约 966ms，库存读模型刷新在后台线程完成，没有阻塞接口返回。

## 后续建议

如果后续在更多 SKU 的商品上仍出现明显慢保存，下一步优先优化：

- 客户 SKU 唯一性校验从逐 SKU `count` 改成批量校验。
- 类目属性和图片元数据的删除重建策略改成差异化写入或批量写入。
- 前端保存成功后列表刷新继续与保存结果解耦，避免把列表重载耗时算到保存体验里。
