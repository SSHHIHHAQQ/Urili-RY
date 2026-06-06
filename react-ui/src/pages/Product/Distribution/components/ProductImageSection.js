import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import ImageUploadField from './ImageUploadField';
import styles from '../style.module.css';
function updateAt(values, index, value) {
    const next = [...values];
    next[index] = value || '';
    return next;
}
export default function ProductImageSection({ mainImageUrl, galleryUrls, onMainImageChange, onGalleryChange, }) {
    const normalizedGalleryUrls = Array.from({ length: 7 }, (_, index) => galleryUrls[index] || '');
    return (_jsxs("div", { className: styles.imageSection, children: [_jsx("div", { className: styles.imageSectionHeader, children: _jsxs("div", { children: [_jsx("div", { className: styles.sectionTitle, children: "\u5546\u54C1\u56FE\u7247" }), _jsx("div", { className: styles.sectionHint, children: "\u5EFA\u8BAE\u4E3B\u56FE\u6E05\u6670\u5C55\u793A\u5546\u54C1\u4E3B\u4F53\uFF1B\u8F6E\u64AD\u56FE\u7528\u4E8E\u5217\u8868\u548C\u8BE6\u60C5\u8865\u5145\u5C55\u793A\u3002" })] }) }), _jsxs("div", { className: styles.imageGrid, children: [_jsx(ImageUploadField, { label: "\u4E3B\u56FE", required: true, value: mainImageUrl, onChange: onMainImageChange }), normalizedGalleryUrls.map((url, index) => (_jsx(ImageUploadField, { label: index === 0 ? '尺寸图' : undefined, reserveLabelSpace: true, value: url, onChange: (value) => onGalleryChange(updateAt(normalizedGalleryUrls, index, value)) }, `gallery-${index}`)))] })] }));
}
