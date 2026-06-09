# 2026-06-07 三端 P0/P1：角色与部门隔离测试收口记录

## 参考方向

- 参考方案：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`
- 当前模式：只处理 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失。
- 明确不做：浏览器运行态验收、截图、DOM 检测、UI 细调。

## 子 Agent 使用情况

- 历史记录（已过期口径）：按用户要求优先尝试 GPT-5.3 Codex；本轮 6 个 `gpt-5.3-codex-spark` 子 Agent 均因额度限制失败并已关闭，平台提示恢复时间为 `2026-06-13 01:59`。
- 降级启动 6 个 `gpt-5.4` 只读扫描 Agent，切片覆盖 seller 部门/角色、buyer 部门/角色、`roleMenuTreeselect` checkedKeys、旧 DDL、旧 `sys_menu` seed、当前串端风险复核；6 个有效子 Agent 均已完成并关闭。
- 已采纳 P1：seller/buyer 部门实体自身跨主体写入/删除缺少运行时测试，OWNER 角色禁停用/禁删除缺少测试，`roleMenuTreeselect` checkedKeys 主体隔离应显式测试。
- 记录为后续 P1：旧 DDL 可重放性、旧 `sys_menu` seed slot/signature guard、来源仓库库存读模型半实现与未确认 DDL 边界。

## 已完成

- `SellerPortalPermissionServiceImplTest` 增加：
  - OWNER 角色禁停用测试，断言 `updateSellerRoleStatus` 不会执行。
  - OWNER 角色禁删除测试，断言 `countSellerAccountRoleByRoleId`、`deleteSellerRoleMenuByRoleId`、`deleteSellerRoleById` 不会执行。
  - `selectMenuIdsByRoleId(...)` 正向测试，断言 checkedKeys 查询带入 `sellerId + roleId`。
  - `selectMenuIdsByRoleId(...)` 错主体角色负向测试，断言不会继续查询 checkedKeys。
- `BuyerPortalPermissionServiceImplTest` 按卖家模板机械复制，替换 buyer service/mapper/字段命名。
- 新增 `SellerPortalDeptServiceImplTest`，覆盖：
  - 更新不属于当前 seller 的部门时 fail-closed，且不执行 update/ancestor/delete。
  - 更新时选择不属于当前 seller 的父部门时 fail-closed。
  - 新增时选择不属于当前 seller 的父部门时 fail-closed。
  - 删除不属于当前 seller 的部门时 fail-closed，且不执行 hasChild、checkAccount、delete。
- 新增 `BuyerPortalDeptServiceImplTest`，按卖家模板机械复制，替换 buyer service/mapper/字段命名。
- `verify-three-terminal.mjs` backend 清单加入 `SellerPortalDeptServiceImplTest` 与 `BuyerPortalDeptServiceImplTest`，保持新增关键测试不被总入口静默漏跑。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller "-Dtest=SellerPortalPermissionServiceImplTest,SellerPortalDeptServiceImplTest" test`：失败；单模块未带 `-am`，读取到本地旧 `ruoyi-system` 依赖，导致既有 `SellerServiceImplTest` 中 `PortalLoginLog` 免密字段方法解析失败。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer "-Dtest=BuyerPortalPermissionServiceImplTest,BuyerPortalDeptServiceImplTest" test`：同上失败，原因是单模块依赖未联动编译。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerPortalPermissionServiceImplTest,SellerPortalDeptServiceImplTest" test`：失败；reactor 依赖模块没有匹配的指定测试，Surefire 报 `No tests matching pattern`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerPortalPermissionServiceImplTest,BuyerPortalDeptServiceImplTest" test`：同上失败。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerPortalPermissionServiceImplTest,SellerPortalDeptServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，seller 新增/相关测试 `16` 个通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerPortalPermissionServiceImplTest,BuyerPortalDeptServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，buyer 新增/相关测试 `16` 个通过。
- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：第一次失败，自动发现新增 `SellerPortalDeptServiceImplTest` / `BuyerPortalDeptServiceImplTest` 未加入清单；补清单后通过。前端 `3` 个 suite / `9` 个测试通过，后端三端契约 ruoyi-system `99`、ruoyi-framework `15`、product `1`、seller `80`、buyer `81` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl integration -am -DskipTests compile`：通过；用于确认当前工作区中来源仓库库存相关既有差异不阻塞编译。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；首次同步返回 `Synced 13 changed files`，`Added: 4, Modified: 9 - 931 nodes in 1.9s`。

## 边界说明

- 本检查点未执行远程 MySQL DDL/DML。
- 本检查点未读取或写入 Redis。
- 本检查点未启动或重启后端。
- 本检查点未做浏览器、截图、DOM 或 UI 细调验收。
- 本检查点未修改来源仓库库存读模型代码；该方向已有未确认方案 `docs/plans/2026-06-07-source-warehouse-stock-grouped-display-plan.md`，且方案明确未确认前不新增表、Mapper、Service、Controller 实现。当前工作区中已存在的相关半实现不在本三端账号权限切片内继续扩展。

## 残留 P1

- 旧 DDL 脚本仍存在裸 `ALTER TABLE ADD COLUMN` / 裸 `CREATE INDEX` 可重放风险，需要按脚本单独收口。
- 旧 `sys_menu` seed 仍存在缺少 slot/signature guard 的 P1，需要按菜单脚本分批收口。
- 来源仓库库存分组读模型当前方案未确认，工作区已有半实现痕迹；后续必须先确认 DDL/读模型方案，再补 Mapper XML、DDL、service 运行时验证和 Markdown 执行记录。
