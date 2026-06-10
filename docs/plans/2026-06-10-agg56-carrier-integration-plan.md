# AGG56 物流商接入方案草案

日期：2026-06-10

## 目标

AGG56 是物流商管理第一家要接入的 API 接入方。它的定位类似“领星 WMS”在上游系统管理中的定位：不是单个最终承运商，而是一个外部系统接入方。

AGG56 返回的物流产品需要作为物流商渠道保存，再由运营映射为系统渠道和标准最终承运商。AGG56 的下单、费用试算、获取面单、取消订单能力，对应物流商管理对外提供的基础能力。

## 凭据处理

用户已提供 AGG56 的 `app_token` 和 `app_key`，但本方案不记录明文值。

后续实现规则：

- 凭据不得写入代码、SQL、Markdown 或日志。
- 管理端只允许保存加密后的凭据和脱敏展示值。
- AGG56 凭据字段属于 AGG56 私有字段，必须保存到 AGG56 扩展表，不得进入物流商通用连接表。
- 授权校验成功后才覆盖旧凭据。
- AGG56 返回的 `access_token` 是短期业务 token，文档建议每 24 小时重新获取一次。
- `access_token` 可缓存，但不能作为长期凭据落明文。

## API 文档来源

- Apifox 公共文档：https://s.apifox.cn/d3922fac-7e3f-445b-b28c-2d9aef493234
- LLMs 索引：https://s.apifox.cn/d3922fac-7e3f-445b-b28c-2d9aef493234/llms.txt

当前 Apifox 文档只公开了接口 path 和 mock server。用户已确认 AGG56 API base URL：

```text
https://www.agg56.com
```

后续实际请求按 `baseUrl + path` 组合，例如 `https://www.agg56.com/api/svc/getToken`。

## 授权校验记录

执行时间：2026-06-10

执行范围：

- 只调用 `POST /api/svc/getToken`。
- 未调用同步渠道、费用试算、创建订单、获取面单、取消订单或 Scan Form 接口。
- 未把 `app_token`、`app_key` 或返回的 `access_token` 写入仓库、文档或日志。

校验结果：

| 字段 | 结果 |
| --- | --- |
| HTTP 调用 | 完成 |
| AGG56 `code` | `200` |
| AGG56 `msg` | `Success` |
| 是否返回 `access_token` | 是 |
| 用户 ID | `90` |
| 用户账号 | `594***@qq.com` |
| 客户代码 | `J3655` |

结论：凭据可用于 AGG56 授权，AGG56 适配器可以进入表设计和实现方案阶段。

## 表边界

AGG56 私有字段和通用物流商字段必须分开：

- 通用物流商核心表只保存接入编号、接入方类型、展示名称、base URL、状态、物流商渠道、系统渠道、面单快照和请求日志等通用事实。
- AGG56 的 `app_token`、`app_key`、授权用户 ID、账号快照、客户代码、短期 token 缓存、发货地址编码候选等，都属于 AGG56 扩展表或 AGG56 适配器私有逻辑。

扩展表设计见：

```text
docs/plans/2026-06-10-agg56-extension-table-design.md
```

## 接口清单

| 能力 | AGG56 接口 | 方法 | 物流商管理中的用途 |
| --- | --- | --- | --- |
| 获取访问授权 | `/api/svc/getToken` | POST | 用 `app_token` + `app_key` 换取短期 `access_token` |
| 获取可用物流产品 | `/api/svc/getShippingMethod` | POST | 同步 AGG56 物流商渠道，返回 `sm_code` / `sm_name` |
| 费用试算 | `/api/svc/rates` | POST | 对外提供报价能力，只写脱敏请求日志，不做费用记录 |
| 创建订单 | `/api/svc/createOrder` | POST | 创建 AGG56 订单并尽量同步返回跟踪号、面单、费用快照 |
| 获取面单 | `/api/svc/getLabel` | POST | 获取或重取已有订单面单；支持未就绪轮询 |
| 取消订单 | `/api/svc/cancelOrder` | POST | 作废 AGG56 订单/面单 |
| 获取发货地址 | `/api/svc/getShipperAddress` | GET | 暂缓；完整发件地址跑不通时再用于同步 AGG56 发货地址编码 |
| Scan Form 申请 | `/api/svc/requireScanForm` | POST | 后续可做 manifest/scan form 能力，第一版可先记录为扩展 |

