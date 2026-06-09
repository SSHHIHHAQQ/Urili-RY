# 切片 5 只读 P0/P1 审计：`verify-three-terminal` / manifest / guard / package scripts / generated dirs

- 审计范围：`react-ui/scripts/verify-three-terminal.mjs`、`react-ui/tests/three-terminal.manifest.json`、guard scripts、`react-ui/package.json`、Jest/Umi setup、`RuoYi-Vue/pom.xml`、当前未跟踪生成目录
- 审计方式：只读；未改业务代码
- 结论：发现 `P1 x 2`，`P0 x 0`

## P1

### 1. `tsc` 仍会把未跟踪的 `src/.umi-undefined` 当源码参与验证，三端总门对“生成目录污染类型检查”未 fail-closed

- 证据：
  - [react-ui/tsconfig.json](E:/Urili-Ruoyi/react-ui/tsconfig.json:21) 仅用 `include: ["./**/*.d.ts", "./**/*.ts", "./**/*.tsx"]`，没有排除 `src/.umi-undefined`
  - [react-ui/scripts/verify-three-terminal.mjs](E:/Urili-Ruoyi/react-ui/scripts/verify-three-terminal.mjs:34) 只是在“前端测试发现”阶段忽略 `.umi-undefined`
  - [react-ui/scripts/verify-three-terminal.mjs](E:/Urili-Ruoyi/react-ui/scripts/verify-three-terminal.mjs:600) 仍会执行 `npm run tsc`
  - 当前工作区实际存在未跟踪目录 `react-ui/src/.umi-undefined/`，其中含大量 `.ts/.tsx` 生成文件
  - [react-ui/tests/verify-three-terminal-backend-gate.test.ts](E:/Urili-Ruoyi/react-ui/tests/verify-three-terminal-backend-gate.test.ts:91) 只验证“测试发现忽略生成目录”，没有覆盖“类型检查也必须隔离生成目录”
- 风险：
  - 当前 gate 并没有真正隔离 accidental generated dir；`tsc` 仍可能因为陈旧或脏的 `.umi-undefined` 失败或误通过，导致三端总门结果受本机残留物影响
  - 这正好落在本次要查的“untracked 生成目录仍可能污染验证”
- 最小修复建议：
  - 在 `react-ui/tsconfig.json` 显式 `exclude` `src/.umi-undefined`、`test-results`，必要时一并排除根级/历史 Umi 生成目录
  - 补一条 gate 合同测试：构造一个会报错的 `src/.umi-undefined/**/*.ts` 临时文件，断言 manifest/gate 仍通过静态发现且 `tsc` 不受影响

### 2. 生成目录忽略规则不完整，当前未跟踪产物会持续污染工作树，掩盖真实验证漂移

- 证据：
  - [react-ui/.gitignore](E:/Urili-Ruoyi/react-ui/.gitignore:32) 仅忽略 `.umi`、`.umi-production`、`.umi-test`
  - [react-ui/.gitignore](E:/Urili-Ruoyi/react-ui/.gitignore:35) 没有覆盖当前实际出现的 `src/.umi-undefined/`
  - [react-ui/.gitignore](E:/Urili-Ruoyi/react-ui/.gitignore:17) 只忽略 `coverage`，没有覆盖当前实际出现的 `test-results/`
  - [E:\Urili-Ruoyi\.gitignore](E:/Urili-Ruoyi/.gitignore:1) 没有忽略当前实际出现的 `.codegraph/` 与仓库根 `.umi-test/`
  - 当前 `git status --short` 已出现 `?? .codegraph/`、`?? .umi-test/`、`?? react-ui/src/.umi-undefined/`、`?? react-ui/test-results/`
- 风险：
  - 虽然其中部分目录已被 `verify-three-terminal` 的测试发现逻辑绕开，但它们仍持续制造未跟踪噪音，降低“工作树脏是代码变更还是生成物残留”的可判读性
  - 这会直接削弱切片式 P0/P1 审计与后续 gate 回归时对真实漂移的识别效率
- 最小修复建议：
  - 在仓库根 `.gitignore` 增加 `.codegraph/`、`.umi-test/`
  - 在 `react-ui/.gitignore` 增加 `src/.umi-undefined/`、`test-results/`
  - 保持 `verify-three-terminal` 当前的发现忽略列表，同时把“这些目录必须被 git ignore”补成一条轻量 guard

## 已核对但本轮未发现 P0/P1

- [react-ui/package.json](E:/Urili-Ruoyi/react-ui/package.json:16) 的公开测试脚本 `test` / `test:coverage` / `test:update` / `test:unit` / `jest` 都直连 `node scripts/verify-three-terminal.mjs`，未发现通过 package script 绕过三端总门
- [react-ui/tests/three-terminal.manifest.json](E:/Urili-Ruoyi/react-ui/tests/three-terminal.manifest.json:121) 已纳入本轮新增的产品分发、商品中心、来源商品库、来源仓库库存、库存调整审核等前端合同测试
- [react-ui/scripts/verify-three-terminal.mjs](E:/Urili-Ruoyi/react-ui/scripts/verify-three-terminal.mjs:251) 的后端 reactor 模块来源于 [RuoYi-Vue/pom.xml](E:/Urili-Ruoyi/RuoYi-Vue/pom.xml:232)，`-pl ... -am` 也是动态拼装，未见回退到硬编码少数模块
