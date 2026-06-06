import { jsx as _jsx } from "react/jsx-runtime";
import { Tag } from 'antd';
const DictTag = (props) => {
    function getDictColor(type) {
        switch (type) {
            case 'primary':
                return 'blue';
            case 'success':
                return 'success';
            case 'info':
                return 'green';
            case 'warning':
                return 'warning';
            case 'danger':
                return 'error';
            case 'default':
            default:
                return 'default';
        }
    }
    function getDictLabelByValue(value) {
        if (value === undefined) {
            return '';
        }
        if (props.enums) {
            const item = props.enums[value];
            return item.label;
        }
        if (props.options) {
            if (!Array.isArray(props.options)) {
                console.log('DictTag options is no array!');
                return '';
            }
            for (const item of props.options) {
                if (item.value === value) {
                    return item.text;
                }
            }
        }
        return String(props.value);
    }
    function getDictListClassByValue(value) {
        if (value === undefined) {
            return 'default';
        }
        if (props.enums) {
            const item = props.enums[value];
            return item.listClass || 'default';
        }
        if (props.options) {
            if (!Array.isArray(props.options)) {
                console.log('DictTag options is no array!');
                return 'default';
            }
            for (const item of props.options) {
                if (item.value === value) {
                    return item.listClass || 'default';
                }
            }
        }
        return String(props.value);
    }
    const getTagColor = () => {
        return getDictColor(getDictListClassByValue(props.value).toLowerCase());
    };
    const getTagText = () => {
        return getDictLabelByValue(props.value);
    };
    return (_jsx(Tag, { color: getTagColor(), children: getTagText() }));
};
export default DictTag;
