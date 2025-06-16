package com.dataline.BajajPortal.controller;

import java.util.*;
import java.util.stream.Collectors;

import com.dataline.BajajPortal.model.master.GstPoInfo;
import com.dataline.BajajPortal.model.master.GstPoInfoProjectionView;
import com.dataline.BajajPortal.services.GstPoInfoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequiredArgsConstructor
@RequestMapping("/gstPoInfo")
public class GstPoInfoController {
    @Autowired
    GstPoInfoService poInfoDetailServices;


    //@PreAuthorize("@vendorPermissionChecker.isPoInfoShow(authentication)")
    @GetMapping("/new")
    public String addnPoinfo(Model model) {
        model.addAttribute("allErrors", "Test error message");
        List<GstPoInfo> gstPoInfo = new ArrayList<GstPoInfo>();
        model.addAttribute("gstPoInfo", gstPoInfo);
        return "vendor/gstPoInfo";
    }

    @GetMapping("/list")
    public String getVendorItemSummary(Model model, HttpSession session) {
        String companyCode = (String) session.getAttribute("companyCode");
        model.addAttribute("gstPoInfoList", poInfoDetailServices.getVendorItemSummary(companyCode));
        return "/vendor/gstPoInfolist";
    }

    //@PreAuthorize("@vendorPermissionChecker.isPoInfoSave(authentication)")
    @PostMapping(value = "/save", consumes = "application/json")
    public ResponseEntity<?> saveOrUpdatePoInfoList(@RequestBody @Valid List<GstPoInfo> gstPoinfolist, BindingResult result, // Renamed parameter
                                                    HttpSession session) { // Renamed method, removed Model

        // --- Basic Validation ---
        if (result.hasErrors()) {
            String errorMessages = result.getAllErrors().stream().map(ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; ")); // Use semicolon
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessages);
        }

        // --- Session/Company Code Validation ---
        String companyCode = (String) session.getAttribute("companyCode");
        if (companyCode == null || companyCode.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Session invalid or company code missing. Please log in again.");
        }
        poInfoDetailServices.addPoInfoDetails(gstPoinfolist, companyCode);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/edit/{vendorCode}/{vendorPlantCode}") // Add vendorPlantCode to the path
    public String updateChallan(
            @PathVariable("vendorCode") String vendorCode,         // Simplified @PathVariable
            @PathVariable("vendorPlantCode") String vendorPlantCode,    // Add @PathVariable here
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) throws JsonProcessingException {
        List<GstPoInfoProjectionView> gstPoInfo = poInfoDetailServices.getPoInfoListByVendorCode(vendorCode, vendorPlantCode);
        if (!gstPoInfo.isEmpty()) { // Check for null AND empty
            model.addAttribute("gstPoInfo", gstPoInfo);
            return "vendor/gstPoInfo";
        } else {
            return "redirect:/gstPoInfo/list"; // Redirect back to list view
        }
    }


    @GetMapping("/get/{vendorCode}/{vendorPlantCode}")
    @ResponseBody
    public List<GstPoInfoProjectionView> getgstPoInfo(@PathVariable(value = "vendorCode") String vendorCode,
                                                      @PathVariable(value = "vendorPlantCode") String vendorPlantCode,

                                                      Model model, HttpSession session) throws JsonProcessingException {
        String companyCode = (String) session.getAttribute("companyCode");
        List<GstPoInfoProjectionView> gstPoInfo = poInfoDetailServices.getPoInfoListByVendorCode(vendorCode, vendorPlantCode);
        if (gstPoInfo != null) {
            model.addAttribute("gstPoinfo", gstPoInfo);
            return gstPoInfo;
        } else {
            return null;
        }
    }


    @GetMapping("/delete/{vendorCode}/{plantCode}")
    @ResponseBody
    public String deleteGstPoinfo(@PathVariable(value = "vendorCode") String vendorCode,
                                  @PathVariable(value = "plantCode") String plantCode,
                                  Model model) {
        poInfoDetailServices.deletePoInfoByVendorCodeAndPlantCode(vendorCode, plantCode);
        return "Gst Po Info Deleted Successfully";
    }


    //@PreAuthorize("@vendorPermissionChecker.isPoInfoDelete(authentication)")
    @GetMapping("/delete/{vendorCode}/{plantCode}/{itemCode}")
    @ResponseBody
    public String deleteGstPoInfoByItemCode(@PathVariable(value = "vendorCode") String vendorCode,
                                            @PathVariable(value = "plantCode") String plantCode,
                                            @PathVariable(value = "itemCode") String itemCode,
                                            Model model) {
        poInfoDetailServices.deletePoInfoByVendorCodeAndPlantCodeAndItemCode(vendorCode, plantCode, itemCode);
        return "Gst Po Info Deleted Successfully";
    }


}