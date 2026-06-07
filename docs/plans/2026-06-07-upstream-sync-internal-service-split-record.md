# 上游系统同步内部拆分实施记录

日期：2026-06-07

## 背景

定时任务入口已经拆成仓库、物流渠道、SKU、SKU仓库尺寸重量、库存几个独立 Job Bean，但内部仍通过 `IUpstreamSystemService` 调用同步方法，导致“主仓接入管理”和“同步执行”职责没有真正分离。

## 本次调整

- 新增 `IUpstreamSyncService`，作为上游系统同步的唯一业务接口。
- `IUpstreamSystemService` 移除同步方法，只保留主仓接入、查询、配对、来源商品/库存读模型查询和请求日志查询。
- 新增 `UpstreamSyncServiceImpl`，只负责同步编排：
  - 同步锁
  - 同步类型规整
  - 手动同步、定时同步、指定 SKU 尺寸重量同步
  - 读模型重建触发
- 新增按同步对象拆分的执行组件：
  - `UpstreamWarehouseSyncComponent`
  - `UpstreamLogisticsChannelSyncComponent`
  - `UpstreamSkuInfoSyncComponent`
  - `UpstreamSkuDimensionSyncComponent`
  - `UpstreamInventorySyncComponent`
- 新增 `UpstreamSyncStateRecorder`，集中处理同步状态、批次、旧状态兼容和同步执行日志写入。
- 新增 `UpstreamLingxingClientFactory`，集中处理领星客户端创建、密钥解密、请求日志回调和领星异常映射。
- 管理端同步接口和 5 个定时任务组件改为注入 `IUpstreamSyncService`，不再通过 `IUpstreamSystemService` 执行同步。

## 拆分后的边界

- `IUpstreamSystemService`：主仓接入管理、授权校验、配对、同步清单查询、请求日志查询。
- `IUpstreamSyncService`：所有同步入口，包括手动同步、定时同步、指定 SKU 尺寸重量同步。
- `sync` 组件包：具体外部接口拉取、staging 落库、差异对比写入。
- `UpstreamSyncStateRecorder`：同步状态、批次、执行日志，不参与外部接口和业务数据写入。

## 验证

- 已执行 `mvn -pl ruoyi-admin -am -DskipTests compile`，结果通过。
- 已停止旧 8080 后端进程，执行 `mvn -pl ruoyi-admin -am -DskipTests package`，结果通过。
- 已使用 `start-backend-local.ps1` 启动新 jar。
- 已验证 `http://127.0.0.1:8080` 返回 HTTP 200。

## 后续建议

`UpstreamSystemServiceImpl` 已移除同步职责，但接入管理、配对、来源读模型查询仍在同一个服务内。当前不是 Job 同步链路的问题；如果后面继续扩展上游系统管理，建议再拆：

- `UpstreamConnectionService`
- `UpstreamWarehousePairingService`
- `UpstreamLogisticsChannelPairingService`
- `UpstreamSkuPairingService`
- `UpstreamSourceReadQueryService`
