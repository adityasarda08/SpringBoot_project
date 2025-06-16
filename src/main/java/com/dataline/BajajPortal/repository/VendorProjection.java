package com.dataline.BajajPortal.repository;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VendorProjection {

    private String vendorCode;
    private String vendorPlantCode;
    private String vendorName;
    private String vendorAddress;
    private String vendorGST;
    private String vendorStateCode;
    private String vendorCIN;
    private String vendorPAN;
    private String pinCode;
    private String companyCode;
    private String invoiceCopies;
    private String remark1;
    private String remark2;
}
