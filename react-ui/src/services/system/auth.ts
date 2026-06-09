import { request } from '@umijs/max';

export type CaptchaImageResult = {
  captchaEnabled?: boolean;
  uuid?: string;
  img?: string;
  code?: number;
  msg?: string;
};

export async function getCaptchaImg(params?: Record<string, any>, options?: Record<string, any>) {
  return request<CaptchaImageResult>('/api/captchaImage', {
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

/** 登录接口 POST /api/login */
export async function login(body: API.LoginParams, options?: Record<string, any>) {
  return request<API.LoginResult>('/api/login', {
    method: 'POST',
    headers: {
      isToken: false,
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  });
}

/** 退出登录接口 DELETE /api/logout */
export async function logout() {
  return request<Record<string, any>>('/api/logout', {
    method: 'delete',
  });
}
