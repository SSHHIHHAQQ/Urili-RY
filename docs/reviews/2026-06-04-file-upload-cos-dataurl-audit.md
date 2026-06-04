# 文件上传与 COS 存储整改审计

日期：2026-06-04

## 结论

当前 COS 文件存储能力已经接好，`/common/upload` 和头像上传会走 `FileStorageService`，在 COS 模式下会上传到腾讯 COS。

真正的问题是卖家/买家主体附件仍然没有走 `/common/upload`。前端会把文件读成 data URL，再作为 `attachment.fileUrl` 提交；后端只校验附件地址非空，然后写入 seller/buyer 表的 `attachment_file_url`。这会继续把 base64 长文本写进业务表，和刚接好的 COS 存储目标冲突。

本次还做了只读运行库核查：当前运行库卖家列表 3 条，其中 1 条有附件，且该附件为 data URL/超长内容；买家列表 1 条，暂无附件。没有输出任何附件内容或连接明文。

## 2026-06-04 执行更新

已完成整改：

- 卖家/买家新增或替换附件改为保存前调用 `/common/upload`。
- 业务表保存 `/profile/...` 资源路径，不再新增 data URL。
- 后端 seller/buyer 保存前校验附件地址必须为 `/profile/...`，旧 data URL 仅允许原样保留。
- seller/buyer 附件保存增加业务扩展名白名单。
- 通用上传白名单补充 `csv`。
- 运行库中发现的 1 条 seller data URL 附件已迁移为 COS 文件路径。
- 迁移后只读核查结果：seller/buyer 附件 data URL 数量均为 0。

仍建议后续处理：

- `/profile/**` 当前匿名可访问，敏感业务附件建议增加模块级受控下载接口。
- `attachment_file_url` 仍是 `longtext`，存量迁移稳定后可另案确认是否收窄为 `varchar(512)`。

## 检查范围

- 前端上传、预览、data URL 相关代码：
  - `react-ui/src/components/PartnerManagement/PartnerManagementPage.tsx`
  - `react-ui/src/types/seller-buyer/party.d.ts`
  - `react-ui/src/pages/User/Center/components/AvatarCropper/index.tsx`
  - `react-ui/src/components/IconSelector/IconPicSearcher.tsx`
  - `react-ui/src/pages/User/Login/index.tsx`
- 后端上传和文件服务：
  - `RuoYi-Vue/ruoyi-admin/src/main/java/com/ruoyi/web/controller/common/CommonController.java`
  - `RuoYi-Vue/ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysProfileController.java`
  - `RuoYi-Vue/ruoyi-framework/src/main/java/com/ruoyi/framework/file/*`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/domain/PartnerProfile.java`
  - `RuoYi-Vue/ruoyi-system/src/main/java/com/ruoyi/system/service/support/PartnerSupport.java`
- 卖家/买家 SQL 和 Mapper：
  - `RuoYi-Vue/sql/seller_buyer_management_seed.sql`
  - `RuoYi-Vue/seller/src/main/resources/mapper/seller/SellerMapper.xml`
  - `RuoYi-Vue/buyer/src/main/resources/mapper/buyer/BuyerMapper.xml`

## 运行库只读核查

数据来源：通过本机后端 `http://127.0.0.1:8080` 登录后调用列表接口，只统计附件 URL 类型，不输出明细。

| 模块 | 列表总数 | 有附件 | data URL | `/profile/` 路径 | HTTP URL | 其他 | 超 2000 字符 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| seller | 3 | 1 | 1 | 0 | 0 | 0 | 1 |
| buyer | 1 | 0 | 0 | 0 | 0 | 0 | 0 |

判断：

- 当前库已经存在至少 1 条 seller 附件 data URL 存量数据。
- 现在如果继续使用管理端上传附件，还会继续产生新的 data URL 入库。

## 问题清单

### P0：卖家/买家附件仍然 data URL 入库

证据：

