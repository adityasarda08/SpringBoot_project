package com.dataline.BajajPortal.controller;

import com.dataline.BajajPortal.model.taxinvoice.TaxInvoice;
import com.dataline.BajajPortal.services.TaxInvoiceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import jakarta.servlet.http.HttpSession;
import net.sf.jasperreports.engine.JRException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
// import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

@Controller
@RequestMapping("/TaxInvoice")
public class TaxInvoiceController {

    private static final Logger logger = Logger.getLogger(TaxInvoiceController.class.getName());

    @Autowired
    private TaxInvoiceService taxInvoiceService;

    @SuppressWarnings("unused")
    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/new")
    public String showUploadForm(Model model) {
        logger.info("Showing upload form");
        List<TaxInvoice> invoices = new ArrayList<>();
        model.addAttribute("invoices", invoices);
        return "TaxInvoice/uploadFile";
    }

    @PostMapping("/uploadEdiFile")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   Model model,
                                   HttpSession session) {
        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.endsWith(".txt") && !fileName.endsWith(".csv"))) {
                model.addAttribute("error", "Invalid file type! Only .txt and .csv files are allowed.");
                return "TaxInvoice/uploadFile";
            }

            // Retrieve company code from session
            String companyCode = (String) session.getAttribute("companyCode");
            if (companyCode == null) {
                model.addAttribute("error", "Company code not found in session");
                return "redirect:/login";
            }

            List<TaxInvoice> processedInvoices = taxInvoiceService.processFileData(file, companyCode);

            if (processedInvoices == null || processedInvoices.isEmpty()) {
                model.addAttribute("error", "No valid invoices were processed");
                return "TaxInvoice/uploadFile";
            }

            List<String> invoiceNumberList = taxInvoiceService.getOnlyInvoiceNumberList(processedInvoices);
            logger.info("Inv No " + invoiceNumberList.toString());
            model.addAttribute("InvoiceNumberList", invoiceNumberList);
            return "TaxInvoice/uploadFile";

        } catch (Exception e) {
            model.addAttribute("error", "Error processing file: " + e.getMessage());
            return "TaxInvoice/uploadFile";
        }
    }

    @GetMapping({"/print/{invoiceNumbers}"})
    @ResponseBody
    public ResponseEntity<InputStreamResource> printInvoices(
            @RequestParam(required = false) List<String> invoiceNumberList,
            HttpSession session) {

        try {
            String companyCode = (String) session.getAttribute("companyCode");
            if (companyCode == null) {
                Document document = new Document();
                String blankPdfFileName = "blank.pdf";
                PdfWriter.getInstance(document, new FileOutputStream(blankPdfFileName));
                document.open();
                document.add(new Paragraph("Error: Company code not found in session"));
                document.close();
                File blankFile = new File(blankPdfFileName);
                HttpHeaders headers = new HttpHeaders();
                headers.add("content-disposition", "inline;filename=" + blankPdfFileName);
                InputStreamResource resource = new InputStreamResource(new FileInputStream(blankFile));
                return ResponseEntity.ok().headers(headers).contentLength(blankFile.length())
                        .contentType(MediaType.parseMediaType("application/pdf")).body(resource);
            }

            // Fetch the projection data for the selected invoices
            String fileName = taxInvoiceService
                    .getTaxInvoicePrintProjectionList(invoiceNumberList, companyCode);

            if (taxInvoiceService.isValidFilePath(fileName)) {
                File file = new File(fileName);
                if (file.exists()) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("content-disposition", "inline;filename=" + fileName);
                    InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
                    return ResponseEntity.ok().headers(headers).contentLength(file.length())
                            .contentType(MediaType.parseMediaType("application/pdf")).body(resource);
                }
            }

            // Create blank PDF with error message if file not found
            Document document = new Document();
            String blankPdfFileName = "blank.pdf";
            PdfWriter.getInstance(document, new FileOutputStream(blankPdfFileName));
            document.open();
            document.add(new Paragraph("Report File Not Found"));
            document.close();
            File blankFile = new File(blankPdfFileName);
            HttpHeaders headers = new HttpHeaders();
            headers.add("content-disposition", "inline;filename=" + blankPdfFileName);
            InputStreamResource resource = new InputStreamResource(new FileInputStream(blankFile));
            return ResponseEntity.ok().headers(headers).contentLength(blankFile.length())
                    .contentType(MediaType.parseMediaType("application/pdf")).body(resource);

        } catch (JRException | FileNotFoundException | DocumentException e) {
            throw new RuntimeException("Error generating invoice PDF: " + e.getMessage(), e);
        }
    }

    @GetMapping("/report")
    public String showReportForm(Model model) {
        model.addAttribute("startDate", "");
        model.addAttribute("endDate", "");
        return "TaxInvoice/itemSummaryForm";
    }

    @GetMapping("/generateReport")
    public String showGenerateReportForm(@RequestParam(value = "startDate", required = false) String startDate,
                                         @RequestParam(value = "endDate", required = false) String endDate,
                                         Model model) {

        // Set default dates if parameters are missing
        if (startDate == null || startDate.isEmpty()) {
            LocalDate now = LocalDate.now();
            startDate = now.withDayOfMonth(1).toString();
        }

        if (endDate == null || endDate.isEmpty()) {
            endDate = LocalDate.now().toString();
        }

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "TaxInvoice/itemSummaryForm";
    }

    @PostMapping("/generateReport")
    @ResponseBody
    public ResponseEntity<InputStreamResource> generateDateRangeReport(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(required = false) String itemCode,
            @RequestParam(required = false) String invoiceNumber,
            HttpSession session) {

        try {
            // Set default dates if parameters are missing
            if (startDate == null || startDate.isEmpty()) {
                // Set default to first day of current month
                LocalDate now = LocalDate.now();
                startDate = now.withDayOfMonth(1).toString();
            }

            if (endDate == null || endDate.isEmpty()) {
                // Set default to current date
                endDate = LocalDate.now().toString();
            }

            String companyCode = (String) session.getAttribute("companyCode");
            if (companyCode == null) {
                Document document = new Document();
                String blankPdfFileName = "blank.pdf";
                PdfWriter.getInstance(document, new FileOutputStream(blankPdfFileName));
                document.open();
                document.add(new Paragraph("Error: Company code not found in session"));
                document.close();
                File blankFile = new File(blankPdfFileName);
                HttpHeaders headers = new HttpHeaders();
                headers.add("content-disposition", "inline;filename=" + blankPdfFileName);
                InputStreamResource resource = new InputStreamResource(new FileInputStream(blankFile));
                return ResponseEntity.ok().headers(headers).contentLength(blankFile.length())
                        .contentType(MediaType.parseMediaType("application/pdf")).body(resource);
            }

            // Format dates if needed
            LocalDate startLocalDate = LocalDate.parse(startDate);
            LocalDate endLocalDate = LocalDate.parse(endDate);

            // Generate the report with both itemCode and invoiceNumber parameters
            String fileName = taxInvoiceService.generateTaxInvoiceDateRange(
                    startLocalDate, endLocalDate, companyCode, itemCode, invoiceNumber);

            if (fileName != null && taxInvoiceService.isValidFilePath(fileName)) {
                File file = new File(fileName);
                if (file.exists()) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("content-disposition", "inline;filename=" + fileName);
                    InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
                    return ResponseEntity.ok().headers(headers).contentLength(file.length())
                            .contentType(MediaType.parseMediaType("application/pdf")).body(resource);
                }
            }

            // Create blank PDF with error message if file not found
            Document document = new Document();
            String blankPdfFileName = "blank.pdf";
            PdfWriter.getInstance(document, new FileOutputStream(blankPdfFileName));
            document.open();
            document.add(new Paragraph("Report File Not Found"));
            document.close();
            File blankFile = new File(blankPdfFileName);
            HttpHeaders headers = new HttpHeaders();
            headers.add("content-disposition", "inline;filename=" + blankPdfFileName);
            InputStreamResource resource = new InputStreamResource(new FileInputStream(blankFile));
            return ResponseEntity.ok().headers(headers).contentLength(blankFile.length())
                    .contentType(MediaType.parseMediaType("application/pdf")).body(resource);
        } catch (RuntimeException e) {
            // Handle the "No records found" exception
            return createErrorPdfResponse("No records found: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Error generating report: " + e.getMessage(), e);
        }
    }

    @GetMapping("/lookup")
    public String showItemLookupForm(Model model) {
        return "Items/itemLookup";
    }


    @GetMapping("/lookupReport")
    public Object getItemLookupReport(
            @RequestParam(required = false) String itemCode,
            Model model,
            HttpSession session) {

        // If itemCode is missing, redirect to the lookup form
        if (itemCode == null || itemCode.trim().isEmpty()) {
            return "redirect:/TaxInvoice/lookup";
        }

        try {
            System.out.println("Processing item lookup report for item code: " + itemCode);

            // Get company code from session
            String companyCode = (String) session.getAttribute("companyCode");

            String fileName = taxInvoiceService.getItemLookupReport(itemCode, companyCode);

            if (taxInvoiceService.isValidFilePath(fileName)) {
                File file = new File(fileName);
                HttpHeaders headers = new HttpHeaders();
                headers.add("content-disposition", "inline;filename=" + fileName);
                InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(file.length())
                        .contentType(MediaType.parseMediaType("application/pdf"))
                        .body(resource);
            } else {
                Document document = new Document();
                String blankPdfFileName = "error.pdf";
                PdfWriter.getInstance(document, new FileOutputStream(blankPdfFileName));
                document.open();
                document.add(new Paragraph(fileName)); // This will contain the error message
                document.close();
                File blankFile = new File(blankPdfFileName);
                HttpHeaders headers = new HttpHeaders();
                headers.add("content-disposition", "inline;filename=" + blankPdfFileName);
                InputStreamResource resource = new InputStreamResource(new FileInputStream(blankFile));
                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(blankFile.length())
                        .contentType(MediaType.parseMediaType("application/pdf"))
                        .body(resource);
            }
        } catch (Exception e) {
            // Log the error
            logger.severe("Error generating report: " + e.getMessage());

            // Add error message to model and return to form
            model.addAttribute("error", "Error generating report: " + e.getMessage());
            return "Items/itemLookup";
        }
    }

    private ResponseEntity<InputStreamResource> createErrorPdfResponse(String errorMessage) {
        try {
            Document document = new Document();
            String errorPdfFileName = "error.pdf";
            PdfWriter.getInstance(document, new FileOutputStream(errorPdfFileName));
            document.open();
            document.add(new Paragraph(errorMessage));
            document.close();

            File errorFile = new File(errorPdfFileName);
            HttpHeaders headers = new HttpHeaders();
            headers.add("content-disposition", "inline;filename=" + errorPdfFileName);
            InputStreamResource resource = new InputStreamResource(new FileInputStream(errorFile));

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(errorFile.length())
                    .contentType(MediaType.parseMediaType("application/pdf"))
                    .body(resource);
        } catch (Exception e) {
            // If we can't even create the error PDF, return a plain text error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(new InputStreamResource(new java.io.ByteArrayInputStream(
                            ("Failed to generate error report: " + e.getMessage()).getBytes())));
        }
    }
    // for vendor wise

    @GetMapping("/vendorwise")
    public String showVendorWiseForm(Model model) {
        return "vendor/vendorwise"; // Path: templates/vendor/vendorwise.html
    }

    @GetMapping("/vendorwiseReport")
    public Object generateVendorWiseReport(@RequestParam(required = false) String vendorCode,
                                           HttpSession session,
                                           Model model) {
        if (vendorCode == null || vendorCode.trim().isEmpty()) {
            return "redirect:/Vendor/vendorwise";
        }
        try {
            String companyCode = (String) session.getAttribute("companyCode");
            String fileName = taxInvoiceService.generateVendorWisePDF(vendorCode, companyCode);

            if (taxInvoiceService.isValidFilePath(fileName)) {
                File file = new File(fileName);
                HttpHeaders headers = new HttpHeaders();
                headers.add("content-disposition", "inline;filename=" + fileName);
                InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
                return ResponseEntity.ok().headers(headers).contentLength(file.length())
                        .contentType(MediaType.parseMediaType("application/pdf")).body(resource);
            } else {
                return createErrorPdfResponse(fileName);
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error generating vendor-wise report: " + e.getMessage());
            return "vendor/vendorwise";
        }
    }

    @GetMapping("/vendorsummary")
    public String showVendorSummaryForm(Model model) {
        model.addAttribute("startDate", "");
        model.addAttribute("endDate", "");
        return "vendor/vendorsummary"; // Path: templates/vendor/vendorsummary.html
    }

    @GetMapping("/generateVendorSummary")
    @ResponseBody
    public ResponseEntity<InputStreamResource> generateVendorSummaryReport(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "vendorCode", required = false) String vendorCode,
            HttpSession session) {

        try {
            if (startDate == null || startDate.isEmpty()) {
                LocalDate now = LocalDate.now();
                startDate = now.withDayOfMonth(1).toString();
            }
            if (endDate == null || endDate.isEmpty()) {
                endDate = LocalDate.now().toString();
            }

            String companyCode = (String) session.getAttribute("companyCode");
            if (companyCode == null) {
                return createErrorPdfResponse("Error: Company code not found in session");
            }

            String fileName = taxInvoiceService.generateVendorSummaryPDF(startDate, endDate, vendorCode, companyCode);
            if (taxInvoiceService.isValidFilePath(fileName)) {
                File file = new File(fileName);
                HttpHeaders headers = new HttpHeaders();
                headers.add("content-disposition", "inline;filename=" + fileName);
                InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
                return ResponseEntity.ok().headers(headers).contentLength(file.length())
                        .contentType(MediaType.parseMediaType("application/pdf")).body(resource);
            }

            return createErrorPdfResponse("Report file not found");
        } catch (Exception e) {
            return createErrorPdfResponse("Error generating vendor summary report: " + e.getMessage());
        }
    }


}