# 远端 seller/buyer 表迁移方案

日期：2026-06-03

状态：已确认并执行。执行记录见 `docs/status/2026-06-03-remote-seller-buyer-migration-execution.md`。

> 过期说明：本文记录的是早期远端迁移方案，其中 `seller_account.user_id` / `buyer_account.user_id` 绑定若依 `sys_user` 的设计已被后续迁移废弃。当前三端独立账号权限方向以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为准。

## 目标

把远端运行库从旧的 `urili_customer` / `urili_customer_account` 单表客户模型，迁移到当前代码已经使用的四张业务表：

```text
seller
buyer
seller_account
buyer_account
```

本方案只处理当前管理端卖家/买家主体与端账号绑定的表、字典、菜单影响。商品、库存、订单、履约、财务、领星接入等业务表不在本次范围内。

## 当前事实

- 当前后端激活配置为 `druid`，数据源来自 `RuoYi-Vue/ruoyi-admin/src/main/resources/application-druid.yml`。
- 本次核对只使用远端库 `fenxiao`，未读取本地数据库。
- 远端当前存在旧表：
  - `urili_customer`：1 条，`customer_kind = seller`
  - `urili_customer_account`：1 条，`portal_type = seller`
- 远端当前不存在：
  - `seller`
  - `buyer`
  - `seller_account`
  - `buyer_account`
- 当前代码已经读写新表：
  - `RuoYi-Vue/ruoyi-system/src/main/resources/mapper/system/UriliSellerMapper.xml` 读写 `seller`、`seller_account`
  - `RuoYi-Vue/ruoyi-system/src/main/resources/mapper/system/UriliBuyerMapper.xml` 读写 `buyer`、`buyer_account`
- `RuoYi-Vue/sql/urili_customer_management_seed.sql` 已包含新表 DDL、旧表迁移过程、新字典、角色和卖家/买家菜单初始化。

## 业务目的

### `seller`

保存卖家主体资料。卖家是独立业务主体，不再和买家混在同一张客户表里。

### `buyer`

保存买家主体资料。买家是独立业务主体，即使现实中和某个卖家公司相同，也不自动关联。

### `seller_account`

绑定 `seller.seller_id` 和若依 `sys_user.user_id`，表示某个若依登录账号属于某个卖家主体。

### `buyer_account`

绑定 `buyer.buyer_id` 和若依 `sys_user.user_id`，表示某个若依登录账号属于某个买家主体。

## 业务逻辑边界

这四张表承载：

- 卖家/买家主体基础资料
- 主体管理状态
- 若依用户和卖家/买家的账号绑定关系
- 账号绑定角色、状态、创建/更新时间、备注

这四张表不承载：

- 商品资料
- 库存数量或库存流水
- 订单、履约、物流、财务流水
- 外部系统请求日志
- 支付、结算、对账数据
- 文件二进制内容的长期存储规则

附件字段当前只保持验证阶段数据兼容。后续正式附件能力应复用 `FileStorageService` 或正式文件模块。

## 复用关系

- 登录账号继续复用若依 `sys_user`。
- 端账号角色继续复用若依 `sys_role`，当前角色 key 保留：
  - `urili_seller`
  - `urili_buyer`
- 菜单与按钮权限继续复用若依 `sys_menu`。
- 操作日志继续通过若依 `@Log` 与 `sys_oper_log`。
- 字典继续复用若依 `sys_dict_type` / `sys_dict_data`。

权限 key 暂时保留 `urili:admin:*`，因为它是安全命名空间，不是表名或 URL path。若后续要去掉权限里的 `urili:`，必须另起一轮权限迁移，不能和本次表迁移混做。

## 表设计

### `seller`

