import { request } from '@umijs/max';
export async function getCaptchaImg(params, options) {
    return request('/api/captchaImage', {
        method: 'GET',
        params: {
            ...params,
        },
        headers: {
            isToken: false,
        },
        ...(options || {}),
    });
}
/** 登录接口 POST /api/login/account */
export async function login(body, options) {
    return request('/api/login', {
        method: 'POST',
        headers: {
            isToken: false,
            'Content-Type': 'application/json',
        },
        data: body,
        ...(options || {}),
    });
}
/** 退出登录接口 POST /api/login/outLogin */
export async function logout() {
    return request('/api/logout', {
        method: 'delete',
    });
}
// 获取手机验证码
export async function getMobileCaptcha(mobile) {
    return request(`/api/login/captcha?mobile=${mobile}`);
}
