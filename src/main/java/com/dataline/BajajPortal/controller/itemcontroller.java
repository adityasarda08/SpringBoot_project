package com.dataline.BajajPortal.controller;

import com.dataline.BajajPortal.model.master.ItemDetails;
import com.dataline.BajajPortal.repository.ItemmasterProjection;
import com.dataline.BajajPortal.services.ItemDetailsServices;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import net.sf.jasperreports.engine.JRException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
// import java.nio.file.Files;
// import java.nio.file.Paths;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/Item")
@RequiredArgsConstructor
public class itemcontroller {

    private final List<String> ITEM_UNITS = Arrays.asList(
            "BAG", "BAL", "BDL", "BKL", "BOU", "BOX", "BTL", "BUN",
            "CAN", "CBM", "CCM", "CMS", "CTN", "DOZ", "DRM", "GGK",
            "GMS", "GRS", "GYD", "KGS", "KLR", "KME", "LTR", "MLT",
            "MTR", "MTS", "NOS", "OTH", "PAC", "PCS", "PRS", "QTL",
            "ROL", "SET", "SQF", "SQM", "SQY", "TBS", "TGM", "THD",
            "TON", "TUB", "UGS", "UNT", "YDS");

    private final ItemDetailsServices ItemService;

    @GetMapping("/new")
    public String showItemsForm(Model model, HttpSession session) {
        try {
            // String companyCode = (String) session.getAttribute("companyCode");
            // if (companyCode == null || companyCode.trim().isEmpty()) {
            // throw new RuntimeException("Company code not found in session");
            // }

            // Create a completely new ItemDetails object
            ItemDetails itemDetails = new ItemDetails();
            // itemDetails.setCompanyCode(companyCode);
            // Explicitly set ID to null for new items
            itemDetails.setId(null);

            System.out.println("Creating new form with empty item: " + itemDetails); // Debug log

            model.addAttribute("ItemDetails", itemDetails);
            model.addAttribute("isEdit", false);
            model.addAttribute("itemUnits", ITEM_UNITS); // Add this line
            return "Items/Itemdetails";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading form: " + e.getMessage());
            return "Items/Itemdetails";
        }
    }

    @GetMapping("/list")
    public String listItems(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            String companyCode = (String) session.getAttribute("companyCode");
            if (companyCode == null || companyCode.trim().isEmpty()) {
                throw new RuntimeException("Company code not found in session");
            }

            List<ItemDetails> itemDetails = ItemService.getItemsByCompanyCode(companyCode);
            model.addAttribute("items", itemDetails);
            model.addAttribute("companyCode", companyCode);

            return "Items/itemslist";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error loading items: " + e.getMessage());
            return "redirect:/Item/items";
        }
    }

    @PostMapping("/details")
    public String addItem(@ModelAttribute ItemDetails itemDetails,
                          Model model,
                          RedirectAttributes redirectAttributes,
                          @RequestParam(value = "isEdit", required = false) Boolean isEdit) {

        try {
            itemDetails.setCompanyCode(model.getAttribute("companyCode").toString());
            System.out.println("Received form submission. IsEdit: " + isEdit); // Debug log
            System.out.println("Item details: " + itemDetails); // Debug log

            // If it's not an edit operation, ensure ID is null
            if (isEdit == null || !isEdit) {
                itemDetails.setId(null);
            }

            ItemService.saveItems(itemDetails);
            redirectAttributes.addFlashAttribute("success",
                    (isEdit != null && isEdit) ? "Item updated successfully!" : "Item added successfully!");
            return "redirect:/Item/list";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("ItemDetails", itemDetails);
            model.addAttribute("isEdit", isEdit != null && isEdit);
            return "Items/Itemdetails";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id,
                               Model model,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            String companyCode = (String) session.getAttribute("companyCode");
            if (companyCode == null || companyCode.trim().isEmpty()) {
                throw new RuntimeException("Company code not found in session");
            }

            ItemDetails itemDetails = ItemService.getItemById(id);
            if (itemDetails == null || !itemDetails.getCompanyCode().equals(companyCode)) {
                throw new RuntimeException("Item not found or does not belong to this company");
            }

            model.addAttribute("ItemDetails", itemDetails);
            model.addAttribute("isEdit", true);
            model.addAttribute("itemUnits", ITEM_UNITS); // Add this line

            return "Items/Itemdetails";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error loading item: " + e.getMessage());
            return "redirect:/Item/list";
        }
    }

    @GetMapping("/delete/{itemCode}")
    public String deleteItem(@PathVariable String itemCode,
                             RedirectAttributes redirectAttributes,
                             HttpSession session) {
        try {
            String companyCode = (String) session.getAttribute("companyCode");
            if (companyCode == null || companyCode.trim().isEmpty()) {
                throw new RuntimeException("Company code not found in session");
            }

            ItemService.deleteItem(companyCode, itemCode);
            redirectAttributes.addFlashAttribute("success", "Item deleted successfully!");

            // Check if there are any items left
            List<ItemDetails> remainingItems = ItemService.getItemsByCompanyCode(companyCode);
            if (remainingItems.isEmpty()) {
                return "redirect:/Item/items";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting item: " + e.getMessage());
        }
        return "redirect:/Item/list";
    }


    @GetMapping("/itemMasterReport")
    @ResponseBody
    public ResponseEntity<InputStreamResource> getItemMasterReport(Model model, HttpSession session)
            throws JRException, FileNotFoundException, DocumentException {

        System.out.println("Item Master Report processing");

        String companyCode = (String) session.getAttribute("companyCode");
        String fileName = ItemService.getItemMasterListForReport(companyCode);

        System.out.println("report File is received");

        if (ItemService.isValidFilePath(fileName)) {
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
                return ResponseEntity.ok().headers(headers).contentLength(blankFile.length())
                        .contentType(MediaType.parseMediaType("application/pdf")).body(resource);
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
            return ResponseEntity.ok().headers(headers).contentLength(blankFile.length())
                    .contentType(MediaType.parseMediaType("application/pdf")).body(resource);
        }
    }

    @GetMapping("/searchlist")
    @ResponseBody
    public List<ItemmasterProjection> getItemProjectionsByCompany(Model model, HttpSession session) {
        String companyCode = (String) session.getAttribute("companyCode");
        return ItemService.getItemProjectionsByCompany(companyCode);
    }
}