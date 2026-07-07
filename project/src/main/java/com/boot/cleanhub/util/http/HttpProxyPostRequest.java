package com.boot.cleanhub.util.http;

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
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;

/**
 * <pre>
 *   HttpProxyPostRequest 빌더 객체
 *  추후 SSL 구현을 위해 JDK 8로 올릴시(httpclient-4.5.14,httpcore-4.4.16,httpmime-4.5.14 버전 수정)  주석 처리 없애면 바로 SSL 사용 가능
 * </pre>
 * @author In-seong Hwang
 * @since 2024.06.20
 * @version 1.0
 */
public class HttpProxyPostRequest {
    private String targetUrl;
    private Map<String, String> params;
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
    private Boolean useSessionCookie;
    private HttpServletResponse response;

    private HttpProxyPostRequest(Builder builder) {
        this.targetUrl = builder.targetUrl;
        this.params = builder.params;
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
        this.useSessionCookie = builder.useSessionCookie;
        this.response = builder.response;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String targetUrl;
        private Map<String, String> params = new HashMap<>();
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
        private Boolean useSessionCookie = false;
        private HttpServletResponse response;

        public Builder targetUrl(String targetUrl) {
            this.targetUrl = targetUrl;
            return this;
        }

        public Builder params(Map<String, String> params) {
            this.params = params;
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

        public Builder useSessionCookie(Boolean useSessionCookie) {
            this.useSessionCookie = useSessionCookie;
            return this;
        }

        public Builder response(HttpServletResponse response) {
            this.response = response;
            return this;
        }

        public HttpProxyPostRequest build() {
            // Set defaults if not provided
            if(this.isHttps) {
                if (this.sslContext == null) {
                    this.sslContext = SSLContexts.createDefault();
                }
                if (this.hostnameVerifier == null) {
                    this.hostnameVerifier = NoopHostnameVerifier.INSTANCE;
                }
            }

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
            
            if (this.response == null) {
                return null;
            }
            return new HttpProxyPostRequest(this);
        }
    }

    // Getters for the fields
    public String getTargetUrl() {
        return targetUrl;
    }

    public Map<String, String> getParams() {
        return params;
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

    public Boolean getUseSessionCookie() {
        return useSessionCookie;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public static void doHttpPost(HttpProxyPostRequest request) throws IllegalArgumentException, IOException, Exception {
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
                if (request.getParams() != null) {
                    List<NameValuePair> params = new ArrayList<>();
                    for (Map.Entry<String, String> entry : request.getParams().entrySet()) {
                        params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                    }
                    httpPost.setEntity(new UrlEncodedFormEntity(params, request.getCharset()));
                }

                // Execute and get response
                try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
                    int responseCode = httpResponse.getStatusLine().getStatusCode();
                    HttpEntity httpEntity = null;
                    Header[] responseHeaders = null;
                    
                    if (responseCode == HttpStatus.SC_OK) {
                        httpEntity = httpResponse.getEntity();
                        HttpServletResponse  response = request.getResponse();
                        if(httpEntity != null) {
                            responseHeaders = httpResponse.getAllHeaders();
                            if(responseHeaders != null && responseHeaders.length > 0) {
                                for(int i = 0; i < responseHeaders.length; i++) {
                                    if(responseHeaders[i].getName().equalsIgnoreCase("set-cookie")) {
                                        if( request.getUseSessionCookie() ) {
                                            response.addHeader(responseHeaders[i].getName(), responseHeaders[i].getValue());
                                        }
                                    } 
                                    response.addHeader(responseHeaders[i].getName(), responseHeaders[i].getValue());
                                }
                            }
                            try (InputStream responseStream = httpEntity.getContent();
                                    ServletOutputStream outputStream = response.getOutputStream();) {
                                String responseContentEncoding = httpEntity.getContentEncoding() != null ? httpEntity.getContentEncoding().getValue() : null;
                                int responseBytesRead = 0;
                                byte[] responseBuffer = new byte[4096];
                                if("gzip".equalsIgnoreCase(responseContentEncoding)) {
                                    try (GZIPInputStream responseGzipStream = new GZIPInputStream(responseStream);) {
                                        while((responseBytesRead = responseStream.read(responseBuffer)) != -1) {
                                            outputStream.write(responseBuffer, 0, responseBytesRead);
                                        }
                                    } 
                                } else {
                                    while((responseBytesRead = responseStream.read(responseBuffer)) != -1) {
                                        outputStream.write(responseBuffer, 0, responseBytesRead);
                                    }
                                }
                                outputStream.flush();
                            } 
                        }
                    } else {
                        throw new IOException();
                    }
                }
            }
        } finally{}
    }
}