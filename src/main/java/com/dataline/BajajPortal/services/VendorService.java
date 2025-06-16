package com.dataline.BajajPortal.services;

import com.dataline.BajajPortal.model.master.VendorDetails;
import com.dataline.BajajPortal.repository.VendorProjection;
import com.dataline.BajajPortal.repository.VendorRepository;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@Transactional
public class VendorService {

    private final VendorRepository vendorRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Autowired
    public VendorService(VendorRepository vendorRepository, SequenceGeneratorService sequenceGeneratorService) {
        this.vendorRepository = vendorRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

//    public static VendorDetails findByVendorCode(String vendorCode) {
////        return vendorRepository.findByVendorCode(vendorCode);
//
//    }

    public VendorDetails saveVendor(VendorDetails vendorDetails) {
        if (vendorDetails.get_id() == null || vendorDetails.get_id().isEmpty()) {
            vendorDetails.set_id(String.valueOf(sequenceGeneratorService.generateSequence(VendorDetails.SEQUENCE_NAME)));
        }
        return vendorRepository.save(vendorDetails);
        //return null;
    }

    public List<VendorDetails> getVendorsByCompanyCode(String companyCode) {
        return vendorRepository.findByCompanyCode(companyCode);
    }

    public List<VendorDetails> getVendorsByVendorCode(String vendorCode) {
        return vendorRepository.findByVendorCode(vendorCode);
    }

    public VendorDetails getVendorById(String id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found with id: " + id));
    }

    public List<VendorDetails> getAllVendors() {
        return vendorRepository.findAll(); // Fetch all vendors from MongoDB
    }

    public void deleteVendor(String id) {
        vendorRepository.deleteById(id);
    }

    public List<VendorDetails> getvendorPlantCode(String vendorPlantCode) {
        return vendorRepository.findByVendorPlantCode(vendorPlantCode);

    }

    /*
        The Following Method is used for checking Vendor Code is available in vendor Details by Vendor Code and Company Code
     */

    boolean isVendorCodeExists(String vendorCode, String plantCode, String companyCode) {
        return vendorRepository.existsByVendorCodeAndVendorPlantCodeAndCompanyCode(vendorCode, plantCode, companyCode);
    }

    public String getvendorListforreport(String companyCode) throws JRException {
        System.out.println("vendor server class started");
        System.out.println("Company code " + companyCode);
        System.out.println("Repository size " + vendorRepository.findvendorbycompanyCode(companyCode).size());
        List<VendorProjection> vendorProjectionsList = vendorRepository.findvendorbycompanyCode(companyCode);

        String reportSavePath = "";
        String reportFilePath = "";
        if (getReportFileLocation() != null) {
            reportSavePath = getReportFileLocation();
            reportFilePath = getReportFileLocation();
        }

        File ReportFileName;
        if (reportFilePath.contains("/")) {
            ReportFileName = new File(reportFilePath + "/VendorMasterReport.jasper");
        } else {
            ReportFileName = new File(reportFilePath + "\\VendorMasterReport.jasper");
        }

        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(vendorProjectionsList);

        Map<String, Object> parameters = new HashMap<>();
        String targetFileName = "vendorMaster.pdf";
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
                reportFilePath = reportFilePath + "/static/report/vendorreport/";
            } else {
                reportFilePath = reportFilePath + "\\static\\report\\vendorreport\\";
            }
            return reportFilePath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * This method is used to get the invoice copies for a given vendor, plant, and company.
     *
     * @param vendorCode  The vendor code.
     * @param plantCode   The plant code.
     * @param companyCode The company code.
     * @return The number of invoice copies.
     */
    public Integer getInvoiceCopyies(String vendorCode, String plantCode, String companyCode) {
        return vendorRepository.findInvoiceCopiesByCompanyCodeAndVendorCodeAndVendorPlantCode(companyCode, vendorCode, plantCode).get(0);
    }

    /**
     * Retrieves a list of vendor projections (code, plant, name) for a given company.
     *
     * @param companyCode The company code.
     * @return List of VendorProjection.
     */
//    public List<VendorProjection> getVendorProjectionsByCompany(String companyCode) {
//        return vendorRepository.findVendorProjectionByCompanyCode(companyCode);
//    }
//
//    public List<VendorProjection> getAllVendorForSearch() {
//        return VendorRepository.findVendorProjectionByCompanyCode();
//    }

    /**
     * Retrieves a list of vendor projections (code, plant, name) for a given company.
     *
     * @param companyCode The company code.
     * @return List of VendorProjection.
     */
    public List<VendorProjection> getVendorProjectionsByCompany(String companyCode) {
        List<VendorProjection> vendors = vendorRepository.findVendorProjectionByCompanyCode(companyCode);
//        // Debug log the data
//        vendors.forEach(v -> System.out.println("Vendor Code: " + v.getVendorCode() +
//                ", Plant Code: " + v.getVendorPlantCode() +
//                ", Name: " + v.getVendorName()));
        return vendors;
    }

    /**
     * getVendorGSTNo method will return the vendor GST No by passing the vendorcode as parameter .
     *
     * @param vendorCode
     * @return
     */
    public String getVendorGSTNo(String vendorCode, String companyCode) {
        List<VendorProjection> vendorGstNoList = vendorRepository.findVendorGstNoByVendorCodeAndCompanyCode(vendorCode, companyCode);
        return vendorGstNoList.isEmpty() ? "URP" : vendorGstNoList.get(0).getVendorGST();
    }

}