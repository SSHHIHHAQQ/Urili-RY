/**
 * @description: Request result set
 */
export var HttpResult;
(function (HttpResult) {
    HttpResult[HttpResult["SUCCESS"] = 200] = "SUCCESS";
    HttpResult[HttpResult["ERROR"] = -1] = "ERROR";
    HttpResult[HttpResult["TIMEOUT"] = 401] = "TIMEOUT";
    HttpResult["TYPE"] = "success";
})(HttpResult || (HttpResult = {}));
/**
 * @description: request method
 */
export var RequestMethod;
(function (RequestMethod) {
    RequestMethod["GET"] = "GET";
    RequestMethod["POST"] = "POST";
    RequestMethod["PUT"] = "PUT";
    RequestMethod["DELETE"] = "DELETE";
})(RequestMethod || (RequestMethod = {}));
/**
 * @description:  contentType
 */
export var ContentType;
(function (ContentType) {
    // json
    ContentType["JSON"] = "application/json;charset=UTF-8";
    // form-data qs
    ContentType["FORM_URLENCODED"] = "application/x-www-form-urlencoded;charset=UTF-8";
    // form-data  upload
    ContentType["FORM_DATA"] = "multipart/form-data;charset=UTF-8";
})(ContentType || (ContentType = {}));
