import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useState } from 'react';
import { clearCacheAll, clearCacheKey, clearCacheName, getCacheValue, listCacheKey, listCacheName, } from '@/services/monitor/cachelist';
import { Button, Card, Col, Form, Input, Popconfirm, Row, Table } from 'antd';
import styles from './index.module.css';
import { ReloadOutlined } from '@ant-design/icons';
import { ProForm } from '@ant-design/pro-components';
import { message } from '@/utils/feedback';
const { TextArea } = Input;
const CacheList = () => {
    const [cacheNames, setCacheNames] = useState([]);
    const [currentCacheName, setCurrentCacheName] = useState('');
    const [cacheKeys, setCacheKeys] = useState([]);
    const [form] = Form.useForm();
    const getCacheNames = () => {
        listCacheName().then((res) => {
            if (res.code === 200) {
                setCacheNames(res.data || []);
            }
        });
    };
    useEffect(() => {
        getCacheNames();
    }, []);
    const getCacheKeys = (cacheName) => {
        if (!cacheName) {
            setCacheKeys([]);
            return;
        }
        listCacheKey(cacheName).then((res) => {
            if (res.code === 200) {
                const keysData = (res.data || []).map((item, index) => ({
                    index: index + 1,
                    cacheKey: item,
                }));
                setCacheKeys(keysData);
            }
        });
    };
    const onClearAll = async () => {
        clearCacheAll().then((res) => {
            if (res.code === 200) {
                message.success('清理全部缓存成功');
                setCacheKeys([]);
                setCurrentCacheName('');
                form.resetFields();
                getCacheNames();
            }
        });
    };
    const refreshCacheNames = () => {
        getCacheNames();
        message.success('刷新缓存列表成功');
    };
    const refreshCacheKeys = () => {
        if (!currentCacheName) {
            message.warning('请先选择缓存名称');
            return;
        }
        getCacheKeys(currentCacheName);
        message.success('刷新键名列表成功');
    };
    const columns = [
        {
            title: '缓存名称',
            dataIndex: 'cacheName',
            key: 'cacheName',
            render: (_, record) => record.cacheName.replace(':', ''),
        },
        {
            title: '备注',
            dataIndex: 'remark',
            key: 'remark',
        },
        {
            title: '操作',
            dataIndex: 'option',
            width: 90,
            render: (_, record) => (_jsx(Button, { type: "link", size: "small", danger: true, onClick: (event) => {
                    event.stopPropagation();
                    clearCacheName(record.cacheName).then((res) => {
                        if (res.code === 200) {
                            message.success(`清理缓存名称[${record.cacheName}]成功`);
                            if (currentCacheName === record.cacheName) {
                                setCacheKeys([]);
                                form.resetFields();
                            }
                        }
                    });
                }, children: "\u5220\u9664" })),
        },
    ];
    const cacheKeysColumns = [
        {
            title: '序号',
            dataIndex: 'index',
            key: 'index',
            width: 80,
        },
        {
            title: '缓存键名',
            dataIndex: 'cacheKey',
            key: 'cacheKey',
            render: (_, record) => record.cacheKey.replace(currentCacheName, ''),
        },
        {
            title: '操作',
            dataIndex: 'option',
            width: 90,
            render: (_, record) => (_jsx(Button, { type: "link", size: "small", danger: true, onClick: (event) => {
                    event.stopPropagation();
                    clearCacheKey(record.cacheKey).then((res) => {
                        if (res.code === 200) {
                            message.success(`清理缓存键名[${record.cacheKey}]成功`);
                            getCacheKeys(currentCacheName);
                            form.resetFields();
                        }
                    });
                }, children: "\u5220\u9664" })),
        },
    ];
    return (_jsx("div", { children: _jsxs(Row, { gutter: [24, 24], children: [_jsx(Col, { span: 8, children: _jsx(Card, { title: "\u7F13\u5B58\u5217\u8868", extra: _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: refreshCacheNames, type: "link" }), className: styles.card, children: _jsx(Table, { rowKey: "cacheName", dataSource: cacheNames, columns: columns, onRow: (record) => ({
                                onClick: () => {
                                    setCurrentCacheName(record.cacheName);
                                    form.resetFields();
                                    getCacheKeys(record.cacheName);
                                },
                            }) }) }) }), _jsx(Col, { span: 8, children: _jsx(Card, { title: "\u952E\u540D\u5217\u8868", extra: _jsx(Button, { icon: _jsx(ReloadOutlined, {}), onClick: refreshCacheKeys, type: "link" }), className: styles.card, children: _jsx(Table, { rowKey: "cacheKey", dataSource: cacheKeys, columns: cacheKeysColumns, onRow: (record) => ({
                                onClick: () => {
                                    if (!currentCacheName)
                                        return;
                                    getCacheValue(currentCacheName, record.cacheKey).then((res) => {
                                        if (res.code === 200) {
                                            form.resetFields();
                                            form.setFieldsValue({
                                                cacheName: res.data.cacheName,
                                                cacheKey: res.data.cacheKey,
                                                cacheValue: res.data.cacheValue,
                                                remark: res.data.remark,
                                            });
                                        }
                                    });
                                },
                            }) }) }) }), _jsx(Col, { span: 8, children: _jsx(Card, { title: "\u7F13\u5B58\u5185\u5BB9", extra: _jsx(Popconfirm, { title: "\u786E\u8BA4\u6E05\u7406\u5168\u90E8\u7F13\u5B58\uFF1F", description: "\u8FD9\u4F1A\u5220\u9664 Redis \u4E2D\u6240\u6709\u7F13\u5B58\uFF0C\u5305\u62EC\u5F53\u524D\u767B\u5F55 token\u3002", onConfirm: onClearAll, children: _jsx(Button, { icon: _jsx(ReloadOutlined, {}), type: "link", children: "\u6E05\u7406\u5168\u90E8" }) }), className: styles.card, children: _jsxs(ProForm, { name: "cacheContent", form: form, labelCol: { span: 8 }, wrapperCol: { span: 16 }, submitter: false, autoComplete: "off", children: [_jsx(Form.Item, { label: "\u7F13\u5B58\u540D\u79F0", name: "cacheName", children: _jsx(Input, {}) }), _jsx(Form.Item, { label: "\u7F13\u5B58\u952E\u540D", name: "cacheKey", children: _jsx(Input, {}) }), _jsx(Form.Item, { label: "\u7F13\u5B58\u5185\u5BB9", name: "cacheValue", children: _jsx(TextArea, { autoSize: { minRows: 2 } }) })] }) }) })] }) }));
};
export default CacheList;