| 字段 | 类型 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `seller_id` | `bigint(20)` | 是 | 自增 | 主键 | 卖家 ID |
| `seller_no` | `varchar(64)` | 是 | 无 | 唯一索引 `uk_seller_no` | 系统内部卖家编号 |
| `seller_code` | `varchar(64)` | 是 | 无 | 唯一索引 `uk_seller_code` | 对外卖家代码 |
| `seller_name` | `varchar(200)` | 是 | 无 | 普通索引 `idx_seller_name` | 卖家全称 |
| `seller_short_name` | `varchar(100)` | 是 | `''` | 普通索引 `idx_seller_short_name` | 卖家简称 |
| `seller_type` | `varchar(32)` | 是 | `COMPANY` | 无 | 主体类型，保存 code |
| `seller_level` | `varchar(32)` | 是 | `L1` | 普通索引 `idx_seller_level` | 卖家等级，保存 code |
| `status` | `char(1)` | 是 | `0` | 普通索引 `idx_seller_status` | 状态：`0` 正常，`1` 停用 |
| `legal_id` | `varchar(100)` | 否 | `''` | 无 | 法人证件号 |
| `business_license_no` | `varchar(100)` | 否 | `''` | 无 | 营业执照号码 |
| `country_code` | `varchar(32)` | 是 | 无 | 无 | 国家/地区 code |
| `state_province` | `varchar(100)` | 是 | `''` | 无 | 省/州 |
| `city` | `varchar(100)` | 是 | 无 | 无 | 城市 |
| `postal_code` | `varchar(32)` | 是 | 无 | 无 | 邮编 |
| `address_line1` | `varchar(255)` | 是 | 无 | 无 | 地址 1 |
| `address_line2` | `varchar(255)` | 否 | `''` | 无 | 地址 2 |
| `contact_name` | `varchar(100)` | 是 | `''` | 无 | 联系人 |
| `contact_phone` | `varchar(64)` | 是 | `''` | 无 | 手机号 |
| `contact_email` | `varchar(128)` | 否 | `''` | 无 | 邮箱 |
| `attachment_file_name` | `varchar(255)` | 否 | `''` | 无 | 附件文件名 |
| `attachment_mime_type` | `varchar(100)` | 否 | `''` | 无 | 附件类型 |
| `attachment_size_bytes` | `bigint` | 否 | `null` | 无 | 附件大小 |
| `attachment_file_url` | `longtext` | 否 | `null` | 无 | 附件地址或验证阶段 data URL |
| `create_by` | `varchar(64)` | 否 | `''` | 无 | 创建者 |
| `create_time` | `datetime` | 否 | `null` | 无 | 创建时间 |
| `update_by` | `varchar(64)` | 否 | `''` | 无 | 更新者 |
| `update_time` | `datetime` | 否 | `null` | 无 | 更新时间 |
| `remark` | `varchar(500)` | 否 | `''` | 无 | 备注 |

### `buyer`

`buyer` 与 `seller` 字段结构一致，业务前缀从 `seller_` 改为 `buyer_`：

| 字段 | 类型 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `buyer_id` | `bigint(20)` | 是 | 自增 | 主键 | 买家 ID |
| `buyer_no` | `varchar(64)` | 是 | 无 | 唯一索引 `uk_buyer_no` | 系统内部买家编号 |
| `buyer_code` | `varchar(64)` | 是 | 无 | 唯一索引 `uk_buyer_code` | 对外买家代码 |
| `buyer_name` | `varchar(200)` | 是 | 无 | 普通索引 `idx_buyer_name` | 买家全称 |
| `buyer_short_name` | `varchar(100)` | 是 | `''` | 普通索引 `idx_buyer_short_name` | 买家简称 |
| `buyer_type` | `varchar(32)` | 是 | `COMPANY` | 无 | 主体类型，保存 code |
| `buyer_level` | `varchar(32)` | 是 | `L1` | 普通索引 `idx_buyer_level` | 买家等级，保存 code |
| 其它公共字段 | 同 `seller` | 同 `seller` | 同 `seller` | 同 `seller` | 资料、附件、审计字段 |

### `seller_account`

