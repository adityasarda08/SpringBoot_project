package com.dataline.BajajPortal.repository;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ItemmasterProjection {

    private String itemCode;
    private String itemName;
    private String itemUnit;
    private String itemLocation;
    private String hsnCode;
    private String itemRemark1;
    private String itemRemark2;
    private String companyCode;
}