- `PartnerManagementPage.tsx` 中 `readFileAsDataUrl` 使用 `FileReader.readAsDataURL`。
- `uploadFileToAttachment` 将读取结果写成 `fileUrl: dataUrl`。
- `Upload` 配置 `beforeUpload={() => false}`，没有调用 `/common/upload`。
- 保存 payload 中 `attachment` 会进入后端 `PartnerProfile.setAttachment`，最终落到 `attachment_file_url`。

影响：

- 文件内容被放大为 base64 文本写进业务表。
- 列表、详情接口会把大段文件内容返回给前端。
- 数据库体积、慢查询、网络传输和浏览器内存都会被放大。
- 附件无法统一走 COS 生命周期、权限、签名 URL、删除和迁移策略。

建议：

- 立即改为前端选择文件后调用 `/api/common/upload`。
- 保存到业务表的 `attachment.fileUrl` 只允许使用后端返回的 `fileName`，也就是 `/profile/...` 资源路径。
- `fileName` 展示名使用后端返回的 `originalFilename`。
- 新增文件时不再调用 `readAsDataURL`。
- 保留旧 data URL 只读兼容，但不允许新写入。

### P0：后端没有限制附件地址来源

证据：

- `PartnerSupport.normalizeAttachment` 只校验附件名称、类型和地址非空。
- `PartnerProfile.Attachment` 提供 `setDataUrl`，会直接把 data URL 写入 `fileUrl`。

影响：

- 即使前端改好了，绕过前端直接请求接口仍可提交 data URL。
- 也可能提交任意外部 URL，导致业务表保存不可控外链。
- 未来如果页面直接渲染这些 URL，会引入隐私、访问失效和潜在 XSS/钓鱼风险。

建议：

- 后端保存 seller/buyer 附件前校验 `fileUrl` 必须以 `/profile/` 开头。
- 明确拒绝：
  - `data:`
  - `blob:`
  - `javascript:`
  - 任意 `http://` / `https://` 外链
- `setDataUrl` 只作为历史兼容读取的过渡能力，不能用于新增或编辑保存。
- 后续完成迁移后删除 `dataUrl` 类型和 setter。

### P1：附件字段使用 `longtext` 会继续纵容大文本入库

证据：

- `seller_buyer_management_seed.sql` 中 seller 和 buyer 的 `attachment_file_url` 都是 `longtext`。

影响：

- 字段类型和“只保存资源路径”的目标不一致。
- 即使业务代码改了，数据库层仍允许超长 data URL 继续写入。

建议：

- 第一阶段不急着改表，先用代码阻断新 data URL。
- 存量迁移完成后，再提交表变更方案，把 `attachment_file_url` 收窄为 `varchar(512)` 或确认后的资源标识字段。
- 表变更属于 DDL，必须另写 Markdown 设计/执行记录并确认。

### P1：`/profile/**` 当前匿名可访问，业务附件可能敏感

证据：

- `SecurityConfig` 对 GET `/profile/**` 配置了 `permitAll`。
- COS 模式下 `CosFileResourceController` 会把 `/profile/**` 重定向到 COS URL。

影响：

- COS 私有桶会生成短时签名 URL，但 `/profile/...` 资源路径本身不需要登录认证。
- 如果附件是营业执照、合同、身份证明、财务凭证等，知道路径的人可以访问。

建议：

- 头像、公开图片可以继续走公开 `/profile/**`。
- 卖家/买家附件应改为受控下载接口，例如 `/seller/admin/sellers/{id}/attachment`、`/buyer/admin/buyers/{id}/attachment`。
- 下载接口先校验登录、权限和主体归属，再由后端返回文件流或短时签名 URL。
- 业务表仍可保存 `/profile/...`，但前端展示/下载不要直接暴露给无权限用户。

### P1：通用下载接口仍偏本地文件系统

证据：

- `CommonController.resourceDownload` 使用 `RuoYiConfig.getProfile()` 拼本地路径读取文件。

影响：

