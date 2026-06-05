export type DetailBlockType = 'TEXT' | 'IMAGE' | 'IMAGE_TEXT' | 'PARAM_TABLE';

export type DetailParamRow = {
  id: string;
  name?: string;
  value?: string;
};

export type DetailContentBlock = {
  id: string;
  type: DetailBlockType;
  title?: string;
  text?: string;
  imageUrl?: string;
  rows?: DetailParamRow[];
};

export const detailBlockTypeOptions: { label: string; value: DetailBlockType }[] = [
  { label: '文本段落', value: 'TEXT' },
  { label: '图片模块', value: 'IMAGE' },
  { label: '图文模块', value: 'IMAGE_TEXT' },
  { label: '参数表模块', value: 'PARAM_TABLE' },
];

export function makeDetailId(prefix = 'detail') {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}
export function createDetailBlock(type: DetailBlockType): DetailContentBlock {
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

function normalizeBlock(block: Partial<DetailContentBlock>): DetailContentBlock | undefined {
  if (!block || !block.type) return undefined;
  if (!detailBlockTypeOptions.some((item) => item.value === block.type)) return undefined;
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

export function parseDetailContent(value?: string): DetailContentBlock[] {
  if (!value?.trim()) return [];
  try {
    const parsed = JSON.parse(value);
    const blocks = Array.isArray(parsed) ? parsed : parsed.blocks;
    if (!Array.isArray(blocks)) return [];
    return blocks.map(normalizeBlock).filter(Boolean) as DetailContentBlock[];
  } catch {
    return [{ id: makeDetailId('text'), type: 'TEXT', text: value }];
  }
}

export function serializeDetailContent(blocks: DetailContentBlock[]) {
  const normalized = blocks.map(normalizeBlock).filter(Boolean);
  if (!normalized.length) return '';
  return JSON.stringify({ version: 1, blocks: normalized });
}
