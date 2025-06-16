package com.dataline.BajajPortal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/pages")
public class ReportController {

    @GetMapping("/vendorReport.html")
    public String vendorReport() {
        return "pages/vendorReport"; // No need for .html
    }

    @GetMapping("/ItemReports.html")
    public String ItemReports() {
        return "pages/ItemReports";
    }
}
