package com.dataline.BajajPortal.repository;

import com.dataline.BajajPortal.model.master.CompanyDetails;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends MongoRepository<CompanyDetails, String> {

    List<CompanyDetails> findByCompanyCode(String companyCode);

    boolean existsByCompanyCode(String companyCode);


    @org.springframework.data.mongodb.repository.Aggregation(pipeline = {
            "{ $match: { companyCode: ?0 } }",
            "{ $project: {companyCode: 1, companyName: 1, companyAddress: 1, companyContact: 1, companyEmail: 1, companyStatus: 1 } }"
    })
    List<CompanyDetails> getCompanyProjectionDetails(String companyCode);

//    List<CompanyDetails> findByinvoiceTemplates(String invoiceTemplates);
//
//    List<CompanyDetails> findBytemplatesName(String templateName);
}