| 字段 | 类型 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `seller_account_id` | `bigint(20)` | 是 | 自增 | 主键 | 卖家账号绑定 ID |
| `seller_id` | `bigint(20)` | 是 | 无 | 联合唯一 `uk_seller_account_seller_user`，普通索引 `idx_seller_account_seller_status` | 关联 `seller.seller_id` |
| `user_id` | `bigint(20)` | 是 | 无 | 唯一索引 `uk_seller_account_user`，联合唯一 `uk_seller_account_seller_user` | 关联 `sys_user.user_id` |
| `account_role` | `varchar(32)` | 是 | `OWNER` | 无 | 卖家侧账号角色 |
| `status` | `char(1)` | 是 | `0` | 普通索引组合字段 | 绑定状态 |
| `create_by` | `varchar(64)` | 否 | `''` | 无 | 创建者 |
| `create_time` | `datetime` | 否 | `null` | 无 | 创建时间 |
| `update_by` | `varchar(64)` | 否 | `''` | 无 | 更新者 |
| `update_time` | `datetime` | 否 | `null` | 无 | 更新时间 |
| `remark` | `varchar(500)` | 否 | `''` | 无 | 备注 |

### `buyer_account`

| 字段 | 类型 | 必填 | 默认值 | 约束/索引 | 含义 |
| --- | --- | --- | --- | --- | --- |
| `buyer_account_id` | `bigint(20)` | 是 | 自增 | 主键 | 买家账号绑定 ID |
| `buyer_id` | `bigint(20)` | 是 | 无 | 联合唯一 `uk_buyer_account_buyer_user`，普通索引 `idx_buyer_account_buyer_status` | 关联 `buyer.buyer_id` |
| `user_id` | `bigint(20)` | 是 | 无 | 唯一索引 `uk_buyer_account_user`，联合唯一 `uk_buyer_account_buyer_user` | 关联 `sys_user.user_id` |
| `account_role` | `varchar(32)` | 是 | `OWNER` | 无 | 买家侧账号角色 |
| `status` | `char(1)` | 是 | `0` | 普通索引组合字段 | 绑定状态 |
| `create_by` | `varchar(64)` | 否 | `''` | 无 | 创建者 |
| `create_time` | `datetime` | 否 | `null` | 无 | 创建时间 |
| `update_by` | `varchar(64)` | 否 | `''` | 无 | 更新者 |
| `update_time` | `datetime` | 否 | `null` | 无 | 更新时间 |
| `remark` | `varchar(500)` | 否 | `''` | 无 | 备注 |

## code/label 与字典

数据库和 API 保存 code，前端展示 label。

| 字典类型 | 选项 | 用途 | 本次处理 |
| --- | --- | --- | --- |
| `subject_type` | `COMPANY`、`PERSON`、`OTHER` | 卖家/买家主体类型 | 新增 |
| `seller_level` | `L1`、`L2`、`L3` | 卖家等级 | 新增 |
| `buyer_level` | `L1`、`L2`、`L3` | 买家等级 | 新增 |
| `seller_account_role` | `OWNER`、`ADMIN`、`STAFF` | 卖家账号角色 | 新增 |
| `buyer_account_role` | `OWNER`、`ADMIN`、`STAFF` | 买家账号角色 | 新增 |
| `country_region` | 25 个国家/地区 code | 国家/地区 | 从旧 `urili_country_region` 口径迁移到无前缀类型 |

旧字典不立即删除：

- `urili_customer_type`
- `urili_country_region`

原因：保留回滚依据，避免旧数据或历史页面仍引用时立即断裂。

## 数据迁移映射

### 旧客户到卖家

| 旧字段 | 新字段 | 转换规则 |
| --- | --- | --- |
| `customer_id` | `seller_id` | 保持原 ID |
| `customer_no` | `seller_no` | 若以 `C` 开头，则改为 `S` + 后续编号 |
| `customer_code` | `seller_code` | 原值迁移 |
| `customer_name` | `seller_name` | 原值迁移 |
| `customer_short_name` | `seller_short_name` | 原值迁移 |
| `customer_type` | `seller_type` | `公司 -> COMPANY`，`个人 -> PERSON`，`其他 -> OTHER` |
| `customer_level` | `seller_level` | `等级1 -> L1`，`等级2 -> L2`，`等级3 -> L3` |
| `attachment_data_url` | `attachment_file_url` | 原值迁移 |
| 其它资料字段 | 同名公共字段 | 原值迁移 |

