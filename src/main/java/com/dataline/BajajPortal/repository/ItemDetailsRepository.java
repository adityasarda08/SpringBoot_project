package com.dataline.BajajPortal.repository;

import com.dataline.BajajPortal.model.master.ItemDetails;
// import jakarta.validation.constraints.NotBlank;
// import jakarta.validation.constraints.Size;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
// import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemDetailsRepository extends MongoRepository<ItemDetails, String> {

    boolean existsByCompanyCodeAndItemCode(String companyCode, String itemCode);

    List<ItemDetails> findByCompanyCode(String companyCode);

    ItemDetails findByCompanyCodeAndItemCode(String companyCode, String itemCode);

    //Optional<ItemDetails> findByItemCode(String itemCode);

    Optional<ItemDetails> findByItemCodeAndCompanyCode(String itemCode, String companyCode);

    @Aggregation(pipeline = {
            "{ $match: { 'companyCode': ?0 } }",
            "{ $project: { '_id': 0, 'itemCode': 1, 'itemName': 1, 'itemUnit': 1, 'itemLocation': 1, 'hsnCode': 1, 'itemRemark1': 1, 'itemRemark2': 1, 'companyCode': 1 } }"
    })
    List<ItemmasterProjection> findItemsByCompanyCode(String companyCode);

    // Aggregation to get projection for report by item code only
    @Aggregation(pipeline = {
            "{ $match: { 'itemCode': ?0 } }",
            "{ $project: { '_id': 0, 'itemCode': 1, 'itemName': 1, 'itemUnit': 1, 'itemLocation': 1, 'hsnCode': 1, 'itemRemark1': 1, 'itemRemark2': 1, 'companyCode': 1 } }"
    })
    List<ItemmasterProjection> findItemsByItemCodeOnly(String itemCode);


}
