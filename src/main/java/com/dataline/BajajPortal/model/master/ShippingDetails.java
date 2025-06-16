package com.dataline.BajajPortal.model.master;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Shipping_Details")
@Data
public class ShippingDetails {
    public static final String SEQUENCE_NAME = "shipping_sequence";

    @Id
    private String shippingid;

    @NotBlank(message = "Shipping code is required")
    @Size(max = 10, message = "Shipping code should not exceed 10 characters")
    private String shiftVendorCode;
    @NotBlank(message = "Shipping vendor name is required")
    @Size(max = 100, message = "Shipping vendor name should not exceed 100 characters")
    private String shiftVendorName;
    @NotBlank(message = "Shipping address line 1 is required")
    @Size(max = 150, message = "Shipping address line 1 should not exceed 150 characters")
    private String shippingAddress;
    @NotBlank(message = "Shipping GST number is required")
    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GST number format")
    private String shippingGstNo;
    @NotBlank(message = "Shipping state code is required")
    @Size(max = 25, message = "Shipping state code should not exceed 25 characters")
    private String shippingStateCode;
    @NotBlank(message = "Shipping state name is required")
    @Size(max = 25, message = "Shipping state name should not exceed 25 characters")
    private String companyCode;
}