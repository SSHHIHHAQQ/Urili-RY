// @ts-ignore
/* eslint-disable */
import { request } from '@umijs/max';
/** 登录接口 POST /api/login/account */
export async function login(body, options) {
    return request('/api/login/account', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        data: body,
        ...(options || {}),
    });
}
/** 发送验证码 POST /api/login/captcha */
export async function getFakeCaptcha(
// 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
params, options) {
    return request('/api/login/captcha', {
        method: 'POST',
        params: {
            ...params,
        },
        ...(options || {}),
    });
}
/** 登录接口 POST /api/login/outLogin */
export async function outLogin(options) {
    return request('/api/login/outLogin', {
        method: 'POST',
        ...(options || {}),
    });
}
