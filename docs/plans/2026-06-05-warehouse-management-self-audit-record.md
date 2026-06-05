# 仓库管理自查修正记录

## 背景

用户指出“主仓的数据已经有了，为什么没接”。本次按“现有能力是否已复用、用户是否能在新菜单里直接看到现有事实源”的标准自查仓库管理实现。

## 自查结论

### 问题 1：官方仓菜单没有显性展示现有主仓仓库同步清单

- 现有事实源：
  - `upstream_system_connection`：主仓接入。
  - `upstream_system_warehouse_candidate`：上游系统已同步的仓库清单。
  - `upstream_system_warehouse_pairing`：上游仓库与系统仓库配对关系。
- 原实现问题：
  - 后端同步逻辑虽然已经通过 `IUpstreamSystemService` 读取同步清单并写入配对，但前端官方仓菜单没有清楚表达官方仓接的是现有主仓仓库同步清单。
  - 官方仓列表只展示上游仓库编码/名称，没有展示主仓接入名称，用户无法直观看出这条官方仓接的是哪个主仓。
- 修正：
  - 官方仓列表联查 `upstream_system_connection.master_warehouse_name` 并展示“主仓接入 + 上游仓库”。
  - 官方同步弹窗选择主仓后，上游仓库下拉直接来自该主仓的上游仓库同步清单。
  - 已配对仓库仍禁止再次选择和同步。

### 问题 2：第三方仓卖家校验缺少不存在兜底

- 原实现问题：
  - 第三方仓绑定卖家时，如果传入不存在的 `sellerId`，后端可能进入空指针。
- 修正：
  - 增加“归属卖家不存在”的明确校验。

### 问题 3：商品发布页仍有静态发货仓库选项

- 现状：
  - `react-ui/src/pages/Product/Distribution/EditPage.tsx` 仍存在硬编码 `warehouseOptions`。
- 判断：
  - 这属于同类“已有仓库主数据但页面未接”的问题。
  - 当前该文件已有大量其它未提交改动，本次没有直接混入修改，避免覆盖非本任务变更。
- 建议：
  - 下一步补仓库 options 接口，然后把商品发布页发货仓库选择替换为真实 `warehouse` 正常仓库选项。

## 本次修改文件

- `RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/domain/Warehouse.java`
- `RuoYi-Vue/warehouse/src/main/java/com/ruoyi/warehouse/service/impl/WarehouseServiceImpl.java`
- `RuoYi-Vue/warehouse/src/main/resources/mapper/warehouse/WarehouseMapper.xml`
- `react-ui/src/pages/Warehouse/WarehouseManagementPage.tsx`
- `react-ui/src/pages/Warehouse/components/OfficialSyncModal.tsx`
- `react-ui/src/types/warehouse/warehouse.d.ts`

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

## 验证结果

- `mvn -pl warehouse -am -DskipTests compile` 通过。
- `mvn -pl ruoyi-admin -am -DskipTests compile` 通过。
- `npm run tsc` 通过。
- `npm run build` 通过。
- `codegraph sync .` 通过。

## 未验证原因

- 尚未执行仓库 SQL seed。
- 尚未重启后端和前端进行浏览器核验。
