package com.dataline.BajajPortal.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
// import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class ShippingDetailsProjection {

    private String shiftVendorCode;
    private String shiftVendorName;
    private String shippingAddress;
    private String shippingGstNo;
    private String shippingStateCode;
    private String companyCode;
}
