package com.dataline.BajajPortal.model.config;

import lombok.Data;
import org.springframework.data.annotation.Id; // Import @Id
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "pdf_extraction_Pattern")
public class PdfExtractionPattern {

    @Id
    private String id; // MongoDB will generate this if not set, or you can manage it

    @Field("vendor_code_Pattern")
    private String vendorCode; // This field now holds the literal vendor code string
    @Field("vendor_code_target_line_number")
    private String vendorCodeTargetLineNumber;

    // ... all your other existing pattern and target_line_number fields ...
    // (invoiceNumber, invoiceDate, etc., remain as they are in your provided file)
    @Field("invoice_number_Pattern")
    private String invoiceNumber;
    @Field("invoice_number_target_line_number")
    private String invoiceNumberTargetLineNumber;

    @Field("invoice_date_Pattern")
    private String invoiceDate;
    @Field("invoice_date_target_line_number")
    private String invoiceDateTargetLineNumber;

    // vendorCode and vendorCodeTargetLineNumber are defined above

    @Field("vendor_plant_code_Pattern")
    private String vendorPlantCode;
    @Field("vendor_plant_code_target_line_number")
    private String vendorPlantCodeTargetLineNumber;

    @Field("gstin_no_Pattern")
    private String gstinNo;
    @Field("gstin_no_target_line_number")
    private String gstinNoTargetLineNumber;

    @Field("hsn_code_Pattern")
    private String hsnCode;
    @Field("hsn_code_target_line_number")
    private String hsnCodeTargetLineNumber;

    @Field("sac_code_Pattern")
    private String sacCode;
    @Field("sac_code_target_line_number")
    private String sacCodeTargetLineNumber;

    @Field("item_code_Pattern")
    private String itemCode;
    @Field("item_code_target_line_number")
    private String itemCodeTargetLineNumber;

    @Field("po_no_Pattern")
    private String poNo;
    @Field("po_no_target_line_number")
    private String poNoTargetLineNumber;

    @Field("line_no_Pattern")
    private String lineNo;
    @Field("line_no_target_line_number")
    private String lineNoTargetLineNumber;

    @Field("invoice_quantity_Pattern")
    private String invoiceQuantity;
    @Field("invoice_quantity_target_line_number")
    private String invoiceQuantityTargetLineNumber;

    @Field("basic_rate_Pattern")
    private String basicRate;
    @Field("basic_rate_target_line_number")
    private String basicRateTargetLineNumber;

    @Field("basic_value_Pattern")
    private String basicValue;
    @Field("basic_value_target_line_number")
    private String basicValueTargetLineNumber;

    @Field("freight_Pattern")
    private String freight;
    @Field("freight_target_line_number")
    private String freightTargetLineNumber;

    @Field("pf_charges_Pattern")
    private String pfCharges;
    @Field("pf_charges_target_line_number")
    private String pfChargesTargetLineNumber;

    @Field("other_charges_Pattern")
    private String otherCharges;
    @Field("other_charges_target_line_number")
    private String otherChargesTargetLineNumber;

    @Field("sub_total_Pattern")
    private String subTotal;
    @Field("sub_total_target_line_number")
    private String subTotalTargetLineNumber;

    @Field("additional_tax_value_Pattern")
    private String additionalTaxValue;
    @Field("additional_tax_value_target_line_number")
    private String additionalTaxValueTargetLineNumber;

    @Field("tax_base_value_Pattern")
    private String taxBaseValue;
    @Field("tax_base_value_target_line_number")
    private String taxBaseValueTargetLineNumber;

    @Field("cgst_percent_Pattern")
    private String cgstPercent;
    @Field("cgst_percent_target_line_number")
    private String cgstPercentTargetLineNumber;

    @Field("cgst_amt_Pattern")
    private String cgstAmt;
    @Field("cgst_amt_target_line_number")
    private String cgstAmtTargetLineNumber;

    @Field("sgst_percent_Pattern")
    private String sgstPercent;
    @Field("sgst_percent_target_line_number")
    private String sgstPercentTargetLineNumber;

    @Field("sgst_amt_Pattern")
    private String sgstAmt;
    @Field("sgst_amt_target_line_number")
    private String sgstAmtTargetLineNumber;

    @Field("utgst_percent_Pattern")
    private String utgstPercent;
    @Field("utgst_percent_target_line_number")
    private String utgstPercentTargetLineNumber;

    @Field("utgst_amt_Pattern")
    private String utgstAmt;
    @Field("utgst_amt_target_line_number")
    private String utgstAmtTargetLineNumber;

    @Field("igst_percent_Pattern")
    private String igstPercent;
    @Field("igst_percent_target_line_number")
    private String igstPercentTargetLineNumber;

    @Field("igst_amt_Pattern")
    private String igstAmt;
    @Field("igst_amt_target_line_number")
    private String igstAmtTargetLineNumber;

    @Field("total_amt_Pattern")
    private String totalAmt;
    @Field("total_amt_target_line_number")
    private String totalAmtTargetLineNumber;

    @Field("e_way_bill_no_Pattern")
    private String eWayBillNo;
    @Field("e_way_bill_no_target_line_number")
    private String eWayBillNoTargetLineNumber;

    @Field("e_way_bill_date_Pattern")
    private String eWayBillDate;
    @Field("e_way_bill_date_target_line_number")
    private String eWayBillDateTargetLineNumber;

    @Field("vehicle_no_Pattern")
    private String vehicleNo;
    @Field("vehicle_no_target_line_number")
    private String vehicleNoTargetLineNumber;

    @Field("bill_to_ship_to_code_Pattern")
    private String billToShipToCode;
    @Field("bill_to_ship_to_code_target_line_number")
    private String billToShipToCodeTargetLineNumber;

    @Field("remarks_Pattern")
    private String remarks;
    @Field("remarks_target_line_number")
    private String remarksTargetLineNumber;

    @Field("signed_qr_code_Pattern")
    private String signedQrCode;
    @Field("signed_qr_code_target_line_number")
    private String signedQrCodeTargetLineNumber;

    @Field("delivery_challan_number_Pattern")
    private String deliveryChallanNumber;
    @Field("delivery_challan_number_target_line_number")
    private String deliveryChallanNumberTargetLineNumber;

    @Field("delivery_challan_date_Pattern")
    private String deliveryChallanDate;
    @Field("delivery_challan_date_target_line_number")
    private String deliveryChallanDateTargetLineNumber;

    @Field("delivery_challan_amt_Pattern")
    private String deliveryChallanAmt;
    @Field("delivery_challan_amt_target_line_number")
    private String deliveryChallanAmtTargetLineNumber;

    @Field("tcs_value_Pattern")
    private String tcsValue;
    @Field("tcs_value_target_line_number")
    private String tcsValueTargetLineNumber;

    @Field("edi_number_Pattern")
    private String ediNumber;
    @Field("edi_number_target_line_number")
    private String ediNumberTargetLineNumber;
}