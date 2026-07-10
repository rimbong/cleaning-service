package com.boot.cleanhub.biz.company.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.boot.cleanhub.biz.company.domain.Company;
import com.boot.cleanhub.biz.company.dto.CompanyRequest;
import com.boot.cleanhub.biz.company.dto.CompanyResponse;
import com.boot.cleanhub.biz.company.repository.CompanyRepository;
import com.boot.cleanhub.error.BizException;
import com.boot.cleanhub.error.ErrorCode;
import com.boot.cleanhub.util.file.FileUtillMo;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   회사(공급자) 프로필 서비스 — 단일 행(설정) 조회/수정.
 *   Flyway 가 빈 1행을 시드하지만, 없을 경우에도 기본 행을 만들어 항상 1개를 보장한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    /** 도장 이미지 허용 확장자 */
    private static final Set<String> STAMP_EXTENSIONS = new HashSet<>(Arrays.asList("png", "jpg", "jpeg", "gif"));

    private final CompanyRepository companyRepository;

    /** 업로드 저장 루트(설정 file.upload-dir) */
    @Value("${file.upload-dir}")
    private String uploadDir;

    /** 회사 프로필(단일 행) 조회. 없으면 기본 행 생성. */
    public CompanyResponse get() {
        return CompanyResponse.from(getSingleton());
    }

    /** 회사 프로필 수정. */
    @Transactional
    public CompanyResponse update(CompanyRequest request) {
        Company company = getSingleton();
        company.setBusinessNumber(request.getBusinessNumber());
        company.setCompanyName(request.getCompanyName());
        company.setOwnerName(request.getOwnerName());
        company.setAddress(request.getAddress());
        company.setBusinessType(request.getBusinessType());
        company.setBusinessItem(request.getBusinessItem());
        company.setPhone(request.getPhone());
        return CompanyResponse.from(companyRepository.saveAndFlush(company));
    }

    /**
     * 도장(인장) 이미지 등록 — 파일시스템(company/stamp)에 저장하고 회사 프로필에 경로 보관.
     * 기존 도장이 있으면 교체(이전 파일 삭제). 이미지 확장자만 허용.
     *
     * @param file 업로드된 도장 이미지(png/jpg/jpeg/gif)
     * @return 갱신된 회사 프로필(도장 등록 여부 포함)
     * @throws BizException 파일이 비었거나 이미지 확장자가 아니면 INVALID_FILE_FORMAT
     */
    @Transactional
    public CompanyResponse uploadStamp(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_FILE_FORMAT);
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (!STAMP_EXTENSIONS.contains(ext)) {
            throw new BizException(ErrorCode.INVALID_FILE_FORMAT);
        }
        Company company = getSingleton();
        deleteStampFile(company); // 기존 도장 정리(있으면)
        try {
            String stored = FileUtillMo.uploadSingleFile(file, uploadDir, "company/stamp");
            company.setStampImagePath(stored);
        } catch (IOException e) {
            throw new BizException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        return CompanyResponse.from(companyRepository.saveAndFlush(company));
    }

    /** 도장 이미지 삭제. */
    @Transactional
    public CompanyResponse removeStamp() {
        Company company = getSingleton();
        deleteStampFile(company);
        company.setStampImagePath(null);
        return CompanyResponse.from(companyRepository.saveAndFlush(company));
    }

    /**
     * 도장 이미지 바이트(없으면 null) — 세금계산서 양식 삽입·미리보기용.
     * 읽기 실패 시에도 양식 출력이 막히지 않도록 null 을 반환한다.
     */
    public byte[] getStampBytes() {
        Company company = getSingleton();
        if (!StringUtils.hasText(company.getStampImagePath())) {
            return null;
        }
        try {
            return FileUtillMo.readBytes(new File(uploadDir, company.getStampImagePath()).getPath());
        } catch (IOException e) {
            return null;
        }
    }

    /** 기존 도장 파일을 디스크에서 정리(best-effort). */
    private void deleteStampFile(Company company) {
        if (StringUtils.hasText(company.getStampImagePath())) {
            FileUtillMo.deleteFile(new File(uploadDir, company.getStampImagePath()).getPath());
        }
    }

    /** 파일명에서 확장자(소문자) 추출. 없으면 빈 문자열. */
    private static String extensionOf(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    /** 단일 행 획득(없으면 생성). */
    @Transactional
    Company getSingleton() {
        return companyRepository.findAll().stream().findFirst().orElseGet(() -> {
            Company c = new Company();
            c.setUpdatedAt(LocalDateTime.now());
            return companyRepository.save(c);
        });
    }
}
