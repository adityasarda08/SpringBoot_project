package com.dataline.BajajPortal.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaxInvoicePrintProjection {

    private String companyName;
    private String companyAddress;
    private String companyPanNo;
    private String companyContactNo;
    private String companyEmail;
    private String companyWebsite;
    private String companyGstNo;
    private String taxReverseCharge;
    private String companyStateCode;
    private String companyCode;
    private String companyCinNo;
    private String companyLogo;
    private String logoName;

    private String vendorAddress;
    private String vendorStateCode;
    private String vendorPAN;
    private String vendorCode;
    private String vendorPlantCode;
    private String vendorName;
    private String vendorCIN;
    private String vendorGST;
    private String invoiceCopies;
    private String remark1;
    private String remark2;

    private String invoiceNumber;
    private Date invoiceDate;
    private String hsnCode;
    private String sacCode;
    private String itemCode;
    private String poNo;
    private String lineNo;
    private Integer invoiceQuantity;
    private Double basicRate;
    private Double basicAmount;
    private Double freight;
    //    private Double PAndFCharges;
    private Double othersCharges;
    private Double subTotal;
    private Double additionalTaxAmount;
    private Double taxableAmount;
    private Double cgstPercentage;
    private Double cgstAmount;
    private Double sgstPercentage;
    private Double sgstAmount;
    private Double igstPercentage;
    private Double igstAmount;
    private Double utgstPercentage;
    private Double utgstAmount;
    private Double totalAmount;
    private String ewayBillNo;
    private Date ewayBillDate;
    private String vehicleNo;
    //private String packingDetails;
    private String shiftVendorCode;
    private String signedQrCode;
    private String deliveryChallanNo;
    private String deliveryChallanDate;
    private Double deliveryChallanAmount;
    private Double tcsValue;
    private String ediNumber;
    private String shiftVendorName;
    private String shippingAddress;
    private String shippingGstNo;
    private String shippingStateCode;
    private String itemName;
    private String itemUnit;
    private String itemRemark1;
    private String itemRemark2;
    private String labels;


}
