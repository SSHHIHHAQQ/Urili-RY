import { jsx as _jsx } from "react/jsx-runtime";
import { history } from '@umijs/max';
import { Button, Result } from 'antd';
const NoFoundPage = () => (_jsx(Result, { status: "404", title: "404", subTitle: "Sorry, the page you visited does not exist.", extra: _jsx(Button, { type: "primary", onClick: () => history.push('/'), children: "Back Home" }) }));
export default NoFoundPage;
