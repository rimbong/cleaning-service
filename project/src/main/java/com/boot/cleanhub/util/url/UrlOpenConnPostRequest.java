package com.boot.cleanhub.util.url;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
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

public class UrlOpenConnPostRequest {
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
    private CookieManager cookieManager;
    private String requestBody;

    // Content-Types that should be handled as a raw string entity body.
    private static final String STRING_ENTITY_CONTENT_TYPES = "application/json;application/xml";

    private UrlOpenConnPostRequest(Builder builder) {
        this.targetUrl = builder.targetUrl;
        this.params = builder.params;
        this.charset = builder.charset;
        this.headers = builder.headers;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.sslContext = builder.sslContext;
        this.isHttps = builder.isHttps;
        this.isRedirectable = builder.isRedirectable;
        this.hostnameVerifier = builder.hostnameVerifier;
        this.cookieManager = builder.cookieManager;
        this.requestBody = builder.requestBody;
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
        private Boolean isHttps = false;
        private Boolean isRedirectable = false;
        private HostnameVerifier hostnameVerifier;
        private CookieManager cookieManager;
        private String requestBody;

        public Builder targetUrl(String targetUrl) {
            this.targetUrl = targetUrl;
            return this;
        }

        public Builder params(Map<String, String> params) {
            this.params = params;
            return this;
        }
        
        public Builder requestBody(String requestBody) {
            this.requestBody = requestBody;
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

        public Builder isHttps(Boolean isHttps) {
            this.isHttps = isHttps;
            return this;
        }

        public Builder isRedirectable(Boolean isRedirectable) {
            this.isRedirectable = isRedirectable;
            return this;
        }

        public Builder hostnameVerifier(HostnameVerifier hostnameVerifier) {
            this.hostnameVerifier = hostnameVerifier;
            return this;
        }

        public Builder cookieManager(CookieManager cookieManager) {
            this.cookieManager = cookieManager;
            return this;
        }

        public UrlOpenConnPostRequest build() {
            System.setProperty("http.keepAlive", "true");
            System.setProperty("http.maxConnections", "100");
            
            return new UrlOpenConnPostRequest(this);
        }
    }

    // Getters for the fields
    public String getTargetUrl() {
        return targetUrl;
    }

    public Map<String, String> getParams() {
        return params;
    }
    
    public String getRequestBody() {
        return requestBody;
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

    public Boolean getIsHttps() {
        return isHttps;
    }

    public Boolean getIsRedirectable() {
        return isRedirectable;
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public CookieManager getCookieManager() {
        return cookieManager;
    }

    public static String doHttpPost(UrlOpenConnPostRequest request) throws Exception {
        StringBuilder responseContent = new StringBuilder();
        HttpURLConnection connection = null;
        try {
            URL url = new URL(request.getTargetUrl());
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

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(request.getConnectTimeout());
            connection.setReadTimeout(request.getReadTimeout());
            connection.setInstanceFollowRedirects(request.getIsRedirectable());
            connection.setDoOutput(true);

            // Set headers from the request object
            boolean isStringEntity = false;
            
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
                if ("Content-Type".equalsIgnoreCase(header.getKey())) {
                    String contentType = header.getValue();
                    String[] contentTypes = STRING_ENTITY_CONTENT_TYPES.split(";");
                    for (String ct : contentTypes) {
                        if (contentType.toLowerCase().contains(ct.toLowerCase())) {
                            isStringEntity = true;
                            break;
                        }
                    }
                }
            }
            
            if(!isStringEntity) {
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=" + request.getCharset());
            }
           
            try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                if (isStringEntity) {
                    // Handle as raw string body (e.g., JSON, XML)
                    if (request.getRequestBody() != null) {
                        out.write(request.getRequestBody().getBytes(request.getCharset()));
                    }
                } else {
                    StringBuilder params = new StringBuilder();
                    if (request.getParams() != null && !request.getParams().isEmpty()) {
                        for (Map.Entry<String, String> entry : request.getParams().entrySet()) {
                            if (params.length() > 0) {
                                params.append("&");
                            }
                            params.append(URLEncoder.encode(entry.getKey(), request.getCharset()))
                                    .append("=")
                                    .append(URLEncoder.encode(entry.getValue(), request.getCharset()));
                        }
                    }
                    out.writeBytes(params.toString());
                }
                out.flush();
            }

            int responseCode = connection.getResponseCode();

            if (request.getCookieManager() != null) {
                saveCookiesFromResponse(request.getTargetUrl(), connection, request.getCookieManager());
            }

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream;
                String contentEncoding = connection.getContentEncoding();
                if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
                    inputStream = new GZIPInputStream(connection.getInputStream());
                } else {
                    inputStream = connection.getInputStream();
                }

                String responseCharSet = request.getCharset(); // Default to request charset
                String responseContentType = connection.getContentType();
                if ( responseContentType != null ) {
                    responseContentType = responseContentType.replaceAll(" ", "");
                    for (String s : responseContentType.split(";")) {
                        if (s.toLowerCase().startsWith("charset=")) {
                            responseCharSet = s.split("=",2)[1];
                            break;
                        }
                    }
                }

                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    int bytesRead;
                    byte[] buffer = new byte[4096];
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    responseContent.append(outputStream.toString(responseCharSet));
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
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = errorStream.read(buffer)) != -1) {
                        baos.write(buffer, 0, length);
                    }
                    String errorResponse = baos.toString("UTF-8");
                    System.err.println("Error Response: " + errorResponse);
                    errorStream.close();
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
