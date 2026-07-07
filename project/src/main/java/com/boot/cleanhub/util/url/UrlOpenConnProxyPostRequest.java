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
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;


public class UrlOpenConnProxyPostRequest {

    private String targetUrl;
    private Map<String, String> params;
    private String charset;
    private Map<String, String> headers;
    private Integer connectTimeout;
    private Integer readTimeout;
    private Boolean useSessionCookie;
    private HttpServletResponse response;
    private SSLContext sslContext;
    private HostnameVerifier hostnameVerifier;
    private Boolean isHttps;
    private CookieManager cookieManager;

    private UrlOpenConnProxyPostRequest(Builder builder) {
        this.targetUrl = builder.targetUrl;
        this.params = builder.params;
        this.charset = builder.charset;
        this.headers = builder.headers;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.useSessionCookie = builder.useSessionCookie;
        this.response = builder.response;
        this.isHttps = builder.isHttps;
        this.sslContext = builder.sslContext;
        this.hostnameVerifier = builder.hostnameVerifier;
        this.cookieManager = builder.cookieManager;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String targetUrl;
        private Map<String, String> params;
        private String charset = "UTF-8";
        private Map<String, String> headers;
        private Integer connectTimeout = 5000;
        private Integer readTimeout = 5000;
        private Boolean useSessionCookie = false;
        private HttpServletResponse response;
        private Boolean isHttps = false;
        private SSLContext sslContext;
        private HostnameVerifier hostnameVerifier;
        private CookieManager cookieManager;

        public Builder targetUrl(String targetUrl) {
            this.targetUrl = targetUrl;
            return this;
        }

        public Builder params(Map<String, String> params) {
            this.params = params;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder useSessionCookie(boolean useSessionCookie) {
            this.useSessionCookie = useSessionCookie;
            return this;
        }

        public Builder response(HttpServletResponse response) {
            this.response = response;
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

        public Builder hostnameVerifier(HostnameVerifier hostnameVerifier) {
            this.hostnameVerifier = hostnameVerifier;
            return this;
        }

        public Builder cookieManager(CookieManager cookieManager) {
            this.cookieManager = cookieManager;
            return this;
        }

        public UrlOpenConnProxyPostRequest build() {
            return new UrlOpenConnProxyPostRequest(this);
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

    public Boolean getIsHttps() {
        return isHttps;
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public CookieManager getCookieManager() {
        return cookieManager;
    }

    public Boolean getUseSessionCookie() {
       return useSessionCookie;
    }

    public HttpServletResponse getResponse() {
        return response;
    }
    public static void doHttpPost(UrlOpenConnProxyPostRequest request) throws Exception {
        if (request.getResponse() == null) {
            throw new IllegalArgumentException("프록시 relay 를 위해 response(HttpServletResponse) 가 필요합니다.");
        }
        HttpURLConnection connection = null;
        try {
            // Create URL connection
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
            connection.setDoOutput(true);

            // Set headers
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
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
                out.flush();
            }

            // Handle response
            int responseCode = connection.getResponseCode();

            if (request.getCookieManager() != null) {
                saveCookiesFromResponse(request.getTargetUrl(), connection, request.getCookieManager());
            }

            if (responseCode == HttpURLConnection.HTTP_OK) {
                HttpServletResponse clientResponse = request.getResponse();

                // [1] 백엔드 응답 헤더를 클라이언트로 그대로 relay.
                //     (status line 인 null key 는 제외, Set-Cookie 는 useSessionCookie 일 때만 전달)
                Map<String, List<String>> headerFields = connection.getHeaderFields();
                if (headerFields != null) {
                    for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
                        String headerName = entry.getKey();
                        if (headerName == null) {
                            continue;
                        }
                        if (headerName.equalsIgnoreCase("set-cookie") && !request.getUseSessionCookie()) {
                            continue;
                        }
                        for (String value : entry.getValue()) {
                            clientResponse.addHeader(headerName, value);
                        }
                    }
                }

                // [2] 응답 본문(body)을 클라이언트로 그대로 스트리밍.
                //     인코딩(Content-Encoding 등) 헤더를 위에서 relay 했으므로, 본문은 디코딩 없이 원본 바이트를 전달한다.
                //     (참고: HttpProxyPostRequest 는 gzip 을 디코딩하면서 Content-Encoding 헤더도 함께 넘겨 이중 인코딩되는 문제가 있어, 여기서는 원본 그대로 relay 한다.)
                try (InputStream inputStream = connection.getInputStream();
                        ServletOutputStream outputStream = clientResponse.getOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                }
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
