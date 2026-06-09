# 2026-06-08 三端 P0/P1 快速推进记录：商品分销权限依赖与 JS 镜像收敛

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 历史记录（已过期口径）：按当时用户规则优先尝试 6 个 `gpt-5.3-codex-spark` 子 Agent；现行规则为默认使用 `gpt-5.4`。
- 平台返回额度限制，提示需等到 `2026-06-14 15:12` 后再试；失败 Agent 已关闭。
- 按 fallback 规则启动并关闭 6 个 `gpt-5.4` 只读子 Agent。
- 采纳的 P1：
  - 商品分销列表和编辑页存在无权限时仍请求卖家、类目、详情、类目 schema 等依赖接口的风险。
  - Product 与 UpstreamSystem 部分 `.js` 镜像仍保留完整实现，存在与 `.tsx` 源分叉风险。
- 未发现新的确定 P0。

## 已修复问题

- `Product/Distribution/index.tsx` 增加依赖接口权限 guard：
  - 只有具备 `seller:admin:list` 时才请求管理端卖家列表。
  - 只有具备 `product:category:list` 时才请求商品类目列表。
  - 无权限时设置为空选项，不发起后台接口请求。
- `Product/Distribution/EditPage.tsx` 增加依赖接口权限 guard：
  - 编辑详情加载前检查 `product:distribution:query`，无权限时提示并返回商品分销列表，不请求详情接口。
  - 只有具备 `seller:admin:list` 时才请求卖家列表。
  - 只有具备 `product:category:list` 时才请求商品类目列表。
  - 只有具备 `product:categoryAttribute:preview` 时才请求类目 schema。
  - 继续保留来源 SKU、官方仓、三方仓已有权限 guard。
- Product 与 UpstreamSystem 的 `.js` 镜像改为纯 re-export，真实逻辑只维护 TS/TSX 源：
  - `Product/Category/index.js`
  - `Product/Attribute/components/AttributeLibrary.js`
  - `Product/Distribution/index.js`
  - `Product/Distribution/EditPage.js`
  - `UpstreamSystem/components/SkuSyncPanel.js`
- `product-distribution-permission-guard.test.ts` 固定商品分销依赖接口权限 guard 和 Product JS 纯 re-export 合同。
- `upstream-system-permission-guard.test.ts` 固定 UpstreamSystem 组件 JS 纯 re-export 合同。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- 历史记录（已过期命令口径）：`cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/product-distribution-permission-guard.test.ts tests/upstream-system-permission-guard.test.ts --runInBand`：当时通过，2 个 suite / 6 个测试；Jest 仍有既有 open handle 提示。当前公开 `npm run test:unit` 入口已收口为 `verify-three-terminal`，复核请使用 `npm run verify:three-terminal` 或直接调用 Jest 二进制。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；4 个前端 guard、React typecheck、11 个 Jest suite / 54 个测试、后端 reactor `test-compile` 和三端合同测试全部通过。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 9 个变更文件，`Modified: 9 - 130 nodes in 908ms`。

## 权限、字典、复用与边界

- 权限检查结果：本轮未新增后端接口；前端只补齐已有依赖接口的权限 gate，避免无权限角色进入页面后触发额外 403。
- 字典/选项复用检查结果：本轮未新增字典或选项字段。
- 复用台账检查结果：本轮未新增公共业务组件或公共后端服务；JS 镜像改纯 re-export 后减少重复实现，不需要追加复用台账条目。
- 重复代码检查结果：已消除 Product/UpstreamSystem 多处 JS/TS 双实现分叉风险。
- 大文件合理性判断结果：本轮主要修改现有页面 guard、镜像文件和合同测试；未新增需要拆分的单一职责业务大文件。
- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮按用户要求未做浏览器、截图、DOM 或 UI 细调验收。

## 残留问题

- P2：`20260606_terminal_log_scope_indexes.sql` 可补文件级 SQL 合同，固定日志 scope 索引策略。
- P2：端内权限 seed helper 可补 `assert_terminal_menu_range_ready` 级别的更强入口断言。
- P2：`requestErrorConfig.ts` 与 `app.tsx` 的 401 处理存在重复逻辑，后续可抽一个端隔离清 token helper。
- P2：`verify:three-terminal` 是三端隔离与关键合同守门，不等同完整浏览器运行态回归；按当前快速推进模式不做浏览器验收。
