# 买家端商城商品权限 DML 与 HTTP smoke 记录

日期：2026-06-05

## 目标

本切片继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`、`docs/plans/2026-06-05-buyer-product-browse-copy-boundary-plan.md` 和 `docs/plans/2026-06-05-buyer-distribution-product-read-template-record.md` 为参考方向。

当前只做一类事情：

- 将 buyer 端商城商品只读权限补入当前远程运行库。
- 重启后端并执行 buyer 真实 HTTP smoke。

本切片不做前端，不执行 DDL，不修改 `sys_menu` / `sys_role`，不输出 `.env.local`、数据库连接串、数据库密码、Redis 密码、token secret 或 JWT。

## 数据源确认

- 激活配置：`spring.profiles.active=druid`。
- MySQL 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml` 与本机 `.env.local` 的 `RUOYI_DB_*` 运行变量。
- Redis 配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` 与本机 `.env.local` 的 `RUOYI_REDIS_*` 运行变量。
- `.env.local` 只在本机读取，不写入记录，不输出明文。
- 本轮目标环境为当前激活远程 MySQL / 远程 Redis，不读取或写入本地 Docker MySQL / Redis。

## DML 范围

目标权限：

- `buyer:product:distribution:list`
- `buyer:product:distribution:query`

目标表：

- `buyer_menu`
- `buyer_role_menu`

执行逻辑：

- 若 `buyer_menu.perms` 不存在，则插入只读按钮权限。
- 将两个权限授权给当前所有 `status = '0'` 且 `del_flag = '0'` 的 buyer role。
- 已存在的 menu 或 role-menu 不重复插入。

回滚方式：

- 删除新增的 `buyer_role_menu` 绑定。
- 删除新增的 `buyer_menu` 权限行。
- 如果权限已被其他后续功能依赖，回滚前需要重新确认。

## 执行结果

- 数据库目标：当前激活远程 MySQL；执行前已核对激活配置，不使用本地 Docker MySQL / Redis。
- DML 类型：幂等 DML，只影响 `buyer_menu` 与 `buyer_role_menu`，不执行 DDL。
- `buyer_menu` 执行前目标权限计数：0。
- `buyer_menu` 新增权限：2。
- `buyer_menu` 执行后目标权限计数：2。
- 当前 active buyer role 数量：1。
- `buyer_role_menu` 执行前目标授权计数：0。
- `buyer_role_menu` 新增授权：2。
- `buyer_role_menu` 执行后目标授权计数：2。
- 未修改 `sys_menu` / `sys_role`。

## HTTP smoke 范围

实际覆盖：

- 无 token 调 buyer 商品列表返回业务 `code=401`。
- buyer 登录。
- `GET /buyer/getInfo` 返回 buyer 端权限集合，包含：
  - `buyer:product:distribution:list`
  - `buyer:product:distribution:query`
- `GET /buyer/product/distribution-products/list`。
- 伪造 `buyerId`、`subjectId`、`accountId`、`terminal`、`sellerId`、系统编码和 sourceType 参数不改变 buyer 商品浏览范围。
- 详情接口只返回 `ON_SALE` SPU。
- SKU 接口只返回 `ON_SALE` SKU。
- 响应不暴露 seller 内部字段、系统编码、供货价、后台审计字段、token 或 Redis key。
- 固定不存在商品详情和 SKU 返回业务 `code=500`、`msg=商城商品不存在`。
- smoke 结束后调用 `/buyer/logout` 清理本次 token。
- logout 后旧 token 调 `/buyer/getInfo` 返回业务 `code=401`。

本轮未提供真实不可见 SPU 样本，因此 OFF_SALE 或无 `ON_SALE` SKU 的真实样本负向未执行；固定不存在商品负向已执行通过。

## 验证命令

- `cd E:\Urili-Ruoyi; Get-Content RuoYi-Vue\ruoyi-admin\src\main\resources\application.yml / application-druid.yml`：已核对激活配置为远程 MySQL / 远程 Redis，未输出连接明文。
- JDBC 幂等 DML：通过本机 `.env.local` 读取运行变量后执行，只输出影响行数，不输出连接串、密码或 token secret。
- `cd E:\Urili-Ruoyi; Get-NetTCPConnection -LocalPort 8080 -State Listen`：确认旧后端进程占用 8080。
- `cd E:\Urili-Ruoyi; Stop-Process -Id <旧后端进程> -Force`：已停止旧 jar，避免打包锁文件。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-admin -am -DskipTests package`：通过，`BUILD SUCCESS`。
- `cd E:\Urili-Ruoyi; .\start-backend-local.ps1`：已启动新后端 jar。
- `GET http://127.0.0.1:8080/captchaImage`：返回 `code=200`，`captchaEnabled=False`；本轮未修改验证码开关。
- `cd E:\Urili-Ruoyi; .\scripts\smoke\buyer-distribution-product-read-template-smoke.ps1 -BuyerUsername '<管理端列表返回的测试买家账号>' -PageSize 10`：通过。
- `PowerShell script parse`：`scripts/smoke/buyer-distribution-product-read-template-smoke.ps1` 解析通过。
- `git diff --check -- <本切片相关文件>`：通过，仅有 LF/CRLF 工作区换行提示。
- 本切片相关文件尾随空白和冲突标记检查：通过。
- 敏感明文检查：未发现数据库连接串、明文密码、Bearer token 或 JWT；命中项均为“不得输出/不得展示”的说明文字。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，输出 `Already up to date`。

## 检查清单

- 新增问题：HTTP smoke 发现当前缺少真实不可见 SPU 样本，无法覆盖 OFF_SALE 或无 `ON_SALE` SKU 的真实样本负向；已用固定不存在商品负向覆盖接口拒绝路径。
- 已修复问题：远程运行库缺少 `buyer:product:distribution:list` / `buyer:product:distribution:query` 权限与 active buyer role 授权，已补齐。
- 残留问题：buyer 前端工作台复制和浏览器 smoke 尚未做。
- 验证命令：已记录在“验证命令”。
- 未验证原因：真实不可见 SPU 样本负向未验证，因为本轮没有可直接复用的 OFF_SALE 或无 `ON_SALE` SKU 样本 ID；不在 smoke 内临时造业务数据。
- 权限检查结果：远程 `buyer_menu` 与 `buyer_role_menu` 已补齐两个 buyer 商品浏览权限；`/buyer/getInfo` 已实测返回这两个权限；未写入 `sys_menu` / `sys_role`。
- 字典/选项复用检查结果：本切片不新增字典。
- 复用台账检查结果：已更新 `docs/architecture/reuse-ledger.md`，登记 buyer 权限 DML 与 HTTP smoke 模板。
- CodeGraph 更新结果：已执行 `codegraph sync .`，输出 `Already up to date`。
- 大文件合理性判断结果：本记录职责单一，不需要拆分。
- 重复代码检查结果：DML 使用最小幂等插入；HTTP smoke 复用 seller smoke 的断言结构并替换 terminal、权限和 buyer 可见性断言。
