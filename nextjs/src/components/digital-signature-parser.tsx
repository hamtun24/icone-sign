// Type definitions for the digital signature validation report
interface Policy {
  policyName: string;
  policyDescription: string;
}

interface Certificate {
  id: string;
  qualifiedName: string;
}

interface CertificateChain {
  certificate: Certificate[];
}

interface SignatureLevel {
  value: string;
  description: string;
}

interface SignatureScope {
  value: string;
  name: string;
  scope: string;
}

interface Signature {
  filename: string | null;
  signingTime: number;
  bestSignatureTime: number;
  signedBy: string;
  certificateChain: CertificateChain;
  signatureLevel: SignatureLevel;
  indication: string;
  subIndication: string;
  errors: string[];
  warnings: string[];
  infos: string[];
  signatureScope: SignatureScope[];
  id: string;
  counterSignature: any | null;
  parentId: string | null;
  signatureFormat: string;
}

interface SimpleReport {
  policy: Policy;
  validationTime: number;
  documentName: string;
  validSignaturesCount: number;
  signaturesCount: number;
  containerType: string | null;
  signature: Signature[];
}

interface SignatureValidationReport {
  simpleReport: SimpleReport;
}

// Main function to interpret and process the signature validation report
function interpretSignatureReport(jsonData: SignatureValidationReport): {
  summary: {
    documentName: string;
    totalSignatures: number;
    validSignatures: number;
    validationDate: Date;
    policyUsed: string;
  };
  signatures: {
    id: string;
    signer: string;
    signingDate: Date;
    status: 'VALID' | 'INVALID' | 'INDETERMINATE';
    format: string;
    errors: string[];
    warnings: string[];
    certificateInfo: {
      signerCertificate: string;
      issuerChain: string[];
    };
  }[];
  isDocumentTrusted: boolean;
} {
  const { simpleReport } = jsonData;
  
  // Process signatures
  const processedSignatures = simpleReport.signature.map(sig => ({
    id: sig.id,
    signer: sig.signedBy,
    signingDate: new Date(sig.signingTime),
    status: sig.indication as 'VALID' | 'INVALID' | 'INDETERMINATE',
    format: sig.signatureFormat,
    errors: sig.errors,
    warnings: sig.warnings,
    certificateInfo: {
      signerCertificate: sig.certificateChain.certificate[0]?.qualifiedName || 'Unknown',
      issuerChain: sig.certificateChain.certificate.slice(1).map(cert => cert.qualifiedName)
    }
  }));

  // Determine if document is trusted (no errors in any signature)
  const isDocumentTrusted = processedSignatures.every(sig => sig.errors.length === 0);

  return {
    summary: {
      documentName: simpleReport.documentName,
      totalSignatures: simpleReport.signaturesCount,
      validSignatures: simpleReport.validSignaturesCount,
      validationDate: new Date(simpleReport.validationTime),
      policyUsed: simpleReport.policy.policyName
    },
    signatures: processedSignatures,
    isDocumentTrusted
  };
}

// Helper function to get detailed signature analysis
function analyzeSignature(signature: Signature): {
  trustLevel: 'HIGH' | 'MEDIUM' | 'LOW';
  issues: string[];
  recommendations: string[];
} {
  const issues: string[] = [];
  const recommendations: string[] = [];
  let trustLevel: 'HIGH' | 'MEDIUM' | 'LOW' = 'HIGH';

  // Analyze errors
  if (signature.errors.length > 0) {
    trustLevel = 'LOW';
    issues.push(...signature.errors);
    
    if (signature.errors.some(error => error.includes('certificate path is not trusted'))) {
      recommendations.push('Verify the certificate authority is properly configured in your trust store');
    }
    
    if (signature.errors.some(error => error.includes('LTV validation'))) {
      recommendations.push('Check long-term validation settings and certificate revocation status');
    }
  }

  // Analyze warnings
  if (signature.warnings.length > 0 && trustLevel === 'HIGH') {
    trustLevel = 'MEDIUM';
    issues.push(...signature.warnings);
  }

  // Check signature level
  if (signature.signatureLevel.value === 'NA') {
    if (trustLevel === 'HIGH') trustLevel = 'MEDIUM';
    recommendations.push('Consider upgrading to a higher signature level (LTA or LTV)');
  }

  return {
    trustLevel,
    issues,
    recommendations
  };
}

// Utility function to format the report for display
function formatReportSummary(report: ReturnType<typeof interpretSignatureReport>): string {
  const { summary, signatures, isDocumentTrusted } = report;
  
  let output = `Document Validation Report\n`;
  output += `========================\n`;
  output += `Document: ${summary.documentName}\n`;
  output += `Validation Date: ${summary.validationDate.toLocaleString()}\n`;
  output += `Policy: ${summary.policyUsed}\n`;
  output += `\n`;
  output += `Total Signatures: ${summary.totalSignatures}\n`;
   output += `\n`;
  signatures.forEach((sig, index) => {
    output += `Signature ${index + 1}:\n`;
    output += `  Signer: ${sig.signer}\n`;
    output += `  Status: ${sig.status}\n`;
    output += `  Format: ${sig.format}\n`;
    output += `  Signing Date: ${sig.signingDate.toLocaleString()}\n`;
    
  
    
    output += `\n`;
  });
  
  return output;
}

// Example usage function
function exampleUsage() {
  // Example JSON data (would typically come from an API or file)
  const exampleData: SignatureValidationReport = {
    simpleReport: {
      policy: {
        policyName: "QES AdESQC TL based",
        policyDescription: "Validate electronic signatures and indicates whether they are Advanced electronic Signatures (AdES)..."
      },
      validationTime: 1757336266799,
      documentName: "signed.xml",
      validSignaturesCount: 0,
      signaturesCount: 2,
      containerType: null,
      signature: [
        {
          filename: null,
          signingTime: 1757329746000,
          bestSignatureTime: 1757336266799,
          signedBy: "ICONE TEST",
          certificateChain: {
            certificate: [
              {
                id: "57A96D61FF452F5D2C9C7F962EF1F023124F72A293B87D4E3C8850448DAB2FC9",
                qualifiedName: "ICONE TEST"
              }
            ]
          },
          signatureLevel: {
            value: "NA",
            description: "Not applicable"
          },
          indication: "INDETERMINATE",
          subIndication: "SIGNATURE_POLICY_NOT_AVAILABLE",
          errors: ["The certificate path is not trusted!"],
          warnings: ["The signature/seal is an INDETERMINATE AdES!"],
          infos: [],
          signatureScope: [
            {
              value: "The full XML file with transformations",
              name: "Full XML File",
              scope: "FULL"
            }
          ],
          id: "id-274951a151dd47029d1dd6722e0172cf",
          counterSignature: null,
          parentId: null,
          signatureFormat: "XAdES-BASELINE-B"
        }
      ]
    }
  };

  // Process the report
  const processedReport = interpretSignatureReport(exampleData);
  console.log(formatReportSummary(processedReport));
  
  return processedReport;
}

export {
  interpretSignatureReport,
  analyzeSignature,
  formatReportSummary,
  exampleUsage,
  type SignatureValidationReport,
  type SimpleReport,
  type Signature
};