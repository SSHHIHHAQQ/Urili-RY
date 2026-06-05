# 2026-06-05 卖家账号锁定低权限负向验收记录

## 背景

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，接在卖家账号锁定/解锁模板之后，只验证一类权限边界：管理端低权限账号可以查看卖家账号列表，但没有 `seller:admin:account:lock` 时不能锁定或解锁卖家账号。

本轮不复制买家，不新增表，不修改业务代码，不修改端内账号、端内角色、端内菜单、日志或会话模型。

## 数据源与影响范围

- 后端运行环境：`http://127.0.0.1:8080`。
- 前端运行环境：`http://127.0.0.1:8001`。
- 本轮通过若依管理端 API 创建临时管理端角色和临时管理端账号，没有手工执行 SQL。
- 临时角色：`codex_lock_negative`。
- 临时账号：`codex_lock_limited`。
- 临时账号密码仅用于本轮验收，未写入记录。
- 验收完成后已退出临时账号会话，并删除临时账号和临时角色。

## 测试角色权限

允许权限：

- `seller:admin:list`
- `seller:admin:query`
- `seller:admin:account:list`

禁止权限：

- `seller:admin:account:lock`
- `seller:admin:account:add`
- `seller:admin:account:edit`
- `seller:admin:account:resetPwd`
- `seller:admin:account:role:query`
- `seller:admin:account:role:edit`
- `seller:admin:directLogin`
- `seller:admin:forceLogout`

实际绑定菜单 ID：

- `2010`
- `2011`
- `2200`
- `2310`

被排除的锁定权限菜单 ID：

- `2322`

## 后端接口验收

使用 `codex_lock_limited` 登录后读取 `/getInfo`：

- 权限返回：`seller:admin:list,seller:admin:account:list,seller:admin:query`。
- `seller:admin:account:list`：存在。
- `seller:admin:account:lock`：不存在。

允许接口：

- `GET /seller/admin/sellers/list?pageNum=1&pageSize=1`：业务 `code=200`。
- `GET /seller/admin/sellers/{sellerId}/accounts`：业务 `code=200`。

拒绝接口：

- `PUT /seller/admin/sellers/{sellerId}/accounts/{accountId}/lock`：业务 `code=403`。
- `PUT /seller/admin/sellers/{sellerId}/accounts/{accountId}/unlock`：业务 `code=403`。

状态核验：

- 验收账号：`sellerId=9`，`accountId=8`。
- 请求前 `lock_status=0`，`lock_reason` 为空。
- 低权限锁定/解锁请求后，`lock_status` 和 `lock_reason` 均未变化。

## 浏览器验收

使用 Playwright CLI 通过真实登录页登录 `codex_lock_limited`，进入卖家管理并打开卖家账号弹窗：

- `/partner/seller` 可打开。
- 卖家列表行内“账号”入口可见，数量 `3`。
- 账号弹窗可打开。
- 账号弹窗展示“锁定”状态列。
- 账号弹窗不展示“锁定账号”或“解锁账号”操作。
- 账号弹窗行内“更多”按钮数量 `0`，说明没有可执行的账号操作入口。
- 管理端 token 存在。
- `seller_access_token=false`。
- `buyer_access_token=false`。
- 截图：`react-ui/output/playwright/seller-lock-lowperm-negative.png`，文件大小 `55735` bytes。

本轮没有读取 Playwright console 明细，因为当前登录页源码会输出登录响应，可能包含 token；本记录不把 console 明细作为权限验收证据。

## 清理结果

- 临时账号 UI 会话 `/logout` 返回业务 `code=200`。
- 清理后 `codex_lock_limited` 用户剩余 `0`。
- 清理后 `codex_lock_negative` 角色剩余 `0`。

## 验证命令

```powershell
cd E:\Urili-Ruoyi
# 后端接口验收：通过 Invoke-RestMethod 创建临时角色/账号，验证允许接口、拒绝接口和状态不变，随后清理。

cd E:\Urili-Ruoyi\react-ui
npx --yes --package @playwright/cli playwright-cli -s=urili-lock-neg open about:blank
npx --yes --package @playwright/cli playwright-cli -s=urili-lock-neg run-code --filename <temp-js-file>
npx --yes --package @playwright/cli playwright-cli -s=urili-lock-neg close

cd E:\Urili-Ruoyi
# 清理核验：通过管理端 API 查询 codex_lock_limited 和 codex_lock_negative，均为 0。
codegraph sync .
```

## CodeGraph 更新结果

- 已执行 `cd E:\Urili-Ruoyi; codegraph sync .`。
- 首次同步结果：`Synced 3 changed files`，命令退出码 `0`。
- 补记 CodeGraph 结果后复跑：`Synced 1 changed files`，命令退出码 `0`。

## 当前判断

- 卖家账号锁定/解锁模板的低权限边界已通过真实账号接口验收和真实浏览器验收。
- 该低权限账号拥有账号列表权限但没有锁定权限，因此本轮证明的是“能看账号，但不能锁定/解锁”，不是靠隐藏整个账号入口绕过验证。
- 买家锁定/解锁仍未复制，继续等待卖家模板验收后再按同构配置复制。
