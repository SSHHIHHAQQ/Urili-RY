import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useState } from 'react';
import { getServerInfo } from '@/services/monitor/server';
import { Card, Col, Row, Table } from 'antd';
import { FormattedMessage } from '@umijs/max';
import styles from './style.module.css';
/* *
 *
 * @author whiteshader@163.com
 * @datetime  2023/02/07
 *
 * */
const columns = [
    {
        title: '属性',
        dataIndex: 'name',
        key: 'name',
    },
    {
        title: '值',
        dataIndex: 'value',
        key: 'value',
    },
];
const memColumns = [
    {
        title: '属性',
        dataIndex: 'name',
        key: 'name',
    },
    {
        title: '内存',
        dataIndex: 'mem',
        key: 'mem',
    },
    {
        title: 'JVM',
        dataIndex: 'jvm',
        key: 'jvm',
    },
];
const hostColumns = [
    {
        title: 'col1',
        dataIndex: 'col1',
        key: 'col1',
    },
    {
        title: 'col2',
        dataIndex: 'col2',
        key: 'col2',
    },
    {
        title: 'col3',
        dataIndex: 'col3',
        key: 'col3',
    },
    {
        title: 'col4',
        dataIndex: 'col4',
        key: 'col4',
    },
];
const diskColumns = [
    {
        title: _jsx(FormattedMessage, { id: "monitor.server.disk.dirName", defaultMessage: "\u76D8\u7B26\u8DEF\u5F84" }),
        dataIndex: 'dirName',
        key: 'dirName',
    },
    {
        title: _jsx(FormattedMessage, { id: "monitor.server.disk.sysTypeName", defaultMessage: "\u6587\u4EF6\u7CFB\u7EDF" }),
        dataIndex: 'sysTypeName',
        key: 'sysTypeName',
    },
    {
        title: _jsx(FormattedMessage, { id: "monitor.server.disk.typeName", defaultMessage: "\u76D8\u7B26\u7C7B\u578B" }),
        dataIndex: 'typeName',
        key: 'typeName',
    },
    {
        title: _jsx(FormattedMessage, { id: "monitor.server.disk.total", defaultMessage: "\u603B\u5927\u5C0F" }),
        dataIndex: 'total',
        key: 'total',
    },
    {
        title: _jsx(FormattedMessage, { id: "monitor.server.disk.free", defaultMessage: "\u53EF\u7528\u5927\u5C0F" }),
        dataIndex: 'free',
        key: 'free',
    },
    {
        title: _jsx(FormattedMessage, { id: "monitor.server.disk.used", defaultMessage: "\u5DF2\u7528\u5927\u5C0F" }),
        dataIndex: 'used',
        key: 'used',
    },
    {
        title: _jsx(FormattedMessage, { id: "monitor.server.disk.usage", defaultMessage: "\u5DF2\u7528\u767E\u5206\u6BD4" }),
        dataIndex: 'usage',
        key: 'usage',
    },
];
const ServerInfo = () => {
    const [cpuData, setCpuData] = useState([]);
    const [memData, setMemData] = useState([]);
    const [hostData, setHostData] = useState([]);
    const [jvmData, setJvmData] = useState([]);
    const [diskData, setDiskData] = useState([]);
    useEffect(() => {
        getServerInfo().then((res) => {
            if (res.code === 200) {
                // const cpuinfo: CpuRowType[] = [];
                // Object.keys(res.data.cpu).forEach((item: any) => {
                //   cpuinfo.push({
                //     name: item,
                //     value: res.data.cpu[item],
                //   });
                // });
                // setCpuData(cpuinfo);
                const cpuinfo = [];
                cpuinfo.push({ name: '核心数', value: res.data.cpu.cpuNum });
                cpuinfo.push({ name: '用户使用率', value: `${res.data.cpu.used}%` });
                cpuinfo.push({ name: '系统使用率', value: `${res.data.cpu.sys}%` });
                cpuinfo.push({ name: '当前空闲率', value: `${res.data.cpu.free}%` });
                setCpuData(cpuinfo);
                const memDatas = [];
                memDatas.push({
                    name: '总内存',
                    mem: `${res.data.mem.total}G`,
                    jvm: `${res.data.jvm.total}M`,
                });
                memDatas.push({
                    name: '已用内存',
                    mem: `${res.data.mem.used}G`,
                    jvm: `${res.data.jvm.used}M`,
                });
                memDatas.push({
                    name: '剩余内存',
                    mem: `${res.data.mem.free}G`,
                    jvm: `${res.data.jvm.free}M`,
                });
                memDatas.push({
                    name: '使用率',
                    mem: `${res.data.mem.usage}%`,
                    jvm: `${res.data.jvm.usage}%`,
                });
                setMemData(memDatas);
                const hostinfo = [];
                hostinfo.push({
                    col1: '服务器名称',
                    col2: res.data.sys.computerName,
                    col3: '操作系统',
                    col4: res.data.sys.osName,
                });
                hostinfo.push({
                    col1: '服务器IP',
                    col2: res.data.sys.computerIp,
                    col3: '系统架构',
                    col4: res.data.sys.osArch,
                });
                setHostData(hostinfo);
                const jvminfo = [];
                jvminfo.push({
                    col1: 'Java名称',
                    col2: res.data.jvm.name,
                    col3: 'Java版本',
                    col4: res.data.jvm.version,
                });
                jvminfo.push({
                    col1: '启动时间',
                    col2: res.data.jvm.startTime,
                    col3: '运行时长',
                    col4: res.data.jvm.runTime,
                });
                jvminfo.push({
                    col1: '安装路径',
                    col2: res.data.jvm.home,
                    col3: '项目路径',
                    col4: res.data.sys.userDir,
                });
                setJvmData(jvminfo);
                const diskinfo = res.data.sysFiles.map((item) => {
                    return {
                        dirName: item.dirName,
                        sysTypeName: item.sysTypeName,
                        typeName: item.typeName,
                        total: item.total,
                        free: item.free,
                        used: item.used,
                        usage: `${item.usage}%`,
                    };
                });
                setDiskData(diskinfo);
            }
        });
    }, []);
    return (_jsxs("div", { children: [_jsxs(Row, { gutter: [24, 24], children: [_jsx(Col, { span: 12, children: _jsx(Card, { title: "CPU", className: styles.card, children: _jsx(Table, { rowKey: "name", pagination: false, showHeader: false, dataSource: cpuData, columns: columns }) }) }), _jsx(Col, { span: 12, children: _jsx(Card, { title: "\u5185\u5B58", className: styles.card, children: _jsx(Table, { rowKey: "name", pagination: false, showHeader: false, dataSource: memData, columns: memColumns }) }) })] }), _jsx(Row, { gutter: [16, 16], children: _jsx(Col, { span: 24, children: _jsx(Card, { title: "\u670D\u52A1\u5668\u4FE1\u606F", className: styles.card, children: _jsx(Table, { rowKey: "col1", pagination: false, showHeader: false, dataSource: hostData, columns: hostColumns }) }) }) }), _jsx(Row, { gutter: [16, 16], children: _jsx(Col, { span: 24, children: _jsx(Card, { title: "Java\u865A\u62DF\u673A\u4FE1\u606F", className: styles.card, children: _jsx(Table, { rowKey: "col1", pagination: false, showHeader: false, dataSource: jvmData, columns: hostColumns }) }) }) }), _jsx(Row, { gutter: [16, 16], children: _jsx(Col, { span: 24, children: _jsx(Card, { title: "\u78C1\u76D8\u72B6\u6001", className: styles.card, children: _jsx(Table, { rowKey: "dirName", pagination: false, dataSource: diskData, columns: diskColumns }) }) }) })] }));
};
export default ServerInfo;
