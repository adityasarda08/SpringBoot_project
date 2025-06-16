package com.dataline.BajajPortal.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@Configuration  // Uncomment this
public class GlobalControllerAdvice {

    @ModelAttribute("companyCode")
    public String addCompanyCodeToModel(HttpSession session) {
        String companyCode = (String) session.getAttribute("companyCode");
        return companyCode;
    }
}