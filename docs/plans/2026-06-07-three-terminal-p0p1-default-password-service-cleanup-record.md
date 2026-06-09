# 2026-06-07 三端 P0/P1 默认密码重置前端 Service 收口记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，继续按快速推进模式只修 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。本轮聚焦 seller/buyer 管理端前端 service 中未接通的“指定密码重置”导出；不做浏览器运行态验收、不做截图/DOM 检测、不做 UI 细调。

## 新增问题

- P1：管理端账号弹窗当前只接入默认密码 `U12346` 重置，但 seller/buyer 前端 service 仍导出未接通的指定密码重置函数，容易让后续复制模板时误以为 UI 仍支持自定义密码弹窗。

## 已修复问题

- 删除 `react-ui/src/services/seller/seller.ts` / `.js` 中未接通的 `resetAdminSellerAccountPassword(...)` 导出。
- 删除 `react-ui/src/services/buyer/buyer.ts` / `.js` 中未接通的 `resetAdminBuyerAccountPassword(...)` 导出。
- 历史记录（已过期口径）：当时保留 `resetAdminSellerAccountDefaultPassword(...)` / `resetAdminBuyerAccountDefaultPassword(...)`，当前 UI 继续只做默认密码重置。当前实现已由后续检查点覆盖：管理端账号“重置密码”必须人工输入 5-20 位临时密码并调用 `resetPwd`。
- 历史记录（已过期口径）：当时 `docs/architecture/reuse-ledger.md` 登记为不导出指定密码重置 service；当前实现已由后续检查点覆盖：seller/buyer 管理端账号重置密码已接入人工临时密码 `resetPwd`，默认密码重置入口已移除。
- `docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md` 已追加本检查点。

## 残留问题

- 历史记录（已过期口径）：当时后端仍保留指定密码重置 API 和 service 能力，前端只是移除未接通导出。当前实现已由后续检查点覆盖：该能力已作为管理端账号“重置密码”的人工临时密码入口接入。
- P1：端内 role-menu 当前已有本端存在性校验，但 `seller_menu` / `buyer_menu` ID 空间仍可能重叠；跨端提交同数字 ID 仍可能绑定成本端同号菜单，后续应做端内菜单 ID 段隔离或稳定 `businessKey` 方案。

## 验证命令

- `cd E:\Urili-Ruoyi; rg -n "resetAdminSellerAccountPassword|resetAdminBuyerAccountPassword" react-ui\src`：无命中，确认前端未接通导出已删除。
- `cd E:\Urili-Ruoyi\react-ui; npm exec tsc -- --noEmit`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node --check src\services\seller\seller.js; node --check src\services\buyer\buyer.js`：通过。

## 未验证原因

- 未做浏览器运行态验收、截图、DOM 检测：用户已明确当前快速推进模式无需浏览器验证。
- 未连接远程 MySQL / Redis，未执行 SQL，未写远程数据：本轮只改前端 service 导出和 Markdown 记录。

## 权限检查结果

- 本轮不新增后端接口、不新增菜单权限、不修改按钮权限。
- 历史记录（已过期口径）：当时管理端账号弹窗仍通过 `resetAccountDefaultPassword` 调默认密码接口。当前实现已由后续检查点覆盖：弹窗提交人工临时密码并调用 `resetPwd`，权限仍使用 `seller:admin:account:resetPwd` / `buyer:admin:account:resetPwd`。

## 字典/选项复用检查结果

- 本轮未新增字典类型、字典数据或前端选项。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记默认密码重置前端 service 口径。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；本轮最终同步输出 `Synced 5 changed files`，`Modified: 5 - 239 nodes in 996ms`。记录更新后复跑输出 `Already up to date`。

## 大文件合理性判断结果

- 本轮只删除 service 未使用导出，不触发大文件拆分判断。

## 重复代码检查结果

- 删除 seller/buyer 两侧同构未使用导出，减少重复残留。
- 没有新增重复业务逻辑。

## 子 Agent 使用记录

- 本轮实现基于前序已完成并关闭的 `gpt-5.4` 前端重置密码 service 子 Agent 只读结论；未额外开启重复扫描子 Agent。

## 一句话总结

历史记录（已过期口径）：当时 seller/buyer 管理端前端 service 已清掉未接通的指定密码重置导出，UI 契约只保留默认密码 `U12346` 重置。当前实现已由后续检查点覆盖：管理端账号“重置密码”使用人工临时密码 `resetPwd`。
