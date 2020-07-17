package com.egzosn.pay.payoneer.api;
import com.egzosn.pay.common.api.BasePayConfigStorage;

/**
 * Payoneer P卡 支付 配置
 * @author Actinia
 * @author  egan
 * <pre>
 * email hayesfu@qq.com
 * date 2018-01-19
 * </pre>
 */
public class PayoneerConfigStorage extends BasePayConfigStorage {
    /**
     * 商户Id
     */
    public String programId;

    /**
     *  应用id
     * @return 空
     */
    @Override
    public String getAppid() {
        return null;
    }


    /**
     * 合作商唯一标识
     *
     */
    @Override
    public String getPid () {
        return programId;
    }

    @Override
    public String getSeller() {
        return null;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

}
