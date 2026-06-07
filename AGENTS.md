# AGENTS.md

本目录是 URILI 的若依验证工程，独立于 `E:\Urili`。后续 AI agent 必须先读本文件。

## 项目定位

- 后端唯一基线：`RuoYi-Vue` 官方后端，路径 `RuoYi-Vue/`。
- 当前前端基线：`ruoyi-react` 的 `antdesign6` 分支前端，路径 `react-ui/`。
- 当前架构方向：管理端、卖家端、买家端三端独立。账号、密码、角色、菜单、权限、部门、登录日志、操作日志和会话必须按端隔离；管理端保留若依 `sys_*` 后台体系，卖家端和买家端不得继续复用 `sys_user` / `sys_role` / `sys_menu` / `sys_dept` 作为端内账号权限体系。
- 最终前端方向：三端物理分离，形成管理端、卖家端、买家端三个前端项目；当前阶段先以 `react-ui/` 作为管理端验证和改造入口。拆出 `seller-ui` / `buyer-ui` 前，必须先按已确认方案完成账号、端入口、菜单域、权限模型和管理端控制权设计。
- 默认 Vue 前端 `RuoYi-Vue/ruoyi-ui` 已删除，后续不要恢复、复制或继续开发它。
- `ruoyi-react` 仓库里的后端副本已删除，后续不要重新引入第二套若依后端。
- 当前目标是验证并改造“若依后端 + React/Ant Design 前端”，再逐步承载 URILI 业务。

## 目录规则

```text
E:\Urili-Ruoyi\
  RuoYi-Vue\        # 官方若依后端
  react-ui\         # 当前 React + Ant Design Pro 前端，现阶段作为管理端
  docker-compose.yml
  README.md
  logs\
```

- 后端业务改动只进入 `RuoYi-Vue/`。
- 当前阶段前端业务改动只进入 `react-ui/` 的管理端范围；等账号模型、端入口、菜单域、权限模型和管理端流程稳定后，再按确认方案拆出管理端、卖家端、买家端三个前端目录。
- 不要把旧项目 `E:\Urili` 的代码直接搬进本工程；需要迁移时先写 Markdown 方案。
- 生成报告、迁移记录、阶段总结、代码审查结论时，都必须生成 Markdown 文件。
- 本工程当前实际使用的数据源以 `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 和 `application-druid.yml` 的激活配置为准；不要默认使用本地 Docker 数据库或 Redis。

## 当前服务与数据源

- MySQL：当前以后端激活的 `application-druid.yml` JDBC URL 为准；不要默认连接 `localhost:3306`。
- Redis：当前以后端 `application.yml` 的 `spring.data.redis` 配置为准；不要默认连接 `localhost:6379`。
- 后端：`http://127.0.0.1:8080`。
- React 前端：`http://127.0.0.1:8001`。
- 默认登录：`admin / admin123`。

## 本机后端启动方式

- 本机后端默认通过 `start-backend-local.ps1` 启动或重启。
- `start-backend-local.ps1` 从 `.env.local` 读取 `RUOYI_*` 运行变量，再启动 `RuoYi-Vue/ruoyi-admin/target/ruoyi-admin.jar`。
- `.env.local` 保存远程 MySQL/Redis 连接信息和 token secret，只允许本机使用，禁止提交、复制到报告或在聊天中明文输出。
- `.env.local` 和 `start-backend-local.ps1` 已被 `.gitignore` 忽略；如果新对话看不到文件，先检查是否被本地清理或工作区未同步。
- 后端停止或 jar 重新打包后，优先使用：

```powershell
cd E:\Urili-Ruoyi
.\start-backend-local.ps1 -Restart
```

- 如果 8080 已经监听但不确定是否为当前 jar，也使用 `-Restart`。
- 不要直接运行 `java -jar .\ruoyi-admin\target\ruoyi-admin.jar`，除非当前 shell 已确认包含完整 `RUOYI_DB_*`、`RUOYI_REDIS_*` 和 `RUOYI_TOKEN_SECRET` 环境变量。

## 数据源确认规则

