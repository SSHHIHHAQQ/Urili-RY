import { jsx as _jsx } from "react/jsx-runtime";
import { useEffect } from 'react';
import { useIntl } from '@umijs/max';
import { Modal, Tabs } from 'antd';
import Highlight from 'react-highlight';
import 'highlight.js/styles/base16/material.css';
const PreviewTableCode = (props) => {
    const intl = useIntl();
    const panes = [];
    const keys = Object.keys(props.data);
    keys.forEach((key) => {
        panes.push({
            key: key + '1',
            label: key.substring(key.lastIndexOf('/') + 1, key.indexOf('.vm')),
            children: _jsx(Highlight, { className: "java", children: props.data[key] }),
        });
    });
    useEffect(() => { }, []);
    return (_jsx(Modal, { width: 900, title: intl.formatMessage({
            id: 'gen.preview',
            defaultMessage: '预览',
        }), open: props.open, destroyOnHidden: true, footer: false, onOk: () => {
            props.onHide();
        }, onCancel: () => {
            props.onHide();
        }, children: _jsx(Tabs, { defaultActiveKey: "1", items: panes }) }));
};
export default PreviewTableCode;