## 授权流程

1. 管理端创建 AGG56 接入，输入最少字段：显示名称、AGG56 API base URL、`app_token`、`app_key`。
2. 后端调用 `/api/svc/getToken`。
3. 请求 body：

```json
{
  "app_token": "***",
  "app_key": "***"
}
```

4. 成功响应包含：
   - `code = 200`
   - `result.access_token`
   - `result.user_info.u_id`
   - `result.user_info.u_account`
   - `result.user_info.u_customer_code`
5. 后端加密保存 `app_token` / `app_key`，保存脱敏值、用户信息快照、最近授权时间。
6. 业务接口请求时把 `access_token` 放入 header 的 `Authorization`。

注意：`getShippingMethod` 文档中的 header 拼写为 `Ahturization`，疑似文档拼写错误。实现时建议先按 `Authorization` 调用；真实联调如失败，再增加 per-adapter headerName 配置或兼容发送。

## 物流商渠道同步

AGG56 的物流产品接口：

- `POST /api/svc/getShippingMethod`
- 响应字段：
  - `sm_code`：物流产品代码，对应创建订单和费用试算的 `sm_code`
  - `sm_name`：物流产品名称

落到 URILI 的规则：

- `sm_code` 保存为外部渠道 code。
- `sm_name` 保存为外部渠道名称。
- 不自动生成系统渠道。
- 运营在物流商管理里把 `sm_code/sm_name` 映射到系统渠道和标准最终承运商。
- AGG56 产品列表中消失的渠道标记为失效，不物理删除。

## 费用试算流程

AGG56 费用试算接口：

- `POST /api/svc/rates`
- 需要 `Authorization` header。
- 必填字段包括：
  - `sm_code`
  - `parcel_quantity`
  - 收件人：`oa_firstname`、`oa_company`、`oa_street_address1`、`oa_street_address2`、`oa_postcode`、`oa_state`、`oa_city`、`oa_country`、`oa_telphone`
  - `box_list`
  - `weight_unit_type`
- 可选字段包括：
  - `parcel_declared_value`
  - `oa_doorplate`
  - `signature_service`
  - `shipper_address`

响应字段包括：

- `sm_code`
- `address_type_text` / `address_type`
- `currency_code`
- `charge_weight`
- `total_charge`
- `shipping_charge`
- `charge_detail[]`

URILI 处理规则：

- 获取费用结果只返回给调用方。
- 请求和响应只进入脱敏外部请求日志。
- 不单独建费用看板或费用业务查询记录。
- 金额字段进入系统 DTO 时必须使用 `BigDecimal`。

## 创建订单与面单流程

AGG56 创建订单接口：

- `POST /api/svc/createOrder`
- 需要 `Authorization` header。
- 关键必填字段：
  - `reference_no`：客户参考号，AGG56 文档说明为客户提供的唯一订单号
  - `sm_code`
  - `parcel_quantity`
  - 收件人信息
  - `box_list`
  - `weight_unit_type`

AGG56 文档说明它采用“垂直下单模式”：下单时会同步向服务商预报，理想情况下立即返回跟踪号、面单、费用等信息；部分渠道不会立即返回面单或跟踪号，需要继续调用获取面单接口。

URILI 处理规则：

