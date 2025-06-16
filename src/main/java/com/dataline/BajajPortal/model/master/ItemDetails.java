package com.dataline.BajajPortal.model.master;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;

@Data
@NoArgsConstructor
@Document(collection = "Item_Details")
@CompoundIndex(name = "company_item_idx", def = "{'company_code': 1, 'item_code': 1}", unique = true)
public class ItemDetails {

    public static final String SEQUENCE_NAME = "item_sequence";
    @Id
    private String id;
    private String itemCode;
    private String itemName;
    private String itemUnit;
    private String itemLocation;
    private String hsnCode;
    private String itemRemark1;
    private String itemRemark2;
    private String companyCode;
}