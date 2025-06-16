package com.dataline.BajajPortal.services;

import com.dataline.BajajPortal.model.master.ItemDetails;
// import com.dataline.BajajPortal.model.master.VendorDetails;
import com.dataline.BajajPortal.repository.ItemDetailsRepository;
import com.dataline.BajajPortal.repository.ItemmasterProjection;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unused")
@Service
@RequiredArgsConstructor
public class ItemDetailsServices {

    private final ItemDetailsRepository itemRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Autowired
    private MongoTemplate mongoTemplate;

    public void saveItems(ItemDetails itemDetails) {

        // Check if an item with this company code and item code already exists
        ItemDetails existingItem = itemRepository.findByCompanyCodeAndItemCode(
                itemDetails.getCompanyCode(),
                itemDetails.getItemCode()
        );

        // For new items
        if (existingItem == null) {
            System.out.println("Creating new item"); // Debug log
            if (itemDetails.getId() == null || itemDetails.getId().isEmpty()) {
                itemDetails.setId(String.valueOf(sequenceGeneratorService.generateSequence(ItemDetails.SEQUENCE_NAME)));
            }
            itemRepository.save(itemDetails);
            return;

        }

        // For updates
        if (itemDetails.getId() != null && itemDetails.getId().equals(existingItem.getId())) {
            // Update existing item
            existingItem.setItemName(itemDetails.getItemName());
            existingItem.setItemUnit(itemDetails.getItemUnit());
            existingItem.setItemLocation(itemDetails.getItemLocation());
            existingItem.setHsnCode(itemDetails.getHsnCode());
            existingItem.setItemRemark1(itemDetails.getItemRemark1());
            existingItem.setItemRemark2(itemDetails.getItemRemark2());
            itemRepository.save(existingItem);
            return;
        }

        // If we get here, it means there's a duplicate item code for this company
        throw new RuntimeException("Item code already exists for this company");
    }

    public List<ItemDetails> getItemsByCompanyCode(String companyCode) {
        if (companyCode == null || companyCode.trim().isEmpty()) {
            throw new RuntimeException("Company code cannot be empty");
        }
        return itemRepository.findByCompanyCode(companyCode);
    }

    public ItemDetails getItemById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new RuntimeException("Item ID cannot be empty");
        }
        return itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found"));
    }

    public ItemDetails getItemByItemCode(String companyCode, String itemCode) {
        if (companyCode == null || itemCode == null) {
            throw new RuntimeException("Company code and item code cannot be null");
        }
        return itemRepository.findByCompanyCodeAndItemCode(companyCode, itemCode);
    }

    public void deleteItem(String companyCode, String itemCode) {
        if (companyCode == null || itemCode == null) {
            throw new RuntimeException("Company code and item code cannot be null");
        }
        ItemDetails itemDetails = itemRepository.findByCompanyCodeAndItemCode(companyCode, itemCode);
        if (itemDetails == null) {
            throw new RuntimeException("Item not found");
        }
        itemRepository.delete(itemDetails);
    }

    public boolean isItemCodeAvailable(String itemCode, String companyCode) {
        return itemRepository.existsByCompanyCodeAndItemCode(companyCode, itemCode);
    }

    public String getItemMasterListForReport(String companyCode) throws JRException {
        System.out.println("Item server class started");
        System.out.println("Company code " + companyCode);
        System.out.println("Repository size " + itemRepository.findItemsByCompanyCode(companyCode).size());
        List<ItemmasterProjection> itemmasterProjectionList = itemRepository.findItemsByCompanyCode(companyCode);

        /*MatchOperation matchOperation = Aggregation.match(
                Criteria.where("companyCode").is(companyCode));
        Aggregation aggregation = Aggregation.newAggregation(matchOperation,
                Aggregation.project("itemCode", "itemName", " itemUnit", "itemLocation", "hsnCode", " itemRemark1", " itemRemark2",
                        " companyCode")
        );

        AggregationResults<ItemmasterProjection> results = mongoTemplate.aggregate(
                aggregation, "Item_Details", ItemmasterProjection.class);
        System.out.println("Report List size " + results.getMappedResults().size());
        for (ItemmasterProjection item : results.getMappedResults()) {
            System.out.println("Item Code " + item.getItemCode());
            System.out.println("Item Name " + item.getItemName());
        itemmasterProjectionList.addAll(results.getMappedResults());
        }*/


        String reportSavePath = "";
        String reportFilePath = "";
        if (getReportFileLocation() != null) {
            reportSavePath = getReportFileLocation();
            reportFilePath = getReportFileLocation();
        }

        File ReportFileName;
        if (reportFilePath.contains("/")) {
            ReportFileName = new File(reportFilePath + "/ItemMasterReport.jasper");
        } else {
            ReportFileName = new File(reportFilePath + "\\ItemMasterReport.jasper");
        }

        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(itemmasterProjectionList);

        Map<String, Object> parameters = new HashMap<>();
        String targetFileName = "ItemMaster.pdf";
        JasperPrint jasperPrint = JasperFillManager.fillReport(ReportFileName.getAbsolutePath(), parameters, dataSource);
        JasperExportManager.exportReportToPdfFile(jasperPrint, (reportSavePath + targetFileName));
        System.out.println((reportSavePath + targetFileName));
        return (reportSavePath + targetFileName);

    }

    public boolean isValidFilePath(String path) {
        System.out.println("File path checking");
        return Files.exists(Paths.get(path));
    }

    /**
     * getReportFileLocation method is used to get the tax invoice print report location .
     *
     * @return
     */
    private String getReportFileLocation() {
        try {
            String reportFilePath = new ClassPathResource("").getFile().getAbsolutePath().toString();
            if (reportFilePath.contains("/")) {
                reportFilePath = reportFilePath + "/static/report/itemreport/";
            } else {
                reportFilePath = reportFilePath + "\\static\\report\\itemreport\\";
            }
            return reportFilePath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getItemLookupReport(String itemCode) throws JRException {
        System.out.println("Item lookup report processing for item code: " + itemCode);

        List<ItemmasterProjection> itemProjections = itemRepository.findItemsByItemCodeOnly(itemCode);

        if (itemProjections.isEmpty()) {
            return "No items found with code: " + itemCode;
        }

        String reportSavePath = "";
        String reportFilePath = "";
        if (getReportFileLocation() != null) {
            reportSavePath = getReportFileLocation();
            reportFilePath = getReportFileLocation();
        }

        File reportFileName;
        if (reportFilePath.contains("/")) {
            reportFileName = new File(reportFilePath + "/itemLookup.jasper");
        } else {
            reportFileName = new File(reportFilePath + "\\itemLookup.jasper");
        }

        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(itemProjections);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ItemCode", itemCode);

        String targetFileName = "ItemLookup_" + itemCode + ".pdf";
        JasperPrint jasperPrint = JasperFillManager.fillReport(reportFileName.getAbsolutePath(), parameters, dataSource);
        JasperExportManager.exportReportToPdfFile(jasperPrint, (reportSavePath + targetFileName));
        System.out.println("Report saved to: " + (reportSavePath + targetFileName));
        return (reportSavePath + targetFileName);
    }

    public List<ItemmasterProjection> getItemProjectionsByCompany(String companyCode) {
        List<ItemmasterProjection> items = itemRepository.findItemsByCompanyCode(companyCode);
        return items;
    }

}