import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import * as React from 'react';
import CopyableIcon from './CopyableIcon';
import { useIntl } from '@umijs/max';
import styles from './style.module.css';
const Category = props => {
    const { icons, title, newIcons, theme } = props;
    const intl = useIntl();
    const [justCopied, setJustCopied] = React.useState(null);
    const copyId = React.useRef(null);
    const onSelect = React.useCallback((type, text) => {
        const { onSelect } = props;
        if (onSelect) {
            onSelect(type, text);
        }
        setJustCopied(type);
        copyId.current = setTimeout(() => {
            setJustCopied(null);
        }, 2000);
    }, []);
    React.useEffect(() => () => {
        if (copyId.current) {
            clearTimeout(copyId.current);
        }
    }, []);
    return (_jsxs("div", { children: [_jsx("h4", { children: intl.formatMessage({
                    id: `app.docs.components.icon.category.${title}`,
                    defaultMessage: '信息',
                }) }), _jsx("ul", { className: styles.anticonsList, children: icons.map(name => (_jsx(CopyableIcon, { name: name, theme: theme, isNew: newIcons.includes(name), justCopied: justCopied, onSelect: onSelect }, name))) })] }));
};
export default Category;
