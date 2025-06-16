package com.dataline.BajajPortal.controller;

import com.dataline.BajajPortal.services.PdfReaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List; // Import List
import java.util.Map;

@Controller
@RequestMapping("/invoice")
public class PdfReaderController {

    private static final Logger logger = LoggerFactory.getLogger(PdfReaderController.class);

    @Autowired
    private PdfReaderService pdfReaderService;

    @GetMapping("/pdf")
    public String showReadPdfPage(Model model) {
        // Initialize model attributes to prevent null issues on first load
        model.addAttribute("pdfData", null);
        model.addAttribute("errorMessage", null);
        model.addAttribute("successMessage", null);
        model.addAttribute("fileName", null);
        return "PDFconvert/uploadPdfInvoice";
    }

    @PostMapping("/upload")
    public String handlePdfUpload(@RequestParam("pdfFile") MultipartFile file, Model model) {
        if (file.isEmpty()) {
            model.addAttribute("errorMessage", "Please select a PDF file to upload.");
            return "PDFconvert/uploadPdfInvoice";
        }

        model.addAttribute("fileName", file.getOriginalFilename()); // Keep fileName at top level

        try {
            Map<String, Object> serviceResult = pdfReaderService.processAndExtractData(file);

            if (serviceResult.containsKey("error")) {
                model.addAttribute("errorMessage", serviceResult.get("error"));
                // Pass a minimal pdfData structure for error display consistency if needed
                Map<String, Object> errorPdfData = new LinkedHashMap<>();
                errorPdfData.put("fileName", file.getOriginalFilename());
                errorPdfData.put("ExtractionGlobalError", serviceResult.get("error"));
                model.addAttribute("pdfData", errorPdfData);

            } else if (Boolean.TRUE.equals(serviceResult.get("success"))) {
                model.addAttribute("pdfData", serviceResult); // Pass the whole serviceResult

                if ("multiple".equals(serviceResult.get("invoiceType"))) {
                    model.addAttribute("successMessage",
                            "PDF processed successfully. Multiple invoices found: " + serviceResult.get("totalInvoices"));
                    logger.info("Multiple invoices extracted from PDF: {}", file.getOriginalFilename());
                    List<Map<String, Object>> multipleInvoices = (List<Map<String, Object>>) serviceResult.get("multipleInvoices");
                    if (multipleInvoices != null) {
                        multipleInvoices.forEach(inv -> logger.info("Invoice Data: {}", inv.get("extractedData")));
                    }
                } else {
                    model.addAttribute("successMessage",
                            "PDF processed successfully using pattern: " + serviceResult.get("patternUsed"));
                    logger.info("Successfully extracted data from PDF: {}", file.getOriginalFilename());
                    logger.info("Pattern used: {}", serviceResult.get("patternUsed"));
                    logger.info("Extracted fields: {}", serviceResult.get("extractedData"));
                }
            } else {
                // This case might be redundant if success is always true when no error
                model.addAttribute("errorMessage", (String) serviceResult.getOrDefault("error", "Unknown processing issue."));
                model.addAttribute("pdfData", serviceResult); // Pass serviceResult even if not fully successful
            }

        } catch (IOException e) {
            logger.error("IOException while processing PDF file: {}", file.getOriginalFilename(), e);
            model.addAttribute("errorMessage", "Error reading PDF file: " + e.getMessage());
            addErrorPdfDataToModel(model, file.getOriginalFilename(), "Error reading PDF file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while processing PDF file: {}", file.getOriginalFilename(), e);
            model.addAttribute("errorMessage", "An unexpected error occurred during processing.");
            addErrorPdfDataToModel(model, file.getOriginalFilename(), "An unexpected error occurred: " + e.getMessage());
        }

        return "PDFconvert/uploadPdfInvoice";
    }

    private void addErrorPdfDataToModel(Model model, String fileName, String errorMessage) {
        Map<String, Object> errorResult = new LinkedHashMap<>();
        errorResult.put("fileName", fileName); // Include fileName for consistency
        errorResult.put("ExtractionGlobalError", errorMessage);
        // Ensure pdfData is not null to avoid template errors
        model.addAttribute("pdfData", errorResult);
    }
}