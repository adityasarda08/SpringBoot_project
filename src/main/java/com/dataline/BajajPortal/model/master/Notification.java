package com.dataline.BajajPortal.model.master;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Document(collection = "Notifications")
@Data
public class Notification {


    @Id
    @Size(max = 10, message = "Notification code should not exceed 10 characters")
    @Indexed(unique = true)
    private String notificationCode;

    @NotNull(message = "Notification date is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")  // ✅ Fix JSON conversion
    @DateTimeFormat(pattern = "yyyy-MM-dd")  // ✅ Fix form submission
    private Date notificationDate;

    @NotBlank(message = "Notification details are required")
    @Size(min = 10, max = 300, message = "Notification details should be between 10 and 300 characters")
    private String notificationDetails;

    @NotNull(message = "Notification expiry date is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")  // ✅ Fix JSON conversion
    @DateTimeFormat(pattern = "yyyy-MM-dd")  // ✅ Fix form submission
    private Date notificationExpiryDate;

    @NotBlank(message = "Company code is required")
    private String userCode;

    @NotBlank(message = "Company code is required")
    @Size(min = 6, message = "Company code should not exceed 6 characters")
    private String companyCode;


}
