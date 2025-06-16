package com.dataline.BajajPortal.services;

import com.dataline.BajajPortal.model.master.Notification;
import com.dataline.BajajPortal.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    // 1️⃣ Fetch all notifications from the database
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    // 2️⃣ Fetch a single notification by ID
    public Optional<Notification> getNotificationById(String id) {
        return notificationRepository.findById(id);
    }

    // 3️⃣ Fetch notifications for a specific company using companyCode
    public List<Notification> getNotificationsByCompanyCode(String companyCode) {
        return notificationRepository.findByCompanyCode(companyCode);
    }

    // 4️⃣ Fetch only active (non-expired) notifications
    public List<Notification> getActiveNotifications() {
        return notificationRepository.findByNotificationExpiryDateAfter(new Date());
    }

    // 5️⃣ Save a notification with a duplicate check for notificationCode
    public Notification saveNotification(Notification notification) {
        Optional<Notification> existingNotification = notificationRepository.findTopByOrderByNotificationCodeDesc();

        // Prevent duplicate notification codes
        if (existingNotification.isPresent() && existingNotification.get().getNotificationCode().equals(notification.getNotificationCode())) {
            throw new RuntimeException("Notification code already exists!");
        }

        return notificationRepository.save(notification);
    }

    // 6️⃣ Delete a notification by ID
    public void deleteNotification(String id) {
        notificationRepository.deleteById(id);
    }
}
