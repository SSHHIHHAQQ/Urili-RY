# 商品配置压测基础数据导入执行记录

## 执行时间

- 日期：2026-06-04
- 模块：管理端基础配置 / 商品分类配置、商品属性配置
- 目的：导入 200+ 级别基础数据，验证商品分类树、商品属性分页、属性选项弹窗和导入链路在较多数据下是否可用。

## 目标环境

- 后端：`http://127.0.0.1:8080`
- 前端：`http://127.0.0.1:8001`
- 数据源确认：后端激活 `druid` 配置，MySQL 与 Redis 连接来自本机 `.env.local` 注入的 `RUOYI_*` 运行变量。
- 敏感信息处理：未在记录中输出 `.env.local`、数据库地址、Redis 地址、token secret 或登录 token 明文。
- 数据影响：本次执行通过已实现的管理端导入接口写入商品配置业务表，属于远端运行库 DML 变更；执行前已向用户提交方案并获得“确认导入”。

## 导入文件

文件目录：`logs/import-seed/product-config-load-20260604/`

| 文件 | 行数 | 说明 |
| --- | ---: | --- |
| `product-category-loadtest-20260604.xlsx` | 260 | 10 个一级分类、50 个二级分类、200 个叶子分类 |
| `product-attribute-loadtest-20260604.xlsx` | 220 | 文本、数值、布尔、单选、多选属性 |
| `product-attribute-option-loadtest-20260604.xlsx` | 300 | 60 个选择型属性，每个 5 个选项 |

## 执行结果

| 步骤 | 结果 |
| --- | --- |
| 商品分类导入 | `code=200`，新增 260，更新 0，错误 0 |
| 商品属性导入 | `code=200`，新增 220，更新 0，错误 0 |
| 商品属性选项导入前预览 | 初次预览失败，原因是依赖属性尚未入库，符合预期 |
| 商品属性选项导入前二次预览 | `code=200`，新增 300，错误 0 |
| 商品属性选项导入 | `code=200`，新增 300，更新 0，错误 0 |

响应记录：

- `logs/import-seed/product-config-load-20260604/category-import-response.json`
- `logs/import-seed/product-config-load-20260604/attribute-import-response.json`
- `logs/import-seed/product-config-load-20260604/attribute-option-preview-response.json`
- `logs/import-seed/product-config-load-20260604/attribute-option-import-response.json`

## 接口验证

- 新登录 `admin / admin123` 后调用 `GET /product/admin/categories/list`：`code=200`，当前分类总数 262，其中本次压测分类 260 条。
- 新登录 `admin / admin123` 后调用 `GET /product/admin/attributes/list?pageNum=1&pageSize=10`：`code=200`，属性总数 220。
- 查询 `loadtest_20260604_attr_single_01` 后调用属性选项接口：返回 5 条选项。
- 不带 token 调用 `GET /product/admin/categories`：返回业务体 `code=401`，文案为“请求访问：/product/admin/categories，认证失败，无法访问系统资源”，与用户看到的错误一致。

## 前端验证

- 刷新 `http://127.0.0.1:8001/basic-config/product-category`：
  - 能看到压测分类数据，例如 `压测分类01 / loadtest_20260604_cat_01`。
  - 页面未出现“认证失败”。
  - 页面不是“功能规划中”占位页。
- 打开 `http://127.0.0.1:8001/basic-config/product-attribute`：
  - 能看到压测属性数据，例如 `loadtest_20260604_attr_multi_20`。
  - 分页显示总数 220。
  - 属性选项弹窗可打开，并显示 `OPT_01` 至 `OPT_05`。
  - 页面未出现“认证失败”。

验证截图：

- `logs/import-seed/product-config-load-20260604/screenshots/product-category-list-verified.png`
- `logs/import-seed/product-config-load-20260604/screenshots/product-attribute-options-verified.png`

## 认证失败处理

用户反馈的错误本质是后端认证失败：请求未携带有效 `Authorization: Bearer ...` 时，若依会返回 `code=401` 和“无法访问系统资源”的业务文案。

已修复前端统一请求拦截器的一个边界问题：

- 原逻辑在 `options.headers` 缺失时，会把 token 写到临时数组对象上，没有保证写回请求配置。
- 原逻辑在 token 距离过期 5 分钟内且存在 refreshToken 时，不会继续带 accessToken，也没有实际刷新 token，可能导致临近过期窗口内请求变成未认证。
- 现逻辑保证 `options.headers` 始终写回，并且 token 未过期时继续携带 accessToken，过期后再清理会话。

修改文件：

- `react-ui/src/app.tsx`

## 验证命令

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc
```

结果：通过。

补充验证：

- 使用新登录 token 直接请求分类和属性接口，通过。
- 使用浏览器刷新分类页、属性页和属性选项弹窗，通过。
- 使用无 token 请求 `/product/admin/categories`，可复现用户看到的 `code=401` 认证失败文案。

## 残留风险

- 导入期间后端进程曾出现退出后重启的情况；导入响应与后续接口验证均显示数据已写入成功，错误日志未发现明确 Java 异常或 OOM。
- 建议后续单独优化导入接口的操作日志记录：当前 `@Log` 可能会把较大的导入结果写入 `sys_oper_log.json_result` 并输出到日志，数据量继续放大时存在日志过大和稳定性风险。
- 如果浏览器仍显示认证失败，优先清理当前前端登录态并重新登录；这不影响已导入数据。
