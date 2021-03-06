package com.egzosn.pay.union.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.alibaba.fastjson.JSONObject;
import com.egzosn.pay.common.api.BasePayService;
import com.egzosn.pay.common.bean.AssistOrder;
import com.egzosn.pay.common.bean.BillType;

import com.egzosn.pay.common.bean.MethodType;
import com.egzosn.pay.common.bean.NoticeParams;
import com.egzosn.pay.common.bean.OrderParaStructure;
import com.egzosn.pay.common.bean.PayMessage;
import com.egzosn.pay.common.bean.PayOrder;
import com.egzosn.pay.common.bean.PayOutMessage;
import com.egzosn.pay.common.bean.RefundOrder;
import com.egzosn.pay.common.bean.TransactionType;
import com.egzosn.pay.common.bean.outbuilder.PayTextOutMessage;
import com.egzosn.pay.common.bean.result.PayException;
import com.egzosn.pay.common.exception.PayErrorException;
import com.egzosn.pay.common.http.HttpConfigStorage;
import com.egzosn.pay.common.http.UriVariables;
import com.egzosn.pay.common.util.DateUtils;
import com.egzosn.pay.common.util.Util;
import com.egzosn.pay.common.util.sign.CertDescriptor;
import com.egzosn.pay.common.util.sign.SignTextUtils;
import com.egzosn.pay.common.util.sign.SignUtils;
import com.egzosn.pay.common.util.sign.encrypt.RSA;
import com.egzosn.pay.common.util.sign.encrypt.RSA2;
import com.egzosn.pay.common.util.str.StringUtils;
import com.egzosn.pay.union.bean.SDKConstants;
import com.egzosn.pay.union.bean.UnionPayBillType;
import com.egzosn.pay.union.bean.UnionPayMessage;
import com.egzosn.pay.union.bean.UnionRefundResult;
import com.egzosn.pay.union.bean.UnionTransactionType;

/**
 * @author Actinia
 * <pre>
 *         email hayesfu@qq.com
 *         create 2017 2017/11/5
 *         </pre>
 */
public class UnionPayService extends BasePayService<UnionPayConfigStorage> {
    /**
     * ????????????
     */
    private static final String TEST_BASE_DOMAIN = "test.95516.com";
    /**
     * ????????????
     */
    private static final String RELEASE_BASE_DOMAIN = "95516.com";
    /**
     * ??????????????????
     */
    private static final String FRONT_TRANS_URL = "https://gateway.%s/gateway/api/frontTransReq.do";
    private static final String BACK_TRANS_URL = "https://gateway.%s/gateway/api/backTransReq.do";
    private static final String SINGLE_QUERY_URL = "https://gateway.%s/gateway/api/queryTrans.do";
    private static final String BATCH_TRANS_URL = "https://gateway.%s/gateway/api/batchTrans.do";
    private static final String FILE_TRANS_URL = "https://filedownload.%s/";
    private static final String APP_TRANS_URL = "https://gateway.%s/gateway/api/appTransReq.do";
    private static final String CARD_TRANS_URL = "https://gateway.%s/gateway/api/cardTransReq.do";
    /**
     * ???????????????
     */
    private volatile CertDescriptor certDescriptor;

    /**
     * ????????????
     *
     * @param payConfigStorage ????????????
     */
    public UnionPayService(UnionPayConfigStorage payConfigStorage) {
        this(payConfigStorage, null);
    }

    public UnionPayService(UnionPayConfigStorage payConfigStorage, HttpConfigStorage configStorage) {
        super(payConfigStorage, configStorage);

    }

