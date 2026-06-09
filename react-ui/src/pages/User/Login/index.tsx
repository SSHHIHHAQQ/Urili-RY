import { getCaptchaImg, login } from '@/services/system/auth';
import {
  AlipayCircleOutlined, LockOutlined, TaobaoCircleOutlined, UserOutlined, WeiboCircleOutlined, } from '@ant-design/icons';
import {
  LoginForm, ProFormCheckbox, ProFormText, } from '@ant-design/pro-components';
import { createStyles } from 'antd-style';
import { FormattedMessage, SelectLang, useIntl, useModel, Helmet } from '@umijs/max';
import { Alert, Col, Row, Image } from 'antd';
import Settings from '../../../../config/defaultSettings';
import React, { useEffect, useState } from 'react';
import { flushSync } from 'react-dom';
import { clearSessionToken, setSessionToken } from '@/access';
import { message } from '@/utils/feedback';
import { selectInitialStateModel } from '@/utils/initialStateModel';
import { getRoutersInfo, setRemoteMenu } from '@/services/session';
import { resolveAdminRedirectFromSearch } from '@/utils/adminRedirect';

const useActionStyles = createStyles(({ token }) => ({
  icon: {
    marginLeft: '8px',
    color: 'rgba(0, 0, 0, 0.2)',
    fontSize: '24px',
    verticalAlign: 'middle',
    cursor: 'pointer',
    transition: 'color 0.3s',
    '&:hover': {
      color: token.colorPrimaryActive,
    },
  },
}));

const ActionIcons = () => {
  const { styles } = useActionStyles();
  return (
    <>
      <AlipayCircleOutlined key="AlipayCircleOutlined" className={styles.icon} />
      <TaobaoCircleOutlined key="TaobaoCircleOutlined" className={styles.icon} />
      <WeiboCircleOutlined key="WeiboCircleOutlined" className={styles.icon} />
    </>
  );
};

const useLangStyles = createStyles(({ token }) => ({
  lang: {
    width: 42,
    height: 42,
    lineHeight: '42px',
    position: 'fixed',
    right: 16,
    borderRadius: token.borderRadius,
    ':hover': {
      backgroundColor: token.colorBgTextHover,
    },
  },
}));

const Lang = () => {
  const { styles } = useLangStyles();
  return (
    <div className={styles.lang} data-lang>
      {SelectLang && <SelectLang />}
    </div>
  );
};

const LoginMessage: React.FC<{
  content: string;
}> = ({ content }) => {
  return (
    <Alert
      style={{
        marginBottom: 24,
      }}
      message={content}
      type="error"
      showIcon
    />
  );
};

