# 2026-06-09 三端分离进度代码审查

- 审查时间：2026-06-09 23:40，Asia/Shanghai
- 审查对象：`E:\Urili-Ruoyi` 当前工作区、2026-06-04 以来提交、三端相关计划/报告、关键后端/前端合同与验证入口
- 审查模式：代码审查与进度评估；未执行远端 MySQL DDL/DML，未读取或写入 Redis，未启动或重启后端，未做浏览器/截图/DOM 验收
- 参考方向：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`

## 1. 审查结论

### P0/P1 结论

- 本轮未发现新的三端账号权限 P0/P1 阻塞项。
- 当前三端账号权限框架整体约 `90%`。这个百分比只覆盖账号、角色、菜单、部门、日志、会话、免密代入、权限 gate、SQL guard 和当前 `react-ui` 验证入口，不等同于“三个物理前端 + 完整 seller/buyer 业务菜单 + 全业务上线”已经完成。
- 如果按“可交付三端 MVP”口径，也就是完成三端物理前端拆分、运行态验收和提交/上线整理，整体约 `68%-72%`。
- 如果把商品、库存、订单、履约、财务、外部系统等完整业务菜单也算进“三端分离完成”，当前只能算约 `40%-45%`，因为这些属于后续业务域建设，不是当前账号权限框架的完成条件。

### 主要风险

1. `P2` 当前脏改动明显夹带管理端商品审核和商品列表性能/展示修复，容易稀释三端框架收口。当前要尽快开发出来，建议先冻结三端框架 P0/P1，只保留能影响编译、guard、接口、权限、串端、service/字段缺失的改动。
2. `P2` 大文件继续膨胀：`ProductReviewServiceImpl.java` 约 1751 行、`ProductReviewBusinessPreview.tsx` 约 2010 行、`Product/Review/index.tsx` 约 1548 行、`PartnerManagementPage.tsx` 约 1125 行。当前不建议立刻重构阻塞 P0/P1，但验收后应拆分，否则后续审核、权限、UI 改动会越来越慢。
3. `P2` 当前测试 gate 很强，但本轮没有做浏览器级实际登录、免密代入、强退、日志查看和菜单渲染验收；代码级通过不能替代最终运行态验收。
4. `P2` `git status` 仍有未提交文件和未跟踪样稿/报告，例如 `react-ui/public/prototypes/product-review-audit-template.html`。提交前必须确认保留范围，继续排除生成目录、缓存、测试结果和本机索引。
5. `P2` 仍有若干尾债：admin/portal 401 分流 helper 双处维护、端内菜单表结构硬约束偏弱、Partner 部门/角色/菜单弹窗契约覆盖不足、`ProductReviewMapper` 新 SQL 结构合同可增强、Jest open handle 提示未清理。

## 2. 当前已完成的工作

| 方向 | 当前状态 | 估算完成度 | 说明 |
| --- | --- | ---: | --- |
| 总体架构方向和规则 | 基本完成 | 95% | 已明确 admin 保留若依 `sys_*`，seller/buyer 独立账号、角色、菜单、部门、日志、会话；旧“复用 sys_user”方向已废弃。 |
| 后端 seller/buyer 模块拆分 | 基本完成 | 90% | 已有独立 `seller` / `buyer` Maven 模块，主账号、子账号、角色、菜单、部门、会话、日志和管理端控制接口已形成同构样板。 |
| 端内账号查询作用域 | 基本完成 | 92% | 当前账号查询保持 `sellerId/buyerId + accountId` 双参数，未发现生产入口裸 `select*AccountById(accountId)`。 |
| Portal 鉴权、会话、日志 | 基本完成 | 90% | 端内 protected endpoint 以 `@PortalPreAuthorize` + `@PortalLog` + `PortalSessionContext.requireSession(...)` 为核心合同。 |
| 管理端控制权 | 基本完成 | 88% | 已覆盖账号状态、锁定/解锁、人工临时密码重置、会话查看、强制踢出、免密代入、登录/操作/票据审计。 |
| 免密代入 | 基本完成 | 90% | 票据短时一次性、Redis key 带 terminal、acting admin、reason、失败审计和跨端拒绝均有代码/测试合同。 |
| SQL migration / seed guard | 接近完成 | 85% | 高影响脚本已有 confirm token、`45000` fail-closed、端内菜单 ID 段、password 非空等合同；表结构硬约束和部分 SQL 完成态合同仍可增强。 |
| React 管理端 seller/buyer 控制面 | 接近完成 | 85% | 管理端 Seller/Buyer 复用 `PartnerManagementPage` 模板，session 参数面已收窄到分页字段。 |
| React portal 最小端入口 | 可用骨架 | 75% | 登录、直登、portal home 骨架已完成；商品/分类样板属于超范围样板，不计入当前账号权限框架完成度；还未拆成物理 `seller-ui` / `buyer-ui`，端内完整业务菜单也未铺开。 |
| 三端验证 gate | 基本完成 | 92% | `verify-three-terminal` 已从 manifest 读取 guard、前端测试、后端合同和 reactor 模块，关键测试漏登记会 fail-closed。 |
| 文档/台账 | 接近完成 | 88% | 计划、执行记录、六小时审查、复用台账较完整；追加式记录较多，后续需要保留“最新口径索引”避免误读旧状态。 |

## 3. 关键证据

- 端内 portal endpoint 已按当前端推导 session，例如 `SellerPortalController` 的用户信息、菜单、资料、账号、日志、会话入口都使用 `@Anonymous` + `@PortalPreAuthorize` + `@PortalLog` + `PortalSessionContext.requireSession("seller")`。
- 前端 portal 请求已剥离可注入范围字段：`react-ui/src/services/portal/session.ts` 中 `PORTAL_SCOPE_PARAM_KEYS` 覆盖 `sellerId`、`buyerId`、`subjectId`、`accountId`、`sellerAccountId`、`buyerAccountId`、`terminal`，并由 `sanitizePortalQueryParams(...)` 过滤。
- 管理端 session 列表参数已收窄：`react-ui/src/services/seller-buyer/sessionParams.ts` 只保留 `pageNum/pageSize`，避免前端误传 `subjectId/accountId/ipaddr/tokenId` 影响范围。
- 管理端查看会话和强退权限已经拆开：`AdminSellerController` 中 session list 用 `seller:admin:session:list`，强退用 `seller:admin:forceLogout`。
- 强退审计会按在线 session 逐条保留 direct-login 上下文，并把本次后台操作人覆盖为当前 admin；无在线 session 时也会写失败/控制日志。
- `PortalDirectLoginSupport` 生成票据时写入 terminal、target subject/account、acting admin、reason 和 tokenHash，Redis payload key 使用 terminal + token hash。
- `PortalAnonymousEndpointContractTest` 会扫描 seller/buyer portal controller，固定 portal endpoint 必须声明端边界注解、不能接收前端身份范围参数、不能回退到若依后台登录上下文。
- `verify-three-terminal.mjs` 已覆盖前端 guard、前端 Jest 路径、后端合同类、explicit frontend critical path 和 explicit backend critical class；本处为 2026-06-09 23:40 快照，最新数量以 `react-ui/tests/three-terminal.manifest.json` 和后续验证检查点为准。

## 4. 本轮现场验证

本轮实际执行并通过：

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run verify:three-terminal
```

