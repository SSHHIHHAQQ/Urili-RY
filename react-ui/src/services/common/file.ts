import { request } from '@umijs/max';

export type CommonUploadResult = API.Result & {
  url?: string;
  fileName?: string;
  newFileName?: string;
  originalFilename?: string;
};

export async function uploadCommonFile(file: File) {
  const formData = new FormData();
  formData.append('file', file);

  return request<CommonUploadResult>('/api/common/upload', {
    method: 'POST',
    data: formData,
    requestType: 'form',
  });
}