const Login: React.FC = () => {
  const [userLoginState, setUserLoginState] = useState<API.LoginResult>({code: 200});
  const { initialState, setInitialState } = useModel('@@initialState', selectInitialStateModel);
  const [captchaEnabled, setCaptchaEnabled] = useState<boolean>(false);
  const [captchaCode, setCaptchaCode] = useState<string>('');
  const [uuid, setUuid] = useState<string>('');

  const { styles: containerStyles } = createStyles(() => ({
    container: {
      display: 'flex',
      flexDirection: 'column',
      height: '100vh',
      overflow: 'auto',
      backgroundImage:
        "url('https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/V-_oS6r-i7wAAAAAAAAAAAAAFl94AQBr')",
      backgroundSize: '100% 100%',
    },
  }))();

  const intl = useIntl();

  const getCaptchaCode = async () => {
    try {
      const response = await getCaptchaImg();
      const nextCaptchaEnabled = response.captchaEnabled === true && !!response.img;
      setCaptchaEnabled(nextCaptchaEnabled);
      if (!nextCaptchaEnabled) {
        setCaptchaCode('');
        setUuid('');
        return;
      }
      const imgdata = `data:image/png;base64,${response.img}`;
      setCaptchaCode(imgdata);
      setUuid(response.uuid || '');
    } catch (error) {
      console.log(error);
      setCaptchaEnabled(false);
      setCaptchaCode('');
      setUuid('');
    }
  };

  const fetchUserInfo = async () => {
    const userInfo = await initialState?.fetchUserInfo?.();
    if (userInfo) {
      flushSync(() => {
        setInitialState((s) => ({
          ...s,
          currentUser: userInfo,
        }));
      });
    }
  };

  const handleSubmit = async (values: API.LoginParams) => {
    try {
      // 登录
      const response = await login(
        captchaEnabled
          ? { ...values, uuid }
          : { ...values, code: undefined, uuid: undefined },
      );
      if (response.code === 200) {
        const defaultLoginSuccessMessage = intl.formatMessage({
          id: 'pages.login.success',
          defaultMessage: '登录成功！',
        });
        const current = new Date();
        const expireTime = current.setTime(current.getTime() + 1000 * 12 * 60 * 60);
        console.log('login response: ', response);
        setSessionToken(response?.token, response?.token, expireTime);
        message.success(defaultLoginSuccessMessage);
        const routers = await getRoutersInfo();
        setRemoteMenu(routers);
        await fetchUserInfo();
        console.log('login ok');
        window.location.replace(resolveAdminRedirectFromSearch(window.location.search));
        return;
      } else {
        console.log(response.msg);
        clearSessionToken();
        // 如果失败去设置用户错误信息
        setUserLoginState({ ...response, type: 'account' });
        if (captchaEnabled) {
          getCaptchaCode();
        }
      }
    } catch (error) {
      const defaultLoginFailureMessage = intl.formatMessage({
        id: 'pages.login.failure',
        defaultMessage: '登录失败，请重试！',
      });
      console.log(error);
      message.error(defaultLoginFailureMessage);
    }
  };
  const { code } = userLoginState;

  useEffect(() => {
    getCaptchaCode();
  }, []);

  return (
    <div className={containerStyles.container}>
      <Helmet>
        <title>
          {intl.formatMessage({
            id: 'menu.login',
            defaultMessage: '登录页',
          })}
          - {Settings.title}
        </title>
      </Helmet>
      <Lang />
      <div
        style={{
          flex: '1',
          padding: '32px 0',
        }}
      >
        <LoginForm
          contentStyle={{
            minWidth: 280,
            maxWidth: '75vw',
          }}
          logo={<img alt="logo" src="/logo.svg" />}
          title="Ant Design"
          subTitle={intl.formatMessage({ id: 'pages.layouts.userLayout.title' })}
          initialValues={{
            autoLogin: true,
          }}
          actions={[
            <FormattedMessage
              key="loginWith"
              id="pages.login.loginWith"
              defaultMessage="其他登录方式"
            />,
            <ActionIcons key="icons" />,
          ]}
          onFinish={async (values) => {
            await handleSubmit(values as API.LoginParams);
          }}
        >
          {code !== 200 && (
            <LoginMessage
              content={intl.formatMessage({
                id: 'pages.login.accountLogin.errorMessage',
                defaultMessage: '账户或密码错误(admin/admin123)',
              })}
            />
          )}
          <ProFormText
            name="username"
            initialValue="admin"
            fieldProps={{
              size: 'large',
              prefix: <UserOutlined />,
            }}
            placeholder={intl.formatMessage({
              id: 'pages.login.username.placeholder',
              defaultMessage: '用户名: admin',
            })}
            rules={[
              {
                required: true,
                message: (
                  <FormattedMessage
                    id="pages.login.username.required"
                    defaultMessage="请输入用户名!"
                  />
                ),
              },
            ]}
          />
          <ProFormText.Password
            name="password"
            initialValue="admin123"
            fieldProps={{
              size: 'large',
              prefix: <LockOutlined />,
            }}
            placeholder={intl.formatMessage({
              id: 'pages.login.password.placeholder',
              defaultMessage: '密码: admin123',
            })}
            rules={[
              {
                required: true,
                message: (
                  <FormattedMessage
                    id="pages.login.password.required"
                    defaultMessage="请输入密码！"
                  />
                ),
              },
            ]}
          />
          {captchaEnabled && (
            <Row>
              <Col flex={3}>
                <ProFormText
                  style={{
                    float: 'right',
                  }}
                  name="code"
                  placeholder={intl.formatMessage({
                    id: 'pages.login.captcha.placeholder',
                    defaultMessage: '请输入验证',
                  })}
                  rules={[
                    {
                      required: true,
                      message: (
                        <FormattedMessage
                          id="pages.searchTable.updateForm.ruleName.nameRules"
                          defaultMessage="请输入验证啊"
                        />
                      ),
                    },
                  ]}
                />
              </Col>
              <Col flex={2}>
                <Image
                  src={captchaCode || undefined}
                  alt="验证码"
                  style={{
                    display: 'inline-block',
                    verticalAlign: 'top',
                    cursor: 'pointer',
                    paddingLeft: '10px',
                    width: '100px',
                  }}
                  preview={false}
                  onClick={() => getCaptchaCode()}
                />
              </Col>
            </Row>
          )}
          <div
            style={{
              marginBottom: 24,
            }}
          >
            <ProFormCheckbox noStyle name="autoLogin">
              <FormattedMessage id="pages.login.rememberMe" defaultMessage="自动登录" />
            </ProFormCheckbox>
            <a
              style={{
                float: 'right',
              }}
            >
              <FormattedMessage id="pages.login.forgotPassword" defaultMessage="忘记密码" />
            </a>
          </div>
        </LoginForm>
      </div>
    </div>
  );
};

export default Login;
