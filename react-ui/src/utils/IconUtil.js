import * as AntdIcons from '@ant-design/icons';
import React from 'react';
const allIcons = AntdIcons;
export function getIcon(name) {
    const icon = allIcons[name];
    return icon || '';
}
export function createIcon(icon) {
    if (typeof icon === 'object') {
        return icon;
    }
    const ele = allIcons[icon];
    if (ele) {
        return React.createElement(allIcons[icon]);
    }
    return '';
}
