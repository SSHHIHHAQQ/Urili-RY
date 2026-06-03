# 管理端客户字段对齐记录

> 说明：本文记录的是早期 `urili_customer` 单表草稿的字段对齐过程，已被 `docs/architecture/2026-06-03-seller-buyer-split-module-design.md` 的 `seller` / `buyer` 分表规则取代。后续实现以卖家/买家独立模块为准，不再继续扩展本文的客户单表口径。

## 背景

当前若依验证工程里的“新建卖家/买家”弹窗此前按最小客户资料模型实现，字段和布局没有对齐原项目 `E:\Urili` 的客户账户表单，导致管理端表单缺少用户名、客户等级、客户类型、证件、营业执照、附件等字段，且布局不符合原项目使用习惯。

本次调整只对齐管理端卖家/买家客户账户字段，不迁移旧项目代码。后端仍以 `RuoYi-Vue` 为核心，账号落若依 `sys_user`，客户资料落 `urili_customer`，客户与端账号绑定落 `urili_customer_account`。

## 原项目字段

原项目新增卖家/买家账户表单字段如下：

| 字段 | 原项目含义 | 若依验证工程落点 |
| --- | --- | --- |
| username | 登录用户名 | `sys_user.user_name`，并通过 `urili_customer_account` 绑定客户 |
| customerLevel | 客户等级 | `urili_customer.customer_level` |
| customerType | 客户类型 | `urili_customer.customer_type`，默认 `公司`，选项来自 `urili_customer_type` |
| customerName | 客户全称 | `urili_customer.customer_name` |
| customerCode | 客户代码，对外沟通使用 | `urili_customer.customer_code`，同一端内唯一，必填 |
| customerShortName | 客户简称 | `urili_customer.customer_short_name` |
| legalId | 法人证件号 | `urili_customer.legal_id` |
| businessLicenseNo | 营业执照号码 | `urili_customer.business_license_no` |
| attachment | 附件 | 暂存到 `urili_customer.attachment_*` 字段，后续可替换为正式文件模块 |
| contactName | 联系人 | `urili_customer.contact_name` |
| phone | 手机号 | `urili_customer.contact_phone` |
| email | 邮箱 | `urili_customer.contact_email` |
| address1 | 地址1 | `urili_customer.address_line1` |
| address2 | 地址2 | `urili_customer.address_line2` |
| city | 城市 | `urili_customer.city` |
| state | 省/州 | `urili_customer.state_province` |
| countryCode | 国家/地区 | `urili_customer.country_code`，选项来自 `urili_country_region` |
| postalCode | 邮编 | `urili_customer.postal_code` |
| remark | 备注 | `urili_customer.remark` |

## 当前实现边界

- 新增客户时，`username` 是客户登录账号，系统会同步创建一个客户主账号，默认密码为 `U12346`。
- 客户类型固定为 `公司`、`个人`、`其他`，由若依字典 `urili_customer_type` 维护。
- 国家/地区保存 2 位代码，页面使用可模糊搜索下拉框，选项由若依字典 `urili_country_region` 维护。
- 买家和卖家即使是同一家公司，也分别创建独立客户和独立 `sys_user` 账号，不自动建立关联。
- 客户代码 `customerCode` 必填，作为对外沟通代码；系统内部客户号 `customerNo` 自动生成，并展示在买家/卖家管理表格。
- `customerNo` 规则：`端前缀 + 年份字母 + 月份字母 + 2位日期 + 4位日流水`。卖家端前缀为 `S`，买家端前缀为 `B`；`2026=A`、`2027=B`，月份 `1月=A`、`2月=B`，日流水按端前缀每天从 `0001` 重新开始。例：2026 年 6 月 3 日第 1 个卖家为 `SAF030001`，第 1 个买家为 `BAF030001`。
- 附件当前按原项目字段形态保存为 data URL，属于验证阶段实现；正式文件服务上线后应替换为文件 ID 或对象存储引用。
- 编辑时登录用户名暂不直接改写，避免破坏若依用户登录标识；如需更换登录账号，后续应单独设计账号变更流程。

## 验证重点

- 卖家/买家新增弹窗字段顺序和原项目一致。
- 新增客户能同时生成客户资料、若依用户和客户端账号绑定。
- 同一客户代码在同一端内不能重复。
- 同一公司可以在买家端和卖家端分别建立独立账号。
- `urili_customer_type` 字典返回 `公司/个人/其他`。
- `urili_country_region` 字典返回 25 个常用国家/地区代码，国家/地区下拉支持按中文、英文和代码模糊搜索。
- 临时卖家客户验证中，列表返回了 `customerNo`，主账号密码哈希匹配 `U12346`，验证后临时数据已清理。
