# 三端 P0/P1 验证入口 Reactor 编译门与收窄发现记录

日期：2026-06-07

## 目标

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，在快速推进模式下只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。

本切片专门收口 `react-ui/scripts/verify-three-terminal.mjs` 的验证范围漂移：

- 后端不能只跑当前显式测试模块而漏掉 `ruoyi-admin` 依赖链编译。
- 后端关键测试发现不能用裸 `Seller|Buyer` 误拦普通业务单测。
- 前端三端验证入口不能扫描整个 `react-ui` 后要求所有 Jest 都进入 `frontendTestPaths`。
- 显式列入清单的后端测试仍必须产出 surefire report，避免空跑。

## 子 Agent

本切片实际并行使用 6 个只读子 Agent，模型为 `gpt-5.4`，分别审计前端 Jest 发现范围、后端 matcher、后端 reactor 编译门、surefire report、验证命令和 Markdown 同步范围。

6 个子 Agent 均未修改文件，均已关闭。最新用户规则已确认：后续新建子 Agent 优先使用 `gpt-5.3-codex-spark`，不可用或受限时再回退 `gpt-5.4`。

## 已完成

- `backendReportModules` 改为从 `RuoYi-Vue/pom.xml` 动态读取 reactor 模块。
- 新增后端编译门：`mvn -pl ruoyi-admin -am -DskipTests test-compile`。
- 后端关键测试自动发现移除裸 `Seller|Buyer`，改为三端、Portal、权限、DirectLogin、SQL Guard、菜单、日志、会话等关键家族。
- 当前 seller/buyer 服务类关键测试通过显式集合保留在 `backendTestClasses` 中，不依赖宽泛类名匹配。
- 前端测试发现范围收窄到 `react-ui/tests`。
- 前端只对 terminal、portal、partner、remote-menu、direct-login、unauthorized、redirect、three-terminal 等关键测试文件 fail-closed。
- `frontendTestPaths` 中配置的文件不存在时直接失败。
- `docs/architecture/reuse-ledger.md` 已同步新口径：全 reactor 编译门 + 关键测试显式清单 + 收窄发现。

## 验证

- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
- 前端 guard：portal token、partner management、seller portal product、buyer portal product 均通过。
- React typecheck：通过。
- 前端 Jest：6 个 suite / 30 个测试通过。
- 后端 reactor 编译门：`mvn -pl ruoyi-admin -am -DskipTests test-compile` 通过，覆盖 `ruoyi-admin` 及其依赖链。
- 后端三端合同测试：`ruoyi-system` 136 个、`ruoyi-framework` 15 个、`product` 1 个、`seller` 91 个、`buyer` 92 个测试通过。
- `three-terminal verification passed.` 已输出。

## 未执行

- 未执行数据库 DDL/DML。
- 未读取或写入远程 MySQL / Redis。
- 未做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## CodeGraph

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 1 changed files`、`Modified: 1 - 28 nodes`。

## 结论

当前三端快速验证入口已从“全项目测试发现”收口为“后端全 reactor 编译门 + 三端关键测试显式清单 + 前端关键目录收窄发现”。旧记录中的“扫描所有后端测试源码 / 扫描整个 react-ui 测试文件”的口径不再代表当前规则。
