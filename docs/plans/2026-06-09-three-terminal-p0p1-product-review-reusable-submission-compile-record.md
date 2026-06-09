# 三端隔离 P0/P1 商品审核复用提交编译修复记录

时间：2026-06-09 01:20，本机 `Asia/Shanghai`。

## 参考方向

- 主参考：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。
- 执行边界：进入快速推进模式，只处理 P0/P1，即编译、guard、接口、权限、串端、service/字段缺失。
- 跳过项：不做浏览器运行态验收，不做截图、DOM 检测和 UI 细调。

## 子 Agent 使用记录

- 实际模型：`gpt-5.4`。
- 数量：6 个。
- 状态：全部关闭。
- 结论：未发现新的可坐实 P0/P1。
- 记录但不阻塞的 P2：
  - 未使用的 swagger 示例 client 仍有不带 `/api` 的示例路径，当前未发现真实引用。
  - 部分非关键 JS/TS companion 仍是双份实现，后续可收敛为 re-export wrapper。
  - direct-login timeout 可以更快主动回传 opener，目前仍可通过管理端自身 bridge timeout 收敛。
  - `portal.*.web.url` seed 对已有错误值只做缺失占位，后续可增加更强校验。
  - 商品共享域仍可补更强架构合同，避免前端价格计算等 P2 漂移。

## 本轮 P0/P1

### P0：商品审核 service 编译断口

现象：

- `node scripts\verify-three-terminal.mjs` 在后端 reactor `test-compile` 阶段失败。
- 编译错误为 `ProductReviewServiceImpl` 未实现 `IProductReviewService.selectLatestRejectedReusableSubmission(Long)`。

处理：

- `RuoYi-Vue/product/src/main/java/com/ruoyi/product/service/impl/ProductReviewServiceImpl.java`
  - 实现 `selectLatestRejectedReusableSubmission(Long spuId)`。
  - 按 `spuId` 查询最近一次可复用的驳回审核。
  - 从 AFTER SPU 快照恢复 `ProductSpu`。
  - 从 AFTER SKU 快照恢复 `ProductSku` 列表并挂回商品。
  - 快照缺失时 fail-safe 返回 `null`，快照 JSON 解析失败时抛 `ServiceException("审核快照解析失败")`。
- `RuoYi-Vue/product/src/test/java/com/ruoyi/product/service/impl/ProductReviewServiceImplTest.java`
  - 新增复用提交恢复测试，覆盖 AFTER SPU 和 AFTER SKU 快照恢复。

### P0：商品分发测试夹具缺少审核 mapper

现象：

- 编译断口修复后，完整三端验证进入后端三端合同测试时失败。
- `ProductDistributionServiceImplTest.selectProductByIdWithSellerScopePushesSellerIdToProductAndSkuQueries` 抛 NPE。
- 根因是 `ProductDistributionServiceImpl` 已依赖 `ProductReviewMapper` 填充最新审核摘要，但测试夹具未注入该 mapper。

处理：

- `RuoYi-Vue/product/src/test/java/com/ruoyi/product/service/impl/ProductDistributionServiceImplTest.java`
  - 为测试 service 注入最小 `ProductReviewMapper` 代理。
  - `selectLatestReviewsBySpuIds` 默认返回空列表，保持旧用例的业务事实为“无最新审核记录”。
  - `countPendingReviewByKey` 默认返回 0，避免测试夹具在无关路径上被待审检测打断。

## 数据源和远端影响

- 本轮没有执行 DDL。
- 本轮没有执行 DML。
- 本轮没有读取或写入远端 MySQL。
- 本轮没有读取或写入远端 Redis。
- 本轮没有启动或重启后端服务。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dtest=ProductReviewServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，`ProductReviewServiceImplTest` 10 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dtest=ProductDistributionServiceImplTest,ProductReviewServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过，17 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs`
  - portal token guard 通过。
  - partner management guard 通过。
  - seller/buyer portal product guard 通过。
  - product upstream mirrors guard 通过。
  - React typecheck 通过。
  - portal session Jest 通过，22 个 suites / 161 个 tests。
  - 后端 reactor `test-compile` 通过。
  - 后端三端合同测试通过，system 200 个、framework 16 个、finance 9 个、inventory 1 个、integration 7 个、product 37 个、seller 97 个、buyer 98 个测试通过。
  - 总结果：`three-terminal verification passed`。
- `cd E:\Urili-Ruoyi; git diff --check`
  - 通过，退出码 0。
  - 仅输出当前工作区已有的 LF/CRLF 换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`
  - 通过。
  - 首次同步输出 `Synced 13 changed files`。
  - 记录补齐后最终复跑输出 `Already up to date`。

## 当前判断

- 本轮只修复商品审核复用提交和测试夹具的 P0/P1 缺口，没有改变三端账号、角色、菜单、部门或 token 隔离方案。
- 管理端仍保持若依 `sys_*` 控制面，seller/buyer 端内体系仍独立。
- P2 项已记录，不阻塞当前快速推进。