结果：

- 前端 guard：5 个通过。
- React typecheck：通过。
- 前端 Jest：24 suites / 196 tests 通过。
- 后端 reactor `test-compile`：14 个模块 SUCCESS。
- 后端三端合同：BUILD SUCCESS。
- 重点后端测试计数：product 64 tests、seller 100 tests、buyer 101 tests 通过。
- 备注：Jest 仍提示 open handle，但退出码为 0；作为 P2 测试清理债，不阻塞当前 P0/P1。

本轮也执行：

```powershell
cd E:\Urili-Ruoyi
git diff --check
```

结果：通过；仅有当前工作区 LF/CRLF 换行转换 warning，无 whitespace error。

本轮未执行：

- 未执行远端 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动、停止或重启后端。
- 未做浏览器、截图、DOM、UI 细调验收。
- 已运行 `codegraph sync .`；结果为 `Already up to date`。

## 5. 当前未提交工作区判断

当前工作区不是干净状态：

- `git diff --shortstat`：38 个已跟踪文件变更，约 3800 行新增、485 行删除。
- 未跟踪项包括：
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/domain/ProductReviewListDisplayItem.java`
  - `RuoYi-Vue/product/src/main/java/com/ruoyi/product/domain/ProductReviewTypeCount.java`
  - `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/SecretCipherSupportTest.java`
  - `docs/plans/2026-06-09-product-code-pool-job-db-execution-record.md`
  - `docs/plans/2026-06-09-product-review-list-performance-investigation.md`
  - `docs/reports/2026-06-09-portal-auth-session-direct-login-readonly-review.md`
  - `docs/reviews/2026-06-09-portal-auth-session-direct-login-readonly-review-codex.md`
  - `react-ui/public/prototypes/product-review-audit-template.html`
  - `react-ui/src/services/seller-buyer/`

判断：

- 三端主线没有看到新的 P0/P1 回退。
- 当前脏改动中较多是商品审核、Product Review 列表/预览、SecretCipherSupport 配置和记录口径修正；这些不应继续扩大三端账号权限框架完成度。
- 提交前建议拆成至少两组：三端框架/guard/记录收口一组，商品审核/商品列表性能与预览一组。这样后续回滚和审查会更清楚。

## 6. 剩余任务与工期估算

### 如果目标是“当前三端账号权限框架收口”

预计剩余 `6-10` 小时。

| 小任务 | 估算 | 目标 |
| --- | ---: | --- |
| 当前 dirty 范围清点与拆提交 | 1-2 小时 | 排除本地生成物、样稿按需确认，避免把相邻商品改动混进三端框架提交。 |
| 记录层与复用台账最终整理 | 1-2 小时 | 让最新口径覆盖旧追加式记录，避免后续 agent 误读已关闭 P1。 |
| P2 中最接近 P1 的 guard 补强 | 2-3 小时 | 优先补 seller/buyer controller 权限契约、`ProductReviewMapper` SQL 结构合同、Partner 弹窗最小契约。 |
| 提交前验证 | 1-2 小时 | 复跑 `npm run verify:three-terminal`、`git diff --check`、`codegraph sync .`，必要时补 targeted Maven/Jest。 |

### 如果目标是“可运行的三端 MVP”

预计剩余 `5-7` 个工作日。

| 阶段 | 估算 | 内容 |
| --- | ---: | --- |
| A. 当前框架收口 | 0.5-1 天 | 清理 dirty worktree、拆提交、跑完整 gate、冻结 P0/P1。 |
| B. 运行态验收 | 0.5-1.5 天 | 读取当前激活数据源配置；启动/重启后端；admin 登录；seller/buyer 登录、免密代入、强退、日志、菜单、401 分流验证。 |
| C. 物理前端拆分 | 2-3 天 | 从 `react-ui` 拆出 `seller-ui` / `buyer-ui`，复制并裁剪 portal 入口、proxy、env、build、权限/路由 guard、登录/直登页。 |
| D. 三端 UI 骨架验收 | 1 天 | admin / seller / buyer 三个前端分别跑 typecheck、关键 Jest、浏览器 smoke；确认菜单和 token 不串端。 |
| E. P2 hardening | 1 天 | 统一 401 helper、补 DTO/Controller 合同、菜单表结构或 seed 完成态 guard 决策、大文件拆分计划。 |
| F. 发布整理 | 0.5 天 | SQL 执行/回滚记录、上线步骤、账号样例、权限矩阵、最终验收报告。 |

### 如果目标包含完整业务菜单

预计还需要 `2-4` 周，且必须按业务域另行设计，不建议和当前账号权限框架混在一个大任务里推进。

最低还要覆盖：

- seller/buyer 端商品、库存、订单、履约、财务的端内菜单和页面。
- 端内业务接口全部从 portal session 推导主体范围。
- 每个业务域的表设计、权限点、审计日志、导入导出、回滚方案。
- 浏览器级验收和真实数据源验收。

## 7. 建议

1. 先把“当前三端账号权限框架”定义为短期完成目标，不要把商品审核 UI、库存策略、WMS 同步继续混进三端主线。否则每天看起来都在推进，但三端收口会被业务细节拖慢。
2. 现在可以进入“提交前收敛”阶段：清 dirty、拆提交、跑总 gate、写最终阶段记录。只要没有新 P0/P1，就不要继续扩功能。
3. 物理拆分 `seller-ui` / `buyer-ui` 应该作为下一阶段第一件事。继续长期在一个 `react-ui` 里模拟三端，会让 token、proxy、路由、菜单缓存和权限 guard 的运行态风险越来越难判断。
4. 大文件拆分放在框架验收后做。建议先拆商品审核前端预览组件、审核 action/hooks、审核列表 columns/service adapter；后端再拆审核快照、差异计算、审核生效、列表投影四块。
5. 三端验收标准建议固定为：代码 gate 通过、admin/seller/buyer 三端浏览器 smoke 通过、远端数据源影响记录明确、菜单和按钮权限正/负向可验证、免密代入和强退审计可追踪、最终 Markdown 验收报告齐全。