- 外部调用方传入的全局唯一业务单号映射为 AGG56 `reference_no`。
- `reference_no` 必须作为创建面单幂等键。
- 如果调用超时或断连，调用方可用同一业务单号重试；AGG56 若已下单成功，可能返回 `order_code`，后续应转入获取面单流程。
- 创建成功后保存面单订单快照：
  - `reference_no`
  - AGG56 `order_code`
  - `order_status`
  - `order_sub_status`
  - `order_waiting_status`
  - `sync_service_status`
  - `logistics_err`
  - `zone`
  - `charge_weight`
  - `labels[]`
  - `fee[]`
  - `fee_detail[]`
  - 脱敏请求/响应摘要

## 获取面单流程

AGG56 获取面单接口：

- `POST /api/svc/getLabel`
- 请求 body：`order_code`
- 返回字段与创建订单类似，包括订单状态、异常、面单、费用、分区和计费重。

AGG56 文档要求：

- 面单未就绪时通常用 `code = 202` 提示。
- 如果 `logistics_err` 有值，表示申请面单失败。
- 可以重复调用，直到获取到需要的信息。
- 频率必须控制在 1 QPS，禁止短时间爆发式请求。

URILI 处理规则：

- 获取面单不得创建新的物流订单。
- 如果本地已有 label 文件，优先返回本地文件引用。
- 如果本地缺失或需要刷新，调用 `/api/svc/getLabel`。
- 对 `code = 202` 做限频重试或后台轮询。
- 一旦 `logistics_err` 非空，标记为面单失败并保留错误信息。
- `label_url` 返回后应下载或转存到当前文件存储体系，避免只依赖 AGG56 外链长期可用性。

## 取消订单流程

AGG56 取消订单接口：

- `POST /api/svc/cancelOrder`
- 请求 body：`order_code`
- 成功示例：`code = 200`、`result = []`、`msg = Success`

AGG56 文档限制：订单草稿、已预报、已提交且未在预报执行中时可以取消。

URILI 处理规则：

- 取消不会删除面单订单记录。
- 成功时更新状态为已取消，并保留取消时间、操作人和 AGG56 响应摘要。
- 失败时保存 AGG56 错误码和错误信息。

## 获取发货地址

AGG56 支持：

- `GET /api/svc/getShipperAddress`
- 返回 `shipper_code`、发件国家、邮编、州、市、地址、电话、公司、是否默认地址等。

虽然当前业务规则是“发件地址由外部调用方传入”，但 AGG56 的下单文档又说明 `shipper_code` 和详细发件地址二选一，且推荐使用发件编码。当前确认口径：

- 第一版创建面单优先使用外部调用方传入的完整发件地址。
- 第一版不依赖 AGG56 `shipper_code`，也不需要物流商管理自动选择发货地址编码。
- 如果真实联调发现完整发件地址跑不通，或 AGG56 的价格分区必须依赖备案地址编码，再补充发货地址编码同步和映射。

## Scan Form

AGG56 支持：

- `POST /api/svc/requireScanForm`
- 请求 `trackingNumber`，多个跟踪号用英文逗号拼接。
- 响应 `scan_form_order` 和 `scan_form_label[]`。

这属于 manifest/交接清单能力，不是创建面单的必需步骤。第一版可先在适配器中预留方法，管理端和外部业务入口后续再确认是否开放。

## 实现边界

第一版 AGG56 接入建议包含：

- AGG56 适配器注册。
- 授权校验和 token 缓存。
- 同步物流产品到物流商渠道。
- 费用试算。
- 创建面单。
- 获取面单。
- 取消面单。
- 外部请求日志脱敏。

暂不包含：

- 轨迹查询。
- Scan Form 页面入口。
- AGG56 发货地址编码同步。
- 费用看板或结算。
- 自动比价决策。
- 卖家自带 AGG56 账号。

## 待用户补充

1. 是否还有独立测试环境 base URL；当前已确认 `https://www.agg56.com` 可完成授权。
2. 是否确认先进入“物流商管理/AGG56 接入”表设计和 SQL 方案阶段。
