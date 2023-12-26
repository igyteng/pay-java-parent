package com.egzosn.pay.wx.v3.bean;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * 证书模式运行时环境
 *
 * @author egan
 * email egzosn@gmail.com
 * date 2021/07/18.20:29
 */
public class CertEnvironment {
    /**
     * 存放私钥
     */
    private PrivateKey privateKey;

    /**
     * 存放公钥
     */
    private PublicKey publicKey;

    /**
     * 公钥序列
     */
    private String serialNumber;


    public CertEnvironment() {
    }

    public CertEnvironment(PrivateKey privateKey, PublicKey publicKey, String serialNumber) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.serialNumber = serialNumber;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
}