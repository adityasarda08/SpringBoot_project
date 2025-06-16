package com.dataline.BajajPortal.model.taxinvoice;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Document(collection = "Tax_Invoice")
@Data
public class TaxInvoice {

    public static final String SEQUENCE_NAME = "tax_sequence";
    public static String tax_sequence;
    @Id
    @Field(name = "invoice_Id")
    private Long invoiceId;

    @NotBlank
    @Size(max = 20)
    private String invoiceNumber;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate invoiceDate;
    @NotBlank
    @Size(max = 50)
    private String vendorCode;
    @NotBlank
    @Size(max = 50)
    private String vendorPlantCode;
    @Transient
    private String vendorName;
    @NotBlank
    @Size(max = 15)
    private String vendorGstNo;
    @NotBlank
    @Size(max = 10)
    private String hsnCode;
    @NotBlank
    @Size(max = 10)
    private String sacCode;
    @NotBlank
    @Size(max = 15)
    private String itemCode;
    @NotBlank
    @Size(max = 20)
    private String poNo;
    @NotBlank
    @Size(max = 10)
    private String lineNo;
    @NotNull
    @Min(0)
    private Integer invoiceQuantity;
    @NotNull
    @Min(0)
    private Double basicRate;
    @NotNull
    @Min(0)
    private Double basicAmount;
    @Min(0)
    private Double freight;
    @Min(0)
    private Double PAndFCharges;
    @Min(0)
    private Double othersCharges;
    @NotNull
    @Min(0)
    private Double subTotal;
    @Min(0)
    private Double additionalTaxAmount;
    @NotNull
    @Min(0)
    private Double taxableAmount;
    @Min(0)
    private Double cgstPercentage;
    @Min(0)
    private Double cgstAmount;
    @Min(0)
    private Double sgstPercentage;
    @Min(0)
    private Double sgstAmount;
    @Min(0)
    private Double igstPercentage;
    @Min(0)
    private Double igstAmount;
    @Min(0)
    private Double utgstPercentage;
    @Min(0)
    private Double utgstAmount;
    @NotNull
    @Min(0)
    private Double totalAmount;
    @Size(max = 50)
    private String ewayBillNo;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate ewayBillDate;
    @Size(max = 20)
    private String vehicleNo;
    @Size(max = 100)
    private String packingDetails;
    @Size(max = 100)
    private String shiftVendorCode;
    @Size(max = 100)
    private String signedQrCode;
    @Size(max = 20)
    private String deliveryChallanNo;
    private String deliveryChallanDate;
    @Min(0)
    private Double deliveryChallanAmount;
    @Min(0)
    private Double tcsValue;
    private String ediNumber;

    @NotBlank(message = "Company code is required")
    @Size(max = 10, message = "Company code should not exceed 10 characters")
    private String companyCode;
}
