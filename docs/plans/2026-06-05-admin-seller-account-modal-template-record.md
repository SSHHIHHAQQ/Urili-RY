# 2026-06-05 管理端卖家账号弹窗模板执行记录

## 目标

以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏，本切片只处理一类问题：管理端卖家账号维护弹窗模板的浏览器验收细节。

## 范围

- 本切片只改 `react-ui/src/components/PartnerManagement/PartnerAccountModal.tsx`。
- 不改后端。
- 不执行 SQL。
- 不改 seller/buyer 账号权限模型。
- 不复制买家。
- 不新增账号级免密代入、账号级日志入口或新的后端接口。

## 已完成

- 收紧账号弹窗表格列宽，避免账号表格在 1366 桌面视口下产生横向溢出。
- 将账号弹窗宽度从 `1000` 调整为 `1040`，让表格在 Ant Design Modal body 内完整显示。
- 账号新增/编辑表单 Modal 增加 `forceRender`，避免隐藏表单先调用 `useForm` 造成 Ant Design console warning。

## 数据源确认

- 本切片未读取数据库。
- 本切片未执行 DDL/DML。
- 浏览器验收时仅通过前端代理调用已有后端接口。

## 验证命令

- `cd E:\Urili-Ruoyi\react-ui; npx biome lint src/components/PartnerManagement/PartnerAccountModal.tsx`：通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run tsc`：通过。

## 浏览器验收

使用 Playwright / Chrome，1366x900 视口：

- 管理端登录成功，业务返回 `code=200`。
- `/partner/seller` 页面可打开。
- 卖家行内“账号”入口可打开账号弹窗。
- 账号弹窗展示登录账号、部门、角色和“分配角色”入口。
- 账号行“更多”菜单展示“重置密码 / 会话 / 强制踢出”。
- 新增账号弹窗展示登录账号、初始密码、部门、账号角色和状态。
- `bodyOverflowX=false`。
- `modalOverflowX=false`。
- `tableOverflowX=false`。
- console error 数量为 0。

## 当前判断

- 管理端卖家账号维护弹窗已形成可验收模板。
- 买家账号弹窗暂不在本切片修改；后续按卖家模板复制时，只替换端类型、文案、权限标识、字段配置和 service。
- 账号级免密代入、账号级日志入口、权限组合细化、索引补齐和服务层测试守卫均属于后续切片，不混入本次 UI 验收修复。

## CodeGraph

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Synced 1 changed files`。
