# 2026-06-05 管理端卖家低权限免密代入负向验收记录

## 背景

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，只做管理端卖家低权限负向验收。

目标是证明低权限管理端账号可以进入卖家列表和普通审计，但不能执行免密代入，也不能查看免密票据审计。

## 数据源与影响范围

- 数据源确认：后端激活 profile 为 `druid`；MySQL / Redis 均通过 `RUOYI_*` 运行变量读取，本记录不输出 `.env.local` 凭证。
- 后端运行环境：`http://127.0.0.1:8080`。
- 前端运行环境：`http://127.0.0.1:8001`。
- 本轮通过若依管理端 API 创建或更新测试角色和测试用户，没有手工执行 SQL。
- 测试角色：`roleId=102`，`roleKey=codex_seller_audit_only`。
- 测试用户：`userId=111`，`userName=codex_seller_lowperm`。
- 测试用户密码仅用于本轮验收，未写入本记录。

## 测试角色权限

允许权限：

- `seller:admin:list`
- `seller:admin:query`
- `seller:admin:loginLog:list`
- `seller:admin:operLog:list`

禁止权限：

- `seller:admin:directLogin`
- `seller:admin:ticket:list`

## 后端接口验收

使用 `codex_seller_lowperm` 登录后读取 `/getInfo`：

- 权限数量：`4`
- `seller:admin:list`：存在
- `seller:admin:directLogin`：不存在
- `seller:admin:ticket:list`：不存在

允许接口：

- `GET /seller/admin/sellers/list?pageNum=1&pageSize=1`：业务 `code=200`
- `GET /seller/admin/sellers/loginLogs/list?pageNum=1&pageSize=1`：业务 `code=200`
- `GET /seller/admin/sellers/operLogs/list?pageNum=1&pageSize=1`：业务 `code=200`

拒绝接口：

- `POST /seller/admin/sellers/{sellerId}/directLogin`：HTTP `200`，业务 `code=403`
- `GET /seller/admin/sellers/directLoginTickets/list?pageNum=1&pageSize=1`：HTTP `200`，业务 `code=403`

## 浏览器验收

使用 Playwright CLI 通过真实登录页登录 `codex_seller_lowperm`，进入卖家管理：

- `/partner/seller` 可打开。
- 卖家列表可见，表格行数 `3`。
- 首行“更多”菜单只出现 `审计`，没有 `登录卖家端` / `directLogin`。
- 点击工具栏 `审计` 后，弹窗只展示 `登录日志` 和 `操作日志`。
- `免密票据` tab 不存在。
- 管理端 token 存在；`seller_access_token` 不存在；`buyer_access_token` 不存在。
- 截图：`output/playwright/seller-lowperm-negative.png`。

## 验证命令

- `Invoke-RestMethod` 后端接口脚本：通过，确认测试角色、测试用户、允许接口和拒绝接口。
- `npx --yes --package @playwright/cli playwright-cli -s=urili-seller-low ...`：通过，确认按钮和 tab 显隐。
- `Get-Item output\playwright\seller-lowperm-negative.png`：通过，截图大小 `92058` bytes。

## 残留问题

- Playwright console 有 2 条无关错误：`CategoryAttributeTemplate.css` 缺失，来源为商品属性组件热更新，不是本次卖家权限页面或接口触发。
- 首次低权限登录后动态菜单需要刷新后才稳定显示左侧菜单；刷新后菜单可展开并进入卖家管理。本轮没有改这个前端路由时序问题，后续应单独切片处理。
- 本轮只做卖家模板；买家低权限负向验收尚未复制。

## 当前判断

- 卖家低权限管理端负向验收模板已跑通：普通列表和普通审计可用，免密代入和免密票据审计在后端被拒绝，在前端不可见。
- 后续复制买家时只替换 terminal、权限前缀、路由和 service，不重新设计低权限验收逻辑。
