package com.egzosn.pay.wx.v3.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;

import static com.egzosn.pay.wx.api.WxConst.OUT_TRADE_NO;
import static com.egzosn.pay.wx.api.WxConst.RETURN_CODE;
import static com.egzosn.pay.wx.api.WxConst.RETURN_MSG_CODE;
import static com.egzosn.pay.wx.api.WxConst.SANDBOXNEW;
import static com.egzosn.pay.wx.api.WxConst.SUCCESS;
import static com.egzosn.pay.wx.v3.utils.WxConst.FAILURE;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.egzosn.pay.common.api.BasePayService;
import com.egzosn.pay.common.bean.BillType;
import com.egzosn.pay.common.bean.CurType;
import com.egzosn.pay.common.bean.MethodType;
import com.egzosn.pay.common.bean.NoticeParams;
import com.egzosn.pay.common.bean.NoticeRequest;
import com.egzosn.pay.common.bean.OrderParaStructure;
import com.egzosn.pay.common.bean.PayMessage;
import com.egzosn.pay.common.bean.PayOrder;
import com.egzosn.pay.common.bean.PayOutMessage;
import com.egzosn.pay.common.bean.RefundOrder;
import com.egzosn.pay.common.bean.TransactionType;
import com.egzosn.pay.common.bean.TransferOrder;
import com.egzosn.pay.common.bean.result.PayException;
import com.egzosn.pay.common.exception.PayErrorException;
import com.egzosn.pay.common.http.HttpConfigStorage;
import com.egzosn.pay.common.http.ResponseEntity;
import com.egzosn.pay.common.http.UriVariables;
import com.egzosn.pay.common.util.DateUtils;
import com.egzosn.pay.common.util.IOUtils;
import com.egzosn.pay.common.util.MapGen;
import com.egzosn.pay.common.util.Util;
import com.egzosn.pay.common.util.sign.SignTextUtils;
import com.egzosn.pay.common.util.sign.SignUtils;
import com.egzosn.pay.common.util.sign.encrypt.RSA2;
import com.egzosn.pay.common.util.str.StringUtils;
import com.egzosn.pay.wx.bean.WxPayError;
import com.egzosn.pay.wx.v3.bean.response.WxPayMessage;
import com.egzosn.pay.wx.bean.WxTransferType;
import com.egzosn.pay.wx.v3.bean.WxAccountType;
import com.egzosn.pay.wx.v3.bean.WxBillType;
import com.egzosn.pay.wx.v3.bean.WxTransactionType;
import com.egzosn.pay.wx.v3.bean.order.Amount;
import com.egzosn.pay.wx.v3.bean.order.RefundAmount;
import com.egzosn.pay.wx.v3.bean.response.Resource;
import com.egzosn.pay.wx.v3.bean.response.WxNoticeParams;
import com.egzosn.pay.wx.v3.bean.response.WxRefundResult;
import com.egzosn.pay.wx.v3.utils.AntCertificationUtil;
import com.egzosn.pay.wx.v3.utils.WxConst;

/**
 * 微信支付服务
 *
 * @author egan
 * <pre>
 * email egzosn@gmail.com
 * date 2016-5-18 14:09:01
 * </pre>
 */
public class WxPayService extends BasePayService<WxPayConfigStorage> {


    /**
     * api服务地址，默认为国内
     */
    private String apiServerUrl = WxConst.URI;


    /**
     * 辅助api
     */
    private volatile WxPayAssistService wxPayAssistService;

    /**
     * 微信参数构造器
     */
    private volatile WxParameterStructure wxParameterStructure;


    public WxPayAssistService getAssistService() {
        if (null == wxPayAssistService) {
            wxPayAssistService = new DefaultWxPayAssistService(this);
            //在这预先进行初始化
            wxPayAssistService.refreshCertificate();
        }
        return wxPayAssistService;
    }

    public void setAssistService(WxPayAssistService wxPayAssistService) {
        this.wxPayAssistService = wxPayAssistService;
    }

    /**
     * 创建支付服务
     *
     * @param payConfigStorage 微信对应的支付配置
     */
    public WxPayService(WxPayConfigStorage payConfigStorage) {
        super(payConfigStorage);
    }

    /**
     * 创建支付服务
     *
     * @param payConfigStorage 微信对应的支付配置
     * @param configStorage    微信对应的网络配置，包含代理配置、ssl证书配置
     */
    public WxPayService(WxPayConfigStorage payConfigStorage, HttpConfigStorage configStorage) {
        super(payConfigStorage, configStorage);
    }


