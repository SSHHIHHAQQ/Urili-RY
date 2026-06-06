import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Image } from 'antd';
import { parseDetailContent } from '../detailContent';
import { resolveResourceUrl } from '../constants';
import styles from '../style.module.css';
export default function DetailContentPreview({ value }) {
    const blocks = parseDetailContent(value);
    if (!blocks.length) {
        return _jsx("span", { className: styles.mutedText, children: "--" });
    }
    return (_jsx("div", { className: styles.detailPreview, children: blocks.map((block) => {
            if (block.type === 'TEXT') {
                return _jsx("p", { children: block.text || '--' }, block.id);
            }
            if (block.type === 'IMAGE') {
                return block.imageUrl ? (_jsx(Image, { width: 160, src: resolveResourceUrl(block.imageUrl) }, block.id)) : _jsx("div", { className: styles.mutedText, children: "\u56FE\u7247\u672A\u4E0A\u4F20" }, block.id);
            }
            if (block.type === 'IMAGE_TEXT') {
                return (_jsxs("div", { className: styles.detailPreviewImageText, children: [block.imageUrl ? _jsx(Image, { width: 120, src: resolveResourceUrl(block.imageUrl) }) : null, _jsxs("div", { children: [block.title ? _jsx("div", { className: styles.detailPreviewTitle, children: block.title }) : null, _jsx("p", { children: block.text || '--' })] })] }, block.id));
            }
            return (_jsx("div", { className: styles.detailPreviewParams, children: (block.rows || []).map((row) => (_jsxs("div", { className: styles.detailPreviewParamRow, children: [_jsx("span", { children: row.name || '--' }), _jsx("span", { children: row.value || '--' })] }, row.id))) }, block.id));
        }) }));
}