    /**
     * ??????????????????
     *
     * @param payConfigStorage ????????????
     */
    @Override
    public UnionPayService setPayConfigStorage(UnionPayConfigStorage payConfigStorage) {
        this.payConfigStorage = payConfigStorage;
        if (null != certDescriptor) {
            return this;
        }
        try {
            certDescriptor = new CertDescriptor();
            certDescriptor.initPrivateSignCert(payConfigStorage.getKeyPrivateCertInputStream(), payConfigStorage.getKeyPrivateCertPwd(), "PKCS12");
            certDescriptor.initPublicCert(payConfigStorage.getAcpMiddleCertInputStream());
            certDescriptor.initRootCert(payConfigStorage.getAcpRootCertInputStream());
        }
        catch (IOException e) {
            LOG.error("", e);
        }


        return this;
    }

    /**
     * ????????????????????????
     *
     * @param transactionType ????????????
     * @return ????????????
     */
    @Override
    public String getReqUrl(TransactionType transactionType) {
        return (payConfigStorage.isTest() ? TEST_BASE_DOMAIN : RELEASE_BASE_DOMAIN);
    }

    /**
     * ???????????????????????????????????????????????????
     *
     * @return ????????????
     */
    public String getReqUrl() {
        return getReqUrl(null);
    }

    public String getFrontTransUrl() {
        return String.format(FRONT_TRANS_URL, getReqUrl());
    }

    public String getBackTransUrl() {
        return String.format(BACK_TRANS_URL, getReqUrl());
    }

    public String getAppTransUrl() {
        return String.format(APP_TRANS_URL, getReqUrl());
    }

    public String getSingleQueryUrl() {
        return String.format(SINGLE_QUERY_URL, getReqUrl());
    }


    public String getFileTransUrl() {
        return String.format(FILE_TRANS_URL, getReqUrl());
    }

    /**
     * ??????????????????
     *
     * @param parameters ???????????????
     * @param order      ??????
     * @return ???????????????
     */
    private Map<String, Object> initNotifyUrl(Map<String, Object> parameters, AssistOrder order) {
        //??????????????????
        OrderParaStructure.loadParameters(parameters, SDKConstants.param_backUrl, payConfigStorage.getNotifyUrl());
        OrderParaStructure.loadParameters(parameters, SDKConstants.param_backUrl, order.getNotifyUrl());
        OrderParaStructure.loadParameters(parameters, SDKConstants.param_backUrl, order);
        return parameters;
    }


    /**
     * ?????????????????????????????????????????????encoding?????????????????????????????????
     *
     * @return ??????????????????
     */
    private Map<String, Object> getCommonParam() {
        Map<String, Object> params = new TreeMap<>();
        UnionPayConfigStorage configStorage = payConfigStorage;
        //??????????????????
        params.put(SDKConstants.param_version, configStorage.getVersion());
        //????????????
        params.put(SDKConstants.param_encoding, payConfigStorage.getInputCharset().toUpperCase());
        //????????????
        params.put(SDKConstants.param_merId, payConfigStorage.getPid());

        //??????????????????
        params.put(SDKConstants.param_txnTime, DateUtils.formatDate(new Date(), DateUtils.YYYYMMDDHHMMSS));
        //??????????????????
        params.put(SDKConstants.param_backUrl, payConfigStorage.getNotifyUrl());

        //????????????
        params.put(SDKConstants.param_currencyCode, "156");
        //??????????????????????????????0 ??????????????????0?????????????????? 1??? ???????????? 2??????????????????
        params.put(SDKConstants.param_accessType, configStorage.getAccessType());
        return params;
    }


    /**
     * ????????????
     *
     * @param result ????????????????????????
     * @return ???????????? true??????
     */
    @Deprecated
    @Override
    public boolean verify(Map<String, Object> result) {


        return verify(new NoticeParams(result));
    }

    /**
     * ????????????
     *
     * @param noticeParams ????????????????????????
     * @return ???????????? true??????
     */
    @Override
    public boolean verify(NoticeParams noticeParams) {
        final Map<String, Object> result = noticeParams.getBody();
        if (null == result || result.get(SDKConstants.param_signature) == null) {
            LOG.debug("???????????????????????????params???" + result);
            return false;
        }
        return this.signVerify(result, (String) result.get(SDKConstants.param_signature));
    }

