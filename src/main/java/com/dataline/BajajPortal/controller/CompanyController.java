package com.dataline.BajajPortal.controller;

import com.dataline.BajajPortal.model.master.CompanyDetails;
import com.dataline.BajajPortal.services.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/companies")
public class CompanyController {


    @Autowired
    private CompanyService companyService;

    /**
     * Retrieves and displays a list of all companies.
     *
     * @param model The model to add the list of companies to.
     * @return The view name for displaying the company list.
     */

    @GetMapping("/list")
    public String listCompanies(Model model) {
        model.addAttribute("companies", companyService.getAllCompanies());
        return "company/companylist";
    }

    @GetMapping("/new")
    public String showAddForm(Model model) {
        model.addAttribute("companyDetails", new CompanyDetails());
        return "company/companydetails";
    }

    @PostMapping("/save")
    public String saveCompany(@Valid @ModelAttribute("companyDetails") CompanyDetails companyDetails,
                              BindingResult result, Model model, HttpSession session) {
        if (result.hasErrors()) {
            // Add this line to show validation errors to user
            model.addAttribute("errorMessage", "Please correct the form errors");
            System.out.println(result.getAllErrors());
            return "company/companydetails";
        }

        try {
            CompanyDetails savedCompanyDetails = companyService.saveCompany(companyDetails);
            session.setAttribute("companyCode", savedCompanyDetails.getCompanyCode());
            return "redirect:/companies/list";
        } catch (Exception e) {
            // Add specific error message
            model.addAttribute("errorMessage", "Error saving company: " + e.getMessage());
            return "company/companydetails";
        }
    }

    @GetMapping("/edit/{companyCode}")
    public String showEditForm(@PathVariable String companyCode, Model model) {
        try {
            CompanyDetails companyDetails = companyService.getCompanyById(companyCode)
                    .orElseThrow(() -> new RuntimeException("Company not found with id: " + companyCode));
//            sword before sending to view ---
            companyDetails.setPassword(null); // Set to null
            model.addAttribute("companyDetails", companyDetails);
            return "company/companydetails";
        } catch (Exception e) {
            return "redirect:/companies/list";
        }
    }


}
