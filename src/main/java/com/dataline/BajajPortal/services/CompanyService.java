package com.dataline.BajajPortal.services;

import com.dataline.BajajPortal.model.master.CompanyDetails;
import com.dataline.BajajPortal.repository.CompanyRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Service
public class CompanyService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);

    @Autowired
    private CompanyRepository companyRepository;

    // Recommended version
    public Optional<CompanyDetails> getCompanyById(String companyCode) {
        logger.debug("Fetching company by ID (companyCode): {}", companyCode);
        // Directly call findById which accepts the String ID
        return companyRepository.findById(companyCode);
        // If companyCode could potentially be null or empty before calling,
        // you might add a check here, but the controller's @PathVariable usually ensures it's not.
    }

    public List<CompanyDetails> getAllCompanies() {
        logger.debug("Fetching all companies");
        return companyRepository.findAll();
    }

    public CompanyDetails saveCompany(CompanyDetails company) {
        try {
            company.set_id(company.getCompanyCode());
            return companyRepository.save(company);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid ObjectId format when updating company", e);
        }
        return company;
    }

    public void deleteCompany(String companyCode) {
        try {
            companyRepository.deleteById(companyCode);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid ObjectId format when deleting company: {}", companyCode, e);
        }
    }

    public CompanyDetails findByCompanyCode(String companyCode) {
        return companyRepository.findByCompanyCode(companyCode).isEmpty() ? null : companyRepository.findByCompanyCode(companyCode).get(0);
    }

    /**
     * Gets the 'invoiceTemplates' status string for a given companyCode.
     *
     * @param companyCode The company code (business key).
     * @return The invoiceTemplates status ("yes" or "no") or null if company not found.
     */
    public String getCompanyTemplatesStatus(String companyCode) {
        Optional<CompanyDetails> companyOpt = Optional.ofNullable(findByCompanyCode(companyCode)); // Find using the code
        return companyOpt.map(CompanyDetails::getInvoiceTemplates).orElse(null); // Extract field
    }

    /**
     * Gets the 'templateName' string for a given companyCode.
     *
     * @param companyCode The company code (business key).
     * @return The templateName or null if company not found or templateName is not set.
     */
    public String getCompanyTemplatesName(String companyCode) {
        Optional<CompanyDetails> companyOpt = Optional.ofNullable(findByCompanyCode(companyCode)); // Find using the code
        return companyOpt.map(CompanyDetails::getTemplateName).orElse(null); // Extract field
    }
}