    /**
     * ????????????
     *
     * @param params ?????????
     * @param sign   ????????????
     * @return ???????????? true??????
     */
    public boolean signVerify(Map<String, Object> params, String sign) {
        SignUtils signUtils = SignUtils.valueOf(payConfigStorage.getSignType());

        String data = SignTextUtils.parameterText(params, "&", "signature");
        switch (signUtils) {
            case RSA:
                data = SignUtils.SHA1.createSign(data, "", payConfigStorage.getInputCharset());
                return RSA.verify(data, sign, verifyCertificate(genCertificateByStr((String) params.get(SDKConstants.param_signPubKeyCert))).getPublicKey(), payConfigStorage.getInputCharset());
            case RSA2:
                data = SignUtils.SHA256.createSign(data, "", payConfigStorage.getInputCharset());
                return RSA2.verify(data, sign, verifyCertificate(genCertificateByStr((String) params.get(SDKConstants.param_signPubKeyCert))).getPublicKey(), payConfigStorage.getInputCharset());
            case SHA1:
            case SHA256:
            case SM3:
                String before = signUtils.createSign(payConfigStorage.getKeyPublic(), "", payConfigStorage.getInputCharset());
                return signUtils.verify(data, sign, "&" + before, payConfigStorage.getInputCharset());
            default:
                return false;
        }
    }


    /**
     * ?????????????????????
     * ??????????????????????????????????????????????????????????????????????????????????????????????????? ??????????????????????????????????????????????????????????????????????????????5?????????????????????????????????????????????
     * ?????????????????????????????????????????????15?????????
     * ???????????????????????????????????????origRespCode??????A6??????00??????????????????????????????
     *
     * @param expirationTime ????????????
     * @return ????????????????????????
     */
    private String getPayTimeout(Date expirationTime) {
        //
        if (null != expirationTime) {
            return DateUtils.formatDate(expirationTime, DateUtils.YYYYMMDDHHMMSS);
        }
        return DateUtils.formatDate(new Timestamp(System.currentTimeMillis() + 30 * 60 * 1000), DateUtils.YYYYMMDDHHMMSS);
    }

    /**
     * ???????????????????????????
     *
     * @param order ????????????
     * @return ????????????
     * @see PayOrder ??????????????????
     */
    @Override
    public Map<String, Object> orderInfo(PayOrder order) {
        Map<String, Object> params = this.getCommonParam();

        UnionTransactionType type = (UnionTransactionType) order.getTransactionType();
        initNotifyUrl(params, order);

        //?????????????????????????????????
        type.convertMap(params);

        params.put(SDKConstants.param_orderId, order.getOutTradeNo());

        if (StringUtils.isNotEmpty(order.getAddition())) {
            params.put(SDKConstants.param_reqReserved, order.getAddition());
        }
        switch (type) {
            case WAP:
            case WEB:
                //todo PCwap??????????????????????????????.txt
            case B2B:
                params.put(SDKConstants.param_txnAmt, Util.conversionCentAmount(order.getPrice()));
                params.put("orderDesc", order.getSubject());
                params.put(SDKConstants.param_payTimeout, getPayTimeout(order.getExpirationTime()));

                params.put(SDKConstants.param_frontUrl, payConfigStorage.getReturnUrl());
                break;
            case CONSUME:
                params.put(SDKConstants.param_txnAmt, Util.conversionCentAmount(order.getPrice()));
                params.put(SDKConstants.param_qrNo, order.getAuthCode());
                break;
            case APPLY_QR_CODE:
                if (null != order.getPrice()) {
                    params.put(SDKConstants.param_txnAmt, Util.conversionCentAmount(order.getPrice()));
                }
                params.put(SDKConstants.param_payTimeout, getPayTimeout(order.getExpirationTime()));
                break;
            default:
                params.put(SDKConstants.param_txnAmt, Util.conversionCentAmount(order.getPrice()));
                params.put(SDKConstants.param_payTimeout, getPayTimeout(order.getExpirationTime()));
                params.put("orderDesc", order.getSubject());
        }
        params.putAll(order.getAttrs());
        params = preOrderHandler(params, order);
        return setSign(params);
    }