- 执行 SQL、查询数据、启动后端、排查登录或权限问题之前，必须先读取当前激活配置，确认目标 MySQL 和 Redis 是远端还是本地。
- 不允许因为 `docker-compose.yml`、README、历史初始化脚本或若依默认习惯，直接假设数据源是 `localhost:3306` 或 `localhost:6379`。
- 当前 `docker-compose.yml` 中的 MySQL/Redis 只作为手动隔离验证用途，默认不启动；只有用户明确要求本地隔离验证时，才允许使用 `docker compose --profile local-infra up -d`。
- 涉及远端数据库 DDL/DML、字典、菜单、权限、账号、业务数据调整时，必须先生成 Markdown 方案或执行记录，并得到用户确认后再执行。
- 每次数据库或 Redis 验证记录必须写清楚目标环境、连接来源、执行命令类型和是否影响远端数据；不能只写“已执行 SQL”。
- 如果发现本地 Docker 数据库或 Redis 正在运行，应先提醒它可能造成误判；除非本次任务明确要求本地隔离环境，否则不要读取或写入它。

## 当前数据库基线

- 当前初始化脚本来自 `RuoYi-Vue/sql/ry_20260417.sql` 和 `RuoYi-Vue/sql/quartz.sql`。
- 初始化脚本只代表若依官方建库基线，不代表当前运行库一定是本地 `ry-vue`；当前运行库必须以激活数据源配置为准。
- `ry_20260417.sql` 和 `quartz.sql` 是 bootstrap-only 建库基线，包含破坏性 `DROP TABLE`；只允许用于全新数据库初始化，不得作为已有本地库或远程库的增量迁移回放。
- 这两份官方基线脚本不纳入普通增量 SQL 的确认 token 机制，但必须保留 `URILI_BOOTSTRAP_ONLY_SQL` 哨兵注释，并由 `SqlExecutionGuardContractTest` 固定“初始化基线”和“增量脚本”边界。
- 当前还没有 URILI 商品、库存、订单、履约、财务、领星等业务表。
- 若依主库当前主要包含：
  - 用户、部门、岗位、角色、菜单、权限关联表
  - 字典、参数配置、操作日志、登录日志
  - 定时任务配置、任务日志、通知公告、代码生成表
- Quartz 表只服务定时调度，不承载业务数据。
- 后续业务表必须独立设计。管理端优先复用若依的用户、部门、角色、菜单、权限、字典、配置和日志能力；卖家端、买家端的账号、部门、角色、菜单、权限、登录日志、操作日志和会话必须独立设计，不再复用若依 `sys_*` 作为端内控制面。

## 开发边界

- 保留若依的用户、角色、菜单、权限、字典、日志、定时任务、代码生成等管理端基础能力。
- 卖家端和买家端的端内账号、密码、角色、菜单、权限、部门、登录日志、操作日志和会话体系必须独立于若依 `sys_*`；管理端通过平台管理接口、主体状态、账号状态、菜单/角色配置、免密代入、强制踢出和审计日志保留控制权。
- URILI 商品、库存、订单、履约、财务、领星接入等业务必须按模块逐步设计，不要用简单 CRUD 直接替代业务规则。
- 后续 AI 开发优先保持小步任务，不要一次同时改后端、前端、数据库和 UI 主题。
- 简单任务可以快速处理；复杂任务必须先读上下文、明确成功标准、分步验证。

## 数据表设计确认规则

- 任何新增业务表、关联表、流水表、外部系统映射表、财务/库存相关表之前，必须先向用户提交 Markdown 设计方案，并得到确认。
- 未确认前，不允许新增 `CREATE TABLE`、大规模 `ALTER TABLE`、Entity、Mapper、Service、Controller、菜单权限或前端页面。
- 设计方案必须说明：
  - 业务目的：为什么需要这张表
  - 业务逻辑：这张表承载什么规则，不承载什么规则
  - 是否能复用现有若依表、字典、配置或已有业务表
  - 与用户、角色、部门、字典、菜单、权限等基础表的关系
  - 字段含义、类型、是否必填、默认值、唯一约束、索引
  - code/label 规则，是否需要进入 `sys_dict`
  - 金额、库存、流水、外部请求日志是否需要只追加
  - 权限点、菜单、按钮、审计日志、导入导出是否受影响
  - 初始化数据、迁移方式、回滚方式
- 日期前缀增量 SQL（例如 `20xxxxxx*.sql`）必须纳入 `SqlExecutionGuardContractTest` 自动发现；包含 dynamic DDL helper 的脚本，例如 `set @ddl = concat('alter table ...')` / `prepare stmt from @ddl`，也必须被视为高影响 SQL，保留确认 token、`45000` fail-closed 和确认调用在执行前的合同。
- 数据表确认通过后，才能进入后端、前端和 SQL 实现。

