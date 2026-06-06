export const detailBlockTypeOptions = [
    { label: '文本段落', value: 'TEXT' },
    { label: '图片模块', value: 'IMAGE' },
    { label: '图文模块', value: 'IMAGE_TEXT' },
    { label: '参数表模块', value: 'PARAM_TABLE' },
];
export function makeDetailId(prefix = 'detail') {
    return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}
export function createDetailBlock(type) {
    if (type === 'PARAM_TABLE') {
        return {
            id: makeDetailId('param'),
            type,
            rows: [{ id: makeDetailId('row'), name: '', value: '' }],
        };
    }
    return {
        id: makeDetailId(type.toLowerCase()),
        type,
    };
}
function normalizeBlock(block) {
    if (!block || !block.type)
        return undefined;
    if (!detailBlockTypeOptions.some((item) => item.value === block.type))
        return undefined;
    return {
        id: block.id || makeDetailId('block'),
        type: block.type,
        title: block.title || '',
        text: block.text || '',
        imageUrl: block.imageUrl || '',
        rows: (block.rows || []).map((row) => ({
            id: row.id || makeDetailId('row'),
            name: row.name || '',
            value: row.value || '',
        })),
    };
}
export function parseDetailContent(value) {
    if (!value?.trim())
        return [];
    try {
        const parsed = JSON.parse(value);
        const blocks = Array.isArray(parsed) ? parsed : parsed.blocks;
        if (!Array.isArray(blocks))
            return [];
        return blocks.map(normalizeBlock).filter(Boolean);
    }
    catch {
        return [{ id: makeDetailId('text'), type: 'TEXT', text: value }];
    }
}
export function serializeDetailContent(blocks) {
    const normalized = blocks.map(normalizeBlock).filter(Boolean);
    if (!normalized.length)
        return '';
    return JSON.stringify({ version: 1, blocks: normalized });
}
