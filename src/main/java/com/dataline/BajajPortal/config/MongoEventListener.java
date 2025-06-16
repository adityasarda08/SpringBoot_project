//package com.dataline.BajajPortal.config;
//
//import com.dataline.BajajPortal.services.SequenceGeneratorService;
//import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
//import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
//import org.springframework.stereotype.Component;
//import com.dataline.BajajPortal.model.master.VendorDetails;
//import com.dataline.BajajPortal.model.master.ItemDetails;
//import com.dataline.BajajPortal.model.master.ShippingDetails;
//
//@Component
//public class MongoEventListener extends AbstractMongoEventListener<Object> {
//
//    private final SequenceGeneratorService sequenceGeneratorService;
//
//    public MongoEventListener(SequenceGeneratorService sequenceGeneratorService) {
//        this.sequenceGeneratorService = sequenceGeneratorService;
//    }
//
//    @Override
//    public void onBeforeConvert(BeforeConvertEvent<Object> event) {
//        Object source = event.getSource();
//
//        if (source instanceof VendorDetails) {
//            VendorDetails vendor = (VendorDetails) source;
//            if (vendor.getVendorId() == null || vendor.getVendorId().isEmpty()) {
//                vendor.setVendorId(String.format("%06d",
//                        sequenceGeneratorService.getNextSequenceValue("vendor_sequence")));
//            }
//        } else if (source instanceof ItemDetails) {
//            ItemDetails item = (ItemDetails) source;
//            if (item.getId() == null || item.getId().isEmpty()) {
//                item.setId(String.format("%06d",
//                        sequenceGeneratorService.getNextSequenceValue("item_sequence")));
//            }
//        } else if (source instanceof ShippingDetails) {
//            ShippingDetails shipping = (ShippingDetails) source;
//            if (shipping.getShippingid() == null || shipping.getShippingid().isEmpty()) {
//                shipping.setShippingid(String.format("%06d",
//                        sequenceGeneratorService.getNextSequenceValue("shipping_sequence")));
//            }
//        }
//    }
//}
