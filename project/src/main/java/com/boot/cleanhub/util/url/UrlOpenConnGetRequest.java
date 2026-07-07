package com.boot.cleanhub.util.url;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

/**
 * <pre>
 *   UrlOpenConnGetRequest 빌더 객체 *
 * </pre>
 * @author In-seong Hwang
 * @since 2024.06.20
 * @version 1.0
 */
public class UrlOpenConnGetRequest {
    private String targetUrl;
    private Map<String, String> params;
    private String charset;
    private Map<String, String> headers;
    private Integer connectTimeout;
    private Integer readTimeout;
    private SSLContext sslContext;
    private HostnameVerifier hostnameVerifier;
    private Boolean isHttps;
    private Boolean isRedirectable;
    private Boolean hasEncQueryString;
    private CookieManager cookieManager;
    
    private UrlOpenConnGetRequest(Builder builder) {
        this.targetUrl = builder.targetUrl;
        this.params = builder.params;
        this.charset = builder.charset;
        this.headers = builder.headers;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.sslContext = builder.sslContext;
        this.hostnameVerifier = builder.hostnameVerifier;
        this.isHttps = builder.isHttps;
        this.isRedirectable = builder.isRedirectable;
        this.hasEncQueryString = builder.hasEncQueryString;
        this.cookieManager = builder.cookieManager;
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
        private HostnameVerifier hostnameVerifier;
        private Boolean isHttps = false;
        private Boolean isRedirectable = false;
        private Boolean hasEncQueryString = false;
        private CookieManager cookieManager;

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

        public Builder hostnameVerifier(HostnameVerifier hostnameVerifier) {
            this.hostnameVerifier = hostnameVerifier;
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

        public Builder hasEncQueryString(Boolean hasEncQueryString) {
            this.hasEncQueryString = hasEncQueryString;
            return this;
        }

        public Builder cookieManager(CookieManager cookieManager) {
            this.cookieManager = cookieManager;
            return this;
        }

        public UrlOpenConnGetRequest build() {
            return new UrlOpenConnGetRequest(this);
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

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public Boolean getIsHttps() {
        return isHttps;
    }

    public Boolean getIsRedirectable() {
        return isRedirectable;
    }

    public Boolean getHasEncQueryString() {
        return hasEncQueryString;
    }

    public CookieManager getCookieManager() {
        return cookieManager;
    }

    // Exception 처리는 호출부에서 해야 할 듯..?
    public static String doHttpGet(UrlOpenConnGetRequest request) throws Exception {
        StringBuilder responseContent = new StringBuilder();
        HttpURLConnection connection = null;
        try {
            // URL param 인코딩 여부
            StringBuilder urlWithParams = new StringBuilder();
            URL u = new URL(request.getTargetUrl());
            String query = u.getQuery();

            if (request.getHasEncQueryString()) {
                // 쿼리 스트링이 이미 인코딩 된 경우 
                urlWithParams.append(request.getTargetUrl());
            } else {
                StringBuilder domain = new StringBuilder();
                domain.append(u.getProtocol()).append("://").append(u.getHost());
                if (u.getPort() != -1) {
                    domain.append(":").append(u.getPort());
                }
                domain.append(u.getPath());

                urlWithParams.append(domain);
                // Set parameters
                if ( query != null ) { 
                    urlWithParams.append("?");
                    String[] queryArr = query.split("&");
                    for (String s : queryArr) {
                        urlWithParams.append( URLEncoder.encode(s.substring(0, s.indexOf("=")), request.getCharset()) )
                                .append("=")
                                .append( URLEncoder.encode(s.substring(s.indexOf("=") + 1), request.getCharset()) )
                                .append("&");
                    }
                    urlWithParams.deleteCharAt(urlWithParams.length() - 1);
                }
            }

            if (request.getParams() != null && !request.getParams().isEmpty()) {
                if (query != null) {
                    urlWithParams.append("&");
                } else {
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

            URL url = new URL(urlWithParams.toString());
            if (request.getIsHttps()) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) url.openConnection();

                if (request.getSslContext() == null ){
                    request.sslContext = SSLContext.getInstance("TLSv1.2");
                    request.sslContext.init(null, new TrustManager[]{new TrustAllCertificates()}, new java.security.SecureRandom());
                    request.hostnameVerifier = new HostnameVerifier() {
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    };
                }
                httpsConnection.setSSLSocketFactory(request.getSslContext().getSocketFactory());
                httpsConnection.setHostnameVerifier(request.getHostnameVerifier());
                connection = httpsConnection;
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }

            if (request.getCookieManager() !=null ) {
                // (전역 오염 방지) CookieHandler.setDefault 미사용 — 아래 수동 방식으로 요청 단위 쿠키 처리
                String cookies = getCookiesForUrl(request.getTargetUrl(),request.getCookieManager());
                if (cookies != null) {
                    connection.setRequestProperty("Cookie", cookies);
                }
            }
            
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(request.getConnectTimeout());
            connection.setReadTimeout(request.getReadTimeout());
            connection.setInstanceFollowRedirects(request.getIsRedirectable());

            if (request.getHeaders() != null) {
                for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            int responseCode = connection.getResponseCode();

            if (request.getCookieManager() != null) {
                saveCookiesFromResponse(request.getTargetUrl(), connection, request.getCookieManager());
            }

            InputStream inputStream = null;
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String encodingType = connection.getContentEncoding();
                if ("gzip".equalsIgnoreCase(encodingType)) {
                    inputStream = new GZIPInputStream(connection.getInputStream());
                }else{
                    inputStream = connection.getInputStream();
                }
                // Read response
                String contentType = connection.getContentType();
                String charSet = request.getCharset();
                if ( contentType != null ) {
                    contentType = contentType.replaceAll(" ", "");
                    for (String s : contentType.split(";")) {
                        if (s.toLowerCase().startsWith("charset=")) {
                            charSet = s.split("=",2)[1];
                            break;
                        }
                    }
                    if (charSet == null || charSet.isEmpty()) {
                        charSet = StandardCharsets.UTF_8.name();
                    }
                }

                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    int bytesRead;
                    byte[] buffer = new byte[4096];
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    responseContent.append(outputStream.toString(charSet));
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }

            } else if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                responseContent.append(connection.getHeaderField("Location"));
            } else {
                InputStream errorStream = connection.getErrorStream();
                if (errorStream != null) {
                    // InputStream을 문자열로 변환
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = errorStream.read(buffer)) != -1) {
                        baos.write(buffer, 0, length);
                    }
                    String errorResponse = baos.toString("UTF-8");
                    System.err.println("Error Response: " + errorResponse);
                } else {
                    System.err.println("Error without response body. Code: " + responseCode);
                }
                throw new IOException("HTTP error code: " + responseCode);
            }
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return responseContent.toString();
    }

