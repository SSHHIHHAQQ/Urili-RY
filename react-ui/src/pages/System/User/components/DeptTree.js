import { jsx as _jsx } from "react/jsx-runtime";
import { useState, useEffect } from 'react';
import { App, Tree } from 'antd';
import { getDeptTree } from '@/services/system/user';
const { DirectoryTree } = Tree;
const DeptTree = (props) => {
    const { message } = App.useApp();
    const [treeData, setTreeData] = useState([]);
    const [expandedKeys, setExpandedKeys] = useState([]);
    const [autoExpandParent, setAutoExpandParent] = useState(true);
    const fetchDeptList = async () => {
        const hide = message.loading('正在查询');
        try {
            await getDeptTree({}).then((res) => {
                const exKeys = [];
                exKeys.push('1');
                setTreeData(res);
                exKeys.push(res[0].children[0].id);
                setExpandedKeys(exKeys);
                props.onSelect(res[0].children[0]);
            });
            hide();
            return true;
        }
        catch (error) {
            hide();
            return false;
        }
    };
    useEffect(() => {
        fetchDeptList();
    }, []);
    const onSelect = (keys, info) => {
        props.onSelect(info.node);
    };
    const onExpand = (expandedKeysValue) => {
        setExpandedKeys(expandedKeysValue);
        setAutoExpandParent(false);
    };
    return (_jsx(DirectoryTree
    // multiple
    , {
        // multiple
        defaultExpandAll: true, onExpand: onExpand, expandedKeys: expandedKeys, autoExpandParent: autoExpandParent, onSelect: onSelect, treeData: treeData }));
};
export default DeptTree;