- 当前前端没有命中 `/common/download/resource`，直接访问 `/profile/...` 在 COS 模式可重定向。
- 但如果后续业务附件复用 `/common/download/resource`，COS 模式下会找本地文件而失败。

建议：

- 短期避免业务附件使用 `/common/download/resource`。
- 中期给 `FileStorageService` 增加统一 `openStream` / `resolveDownload` 能力，再让通用下载接口适配 local/COS。
- 业务敏感附件优先走模块级受控下载接口，而不是无权限通用下载。

### P1：前后端允许文件类型不一致

证据：

- 前端卖家/买家附件 `accept` 包含 `.csv`。
- 后端 `MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION` 当前不包含 `csv`。
- 后端通用上传还允许 `rar`、`zip`、`mp4`、`avi`、`rmvb`、`html`、`htm` 等类型。

影响：

- 前端允许选择 CSV，但改为 `/common/upload` 后后端会拒绝。
- 对卖家/买家资质附件来说，压缩包、视频、HTML 可能不是必要类型，扩大了上传面。

建议：

- 为业务附件定义单独白名单，不直接用通用默认白名单。
- 建议第一版允许：
  - `jpg`
  - `jpeg`
  - `png`
  - `pdf`
  - `doc`
  - `docx`
  - `xls`
  - `xlsx`
  - `csv`
  - `txt`
- 是否允许压缩包、HTML、视频，需要单独确认。

### P1：大小限制和用户提示未统一

证据：

- Spring multipart 配置单文件 10MB、请求 20MB。
- `FileUploadUtils.DEFAULT_MAX_SIZE` 是 50MB。
- 前端附件控件没有明确大小限制。

影响：

- 实际限制以 Spring multipart 先拦截为准，用户可能拿到不友好的错误。
- 代码层配置不一致，后续调整容易误判。

建议：

- 卖家/买家附件第一版按 10MB 单文件限制。
- 前端 `beforeUpload` 或上传前校验大小，提前提示。
- 后端业务附件上传也使用同一限制。
- 如果未来需要 20MB/50MB，统一改配置和文档。

### P2：头像使用 data URL 仅作裁剪预览，最终上传已走文件服务

证据：

- `AvatarCropper` 选择图片后用 `FileReader.readAsDataURL` 做裁剪预览。
- 点击确认后将裁剪后的 `Blob` 通过 `uploadAvatar` 提交。
- 后端 `SysProfileController.avatar` 使用 `fileStorageService.uploadAvatar`。

判断：

- 这不是“data URL 入库”问题。
- 头像最终会进入 COS 或 local，取决于当前 `RUOYI_FILE_STORAGE_TYPE`。

建议：

- 可以保留前端 data URL 预览。
- 只需确认头像返回和展示统一使用 `/profile/avatar/...`。

### P2：图标图片搜索使用 data URL 仅作本地识别

证据：

- `IconPicSearcher` 用 `FileReader.readAsDataURL` 后传给前端模型识别图标。
- 没有后端保存路径，没有业务表写入。

判断：

- 这是纯前端临时预览/识别，不属于文件持久化问题。

建议：

- 暂不整改。
- 后续如果图标搜索结果要保存图片，再接入统一文件服务。

### P2：验证码和 CSS data URI 不是上传存储问题

证据：

- 登录页验证码拼接 `data:image/png;base64,...` 是后端验证码展示。
- cropper CSS 使用静态 data URI 背景图。

判断：

- 这两个都不是用户上传文件持久化。
- 不需要改到 COS。

## 建议整改方案

### 第一步：阻断新 data URL 入库

前端：

- 新增统一上传服务，例如 `react-ui/src/services/common/file.ts`。
- 封装 `uploadCommonFile(file: File)`，POST 到 `/api/common/upload`。
- 在 `PartnerManagementPage` 的附件 `Upload` 中：
  - 上传时立即调用 `/common/upload`。
  - 成功后把 `UploadFile.response` 设置为标准 `PartyAttachment`。
  - `fileUrl` 使用后端返回的 `/profile/...` 路径。
  - `fileName` 使用 `originalFilename`。
  - `sizeBytes` 使用本地文件 size。
  - `mimeType` 使用本地文件 type 或后端返回类型。
