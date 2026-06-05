# 仓库管理实现记录

## 当前目标

落地“仓库管理 / 官方仓库”和“仓库管理 / 第三方仓库”首版，实现仓库主数据、第三方仓归属卖家、官方仓同步上游主仓仓库并自动配对、美国州/城市完整字典和管理端页面。

## 已完成事项

- 新增后端 Maven 模块 `warehouse`，并接入根 `pom.xml` 与 `ruoyi-admin/pom.xml`。
- 新增后端仓库表设计对应的 Domain、Mapper、Service、Controller。
- 新增 SQL：
  - `RuoYi-Vue/sql/warehouse_management_seed.sql`
  - `RuoYi-Vue/sql/warehouse_us_address_seed.sql`
- `warehouse_us_address_seed.sql` 使用 U.S. Census Bureau 2025 Gazetteer National Places 文件生成：
  - 51 个州级选项，含 District of Columbia。
  - 32058 条美国 city/place 候选。
  - 保存 `place_geoid`、`city_name`、`place_name`、`place_type`，地址字段仍保存城市基础名称。
- 新增 React 管理端页面：
  - `react-ui/src/pages/Warehouse/Official/index.tsx`
  - `react-ui/src/pages/Warehouse/ThirdParty/index.tsx`
  - 共享页面和表单组件位于 `react-ui/src/pages/Warehouse`。
- 官方仓菜单已显性接入现有主仓仓库数据：
  - 官方仓列表展示主仓接入名称和上游仓库信息。
  - 官方同步弹窗选择主仓后，上游仓库下拉直接读取该主仓的仓库同步清单。
  - 已配对的上游仓库在下拉中禁用选择。
- 新增前端 service 和类型：
  - `react-ui/src/services/warehouse/warehouse.ts`
  - `react-ui/src/types/warehouse/warehouse.d.ts`
- 更新复用台账，登记仓库主数据、美国州/城市字典、官方仓上游配对复用规则。

## 关键业务规则

- 仓库编码全局唯一。
- 仓库状态使用若依 `0=正常`、`1=停用`。
- 仓库表不新增 `source` 字段。
- 官方仓同步时：
  - 必须选择已启用的上游主仓接入。
  - 上游仓库必须存在于同步清单且状态为 `ACTIVE`。
  - 已配对上游仓库禁止再次同步。
  - 创建官方仓和写入 `upstream_system_warehouse_pairing` 在同一事务内完成。
- 第三方仓只允许绑定正常状态卖家。
- 结算币种必须来自启用的 `finance_currency`。
- 国家选择 `US` 时启用美国州/城市字典联动；其他国家不启用字典约束，按普通文本保存。
- 不做物理删除，不做导出。

## 权限检查结果

- 官方仓库：
  - `warehouse:official:list`
  - `warehouse:official:add`
  - `warehouse:official:edit`
  - `warehouse:official:status`
  - `warehouse:official:sync`
- 第三方仓库：
  - `warehouse:thirdParty:list`
  - `warehouse:thirdParty:add`
  - `warehouse:thirdParty:edit`
  - `warehouse:thirdParty:status`
- 前端按钮和后端接口都按权限点控制。

## 字典/选项复用检查结果

- 国家/地区复用 `country_region`。
- 仓库状态复用若依正常/停用口径。
- 结算币种复用 `finance_currency` 启用币种。
- 第三方仓卖家选项由仓库后端接口返回正常状态卖家。
- 美国完整州/城市不放入 `sys_dict_data`，使用专用表。

## 复用台账检查结果

- 已追加 `docs/architecture/reuse-ledger.md` 的仓库模块条目。

## 大文件合理性判断结果

- `warehouse_us_address_seed.sql` 是完整美国城市 seed，体积较大属于数据初始化文件，合理。
- React 页面按共享组件拆分：
  - `WarehouseManagementPage.tsx`
  - `WarehouseFields.tsx`
  - `WarehouseFormModal.tsx`
  - `OfficialSyncModal.tsx`
- 后端 Service 承担 CRUD、同步和选项查询，当前仍在可维护范围；若后续加入库存、费用、履约，应拆分仓库主数据服务和同步服务。

## 重复代码检查结果

- 官方仓和第三方仓复用同一套前端列表/表单组件。
- 官方仓同步复用 integration 的正式配对服务，不重复写配对表逻辑。
- 官方仓同步清单复用 `upstream_system_warehouse_candidate` 和 `upstream_system_warehouse_pairing`，不重新维护一份上游仓库同步数据。
- 国家/美国州城市联动在 `WarehouseFields` 中集中维护。

## 自查修正结果

- 已补 `docs/plans/2026-06-05-warehouse-management-self-audit-record.md`。
- 本次自查确认最初实现存在一个体验和集成层面的遗漏：官方仓菜单没有直接把现有主仓仓库同步清单展示出来。
- 已修正：
  - `WarehouseMapper.xml` 联查 `upstream_system_connection.master_warehouse_name`。
  - `WarehouseManagementPage.tsx` 在“上游配对”列展示主仓接入和上游仓库。
  - `OfficialSyncModal.tsx` 的上游仓库下拉直接读取现有上游仓库同步清单，并禁用已配对项。
  - `WarehouseServiceImpl` 补第三方仓卖家不存在兜底。
- 仍需后续处理：
  - 商品发布页发货仓库仍是静态选项，应在下一小步接入真实 `warehouse` 正常仓库 options。

## 验证命令

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -pl warehouse -am -DskipTests compile
mvn -pl ruoyi-admin -am -DskipTests compile

cd E:\Urili-Ruoyi\react-ui
npm run tsc
npm run build

cd E:\Urili-Ruoyi
codegraph sync .
```

## 已验证事项

- `mvn -pl warehouse -am -DskipTests compile` 通过。
- `mvn -pl ruoyi-admin -am -DskipTests compile` 通过。
- `npm run tsc` 通过。
- `npm run build` 通过。
- `codegraph sync .` 通过。

## 未验证原因

- 尚未执行数据库 SQL；本次实现未直接连接或修改运行库。
- 尚未重启后端和前端浏览器核验，因为当前未执行数据库 seed，菜单和表尚未进入运行库。

## 残留问题

- 需要执行 `warehouse_management_seed.sql` 和 `warehouse_us_address_seed.sql` 后，才能在真实菜单和页面中验证完整链路。
- 美国城市 seed 采用 U.S. Census Places 口径，不等同于 USPS 全量邮政城市别名；后续如需要 USPS 地址库，应另行设计。
