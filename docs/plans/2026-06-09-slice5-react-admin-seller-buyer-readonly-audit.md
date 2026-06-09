# 切片 5 只读审计：React 管理端 seller/buyer 管理页、service、路由、权限按钮

日期：2026-06-09

## 审计范围

- 目标文档：`docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`
- 切片范围：React 管理端 `seller` / `buyer` 管理页、service、路由、权限按钮
- 快速推进口径：只查 P0 / P1（编译、guard、接口、权限、串端、service / 字段缺失）
- 约束：只读审计，不改文件

## 结论

本切片未发现 P0 / P1 问题。

当前实现满足本轮关注点：

- 卖家页与买家页都复用同一个 `PartnerManagementPage` 模板，买家页没有串用 seller service。
- buyer 页是基于 seller 页的配置化复制，核心差异集中在 `moduleKey`、文案、字段名、权限前缀、service 绑定和路由入口。
- `resetPwd`、`directLogin`、`session/list`、`forceLogout`、`account role` 的前端按钮权限与后端接口 `@PreAuthorize` 一致，没有发现“前端显示但后端 403”或“前端串用另一端权限”的 P0 / P1 缺口。
- seller / buyer 路由入口各自绑定本端 authority，没有发现 buyer 页面落到 seller 组件或 seller authority 的问题。

## P0 / P1 清单

无。

## 依据

### 1. seller / buyer 页面是同一模板驱动，且未串用对端 service

- `react-ui/src/pages/Seller/index.tsx:1-117` 通过 `PartnerManagementPage` 注入 `sellerConfig`，所有 API 均来自 `@/services/seller/seller`。
- `react-ui/src/pages/Buyer/index.tsx:1-118` 通过同一 `PartnerManagementPage` 注入 `buyerConfig`，所有 API 均来自 `@/services/buyer/buyer`。
- 两页的差异集中在：
  - `moduleKey`：`seller` / `buyer`
  - 字段：`sellerId/sellerAccountId` 与 `buyerId/buyerAccountId`
  - 权限前缀：`seller:admin:*` 与 `buyer:admin:*`
  - service 绑定：seller API 与 buyer API 各自独立

对应证据：

- `react-ui/src/pages/Seller/index.tsx:43-113`
- `react-ui/src/pages/Buyer/index.tsx:43-114`

### 2. 路由入口按端隔离，buyer 没有复用 seller 路由权限

- `react-ui/config/routes.ts:49-52`：`/seller` -> `authority: ['seller:admin:list']` -> `component: './Seller'`
- `react-ui/config/routes.ts:55-58`：`/buyer` -> `authority: ['buyer:admin:list']` -> `component: './Buyer'`

这说明 seller / buyer 管理页入口在静态路由层已经按端隔离，没有出现 buyer 入口挂 seller authority 的问题。

### 3. 共享模板的权限前缀来自 `moduleKey`，不会在组件内部写死 seller / buyer

- `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:107-135` 把 `moduleKey` 限定为 `'seller' | 'buyer'`，所有 service 能力通过 `config.services` 注入。
- `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:482-488` 使用 ``${permPrefix}:account:*`` 生成账号权限，其中 `permPrefix = ${config.moduleKey}:admin`。
- `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:863-889` 的主体级“登录端 / 会话 / 强制踢出”按钮全部按 ``${permPrefix}:directLogin``、``${permPrefix}:session:list``、``${permPrefix}:forceLogout`` 判断。
- `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:245-256` 的账号角色入口同时要求：
  - `*:admin:role:query`
  - `*:admin:account:role:query`
  - `*:admin:account:role:edit`
- `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:646-662` 的账号级“登录端 / 重置密码 / 会话 / 强制踢出”按钮分别按 `directLogin`、`account:resetPwd`、`session:list`、`forceLogout` 控制。

这与计划文档要求一致，未发现组件内写死 seller/buyer 前缀、导致串端权限判断的情况。

### 4. 前后端权限链路一致

#### account role

前端：

- `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:254-256`

后端：

- seller：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:91-103`
- buyer：`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:91-103`

结论：前端“分配角色”入口要求 `role:query + account:role:query + account:role:edit`，与后端读写接口权限组合一致。

#### resetPwd

前端：

- `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:652-653`

后端：

- seller：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:143-149`
- buyer：`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:143-149`

结论：前端按钮权限与后端 `*:admin:account:resetPwd` 一致。

#### session/list

前端：

- 主体级：`react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:881-883`
- 账号级：`react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:655-656`

后端：

- seller：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:152-169`
- buyer：`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:152-169`

结论：前端统一按 `*:admin:session:list` 控制列表查看，后端也分别以 `*:admin:session:list` 保护 subject/account 两类会话列表接口，符合计划文档“查看会话不能绑定强退权限”的要求。

#### forceLogout

前端：

- 主体级：`react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:887-889`
- 账号级：`react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:661-662`

后端：

- seller：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:172-185`
- buyer：`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:172-185`

结论：前后端统一使用 `*:admin:forceLogout`，没有把会话查看和强退混绑。

#### directLogin

前端：

- 主体级：`react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx:863-865`
- 账号级：`react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx:646-647`

后端：

- seller：`RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/controller/AdminSellerController.java:188-204`
- buyer：`RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/controller/AdminBuyerController.java:188-204`

结论：前后端统一使用 `*:admin:directLogin`，且 seller / buyer 各走自己的 endpoint。

### 5. service endpoint 也按端拆分，没有串用

- seller service：
  - account role：`react-ui/src/services/seller/seller.ts:136-142`
  - role：`react-ui/src/services/seller/seller.ts:203-246`
  - resetPwd：`react-ui/src/services/seller/seller.ts:256`
  - sessions / forceLogout：`react-ui/src/services/seller/seller.ts:265-289`
  - directLogin：`react-ui/src/services/seller/seller.ts:298-309`
- buyer service：
  - account role：`react-ui/src/services/buyer/buyer.ts:92-98`
  - role：`react-ui/src/services/buyer/buyer.ts:159-202`
  - resetPwd：`react-ui/src/services/buyer/buyer.ts:256`
  - sessions / forceLogout：`react-ui/src/services/buyer/buyer.ts:265-289`
  - directLogin：`react-ui/src/services/buyer/buyer.ts:298-309`

未发现 buyer 页调用 `/api/seller/admin/**`，也未发现 seller 页调用 `/api/buyer/admin/**`。

## 备注

- `buyer` 相比 `seller` 存在 `balanceTitle` 文案和 `showRechargePlaceholder` 的配置差异，属于展示层差异，不是本轮 P0 / P1 的串端、权限或接口缺陷。
- 本次为只读静态审计，未执行浏览器、接口回放或编译验证；按用户要求仅输出 P0 / P1 结论。
