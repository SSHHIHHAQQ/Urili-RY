import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Button, Checkbox, Empty, Input, Select, Space, Spin, Tree } from 'antd';
import { categoryStatusOptions } from './categoryAttributeFilterUtils';
export default function CategoryTreeFilterPanel({ treeData, selectedCategoryId, expandedCategoryKeys, autoExpandParent, loading, loadingMore, searchMode, searchResultLoaded, searchResultTotal, searchHasMore, categoryKeyword, categoryStatus, categoryLevel, categoryLevelOptions, leafOnly, onCategoryKeywordChange, onCategoryStatusChange, onCategoryLevelChange, onLeafOnlyChange, onExpandedCategoryKeysChange, onLoadCategoryChildren, onLoadMoreSearchResults, onSelectCategory, }) {
    const hasExpandedCategory = expandedCategoryKeys.length > 0;
    const showSearchStatus = searchMode && Number(searchResultTotal || 0) > 0;
    return (_jsxs("div", { style: {
            display: 'flex',
            flexDirection: 'column',
            height: '100%',
            minHeight: 0,
        }, children: [_jsxs(Space, { orientation: "vertical", size: 12, style: { width: '100%' }, children: [_jsx(Input.Search, { allowClear: true, placeholder: "\u641C\u7D22\u7C7B\u76EE\u540D\u79F0/\u7F16\u7801/\u8DEF\u5F84", value: categoryKeyword, onChange: (event) => onCategoryKeywordChange(event.target.value) }), _jsx(Select, { value: categoryStatus, options: categoryStatusOptions, onChange: onCategoryStatusChange, style: { width: '100%' } }), _jsx(Select, { value: categoryLevel, options: categoryLevelOptions, onChange: onCategoryLevelChange, style: { width: '100%' } }), _jsx(Checkbox, { checked: leafOnly, onChange: (event) => onLeafOnlyChange(event.target.checked), children: "\u53EA\u770B\u672B\u7EA7\u7C7B\u76EE" }), !searchMode && hasExpandedCategory ? (_jsx(Button, { size: "small", onClick: () => onExpandedCategoryKeysChange([], false), children: "\u6536\u8D77\u5168\u90E8" })) : null] }), _jsx("div", { style: {
                    flex: 1,
                    minHeight: 0,
                    marginTop: 12,
                    overflow: 'auto',
                    paddingRight: 4,
                }, onScroll: (event) => {
                    if (!searchMode || !searchHasMore || loadingMore) {
                        return;
                    }
                    const target = event.currentTarget;
                    const distanceToBottom = target.scrollHeight - target.scrollTop - target.clientHeight;
                    if (distanceToBottom <= 48) {
                        onLoadMoreSearchResults?.();
                    }
                }, children: _jsx(Spin, { spinning: !!loading, children: treeData.length ? (_jsx(Tree, { blockNode: true, treeData: treeData, selectedKeys: selectedCategoryId ? [selectedCategoryId] : [], expandedKeys: expandedCategoryKeys, autoExpandParent: autoExpandParent, loadData: (node) => {
                            const categoryId = Number(node.key);
                            if (searchMode ||
                                !categoryId ||
                                node.isLeaf ||
                                !onLoadCategoryChildren) {
                                return Promise.resolve();
                            }
                            return onLoadCategoryChildren(categoryId);
                        }, onExpand: (keys) => onExpandedCategoryKeysChange(keys, false), onSelect: (keys) => {
                            const key = keys[0];
                            if (key) {
                                onSelectCategory(Number(key));
                            }
                        } })) : (_jsx(Empty, { image: Empty.PRESENTED_IMAGE_SIMPLE, description: "\u6682\u65E0\u5339\u914D\u7C7B\u76EE" })) }) }), showSearchStatus ? (_jsxs("div", { style: {
                    flex: '0 0 auto',
                    paddingTop: 8,
                    color: '#8c8c8c',
                    fontSize: 12,
                }, children: ["\u5DF2\u663E\u793A ", searchResultLoaded || 0, " / ", searchResultTotal || 0, loadingMore ? '，加载中...' : searchHasMore ? '，向下滚动加载更多' : ''] })) : null] }));
}