    /**
     * 初始化之后执行
     */
    @Override
    protected void initAfter() {
        new Thread(() -> {
            payConfigStorage.loadCertEnvironment();
            wxParameterStructure = new WxParameterStructure(payConfigStorage);
            //在这预先进行初始化
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException e) {

            }
            getAssistService();
        }).start();


    }

    /**
     * 设置api服务器地址
     *
     * @param apiServerUrl api服务器地址
     * @return 自身
     */
    public WxPayService setApiServerUrl(String apiServerUrl) {
        this.apiServerUrl = apiServerUrl;
        return this;
    }

    /**
     * 根据交易类型获取url
     *
     * @param transactionType 交易类型
     * @return 请求url
     */
    @Override
    public String getReqUrl(TransactionType transactionType) {
        String type = transactionType.getType();
        String partnerStr = "";
        if (payConfigStorage.isPartner()) {
            partnerStr = "/partner";
        }
        type = type.replace("{partner}", partnerStr);
        return apiServerUrl + (payConfigStorage.isTest() ? SANDBOXNEW : "") + type;
    }


    /**
     * 回调校验
     *
     * @param params 回调回来的参数集
     * @return 签名校验 true通过
     */
    @Override
    public boolean verify(Map<String, Object> params) {
        throw new PayErrorException(new WxPayError("", "微信V3不支持方式"));

    }

    /**
     * 验签，使用微信平台证书.
     *
     * @param noticeParams 通知参数
     * @return the boolean
     */
    public boolean verify(NoticeParams noticeParams) {

        //当前使用的微信平台证书序列号
        String serial = noticeParams.getHeader("wechatpay-serial");
        //微信服务器的时间戳
        String timestamp = noticeParams.getHeader("wechatpay-timestamp");
        //微信服务器提供的随机串
        String nonce = noticeParams.getHeader("wechatpay-nonce");
        //微信平台签名
        String signature = noticeParams.getHeader("wechatpay-signature");

        Certificate certificate = getAssistService().getCertificate(serial);

        Map<String, Object> attr = noticeParams.getAttr();

        if (Util.isEmpty(attr)) {
            throw new PayErrorException(new WxPayError(FAILURE, "请勿置空NoticeParams.attr中的数据"));
        }

        //这里为微信回调时的请求内容体，原值数据
        String body = (String) attr.get(WxConst.RESP_BODY);
        //签名信息
        String signText = StringUtils.joining("\n", timestamp, nonce, body);

        return RSA2.verify(signText, signature, certificate, payConfigStorage.getInputCharset());
    }


    /**
     * 微信统一下单接口
     *
     * @param order 支付订单集
     * @return 下单结果
     */
    public JSONObject unifiedOrder(PayOrder order) {

        //统一下单
        Map<String, Object> parameters = wxParameterStructure.getPublicParameters();
        wxParameterStructure.initPartner(parameters);
        // 商品描述
        OrderParaStructure.loadParameters(parameters, "description", order.getSubject());
        OrderParaStructure.loadParameters(parameters, "description", order.getBody());
        // 订单号
        parameters.put(WxConst.OUT_TRADE_NO, order.getOutTradeNo());
        //交易结束时间
        if (null != order.getExpirationTime()) {
            parameters.put("time_expire", DateUtils.formatDate(order.getExpirationTime(), DateUtils.YYYYMMDDHHMMSS));
        }
        OrderParaStructure.loadParameters(parameters, "attach", order.getAddition());
        wxParameterStructure.initNotifyUrl(parameters, order);
        //订单优惠标记
        OrderParaStructure.loadParameters(parameters, "goods_tag", order);
        parameters.put("amount", Amount.getAmount(order.getPrice(), order.getCurType()));

        //优惠功能
        OrderParaStructure.loadParameters(parameters, "detail", order);
        //支付场景描述
        OrderParaStructure.loadParameters(parameters, WxConst.SCENE_INFO, order);
        wxParameterStructure.loadSettleInfo(parameters, order);

        TransactionType transactionType = order.getTransactionType();
        ((WxTransactionType) transactionType).setAttribute(parameters, order);

        return getAssistService().doExecute(parameters, order);
    }


    /**
     * 返回创建的订单信息
     *
     * @param order 支付订单
     * @return 订单信息
     * @see PayOrder 支付订单信息
     */
    @Override
    public Map<String, Object> orderInfo(PayOrder order) {

        ////统一下单
        JSONObject result = unifiedOrder(order);
        //如果是扫码支付或者刷卡付无需处理，直接返回
        if (((WxTransactionType) order.getTransactionType()).isReturn()) {
            return result;
        }

        Map<String, Object> params = new LinkedHashMap<>();
        String appId = payConfigStorage.getAppId();
        String timeStamp = String.valueOf(DateUtils.toEpochSecond());
        String randomStr = SignTextUtils.randomStr();
        String prepayId = result.getString("prepay_id");
        if (WxTransactionType.JSAPI == order.getTransactionType()) {
            params.put("appId", appId);
            params.put("timeStamp", timeStamp);
            params.put("nonceStr", randomStr);
            prepayId = "prepay_id=" + prepayId;
            params.put("package", prepayId);
            params.put("signType", SignUtils.RSA.getName());
        }
        else if (WxTransactionType.APP == order.getTransactionType()) {
            params.put(WxConst.APPID, appId);
            params.put("partnerid", payConfigStorage.getMchId());
            params.put("timestamp", timeStamp);
            params.put("noncestr", randomStr);
            params.put("prepayid", prepayId);
            params.put("package", "Sign=WXPay");
        }
        String signText = StringUtils.joining("\n", appId, timeStamp, prepayId);
        String paySign = createSign(signText, payConfigStorage.getInputCharset());
        params.put(WxTransactionType.JSAPI.equals(order.getTransactionType()) ? "paySign" : "sign", paySign);
        return params;

    }


    /**
     * 签名
     *
     * @param content           需要签名的内容 不包含key
     * @param characterEncoding 字符编码
     * @return 签名结果
     */
    @Override
    public String createSign(String content, String characterEncoding) {
        PrivateKey privateKey = payConfigStorage.getCertEnvironment().getPrivateKey();
        return RSA2.sign(content, privateKey, characterEncoding);
    }

    /**
     * 将请求参数或者请求流转化为 Map
     *
     * @param parameterMap 请求参数
     * @param is           请求流
     * @return 获得回调的请求参数
     */
    @Override
    public Map<String, Object> getParameter2Map(Map<String, String[]> parameterMap, InputStream is) {
        throw new PayErrorException(new WxPayError(FAILURE, "微信V3不支持方式"));

    }

    /**
     * 将请求参数或者请求流转化为 Map
     *
     * @param request 通知请求
     * @return 获得回调的请求参数
     */
    @Override
    public NoticeParams getNoticeParams(NoticeRequest request) {
        WxNoticeParams noticeParams = null;
        try (InputStream is = request.getInputStream()) {
            String body = IOUtils.toString(is);
            noticeParams = JSON.parseObject(body, WxNoticeParams.class);
            noticeParams.setAttr(new MapGen<String, Object>(WxConst.RESP_BODY, body).getAttr());
            Resource resource = noticeParams.getResource();
            String associatedData = resource.getAssociatedData();
            String nonce = resource.getNonce();
            String ciphertext = resource.getCiphertext();
            String data = AntCertificationUtil.decryptToString(associatedData, nonce, ciphertext, payConfigStorage.getV3ApiKey(), payConfigStorage.getInputCharset());
            noticeParams.setBody(JSON.parseObject(data));
        }
        catch (IOException e) {
            LOG.error("获取回调参数异常", e);
        }
        Map<String, List<String>> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, Collections.list(request.getHeaders(name)));
        }
        noticeParams.setHeaders(headers);
        return noticeParams;
    }

    /**
     * 获取输出消息，用户返回给支付端
     *
     * @param code    状态
     * @param message 消息
     * @return 返回输出消息
     */
    @Override
    public PayOutMessage getPayOutMessage(String code, String message) {
        return PayOutMessage.JSON().content("code", code).content("message", message).build();
    }


    /**
     * 获取成功输出消息，用户返回给支付端
     * 主要用于拦截器中返回
     *
     * @param payMessage 支付回调消息
     * @return 返回输出消息
     */
    @Override
    public PayOutMessage successPayOutMessage(PayMessage payMessage) {
        return getPayOutMessage("SUCCESS", "成功");
    }


    /**
     * 获取输出消息，用户返回给支付端, 针对于web端
     *
     * @param orderInfo 发起支付的订单信息
     * @param method    请求方式  "post" "get",
     * @return 获取输出消息，用户返回给支付端, 针对于web端
     * @see MethodType 请求类型
     */
    @Override
    public String buildRequest(Map<String, Object> orderInfo, MethodType method) {
        if (!SUCCESS.equals(orderInfo.get(RETURN_CODE))) {
            throw new PayErrorException(new WxPayError((String) orderInfo.get(RETURN_CODE), (String) orderInfo.get(RETURN_MSG_CODE)));
        }
        if (WxTransactionType.H5.name().equals(orderInfo.get("trade_type"))) {
            return String.format("<script type=\"text/javascript\">location.href=\"%s%s\"</script>", orderInfo.get("mweb_url"), StringUtils.isEmpty(payConfigStorage.getReturnUrl()) ? "" : "&redirect_url=" + URLEncoder.encode(payConfigStorage.getReturnUrl()));
        }
        throw new UnsupportedOperationException();

    }

    /**
     * 获取输出二维码信息,
     *
     * @param order 发起支付的订单信息
     * @return 返回二维码信息,，支付时需要的
     */
    @Override
    public String getQrPay(PayOrder order) {
        order.setTransactionType(WxTransactionType.NATIVE);
        Map<String, Object> orderInfo = orderInfo(order);

        return (String) orderInfo.get("code_url");
    }

    /**
     * 刷卡付,pos主动扫码付款
     *
     * @param order 发起支付的订单信息
     * @return 返回支付结果
     */
    @Override
    public Map<String, Object> microPay(PayOrder order) {
        throw new PayErrorException(new PayException("failure", "V3暂时没有提供此功能，请查看V2版本功能"));
    }

    /**
     * 交易查询接口
     *
     * @param transactionId 微信支付平台订单号
     * @param outTradeNo    商户单号
     * @return 返回查询回来的结果集，支付方原值返回
     */
    @Override
    public Map<String, Object> query(String transactionId, String outTradeNo) {

        String parameters = wxParameterStructure.getSpParameters();
        WxTransactionType transactionType = WxTransactionType.QUERY_TRANSACTION_ID;
        String uriVariable = transactionId;
        if (StringUtils.isNotEmpty(outTradeNo)) {
            transactionType = WxTransactionType.QUERY_OUT_TRADE_NO;
            uriVariable = outTradeNo;
        }
        return getAssistService().doExecute(parameters, transactionType, uriVariable);
    }


    /**
     * 交易关闭接口
     *
     * @param transactionId 支付平台订单号
     * @param outTradeNo    商户单号
     * @return 返回支付方交易关闭后的结果
     */
    @Override
    public Map<String, Object> close(String transactionId, String outTradeNo) {
        String parameters = wxParameterStructure.getSpParameters();
        return getAssistService().doExecute(parameters, WxTransactionType.CLOSE, outTradeNo);
    }

    /**
     * 申请退款接口
     *
     * @param refundOrder 退款订单信息
     * @return 返回支付方申请退款后的结果
     */
    @Override
    public WxRefundResult refund(RefundOrder refundOrder) {
        //获取公共参数
        Map<String, Object> parameters = wxParameterStructure.initSubMchId(null);

        OrderParaStructure.loadParameters(parameters, "transaction_id", refundOrder.getTradeNo());
        OrderParaStructure.loadParameters(parameters, OUT_TRADE_NO, refundOrder.getOutTradeNo());
        OrderParaStructure.loadParameters(parameters, "out_refund_no", refundOrder.getRefundNo());
        OrderParaStructure.loadParameters(parameters, "reason", refundOrder.getDescription());
        OrderParaStructure.loadParameters(parameters, "funds_account", refundOrder);
        wxParameterStructure.initNotifyUrl(parameters, refundOrder);
        RefundAmount refundAmount = new RefundAmount();
        refundAmount.setRefund(Util.conversionCentAmount(refundOrder.getRefundAmount()));
        refundAmount.setTotal(Util.conversionCentAmount(refundOrder.getTotalAmount()));
        CurType curType = refundOrder.getCurType();
        if (null != curType) {
            refundAmount.setCurrency(curType.getType());
        }
        parameters.put("amount", refundAmount);
        OrderParaStructure.loadParameters(parameters, "amount", refundOrder);
        return WxRefundResult.create(getAssistService().doExecute(parameters, WxTransactionType.REFUND));
    }


    /**
     * 查询退款
     *
     * @param refundOrder 退款订单单号信息
     * @return 返回支付方查询退款后的结果
     */
    @Override
    public Map<String, Object> refundquery(RefundOrder refundOrder) {
        String parameters = UriVariables.getMapToParameters(wxParameterStructure.initSubMchId(null));
        return getAssistService().doExecute(parameters, WxTransactionType.REFUND_QUERY, refundOrder.getRefundNo());
    }

    /**
     * 下载对账单
     *
     * @param billDate 账单时间：日账单格式为yyyy-MM-dd，月账单格式为yyyy-MM。
     * @param billType 账单类型 内部自动转化 {@link BillType}
     * @return 返回支付方下载对账单的结果
     */
    @Override
    public Map<String, Object> downloadBill(Date billDate, String billType) {
        BillType wxBillType = WxBillType.forType(billType);
        if (null == wxBillType) {
            wxBillType = WxAccountType.forType(billType);
        }
        return downloadBill(billDate, wxBillType);
    }

    /**
     * 目前只支持
     * 对账单中涉及金额的字段单位为“元”。
     *
     * @param billDate 下载对账单的日期，格式：20140603
     * @param billType 账单类型 {@link WxBillType} 与 {@link WxAccountType}
     * @return 返回支付方下载对账单的结果, 如果【账单类型】为gzip的话则返回值中key为data值为gzip的输入流
     */
    public Map<String, Object> downloadBill(Date billDate, BillType billType) {
        //获取公共参数
        Map<String, Object> parameters = new HashMap<>(5);

        //目前只支持日账单
        parameters.put("bill_date", DateUtils.formatDate(billDate, DateUtils.YYYYMMDD));
        String fileType = billType.getFileType();
        OrderParaStructure.loadParameters(parameters, "tar_type", fileType);
        if (billType instanceof WxAccountType) {
            OrderParaStructure.loadParameters(parameters, "account_type", billType.getType());
        }
        else {
            wxParameterStructure.initSubMchId(parameters).put("bill_type", billType.getType());
        }
        String body = UriVariables.getMapToParameters(parameters);
        JSONObject result = getAssistService().doExecute(body, WxTransactionType.valueOf(billType.getCustom()));
        String downloadUrl = result.getString("download_url");
        MethodType methodType = MethodType.GET;
        HttpEntity entity = getAssistService().buildHttpEntity(downloadUrl, "", methodType.name());
        ResponseEntity<InputStream> responseEntity = requestTemplate.doExecuteEntity(downloadUrl, entity, InputStream.class, methodType);
        InputStream inputStream = responseEntity.getBody();
        int statusCode = responseEntity.getStatusCode();
        if (statusCode >= 400) {
            try {
                String errorText = IOUtils.toString(inputStream);
                JSONObject json = JSON.parseObject(errorText);
                throw new PayErrorException(new WxPayError(statusCode + "", json.getString(WxConst.MESSAGE), errorText));
            }
            catch (IOException e) {
                throw new PayErrorException(new WxPayError(statusCode + "", ""));
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("file", inputStream);
        return result;
    }


    /**
     * 转账
     *
     * @param order 转账订单
     * @return 对应的转账结果
     */
    @Override
    public Map<String, Object> transfer(TransferOrder order) {
        throw new PayErrorException(new WxPayError("", "等待作者实现"));
    }


    /**
     * 转账查询
     *
     * @param outNo          商户转账订单号
     * @param wxTransferType 微信转账类型，.....这里没办法了只能这样写(┬＿┬)，请见谅 {@link WxTransferType}
     *                       <p>
     *                       <a href="https://pay.weixin.qq.com/wiki/doc/api/tools/mch_pay.php?chapter=14_3">企业付款到零钱</a>
     *                       <a href="https://pay.weixin.qq.com/wiki/doc/api/tools/mch_pay.php?chapter=24_3">商户企业付款到银行卡</a>
     *                       </p>
     * @return 对应的转账订单
     */
    @Override
    public Map<String, Object> transferQuery(String outNo, String wxTransferType) {
        throw new PayErrorException(new WxPayError("", "等待作者实现"));
    }


    /**
     * 创建消息
     *
     * @param message 支付平台返回的消息
     * @return 支付消息对象
     */
    @Override
    public PayMessage createMessage(Map<String, Object> message) {
        return WxPayMessage.create(message);
    }


}
