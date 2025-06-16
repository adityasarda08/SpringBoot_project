package com.dataline.BajajPortal.services;

import com.dataline.BajajPortal.model.config.PdfExtractionPattern;
import com.dataline.BajajPortal.repository.PdfExtractionPatternRepository;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PdfReaderService {

    private static final Logger logger = LoggerFactory.getLogger(PdfReaderService.class);
    private static final Set<String> INVALID_INVOICE_NUMBER_KEYWORDS = Set.of(
            "invoice", "number", "no", "num", "inv", "document",
            "date", "po", "order", "bill", "statement", "page",
            "ref", "reference", "tax", "gst"
    );

    @Autowired
    private PdfExtractionPatternRepository patternRepository;

    // Helper method to decode QR from a BufferedImage
    private String decodeQrFromBufferedImage(BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            return null;
        }
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new QRCodeReader().decode(bitmap);
            String decodedText = result.getText(); // Store it
            logger.info("Successfully decoded QR. Raw content: [{}]", decodedText); // Log it
            return decodedText;
        } catch (NotFoundException e) {
            logger.info("No QR code found in a specific image processed by decodeQrFromBufferedImage."); // More visible for debugging
        } catch (ChecksumException | FormatException e) {
            logger.warn("QR code found but could not be decoded (checksum/format error).", e);
        } catch (Exception e) {
            logger.error("Error decoding QR code from image.", e);
        }
        return null;
    }


    // Helper method to extract and decode QR codes from a single PDF page
    private List<String> extractQrContentsFromPage(PDPage page) {
        List<String> qrContents = new ArrayList<>();
        PDResources resources = page.getResources();
        if (resources == null) {
            logger.info("Page has no resources, cannot extract QR codes.");
            return qrContents;
        }

        int imageXObjectCount = 0; // Counter for images
        for (COSName xObjectName : resources.getXObjectNames()) {
            PDXObject xObject;
            try {
                xObject = resources.getXObject(xObjectName);
            } catch (IOException e) {
                logger.error("Error getting XObject {} from page resources.", xObjectName.getName(), e);
                continue;
            }

            if (xObject instanceof PDImageXObject) {
                imageXObjectCount++; // Increment counter
                PDImageXObject imageObject = (PDImageXObject) xObject;
                try {
                    logger.info("Processing image XObject: {} for QR decoding.", xObjectName.getName());
                    BufferedImage bufferedImage = imageObject.getImage();
                    if (bufferedImage != null) {
                        String decodedText = decodeQrFromBufferedImage(bufferedImage);
                        if (decodedText != null && !decodedText.isEmpty()) {
                            qrContents.add(decodedText);
                            // logger.info("Successfully decoded QR code content from page."); // Already logged in decodeQrFromBufferedImage
                        }
                    } else {
                        logger.warn("Image XObject {} produced a null BufferedImage.", xObjectName.getName());
                    }
                } catch (IOException e) {
                    logger.error("Error processing image {} for QR decoding.", xObjectName.getName(), e);
                }
            }
        }
        logger.info("Processed {} image XObjects on this page for QR codes. Found {} QR contents.", imageXObjectCount, qrContents.size());
        return qrContents;
    }


    public Map<String, Object> processAndExtractData(MultipartFile file) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            result.put("error", "ERROR: Please upload a valid PDF file.");
            logger.warn("Invalid file type uploaded: {}", originalFilename);
            return result;
        }

        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            List<PageData> pageDataList = extractPageWiseData(document);
            Map<String, List<PageData>> invoiceGroups = groupPagesByInvoice(pageDataList);

            result.put("fileName", originalFilename);
            result.put("pageCount", document.getNumberOfPages());

            if (invoiceGroups.size() <= 1 || (invoiceGroups.size() == 1 && invoiceGroups.containsKey("default"))) {
                if (invoiceGroups.containsKey("default") && invoiceGroups.get("default").size() == pageDataList.size()) {
                    logger.info("All pages grouped under 'default'. Processing as a single document.");
                }
                return processSingleInvoice(result, pageDataList);
            } else {
                return processMultipleInvoices(result, invoiceGroups);
            }

        } catch (Exception e) {
            logger.error("Error processing PDF: {}", e.getMessage(), e);
            result.put("error", "Error processing PDF: " + e.getMessage());
            result.put("success", false);
            return result;
        }
    }

    private List<PageData> extractPageWiseData(PDDocument document) throws IOException {
        List<PageData> pageDataList = new ArrayList<>();
        PDFTextStripper textStripper = new PDFTextStripper();
        textStripper.setSortByPosition(true);

        for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
            textStripper.setStartPage(pageNum + 1);
            textStripper.setEndPage(pageNum + 1);
            String pageText = textStripper.getText(document);
            String[] lines = pageText.split("\\R");

            // --- BEGIN: Added logging for page content ---
            logger.info("--- Extracted Text for Page: {} ---", pageNum + 1);
            if (lines.length == 0) {
                logger.info("No text lines extracted for page {}.", pageNum + 1);
            } else {
                for (int i = 0; i < lines.length; i++) {
                    logger.info("Page {} | Line {}: {}", pageNum + 1, i + 1, lines[i]);
                }
            }
            logger.info("--- End of Text for Page: {} ---", pageNum + 1);
            // --- END: Added logging for page content ---

            // Extract QR codes from the current page
            PDPage pdPage = document.getPage(pageNum);
            List<String> qrContentsOnPage = extractQrContentsFromPage(pdPage);

            pageDataList.add(new PageData(pageNum + 1, lines, qrContentsOnPage));
        }
        return pageDataList;
    }

    private boolean isLikelyInvalidInvoiceNumber(String extractedInvoiceNumber) {
        if (extractedInvoiceNumber == null || extractedInvoiceNumber.trim().isEmpty()) {
            return false;
        }
        String trimmedLower = extractedInvoiceNumber.trim().toLowerCase();
        return INVALID_INVOICE_NUMBER_KEYWORDS.contains(trimmedLower);
    }

    private Map<String, List<PageData>> groupPagesByInvoice(List<PageData> pageDataList) {
        Map<String, List<PageData>> invoiceGroups = new LinkedHashMap<>();
        List<PdfExtractionPattern> allPatterns = patternRepository.findAll();
        String lastValidInvoiceNumber = null;

        for (PageData pageData : pageDataList) {
            String invoiceNumber = extractInvoiceNumberFromPage(pageData, allPatterns);
            if (isLikelyInvalidInvoiceNumber(invoiceNumber)) {
                logger.warn("Extracted invoice number '{}' from page {} is considered invalid.", invoiceNumber, pageData.getPageNumber());
                invoiceNumber = null;
            }

            if (invoiceNumber != null && !invoiceNumber.trim().isEmpty()) {
                invoiceGroups.computeIfAbsent(invoiceNumber, k -> new ArrayList<>()).add(pageData);
                lastValidInvoiceNumber = invoiceNumber;
            } else {
                if (lastValidInvoiceNumber != null) {
                    invoiceGroups.get(lastValidInvoiceNumber).add(pageData);
                } else {
                    invoiceGroups.computeIfAbsent("default", k -> new ArrayList<>()).add(pageData);
                }
            }
        }
        if (invoiceGroups.size() > 1 && invoiceGroups.containsKey("default")) {
            logger.info("Multiple invoice groups found; 'default' group will be ignored for multi-invoice processing if other specific invoices exist.");
        }
        return invoiceGroups;
    }

    private String extractInvoiceNumberFromPage(PageData pageData, List<PdfExtractionPattern> allPatterns) {
        for (PdfExtractionPattern pattern : allPatterns) {
            if (pattern.getVendorCode() != null && !pattern.getVendorCode().trim().isEmpty() &&
                    containsVendorCode(pageData.getLines(), pattern.getVendorCode())) {
                String potentialInvoiceNumber = extractField(pageData.getLines(), pattern.getInvoiceNumber(), pattern.getInvoiceNumberTargetLineNumber());
                if (potentialInvoiceNumber != null && !potentialInvoiceNumber.trim().isEmpty()) {
                    return potentialInvoiceNumber;
                }
            }
        }
        return null;
    }

    private Map<String, Object> processSingleInvoice(Map<String, Object> result, List<PageData> pageDataList) {
        // String[] allLines = combineLines(pageDataList); // No longer needed here if extractDataUsingPattern takes List<PageData>
        // result.put("rawText", allLines); // rawText will be part of invoiceData now

        PdfExtractionPattern matchingPattern = findMatchingPatternFromPages(pageDataList);
        if (matchingPattern != null) {
            Map<String, Object> invoiceData = extractDataUsingPattern(pageDataList, matchingPattern); // Changed to return Map<String, Object>
            result.putAll(invoiceData); // Merge extracted data into the main result
            result.put("patternUsed", matchingPattern.getVendorCode());
            result.put("success", invoiceData.getOrDefault("success", true)); // Use success from invoiceData
            result.put("invoiceType", "single");
            // textFormat is now inside invoiceData
        } else {
            result.put("error", "No matching extraction pattern found for this document (single processing).");
            result.put("success", false);
            result.put("invoiceType", "single_error");
            result.put("rawText", combineLines(pageDataList)); // Add raw text even on error
        }
        return result;
    }

    private Map<String, Object> processMultipleInvoices(Map<String, Object> result, Map<String, List<PageData>> invoiceGroups) {
        List<Map<String, Object>> invoicesDataList = new ArrayList<>();
        List<String> allTextFormats = new ArrayList<>();

        for (Map.Entry<String, List<PageData>> entry : invoiceGroups.entrySet()) {
            String invoiceNumberKey = entry.getKey();
            List<PageData> currentInvoicePages = entry.getValue();

            if ("default".equals(invoiceNumberKey) && invoiceGroups.size() > 1) {
                logger.info("Skipping 'default' group in multi-invoice processing.");
                continue;
            }

            PdfExtractionPattern matchingPattern = findMatchingPatternFromPages(currentInvoicePages);
            Map<String, Object> individualInvoiceResult = new LinkedHashMap<>(); // For this specific invoice

            if (matchingPattern != null) {
                Map<String, Object> extractedInvoiceData = extractDataUsingPattern(currentInvoicePages, matchingPattern);

                String actualInvoiceNumber = ((Map<String, String>) extractedInvoiceData.getOrDefault("extractedData", new HashMap<>()))
                        .getOrDefault("invoiceNumber", invoiceNumberKey);
                if (isLikelyInvalidInvoiceNumber(actualInvoiceNumber) && !"default".equals(invoiceNumberKey)) {
                    logger.warn("Pattern {} applied to group '{}' still resulted in an invalid invoice number '{}'.",
                            matchingPattern.getVendorCode(), invoiceNumberKey, actualInvoiceNumber);
                }

                individualInvoiceResult.put("invoiceNumber", actualInvoiceNumber);
                individualInvoiceResult.put("pages", currentInvoicePages.stream().map(PageData::getPageNumber).collect(Collectors.toList()));
                individualInvoiceResult.putAll(extractedInvoiceData); // This includes "extractedData", "rawText", "textFormat", "success"
                individualInvoiceResult.put("patternUsed", matchingPattern.getVendorCode());
                // "success" is already in extractedInvoiceData

                if ((Boolean) extractedInvoiceData.getOrDefault("success", false) && extractedInvoiceData.containsKey("textFormat")) {
                    allTextFormats.add((String) extractedInvoiceData.get("textFormat"));
                }
                invoicesDataList.add(individualInvoiceResult);
            } else {
                logger.warn("No matching pattern found for invoice group key '{}' (pages: {}). This group will be skipped.",
                        invoiceNumberKey, currentInvoicePages.stream().map(PageData::getPageNumber).collect(Collectors.toList()));

                Map<String, Object> errorInvoiceData = new LinkedHashMap<>();
                errorInvoiceData.put("invoiceNumber", invoiceNumberKey);
                errorInvoiceData.put("pages", currentInvoicePages.stream().map(PageData::getPageNumber).collect(Collectors.toList()));
                errorInvoiceData.put("error", "No matching extraction pattern found for this invoice group.");
                errorInvoiceData.put("success", false);
                errorInvoiceData.put("rawText", combineLines(currentInvoicePages));
                invoicesDataList.add(errorInvoiceData);
            }
        }

        result.put("multipleInvoices", invoicesDataList);
        result.put("totalInvoicesProcessed", invoicesDataList.stream().filter(inv -> (Boolean) inv.getOrDefault("success", false)).count());
        result.put("totalInvoiceGroupsAttempted", invoiceGroups.size());
        result.put("success", !invoicesDataList.isEmpty() && invoicesDataList.stream().anyMatch(inv -> (Boolean) inv.getOrDefault("success", false)));
        result.put("invoiceType", "multiple");
        result.put("textFormats", allTextFormats);

        // Backward compatibility for top-level fields (extractedData, patternUsed, rawText, textFormat)
        invoicesDataList.stream()
                .filter(inv -> (Boolean) inv.getOrDefault("success", false))
                .findFirst()
                .ifPresent(firstInv -> {
                    result.put("extractedData", firstInv.get("extractedData"));
                    result.put("patternUsed", firstInv.get("patternUsed"));
                    result.put("rawText", firstInv.get("rawText"));
                    result.put("textFormat", firstInv.get("textFormat"));
                });
        return result;
    }


    private String generateTextFormat(Map<String, String> extractedData) {
        final String[] FIELD_ORDER = {
                "vendorPlantCode", "vendorCode", "gstinNo", "hsnCode", "sacCode", "itemCode", "poNo", "lineNo",
                "invoiceNumber", "invoiceDate", "invoiceQuantity", "basicRate", "basicValue", "freight",
                "pfCharges", "otherCharges", "subTotal", "additionalTaxValue", "taxBaseValue", "cgstPercent",
                "cgstAmt", "sgstPercent", "sgstAmt", "utgstPercent", "utgstAmt", "igstPercent", "igstAmt",
                "totalAmt", "eWayBillNo", "eWayBillDate", "vehicleNo", "billToShipToCode", "remarks",
                "signedQrCode", "deliveryChallanNumber", "deliveryChallanDate", "deliveryChallanAmt",
                "tcsValue", "ediNumber"
        };
        return Arrays.stream(FIELD_ORDER)
                .map(field -> extractedData.getOrDefault(field, "0").trim())
                .collect(Collectors.joining(","));
    }

    private String[] combineLines(List<PageData> pageDataList) {
        return pageDataList.stream()
                .flatMap(page -> Arrays.stream(page.getLines()))
                .toArray(String[]::new);
    }

    private boolean containsVendorCode(String[] lines, String vendorCode) {
        if (vendorCode == null || vendorCode.trim().isEmpty()) return false;
        String trimmedVendorCode = vendorCode.trim();
        return Arrays.stream(lines).anyMatch(line -> line != null && line.contains(trimmedVendorCode));
    }

    // Modified to take List<PageData> to find pattern based on combined text
    private PdfExtractionPattern findMatchingPatternFromPages(List<PageData> pageDataList) {
        String[] combinedLines = combineLines(pageDataList);
        return findMatchingPattern(combinedLines); // Reuse existing logic
    }

    private PdfExtractionPattern findMatchingPattern(String[] lines) { // Kept for internal use by findMatchingPatternFromPages
        List<PdfExtractionPattern> allPatterns = patternRepository.findAll();
        for (PdfExtractionPattern pattern : allPatterns) {
            String vendorCode = pattern.getVendorCode();
            if (vendorCode != null && !vendorCode.trim().isEmpty() && containsVendorCode(lines, vendorCode)) {
                logger.info("Found matching pattern with vendor code '{}'", vendorCode);
                return pattern;
            }
        }
        logger.warn("No matching vendor pattern found in the provided lines.");
        return null;
    }

    // Modified to take List<PageData> instead of String[] lines
    // Returns a map that includes extractedData, rawText, textFormat, and success status
    private Map<String, Object> extractDataUsingPattern(List<PageData> invoicePages, PdfExtractionPattern pattern) {
        Map<String, Object> resultData = new LinkedHashMap<>();
        Map<String, String> extractedFields = new LinkedHashMap<>();
        String[] combinedLinesForText = combineLines(invoicePages);

        // Extract other fields as before
        extractedFields.put("vendorCode", extractField(combinedLinesForText, pattern.getVendorCode(), pattern.getVendorCodeTargetLineNumber()));
        extractedFields.put("invoiceNumber", extractField(combinedLinesForText, pattern.getInvoiceNumber(), pattern.getInvoiceNumberTargetLineNumber()));
        // ... other field extractions ...

        // --- BEGIN: Special handling for invoiceDate formatting ---
        String originalInvoiceDateStr = extractField(combinedLinesForText, pattern.getInvoiceDate(), pattern.getInvoiceDateTargetLineNumber());
        if (originalInvoiceDateStr != null && !originalInvoiceDateStr.trim().isEmpty()) {
            String trimmedDate = originalInvoiceDateStr.trim();
            try {
                // Input formatter for "dd MMM yyyy" (e.g., "05 JUN 2025")
                // Using DateTimeFormatterBuilder for case-insensitive month parsing
                DateTimeFormatter inputFormatter = new DateTimeFormatterBuilder()
                        .parseCaseInsensitive() // Handles "JUN", "Jun", "jun"
                        .appendPattern("dd MMM yyyy")
                        .toFormatter(Locale.ENGLISH); // Crucial for English month names

                LocalDate date = LocalDate.parse(trimmedDate, inputFormatter);

                // Output formatter for "yyyyMMdd"
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                String formattedDate = date.format(outputFormatter);
                extractedFields.put("invoiceDate", formattedDate);
                logger.info("Successfully formatted invoiceDate from '{}' to '{}'", trimmedDate, formattedDate);
            } catch (DateTimeParseException e) {
                logger.warn("Could not parse invoiceDate '{}' from format 'dd MMM yyyy'. Keeping original value. Error: {}", trimmedDate, e.getMessage());
                extractedFields.put("invoiceDate", trimmedDate); // Keep original if parsing fails
            }
        } else {
            extractedFields.put("invoiceDate", originalInvoiceDateStr); // Handles null or already empty strings
        }
        // --- END: Special handling for invoiceDate formatting ---

        extractedFields.put("vendorPlantCode", extractField(combinedLinesForText, pattern.getVendorPlantCode(), pattern.getVendorPlantCodeTargetLineNumber()));
        extractedFields.put("gstinNo", extractField(combinedLinesForText, pattern.getGstinNo(), pattern.getGstinNoTargetLineNumber()));
        // ... continue with all other field extractions as they were ...
        extractedFields.put("hsnCode", extractField(combinedLinesForText, pattern.getHsnCode(), pattern.getHsnCodeTargetLineNumber()));
        extractedFields.put("sacCode", extractField(combinedLinesForText, pattern.getSacCode(), pattern.getSacCodeTargetLineNumber()));
        extractedFields.put("itemCode", extractField(combinedLinesForText, pattern.getItemCode(), pattern.getItemCodeTargetLineNumber()));
        extractedFields.put("poNo", extractField(combinedLinesForText, pattern.getPoNo(), pattern.getPoNoTargetLineNumber()));
        extractedFields.put("lineNo", extractField(combinedLinesForText, pattern.getLineNo(), pattern.getLineNoTargetLineNumber()));
        extractedFields.put("invoiceQuantity", extractField(combinedLinesForText, pattern.getInvoiceQuantity(), pattern.getInvoiceQuantityTargetLineNumber()));
        extractedFields.put("basicRate", extractField(combinedLinesForText, pattern.getBasicRate(), pattern.getBasicRateTargetLineNumber()));
        extractedFields.put("basicValue", extractField(combinedLinesForText, pattern.getBasicValue(), pattern.getBasicValueTargetLineNumber()));
        extractedFields.put("freight", extractField(combinedLinesForText, pattern.getFreight(), pattern.getFreightTargetLineNumber()));
        extractedFields.put("pfCharges", extractField(combinedLinesForText, pattern.getPfCharges(), pattern.getPfChargesTargetLineNumber()));
        extractedFields.put("otherCharges", extractField(combinedLinesForText, pattern.getOtherCharges(), pattern.getOtherChargesTargetLineNumber()));
        extractedFields.put("subTotal", extractField(combinedLinesForText, pattern.getSubTotal(), pattern.getSubTotalTargetLineNumber()));
        extractedFields.put("additionalTaxValue", extractField(combinedLinesForText, pattern.getAdditionalTaxValue(), pattern.getAdditionalTaxValueTargetLineNumber()));
        extractedFields.put("taxBaseValue", extractField(combinedLinesForText, pattern.getTaxBaseValue(), pattern.getTaxBaseValueTargetLineNumber()));
        extractedFields.put("cgstPercent", extractField(combinedLinesForText, pattern.getCgstPercent(), pattern.getCgstPercentTargetLineNumber()));
        extractedFields.put("cgstAmt", extractField(combinedLinesForText, pattern.getCgstAmt(), pattern.getCgstAmtTargetLineNumber()));
        extractedFields.put("sgstPercent", extractField(combinedLinesForText, pattern.getSgstPercent(), pattern.getSgstPercentTargetLineNumber()));
        extractedFields.put("sgstAmt", extractField(combinedLinesForText, pattern.getSgstAmt(), pattern.getSgstAmtTargetLineNumber()));
        extractedFields.put("utgstPercent", extractField(combinedLinesForText, pattern.getUtgstPercent(), pattern.getUtgstPercentTargetLineNumber()));
        extractedFields.put("utgstAmt", extractField(combinedLinesForText, pattern.getUtgstAmt(), pattern.getUtgstAmtTargetLineNumber()));
        extractedFields.put("igstPercent", extractField(combinedLinesForText, pattern.getIgstPercent(), pattern.getIgstPercentTargetLineNumber()));
        extractedFields.put("igstAmt", extractField(combinedLinesForText, pattern.getIgstAmt(), pattern.getIgstAmtTargetLineNumber()));
        extractedFields.put("totalAmt", extractField(combinedLinesForText, pattern.getTotalAmt(), pattern.getTotalAmtTargetLineNumber()));
        extractedFields.put("eWayBillNo", extractField(combinedLinesForText, pattern.getEWayBillNo(), pattern.getEWayBillNoTargetLineNumber()));
        extractedFields.put("eWayBillDate", extractField(combinedLinesForText, pattern.getEWayBillDate(), pattern.getEWayBillDateTargetLineNumber()));
        extractedFields.put("vehicleNo", extractField(combinedLinesForText, pattern.getVehicleNo(), pattern.getVehicleNoTargetLineNumber()));
        extractedFields.put("billToShipToCode", extractField(combinedLinesForText, pattern.getBillToShipToCode(), pattern.getBillToShipToCodeTargetLineNumber()));
        extractedFields.put("remarks", extractField(combinedLinesForText, pattern.getRemarks(), pattern.getRemarksTargetLineNumber()));
        extractedFields.put("deliveryChallanNumber", extractField(combinedLinesForText, pattern.getDeliveryChallanNumber(), pattern.getDeliveryChallanNumberTargetLineNumber()));
        extractedFields.put("deliveryChallanDate", extractField(combinedLinesForText, pattern.getDeliveryChallanDate(), pattern.getDeliveryChallanDateTargetLineNumber()));
        extractedFields.put("deliveryChallanAmt", extractField(combinedLinesForText, pattern.getDeliveryChallanAmt(), pattern.getDeliveryChallanAmtTargetLineNumber()));
        extractedFields.put("tcsValue", extractField(combinedLinesForText, pattern.getTcsValue(), pattern.getTcsValueTargetLineNumber()));
        extractedFields.put("ediNumber", extractField(combinedLinesForText, pattern.getEdiNumber(), pattern.getEdiNumberTargetLineNumber()));

        // --- Special handling for signedQrCode ---
        List<String> allQrContentsForInvoice = invoicePages.stream()
                .flatMap(pd -> pd.getQrCodeContents().stream())
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        String signedQrCodeValue = null;
        if (!allQrContentsForInvoice.isEmpty()) {
            logger.info("Attempting to extract signed QR code. All decoded QR contents for this invoice: {}", allQrContentsForInvoice);
            String qrRegexFromDb = pattern.getSignedQrCode();
            logger.info("Regex for signedQrCode from DB: [{}]", qrRegexFromDb);

            if (qrRegexFromDb != null && !qrRegexFromDb.trim().isEmpty()) {
                signedQrCodeValue = extractField(
                        allQrContentsForInvoice.toArray(new String[0]),
                        qrRegexFromDb,
                        null
                );
                logger.info("Result of extractField for signedQrCode: [{}]", signedQrCodeValue);
            } else {
                logger.warn("Signed QR Code regex is null or empty in the database pattern. Skipping QR extraction for this field.");
            }
        } else {
            logger.info("No decoded QR contents found for this invoice to attempt signedQrCode extraction.");
        }
        extractedFields.put("signedQrCode", signedQrCodeValue); // Use the extracted value
        // --- End special handling for signedQrCode ---

        extractedFields.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().trim().isEmpty());

        resultData.put("extractedData", extractedFields);
        resultData.put("rawText", combinedLinesForText);
        resultData.put("textFormat", generateTextFormat(extractedFields));
        resultData.put("success", true);

        return resultData;
    }


    private String extractField(String[] linesToSearch, String fieldPattern, String targetLineNumberStr) {
        if (fieldPattern == null || fieldPattern.trim().isEmpty()) {
            return null;
        }
        // ... (rest of extractField, extractFromTargetLine, extractFromAllLines, extractUsingPattern are unchanged)
        Integer targetLineNum = null;
        if (targetLineNumberStr != null && !targetLineNumberStr.trim().isEmpty()) {
            try {
                targetLineNum = Integer.parseInt(targetLineNumberStr.trim());
            } catch (NumberFormatException e) {
                logger.warn("Invalid target line number '{}' for pattern '{}'. Will search relevant lines or all lines.", targetLineNumberStr, fieldPattern);
            }
        }

        if (targetLineNum != null) {
            return extractFromTargetLine(linesToSearch, fieldPattern, targetLineNum - 1);
        } else {
            return extractFromAllLines(linesToSearch, fieldPattern);
        }
    }

    private String extractFromTargetLine(String[] lines, String fieldPattern, int targetLineIndex) {
        int startLine = Math.max(0, targetLineIndex - 2);
        int endLine = Math.min(lines.length - 1, targetLineIndex + 2);

        for (int i = startLine; i <= endLine; i++) {
            if (lines[i] == null) continue;
            String result = extractUsingPattern(lines[i], fieldPattern);
            if (result != null) return result;
        }
        if (targetLineIndex >= 0 && targetLineIndex < lines.length && lines[targetLineIndex] != null) {
            String result = extractUsingPattern(lines[targetLineIndex], fieldPattern);
            if (result != null) return result;
        }
        return null;
    }

    private String extractFromAllLines(String[] lines, String fieldPattern) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i] == null) continue;
            String result = extractUsingPattern(lines[i], fieldPattern);
            if (result != null) return result;
        }
        return null;
    }

    private String extractUsingPattern(String line, String fieldPattern) {
        if (line == null || fieldPattern == null || fieldPattern.trim().isEmpty()) {
            return null;
        }
        try {
            Pattern regexPattern = Pattern.compile(fieldPattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = regexPattern.matcher(line);
            if (matcher.find()) {
                String extractedValue = (matcher.groupCount() > 0) ? matcher.group(1) : matcher.group(0);
                return (extractedValue != null) ? extractedValue.trim() : null;
            }
        } catch (java.util.regex.PatternSyntaxException e) {
            logger.warn("Invalid regex syntax for pattern '{}': {}", fieldPattern, e.getMessage());
            // Simplified fallback, consider removing if too naive
            if (line.toLowerCase().contains(fieldPattern.toLowerCase())) {
                int index = line.toLowerCase().indexOf(fieldPattern.toLowerCase());
                String afterPattern = line.substring(index + fieldPattern.length()).trim();
                if (!afterPattern.isEmpty()) {
                    String[] parts = afterPattern.split("\\s+", 2);
                    if (parts.length > 0 && !parts[0].isEmpty()) return parts[0].trim();
                }
            }
        } catch (Exception e) {
            logger.error("Error during regex matching for pattern '{}' on line '{}': {}", fieldPattern, line, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Simple PageData class
     */
    private static class PageData {
        private final int pageNumber;
        private final String[] lines;
        private final List<String> qrCodeContents; // Added for QR data

        public PageData(int pageNumber, String[] lines, List<String> qrCodeContents) {
            this.pageNumber = pageNumber;
            this.lines = lines;
            this.qrCodeContents = (qrCodeContents != null) ? List.copyOf(qrCodeContents) : Collections.emptyList();
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public String[] getLines() {
            return Arrays.copyOf(lines, lines.length); // Return a copy
        }

        public List<String> getQrCodeContents() {
            return qrCodeContents; // Already immutable or a copy
        }

        @Override
        public String toString() {
            return "PageData{pageNumber=" + pageNumber +
                    ", lines=" + (lines != null ? lines.length : 0) +
                    ", qrCodes=" + (qrCodeContents != null ? qrCodeContents.size() : 0) + "}";
        }
    }
}