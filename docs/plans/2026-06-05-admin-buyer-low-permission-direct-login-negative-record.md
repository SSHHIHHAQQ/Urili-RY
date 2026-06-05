# 2026-06-05 管理端买家低权限免密代入负向验收记录

## 背景

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，在卖家低权限负向验收通过后，只按同一模板复制买家侧验收。

目标是证明低权限管理端账号可以进入买家列表和普通审计，但不能执行免密代入，也不能查看免密票据审计。

## 数据源与影响范围

- 数据源确认：后端激活 profile 为 `druid`；MySQL / Redis 均通过 `RUOYI_*` 运行变量读取，本记录不输出 `.env.local` 凭证。
- 后端运行环境：`http://127.0.0.1:8080`。
- 前端运行环境：`http://127.0.0.1:8001`。
- 本轮通过若依管理端 API 创建或更新测试角色和测试用户，没有手工执行 SQL。
- 测试角色：`roleId=103`，`roleKey=codex_buyer_audit_only`。
- 测试用户：`userId=112`，`userName=codex_buyer_lowperm`。
- 测试用户密码仅用于本轮验收，未写入本记录。
- 角色名使用 ASCII：`Codex Buyer Audit Only`。原因是运行库里中文测试角色名显示存在转码冲突，继续使用中文名会与卖家测试角色名冲突。

## 测试角色权限

允许权限：

- `buyer:admin:list`
- `buyer:admin:query`
- `buyer:admin:loginLog:list`
- `buyer:admin:operLog:list`

禁止权限：

- `buyer:admin:directLogin`
- `buyer:admin:ticket:list`

## 后端接口验收

使用 `codex_buyer_lowperm` 登录后读取 `/getInfo`：

- 权限数量：`4`
- `buyer:admin:list`：存在
- `buyer:admin:directLogin`：不存在
- `buyer:admin:ticket:list`：不存在

允许接口：

- `GET /buyer/admin/buyers/list?pageNum=1&pageSize=1`：业务 `code=200`
- `GET /buyer/admin/buyers/loginLogs/list?pageNum=1&pageSize=1`：业务 `code=200`
- `GET /buyer/admin/buyers/operLogs/list?pageNum=1&pageSize=1`：业务 `code=200`

拒绝接口：

- `POST /buyer/admin/buyers/{buyerId}/directLogin`：HTTP `200`，业务 `code=403`
- `GET /buyer/admin/buyers/directLoginTickets/list?pageNum=1&pageSize=1`：HTTP `200`，业务 `code=403`

## 浏览器验收

使用 Playwright CLI 通过真实登录页登录 `codex_buyer_lowperm`，进入买家管理：

- `/partner/buyer` 可打开。
- 买家列表可见，表格行数 `1`。
- 首行“更多”菜单只出现 `审计`，没有 `登录买家端` / `directLogin`。
- 点击工具栏 `审计` 后，弹窗只展示 `登录日志` 和 `操作日志`。
- `免密票据` tab 不存在。
- 管理端 token 存在；`seller_access_token` 不存在；`buyer_access_token` 不存在。
- 截图：`output/playwright/buyer-lowperm-negative.png`。

## 验证命令

- `Invoke-RestMethod` 后端接口脚本：通过，确认测试角色、测试用户、允许接口和拒绝接口。
- `npx --yes --package @playwright/cli playwright-cli -s=urili-buyer-low ...`：通过，确认按钮和 tab 显隐。
- `npx --yes --package @playwright/cli playwright-cli -s=urili-buyer-low console error`：通过，`Errors: 0`，`Warnings: 0`。
- `Get-Item output\playwright\buyer-lowperm-negative.png`：通过，截图大小 `110909` bytes。

## 当前判断

- 买家低权限管理端负向验收已按卖家模板复制通过：普通列表和普通审计可用，免密代入和免密票据审计在后端被拒绝，在前端不可见。
- seller/buyer 两端低权限免密代入负向验收现在均有运行时证据。
- 本轮没有改业务代码、没有改表结构、没有手工执行 SQL。
