# 商品图片坏图原因排查记录

## 背景

管理端「商品管理 / 商城商品列表」中，部分已经保存成功的商品图片显示为坏图。

## 排查范围

- 前端图片上传控件：`react-ui/src/pages/Product/Distribution/components/ImageUploadField.tsx`
- 商品列表图片渲染：`react-ui/src/pages/Product/Distribution/constants.ts`
- 通用上传接口：`RuoYi-Vue/ruoyi-admin/src/main/java/com/ruoyi/web/controller/common/CommonController.java`
- 文件存储实现：`RuoYi-Vue/ruoyi-framework/src/main/java/com/ruoyi/framework/file/`
- 当前远端运行库 `fenxiao` 中 `product_spu.main_image_url` 与 `product_sku.sku_image_url` 的只读统计

## 结论

坏图主要分为两类。

第一类是真实上传图片被保存成 COS 临时签名 URL。当前 COS 存储会返回稳定资源路径 `/profile/upload/...`，也会返回一个带签名的临时访问 URL。前端上传控件原先优先保存 `resp.url`，导致商品表保存了带 `sign` 的临时 URL。该 URL 过期后，商品列表、编辑页和预览页再次渲染时就会坏图。

第二类是 2026-06-05 生成的历史演示商品数据直接写入了 Unsplash 外链。其中至少有一条外链当前返回 404，例如 `SPU202606050015 / 全棉四件套床单套件` 的主图。该类不是上传链路问题，而是历史演示数据使用外部图片地址，外部地址失效或当前环境不可访问。

## 数据证据

只读统计显示：

```text
product_spu.main_image_url:
- LOCAL_DEMO_STATIC: 7
- PROFILE_ABSOLUTE: 1
- UNSPLASH: 23

product_sku.sku_image_url:
- LOCAL_DEMO_STATIC: 35
- PROFILE_ABSOLUTE: 3
- UNSPLASH: 69
```

其中 `PROFILE_ABSOLUTE` 是已保存的 COS 签名 URL，包含 `q-sign-time` 等临时签名参数；`UNSPLASH` 是历史演示数据外链。

## 本次修复

1. 上传控件保存图片时，优先保存 `resp.fileName`，也就是稳定资源路径 `/profile/upload/...`，不再优先保存临时签名 URL。
2. 图片展示时，如果历史数据里已经保存了包含 `/profile/` 的绝对 URL，则转换为 `/api/profile/...`，交给后端重新生成当前有效的 COS 访问地址。

## 未直接修改的数据

本次没有直接修改远端数据库中的历史商品图片 URL。旧的 Unsplash 演示外链仍属于历史数据问题，需要后续按数据修复流程替换为本地样板图或正式上传图。
