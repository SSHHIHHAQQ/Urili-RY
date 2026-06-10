# 美国标准最终承运商字典草案

日期：2026-06-10

## 目的

本文件用于物流商管理的“标准最终承运商”字典设计。它不是物流商 API 接入方清单，也不是系统渠道清单。

标准最终承运商用于把物流商或中间商返回的不稳定渠道名称归一化。例如外部渠道名可能是 `USPS超快`、`美国邮政`、`U-GA`，运营可以统一映射到 `USPS`，再由系统渠道承载内部稳定编码。

## 当前口径

2026-06-10 用户已提供第一版承运商清单。后续首批字典 seed 以用户提供清单为准，不再继续按公开资料扩展。

公开调研只作为理解“承运商/中间商/服务产品/区域尾端混合存在”的背景，不作为本轮落库范围的权威来源。

## 调研结论

美国不存在一个稳定的“所有快递商官方全集”。原因是：

- 全国性承运商、区域承运商、电商最后一公里、跨境包裹商和大件/卡车尾端服务都可能承担最终派送。
- 中间商返回的最终承运商名称不统一，同一家承运商可能有旧品牌、别名或服务名。
- 新兴最后一公里承运商变化很快，需要字典支持后续增补。

因此第一版建议做“用户清单覆盖 + 可扩展”的字典 seed：先完整承接用户提供的承运商名称，后续遇到物流商 API 返回的新名称，再通过物流商渠道映射和字典增量补充。

## 用户清单处理规则

- 用户提供的名称是第一版原始来源。
- 精确重复项去重，例如完全相同的 `J&T` 只保留一条。
- 大小写、地区、服务差异默认不擅自合并，例如 `FedEx`、`fedex-express`、`FedEx US`、`FedEx EU`、`Fedex Smartpost` 先保留为不同字典项或别名候选。
- 明显属于服务产品、平台渠道或特殊操作的值，例如 `DDP`、`SELFPICK`、`FBA TRUCKING`、`TRUCKING-SELF`，先保留在清单预览中，落库前再确认是否进入同一个 `logistics_final_carrier` 字典，还是拆到渠道/运输方式字典。
- `Other`、`Custom`、`自营` 属于兜底项，保留但需要固定排序靠后。
- 中文值例如 `菜鸟`、`澳邮小包`、`自营` 保留原文 label；生成 SQL 时必须使用 UTF-8。
- 稳定 code 建议由原始名称规范化生成：大写、去首尾空格、括号内容转下划线、非字母数字统一为下划线，再人工处理冲突。
- 原始 label 必须保留，不能因为生成 code 而丢失用户给出的大小写和地区写法。

## 建议字典类型

| 字典项 | 建议值 |
| --- | --- |
| `dict_type` | `logistics_final_carrier` |
| 业务含义 | 标准最终承运商 |
| 存储值 | `dict_value` 保存稳定 code |
| 展示值 | `dict_label` 保存中文/英文展示名 |
| 别名处理 | 物流商渠道映射表保存外部原始名称和标准承运商 code，不把所有别名塞进 `sys_dict_data` |
| 初始化方式 | 后续生成 guarded SQL，确认后写入 `sys_dict_type` / `sys_dict_data` |

## 首批字典 seed 草案

以下表格是公开调研阶段形成的主流承运商草案。由于用户已经提供完整业务清单，本表不再作为最终 seed 范围，只作为后续归并、排序和分类时的参考。

