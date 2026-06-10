export function sanitizePartnerSessionPageParams(
  params?: API.Partner.PartnerSessionPageParams,
): API.Partner.PartnerSessionPageParams {
  const result: API.Partner.PartnerSessionPageParams = {};
  if (!params) {
    return result;
  }
  if (params.pageNum != null) {
    result.pageNum = params.pageNum;
  }
  if (params.pageSize != null) {
    result.pageSize = params.pageSize;
  }
  return result;
}
