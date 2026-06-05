# 2026-06-06 免密 Redis 明文 Token 收敛记录

## 目标范围

继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“先做一套标准卖家模板，验收通过后复制买家；每个切片只改一类东西”的节奏推进。

本轮只处理一类问题：管理端免密代入的一次性明文 token 仍可返回给管理端用于短时直登链接，但 Redis 侧不再用明文 token 做 key，也不在 payload 中保存明文 token。本轮不改前端、不执行 SQL、不连接远程 MySQL / Redis。

## 已修复问题

- `PortalDirectLoginSupport` 继续生成明文一次性 token 并返回给管理端 `PortalDirectLoginResult`，保持 direct-login 链接契约不变。
- Redis key 从 `portal_direct_login:{token}` 改为 `portal_direct_login:{token_hash}`。
- `PortalDirectLoginToken` 删除 `token` 字段，Redis payload 不再保存明文 token。
- `consumeToken(...)` 先对请求 token 做 hash，再用 hash 查询审计票据和 Redis payload。
- `PortalDirectLoginSupportTest` 更新为固定 DB `token_hash`、Redis hash key、payload 无 token 字段、30 分钟 TTL、一次性消费、跨端拒绝、过期删除和 validator 失败不消费票据。
- seller/buyer service 测试假件同步删除 `PortalDirectLoginToken#setToken(...)`。

## 新增问题

- 无新增已知问题。

## 残留问题

- 明文 token 仍会返回给管理端并作为 direct-login URL 参数，这是免密链接消费所需的短时明文；管理端接口已通过 `@Log(isSaveResponseData=false)` 避免写入 `sys_oper_log` 响应体。
- 本轮未清理历史 Redis payload；旧 payload 最长 30 分钟自然过期。

## 权限检查结果

- 本轮不新增权限点、不新增菜单、不改 `sys_menu` / `seller_menu` / `buyer_menu`。
- 管理端免密代入仍由 `seller:admin:directLogin` / `buyer:admin:directLogin` 控制。

## 字典/选项复用检查结果

- 不涉及字典、选项或 code/label 变更。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记免密 Redis 明文 token 收敛规则。

## 数据源与 SQL 检查

- 本轮未读取 `.env.local`。
- 本轮未连接远程 MySQL / Redis。
- 本轮未执行 DDL/DML。
- 本轮不需要新增 SQL 或 seed。

## 大文件合理性判断结果

- `PortalDirectLoginSupport.java`：244 行，未触发 300 行判断阈值。
- `PortalDirectLoginToken.java`：120 行，未触发 300 行判断阈值。
- `PortalDirectLoginSupportTest.java`：510 行，触发 500 行判断阈值；职责仍集中在免密票据生成、消费、Redis payload、状态机和原因校验，暂不拆分。
- `SellerServiceImplTest.java` / `BuyerServiceImplTest.java` 为既有大文件，本轮只删除测试假件中的旧 token 字段设置，不扩大职责。

## 重复代码检查结果

- seller/buyer service 测试假件继续保持同构镜像，只替换 terminal、主体 ID 字段和文案；本轮不抽跨端测试基类，避免重新耦合 seller/buyer 模块。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PortalDirectLoginSupportTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test

cd E:\Urili-Ruoyi
rg -n "[ \t]+$" RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalDirectLoginToken.java RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md docs/plans/2026-06-06-direct-login-redis-token-hash-hardening-record.md
git diff --check -- RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PortalDirectLoginToken.java RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/service/support/PortalDirectLoginSupportTest.java RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java docs/architecture/reuse-ledger.md docs/plans/2026-06-04-three-terminal-isolation-goal-tracker.md
codegraph sync .
```

## 验证结果

- `PortalDirectLoginSupportTest`：通过，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。
- `SellerServiceImplTest`：通过，`Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`。
- `BuyerServiceImplTest`：通过，`Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`。
- 尾随空白检查已执行，未发现匹配。
- `git diff --check -- <本轮触碰的已跟踪代码和文档文件>`：通过，仅有 LF/CRLF 工作区换行提示。

## 未验证原因

- 未做 HTTP smoke：本轮只收敛 Redis 存储形态，未改 direct-login 接口参数或前端入口。
- 未做前端验证：本轮不改前端。
- 未做数据库验证：本轮不改 SQL 或远程数据。

## CodeGraph 更新结果

- `cd E:\Urili-Ruoyi; codegraph sync .`：通过，CodeGraph 返回 `Already up to date`。
