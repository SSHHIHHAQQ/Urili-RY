# 三端隔离 P0/P1 快速推进：商品编辑权限入口与 Guard Manifest 收口记录

日期：2026-06-08

## 参考方向

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，只处理 P0/P1：

- 编译
- guard
- 接口
- 权限
- 串端
- service/字段缺失

本轮不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 子 Agent 使用情况

- 历史记录（已过期口径）：优先尝试 6 个 GPT-5.3 Codex 子 Agent，工具模型为 `gpt-5.3-codex-spark`。
- 平台返回额度限制，提示需等到 `2026-06-14 15:12` 后再试；6 个失败的 5.3 子 Agent 已关闭。
- 按 fallback 规则启动 6 个 `gpt-5.4` 只读子 Agent；6 个已全部完成并关闭。
- 主 Agent 采纳确定 P1，P2 或超出当前小切片的治理项记录但不阻塞。

## 采纳的 P1

### 1. 商品编辑入口与路由权限语义不一致

商品编辑路由已要求 `product:distribution:query + product:distribution:edit`，且 `authorityMode = all`。但列表页编辑按钮只判断 `product:distribution:edit`，会导致只有 edit、缺少 query 的用户看到编辑入口，点击后进入前端 403。

### 2. `--check-manifest` 对 guard 脚本目标文件 fail-open

`verify-three-terminal` 已校验 manifest 中 guard 脚本名称和 `package.json` 命令字符串一致，但没有校验 `node scripts/*.mjs` 指向的脚本文件是否真实存在。脚本被删或路径失效时，`--check-manifest` 仍可能通过。

## 已完成

- `ProductDistribution/index.tsx` 新增 `canEditDistributionProduct = query && edit`，SPU 和 SKU 两处编辑入口统一使用该条件。
- `ProductDistribution/EditPage.tsx` 新增 `canEditDistributionProduct`，编辑页保存按钮和 `submit()` 入口复用同一条件。
- `product-distribution-permission-guard.test.ts` 增加契约测试，固定编辑入口、编辑页动作与路由 guard 一致。
- `verify-three-terminal.mjs` 增加 `assertFrontendGuardScriptTargetsExist()`，在 manifest 校验阶段解析 `node scripts/*.mjs` 并校验目标文件存在。

## 验证结果

- `cd E:\Urili-Ruoyi\react-ui; node --check scripts\verify-three-terminal.mjs`：通过。
- `cd E:\Urili-Ruoyi\react-ui; node scripts\verify-three-terminal.mjs --check-manifest`：通过。
- 历史记录（已过期命令口径）：`cd E:\Urili-Ruoyi\react-ui; npm run test:unit -- --runTestsByPath tests/product-distribution-permission-guard.test.ts --runInBand`：当时通过，1 个 suite / 5 个测试；当前公开 `npm run test:unit` 入口已收口为 `verify-three-terminal`，复核请使用 `npm run verify:three-terminal` 或直接调用 Jest 二进制。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc -- --pretty false`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过。
  - 4 个前端 guard 通过。
  - React typecheck 通过。
  - 12 个 Jest suite / 66 个测试通过。
  - 后端 reactor `test-compile` 通过。
  - 后端三端合同测试通过。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML。
- 本轮未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。

## 记录但不阻塞

- SQL 子 Agent 指出 `seller_buyer_management_seed.sql` 是历史综合 seed，`PATCH_EXISTING` 里的高影响 DML 仍缺少统一的精确 target count/signature 与事务/post-check 收口；该文件拆分和精确目标治理范围较大，后续应单独做 SQL 治理切片。
- 日期前缀 SQL 自动发现当前已 fail-closed 校验确认 token，但还未对所有历史 seed 一刀切要求 target signature。由于现有历史 seed 类型混杂，直接加全局规则会误伤大量已确认初始化脚本，后续应先分类再收紧。
- 新增关键测试自动发现仍依赖命名和路径启发式。当前三端相关关键测试已在 manifest 中；finance/inventory/warehouse 等非三端业务测试不在本轮强行纳入。
