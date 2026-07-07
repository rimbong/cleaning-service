package com.boot.cleanhub.util.file;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.springframework.web.multipart.MultipartFile;

import com.boot.cleanhub.common.dto.PBox;
import com.boot.cleanhub.util.common.UtilMo;
import com.boot.cleanhub.util.date.DateUtil;
import com.boot.cleanhub.util.format.EnDecodingMO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class FileUtillMo {

    /*
	 * <pre>
	 * 파일 읽기 메소드
	 * </pre>
	 * 
	 * @param file : 파일
	 *            
	 * @return
	 */
	public static String readFile(File file) throws Exception {
		BufferedReader read = new BufferedReader(new FileReader(file));
		String line=null;
		String fileData = "";
		while((line = read.readLine()) != null){
			fileData += line + "\n";
		}
		read.close();
		return fileData;
	}
		
	/*
	 * <pre>
	 * 파일 삭제 메소드
	 * </pre>
	 * 
	 * @param source
	 *            : 삭제할 파일 경로
	 * @return
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
     * fileDownload
     * </pre>
     * 
     * @author In-seong Hwang
     * @version 1.0
     * 
    */
    public static void downloadFile(PBox pBox, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String fileName = null;
        String fileFullPath = null;
        String fileExt = null;
        FileInputStream fis = null;
        BufferedOutputStream bos = null;

        try {
            fileName = pBox.getString("fileName");
            fileFullPath = pBox.getString("fileFullPath");
            fileExt = separateFileName(fileFullPath)[1];
            File file = new File(fileFullPath);
            
            if (!file.exists() || !file.isFile()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: " + fileName);
                return;
            }

            String docName = getDocNameByBrowser(fileName, request);
            String contentType = Files.probeContentType(file.toPath());

            if("apk".equalsIgnoreCase(fileExt)) {
            	contentType = "application/vnd.android.package-archive";
            }

            response.setContentType(contentType);
            response.setHeader("Expires", "0");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + docName + "\"");
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Pragma", "No-Cache");
            response.setHeader("Cache-Control", "No-Cache");
            response.setHeader("Content-Length", String.valueOf(file.length()) );

            byte[] outputByte = new byte[1024];
            fis = new FileInputStream(file);
            bos = new BufferedOutputStream(response.getOutputStream(), 1024);
            while ( fis.read(outputByte, 0, 1024) != -1) {
                if (outputByte != null) {
                    bos.write(outputByte, 0, 1024);
                }
            }
            bos.flush();
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while downloading file: " + e.getMessage());
            throw e;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception e) {
            }
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (Exception e) {
            }
        }
    }
    /**
     * <pre>
     * fileDownload
     * </pre>
     * 
     * @author In-seong Hwang
     * @since 2023.03.14
     * @version 1.0
     * 
    */
    public static void downloadFile(String dirPath,String filePath,HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String fileName = null;
        String fileExt = null;
        String fileFullPath = null;        
        FileInputStream fis = null;
        BufferedOutputStream bos = null;

        try {
            fileName = separateFileName(filePath)[0];
            fileExt = separateFileName(filePath)[1];
            fileFullPath = dirPath + filePath;
            File file = new File(fileFullPath);
            
            if (!file.exists() || !file.isFile()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: " + fileName);
                return;
            }

            String docName = getDocNameByBrowser(fileName, request);
            docName = docName + "." + fileExt;
            String contentType = Files.probeContentType(file.toPath());

            if("apk".equalsIgnoreCase(fileExt)) {
            	contentType = "application/vnd.android.package-archive";
            }

            response.setContentType(contentType);
            response.setHeader("Expires", "0");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + docName + "\"");
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Pragma", "No-Cache");
            response.setHeader("Cache-Control", "No-Cache");
            response.setHeader("Content-Length", String.valueOf(file.length()) );

            byte[] outputByte = new byte[1024];
            fis = new FileInputStream(file);
            bos = new BufferedOutputStream(response.getOutputStream(), 1024);
            while ( fis.read(outputByte, 0, 1024) != -1) {
                if (outputByte != null) {
                    bos.write(outputByte, 0, 1024);
                }
            }
            bos.flush();
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while downloading file: " + e.getMessage());
            throw e;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception e) {
            }
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * <pre>
     * 현재 브라우저 정보를 기반으로 파일명을 만들어준다.
     * </pre>
     * 
     * @param request
     * @param fileName
     * @throws Exception
     */
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
            // docName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
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
     * @param oriFileName
     *                    : 원본파일 명
     * @return
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
     * 단일 파일 업로드 메서드
     * </pre>
     * 
     * @param mlt       : multipartFile
     * @param file_path : 파일 경로
     * @param target    : 목적 파일 디렉토리 경로
     * @param box       : 원본파일명 및 저장경로를 저장할 박스
     * 
     * @return 성공여부 (true : 성공, false : 실패)
     *         box에 속성 추가 originFileName(원본파일명), saveFilePath(파일저장경로)
     */
    public static boolean uploadSingleFile(MultipartFile mlt, String file_path, String target, PBox box) {
        boolean result = true; // 반환값
        try {
            // 저장 디렉토리 생성
            File save_file_dir = new File(file_path + target);
            if (!save_file_dir.exists()) {
                save_file_dir.mkdirs();
            }

            // 파라미터 설정 [원본파일명, 저장경로]
            String origin_file_name = mlt.getOriginalFilename();
            box.set("originFileName", origin_file_name); // 원본파일명

            String save_file_name = generateAlphaNumericName(origin_file_name);
            String save_file_path = target + save_file_name;
            box.set("saveFilePath", save_file_path); // 파일저장경로

            // 파일 저장
            File save_file = new File(file_path + save_file_path); // 저장할 파일 경로
            mlt.transferTo(save_file); // 파일 저장
            if (!save_file.exists()) {
                // 정상적으로 업로드되지 않은 경우 에러 발생
                return false;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }

    /**
     * <pre>
     * 단일 파일 업로드 메서드
     * </pre>
     * 
     * @param mlt            : multipartFile
     * @param file_path      : 파일 경로 (서버상 고정된 경로 (properties 파일을 읽어온 값))
     * @param target         : 목적 파일 디렉토리 경로
     * @param save_file_name : 저장 파일명
     * 
     * @return 성공여부 (true : 성공, false : 실패)
     */
    public static boolean uploadSingleFile(MultipartFile mlt, String file_path, String target, String save_file_name) {
        boolean result = true; // 반환값
        try {
            // 저장 디렉토리 생성
            File save_file_dir = new File(file_path + target);
            if (!save_file_dir.exists()) {
                save_file_dir.mkdirs();
            }

            // 파일 저장
            String save_file_path = target + save_file_name;

            File save_file = new File(file_path + save_file_path); // 저장할 파일 경로
            mlt.transferTo(save_file); // 파일 저장
            if (!save_file.exists()) {
                // 정상적으로 업로드되지 않은 경우 에러 발생
                return false;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }
    
    /**
	 * <pre>
	 * 파일 복사 메소드
	 * </pre>
	 * @param source
	 *            : 원본 파일 경로 , target : 목적 파일 경로
	 * @return
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
	 * @param srcContentType
	 * 			: 검증할 파일의 확장자, type : 체크할 유형 (image, doc, video, audio, all)
	 * @return  
	 */
	public static boolean checkFileExtension(String srcContentType, String type) {
		boolean result = false;	// 반환값
		List<String> extArray = null;	// 허용할 확장자를 담을 리스트 변수
		
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
	 * 	파일을 BASE64 String으로 변환
	 * </pre>
	 * 
	 * @param file
	 * 			: String 타입으로 변환할 파일
	 * @throws IOException
	 * @return String으로 변환된 파일 (String)
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

    public static void createQR(String input, OutputStream os) throws Exception{
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode( input, BarcodeFormat.QR_CODE, 400, 400 );
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage( bitMatrix );

        // ImageIO를 사용하여 파일쓰기
        ImageIO.write( bufferedImage, "png", os ); 
    }
}