## 复用台账与重复代码规则

- 开发前必须先查复用台账，再搜索现有代码，避免重复实现。
- 复用台账建议维护在 `docs/architecture/reuse-ledger.md`。
- 后端重点检查 Controller、Service、Mapper、Mapper XML、字典、权限标识、查询条件、分页、导入导出、校验、金额/库存计算、外部系统适配器。
- 前端重点检查页面组件、表格列、表单项、hooks、services、枚举/选项、格式化方法。
- 抽取新的公共方法、组件、字典、选项、适配器后，必须登记到复用台账。
- 每次完成代码更新后，必须在仓库根目录重新运行 `codegraph sync .` 更新 CodeGraph 索引；如果是首次使用或 `.codegraph/` 缺失，先运行 `codegraph init .`；如果需要全量重建索引，运行 `codegraph index .`。
- `.codegraph/` 属于本机索引目录，默认不作为业务代码、报告或迁移产物提交；如确需提交索引产物，必须先确认范围和体积。
- CodeGraph 更新结果或无法执行原因必须写入阶段记录、代码审查记录或最终回复。
- 不允许在多个 React 页面、Java Service、Mapper XML 中复制相同业务逻辑。

## 字典、字段与选项集中维护

- 若依已有 `sys_dict` 能力，业务状态、类型、币种、国家、地区、仓库类型、上游系统类型等，应优先使用若依字典。
- API 与数据库默认保存 `code`，前端负责通过字典或统一 option catalog 转成 `label`。
- React 页面不得内联大段状态下拉、类型映射、颜色映射。
- mock 数据必须使用与真实接口一致的 code，不允许 mock 一套、真实接口一套。
- 新增业务字段前，需要明确字段含义、存储 code 还是展示 label、是否进入若依字典、是否影响权限/导入导出/查询筛选。

## 权限规则

- 新增后端接口时，必须同步权限标识。
- 管理端若依后端接口按场景配置 `@PreAuthorize("@ss.hasPermi('xxx')")`、`@Log`、菜单权限 `sys_menu.perms` 和按钮权限。
- 卖家端、买家端接口不得直接依赖 `sys_menu` / `sys_role` 判断端内权限；后续必须分别读取 `seller_menu` / `seller_role` 或 `buyer_menu` / `buyer_role` 等端内权限模型。
- React 前端新增页面、按钮、批量操作、导出、删除、审核等操作时，必须同步前端权限控制。
- 新增功能至少验证：有权限用户可访问，无权限用户不可访问，菜单/按钮权限缺失时前端不展示或后端拒绝。
- 不允许只在前端隐藏按钮而不加后端权限。
- 管理端 `sys_menu` seed 必须明确菜单所有权；同一个 `menu_id` 的最终签名默认只能由一个 seed 负责。通用菜单 seed 不得回放覆盖已由专用业务 seed 接管的菜单；专用 seed 可以显式允许旧占位签名用于历史库迁移，但必须在写入前做 slot/signature guard。同 ID guard 必须覆盖 `parent_id` 和 `menu_type`，避免历史菜单挂错父级或类型时被静默改写。`2010` 主体管理顶级目录只能由 `top_menu_seed.sql` 写入，`seller_buyer_management_seed.sql` 和 `20260606_admin_partner_page_direct_login_seed.sql` 只能断言该顶级目录已存在且签名正确，不得再 upsert `2010`。确需保留历史兼容增量 seed 时，只允许重复写入已明确记录的同一最终签名，并必须先做 slot/signature guard；不得借兼容 seed 改变最终 owner 或扩散到新的 `menu_id`。

## 模块边界

- 后端遵循 Controller -> Service -> Mapper 分层。
- Controller 不直接调用 Mapper。
- Service 不应绕过业务模块边界直接读写其他模块的表实现细节。
- 跨业务模块调用应通过 Service、Facade 或明确的公共接口。
- 前端页面只能通过 API service 调用后端，不允许绕过后端权限或模拟后端业务规则。
- 模块内部实现不得被其他模块随意 import；需要复用时应暴露稳定 public entry。

## 业务模块与数据归属规则

