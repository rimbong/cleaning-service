package com.boot.cleanhub.util.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

/**
 * <pre>
 *   HttpPostRequest 빌더 객체
 *  추후 SSL 구현을 위해 JDK 8로 올릴시(httpclient-4.5.14,httpcore-4.4.16,httpmime-4.5.14 버전 수정)  주석 처리 없애면 바로 SSL 사용 가능
 * </pre>
 * @author In-seong Hwang
 * @since 2024.06.20
 * @version 1.0
 */
public class HttpPostRequest {
    private static final String STRING_ENTITY_BODY_HEADER = "application/json;application/xml";
    private String targetUrl;
    private Map<String, String> params;
    private String bodyData;
    private String charset;
    private Map<String, String> headers;
    private Integer connectTimeout;
    private Integer readTimeout;
    private SSLContext sslContext;
    private RequestConfig requestConfig;
    private PoolingHttpClientConnectionManager connectionManager;
    private List<Header> headerList;
    private HostnameVerifier hostnameVerifier;
    private CookieStore cookieStore;
    private Boolean isHttps;
    private Boolean isRedirectable;


    private HttpPostRequest(Builder builder) {
        this.targetUrl = builder.targetUrl;
        this.params = builder.params;
        this.bodyData = builder.bodyData;
        this.charset = builder.charset;
        this.headers = builder.headers;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.sslContext = builder.sslContext;
        this.requestConfig = builder.requestConfig;
        this.connectionManager = builder.connectionManager;
        this.headerList = builder.headerList;
        this.hostnameVerifier = builder.hostnameVerifier;
        this.cookieStore = builder.cookieStore;
        this.isHttps = builder.isHttps;
        this.isRedirectable = builder.isRedirectable;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String targetUrl;
        private Map<String, String> params = new HashMap<>();
        private String bodyData;
        private String charset = "UTF-8";
        private Map<String, String> headers = new HashMap<>();
        private Integer connectTimeout = 5000;
        private Integer readTimeout = 5000;
        private SSLContext sslContext;
        private RequestConfig requestConfig;
        private PoolingHttpClientConnectionManager connectionManager;
        private List<Header> headerList = new ArrayList<>();
        private HostnameVerifier hostnameVerifier;
        private CookieStore cookieStore;
        private Boolean isHttps = false;
        private Boolean isRedirectable = false;

        public Builder targetUrl(String targetUrl) {
            this.targetUrl = targetUrl;
            return this;
        }

        public Builder params(Map<String, String> params) {
            this.params = params;
            return this;
        }

        public Builder bodyData(String bodyData) {
            this.bodyData = bodyData;
            return this;
        }

        public Builder charset(String charset) {
            this.charset = charset;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }
      
        public Builder connectTimeout(Integer connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(Integer readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder sslContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public Builder requestConfig(RequestConfig requestConfig) {
            this.requestConfig = requestConfig;
            return this;
        }

        public Builder connectionManager(PoolingHttpClientConnectionManager connectionManager) {
            this.connectionManager = connectionManager;
            return this;
        }

        public Builder headerList(List<Header> headerList) {
            this.headerList = headerList;
            return this;
        }

        public Builder hostnameVerifier(HostnameVerifier hostnameVerifier) {
            this.hostnameVerifier = hostnameVerifier;
            return this;
        }

        public Builder cookieStore(CookieStore cookieStore) {
            this.cookieStore = cookieStore;
            return this;
        }

        public Builder isHttps(Boolean isHttps) {
            this.isHttps = isHttps;
            return this;
        }

        public Builder isRedirectable(Boolean isRedirectable) {
            this.isRedirectable = isRedirectable;
            return this;
        }

        public HttpPostRequest build() {
            // Set defaults if not provided
            if(this.isHttps) {
                if (this.sslContext == null) {

                    /* 
                    아래 설정은 SSLContextBuilder를 사용하여 SSL 컨텍스트를 직접 빌드하며, TrustSelfSignedStrategy를 통해 **자가 서명된 인증서(self-signed certificates)**도 신뢰하도록 설정합니다. 
                    즉, 공인된 인증서뿐만 아니라 자가 서명된 인증서도 허용하는 방식입니다. 개발 환경이나 테스트 환경에서 자주 사용되는 자가 서명된 인증서도 신뢰할 수 있도록 설정됩니다. 
                    별도의 키스토어에 인증서를 추가하지 않아도 됩니다.

                    SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(new TrustSelfSignedStrategy())
                    .build(); 
                    */


                    /* 
                    Apache HttpComponents 라이브러리에서 제공하는 기본 SSL 컨텍스트를 생성합니다.
                    SSLContexts.createDefault()는 자바 시스템에서 기본적으로 신뢰하는 인증서를 기반으로 SSL 통신을 설정합니다. 
                    즉, 이 방식은 운영 체제나 자바 런타임 환경에 내장된 **신뢰할 수 있는 인증 기관(CA)**의 인증서 목록을 사용하여 SSL/TLS 인증서를 검증합니다.
                    장점: 
                    보안성이 더 높습니다. 왜냐하면, 시스템이 신뢰하는 CA 인증서를 기반으로 SSL 연결을 검증하기 때문에 신뢰할 수 없는 연결을 차단할 수 있습니다.
                    자바 시스템에 이미 설치된 인증서(CA)를 사용하기 때문에 별도의 수동 인증서 관리가 필요하지 않습니다.

                    단점: 만약 서버가 자체 서명된 인증서(self-signed certificate)를 사용한다면, 기본 설정에서는 인증이 실패합니다. 이런 경우엔 시스템에 해당 인증서를 수동으로 추가해야 합니다. 
                    */
                    this.sslContext = SSLContexts.createDefault();

                    /* 
                    클라이언트 <-------------> 서버  ( 이 설정은 클라이언트에서 https ssl 설정을 하는 작업 )
                    자바 애플리케이션에서 **자체 서명된 인증서(self-signed certificate)**나 신뢰할 수 없는 인증서를 수동으로 추가하려면, 
                    해당 인증서를 **Java 키스토어(Keystore)**에 추가하여 신뢰할 수 있도록 구성해야 합니다. 이는 keytool이라는 자바 내장 도구를 사용하여 처리할 수 있습니다. 
                    아래는 그 과정에 대한 단계별 설명입니다. 
                    
                    1. cacerts 파일 위치 확인
                    자바 키스토어(cacerts) 파일은 JRE(Java Runtime Environment)의 lib/security 폴더에 위치합니다.

                    예: /path/to/java/jre/lib/security/cacerts

                    2. 인증서 추가하기
                    인증서를 키스토어에 추가하려면 keytool 명령을 사용합니다 
                    keytool -importcert -file /path/to/certificate.crt -keystore /path/to/java/jre/lib/security/cacerts -alias mycert
                        /path/to/certificate.crt는 앞서 추출한 인증서 파일의 경로입니다.
                        /path/to/java/jre/lib/security/cacerts는 JRE에서 사용하는 키스토어 파일 경로입니다.
                        -alias mycert는 인증서에 부여할 별칭입니다. 고유하게 설정해 주세요.

                    3. 키스토어 비밀번호
                    keytool 명령을 실행하면 키스토어에 액세스하기 위한 비밀번호를 묻습니다. 기본 비밀번호는 changeit입니다. 만약 비밀번호를 변경한 적이 있다면 변경된 비밀번호를 입력해야 합니다.

                    4. 인증서 추가 후 확인
                    인증서가 성공적으로 추가되었다면 keytool 명령어로 확인할 수 있습니다.
                    keytool -list -v -keystore /path/to/java/jre/lib/security/cacerts

                    */

                   /* 
                   클라이언트 <-------------> 서버  ( 이 설정은 서버에서 https ssl 설정을 하는 작업 )
                   
                   https://ikinox.tistory.com/16
                   
                    WAS에 SSL을 설정한다는 것은 서버 측에서 HTTPS(SSL/TLS) 연결을 설정하는 것을 의미합니다. 주로 다음과 같은 작업이 포함됩니다:

                    SSL 인증서 설치: 공인된 인증 기관(CA)에서 발급받은 인증서를 서버에 설치합니다. 자가 서명된 인증서를 사용할 수도 있지만, 
                    보통 공인된 인증서를 사용하여 클라이언트가 서버를 신뢰할 수 있도록 합니다.
                    SSL 포트 설정: 서버에서 HTTPS를 통해 연결을 처리할 수 있도록 포트(보통 443번 포트)를 설정합니다.
                    SSL 키스토어 관리: 서버는 자체적으로 키스토어(서버의 인증서를 저장하는 파일)를 관리하고, 이 키스토어에 설치된 인증서를 사용하여 클라이언트와의 SSL/TLS 연결을 처리합니다.
                    예시: Tomcat, WebSphere, JBoss 같은 WAS에서 SSL을 설정하는 과정은 WAS가 HTTPS 요청을 처리할 수 있도록 SSL 인증서를 바인딩하고, SSL 연결을 처리하도록 설정하는 과정입니다. 
                    이 작업은 서버 관리자가 수행합니다.

                    이후 server.xml에  아래 설정을 한다.
                    connector port="443" protocol="org.apache.coyote.http11.Http11NioProtocol" 
                    maxthreads="150" sslenabled="true" scheme="https" secure="true" 
                    clientauth="false" sslprotocol="TLS" 
                    keystorepass="password" keystorefile="C:/OpenSSL-Win64/bin/.keystore" 


                    차이점 요약
                    서버 측 SSL 설정 (WAS): 서버가 클라이언트와의 SSL 연결을 처리할 수 있도록 설정. 서버에 인증서를 설치하고 HTTPS 연결을 수락.
                    클라이언트 측 SSL 설정 (자바 애플리케이션): 자바 애플리케이션이 서버와 안전한 SSL 연결을 설정할 수 있도록 클라이언트에서 인증서 검증. 자바 키스토어에 신뢰할 수 있는 인증서를 추가하거나, SSLContext를 설정.

                    */
                }
                if (this.hostnameVerifier == null) {
                    this.hostnameVerifier = NoopHostnameVerifier.INSTANCE;
                }
            }
            /* 
            connectionManager는 Apache HttpClient에서 HTTP 연결을 관리하는 역할을 합니다. 
            특히 PoolingHttpClientConnectionManager는 여러 요청에서 HTTP 연결을 재사용하고, 연결 풀을 관리하는 기능을 제공합니다.

            역할:
            1) 연결 재사용: HTTP 클라이언트는 각 요청마다 새로운 연결을 생성하는 대신, 기존 연결을 재사용할 수 있습니다. 이는 성능 향상에 매우 유리하며, 특히 다수의 요청을 동일한 서버로 보낼 때 효과적입니다.
            2) 연결 풀 관리: PoolingHttpClientConnectionManager는 일정한 수의 연결을 풀에 보관하고 필요에 따라 연결을 할당합니다. 풀에 여유 연결이 없으면 새 연결을 생성하거나, 기존 연결이 해제될 때까지 대기할 수 있습니다.
            3) 동시성 관리: 여러 스레드에서 동시에 HTTP 요청을 보내는 경우, PoolingHttpClientConnectionManager는 이러한 요청들이 충돌하지 않도록 연결을 안전하게 관리합니다.

            Apache HttpClient에서의 사용:
            Apache HttpClient에서 connectionManager를 설정하는 것은 성능을 최적화하고, 리소스 사용을 효율적으로 관리하기 위해 권장되는 방법입니다. 특히 PoolingHttpClientConnectionManager를 사용하면 다중 연결을 효과적으로 관리할 수 있어, 고부하 환경에서 매우 유용합니다.

            해당 코드에서 PoolingHttpClientConnectionManager를 사용하면, HTTPS를 포함한 HTTP 요청을 효율적으로 관리하며, 필요한 SSL 설정(예: SSLContext와 HostnameVerifier)을 적용한 상태로 안전하게 통신할 수 있습니다. 
            */
            if ( this.connectionManager == null) {
                RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.<ConnectionSocketFactory>create()
                                                                                .register("http", PlainConnectionSocketFactory.INSTANCE);
                if (this.isHttps) {
                    registryBuilder.register("https", new SSLConnectionSocketFactory(this.sslContext,this.hostnameVerifier));
                }
                this.connectionManager = new PoolingHttpClientConnectionManager(registryBuilder.build());
            }
            
            if (this.requestConfig == null) {
                this.requestConfig = RequestConfig.custom()
                        .setSocketTimeout(readTimeout)
                        .setConnectTimeout(connectTimeout)
                        .setConnectionRequestTimeout(connectTimeout)
                        .build();
            }

            if(this.headers != null) {
                for (Map.Entry<String,String> e : this.headers.entrySet()) {
                    this.headerList.add(new BasicHeader(e.getKey(), e.getValue()));
                }
            }
            
            return new HttpPostRequest(this);
        }
    }
    
    // Getters for the fields
    public String getTargetUrl() {
        return targetUrl;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getBodyData() {
        return bodyData;
    }

    public String getCharset() {
        return charset;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public RequestConfig getRequestConfig() {
        return requestConfig;
    }

    public PoolingHttpClientConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public List<Header> getHeaderList() {
        return headerList;
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public CookieStore getCookieStore() {
        return cookieStore;
    }

    public Boolean getIsHttps() {
        return isHttps;
    }
    
    public Boolean getIsRedirectable() {
        return isRedirectable;
    }

    public static String doHttpPost(HttpPostRequest request) throws IllegalArgumentException, IOException, Exception {
        StringBuilder responseContent = new StringBuilder();
        HttpClientBuilder builder = null;
        try {
            // Create an HttpClient with the provided configuration
            builder = HttpClients.custom()
                                .setConnectionManager(request.getConnectionManager())
                                .setDefaultRequestConfig(request.getRequestConfig())                                
                                .setDefaultHeaders(request.getHeaderList())
                                .setDefaultCookieStore(request.getCookieStore());
            if (request.getIsHttps()) {
                builder.setSSLContext(request.getSslContext())
                       .setSSLHostnameVerifier(request.getHostnameVerifier());
            } 

            if (!request.getIsRedirectable()) {
                builder.disableRedirectHandling();
            }

            try (CloseableHttpClient httpClient = builder.build()) {
                URI uri = new URI(request.getTargetUrl());
                HttpPost httpPost = new HttpPost(uri);
    
                // Set parameters
                String contentType = "";
                for (Header header : request.getHeaderList()) {
                    if ("Content-Type".equalsIgnoreCase(header.getName())) {
                        contentType = header.getValue();
                        break;
                    }
                }
                
                if (contentType.length()>0 && STRING_ENTITY_BODY_HEADER.contains(contentType)) {
                    StringEntity entity = new StringEntity( request.getBodyData(), ContentType.APPLICATION_JSON);
                    httpPost.setEntity(entity);
                } else {
                    if (request.getParams() != null) {
                        List<NameValuePair> params = new ArrayList<>();
                        for (Map.Entry<String, String> entry : request.getParams().entrySet()) {
                            params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                        }
                        httpPost.setEntity(new UrlEncodedFormEntity(params, request.getCharset()));
                    }    
                }

                // Execute and get response
                try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
                    int responseCode = httpResponse.getStatusLine().getStatusCode();
                    HttpEntity httpEntity = null;
                    Header[] headers = null;
    
                    if (responseCode == HttpStatus.SC_OK) {
                        httpEntity = httpResponse.getEntity();
                        if (httpEntity != null) {
                            String encodingType = httpEntity.getContentEncoding() != null
                                    ? httpEntity.getContentEncoding().getValue()
                                    : null;
                            if ("gzip".equalsIgnoreCase(encodingType)) {
                                try (InputStream responseStream = httpEntity.getContent();
                                        GZIPInputStream responseGzipStream = new GZIPInputStream(responseStream);
                                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                                    int responseBytesRead = 0;
                                    byte[] responseBuffer = new byte[4096];
    
                                    while ((responseBytesRead = responseGzipStream.read(responseBuffer)) != -1) {
                                        outputStream.write(responseBuffer, 0, responseBytesRead);
                                    }
                                    responseContent.append(outputStream.toString(request.getCharset()));
                                }
    
                            } else {
                                responseContent.append(EntityUtils.toString(httpEntity, request.getCharset()));
                            }
                        }
                    } else if (responseCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                        headers = httpResponse.getHeaders("Location");
                        if (headers.length > 0) {
                            responseContent.append(headers[0].getValue());
                        }
                    } else {
                        throw new IOException();
                    }
                }
            }
            return responseContent.toString();
        }finally{}
    } 
}