# 2026-06-08 三端隔离 P0/P1 收口记录：Product/Integration 边界与分页契约

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。

本轮执行模式：只修 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用

- 历史记录（已过期口径）：按最新要求优先尝试 GPT-5.3 Codex，工具模型 `gpt-5.3-codex-spark`。
- 平台返回 GPT-5.3 Codex 额度不可用，提示到 `2026-06-14 15:12` 后再试，失败 Agent 已关闭。
- 按 fallback 规则使用并关闭 6 个 `gpt-5.4` 子 Agent。
- 采纳的 P1：
  - product 模块仍通过 `ProductDistributionMapper` 直接读取 `source_product_dimension_group` / `source_product_warehouse_detail`。
  - `20260608_product_center_menu_seed.sql` 缺少专项 seed guard 合同。
  - `UpstreamSystem` 请求日志、`Finance/Currency` 三处列表、`Product/Attribute` 属性列表仍直接透传 ProTable `params`，缺 `current -> pageNum` 契约。

## 已完成

- 新增 integration-owned DTO：`SourceProductBindingSnapshot`。
- 扩展 `ISourceSkuPairingProjectionService`，新增 `selectOfficialSourceBindingSnapshot(...)` 只读端口。
- 将来源 SKU 快照 SQL 从 `ProductDistributionMapper.xml` 移到 `UpstreamSystemMapper.xml`。
- `ProductDistributionServiceImpl` 改为通过 integration port 获取来源 SKU 快照，再转成 product 内部绑定对象继续执行原业务校验。
- 删除 product mapper 的 `selectSourceBindingSnapshot(...)` 方法和 SQL。
- `ProductDistributionMapperContractTest` 移除 `selectSourceBindingSnapshot` 外部表 allowlist，并固定 product mapper 不得再出现 `source_product_dimension_group` / `source_product_warehouse_detail`。
- 前端 4 处 ProTable 请求入口统一把 `current/pageSize` 显式转成 `pageNum/pageSize`：
  - `UpstreamSystem/components/SyncTabs.tsx`
  - `Finance/Currency/index.tsx`
  - `Finance/Currency/components/SyncSettingsPanel.tsx`
  - `Product/Attribute/components/AttributeLibrary.tsx`
- 前端合同测试补充分页参数映射断言。
- `SqlExecutionGuardContractTest` 已补 `20260608_product_center_menu_seed.sql` 的 parent/slot/signature/completion guard 专项合同。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，72 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl integration,product -am "-Dtest=ProductDistributionMapperContractTest,ProductDistributionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，15 个测试。
- `cd E:\Urili-Ruoyi\react-ui; .\node_modules\.bin\jest.cmd --config .\jest.config.ts tests\upstream-system-permission-guard.test.ts tests\finance-currency-contract.test.ts tests\product-distribution-permission-guard.test.ts --runInBand`：通过，3 个 suite / 18 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dtest=ProductDistributionMapperContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，9 个测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：顺序复跑通过；前端 guard、React typecheck、20 个 Jest suite / 142 个测试、后端 reactor `test-compile`、后端三端合同均通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，退出码 0；仅输出当前工作区 LF/CRLF 换行提示。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 同步 18 个变更文件。

## 说明

- 第一次完整 `verify:three-terminal` 中 product 合同测试曾出现一次失败；随后单独复跑 product 合同通过，再顺序复跑完整 `verify:three-terminal` 通过。按最终顺序复跑结果判断，本轮没有残留 P0/P1 阻塞。
- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 当前残留项

- P2：direct-login 页 token timeout 失败回传可以更快，目前管理端仍会通过自身 bridge 超时收敛。
- P2：`app.tsx` 与 `requestErrorConfig.ts` 的 401 分流逻辑仍有重复，当前行为一致，后续可抽共享 helper。
- P2：管理端审计查询的 accountId + subjectId fail-closed 主要靠运行时服务层守卫，后续可增加更强的架构合同。
- P2：`verify-three-terminal` 仍允许纯 re-export 的 `.test.js` twin 存在，后续可以统一清理生成副本。
