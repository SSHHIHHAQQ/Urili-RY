import type { Dispatch, SetStateAction } from 'react';
import type { Settings as LayoutSettings } from '@ant-design/pro-components';

export type InitialStateData = {
  settings?: Partial<LayoutSettings>;
  currentUser?: API.CurrentUser;
  loading?: boolean;
  fetchUserInfo?: () => Promise<API.CurrentUser | undefined>;
};

export type InitialStateModel = {
  initialState?: InitialStateData;
  setInitialState: Dispatch<SetStateAction<InitialStateData | undefined>>;
  loading?: boolean;
  refresh?: () => void;
};

export function selectInitialStateModel(model: unknown) {
  return model as InitialStateModel;
}
