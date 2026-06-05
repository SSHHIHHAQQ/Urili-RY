# 来源商品库首版页面执行记录

日期：2026-06-05

## 目标

把「商品管理 / 来源商品库」从占位页落成首版只读列表，展示各来源系统同步回来的 SKU 基础信息，并提供单行详情抽屉。

## 范围

- 后端新增只读分页接口：`GET /integration/admin/source-products/list`。
- 前端新增页面：`react-ui/src/pages/Product/SourceProductLibrary/index.tsx`。
- 菜单 `2400` 从占位组件切到新页面组件。
- 本次不新增业务表，不修改来源 SKU、配对关系、商城商品或库存事实。

## 数据源确认

- 激活配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`，`spring.profiles.active=druid`
- MySQL 连接来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml` 的 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD`
- 目标环境：以后端激活配置读取到的远端 MySQL 为准；未使用本地 Docker MySQL。
- 执行命令类型：菜单 DML，仅更新 `sys_menu.menu_id = 2400` 的组件和备注
- 数据影响：不改业务表，不改来源商品数据，不改配对关系

## 实现内容

- 后端新增 `AdminSourceProductController`，提供管理端只读分页接口：`GET /integration/admin/source-products/list`。
- 后端复用 `upstream_system_sku_candidate` 作为来源 SKU 快照表，联查 `upstream_system_connection` 和 `upstream_system_sku_pairing`，不新增重复事实表。
- 后端新增 `SourceProductQuery` 和 `SourceProductItem`，支持按来源系统、主仓、来源 SKU、商品名、识别码、分类、审核状态、危险品、同步状态、配对状态和关键词筛选。
- 接口权限复用菜单权限 `product:list:list`；首版不新增按钮权限。
- 前端新增 `Product/SourceProductLibrary` 页面，使用 ProTable 展示来源系统、主仓、来源 SKU、商品基础资料、条码/FNSKU、分类、审核状态、危险品、尺寸重量、申报信息、同步状态、配对结果和更新时间。
- 前端新增详情抽屉，展示图片、来源、基础信息、识别码与分类、尺寸重量、申报信息、商城配对和来源快照摘要；不展示原始 `source_payload_json`。
- 前端抽取 `react-ui/src/services/integration/constants.ts` 作为 integration 共享状态/来源系统选项，`UpstreamSystem/constants.ts` 改为复用该文件。
- 菜单种子 `business_menu_seed.sql` 已将 `2400 来源商品库` 指向 `Product/SourceProductLibrary/index`。
- 执行脚本 `RuoYi-Vue/sql/20260605_source_product_library_menu_component.sql` 已用于更新当前运行库菜单组件。
- 运行时修复：
  - 清理 `react-ui/node_modules/.cache` 后重启 8001，修复浏览器仍引用旧 CSS chunk 的问题。
  - 补齐静态路由依赖的 `react-ui/src/pages/Product/Distribution/EditPage.tsx`，避免 Umi 清缓存后因缺少 `Product/Distribution/EditPage` 无法启动。
  - 补齐 `react-ui/src/pages/Product/Distribution/style.module.css`，避免商城商品组件引用缺失样式导致 Webpack 编译不完整。
  - 将来源商品库页面的 Ant Design 6 废弃属性改为新写法：`Drawer size`、`Space orientation`。
- 列表二次收敛：
  - 「来源系统」和仓库名称合并为「来源仓库」列，上方显示来源系统，下方显示仓库名称，不再显示主仓系统代码。
  - 列表移除图片、来源分类、危险品和申报信息展示。
  - 尺寸拆成「客户尺寸」和「仓库尺寸」两列，分别显示尺寸和重量。
  - 「最近发现」改为「同步时间」，并与更新时间合并在同一列上下展示。
  - 筛选区移除主仓编号、分类和危险品，保留来源系统、仓库名称、来源 SKU、商品名称、识别码、审核状态、同步状态和配对状态。

## 验证命令

- `mvn -pl integration -am -DskipTests package`
  - 结果：通过。
- `npm run tsc`
  - 目录：`react-ui`
  - 结果：通过。
- `mvn -DskipTests package`
  - 目录：`RuoYi-Vue`
  - 结果：通过。
  - 备注：首次执行时因运行中的 `ruoyi-admin.jar` 锁住 jar，Spring Boot repackage 无法 rename；停止 8080 对应 Java 进程后重新执行通过。
- `.\start-backend-local.ps1 -Restart`
  - 目录：仓库根目录
  - 结果：后端重启成功，`http://127.0.0.1:8080` 返回 200。
- 浏览器验证：
  - 使用 `admin / admin123` 登录管理端。
  - 打开 `http://127.0.0.1:8001/product/list`。
  - 结果：页面标题为「来源商品库」，列表加载领星数据，总数 5401 条；详情抽屉可打开并展示 SKU、来源、尺寸、申报和快照信息。
- CSS chunk 复验：
  - 重新启动 `http://127.0.0.1:8001`。
  - 使用 Playwright 新浏览器会话从根路径登录，点击「商品管理 / 来源商品库」进入页面。
  - 结果：console `Errors: 0, Warnings: 0`；`GET /api/integration/admin/source-products/list?pageNum=1&pageSize=20` 返回 200；未再出现 `Loading CSS chunk ... failed`。
