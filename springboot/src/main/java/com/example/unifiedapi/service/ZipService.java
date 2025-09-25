package com.example.unifiedapi.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ZipService {
    
    private static final Logger logger = LoggerFactory.getLogger(ZipService.class);
    
    private static final String TEMP_DIR = "temp/downloads";
    private static final String ZIP_BASE_NAME = "processed_invoices";
    
    /**
     * Create a ZIP file containing all processed files
     * 
     * @param signedXmlFiles Map of filename to signed XML content
     * @param validationReports Map of filename to validation report JSON
     * @param htmlFiles Map of filename to HTML content
     * @param username User identifier for the ZIP file name
     * @return Path to the created ZIP file
     * @throws IOException if ZIP creation fails
     */
    public Path createProcessedFilesZip(Map<String, String> signedXmlFiles,
                                      Map<String, String> validationReports,
                                      Map<String, String> htmlFiles,
                                      String username) throws IOException {
        
        logger.info("Creating ZIP file for {} signed files, {} validation reports, {} HTML files", 
                   signedXmlFiles.size(), validationReports.size(), htmlFiles.size());
        
        // Create temp directory if it doesn't exist
        Path tempDir = Paths.get(TEMP_DIR);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }
        
        // Generate unique ZIP filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String zipFileName = String.format("%s_%s_%s.zip", ZIP_BASE_NAME, username, timestamp);
        Path zipPath = tempDir.resolve(zipFileName);
        
        try (FileOutputStream fos = new FileOutputStream(zipPath.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            // Add signed XML files
            addFilesToZip(zos, signedXmlFiles, "Factures_Signees/", ".xml");
            
            // Add validation reports
            addFilesToZip(zos, validationReports, "Rapport_De_Validation/", ".json");
            
            // Add HTML files
            addFilesToZip(zos, htmlFiles, "Facture_Pdf/", ".html");
            
            // Add summary file
            addSummaryFile(zos, signedXmlFiles.size(), validationReports.size(), htmlFiles.size(), username);
            
            logger.info("ZIP file created successfully: {}", zipPath);
            
        } catch (IOException e) {
            logger.error("Failed to create ZIP file: {}", e.getMessage(), e);
            // Clean up partial file
            try {
                Files.deleteIfExists(zipPath);
            } catch (IOException cleanupEx) {
                logger.warn("Failed to clean up partial ZIP file: {}", cleanupEx.getMessage());
            }
            throw e;
        }
        
        return zipPath;
    }

    /**
     * Creates a ZIP file containing all processed files and reports, including error reports
     *
     * @param signedXmlFiles Map of filename to signed XML content
     * @param validationReports Map of filename to validation report JSON
     * @param htmlFiles Map of filename to HTML content
     * @param errorReports Map of filename to error report content
     * @param username User identifier for the ZIP file name
     * @return Path to the created ZIP file
     * @throws IOException if ZIP creation fails
     */
    public Path createProcessedFilesZipWithErrors(Map<String, String> signedXmlFiles,
                                                Map<String, String> validationReports,
                                                Map<String, String> htmlFiles,
                                                Map<String, String> errorReports,
                                                Map<String, String> ttnInvoiceIds,
                                                String username) throws IOException {

        logger.info("Creating ZIP file for {} signed files, {} validation reports, {} HTML files, {} error reports",
                   signedXmlFiles.size(), validationReports.size(), htmlFiles.size(), errorReports.size());

        // Create temp directory if it doesn't exist
        Path tempDir = Paths.get(TEMP_DIR);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String zipFileName = String.format("invoice_processing_%s_%s.zip", username, timestamp);
        Path zipPath = tempDir.resolve(zipFileName);

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {

            // Add signed XML files
            if (!signedXmlFiles.isEmpty()) {
                addFilesToZip(zos, signedXmlFiles, "Facture_Signees/", ".xml");
            }

            // Add validation reports
            if (!validationReports.isEmpty()) {
                addFilesToZip(zos, validationReports, "Rapport_de_validation/", ".json");
            }

            // Add HTML files
            if (!htmlFiles.isEmpty()) {
                addFilesToZip(zos, htmlFiles, "Facture_pdf/", ".html");
            }

            // Add error reports
            if (!errorReports.isEmpty()) {
                addFilesToZip(zos, errorReports, "Rapport_des_erreurs/", ".txt");
            }

            // Add summary report
            String summaryReport = createSummaryReportWithErrors(signedXmlFiles, validationReports, htmlFiles, errorReports, ttnInvoiceIds, username);
            ZipEntry summaryEntry = new ZipEntry("PROCESSING_SUMMARY.txt");
            zos.putNextEntry(summaryEntry);
            zos.write(summaryReport.getBytes("UTF-8"));
            zos.closeEntry();

            logger.info("Successfully created ZIP file with all results: {}", zipPath);
        }

        return zipPath;
    }

    /**
     * Add files to ZIP with specified directory and extension
     */
    private void addFilesToZip(ZipOutputStream zos, Map<String, String> files,
                              String directory, String extension) throws IOException {

        for (Map.Entry<String, String> entry : files.entrySet()) {
            String originalFilename = entry.getKey();
            String content = entry.getValue();

            // Clean filename and remove original extension, then add new one
            String cleanFilename = cleanFilename(originalFilename);
            String baseFilename = removeExtension(cleanFilename);
            String zipEntryName = directory + baseFilename + extension;
            
            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            zos.putNextEntry(zipEntry);
            
            byte[] contentBytes = content.getBytes("UTF-8");
            zos.write(contentBytes);
            zos.closeEntry();
            
            logger.debug("Added file to ZIP: {}", zipEntryName);
        }
    }
    
    /**
     * Add a summary file to the ZIP
     */
    private void addSummaryFile(ZipOutputStream zos, int xmlCount, int reportCount, 
                               int htmlCount, String username) throws IOException {
        
        String summary = createSummaryContent(xmlCount, reportCount, htmlCount, username);
        
        ZipEntry summaryEntry = new ZipEntry("PROCESSING_SUMMARY.txt");
        zos.putNextEntry(summaryEntry);
        zos.write(summary.getBytes("UTF-8"));
        zos.closeEntry();
        
        logger.debug("Added summary file to ZIP");
    }
    
    /**
     * Create summary content
     */
    private String createSummaryContent(int xmlCount, int reportCount, int htmlCount, String username) {
    StringBuilder summary = new StringBuilder();
    summary.append("=== RÉSUMÉ DU TRAITEMENT DES FACTURES ===\n\n");
    summary.append("Date de traitement : ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
    summary.append("Utilisateur : ").append(username).append("\n\n");

    summary.append("Fichiers traités :\n");
    summary.append("- Fichiers XML signés : ").append(xmlCount).append("\n");
    summary.append("- Rapports de validation : ").append(reportCount).append("\n");
    summary.append("- Aperçus HTML : ").append(htmlCount).append("\n\n");

    summary.append("Structure du répertoire :\n");
    summary.append("├── signed_xml/          (Factures XML signées numériquement)\n");
    summary.append("├── validation_reports/  (Rapports de validation de signature XML)\n");
    summary.append("├── html_previews/       (Aperçu HTML des factures)\n");
    summary.append("└── PROCESSING_SUMMARY.txt (Ce fichier)\n\n");

    summary.append("Étapes du traitement :\n");
    summary.append("1. Les fichiers XML ont été signés numériquement avec ANCE SEAL\n");
    summary.append("2. Les fichiers signés ont été sauvegardés dans le système TTN e-facturation\n");
    summary.append("3. Les signatures XML ont été validées\n");
    summary.append("4. Les fichiers XML ont été transformés en HTML pour aperçu\n");
    summary.append("5. Tous les fichiers ont été regroupés dans cette archive ZIP\n\n");

    summary.append("Remarques :\n");
    summary.append("- Seuls les fichiers traités avec succès sont inclus\n");
    summary.append("- Consultez les rapports de validation pour les détails de vérification de signature\n");
    summary.append("- Les aperçus HTML montrent l'apparence des factures lors de l'affichage\n");

    return summary.toString();
    }
    
    /**
     * Clean filename by removing unwanted prefixes and characters
     */
    private String cleanFilename(String filename) {
        if (filename == null) return "unknown";

        // Remove any numeric prefixes like "2025-1-" or "123-"
        String cleaned = filename.replaceAll("^\\d+-\\d*-?", "");

        // If the cleaned filename is empty or too short, use original
        if (cleaned.length() < 3) {
            cleaned = filename;
        }

        // Remove any leading/trailing whitespace and invalid characters
        cleaned = cleaned.trim().replaceAll("[^a-zA-Z0-9._-]", "_");

        return cleaned;
    }

    /**
     * Remove file extension from filename
     */
    private String removeExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(0, lastDotIndex);
        }
        return filename;
    }
    
    /**
     * Clean up old ZIP files (older than specified hours)
     */
    public void cleanupOldZipFiles(int hoursOld) {
        try {
            Path tempDir = Paths.get(TEMP_DIR);
            if (!Files.exists(tempDir)) {
                return;
            }
            
            long cutoffTime = System.currentTimeMillis() - (hoursOld * 60 * 60 * 1000L);
            
            Files.list(tempDir)
                .filter(path -> path.toString().endsWith(".zip"))
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        logger.info("Cleaned up old ZIP file: {}", path);
                    } catch (IOException e) {
                        logger.warn("Failed to delete old ZIP file {}: {}", path, e.getMessage());
                    }
                });
                
        } catch (IOException e) {
            logger.error("Error during ZIP cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get the download URL for a ZIP file
     */
    public String getDownloadUrl(Path zipPath, String baseUrl) {
        String filename = zipPath.getFileName().toString();
        return baseUrl + "/workflow/download/" + filename;
    }
    
    /**
     * Check if ZIP file exists and is readable
     */
    public boolean isZipFileAvailable(String filename) {
        try {
            Path zipPath = Paths.get(TEMP_DIR, filename);
            return Files.exists(zipPath) && Files.isReadable(zipPath);
        } catch (Exception e) {
            logger.warn("Error checking ZIP file availability: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Create summary report including error information
     */
    private String createSummaryReportWithErrors(Map<String, String> signedXmlFiles,
                                                Map<String, String> validationReports,
                                                Map<String, String> htmlFiles,
                                                Map<String, String> errorReports,
                                                Map<String, String> ttnInvoiceIds,
                                                String username) {
        StringBuilder summary = new StringBuilder();
        summary.append("=== RÉSUMÉ DU TRAITEMENT DES FACTURES ===\n\n");
        summary.append("Date de traitement : ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        summary.append("Utilisateur : ").append(username).append("\n\n");

        summary.append("Fichiers traités :\n");
        summary.append("- Fichiers XML signés : ").append(signedXmlFiles.size()).append("\n");
        summary.append("- Rapports de validation : ").append(validationReports.size()).append("\n");
        summary.append("- Aperçus HTML : ").append(htmlFiles.size()).append("\n");
        summary.append("- Rapports d'erreur : ").append(errorReports.size()).append("\n\n");

        int totalFiles = signedXmlFiles.size() + errorReports.size();
        int successfulFiles = signedXmlFiles.size();
        int failedFiles = errorReports.size();

        summary.append("Statistiques globales :\n");
        summary.append("- Total de fichiers traités : ").append(totalFiles).append("\n");
        summary.append("- Succès : ").append(successfulFiles).append("\n");
        summary.append("- Échecs : ").append(failedFiles).append("\n");
        if (totalFiles > 0) {
            double successRate = (double) successfulFiles / totalFiles * 100;
            summary.append("- Taux de réussite : ").append(String.format("%.1f%%", successRate)).append("\n\n");
        }

        if (!signedXmlFiles.isEmpty()) {
            summary.append("=== FICHIERS TRAITÉS AVEC SUCCÈS ===\n");
            signedXmlFiles.keySet().forEach(filename -> {
                String ttnId = ttnInvoiceIds != null ? ttnInvoiceIds.get(filename) : null;
                summary.append("✓ ").append(filename);
                if (ttnId != null && !ttnId.isEmpty()) {
                    summary.append("  (ID TTN : ").append(ttnId).append(")");
                }
                summary.append("\n");
            });
            summary.append("\n");
        }

        if (!errorReports.isEmpty()) {
            summary.append("=== FICHIERS EN ÉCHEC ===\n");
            errorReports.keySet().forEach(filename -> {
                String originalFilename = filename.replace("_error.txt", "");
                summary.append("✗ ").append(originalFilename).append(" (voir Rapport_des_erreurs/").append(filename).append(")\n");
            });
            summary.append("\n");
        }

        summary.append("=== CONTENU DE L'ARCHIVE ZIP ===\n");
        if (!signedXmlFiles.isEmpty()) {
            summary.append("📁 Factures_Signees/ - Fichiers XML signés avec succès\n");
        }
        if (!validationReports.isEmpty()) {
            summary.append("📁 Rapports_De_Validation/ - Rapports de validation de signature XML\n");
        }
        if (!htmlFiles.isEmpty()) {
            summary.append("📁 Factures_Pdf/ - Rapports formatés HTML\n");
        }
        if (!errorReports.isEmpty()) {
            summary.append("📁 Rapports_Des_Erreurs/ - Rapports détaillés pour les fichiers en échec\n");
        }
        summary.append("📄 PROCESSING_SUMMARY.txt - Ce fichier de résumé\n\n");

        summary.append("=== PROCHAINES ÉTAPES ===\n");
        if (failedFiles > 0) {
            summary.append("• Consultez les rapports d'erreur dans le dossier Rapport_des_erreurs/\n");
            summary.append("• Corrigez les problèmes et relancez le traitement pour les fichiers en échec\n");
            summary.append("• Vérifiez les identifiants et la connectivité réseau si nécessaire\n");
        }
        if (successfulFiles > 0) {
            summary.append("• Les fichiers XML signés sont prêts pour la soumission\n");
            summary.append("• Les rapports de validation confirment l'intégrité des signatures\n");
        }

        return summary.toString();
    }

    /**
     * Get ZIP file path by filename
     */
    public Path getZipFilePath(String filename) {
        return Paths.get(TEMP_DIR, filename);
    }
}