    /**
     * ?????????????????????
     *
     * @param parameters ????????????
     * @return ????????????
     */
    private Map<String, Object> setSign(Map<String, Object> parameters) {

        SignUtils signUtils = SignUtils.valueOf(payConfigStorage.getSignType());

        String signStr;
        switch (signUtils) {
            case RSA:
                parameters.put(SDKConstants.param_signMethod, SDKConstants.SIGNMETHOD_RSA);
                parameters.put(SDKConstants.param_certId, certDescriptor.getSignCertId());
                signStr = SignUtils.SHA1.createSign(SignTextUtils.parameterText(parameters, "&", "signature"), "", payConfigStorage.getInputCharset());
                parameters.put(SDKConstants.param_signature, RSA.sign(signStr, certDescriptor.getSignCertPrivateKey(payConfigStorage.getKeyPrivateCertPwd()), payConfigStorage.getInputCharset()));
                break;
            case RSA2:
                parameters.put(SDKConstants.param_signMethod, SDKConstants.SIGNMETHOD_RSA);
                parameters.put(SDKConstants.param_certId, certDescriptor.getSignCertId());
                signStr = SignUtils.SHA256.createSign(SignTextUtils.parameterText(parameters, "&", "signature"), "", payConfigStorage.getInputCharset());
                parameters.put(SDKConstants.param_signature, RSA2.sign(signStr, certDescriptor.getSignCertPrivateKey(payConfigStorage.getKeyPrivateCertPwd()), payConfigStorage.getInputCharset()));
                break;
            case SHA1:
            case SHA256:
            case SM3:
                String key = payConfigStorage.getKeyPrivate();
                signStr = SignTextUtils.parameterText(parameters, "&", "signature");
                key = signUtils.createSign(key, "", payConfigStorage.getInputCharset()) + "&";
                parameters.put(SDKConstants.param_signature, signUtils.createSign(signStr, key, payConfigStorage.getInputCharset()));
                break;
            default:
                throw new PayErrorException(new PayException("sign fail", "????????????????????????"));
        }


        return parameters;
    }