- 开发前必须先判断当前需求属于哪个业务模块，再决定建表、接口、Service、Mapper、前端页面和权限点。
- 不允许先做一个宽泛通用表，再靠 `type`、`kind`、`biz_type` 等字段长期混放不同业务主体。
- 如果业务主体天然不同，必须优先拆成独立模块和独立表。例如卖家和买家应分别进入 `RuoYi-Vue/seller`、`RuoYi-Vue/buyer` Maven 子模块，而不是共用一个 `customer` 表。
- 业务模块至少要区分：卖家 `seller`、买家 `buyer`、商品 `product`、库存 `inventory`、订单 `order`、履约 `fulfillment`、财务 `finance`、外部系统接入 `integration`。
- 平台管理端基础能力优先复用若依 `sys_user`、`sys_role`、`sys_menu`、`sys_dict`、`sys_oper_log` 等基础表。
- 卖家端、买家端必须另起端内账号权限控制面：`seller_account` / `buyer_account` 存端内登录账号和密码密文，`seller_role` / `buyer_role` 存端内角色，`seller_menu` / `buyer_menu` 存端内菜单权限，`seller_dept` / `buyer_dept` 存端内部门，`seller_login_log` / `buyer_login_log` 和 `seller_oper_log` / `buyer_oper_log` 存端内日志。
- 主体资料表、业务事实表、流水表、外部请求日志表必须分清楚；不要把订单、库存、财务流水或外部请求日志塞进卖家、买家等主体资料表。
- 跨模块调用不能直接读写对方 Mapper 或表实现细节，应通过 Service、Facade 或明确公共接口。
- 如果一个需求同时涉及多个模块，必须先写 Markdown 方案说明模块边界、数据归属、表关系和权限点，再进入实现。
- 数据库业务表命名应按业务对象直接命名，例如 `seller`、`buyer`、`seller_account`、`buyer_account`，不要为了项目名前缀默认加 `urili_`。
- 当前业务模块代码命名空间也应按业务对象直接命名，不要默认加 `Urili` / `urili`。例如 Maven module 使用 `seller`、`buyer`；Java 包使用 `com.ruoyi.seller`、`com.ruoyi.buyer`；Java 类使用 `Seller`、`Buyer`、`SellerMapper`、`ISellerService`；接口使用 `/seller/...`、`/buyer/...`；权限使用 `seller:...`、`buyer:...`；前端目录使用 `pages/Seller`、`pages/Buyer`、`services/seller`、`services/buyer`。

## 三端独立账号权限规则

