import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, PlusOutlined, } from '@ant-design/icons';
import { Button, Input, Select } from 'antd';
import { createDetailBlock, detailBlockTypeOptions, makeDetailId, } from '../detailContent';
import ImageUploadField from './ImageUploadField';
import styles from '../style.module.css';
function updateParamRows(rows, rowId, patch) {
    return (rows || []).map((row) => (row.id === rowId ? { ...row, ...patch } : row));
}
export default function DetailContentBuilder({ value, onChange }) {
    const addBlock = (type) => {
        onChange([...value, createDetailBlock(type)]);
    };
    const updateBlock = (id, patch) => {
        onChange(value.map((block) => (block.id === id ? { ...block, ...patch } : block)));
    };
    const moveBlock = (index, offset) => {
        const nextIndex = index + offset;
        if (nextIndex < 0 || nextIndex >= value.length)
            return;
        const next = [...value];
        const [current] = next.splice(index, 1);
        next.splice(nextIndex, 0, current);
        onChange(next);
    };
    const removeBlock = (id) => {
        onChange(value.filter((block) => block.id !== id));
    };
    const renderBlockBody = (block) => {
        if (block.type === 'TEXT') {
            return (_jsx(Input.TextArea, { rows: 4, value: block.text, placeholder: "\u586B\u5199\u8BE6\u60C5\u6BB5\u843D\uFF0C\u4F8B\u5982\u6750\u8D28\u3001\u4F7F\u7528\u573A\u666F\u3001\u4FDD\u517B\u8BF4\u660E\u3002", onChange: (event) => updateBlock(block.id, { text: event.target.value }) }));
        }
        if (block.type === 'IMAGE') {
            return (_jsx(ImageUploadField, { value: block.imageUrl, onChange: (imageUrl) => updateBlock(block.id, { imageUrl }) }));
        }
        if (block.type === 'IMAGE_TEXT') {
            return (_jsxs("div", { className: styles.detailImageTextGrid, children: [_jsx(ImageUploadField, { value: block.imageUrl, onChange: (imageUrl) => updateBlock(block.id, { imageUrl }) }), _jsxs("div", { className: styles.detailTextFields, children: [_jsx(Input, { value: block.title, placeholder: "\u56FE\u6587\u6807\u9898", onChange: (event) => updateBlock(block.id, { title: event.target.value }) }), _jsx(Input.TextArea, { rows: 4, value: block.text, placeholder: "\u586B\u5199\u56FE\u7247\u65C1\u7684\u8BF4\u660E\u6587\u5B57\u3002", onChange: (event) => updateBlock(block.id, { text: event.target.value }) })] })] }));
        }
        return (_jsxs("div", { className: styles.detailParamTable, children: [(block.rows || []).map((row) => (_jsxs("div", { className: styles.detailParamRow, children: [_jsx(Input, { value: row.name, placeholder: "\u53C2\u6570\u540D", onChange: (event) => updateBlock(block.id, {
                                rows: updateParamRows(block.rows, row.id, { name: event.target.value }),
                            }) }), _jsx(Input, { value: row.value, placeholder: "\u53C2\u6570\u503C", onChange: (event) => updateBlock(block.id, {
                                rows: updateParamRows(block.rows, row.id, { value: event.target.value }),
                            }) }), _jsx(Button, { danger: true, type: "link", onClick: () => updateBlock(block.id, { rows: (block.rows || []).filter((item) => item.id !== row.id) }), children: "\u5220\u9664" })] }, row.id))), _jsx(Button, { icon: _jsx(PlusOutlined, {}), onClick: () => updateBlock(block.id, {
                        rows: [...(block.rows || []), { id: makeDetailId('row'), name: '', value: '' }],
                    }), children: "\u65B0\u589E\u53C2\u6570" })] }));
    };
    return (_jsxs("div", { className: styles.detailBuilder, children: [_jsx("div", { className: styles.detailToolbar, children: detailBlockTypeOptions.map((item) => (_jsx(Button, { icon: _jsx(PlusOutlined, {}), onClick: () => addBlock(item.value), children: item.label }, item.value))) }), value.length ? (_jsx("div", { className: styles.detailBlockList, children: value.map((block, index) => (_jsxs("div", { className: styles.detailBlock, children: [_jsxs("div", { className: styles.detailBlockHeader, children: [_jsx(Select, { value: block.type, options: detailBlockTypeOptions, style: { width: 160 }, onChange: (type) => updateBlock(block.id, createDetailBlock(type)) }), _jsxs("div", { className: styles.detailBlockActions, children: [_jsx(Button, { icon: _jsx(ArrowUpOutlined, {}), disabled: index === 0, onClick: () => moveBlock(index, -1) }), _jsx(Button, { icon: _jsx(ArrowDownOutlined, {}), disabled: index === value.length - 1, onClick: () => moveBlock(index, 1) }), _jsx(Button, { danger: true, icon: _jsx(DeleteOutlined, {}), onClick: () => removeBlock(block.id) })] })] }), renderBlockBody(block)] }, block.id))) })) : (_jsx("div", { className: styles.detailEmpty, children: "\u6682\u65E0\u8BE6\u60C5\u6A21\u5757" }))] }));
}
