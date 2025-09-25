package com.example.unifiedapi.util;

public class XmlUtils {
    /**
     * Extracts the content inside the <xmlContent>...</xmlContent> tag from a SOAP response string.
     * Returns null if not found.
     */
    public static String extractXmlContent(String soapResponse) {
        if (soapResponse == null) return null;
        String startTag = "<xmlContent>";
        String endTag = "</xmlContent>";
        int start = soapResponse.indexOf(startTag);
        int end = soapResponse.indexOf(endTag);
        if (start == -1 || end == -1 || end < start) return null;
        start += startTag.length();
        return soapResponse.substring(start, end).trim();

    }
}
