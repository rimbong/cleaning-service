package com.boot.cleanhub.biz.company.dto;

import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   회사(공급자) 프로필 수정 요청. 모든 값이 선택(설정 화면에서 채운다).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
@Setter
public class CompanyRequest {

    @Size(max = 20)
    private String businessNumber;

    @Size(max = 100)
    private String companyName;

    @Size(max = 50)
    private String ownerName;

    @Size(max = 255)
    private String address;

    @Size(max = 50)
    private String businessType;

    @Size(max = 50)
    private String businessItem;

    @Size(max = 30)
    private String phone;
}
