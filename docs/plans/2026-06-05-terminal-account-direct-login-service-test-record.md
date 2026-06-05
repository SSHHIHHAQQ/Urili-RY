# 2026-06-05 端账号级免密服务测试守卫执行记录

## 目标

本轮继续以 `docs/plans/2026-06-04-three-terminal-isolation-control-plan.md` 为开发方向，按“每个切片只改一类东西”的节奏，只补一类自动化守卫：管理端生成卖家/买家账号级免密票据时，必须绑定当前主体下的端内账号，不能跨主体生成票据。

## 范围

- 本轮只补 seller/buyer service 单元测试。
- 本轮不改接口行为、不改前端、不执行 SQL、不改远程数据库。
- 本轮不引入 Mockito；沿用现有测试风格，用动态代理 stub Mapper，用 recording `PortalDirectLoginSupport` 记录入参。
- `seller/pom.xml` 和 `buyer/pom.xml` 新增 JUnit test 依赖。当前工作区中这两个 POM 的 `product` 依赖为既有改动，本轮没有回退。

## 已完成

- 新增 `RuoYi-Vue/seller/src/test/java/com/ruoyi/seller/service/impl/SellerServiceImplTest.java`。
- 新增 `RuoYi-Vue/buyer/src/test/java/com/ruoyi/buyer/service/impl/BuyerServiceImplTest.java`。
- 卖家测试覆盖：
  - 成功生成账号级免密时，`PortalDirectLoginSupport.createToken(...)` 入参 terminal 为 `seller`，partner 为当前卖家，account 为传入的 `seller_account`。
  - 账号属于其他卖家时抛出 `卖家账号不存在`，不会生成票据。
  - 卖家主体停用时抛出 `卖家已停用，不能免密登录`，不会生成票据。
- 买家测试覆盖：
  - 成功生成账号级免密时，`PortalDirectLoginSupport.createToken(...)` 入参 terminal 为 `buyer`，partner 为当前买家，account 为传入的 `buyer_account`。
  - 账号属于其他买家时抛出 `买家账号不存在`，不会生成票据。
  - 买家主体停用时抛出 `买家已停用，不能免密登录`，不会生成票据。

## 验证结果

- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller -Dtest=SellerServiceImplTest test`：通过，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl buyer -Dtest=BuyerServiceImplTest test`：通过，`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。
- `cd E:\Urili-Ruoyi\RuoYi-Vue; mvn -pl seller,buyer -am test`：通过。
  - `ruoyi-system`：`Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`。
  - `finance`：`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。
  - `seller`：`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。
  - `buyer`：`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`。

## 大文件判断

- `SellerServiceImpl.java` / `BuyerServiceImpl.java` 均已超过 500 行，前两轮已有记录提示后续可拆 account/direct-login service。
- 本轮只补测试，不继续扩大 service 职责。
- 新增测试文件职责单一，分别守住 seller/buyer 账号级免密 service 行为；暂不抽公共测试基类，避免跨模块测试耦合。

## 当前判断

- 账号级免密代入已有 service 层测试守卫。
- 当前守卫不替代接口鉴权、菜单权限和浏览器烟测；它只证明 service 不会对跨主体端内账号生成免密票据。
- 后续继续扩展账号、角色、菜单、部门、日志或会话时，应继续按 seller 先验收、buyer 模板化复制的节奏补对应测试。
