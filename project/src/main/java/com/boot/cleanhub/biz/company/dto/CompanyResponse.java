package com.boot.cleanhub.biz.company.dto;

import java.time.LocalDateTime;

import com.boot.cleanhub.biz.company.domain.Company;

import lombok.Getter;

/**
 * <pre>
 *   회사(공급자) 프로필 응답.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
public class CompanyResponse {

    private final Long id;
    private final String businessNumber;
    private final String companyName;
    private final String ownerName;
    private final String address;
    private final String businessType;
    private final String businessItem;
    private final String phone;
    private final LocalDateTime updatedAt;

    private CompanyResponse(Company c) {
        this.id = c.getId();
        this.businessNumber = c.getBusinessNumber();
        this.companyName = c.getCompanyName();
        this.ownerName = c.getOwnerName();
        this.address = c.getAddress();
        this.businessType = c.getBusinessType();
        this.businessItem = c.getBusinessItem();
        this.phone = c.getPhone();
        this.updatedAt = c.getUpdatedAt();
    }

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(company);
    }
}
