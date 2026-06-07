# 2026-06-07 三端 P0/P1 快速推进：Guard 命令绑定与显式强退运行时证明记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 本轮边界

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器、截图、DOM 或 UI 细调验收。
- 本轮不改 seller/buyer 强退生产逻辑，只补运行时测试证明。

## 子 Agent 执行情况

- 本轮开始时已有 6 个 `gpt-5.4` 只读子 Agent 在跑 P0/P1 扫描；已全部关闭。
- 子 Agent 结论：
  - SQL/seed、portal 自助接口、token/session、模块边界未发现可落地 P0/P1。
  - 前端 guard/manifest 面发现 1 个 P1：manifest 只绑定脚本名，未绑定命令文本，也未反向校验所有 `guard:*` 是否入清单。
  - direct-login/session/audit 面发现 1 个 P1：显式强退成功路径缺少正向运行时测试，不能直接卡住 `ticketId/reason` 保留、`actingAdmin*` 归当前后台管理员和端内 token 删除。
- 用户补充模型要求后，新开 1 个 `gpt-5.3-codex-spark` 只读 fixture 子 Agent；平台返回用量限制，提示需等到 `2026-06-08 01:14` 后再试，已关闭。
- 按 fallback 规则降级新开 1 个 `gpt-5.4` 只读 fixture 子 Agent；已关闭，结论与本轮补测试方式一致。

## 新增问题

- P1：`react-ui/tests/three-terminal.manifest.json` 中 `frontendGuardScripts` 只登记脚本名，不能防止 package script 被改成 no-op 或新增 `guard:*` 后未登记。
- P1：seller/buyer 显式强退成功路径缺少运行时正向测试；已有实现看起来正确，但此前主要靠源码合同和密码重置强退路径间接覆盖。

## 已修复问题

- `react-ui/tests/three-terminal.manifest.json`
  - `frontendGuardScripts` 从字符串数组升级为 `{ name, expectedCommand }`。
- `react-ui/scripts/verify-three-terminal.mjs`
  - 校验 `package.json.scripts[name] === expectedCommand`。
  - 反向扫描 `package.json` 中所有 `guard:*`，要求全部登记到 manifest。
  - 继续从 manifest 执行 guard、前端测试和后端三端合同测试。
- `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java`
  - 新增 `forceLogoutSellerSessionsAuditsDirectLoginSessionsWithCurrentAdminAndDeletesSellerTokens`。
  - 新增 `forceLogoutSellerAccountSessionsAuditsDirectLoginSessionsWithCurrentAdminAndDeletesSellerTokens`。
  - 两条测试均断言普通 session 和 direct-login session 分别写日志，direct-login 保留 `ticketId/reason`，`actingAdmin*` 覆盖为当前后台管理员，Redis token 只按 `seller` terminal 删除。
- `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`
  - 按卖家模板机械复制 buyer 显式强退主体范围和账号范围正向测试。
- `docs/architecture/reuse-ledger.md`
  - 修正强退 direct-login 审计口径：保留 `directLogin/ticketId/reason`，`actingAdmin*` 归当前后台管理员。
  - 补充前端 guard manifest 必须绑定 `expectedCommand` 和反向登记 `guard:*` 的复用规则。

## 验证命令

- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；`SellerServiceImplTest` 54 个测试通过，`BuyerServiceImplTest` 54 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 7 个 Jest suite / 33 个测试通过；后端 `ruoyi-system` 151、`ruoyi-framework` 15、`integration` 5、`product` 6、`seller` 96、`buyer` 97 个测试通过；`ruoyi-admin -am -DskipTests test-compile` reactor 编译通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; rg -n "[ \t]+$" ...`：无输出，本轮触碰文件未发现尾随空白。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 3 个变更文件，Modified 3，共 475 个节点。

## 权限与数据边界

- 本轮未新增后端接口或权限标识。
- 本轮未修改数据库结构、SQL seed、Redis key 或远端数据。
- 强退仍复用既有管理端强退入口和端内 token 删除能力；本轮只补测试证明。
- 已更新 `docs/architecture/reuse-ledger.md` 和本记录；CodeGraph 已同步。

## 残留项

- seller/buyer 模块仍模块级依赖 `ruoyi-system`；当前未发现生产代码引入 `sys_user/sys_role/sys_menu/sys_dept` 控制面，后续可补 import-level contract 作为 P2 加固。
- 未来如继续新增 `guard:*` 脚本，必须同步写入 manifest 的 `name + expectedCommand`。
