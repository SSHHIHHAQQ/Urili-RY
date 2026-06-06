import { jsx as _jsx } from "react/jsx-runtime";
import { PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Tabs } from 'antd';
import AttributeLibrary from './components/AttributeLibrary';
import CategoryAttributeTemplate from './components/CategoryAttributeTemplate';
export default function ProductAttributePage() {
    const access = useAccess();
    return (_jsx(PageContainer, { title: false, children: _jsx(Tabs, { items: [
                {
                    key: 'attribute',
                    label: '属性库',
                    children: _jsx(AttributeLibrary, { access: access }),
                },
                {
                    key: 'categoryAttribute',
                    label: '类目属性模板',
                    children: _jsx(CategoryAttributeTemplate, { access: access }),
                },
            ] }) }));
}
