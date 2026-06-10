# 2026-06-10 买家/卖家管理样本主体数据执行记录

## 背景

用户要求在管理端“买家管理”和“卖家管理”各新增 10 位样本主体，并要求信息尽量真实，不使用“测试公司”这类占位文案。

本次仅补充管理端买家/卖家主体样本数据，不新增表、不改菜单、不改权限、不改代码。

## 数据源确认

- 后端配置来源：`RuoYi-Vue/ruoyi-admin/src/main/resources/application.yml` + `application-druid.yml` + `.env.local`。
- MySQL：`.env.local` 中 `RUOYI_DB_URL` 指向远端 `fenxiao` 库，端口 `28634`。
- Redis：`.env.local` 中 `RUOYI_REDIS_HOST` 指向远端 Redis，逻辑库 `1`。
- 本次不读取、不写入本地 Docker MySQL/Redis。
- 本记录不输出 `.env.local` 中的密码、token secret、Redis 密码或外部密钥。

## 写入方式

优先通过现有后端管理端接口写入：

- `POST /seller/admin/sellers`
- `POST /buyer/admin/buyers`

选择接口写入的原因：

- 复用 `SellerServiceImpl.insertSeller(...)` / `BuyerServiceImpl.insertBuyer(...)` 的校验逻辑。
- 自动生成 `seller_no` / `buyer_no`。
- 自动创建端内 OWNER 主账号。
- 自动绑定端内 owner 角色和默认菜单权限。
- 自动写入管理端操作日志。

## 样本数据边界

- 公司名、地址、联系人、邮箱均为真实风格的虚构样本，不冒用真实注册公司作为业务主体。
- 联系电话使用虚构/保留号码段或样本号码风格，避免误写真实个人联系方式。
- 邮箱使用 `.example` 保留域名，避免误发邮件。
- 主账号初始密码使用当前服务层既有默认逻辑，本记录不写明明文密码。

## 拟新增卖家

| 序号 | sellerCode | sellerName | sellerShortName | username | sellerLevel | 国家/城市 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | SELL-US-PRT-001 | Pacific Ridge Trading LLC | Pacific Ridge | s_pacificridge | L1 | US / Portland |
| 2 | SELL-US-BHC-002 | Blue Harbor Consumer Goods Inc. | Blue Harbor | s_blueharbor | L2 | US / Long Beach |
| 3 | SELL-US-RWO-003 | Redwood Outdoor Supply Co. | Redwood Outdoor | s_redwoodoutdoor | L2 | US / Seattle |
| 4 | SELL-US-SPE-004 | SummitPeak Electronics Distribution LLC | SummitPeak | s_summitpeak | L1 | US / Austin |
| 5 | SELL-US-MKW-005 | Meridian Kitchenware Manufacturing Inc. | Meridian Kitchenware | s_meridiankw | L3 | US / Chicago |
| 6 | SELL-US-NAP-006 | Northstar Apparel Supply LLC | Northstar Apparel | s_northstarapparel | L2 | US / Columbus |
| 7 | SELL-CN-GHT-007 | Greenfield Home Textile Co., Ltd. | Greenfield Textile | s_greenfieldtextile | L2 | CN / Ningbo |
| 8 | SELL-CN-ZSP-008 | Zhejiang Sunrise Packaging Co., Ltd. | Sunrise Packaging | s_sunrisepackaging | L1 | CN / Hangzhou |
| 9 | SELL-US-EPP-009 | Evergreen Pet Products LLC | Evergreen Pet | s_evergreenpet | L3 | US / Denver |
| 10 | SELL-US-ASL-010 | Apex Smart Living Ltd. | Apex Smart Living | s_apexsmartliving | L1 | US / San Jose |

## 拟新增买家

| 序号 | buyerCode | buyerName | buyerShortName | username | buyerLevel | 国家/城市 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | BUY-US-CRG-001 | Cascadia Retail Group LLC | Cascadia Retail | b_cascadiaretail | L1 | US / Portland |
| 2 | BUY-US-MMM-002 | Maple & Main Marketplace Inc. | Maple Main | b_maplemain | L2 | US / Minneapolis |
| 3 | BUY-US-LHS-003 | Lakeside Home Stores LLC | Lakeside Home | b_lakesidehome | L2 | US / Madison |
| 4 | BUY-US-UNC-004 | UrbanNest Commerce Ltd. | UrbanNest | b_urbannest | L1 | US / New York |
| 5 | BUY-US-HOR-005 | Horizon Outfitters Retail Inc. | Horizon Outfitters | b_horizonoutfit | L3 | US / Salt Lake City |
| 6 | BUY-US-BDS-006 | Briarwood Department Stores LLC | Briarwood Stores | b_briarwoodstores | L2 | US / Charlotte |
| 7 | BUY-US-NGW-007 | Northgate Online Wholesale Inc. | Northgate Online | b_northgateonline | L2 | US / Phoenix |
| 8 | BUY-US-SOS-008 | Skyline Office Supply Buyers LLC | Skyline Office | b_skylineoffice | L1 | US / Dallas |
| 9 | BUY-US-GBI-009 | Golden Bridge Imports LLC | Golden Bridge | b_goldenbridge | L3 | US / Los Angeles |
| 10 | BUY-US-HLP-010 | HarborLane Pharmacy Retail LLC | HarborLane Pharmacy | b_harborlane | L2 | US / Tampa |

