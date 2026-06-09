# 2026-06-09 切片 D 只读 P0/P1 审查

- 模型：`gpt-5.4`
- 模式：`review-only`
- 范围：
  - `react-ui/scripts/verify-three-terminal.mjs`
  - `react-ui/tests/three-terminal.manifest.json`
  - guard 脚本与 guard 自测
  - `react-ui/tsconfig.json`
  - 根目录与 `react-ui/.gitignore`
  - `RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml`

## 结论

本切片未发现新的 `P0/P1` 漏项。用户点名的 4 类关注项目前都已被 gate 覆盖：

1. `/.umi-test`、`/.umi-production`、`/.umi-undefined`、`/test-results`
2. `application.yml` 中 token/secret 环境占位
3. 商品审核页 `review-only` 回归
4. guard 脚本与 public test script 不得绕过 `verify-three-terminal`

## Findings

### 1. 未发现新的 P0/P1 漏项

证据：

- `verify-three-terminal` 已在发现阶段忽略本地生成目录：`react-ui/scripts/verify-three-terminal.mjs:27-36`
- `verify-three-terminal` 已把 frontend critical 覆盖收口到 manifest + 关键路径模式：`react-ui/scripts/verify-three-terminal.mjs:42-44,370-385`
- `verify-three-terminal` 已把 public test scripts 强制指向 verifier，避免直接绕过：`react-ui/scripts/verify-three-terminal.mjs:194-217`
- `verify-three-terminal-backend-gate` 已自测本地生成目录忽略与 typecheck/noise 隔离：`react-ui/tests/verify-three-terminal-backend-gate.test.ts:104-176`
- `tsconfig` 已排除 `src/.umi-test`、`src/.umi-undefined`、`src/.umi-production`、`test-results`：`react-ui/tsconfig.json:25-30`
- 根 `.gitignore` 与 `react-ui/.gitignore` 已覆盖对应生成物：`.gitignore:5-14`，`react-ui/.gitignore:35-38`
- `application.yml` 中 secret 仍是环境变量占位，不是明文：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml:30-31,117`
- gate 自测已固定 secret placeholder 合同：`react-ui/tests/verify-three-terminal-backend-gate.test.ts:178-193`
- 商品审核页 `review-only` 回归仍被 `product-distribution-permission-guard` 钉住，包括禁止 `继续编辑`、`canContinueRejectedReview`、`?reviewId=`：`react-ui/tests/product-distribution-permission-guard.test.ts:158,187-189`
- 商品审核相关前端回归测试已纳入 manifest 执行清单：`react-ui/tests/three-terminal.manifest.json:137-146`

### 2. 非阻塞硬化建议 1：把商品审核回归从“关键路径命中”提升为“显式 critical”

当前状态：

- `tests/product-distribution-permission-guard.test.ts` 已在 `frontendTestPaths` 中：`react-ui/tests/three-terminal.manifest.json:137-146`
- 但它不在 `criticalFrontendExplicitTestPaths` 中：`react-ui/tests/three-terminal.manifest.json:148-162`
- 目前它主要依赖 `criticalFrontendTestPathPattern` 中的 `product-distribution-permission` 关键字被识别为 critical：`react-ui/scripts/verify-three-terminal.mjs:42,378-385`

判断：

- 这不是当前 `P1` 缺口，因为现在移出 manifest 仍会被 `criticalFrontendTestPathPattern` 拦住。
- 但它对“文件名保持不变”的依赖比显式 critical 稍强，可读性和抗重命名能力略弱。

最小修复建议：

1. 把 `tests/product-distribution-permission-guard.test.ts` 加入 `criticalFrontendExplicitTestPaths`
2. 在 `react-ui/tests/verify-three-terminal-backend-gate.test.ts` 的“remove critical frontend test”用例里补一个显式 removal case

### 3. 非阻塞硬化建议 2：对 `src/.umi-test` 补显式 ignore 断言

当前状态：

- verifier 和 typecheck 已明确使用 `src/.umi-test`：`react-ui/scripts/verify-three-terminal.mjs:23-24`，`react-ui/tsconfig.json:27`
- 但 gitignore 侧目前主要依赖通用 `.umi-test` 模式：
  - 根 `.gitignore`：`.gitignore:6`
  - `react-ui/.gitignore`：`react-ui/.gitignore:35`
- `verify-three-terminal-backend-gate` 目前也只断言根 `.gitignore` 含 `.umi-test/`，没有显式断言 `react-ui/src/.umi-test/`：`react-ui/tests/verify-three-terminal-backend-gate.test.ts:153`

判断：

- 这不是当前 `P1` 缺口，现有通配规则在当前 Git 语义下可工作。
- 但它对 ignore 语义的隐式依赖比 `src/.umi-undefined`、`src/.umi-production` 更强，后续维护时更容易被误改。

最小修复建议：

1. 根 `.gitignore` 显式补 `react-ui/src/.umi-test/`
2. `react-ui/.gitignore` 显式补 `/src/.umi-test`
3. 在 `react-ui/tests/verify-three-terminal-backend-gate.test.ts` 增加对应断言，和 `.umi-undefined` / `.umi-production` 保持同一强度

## 已执行命令

```powershell
cd E:\Urili-Ruoyi\react-ui
node scripts/verify-three-terminal.mjs --check-manifest
```

结果：`three-terminal manifest check passed.`

## 建议复核命令

```powershell
cd E:\Urili-Ruoyi\react-ui
node scripts/verify-three-terminal.mjs --check-manifest
npx jest tests/verify-three-terminal-backend-gate.test.ts --runInBand
npx jest tests/product-distribution-permission-guard.test.ts --runInBand
```

## 本次未做

- 未执行全量 `npm test`
- 未执行后端 `mvn test`
- 未做浏览器级验证
- 未修改业务代码或配置
