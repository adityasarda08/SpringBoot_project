package com.dataline.BajajPortal.services;

import com.dataline.BajajPortal.model.taxinvoice.TaxInvoice;
import com.dataline.BajajPortal.repository.TaxInvoicePrintProjection;
import com.dataline.BajajPortal.repository.TaxInvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Sort;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

@Service
@Slf4j
public class TaxInvoiceService {
    private static final Logger logger = LoggerFactory.getLogger(TaxInvoiceService.class);
    @Autowired
    private TaxInvoiceRepository taxInvoiceRepository;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private ItemDetailsServices itemDetailsServices;

    @Autowired
    private ShippingDetailsService shippingDetailsService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SequenceGeneratorService sequenceGeneratorService;

    @Autowired
    private CompanyService companyService;


    /**
     * Process the CSV file data to create TaxInvoices.
     * Returns a map with both the projections (for printing) and the saved
     * invoices.
     */

    public List<TaxInvoice> processFileData(MultipartFile file, String companyCode) throws Exception {

        List<TaxInvoice> validInvoices = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        log.info("Processing file data for company code: {}", companyCode);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                processFileLine(line, lineNumber, companyCode, validInvoices, warnings);
            }
        }

        // Log warnings but don't throw exception
        if (!warnings.isEmpty()) {
            log.warn("Validation warnings occurred:\n{}", String.join("\n", warnings));
        }

        // Save the valid invoices to the database
        List<TaxInvoice> savedInvoices = taxInvoiceRepository.saveAll(validInvoices);

        if (savedInvoices.isEmpty()) {
            return null;
        } else {
            return savedInvoices;
        }
    }

    public List<String> getOnlyInvoiceNumberList(List<TaxInvoice> taxInvoiceList) {
        List<String> invoiceNumberList = new ArrayList<>();
        for (TaxInvoice invoice : taxInvoiceList) {
            invoiceNumberList.add(invoice.getInvoiceNumber());
        }
        return invoiceNumberList;
    }
    // Inside TaxInvoiceService.java

    private void processFileLine(String line, int lineNumber, String companyCode,
                                 List<TaxInvoice> validInvoices,
                                 List<String> warnings) {
        String[] data = null; // Declare outside try
        try {
            data = line.split(","); // Split the line

            // *** ADDED: Log the length of the split array ***
            log.debug("Line {}: Number of columns found = {}", lineNumber, data.length);
            // ***********************************************

            // --- Optional but Recommended: Pre-check length before validation ---
            // Find the highest index accessed *before* padding (e.g., 32 in validateInvoiceData)
            int minRequiredColumnsForValidation = 39; // Index 38 needs length 39
            if (data.length < minRequiredColumnsForValidation) {
                String shortLineWarning = String.format(
                        "Line %d: Insufficient columns found (%d). Expected at least %d for validation. Skipping validation and mapping.",
                        lineNumber, data.length, minRequiredColumnsForValidation);
                log.warn(shortLineWarning);
                warnings.add(shortLineWarning);
                return; // Skip processing this line further
            }
            // --- End Optional Pre-check ---


            if (data.length > 0) { // Keep original check, though pre-check might make it redundant
                // Changed to non-blocking validation
                validateInvoiceData(data, warnings, lineNumber, companyCode);

                // Continue processing regardless of validation warnings
                TaxInvoice newInvoice = getTaxInvoiceFromFileLine(data, companyCode);

                // Check if invoice already exists (rest of the logic remains the same)
                List<TaxInvoice> existingInvoices = taxInvoiceRepository
                        .findByInvoiceNumberAndCompanyCode(newInvoice.getInvoiceNumber(), companyCode);

                if (!existingInvoices.isEmpty()) {
                    TaxInvoice existingInvoice = existingInvoices.get(0);
                    if (!existingInvoice.equals(newInvoice)) {
                        newInvoice.setInvoiceId(existingInvoice.getInvoiceId());
                        validInvoices.add(newInvoice);
                    } else {
                        log.info("Invoice {} already exists with no changes. Skipping update.",
                                newInvoice.getInvoiceNumber());
                    }
                } else {
                    newInvoice.setInvoiceId(sequenceGeneratorService.generateSequence(TaxInvoice.tax_sequence));
                    validInvoices.add(newInvoice);
                }
            }
        } catch (IndexOutOfBoundsException ioobe) { // *** CATCH IndexOutOfBoundsException SPECIFICALLY ***
            // Log detailed information when this specific error occurs
            int problematicIndex = -1;
            // Try to extract the index from the exception message (format might vary)
            String message = ioobe.getMessage();
            if (message != null) {
                try {
                    // Example: "Index 32 out of bounds for length 15"
                    String indexStr = message.replaceAll("[^0-9].*", "").trim(); // Extract leading numbers
                    if (!indexStr.isEmpty()) {
                        problematicIndex = Integer.parseInt(indexStr);
                    }
                } catch (NumberFormatException nfe) { /* ignore if parsing fails */ }
            }
            String errorMsg = String.format(
                    "Line %d: Index Out Of Bounds! Attempted to access index %d, but line only has %d columns. Raw line: [%s]",
                    lineNumber,
                    problematicIndex != -1 ? problematicIndex : -99, // Show extracted index or -99
                    (data != null ? data.length : 0), // Show actual length
                    line // Show the raw line content
            );
            log.error(errorMsg, ioobe); // Log the detailed message AND the stack trace
            warnings.add(errorMsg);
            // Continue processing other lines

        } catch (Exception e) { // Catch other general exceptions
            // *** Ensure stack trace is logged for other errors too ***
            log.error("Error processing line {}: {}", lineNumber, e.getMessage(), e); // Add 'e' to log stack trace
            warnings.add(String.format("Line %d: General Error - %s", lineNumber, e.getMessage()));
            // Continue processing other lines
        }
    }

    //fetch invoice for the period METHOD
    public List<TaxInvoicePrintProjection> fetchInvoicesForPeriod(String companyCode, LocalDate fromDate, LocalDate toDate) {
        Criteria criteria = Criteria.where("companyCode").is(companyCode)
                .and("invoiceDate").gte(fromDate).lte(toDate);

        MatchOperation matchOperation = Aggregation.match(criteria);

        // Project only required fields
        ProjectionOperation projectionOperation = Aggregation.project()
                .andInclude(
                        "invoiceNumber", "invoiceDate", "companyName", "totalAmount",
                        "vendorName", "vendorGstNo", "itemCode", "itemName", "basicAmount",
                        "cgstAmount", "sgstAmount", "igstAmount", "totalAmount"
                );

        // Sort by Invoice Date
        SortOperation sortOperation = Aggregation.sort(Sort.Direction.ASC, "invoiceDate");

        Aggregation aggregation = newAggregation(matchOperation, projectionOperation, sortOperation);

        return mongoTemplate.aggregate(aggregation, "Tax_Invoice", TaxInvoicePrintProjection.class).getMappedResults();
    }

    // Inside TaxInvoiceService.java

    private void validateInvoiceData(String[] data, List<String> warnings, int lineNumber, String companyCode) {
        try { // Optional: Wrap sections if needed for super granular logging

            // --- Vendor Code Check ---
            if (data.length > 1) { // Check length before accessing index 1 and 0
                boolean vendorCodeExists = vendorService.isVendorCodeExists(data[1].trim(), data[0].trim(), companyCode);
                if (!vendorCodeExists) {
                    // *** ADDED Line Number ***
                    warnings.add(String.format("Line %d: Vendor code/Plant code combination does not exist: %s / %s",
                            lineNumber, data[1].trim(), data[0].trim()));
                }
            } else {
                warnings.add(String.format("Line %d: Insufficient columns for Vendor/Plant code check.", lineNumber));
            }

            // --- Item Code Check ---
            if (data.length > 5) { // Check length before accessing index 5
                boolean itemCodeExists = itemDetailsServices.isItemCodeAvailable(data[5].trim(), companyCode);
                if (!itemCodeExists) {
                    // *** ADDED Line Number ***
                    warnings.add(String.format("Line %d: Item code does not exist: %s", lineNumber, data[5].trim()));
                }
            } else {
                warnings.add(String.format("Line %d: Insufficient columns for Item code check.", lineNumber));
            }


            // --- Shipping Vendor Code Check ---
            if (data.length > 32) { // Check length before accessing index 32
                boolean shiftVendorCodeExists = shippingDetailsService.shiftVendorCodeAvailable(data[32].trim(), companyCode);
                if (!shiftVendorCodeExists) {
                    // *** ADDED Line Number ***
                    warnings.add(String.format("Line %d: Shift Vendor Code does not exist: %s", lineNumber, data[32].trim()));
                }
            } else {
                warnings.add(String.format("Line %d: Insufficient columns for Shipping Vendor code check.", lineNumber));
            }


            // --- Invoice Date Check ---
            if (data.length > 9) { // Check length before accessing index 9
                String invDateStr = data[9].trim();
                if (invDateStr.isEmpty()) {
                    // *** ADDED Line Number ***
                    warnings.add(String.format("Line %d: Invoice Date is empty", lineNumber));
                } else if (!invDateStr.matches("\\d{8}")) {
                    // *** ADDED Line Number ***
                    warnings.add(String.format("Line %d: Invoice Date format incorrect (yyyyMMdd): %s", lineNumber, invDateStr));
                }
            } else {
                warnings.add(String.format("Line %d: Insufficient columns for Invoice Date check.", lineNumber));
            }


            // Check for empty fields using the modified helper
            // *** Pass lineNumber to checkEmptyField ***
            checkEmptyField(data, 1, "Vendor code", warnings, lineNumber);
            checkEmptyField(data, 8, "Invoice Number", warnings, lineNumber);
            // Add more field checks as needed

        } catch (IndexOutOfBoundsException ioobe) {
            // This catch is less likely to be hit if the pre-check in processFileLine is active,
            // but provides extra safety.
            log.error("Index error within validateInvoiceData on line {}: {}", lineNumber, ioobe.getMessage(), ioobe);
            warnings.add(String.format("Line %d: Validation Error - %s", lineNumber, ioobe.getMessage()));
            // Re-throw or handle as needed, but processFileLine should catch it anyway
        }
    }

    // Inside TaxInvoiceService.java

    /**
     * Helper method to check if a field is empty and add warning if it is.
     * *** ADDED lineNumber parameter ***
     */
    private void checkEmptyField(String[] data, int index, String fieldName, List<String> warnings, int lineNumber) {
        if (index >= data.length || data[index] == null || data[index].trim().isEmpty()) {
            // *** ADDED Line Number to message ***
            warnings.add(String.format("Line %d: %s is empty", lineNumber, fieldName));
        }
    }


    /**
     * Maps CSV data to a TaxInvoice object.
     * Modified to handle empty fields more gracefully.
     */
    private TaxInvoice getTaxInvoiceFromFileLine(String[] data, String companyCode) {
        try {
            TaxInvoice invoice = new TaxInvoice();

            // Make sure data array is large enough
            String[] safeData = ensureDataLength(data, 39);

            // Mapping string fields safely
            invoice.setVendorPlantCode(getSafeValue(safeData, 0));
            invoice.setVendorCode(getSafeValue(safeData, 1));
            invoice.setVendorGstNo(getSafeValue(safeData, 2));
            invoice.setHsnCode(getSafeValue(safeData, 3));
            invoice.setSacCode(getSafeValue(safeData, 4));
            invoice.setItemCode(getSafeValue(safeData, 5));
            invoice.setPoNo(getSafeValue(safeData, 6));
            invoice.setLineNo(getSafeValue(safeData, 7));
            invoice.setInvoiceNumber(getSafeValue(safeData, 8));

            // Mapping the invoice date field (format: yyyyMMdd)
            String invDateStr = getSafeValue(safeData, 9);
            if (invDateStr.matches("\\d{8}")) {
                invoice.setInvoiceDate(LocalDate.parse(invDateStr, DateTimeFormatter.ofPattern("yyyyMMdd")));
            }

            // Mapping numeric fields with fallbacks to default values
            invoice.setInvoiceQuantity(safeParseInteger(safeData[10], 0));
            invoice.setBasicRate(safeParseDouble(safeData[11], 0.0));
            invoice.setBasicAmount(safeParseDouble(safeData[12], 0.0));
            invoice.setFreight(safeParseDouble(safeData[13], 0.0));
            invoice.setPAndFCharges(safeParseDouble(safeData[14], 0.0));
            invoice.setOthersCharges(safeParseDouble(safeData[15], 0.0));
            invoice.setSubTotal(safeParseDouble(safeData[16], 0.0));
            invoice.setAdditionalTaxAmount(safeParseDouble(safeData[17], 0.0));
            invoice.setTaxableAmount(safeParseDouble(safeData[18], 0.0));
            invoice.setCgstPercentage(safeParseDouble(safeData[19], 0.0));
            invoice.setCgstAmount(safeParseDouble(safeData[20], 0.0));
            invoice.setSgstPercentage(safeParseDouble(safeData[21], 0.0));
            invoice.setSgstAmount(safeParseDouble(safeData[22], 0.0));
            invoice.setIgstPercentage(safeParseDouble(safeData[23], 0.0));
            invoice.setIgstAmount(safeParseDouble(safeData[24], 0.0));
            invoice.setUtgstPercentage(safeParseDouble(safeData[25], 0.0));
            invoice.setUtgstAmount(safeParseDouble(safeData[26], 0.0));
            invoice.setTotalAmount(safeParseDouble(safeData[27], 0.0));

            invoice.setEwayBillNo(getSafeValue(safeData, 28));

//            // Mapping Eway Bill Date (format: yyyyMMdd)
//            String ewayBillDateStr = getSafeValue(safeData, 29);
            // --- Eway Bill Date (Index 29) --- (Line ~286 in original request)
            String ewayBillDateStr = getSafeValue(safeData, 29);
            if (ewayBillDateStr.matches("\\d{8}")) {
                invoice.setEwayBillDate(LocalDate.parse(ewayBillDateStr, DateTimeFormatter.ofPattern("yyyyMMdd")));
            }
            invoice.setVehicleNo(getSafeValue(safeData, 30));
            invoice.setPackingDetails(getSafeValue(safeData, 31));
            invoice.setShiftVendorCode(getSafeValue(safeData, 32));
            invoice.setSignedQrCode(getSafeValue(safeData, 33));
            invoice.setDeliveryChallanNo(getSafeValue(safeData, 34));
            invoice.setDeliveryChallanAmount(safeParseDouble(safeData[35], 0.0));
            invoice.setDeliveryChallanDate(getSafeValue(safeData, 36));
            invoice.setTcsValue(safeParseDouble(safeData[37], 0.0));
            invoice.setEdiNumber(getSafeValue(safeData, 38));
            invoice.setCompanyCode(companyCode);

            return invoice;

        } catch (Exception e) {
            throw new RuntimeException("Error mapping data: " + e.getMessage(), e);
        }
    }

    /**
     * Utility method to ensure data array has enough elements
     */
    private String[] ensureDataLength(String[] data, int requiredLength) {
        if (data.length >= requiredLength) {
            return data;
        }

        String[] extendedData = new String[requiredLength];
        System.arraycopy(data, 0, extendedData, 0, data.length);

        // Fill remaining elements with empty strings
        for (int i = data.length; i < requiredLength; i++) {
            extendedData[i] = "";
        }

        return extendedData;
    }

    /**
     * Utility method to safely get a value from data array
     */
    private String getSafeValue(String[] data, int index) {
        if (index < data.length && data[index] != null) {
            return data[index].trim();
        }
        return "";
    }

    /**
     * Utility method to parse and validate double values with fallback.
     */
    private Double safeParseDouble(String value, Double defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid number format for value: {}. Using default: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Utility method to parse and validate integer values with fallback.
     */
    private Integer safeParseInteger(String value, Integer defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer format for value: {}. Using default: {}", value, defaultValue);
            return defaultValue;
        }
    }

    // The rest of the methods remain the same

    /**
     * Retrieves all tax invoices for a given company.
     */
    public List<TaxInvoice> getAllTaxInvoices(String companyCode) {
        return taxInvoiceRepository.findByCompanyCode(companyCode);
    }

    /**
     * Retrieves a single tax invoice by ID and company code.
     */
    public Optional<TaxInvoice> getTaxInvoiceById(Long id, String companyCode) {
        return taxInvoiceRepository.findByInvoiceIdAndCompanyCode(id, companyCode);
    }


    public String getTaxInvoicePrintProjectionList(
            List<String> invoiceNumber, String companyCode) throws JRException { // Renamed parameter for clarity

        if (invoiceNumber == null || invoiceNumber.isEmpty()) {
            throw new IllegalArgumentException("No invoice numbers provided");
        }

        // *** Replace MongoTemplate aggregation with Repository call ***
        List<TaxInvoicePrintProjection> combinedProjectionList = taxInvoiceRepository.findProjectedInvoiceByNumberAndCompany(invoiceNumber, companyCode);


        if (combinedProjectionList.isEmpty()) {
            return null;
        } else {

            List<TaxInvoicePrintProjection> finalProjectionList = new ArrayList<>();

            Integer TotalInvoiceCopyies = vendorService.getInvoiceCopyies(combinedProjectionList.get(0).getVendorCode(), combinedProjectionList.get(0).getVendorPlantCode(), companyCode);

            // Define labels
            List<String> labels = List.of(
                    "1-Original for Recipient",
                    "2-Duplicate for Transporter",
                    "3-Duplicate for Supplier",
                    "4-Extra Copy",
                    "5-Office Copy"
            );


            for (TaxInvoicePrintProjection taxInvoicePrintProjection : combinedProjectionList) {

                System.out.println("Logo Status " + taxInvoicePrintProjection.getCompanyLogo());
                System.out.println("Logo Name " + taxInvoicePrintProjection.getLogoName());

                if (TotalInvoiceCopyies > 5) {

                } else {
                    for (int row = 0; row < TotalInvoiceCopyies; row++) {
                        TaxInvoicePrintProjection copy = new TaxInvoicePrintProjection();
                        BeanUtils.copyProperties(taxInvoicePrintProjection, copy);
                        copy.setLabels(labels.get(row));
                        finalProjectionList.add(copy);
                    }
                }
            }

            // Sort the finalProjectionList by invoice number and label
            Collections.sort(finalProjectionList, Comparator
                    .comparing(TaxInvoicePrintProjection::getInvoiceNumber)
                    .thenComparing(TaxInvoicePrintProjection::getLabels));


            String reportSavePath = null;
            reportSavePath = "";
            String reportFilePath = "";
            if (getReportFileLocation() != null) {
                reportSavePath = getReportFileLocation();
                reportFilePath = getReportFileLocation();
            }

            // Determine which template to use
            File reportFile;

            if (companyService.getCompanyTemplatesStatus(companyCode).equalsIgnoreCase("Yes")) {
                System.out.println(companyService.getCompanyTemplatesName(companyCode));

                if (reportSavePath.contains("/")) {
                    reportSavePath = reportSavePath + "companies/" + companyCode + "/";
                    reportFilePath = reportFilePath + "companies/" + companyCode + "/" + companyService.getCompanyTemplatesName(companyCode) + ".jasper";
                } else {
                    reportSavePath = reportSavePath + "companies\\" + companyCode + "\\";
                    reportFilePath = reportFilePath + "companies\\" + companyCode + "\\" + companyService.getCompanyTemplatesName(companyCode) + ".jasper";
                }

            } else {

                if (reportSavePath.contains("/")) {
                    reportSavePath = reportSavePath + "Taxinvoiceerport/" + "/";
                    reportSavePath = reportSavePath + "Taxinvoiceerport/" + "/" + companyService.getCompanyTemplatesName(companyCode) + ".jasper";

                } else {
                    reportSavePath = reportSavePath + "Taxinvoiceerport\\" + "\\";
                    reportFilePath = reportFilePath + "Taxinvoiceerport/" + "/" + companyService.getCompanyTemplatesName(companyCode) + ".jasper";

                }
            }

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(finalProjectionList);
            Map<String, Object> parameters = new HashMap<>();
            JasperPrint jasperPrint = JasperFillManager.fillReport(reportFilePath, parameters, dataSource);
            // End loop for invoiceNumbers
            String targetFileName = "TaxInvoiceMaster.pdf";
            JasperExportManager.exportReportToPdfFile(jasperPrint, (reportSavePath + targetFileName));
            return (reportSavePath + targetFileName);

        }

    }

    public boolean isValidFilePath(String path) {
        System.out.println("File path checking");
        if (path == null || path.isEmpty()) {
            return false;
        }
        return Files.exists(Paths.get(path));
    }

    /**
     * getReportFileLocation method is used to get the tax invoice print report
     * location .
     *
     * @return
     */
    private String getReportFileLocation() {
        try {
            String reportFilePath = new ClassPathResource("").getFile().getAbsolutePath().toString();
            if (reportFilePath.contains("/")) {
                reportFilePath = reportFilePath + "/static/report/";
            } else {
                reportFilePath = reportFilePath + "\\static\\report\\";
            }
            return reportFilePath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Generate a tax invoice report for a date range with optional item code and
     * invoice number filters
     *
     * @param startDate     Starting date for the report
     * @param endDate       Ending date for the report
     * @param companyCode   The company code
     * @param itemCode      Optional item code to filter by
     * @param invoiceNumber Optional invoice number to filter by
     * @return Path to the generated PDF file
     * @throws JRException If there's an error generating the report
     */
    public String generateTaxInvoiceDateRange(LocalDate startDate, LocalDate endDate, String companyCode,
                                              String itemCode, String invoiceNumber) throws JRException {
        List<TaxInvoicePrintProjection> taxInvoicePrintProjectionList =
                taxInvoiceRepository.findProjectedInvoicesByDateRangeAndFilters(
                        startDate, endDate, companyCode, itemCode, invoiceNumber
                );

        if (!taxInvoicePrintProjectionList.isEmpty()) {
            // Generate the report
            String reportSavePath = getReportFileLocation();
            String reportFilePath = getReportFileLocation();

            if (reportSavePath == null || reportFilePath == null) {
                throw new RuntimeException("Report file location not found");
            }

            // Use a different report template for date range reports
            File reportFile;
            if (reportFilePath.contains("/")) {
                reportFile = new File(reportFilePath + "Taxinvoiceerport/" + "/DateSeperate.jasper");
            } else {
                reportFile = new File(reportFilePath + "\\Taxinvoiceerport" + "\\DateSeperate.jasper");
            }

            if (!reportFile.exists()) {
                throw new RuntimeException("Report template file not found: " + reportFile.getAbsolutePath());
            }

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(taxInvoicePrintProjectionList);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("startDate", Date.valueOf(startDate));
            parameters.put("endDate", Date.valueOf(endDate));
            parameters.put("reportTitle", "Tax Invoice Report: " + startDate + " to " + endDate);

            // Add filter information to the report title
            if (itemCode != null && !itemCode.isEmpty()) {
                parameters.put("reportTitle", parameters.get("reportTitle") + " (Item: " + itemCode + ")");
            }

            if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
                parameters.put("reportTitle", parameters.get("reportTitle") + " (Invoice: " + invoiceNumber + ")");
            }

            String targetFileName = "TaxInvoiceDateRangeReport_" + startDate + "_to_" + endDate;

            // Add filters to the filename
            if (itemCode != null && !itemCode.isEmpty()) {
                targetFileName += "_item_" + itemCode;
            }
            if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
                targetFileName += "_inv_" + invoiceNumber;
            }

            targetFileName += ".pdf";
            JasperPrint jasperPrint = JasperFillManager.fillReport(reportFile.getAbsolutePath(), parameters,
                    dataSource);
            String fullPath = reportSavePath + targetFileName;
            JasperExportManager.exportReportToPdfFile(jasperPrint, fullPath);

            return fullPath;
        } else {
            throw new RuntimeException("No records found for the specified criteria");
        }
    }

    public String getItemLookupReport(String itemCode, String companyCode) throws JRException {
        System.out.println("Item lookup report processing for item code: " + itemCode);
        List<TaxInvoicePrintProjection> taxInvoicePrintProjectionList =
                taxInvoiceRepository.findProjectedInvoicesByItemCodeAndCompany(itemCode, companyCode);

        System.out.println("Found " + taxInvoicePrintProjectionList.size() + " records for item code: " + itemCode);

        // if (taxInvoicePrintProjectionList.isEmpty()) {
        // throw new RuntimeException("No records found for item code: " + itemCode); }

        String reportSavePath = "";
        String reportFilePath = "";
        if (getReportFileLocation() != null) {
            reportSavePath = getReportFileLocation();
            reportFilePath = getReportFileLocation();
        }

        File reportFileName;
        if (reportFilePath.contains("/")) {
            reportFileName = new File(reportFilePath + "/Taxinvoiceerport" + "/itemLookup.jasper");
        } else {
            reportFileName = new File(reportFilePath + "\\Taxinvoiceerport" + "\\itemLookup.jasper");
        }

        if (!reportFileName.exists()) {
            return "Report template not found at: " + reportFileName.getAbsolutePath();
        }

        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(taxInvoicePrintProjectionList);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ItemCode", itemCode);

        String targetFileName = "ItemLookup_" + itemCode + ".pdf";
        JasperPrint jasperPrint = JasperFillManager.fillReport(reportFileName.getAbsolutePath(), parameters,
                dataSource);
        JasperExportManager.exportReportToPdfFile(jasperPrint, (reportSavePath + targetFileName));
        System.out.println("Report saved to: " + (reportSavePath + targetFileName));
        return (reportSavePath + targetFileName);
    }

    /**
     * Generate a tax invoice report for a specific vendor with optional filters.
     *
     * @param startDate       Starting date for the report
     * @param endDate         Ending date for the report
     * @param companyCode     The company code
     * @param vendorCode      Vendor code (required)
     * @param vendorPlantCode Optional vendor plant code to filter
     * @param invoiceNumber   Optional invoice number to filter
     * @return Path to the generated PDF file
     * @throws JRException If there's an error generating the report
     */
    public String generateTaxInvoiceVendorWise(LocalDate startDate, LocalDate endDate, String companyCode,
                                               String vendorCode, String vendorPlantCode, String invoiceNumber) throws JRException {

        Criteria matchCriteria = Criteria.where("companyCode").is(companyCode)
                .and("invoiceDate").gte(startDate).lte(endDate)
                .and("vendorCode").is(vendorCode);

        if (vendorPlantCode != null && !vendorPlantCode.isEmpty()) {
            matchCriteria = matchCriteria.and("vendorPlantCode").is(vendorPlantCode);
        }

        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            matchCriteria = matchCriteria.and("invoiceNumber").is(invoiceNumber);
        }

        MatchOperation matchOperation = Aggregation.match(matchCriteria);

        Aggregation aggregation = newAggregation(
                matchOperation,

                // Company Lookup
                LookupOperation.newLookup()
                        .from("Company_Details")
                        .localField("companyCode")
                        .foreignField("companyCode")
                        .as("company"),
                Aggregation.unwind("company", true),

                // Vendor Lookup
                LookupOperation.newLookup()
                        .from("Vendor_Details")
                        .localField("vendorCode")
                        .foreignField("vendorCode")
                        .pipeline(
                                newAggregation(
                                        Aggregation.match(Criteria.where("companyCode").is(companyCode)
                                                .and("vendorPlantCode").is("$$vendorPlantCode")))
                                        .getPipeline())
                        .as("vendor"),
                Aggregation.unwind("vendor", true),

                // Item Lookup
                LookupOperation.newLookup()
                        .from("Item_Details")
                        .localField("itemCode")
                        .foreignField("itemCode")
                        .pipeline(
                                newAggregation(
                                        Aggregation.match(Criteria.where("companyCode").is(companyCode)))
                                        .getPipeline())
                        .as("item"),
                Aggregation.unwind("item", true),

                Aggregation.project("invoiceNumber", "company.companyName", "company.companyAddress", "company.companyPanNo", "company.companyGstNo",
                        "company.companyStateCode", "company.taxReverseCharge", "company.companyContactNo", "company.companyEmail", "company.companyWebsite",
                        "invoiceDate", "vendorCode", "vendorPlantCode", "vendor.vendorName", "vendor.vendorAddress", "vendor.vendorGstNo", "vendor.vendorCIN",
                        "vendor.vendorStateCode", "vendor.vendorPAN", "item.hsnCode", "sacCode", "item.itemCode", "poNo", "lineNo", "invoiceQuantity",
                        "basicRate", "basicAmount", "freight", "othersCharges", "subTotal", "additionalTaxAmount", "taxableAmount", "cgstPercentage",
                        "cgstAmount", "sgstPercentage", "sgstAmount", "igstPercentage", "igstAmount", "utgstPercentage", "utgstAmount", "totalAmount",
                        "ewayBillNo", "ewayBillDate", "vehicleNo", "vendor.remark1", "vendor.remark2", "ediNumber", "shiftVendorCode", "item.itemRemark1",
                        "item.itemRemark2", "signedQrCode", "deliveryChallanNo", "deliveryChallanDate", "deliveryChallanAmount", "tcsValue",
                        "item.itemName", "item.itemUnit"),

                Aggregation.sort(Sort.Direction.ASC, "invoiceDate")
        );

        AggregationResults<TaxInvoicePrintProjection> results = mongoTemplate.aggregate(
                aggregation, "Tax_Invoice", TaxInvoicePrintProjection.class);

        List<TaxInvoicePrintProjection> taxInvoiceList = results.getMappedResults();

        if (taxInvoiceList.isEmpty()) {
            throw new RuntimeException("No records found for the specified criteria");
        }

        // Report file path setup
        String reportSavePath = getReportFileLocation();
        String reportFilePath = getReportFileLocation();

        File reportFile = new File(reportFilePath + (reportFilePath.contains("/") ? "/DateSeperate.jasper" : "\\DateSeperate.jasper"));
        if (!reportFile.exists()) {
            throw new RuntimeException("Report template file not found: " + reportFile.getAbsolutePath());
        }

        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(taxInvoiceList);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startDate", Date.valueOf(startDate));
        parameters.put("endDate", Date.valueOf(endDate));
        parameters.put("reportTitle", "Vendor-wise Tax Invoice Report: " + startDate + " to " + endDate + " (Vendor: " + vendorCode + ")");

        // Additional info in report title
        if (vendorPlantCode != null && !vendorPlantCode.isEmpty()) {
            parameters.put("reportTitle", parameters.get("reportTitle") + " (Plant: " + vendorPlantCode + ")");
        }
        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            parameters.put("reportTitle", parameters.get("reportTitle") + " (Invoice: " + invoiceNumber + ")");
        }

        // Filename logic
        String targetFileName = "VendorWise_TaxInvoice_" + startDate + "_to_" + endDate + "_vendor_" + vendorCode;
        if (vendorPlantCode != null && !vendorPlantCode.isEmpty()) {
            targetFileName += "_plant_" + vendorPlantCode;
        }
        if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
            targetFileName += "_inv_" + invoiceNumber;
        }
        targetFileName += ".pdf";

        JasperPrint jasperPrint = JasperFillManager.fillReport(reportFile.getAbsolutePath(), parameters, dataSource);
        String fullPath = reportSavePath + targetFileName;
        JasperExportManager.exportReportToPdfFile(jasperPrint, fullPath);

        return fullPath;
    }

    //generate vendor summary pdf
    public String generateVendorSummaryPDF(String startDate, String endDate, String vendorCode, String companyCode) {
        try {
            LocalDate fromDate = LocalDate.parse(startDate);
            LocalDate toDate = LocalDate.parse(endDate);

            // Build criteria
            Criteria criteria = Criteria.where("companyCode").is(companyCode)
                    .and("invoiceDate").gte(fromDate).lte(toDate);

            if (vendorCode != null && !vendorCode.isEmpty()) {
                criteria = criteria.and("vendorCode").is(vendorCode);
            }

            MatchOperation match = Aggregation.match(criteria);
            ProjectionOperation projection = Aggregation.project()
                    .andInclude("invoiceDate", "vendorCode", "vendorName", "totalAmount", "invoiceNumber");

            SortOperation sort = Aggregation.sort(Sort.Direction.ASC, "invoiceDate");

            Aggregation aggregation = Aggregation.newAggregation(match, projection, sort);

            List<TaxInvoicePrintProjection> result = mongoTemplate.aggregate(
                    aggregation, "Tax_Invoice", TaxInvoicePrintProjection.class
            ).getMappedResults();

            if (result.isEmpty()) {
                throw new RuntimeException("No records found for the specified vendor and date range.");
            }

            // Define exact report path
            String jasperFilePath = "Z:\\jnec\\2025\\BajajPortal\\src\\main\\resources\\Static\\report\\vendorreport\\VendorSummaryForThePeriod.jasper";
            File reportFile = new File(jasperFilePath);
            if (!reportFile.exists()) {
                throw new RuntimeException("Report template not found at: " + reportFile.getAbsolutePath());
            }

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(result);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("startDate", java.sql.Date.valueOf(fromDate));
            parameters.put("endDate", java.sql.Date.valueOf(toDate));
            parameters.put("vendorCode", vendorCode);

            String reportFileName = "VendorSummary_" + fromDate + "_to_" + toDate;
            if (vendorCode != null && !vendorCode.isEmpty()) {
                reportFileName += "_vendor_" + vendorCode;
            }
            reportFileName += ".pdf";

            // Save to same folder for consistency
            String reportSavePath = jasperFilePath.substring(0, jasperFilePath.lastIndexOf("\\") + 1);
            String fullOutputPath = reportSavePath + reportFileName;

            File jasperFile = new File(jasperFilePath);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperFile.getAbsolutePath(), parameters, dataSource);
            JasperExportManager.exportReportToPdfFile(jasperPrint, fullOutputPath);

            return fullOutputPath;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Vendor Summary report: " + e.getMessage(), e);
        }
    }

    // generate vendor wise pdf
    public String generateVendorWisePDF(String vendorCode, String companyCode) {
        try {
            List<TaxInvoicePrintProjection> taxInvoicePrintProjectionList =
                    taxInvoiceRepository.findProjectedInvoicesByVendorCodeAndCompany(vendorCode, companyCode);

            if (taxInvoicePrintProjectionList.isEmpty()) {
                throw new RuntimeException("No invoice data found for vendor: " + vendorCode);
            }

            String reportSavePath = "";
            String reportFilePath = "";
            if (getReportFileLocation() != null) {
                reportSavePath = getReportFileLocation();
                reportFilePath = getReportFileLocation();
            }

            File reportFile;
            if (reportFilePath.contains("/")) {
                reportFile = new File(reportFilePath + "/VendorWiseForThePeriod.jasper");
            } else {
                reportFile = new File(reportFilePath + "\\VendorWiseForThePeriod.jasper");
            }

            if (!reportFile.exists()) {
                throw new FileNotFoundException("Report template not found: " + reportFile.getAbsolutePath());
            }

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(taxInvoicePrintProjectionList);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("vendorCode", vendorCode);
            parameters.put("reportTitle", "Vendor Wise Invoice Report for " + vendorCode);


            String reportSaveDir = reportFilePath;

            String reportFileName = "VendorWise_" + vendorCode + ".pdf";
            String fullOutputPath = reportSavePath + reportFileName;

            JasperPrint jasperPrint = JasperFillManager.fillReport(reportFile.getAbsolutePath(), parameters, dataSource);
            JasperExportManager.exportReportToPdfFile(jasperPrint, fullOutputPath);
            System.out.println("Report saved to: " + fullOutputPath);
            return fullOutputPath;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Vendor Wise report: " + e.getMessage(), e);
        }
    }


}