本次远端只有 `customer_kind = seller`，所以预计迁移：

- `seller`：1 条
- `seller_account`：1 条
- `buyer`：0 条
- `buyer_account`：0 条

### 旧客户到买家

同卖家规则，前缀改为 `buyer_`。远端当前没有 `customer_kind = buyer` 数据，但仍创建空表，保证买家管理接口可正常查询。

### 旧账号绑定

| 旧字段 | 新字段 | 转换规则 |
| --- | --- | --- |
| `account_id` | `seller_account_id` / `buyer_account_id` | 保持原 ID |
| `customer_id` | `seller_id` / `buyer_id` | 按 `portal_type` 分流 |
| `user_id` | `user_id` | 原值迁移 |
| `account_role` | `account_role` | 转大写，例如 `owner -> OWNER` |
| `status` | `status` | 原值迁移 |
| 审计字段 | 审计字段 | 原值迁移 |

## 约束与风险

- 新表不加物理外键，保持当前若依风格，依靠 Service 校验和索引保证数据一致性。
- `seller_account.user_id` 和 `buyer_account.user_id` 各自唯一。
- 跨表“同一个用户不能同时绑定卖家和买家”不能只靠单表唯一索引完成，当前 Service 已查询两张绑定表并抛出“该用户已绑定端账号”，后续如果端账号模型扩大，建议评估统一绑定表或事务级校验。
- 旧表不删除、不改名，作为回滚依据保留。
- `insert ignore` 迁移是幂等写入，重复执行不会重复插入同主键数据，但如果新表已有手工改动，需要先人工确认差异。

## 执行方案

### 阶段 1：远端预检查

只读检查：

```sql
select table_name
from information_schema.tables
where table_schema = database()
  and table_name in (
    'urili_customer',
    'urili_customer_account',
    'seller',
    'buyer',
    'seller_account',
    'buyer_account'
  );

select customer_kind, count(*)
from urili_customer
group by customer_kind;

select portal_type, count(*)
from urili_customer_account
group by portal_type;
```

必须满足：

- 旧表存在。
- 新四表不存在或为空。
- 旧表分组数量与本方案一致，当前应为 `seller = 1`。

### 阶段 2：执行现有 seed 脚本

确认后执行：

```powershell
cd E:\Urili-Ruoyi
Get-Content -Raw -Encoding UTF8 RuoYi-Vue\sql\urili_customer_management_seed.sql |
  docker run --rm -i --env "MYSQL_PWD=<masked>" mysql:8.4 mysql --host="<remote-host>" --port="<remote-port>" --user="<user>" --database="fenxiao" --default-character-set=utf8mb4
```

该脚本会：

- 创建 `seller`、`buyer`、`seller_account`、`buyer_account`。
- 补齐旧 `urili_customer` 兼容字段。
- 执行 `migrate_urili_customer_to_seller_buyer`。
- 新增或更新 `urili_seller`、`urili_buyer` 角色。
- 新增新字典类型和值。
- 更新客户管理、卖家管理、买家管理菜单，其中 `2011/2012` 的 component 会从远端旧值 `Urili/Customer/...` 改为当前前端存在的 `Urili/Seller/index`、`Urili/Buyer/index`。

### 阶段 3：清理远端残留菜单 path

seed 脚本只处理客户管理相关菜单，不会处理仓库、渠道、计费等旧残留 path。确认后建议追加执行：

