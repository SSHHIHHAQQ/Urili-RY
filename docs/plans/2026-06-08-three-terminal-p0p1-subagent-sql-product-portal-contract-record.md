# 2026-06-08 三端隔离 P0/P1 子 Agent 收敛与合同收口记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

执行模式：快速推进，只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 执行

- 历史记录（已过期口径）：本轮当时按旧规则优先尝试 GPT-5.3 Codex；因本轮开始前同日已确认 `gpt-5.3-codex-spark` 额度限制，实际回退使用 6 个 `gpt-5.4` 只读子 Agent。当前现行规则已改为默认使用 `gpt-5.4`，除非用户在当前任务中重新明确要求，否则不要再把 GPT-5.3 Codex 作为首选。
- 6 个子 Agent 均已关闭，结论已由主 Agent 复核后吸收。
- 后端隔离切片：未发现 seller/buyer 端内权限继续复用 `sys_*`、裸 `accountId` 查询或跨端权限前缀的 P0/P1。
- 免密登录/401 切片：未发现免密票据串端消费、foreign audit 污染、401 后继续当成功结果处理的 P0/P1；记录 1 个 P2 可观测性问题。
- SQL guard 切片：发现 3 个 P1，分别是菜单 ID 重排 preview 未覆盖 `parent_id` 更新目标、商品状态迁移缺 exact target guard、确认过程合同泛匹配。
- React guard 切片：发现 1 个 P1，portal service 的 token/401 隔离测试覆盖不完整；记录若干 P2。
- product/inventory/integration 切片：发现 1 个 P1，product 直接写 `upstream_system_sku_pairing` 后未补齐来源库存刷新链路。
- 文档/manifest/pom 切片：发现文档口径 P1，目标追踪旧检查点仍可能误导当前密码重置语义；未发现 package/pom/manifest P0。

## 新增问题

- P1：`react-ui/src/services/portal/session.ts` 承载 portal 请求面，但原测试只覆盖少数调用点，不能防止未来请求漏掉端 token header 和 `isToken:false`。
- P1：`ProductDistributionServiceImpl` 写入/删除 `upstream_system_sku_pairing` 后，只刷新来源读模型和商品库存总览，未刷新 inventory SKU pairing 快照、来源仓库存读模型和来源库存总览。
- P1：`20260607_terminal_menu_id_range_isolation.sql` 的 expected target count/signature 只覆盖低位 menu ID，没有覆盖实际会更新的低位 `parent_id` 行。
- P1：`20260605_product_distribution_status_price_log.sql` 的历史 `DISABLED` 状态批量迁移缺少 exact target count/signature。
- P1：`SqlExecutionGuardContractTest` 原确认调用合同只泛匹配 `assert_*_confirmed()`，不能证明调用的是当前脚本自己声明的确认过程。
- P1：目标追踪顶部缺少当前口径索引，旧检查点中的 `resetDefaultPwd` / 默认密码重置表述可能被误读。

## 已修复问题

- `react-ui/tests/portal-session-request.test.ts` 新增表驱动合同，覆盖全部 portal 认证请求导出函数，统一断言端内 URL、端 token header、`isToken:false` 和 scope 参数剥离。
- `ISourceReadModelRefreshService` 新增 `refreshOfficialMasterSkuPairingByConnection(...)` facade。
- `SourceReadModelRefreshServiceImpl` 通过公开 facade 顺序刷新来源商品读模型、inventory SKU pairing 快照、来源仓库存读模型和来源库存总览。
- `ProductDistributionServiceImpl` 在来源绑定释放、投影同步后收集受影响 `connectionCode`，通过 integration facade 刷新来源库存链路。
- `InventoryOverviewRefreshContractTest` 增加 product -> integration facade 和来源库存刷新链路合同。
- `20260607_terminal_menu_id_range_isolation.sql` 将低位 `parent_id` 行纳入 seller/buyer menu exact target count/signature。
- `20260605_product_distribution_status_price_log.sql` 为 `product_spu` / `product_sku` 历史 `DISABLED` 批量更新增加 expected count/signature 和执行前断言。
- `SqlExecutionGuardContractTest` 收紧确认过程合同，要求 first DDL/DML 之前调用脚本内声明的 confirmed procedure。
- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 顶部新增现行口径索引，明确密码重置当前语义。
- `docs/architecture/reuse-ledger.md` 更新 product -> integration facade 复用规则。

## 权限检查结果

- 子 Agent 与本地验证未发现新的 seller/buyer 端内菜单、权限前缀、空 authority、portal 401 或免密串端 P0。
- 本轮未新增后端业务接口、菜单或按钮权限。
- SQL 修改仅增加 guard 和预览目标断言，不实际执行远程 DDL/DML。

## 字典与选项复用

- 本轮未新增字典、状态 code 或前端 option catalog。
- 未修改国家/地区、业务状态、币种等字典逻辑。

## 复用台账

- 已更新 `docs/architecture/reuse-ledger.md` 的 Product 到 Integration 公开 facade 边界模板。
- 后续跨模块写入或替换 `upstream_system_sku_pairing` 投影时，必须复用 `refreshOfficialMasterSkuPairingByConnection(...)` 或先扩展同一公开 facade。

## 大文件与重复代码

- 本轮未新增超过 300 行的代码文件。
- `portal-session-request.test.ts` 增加表驱动 case，但职责仍单一：验证 portal request 隔离。
- 未处理 P2 级 401 helper 双份实现和 guard 脚本重复描述，按快速模式记录不阻塞。

## 验证命令

- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/portal-session-request.test.ts --runInBand`：通过，1 个 suite / 26 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl inventory "-Dtest=InventoryOverviewRefreshContractTest" test`：通过，1 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，63 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl integration,inventory,product -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dtest=ProductDistributionServiceImplTest,ProductDistributionMapperContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，13 个 product 测试。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 14 个 Jest suite / 93 个测试、React typecheck、四个前端 guard、后端 reactor test-compile 和后端合同链路均通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，索引已是最新。
- `cd E:\Urili-Ruoyi; git diff --check`：通过，退出码 0；仅输出 LF/CRLF 提示。

## 未验证原因

- 未执行浏览器运行态、截图或 DOM 检测：用户已明确本阶段无需。
- 未执行远程 MySQL DDL/DML，未读取或写入 Redis：本轮只做代码、测试和 SQL guard 文件收口。
- 未启动或重启后端：本轮目标是代码级 P0/P1 收口，且全量三端验证已覆盖编译和合同。

## 当前残留

- P2：`Portal/DirectLogin` 请求级失败信息仍会折叠成通用文案，影响排障但不破坏隔离边界。
- P2：401 跳转逻辑在 `app.tsx` 与 `requestErrorConfig.ts` 两个入口重复实现，后续可抽共享 helper。
- P2：`portalRequest` absolute admin URL 负例仍可补更细单测。
- P2：`RemoteMenuRouteGuard` 静态回退约束在多个 guard 脚本里重复描述。
- P2：product mapper 直接写 integration 表仍是技术债；本轮已补刷新链路，后续应把 `upstream_system_sku_pairing` 写入收回 integration facade。