- 当前参考方向以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为准；若旧文档仍写“卖家/买家账号继续复用若依 sys_user”，视为过期结论。
- 管理端账号只代表平台管理员、运营、客服、财务、风控等后台人员，不代表卖家或买家员工。
- 卖家端账号只代表卖家主体下的主账号或员工子账号；买家端账号只代表买家主体下的主账号或员工子账号。
- 同一现实公司如果同时是卖家和买家，也必须分别创建卖家主体、买家主体、卖家端账号和买家端账号；系统默认不建立二者关联。
- 管理端不通过混用账号体系获得控制权，而是通过平台管理接口、主体状态、账号状态、菜单/角色配置、免密代入、强制踢出和审计日志保留控制权。
- 三端登录入口、token/session、Redis key、权限校验和数据范围必须能明确区分 `admin` / `seller` / `buyer`。
- 卖家端接口不能相信前端传入的 `sellerId`，必须从当前卖家端 token 身份推导；买家端接口不能相信前端传入的 `buyerId`，必须从当前买家端 token 身份推导。
- 卖家/买家账号查询必须下推到 SQL 层同时带 `seller_id/buyer_id + account_id` 约束；管理端登录日志、操作日志和免密票据审计列表按账号筛选时也必须显式提供对应主体 ID，不允许通过裸 `select*AccountById(accountId)` 反查主体 ID。Mapper 接口和 XML 不得保留裸 `select*AccountById(accountId)` 声明，生产代码中的裸 accountId mapper 调用必须被静态契约拦截。
- 管理端免密代入卖家端或买家端必须短时、一次性、可审计，必须记录 acting admin、目标端、目标主体、目标账号、原因、过期时间、使用时间和 IP；不能无痕冒充端内员工。
- 管理端免密代入打开 seller/buyer 端窗口后，前端成功提示必须等待目标 portal 端完成 `/direct-login` 消费并通过 `PORTAL_DIRECT_LOGIN_RESULT_MESSAGE` 回传成功；不能只收到 `READY` 或只发出 token 就提示成功。
- `portal.seller.web.url` / `portal.buyer.web.url` 属于环境配置；seed 脚本只允许在缺失时插入本地验证占位地址，不得无条件覆盖已有值，避免远端回放把真实端地址改回 `127.0.0.1:8001`。
- 管理端强制踢出、账号锁定导致的强退、密码重置导致的强退等后台控制动作，登录日志里的 `actingAdminId` / `actingAdminName` 必须记录当前执行控制动作的管理端账号；如果被控制的是免密代入会话，可以保留 `directLogin` / `directLoginTicketId` 标记会话来源，但不得把原票据签发人误写成本次操作人。
- 管理端重置卖家/买家端账号密码时，“重置密码”默认语义是人工输入 5-20 位临时密码并调用端账号 `resetPwd` 接口；不得把该入口静默退回默认密码 `U12346`。当前实现不保留 `resetDefaultPwd` 默认密码重置接口；如未来确需恢复默认密码能力，必须重新确认，并作为独立操作入口继续使用 `*:admin:account:resetPwd` 权限和端账号密码表。
- 免密代入 Redis payload key 必须使用 `portal_direct_login:{terminal}:{token_hash}`；认证链路不得读取旧 `portal_direct_login:{token_hash}`，旧 key 只允许作为历史残留清理目标，不得新增依赖。
- 卖家端、买家端消费免密票据时，如果票据 `terminal` 与当前端不匹配，不得把外端票据的 `ticketId`、`actingAdmin*`、`reason`、目标主体或目标账号写入当前端登录日志、操作日志或会话；应按当前端普通失败记录或直接拒绝。
- `seller_oper_log` / `buyer_oper_log` 的免密代入审计必须落结构化字段，至少包含 `direct_login`、`direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`；`oper_param` 文本前缀只能作为兼容信息，不能作为唯一审计来源。
- 卖家端、买家端自助日志接口不得直接返回 `PortalLoginLog` / `PortalOperLog` 内部审计模型；必须映射为 `PortalOwnLoginLogProfile` / `PortalOwnOperLogProfile` 等端内可见 DTO，不返回 `subjectId`、`accountId`、`directLoginTicketId`、`actingAdminId`、`actingAdminName`、`directLoginReason`、`operParam`、`jsonResult`、`tokenId` 或 Redis key。管理端审计接口继续返回完整结构化审计字段。
- 卖家端、买家端端内菜单写入必须 fail-closed：页面菜单 `C` 和按钮菜单 `F` 的 `perms` 必填，必须使用当前端前缀 `seller:` / `buyer:`，禁止 `*` 通配和 `seller:admin:` / `buyer:admin:` 管理端命名空间；页面菜单 `C` 的 `component` 必填，并且必须使用当前端页面根路径，不得为空回退到共享占位页。
- 当前 `seller_menu` / `buyer_menu` 是端级共享菜单模板，不是单个卖家/买家的主体私有菜单；端内角色绑定菜单时，后端必须先全量校验提交的 `menuIds` 均存在于当前端对应菜单表，任何不存在或跨端菜单 ID 都必须在写 `seller_role_menu` / `buyer_role_menu` 前 fail-closed。
- `seller_menu` / `buyer_menu` 的数字 ID 空间必须保持不重叠：seller 端菜单使用 `100000-199999`，buyer 端菜单使用 `200000-299999`。fresh seed 和三端隔离迁移必须分别从 `auto_increment=100000` / `auto_increment=200000` 开始；已运行库重排必须使用 guarded 迁移脚本并同步更新 role-menu 与 `parent_id`，不得手写无 guard 的主键重排 SQL。
- React 远程菜单路由 guard 必须对空 `authority` 拒绝访问，不能把空权限当作允许；涉及三端 token、代理、菜单和权限的 `.ts/.tsx` 与 `.js` 镜像必须纳入 guard 脚本，尤其是 `access.js`、`proxy.js` 这类运行入口镜像。

## 文件大小与拆分判断规则

- 若依官方历史文件不作为治理目标，不要求为了满足行数阈值而重构历史代码。
- 行数阈值不是强制拆分命令，而是自我检查触发点。
- 当新增或修改文件达到以下规模时，必须先做一次合理性判断：
  - 300 行：开始检查职责是否过多
  - 400 行：需要明确说明是否应拆分
  - 500 行：必须给出不拆分或拆分的理由