```sql
update sys_menu
set path = 'warehouse-official', update_by = 'admin', update_time = sysdate()
where menu_id = 2021 and path = 'urili-warehouse-official';

update sys_menu
set path = 'warehouse-third-party', status = '1', update_by = 'admin', update_time = sysdate()
where menu_id = 2022 and path = 'urili-warehouse-third-party';

update sys_menu
set path = 'upstream-system', update_by = 'admin', update_time = sysdate()
where menu_id = 2031 and path = 'urili-upstream-system';

update sys_menu
set path = 'channel', update_by = 'admin', update_time = sysdate()
where menu_id = 2040 and path = 'urili-channel';

update sys_menu
set path = 'channel-system', update_by = 'admin', update_time = sysdate()
where menu_id = 2041 and path = 'urili-channel-system';

update sys_menu
set path = 'channel-customer', update_by = 'admin', update_time = sysdate()
where menu_id = 2042 and path = 'urili-channel-customer';

update sys_menu
set path = 'billing-handling-fee', update_by = 'admin', update_time = sysdate()
where menu_id = 2051 and path = 'urili-billing-handling-fee';

update sys_menu
set path = 'billing-freight', update_by = 'admin', update_time = sysdate()
where menu_id = 2052 and path = 'urili-billing-freight';

update sys_menu
set path = 'billing-quote-scheme', update_by = 'admin', update_time = sysdate()
where menu_id = 2053 and path = 'urili-billing-quote-scheme';
```

说明：

- `2022` 当前是可见且启用，但本工程前端没有 `Urili/Warehouse/ThirdParty/index` 页面。建议同时改 path 并停用，避免用户点到不存在的页面。
- 其它菜单当前已禁用或隐藏禁用，本次只去掉 URL path 的 `urili-` 前缀，不改权限 key。
- `component` 仍保留 `Urili/Warehouse/...` 等值，因为这些页面目前没有对应前端实现。后续真正开发仓库、渠道、计费模块时，应按当时的前端目录再同步调整 component。

### 阶段 4：执行后验证

远端 SQL 验证：

```sql
select count(*) from seller;
select count(*) from seller_account;
select count(*) from buyer;
select count(*) from buyer_account;

select seller_id, seller_no, seller_code, seller_type, seller_level, status
from seller;

select seller_account_id, seller_id, user_id, account_role, status
from seller_account;

select menu_id, menu_name, path, component, visible, status, perms
from sys_menu
where menu_id in (2010, 2011, 2012, 2021, 2022, 2031, 2040, 2041, 2042, 2051, 2052, 2053)
order by menu_id;

select dict_type, count(*)
from sys_dict_data
where dict_type in (
  'subject_type',
  'seller_level',
  'buyer_level',
  'seller_account_role',
  'buyer_account_role',
  'country_region'
)
group by dict_type;
```

预期结果：

- `seller = 1`
- `seller_account = 1`
- `buyer = 0`
- `buyer_account = 0`
- `seller.seller_no` 从旧 `customer_no` 转为 `S...`
- `seller.seller_type = COMPANY`
- `seller.seller_level = L1`
- `seller_account.account_role = OWNER`
- `sys_menu.path` 不再出现 `urili-*`
- `2011` component 为 `Urili/Seller/index`
- `2012` component 为 `Urili/Buyer/index`

应用验证：

- 后端重启后，卖家列表接口不再因为缺 `seller` 表报错。
- 买家列表接口不再因为缺 `buyer` 表报错。
- 有权限用户可进入卖家/买家管理。
- 无权限用户仍由后端 `@PreAuthorize` 拒绝。
- 字典展示 label，接口和数据库保存 code。

## 回滚方案

因为旧表不删除，本次回滚优先恢复代码可用性和菜单状态。

如果迁移后尚未产生新的卖家/买家业务数据：

```sql
drop table if exists seller_account;
drop table if exists buyer_account;
drop table if exists seller;
drop table if exists buyer;
```

如需恢复菜单 path：

```sql
update sys_menu set path = 'urili-warehouse-official' where menu_id = 2021;
update sys_menu set path = 'urili-warehouse-third-party', status = '0' where menu_id = 2022;
update sys_menu set path = 'urili-upstream-system' where menu_id = 2031;
update sys_menu set path = 'urili-channel' where menu_id = 2040;
update sys_menu set path = 'urili-channel-system' where menu_id = 2041;
update sys_menu set path = 'urili-channel-customer' where menu_id = 2042;
update sys_menu set path = 'urili-billing-handling-fee' where menu_id = 2051;
update sys_menu set path = 'urili-billing-freight' where menu_id = 2052;
update sys_menu set path = 'urili-billing-quote-scheme' where menu_id = 2053;
```

