package com.dataline.BajajPortal.services;

import com.dataline.BajajPortal.model.master.ShippingDetails;
import com.dataline.BajajPortal.model.master.ShippingListProjection;
import com.dataline.BajajPortal.repository.ShippingDetailsProjection;
import com.dataline.BajajPortal.repository.ShippingDetailsRepository;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShippingDetailsService {

    @Autowired
    private ShippingDetailsRepository shippingDetailsRepository;
    @Autowired
    private SequenceGeneratorService sequenceGeneratorService;

    public static boolean isValidFilePath(String path) {
        System.out.println("File path checking");
        return Files.exists(Paths.get(path));
    }

    /**
     * getReportFileLocation method is used to get the tax invoice print report location .
     *
     * @return
     */
    private static String getReportFileLocation() {
        try {
            String reportFilePath = new ClassPathResource("").getFile().getAbsolutePath().toString();
            if (reportFilePath.contains("/")) {
                reportFilePath = reportFilePath + "/static/report/shippingreport/";
            } else {
                reportFilePath = reportFilePath + "\\static\\report\\shippingreport\\";
            }
            return reportFilePath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getshippingListforreport(String companyCode) throws JRException {
        System.out.println("vendor server class started");
        System.out.println("Company code " + companyCode);
        System.out.println("Repository size " + shippingDetailsRepository.findShippingsbycompanyCode(companyCode).size());
        List<ShippingDetailsProjection> shippingProjectionsList = shippingDetailsRepository.findShippingsbycompanyCode(companyCode);

        String reportSavePath = "";
        String reportFilePath = "";
        if (getReportFileLocation() != null) {
            reportSavePath = getReportFileLocation();
            reportFilePath = getReportFileLocation();
        }

        File ReportFileName;
        if (reportFilePath.contains("/")) {
            ReportFileName = new File(reportFilePath + "/ShippingMasterReport.jasper");
        } else {
            ReportFileName = new File(reportFilePath + "\\ShippingMasterReport.jasper");
        }

        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(shippingProjectionsList);

        Map<String, Object> parameters = new HashMap<>();
        String targetFileName = "ShippingMaster.pdf";
        JasperPrint jasperPrint = JasperFillManager.fillReport(ReportFileName.getAbsolutePath(), parameters, dataSource);
        JasperExportManager.exportReportToPdfFile(jasperPrint, (reportSavePath + targetFileName));
        System.out.println((reportSavePath + targetFileName));
        return (reportSavePath + targetFileName);

    }

    public List<ShippingDetails> getShippingsbycompanycode(String companyCode) {
        return shippingDetailsRepository.findByCompanyCode(companyCode);
    }

    public boolean addShippingDetails(ShippingDetails shippingDetails) {
        if (shippingDetails.getShippingid() == null || shippingDetails.getShippingid().isEmpty()) {
            shippingDetails.setShippingid(String.valueOf(sequenceGeneratorService.generateSequence(ShippingDetails.SEQUENCE_NAME)));
        }
        shippingDetailsRepository.save(shippingDetails);
        return true;
    }

    List<ShippingDetails> getAllShippingList() {
        return shippingDetailsRepository.findAll();
    }

    public List<ShippingListProjection> getShippingListByCompanyCode(String companyCode) {
        return shippingDetailsRepository.findAList(companyCode);
    }

    public ShippingDetails getShippingDetailsByVendorCodeAndCompanyCode(String shiftVendorCode, String companyCode) {
        List<ShippingDetails> shippingDetailslist = shippingDetailsRepository
                .findByShiftVendorCodeAndCompanyCode(shiftVendorCode, companyCode);
        return shippingDetailslist.isEmpty() ? null : shippingDetailslist.get(0);
    }

    public void deleteShippingDetails(String shiftVendorCode) {
        shippingDetailsRepository.deleteById(shiftVendorCode);
    }

    public List<ShippingDetails> getbycompanycode(String companyCode) {
        return shippingDetailsRepository.findByCompanyCode(companyCode);
    }

    public boolean shiftVendorCodeAvailable(String shiftVendorCode, String companyCode) {
        return shippingDetailsRepository.existsByShiftVendorCodeAndCompanyCode(shiftVendorCode, companyCode);
    }


}
