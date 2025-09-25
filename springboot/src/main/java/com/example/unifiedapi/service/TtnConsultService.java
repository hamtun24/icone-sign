package com.example.unifiedapi.service;

import com.example.unifiedapi.dto.TtnConsultEfactRequest;
import com.example.unifiedapi.dto.TtnConsultEfactResponse;
import com.example.unifiedapi.dto.TtnConsultHtmlRequest;
import com.example.unifiedapi.dto.TtnConsultHtmlResponse;
import com.example.unifiedapi.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

/**
 * Service for TTN consultation with HTML transformation
 */
@Service
public class TtnConsultService {
    
    private static final Logger logger = LoggerFactory.getLogger(TtnConsultService.class);
    
    private final TtnOperationsService ttnOperationsService;
    private final TtnTransformationService ttnTransformationService;
    private final UserCredentialsService userCredentialsService;
    private final OperationLogService operationLogService;
    
    @Autowired
    public TtnConsultService(TtnOperationsService ttnOperationsService,
                            TtnTransformationService ttnTransformationService,
                            UserCredentialsService userCredentialsService,
                            OperationLogService operationLogService) {
        this.ttnOperationsService = ttnOperationsService;
        this.ttnTransformationService = ttnTransformationService;
        this.userCredentialsService = userCredentialsService;
        this.operationLogService = operationLogService;
    }
    
    /**
     * Consult TTN e-facturation system and transform results to HTML
     */
    public TtnConsultHtmlResponse consultWithHtml(TtnConsultHtmlRequest request) {
      
        
        TtnConsultHtmlResponse response = new TtnConsultHtmlResponse();
        
        try {
            // Get user credentials
            String ttnUsername = userCredentialsService.getTtnUsername();
            String ttnPassword = userCredentialsService.getTtnPassword();
            String ttnMatriculeFiscal = userCredentialsService.getTtnMatriculeFiscal();
            
            // Create TTN consult request
            TtnConsultEfactRequest ttnRequest = new TtnConsultEfactRequest(
                ttnUsername, ttnPassword, ttnMatriculeFiscal, request.getCriteria()
            );
            
            // Perform consultation
            logger.info("Consulting TTN e-facturation system...");
            TtnConsultEfactResponse ttnResponse = ttnOperationsService.consultEfact(ttnRequest);
            
            if (!ttnResponse.isSuccess()) {
                logger.warn("TTN consultation failed: {}", ttnResponse.getError());
                response.setSuccess(false);
                response.setError(ttnResponse.getError());
                response.setRawResponse(ttnResponse.getRawResponse());
                return response;
            }
            
            // Copy basic response data
            response.setSuccess(true);
            response.setCount(ttnResponse.getCount());
            response.setInvoices(ttnResponse.getInvoices());
            response.setRawResponse(ttnResponse.getRawResponse());
            
            // Transform to HTML if invoices found and XML content is available
            if (ttnResponse.getCount() > 0 && shouldTransformToHtml(request)) {
                logger.info("Attempting HTML transformation for {} invoices", ttnResponse.getCount());
                
                try {
                    String htmlContent = transformInvoicesToHtml(
                        ttnResponse, ttnUsername, ttnPassword, ttnMatriculeFiscal, request
                    );
                    response.setHtmlContent(htmlContent);
                    logger.info("HTML transformation completed successfully");
                } catch (Exception htmlError) {
                    logger.warn("HTML transformation failed, continuing without HTML: {}", htmlError.getMessage());
                    response.setHtmlTransformationError("HTML transformation failed: " + htmlError.getMessage());
                }
            }
            
            // Log successful operation
            operationLogService.logOperationWithDetails(
                "TTN_CONSULT_HTML", "SUCCESS", null, null,
                "TTN consultation with HTML completed successfully",
                ttnUsername, ttnMatriculeFiscal, null
            );
            
            logger.info("TTN consultation with HTML completed successfully - {} invoices found", 
                       response.getCount());
            
        } catch (Exception e) {
            logger.error("Error in TTN consultation with HTML: {}", e.getMessage(), e);
            
            response.setSuccess(false);
            response.setError("Consultation failed: " + e.getMessage());
            
            // Log failed operation
            try {
                String ttnUsername = userCredentialsService.getTtnUsername();
                String ttnMatriculeFiscal = userCredentialsService.getTtnMatriculeFiscal();
                
                operationLogService.logOperationWithDetails(
                    "TTN_CONSULT_HTML", "FAILURE", null, null,
                    "TTN consultation with HTML failed",
                    ttnUsername, ttnMatriculeFiscal, e.getMessage()
                );
            } catch (Exception logError) {
                logger.warn("Failed to log operation details: {}", logError.getMessage());
            }
        }
        
        return response;
    }
    
