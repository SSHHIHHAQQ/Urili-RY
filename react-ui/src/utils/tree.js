import { parse } from 'querystring';
/**
 * 构造树型结构数据
 * @param {*} data 数据源
 * @param {*} id id字段 默认 'id'
 * @param {*} parentId 父节点字段 默认 'parentId'
 * @param {*} children 孩子节点字段 默认 'children'
 */
export function buildTreeData(data, id, name, parentId, parentName, children) {
    const config = {
        id: id || 'id',
        name: name || 'name',
        parentId: parentId || 'parentId',
        parentName: parentName || 'parentName',
        childrenList: children || 'children',
    };
    const childrenListMap = [];
    const nodeIds = [];
    const tree = [];
    data.forEach((item) => {
        const d = item;
        const pId = d[config.parentId];
        if (!childrenListMap[pId]) {
            childrenListMap[pId] = [];
        }
        d.key = d[config.id];
        d.title = d[config.name];
        d.value = d[config.id];
        d[config.childrenList] = null;
        nodeIds[d[config.id]] = d;
        childrenListMap[pId].push(d);
    });
    data.forEach((item) => {
        const d = item;
        const pId = d[config.parentId];
        if (!nodeIds[pId]) {
            d[config.parentName] = '';
            tree.push(d);
        }
    });
    function adaptToChildrenList(item) {
        const o = item;
        if (childrenListMap[o[config.id]]) {
            if (!o[config.childrenList]) {
                o[config.childrenList] = [];
            }
            o[config.childrenList] = childrenListMap[o[config.id]];
        }
        if (o[config.childrenList]) {
            o[config.childrenList].forEach((child) => {
                const c = child;
                c[config.parentName] = o[config.name];
                adaptToChildrenList(c);
            });
        }
    }
    tree.forEach((t) => {
        adaptToChildrenList(t);
    });
    return tree;
}
export const getPageQuery = () => parse(window.location.href.split('?')[1]);
export function formatTreeData(arrayList) {
    const treeSelectData = arrayList.map((item) => {
        const node = {
            id: item.id,
            title: item.label,
            key: `${item.id}`,
            value: item.id,
        };
        if (item.children) {
            node.children = formatTreeData(item.children);
        }
        return node;
    });
    return treeSelectData;
}
