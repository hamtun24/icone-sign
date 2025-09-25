package com.example.unifiedapi.dto;

/**
 * Data transfer object for file information used in parallel processing.
 * Contains all necessary file data to process invoices without holding MultipartFile references.
 */
public class FileData {
    private final String filename;
    private final long size;
    private final String contentType;
    private final byte[] content;
    
    public FileData(String filename, long size, String contentType, byte[] content) {
        this.filename = filename;
        this.size = size;
        this.contentType = contentType;
        this.content = content != null ? content.clone() : new byte[0];
    }
    
    public String getFilename() {
        return filename;
    }
    
    public long getSize() {
        return size;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public byte[] getContent() {
        return content.clone(); // Return copy to prevent external modification
    }
    
    @Override
    public String toString() {
        return String.format("FileData{filename='%s', size=%d, contentType='%s'}", 
                           filename, size, contentType);
    }
}
