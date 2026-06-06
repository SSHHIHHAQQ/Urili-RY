# Java CPU 高占用问题处理方案

生成时间：2026-06-06

## 结论

本次 CPU 高占用按上游系统定时任务问题处理。诊断报告定位到热点链路主要是：

```text
upstreamSystemTask.syncSkus
 -> UpstreamSystemServiceImpl.syncSkusOnly
 -> LingxingOpenApiClient.listProductSkuPage
 -> java.net.http.HttpClient.send
 -> JDK HTTPS / SSL unwrap worker threads
```

库存同步任务会叠加外部请求和失败重试压力，但报告中的主要 CPU 热点是 SKU 定时同步分页拉取触发的 JDK `HttpClient-*` worker/selector 线程堆积。

当前运行库中两个上游任务均为 10 分钟：

| 任务 | 调用目标 | Cron | 状态 |
| --- | --- | --- | --- |
| 领星 SKU 每 10 分钟同步 | `upstreamSystemTask.syncSkus` | `0 0/10 * * * ?` | 启用 |
| 领星 SKU 库存每 10 分钟同步 | `upstreamSystemTask.syncInventory` | `0 0/10 * * * ?` | 启用 |

## 根因判断

1. `LingxingOpenApiClient` 每次构造都会创建新的 JDK `HttpClient`。
2. `UpstreamSystemServiceImpl.createClient(...)` 在授权、同步、SKU 同步、库存同步等入口都会创建新的 `LingxingOpenApiClient`。
3. SKU 同步是分页拉取；多个主仓连接串行执行，每轮会产生较多 HTTPS 请求。
4. 10 分钟频率过高，任务多轮执行后 JDK `HttpClient-*` worker/selector 线程数量异常增长。
5. 库存任务在接口无权限或接口选型错误时，每 10 分钟持续失败，会进一步增加无效外部请求和日志写入。

## 处理方案

### 一、立即止血

目标是先恢复 Java CPU，避免继续放大线程堆积。

1. 临时暂停 `upstreamSystemTask.syncSkus`。
2. 临时暂停 `upstreamSystemTask.syncInventory`，或至少暂停没有完成接口权限验证的库存任务。
3. 重启 `ruoyi-admin.jar`，清掉已经堆积的 JDK `HttpClient-*` 线程。
4. 观察 10-15 分钟：
   - Java CPU 是否回落。
   - Java 线程数是否稳定。
   - `HttpClient-*` worker/selector 数量是否不再增长。

### 二、代码修复

#### 1. 复用 HTTP 客户端

把领星适配器的 HTTP 客户端改为可复用对象，不再每次同步创建新的 JDK `HttpClient`。

推荐实现：

- 保留 `LingxingOpenApiClient` 作为带凭证和日志回调的轻量对象。
- 把底层 `HttpClient` 改为共享实例。
- 为共享 `HttpClient` 配置受控线程池，避免 JDK 默认 executor 无边界扩张。
- 明确连接超时、请求超时和线程命名。

#### 2. 限制同步任务请求量

对 SKU 同步增加硬边界：

- 单次任务最大页数。
- 单次任务最大耗时。
- 页间轻量限速。
- 如果领星分页总数异常或页码不前进，直接失败并记录错误。

#### 3. 调整调度策略

SKU 主数据不适合每 10 分钟全量拉取。

建议：

- SKU 全量同步：改为 30-60 分钟一次，或先改为 1 小时一次。
- WMS 尺寸重量同步：从 SKU 全量同步中拆出来，改为手动或低频任务。
- 库存同步：保留 10 分钟频率，但必须在接口确认、HttpClient 复用和失败熔断完成后再启用。

#### 4. 增加失败熔断

对领星接口错误码做分类：

- `11008 / 无接口权限`：不要每 10 分钟持续重试，记录状态后熔断，等待人工重新授权或手动恢复。
- 网络超时/429/5xx：允许有限重试。
- 参数错误/权限错误：不重试。

#### 5. 避免任务互相叠加

继续使用若依 `sys_job` / Quartz 任务，不另建调度器。

需要保证：

- SKU 同步和库存同步不同时跑同一个主仓。
- 同一个上游连接加全局同步锁，而不只是单方法内的局部锁。
- 任务超时后要释放锁并记录失败。

### 三、验证方案

1. 暂停任务并重启后，采样 CPU 和线程数，确认止血有效。
2. 修复代码后运行：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl integration -am -DskipTests compile
mvn -pl ruoyi-admin -am -DskipTests package
```

3. 重启后端：

```powershell
cd E:\Urili-Ruoyi
.\start-backend-local.ps1 -Restart
```

4. 先只启用单个低频 SKU 任务，观察：
   - Java CPU 是否低于合理范围。
   - `HttpClient-*` 线程数是否稳定。
   - 领星请求日志是否按预期记录。
5. 再启用库存任务，确认库存接口返回成功后，再恢复 10 分钟频率。

## 推荐执行顺序

1. 先暂停两个上游定时任务并重启后端止血。
2. 修复 `LingxingOpenApiClient` 的 `HttpClient` 生命周期。
3. 加入 SKU 同步页数、耗时、限速和失败熔断。
4. 调整 SKU 定时任务频率，库存任务暂时保持 10 分钟但必须具备权限错误熔断。
5. 分阶段恢复任务并做 CPU/线程复测。

## 暂不建议

- 不建议继续让 SKU 全量同步每 10 分钟执行。
- 不建议只重启后端但不改代码；重启只能清理已堆积线程，任务继续跑还会复发。
- 不建议绕过若依定时任务自己写调度器；仍应使用 `sys_job` / Quartz，只修任务实现和任务配置。
