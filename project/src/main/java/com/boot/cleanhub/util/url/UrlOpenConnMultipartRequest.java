package com.boot.cleanhub.util.url;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

public class UrlOpenConnMultipartRequest {

    private String targetUrl;
    private Map<String, Object> params;
    private String charset;
    private Map<String, String> headers;
    private Integer connectTimeout;
    private Integer readTimeout;
    private Boolean isHttps;
    private SSLContext sslContext;
    private HostnameVerifier hostnameVerifier;
    private CookieManager cookieManager;

    private UrlOpenConnMultipartRequest(Builder builder) {
        this.targetUrl = builder.targetUrl;
        this.params = builder.params;
        this.charset = builder.charset;
        this.headers = builder.headers;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
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
        private Map<String, Object> params = new HashMap<>();
        private String charset = "UTF-8";
        private Map<String, String> headers = new HashMap<>();
        private Integer connectTimeout = 5000;
        private Integer readTimeout = 5000;
        private Boolean isHttps = false;
        private SSLContext sslContext;
        private HostnameVerifier hostnameVerifier;
        private CookieManager cookieManager;

        public Builder targetUrl(String targetUrl) {
            this.targetUrl = targetUrl;
            return this;
        }

        public Builder params(Map<String, Object> params) {
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

        public Builder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(int readTimeout) {
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

        public Builder hostnameVerifier(HostnameVerifier hostnameVerifier) {
            this.hostnameVerifier = hostnameVerifier;
            return this;
        }

        public Builder cookieManager(CookieManager cookieManager) {
            this.cookieManager = cookieManager;
            return this;
        }

        public UrlOpenConnMultipartRequest build() {
            return new UrlOpenConnMultipartRequest(this);
        }
    }

    // Getters for the fields
    public String getTargetUrl() {
        return targetUrl;
    }

    public Map<String, Object> getParams() {
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

    public static String doHttpPost(UrlOpenConnMultipartRequest request) throws Exception {
        String boundary = "Boundary-" + System.currentTimeMillis();
        HttpURLConnection connection = null;
        StringBuilder responseContent = new StringBuilder();

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

            // (버그 수정) 여기서 url.openConnection() 을 다시 호출하면 위의 SSL/쿠키 설정이 모두 사라지므로 제거함.
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(request.connectTimeout);
            connection.setReadTimeout(request.readTimeout);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

             // Set headers
             for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            // Write request body
            try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                if (request.getParams() != null && !request.getParams().isEmpty()) {
                    for (Map.Entry<String, Object> entry : request.params.entrySet()) {
                        if (entry.getValue() instanceof String) {
                            addTextPart(out, boundary, entry.getKey(), (String) entry.getValue(), request.charset);
                        } else if (entry.getValue() instanceof File) {
                            addFilePart(out, boundary, entry.getKey(), (File) entry.getValue());
                        }
                    }
                }
                out.writeBytes("--" + boundary + "--\r\n");
                out.flush();
            }

            // Read response
            int responseCode = connection.getResponseCode();
            
            if (request.getCookieManager() != null) {
                saveCookiesFromResponse(request.getTargetUrl(), connection, request.getCookieManager());
            }

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                // String encoding = connection.getHeaderField("Content-Encoding");

                inputStream = "gzip".equalsIgnoreCase(connection.getContentEncoding())
                    ? new GZIPInputStream(inputStream)
                    : inputStream;

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

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charSet))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseContent.append(line).append("\n");
                    }
                }
            } else{
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

    private static void addTextPart(DataOutputStream out, String boundary, String name, String value, String charset) throws IOException {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n");
        out.writeBytes("Content-Type: text/plain; charset=" + charset + "\r\n\r\n");
        out.writeBytes(value + "\r\n");
    }

    private static void addFilePart(DataOutputStream out, String boundary, String name, File file) throws IOException {
        String mimeType = Files.probeContentType(file.toPath());
        mimeType = mimeType == null ? "application/octet-stream" : mimeType;

        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + file.getName() + "\"\r\n");
        out.writeBytes("Content-Type: " + mimeType + "\r\n\r\n");

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        out.writeBytes("\r\n");
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
