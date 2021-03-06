

## 支付宝支付简单例子

#### 支付配置
##### 普通公钥
```java
    
        
        AliPayConfigStorage aliPayConfigStorage = new AliPayConfigStorage();
        aliPayConfigStorage.setPid("合作者id");
        aliPayConfigStorage.setAppId("应用id");
//      aliPayConfigStorage.setAppAuthToken("ISV代商户代用，指定appAuthToken");
        aliPayConfigStorage.setKeyPublic("支付宝公钥");
        aliPayConfigStorage.setKeyPrivate("应用私钥");
        aliPayConfigStorage.setNotifyUrl("异步回调地址");
        aliPayConfigStorage.setReturnUrl("同步回调地址");
        aliPayConfigStorage.setSignType("签名方式");
        aliPayConfigStorage.setSeller("收款账号");
        aliPayConfigStorage.setInputCharset("utf-8");
        //是否为测试账号，沙箱环境
        aliPayConfigStorage.setTest(true);
        
```
##### 证书公钥
```java
    
        
        AliPayConfigStorage aliPayConfigStorage = new AliPayConfigStorage();
        aliPayConfigStorage.setPid("合作者id");
        aliPayConfigStorage.setAppId("应用id");
//      aliPayConfigStorage.setAppAuthToken("ISV代商户代用，指定appAuthToken");
        aliPayConfigStorage.setKeyPrivate("应用私钥");
        //设置为证书方式
        aliPayConfigStorage.setCertSign(true);
        //设置证书存储方式，这里为路径
        aliPayConfigStorage.setCertStoreType(CertStoreType.PATH);
        aliPayConfigStorage.setMerchantCert("请填写您的应用公钥证书文件路径，例如：d:/appCertPublicKey_2019051064521003.crt");
        aliPayConfigStorage.setAliPayCert("请填写您的支付宝公钥证书文件路径，例如：d:/alipayCertPublicKey_RSA2.crt");
        aliPayConfigStorage.setAliPayRootCert("请填写您的支付宝根证书文件路径，例如：d:/alipayRootCert.crt");
        aliPayConfigStorage.setNotifyUrl("异步回调地址");
        aliPayConfigStorage.setReturnUrl("同步回调地址");
        aliPayConfigStorage.setSignType("签名方式");
        aliPayConfigStorage.setSeller("收款账号");
        aliPayConfigStorage.setInputCharset("utf-8");
        //是否为测试账号，沙箱环境
        aliPayConfigStorage.setTest(true);
        
```


#### 网络请求配置

```java

        HttpConfigStorage httpConfigStorage = new HttpConfigStorage();
        /* 网路代理配置 根据需求进行设置**/
        //http代理地址
        httpConfigStorage.setHttpProxyHost("192.168.1.69");
        //代理端口
        httpConfigStorage.setHttpProxyPort(3308);
        //代理用户名
        httpConfigStorage.setAuthUsername("user");
        //代理密码
        httpConfigStorage.setAuthPassword("password");
        /* /网路代理配置 根据需求进行设置**/
    
         /* 网络请求ssl证书 根据需求进行设置**/
        //设置ssl证书路径 跟着setCertStoreType 进行对应
        httpConfigStorage.setKeystore("证书文件流，证书字符串信息或证书绝对地址");
        //设置ssl证书对应的密码
        httpConfigStorage.setStorePassword("证书对应的密码");
        //设置ssl证书对应的存储方式
        httpConfigStorage.setCertStoreType(CertStoreType.PATH);
        
        /* /网络请求ssl证书**/
        
      /* /网络请求连接池**/
        //最大连接数
        httpConfigStorage.setMaxTotal(20);
        //默认的每个路由的最大连接数
        httpConfigStorage.setDefaultMaxPerRoute(10);
        
```


#### 创建支付服务


```java
    //支付服务
     AliPayService service = new AliPayService(aliPayConfigStorage);

     //设置网络请求配置根据需求进行设置
     //service.setRequestTemplateConfigStorage(httpConfigStorage)
     
```
#### 精简版支付回调配置，主要用于实现业务与支付代码隔离使用，下面会讲到支付回调处理
```java

      //增加支付回调消息拦截器
      service.addPayMessageInterceptor(new AliPayMessageInterceptor());
      //设置回调消息处理
      service.setPayMessageHandler(spring.getBean(AliPayMessageHandler.class));

```


#### 创建支付订单信息

```java

        //支付订单基础信息
        PayOrder payOrder = new PayOrder("订单title", "摘要",  BigDecimal.valueOf(0.01) , UUID.randomUUID().toString().replace("-", ""));
  
``` 

#### 扫码付

```java

 
        /*-----------扫码付-------------------*/
        payOrder.setTransactionType(AliTransactionType.SWEEPPAY);
        //获取扫码付的二维码
//        String image = service.getQrPay(payOrder);
        BufferedImage image = service.genQrPay(payOrder);
        /*-----------/扫码付-------------------*/

``` 

