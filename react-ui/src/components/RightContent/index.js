import { jsx as _jsx } from "react/jsx-runtime";
import { QuestionCircleOutlined } from '@ant-design/icons';
import { SelectLang as UmiSelectLang } from '@umijs/max';
export const SelectLang = () => {
    return (_jsx(UmiSelectLang, { style: {
            padding: 4,
        } }));
};
export const Question = () => {
    return (_jsx("div", { style: {
            display: 'flex',
            height: 26,
        }, onClick: () => {
            window.open('https://pro.ant.design/docs/getting-started');
        }, children: _jsx(QuestionCircleOutlined, {}) }));
};
