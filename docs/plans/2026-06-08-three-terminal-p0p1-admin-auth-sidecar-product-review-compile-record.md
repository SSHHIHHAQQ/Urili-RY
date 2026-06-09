# 2026-06-08 三端独立 P0/P1 Admin Auth Sidecar 与 Product Review 编译收口记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 执行情况

- 历史记录（已过期口径）：按当时用户要求先尝试 6 个 GPT-5.3 Codex 子 Agent（`gpt-5.3-codex-spark`）；现行规则为默认使用 `gpt-5.4`。
- 平台返回额度限制，提示需要等到 2026-06-14 15:12 后再试；失败子 Agent 已关闭。
- 回退使用 6 个 `gpt-5.4` 只读子 Agent，覆盖 seller/buyer 后端隔离、portal auth/session/direct-login/log、SQL guard、React runtime guard、product/inventory/integration、验证 manifest 六个切片。
- 6 个 `gpt-5.4` 子 Agent 均已关闭。
- 采纳 1 个 P1：admin 登录/会话相关源码目录 `.js` sidecar 是独立实现，未被三端验证门禁约束，可能和 TS/TSX 主实现漂移。
- 记录但不阻塞的 P2：direct-login 子页 token 等待 5 秒与 opener bridge 15 秒超时不一致；`ProductDistributionMapper.xml` 仍有合同允许的 product 直接读 integration read model 技术债。

## 已完成

- 将以下 admin auth/runtime sidecar 改为纯 re-export，避免运行入口绕过 TS/TSX 主实现：
  - `react-ui/src/utils/initialStateModel.js`
  - `react-ui/src/pages/User/Login/index.js`
  - `react-ui/src/components/RightContent/AvatarDropdown.js`
  - `react-ui/src/pages/Welcome.js`
  - `react-ui/src/components/index.js`
- 新增 `react-ui/tests/admin-auth-sidecar-contract.test.ts`，固定上述 sidecar 必须保持纯 re-export。
- 将 `admin-auth-sidecar-contract.test.ts` 纳入 `react-ui/tests/three-terminal.manifest.json`。
- 更新 `react-ui/scripts/verify-three-terminal.mjs` 的关键前端测试发现正则，使 `auth-sidecar` 类测试不能从 manifest 漂出。
- 完整验证时暴露后端 P0：`ProductReviewServiceImpl` 调用了编辑审核相关方法但当前源码缺失/残缺，导致 product 模块编译失败。
- 收敛 `ProductReviewServiceImpl` 的编辑审核实现：提交编辑审核时写 before/after 快照；审批时校验正式商品快照未变化，再通过 `IProductDistributionService.applyReviewedProductUpdate(...)` 应用变更，未绕过商品服务直接写 Mapper。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; npx jest --config jest.config.ts --runTestsByPath tests/admin-auth-sidecar-contract.test.ts --runInBand`：通过，1 个 suite / 5 个测试。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dtest=ProductReviewServiceImplTest" "-DfailIfNoTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`ProductReviewServiceImplTest` 4 个测试通过，product 模块编译通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 15 个 Jest suite / 101 个测试、React typecheck、4 个前端 guard、后端 reactor test-compile 和后端三端合同测试均通过。

## 未执行

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图、DOM 或 UI 细调验收。

## 残留

- P2：direct-login 子页 token 等待 5 秒与 opener bridge 15 秒超时不一致，可后续统一。
- P2：`ProductDistributionMapper.xml` 仍有合同允许的 product 直接读 integration read model 技术债，本轮不处理。