#### APP支付

```java

        /*-----------APP-------------------*/
        payOrder.setTransactionType(AliTransactionType.APP);
        //获取APP支付所需的信息组，直接给app端就可使用
        Map appOrderInfo = service.orderInfo(payOrder);
        /*-----------/APP-------------------*/

``` 
#### 小程序支付

```java

        /*-----------APP-------------------*/
        payOrder.setTransactionType(AliTransactionType.MINAPP);
        payOrder.setOpenid("支付宝小程序授权登录成功后获取到的支付宝 user_id")
        //获取小程序支付所需的信息组，直接给小程序网页端就可使用
        Map appOrderInfo = service.orderInfo(payOrder);
        /*-----------/APP-------------------*/

``` 

#### 即时到帐 WAP 网页支付

```java

        /*-----------即时到帐 WAP 网页支付-------------------*/
//        payOrder.setTransactionType(AliTransactionType.WAP); //WAP支付

        payOrder.setTransactionType(AliTransactionType.PAGE); // 即时到帐 PC网页支付
        //获取支付所需的信息
        Map directOrderInfo = service.orderInfo(payOrder);
        //获取表单提交对应的字符串，将其序列化到页面即可,
        String directHtml = service.buildRequest(directOrderInfo, MethodType.POST);
        /*-----------/即时到帐 WAP 网页支付-------------------*/

``` 

#### 条码付 声波付

```java

        /*-----------条码付 声波付-------------------*/

//        payOrder.setTransactionType(AliTransactionType.WAVE_CODE); //声波付
        payOrder.setTransactionType(AliTransactionType.BAR_CODE);//条码付

        payOrder.setAuthCode("条码信息或者声波信息");
        // 支付结果
        Map params = service.microPay(payOrder);
        /*-----------/条码付 声波付-------------------*/

``` 

#### 回调处理
###### 方式一

```java

        /*-----------回调处理-------------------*/
           //HttpServletRequest request;
         Map<String, Object> params = service.getParameter2Map(request.getParameterMap(), request.getInputStream());
        if (service.verify(params)){
            System.out.println("支付成功");
            return;
        }
        System.out.println("支付失败");


        /*-----------回调处理-------------------*/

```
###### 方式二，对应的业务逻辑在对应的处理器里面执行

```java

        /*-----------回调处理-------------------*/
           //HttpServletRequest request;
           System.out.println(service.payBack(request.getParameterMap(), request.getInputStream()).toMessage());


        /*-----------回调处理-------------------*/

```



#### 统一收单交易结算接口

```java
        OrderSettle order = new OrderSettle();
        order.setTradeNo("支付宝单号");
        order.setOutRequestNo("商户单号");
        order.setAmount(new BigDecimal(100));
        order.setDesc("线下转账");
        Map result = service.settle(order);

```


#### 支付订单查询

```java
        
      Map result = service.query("支付宝单号", "我方系统单号");

```


#### 交易关闭接口
  ```java

          Map result = service.close("支付宝单号", "我方系统单号");

```
#### 交易撤销接口
  ```java

          Map result = service.cancel("支付宝单号", "我方系统单号");

```


#### 申请退款接口
  ```java
          //过时方法
         //Map result = service.refund("支付宝单号", "我方系统单号", "退款金额", "订单总金额");
         //支付宝单号与我方系统单号二选一
         RefundOrder order = new RefundOrder("支付宝单号", "我方系统单号", "退款金额", "订单总金额");
         //非必填， 根据业务需求而定，可用于多次退款
         order.setRefundNo("退款单号")
         AliRefundResult result = service.refund(order);

```


#### 查询退款
  ```java
        RefundOrder order = new RefundOrder();
        order.setOutTradeNo("我方系统商户单号");
        order.setTradeNo("支付宝单号");
        //退款金额
        order.setRefundAmount(new BigDecimal(1));
        order.setRefundNo("退款单号");
        order.setDescription("");
        Map result = service.refundquery();
            
```

#### 下载对账单
  ```java

          Map result = service.downloadbill("账单时间：日账单格式为yyyy-MM-dd，月账单格式为yyyy-MM", "账单类型");

```

#### 转账
  ```java
        order.setOutBizNo("转账单号");
        order.setTransAmount(new BigDecimal(10));
        order.setOrderTitle("转账业务的标题");
        order.setIdentity("参与方的唯一标识");
        order.setIdentityType("参与方的标识类型，目前支持如下类型：");
        order.setName("参与方真实姓名");
        order.setRemark("转账备注, 非必填");
        //单笔无密转账到支付宝账户
        order.setTransferType(AliTransferType.TRANS_ACCOUNT_NO_PWD);
        //单笔无密转账到银行卡
//        order.setTransferType(AliTransferType.TRANS_BANKCARD_NO_PWD);
        Map result = service.transfer(order);

```
#### 转账查询
  ```java
       Map result = service.transferQuery("商户转账订单号", "支付平台转账订单号");
```
