
package com.example.unifiedapi.controller;

import com.example.unifiedapi.dto.TtnConsultEfactRequest;
import com.example.unifiedapi.dto.TtnConsultEfactResponse;
import com.example.unifiedapi.service.TtnOperationsService;
import com.example.unifiedapi.service.TtnTransformationService;
import com.example.unifiedapi.service.UserCredentialsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestController
@RequestMapping("/transform-ttn-invoice")
@CrossOrigin(origins = "*")
public class TtnTransformController {
    private static final Logger logger = LoggerFactory.getLogger(TtnTransformController.class);

    private final TtnOperationsService ttnOperationsService;
    private final TtnTransformationService ttnTransformationService;
    private final UserCredentialsService userCredentialsService;

    @Autowired
    public TtnTransformController(TtnOperationsService ttnOperationsService, TtnTransformationService ttnTransformationService, UserCredentialsService userCredentialsService) {
        this.ttnOperationsService = ttnOperationsService;
        this.ttnTransformationService = ttnTransformationService;
        this.userCredentialsService = userCredentialsService;
    }

    @GetMapping
    public ResponseEntity<?> transformTtnInvoice(
        @RequestParam String ttnInvoiceId,
        @RequestParam(required = false) String filename
    ) {
    logger.info("[TTN TRANSFORM] Received request for ttnInvoiceId={} filename={}", ttnInvoiceId, filename);
    Map<String, Object> result = new HashMap<>();
        try {
            // 1. Get TTN credentials for the current user
            String ttnUsername = userCredentialsService.getTtnUsername();
            String ttnPassword = userCredentialsService.getTtnPassword();
            String ttnMatriculeFiscal = userCredentialsService.getTtnMatriculeFiscal();

            // 2. Consult TTN for the invoice
            TtnConsultEfactRequest consultRequest = new TtnConsultEfactRequest();
            consultRequest.setUsername(ttnUsername);
            consultRequest.setPassword(ttnPassword);
            consultRequest.setMatriculeFiscal(ttnMatriculeFiscal);
            consultRequest.setCriteria(ttnInvoiceId);

            TtnConsultEfactResponse consultResponse = ttnOperationsService.consultEfact(consultRequest);
            String rawResponse = consultResponse.getRawResponse();

            // 3. Extract <xmlContent> from the SOAP response
            String xmlContent = com.example.unifiedapi.util.XmlUtils.extractXmlContent(rawResponse);
            logger.info("xml----------------------------------------",xmlContent);
            if (xmlContent == null || xmlContent.isEmpty()) {
                result.put("success", false);
                result.put("message", "xmlContent not available yet");
                return ResponseEntity.ok(result);
            }

            // 4. Transform to HTML
            String html = ttnTransformationService.transformXmlToHtml(
                    xmlContent, ttnUsername, ttnPassword, ttnMatriculeFiscal, filename != null ? filename : ttnInvoiceId
            );
            // 5. Return a link or the HTML content (for demo, return HTML directly)
            result.put("success", true);
            result.put("html", html);
            // Optionally, you could save the HTML and return a URL
            return ResponseEntity.ok(result);
        } catch (Exception e) {
                        logger.error("[TTN TRANSFORM] Error processing ttnInvoiceId={}", ttnInvoiceId, e);

            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
