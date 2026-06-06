import { jsx as _jsx } from "react/jsx-runtime";
import { PageContainer } from '@ant-design/pro-components';
import { Card, Typography } from 'antd';
const { Text } = Typography;
export default function PlannedPage() {
    return (_jsx(PageContainer, { children: _jsx(Card, { children: _jsx(Text, { type: "secondary", children: "\u529F\u80FD\u89C4\u5212\u4E2D" }) }) }));
}
