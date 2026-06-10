# 图片资源存储排查与修复执行记录

日期：2026-06-10

## 目标

排查当前代码和运行库中是否仍存在图片以 base64 / data URL 形式落库的问题，并将商品、主体附件、审核快照、操作日志等会写入图片相关字段的链路统一改为“上传文件服务后保存资源路径”。

## 数据源与执行边界

- 连接来源：本机 `.env.local` 中的当前后端运行配置。
- 目标库：`fenxiao`。
- Redis：本次未读写 Redis。
- SQL 类型：
  - 只读扫描：商品图片字段、审核快照字段、主体附件字段、管理端/端内操作日志字段。
  - 数据修正：仅对 `sys_oper_log.oper_param` 中命中的 2 条历史日志做 inline base64 脱敏更新。
- 未执行业务表 DML：商品、SKU、商品图片、审核快照、卖家、买家表均未做数据更新。

## 排查结论

当前商品业务表没有发现图片 base64 落库：

| 表字段 | 非空数 | base64 命中 |
| --- | ---: | ---: |
| `product_spu.main_image_url` | 46 | 0 |
| `product_spu.detail_content` | 40 | 0 |
| `product_sku.sku_image_url` | 133 | 0 |
| `product_image.image_url` | 330 | 0 |
| `product_review_request.main_image_url_before` | 22 | 0 |
| `product_review_request.main_image_url_after` | 34 | 0 |
| `product_review_snapshot.payload_json` | 203 | 0 |
| `seller.attachment_file_url` | 1 | 0 |
| `buyer.attachment_file_url` | 0 | 0 |
| `sys_oper_log.oper_param` | 1363 | 0 |
| `seller_oper_log.oper_param` | 244 | 0 |
| `buyer_oper_log.oper_param` | 172 | 0 |

本次发现的实际污染点是历史操作日志：`sys_oper_log.oper_param` 曾有 2 条请求参数包含 `data:*;base64,...`。已将这 2 条日志中的内联 base64 替换为 `[inline image base64 redacted]`，复查后 `sys_oper_log.oper_param` 命中数为 0。

## 代码修复

1. 新增统一图片资源工具：
   - `RuoYi-Vue/ruoyi-common/src/main/java/com/ruoyi/common/utils/file/ImageResourceUtils.java`
   - 能力：
     - 拒绝 `data:image/...;base64,...`、`;base64,`、常见原始图片 base64 前缀。
     - 将 `/api/profile/...`、带域名的 `/profile/...?...` 规范为稳定 `/profile/...`。
     - 清洗日志文本中的 inline base64，避免操作日志继续落库大字段。

2. 商品链路：
   - `ProductDistributionServiceImpl`
   - 保存 SPU 主图、详情图文图片、SKU 图、商品图库时统一校验和规范化资源路径。

3. 审核链路：
   - `ProductReviewServiceImpl`
   - 新增商品审核、商品资料变更审核、供货价变更审核创建快照前校验图片字段，避免旧污染数据进入审核快照。

4. 来源商品链路：
   - `LingxingSkuSyncItemMapper`
   - 来源 SKU 图片如果是 inline base64，直接丢弃为空；`/profile/...` 类路径会被规范化。

5. 操作日志链路：
   - `LogAspect`
   - `PortalLogAspect`
   - 管理端和端内操作日志入库前统一脱敏 `data:*;base64,...`。

6. 主体附件链路：
   - `PartnerSupport`
   - `PartnerProfile.Attachment`
   - `PartnerManagementPage`
   - 移除旧 `dataUrl` 兼容提交入口，附件保存只接受文件服务返回的 `/profile/...`。

7. 构建修正：
   - `RuoYi-Vue/pom.xml`
   - 当前工作区已有 `logistics` 模块被纳入 reactor，补齐 dependencyManagement 版本，避免相关模块构建因缺失版本失败。

## 剩余扫描项说明

静态扫描仍能看到以下 base64 / data URL 使用，但不属于数据库持久化路径：

- 登录验证码：后端返回验证码 base64，前端只用于登录页展示。
- 头像裁剪：前端本地预览使用 data URL，最终提交 `Blob/FormData` 到头像上传接口，后端保存 `/profile/...`。
- 图标搜索器：前端本地图片识别预览，不提交后端。
- Cropper CSS 内联小背景图：静态样式资源，不入库。
- 单元测试中的 data URL：用于验证拒绝和脱敏逻辑。

## 验证

- `mvn -pl product -am "-Dtest=ImageResourceUtilsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，5 项测试。
- `mvn -pl ruoyi-framework -am "-Dtest=LogAspectSensitiveFieldFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，7 项测试。
- `mvn -pl ruoyi-system -am "-Dtest=PartnerSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过，12 项测试。
- `npx jest --config jest.config.ts tests/partner-management-contract.test.ts --runInBand`：通过，7 项测试。
- `mvn -pl ruoyi-framework,ruoyi-system,product,integration -am -DskipTests package`：通过。
- `git diff --check`：通过，仅有工作区既有 CRLF 提示。
