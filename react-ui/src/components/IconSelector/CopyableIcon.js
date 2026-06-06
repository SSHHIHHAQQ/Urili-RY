import { jsx as _jsx } from "react/jsx-runtime";
import * as React from 'react';
import { Tooltip } from 'antd';
import clsx from 'clsx';
import * as AntdIcons from '@ant-design/icons';
import styles from './style.module.css';
const allIcons = AntdIcons;
const CopyableIcon = ({ name, justCopied, onSelect, theme, }) => {
    const className = clsx({
        copied: justCopied === name,
        [theme]: !!theme,
    });
    return (_jsx("li", { className: className, onClick: () => {
            if (onSelect) {
                onSelect(theme, name);
            }
        }, children: _jsx(Tooltip, { title: name, children: React.createElement(allIcons[name], { className: styles.anticon }) }) }));
};
export default CopyableIcon;
