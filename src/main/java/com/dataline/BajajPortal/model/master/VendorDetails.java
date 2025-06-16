package com.dataline.BajajPortal.model.master;

import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "Vendor_Details")
public class VendorDetails {
    public static final String SEQUENCE_NAME = "vendor_sequence";
    @Id
    private String _id;
    private String vendorCode;
    @Size(min = 4, max = 4)
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

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        this._id = id;
    }
}
