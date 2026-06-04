# 上游系统管理菜单目标追踪

日期：2026-06-04

状态：首版已落地并完成真实联调验证。

## 总目标

在 `E:\Urili-Ruoyi` 中完成“上游系统管理”菜单迁移与若依化落地，实现领星主仓接入、真实授权与同步、仓库配对、物流渠道配对、SKU 配对、菜单权限、审计/日志、前后端联调和验证记录。

## 已确认输入

- 主仓名称：`CA012`
- 接入编号：`LX-CA012`
- 领星凭证：已加密保存；页面、接口响应、日志和报告只展示脱敏信息。
- 系统仓库配对规则：系统仓库只能配对一次；同一主仓接入下领星仓库只能配对一次。
- 物流渠道配对规则：同一个领星主仓渠道可以配对多个系统渠道；系统渠道只能配对一次。
- 系统仓库和系统物流渠道来源：第一版手动录入 `code + name`。
- 菜单位置：按当前若依工程位置放在“海外仓服务设置”下。
- 页面文案：使用“同步清单”，不使用“候选”。
- MySQL 搜索方案：第一版使用 `like + 普通索引`；本次真实 SKU 5400 条分页查询可用。
- code 口径修正：上游系统类型和结算类型沿用旧项目 code，新增主仓时显式选择上游系统类型。

## 范围内工作

| 序号 | 事项 | 状态 | 当前结果 |
| --- | --- | --- | --- |
| 1 | 执行前配置确认 | 已完成 | 已读取 `application.yml`、`application-druid.yml`，按当前激活配置和 `.env.local` 连接实际 MySQL/Redis |
| 2 | 数据库执行记录 | 已完成 | 已生成并更新 `docs/plans/2026-06-04-upstream-system-management-db-execution-record.md` |
| 3 | 后端 `integration` 模块 | 已完成 | 已新增 Maven 模块并接入 `ruoyi-admin` |
| 4 | 数据表与 SQL | 已完成 | 已新增接入、同步清单、配对、同步状态、请求日志、SKU 配对审计相关表 |
| 5 | 字典与权限种子 | 已完成 | 已新增菜单、按钮权限和上游系统相关字典 |
| 6 | 领星适配器 | 已完成 | 已实现签名、授权校验、仓库/物流渠道/SKU 拉取和脱敏日志 |
| 7 | 主仓接入与凭证保存 | 已完成 | `LX-CA012` 已加密保存真实凭证，页面展示脱敏 Key |
| 8 | 仓库同步清单 | 已完成 | 真实同步仓库 1 个 |
| 9 | 仓库配对 | 已完成 | 临时配对验证通过，重复系统仓库被拒绝，临时配对已删除 |
| 10 | 物流渠道同步清单 | 已完成 | 真实同步物流渠道 7 条，页面按渠道聚合展示 |
| 11 | 物流渠道配对 | 已完成 | 同一领星渠道多系统渠道验证通过，重复系统渠道被拒绝，临时配对已删除 |
| 12 | SKU 同步清单 | 已完成 | 真实同步 SKU 5400 条，分页查询可用 |
| 13 | SKU 配对 | 已完成 | 临时 SKU 配对创建/重复拒绝/解除验证通过，临时配对已删除 |
| 14 | 请求日志 | 已完成 | 请求日志 107 条，抽查无明文 `appSecret` 或凭证字段，敏感签名字段脱敏 |
| 15 | React 页面 | 已完成 | 已拆分为页面、组件、service、types、constants、helpers |
| 16 | 复用台账 | 已完成 | 已更新 `docs/architecture/reuse-ledger.md` |
| 17 | 构建与测试 | 已完成 | 后端 Maven、前端 tsc/build、新增目录 lint、真实同步、权限和浏览器联调已验证 |
| 18 | code 口径修正 | 已完成 | `systemKind` 改为新增时可选；结算类型回到 `upstream-payable`、`self-operated-receivable`；编号生成回到 `LX-{主仓名前缀}-{8位随机后缀}` |
| 19 | 阶段总结 | 进行中 | 待生成本次首版落地总结 Markdown |

## 范围外工作

- 不做箱规配对正式保存。
- 不做增值服务配对正式保存。
- 不做订单推送、履约状态回传、库存同步、费用入账。
- 不物理删除主仓接入。
- 不恢复 Vue 前端 `ruoyi-ui`。
- 不从旧项目直接复制代码。

## 当前验证结果

| 类型 | 命令或动作 | 状态 |
| --- | --- | --- |
| 数据源确认 | 读取后端激活配置和 `.env.local` 数据源变量 | 已完成 |
| SQL 执行 | `RuoYi-Vue/sql/upstream_system_management_seed.sql` | 已完成，20 条语句执行成功 |
| 后端构建 | `mvn -DskipTests install` | 已通过 |
| 前端类型检查 | `npm run tsc` | 已通过 |
| 新增目录 lint | `npx biome lint src/pages/UpstreamSystem src/services/integration src/types/integration` | 已通过 |
| 前端构建 | `npm run build` | 已通过 |
| code 口径修正后端编译 | `mvn -pl integration -am -DskipTests package` | 已通过 |
| code 口径修正前端检查 | `npx biome check --write src/pages/UpstreamSystem src/types/integration/upstream-system.d.ts`、`npm run build` | 已通过 |
| code 口径修正类型检查 | `npm run tsc` | 曾通过；补充旧值归一化后再次运行时被无关 `PartnerAuditModal.tsx` 类型错误阻断 |
| code 口径浏览器验证 | 新增/编辑主仓弹窗显示“上游系统类型”，默认/归一为“领星WMS”；结算类型默认/归一为“上游仓（应付）” | 已通过 |
| 全量前端 lint | `npm run biome:lint` | 未通过，失败来自历史文件 `src/app.tsx`、`src/components/IconSelector/*` 等，不是本次新增目录 |
| 浏览器验证 | 登录管理端并打开“海外仓服务设置 / 上游系统管理” | 已通过 |
| 真实授权和同步 | 使用真实 `LX-CA012` 调用授权和同步 | 已通过，仓库 1、渠道 7、SKU 5400 |
| 配对验证 | 仓库、物流渠道、SKU 临时配对和重复拦截 | 已通过，临时配对已删除 |
| 权限验证 | `admin` 可访问，默认 `ry` 访问接口返回 403 | 已通过 |
| 日志验证 | 请求日志脱敏抽查 | 已通过 |

## 残留事项

- `UpstreamSystemServiceImpl.java` 当前 699 行，超过 500 行阈值；首版保留在一个 Service 中是为了集中事务、同步状态、配对约束和审计写入。后续如果加入订单、库存、费用或更多外部系统，应拆出同步任务服务、配对服务和请求日志查询服务。
- 全量 `biome:lint` 仍有历史问题，未在本次任务中清理，避免改动无关文件。
- `RuoYi-Vue/sql/20260604_upstream_system_code_correction.sql` 已生成但未直接执行远端数据库；执行前需按数据源确认规则单独记录。
- 最新一次 `npm run tsc` 失败来自无关 `src/components/PartnerManagement/PartnerAuditModal.tsx` 与 partner service 类型不匹配；本次未处理该模块。
