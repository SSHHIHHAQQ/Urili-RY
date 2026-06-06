import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import * as React from 'react';
import Icon, * as AntdIcons from '@ant-design/icons';
import { Radio, Input, Empty } from 'antd';
import Category from './Category';
import IconPicSearcher from './IconPicSearcher';
import { FilledIcon, OutlinedIcon, TwoToneIcon } from './themeIcons';
import { categories } from './fields';
// import { useIntl } from '@umijs/max';
export var ThemeType;
(function (ThemeType) {
    ThemeType["Filled"] = "Filled";
    ThemeType["Outlined"] = "Outlined";
    ThemeType["TwoTone"] = "TwoTone";
})(ThemeType || (ThemeType = {}));
const allIcons = AntdIcons;
const IconSelector = (props) => {
    // const intl = useIntl();
    // const { messages } = intl;
    const { onSelect } = props;
    const [displayState, setDisplayState] = React.useState({
        theme: ThemeType.Outlined,
        searchKey: '',
    });
    const newIconNames = [];
    const debounceTimerRef = React.useRef(undefined);
    const handleSearchIcon = React.useCallback((searchKey) => {
        clearTimeout(debounceTimerRef.current);
        debounceTimerRef.current = setTimeout(() => {
            setDisplayState(prevState => ({ ...prevState, searchKey }));
        }, 300);
    }, []);
    const handleChangeTheme = React.useCallback((e) => {
        setDisplayState(prevState => ({ ...prevState, theme: e.target.value }));
    }, []);
    const renderCategories = React.useMemo(() => {
        const { searchKey = '', theme } = displayState;
        const categoriesResult = Object.keys(categories)
            .map((key) => {
            let iconList = categories[key];
            if (searchKey) {
                const matchKey = searchKey
                    // eslint-disable-next-line prefer-regex-literals
                    .replace(new RegExp(`^<([a-zA-Z]*)\\s/>$`, 'gi'), (_, name) => name)
                    .replace(/(Filled|Outlined|TwoTone)$/, '')
                    .toLowerCase();
                iconList = iconList.filter((iconName) => iconName.toLowerCase().includes(matchKey));
            }
            // CopyrightCircle is same as Copyright, don't show it
            iconList = iconList.filter((icon) => icon !== 'CopyrightCircle');
            return {
                category: key,
                icons: iconList.map((iconName) => iconName + theme).filter((iconName) => allIcons[iconName]),
            };
        })
            .filter(({ icons }) => !!icons.length)
            .map(({ category, icons }) => (_jsx(Category, { title: category, theme: theme, icons: icons, newIcons: newIconNames, onSelect: (type, name) => {
                if (onSelect) {
                    onSelect(name, allIcons[name]);
                }
            } }, category)));
        return categoriesResult.length === 0 ? _jsx(Empty, { style: { margin: '2em 0' } }) : categoriesResult;
    }, [displayState.searchKey, displayState.theme]);
    return (_jsxs(_Fragment, { children: [_jsxs("div", { style: { display: 'flex', justifyContent: 'space-between' }, children: [_jsx(Radio.Group, { value: displayState.theme, onChange: handleChangeTheme, size: "large", optionType: "button", buttonStyle: "solid", options: [
                            {
                                label: _jsx(Icon, { component: OutlinedIcon }),
                                value: ThemeType.Outlined
                            },
                            {
                                label: _jsx(Icon, { component: FilledIcon }),
                                value: ThemeType.Filled
                            },
                            {
                                label: _jsx(Icon, { component: TwoToneIcon }),
                                value: ThemeType.TwoTone
                            },
                        ] }), _jsx(Input.Search
                    // placeholder={messages['app.docs.components.icon.search.placeholder']}
                    , {
                        // placeholder={messages['app.docs.components.icon.search.placeholder']}
                        style: { margin: '0 10px', flex: 1 }, allowClear: true, onChange: e => handleSearchIcon(e.currentTarget.value), size: "large", autoFocus: true, suffix: _jsx(IconPicSearcher, {}) })] }), renderCategories] }));
};
export default IconSelector;