- 列表收敛复验：
  - 使用 Playwright 新浏览器会话从根路径登录，点击「商品管理 / 来源商品库」进入页面。
  - 结果：表头为「来源仓库、来源 SKU、来源商品、条码 / FNSKU、审核状态、客户尺寸、仓库尺寸、同步状态、配对状态、匹配客户、商城商品、同步时间、操作」。
  - 结果：列表不再展示图片、来源分类、危险品和申报信息；接口返回 200；console `Errors: 0, Warnings: 0`。
- `codegraph sync .`
  - 目录：仓库根目录
  - 结果：通过；来源商品库落地后同步成功，CSS chunk 修复后再次同步 `Synced 6 changed files`。

## 菜单 DML 执行结果

- 先使用中文菜单名作为条件执行时，因临时 SQL runner 的中文字面量编码问题匹配 0 行。
- 随后使用稳定 `menu_id = 2400` 和 `menu_type = 'C'` 执行，更新 1 行。
- 复查结果：
  - `menu_id = 2400`
  - `component = Product/SourceProductLibrary/index`
  - `route_name = SourceProductLibrary`
  - `perms = product:list:list`

## 权限与复用检查

- 权限检查：后端接口已加 `@PreAuthorize("@ss.hasPermi('product:list:list')")`，与菜单权限一致。
- 字典/选项复用检查：来源系统、同步状态、配对状态已抽到 `react-ui/src/services/integration/constants.ts`；领星危险品和审核状态首版保留在来源商品库页面常量，后续若跨页面复用再上移。
- 复用台账检查：已在 `docs/architecture/reuse-ledger.md` 登记来源商品库只读视图和 integration 共享选项。
- CodeGraph 更新结果：已执行 `codegraph sync .`，最终状态为 `Already up to date`。
- 大文件合理性判断：新增页面文件职责单一，主要为列表列配置和请求适配；详情内容单独拆为 `SourceProductDetailDrawer.tsx`。
- 重复代码检查：已去掉上游系统页面内的来源系统、同步状态、配对状态重复常量。

## 残留问题

- 首版不做商城商品草稿生成。
- 首版不做库存展示。
- SKU 配对仍在「上游系统管理」处理。
- 当前仍只展示来源 SKU 快照摘要，库存、同步差异、草稿创建和批量操作待后续版本设计。

## 2026-06-05 表格交互修复追加记录

### 修复内容

- 来源商品库列表的“来源商品”列增加列宽约束，长商品名、别名或描述不再横向穿透到后续列。
- 全局 `ProTable` 列表样式调整为表格内容区独立滚动，表头在滚动容器内固定。
- 新增统一分页配置 `getProTablePagination`，默认开启每页条数切换，并用 `defaultPageSize` 替代写死受控 `pageSize`。
- 替换当前发现的 `pagination={{ pageSize: ... }}` 写法，覆盖来源商品库、上游系统同步页、SKU 同步面板和主体审计弹窗。

### 验证结果

- `npx biome lint src/utils/proTableSearch.ts src/global.css src/pages/Product/SourceProductLibrary/index.tsx src/pages/Product/SourceProductLibrary/style.module.css src/components/PartnerManagement/PartnerAuditModal.tsx src/pages/UpstreamSystem/components/SyncTabs.tsx src/pages/UpstreamSystem/components/SkuSyncPanel.tsx`
  - 结果：通过。
- `npm run tsc`
  - 结果：未通过。
  - 未通过原因：当前工作区无关文件 `src/pages/Product/Attribute/components/CategoryAttributeTemplate.tsx` 存在 JSX 标签未闭合语法错误，错误位置为 269、414、416、417 行，阻断全量 TypeScript 编译。
- Playwright 浏览器验证：
  - 登录 `admin / admin123` 后进入 `http://127.0.0.1:8001/product/list`。
  - 表格滚动容器为 `.ant-table-content`，滚动 `scrollTop = 500` 后首个表头单元格 top 从 `361.59375` 到 `361.59375`，确认表头固定。
  - 来源商品列 DOM 检查 `overflowCells = []`，确认当前可见行未发生横向溢出。
  - 每页条数从 20 切换到 50 后，分页文案为“第 1-50 条/总共 5401 条”，网络请求为 `GET /api/integration/admin/source-products/list?pageNum=1&pageSize=50`。
  - Console 检查：`Errors: 0, Warnings: 0`。
  - 截图证据：`.playwright-cli/page-2026-06-05T03-58-06-242Z.png`。
- `codegraph sync .`
  - 结果：通过，`Synced 6 changed files`。

### 滚动修正

- 问题：上一版全局样式虽然脚本可设置 `scrollTop`，但 Ant Design 横向滚动表格在 `.ant-table-content` 上存在内联 `overflow-y: hidden`，导致真实鼠标滚轮无法纵向滚动；同时 `thead` 和 `th` 双重 sticky 有表头超出表格框的风险。
- 修复：全局样式保留内容区高度约束，改为强制 `.ant-table-content` / `.ant-table-body` `overflow-y: auto`；取消 `thead` 自身 sticky，只保留 `th` sticky。
- Playwright 复验：
  - `.ant-table-content` 计算样式为 `overflow: auto/auto`，`clientHeight = 297`，`scrollHeight = 1427`。
  - 鼠标滚轮滚动后 `.ant-table-content.scrollTop = 600`，确认真实滚轮可用。
  - 滚动后表头 top 保持 `361.59375`，tbody 首行 top 为 `-191.40625`，确认表头冻结、数据行正常滚动。
  - 表头单元格仍在 `.ant-table-content` 垂直边界内，未超出表格框。
  - Console 检查：`Errors: 0, Warnings: 0`。
  - 截图证据：`.playwright-cli/page-2026-06-05T04-11-34-421Z.png`。
