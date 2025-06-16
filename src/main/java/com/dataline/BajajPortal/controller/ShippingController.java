package com.dataline.BajajPortal.controller;

import com.dataline.BajajPortal.model.master.ShippingDetails;
import com.dataline.BajajPortal.model.master.ShippingListProjection;
import com.dataline.BajajPortal.services.ShippingDetailsService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import net.sf.jasperreports.engine.JRException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/shiftTo")
public class ShippingController {

    @Autowired
    private ShippingDetailsService shippingService;

    // List all shippings
    @GetMapping("/list")
    public String listShippings(Model model, HttpSession session) {
        String companyCode = (String) session.getAttribute("companyCode");
        // Get only vendors for the logged-in company
        List<ShippingListProjection> shipping = shippingService.getShippingListByCompanyCode(companyCode);
        model.addAttribute("shipping", shipping);
        model.addAttribute("companyCode", companyCode);

        return "Shipping/ShippingList";
    }

    // Show form to add a new shipping
    @GetMapping("/new")
    public String showAddForm(Model model, HttpSession session) {
//        String companyCode = (String) session.getAttribute("companyCode");
        ShippingDetails shipping = new ShippingDetails();
//        shipping.setCompanyCode(companyCode);
        model.addAttribute("shipping", shipping);
        return "Shipping/ShippingMaster";
    }

    // Save a new or updated shipping
    @PostMapping("/save")
    public String saveShipping(@Valid @ModelAttribute("shipping") ShippingDetails shipping, BindingResult result,
                               Model model, HttpSession session) {
        try {
            String companyCode = (String) session.getAttribute("companyCode");
            shipping.setCompanyCode(companyCode); // Set company code before saving

            if (result.hasErrors()) {
                return "Shipping/ShippingMaster"; // Return to the form if validation fails
            }

            shippingService.addShippingDetails(shipping);
            return "redirect:/shiftTo/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error saving shipping: " + e.getMessage());
            return "Shipping/ShippingMaster";
        }
    }

    // Show form to edit an existing shipping
    @GetMapping("/edit/{shiftVendorCode}/{companyCode}")
    public String showEditForm(@PathVariable("shiftVendorCode") String shiftVendorCode,
                               @PathVariable("companyCode") String companyCode, Model model) {
        ShippingDetails shipping = shippingService.getShippingDetailsByVendorCodeAndCompanyCode(shiftVendorCode,
                companyCode);
        if (shipping == null) {
            return "redirect:/shiftTo/list"; // Redirect if shipping not found
        } else {

            model.addAttribute("shipping", shipping);
            return "Shipping/ShippingMaster"; // Points to the form for editing
        }

    }

    @GetMapping("/delete/{shiftVendorCode}")
    public String deleteShipping(@PathVariable String shiftVendorCode, RedirectAttributes redirectAttributes,
                                 HttpSession session) {
        try {
            shippingService.deleteShippingDetails(shiftVendorCode);
            redirectAttributes.addFlashAttribute("success", "Shipping details deleted successfully!");
            String companyCode = (String) session.getAttribute("companyCode");
            List<ShippingDetails> shipping = shippingService.getbycompanycode(companyCode);
            if (shipping.isEmpty()) {
                return "redirect:/shiftTo/new"; // Redirect to the form page if no items left
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting shipping details: " + e.getMessage());
        }
        return "redirect:/shiftTo/list";
    }

    //reports
    @GetMapping("/ShippingMasterReport")
    @ResponseBody
    public ResponseEntity<InputStreamResource> getshippingMasterReport(Model model, HttpSession session) throws JRException, FileNotFoundException, DocumentException {
        String companyCode = (String) session.getAttribute("companyCode");
        String fileName = shippingService.getshippingListforreport(companyCode);


        if (ShippingDetailsService.isValidFilePath(fileName)) {
            if (fileName != null) {
                File file = new File(fileName);
                HttpHeaders headers = new HttpHeaders();
                headers.add("content-disposition", "inline;filename=" + fileName);
                InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
                return ResponseEntity.ok().headers(headers).contentLength(file.length())
                        .contentType(MediaType.parseMediaType("application/pdf")).body(resource);
            } else {
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
                return ResponseEntity.ok().headers(headers).contentLength(blankFile.length()).contentType(MediaType.parseMediaType("application/pdf")).body(resource);
            }
        } else {
            Document document = new Document();
            String blankPdfFileName = "blank.pdf";
            PdfWriter.getInstance(document, new FileOutputStream(blankPdfFileName));
            document.open();
            // Write some message to the PDF file
            document.add(new Paragraph(fileName));
            document.close();
            File blankFile = new File(blankPdfFileName);
            HttpHeaders headers = new HttpHeaders();
            headers.add("content-disposition", "inline;filename=" + blankPdfFileName);
            InputStreamResource resource = new InputStreamResource(new FileInputStream(blankFile));
            return ResponseEntity.ok().headers(headers).contentLength(blankFile.length()).contentType(MediaType.parseMediaType("application/pdf")).body(resource);
        }
    }


}
