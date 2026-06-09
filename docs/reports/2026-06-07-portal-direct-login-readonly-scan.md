# 只读扫描报告：Portal Direct Login P0/P1

- 扫描时间：2026-06-07
- 扫描方式：只读代码扫描
- 限定范围：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java`
  - `seller/buyer ServiceImpl`
  - `seller/buyer PortalAuthController`
  - `seller/buyer Mapper XML`
- 明确未执行：
  - 未修改源码
  - 未连接 DB / Redis
  - 未运行接口

> 2026-06-09 记录层 P1 修正：本文中的 seller/buyer 自助日志与会话 DTO 泄漏 P1 已由后续 `PortalOwnLoginLogProfile`、`PortalOwnOperLogProfile`、`PortalOwnSessionProfile` 脱敏 DTO 收口，并由 `PortalSelfServiceSurfaceContractTest` 固定。本文保留历史证据，不再代表当前开放 P1。

## 结论

- 未发现 `P0`
- 未发现“免密登录串端” `P0/P1`
- 未发现“旧 Redis key 被认证链路读取依赖” `P0/P1`
- 历史发现 `P1`：seller/buyer 端内自助日志与会话查询直接返回管理端免密审计字段，构成越权信息泄漏；当前已关闭

## Findings

### P1 - seller 端内自助日志/会话返回内部免密审计字段

- 证据路径：
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:325`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:332`
  - `RuoYi-Vue/seller/src/main/java/com/ruoyi/seller/service/impl/SellerServiceImpl.java:339`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:67`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:86`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:384`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:430`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml:491`
- 说明：
  - `selectSellerOwnLoginLogList(...)` 与 `selectSellerOwnOperLogList(...)` 直接复用管理审计模型 `PortalLoginLog` / `PortalOperLog`
  - `selectSellerOwnSessionList(...)` 直接返回 `PortalSessionProfile`
  - `SellerMapper.xml` 结果映射与查询字段中包含：
    - `direct_login_ticket_id`
    - `acting_admin_id`
    - `acting_admin_name`
    - `direct_login_reason`
    - 以及操作日志中的 `oper_param`、`json_result`
- 风险：
  - seller 端员工可见管理端代入票据 ID、代入管理员身份和代入原因
  - 操作日志还可能暴露内部请求参数与响应片段
- 最小修复建议：
  - 为 seller 端自助日志与自助会话单独定义脱敏 DTO，例如 `PortalOwnLoginLogProfile` / `PortalOwnOperLogProfile` / `PortalOwnSessionProfile`
  - `SellerServiceImpl` 的 `selectSellerOwn*` 改为在 service 层映射 DTO，不再直接返回 `PortalLoginLog` / `PortalOperLog` / `PortalSessionProfile`
  - `SellerMapper.xml` 为 self-service 查询单独建 `resultMap` / `select`，去掉 `direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`、`oper_param`、`json_result`

### P1 - buyer 端内自助日志/会话返回内部免密审计字段

- 证据路径：
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:325`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:332`
  - `RuoYi-Vue/buyer/src/main/java/com/ruoyi/buyer/service/impl/BuyerServiceImpl.java:339`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:67`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:86`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:384`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:430`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml:491`
- 说明：
  - `selectBuyerOwnLoginLogList(...)` 与 `selectBuyerOwnOperLogList(...)` 直接复用管理审计模型 `PortalLoginLog` / `PortalOperLog`
  - `selectBuyerOwnSessionList(...)` 直接返回 `PortalSessionProfile`
  - `BuyerMapper.xml` 结果映射与查询字段中包含：
    - `direct_login_ticket_id`
    - `acting_admin_id`
    - `acting_admin_name`
    - `direct_login_reason`
    - 以及操作日志中的 `oper_param`、`json_result`
- 风险：
  - buyer 端员工可见管理端代入票据 ID、代入管理员身份和代入原因
  - 操作日志还可能暴露内部请求参数与响应片段
- 最小修复建议：
  - 为 buyer 端自助日志与自助会话单独定义脱敏 DTO，例如 `PortalOwnLoginLogProfile` / `PortalOwnOperLogProfile` / `PortalOwnSessionProfile`
  - `BuyerServiceImpl` 的 `selectBuyerOwn*` 改为在 service 层映射 DTO，不再直接返回 `PortalLoginLog` / `PortalOperLog` / `PortalSessionProfile`
  - `BuyerMapper.xml` 为 self-service 查询单独建 `resultMap` / `select`，去掉 `direct_login_ticket_id`、`acting_admin_id`、`acting_admin_name`、`direct_login_reason`、`oper_param`、`json_result`

## 未命中项

### 免密登录串端

- `PortalDirectLoginSupport.consumeToken(...)` 先按 `tokenHash` 取票据，再校验 `ticket.terminal == portalType`
- payload 读取使用 `cacheKey(portalType, tokenHash)`，不是裸 `tokenHash`
- seller/buyer `directLogin*` 也都用 `select*AccountByIdAnd*Id(subjectId, accountId)` 绑定主体与账号
- 在本次限定范围内，未发现 seller 票据可在 buyer 端消费，或 buyer 票据可在 seller 端消费的 `P0/P1`

### 旧 Redis key 依赖

- `PortalDirectLoginSupport` 新 key 为 `portal_direct_login:{terminal}:{token_hash}`
- 本次范围内未发现认证链路读取旧 key `portal_direct_login:{token_hash}`
- 仅看到删除兼容残留：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:350`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PortalDirectLoginSupport.java:361`
