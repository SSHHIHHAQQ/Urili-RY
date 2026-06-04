# 上游系统管理布局与 SKU 体验优化目标追踪

日期：2026-06-04

状态：进行中。

## 总目标

按若依开发规范优化“上游系统管理”，在不照搬旧项目视觉设计的前提下，补齐旧项目已成熟的主仓工作台逻辑和 SKU 管理体验。

## 本轮边界

- 前端业务改动只进入 `react-ui/`。
- 后端业务改动只进入 `RuoYi-Vue/`。
- 不恢复 Vue 前端 `ruoyi-ui`。
- 不直接复制旧项目代码，只参考业务逻辑和分布结构。
- 不新增箱配对、增值服务配对、导入配对、自动配对、SKU 详情等旧项目占位入口。
- 不物理删除主仓接入，继续使用启停。
- 本轮优先复用现有表结构；如后续确需新增统计字段，再单独写数据库执行记录并按规则执行。

## 目标清单

| 序号 | 事项 | 状态 | 完成标准 |
| --- | --- | --- | --- |
| 1 | 目标追踪 | 进行中 | 生成本文件并持续更新 |
| 2 | 当前代码核对 | 进行中 | 核对若依现有 Controller/Service/Mapper/API/React 页面，避免重复实现 |
| 3 | 左侧主仓列表 | 未开始 | 页面改为左侧主仓列表选择，支持搜索、分组、选中态 |
| 4 | 右侧基本信息 | 未开始 | 右上展示当前主仓摘要和操作按钮，随左侧选择变化 |
| 5 | 右侧配置 Tabs | 未开始 | 保留仓库、物流渠道、SKU、请求日志 Tabs，并随当前主仓变化 |
| 6 | 主仓排序 | 未开始 | 新增若依风格排序接口和左侧调整位置交互，保存 `display_order` |
| 7 | SKU 同步摘要 | 未开始 | SKU Tab 展示状态、上次同步、下次同步 |
| 8 | SKU 搜索增强 | 未开始 | 支持全部、领星 masterSku、产品名、系统 SKU、客户字段筛选 |
| 9 | SKU 客户字段 | 未开始 | SKU 配对弹窗展示并保存 `customerName` |
| 10 | SKU-only 同步 | 未开始 | 新增只同步 SKU 的后端接口和前端按钮 |
| 11 | 并发保护 | 未开始 | 避免同一主仓重复触发 SKU 同步 |
| 12 | 权限同步 | 未开始 | 新增/调整接口继续使用若依 `@PreAuthorize`、菜单按钮权限和前端权限控制 |
| 13 | 文档记录 | 未开始 | 更新复用台账和阶段总结 Markdown |
| 14 | 验证 | 未开始 | 后端构建、前端 tsc/build/lint、新页面浏览器验证、接口验证 |

## 暂缓项

- SKU 同步精确统计字段：`totalFetched`、`inserted`、`updated`、`markedMissing`。
- 若依定时任务每 10 分钟后台同步 SKU。
- `resolveMasterSku(connectionCode, systemSku)` 下游硬拦截服务。

暂缓原因：这些涉及更明确的后续业务调用或表结构/调度配置，本轮先把管理端工作台和手动同步体验做稳。

## 验证要求

- `mvn -DskipTests install`
- `npm run tsc`
- `npx biome lint src/pages/UpstreamSystem src/services/integration src/types/integration`
- `npm run build`
- 浏览器打开 `http://127.0.0.1:8001/overseas-warehouse-service/upstream-system`
- `admin` 账号可访问和操作；无权限账号接口仍返回 403
- 真实 `LX-CA012` 数据不丢失，凭证不明文输出