- 如果文件职责单一、结构清晰、拆分后反而增加理解成本，可以不拆。
- 如果文件同时包含多个职责、大量重复逻辑、复杂表单/表格配置、混杂 API 调用与业务规则，则应考虑拆分。
- 不为了拆分而拆分；拆分必须让职责更清楚、复用更容易、维护成本更低。

## React 前端开发规则

- 前端可以先行验证流程，但不能按一次性原型写法堆页面。
- 三端前端页面不展示 `PageContainer` 自动生成的页面级标题，也不要在筛选区、表格区上方额外手写页面标题；后续管理端、卖家端、买家端新增页面同样遵守。需要表达当前区域时，可以使用表格、卡片或弹窗自身标题。
- 三端前端页面的主要内容分块必须填满可视区域；即使表格行数、表单项或占位内容不足，也要让主数据块、表单块或占位块按剩余高度撑开，避免页面下半屏露出大面积空白背景。后续新增页面优先复用全局 `PageContainer` / `ProTable` 撑满规则，不要为单页另起特殊高度。
- 三端前端列表页高度必须自适应可视区域：表格主数据区默认至少保留 60% 的可用高度；上半部分包括面包屑、Tabs 和筛选区，必须根据视口高度自动缩小 padding、行距和区块间距，不允许因为查询按钮或固定间距撑出大块空白；表格滚动只发生在数据体区域，表头必须固定在表格顶部，不能跟随数据行一起滚动。
- 三端前端表格分页器必须位于主数据块底部；即使表格数据行很少，分页器也不能跟着数据停在中间。分页器上方必须保留一条浅色横向分隔线，作为表格内容和底部分页区的边界。
- 页面应按需要拆分 `components`、`hooks`、`services`、`types`、`constants`、`mock`。
- mock 必须集中维护，并标明与真实接口的边界。
- mock 不能伪装成真实业务规则，真实规则以后端和数据库设计为准。
- 表格列、表单选项、状态颜色、格式化方法应优先复用，不要散落在页面内部。
- 对已经确认过的同构管理端 UI 模式，优先模板化推进：先把卖家侧做成一套标准样板并验证，再复制成买家侧，只替换端类型、文案、路由、权限标识、字段配置和 service；不要在每个同构页面上重新设计。
- 三端前端列表页 ProTable 筛选区必须统一使用 `getPersistedProTableSearch(...)`，默认采用 Ant Design Pro 原生 `vertical` 查询布局，也就是字段名在上、输入框在下；不要在页面内直接散写 `search={{ ... }}` 或另起筛选布局。
- 三端前端筛选区必须按内容区宽度响应式降列：宽屏优先一行 6 个字段，中屏自动降为 4 个或 3 个字段，小屏降为 2 个字段；宁可换行，不允许把输入框压缩成不可用的小块。
- 日期范围、金额区间、余额区间、库存区间等长控件默认占 2 个筛选格；普通输入框也要保留最小可用宽度。当前 `react-ui` 先落地该规则，后续拆出 `seller-ui` / `buyer-ui` 后必须沿用同一套筛选预设。
- 筛选字段超过当前断点可展示数量时按统一网格自动换行；只有弹窗内小表格、纯明细表、无查询条件表格等确有理由的场景，才允许显式 `search={false}` 或在复用台账中说明例外。
- 三端前端所有业务选择器默认必须可模糊搜索，包括 `Select`、`ProFormSelect`、`TreeSelect`、`ProFormTreeSelect` 以及 ProTable 中 `valueType: 'select'` 或 `valueEnum` 生成的查询下拉；优先复用 `@/utils/selectSearch` 的 `SEARCHABLE_SELECT_PROPS` / `SEARCHABLE_TREE_SELECT_PROPS`，支持按 label、value/code、title 等文本模糊匹配。操作列“更多”这种命令 `Dropdown` 不按搜索下拉处理。
- 同一业务指标的最小值/最大值筛选，例如余额最小/余额最大、金额区间、库存区间，不得拆成两个独立筛选字段；前端应合并为一个区间输入字段，优先使用 Ant Design 原生组合控件和默认输入框样式，不自定义特殊容器，不使用假的禁用输入框，并在提交参数时转换为后端需要的最小/最大参数。
- 三端前端表格操作列如果同一行超过 2 个操作，最多保留 2 个高频操作按钮直接展示，其余操作必须使用 Ant Design `Dropdown` 收进“更多”下拉菜单；不要在一行里平铺 3 个及以上文字按钮。
- 三端前端表格操作列的行内操作和“更多”下拉菜单项默认只展示文字，不加操作图标；“更多”作为下拉触发器必须使用 Ant Design 小下箭头提示可展开。
- 三端 portal 401 处理必须按端隔离：`/api/seller/**`、`/api/buyer/**` 的非 admin 请求只能清当前端 token，并跳转到对应 `/seller/login` 或 `/buyer/login`，且必须带当前 portal 路由的 `redirect` 参数；`/api/seller/admin/**`、`/api/buyer/admin/**` 仍按管理端 401 处理。
- React 全局响应拦截器发现响应体 `code/errorCode = 401` 后，完成清 token 和跳登录后必须 reject/throw，不能继续把原 response 交给业务页面当成功结果处理。

