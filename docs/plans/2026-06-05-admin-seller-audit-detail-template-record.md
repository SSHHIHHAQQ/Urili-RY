# 管理端卖家审计详情模板执行记录

日期：2026-06-05

## 背景

当前开发方向以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为准：管理端保留若依后台控制权，卖家端和买家端账号、权限、日志、会话独立。

前序切片已经完成免密票据 `tokenHash` 响应脱敏和 `PortalDirectLoginSupport` 生命周期测试。本轮只处理一类问题：管理端卖家审计弹窗详情字段补齐。后端接口、权限点、SQL 和买家业务 service 均不在本轮修改范围。

## 本次改动

- 修改 `react-ui/src/components/PartnerManagement/PartnerAuditModal.tsx`。
- 在登录日志 tab 增加展开行详情：
  - 登录地点
  - 浏览器
  - 操作系统
  - 登录提示
- 在操作日志 tab 增加展开行详情：
  - 请求地址
  - 操作 IP
  - 操作地点
  - 方法名
  - 异常信息
- 在免密票据 tab 增加展开行详情：
  - 目标端
  - 签发人 ID
  - 使用 IP
  - 创建人
  - 更新人
  - 更新时间
  - 代入原因
  - 备注
- 主表列不继续加宽，详情字段通过 Ant Design `Descriptions` 展示，减少审计弹窗横向挤压。
- 更新 `docs/architecture/reuse-ledger.md` 的 `PartnerAuditModal` 条目。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npx biome lint src/components/PartnerManagement/PartnerAuditModal.tsx
npm run tsc
```

验证结果：

- `npx biome lint src/components/PartnerManagement/PartnerAuditModal.tsx`：通过。
- `npm run tsc`：通过。

## 浏览器验证

数据源确认：

- 后端激活配置为 `druid`。
- MySQL 连接通过 `RUOYI_DB_URL`、`RUOYI_DB_USERNAME`、`RUOYI_DB_PASSWORD` 运行变量读取。
- Redis 连接通过 `RUOYI_REDIS_*` 运行变量读取。
- 本轮未输出 `.env.local` 凭证，未执行 SQL。

浏览器步骤：

- 使用 Playwright CLI 打开 `http://127.0.0.1:8001/partner/seller`。
- 使用默认账号 `admin / admin123` 登录。
- 打开卖家管理工具栏“审计”弹窗。
- 切换到“免密票据”tab，展开第一行。
- 切换到“操作日志”tab，展开第一行。
- 检查浏览器 console error。

浏览器结果：

- 卖家审计弹窗正常打开。
- “免密票据”展开行可见 `目标端`、`签发人ID`、`使用IP`、`创建人`、`更新人`、`更新时间`、`代入原因`、`备注`。
- “操作日志”展开行可见 `请求地址`、`操作IP`、`操作地点`、`方法名`、`异常信息`。
- Playwright console 检查：`Errors: 0`。

## 未验证原因

- 本轮没有复制或单独打开买家管理审计弹窗；该组件为 seller/buyer 共用组件，买家侧后续按同构入口单独验收。
- 本轮不做低权限账号负向验证。
- 本轮不做端内直登浏览器消费流程验收。

## 权限检查结果

- 本轮不新增接口和权限点。
- 审计弹窗 tab 显隐仍由 `seller:admin:loginLog:list`、`seller:admin:operLog:list`、`seller:admin:ticket:list` 等既有权限控制。
- 买家侧继续使用对应 `buyer:admin:*` 权限控制。

## 字典/选项复用检查结果

- 本轮未新增字典或选项。
- 登录状态、操作状态、票据状态继续复用 `PartnerAuditModal` 内既有 `valueEnum`。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md` 的 `PartnerAuditModal` 条目。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 格式检查结果

- `cd E:\Urili-Ruoyi; git diff --check -- react-ui/src/components/PartnerManagement/PartnerAuditModal.tsx docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-05-admin-seller-audit-detail-template-record.md`：通过，仅有 LF/CRLF 归一化提示。

## 大文件合理性判断结果

- `PartnerAuditModal.tsx` 当前约 435 行，已超过 400 行自检阈值，因此明确判断是否拆分。
- 本轮只增加详情渲染函数和 `ProTable` 展开行配置，职责仍集中在只读审计弹窗。
- 本轮暂不拆分，避免在一个已稳定的共用弹窗上引入额外组件边界；后续如果继续增加导出、详情抽屉、更多审计 tab 或写操作，再优先拆出审计 tab/详情组件。

## 重复代码检查结果

- 没有为 seller/buyer 分别复制审计弹窗。
- 登录日志、操作日志、免密票据详情均复用同一个 `renderDetailText(...)` 文本渲染方法。

## 残留问题

- 低权限账号下 direct-login / ticket 审计入口隐藏和后端拒绝仍需单独验证。
- 端内直登入口的浏览器消费流程仍需单独验收。
