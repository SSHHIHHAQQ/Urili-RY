# 2026-06-07 三端独立 P0/P1 integration 管理端权限合同与 surefire 覆盖记录

## 目标

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，在快速推进模式下只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本轮目标是收口 `integration` 模块只有 reactor compile 覆盖、没有模块内 surefire report 的缺口。

## 本轮范围

- 不做浏览器运行态、截图、DOM 检测或 UI 细调。
- 不执行远程 MySQL DDL/DML。
- 不读取或写入 Redis。
- 不启动或重启后端。
- 本切片范围较小，未启动子 Agent；后续需要子 Agent 时按最新规则优先使用 GPT-5.3 Codex，不可用再回退 `gpt-5.4`。

## 已完成

- `RuoYi-Vue/integration/pom.xml` 增加 JUnit4 测试依赖，保持与 seller/buyer/product/finance 等已有测试模块一致。
- 新增 `RuoYi-Vue/integration/src/test/java/com/ruoyi/integration/architecture/IntegrationAdminPermissionContractTest.java`：
  - 固定 integration 管理端 controller 必须走 `/integration/admin/**`。
  - 禁止 integration 管理端 controller 使用 anonymous、portal、seller 或 buyer 端权限面。
  - 固定 mutating handler 必须有 `@Log`。
  - 固定 `AdminUpstreamSystemController` 精确权限与同步类型二次权限检查。
  - 固定来源商品库和来源仓库库存只读接口的当前管理端菜单权限。
  - 固定对应权限必须能在现有 seed SQL 中找到。
- `react-ui/scripts/verify-three-terminal.mjs` 将 `IntegrationAdminPermissionContractTest` 纳入后端合同清单，确保总验证入口必须执行 integration 模块测试并检查 surefire XML。
- `docs/architecture/reuse-ledger.md` 记录 integration 管理端权限合同复用规则。
- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 增加本轮检查点。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl integration -am "-Dtest=IntegrationAdminPermissionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
  - `IntegrationAdminPermissionContractTest`：4 个测试通过，0 失败，0 跳过。
- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过。
  - 前端 guard、TypeScript、6 个 Jest suite / 30 个测试通过。
  - 后端 reactor test-compile 通过。
  - 后端三端合同通过，integration 模块生成并校验 `TEST-com.ruoyi.integration.architecture.IntegrationAdminPermissionContractTest.xml`。

## 残留

- 本轮未发现新的 P0/P1 残留。
- 后续 integration 新增或改造管理端接口时，必须同步 controller 权限、seed SQL、前端权限 gate、`IntegrationAdminPermissionContractTest` 和 `verify-three-terminal` 清单。
