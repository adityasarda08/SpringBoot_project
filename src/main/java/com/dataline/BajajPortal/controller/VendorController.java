package com.dataline.BajajPortal.controller;

import com.dataline.BajajPortal.model.master.VendorDetails;

import com.dataline.BajajPortal.repository.VendorProjection;
import com.dataline.BajajPortal.services.VendorService;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vendors")
@RequiredArgsConstructor
public class VendorController {

    // Add a logger instance
//    private static final Logger logger = LoggerFactory.getLogger(VendorController.class); // <<< ADD THIS

    @Autowired
    private final VendorService vendorService;


    @GetMapping("/new")
    public String showVendorForm(Model model, HttpSession session) {
        VendorDetails VendorDetails = new VendorDetails();
        VendorDetails.set_id(null);  // Ensure new vendor doesn't have an ID
        model.addAttribute("VendorDetails", VendorDetails);
        return "vendor/vendorform";
    }

    @PostMapping("/save")
    public String addVendor(@ModelAttribute VendorDetails vendorDetails,
                            HttpSession session,  // Add this
                            Model model) {
        try {
            String companyCode = (String) session.getAttribute("companyCode");
            vendorDetails.setCompanyCode(companyCode); // Ensure company code is set
            vendorService.saveVendor(vendorDetails);
            return "redirect:/vendors/list";
        } catch (Exception e) {
            model.addAttribute("error", "Error saving vendor: " + e.getMessage());
            model.addAttribute("VendorDetails", vendorDetails);
            return "vendor/vendorform";
        }
    }

    @GetMapping("/list")
    public String listVendors(Model model, HttpSession session) {
        String companyCode = (String) session.getAttribute("companyCode");
        System.out.println("Company Code in list: " + companyCode);
        List<VendorDetails> VendorDetails = vendorService.getVendorsByCompanyCode(companyCode);
        System.out.println("Number of vendors found: " + VendorDetails.size()); // Debug line
        model.addAttribute("vendors", VendorDetails);
        model.addAttribute("companyCode", companyCode);
        return "vendor/vendorlist";
    }

    @GetMapping("/edit/{id}")
    public String showUpdateForm(@PathVariable String id, Model model) {
        try {
            VendorDetails VendorDetails = vendorService.getVendorById(id);
            model.addAttribute("VendorDetails", VendorDetails);
            return "vendor/vendorform";
        } catch (IllegalArgumentException e) {
            return "redirect:/vendors/list";
        }
    }

    @GetMapping("/delete/{vendorId}")
    public String deleteUser(@PathVariable String vendorId, RedirectAttributes redirectAttributes,
                             HttpSession session) {
        try {
            vendorService.deleteVendor(vendorId);
            redirectAttributes.addFlashAttribute("message", "vendor deleted successfully!");
            String companyCode = (String) session.getAttribute("companyCode");
            List<VendorDetails> VendorDetails = vendorService.getVendorsByCompanyCode(companyCode);
            if (VendorDetails.isEmpty()) {
                return "redirect:/vendors/form"; // Redirect to the form page if no items left
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting Item: " + e.getMessage());
        }
        return "redirect:/vendors/list";
    }

    @GetMapping("/vendorMasterReport")
    @ResponseBody
    public ResponseEntity<InputStreamResource> getvendorMasterReport(Model model, HttpSession session) throws JRException, FileNotFoundException, DocumentException {
        String companyCode = (String) session.getAttribute("companyCode");
        String fileName = vendorService.getvendorListforreport(companyCode);


        if (vendorService.isValidFilePath(fileName)) {
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


    @GetMapping("/searchlist")
    @ResponseBody
    public List<VendorProjection> getVendorListForSearch(Model model, HttpSession session) {
        String companyCode = (String) session.getAttribute("companyCode");
        return vendorService.getVendorProjectionsByCompany(companyCode);
    }

    @GetMapping("/getGstNo/{vendorCode}")
    @ResponseBody
    public String getVendorGSTNo(@PathVariable("vendorCode") String vendorCode, Model model, String companyCode) {
        return vendorService.getVendorGSTNo(vendorCode, companyCode);
    }

}