| sort | dict_value | dict_label | 分类 | 备注/常见别名 |
| ---: | --- | --- | --- | --- |
| 1 | `USPS` | USPS / 美国邮政 | 全国邮政包裹 | United States Postal Service、美国邮政 |
| 2 | `UPS` | UPS | 全国包裹 | United Parcel Service |
| 3 | `FEDEX` | FedEx | 全国包裹 | FedEx Express、FedEx Ground |
| 4 | `DHL_ECOMMERCE` | DHL eCommerce | 电商包裹/跨境 | DHL eCommerce、DHL Global Mail |
| 5 | `DHL_EXPRESS` | DHL Express | 国际快递 | DHL Express |
| 6 | `AMAZON_LOGISTICS` | Amazon Logistics / Amazon Shipping | 全国/电商包裹 | Amazon Shipping、Amazon Logistics、TBA |
| 7 | `ONTRAC` | OnTrac | 区域/最后一公里 | OnTrac、LaserShip |
| 8 | `GLS_US` | GLS US | 区域包裹 | GLS、GSO、Golden State Overnight |
| 9 | `LSO` | LSO / Lone Star Overnight | 区域包裹 | LSO、Lone Star Overnight |
| 10 | `SPEE_DEE` | Spee-Dee Delivery | 区域包裹 | Spee-Dee |
| 11 | `VEHO` | Veho | 电商最后一公里 | Veho |
| 12 | `UNIUNI` | UniUni | 电商最后一公里 | UniUni |
| 13 | `GOFO` | GOFO | 电商最后一公里 | Gofo、GoFo Express |
| 14 | `JITSU` | Jitsu | 电商最后一公里 | Jitsu、AxleHire 历史品牌需单独核对 |
| 15 | `SPEEDX` | SpeedX | 电商最后一公里 | SpeedX |
| 16 | `BETTER_TRUCKS` | Better Trucks | 区域/最后一公里 | Better Trucks |
| 17 | `ROADIE` | Roadie | 同城/最后一公里 | Roadie，UPS 体系下同城配送能力 |
| 18 | `CDL_LAST_MILE` | CDL Last Mile | 区域/最后一公里 | CDL |
| 19 | `LMS` | Last Mile Solutions | 区域/最后一公里 | LMS、Last Mile Solutions |
| 20 | `CIRRO_ECOMMERCE` | CIRRO E-Commerce | 电商包裹 | CIRRO、CirroParcel |
| 21 | `COURIER_EXPRESS` | Courier Express | 区域/最后一公里 | Courier Express |
| 22 | `OSM_WORLDWIDE` | OSM Worldwide | 电商包裹/USPS 汇流 | OSM |
| 23 | `PITNEY_BOWES` | Pitney Bowes / Newgistics | 电商包裹/USPS 汇流 | Pitney Bowes、Newgistics |
| 24 | `ASENDIA` | Asendia | 跨境电商包裹 | Asendia、Globegistics |
| 25 | `LANDMARK_GLOBAL` | Landmark Global | 跨境电商包裹 | Landmark Global |
| 26 | `PASSPORT` | Passport | 跨境电商包裹 | Passport Shipping |
| 27 | `APC_POSTAL_LOGISTICS` | APC Postal Logistics | 跨境电商包裹 | APC、APC Postal Logistics |
| 28 | `SKYPOSTAL` | SkyPostal | 跨境电商包裹 | SkyPostal |
| 29 | `RRD` | RRD / RR Donnelley | 电商/跨境/物流 | RR Donnelley、RRD |
| 30 | `SPEEDPAK` | SpeedPAK | 电商跨境进口 | SpeedPAK、Orange Connex |
| 31 | `EBAY_DELIVERY_SERVICES` | eBay Delivery Services | 平台物流 | eBay delivery services |
| 32 | `CANPAR` | Canpar | 美加跨境/区域包裹 | Canpar USA |
| 33 | `GOFOR` | GoFor | 大件/最后一公里 | GoFor、big & bulky |
| 34 | `CDS_LOGISTICS` | CDS Logistics | 大件/最后一公里 | CDS Logistics |
| 35 | `WARP` | Warp | 卡车/最后一公里 | Cargo van、box truck、LTL/FTL 网络 |
| 36 | `HOVERSHIP` | Hovership | 区域/最后一公里 | Hovership |
| 900 | `OTHER` | 其他承运商 | 兜底 | 字典暂未覆盖但人工已确认的承运商 |
| 999 | `UNKNOWN` | 未识别承运商 | 兜底 | 物流商渠道未完成标准化时临时使用 |

## 暂不直接放入首批 seed 的对象

以下对象更像服务、渠道、平台能力或 LTL/货运网络，不建议第一版直接混入“快递/尾端最终承运商”主字典，除非后续实际中间商渠道明确返回这些名称：

- `UPS SurePost`、`UPS Mail Innovations`、`FedEx Ground Economy/SmartPost`：更像服务产品或汇流渠道，不是新的最终承运商。
- `FedEx Freight`、`TForce Freight`、`R+L Carriers`、`Saia`、`Estes`、`Old Dominion`、`ABF Freight`、`XPO`：LTL/货运承运商，建议后续如果做大件/卡车履约，再进入 `logistics_freight_carrier` 或扩展分类。
- 本地 courier、白手套大件安装、家具配送商：数量非常多，不适合一次性穷举；应通过物流商渠道人工映射增补。

## 后续落库规则

- 未经确认前，不生成或执行 `sys_dict` 写入 SQL。
- 真正落库时应生成带确认 token 的幂等 SQL，并记录执行目标数据源。
- `dict_value` 一旦用于系统渠道或面单记录，不应随意重命名；别名变化应通过物流商渠道映射解决。
- 删除或禁用字典值前必须检查是否已有系统渠道、物流商渠道映射或面单记录引用。
- 生成 SQL 前必须先输出 Markdown 预览，包含：原始数量、精确去重数量、生成 code、label、分类、是否进入 `logistics_final_carrier`、需要人工确认的异常项。

## 调研来源

- Pitney Bowes Parcel Shipping Index 2026：美国包裹市场主流承运商和替代承运商趋势，列出 OnTrac、GLS、LSO、Spee-Dee、Veho、UniUni、Gofo、Jitsu、SpeedX、Better Trucks 等。
- EasyPost Carrier Guides：多承运商 API 支持 carrier account、费率、标签、追踪等能力，并列出 USPS、UPS、FedEx、DHL Express、GLS US、LSO、OnTrac、Spee-Dee、CDL、CIRRO、Courier Express 等 carrier guide。
- UPS、FedEx、USPS、DHL eCommerce、Amazon Shipping、GLS US、Veho、GOFO、Better Trucks、OSM Worldwide、Asendia、Landmark Global、Passport、APC Postal Logistics 等官网说明。