## 执行前预检

- `GET /captchaImage` 返回 `captchaEnabled=false`。
- 管理端 `admin / admin123` 登录成功，返回 token 未在记录中明文输出。
- `GET /seller/admin/sellers/list?pageNum=1&pageSize=200`：当前总数 4。
- `GET /buyer/admin/buyers/list?pageNum=1&pageSize=200`：当前总数 1。
- 拟写入的 `sellerCode` / `buyerCode` 和 `username` 均使用本次独立前缀，预期不与当前列表冲突。

## 回滚方式

如需回滚，优先通过管理端删除/停用流程处理；若必须数据库回滚，应按本记录中的 `sellerCode` / `buyerCode` 精确定位，并同时核对自动创建的端内 OWNER 账号、owner 角色、账号角色绑定和操作日志，不允许按宽泛时间范围删除。

## 执行结果

已执行。

执行时间：2026-06-10 19:28 后。

执行方式：

- 管理端登录成功，token 未明文落盘。
- 逐条调用 `POST /seller/admin/sellers` 新增卖家主体。
- 逐条调用 `POST /buyer/admin/buyers` 新增买家主体。
- 每条主体新增后由后端服务层自动创建 1 个 OWNER 主账号，并绑定 owner 角色和默认端内菜单权限。

执行后接口校验：

| 类型 | 执行前总数 | 新增数量 | 执行后总数 |
| --- | ---: | ---: | ---: |
| 卖家 | 4 | 10 | 14 |
| 买家 | 1 | 10 | 11 |

新增卖家结果：

| sellerId | sellerNo | sellerCode | sellerName | username | 主账号数 |
| ---: | --- | --- | --- | --- | ---: |
| 11 | SAF100001 | SELL-US-PRT-001 | Pacific Ridge Trading LLC | s_pacificridge | 1 |
| 12 | SAF100002 | SELL-US-BHC-002 | Blue Harbor Consumer Goods Inc. | s_blueharbor | 1 |
| 13 | SAF100003 | SELL-US-RWO-003 | Redwood Outdoor Supply Co. | s_redwoodoutdoor | 1 |
| 14 | SAF100004 | SELL-US-SPE-004 | SummitPeak Electronics Distribution LLC | s_summitpeak | 1 |
| 15 | SAF100005 | SELL-US-MKW-005 | Meridian Kitchenware Manufacturing Inc. | s_meridiankw | 1 |
| 16 | SAF100006 | SELL-US-NAP-006 | Northstar Apparel Supply LLC | s_northstarapparel | 1 |
| 17 | SAF100007 | SELL-CN-GHT-007 | Greenfield Home Textile Co., Ltd. | s_greenfieldtextile | 1 |
| 18 | SAF100008 | SELL-CN-ZSP-008 | Zhejiang Sunrise Packaging Co., Ltd. | s_sunrisepackaging | 1 |
| 19 | SAF100009 | SELL-US-EPP-009 | Evergreen Pet Products LLC | s_evergreenpet | 1 |
| 20 | SAF100010 | SELL-US-ASL-010 | Apex Smart Living Ltd. | s_apexsmartliving | 1 |

新增买家结果：

| buyerId | buyerNo | buyerCode | buyerName | username | 主账号数 |
| ---: | --- | --- | --- | --- | ---: |
| 3 | BAF100001 | BUY-US-CRG-001 | Cascadia Retail Group LLC | b_cascadiaretail | 1 |
| 4 | BAF100002 | BUY-US-MMM-002 | Maple & Main Marketplace Inc. | b_maplemain | 1 |
| 5 | BAF100003 | BUY-US-LHS-003 | Lakeside Home Stores LLC | b_lakesidehome | 1 |
| 6 | BAF100004 | BUY-US-UNC-004 | UrbanNest Commerce Ltd. | b_urbannest | 1 |
| 7 | BAF100005 | BUY-US-HOR-005 | Horizon Outfitters Retail Inc. | b_horizonoutfit | 1 |
| 8 | BAF100006 | BUY-US-BDS-006 | Briarwood Department Stores LLC | b_briarwoodstores | 1 |
| 9 | BAF100007 | BUY-US-NGW-007 | Northgate Online Wholesale Inc. | b_northgateonline | 1 |
| 10 | BAF100008 | BUY-US-SOS-008 | Skyline Office Supply Buyers LLC | b_skylineoffice | 1 |
| 11 | BAF100009 | BUY-US-GBI-009 | Golden Bridge Imports LLC | b_goldenbridge | 1 |
| 12 | BAF100010 | BUY-US-HLP-010 | HarborLane Pharmacy Retail LLC | b_harborlane | 1 |

