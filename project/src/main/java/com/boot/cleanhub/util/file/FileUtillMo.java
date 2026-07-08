package com.boot.cleanhub.util.file;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import com.boot.cleanhub.common.dto.PBox;
import com.boot.cleanhub.util.common.UtilMo;
import com.boot.cleanhub.util.date.DateUtil;
import com.boot.cleanhub.util.format.EnDecodingMO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class FileUtillMo {

    // =====================================================================
    //  blob(byte[]) ↔ 파일 브리지 (2026.07 추가)
    //  ※ 기존 레거시 메서드는 예외를 삼키는(printStackTrace/빈 catch) 것들이 있으나,
    //    아래 신규 메서드는 실패 시 IOException 을 던져 호출부가 처리하도록 한다(프로젝트 규칙).
    // =====================================================================

    /**
     * <pre>
     *   바이트 배열을 파일로 저장한다(blob → 파일). 상위 디렉터리는 없으면 생성한다.
     *   DB 등에 담긴 바이너리(blob)를 파일시스템으로 내보낼 때 사용.
     * </pre>
     *
     * @param data     저장할 바이트
     * @param fullPath 저장할 파일의 전체 경로
     * @throws IOException 디렉터리 생성/파일 쓰기 실패
     */
    public static void writeBytes(byte[] data, String fullPath) throws IOException {
        File file = new File(fullPath);
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    /**
     * <pre>
     *   파일을 바이트 배열로 읽는다(파일 → blob). 다운로드 응답 바디 등에 사용.
     * </pre>
     *
     * @param fullPath 읽을 파일의 전체 경로
     * @return 파일 전체 바이트
     * @throws IOException 파일이 없거나 읽기 실패
     */
    public static byte[] readBytes(String fullPath) throws IOException {
        return Files.readAllBytes(new File(fullPath).toPath());
    }

    /**
     * <pre>
     *   파일 다운로드 응답을 표준 형태로 만들어 반환한다(REST 컨트롤러용).
     *   레거시 downloadFile(HttpServletResponse 직접 스트리밍)과 달리 ResponseEntity 를 반환해
     *   스프링이 응답을 처리하게 한다. 콘텐트타입은 저장된 값을 그대로 쓰고(없으면 octet-stream),
     *   파일명은 브라우저 판별 없이 UTF-8 퍼센트 인코딩으로 통일한다(프론트가 decodeURIComponent 로 복원).
     * </pre>
     *
     * @param data             파일 바이트(예: readBytes 로 읽은 값)
     * @param originalFilename 다운로드 시 보일 원본 파일명
     * @param contentType      MIME 타입(비면 application/octet-stream)
     * @return 다운로드용 ResponseEntity(attachment)
     */
    public static ResponseEntity<byte[]> downloadResponse(byte[] data, String originalFilename, String contentType) {
        MediaType mediaType = (contentType != null && !contentType.isEmpty())
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;
        String name = (originalFilename != null && !originalFilename.isEmpty()) ? originalFilename : "download";
        String encodedName = UriUtils.encode(name, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName + "\"")
                .body(data);
    }

    /**
     * <pre>
     *   파일시스템의 파일을 읽어 다운로드 응답으로 만든다(baseDir + relativePath 를 합쳐 읽음).
     *   uploadSingleFile 이 반환한 저장 상대경로를 그대로 넘기면 되며,
     *   uploadSingleFile(baseDir, target)·downloadFile(dirPath, filePath) 와 인자 스타일을 맞춘다.
     *   (파일 본문을 이미 들고 있으면 byte[] 오버로드를 쓴다 — 예: DB blob·메모리 생성물)
     * </pre>
     *
     * @param baseDir          저장 루트 디렉터리(설정값 file.upload-dir)
     * @param relativePath     baseDir 기준 저장 상대경로(예: contract/10/2026..._ab.pdf)
     * @param originalFilename 다운로드 시 보일 원본 파일명
     * @param contentType      MIME 타입(비면 application/octet-stream)
     * @return 다운로드용 ResponseEntity(attachment)
     * @throws IOException 파일 읽기 실패
     */
    public static ResponseEntity<byte[]> downloadResponse(String baseDir, String relativePath,
            String originalFilename, String contentType) throws IOException {
        byte[] data = readBytes(new File(baseDir, relativePath).getPath());
        return downloadResponse(data, originalFilename, contentType);
    }

    /**
     * <pre>
     * 파일 읽기 메소드
     * </pre>
     *
     * @param file : 파일
     * @return 파일 내용 문자열
     */
    public static String readFile(File file) throws Exception {
        BufferedReader read = new BufferedReader(new FileReader(file));
        String line = null;
        String fileData = "";
        while ((line = read.readLine()) != null) {
            fileData += line + "\n";
        }
        read.close();
        return fileData;
    }

    /**
     * <pre>
     * 파일 삭제 메소드
     * </pre>
     *
     * @param source : 삭제할 파일 경로
     * @return 성공 여부
     */
    public static boolean deleteFile(String source) {
        Boolean result = false;
        File deleteFile = null;
        try {
            deleteFile = new File(source);
            if (deleteFile.exists()) {
                deleteFile.delete();
            }
            result = true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    /**
     * <pre>
     *   파일 다운로드(서블릿 직접 스트리밍) — PBox(fileName, fileFullPath) 기반.
     *   대용량도 메모리에 다 올리지 않고 청크 전송한다. REST 컨트롤러라면 downloadResponse 를 권장.
     * </pre>
     *
     * @author In-seong Hwang
     * @version 2.0 (2026.07 — 표준 파일명 인코딩·try-with-resources·octet-stream fallback 로 보완)
     */
    public static void downloadFile(PBox pBox, HttpServletResponse response) throws IOException {
        streamToResponse(new File(pBox.getString("fileFullPath")), pBox.getString("fileName"), response);
    }

    /**
     * <pre>
     *   파일 다운로드(서블릿 직접 스트리밍) — dirPath + filePath 기반.
     * </pre>
     *
     * @author In-seong Hwang
     * @since 2023.03.14
     * @version 2.0 (2026.07 — 보완)
     */
    public static void downloadFile(String dirPath, String filePath, HttpServletResponse response) throws IOException {
        String[] parts = separateFileName(filePath);
        streamToResponse(new File(dirPath + filePath), parts[0] + "." + parts[1], response);
    }

    /**
     * <pre>
     *   파일을 HTTP 응답으로 스트리밍한다(청크 전송 — 대용량도 메모리에 다 올리지 않음).
     *   - 파일명: 브라우저 판별 없이 RFC 5987(UTF-8) 방식으로 인코딩(ASCII 대체 + filename*).
     *   - 콘텐트타입: 디스크 추정값(없으면 application/octet-stream), apk 특례 유지.
     *   - 자원: try-with-resources 로 정리(예외 삼키는 빈 catch 제거).
     *   스트리밍 시작 전에 파일 존재를 확인해 404 를 먼저 보낸다.
     * </pre>
     *
     * @param file         보낼 파일
     * @param downloadName 다운로드 시 보일 파일명
     * @param response     HTTP 응답
     * @throws IOException 파일 읽기/응답 쓰기 실패
     */
    private static void streamToResponse(File file, String downloadName, HttpServletResponse response) throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            return;
        }
        String contentType = Files.probeContentType(file.toPath());
        if (contentType == null || contentType.isEmpty()) {
            contentType = "application/octet-stream";
        }
        if (downloadName != null && downloadName.toLowerCase().endsWith(".apk")) {
            contentType = "application/vnd.android.package-archive";
        }
        response.setContentType(contentType);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(downloadName));
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()));
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");

        byte[] buffer = new byte[8192];
        try (InputStream in = new FileInputStream(file);
                OutputStream out = response.getOutputStream()) {
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            out.flush();
        }
    }

    /**
     * <pre>
     *   Content-Disposition(attachment) 헤더값을 만든다.
     *   비ASCII(한글 등)는 헤더에 직접 못 넣으므로, ASCII 대체 파일명 + RFC 5987 filename*(UTF-8)을 함께 준다.
     * </pre>
     *
     * @param filename 원본 파일명
     * @return Content-Disposition 헤더 값
     */
    private static String contentDisposition(String filename) {
        String safe = (filename == null || filename.isEmpty()) ? "download" : filename;
        String asciiFallback = safe.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "_");
        String utf8 = UriUtils.encode(safe, StandardCharsets.UTF_8);
        return "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + utf8;
    }

    /**
     * <pre>
     * 현재 브라우저 정보를 기반으로 파일명을 만들어준다.
     * </pre>
     *
     * @param fileName 원본 파일명
     * @param request  요청(User-Agent 판별용)
     * @return 브라우저별 인코딩된 파일명
     * @throws Exception 인코딩 실패
     * @deprecated 브라우저 판별(User-Agent) 방식은 구식·부정확하다. 신규 코드는 표준
     *             {@link #contentDisposition(String)}(RFC 5987) 또는 downloadResponse 를 사용할 것.
     */
    @Deprecated
    public static String getDocNameByBrowser(String fileName, HttpServletRequest request) throws Exception {
        PBox header = new PBox();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            header.set(headerName, request.getHeader(headerName));
        }
        String browser = header.getString("user-agent");
        String docName = "";

        if (browser.contains("Trident") || browser.contains("MSIE") || browser.contains("Edge")) {
            docName = UtilMo.mappingUnicode(EnDecodingMO.encodeUTF8(fileName));
        } else if (browser.contains("Firefox")) {
            docName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
        } else if (browser.contains("Opera")) {
            docName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
        } else if (browser.contains("Chrome")) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < fileName.length(); i++) {
                char c = fileName.charAt(i);
                if (c > '~') {
                    sb.append(URLEncoder.encode("" + c, "UTF-8"));
                } else {
                    sb.append(c);
                }
            }
            docName = sb.toString();
            docName = docName.replaceAll("\\,", "%20");
        } else if (browser.contains("Safari")) {
            docName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
        }

        docName = docName.replaceAll(";", "%3B");

        return docName;
    }

    /**
     * <pre>
     *   파일 파일명, 확장자 분리 메소드
     * </pre>
     *
     * @param oriFileName : 원본파일 명
     * @return [파일명, 확장자]
     */
    public static String[] separateFileName(String oriFileName) {
        int file = oriFileName.lastIndexOf("/");
        int ext = oriFileName.lastIndexOf(".");
        String fileName = oriFileName.substring(file + 1, ext);
        String fileExt = oriFileName.substring(ext + 1);

        return new String[] { fileName, fileExt };
    }

    /**
     * <pre>
     * AlphaNumeric 파일명 생성 메서드
     * yyyyMMddHHmmss_[알파뉴머릭 6자리].[확장자]
     * </pre>
     *
     * @author In-seong Hwang
     * @since 2023.03.14
     */
    public static String generateAlphaNumericName(String origin_file_name) {
        String file_ext = separateFileName(origin_file_name)[1];
        String gen_name = DateUtil.format(LocalDateTime.now(), DateUtil.YYYYMMDDHHMMSS_FORMATTER) + "_" + UtilMo.getRandomString(6);

        StringBuffer file_name = new StringBuffer();
        file_name.append(gen_name);
        file_name.append(".");
        file_name.append(file_ext);

        return file_name.toString();
    }

    /**
     * <pre>
     *   단일 파일 업로드 — 고유 파일명(자동 생성)으로 저장하고 저장 상대경로를 반환한다.
     *   저장 상대경로(target + '/' + 저장파일명)만 DB 에 보관하면 된다.
     * </pre>
     *
     * @param mlt     : 업로드 파일
     * @param baseDir : 저장 루트 디렉터리(설정값 file.upload-dir)
     * @param target  : 루트 아래 하위 경로(예: contract/10)
     * @return 저장된 상대경로(구분자 '/')
     * @throws IOException 디렉터리 생성/파일 저장 실패
     */
    public static String uploadSingleFile(MultipartFile mlt, String baseDir, String target) throws IOException {
        return uploadSingleFile(mlt, baseDir, target, generateAlphaNumericName(mlt.getOriginalFilename()));
    }

    /**
     * <pre>
     *   단일 파일 업로드 — 저장 파일명을 지정한다(실제 저장 로직의 단일 진입점).
     *   과거에는 실패해도 예외를 삼키고 true 를 반환했으나, 이제 실패 시 IOException 을 던진다.
     * </pre>
     *
     * @param mlt          : 업로드 파일
     * @param baseDir      : 저장 루트 디렉터리
     * @param target       : 루트 아래 하위 경로
     * @param saveFileName : 저장할 파일명
     * @return 저장된 상대경로(구분자 '/')
     * @throws IOException 디렉터리 생성/파일 저장 실패
     */
    public static String uploadSingleFile(MultipartFile mlt, String baseDir, String target, String saveFileName)
            throws IOException {
        File dir = new File(baseDir, target).getAbsoluteFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("업로드 디렉터리 생성 실패: " + dir.getPath());
        }
        File dest = new File(dir, saveFileName);
        try (InputStream in = mlt.getInputStream()) {
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        String rel = target.endsWith("/") ? target + saveFileName : target + "/" + saveFileName;
        return rel.replace("\\", "/");
    }

    /**
     * <pre>
     * 파일 복사 메소드
     * </pre>
     *
     * @param source : 원본 파일 경로
     * @param target : 목적 파일 경로
     */
    public static void copyFile(String source, String target) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(target);

            int data = 0;
            byte[] outputByte = new byte[4096];
            while ((data = fis.read(outputByte, 0, 4096)) != -1) {
                fos.write(outputByte, 0, data);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * <pre>
     * 확장자 검증 메소드
     * </pre>
     *
     * @param srcContentType : 검증할 파일의 콘텐트타입
     * @param type           : 체크할 유형 (image, doc, video, audio, all)
     * @return 허용 여부
     */
    public static boolean checkFileExtension(String srcContentType, String type) {
        boolean result = false; // 반환값
        List<String> extArray = null; // 허용할 확장자를 담을 리스트 변수

        try {
            // 허용할 확장자 세팅
            if ("image".equals(type)) {
                // 이미지 : jpg,jpeg,gif,png,bmp
                extArray = Arrays.asList("image/jpeg", "image/png", "image/bmp", "image/gif");

            } else if ("doc".equals(type)) {
                // 문서 : hwp, xls xlsx, txt, doc, docx, pdf, ppt, pptx
                extArray = Arrays.asList("application/haansofthwp", "application/x-hwp", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/plain", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/pdf", "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation");

            } else if ("video".equals(type)) {
                // 영상 : mp4,avi,mkv,wmv,mov mpg, mpeg
                extArray = Arrays.asList("video/mp4", "video/x-msvideo", "video/x-matroska", "video/x-ms-wmv", "video/quicktime", "video/mpeg");

            } else if ("audio".equals(type)) {
                // 음원 : mp3, ogg, wma, wav
                extArray = Arrays.asList("audio/mpeg3", "audio/ogg", "audio/x-ms-wma", "audio/x-wav");

            } else if ("all".equals(type)) {
                // 전체
                extArray = Arrays.asList("image/jpeg", "image/png", "image/bmp", "image/gif",
                        "application/haansofthwp", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/plain", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/pdf", "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "video/mp4", "video/x-msvideo", "video/x-matroska", "video/x-ms-wmv", "video/quicktime", "video/mpeg",
                        "audio/mpeg3", "audio/ogg", "audio/x-ms-wma", "audio/x-wav");

            } else {
                // 잘못된 유형이 전달된 경우 false 반환
                return false;
            }

            // 해당 유형의 리스트에 검증할 파일의 확장자가 포함되어 있는지 확인 후 포함여부에 따라 true, false값 반환
            result = extArray.contains(srcContentType);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }

    /**
     * <pre>
     * 파일을 BASE64 String으로 변환
     * </pre>
     *
     * @param file : String 타입으로 변환할 파일
     * @return String으로 변환된 파일
     * @throws IOException 읽기 실패
     */
    public static String fileToString(File file) throws IOException {
        String fileString = new String();
        FileInputStream inputStream = null;
        ByteArrayOutputStream byteOutStream = null;

        try {
            inputStream = new FileInputStream(file);
            byteOutStream = new ByteArrayOutputStream();

            int len = 0;
            byte[] buf = new byte[1024];

            while ((len = inputStream.read(buf)) != -1) {
                byteOutStream.write(buf, 0, len);
            }

            byte[] fileArray = byteOutStream.toByteArray();
            fileString = new String(Base64.encodeBase64(fileArray));

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (byteOutStream != null) {
                byteOutStream.close();
            }
        }

        return fileString;
    }

    public static void createQR(String input, OutputStream os) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(input, BarcodeFormat.QR_CODE, 400, 400);
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        // ImageIO를 사용하여 파일쓰기
        ImageIO.write(bufferedImage, "png", os);
    }
}
