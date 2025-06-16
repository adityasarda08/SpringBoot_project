package com.dataline.BajajPortal.repository;

import com.dataline.BajajPortal.model.master.ShippingDetails;
import com.dataline.BajajPortal.model.master.ShippingListProjection;

import java.util.List;
// import java.util.Optional;

// import jakarta.validation.constraints.Size;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ShippingDetailsRepository extends MongoRepository<ShippingDetails, String> {
    @Aggregation(pipeline = {
            "{$match: { 'companyCode' : ?0 } }",
            "{$project: {'_id':0, 'shiftVendorCode':1, 'shiftVendorName':1, 'shippingAddress':1, 'shippingGstNo':1, 'shippingStateCode':1, 'companyCode': 1  }}"
    })
    List<ShippingDetailsProjection> findShippingsbycompanyCode(String companyCode);

//    @Query("{}")
//    List<ShippingListProjection> findAList();

    @Query("{'companyCode': ?0}")
    List<ShippingListProjection> findAList(String companyCode);

    List<ShippingDetails> findByCompanyCode(String companyCode);

    List<ShippingDetails> findAll();

    List<ShippingDetails> findByShiftVendorCodeAndCompanyCode(String shiftVendorCode, String companyCode);

    boolean existsByShiftVendorCodeAndCompanyCode(String shiftVendorCode, String companyCode);
}