未执行项：

- 未直接执行 SQL。
- 未新增或修改 DDL。
- 未修改菜单、权限、前端页面或后端代码。
- 未输出或保存任何明文密钥。

## 第二次补齐计划：各补到 35 位

用户后续要求“买家和卖家都填 35 位”。本次按管理端列表总数理解：卖家管理和买家管理各自补齐到 35 位。

执行前复核：

- 卖家当前总数：14；需要新增 21 位。
- 买家当前总数：11；需要新增 24 位。

本次仍然只通过后端管理端接口新增主体：

- `POST /seller/admin/sellers`
- `POST /buyer/admin/buyers`

本次新增样本继续沿用真实风格虚构数据原则：

- 公司名、联系人、地址、邮箱为业务样本数据，不冒用真实注册主体。
- 邮箱使用 `.example` 保留域名。
- 电话使用样本号码风格，避免写入真实个人联系方式。
- 主账号由服务层自动创建，本记录不写明明文密码。

本次拟使用的代码范围：

- 卖家：`SELL-US-VNW-011` 至 `SELL-CN-HMT-031`，共 21 位。
- 买家：`BUY-US-FMR-011` 至 `BUY-US-PRM-034`，共 24 位。

第二次执行结果：已执行。

执行方式：

- 管理端登录成功，token 未明文落盘。
- 写入前再次检查当前总数、`sellerCode` / `buyerCode` 和 `username` 冲突。
- 逐条调用 `POST /seller/admin/sellers` 新增 21 位卖家主体。
- 逐条调用 `POST /buyer/admin/buyers` 新增 24 位买家主体。
- 每条主体新增后由后端服务层自动创建 1 个 OWNER 主账号，并绑定 owner 角色和默认端内菜单权限。

第二次执行后接口校验：

| 类型 | 第二次执行前总数 | 第二次新增数量 | 第二次执行后总数 |
| --- | ---: | ---: | ---: |
| 卖家 | 14 | 21 | 35 |
| 买家 | 11 | 24 | 35 |

第二次新增卖家结果：

| sellerId | sellerNo | sellerCode | sellerName | username | 主账号数 |
| ---: | --- | --- | --- | --- | ---: |
| 21 | SAF100011 | SELL-US-VNW-011 | ValleyNorth Wholesale Supply LLC | s_valleynorth | 1 |
| 22 | SAF100012 | SELL-US-CFT-012 | ClearFork Tools and Hardware Inc. | s_clearforktools | 1 |
| 23 | SAF100013 | SELL-US-OMP-013 | Oakmont Market Products LLC | s_oakmontproducts | 1 |
| 24 | SAF100014 | SELL-US-SMB-014 | SilverMile Baby & Kids Co. | s_silvermilekids | 1 |
| 25 | SAF100015 | SELL-US-HGC-015 | Harbor Grove Candles LLC | s_harborgrove | 1 |
| 26 | SAF100016 | SELL-US-POM-016 | Prairie Oak Furniture Makers Inc. | s_prairieoak | 1 |
| 27 | SAF100017 | SELL-US-WCP-017 | Westward Craft Paper Co. | s_westwardpaper | 1 |
| 28 | SAF100018 | SELL-US-MAS-018 | Marble Arch Sports Goods LLC | s_marblearch | 1 |
| 29 | SAF100019 | SELL-US-RCL-019 | RiverCrest Lighting LLC | s_rivercrestlight | 1 |
| 30 | SAF100020 | SELL-US-CBC-020 | Cedar Bloom Cosmetics LLC | s_cedarbloom | 1 |
| 31 | SAF100021 | SELL-CN-SKC-021 | Shenzhen Keenlink Components Co., Ltd. | s_keenlink | 1 |
| 32 | SAF100022 | SELL-CN-QWA-022 | Qingdao Wavecrest Outdoor Gear Co., Ltd. | s_wavecrest | 1 |
| 33 | SAF100023 | SELL-CN-YHP-023 | Yiwu Horizon Gifts & Premiums Co., Ltd. | s_horizongifts | 1 |
| 34 | SAF100024 | SELL-CN-NKD-024 | Nanjing Kenda Daily Necessities Co., Ltd. | s_kendadaily | 1 |
| 35 | SAF100025 | SELL-CN-XFC-025 | Xiamen FreshCoast Household Products Co., Ltd. | s_freshcoast | 1 |
| 36 | SAF100026 | SELL-US-EWS-026 | Eastwood Stationery Works LLC | s_eastwoodstat | 1 |
| 37 | SAF100027 | SELL-US-LHA-027 | LakeHouse Artisan Decor Inc. | s_lakehousedecor | 1 |
| 38 | SAF100028 | SELL-US-BLT-028 | BrightLane Tech Accessories LLC | s_brightlanetech | 1 |
| 39 | SAF100029 | SELL-US-NVF-029 | NorthVale Foodservice Supplies Inc. | s_northvalefood | 1 |
| 40 | SAF100030 | SELL-US-TBP-030 | TrailBridge Pet Supply Co. | s_trailbridgepet | 1 |
| 41 | SAF100031 | SELL-CN-HMT-031 | Hefei Mingtai Storage Solutions Co., Ltd. | s_mingtaistorage | 1 |

