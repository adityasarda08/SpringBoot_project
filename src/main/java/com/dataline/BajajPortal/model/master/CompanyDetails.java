package com.dataline.BajajPortal.model.master;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.URL;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;

@Document(collection = "Company_Details")
@Data
public class CompanyDetails {


    @Id
    private String _id;


    private String userRole;
    @NotBlank(message = "Company code is required")
    @Size(max = 6, message = "Company code should not exceed 6 characters")
    @Indexed(unique = true)
    private String companyCode;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Company name is required")
    @Size(max = 100, message = "Company name should not exceed 100 characters")
    private String companyName;

    @NotBlank(message = "Company address is required")
    @Size(max = 150, message = "Company address should not exceed 150 characters")
    private String companyAddress;

    @NotBlank(message = "GST number is required")
    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
            message = "Invalid GST number format")
    private String companyGstNo;

    @NotBlank(message = "State code is required")
    @Size(max = 25, message = "State code should not exceed 25 characters")
    private String companyStateCode;

    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}", message = "Invalid PAN number format")
    private String companyPanNo;

    private String companyCinNo;
    private String companyRemark1;
    private String companyRemark2;


    // Changed to optional
    @Size(max = 25)
    private String companyContactNo;

    private String companyStateName;
    private String companyPinCode;

    // Changed to optional
    @Email(message = "Invalid email format")
    private String companyEmail;

    private String companyWebsite;

    @NotBlank(message = "Tax reverse charge is required")
    @Pattern(regexp = "^(yes|no)$", flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Tax reverse charge must be either 'yes' or 'no'")
    private String taxReverseCharge;

    @NotBlank(message = "Invoice Templates is required")
    @Pattern(regexp = "^(yes|no)$", flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Invoice Templates must be either 'yes' or 'no'")
    private String invoiceTemplates;

    private String templateName;

    @Pattern(regexp = "^(Yes|No)$", message = "Invoice Images must be either '1' or 'No'")
    private String companyLogo;

    @Pattern(regexp = "^(Yes|No)$", message = "Invoice Signature must be either '1' or 'No'")
    private String invoiceSignature;

    private String invoiceSignatureName;
    private String logoName;

    // Custom validation for conditional requirements
    @AssertTrue(message = "Signature Name and Image Name are required when Invoice Images or Invoice Signature is enabled")
    private boolean isValidSignatureAndImageName() {
        if ("Yes".equals(companyLogo)) {
            return logoName != null && !logoName.trim().isEmpty();
        }
        if ("Yes".equals(invoiceSignature)) {
            return invoiceSignatureName != null && !invoiceSignatureName.trim().isEmpty();
        }
        return true;
    }

    @AssertTrue(message = "Template Name is required when Invoice Templates is set to 'yes'")
    private boolean isValidTemplateName() {
        if ("yes".equalsIgnoreCase(invoiceTemplates)) {
            return templateName != null && !templateName.trim().isEmpty();
        }
        return true;
    }

    public boolean equalsIgnoreCase(String yes) {
        return false;
    }
}