    // SSL 인증, 이 부분은 나중에 인증 방식 바뀌면 바꿔야 할 듯?
    static class TrustAllCertificates implements javax.net.ssl.X509TrustManager {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }
    }

    // 쿠키 가져오기
    private static String getCookiesForUrl(String urlString, CookieManager cookieManager) {
        URI uri = URI.create(urlString);
        List<HttpCookie> cookies = cookieManager.getCookieStore().get(uri);
        StringBuilder cookieHeader = new StringBuilder();
        for (HttpCookie cookie : cookies) {
            if (cookieHeader.length() > 0) {
                cookieHeader.append("; ");
            }
            cookieHeader.append(cookie.getName()).append("=").append(cookie.getValue());
        }
        return cookieHeader.length() > 0 ? cookieHeader.toString() : null;
    }

    // 쿠키 저장하기
    private static void saveCookiesFromResponse(String urlString, HttpURLConnection connection, CookieManager cookieManager) {
        URI uri = URI.create(urlString);
        Map<String, List<String>> headerFields = connection.getHeaderFields();
        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            if (entry.getKey() == null || !entry.getKey().equalsIgnoreCase("Set-Cookie")) {
                continue;
            }
            for (String cookieHeader : entry.getValue()) {
                cookieManager.getCookieStore().add(uri, HttpCookie.parse(cookieHeader).get(0));
            }
        }
    }
}