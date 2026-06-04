# 端内操作日志写入链路检查点

日期：2026-06-04

## 参考方向

本检查点继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向。

本轮目标不是新增表，而是让已落地的 `seller_oper_log` / `buyer_oper_log` 从“只可查询”推进到“有真实端内接口写入”。

## 已完成

- 新增公共注解 `@PortalLog`：
  - `RuoYi-Vue/ruoyi-common/src/main/java/com/ruoyi/common/annotation/PortalLog.java`
- 新增端内操作日志切面：
  - `RuoYi-Vue/ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/PortalLogAspect.java`
- 新增端内操作日志异步落库入口：
  - `RuoYi-Vue/ruoyi-framework/src/main/java/com/ruoyi/framework/manager/factory/AsyncFactory.java`
- 新增端内操作日志 service 和 mapper：
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/IPortalOperLogService.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/impl/PortalOperLogServiceImpl.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/mapper/PortalOperLogMapper.java`
  - `RuoYi-Vue/ruoyi-system/src/main/resources/mapper/system/PortalOperLogMapper.xml`
- 卖家端当前已接入：
  - `GET /seller/getInfo`
  - `GET /seller/getRouters`
- 买家端当前已接入：
  - `GET /buyer/getInfo`
  - `GET /buyer/getRouters`

## 设计说明

- 管理端 `sys_oper_log` 不变，继续由若依原 `@Log` 写入。
- 卖家端和买家端新增 `@PortalLog`，不写 `sys_oper_log`。
- 切面通过 `PortalTokenSupport.getSession(terminal)` 读取当前端 token。
- 没有对应端 session 的请求不写入端内操作日志，避免匿名请求污染主体日志。
- 卖家端写入 `seller_oper_log`，买家端写入 `buyer_oper_log`。
- 后续真实端内业务接口接入时，只需要按端标记 `@PortalLog(terminal = "seller" | "buyer", ...)`。

## 数据源确认

- 配置来源：本机 `.env.local`。
- MySQL 目标：远程环境，连接地址已脱敏。
- Redis 目标：远程环境，连接地址已脱敏。
- 凭证处理：只确认存在，未在命令输出、文档或聊天中记录明文。
- 本轮没有执行 DDL。
- 验证过程中通过真实接口产生少量 DML：
  - 生成卖家/买家免密票据。
  - 消费卖家/买家免密票据。
  - 写入卖家/买家登录日志、会话和操作日志。

## 验证结果

- `mvn -DskipTests install`：
  - 第一次 Java 编译已通过，最终 repackage 因当前运行中的 `ruoyi-admin.jar` 文件锁失败。
  - 停止 8080 旧进程后重跑成功。
- 后端启动：
  - 使用 `.\start-backend-local.ps1 -Restart`。
  - `8080` 正常监听。
  - `http://127.0.0.1:8080` 返回 HTTP 200。
- 接口验证：
  - 管理端 `admin / admin123` 登录成功。
  - 卖家免密代入成功，`/seller/getInfo` 和 `/seller/getRouters` 均返回 `code=200`。
  - 买家免密代入成功，`/buyer/getInfo` 和 `/buyer/getRouters` 均返回 `code=200`。
  - 管理端查询卖家操作日志：`/seller/admin/sellers/operLogs/list` 返回 `total=2`。
  - 管理端查询买家操作日志：`/buyer/admin/buyers/operLogs/list` 返回 `total=2`。

## 权限检查

- 管理端日志查询仍使用既有权限：
  - `seller:admin:operLog:list`
  - `buyer:admin:operLog:list`
- 端内操作日志写入不依赖管理端 `sys_role` / `sys_menu`。
- 当前接入的是端内已认证接口，依赖端 token session，不接受前端传入的 `sellerId` / `buyerId` 决定日志归属。

## 复用检查

- `@PortalLog` 是后续卖家端/买家端业务接口的统一写入入口。
- `PortalOperLog` 继续作为管理端审计查询对象。
- `PartnerAuditModal` 继续作为管理端查看登录日志、操作日志和 ticket 的统一 UI。
- 已更新 `docs/architecture/reuse-ledger.md`。

## 剩余事项

- 当前只覆盖端内基础入口 `getInfo` / `getRouters`。
- 后续真实卖家端/买家端业务接口必须继续接入：
  - 从端 token 推导主体范围。
  - 不信任前端传入的 `sellerId` / `buyerId`。
  - 使用 `@PortalLog` 写入端内操作日志。
- 可考虑为端内写操作补更细的权限校验注解或统一拦截器，避免每个业务接口手写权限判断。
