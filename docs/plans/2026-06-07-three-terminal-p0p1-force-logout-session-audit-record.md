# 2026-06-07 三端 P0/P1 快速推进：强制踢出逐 Session 审计记录

本记录继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为参考方向。当前只处理 P0/P1：编译、guard、接口、权限、串端、service/字段缺失；不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

## 本轮边界

- 未执行远程 MySQL DDL/DML。
- 未读取或写入 Redis。
- 未启动或重启后端。
- 未修改 source-product / integration 旁支脏改。
- 本切片未新增子 Agent：改动范围集中在 seller/buyer 强制踢出链路，主 Agent 本地实现和验证更直接；后续横向扫描类任务再启用子 Agent，并按 AGENTS 优先 GPT-5.3 Codex，不可用时降级 `gpt-5.4`。

## 新增问题

- P1：强制踢出、密码重置后的踢出、锁定/停用触发的踢出此前只写一条汇总登录日志，无法按被踢 session 保留 direct-login 的 `ticketId`、acting admin 和 reason。

## 已修复问题

- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/mapper/SellerMapper.java`
  - 新增 `selectOnlineSellerSessionList(...)`。
- `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml`
  - 新增在线 seller session 列表查询，投影 `directLogin`、`directLoginTicketId`、`actingAdminId`、`actingAdminName`、`directLoginReason`。
- `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java`
  - 强制踢出前读取完整在线 session 列表。
  - 按每个 session 写踢出登录日志；direct-login session 复用 `PortalTokenSupport.buildDirectLoginLog(..., session)`。
  - Redis token 删除使用同一批 session tokenId。
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/mapper/BuyerMapper.java`
  - 按卖家模板新增 `selectOnlineBuyerSessionList(...)`。
- `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml`
  - 按卖家模板新增在线 buyer session 列表查询。
- `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java`
  - 按卖家模板机械复制强制踢出逐 session 审计。
- `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java`
  - 密码重置强制踢出测试升级为一条普通 session、一条 direct-login session，断言写两条日志并保留 direct-login 审计字段。
- `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`
  - 按卖家模板机械复制测试。
- `RuoYi-Vue/ruoyi-system/src/test/java/com/ruoyi/system/architecture/PortalLoginSessionConsistencyContractTest.java`
  - 新增静态契约，固定强制踢出必须读取在线 session 列表，并保留 direct-login 审计字段投影。
- `docs/architecture/reuse-ledger.md`
  - 补充强制踢出逐 session 审计模板。

## 残留问题

- P1：ticket 完全无法解析的免密失败路径仍缺少 ticket 级失败上下文。
- P1：新增高影响 SQL 自动发现和 `verify-three-terminal` 后端模块覆盖边界仍需后续验证治理切片收口。

## 验证命令

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -am "-Dtest=SellerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`46` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -am "-Dtest=BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，`46` 个测试通过。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system -Dtest=PortalLoginSessionConsistencyContractTest test`：第一次因契约断言把 XML 中 `>=` 写成 `&gt;=` 失败；修正后通过，`2` 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；结果为 `Synced 10 changed files`，`Modified: 10 - 794 nodes in 1.1s`。

## 未验证原因

- 未做浏览器、截图、DOM 或 UI 细调验收：用户已明确本阶段无需浏览器运行态验收。
- 未执行远程 SQL：本轮没有新增 DDL/DML，也没有修改远程库必要。
- 未启动后端：本轮已用 service 测试和静态契约覆盖修改点。

## 权限检查结果

- 本轮未新增后端接口或权限标识。
- 强制踢出仍复用既有管理端权限入口；本轮只修 service / mapper 审计链路。

## 字典/选项复用检查结果

- 本轮未新增业务字典、选项或前端下拉。

## 复用台账检查结果

- 已更新 `docs/architecture/reuse-ledger.md`，记录强制踢出逐 session 审计模板。

## CodeGraph 更新结果

- `codegraph sync .` 已执行，结果为 `Synced 10 changed files`，`Modified: 10 - 794 nodes in 1.1s`。

## 大文件合理性判断结果

- `SellerServiceImpl` / `BuyerServiceImpl` 是既有同构 service，本轮只增加强制踢出 helper 逻辑，未引入新大文件。
- `SellerServiceImplTest` / `BuyerServiceImplTest` 已是既有大测试文件，本轮只升级相关测试和 recording mapper，不另拆测试文件，避免破坏当前同构测试结构。

## 重复代码检查结果

- 卖家先修，买家按同构模板机械复制，仅替换端名、字段名、方法名和文案。
- 未新增公共抽象，因为 seller/buyer 当前 mapper 和 service 已按端物理隔离；公共 direct-login 审计逻辑继续复用 `PortalTokenSupport`。
