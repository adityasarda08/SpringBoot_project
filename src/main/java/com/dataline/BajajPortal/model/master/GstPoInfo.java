package com.dataline.BajajPortal.model.master;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.security.PrivateKey;

@Data
@NoArgsConstructor
@Document(collection = "GstPoInfo_Details")
public class GstPoInfo {

    public static final String SEQUENCE_NAME = "GstInfo_sequence";

    @Id
    private String _id;

    private String vendorCode;
    @Transient
    private String vendorName;
    private String vendorPlantCode;
    private String itemCode;
    @Transient
    private String itemName;
    @Transient
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
