# 物流商账号模型改造计划

日期：2026-06-10

## 问题修正

AGG56 不是一个固定物流商账号，而是一个物流商系统类型。平台后续会接入多个 AGG56 账号，每个账号有自己的 `app_token`、`app_key`、账号权限、可用渠道和请求日志。

当前第一版实现里，`connectionCode` 被放到了前端表格和新增表单中，容易被理解成运营需要维护的业务编号。这个不符合真实业务。运营真正需要维护的是：

- 物流商名称：给这个接入账号起一个业务可识别的名字，例如 `AGG56账号1亿`、`AGG56-CA02`。
- 物流商系统：选择这个账号接入哪个系统，例如 `AGG56`。
- APP Token / APP Key：这个账号在 AGG56 系统里的授权凭据。

## 正确概念

### 物流商系统

代表要对接的外部系统类型，例如：

- AGG56
- 后续可能接 UPS、FedEx、USPS、其他中间商系统

物流商系统只决定适配器、API 地址默认值、授权字段模板和接口解析方式。它不代表某一个账号。

### 物流商账号

代表平台配置的一组外部系统账号。多个账号可以接入同一个物流商系统。

例子：

| 物流商名称 | 物流商系统 | 凭据 |
| --- | --- | --- |
| AGG56账号1亿 | AGG56 | 账号 A 的 app_token / app_key |
| AGG56账号2亿 | AGG56 | 账号 B 的 app_token / app_key |
| AGG56测试账号 | AGG56 | 测试账号的 app_token / app_key |

渠道、报价、下单、取消面单、获取面单、请求日志都必须归属到具体“物流商账号”，不能归属到 AGG56 这个系统类型本身。

## 字段改造

### 前端用户可见字段

新增/编辑物流商账号时只展示这些字段：

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| 物流商名称 | 是 | 运营自定义名称，例如 `AGG56账号1亿` |
| 物流商系统 | 是 | 字典选择，第一版为 `AGG56` |
| APP Token | 新增授权时必填 | AGG56 账号的 `app_token` |
| APP Key | 新增授权时必填 | AGG56 账号的 `app_key` |
| API 地址 | 否 | 默认 `https://www.agg56.com`，默认不要求运营填写 |
| 状态 | 是 | 启用/停用 |
| 备注 | 否 | 运营备注 |

删除用户可见字段：

- 接入编号
- 接入编码
- `connectionCode` 输入框

### 后端内部字段

后端仍然需要一个内部主键来关联物流商渠道、渠道映射、面单订单、包裹文件和请求日志。这个字段不让运营填写，不在页面作为业务字段展示。

推荐改成：

| 字段 | 说明 |
| --- | --- |
| `carrier_account_id` | 自增主键，内部使用 |
| `carrier_name` | 物流商名称，运营可见 |
| `provider_kind` | 物流商系统类型，例如 `AGG56` |

不再使用人工填写的 `connection_code` 作为业务入口。

## 表结构改造

因为 `20260610_logistics_carrier_management.sql` 已经执行过，需要新增一份补丁 SQL，而不是直接改历史执行记录。

计划新增：

```text
RuoYi-Vue/sql/20260610_logistics_carrier_account_refactor.sql
```

补丁 SQL 做这些事：

1. 给主表新增 `carrier_account_id bigint auto_increment`。
2. 把 `connection_name` 改名或语义调整为 `carrier_name`。
3. 子表统一增加 `carrier_account_id`，并从旧 `connection_code` 回填。
4. 物流商渠道、渠道映射、面单订单、包裹文件、请求日志统一改用 `carrier_account_id` 关联具体物流商账号。
5. `logistics_agg56_connection` 改成按 `carrier_account_id` 保存 AGG56 账号凭据。
6. 如果当前物流商表已有业务数据，迁移脚本必须先预览并 fail-closed；当前刚上线没有业务数据时按空表迁移。
7. 迁移完成后，页面和接口不再接受用户传入 `connectionCode`。

## 授权流程改造

