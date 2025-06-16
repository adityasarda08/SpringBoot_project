package com.dataline.BajajPortal.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class logincontroller {

    @Autowired
    private com.dataline.BajajPortal.services.loginservices loginservices;

    @GetMapping("/login")
    public String loginUser(Model model) {
        System.out.println("in");
        return "pages/loginpage";
    }

    @PostMapping("/login")
    public String newLogin(@RequestParam("companyCode") String companyCode,
                           @RequestParam("password") String password,
                           HttpSession session,
                           Model model) {

        System.out.println("Company Code " + companyCode);
        System.out.println("Password " + password);

        if (loginservices.verifyCompany(companyCode, password).equalsIgnoreCase("Logged in successfully")) {
            // Store in session
            session.setAttribute("companyCode", companyCode);
            // You can also store additional attributes if needed
            session.setAttribute("isLoggedIn", true);
            return "redirect:/user/dashboard";
        } else {
            model.addAttribute("error", "Invalid company code or password");
            return "pages/loginpage";
        }
    }

    // Optional: Method to check if user is logged in
    @SuppressWarnings("unused")
    private boolean isUserLoggedIn(HttpSession session) {
        return session.getAttribute("companyCode") != null;
    }

    // Optional: Method to get company code from any controller
    protected String getCompanyCode(HttpSession session) {
        return (String) session.getAttribute("companyCode");
    }
}