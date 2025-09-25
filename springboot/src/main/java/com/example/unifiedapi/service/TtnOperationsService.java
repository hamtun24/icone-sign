package com.example.unifiedapi.service;

import com.example.unifiedapi.dto.TtnSaveEfactRequest;
import com.example.unifiedapi.dto.TtnConsultEfactRequest;
import com.example.unifiedapi.dto.TtnSaveEfactResponse;
import com.example.unifiedapi.dto.TtnConsultEfactResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TtnOperationsService {
    
    private static final Logger logger = LoggerFactory.getLogger(TtnOperationsService.class);
    
    @Value("${ttn.soap.url}")
    private String soapUrl;
    
    private final RestTemplate restTemplate;
    private final OperationLogService operationLogService;
    
    @Autowired
    public TtnOperationsService(RestTemplate restTemplate, OperationLogService operationLogService) {
        this.restTemplate = restTemplate;
        this.operationLogService = operationLogService;
    }
    
    public TtnSaveEfactResponse saveEfact(TtnSaveEfactRequest request, List<MultipartFile> files) {
        logger.info("Processing saveEfact request for user: {}, matricule: {}, files: {}", 
                   request.getUsername(), request.getMatriculeFiscal(), files.size());
        
        TtnSaveEfactResponse response = new TtnSaveEfactResponse();
        response.setSuccess(true);
        response.setTotalProcessed(files.size());
        response.setResults(new ArrayList<>());
        
        int successCount = 0;
        int errorCount = 0;
        
        for (MultipartFile file : files) {
            TtnSaveEfactResponse.FileResult fileResult = new TtnSaveEfactResponse.FileResult();
            fileResult.setFilename(file.getOriginalFilename());
            
            try {
                logger.info("Processing file: {}", file.getOriginalFilename());
                
                // Convert file to base64
                String base64Content = Base64.getEncoder().encodeToString(file.getBytes());
                
                // Create SOAP request
                String soapRequest = createSaveEfactSoapRequest(
                    request.getUsername(), 
                    request.getPassword(), 
                    request.getMatriculeFiscal(), 
                    base64Content
                );
                
                // Send SOAP request
                String soapResponse = sendSoapRequest(soapRequest);
                logger.debug("SOAP response for file {}: {}", file.getOriginalFilename(), soapResponse);
                
                // Parse response
                String reference = extractReferenceFromResponse(soapResponse);
                
                fileResult.setSuccess(true);
                fileResult.setReference(reference);
                fileResult.setRawResponse(soapResponse);
                successCount++;
                
                // Log successful operation
                operationLogService.logOperationWithDetails(
                    "TTN_SAVE", "SUCCESS", file.getSize(), file.getOriginalFilename(),
                    "File processed successfully", request.getUsername(), 
                    request.getMatriculeFiscal(), null
                );
                
                logger.info("File {} processed successfully with reference: {}", 
                           file.getOriginalFilename(), reference);
                
            } catch (Exception e) {
                logger.error("Error processing file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                
                fileResult.setSuccess(false);
                fileResult.setError(e.getMessage());
                errorCount++;
                
                // Log failed operation
                operationLogService.logOperationWithDetails(
                    "TTN_SAVE", "FAILURE", file.getSize(), file.getOriginalFilename(),
                    "File processing failed", request.getUsername(), 
                    request.getMatriculeFiscal(), e.getMessage()
                );
            }
            
            response.getResults().add(fileResult);
        }
        
        response.setSuccessCount(successCount);
        response.setErrorCount(errorCount);
        
        logger.info("SaveEfact completed. Total: {}, Success: {}, Errors: {}", 
                   files.size(), successCount, errorCount);
        
        return response;
    }
    
    public TtnConsultEfactResponse consultEfact(TtnConsultEfactRequest request) {
        logger.info("Processing consultEfact request for user: {}, matricule: {}", 
                   request.getUsername(), request.getMatriculeFiscal());
        
        try {
            // Create SOAP request
            String soapRequest = createConsultEfactSoapRequest(
                request.getUsername(), 
                request.getPassword(), 
                request.getMatriculeFiscal(), 
                request.getCriteria()
            );
            logger.info("idSaveEfact criteria: {}", request.getCriteria());
            // Wait 5 seconds before sending the SOAP request
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting before consultEfact request");
            }
            // Send SOAP request
            String soapResponse = sendSoapRequest(soapRequest);
            logger.debug("SOAP response for consultEfact: {}", soapResponse);
            
            // Parse response
            TtnConsultEfactResponse response = parseConsultEfactResponse(soapResponse);
            response.setRawResponse(soapResponse);
            
            // Log successful operation
            operationLogService.logOperationWithDetails(
                "TTN_CONSULT", "SUCCESS", null, null,
                "Consult operation completed", request.getUsername(), 
                request.getMatriculeFiscal(), null
            );
            
            logger.info("ConsultEfact completed successfully");
            return response;
            
        } catch (Exception e) {
            logger.error("Error in consultEfact: {}", e.getMessage(), e);
            
            // Log failed operation
            operationLogService.logOperationWithDetails(
                "TTN_CONSULT", "FAILURE", null, null,
                "Consult operation failed", request.getUsername(), 
                request.getMatriculeFiscal(), e.getMessage()
            );
            
            TtnConsultEfactResponse response = new TtnConsultEfactResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());
            return response;
        }
    }
    
    private String createSaveEfactSoapRequest(String username, String password, String matriculeFiscal, String base64Content) {
        return String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
            "xmlns:ser=\"http://services.elfatoura.tradenet.com.tn/\">" +
            "<soap:Header/>" +
            "<soap:Body>" +
            "<ser:saveEfact>" +
            "<arg0>%s</arg0>" +
            "<arg1>%s</arg1>" +
            "<arg2>%s</arg2>" +
            "<arg3>%s</arg3>" +
            "</ser:saveEfact>" +
            "</soap:Body>" +
            "</soap:Envelope>",
            username, password, matriculeFiscal, base64Content
        );
    }
    
    private String createConsultEfactSoapRequest(String username, String password, String matriculeFiscal, Object criteria) {
        // Serialize criteria to proper XML format
        String criteriaXml = serializeCriteriaToXml(criteria);

        logger.debug("Generated criteria XML: {}", criteriaXml);

        return String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
            "xmlns:ser=\"http://services.elfatoura.tradenet.com.tn/\">" +
            "<soapenv:Header/>" +
            "<soapenv:Body>" +
            "<ser:consultEfact>" +
            "<!--Optional:-->" +
            "<arg0>%s</arg0>" +
            "<!--Optional:-->" +
            "<arg1>%s</arg1>" +
            "<!--Optional:-->" +
            "<arg2>%s</arg2>" +
            "<arg3>%s</arg3>" +
            "</ser:consultEfact>" +
            "</soapenv:Body>" +
            "</soapenv:Envelope>",
            username, password, matriculeFiscal, criteriaXml
        );
    }

    /**
     * Serialize criteria object to XML format expected by TTN service
     * Based on real SOAP UI format: criteria elements directly inside <arg3>
     */
    private String serializeCriteriaToXml(Object criteria) {
        if (criteria == null) {
            // Return empty string for no criteria
            return "";
        }

        StringBuilder xmlBuilder = new StringBuilder();

        try {
            if (criteria instanceof Map) {
                Map<?, ?> criteriaMap = (Map<?, ?>) criteria;

                for (Map.Entry<?, ?> entry : criteriaMap.entrySet()) {
                    String key = entry.getKey().toString();
                    Object value = entry.getValue();

                    if (value != null && !value.toString().trim().isEmpty()) {
                        // Direct XML elements without wrapper, matching SOAP UI format
                        xmlBuilder.append("<").append(key).append(">")
                                 .append(escapeXml(value.toString()))
                                 .append("</").append(key).append(">");
                    }
                }
            } else if (criteria instanceof String) {
                // If it's a String, wrap in <idSaveEfact>
                xmlBuilder.append("<idSaveEfact>").append(escapeXml(criteria.toString())).append("</idSaveEfact>");
            } else {
                // If it's not a Map or String, try to convert it to a simple element
                logger.warn("Criteria is not a Map, attempting to serialize: {}", criteria.getClass().getSimpleName());
                xmlBuilder.append("<data>").append(escapeXml(criteria.toString())).append("</data>");
            }
        } catch (Exception e) {
            logger.error("Error serializing criteria to XML: {}", e.getMessage(), e);
            xmlBuilder.append("<error>Failed to serialize criteria</error>");
        }

        return xmlBuilder.toString();
    }

    /**
     * Parse the SOAP response from consultEfact operation
     */
    private TtnConsultEfactResponse parseConsultEfactResponse(String soapResponse) {
        TtnConsultEfactResponse response = new TtnConsultEfactResponse();

        try {
            // Check if it's a SOAP fault
            if (soapResponse.contains("<S:Fault") || soapResponse.contains("<soap:Fault")) {
                response.setSuccess(false);

                // Extract fault message
                Pattern faultPattern = Pattern.compile("<faultMessage>([^<]+)</faultMessage>");
                Matcher faultMatcher = faultPattern.matcher(soapResponse);

                if (faultMatcher.find()) {
                    response.setError(faultMatcher.group(1));
                } else {
                    response.setError("SOAP Fault occurred");
                }

                response.setCount(0);
                response.setInvoices(new ArrayList<>());
                return response;
            }

            // Parse successful response
            if (soapResponse.contains("<ns2:consultEfactResponse") || soapResponse.contains("consultEfactResponse")) {
                response.setSuccess(true);

                // Count the number of <return> elements (each represents an invoice)
                Pattern returnPattern = Pattern.compile("<return>.*?</return>", Pattern.DOTALL);
                Matcher returnMatcher = returnPattern.matcher(soapResponse);

                List<Object> invoices = new ArrayList<>();
                int count = 0;

                while (returnMatcher.find()) {
                    count++;
                    String returnXml = returnMatcher.group();

                    // Parse individual invoice data
                    Map<String, Object> invoice = parseInvoiceFromXml(returnXml);
                    invoices.add(invoice);
                }

                response.setCount(count);
                response.setInvoices(invoices);

                logger.info("Successfully parsed {} invoices from consultEfact response", count);
            } else {
                response.setSuccess(false);
                response.setError("Unexpected response format");
                response.setCount(0);
                response.setInvoices(new ArrayList<>());
            }

        } catch (Exception e) {
            logger.error("Error parsing consultEfact response: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setError("Failed to parse response: " + e.getMessage());
            response.setCount(0);
            response.setInvoices(new ArrayList<>());
        }

        return response;
    }

    /**
     * Parse individual invoice data from XML return element
     */
    private Map<String, Object> parseInvoiceFromXml(String returnXml) {
        Map<String, Object> invoice = new HashMap<>();

        try {
            // Extract common fields
            invoice.put("amount", extractXmlValue(returnXml, "amount"));
            invoice.put("amountTax", extractXmlValue(returnXml, "amountTax"));
            invoice.put("dateDocument", extractXmlValue(returnXml, "dateDocument"));
            invoice.put("dateProcess", extractXmlValue(returnXml, "dateProcess"));
            invoice.put("documentNumber", extractXmlValue(returnXml, "documentNumber"));
            invoice.put("documentType", extractXmlValue(returnXml, "documentType"));

            // Extract acknowledgments and errors if present
            if (returnXml.contains("<listAcknowlegments>")) {
                Map<String, Object> acknowledgments = new HashMap<>();
                acknowledgments.put("dateAck", extractXmlValue(returnXml, "dateAck"));

                // Extract errors
                if (returnXml.contains("<errors>")) {
                    Map<String, Object> errors = new HashMap<>();
                    errors.put("errorDescription", extractXmlValue(returnXml, "errorDescription"));
                    errors.put("errorId", extractXmlValue(returnXml, "errorId"));
                    acknowledgments.put("errors", errors);
                }

                invoice.put("acknowledgments", acknowledgments);
            }

            // Set ID and reference for compatibility
            invoice.put("id", extractXmlValue(returnXml, "documentNumber"));
            invoice.put("reference", extractXmlValue(returnXml, "documentNumber"));
            invoice.put("status", extractXmlValue(returnXml, "documentType"));
            invoice.put("date", extractXmlValue(returnXml, "dateDocument"));

        } catch (Exception e) {
            logger.error("Error parsing invoice XML: {}", e.getMessage(), e);
            invoice.put("error", "Failed to parse invoice data");
        }

        return invoice;
    }

    /**
     * Extract value from XML element
     */
    private String extractXmlValue(String xml, String elementName) {
        Pattern pattern = Pattern.compile("<" + elementName + ">([^<]*)</" + elementName + ">");
        Matcher matcher = pattern.matcher(xml);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * Escape XML special characters
     */
    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
    
    private String sendSoapRequest(String soapRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        headers.set("SOAPAction", "");
        
        HttpEntity<String> entity = new HttpEntity<>(soapRequest, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(soapUrl, entity, String.class);
        
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("SOAP request failed with status: " + response.getStatusCode());
        }
    }
    
    private String extractReferenceFromResponse(String soapResponse) {
        // Extract reference from SOAP response
        // Pattern: "Facture enregistree avec ID 1805137 est en cours de validation"
        Pattern pattern = Pattern.compile("<return>(.*?)</return>");
        Matcher matcher = pattern.matcher(soapResponse);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return soapResponse; // Return full response if pattern not found
    }
}
