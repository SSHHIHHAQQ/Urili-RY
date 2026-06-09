# 2026-06-07 三端 P0/P1 快速推进：端内菜单 Fail-Closed 记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按当前快速推进模式只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调；本轮未执行远程 MySQL / Redis DDL/DML。

## 子 Agent 使用情况

- 历史记录（已过期口径）：用户最新指定：子 Agent 优先使用 GPT-5.3 Codex，工具模型为 `gpt-5.3-codex-spark`；不可用时降级 `gpt-5.4`。
- 本轮先尝试 GPT-5.3 Codex，平台返回用量/可用性限制，提示恢复时间为 `2026-06-13 01:59`；失败子 Agent 已关闭。
- 实际降级使用并关闭 6 个 `gpt-5.4` 只读子 Agent，切片覆盖 seller 后端、buyer 后端、React 管理模板、portal endpoint、SQL/seed、验证脚本。

## 新增问题

- P1：`seller_menu` / `buyer_menu` 的页面或按钮菜单允许空 `perms` 时，前端动态路由 `RemoteMenuRouteGuard` 会把空 authority 当作允许，形成 fail-open。
- P1：页面菜单 `component` 允许为空或不带当前端根路径时，会回退到共享占位页或可能加载非本端页面，端内组件边界不够 fail-closed。
- P1：`check-portal-token-isolation.mjs` 原先只检查 `config/proxy.ts`，未检查 `proxy.js`；也未显式检查 `access.js` 的三端 token key 镜像。

## 已修复问题

- `PortalPermissionSupport.normalizeTerminalMenu(...)` 增加端内菜单写入校验：
  - 页面菜单 `C` 和按钮菜单 `F` 的 `perms` 必填。
  - `perms` 必须使用当前端前缀 `seller:` / `buyer:`。
  - 禁止 `*` 通配权限。
  - 禁止 `seller:admin:` / `buyer:admin:` 管理端命名空间写入端内菜单。
  - 页面菜单 `C` 的 `component` 必填，并要求使用当前端页面根路径。
- seller / buyer 管理端菜单写入 service 继续复用同一套 `normalizeTerminalMenu(...)`，卖家标准模板已机械复制到买家。
- `PartnerMenuModal.tsx` 前端校验同步收紧：
  - 页面组件不能为空，且必须使用当前端根。
  - 页面/按钮权限不能为空。
  - 多权限逗号分隔时逐个校验前缀、通配符和 admin namespace。
- `RemoteMenuRouteGuard` 改为空 authority 拒绝访问，不再把空权限视为允许。
- `check-partner-management-template.mjs` 增加端内菜单 fail-closed 和空 authority guard 断言。
- `check-portal-token-isolation.mjs` 同时检查 `proxy.ts` / `proxy.js`，并显式检查 `access.ts` / `access.js` 的三端 token key 与清理函数。
- `AGENTS.md` 已补充端内菜单 fail-closed 和远程菜单空 authority 规则。

## 残留问题

- 本切片未改 SQL seed / legacy helper；SQL 子 Agent 提到的高影响脚本治理项不在本切片落地，继续按 SQL guard 队列处理。
- 本切片未做浏览器运行态验收、截图、DOM 检测或 UI 细调，这是用户明确要求的快速推进边界。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=SellerPortalPermissionServiceImplMenuTreeTest,BuyerPortalPermissionServiceImplMenuTreeTest,PortalPermissionSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`PortalPermissionSupportTest` 4 个用例、seller 菜单 service 6 个用例、buyer 菜单 service 6 个用例通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts/check-portal-token-isolation.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/remote-menu-route-guard.test.ts --runInBand`：通过，1 个 suite、3 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过，输出 `three-terminal verification passed.`；前端 4 个 test suite、12 个测试通过；后端 ruoyi-system 102、ruoyi-framework 15、product 1、seller 83、buyer 84 个测试通过。

## 未验证原因

- 未执行浏览器运行态验收、截图和 DOM 检测：用户已明确当前快速推进模式无需浏览器、截图、DOM 或 UI 细调。
- 未连接远程 MySQL / Redis：本切片不执行 SQL 和远程数据变更。

## 权限检查结果

- 页面/按钮菜单权限由后端写入层和前端弹窗双层校验。
- 空 authority 的动态或静态远程菜单路由现在返回 403，不再放行。
- `roleMenuTreeselect` 后端权限闭包已要求同时具备 `role:query` 和 `menu:query`，并纳入 seller/buyer 权限合同测试。

## 字典/选项复用检查结果

- 本切片未新增字典、业务选项或 code/label 映射。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，新增端内菜单 fail-closed 与远程菜单空 authority guard 模板。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 14 changed files`。

## 大文件合理性判断结果

- `PartnerMenuModal.tsx`、`check-partner-management-template.mjs`、`PortalPermissionSupport.java` 只补 guard 逻辑，不在本轮做结构拆分。
- seller/buyer 菜单 service 测试文件已有一定规模，但本轮只补对称断言和小 helper；拆分测试会引入额外结构性改动，不符合当前 P0/P1 快速模式。

## 重复代码检查结果

- 后端端内菜单校验集中在 `PortalPermissionSupport`，seller/buyer service 共享，不复制业务规则。
- 前端同构管理模板继续由 `PartnerMenuModal` 和 `check-partner-management-template.mjs` 统一约束，未分别为 seller/buyer 写两套校验逻辑。
