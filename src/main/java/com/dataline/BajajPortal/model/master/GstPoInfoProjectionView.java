package com.dataline.BajajPortal.model.master;

import lombok.Data;

@Data
public class GstPoInfoProjectionView {

    private String _id;
    private String vendorCode;
    private String vendorName;
    private String vendorPlantCode;
    private String itemCode;
    private String itemName;
    private String itemUnit;
    private Double itemRate;
    private String poNumber;
    private String poLineNumber;
    private Double cgstPer;
    private Double sgstPer;
    private Double igstPer;
    private Double utgstPer;
    private String companyCode;
}
