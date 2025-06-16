package com.dataline.BajajPortal.repository;

import com.dataline.BajajPortal.model.master.GstPoInfoList;
import com.dataline.BajajPortal.model.master.GstPoInfo;
import com.dataline.BajajPortal.model.master.GstPoInfoProjectionView;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GstPoInfoRepository extends MongoRepository<GstPoInfo, String> {

    List<GstPoInfo> findByVendorCodeAndVendorPlantCode(String vendorCode, String plantCode);

    List<GstPoInfo> findByVendorCode(String vendorCode);

    List<GstPoInfo> findByVendorCodeAndCompanyCode(String vendorCode, String companyCode);

    Optional<GstPoInfo> findByVendorCodeAndVendorPlantCodeAndItemCode(String vendorCode, String vendorPlantCode, String itemCode);

    /**
     * Checks if a GstPoInfo document exists for the given vendor code, plant code, and item code.
     * Spring Data MongoDB automatically generates the query based on the method name.
     * Equivalent to MongoDB query: db.gstPoInfo.countDocuments({ vendorCode: ?, vendorPlantCode: ?, itemCode: ? }) > 0
     *
     * @param vendorCode      The vendor code to check.
     * @param vendorPlantCode The vendor plant code to check.
     * @param itemCode        The item code to check.
     * @return true if a document with the specified combination exists, false otherwise.
     */
    boolean existsByVendorCodeAndVendorPlantCodeAndItemCode(String vendorCode, String vendorPlantCode, String itemCode);

    @Aggregation(pipeline = {
            // Stage 1: Match documents by companyCode (Uses the method parameter ?0)
            "{ $match: {companyCode: ?0} }",

            "{ $lookup: { \n" +
                    "    from: 'Vendor_Details', \n" +
                    "    let: { docVendorCode: '$vendorCode', docVendorPlantCode: '$vendorPlantCode', docCompanyCode: '$companyCode' }, \n" + // Use companyCode from TaxInvoice doc
                    "    pipeline: [ \n" +
                    "        { $match: { \n" +
                    "            $expr: { \n" +
                    "                $and: [ \n" +
                    "                    { $eq: ['$vendorCode', '$$docVendorCode'] }, \n" +
                    "                    { $eq: ['$vendorPlantCode', '$$docVendorPlantCode'] }, \n" +
                    "                    { $eq: ['$companyCode', '$$docCompanyCode'] } \n" +
                    "                ] \n" +
                    "            } \n" +
                    "        } } \n" +
                    "    ], \n" +
                    "    as: 'vendor' \n" +
                    "} }",

            "{ $unwind: { \n" +
                    "    path: '$vendor', \n" +
                    "    preserveNullAndEmptyArrays: true \n" + // Keep GstPoInfo even if no matching itemDetails
                    "} }",

            // Stage 2: Group by vendorCode and vendorPlantCode
            "{ $group: { \n" +
                    "    _id: { vendorCode: '$vendorCode', vendorPlantCode: '$vendorPlantCode' }, \n" + // Grouping key
                    // Get vendorName from the first document in the group (assuming it's consistent within the group)
                    "    vendorName: { $first: '$vendor.vendorName' }, \n" +
                    // Count the number of documents (items) in each group
                    "    itemCount: { $sum: 1 } \n" +
                    "} }",

            // Stage 3: Project the final desired output structure matching GstPoInfoList
            "{ $project: { \n" +
                    "    _id: 0, \n" + // Exclude the default _id
                    "    vendorCode: '$_id.vendorCode', \n" +
                    "    vendorPlantCode: '$_id.vendorPlantCode', \n" +
                    "    vendorName: {$ifNull:['$vendorName','N/A']},\n" +
                    "    itemCount: '$itemCount' \n" + // Include the calculated itemCount
                    "} }",

            // Stage 4: Optional Sort
            "{ $sort: { vendorCode: 1, vendorPlantCode: 1 } }"
    })
        // Ensure the method signature matches the service/controller calls
    List<GstPoInfoList> findVendorItemSummary(String companyCode);

    /**
     * Finds GstPoInfo records for a specific vendor and company, enriching them
     * with itemName and itemUnit from the 'itemDetails' collection.
     *
     * @param vendorCode      The vendor code to filter by.
     * @param vendorPlantCode The company code to filter by.
     * @return A list of GstPoInfo objects, potentially enriched with itemName and itemUnit.
     */
    @Aggregation(pipeline = {
            // Stage 1: Match GstPoInfo documents by vendorCode and companyCode
            "{ $match: { vendorCode: ?0 ,vendorPlantCode: ?1} }",
            // Stage 2: Perform a left outer join with the 'Item_Details' collection
            // Using let/pipeline to match on BOTH itemCode AND companyCode
            "{ $lookup: { \n" +
                    "    from: 'Item_Details', \n" + // The collection to join with
                    "    let: { \n" + // Define variables from the GstPoInfo document
                    "        gstItemCode: '$itemCode', \n" +
                    "        gstCompanyCode: '$companyCode' \n" +
                    "    }, \n" +
                    "    pipeline: [ \n" + // Pipeline to run on Item_Details
                    "      { $match: { \n" + // Match stage within the pipeline
                    "          $expr: { \n" + // Use $expr to compare fields with variables
                    "            $and: [ \n" + // Both conditions must be true
                    "              { $eq: ['$itemCode', '$$gstItemCode'] }, \n" + // Item_Details.itemCode == GstPoInfo.itemCode
                    "              { $eq: ['$companyCode', '$$gstCompanyCode'] } \n" + // Item_Details.companyCode == GstPoInfo.companyCode
                    "            ] \n" +
                    "          } \n" +
                    "        } \n" +
                    "      }, \n" +
                    // Optional: If you expect only one matching item detail per company/item, add a limit
                    // "      { $limit: 1 } \n" +
                    "    ], \n" +
                    "    as: 'itemDetailsInfo' \n" + // Output array field name
                    "} }",

            // Stage 3: Deconstruct the itemDetailsInfo array. Preserve docs even if no match found.
            "{ $unwind: { \n" +
                    "    path: '$itemDetailsInfo', \n" +
                    "    preserveNullAndEmptyArrays: true \n" + // Keep GstPoInfo even if no matching itemDetails
                    "} }",

            "{ $lookup: { \n" +
                    "    from: 'Vendor_Details', \n" +
                    "    let: { docVendorCode: '$vendorCode', docVendorPlantCode: '$vendorPlantCode', docCompanyCode: '$companyCode' }, \n" + // Use companyCode from TaxInvoice doc
                    "    pipeline: [ \n" +
                    "        { $match: { \n" +
                    "            $expr: { \n" +
                    "                $and: [ \n" +
                    "                    { $eq: ['$vendorCode', '$$docVendorCode'] }, \n" +
                    "                    { $eq: ['$vendorPlantCode', '$$docVendorPlantCode'] }, \n" +
                    "                    { $eq: ['$companyCode','$$docCompanyCode' ] } /* Use companyCode parameter ?1 */ \n" +
                    "                ] \n" +
                    "            } \n" +
                    "        } } \n" +
                    "    ], \n" +
                    "    as: 'vendor' \n" +
                    "} }",

            "{ $unwind: { \n" +
                    "    path: '$vendor', \n" +
                    "    preserveNullAndEmptyArrays: true \n" +
                    "} }",

            // Stage 4: Project the final structure - INCLUDE ALL NEEDED FIELDS FOR THE FORM
            "{ $project: { \n" +
                    // --- Include ALL necessary original fields from GstPoInfo ---
                    "    _id: 1, \n" +
                    "    vendorCode: 1, \n" +
                    "    vendorPlantCode: 1, \n" +
                    "    companyCode: 1, \n" + // <-- Added
                    "    itemCode: 1, \n" +    // <-- Added
                    "    poNumber: 1, \n" +  // <-- Added
                    "    itemRate: 1, \n" +   // <-- Added (Make sure GstPoInfo model has this)
                    "    poLineNumber: 1, \n" + // <-- Added (Make sure GstPoInfo model has this)
                    "    cgstPer: 1, \n" +    // <-- Added (Make sure GstPoInfo model has this)
                    "    sgstPer: 1, \n" +    // <-- Added (Make sure GstPoInfo model has this)
                    "    igstPer: 1, \n" +    // <-- Added (Make sure GstPoInfo model has this)
                    "    utgstPer: 1, \n" +   // <-- Added (Make sure GstPoInfo model has this)
                    "    vendorName: { $ifNull: ['$vendor.vendorName','N/A' ]}, \n" +
                    "    itemName: { $ifNull: [ '$itemDetailsInfo.itemName', 'N/A' ] }, \n" +
                    "    itemUnit: { $ifNull: [ '$itemDetailsInfo.itemUnit', 'N/A' ] } \n" +
                    "} }"
    })
    List<GstPoInfoProjectionView> findWithItemDetailsByVendorCodeVendorPlantCode(String vendorCode, String vendorPlantCode); // Changed from GstPoInfoList

    void deleteByVendorCodeAndVendorPlantCode(String vendorCode, String vendorPlantCode);

    void deleteByVendorCodeAndVendorPlantCodeAndItemCode(String vendorCode, String vendorPlantCode, String itemCode);


}