    /**
     * ???????????????
     *
     * @param cert ?????????????????????
     */
    private X509Certificate verifyCertificate(X509Certificate cert) {
        try {
            cert.checkValidity();//???????????????
            X509Certificate middleCert = certDescriptor.getPublicCert();
            X509Certificate rootCert = certDescriptor.getRootCert();

            X509CertSelector selector = new X509CertSelector();
            selector.setCertificate(cert);

            Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>();
            trustAnchors.add(new TrustAnchor(rootCert, null));
            PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustAnchors, selector);

            Set<X509Certificate> intermediateCerts = new HashSet<X509Certificate>();
            intermediateCerts.add(rootCert);
            intermediateCerts.add(middleCert);
            intermediateCerts.add(cert);

            pkixParams.setRevocationEnabled(false);

            CertStore intermediateCertStore = CertStore.getInstance("Collection", new CollectionCertStoreParameters(intermediateCerts));
            pkixParams.addCertStore(intermediateCertStore);

            CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");

            /*PKIXCertPathBuilderResult result = (PKIXCertPathBuilderResult)*/
            builder.build(pkixParams);
            return cert;
        }
        catch (java.security.cert.CertPathBuilderException e) {
            LOG.error("verify certificate chain fail.", e);
        }
        catch (CertificateExpiredException e) {
            LOG.error("", e);
        }
        catch (GeneralSecurityException e) {
            LOG.error("", e);
        }
        return null;
    }

    /**
     * ????????????
     *
     * @param order ???????????????????????????
     * @return ??????????????????
     */

    public JSONObject postOrder(PayOrder order, String url) {
        Map<String, Object> params = orderInfo(order);
        String responseStr = getHttpRequestTemplate().postForObject(url, params, String.class);
        JSONObject response = UriVariables.getParametersToMap(responseStr);
        if (response.isEmpty()) {
            throw new PayErrorException(new PayException("failure", "??????????????????!", responseStr));
        }
        return response;
    }

    @Override
    public String toPay(PayOrder order) {

        if (null == order.getTransactionType()) {
            order.setTransactionType(UnionTransactionType.WEB);
        }
        else if (UnionTransactionType.WEB != order.getTransactionType() && UnionTransactionType.WAP != order.getTransactionType() && UnionTransactionType.B2B != order.getTransactionType()) {
            throw new PayErrorException(new PayException("-1", "?????????????????????:" + order.getTransactionType()));
        }

        return super.toPay(order);
    }

    /**
     * ????????????????????????????????????????????????,
     *
     * @param order ???????????????????????????
     * @return ???????????????????????????????????????
     */
    @Override
    public String getQrPay(PayOrder order) {
        order.setTransactionType(UnionTransactionType.APPLY_QR_CODE);
        JSONObject response = postOrder(order, getBackTransUrl());
        if (this.verify(response)) {
            if (SDKConstants.OK_RESP_CODE.equals(response.get(SDKConstants.param_respCode))) {
                //??????
                return (String) response.get(SDKConstants.param_qrCode);
            }
            throw new PayErrorException(new PayException((String) response.get(SDKConstants.param_respCode), (String) response.get(SDKConstants.param_respMsg), response.toJSONString()));
        }
        throw new PayErrorException(new PayException("failure", "??????????????????", response.toJSONString()));
    }

    /**
     * ?????????,pos??????????????????(?????????)
     *
     * @param order ???????????????????????????
     * @return ??????????????????
     */
    @Override
    public Map<String, Object> microPay(PayOrder order) {
        order.setTransactionType(UnionTransactionType.CONSUME);
        JSONObject response = postOrder(order, getBackTransUrl());
        return response;
    }


    /**
     * ?????????????????????X509Certificate??????.
     *
     * @param x509CertString ?????????
     * @return X509Certificate
     */
    public static X509Certificate genCertificateByStr(String x509CertString) {
        X509Certificate x509Cert = null;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream tIn = new ByteArrayInputStream(x509CertString.getBytes("ISO-8859-1"));
            x509Cert = (X509Certificate) cf.generateCertificate(tIn);
        }
        catch (Exception e) {
            throw new PayErrorException(new PayException("??????????????????", "gen certificate error:" + e.getLocalizedMessage()));
        }
        return x509Cert;
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param code    ??????
     * @param message ??????
     * @return ??????????????????
     */
    @Override
    public PayOutMessage getPayOutMessage(String code, String message) {
        return PayTextOutMessage.TEXT().content(code.toLowerCase()).build();
    }

    /**
     * ???????????????????????????????????????????????????
     * ??????????????????????????????
     *
     * @param payMessage ??????????????????
     * @return ??????????????????
     */
    @Override
    public PayOutMessage successPayOutMessage(PayMessage payMessage) {
        return getPayOutMessage("ok", null);
    }

    /**
     * ??????????????????????????????Html??????
     *
     * @param orderInfo ???????????????????????????
     * @param method    ????????????  "post" "get",
     * @return ?????????????????????Html????????????????????????, ?????????PC???
     * @see MethodType ????????????
     */
    @Override
    public String buildRequest(Map<String, Object> orderInfo, MethodType method) {
        StringBuffer sf = new StringBuffer();
        sf.append("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=" + payConfigStorage.getInputCharset() + "\"/></head><body>");
        sf.append("<form id = \"pay_form\" action=\"" + getFrontTransUrl() + "\" method=\"post\">");
        if (null != orderInfo && 0 != orderInfo.size()) {
            for (Map.Entry<String, Object> entry : orderInfo.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                sf.append("<input type=\"hidden\" name=\"" + key + "\" id=\"" + key + "\" value=\"" + value + "\"/>");
            }
        }
        sf.append("</form>");
        sf.append("</body>");
        sf.append("<script type=\"text/javascript\">");
        sf.append("document.all.pay_form.submit();");
        sf.append("</script>");
        sf.append("</html>");
        return sf.toString();
    }

    /**
     * ???????????????????????????????????????????????????
     * ?????????????????????????????????????????????Pay???
     *
     * @param order ????????????
     * @return ???????????????????????????  ???????????????
     */
    @Override
    public Map<String, Object> app(PayOrder order) {
        if (null == order.getTransactionType()) {
            order.setTransactionType(UnionTransactionType.APP);
        }
        JSONObject response = postOrder(order, getAppTransUrl());
        if (this.verify(response)) {
            if (SDKConstants.OK_RESP_CODE.equals(response.get(SDKConstants.param_respCode))) {
//                //??????,??????tn???
//                String tn =  (String)response.get(SDKConstants.param_tn);
//                //TODO
                return response;
            }
            throw new PayErrorException(new PayException((String) response.get(SDKConstants.param_respCode), (String) response.get(SDKConstants.param_respMsg), response.toJSONString()));
        }
        throw new PayErrorException(new PayException("failure", "??????????????????", response.toJSONString()));
    }

    /**
     * ??????????????????
     *
     * @param tradeNo    ?????????????????????
     * @param outTradeNo ????????????
     * @return ??????????????????????????????????????????????????????
     */
    @Override
    public Map<String, Object> query(String tradeNo, String outTradeNo) {
        return query(new AssistOrder(tradeNo, outTradeNo));

    }

    /**
     * ??????????????????
     *
     * @param assistOrder ????????????
     * @return ??????????????????????????????????????????????????????
     */
    @Override
    public Map<String, Object> query(AssistOrder assistOrder) {
        Map<String, Object> params = this.getCommonParam();
        UnionTransactionType.QUERY.convertMap(params);
        params.put(SDKConstants.param_orderId, assistOrder.getOutTradeNo());
        this.setSign(params);
        String responseStr = getHttpRequestTemplate().postForObject(this.getSingleQueryUrl(), params, String.class);
        JSONObject response = UriVariables.getParametersToMap(responseStr);
        if (this.verify(new NoticeParams(response))) {
            if (SDKConstants.OK_RESP_CODE.equals(response.getString(SDKConstants.param_respCode))) {
                String origRespCode = response.getString(SDKConstants.param_origRespCode);
                if ((SDKConstants.OK_RESP_CODE).equals(origRespCode)) {
                    //???????????????????????????????????????
                    return response;
                }
            }
            throw new PayErrorException(new PayException(response.getString(SDKConstants.param_respCode), response.getString(SDKConstants.param_respMsg), response.toJSONString()));
        }
        throw new PayErrorException(new PayException("failure", "??????????????????", response.toJSONString()));
    }


    /**
     * ????????????/????????????
     *
     * @param origQryId    ????????????????????????.
     * @param orderId      ????????????
     * @param refundAmount ????????????
     * @param type         UnionTransactionType.REFUND  ??????UnionTransactionType.CONSUME_UNDO
     * @return ???????????????????????????????????????
     */
    public UnionRefundResult unionRefundOrConsumeUndo(String origQryId, String orderId, BigDecimal refundAmount, UnionTransactionType type) {
        return unionRefundOrConsumeUndo(new RefundOrder(orderId, origQryId, refundAmount), type);

    }

    /**
     * ????????????/????????????
     *
     * @param refundOrder ??????????????????
     * @param type        UnionTransactionType.REFUND  ??????UnionTransactionType.CONSUME_UNDO
     * @return ???????????????????????????????????????
     */
    public UnionRefundResult unionRefundOrConsumeUndo(RefundOrder refundOrder, UnionTransactionType type) {
        Map<String, Object> params = this.getCommonParam();
        type.convertMap(params);
        params.put(SDKConstants.param_orderId, refundOrder.getRefundNo());
        params.put(SDKConstants.param_txnAmt, Util.conversionCentAmount(refundOrder.getRefundAmount()));
        params.put(SDKConstants.param_origQryId, refundOrder.getTradeNo());
        params.putAll(refundOrder.getAttrs());
        this.setSign(params);
        String responseStr = getHttpRequestTemplate().postForObject(this.getBackTransUrl(), params, String.class);
        JSONObject response = UriVariables.getParametersToMap(responseStr);

        if (this.verify(new NoticeParams(response))) {
            final UnionRefundResult refundResult = UnionRefundResult.create(response);
            if (SDKConstants.OK_RESP_CODE.equals(refundResult.getRespCode())) {
                return refundResult;

            }
            throw new PayErrorException(new PayException(response.getString(SDKConstants.param_respCode), response.getString(SDKConstants.param_respMsg), response.toJSONString()));
        }
        throw new PayErrorException(new PayException("failure", "??????????????????", response.toJSONString()));
    }

    /**
     * ??????????????????
     *
     * @param tradeNo    ?????????????????????
     * @param outTradeNo ????????????
     * @return ???????????????????????????????????????
     */
    @Override
    public Map<String, Object> close(String tradeNo, String outTradeNo) {
        return Collections.emptyMap();
    }

    /**
     * ??????????????????
     *
     * @param assistOrder ????????????
     * @return ???????????????????????????????????????
     */
    @Override
    public Map<String, Object> close(AssistOrder assistOrder) {
        return Collections.emptyMap();
    }

    @Override
    public UnionRefundResult refund(RefundOrder refundOrder) {
        return unionRefundOrConsumeUndo(refundOrder, UnionTransactionType.REFUND);
    }


    /**
     * ????????????
     *
     * @param refundOrder ????????????????????????
     * @return ???????????????????????????????????????
     */
    @Override
    public Map<String, Object> refundquery(RefundOrder refundOrder) {
        return Collections.emptyMap();
    }

    /**
     * ???????????????
     *
     * @param billDate ????????????
     * @param fileType ???????????? ?????????????????????????????????00??????
     * @return ??????fileContent ????????????????????????
     */
    @Override
    public Map<String, Object> downloadBill(Date billDate, String fileType) {
        return downloadBill(billDate, new UnionPayBillType(fileType));
    }

    /**
     * ???????????????
     *
     * @param billDate ????????????
     * @param billType ????????????
     * @return ??????fileContent ????????????????????????
     */
    @Override
    public Map<String, Object> downloadBill(Date billDate, BillType billType) {

        Map<String, Object> params = this.getCommonParam();
        UnionTransactionType.FILE_TRANSFER.convertMap(params);

        params.put(SDKConstants.param_settleDate, DateUtils.formatDate(billDate, DateUtils.MMDD));
        params.put(SDKConstants.param_fileType, billType.getFileType());
        params.remove(SDKConstants.param_backUrl);
        params.remove(SDKConstants.param_currencyCode);
        this.setSign(params);
        String responseStr = getHttpRequestTemplate().postForObject(this.getFileTransUrl(), params, String.class);
        JSONObject response = UriVariables.getParametersToMap(responseStr);
        if (this.verify(response)) {
            if (SDKConstants.OK_RESP_CODE.equals(response.get(SDKConstants.param_respCode))) {
                return response;

            }
            throw new PayErrorException(new PayException(response.get(SDKConstants.param_respCode).toString(), response.get(SDKConstants.param_respMsg).toString(), response.toString()));

        }
        throw new PayErrorException(new PayException("failure", "??????????????????", response.toString()));
    }


    /**
     * ????????????
     *
     * @param message ???????????????????????????
     * @return ??????????????????
     */
    @Override
    public PayMessage createMessage(Map<String, Object> message) {
        return UnionPayMessage.create(message);
    }
}
