# 2026-06-07 三端 P0/P1 快速推进：免密串端失败审计隔离记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 本轮边界

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 本轮代码改动只收紧免密登录 token 串端失败时的审计上下文隔离，并按卖家模板机械复制买家。

## 子 Agent 使用情况

- 先按用户要求尝试 6 个 `gpt-5.3-codex-spark` 子 Agent；平台返回额度限制，恢复时间为 `2026-06-13 01:59`，失败 Agent 已关闭。
- 降级启动 6 个 `gpt-5.4` 只读子 Agent，覆盖 direct-login Redis key、seller/buyer 免密链路、测试落点、前端影响、规则文档和残留 P1 排序。
- 子 Agent 结论中，Redis 新 key 已是 `portal_direct_login:{terminal}:{token_hash}`；同时发现 wrong-terminal 免密失败不会创建会话，但会把外端票据结构化审计上下文写入当前端登录失败日志，采纳为 P1。

## 新增问题

- P1：卖家端消费买家免密票据或买家端消费卖家免密票据时，`PortalDirectLoginSupport` 会拒绝并不创建端 session，但失败审计器仍可能收到外端 ticket payload。
- 结果是当前端登录失败日志可能写入外端 `ticketId`、`actingAdmin*`、`reason`、目标主体或目标账号，形成审计串端。

## 已修复问题

- `PortalDirectLoginSupport`
  - 票据 `terminal` 与当前消费端不匹配时，直接抛出 `免密登录票据端类型不匹配`，不再调用当前端失败审计器并传入外端 ticket payload。
- `SellerServiceImpl`
  - 卖家端 direct-login 失败如果收到非 `seller` token，只记录当前端普通登录失败，不写外端结构化免密审计字段。
  - 将 `免密登录票据端类型不匹配` 纳入无账号上下文失败分支。
- `BuyerServiceImpl`
  - 按卖家模板机械复制同一规则。
- `PortalDirectLoginSupportTest`
  - 固定 terminal mismatch 不消费票据、不删除 Redis payload，并且不调用失败审计器。
- `SellerServiceImplTest` / `BuyerServiceImplTest`
  - 固定外端免密 token 失败时，当前端登录日志不写 username、ticketId、acting admin、reason 等外端审计上下文。
- `AGENTS.md` 和 `docs/architecture/reuse-ledger.md`
  - 同步免密 Redis key 端前缀规则，以及 wrong-terminal 失败不得写外端审计上下文的规则。

## 残留问题

- 旧 `portal_direct_login:{token_hash}` Redis key 仍保留 30 分钟兼容读取/清理策略；完全移除 legacy fallback 后续可单独硬化。
- 本轮只修登录失败审计串端；操作日志、会话审计等其他链路已由既有结构化审计规则和相邻测试覆盖，未扩大改动面。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system "-Dtest=PortalDirectLoginSupportTest,SqlExecutionGuardContractTest" test`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -DskipTests install`
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer "-Dtest=SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 验证结果

- `PortalDirectLoginSupportTest` + `SqlExecutionGuardContractTest`：通过，合计 `41` 个测试通过。
- `ruoyi-system -DskipTests install`：通过，用于刷新 seller/buyer 本地依赖。
- `SellerServiceImplTest` + `BuyerServiceImplTest`：通过，两端各 `49` 个测试通过，合计 `98` 个测试通过。

## 未验证原因

- 未执行远程 SQL、未读取或写入 Redis：本轮是代码路径和单元契约收口，不需要动运行库。
- 未做浏览器、截图、DOM 或 UI 细调验收：用户已明确当前快速模式无需浏览器运行态验收。
- 未运行完整三端总验证：本轮是窄范围 direct-login 串端审计收口，先跑最小必要测试；收尾只补 `git diff --check` 和 `codegraph sync .`。

## 权限检查结果

- 本轮不新增接口、菜单或按钮权限。
- 免密消费仍沿用既有 seller/buyer direct-login 入口；本轮只避免外端失败 ticket 上下文进入当前端日志。

## 字典/选项复用检查结果

- 本轮未新增字典、选项或前端下拉。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，登记 `PortalDirectLoginSupport` wrong-terminal 失败不得写外端审计上下文。

## CodeGraph 更新结果

- `codegraph sync .`：通过，返回 `Synced 7 changed files`，`Modified: 7 - 830 nodes`。

## 大文件合理性判断结果

- `SellerServiceImpl.java` / `BuyerServiceImpl.java` 已超过 500 行，但本轮只在既有 direct-login 失败记录分支内小范围补 guard，和当前服务已有登录职责一致；不在本切片拆分。
- `SellerServiceImplTest.java` / `BuyerServiceImplTest.java` 已超过 500 行，本轮只补同类 direct-login 负向测试，后续如继续增长可按登录、账号、会话职责拆测试类。

## 重复代码检查结果

- seller 先做一套标准实现，buyer 按端类型、文案和 service 名称机械复制。
- Redis key 拼接仍集中在 `PortalDirectLoginSupport`，seller/buyer service 不重复拼接 key。
