# 上游系统主仓切换数据串用核查与修复方案

## 现象

新增 `NY013` 主仓后，左侧能看到两条主仓接入；右侧标题切换为 `NY013`，但仓库同步清单仍显示 `CA012` 的仓库信息，例如 `CA91244744 / MEISU`。

## 核查边界

- 当前后端激活配置：`spring.profiles.active=druid`。
- 当前 MySQL 来源：`.env.local` 中 `RUOYI_DB_URL`，目标库为远端 `fenxiao`。
- 本次只做只读核查，未执行 DDL/DML。
- 未输出数据库密码、Redis 密码、外部系统密钥或明文凭证。

## 表结构核查结论

运行库当前核心表按 `connection_code` 做隔离：

| 表 | 隔离设计 |
| --- | --- |
| `upstream_system_connection` | 主键：`connection_code` |
| `upstream_system_warehouse_candidate` | 主键：`connection_code + warehouse_code` |
| `upstream_system_logistics_channel_candidate` | 主键：`connection_code + warehouse_code + channel_code` |
| `upstream_system_sku_candidate` | 主键：`connection_code + master_sku` |
| `upstream_system_sku_sync_state` | 主键：`connection_code` |
| `upstream_system_request_log` | 通过 `connection_code` 查询和统计 |

配对表当前规则也符合前面确认：

- 仓库配对：`system_warehouse_code` 全局唯一，系统仓库只能配一次；`connection_code + upstream_warehouse_code` 唯一，同一主仓下上游仓库只能配一次。
- 物流渠道配对：`system_channel_code` 全局唯一，系统渠道只能配一次；同一上游渠道允许配多个系统渠道，所以 `connection_code + upstream_channel_code` 只是普通索引。
- SKU 配对：`connection_code + master_sku` 和 `connection_code + system_sku` 分别唯一，按主仓接入隔离。

## 运行库数据核查结果

只读查询结果显示，实际同步清单并没有互相覆盖：

| connection_code | 主仓 | 仓库同步清单 |
| --- | --- | --- |
| `LX-CA012` | `CA012` | `CA91244744 / MEISU` |
| `LX-NY013-3275A1E1` | `NY013` | `NY11751 / 富东海外仓` |

其他同步数据也分别存在：

| connection_code | 物流渠道数 | SKU 数 | SKU 同步状态 |
| --- | ---: | ---: | --- |
| `LX-CA012` | 7 | 5401 | `FRESH` |
| `LX-NY013-3275A1E1` | 3 | 5333 | `FRESH` |

同时未发现同一个 `warehouse_code + warehouse_name` 出现在多个 `connection_code` 下。

## 具体原因

根因在前端，不是表结构。

`react-ui/src/pages/UpstreamSystem/components/SyncTabs.tsx` 中仓库、物流渠道、请求日志三个 `ProTable` 的 `request` 使用了闭包里的 `selectedCode`，但没有把 `selectedCode` 放进 `params` 或 `key`。因此左侧切换主仓时：

- 右侧摘要区会立即换成新主仓，因为它直接读取 `selectedConnection`。
- 下方 `ProTable` 不一定重新请求，仍保留上一主仓的数据。
- 这就造成“标题是 NY013，表格还是 CA012”的错觉。

还有一个时序问题：

`reloadCurrent()` 里先 `fetchConnections()` 再立刻 `reloadTabs()`。React 的 `selectedConnection` 状态更新不是同步提交的，`reloadTabs()` 触发时，表格里的 `selectedCode` 闭包可能仍是旧主仓。所以同步后也可能继续刷新旧主仓表格。

`SkuSyncPanel` 已经把 `selectedCode` 放进 `params`，所以它比仓库、物流、日志三个表更不容易出现这个问题。

## 修复方案

### 方案 A：最小修复，推荐立即做

1. `SyncTabs` 内给仓库、物流渠道、请求日志 `ProTable` 增加 `params={{ selectedCode }}`。
2. 给这几个表增加和 `selectedCode` 相关的 `key`，例如 `key={`warehouse-${selectedCode}`}`，切换主仓时强制丢弃旧表格状态。
3. 父页面增加 `useEffect(() => reloadTabs(), [selectedCode])` 或者把刷新动作收敛到 `SyncTabs` 内，让主仓变化成为唯一刷新触发源。
4. 调整 `reloadCurrent()`：同步成功后只刷新主仓列表并选中目标主仓，不立刻用旧闭包 reload 表格；等 `selectedCode` 变化后统一触发表格 reload。

