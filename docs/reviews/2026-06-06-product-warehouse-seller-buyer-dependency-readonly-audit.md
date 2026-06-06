# 只读审计：product/warehouse/seller/buyer 依赖与映射检查（E:\Urili-Ruoyi）

日期：2026-06-06

范围：
- 读取 `RuoYi-Vue/pom.xml`
- 读取 `RuoYi-Vue/*/pom.xml`（`product`/`warehouse`/`seller`/`buyer`）
- 关注 `product`/`warehouse`/`seller`/`buyer` Java 接口依赖关系
- 关注 mapper XML 与 domain 的字段映射

## P0/P1 发现

| 文件/位置 | 等级 | 问题 | 证据 |
|---|---|---|---|
| `RuoYi-Vue/product/pom.xml:33-35`（`product -> warehouse`） + `RuoYi-Vue/seller/pom.xml:26-30`（`seller -> product`） | **P1** | 发现**隐含循环依赖风险**：`product` 与 `seller` 之间在 `dependency:tree` 中形成循环链路（`product -> warehouse -> seller -> product`）。这条 `seller` 依赖关系未在 `product/warehouse` 的声明中显式出现，但在编译依赖树里出现，需确认是否为污染/旧构建产物/间接依赖解析导致。 | `mvn -pl product -DskipTests dependency:tree -Dverbose` 输出包含 `com.ruoyi:seller:jar:3.9.2:compile` 在 `com.ruoyi:warehouse` 分支下，并继续到 `com.ruoyi:product:jar:3.9.2:compile`。

## 发现状态

- **P0：无明确 P0**（未见编译阻断、方法签名不匹配、编译级循环直接引用）。

## 已执行的检查命令与结果摘要

1. 依赖与构建
- `mvn -pl seller,buyer,product,warehouse -am -DskipTests compile`：BUILD SUCCESS
- `mvn -pl seller,buyer,product,warehouse -am -DskipTests test-compile`：BUILD SUCCESS
- `mvn -pl product -DskipTests dependency:tree`：产出依赖树中出现 `product -> warehouse -> seller -> product` 的间接链
- `mvn -pl warehouse -DskipTests dependency:tree`：单独 `warehouse` 分支不出现 `seller`
- `mvn -pl product -DskipTests dependency:list -DincludeArtifactIds=seller -DincludeTypes=jar`：返回 `com.ruoyi:seller:jar:3.9.2:compile`
- `mvn -pl product -DskipTests dependency:tree -Dincludes=...`：无直接输出，说明该路径非直接声明。

2. Mapper/domain 映射
- 自写字段对齐脚本（读取 `product/warehouse/seller/buyer` 的 `mapper/*.xml` 与 `*domain*/*.java`）
- 结果：`[PASS] product` / `[PASS] warehouse` / `[PASS] seller` / `[PASS] buyer`

## 说明

- 本次为只读审计，不改动任何代码。
- 建议优先核查这条 `product -> warehouse -> seller -> product` 关系是否来自本地 `.m2` 缓存旧版 `com.ruoyi:warehouse` 或构建依赖解析偏差；一旦核实为真实循环需立即打断为显式可控依赖方向。

## 主代理复核补记

- 复核时间：2026-06-06。
- 复核结论：上述 P1 不是当前源码 POM 形成的真实循环，而是本机 Maven 本地仓库中旧 `warehouse-3.9.2` 构件仍带有历史 seller 依赖导致的解析污染。
- 复核处理：已执行 `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl warehouse -am -DskipTests install`，重新安装当前 `warehouse` 及上游构件。
- 复核验证：随后执行 `mvn -pl product -DskipTests dependency:list -DincludeArtifactIds=seller -DincludeTypes=jar`，返回 `none`；执行 `mvn -pl product -DskipTests dependency:tree "-Dincludes=com.ruoyi:seller"`，`BUILD SUCCESS` 且无 seller 依赖输出。
- 当前状态：源码层 `product -> warehouse`、`seller -> product` 仍成立，但 `warehouse` 当前不依赖 seller；本轮未发现真实 Maven 闭环。
