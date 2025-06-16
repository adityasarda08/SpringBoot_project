package com.dataline.BajajPortal.repository;

import com.dataline.BajajPortal.model.master.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    // Get the latest notification based on notificationCode (assuming it's sequential)
    Optional<Notification> findTopByOrderByNotificationCodeDesc();

    // Find all notifications for a specific company
    List<Notification> findByCompanyCode(String companyCode);

    // Find notifications that have not expired
    List<Notification> findByNotificationExpiryDateAfter(java.util.Date currentDate);

    // Find notifications by date range
    List<Notification> findByNotificationDateBetween(java.util.Date startDate, java.util.Date endDate);
}
