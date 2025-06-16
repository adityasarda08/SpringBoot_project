package com.dataline.BajajPortal.services;

//import jakarta.validation.constraints.NotBlank;

public class DuplicateDepartmentCodeException extends RuntimeException {
    public DuplicateDepartmentCodeException(String message) {
        super(message);
    }
}
