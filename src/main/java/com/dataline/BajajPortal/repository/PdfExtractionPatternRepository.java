package com.dataline.BajajPortal.repository;

import com.dataline.BajajPortal.model.config.PdfExtractionPattern;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PdfExtractionPatternRepository extends MongoRepository<PdfExtractionPattern, String> {

    /**
     * Find pattern by vendor code
     */
    Optional<PdfExtractionPattern> findByVendorCode(String vendorCode);

    /**
     * Get all patterns - useful for trying to match when vendor code is not found
     */
    List<PdfExtractionPattern> findAll();
}