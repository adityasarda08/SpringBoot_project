package com.dataline.BajajPortal.repository;

import com.dataline.BajajPortal.model.master.VendorDetails;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorRepository extends MongoRepository<VendorDetails, String> {
    List<VendorDetails> findByCompanyCode(String companyCode);

    Optional<VendorDetails> findById(String id);

    boolean existsByVendorCodeAndVendorPlantCodeAndCompanyCode(String vendorCode, String plantCode, String companyCode);

    Optional<VendorDetails> findByVendorCodeAndVendorPlantCode(String vendorCode, String plantCode);

    List<VendorDetails> findByVendorPlantCode(String vendorPlantCode);

    List<VendorDetails> findByVendorCode(String vendorCode);

    Optional<VendorDetails> findByVendorCodeAndVendorPlantCodeAndCompanyCode(
            String vendorCode,
            String plantCode,
            String companyCode
    );

    @Aggregation(pipeline = {
            "{$match: { 'companyCode' : ?0 } }",
            "{$project: {'_id':0, 'vendorCode':1, 'vendorPlantCode':1, 'vendorName':1, 'vendorAddress':1, 'vendorGST':1, 'vendorCIN':1, 'vendorPAN':1, 'pinCode':1, 'remark1': 1, 'remark2': 1, 'companyCode': 1  }}"
    })
    List<VendorProjection> findvendorbycompanyCode(String companyCode);

    @Aggregation(pipeline = {
            "{$match: { 'companyCode' : ?0, 'vendorCode': ?1, 'vendorPlantCode': ?2 } }",
            "{$project: {'invoiceCopies':1, '_id':0}}"
    })
    List<Integer> findInvoiceCopiesByCompanyCodeAndVendorCodeAndVendorPlantCode(String companyCode, String vendorCode, String vendorPlantCode);


    /**
     * Fetches a projection of vendor details (code, plant, name) for a specific company,
     * excluding deleted vendors.
     *
     * @param companyCode The company code to filter by.
     * @return A list of VendorProjection objects.
     */
    @Aggregation(pipeline = {
            "{ $match: { \n" +
                    "    companyCode: ?0, \n" +
                    "} }",

            "{ $project: { \n" +
                    "    _id: 0, \n" +
                    "    vendorCode: 1, \n" +
                    "    vendorPlantCode: 1, \n" +
                    "    vendorName: 1 ,\n" +
                    "} }"
    })
    List<VendorProjection> findVendorProjectionByCompanyCode(String companyCode);

    /**
     * Finds the GST number for a specific vendor code and company code.
     *
     * @param vendorCode  The vendor code to search for.
     * @param companyCode The company code to filter by.
     * @return An Optional containing the vendorGstNo if found, otherwise empty.
     */
    @Aggregation(pipeline = {
            "{ $match: {\n" +
                    " companyCode: ?0, \n" +
                    " vendorCode: ?1, \n" +
                    "} }",
            "{ $project: { \n" +
                    "    _id: 0, \n" +
                    "    vendorGST: 1, \n" +
                    "} }"
    })
    List<VendorProjection> findVendorGstNoByVendorCodeAndCompanyCode(String vendorCode, String companyCode);

}

