package com.boot.cleanhub.util.image;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.codec.binary.Base64;

/**
 * <pre>
 * 	ImageMO 공통 모듈 Class
 * </pre>
 */
public final class ImageMo {

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

		// try-with-resources: 스트림 생성 실패 시 finally NPE 없이 안전하게 자동 close
		try (FileInputStream inputStream = new FileInputStream(file);
				ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream()) {

			int len = 0;
			byte[] buf = new byte[1024];

			while ((len = inputStream.read(buf)) != -1) {
				byteOutStream.write(buf, 0, len);
			}

			byte[] fileArray = byteOutStream.toByteArray();
			fileString = new String(Base64.encodeBase64(fileArray));

		} catch (IOException e) {
			e.printStackTrace();
		}

		return fileString;
	}
	
	/**
	 * <pre>
	 * 	이미지 리사이징
	 * </pre>
	 * 
	 * @param originalImage
	 * 			: 리사이징할 원본 이미지
	 * @param imageType
	 * 			: 이미지 타입
	 * @return 리사이즈된 이미지 (BufferedImage)
	 */
	public static BufferedImage resizeImage(BufferedImage originalImage, int imageType) {
		
		BufferedImage resizedImage = new BufferedImage(500, 500, imageType);
		Graphics2D graphics2d = resizedImage.createGraphics();
		graphics2d.drawImage(originalImage, 0, 0, 500, 500, null);
		graphics2d.dispose();

		return resizedImage;
	}
	
	/**
	 * <pre>
	 * 	파일의 Height를 반환
	 * </pre>
	 * 
	 * @param image
	 * 			: 이미지 파일
	 * @throws IOException
	 * @return 파일의 Height (int)
	 */
	public static int getHeight(final File image) throws IOException {

		// 운영 서버 동작시 javax.imageio.ImageIO의 java.lang.NoClassDefFoundError 방지를 위한 SETTING
		System.setProperty("java.awt.headless", "true");

		BufferedImage bufferedImage = ImageIO.read(image);
		return bufferedImage.getHeight();
	}

	/**
	 * <pre>
	 * 	이미지 바이트의 Width를 반환(읽기 실패 시 0).
	 * </pre>
	 *
	 * @param bytes
	 * 			: 이미지 바이트
	 * @return 이미지의 Width (px). 읽지 못하면 0
	 */
	public static int getWidth(final byte[] bytes) {

		// 운영 서버 동작시 javax.imageio.ImageIO의 java.lang.NoClassDefFoundError 방지를 위한 SETTING
		System.setProperty("java.awt.headless", "true");

		if (bytes == null || bytes.length == 0) {
			return 0;
		}
		try {
			BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
			return bufferedImage != null ? bufferedImage.getWidth() : 0;
		} catch (IOException e) {
			return 0;
		}
	}

	/**
	 * <pre>
	 * 	Safe DPI 설정
	 *  (maxHeightInInches를 통해 설정된 Height가 이미지의 Height보다 작을 경우,
	 *  또는 160dpi보다 작을 경우) 
	 * </pre>
	 * 
	 * @param image
	 *			: 이미지 파일
	 * @param maxHeightInInches
	 * 			: 이미지의 최대 허용 Height (inch 기준)  
	 * @throws IOException
	 */
	public static void setSafeDpi(final File image, final int maxHeightInInches) throws IOException {
		final int imageHeight = getHeight(image);
		final int defaultDpi = 160;
		final int defaultMaxHeight = maxHeightInInches * defaultDpi;

  		// Images that do not fit by default must be
		if (imageHeight > defaultMaxHeight) {
	  		final double dpi = imageHeight * 1.0 / maxHeightInInches;
     		setDPI(image, (int) Math.round(dpi));
	  	} else {
	  		setDPI(image);
		}
	}

	/**
	 * <pre>
	 * 	기본 DPI 설정 (Default 160 Dpi) 
	 * </pre>
	 * 
	 * @param image
	 *			: 이미지 파일
	 * @throws IOException
	 */
	public static void setDPI(final File image) throws IOException {
		setDPI(image, 160);
	}

	/**
	 * Inch당 도트수에 따른 DPI 설정
	 * 
	 * @author Hyun-sung Kim
	 * @since 2016.04.17.
	 * @param image
	 *			: 이미지 파일
	 * @param dotsPerInch
	 *			: Inch당 도트수
	 * @throws IOException
	 */
	public static void setDPI(final File image, final int dotsPerInch) throws IOException {
	   
		// 운영 서버 동작시 javax.imageio.ImageIO의 java.lang.NoClassDefFoundError 방지를 위한 SETTING
		System.setProperty("java.awt.headless", "true");
		
		BufferedImage in = ImageIO.read(image);
		File updatedImage = File.createTempFile(image.getName(), ".tmp");
		saveBufferedImage(in, updatedImage, dotsPerInch);
		
		// commons-io(FileUtils) 의존 제거: JDK NIO(Files)로 대체
		Files.deleteIfExists(image.toPath());
		Files.move(updatedImage.toPath(), image.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	/**
	 * <pre>
	 *  DPI 세팅을 통한 이미지 저장
	 * </pre>
	 * 
	 * @param bufferedImage 
	 * 			: 저장할 이미지
	 * @param outputFile 
	 * 			: 이미지로 저장할 파일
	 * @param dotsPerInch 
	 * 			: DPI 세팅시 사용되는 인치당 도트수
	 * @throws IOException
	 */
	private static void saveBufferedImage(final BufferedImage bufferedImage, final File outputFile, final int dotsPerInch) throws IOException {
	   
		// 운영 서버 동작시 javax.imageio.ImageIO의 java.lang.NoClassDefFoundError 방지를 위한 SETTING
		System.setProperty("java.awt.headless", "true");
		
		for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName("png"); iw.hasNext();) {
			
			ImageWriter writer = iw.next();
			ImageWriteParam writeParam = writer.getDefaultWriteParam();
			ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
			IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
			
			if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()) {
				 continue;
			}
			
			setDPI(metadata, dotsPerInch);
			
			final ImageOutputStream stream = ImageIO.createImageOutputStream(outputFile);
			
			try {
				writer.setOutput(stream);
			    writer.write(metadata, new IIOImage(bufferedImage, null, metadata), writeParam);
			} finally {
				stream.close();
			}
			
			break;
		}
	}

	/**
	 * <pre>
	 *  DPI 세팅을 통한 이미지 메타데이터 생성
	 * </pre>
	 * 
	 * @param metadata 
	 * 			: 이미지의 메타데이터
	 * @param dotsPerInch
	 * 			: DPI 세팅시 사용되는 인치당 도트수
	 * @throws IIOInvalidTreeException
	 */
	private static void setDPI(IIOMetadata metadata, final int dotsPerInch) throws IIOInvalidTreeException {

		final double inchesPerMillimeter = 1.0 / 25.4;	// 밀리미터당 인치수
		final double dotsPerMillimeter = dotsPerInch * inchesPerMillimeter;	// 밀리미터당 도트수
		
		// 수평 픽셀 사이즈 SETTING
		IIOMetadataNode horizontalPixelSize = new IIOMetadataNode("HorizontalPixelSize");
		horizontalPixelSize.setAttribute("value", Double.toString(dotsPerMillimeter));
		
		// 수직 픽셀 사이즈 SETTING
		IIOMetadataNode verticalPixelSize = new IIOMetadataNode("VerticalPixelSize");
		verticalPixelSize.setAttribute("value", Double.toString(dotsPerMillimeter));
		
		// 치수 SETTING
		IIOMetadataNode dimension = new IIOMetadataNode("Dimension");
		dimension.appendChild(horizontalPixelSize);
		dimension.appendChild(verticalPixelSize);
		
		IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
		root.appendChild(dimension);
		
		metadata.mergeTree("javax_imageio_1.0", root);
	}

	private ImageMo() {
		// Not used.
	}
}