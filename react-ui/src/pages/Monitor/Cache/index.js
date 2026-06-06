import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useMemo, useState } from 'react';
import { Card, Col, Empty, Progress, Row, Spin, Table, Typography } from 'antd';
import styles from './index.module.css';
import { getCacheInfo } from '@/services/monitor/cache';
const baseInfoColumns = [
    { title: '字段一', dataIndex: 'col1', key: 'col1' },
    { title: '值一', dataIndex: 'col2', key: 'col2' },
    { title: '字段二', dataIndex: 'col3', key: 'col3' },
    { title: '值二', dataIndex: 'col4', key: 'col4' },
    { title: '字段三', dataIndex: 'col5', key: 'col5' },
    { title: '值三', dataIndex: 'col6', key: 'col6' },
    { title: '字段四', dataIndex: 'col7', key: 'col7' },
    { title: '值四', dataIndex: 'col8', key: 'col8' },
];
const toNumber = (value) => {
    const numberValue = Number(value);
    return Number.isFinite(numberValue) ? numberValue : 0;
};
const clampPercent = (value) => Math.max(0, Math.min(100, value));
const formatPercent = (value) => `${(value || 0).toFixed(2)}%`;
const CacheInfo = () => {
    const [loading, setLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState('');
    const [cacheInfo, setCacheInfo] = useState();
    useEffect(() => {
        let mounted = true;
        setLoading(true);
        setErrorMessage('');
        getCacheInfo()
            .then((res) => {
            if (!mounted)
                return;
            if (res.code !== 200 || !res.data) {
                setErrorMessage(res.msg || '缓存监控数据获取失败');
                return;
            }
            setCacheInfo(res.data);
        })
            .catch((error) => {
            if (!mounted)
                return;
            setErrorMessage(error?.message || '缓存监控数据获取失败');
        })
            .finally(() => {
            if (mounted) {
                setLoading(false);
            }
        });
        return () => {
            mounted = false;
        };
    }, []);
    const baseInfoData = useMemo(() => {
        const info = cacheInfo?.info;
        if (!info)
            return [];
        return [
            {
                key: 'server',
                col1: 'Redis 版本',
                col2: info.redis_version || '-',
                col3: '运行模式',
                col4: info.redis_mode === 'standalone' ? '单机' : '集群',
                col5: '端口',
                col6: info.tcp_port || '-',
                col7: '客户端数',
                col8: info.connected_clients || '-',
            },
            {
                key: 'runtime',
                col1: '运行时间(天)',
                col2: info.uptime_in_days || '0',
                col3: '使用内存',
                col4: info.used_memory_human || '-',
                col5: '使用 CPU',
                col6: `${info.used_cpu_user_children || 0}%`,
                col7: '内存配置',
                col8: info.maxmemory_human || '-',
            },
            {
                key: 'persistence',
                col1: 'AOF 是否开启',
                col2: info.aof_enabled === '0' ? '否' : '是',
                col3: 'RDB 是否成功',
                col4: info.rdb_last_bgsave_status || '-',
                col5: 'Key 数量',
                col6: cacheInfo?.dbSize ?? 0,
                col7: '网络入口/出口',
                col8: `${info.instantaneous_input_kbps || 0}/${info.instantaneous_output_kbps || 0} kps`,
            },
        ];
    }, [cacheInfo]);
    const commandRows = useMemo(() => {
        const commandStats = cacheInfo?.commandStats || [];
        const rows = commandStats.map((item) => ({
            key: item.name,
            name: item.name,
            value: toNumber(item.value),
            percent: 0,
        }));
        const maxValue = Math.max(...rows.map((row) => row.value), 0);
        return rows
            .map((row) => ({
            ...row,
            percent: maxValue > 0 ? clampPercent((row.value / maxValue) * 100) : 0,
        }))
            .sort((left, right) => right.value - left.value);
    }, [cacheInfo]);
    const memoryUsagePercent = useMemo(() => {
        const info = cacheInfo?.info;
        if (!info)
            return 0;
        const usedMemory = toNumber(info.used_memory);
        const totalMemory = toNumber(info.total_system_memory);
        if (totalMemory <= 0)
            return 0;
        return clampPercent((usedMemory / totalMemory) * 100);
    }, [cacheInfo]);
    const commandColumns = [
        {
            title: '命令',
            dataIndex: 'name',
            key: 'name',
        },
        {
            title: '调用次数',
            dataIndex: 'value',
            key: 'value',
            width: 120,
        },
        {
            title: '占比',
            dataIndex: 'percent',
            key: 'percent',
            render: (_, record) => (_jsx(Progress, { percent: Number(record.percent.toFixed(2)), size: "small" })),
        },
    ];
    if (loading) {
        return (_jsx(Card, { className: styles.card, children: _jsx(Spin, {}) }));
    }
    if (errorMessage) {
        return (_jsx(Card, { className: styles.card, children: _jsx(Empty, { description: errorMessage }) }));
    }
    return (_jsxs("div", { children: [_jsx(Row, { gutter: [24, 24], children: _jsx(Col, { span: 24, children: _jsx(Card, { title: "\u57FA\u672C\u4FE1\u606F", className: styles.card, children: _jsx(Table, { rowKey: "key", pagination: false, showHeader: false, dataSource: baseInfoData, columns: baseInfoColumns }) }) }) }), _jsxs(Row, { gutter: [24, 24], children: [_jsx(Col, { span: 12, children: _jsx(Card, { title: "\u547D\u4EE4\u7EDF\u8BA1", className: styles.card, children: _jsx(Table, { rowKey: "key", pagination: false, dataSource: commandRows, columns: commandColumns }) }) }), _jsx(Col, { span: 12, children: _jsx(Card, { title: "\u5185\u5B58\u4FE1\u606F", className: styles.card, children: _jsxs("div", { style: { textAlign: 'center', padding: '24px 0' }, children: [_jsx(Progress, { type: "dashboard", percent: Number(memoryUsagePercent.toFixed(2)), format: formatPercent }), _jsx(Typography.Title, { level: 4, style: { marginTop: 16 }, children: cacheInfo?.info.used_memory_human || '-' }), _jsxs(Typography.Text, { type: "secondary", children: ["\u603B\u7CFB\u7EDF\u5185\u5B58\uFF1A", cacheInfo?.info.total_system_memory_human || '-'] })] }) }) })] })] }));
};
export default CacheInfo;