第二次新增买家结果：

| buyerId | buyerNo | buyerCode | buyerName | username | 主账号数 |
| ---: | --- | --- | --- | --- | ---: |
| 13 | BAF100011 | BUY-US-FMR-011 | Fieldstone Market Retailers LLC | b_fieldstonemkt | 1 |
| 14 | BAF100012 | BUY-US-RLB-012 | RiverLane Boutique Collective Inc. | b_riverlanebout | 1 |
| 15 | BAF100013 | BUY-US-WHD-013 | WestHill Digital Commerce LLC | b_westhilldigital | 1 |
| 16 | BAF100014 | BUY-US-CPS-014 | Canyon Point Stores LLC | b_canyonpoint | 1 |
| 17 | BAF100015 | BUY-US-ORC-015 | Orchard Row Commerce Inc. | b_orchardrow | 1 |
| 18 | BAF100016 | BUY-US-EHC-016 | ElmHouse Catalog Retail LLC | b_elmhouse | 1 |
| 19 | BAF100017 | BUY-US-SGC-017 | StoneGate Club Stores Inc. | b_stonegates | 1 |
| 20 | BAF100018 | BUY-US-MPC-018 | MetroPlex Convenience Buyers LLC | b_metroplex | 1 |
| 21 | BAF100019 | BUY-US-HVR-019 | HighView Retail Partners LLC | b_highviewretail | 1 |
| 22 | BAF100020 | BUY-US-FRC-020 | FreshRoute Commerce LLC | b_freshroute | 1 |
| 23 | BAF100021 | BUY-US-GLS-021 | GreenLeaf Specialty Shops LLC | b_greenleafshops | 1 |
| 24 | BAF100022 | BUY-US-RQC-022 | RedQuill Commerce Group Inc. | b_redquill | 1 |
| 25 | BAF100023 | BUY-US-MLW-023 | MidLake Wholesale Buyers LLC | b_midlakebuyers | 1 |
| 26 | BAF100024 | BUY-US-OLR-024 | OakLine Retail Network LLC | b_oaklinenetwork | 1 |
| 27 | BAF100025 | BUY-US-PWR-025 | PineWorks Retail Alliance Inc. | b_pineworks | 1 |
| 28 | BAF100026 | BUY-US-AFS-026 | Atlas Family Stores LLC | b_atlasfamilystore | 1 |
| 29 | BAF100027 | BUY-US-BCC-027 | BlueCrest Club Commerce Inc. | b_bluecrestclub | 1 |
| 30 | BAF100028 | BUY-US-SBW-028 | SouthBay Wholesale Outlet LLC | b_southbayoutlet | 1 |
| 31 | BAF100029 | BUY-US-LRC-029 | Lighthouse Retail Cooperative LLC | b_lighthouseretail | 1 |
| 32 | BAF100030 | BUY-US-HMC-030 | HomeMakers Central Buying LLC | b_homemakers | 1 |
| 33 | BAF100031 | BUY-US-CBR-031 | Cobalt Ridge Retail LLC | b_cobaltridge | 1 |
| 34 | BAF100032 | BUY-US-KDM-032 | Keystone Direct Merchants Inc. | b_keystonedirect | 1 |
| 35 | BAF100033 | BUY-US-TMS-033 | Timberline Market Stores LLC | b_timberlinemkt | 1 |
| 36 | BAF100034 | BUY-US-PRM-034 | Parkside Retail Merchants LLC | b_parksideretail | 1 |

第二次未执行项：

- 未直接执行 SQL。
- 未新增或修改 DDL。
- 未修改菜单、权限、前端页面或后端代码。
- 未输出或保存任何明文密钥。