优点：改动小，直接解决截图问题。

风险：表格会在切换主仓时重新挂载，当前页码、筛选状态会重置。这是合理行为，因为不同主仓的数据不应该复用旧分页状态。

### 方案 B：增强前端防串用

在方案 A 基础上：

1. 仓库表 rowKey 改为 `selectedCode + ':' + warehouseCode`。
2. 物流渠道表 rowKey 改为 `selectedCode + ':' + channelCode`。
3. 请求返回后做一次轻量断言：当前 `selectedCode` 已变化时丢弃旧请求结果，避免慢请求回写旧数据。

优点：能防止快速切换主仓时的异步请求回填。

风险：代码略多，但仍属于前端层防护，不改后端和表。

### 方案 C：后端响应增加防误读字段

接口返回仓库、物流渠道、SKU 同步清单时保留 `connectionCode`，前端表格可显示或调试校验。

目前后端 DTO 已经带 `connectionCode`，不需要改表；只是前端可以在调试或隐藏字段中利用它做断言。

## 推荐执行顺序

1. 先做方案 A。
2. 同时加方案 B 的 rowKey 和慢请求防护。
3. 不改表，不迁移数据。
4. 浏览器验证：
   - 默认进入 `CA012`，仓库表显示 `CA91244744 / MEISU`。
   - 切换 `NY013`，仓库表显示 `NY11751 / 富东海外仓`。
   - 再切回 `CA012`，仓库表恢复 `CA91244744 / MEISU`。
   - 对 `NY013` 点同步后，摘要和仓库/物流/SKU/日志仍保持 `NY013` 数据。

## 当前不建议做的事

- 不要改表结构。当前运行库表结构已经按 `connection_code` 隔离。
- 不要清空同步清单。库里数据本身是区分开的，清空不能解决前端缓存问题。
- 不要把新增主仓逻辑改回只支持一条主仓。问题不是多主仓模型，而是前端表格刷新边界。

## 修复执行记录（2026-06-05）

### 已修复问题

- 已修正 `react-ui/src/pages/UpstreamSystem/index.tsx` 的同步后刷新时序：刷新主仓列表后，只有当前选中主仓未变化时才立即 reload 右侧表格，避免用旧 `selectedCode` 闭包刷新。
- 已给 `SyncTabs` 按当前 `selectedCode` 重挂载：切换主仓时，右侧仓库、物流、SKU、日志区域重新创建，不再保留上一主仓的表格状态。
- 已给仓库、物流、请求日志三个 `ProTable` 加入 `params={{ selectedCode }}`，并把仓库、物流表 `rowKey` 改为包含 `selectedCode` 的组合 key，避免跨主仓复用行缓存。
- 已在仓库、物流、日志请求中固定本次请求的 `requestCode`，保证接口调用明确绑定当前主仓。

### 未修改内容

- 未修改后端接口。
- 未修改表结构。
- 未执行 DDL/DML。
- 未清空或迁移任何同步清单数据。

### 验证命令

| 命令 | 结果 |
| --- | --- |
| `npx biome check --write src/pages/UpstreamSystem` | 通过，自动整理 3 个文件 |
| `npm run build` | 通过 |
| `npm run tsc` | 通过 |
| `codegraph sync .` | 通过，首次同步 `Synced 7 changed files`，最终复跑 `Already up to date` |

### 浏览器验证

验证页面：`http://127.0.0.1:8001/overseas-warehouse-service/upstream-system`

| 操作 | 验证结果 |
| --- | --- |
| 登录后默认查看 `CA012` | 右侧仓库清单显示 `CA91244744 / MEISU` |
| 左侧切换到 `NY013` | 右侧仓库清单显示 `NY11751 / 富东海外仓` |
| 再切回 `CA012` | 右侧仓库清单恢复 `CA91244744 / MEISU` |

浏览器请求日志也确认切换时分别调用了：

- `/api/integration/admin/upstream-systems/LX-CA012/warehouses`
- `/api/integration/admin/upstream-systems/LX-NY013-3275A1E1/warehouses`

### 残留问题

- 页面控制台仍有一条 Ant Design `useForm` 未连接 Form 的提示，发生在页面加载阶段；它不是本次主仓数据串用的原因，本次未展开处理。
