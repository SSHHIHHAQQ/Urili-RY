import { Button, Checkbox, Empty, Input, Select, Space, Spin, Tree } from 'antd';
import type { Key } from 'react';
import { categoryStatusOptions } from './categoryAttributeFilterUtils';

type CategoryTreeFilterPanelProps = {
  treeData: any[];
  selectedCategoryId?: number;
  expandedCategoryKeys: Key[];
  autoExpandParent: boolean;
  loading?: boolean;
  loadingMore?: boolean;
  searchMode?: boolean;
  searchResultLoaded?: number;
  searchResultTotal?: number;
  searchHasMore?: boolean;
  categoryKeyword: string;
  categoryStatus: string;
  categoryLevel: string;
  categoryLevelOptions: { label: string; value: string }[];
  leafOnly: boolean;
  onCategoryKeywordChange: (value: string) => void;
  onCategoryStatusChange: (value: string) => void;
  onCategoryLevelChange: (value: string) => void;
  onLeafOnlyChange: (value: boolean) => void;
  onExpandedCategoryKeysChange: (
    keys: Key[],
    autoExpandParent: boolean,
  ) => void;
  onLoadCategoryChildren?: (categoryId: number) => Promise<void>;
  onLoadMoreSearchResults?: () => void;
  onSelectCategory: (categoryId: number) => void;
};

export default function CategoryTreeFilterPanel({
  treeData,
  selectedCategoryId,
  expandedCategoryKeys,
  autoExpandParent,
  loading,
  loadingMore,
  searchMode,
  searchResultLoaded,
  searchResultTotal,
  searchHasMore,
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
  onLoadCategoryChildren,
  onLoadMoreSearchResults,
  onSelectCategory,
}: CategoryTreeFilterPanelProps) {
  const hasExpandedCategory = expandedCategoryKeys.length > 0;
  const showSearchStatus = searchMode && Number(searchResultTotal || 0) > 0;

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        minHeight: 0,
      }}
    >
      <Space orientation="vertical" size={12} style={{ width: '100%' }}>
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
        {!searchMode && hasExpandedCategory ? (
          <Button
            size="small"
            onClick={() => onExpandedCategoryKeysChange([], false)}
          >
            收起全部
          </Button>
        ) : null}
      </Space>
      <div
        style={{
          flex: 1,
          minHeight: 0,
          marginTop: 12,
          overflow: 'auto',
          paddingRight: 4,
        }}
        onScroll={(event) => {
          if (!searchMode || !searchHasMore || loadingMore) {
            return;
          }
          const target = event.currentTarget;
          const distanceToBottom =
            target.scrollHeight - target.scrollTop - target.clientHeight;
          if (distanceToBottom <= 48) {
            onLoadMoreSearchResults?.();
          }
        }}
      >
        <Spin spinning={!!loading}>
          {treeData.length ? (
            <Tree
              blockNode
              treeData={treeData}
              selectedKeys={selectedCategoryId ? [selectedCategoryId] : []}
              expandedKeys={expandedCategoryKeys}
              autoExpandParent={autoExpandParent}
              loadData={(node) => {
                const categoryId = Number(node.key);
                if (
                  searchMode ||
                  !categoryId ||
                  node.isLeaf ||
                  !onLoadCategoryChildren
                ) {
                  return Promise.resolve();
                }
                return onLoadCategoryChildren(categoryId);
              }}
              onExpand={(keys) => onExpandedCategoryKeysChange(keys, false)}
              onSelect={(keys) => {
                const key = keys[0];
                if (key) {
                  onSelectCategory(Number(key));
                }
              }}
            />
          ) : (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="暂无匹配类目"
            />
          )}
        </Spin>
      </div>
      {showSearchStatus ? (
        <div
          style={{
            flex: '0 0 auto',
            paddingTop: 8,
            color: '#8c8c8c',
            fontSize: 12,
          }}
        >
          已显示 {searchResultLoaded || 0} / {searchResultTotal || 0}
          {loadingMore ? '，加载中...' : searchHasMore ? '，向下滚动加载更多' : ''}
        </div>
      ) : null}
    </div>
  );
}
