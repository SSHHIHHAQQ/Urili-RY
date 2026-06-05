# 2026-06-05 管理端免密代入权限契约守卫记录

## 背景

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只处理管理端免密代入和免密票据审计的权限契约守卫。

本轮不改数据库、不执行 SQL、不改接口业务逻辑、不改页面交互，只补自动化架构测试和记录。

## 已完成

- 新增 `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/AdminDirectLoginPermissionContractTest.java`。
- 锁定卖家/买家管理端主体级免密代入接口必须使用 `seller:admin:directLogin` / `buyer:admin:directLogin`。
- 锁定卖家/买家管理端账号级免密代入接口必须使用 `seller:admin:directLogin` / `buyer:admin:directLogin`。
- 锁定卖家/买家免密票据审计列表接口必须使用 `seller:admin:ticket:list` / `buyer:admin:ticket:list`。
- 校验免密代入接口必须声明管理端 `@Log`，避免生成短时票据但没有后台操作日志。
- 校验 `RuoYi-Vue/sql/seller_buyer_management_seed.sql` 必须包含四个权限点。
- 校验共享前端模板必须按 `directLogin` 和 `ticket:list` 权限控制主体行入口、账号行入口、全局审计入口和免密票据 tab。
- 更新 `docs/architecture/reuse-ledger.md`，登记该权限守卫的复用边界。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=AdminDirectLoginPermissionContractTest test`：首次失败，原因是测试从 `RuoYi-Vue/ruoyi-system` 运行时只向上查找到 `RuoYi-Vue`，没有继续定位仓库根目录，已修正测试自身路径定位。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=AdminDirectLoginPermissionContractTest test`：通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system test`：通过，`Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`。

## 当前判断

- 卖家标准模板当前已具备自动化权限契约守卫：免密代入不是普通查询/编辑权限，免密票据审计不是普通审计入口的无条件内容。
- 买家侧使用同构 controller 和共享前端模板，当前已被同一个测试一并守住；后续复制买家时只替换 terminal、路径、权限前缀和 service，不重新设计权限模型。
- 本测试是静态契约守卫，不替代真实低权限账号浏览器负向验收。后续如需要证明“低权限账号看不到按钮且接口返回无权限”，仍需创建或复用低权限管理端角色做运行时验收。

## 未触碰范围

- 未连接远程 MySQL / Redis。
- 未执行 DDL / DML。
- 未启动或重启后端。
- 未修改 seller/buyer 业务 service。
- 未修改 React 页面布局。
