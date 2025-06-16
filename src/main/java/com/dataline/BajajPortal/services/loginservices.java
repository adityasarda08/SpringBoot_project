package com.dataline.BajajPortal.services;

import com.dataline.BajajPortal.model.master.CompanyDetails;
import com.dataline.BajajPortal.repository.CompanyRepository;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Service
public class loginservices {

    private static final Logger logger = LoggerFactory.getLogger(loginservices.class);
    private static final String COMPANY_CODE_KEY = "companyCode";
    private static final String IS_LOGGED_IN_KEY = "isLoggedIn";

    @Autowired
    private CompanyRepository companyRepository;

    // Helper method to get current session
    private HttpSession getSession() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attr.getRequest().getSession(true);
    }

    /**
     * Verify company credentials and store in session if valid
     */
    public String verifyCompany(String companyCode, String password) {

        try {
            List<CompanyDetails> companyDetailslist = companyRepository.findByCompanyCode(companyCode);

            if (companyDetailslist.isEmpty()) {
                System.out.println("Company data not found ");

                return "Company not found";
            } else {
                CompanyDetails companyDetails = companyDetailslist.get(0);
                System.out.println("Db Company Code " + companyDetails.getCompanyCode());
                System.out.println("Db Company password " + companyDetails.getPassword());

                if (companyDetailslist.get(0).getPassword().equals(password)) {

                    HttpSession session = getSession();
                    session.setAttribute(COMPANY_CODE_KEY, companyCode);
                    session.setAttribute(IS_LOGGED_IN_KEY, true);
                    return "Logged in successfully";
                } else {
                    return "Incorrect password";
                }
            }

        } catch (Exception e) {
            logger.error("Error verifying company: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get company details by company code
     */
    public CompanyDetails getCompanyDetails() {
        String companyCode = (String) getSession().getAttribute(COMPANY_CODE_KEY);
        return companyRepository.findByCompanyCode(companyCode).isEmpty() ? null : companyRepository.findByCompanyCode(companyCode).get(0);
    }

    /**
     * Get current company code
     */
    public String getCurrentCompanyCode() {
        return (String) getSession().getAttribute(COMPANY_CODE_KEY);
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        HttpSession session = getSession();
        return session.getAttribute(IS_LOGGED_IN_KEY) != null &&
                (Boolean) session.getAttribute(IS_LOGGED_IN_KEY);
    }

}