不建议回滚或删除若依角色、字典类型和字典数据，除非确认没有任何页面或接口引用。

如果迁移后已经新增业务数据，不能直接 drop 新表，应先导出新表、比对业务影响，再单独设计回滚。

## 权限检查结果

- 本次保留 `urili:admin:seller:*` 和 `urili:admin:buyer:*`。
- 后端控制器当前使用 `@PreAuthorize("@ss.hasPermi('urili:admin:...')")`。
- 远端 `sys_menu.perms` 与后端权限命名空间保持一致。
- 菜单 path 去掉 `urili-` 不影响权限 key。

## 字典/选项复用检查结果

- 新字典已在 `docs/architecture/reuse-ledger.md` 登记。
- 本次执行后远端会补齐 `subject_type`、`seller_level`、`buyer_level`、`seller_account_role`、`buyer_account_role`、`country_region`。
- 旧 `urili_customer_type`、`urili_country_region` 保留为历史兼容，不继续作为新页面依赖。

## 复用台账检查结果

已检查 `docs/architecture/reuse-ledger.md`：

- 卖家/买家主体类型、等级、账号角色、国家/地区已有复用规则。
- 后端公共支撑类 `UriliPartnerSupport`、`UriliPortalAccountSupport` 已登记。
- 前端 `PartnerManagementPage` 当前仍登记在 `react-ui/src/pages/Urili/...`，后续如果前端目录去掉 `Urili`，必须同步更新复用台账。

## 大文件合理性判断结果

本方案不新增实现文件，不触发 300/400/500 行代码文件拆分判断。

需要注意：`PartnerManagementPage.tsx` 是卖家/买家共用的大组件。后续前端目录改名时不要顺手扩大职责，应只做路径和 import 迁移。

## 重复代码检查结果

当前主模式是卖家/买家分表、Service 分模块、公共机械逻辑通过支撑类复用。

本次迁移会收敛旧 `customer` 单表模式，不新增第二套业务逻辑。

## 新增问题

1. 远端库缺少当前代码依赖的新四表，需要执行 DDL。
2. 远端旧客户和账号数据需要迁移到新表，不能直接删除旧表。
3. 远端 `2011/2012` 菜单 component 仍指向旧 `Urili/Customer/...`。
4. 远端还有多个 `urili-*` 菜单 path 残留，其中 `2022` 当前启用但前端页面不存在，建议迁移时停用。

## 已修复问题

- 本方案阶段未执行写操作，因此没有落地修复。
- 顶级菜单 path 此前已修为 `customer`、`product`、`order`、`inventory`、`warehouse`、`overseas-warehouse-service`、`finance`。

## 残留问题

- 前端目录仍是 `react-ui/src/pages/Urili/`、`react-ui/src/services/urili/`、`react-ui/src/types/urili/`。
- API 路径仍是 `/api/urili/admin/...`。
- Java 类名仍带 `Urili`。

这些不建议和本次远端表迁移混在同一步处理。表迁移验证通过后，再做前端目录与 service/type 命名清理。

## 未验证原因

- 未执行远端 DDL/DML，因为按照项目规则，新增业务表和远端数据调整必须先提交 Markdown 方案并得到确认。
- 未运行后端编译、前端构建、接口请求或浏览器验证，因为本阶段只交付迁移方案，没有落库。

## 确认后执行范围

如确认本方案，下一步执行范围为：

1. 对远端 `fenxiao` 执行 `RuoYi-Vue/sql/urili_customer_management_seed.sql`。
2. 对远端 `sys_menu` 执行阶段 3 的残留 path 清理 SQL，并停用 `2022`。
3. 执行阶段 4 的远端 SQL 验证。
4. 生成 Markdown 执行记录，写明实际执行命令类型、远端目标、影响行数、验证结果和未验证事项。
