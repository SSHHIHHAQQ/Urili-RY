# 2026-06-07 三端 P0/P1 快速推进：商品侧上游 SKU 配对投影删除作用域

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

本切片收口 `product` 侧维护 `upstream_system_sku_pairing` 投影时按裸 `system_sku` 删除的问题，避免同一平台 SKU 在多个上游 connection 下被跨 connection 误删。

## 子 Agent 执行情况

- 先按用户最新要求启动 6 个 `gpt-5.3-codex-spark` 只读子 Agent。
- 6 个 `gpt-5.3-codex-spark` 均因额度限制失败，平台提示需等到 2026-06-08 01:14/01:15 后再试，失败 Agent 已关闭。
- 按 fallback 规则改用 6 个 `gpt-5.4` 只读子 Agent，并已全部关闭。
- 采纳的 P0：`ProductDistributionMapper.xml` 的 `deleteUpstreamSkuPairingsBySystemSku` 只按 `system_sku` 删除 `upstream_system_sku_pairing`，可能误删其他 `connection_code` 下的配对投影。
- 未采纳为本轮改动的 P1/P2：前端模板 JS/TS 等强校验、frontend guard manifest source-of-truth、direct-login 行为回归测试、来源商品库权限命名空间迁移。这些不阻塞当前 P0。

## 已完成

- `ProductDistributionMapper.java` 将裸删除方法改为 `deleteUpstreamSkuPairingsBySystemSkuAndConnectionCodes(systemSku, connectionCodes)`。
- `ProductDistributionMapper.xml` 将删除条件改为 `system_sku + connection_code in (...)`。
- `selectSourceConnectionCodesByDimensionGroup` 不再过滤 `status = 'ACTIVE'`，用于删除旧投影时覆盖旧来源维度下的历史 connection。
- `ProductDistributionServiceImpl`：
  - 来源解绑时按当前绑定的来源维度 connection 集合删除投影。
  - 来源换绑或系统 SKU 变化时，先按旧绑定来源维度删除旧投影，再按新绑定来源维度 upsert 新投影。
  - 新建或同来源更新时，按当前来源维度 connection 集合删除后再 upsert，避免跨 connection 误删。
- `ProductDistributionMapperContractTest` 新增合同，固定：
  - 禁止回退到裸 `deleteUpstreamSkuPairingsBySystemSku`。
  - 删除必须包含 `connection_code in` 和 `collection="connectionCodes"`。
  - connection 解析不能只过滤 ACTIVE 来源明细。
- `react-ui/config/routes.ts` 和 `react-ui/config/routes.js` 补齐 `/product/distribution/create`、`/product/distribution/edit/:spuId` 静态 fallback 路由权限 guard，防止直达商品分销编辑页绕过 `RemoteMenuRouteGuard`。
- `docs/architecture/reuse-ledger.md` 增加上游 SKU 配对投影删除作用域模板。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dtest=ProductDistributionMapperContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`ProductDistributionMapperContractTest` 4 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：首次发现 `product-distribution-permission-guard.test.ts` 失败，原因是商品分销创建/编辑静态 fallback 路由缺权限 guard；补齐后重跑通过。前端 7 个 Jest suite / 33 个测试通过；后端 `ruoyi-system` 151、`ruoyi-framework` 15、`integration` 5、`product` 6、`seller` 94、`buyer` 95 个测试通过；`ruoyi-admin -am -DskipTests test-compile` reactor 编译通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; rg -n "[ \t]+$" ...`：无输出，本轮触碰文件未发现尾随空白。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；首次同步 2 个变更文件，Modified 2，共 2 个节点；写入记录后复跑结果为 `Already up to date`。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 当前残留项

- `product` 侧仍存在 mapper 直接读写 integration/source 表的债务；本轮只修跨 connection 删除 P0，长期仍建议将 `upstream_system_sku_pairing` 写入下沉到 integration 公开 facade。
- 前端 seller/buyer JS/TS 镜像等强校验、service 函数级 URL 合同和 guard manifest source-of-truth 可作为后续 P1 加固。
- direct-login 跨端失败审计和后台强退 actingAdmin 归属已有源码合同，后续可补运行态回归测试。
