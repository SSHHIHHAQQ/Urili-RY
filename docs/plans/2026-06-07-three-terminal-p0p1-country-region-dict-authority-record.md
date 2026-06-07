# 2026-06-07 三端 P0/P1：国家/地区字典权威校验

## 目标

参考 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md`，继续按三端独立方向推进 P0/P1。当前只处理编译、guard、接口、权限、串端、service/字段缺失，不做浏览器运行态验收、截图、DOM 检测或 UI 细调。

本轮聚焦 seller/buyer 主体资料的 `countryCode`。此前后端只做必填、转大写和 2 位长度校验，未把保存值绑定到若依 `country_region` 字典，伪造请求可写入不在字典范围内的 2 位代码。

## 子 Agent 执行情况

- 先按用户最新要求尝试 6 个 `gpt-5.3-codex-spark` 只读子 Agent；平台返回用量限制，失败 Agent 已关闭。
- 回退使用 `gpt-5.4`，共 6 个只读子 Agent 完成扫描，均已关闭。
- 采纳的 P1：
  - `country_region` 已由 `seller_buyer_management_seed.sql` seed，但 seller/buyer 主体保存服务未引用它。
  - `PartnerSupport.normalizeCommonProfile(...)` 只检查 2 位长度，未检查 `[A-Z]{2}` 形态和字典 membership。
  - `SellerServiceImplTest` / `BuyerServiceImplTest` 原先主要覆盖账号生命周期，缺少主体保存 fail-closed 用例。
- 未纳入本轮：
  - 前端 `country_region` fallback 策略是否改为硬失败，需要产品策略确认；本轮只收口后端服务层权威校验。
  - ProductDistributionMapper 跨 integration/source 表边界和库存聚合占位 guard 作为后续 P1。

## 已完成

- `PartnerSupport` 新增 `COUNTRY_REGION_DICT_TYPE` 常量和 `assertCountryRegionCode(...)`。
- `PartnerSupport.normalizeCommonProfile(...)` 从“长度等于 2”升级为 `[A-Z]{2}` 形态校验。
- `SellerServiceImpl` 注入若依 `ISysDictTypeService`，在 seller 主体 normalize 后调用 `selectDictDataByType("country_region")`，并 fail-closed 校验 `countryCode` 是否存在于启用字典项。
- `BuyerServiceImpl` 按 seller 模板机械复制，同步接入同一套 `country_region` 字典权威校验。
- `PartnerSupportTest` 补充国家/地区 code 接受、字典外拒绝、非字母拒绝和字典缺失 fail-closed 用例。
- `SellerServiceImplTest` / `BuyerServiceImplTest` 各补一个主体新增负例，固定未知国家/地区 code 必须在写 mapper 前失败。
- `SellerServiceImplTest` / `BuyerServiceImplTest` 的测试 service 构造默认注入 `CN/US` 字典替身，避免账号生命周期测试被未注入字典服务干扰。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl ruoyi-system,seller,buyer -am "-Dtest=PartnerSupportTest,SellerServiceImplTest,BuyerServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过；`PartnerSupportTest` 11 个、`SellerServiceImplTest` 51 个、`BuyerServiceImplTest` 51 个测试通过。
- `cd E:\Urili-Ruoyi\react-ui; npm run verify:three-terminal`：通过；前端 7 个 Jest suite / 33 个测试通过；后端 ruoyi-system 147、ruoyi-framework 15、integration 4、product 3、seller 92、buyer 93 个测试通过。
- `cd E:\Urili-Ruoyi; git diff --check`：通过；仅有 LF/CRLF 工作区换行提示，无 whitespace 错误。
- `cd E:\Urili-Ruoyi; codegraph sync .`：通过；同步 6 个变更文件，Modified 6，共 726 个节点。

## 边界说明

- 本轮未执行远程 MySQL DDL/DML，未读取或写入 Redis。
- 本轮未启动或重启后端。
- 本轮未做浏览器、截图、DOM 或 UI 细调验收。
- `country_region` 当前 seed 仍在 `seller_buyer_management_seed.sql` 综合 seed 内，未拆独立国家/地区增量脚本；本轮只接入服务层权威校验，不改变数据库结构。
- 前端主体管理页的字典请求失败 fallback 暂未调整，避免把产品策略问题混进后端 P1 收口。

## 残留 P1

- ProductDistributionMapper 仍直接读写来源商品、来源仓、`upstream_system_sku_pairing` 等 integration/source 表；下一步适合先补 mapper/XML debt allowlist contract，冻结当前跨边界方法和表集合，避免继续扩散。
- 商品库存聚合字段 `available_stock`、`warehouse_count`、`inventory_status` 和 `stock_update_time` 仍是显式占位；下一步适合先补合同，防止用 `master_sku` 或 source/integration 表快速拼接伪库存，真实接通必须等库存事实源和汇总规则设计。
