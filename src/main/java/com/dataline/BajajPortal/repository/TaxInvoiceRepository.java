package com.dataline.BajajPortal.repository;

import com.dataline.BajajPortal.model.taxinvoice.TaxInvoice;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaxInvoiceRepository extends MongoRepository<TaxInvoice, Long> {

    // Find by company code
    List<TaxInvoice> findByCompanyCode(String companyCode);

    // Find by invoice ID and company code
    Optional<TaxInvoice> findByInvoiceIdAndCompanyCode(Long invoiceId, String companyCode);

    // Find by invoice number and company code
    List<TaxInvoice> findByInvoiceNumberAndCompanyCode(String invoiceNumber, String companyCode);

    // Other query methods...

    @Query(value = "{ 'invoiceNumber': ?0, 'companyCode': ?1 }", fields = "{ 'vendorPlantCode': 1, '_id': 0 }")
    List<String> findVendorPlantCodeByInvoiceNumberAndCompanyCode(String invoiceNumber, String companyCode);

    // Find by vendor code and company code
    List<TaxInvoice> findByVendorCodeAndCompanyCode(String vendorCode, String companyCode);

    // Find by date range and company code
    @Query("{'invoiceDate': {$gte: ?0, $lte: ?1}, 'companyCode': ?2}")
    List<TaxInvoice> findByInvoiceDateBetweenAndCompanyCode(LocalDate startDate, LocalDate endDate, String companyCode);

    // Find by vendor code, date range, and company code
    @Query("{'vendorCode': ?0, 'invoiceDate': {$gte: ?1, $lte: ?2}, 'companyCode': ?3}")
    List<TaxInvoice> findByVendorCodeAndInvoiceDateBetweenAndCompanyCode(
            String vendorCode,
            LocalDate startDate,
            LocalDate endDate,
            String companyCode
    );

    // Check if invoice number exists for company
    boolean existsByInvoiceNumberAndCompanyCode(String invoiceNumber, String companyCode);

    // Custom query to find invoices with total amount greater than specified value
    @Query("{'totalAmount': {$gt: ?0}, 'companyCode': ?1}")
    List<TaxInvoice> findByTotalAmountGreaterThanAndCompanyCode(Double amount, String companyCode);

    // Find pending invoices (without Eway bill number)
    @Query("{'ewayBillNo': {$exists: false}, 'companyCode': ?0}")
    List<TaxInvoice> findPendingInvoicesByCompanyCode(String companyCode);

    @Query(value = "{ 'invoiceNumber': { $in: ?0 }, 'companyCode': ?1 }", fields = "{ 'vendorPlantCode': 1 }")
    List<String> findDistinctVendorPlantCodesByInvoiceNumbersAndCompanyCode(List<String> invoiceNumbers, String companyCode);

    // Find by vendor plant code
    List<TaxInvoice> findByVendorPlantCode(String vendorPlantCode);

    // Count invoices by vendor code and company code
    long countByVendorCodeAndCompanyCode(String vendorCode, String companyCode);

    List<TaxInvoice> findByInvoiceDateBetweenAndItemCodeAndCompanyCode(
            LocalDate startDate, LocalDate endDate, String itemCode, String companyCode);


    // --- New Aggregation Method ---

    /**
     * Performs an aggregation to fetch detailed TaxInvoice data suitable for printing,
     * joining with Company, Vendor, Item, and Shipping details.
     *
     * @param invoiceNumber The specific invoice number to fetch.
     * @param companyCode   The company code context for lookups.
     * @return A list containing the projected invoice details (usually one element).
     */
    @Aggregation(pipeline = {
            // Stage 1: Match the specific invoice in the Tax_Invoice collection
            //"{ $match: { invoiceNumber: ?0 } }", // ?0 corresponds to the first parameter (invoiceNumber)

            // Assuming ?0 is bound to a List<String> or String[] containing the invoice numbers
            "{ $match: { invoiceNumber: { $in: ?0 },  \n" +
                    " companyCode: ?1 \n" +
                    "} }",

            // Stage 2: Lookup Company Details
            "{ $lookup: { from: 'Company_Details', localField: 'companyCode', foreignField: 'companyCode', as: 'company' } }",
            // Stage 3: Unwind Company (handle cases where company might not be found)
            "{ $unwind: { path: '$company', preserveNullAndEmptyArrays: true } }",

            // Stage 4: Lookup Vendor Details (Complex lookup matching vendorCode, vendorPlantCode, and companyCode)
            "{ $lookup: { \n" +
                    "    from: 'Vendor_Details', \n" +
                    "    let: { docVendorCode: '$vendorCode', docVendorPlantCode: '$vendorPlantCode', docCompanyCode: '$companyCode' }, \n" + // Use companyCode from TaxInvoice doc
                    "    pipeline: [ \n" +
                    "        { $match: { \n" +
                    "            $expr: { \n" +
                    "                $and: [ \n" +
                    "                    { $eq: ['$vendorCode', '$$docVendorCode'] }, \n" +
                    "                    { $eq: ['$vendorPlantCode', '$$docVendorPlantCode'] }, \n" +
                    "                    { $eq: ['$companyCode', ?1] } /* Use companyCode parameter ?1 */ \n" +
                    "                ] \n" +
                    "            } \n" +
                    "        } } \n" +
                    "    ], \n" +
                    "    as: 'vendor' \n" +
                    "} }",
            // Stage 5: Unwind Vendor
            "{ $unwind: { path: '$vendor', preserveNullAndEmptyArrays: true } }",

            // Stage 6: Lookup Item Details (Matching itemCode and companyCode)
            "{ $lookup: { \n" +
                    "    from: 'Item_Details', \n" +
                    "    let: { docItemCode: '$itemCode' }, \n" + // Use itemCode from TaxInvoice doc
                    "    pipeline: [ \n" +
                    "        { $match: { \n" +
                    "            $expr: { \n" +
                    "                $and: [ \n" +
                    "                    { $eq: ['$itemCode', '$$docItemCode'] }, \n" +
                    "                    { $eq: ['$companyCode', ?1] } /* Use companyCode parameter ?1 */ \n" +
                    "                ] \n" +
                    "            } \n" +
                    "        } } \n" +
                    "    ], \n" +
                    "    as: 'item' \n" +
                    "} }",
            // Stage 7: Unwind Item
            "{ $unwind: { path: '$item', preserveNullAndEmptyArrays: true } }",

            // Stage 8: Lookup Shipping Details (Matching shiftVendorCode and companyCode)
            "{ $lookup: { \n" +
                    "    from: 'Shipping_Details', \n" +
                    "    let: { docShiftVendorCode: '$vendorPlantCode' }, \n" + // Use shiftVendorCode from TaxInvoice doc
                    "    pipeline: [ \n" +
                    "        { $match: { \n" +
                    "            $expr: { \n" +
                    "                $and: [ \n" +
                    "                    { $eq: ['$shiftVendorCode', '$$docShiftVendorCode'] }, \n" +
                    "                    { $eq: ['$companyCode', ?1] } /* Use companyCode parameter ?1 */ \n" +
                    "                ] \n" +
                    "            } \n" +
                    "        } } \n" +
                    "    ], \n" +
                    "    as: 'shippingDetails' \n" +
                    "} }",
            // Stage 9: Unwind Shipping Details
            "{ $unwind: { path: '$shippingDetails', preserveNullAndEmptyArrays: true } }",
            // Stage 10: Project the final structure matching TaxInvoicePrintProjection
            "{ $project: { \n" +
                    "    _id: 0, \n" + // Comment removed
                    "    invoiceNumber: 1, \n" + "    companyCinNo: '$company.companyCinNo', \n" + "   " +
                    "    companyName: '$company.companyName', \n" + "companyLogo: '$company.companyLogo', \n" + "logoName: '$company.logoName', \n" +
                    "    companyAddress: '$company.companyAddress', \n" + "    companyPanNo: '$company.companyPanNo', \n" +
                    "    companyGstNo: '$company.companyGstNo', \n" + "    companyStateCode: '$company.companyStateCode', \n" +
                    "    taxReverseCharge: '$company.taxReverseCharge', \n" + "    companyContactNo: '$company.companyContactNo', \n" +
                    "    companyEmail: '$company.companyEmail', \n" + "    companyWebsite: '$company.companyWebsite', \n" +
                    "    invoiceDate: 1, \n" + "    vendorCode: 1, \n" + "    vendorPlantCode: 1, \n" +
                    "    vendorName: '$vendor.vendorName', \n" + "    invoiceCopies: '$vendor.invoiceCopies', \n" +
                    "    vendorAddress: '$vendor.vendorAddress', \n" + "    vendorGST: '$vendor.vendorGST', \n" +
                    "    vendorCIN: '$vendor.vendorCIN', \n" + "    vendorStateCode: '$vendor.vendorStateCode', \n" +
                    "    vendorPAN: '$vendor.vendorPAN', \n" + "    hsnCode: '$item.hsnCode',\n" + "    sacCode: 1,\n" +
                    "    itemCode: '$item.itemCode', \n" + "    poNo: 1, \n" + "    lineNo: 1, \n" + "   " +
                    "    invoiceQuantity: 1, \n" + "    basicRate: 1, \n" +
                    "    basicAmount: 1, \n" + "    freight: 1, \n" +
                    "    othersCharges: 1, \n" + "    subTotal: 1, \n" + "    additionalTaxAmount: 1, \n" + "    taxableAmount: 1, \n" + "    cgstPercentage: 1, \n" +
                    "    cgstAmount: 1, \n" + "    sgstPercentage: 1, \n" + "    sgstAmount: 1, \n" + "    igstPercentage: 1, \n" +
                    "    igstAmount: 1, \n" + "    utgstPercentage: 1, \n" + "    utgstAmount: 1, \n" +
                    "    totalAmount: 1, \n" + "    ewayBillNo: 1, \n" + "    ewayBillDate: 1, \n" + "    vehicleNo: 1, \n" + "    remark1: '$vendor.remark1', \n" +
                    "    remark2: '$vendor.remark2', \n" +
                    "    ediNumber: 1, \n" + "    shiftVendorCode: 1, \n" + "    itemRemark1: '$item.itemRemark1', \n" + "    itemRemark2: '$item.itemRemark2', \n" +
                    "    signedQrCode: 1, \n" +
                    "    deliveryChallanNo: 1, \n" + "    deliveryChallanDate: 1, \n" + "    deliveryChallanAmount: 1, \n" + "    tcsValue: 1, \n" +
                    "    itemName: '$item.itemName', \n" + "    itemUnit: '$item.itemUnit', \n" +
                    "    shiftVendorName: '$shippingDetails.shiftVendorName', \n" + "    shippingAddress: '$shippingDetails.shippingAddress', \n" +
                    "    shippingGstNo: '$shippingDetails.shippingGstNo', \n" +
                    "    shippingStateCode: '$shippingDetails.shippingStateCode', \n" + "    companyCode: '$company.companyCode', \n" +
                    "    labels: { $literal: '' }, \n" +
                    "} }"
    })
    List<TaxInvoicePrintProjection> findProjectedInvoiceByNumberAndCompany(List<String> invoiceNumber, String companyCode);


    /**
     * Performs an aggregation to fetch detailed TaxInvoice data for a date range,
     * optionally filtered by itemCode and invoiceNumber.
     * Joins with Company, Vendor, and Item details.
     *
     * @param startDate     The start date of the range (inclusive).
     * @param endDate       The end date of the range (inclusive).
     * @param companyCode   The company code context for lookups.
     * @param itemCode      Optional item code to filter by (can be null or empty).
     * @param invoiceNumber Optional invoice number to filter by (can be null or empty).
     * @return A list containing the projected invoice details.
     */
    @Aggregation(pipeline = {
            // Stage 1: Match based on date range, company, and optional filters
            "{ $match: { \n" +
                    "    $expr: { \n" +
                    "        $and: [ \n" +
                    "            { $eq: ['$companyCode', ?2] }, \n" +          // companyCode (param ?2)
                    "            { $gte: ['$invoiceDate', ?0] }, \n" +          // startDate (param ?0)
                    "            { $lte: ['$invoiceDate', ?1] }, \n" +          // endDate (param ?1)
                    // Conditionally match itemCode if ?3 is not null/empty
                    "            { $cond: { \n" +
                    "                if: { $and: [ { $ne: [?3, null] }, { $ne: [?3, ''] } ] }, \n" +
                    "                then: { $eq: ['$itemCode', ?3] }, \n" + // itemCode (param ?3)
                    "                else: true \n" + // If null/empty, this condition passes
                    "            } }, \n" +
                    // Conditionally match invoiceNumber if ?4 is not null/empty
                    "            { $cond: { \n" +
                    "                if: { $and: [ { $ne: [?4, null] }, { $ne: [?4, ''] } ] }, \n" +
                    "                then: { $eq: ['$invoiceNumber', ?4] }, \n" + // invoiceNumber (param ?4)
                    "                else: true \n" + // If null/empty, this condition passes
                    "            } } \n" +
                    "        ] \n" +
                    "    } \n" +
                    "} }",

            // Stage 2: Lookup Company Details
            "{ $lookup: { \n" +
                    "    from: 'Company_Details', \n" +
                    "    localField: 'companyCode', \n" +
                    "    foreignField: 'companyCode', \n" +
                    "    as: 'company' \n" +
                    "} }",
            // Stage 3: Unwind Company
            "{ $unwind: { path: '$company', preserveNullAndEmptyArrays: true } }",

            // Stage 4: Lookup Vendor Details (Matching vendorCode, vendorPlantCode from TaxInvoice, and companyCode param)
            "{ $lookup: { \n" +
                    "    from: 'Vendor_Details', \n" +
                    "    localField: 'vendorCode', \n" + // Match outer vendorCode
                    "    foreignField: 'vendorCode', \n" +
                    "    let: { outer_vendorPlantCode: '$vendorPlantCode' }, \n" + // Variable from Tax_Invoice
                    "    pipeline: [ \n" +
                    "        { $match: { \n" +
                    "            $expr: { \n" +
                    "                $and: [ \n" +
                    "                    { $eq: ['$companyCode', ?2] }, \n" + // companyCode parameter (?2)
                    "                    { $eq: ['$vendorPlantCode', '$$outer_vendorPlantCode'] } \n" + // Match plant code from Tax_Invoice
                    "                ] \n" +
                    "            } \n" +
                    "        } } \n" +
                    "    ], \n" +
                    "    as: 'vendor' \n" +
                    "} }",
            // Stage 5: Unwind Vendor
            "{ $unwind: { path: '$vendor', preserveNullAndEmptyArrays: true } }",

            // Stage 6: Lookup Item Details (Matching itemCode from TaxInvoice and companyCode param)
            "{ $lookup: { \n" +
                    "    from: 'Item_Details', \n" +
                    "    localField: 'itemCode', \n" + // Match outer itemCode
                    "    foreignField: 'itemCode', \n" +
                    "    pipeline: [ \n" +
                    "        { $match: { \n" +
                    "            $expr: { $eq: ['$companyCode', ?2] } \n" + // companyCode parameter (?2)
                    "        } } \n" +
                    "    ], \n" +
                    "    as: 'item' \n" +
                    "} }",
            // Stage 7: Unwind Item
            "{ $unwind: { path: '$item', preserveNullAndEmptyArrays: true } }",

            // Stage 8: Project the final structure (Ensure all fields match TaxInvoicePrintProjection and are quoted correctly)
            "{ $project: { \n" +
                    "    \"_id\": 0, \n" + "    \"invoiceNumber\": 1, \n" + "    \"companyName\": \"$company.companyName\", \n" + "    \"companyAddress\": \"$company.companyAddress\", \n" +
                    "    \"companyPanNo\": \"$company.companyPanNo\", \n" + "    \"companyGstNo\": \"$company.companyGstNo\", \n" +
                    "    \"companyStateCode\": \"$company.companyStateCode\", \n" + "    \"taxReverseCharge\": \"$company.taxReverseCharge\", \n" + "    \"companyContactNo\": \"$company.companyContactNo\", \n" +
                    "    \"companyEmail\": \"$company.companyEmail\", \n" + "    \"companyWebsite\": \"$company.companyWebsite\", \n" + "    \"invoiceDate\": 1, \n" + "    \"vendorCode\": 1, \n" +
                    "    \"vendorPlantCode\": 1, \n" + "    \"vendorName\": \"$vendor.vendorName\", \n" + "    \"vendorAddress\": \"$vendor.vendorAddress\", \n" + "    \"vendorGstNo\": \"$vendor.vendorGstNo\", \n" +
                    "    \"vendorCIN\": \"$vendor.vendorCIN\", \n" + "    \"vendorStateCode\": \"$vendor.vendorStateCode\", \n" + "    \"vendorPAN\": \"$vendor.vendorPAN\", \n" +
                    "    \"hsnCode\": \"$item.hsnCode\", \n" + // Note: hsnCode comes from item
                    "    \"sacCode\": 1, \n" + // Note: sacCode comes from TaxInvoice
                    "    \"itemCode\": \"$item.itemCode\", \n" + // Note: itemCode comes from item
                    "    \"poNo\": 1, \n" + "    \"lineNo\": 1, \n" + "    \"invoiceQuantity\": 1, \n" + "    \"basicRate\": 1, \n" + "    \"basicAmount\": 1, \n" + "    \"freight\": 1, \n" +
                    "    \"othersCharges\": 1, \n" + "    \"subTotal\": 1, \n" + "    \"additionalTaxAmount\": 1, \n" + "    \"taxableAmount\": 1, \n" + "    \"cgstPercentage\": 1, \n" +
                    "    \"cgstAmount\": 1, \n" + "    \"sgstPercentage\": 1, \n" + "    \"sgstAmount\": 1, \n" + "    \"igstPercentage\": 1, \n" + "    \"igstAmount\": 1, \n" +
                    "    \"utgstPercentage\": 1, \n" + "    \"utgstAmount\": 1, \n" + "    \"totalAmount\": 1, \n" + "    \"ewayBillNo\": 1, \n" + "    \"ewayBillDate\": 1, \n" + "    \"vehicleNo\": 1, \n" +
                    "    \"remark1\": \"$vendor.remark1\", \n" + // Note: remark1 from vendor
                    "    \"remark2\": \"$vendor.remark2\", \n" + // Note: remark2 from vendor
                    "    \"ediNumber\": 1, \n" + "    \"shiftVendorCode\": 1, \n" +
                    "    \"itemRemark1\": \"$item.itemRemark1\", \n" + // Note: itemRemark1 from item
                    "    \"itemRemark2\": \"$item.itemRemark2\", \n" + // Note: itemRemark2 from item
                    "    \"signedQrCode\": 1, \n" + "    \"deliveryChallanNo\": 1, \n" +
                    "    \"deliveryChallanDate\": 1, \n" + "    \"deliveryChallanAmount\": 1, \n" + "    \"tcsValue\": 1, \n" + "    \"itemName\": \"$item.itemName\", \n" + "    \"itemUnit\": \"$item.itemUnit\" \n" + // Note: itemUnit from item
                    "} }",

            // Stage 9: Sort by invoice date
            "{ $sort: { \"invoiceDate\": 1 } }" // 1 for ASC
    })
    List<TaxInvoicePrintProjection> findProjectedInvoicesByDateRangeAndFilters(
            LocalDate startDate,         // ?0
            LocalDate endDate,           // ?1
            String companyCode,          // ?2
            String itemCode,             // ?3 (Optional)
            String invoiceNumber         // ?4 (Optional)
    );

    /**
     * Performs an aggregation to fetch detailed TaxInvoice data filtered by itemCode and companyCode.
     * Joins with Company, Vendor, and Item details.
     *
     * @param itemCode    The specific item code to filter by.
     * @param companyCode The company code context for lookups.
     * @return A list containing the projected invoice details for the specified item.
     */
    @Aggregation(pipeline = {
            // Stage 1: Match based on itemCode and companyCode
            "{ $match: { itemCode: ?0, companyCode: ?1 } }", // ?0 = itemCode, ?1 = companyCode

            // Stage 2: Lookup Company Details
            "{ $lookup: { from: 'Company_Details', localField: 'companyCode', foreignField: 'companyCode', as: 'company' } }",
            // Stage 3: Unwind Company
            "{ $unwind: { path: '$company', preserveNullAndEmptyArrays: true } }",

            // Stage 4: Lookup Vendor Details (Matching vendorCode, vendorPlantCode from TaxInvoice, and companyCode param)
            "{ $lookup: { \n" +
                    "    from: 'Vendor_Details', \n" +
                    "    localField: 'vendorCode', \n" + // Match outer vendorCode
                    "    foreignField: 'vendorCode', \n" +
                    "    let: { outer_vendorPlantCode: '$vendorPlantCode' }, \n" + // Variable from Tax_Invoice
                    "    pipeline: [ \n" +
                    "        { $match: { \n" +
                    "            $expr: { \n" +
                    "                $and: [ \n" +
                    "                    { $eq: ['$companyCode', ?1] }, \n" + // companyCode parameter (?1)
                    "                    { $eq: ['$vendorPlantCode', '$$outer_vendorPlantCode'] } \n" + // Match plant code from Tax_Invoice
                    "                ] \n" +
                    "            } \n" +
                    "        } } \n" +
                    "    ], \n" +
                    "    as: 'vendor' \n" +
                    "} }",
            // Stage 5: Unwind Vendor
            "{ $unwind: { path: '$vendor', preserveNullAndEmptyArrays: true } }",

            // Stage 6: Lookup Item Details (Matching itemCode from TaxInvoice and companyCode param)
            // Note: We already matched on itemCode in Stage 1, but we still need the lookup to get item details
            "{ $lookup: { \n" +
                    "    from: 'Item_Details', \n" +
                    "    localField: 'itemCode', \n" + // Match outer itemCode
                    "    foreignField: 'itemCode', \n" +
                    "    pipeline: [ \n" +
                    "        { $match: { \n" +
                    "            $expr: { $eq: ['$companyCode', ?1] } \n" + // companyCode parameter (?1)
                    "        } } \n" +
                    "    ], \n" +
                    "    as: 'item' \n" +
                    "} }",
            // Stage 7: Unwind Item
            "{ $unwind: { path: '$item', preserveNullAndEmptyArrays: true } }",

            // Stage 8: Project the final structure (Same projection as other methods)
            "{ $project: { \n" +
                    "    \"_id\": 0, \n" +
                    "    \"invoiceNumber\": 1, \n" +
                    "    \"companyName\": \"$company.companyName\", \n" + "    \"companyAddress\": \"$company.companyAddress\", \n" + "    \"companyPanNo\": \"$company.companyPanNo\", \n" +
                    "    \"companyGstNo\": \"$company.companyGstNo\", \n" + "    \"companyStateCode\": \"$company.companyStateCode\", \n" + "    \"taxReverseCharge\": \"$company.taxReverseCharge\", \n" +
                    "    \"companyContactNo\": \"$company.companyContactNo\", \n" + "    \"companyEmail\": \"$company.companyEmail\", \n" + "    \"companyWebsite\": \"$company.companyWebsite\", \n" +
                    "    \"invoiceDate\": 1, \n" + "    \"vendorCode\": 1, \n" + "    \"vendorPlantCode\": 1, \n" + "    \"vendorName\": \"$vendor.vendorName\", \n" + "    \"vendorAddress\": \"$vendor.vendorAddress\", \n" +
                    "    \"vendorGstNo\": \"$vendor.vendorGstNo\", \n" + "    \"vendorCIN\": \"$vendor.vendorCIN\", \n" + "    \"vendorStateCode\": \"$vendor.vendorStateCode\", \n" +
                    "    \"vendorPAN\": \"$vendor.vendorPAN\", \n" +
                    "    \"hsnCode\": \"$item.hsnCode\", \n" +
                    "    \"sacCode\": 1, \n" +
                    "    \"itemCode\": \"$item.itemCode\", \n" +
                    "    \"poNo\": 1, \n" +
                    "    \"lineNo\": 1, \n" +
                    "    \"invoiceQuantity\": 1, \n" +
                    "    \"basicRate\": 1, \n" +
                    "    \"basicAmount\": 1, \n" +
                    "    \"freight\": 1, \n" +
                    "    \"othersCharges\": 1, \n" +
                    "    \"subTotal\": 1, \n" +
                    "    \"additionalTaxAmount\": 1, \n" +
                    "    \"taxableAmount\": 1, \n" +
                    "    \"cgstPercentage\": 1, \n" +
                    "    \"cgstAmount\": 1, \n" +
                    "    \"sgstPercentage\": 1, \n" +
                    "    \"sgstAmount\": 1, \n" +
                    "    \"igstPercentage\": 1, \n" +
                    "    \"igstAmount\": 1, \n" +
                    "    \"utgstPercentage\": 1, \n" +
                    "    \"utgstAmount\": 1, \n" +
                    "    \"totalAmount\": 1, \n" +
                    "    \"ewayBillNo\": 1, \n" +
                    "    \"ewayBillDate\": 1, \n" +
                    "    \"vehicleNo\": 1, \n" +
                    "    \"remark1\": \"$vendor.remark1\", \n" +
                    "    \"remark2\": \"$vendor.remark2\", \n" +
                    "    \"ediNumber\": 1, \n" +
                    "    \"shiftVendorCode\": 1, \n" +
                    "    \"itemRemark1\": \"$item.itemRemark1\", \n" +
                    "    \"itemRemark2\": \"$item.itemRemark2\", \n" +
                    "    \"signedQrCode\": 1, \n" +
                    "    \"deliveryChallanNo\": 1, \n" +
                    "    \"deliveryChallanDate\": 1, \n" +
                    "    \"deliveryChallanAmount\": 1, \n" +
                    "    \"tcsValue\": 1, \n" +
                    "    \"itemName\": \"$item.itemName\", \n" +
                    "    \"itemUnit\": \"$item.itemUnit\" \n" +
                    "} }",

            // Stage 9: Sort by invoice date (Optional, but good for consistency)
            "{ $sort: { \"invoiceDate\": 1 } }"
    })
    List<TaxInvoicePrintProjection> findProjectedInvoicesByItemCodeAndCompany(
            String itemCode,    // ?0
            String companyCode  // ?1
    );

    /**
     * Performs an aggregation to fetch detailed TaxInvoice data filtered by vendorCode and companyCode.
     * Joins with Company, Vendor, and Item details.
     *
     * @param vendorCode  The specific vendor code to filter by.
     * @param companyCode The company code context for lookups.
     * @return A list containing the projected invoice details for the specified vendor.
     */
    @Aggregation(pipeline = {
            // Stage 1: Match based on vendorCode and companyCode
            "{ $match: { vendorCode: ?0, companyCode: ?1 } }", // ?0 = vendorCode, ?1 = companyCode

            // Stage 2: Lookup Company Details
            "{ $lookup: { from: 'Company_Details', localField: 'companyCode', foreignField: 'companyCode', as: 'company' } }",
            // Stage 3: Unwind Company
            "{ $unwind: { path: '$company', preserveNullAndEmptyArrays: true } }",

            // Stage 4: Lookup Vendor Details (Matching vendorCode, vendorPlantCode from TaxInvoice, and companyCode param)
            "{ $lookup: { \n" +
                    "    from: 'Vendor_Details', \n" +
                    "    localField: 'vendorCode', \n" + // Match outer vendorCode
                    "    foreignField: 'vendorCode', \n" +
                    "    let: { outer_vendorPlantCode: '$vendorPlantCode' }, \n" + // Variable from Tax_Invoice
                    "    pipeline: [ \n" +
                    "        { $match: { \n" +
                    "            $expr: { \n" +
                    "                $and: [ \n" +
                    "                    { $eq: ['$companyCode', ?1] }, \n" + // companyCode parameter (?1)
                    "                    { $eq: ['$vendorPlantCode', '$$outer_vendorPlantCode'] } \n" + // Match plant code from Tax_Invoice
                    "                ] \n" +
                    "            } \n" +
                    "        } } \n" +
                    "    ], \n" +
                    "    as: 'vendor' \n" +
                    "} }",
            // Stage 5: Unwind Vendor
            "{ $unwind: { path: '$vendor', preserveNullAndEmptyArrays: true } }",

            // Stage 6: Lookup Item Details (Matching itemCode from TaxInvoice and companyCode param)
            // Note: We already matched on itemCode in Stage 1, but we still need the lookup to get item details
            "{ $lookup: { \n" +
                    "    from: 'Item_Details', \n" +
                    "    localField: 'itemCode', \n" + // Match outer itemCode
                    "    foreignField: 'itemCode', \n" +
                    "    pipeline: [ \n" +
                    "        { $match: { \n" +
                    "            $expr: { $eq: ['$companyCode', ?1] } \n" + // companyCode parameter (?1)
                    "        } } \n" +
                    "    ], \n" +
                    "    as: 'item' \n" +
                    "} }",
            // Stage 7: Unwind Item
            "{ $unwind: { path: '$item', preserveNullAndEmptyArrays: true } }",

            // Stage 8: Project the final structure (Same projection as other methods)
            "{ $project: { \n" +
                    "    \"_id\": 0, \n" +
                    "    \"invoiceNumber\": 1, \n" +
                    "    \"companyName\": \"$company.companyName\", \n" + "    \"companyAddress\": \"$company.companyAddress\", \n" + "    \"companyPanNo\": \"$company.companyPanNo\", \n" +
                    "    \"companyGstNo\": \"$company.companyGstNo\", \n" + "    \"companyStateCode\": \"$company.companyStateCode\", \n" + "    \"taxReverseCharge\": \"$company.taxReverseCharge\", \n" +
                    "    \"companyContactNo\": \"$company.companyContactNo\", \n" + "    \"companyEmail\": \"$company.companyEmail\", \n" + "    \"companyWebsite\": \"$company.companyWebsite\", \n" +
                    "    \"invoiceDate\": 1, \n" + "    \"vendorCode\": 1, \n" + "    \"vendorPlantCode\": 1, \n" + "    \"vendorName\": \"$vendor.vendorName\", \n" + "    \"vendorAddress\": \"$vendor.vendorAddress\", \n" +
                    "    \"vendorGstNo\": \"$vendor.vendorGstNo\", \n" + "    \"vendorCIN\": \"$vendor.vendorCIN\", \n" + "    \"vendorStateCode\": \"$vendor.vendorStateCode\", \n" +
                    "    \"vendorPAN\": \"$vendor.vendorPAN\", \n" +
                    "    \"hsnCode\": \"$item.hsnCode\", \n" +
                    "    \"sacCode\": 1, \n" +
                    "    \"itemCode\": \"$item.itemCode\", \n" +
                    "    \"poNo\": 1, \n" +
                    "    \"lineNo\": 1, \n" +
                    "    \"invoiceQuantity\": 1, \n" +
                    "    \"basicRate\": 1, \n" +
                    "    \"basicAmount\": 1, \n" +
                    "    \"freight\": 1, \n" +
                    "    \"othersCharges\": 1, \n" +
                    "    \"subTotal\": 1, \n" +
                    "    \"additionalTaxAmount\": 1, \n" +
                    "    \"taxableAmount\": 1, \n" +
                    "    \"cgstPercentage\": 1, \n" +
                    "    \"cgstAmount\": 1, \n" +
                    "    \"sgstPercentage\": 1, \n" +
                    "    \"sgstAmount\": 1, \n" +
                    "    \"igstPercentage\": 1, \n" +
                    "    \"igstAmount\": 1, \n" +
                    "    \"utgstPercentage\": 1, \n" +
                    "    \"utgstAmount\": 1, \n" +
                    "    \"totalAmount\": 1, \n" +
                    "    \"ewayBillNo\": 1, \n" +
                    "    \"ewayBillDate\": 1, \n" +
                    "    \"vehicleNo\": 1, \n" +
                    "    \"remark1\": \"$vendor.remark1\", \n" +
                    "    \"remark2\": \"$vendor.remark2\", \n" +
                    "    \"ediNumber\": 1, \n" +
                    "    \"shiftVendorCode\": 1, \n" +
                    "    \"itemRemark1\": \"$item.itemRemark1\", \n" +
                    "    \"itemRemark2\": \"$item.itemRemark2\", \n" +
                    "    \"signedQrCode\": 1, \n" +
                    "    \"deliveryChallanNo\": 1, \n" +
                    "    \"deliveryChallanDate\": 1, \n" +
                    "    \"deliveryChallanAmount\": 1, \n" +
                    "    \"tcsValue\": 1, \n" +
                    "    \"itemName\": \"$item.itemName\", \n" +
                    "    \"itemUnit\": \"$item.itemUnit\" \n" +
                    "} }",

            // Stage 9: Sort by invoice date (Optional, but good for consistency)
            "{ $sort: { \"invoiceDate\": 1 } }"
    })
    List<TaxInvoicePrintProjection> findProjectedInvoicesByVendorCodeAndCompany(
            String vendorCode,    // ?0
            String companyCode  // ?1
    );

}
