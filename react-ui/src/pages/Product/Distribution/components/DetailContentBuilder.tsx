import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  DeleteOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { Button, Input, Select } from 'antd';
import {
  createDetailBlock,
  detailBlockTypeOptions,
  makeDetailId,
  type DetailBlockType,
  type DetailContentBlock,
  type DetailParamRow,
} from '../detailContent';
import ImageUploadField from './ImageUploadField';
import styles from '../style.module.css';

type DetailContentBuilderProps = {
  value: DetailContentBlock[];
  onChange: (value: DetailContentBlock[]) => void;
};

function updateParamRows(rows: DetailParamRow[] | undefined, rowId: string, patch: Partial<DetailParamRow>) {
  return (rows || []).map((row) => (row.id === rowId ? { ...row, ...patch } : row));
}
export default function DetailContentBuilder({ value, onChange }: DetailContentBuilderProps) {
  const addBlock = (type: DetailBlockType) => {
    onChange([...value, createDetailBlock(type)]);
  };

  const updateBlock = (id: string, patch: Partial<DetailContentBlock>) => {
    onChange(value.map((block) => (block.id === id ? { ...block, ...patch } : block)));
  };

  const moveBlock = (index: number, offset: number) => {
    const nextIndex = index + offset;
    if (nextIndex < 0 || nextIndex >= value.length) return;
    const next = [...value];
    const [current] = next.splice(index, 1);
    next.splice(nextIndex, 0, current);
    onChange(next);
  };

  const removeBlock = (id: string) => {
    onChange(value.filter((block) => block.id !== id));
  };

  const renderBlockBody = (block: DetailContentBlock) => {
    if (block.type === 'TEXT') {
      return (
        <Input.TextArea
          rows={4}
          value={block.text}
          placeholder="填写详情段落，例如材质、使用场景、保养说明。"
          onChange={(event) => updateBlock(block.id, { text: event.target.value })}
        />
      );
    }
    if (block.type === 'IMAGE') {
      return (
        <ImageUploadField
          value={block.imageUrl}
          onChange={(imageUrl) => updateBlock(block.id, { imageUrl })}
        />
      );
    }
    if (block.type === 'IMAGE_TEXT') {
      return (
        <div className={styles.detailImageTextGrid}>
          <ImageUploadField
            value={block.imageUrl}
            onChange={(imageUrl) => updateBlock(block.id, { imageUrl })}
          />
          <div className={styles.detailTextFields}>
            <Input
              value={block.title}
              placeholder="图文标题"
              onChange={(event) => updateBlock(block.id, { title: event.target.value })}
            />
            <Input.TextArea
              rows={4}
              value={block.text}
              placeholder="填写图片旁的说明文字。"
              onChange={(event) => updateBlock(block.id, { text: event.target.value })}
            />
          </div>
        </div>
      );
    }
    return (
      <div className={styles.detailParamTable}>
        {(block.rows || []).map((row) => (
          <div className={styles.detailParamRow} key={row.id}>
            <Input
              value={row.name}
              placeholder="参数名"
              onChange={(event) =>
                updateBlock(block.id, {
                  rows: updateParamRows(block.rows, row.id, { name: event.target.value }),
                })
              }
            />
            <Input
              value={row.value}
              placeholder="参数值"
              onChange={(event) =>
                updateBlock(block.id, {
                  rows: updateParamRows(block.rows, row.id, { value: event.target.value }),
                })
              }
            />
            <Button
              danger
              type="link"
              onClick={() => updateBlock(block.id, { rows: (block.rows || []).filter((item) => item.id !== row.id) })}
            >
              删除
            </Button>
          </div>
        ))}
        <Button
          icon={<PlusOutlined />}
          onClick={() =>
            updateBlock(block.id, {
              rows: [...(block.rows || []), { id: makeDetailId('row'), name: '', value: '' }],
            })
          }
        >
          新增参数
        </Button>
      </div>
    );
  };

  return (
    <div className={styles.detailBuilder}>
      <div className={styles.detailToolbar}>
        {detailBlockTypeOptions.map((item) => (
          <Button key={item.value} icon={<PlusOutlined />} onClick={() => addBlock(item.value)}>
            {item.label}
          </Button>
        ))}
      </div>

      {value.length ? (
        <div className={styles.detailBlockList}>
          {value.map((block, index) => (
            <div className={styles.detailBlock} key={block.id}>
              <div className={styles.detailBlockHeader}>
                <Select
                  value={block.type}
                  options={detailBlockTypeOptions}
                  style={{ width: 160 }}
                  onChange={(type) => updateBlock(block.id, createDetailBlock(type))}
                />
                <div className={styles.detailBlockActions}>
                  <Button icon={<ArrowUpOutlined />} disabled={index === 0} onClick={() => moveBlock(index, -1)} />
                  <Button icon={<ArrowDownOutlined />} disabled={index === value.length - 1} onClick={() => moveBlock(index, 1)} />
                  <Button danger icon={<DeleteOutlined />} onClick={() => removeBlock(block.id)} />
                </div>
              </div>
              {renderBlockBody(block)}
            </div>
          ))}
        </div>
      ) : (
        <div className={styles.detailEmpty}>暂无详情模块</div>
      )}
    </div>
  );
}
