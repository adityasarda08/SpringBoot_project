package com.dataline.BajajPortal.controller;

import com.dataline.BajajPortal.model.master.Notification;
import com.dataline.BajajPortal.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller  // ✅ Use @Controller to return HTML views
@RequestMapping("/notifications") // ✅ Base path for all notification routes
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // 1️⃣ Show Notification Form (HTML Page)
    @GetMapping("/new")
    public String showNotificationForm(Model model) {
        model.addAttribute("notification", new Notification()); // ✅ Pass empty Notification object for form binding
        return "Notifications/Notification";  // ✅ Returns the form located in /templates/pages/Notification.html
    }

    // ✅ Fix: Add a POST endpoint for saving notifications
    @PostMapping("/save")
    public String saveNotification(@ModelAttribute Notification notification) {
        notificationService.saveNotification(notification);
        return "redirect:/notifications/list";  // Redirect to notification view page
    }

    // 2️⃣ View Notifications Page (HTML Page)
    @GetMapping("/list")
    public String showNotificationPage(Model model) {
        model.addAttribute("notifications", notificationService.getAllNotifications()); // ✅ Pass data to frontend
        return "Notifications/notificationList"; // ✅ Returns /templates/pages/notificationList.html
    }

    @GetMapping("/dashlist")
    public String showNotificationDashboard(Model model) {
        model.addAttribute("notifications", notificationService.getAllNotifications()); // ✅ Pass data to frontend
        return "Notifications/notificationDashboard"; // ✅ Returns /templates/pages/notificationDashboard.html
    }

    // 3️⃣ API: Get All Notifications (JSON Response)
    @GetMapping("/searchList")
    @ResponseBody
    public List<Notification> getAllNotifications() {
        return notificationService.getAllNotifications();
    }

    // 3️⃣ API: Get All Notifications (JSON Response)
    @GetMapping("/dashboardList")
    public String getDashboardList(Model model) {
        model.addAttribute("notifications", notificationService.getAllNotifications());
        return "notificationList";
    }

    // 4️⃣ API: Get Notification by ID (JSON Response)
    @GetMapping("/edit/{notificationCode}")
    public String getNotificationById(@PathVariable String id) {
//         return notificationService.getNotificationById(id);
        return "";
    }

    // 5️⃣ API: Get Notifications by Company Code (JSON Response)
    @GetMapping("/searchList/{companyCode}")
    @ResponseBody
    public List<Notification> getNotificationsByCompanyCode(@PathVariable String companyCode) {
        return notificationService.getNotificationsByCompanyCode(companyCode);
    }

}