    /**
     * Determine if HTML transformation should be attempted
     */
    private boolean shouldTransformToHtml(TtnConsultHtmlRequest request) {
        // Transform to HTML if specific invoice ID is requested (single invoice scenario)
        if (request.getCriteria() instanceof Map) {
            Map<?, ?> criteria = (Map<?, ?>) request.getCriteria();
            return criteria.containsKey("idSaveEfact") && 
                   criteria.get("idSaveEfact") != null && 
                   !criteria.get("idSaveEfact").toString().trim().isEmpty();
        }
        return false;
    }
    
    /**
     * Transform invoice data to HTML using TTN consultation and transformation services
     */
    private String transformInvoicesToHtml(TtnConsultEfactResponse ttnResponse, 
                                          String ttnUsername, String ttnPassword, 
                                          String ttnMatriculeFiscal, TtnConsultHtmlRequest request) 
                                          throws Exception {
        
        // Extract idSaveEfact from criteria for individual invoice consultation
        String idSaveEfact = null;
        if (request.getCriteria() instanceof Map) {
            Map<?, ?> criteria = (Map<?, ?>) request.getCriteria();
            Object id = criteria.get("idSaveEfact");
            if (id != null) {
                idSaveEfact = id.toString().trim();
            }
        }
        
        if (idSaveEfact == null || idSaveEfact.isEmpty()) {
            throw new RuntimeException("idSaveEfact is required for HTML transformation");
        }
        
        logger.info("Transforming invoice with ID: {} to HTML", idSaveEfact);
        
        // Create specific request for the invoice ID to get XML content
        TtnConsultEfactRequest specificRequest = new TtnConsultEfactRequest(
            ttnUsername, ttnPassword, ttnMatriculeFiscal, idSaveEfact
        );
        
        // Retry mechanism to get XML content (similar to InvoiceFileProcessor)
        String base64XmlContent = null;
      
      
            try {
                TtnConsultEfactResponse specificResponse = ttnOperationsService.consultEfact(specificRequest);
                
                if (specificResponse.isSuccess()) {
                    String rawResponse = specificResponse.getRawResponse();
                    base64XmlContent = extractXmlContentFromResponse(rawResponse);
                    
                    if (base64XmlContent != null && !base64XmlContent.trim().isEmpty()) {
                        logger.info("Successfully extracted XML content for invoice: {}", idSaveEfact);
                        
                    }
                }
            } catch (Exception e) {
                logger.error("Error extracting xmlContent: {}", e.getMessage(), e);
            }
               
                
           
        
    
        
        // Transform to HTML using the transformation service
        String htmlContent = ttnTransformationService.transformXmlToHtml(
            base64XmlContent, ttnUsername, ttnPassword, ttnMatriculeFiscal, 
            "invoice_" + idSaveEfact + ".xml"
        );
        
        logger.info("HTML transformation completed for invoice: {}", idSaveEfact);
        return htmlContent;
    }
    
    /**
     * Extract XML content from TTN SOAP response
     */
    private String extractXmlContentFromResponse(String rawResponse) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(rawResponse.getBytes()));
            XPath xpath = XPathFactory.newInstance().newXPath();
            String expression = "//*[local-name()='xmlContent']";
            String xmlContent = (String) xpath.evaluate(expression, doc, XPathConstants.STRING);
            
            return (xmlContent != null && !xmlContent.trim().isEmpty()) ? xmlContent.trim() : null;
        } catch (Exception e) {
            logger.warn("Failed to extract XML content from response: {}", e.getMessage());
            return null;
        }
    }
}