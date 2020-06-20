package com.egzosn.pay.common.http;

import org.apache.http.Header;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;

import static com.egzosn.pay.common.http.UriVariables.getMapToParameters;

/**
 * 请求实体，包含请求头，内容类型，编码类型等
 *
 * @author egan
 *         <pre>
*               email egzosn@gmail.com
*               date 2017/12/20
*           </pre>
 */
public class HttpStringEntity extends StringEntity {
    /**
     * 请求头
     */
    private List<Header> headers;

    /**
     * 构造器
     *
     * @param request 请求体
     * @param headers 请求头
     *
     * @throws UnsupportedEncodingException 不支持默认的HTTP字符集
     */
    public HttpStringEntity(Map<String, Object> request, Header... headers) throws UnsupportedEncodingException {
        this(getMapToParameters(request), headers);

    }

    /**
     * 构造器
     *
     * @param request 请求体
     * @param headers 请求头
     *
     * @throws UnsupportedEncodingException 不支持默认的HTTP字符集
     */
    public HttpStringEntity(Map<String, Object> request, Map<String, String> headers) throws UnsupportedEncodingException {
        this(getMapToParameters(request), headers);

    }

    /**
     * 构造器
     *
     * @param request     请求体
     * @param contentType 内容类型
     */
    public HttpStringEntity(Map<String, Object> request, ContentType contentType) {
        super(getMapToParameters(request), contentType);
    }

    /**
     * 构造器
     *
     * @param request 请求体
     * @param charset 字符类型
     */
    public HttpStringEntity(Map<String, Object> request, String charset) {
        super(getMapToParameters(request), charset);
    }

    /**
     * 构造器
     *
     * @param request 请求体
     * @param charset 字符类型
     */
    public HttpStringEntity(Map<String, Object> request, Charset charset) {
        super(getMapToParameters(request), charset);
    }

    /**
     * 构造器
     *
     * @param request 请求体
     *
     * @throws UnsupportedEncodingException 不支持默认的HTTP字符集
     */
    public HttpStringEntity(Map<String, Object> request) throws UnsupportedEncodingException {
        super(getMapToParameters(request));
    }

    /**
     * 构造器
     *
     * @param request     请求体
     * @param contentType 内容类型
     *
     * @throws UnsupportedEncodingException 不支持默认的HTTP字符集
     */
    public HttpStringEntity(String request, ContentType contentType) throws UnsupportedCharsetException {
        super(request, contentType);
    }

    /**
     * 构造器
     *
     * @param request 请求体
     * @param charset 字符类型
     *
     * @throws UnsupportedEncodingException 不支持默认的HTTP字符集
     */
    public HttpStringEntity(String request, String charset) throws UnsupportedCharsetException {
        super(request, charset);
    }

    /**
     * 构造器
     *
     * @param request 请求体
     * @param charset 字符类型
     */
    public HttpStringEntity(String request, Charset charset) {
        super(request, charset);
    }

    /**
     * 构造器
     *
     * @param request 请求体
     * @param headers 请求头
     *
     * @throws UnsupportedEncodingException 不支持默认的HTTP字符集
     */
    public HttpStringEntity(String request, Header... headers) throws UnsupportedEncodingException {
        super(request);
        if (null == headers) {
            this.headers = Arrays.asList(headers);
        }
    }

    /**
     * 构造器
     *
     * @param request 请求体
     * @param headers 请求头
     *
     * @throws UnsupportedEncodingException 不支持默认的HTTP字符集
     */
    public HttpStringEntity(String request, Map<String, String> headers) throws UnsupportedEncodingException {
        super(request);
        this.headers = new ArrayList<>();
        for (String key : headers.keySet()) {
            this.headers.add(new BasicHeader(key, headers.get(key)));
        }
    }

    /**
     * 获取请求头集
     *
     * @return 请求头集
     */
    public List<Header> getHeaders() {
        return headers;
    }

    /**
     * 添加请求头
     *
     * @param header 请求头
     */
    public void addHeader(Header header) {
        if (null == this.headers) {
            this.headers = new ArrayList<>();
        }
        this.headers.add(header);
    }

    /**
     * 设置请求头集
     *
     * @param headers 请求头集
     */
    public void setHeaders(List<Header> headers) {
        this.headers = headers;
    }

    /**
     * 设置请求头集
     *
     * @param headers 请求头集
     */
    public void setHeaders(Map<String, String> headers) {
        for (String key : headers.keySet()) {
            addHeader(new BasicHeader(key, headers.get(key)));
        }
    }


}
