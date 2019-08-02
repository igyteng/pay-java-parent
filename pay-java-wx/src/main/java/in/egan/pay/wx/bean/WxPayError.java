/*
 * Copyright 2002-2017 the original huodull or egan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package in.egan.pay.wx.bean;

import com.alibaba.fastjson.JSONObject;
import in.egan.pay.common.bean.result.PayError;

/**
 * @author: egan
 * @email egzosn@gmail.com
 * @date 2017/3/6 17:31
 */
public class WxPayError implements PayError {

    private String errorCode;

    private String errorMsg;
    private String content;


    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getErrorMsg() {
        return errorMsg;
    }

    public WxPayError(String errorCode, String errorMsg) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public WxPayError(String errorCode, String errorMsg, String content) {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
        this.content = content;
    }

    @Override
    public String getString() {
            return "支付错误: errcode=" + errorCode + ", errmsg=" + errorMsg + (null == content ? "" : "\n content:" + content);
    }
}
