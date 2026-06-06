import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect, useState } from 'react';
import BaseInfo from './components/BaseInfo';
import { Card, Layout, Steps } from 'antd';
import ColumnInfo from './components/ColumnInfo';
import GenInfo from './components/GenInfo';
import { getGenCode, updateData } from './service';
import { formatTreeData } from '@/utils/tree';
import styles from './style.module.css';
import { getMenuTree } from '@/services/system/menu';
import { getDictTypeList } from '@/services/system/dict';
import queryString from 'query-string';
import { useLocation } from '@umijs/max';
import { message } from '@/utils/feedback';
const { Content } = Layout;
const TableList = () => {
    const location = useLocation();
    const query = queryString.parse(location.search);
    const { id } = query;
    const tableId = id;
    const [currentStep, setCurrentStep] = useState(0);
    const [columnData, setColumnData] = useState([]);
    const [baseInfoData, setBaseInfoData] = useState([]);
    const [genInfoData, setGenInfoData] = useState([]);
    const [menuTree, setMenuTree] = useState([]);
    const [dictData, setDictData] = useState([]);
    const [tableInfo, setTableInfo] = useState([]);
    const [formData, setFormData] = useState([]);
    const [stepComponent, setStepComponent] = useState([]);
    const [stepKey, setStepKey] = useState('');
    const getCurrentStepAndComponent = (key) => {
        if (key === 'base') {
            return (_jsx(BaseInfo, { values: baseInfoData,
                // eslint-disable-next-line @typescript-eslint/no-use-before-define
                onStepSubmit: onNextStep }));
        }
        if (key === 'column') {
            return (_jsx(ColumnInfo, { data: columnData, dictData: dictData,
                // eslint-disable-next-line @typescript-eslint/no-use-before-define
                onStepSubmit: onNextStep }));
        }
        if (key === 'gen') {
            return (_jsx(GenInfo, { values: genInfoData, menuData: menuTree, tableInfo: tableInfo,
                // eslint-disable-next-line @typescript-eslint/no-use-before-define
                onStepSubmit: onNextStep }));
        }
        return null;
    };
    const onNextStep = (step, values, direction) => {
        let stepKey = 'base';
        if (step === 'base') {
            setStepKey('column');
            setCurrentStep(1);
            setFormData(values);
            setStepComponent(getCurrentStepAndComponent(stepKey));
        }
        else if (step === 'column') {
            if (direction === 'prev') {
                setStepKey('base');
                setCurrentStep(0);
            }
            else {
                setStepKey('gen');
                const tableData = formData || {};
                tableData.columns = values;
                setCurrentStep(2);
                setFormData(tableData);
            }
            setStepComponent(getCurrentStepAndComponent(stepKey));
        }
        else if (step === 'gen') {
            if (direction === 'prev') {
                setStepKey('column');
                setCurrentStep(1);
                setStepComponent(getCurrentStepAndComponent(stepKey));
            }
            else {
                const postData = {
                    ...formData,
                    ...values,
                    params: values,
                    tableId: tableId,
                };
                setFormData(postData);
                updateData({ ...postData }).then((res) => {
                    if (res.code === 200) {
                        message.success('提交成功');
                        history.back();
                    }
                    else {
                        message.success('提交失败');
                    }
                });
            }
        }
    };
    useEffect(() => {
        setStepComponent(getCurrentStepAndComponent(stepKey));
    }, [stepKey]);
    useEffect(() => {
        getGenCode(tableId).then((res) => {
            if (res.code === 200) {
                setBaseInfoData(res.data.info);
                setColumnData(res.data.rows);
                setGenInfoData(res.data.info);
                setTableInfo(res.data.tables);
                setStepKey('base');
            }
            else {
                message.error(res.msg);
            }
        });
        getMenuTree().then((res) => {
            if (res.code === 200) {
                const treeData = formatTreeData(res.data);
                setMenuTree(treeData);
            }
            else {
                message.error(res.msg);
            }
        });
        getDictTypeList().then((res) => {
            if (res.code === 200) {
                const dicts = (res.rows ?? []).map((item) => {
                    return {
                        label: item.dictName,
                        value: item.dictType,
                    };
                });
                setDictData(dicts);
            }
            else {
                message.error(res.msg);
            }
        });
    }, []);
    // const onFinish = (values: any) => {
    //   console.log('Success:', values);
    // };
    // const onFinishFailed = (errorInfo: any) => {
    //   console.log('Failed:', errorInfo);
    // };
    return (_jsx(Content, { children: _jsxs(Card, { className: styles.tabsCard, variant: "borderless", children: [_jsx(Steps, { current: currentStep, className: styles.steps, items: [
                        {
                            title: '基本信息',
                        },
                        {
                            title: '字段信息',
                        },
                        {
                            title: '生成信息',
                        },
                    ] }), stepComponent] }) }));
};
export default TableList;
