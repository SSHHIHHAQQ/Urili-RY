# 2026-06-07 三端 P0/P1：商品 Mapper、端内菜单区间与管理端授权签名收口记录

## 范围

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理当前快速推进模式中的 P0/P1：编译、guard、接口、权限、串端、service/字段缺失。不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent

- 按用户最新要求先尝试 6 个 `gpt-5.3-codex-spark` 只读子 Agent；平台返回用量限制，失败 Agent 已关闭。
- 随后回退使用并关闭 6 个 `gpt-5.4` 只读子 Agent，覆盖 seller/buyer 后端、portal 认证权限日志、SQL seed、React guard、integration/product/warehouse 和验证文档。
- 采纳的 P0/P1：商品 SPU resultMap 串入 SKU 绑定字段、terminal 菜单 seed 缺少 ID 区间前置 guard、admin partner 授权/清理脚本菜单签名不够完整。
- 排除的误报：seller/buyer 后端 `@Log` 与 `ServiceException` 字符串编译错误为编码显示误报，`mvn -pl seller,buyer -am -DskipTests compile` 已在前序排查中通过。

## 已完成

- `ProductDistributionMapper.xml` 删除 `ProductSpuResult` 中误映射到 `ProductSpu` 不存在的 SKU 来源绑定字段，避免商品 SPU 列表/详情运行时 resultMap 写入不存在属性。
- 新增 `ProductDistributionMapperContractTest`，固定 `ProductSpuResult` 不得映射 SKU 来源绑定字段，`ProductSkuSourceBindingResult` 保留绑定字段。
- `verify-three-terminal.mjs` 纳入 `ProductDistributionMapperContractTest`，总验证入口必须执行该商品 mapper 合同。
- 所有当前会 `insert into seller_menu` / `insert into buyer_menu` 的 terminal 权限 seed 增加 `assert_terminal_menu_range_ready()`：
  - 校验目标端菜单表存在。
  - 校验 seller 菜单 ID 在 `100000-199999`。
  - 校验 buyer 菜单 ID 在 `200000-299999`。
  - 校验对应 `AUTO_INCREMENT` 不低于区间起点。
  - 调用点位于 terminal 菜单插入之前。
- `TerminalSqlIsolationContractTest` 新增自动发现合同，扫描 `RuoYi-Vue/sql` 下所有写 terminal 菜单的 SQL，强制要求区间 guard。
- `20260606_admin_partner_role_menu_grant.sql` 和 `20260606_admin_partner_non_admin_button_grant_cleanup.sql` 增加 admin partner 页面 `path/component/route/perms` 完整签名校验。
- admin partner 按钮授权/清理增加 `child.path='#'`、`child.component=''`、`child.route_name=''` 条件，避免脏按钮菜单被误授权或误清理。
- `AdminDirectLoginPermissionContractTest` 同步固定页面签名和按钮签名条件。
- 更新 `docs/architecture/reuse-ledger.md` 和目标追踪文档。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl product -am "-Dtest=ProductDistributionMapperContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过；`ProductDistributionMapperContractTest` 1 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=TerminalSqlIsolationContractTest,AdminDirectLoginPermissionContractTest" test`
  - 通过；`TerminalSqlIsolationContractTest` 12 个测试通过，`AdminDirectLoginPermissionContractTest` 1 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过；前端 6 个 Jest suite / 30 个测试通过。
  - 后端三端合同通过：ruoyi-system 143、ruoyi-framework 15、integration 4、product 2、seller 89、buyer 90 个测试通过。

## 边界

- 未执行任何远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器、截图、DOM 或 UI 细调验收。

## 残留 P1

- 商品库存聚合字段当前仍有后端硬编码空值和前端展示之间的业务衔接缺口，属于商品/库存读模型后续切片，不在本轮 P0 修复范围内。
- `product` 直接依赖 integration `impl` 和 mapper 直接读写 integration 表的模块边界问题仍需后续单独设计，不在本轮快速修补范围内。
- 商品编辑页调用来源商品库和仓库 admin 接口的权限依赖仍需后续用 product-scoped 选择接口或显式权限 guard 收口。