## 外部系统与领星接入边界

- 对接领星、WMS、ERP、物流、支付等外部系统时，必须有明确适配层。
- 外部请求必须记录 traceId、请求时间、响应时间、外部单号、本地业务单号、错误码与错误信息。
- 必须配置合理的超时、重试、错误映射和脱敏日志。
- 不允许外部系统适配器绕过业务模块直接写事实源表。
- 外部请求日志、库存流水、财务流水原则上只追加，不直接覆盖历史记录。

## 数据与财务底线

- 金额字段必须使用 `BigDecimal`，禁止使用 `float` 或 `double`。
- 库存、财务、结算、外部请求日志必须保留可追溯记录。
- 涉及 PII、token、密钥、外部账号、手机号、邮箱等信息时，日志必须脱敏。
- 删除、作废、冲销等动作必须能追踪操作人、操作时间和原因。
- 财务和库存数据不得通过简单 CRUD 直接覆盖核心状态。

## AI Agent 工程守则

总原则：非平凡任务优先谨慎和可验证，不盲目追求速度。简单任务可以快速处理；复杂任务必须先读上下文、明确成功标准、分步验证。

- 需要使用子 Agent 时，优先使用 GPT-5.3 Codex；如果平台返回不可用、额度限制或上下文失败，再降级使用 `gpt-5.4`。子 Agent 完成、失败或不再需要后必须关闭，并在 Markdown 检查点记录实际模型、数量和结论处理。

### 规则 1：先想清楚，再写代码

- 开始前先说明当前假设、已知事实和目标边界。
- 如果存在高风险、不确定或不可逆决策，先提问确认。
- 如果只是低风险、可回滚、可验证的不确定点，先通过读代码、查日志、跑命令做最小验证。
- 当需求存在多种解释时，给出多个可选方向，并说明推荐选择。
- 如果有更简单可靠的方案，必须主动指出。

### 规则 2：简单优先，但不能省略业务规则

- 写能解决问题的最小必要代码，不做投机性扩展。
- 不提前建设未被要求的能力。
- 不为一次性逻辑强行抽象。
- URILI 商品、库存、订单、履约、财务、领星接入等业务，不能用简单 CRUD 替代业务规则。
- 判断标准：资深工程师看到后，会不会觉得这个实现明显过度设计，或明显漏掉业务边界。

### 规则 3：小步、精准修改

- 只改完成任务必须改的文件。
- 只清理自己引入的问题，不顺手重构无关代码。
- 不随意改相邻代码、注释、格式、目录结构或命名风格。
- 匹配当前代码库已有风格。
- 后端业务改动只进入 `RuoYi-Vue/`，前端业务改动只进入 `react-ui/`。

### 规则 4：以目标和验证驱动执行

- 先定义“完成”的标准，再开始实现。
- 不机械执行步骤；每一步都要服务于目标。
- 每完成一个关键步骤，都要验证它是否真的推进了目标。
- 如果验证失败，回到事实和目标重新调整。
- 对复杂任务，必须留下 Markdown 记录，说明做了什么、验证了什么、还剩什么。

### 规则 5：模型负责判断，工具负责事实

- AI 模型适合做判断、归纳、方案设计、文本整理、风险识别。
- 能由代码、命令、测试、日志、编译、数据库查询回答的问题，优先用工具验证。
- 不用模型猜测确定性事实。
- 不用模型替代测试、编译、接口请求或运行结果。

### 规则 6：控制上下文，阶段性沉淀

