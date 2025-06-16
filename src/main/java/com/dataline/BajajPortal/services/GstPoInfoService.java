package com.dataline.BajajPortal.services;

import com.dataline.BajajPortal.model.master.*;

import com.dataline.BajajPortal.repository.GstPoInfoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GstPoInfoService {

    @Autowired
    private GstPoInfoRepository gstPoInfoRepository;

    @Autowired
    private SequenceGeneratorService sequenceGeneratorService;

    public List<GstPoInfo> getAllinfo() {
        return gstPoInfoRepository.findAll();
    }

    public boolean addPoInfoDetails(List<GstPoInfo> poinfolist, String companyCode) {

        for (GstPoInfo gstPoInfo : poinfolist) {
            gstPoInfo.setCompanyCode(companyCode);
            if (gstPoInfo.get_id() == null || gstPoInfo.get_id().isEmpty()) {
                gstPoInfo.set_id(String.valueOf(sequenceGeneratorService.generateSequence(
                        GstPoInfo.SEQUENCE_NAME)));
            }
        }
        gstPoInfoRepository.saveAll(poinfolist);
        return true;
    }


    /**
     * Gets the vendor PO summary list for a specific company using aggregation.
     *
     * @param companyCode The company code to filter by.
     * @return A list of VendorPoSummary objects.
     */
    // Renamed method, added companyCode parameter, changed return type
    public List<GstPoInfoList> getVendorItemSummary(String companyCode) {
        // --- Call the correct repository method ---
        List<GstPoInfoList> summaryList = gstPoInfoRepository.findVendorItemSummary(companyCode);
        if (summaryList.isEmpty()) {
            return null;
        } else {
            return summaryList;
        }
    }

    /**
     * Gets GstPoInfo list for a vendor and company, enriched with item details using aggregation.
     *
     * @param vendorCode      The vendor code.
     * @param vendorPlantCode The company code.
     * @return List of GstPoInfo enriched with itemName and itemUnit, or empty list if not found.
     */
    public List<GstPoInfoProjectionView> getPoInfoListByVendorCode(String vendorCode, String vendorPlantCode) {
        // --- Directly call the aggregation method ---
        List<GstPoInfoProjectionView> PoInfoList = gstPoInfoRepository.findWithItemDetailsByVendorCodeVendorPlantCode(vendorCode, vendorPlantCode);
        // --- Logging the results ---
        if (PoInfoList.isEmpty()) {
            return Collections.emptyList();
        } else {
            return PoInfoList;
        }
    }

    @Transactional
    public void deletePoInfoByVendorCodeAndPlantCode(String VendorCode, String PlantCode) {
        gstPoInfoRepository.deleteByVendorCodeAndVendorPlantCode(VendorCode, PlantCode);
    }

    @Transactional
    public void deletePoInfoByVendorCodeAndPlantCodeAndItemCode(String VendorCode, String PlantCode, String ItemCode) {
        gstPoInfoRepository.deleteByVendorCodeAndVendorPlantCodeAndItemCode(VendorCode, PlantCode, ItemCode);
    }
}

