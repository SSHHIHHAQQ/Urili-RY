# 库存调整审核 SQL 执行记录

日期：2026-06-09

## 用户确认

- 用户已确认执行库存调整审核迁移。

## 数据源确认

- 激活配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`
- 激活 profile：`druid`
- MySQL 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`
- 连接变量来源：本机 `.env.local` 的 `RUOYI_DB_URL` / `RUOYI_DB_USERNAME` / `RUOYI_DB_PASSWORD`
- 目标环境：远端 MySQL
- 目标库：远端运行库，名称已脱敏
- Redis：本次 SQL 不访问 Redis；仅确认 Redis 仍由 `RUOYI_REDIS_*` 变量提供

## 执行范围

- 执行脚本：`RuoYi-Vue/sql/20260609_inventory_adjustment_review.sql`
- 执行命令类型：DDL + 菜单/任务/默认策略 DML
- 影响范围：
  - 新增库存调整审核策略、绑定、审核单、操作日志、SKU 日销量聚合表。
  - 菜单 `2452` 从库存调整审核占位页切换到真实页面。
  - 新增库存调整审核按钮权限。
  - 新增 Quartz 到期自动生效任务。
- 不包含：业务库存数据变更、审核单历史数据回填、WMS 外部库存动作。

## 回滚方式

- 默认不自动回滚：当前后端和前端已依赖库存调整审核表、菜单、按钮权限、默认策略和 Quartz 到期任务。
- 如需回滚，必须先部署不再读写库存调整审核表和策略字段的代码版本，并停用对应 Quartz 任务。
- 受控回滚顺序应为：停用/删除新增 Quartz 任务，移除库存调整审核按钮权限并恢复菜单占位，确认无审核单业务数据后再处理新增审核表、策略表和聚合表。
- 如已产生审核单、操作日志或策略绑定，不得直接删除历史事实；必须先给出业务作废/迁移方案并单独确认。

## 执行通道

- 本机未安装 `mysql` CLI。
- Docker CLI 存在，但 Docker daemon 当前未运行。
- 本次采用本机 Maven 缓存中的 `mysql-connector-j`，通过 JShell/JDBC 执行迁移脚本。
- 脚本执行前会先计算 `sys_menu` 与 `sys_job` 精确目标集合的 count/signature，并作为迁移 guard 变量传入。

## SQL 确认 token

- 执行前设置 `@confirm_inventory_adjustment_review = 'APPLY_INVENTORY_ADJUSTMENT_REVIEW'`。
- 执行前设置 `@inventory_adjustment_review_menu_expected_count = '1'`。
- 执行前设置 `@inventory_adjustment_review_menu_expected_signature = 'd07ae07afd09c4e564504174c05772bf3da1d253eae86d93a81a0669365d448a'`。
- 执行前设置 `@inventory_adjustment_review_job_expected_count = '0'`。
- 执行前设置 `@inventory_adjustment_review_job_expected_signature = 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'`。
- 脚本入口 `assert_inventory_adjustment_review_confirmed()` 会在 token、count 或 signature 缺失/格式错误时 `45000` fail-closed；目标集合签名不匹配时由 `assert_inventory_adjustment_review_target_signatures()` 拒绝执行。

## 执行结果

- 执行成功。

## 执行前预览签名

- 目标库：远端运行库，名称已脱敏
- `sys_menu` 精确目标集合：
  - count：`1`
  - signature：`d07ae07afd09c4e564504174c05772bf3da1d253eae86d93a81a0669365d448a`
- `sys_job` 精确目标集合：
  - count：`0`
  - signature：`e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`

## 执行过程修正

- 首次 JDBC 预跑发现远端 MySQL 不接受 `set session group_concat_max_len = greatest(...)`，已改为固定值 `1048576`。
- 正式执行时发现 MySQL 临时表不能在同一个查询中重复引用，已将菜单 slot guard 与 completion assert 改为单次引用或分步变量计算。
- 中间失败均发生在幂等 DDL 或未提交事务阶段；最终以修正后的完整脚本重新执行成功。

## 执行后核验

- 新增/确认表数量：`5`
- 菜单 `2452` 组件：`Inventory/AdjustmentReview/index`
- 库存调整审核按钮权限数量：`6`
- Quartz 任务数量：`1`
- 默认审核策略数量：`1`
- 默认全局策略绑定数量：`1`

## 验证命令

- `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest#inventoryAdjustmentReviewMustUseExactTargetsAndCompletionAssert" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：通过，`1` 个测试。

## 备注

- 复跑完整 `SqlExecutionGuardContractTest` 时，失败点在无关脚本 `20260606_admin_partner_role_menu_grant.sql` 的既有合同，不属于库存调整审核迁移；本次只将库存调整审核对应合同方法作为迁移后验证依据。