- 长任务必须控制上下文，不要在一次对话里无边界膨胀。
- 当任务接近上下文上限、步骤过多或信息开始混乱时，生成 Markdown 阶段总结。
- 阶段总结至少包括：当前目标、已完成事项、已验证事项、未解决问题、下一步建议。
- 不要静默越过上下文或任务边界。

### 规则 7：发现冲突时，不要折中混合

- 如果代码库里存在两个互相矛盾的模式，必须选一个主模式。
- 优先选择更新、更稳定、更符合当前项目定位、测试覆盖更充分的模式。
- 说明为什么选择它。
- 把另一个模式标记为后续清理项，不要把两套风格混在一起。

### 规则 8：写之前必须先读

- 新增代码前，先阅读相关模块、导出入口、直接调用方、共享工具和权限模型。
- 涉及业务规则时，先查复用台账、若依字典、权限标识、现有 Service/Mapper/API。
- “看起来无关”在复杂项目里经常是危险信号。
- 如果不理解某段代码为什么这样设计，先尝试查调用链、提交上下文、运行行为。
- 仍然不清楚时，再明确说明不确定点。

### 规则 9：测试验证意图，而不只是验证现象

- 测试应该表达业务规则为什么重要，而不是只断言当前输出是什么。
- 如果业务逻辑变化后测试仍然不会失败，这个测试价值不足。
- 优先覆盖权限、菜单、接口契约、数据边界、字典 code/label、金额/库存规则和关键业务规则。
- 如果没有运行测试，必须明确说明原因。

### 规则 10：关键步骤后做检查点

- 每个重要阶段后，记录已完成什么、已验证什么、还剩什么，以及有哪些风险或不确定点。
- 涉及建表、权限、字典、外部系统、财务/库存数据时，检查点必须明确写出当前判断。
- 不要在无法解释当前状态时继续推进。
- 如果任务失去脉络，先停下来重述事实、目标和下一步。

### 规则 11：遵守当前代码库约定

- 即使个人不喜欢，也优先遵守当前项目的结构、命名、接口、权限和样式约定。
- 代码库一致性高于个人偏好。
- 如果认为现有约定有害，先指出风险和替代方案，不要私自另起一套。
- 不恢复 `RuoYi-Vue/ruoyi-ui`，不重新引入第二套若依后端。

### 规则 12：失败要说清楚

- 如果跳过了测试、编译、运行、截图、接口验证或数据库验证，必须说明。
- “测试通过”只有在实际运行并通过后才能说。
- “完成”不能包含静默跳过的关键事项。
- 默认暴露不确定性，而不是隐藏问题。
- 如果只完成了方案或草稿，必须明确说这是方案或草稿，不是已落地实现。

### 子 Agent 使用规则

- 需要并行检查或大型任务拆分时，子 Agent 模型优先使用 GPT-5.3 Codex（工具模型 `gpt-5.3-codex-spark`）；如果不可用，再回退 `gpt-5.4`。
- 回退模型时，必须在阶段记录或最终回复中写明不可用原因、实际使用的模型和子 Agent 数量。
- 子 Agent 的读写范围必须由主 Agent 明确；用完必须关闭，主 Agent 负责合并结论、复核冲突和执行最终验证。

## 代码审查交付清单

代码审查或阶段总结必须输出 Markdown，并至少包含：

- 新增问题
- 已修复问题
- 残留问题
- 验证命令
- 未验证原因
- 权限检查结果
- 字典/选项复用检查结果
- 复用台账检查结果
- CodeGraph 更新结果
- 大文件合理性判断结果
- 重复代码检查结果

## 常用命令

```powershell
cd E:\Urili-Ruoyi
# 默认不要启动本地 MySQL/Redis；先确认 application.yml/application-druid.yml 的激活数据源
# 仅在明确需要本地隔离验证时使用：
docker compose --profile local-infra up -d

cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -DskipTests install

cd E:\Urili-Ruoyi
.\start-backend-local.ps1 -Restart

cd E:\Urili-Ruoyi\react-ui
npm install
$env:PORT='8001'; npm run dev

cd E:\Urili-Ruoyi
# 全局安装（已安装可跳过）
npm i -g @colbymchenry/codegraph
# 首次初始化并建立索引
codegraph init .
# 每次代码更新后同步最新索引
codegraph sync .
# 如需全量重建索引
codegraph index .
```
