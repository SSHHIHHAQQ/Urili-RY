# 2026-06-08 三端 P0/P1：Portal Path、SQL Guard 与验证闸门记录

参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`。

本轮继续按快速推进口径执行：只处理 P0/P1，也就是编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 情况

- 本轮修复收口使用的是前序已经启动并关闭的 6 个 `gpt-5.4` 只读子 Agent 结果，覆盖 seller/buyer 后端、Portal 鉴权与 direct-login、SQL guard、React guard、product/inventory/integration/warehouse、验证闸门。
- 历史记录（已过期口径）：用户已重新确认后续子 Agent 模型优先级：优先使用 GPT-5.3 Codex，工具模型 `gpt-5.3-codex-spark`；不可用、额度限制或上下文失败时，再回退 `gpt-5.4`。
- 用户确认后本轮未再新建子 Agent；前序 6 个子 Agent 均已关闭。

## 采纳的 P1

- Portal 路径判断仍把 `/seller/login/*`、`/seller/direct-login/*` 这类子路径识别为端内路径，可能让非预期 redirect 被当成 portal 合法回跳。
- SQL 高影响脚本自动发现漏掉 standalone / dynamic `drop index`。
- 端内菜单自动发现只覆盖 `insert into seller_menu/buyer_menu`，没有覆盖 `update`、`delete` 对端内菜单的变更。
- 三端验证闸门的关键后端测试路径没有覆盖完整 `product/src/test/java` 与 `warehouse/src/test/java`，验证脚本自身也缺少动态 reactor 模块和 `-am` 接线合同。

## 已完成

- `react-ui/src/utils/portalPaths.ts` 收窄 `isPortalTerminalPath(...)`：
  - `/seller/login`、`/buyer/login` 只允许精确匹配。
  - `/seller/direct-login`、`/buyer/direct-login` 只允许精确匹配。
  - 只有 `/seller/portal/**`、`/buyer/portal/**` 允许子路径。
- `portal-session-request.test.ts` 增加 `/seller/login/next` 与 `/seller/direct-login/next` 的负例合同。
- `SqlExecutionGuardContractTest` 把 `drop index` 纳入高影响 SQL 自动发现，包括 dynamic DDL helper 场景。
- `SqlExecutionGuardContractTest` 将端内菜单发现从 insert 扩展到 insert/update/delete，并要求普通端内菜单 mutation 仍有当前端 permission slot guard。
- 对 `20260607_terminal_menu_id_range_isolation.sql` 和 `20260608_terminal_menu_auto_increment_reset.sql` 做精确文件级例外：这两个是 ID range / auto_increment 维护脚本，已有专门合同覆盖，不按权限 seed 处理。
- `verify-three-terminal.mjs` 的关键后端测试路径纳入完整 product 与 warehouse 测试目录。
- `verify-three-terminal-backend-gate.test.ts` 固定 product / warehouse 自动发现、动态 backend reactor modules 读取和 Maven `-am` 接线。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/portal-session-request.test.ts tests/verify-three-terminal-backend-gate.test.ts --runInBand`：通过，2 个 suite / 30 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`：通过，67 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，seller 54 个测试、buyer 54 个测试。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- `mvn -pl seller,buyer "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" test` 不带 `-am` 会使用本地仓库里的旧依赖并暴露无关编译错误；当前以 `-am` 作为三端快速验证的正确入口。

## 当前残留项

- P2：端内账号用户名当前仍是端级唯一查询；如果后续改为主体内唯一，需要重新设计登录入口和唯一索引。
- P2：`verify-three-terminal` 对纯 re-export 的 `.test.js` mirror 仍允许存在，后续可以统一清理生成副本。
