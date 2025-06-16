package com.dataline.BajajPortal.controller;

import com.dataline.BajajPortal.services.SimplePdfToEdiService; // Adjust import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
// HttpHeaders, HttpStatus, MediaType, ResponseEntity are not needed here if we always return a view name
// and handle download via a separate mechanism or a link on the page.
// However, if you wanted a *separate* endpoint just for download, they would be.
// For now, we'll focus on displaying data on the page.
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // Keep for redirect error messages

import java.io.IOException;
// StandardCharsets is not directly used if we are not building ResponseEntity<byte[]> here
// import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/simple-pdf-edi") // New base path for this feature
public class SimplePdfToEdiController {

    private static final Logger logger = LoggerFactory.getLogger(SimplePdfToEdiController.class);

    private final SimplePdfToEdiService simplePdfToEdiService;

    @Autowired
    public SimplePdfToEdiController(SimplePdfToEdiService simplePdfToEdiService) {
        this.simplePdfToEdiService = simplePdfToEdiService;
    }

    @GetMapping("/upload")
    public String showUploadPage(Model model) {
        // This just shows the upload form.
        // Flash attributes from redirects (e.g., error messages) will be available in the model.
        // Add initialPage attribute if your HTML uses it to control display on first load
        if (!model.containsAttribute("initialPage")) { // Check if not already set by a redirect
            model.addAttribute("initialPage", true);
        }
        return "SimpleEdiConverter/simplePdfToEdiUpload"; // Path to your new HTML file
    }

    @PostMapping("/generate")
    public String generateEdiFromPdf(@RequestParam("file") MultipartFile file,
                                     RedirectAttributes redirectAttributes, Model model) {
        // Basic File Validation
        if (file.isEmpty()) {
            // Use RedirectAttributes for errors when redirecting
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a PDF file to upload.");
            // Redirect back to the upload page
            return "redirect:/simple-pdf-edi/upload";
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.toLowerCase().endsWith(".pdf"))) {
            String contentType = file.getContentType();
            if (!"application/pdf".equals(contentType)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid file type. Please upload a PDF file (.pdf).");
                return "redirect:/simple-pdf-edi/upload";
            }
        }

        try {
            String commaSeparatedText = simplePdfToEdiService.generateCommaSeparatedTextFromPdf(file);

            // Add the generated text and filename to the model to be displayed on the page
            model.addAttribute("generatedText", commaSeparatedText);
            model.addAttribute("originalFileName", originalFilename);
            model.addAttribute("successMessage", "PDF processed. Comma-separated text generated below.");
            model.addAttribute("initialPage", false); // No longer the initial page load

            // Return the view name to display the results on the same page
            return "SimpleEdiConverter/simplePdfToEdiUpload";

        } catch (IOException | IllegalArgumentException e) {
            logger.error("Error generating comma-separated text for file {}: {}",
                    (originalFilename != null ? originalFilename : "N/A"), e.getMessage());
            // Add error to model for display on the same page
            model.addAttribute("errorMessage", "Error processing PDF: " + e.getMessage());
            model.addAttribute("initialPage", false);
            return "SimpleEdiConverter/simplePdfToEdiUpload"; // Show error on the same page

        } catch (Exception e) {
            logger.error("Unexpected error generating comma-separated text for file {}: {}",
                    (originalFilename != null ? originalFilename : "N/A"), e.getMessage(), e);
            model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
            model.addAttribute("initialPage", false);
            return "SimpleEdiConverter/simplePdfToEdiUpload"; // Show error on the same page
        }
        // The helper showUploadPageWithError is no longer strictly needed if we always return the view name
        // and add attributes directly to the model.
    }

    // The helper method showUploadPageWithError can be removed if you handle errors by
    // directly adding to the model and returning the view name as shown above.
    // If you still want to use it for some specific cases or cleaner code, ensure it returns String.
    /*
    private String showUploadPageWithError(Model model, String errorMessage) {
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("initialPage", false); // Not initial page if error occurred
        // Ensure other model attributes for the page are reset or set appropriately
        if (!model.containsAttribute("generatedText")) model.addAttribute("generatedText", null);
        if (!model.containsAttribute("originalFileName")) model.addAttribute("originalFileName", null);
        if (!model.containsAttribute("successMessage")) model.addAttribute("successMessage", null);
        return "SimpleEdiConverter/simplePdfToEdiUpload";
    }
    */
}