### 新增物流商账号

1. 运营点击“新增”。
2. 填写“物流商名称”。
3. 选择“物流商系统 = AGG56”。
4. 填写 `APP Token` 和 `APP Key`。
5. 后端调用 AGG56 授权接口校验。
6. 校验成功后：
   - 保存物流商账号。
   - 加密保存 `APP Token` 和 `APP Key`。
   - 保存脱敏值用于展示。
   - 写入授权请求日志。
   - 状态变为 `已配置`。
7. 校验失败时：
   - 不保存明文凭据。
   - 不覆盖已有有效凭据。
   - 写入失败请求日志。

### 修改物流商账号

普通编辑只改：

- 物流商名称
- 状态
- API 地址
- 备注

更换 `APP Token` / `APP Key` 走单独“授权/更换密钥”入口。

### 多账号支持

后端不限制同一个 `provider_kind = AGG56` 只能有一条记录。允许新增多个 AGG56 物流商账号。

每个账号独立拥有：

- 加密凭据
- AGG56 用户信息快照
- 物流商渠道
- 渠道映射
- 面单订单
- 外部请求日志

## 接口改造

管理端接口语义从“carrier connection”改成“carrier account”。

建议接口：

| 接口 | 说明 |
| --- | --- |
| `GET /logistics/admin/carrier-accounts/list` | 查询物流商账号列表 |
| `POST /logistics/admin/carrier-accounts` | 新增物流商账号，可同时提交 AGG56 token/key |
| `PUT /logistics/admin/carrier-accounts/{carrierAccountId}` | 编辑物流商账号基础信息 |
| `PUT /logistics/admin/carrier-accounts/{carrierAccountId}/agg56-credentials` | 更换 AGG56 凭据 |
| `POST /logistics/admin/carrier-accounts/{carrierAccountId}/authorize` | 授权校验 |
| `POST /logistics/admin/carrier-accounts/{carrierAccountId}/channels/sync` | 同步该账号的物流商渠道 |

外部模块下单、报价、取消面单时，也指定 `carrierAccountId`，而不是指定 `AGG56` 系统类型。

## 前端改造

列表列调整：

- 删除“接入编号”列。
- “接入名称”改为“物流商名称”。
- “接入方”改为“物流商系统”。
- 保留授权状态、账号脱敏信息、最近授权时间、最近同步时间。

新增表单调整：

- 删除“接入编号”输入框。
- 第一项为“物流商名称”。
- “物流商系统”默认 AGG56。
- AGG56 时显示 `APP Token` / `APP Key`。
- API 地址放到高级字段或默认隐藏，默认使用 `https://www.agg56.com`。

授权弹窗调整：

- 标题使用“更换密钥 - {物流商名称}”。
- 展示当前脱敏 token/key、AGG56 账号脱敏信息和客户代码。
- 原文凭据永不回显。

## 验证计划

1. 执行补丁 SQL 前，先写 SQL 执行记录并确认数据源。
2. 后端编译：
   - `mvn -pl logistics,ruoyi-admin -am -DskipTests compile`
3. SQL guard：
   - `mvn -pl ruoyi-system -am "-Dtest=SqlExecutionGuardContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
4. 前端类型检查：
   - `npm run tsc`
5. 敏感信息扫描：
   - 扫描用户给过的 AGG56 token/key 片段，确认没有写入源码、SQL、文档和日志。
6. 浏览器验证：
   - 页面不再出现“接入编号”。
   - 能新增多个 `AGG56` 物流商账号。
   - 每个账号可以独立保存或更换 APP Token / APP Key。
   - 物流商渠道、渠道映射和请求日志都按具体账号隔离。
7. CodeGraph：
   - 完成后执行 `codegraph sync .`。

## 本轮不做

- 不把 AGG56 token/key 写进 SQL seed。
- 不把 AGG56 做成系统唯一账号。
- 不让运营手动维护内部主键。
- 不做比价策略、费用看板、轨迹查询或结算能力。
