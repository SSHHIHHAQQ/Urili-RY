import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import React, { useCallback, useEffect, useState } from 'react';
import { Upload, Tooltip, Popover, Modal, Progress, Spin, Result } from 'antd';
import * as AntdIcons from '@ant-design/icons';
import { useIntl } from '@umijs/max';
import styles from './style.module.css';
const allIcons = AntdIcons;
const { Dragger } = Upload;
const PicSearcher = () => {
    const intl = useIntl();
    const { formatMessage } = intl;
    const [state, setState] = useState({
        loading: false,
        modalOpen: false,
        popoverVisible: false,
        icons: [],
        fileList: [],
        error: false,
        modelLoaded: false,
    });
    const predict = (imgEl) => {
        try {
            let icons = window.antdIconClassifier.predict(imgEl);
            if (typeof gtag !== 'undefined' && icons.length) {
                gtag('event', 'icon', {
                    event_category: 'search-by-image',
                    event_label: icons[0].className,
                });
            }
            icons = icons.map(i => ({ score: i.score, type: i.className.replace(/\s/g, '-') }));
            setState(prev => ({ ...prev, loading: false, error: false, icons }));
        }
        catch {
            setState(prev => ({ ...prev, loading: false, error: true }));
        }
    };
    // eslint-disable-next-line class-methods-use-this
    const toImage = (url) => new Promise(resolve => {
        const img = new Image();
        img.setAttribute('crossOrigin', 'anonymous');
        img.src = url;
        img.onload = () => {
            resolve(img);
        };
    });
    const uploadFile = useCallback((file) => {
        setState(prev => ({ ...prev, loading: true }));
        const reader = new FileReader();
        reader.onload = () => {
            toImage(reader.result).then((img) => predict(img));
            setState(prev => ({
                ...prev,
                fileList: [{ uid: 1, name: file.name, status: 'done', url: reader.result }],
            }));
        };
        reader.readAsDataURL(file);
    }, []);
    const onPaste = useCallback((event) => {
        const items = event.clipboardData && event.clipboardData.items;
        let file = null;
        if (items && items.length) {
            for (let i = 0; i < items.length; i++) {
                if (items[i].type.includes('image')) {
                    file = items[i].getAsFile();
                    break;
                }
            }
        }
        if (file) {
            uploadFile(file);
        }
    }, []);
    const toggleModal = useCallback(() => {
        setState(prev => ({
            ...prev,
            modalOpen: !prev.modalOpen,
            popoverVisible: false,
            fileList: [],
            icons: [],
        }));
        if (!localStorage.getItem('disableIconTip')) {
            localStorage.setItem('disableIconTip', 'true');
        }
    }, []);
    useEffect(() => {
        const script = document.createElement('script');
        script.onload = async () => {
            await window.antdIconClassifier.load();
            setState(prev => ({ ...prev, modelLoaded: true }));
            document.addEventListener('paste', onPaste);
        };
        script.src = 'https://cdn.jsdelivr.net/gh/lewis617/antd-icon-classifier@0.0/dist/main.js';
        document.head.appendChild(script);
        setState(prev => ({ ...prev, popoverVisible: !localStorage.getItem('disableIconTip') }));
        return () => {
            document.removeEventListener('paste', onPaste);
        };
    }, []);
    return (_jsxs("div", { className: styles.iconPicSearcher, children: [_jsx(Popover, { content: formatMessage({ id: 'app.docs.components.icon.pic-searcher.intro' }), open: state.popoverVisible, children: _jsx(AntdIcons.CameraOutlined, { className: styles.iconPicBtn, onClick: toggleModal }) }), _jsxs(Modal, { title: intl.formatMessage({
                    id: 'app.docs.components.icon.pic-searcher.title',
                    defaultMessage: '信息',
                }), open: state.modalOpen, onCancel: toggleModal, footer: null, children: [state.modelLoaded || (_jsx(Spin, { spinning: !state.modelLoaded, tip: formatMessage({
                            id: 'app.docs.components.icon.pic-searcher.modelloading',
                        }), children: _jsx("div", { style: { height: 100 } }) })), state.modelLoaded && (_jsxs(Dragger, { accept: "image/jpeg, image/png", listType: "picture", customRequest: o => uploadFile(o.file), fileList: state.fileList, showUploadList: { showPreviewIcon: false, showRemoveIcon: false }, children: [_jsx("p", { className: "ant-upload-drag-icon", children: _jsx(AntdIcons.InboxOutlined, {}) }), _jsx("p", { className: "ant-upload-text", children: formatMessage({ id: 'app.docs.components.icon.pic-searcher.upload-text' }) }), _jsx("p", { className: "ant-upload-hint", children: formatMessage({ id: 'app.docs.components.icon.pic-searcher.upload-hint' }) })] })), _jsx(Spin, { spinning: state.loading, tip: formatMessage({ id: 'app.docs.components.icon.pic-searcher.matching' }), children: _jsxs("div", { className: styles.iconPicSearchResult, children: [state.icons.length > 0 && (_jsx("div", { className: styles.resultTip, children: formatMessage({ id: 'app.docs.components.icon.pic-searcher.result-tip' }) })), _jsxs("table", { children: [state.icons.length > 0 && (_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { className: styles.colIcon, children: formatMessage({ id: 'app.docs.components.icon.pic-searcher.th-icon' }) }), _jsx("th", { children: formatMessage({ id: 'app.docs.components.icon.pic-searcher.th-score' }) })] }) })), _jsx("tbody", { children: state.icons.map(icon => {
                                                const { type } = icon;
                                                const iconName = `${type
                                                    .split('-')
                                                    .map(str => `${str[0].toUpperCase()}${str.slice(1)}`)
                                                    .join('')}Outlined`;
                                                return (_jsxs("tr", { children: [_jsx("td", { className: styles.colIcon, children: _jsx(Tooltip, { title: icon.type, placement: "right", children: React.createElement(allIcons[iconName]) }) }), _jsx("td", { children: _jsx(Progress, { percent: Math.ceil(icon.score * 100) }) })] }, iconName));
                                            }) })] }), state.error && (_jsx(Result, { status: "500", title: "503", subTitle: formatMessage({ id: 'app.docs.components.icon.pic-searcher.server-error' }) }))] }) })] })] }));
};
export default PicSearcher;
