# 2026-06-08 三端 P0/P1：Full Verifier、SQL Exact Target Guard 与文档漂移收口记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前执行快速推进模式：只处理 P0/P1，包括编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用

- 按用户最新要求，本轮 6 个子 Agent 全部使用 `gpt-5.4`，未再把 GPT-5.3 Codex 作为首选。
- 6 个子 Agent 覆盖：三端 verifier、seller/buyer 后端隔离、portal token/direct-login、SQL seed guard、React 管理端模板、Markdown/AGENTS 口径一致性。
- 6 个子 Agent 均已完成并关闭。
- 采纳的 P1：
  - `20260608_product_center_menu_seed.sql` 缺少对将被改写的 `sys_menu` 展示字段做 exact target count/signature。
  - `20260608_product_review.sql` 缺少对 `sys_menu`、`sys_dict_type`、`sys_dict_data` 目标集合做 exact target count/signature。
  - 多份旧 Markdown 仍把 GPT-5.3 优先或默认密码重置写成未过期口径。
- 复核后降级为 P2/假红：
  - seller portal product 指定测试在不带 `-am` 的局部 Maven 命令下出现 `NoSuchMethodError`，根因是读取本机 Maven 仓库旧 `product` 构件；正式三端 verifier 和带 `-am` 的定向命令均通过。

## 已完成

- `RuoYi-Vue/sql/20260608_product_center_menu_seed.sql`
  - 增加 `@product_center_menu_seed_expected_count` 与 `@product_center_menu_seed_expected_signature`。
  - 新增 `assert_product_center_menu_seed_targets()`，在写 `sys_menu` 前按当前目标集合的 count + SHA-256 signature fail-closed。
  - exact signature 覆盖 `menu_name/order_num/path/component/route_name/perms/icon/remark` 等菜单展示和权限语义字段。
- `RuoYi-Vue/sql/20260608_product_review.sql`
  - 增加 `@product_review_menu_*`、`@product_review_dict_type_*`、`@product_review_dict_data_*` 三组 expected count/signature。
  - 新增 `assert_product_review_seed_target_signatures()`，在建表和写入菜单/字典前锁定预览目标集合。
- `SqlExecutionGuardContractTest`
  - 固定上述 exact target guard、错误信息、执行顺序和清理过程，防止后续回退到只校验局部槽位。
- `docs/architecture/reuse-ledger.md`
  - 记录 seller/buyer portal 商品服务定向测试必须带 `-am`，避免不带 reactor 依赖时读取旧构件造成假红。
- 旧 Markdown 口径清理：
  - 对旧默认密码重置、`resetOwnerPassword`、`resetDefaultPassword`、GPT-5.3 优先等容易误读为当前规则的行补充“历史记录（已过期口径）”。
  - 当前规则保持：子 Agent 默认 `gpt-5.4`；账号重置密码只保留人工临时密码 `resetPwd`。

## 验证

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=SqlExecutionGuardContractTest" test`
  - 通过：75 个测试。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -q -pl seller -am "-Dtest=SellerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -q -pl buyer -am "-Dtest=BuyerPortalProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`
  - 通过：4 个前端 guard、React typecheck、20 个 Jest suite / 148 个测试、后端 reactor `test-compile`、后端三端合同测试全部通过。
  - 后端三端合同统计：`ruoyi-system` 199、`ruoyi-framework` 16、`finance` 9、`inventory` 1、`integration` 6、`product` 35、`seller` 96、`buyer` 97 个测试。

## 未执行

- 未执行远端 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未做浏览器运行态、截图、DOM 或 UI 细调验收。

## 残留

- 本轮没有确认新的 P0/P1 残留。
- P2 仅记录不阻塞：
  - `Product` / `UpstreamSystem` 仍有部分 `.js` 镜像不是纯 re-export，后续改同类运行入口时仍需注意双份源码漂移。
  - `verify-three-terminal` 的关键测试发现仍以当前命名模式为准，未来若新增 `*Tests.java` 这类命名需要扩展规则。
  - `check:compact-date-range` 不是 `guard:*`，不属于当前三端 verifier 的 P0/P1 阻断范围。
