package com.boot.cleanhub.util.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
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
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
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
import org.apache.http.ssl.SSLContexts;

/**
 * <pre>
 *   HttpGetFileRequest 빌더 객체
 *  추후 SSL 구현을 위해 JDK 8로 올릴시(httpclient-4.5.14,httpcore-4.4.16,httpmime-4.5.14 버전 수정)  주석 처리 없애면 바로 SSL 사용 가능
 * </pre>
 * @author In-seong Hwang
 * @since 2024.06.20
 * @version 1.0
 */
public class HttpGetFileRequest {
    private String targetUrl;
    private String saveFilePath;
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
    private Boolean isOverwrite;
    private Boolean hasEncQueryString;

    private HttpGetFileRequest(Builder builder) {
        this.targetUrl = builder.targetUrl;
        this.saveFilePath = builder.saveFilePath;
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
        this.isOverwrite = builder.isOverwrite;
        this.hasEncQueryString = builder.hasEncQueryString;
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String targetUrl;
        private String saveFilePath;
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
        private Boolean isOverwrite = false;
        private Boolean hasEncQueryString = false;

        public Builder targetUrl(String targetUrl) {
            this.targetUrl = targetUrl;
            return this;
        }
        
        public Builder saveFilePath(String saveFilePath) {
            this.saveFilePath = saveFilePath;
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

        public Builder isOverwrite(Boolean isOverwrite) {
            this.isOverwrite = isOverwrite;
            return this;
        }
        
        public Builder hasEncQueryString(Boolean hasEncQueryString) {
            this.hasEncQueryString = hasEncQueryString;
            return this;
        }

        public HttpGetFileRequest build() {
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

            return new HttpGetFileRequest(this);
        }
    }

    // Getters for the fields
    public String getTargetUrl() {
        return targetUrl;
    }

    public String getSaveFilePath() {
        return saveFilePath;
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

    public Boolean getIsOverwrite() {
        return isOverwrite;
    }

    public Boolean getHasEncQueryString() {
        return hasEncQueryString;
    }

    public static void doHttpGet(HttpGetFileRequest request) throws IllegalArgumentException, IOException, Exception {
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
            
            File saveFile = new File(request.getSaveFilePath());
            if(saveFile.exists()) {
				if(!request.getIsOverwrite()) {
					throw new IllegalArgumentException("[" + request.getSaveFilePath() + "] 파일이 존재하여 다운로드 저장할 수 없습니다.");
				}

				if(saveFile.isDirectory()) {
					throw new IllegalArgumentException("[" + request.getSaveFilePath() + "] 대상이 디렉토리 입니다.");
				}

				if(!saveFile.canWrite()) {
					throw new IllegalArgumentException("[" + request.getSaveFilePath() + "] 파일을 저장할 수 없는 상태입니다.");
				}
			} else {
				if(!saveFile.getParentFile().exists()) {
					if(!saveFile.getParentFile().mkdirs()) {
						throw new IllegalArgumentException("[" + request.getSaveFilePath() + "] 파일을 저장할 디렉토리를 생성할 수 업습니다.");
					}
				}

				if(!saveFile.getParentFile().canWrite()) {
					throw new IllegalArgumentException("[" + request.getSaveFilePath() + "] 파일을 저장할 수 없는 상태입니다.");
				}
			}

            try (CloseableHttpClient httpClient = builder.build()) {
                HttpGet httpGet;
                if(request.getHasEncQueryString()) {
                    // 쿼리 스트링이 이미 인코딩 된 경우 -> uriBuilder 사용 못함
                    // uriBuilder.build() 시 내부적으로 파라미터를 URL 인코딩을 한다 (더블 인코딩시 에러가 날 수 있음)
                    URL u = new URL(request.getTargetUrl());
                    StringBuilder urlWithParams = new StringBuilder();                    
                    urlWithParams.append(request.getTargetUrl());

                    String query = u.getQuery();
                    if (request.getParams() != null && !request.getParams().isEmpty()) {
                        if ( query != null ) { 
                            urlWithParams.append("&");
                        }else{
                            urlWithParams.append("?");
                        }
                        StringBuilder params = new StringBuilder();
                        for (Map.Entry<String, String> entry : request.getParams().entrySet()) {
                            if (params.length() > 0 ) {
                                params.append("&");
                            }

                            params.append(URLEncoder.encode(entry.getKey(),request.getCharset()))
                                    .append("=")
                                    .append(URLEncoder.encode(entry.getValue(),request.getCharset()));
                        }
                        urlWithParams.append(params);
                    }
                    httpGet = new HttpGet(urlWithParams.toString());
                } else {
                    URL u = new URL(request.getTargetUrl());
                    String query = u.getQuery();
                    StringBuilder domain = new StringBuilder();
                    domain.append(u.getProtocol()).append("://").append(u.getHost());
                    if (u.getPort() != -1) {
                        domain.append(":").append(u.getPort());
                    }
                    domain.append(u.getPath());

                    URIBuilder uriBuilder = new URIBuilder(domain.toString());
                    uriBuilder.setCharset(Charset.forName(request.getCharset()));
    
                    // Set parameters
                    if ( query != null ) { 
                        String[] queryArr = query.split("&");
                        for (String s : queryArr) {
                            uriBuilder.addParameter(s.substring(0,s.indexOf("=")), s.substring(s.indexOf("=")+1) );
                        }
                    }

                    if (request.getParams() != null) {
                        for (Map.Entry<String, String> entry : request.getParams().entrySet()) {
                            uriBuilder.addParameter(entry.getKey(), entry.getValue());
                        }
                    }
                    
                    httpGet = new HttpGet(uriBuilder.build());                    
                }

                // Execute and get response
                try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
                    int responseCode = httpResponse.getStatusLine().getStatusCode();
                    HttpEntity httpEntity = null;
    
                    if (responseCode == HttpStatus.SC_OK) {
                        httpEntity = httpResponse.getEntity();
                        if (httpEntity != null) {
                            String encodingType = httpEntity.getContentEncoding() != null
                                    ? httpEntity.getContentEncoding().getValue()
                                    : null;
                            if ("gzip".equalsIgnoreCase(encodingType)) {
                                try (InputStream responseStream = httpEntity.getContent();
                                        GZIPInputStream responseGzipStream = new GZIPInputStream(responseStream);
                                        FileOutputStream outputStream = new FileOutputStream(saveFile,false)) {
                                    int responseBytesRead = 0;
                                    byte[] responseBuffer = new byte[4096];
    
                                    while ((responseBytesRead = responseGzipStream.read(responseBuffer)) != -1) {
                                        outputStream.write(responseBuffer, 0, responseBytesRead);
                                    }                                    
                                }
                            } else {
                                try (InputStream responseStream = httpEntity.getContent();
                                        FileOutputStream outputStream = new FileOutputStream(saveFile,false)) {
                                    int responseBytesRead = 0;
                                    byte[] responseBuffer = new byte[4096];
    
                                    while ((responseBytesRead = responseStream.read(responseBuffer)) != -1) {
                                        outputStream.write(responseBuffer, 0, responseBytesRead);
                                    }                                    
                                } 
                            }
                        }
                    } else {
                        throw new IOException("[" + responseCode + "] 결과값이 전송되었습니다.");
                    }
                }
            }
        } finally{}
    }
}