# 腾讯云 COS 文件存储配置执行记录

## 目标

- 在现有 `FileStorageService` 抽象下新增腾讯云 COS 存储实现。
- 本地运行可通过环境变量切换到 COS，不把 SecretId、SecretKey 写入仓库。
- 保持 `/common/upload`、`/common/uploads`、`/system/user/profile/avatar` 的接口响应结构不变。

## 配置来源

- SecretId、SecretKey：用户在当前对话提供。为避免在补丁或命令中二次暴露明文，本次没有自动写入 `.env.local`，需要通过本机安全方式补充；本文档不记录明文。
- Bucket：`fenxiao-1306508427`
- Region：`ap-guangzhou`
- Endpoint：`https://fenxiao-1306508427.cos.ap-guangzhou.myqcloud.com`

## 已修改内容

- 新增 `CosFileStorageService`，负责上传、删除和生成 COS 访问地址。
- 新增 `CosFileResourceController`，在 COS 模式下将稳定的 `/profile/**` 路径重定向到 COS 地址，避免头像和历史调用方直接依赖对象存储路径。
- 新增 `CosFileStorageProperties`，集中读取 `ruoyi.file-storage.cos.*` 配置。
- `application.yml` 新增 COS 配置占位符，默认仍为 `local`。
- `.env.example` 新增 COS 环境变量示例，真实密钥不提交。
- `reuse-ledger.md` 记录文件存储复用规则。

## 安全判断

- SecretId、SecretKey 不写入 Git 跟踪文件。
- 默认 `TENCENT_COS_PUBLIC_READ=false`，后端生成临时访问地址或重定向地址。
- COS 对象 key 默认位于 `profile/` 前缀下；业务持久化路径继续使用 `/profile/...`。
- 本次不执行数据库 DDL/DML，不调整远端数据库、Redis、字典、菜单或权限数据。

## 验证结果

- 已运行 `mvn -pl ruoyi-framework -am -DskipTests compile`，结果通过。
- 已运行 `mvn -pl ruoyi-admin -am -DskipTests compile`，结果通过。
- 如需端到端验证，重启后端后通过 `/common/upload` 上传测试文件，再检查返回的 `fileName` 和 `url`。

## 未验证原因

- 未执行真实 `/common/upload` 上传验证：真实 SecretId、SecretKey 尚未写入本机运行环境。
