# 2026-06-07 三端 P0/P1：余额/充值占位边界收口记录

## 参考方向

- 参考方案：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`
- 当前模式：只处理 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失。
- 明确不做：浏览器运行态验收、截图、DOM 检测、UI 细调。

## 子 Agent 使用情况

- 按用户要求优先尝试 GPT-5.3 Codex；本轮 6 个 `gpt-5.3-codex-spark` 子 Agent 均因额度限制失败并已关闭，平台提示恢复时间为 `2026-06-13 01:59`。
- 降级启动 6 个 `gpt-5.4` 只读扫描 Agent，并已全部关闭。
- 已采纳 P1：余额占位不能以 `USD 0.00` 形式返回、不能参与 `balanceMin/balanceMax` 查询、不能透传到 seller/buyer 端内 profile，买家充值列不能表现成可操作动作。
- 记录为后续 P1：部门/角色运行时隔离测试缺口、旧 DDL 可重放性、旧 `sys_menu` seed slot/signature guard。

## 已完成

- 后端 seller/buyer 管理端 mapper 不再投影 `account_balance` / `balance_currency`，也不再在 resultMap 中映射 `accountBalance` / `balanceCurrency`。
- 后端 seller/buyer 管理端 mapper 删除 `params.balanceMin` / `params.balanceMax` 查询条件，避免用常量 `0.00` 做假余额筛选。
- `PartnerProfile` 移除 `accountBalance` / `balanceCurrency`，管理端列表和详情不再把占位余额作为主体资料字段返回。
- `PortalSubjectProfile` 移除 `accountBalance` / `balanceCurrency`，seller/buyer 端内 `/profile` 不再透传占位余额。
- seller/buyer portal controller 的 `buildProfile(...)` 不再设置余额字段。
- 前端 `PartnerManagementPage` 删除余额区间查询控件，不再向后端发送 `params[balanceMin]` / `params[balanceMax]`；旧浏览器缓存里如果仍有这些字段，也会在 `buildListParams(...)` 中删除。
- 前端余额列保留为明确占位展示，只显示“待接入 / 占位”，不再格式化成 `USD 0.00`。
- 买家充值列改为“充值能力 / 规划中”，保留占位字段但不表现成可操作充值动作。
- seller/buyer 管理页 `searchFieldCount` 从 `8` 调整为 `7`，匹配移除余额筛选后的实际筛选字段数量。
- `AdminAccountPermissionUiContractTest` 增加余额/充值占位边界静态契约，覆盖 `.tsx` 与 `.js` sidecar、mapper 和 profile DTO。
- `PortalHomeProfileSerializationTest` 增加 profile 不序列化 `accountBalance` / `balanceCurrency` 的断言。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=AdminAccountPermissionUiContractTest,PortalHomeProfileSerializationTest test`：第一次 PowerShell 未给 `-Dtest` 加引号，逗号被解析导致命令未执行；加引号后通过，`6` 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run guard:partner-management`：通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am -DskipTests compile`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：第一次在 `tsc` 阶段发现 `Space` import 被误删；修复后通过。前端 `3` 个 suite / `9` 个测试通过，后端三端契约 ruoyi-system `99`、ruoyi-framework `15`、product `1`、seller `72`、buyer `73` 个测试通过。

## 边界说明

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。
- 本检查点未做浏览器、截图、DOM 或 UI 细调验收。
- 真实余额、充值、结算仍未设计和落库，后续必须单独走 finance 设计方案，不复用主体资料表或 profile DTO 承载资金事实。

## 残留 P1

- 部门树跨主体写入/删除、`roleMenuTreeselect` checkedKeys 主体隔离、OWNER 角色禁停用/禁删除仍需补运行时隔离契约测试。
- 旧 DDL 脚本仍存在裸 `ALTER TABLE ADD COLUMN` / 裸 `CREATE INDEX` 可重放风险，需要按脚本单独收口。
- 旧 `sys_menu` seed 仍存在缺少 slot/signature guard 的 P1，需要按菜单脚本分批收口。
