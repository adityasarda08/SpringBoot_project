package com.dataline.BajajPortal.services; // Adjust to your package structure

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class SimplePdfToEdiService {

    private static final Logger logger = LoggerFactory.getLogger(SimplePdfToEdiService.class);

    /**
     * Extracts all text from an uploaded PDF file and joins the lines with commas.
     *
     * @param pdfFile The uploaded PDF file.
     * @return A single string where lines from the PDF are joined by commas.
     * @throws IOException              If an error occurs during PDF reading or processing.
     * @throws IllegalArgumentException If the PDF file is null or empty.
     */
    public String generateCommaSeparatedTextFromPdf(MultipartFile pdfFile) throws IOException, IllegalArgumentException {
        if (pdfFile == null || pdfFile.isEmpty()) {
            logger.warn("Attempted to process a null or empty PDF file.");
            throw new IllegalArgumentException("PDF file cannot be null or empty.");
        }

        logger.info("Processing PDF file: {}", pdfFile.getOriginalFilename());

        try (InputStream inputStream = pdfFile.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            if (document.isEncrypted()) {
                logger.warn("Attempted to process an encrypted PDF: {}", pdfFile.getOriginalFilename());
                // For this simple service, we might choose not to handle encrypted PDFs.
                // Or, you could attempt decryption if a password mechanism exists.
                throw new IllegalArgumentException("Encrypted PDF files are not supported by this simple converter.");
            }

            PDFTextStripper pdfStripper = new PDFTextStripper();
            String fullText = pdfStripper.getText(document);

            if (fullText == null || fullText.trim().isEmpty()) {
                logger.info("No text content found in PDF: {}", pdfFile.getOriginalFilename());
                return ""; // Return empty string if no text
            }

            // Split the text into lines based on common line break characters
            // \\R matches any Unicode linebreak sequence
            String[] lines = fullText.split("\\R");

            // Join the lines with a comma
            // Filter out empty lines that might result from multiple consecutive line breaks
            String commaSeparatedText = Arrays.stream(lines)
                    .map(String::trim) // Trim each line
                    .filter(line -> !line.isEmpty()) // Remove empty lines
                    .collect(Collectors.joining(","));

            logger.info("Successfully generated comma-separated text for PDF: {}", pdfFile.getOriginalFilename());
            return commaSeparatedText;

        } catch (IOException e) {
            logger.error("IOException during PDF processing for file: {}", pdfFile.getOriginalFilename(), e);
            throw new IOException("Failed to read or parse the PDF file: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during PDF processing for file: {}", pdfFile.getOriginalFilename(), e);
            throw new IOException("An unexpected error occurred while processing the PDF: " + e.getMessage(), e);
        }
    }
}
