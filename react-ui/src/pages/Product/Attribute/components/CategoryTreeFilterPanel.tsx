import { Button, Checkbox, Empty, Input, Select, Space, Tree } from 'antd';
import type { Key } from 'react';
import { categoryStatusOptions } from './categoryAttributeFilterUtils';

type CategoryTreeFilterPanelProps = {
  treeData: any[];
  selectedCategoryId?: number;
  expandedCategoryKeys: Key[];
  autoExpandParent: boolean;
  visibleCategoryKeys: Key[];
  categoryKeyword: string;
  categoryStatus: string;
  categoryLevel: string;
  categoryLevelOptions: { label: string; value: string }[];
  leafOnly: boolean;
  onCategoryKeywordChange: (value: string) => void;
  onCategoryStatusChange: (value: string) => void;
  onCategoryLevelChange: (value: string) => void;
  onLeafOnlyChange: (value: boolean) => void;
  onExpandedCategoryKeysChange: (keys: Key[], autoExpandParent: boolean) => void;
  onSelectCategory: (categoryId: number) => void;
};

export default function CategoryTreeFilterPanel({
  treeData,
  selectedCategoryId,
  expandedCategoryKeys,
  autoExpandParent,
  visibleCategoryKeys,
  categoryKeyword,
  categoryStatus,
  categoryLevel,
  categoryLevelOptions,
  leafOnly,
  onCategoryKeywordChange,
  onCategoryStatusChange,
  onCategoryLevelChange,
  onLeafOnlyChange,
  onExpandedCategoryKeysChange,
  onSelectCategory,
}: CategoryTreeFilterPanelProps) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: 0 }}>
      <Space direction="vertical" size={12} style={{ width: '100%' }}>
        <Input.Search
          allowClear
          placeholder="搜索类目名称/编码/路径"
          value={categoryKeyword}
          onChange={(event) => onCategoryKeywordChange(event.target.value)}
        />
        <Select
          value={categoryStatus}
          options={categoryStatusOptions}
          onChange={onCategoryStatusChange}
          style={{ width: '100%' }}
        />
        <Select
          value={categoryLevel}
          options={categoryLevelOptions}
          onChange={onCategoryLevelChange}
          style={{ width: '100%' }}
        />
        <Checkbox
          checked={leafOnly}
          onChange={(event) => onLeafOnlyChange(event.target.checked)}
        >
          只看末级类目
        </Checkbox>
        <Space>
          <Button
            size="small"
            onClick={() => onExpandedCategoryKeysChange(visibleCategoryKeys, true)}
          >
            展开全部
          </Button>
          <Button
            size="small"
            onClick={() => onExpandedCategoryKeysChange([], false)}
          >
            收起全部
          </Button>
        </Space>
      </Space>
      <div
        style={{
          flex: 1,
          minHeight: 0,
          marginTop: 12,
          overflow: 'auto',
          paddingRight: 4,
        }}
      >
        {treeData.length ? (
          <Tree
            treeData={treeData}
            selectedKeys={selectedCategoryId ? [selectedCategoryId] : []}
            expandedKeys={expandedCategoryKeys}
            autoExpandParent={autoExpandParent}
            onExpand={(keys) => onExpandedCategoryKeysChange(keys, false)}
            onSelect={(keys) => {
              const key = keys[0];
              if (key) {
                onSelectCategory(Number(key));
              }
            }}
          />
        ) : (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无匹配类目" />
        )}
      </div>
    </div>
  );
}