- 删除新增路径中的 `readFileAsDataUrl` 调用。
- `dataUrl` fallback 只保留为历史兼容展示。

后端：

- 在 `PartnerSupport.normalizeAttachment` 增加路径校验：
  - 新增/编辑时只允许 `/profile/`。
  - 拒绝 `data:`、`blob:`、外链和空白路径。
- 视实现方式增加一个明确方法，例如 `validateManagedAttachmentPath`。
- 保持历史数据读取不崩，但不允许再次保存 data URL。

验收：

- 新增卖家上传附件后，接口 payload 中 `attachment.fileUrl` 是 `/profile/upload/...`。
- 数据库新增/更新行不再出现 `data:`。
- COS 桶中能看到对应对象。

### 第二步：处理存量 data URL

当前运行库至少有 1 条 seller 附件 data URL。

建议：

- 先保留兼容展示，避免老数据突然不可访问。
- 另写迁移方案：
  - 查询 seller/buyer 中 `attachment_file_url like 'data:%'` 的记录数。
  - 将 data URL 解析为文件内容。
  - 通过后端文件服务或迁移脚本上传到 COS。
  - 回填 `/profile/...` 路径。
  - 记录成功、失败、跳过原因。
- 迁移前必须备份，迁移后抽样访问验证。

### 第三步：收口业务附件访问权限

建议：

- 头像等公开资源继续走 `/profile/**`。
- 卖家/买家附件增加受控下载接口。
- 前端列表/详情里不直接把敏感附件 URL 暴露为匿名可访问链接。
- 后端根据 `seller:admin:query` / `buyer:admin:query` 或更细权限控制下载。

### 第四步：统一文件类型和大小策略

建议：

- 为业务附件增加单独配置或常量：
  - 允许扩展名
  - 最大大小
  - 是否允许压缩包
  - 是否允许 HTML
- 前后端共享同一文档规则。
- 更新 `docs/architecture/reuse-ledger.md`，登记业务附件上传规则。

## 推荐执行顺序

1. 改前端卖家/买家附件上传，接 `/common/upload`。
2. 改后端 `PartnerSupport`，拒绝新 data URL 和外链。
3. 运行前后端验证，并真实上传一个卖家/买家附件到 COS。
4. 做只读统计，确认新数据不再出现 data URL。
5. 单独写存量 data URL 迁移方案。
6. 再决定是否做受控下载接口和字段类型收窄。

## 验证命令建议

后端编译：

```powershell
cd E:\Urili-Ruoyi\RuoYi-Vue
mvn -DskipTests install
```

前端类型检查：

```powershell
cd E:\Urili-Ruoyi\react-ui
npm run tsc
```

浏览器验证：

- 登录管理端。
- 新增或编辑卖家附件。
- 新增或编辑买家附件。
- 确认保存后详情能展示附件。
- 确认网络请求中保存 payload 的 `attachment.fileUrl` 不是 data URL。
- 确认后端返回路径为 `/profile/...`。

只读运行库验证：

- 统计 `seller.attachment_file_url` 和 `buyer.attachment_file_url` 是否仍有 `data:%`。
- 统计超长附件地址数量。
- 不输出附件内容。

## 已落地与剩余事项

已落地：

- 已修改 Java/React 代码。
- 已通过业务 API 迁移存量 data URL。
- 已运行 Maven 编译和前端类型检查。
- 已真实验证 `/common/upload` 返回 COS 访问 URL。

未做：

- 未执行数据库 DDL 或直接 SQL。
- 未修改 COS 权限模型。
- 未实现模块级受控下载接口。
- 未收窄 `attachment_file_url` 字段类型。

本文件已更新为审计、整改建议和执行结果记录。
