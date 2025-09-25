'use client'

import { useState } from 'react'
import { motion } from 'framer-motion'
import { Shield, CheckCircle, AlertCircle, Loader2, ArrowLeft } from 'lucide-react'
import { toast } from 'sonner'
import { interpretSignatureReport, formatReportSummary, type SignatureValidationReport } from './digital-signature-parser'

interface AnceValidationPageProps {
  onBack: () => void
}

interface ValidationResult {
  valid: boolean
  report?: string
  error?: string
  rawResponse?: string
}

export function AnceValidationPage({ onBack }: AnceValidationPageProps) {
  const [file, setFile] = useState<File | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [result, setResult] = useState<ValidationResult | null>(null)
  const [showRawResponse, setShowRawResponse] = useState(false)

  const handleValidation = async () => {
    if (!file) {
      toast.error('Veuillez sélectionner un fichier XML')
      return
    }

    setIsLoading(true)
    setResult(null)

    try {
      const token = localStorage.getItem('auth_token')
      if (!token) {
        toast.error('Vous devez être connecté pour valider avec ANCE')
        return
      }

      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1'
      const formData = new FormData()
      formData.append('file', file)

      const response = await fetch(`${apiUrl}/ance-seal/validate`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
        body: formData,
      })

      const data = await response.json()

      // Align with backend: { valid, report, error }
      setResult({
        valid: data.valid,
        report: data.report,
        error: data.error,
        rawResponse: JSON.stringify(data, null, 2)
      })

      if (response.ok && data.valid) {
        toast.success('Validation réussie ✔')
      } else {
        toast.error(data.error || 'Erreur de validation')
      }
    } catch (error) {
      console.error('Erreur de validation:', error)
      toast.error('Erreur de connexion au serveur')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center gap-4">
        <motion.button
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
          onClick={onBack}
          className="p-2 hover:bg-gray-100 rounded-full transition-colors"
        >
          <ArrowLeft className="w-6 h-6 text-gray-600" />
        </motion.button>

        <div>
          <h2 className="text-2xl font-display font-bold text-gray-800">
            Validation ANCE
          </h2>
          <p className="text-gray-600">
            Validez vos signatures XML avec le sceau ANCE
          </p>
        </div>
      </div>

      {/* Info Message */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-purple-50 rounded-2xl p-6 border border-purple-200"
      >
        <div className="flex items-center gap-3">
          <Shield className="w-5 h-5 text-purple-600" />
          <div>
            <h3 className="text-lg font-semibold text-purple-800">Validation Sécurisée</h3>
            <p className="text-purple-600 text-sm">
              Le service ANCE vérifie la conformité et l’authenticité des signatures.
            </p>
          </div>
        </div>
      </motion.div>

      {/* File Upload */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="bg-white rounded-2xl p-6 border border-gray-200 shadow-sm"
      >
        <h3 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
          <Shield className="w-5 h-5 text-purple-600" />
          Facture XML
        </h3>

        <input
          type="file"
          accept=".xml"
          onChange={(e) => setFile(e.target.files?.[0] || null)}
          className="block w-full text-sm text-gray-600
                     file:mr-4 file:py-2 file:px-4
                     file:rounded-full file:border-0
                     file:text-sm file:font-semibold
                     file:bg-purple-50 file:text-purple-700
                     hover:file:bg-purple-100"
        />

        <div className="mt-6">
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={handleValidation}
            disabled={isLoading}
            className="px-8 py-3 rounded-xl inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed
                       bg-purple-600 hover:bg-purple-700 text-white font-semibold"
          >
            {isLoading ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
              <Shield className="w-5 h-5" />
            )}
            {isLoading ? 'Validation en cours...' : 'Valider'}
          </motion.button>
        </div>
      </motion.div>

      {/* Results */}
      {result && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-white rounded-2xl p-6 border border-gray-200 shadow-sm"
        >
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-800 flex items-center gap-2">
              {result.valid ? (
                <CheckCircle className="w-5 h-5 text-green-500" />
              ) : (
                <AlertCircle className="w-5 h-5 text-red-500" />
              )}
              Résultats de la Validation
            </h3>

            {result.rawResponse && (
              <button
                onClick={() => setShowRawResponse(!showRawResponse)}
                className="text-sm text-purple-600 hover:text-purple-800 transition-colors"
              >
                {showRawResponse ? 'Masquer' : 'Voir'} la réponse brute
              </button>
            )}
          </div>

          {/* Professional digital signature report presentation */}
          {result.report && (() => {
            try {
              const parsed: SignatureValidationReport = JSON.parse(result.report);
              const processed = interpretSignatureReport(parsed);
              const signatureCount = processed.signatures.length;
              return (
                <>
                  <div className="bg-gray-50 rounded-lg p-4 mb-4">
                    <pre className="whitespace-pre-wrap text-xs text-gray-800">
                      {formatReportSummary(processed)}
                    </pre>
                  </div>
                  <div
                    className={
                      signatureCount === 2
                        ? 'flex items-center gap-3 bg-green-50 border border-green-200 rounded-xl p-4 mb-2 shadow-sm'
                        : 'flex items-center gap-3 bg-red-50 border border-red-200 rounded-xl p-4 mb-2 shadow-sm'
                    }
                  >
                    {signatureCount === 2 ? (
                      <>
                        <CheckCircle className="w-6 h-6 text-green-500" />
                        <span className="text-green-800 font-bold text-base">Signature valide</span>
                        
                      </>
                    ) : (
                      <>
                        <AlertCircle className="w-6 h-6 text-red-500" />
                        <span className="text-red-800 font-bold text-base">Signature non valide</span>
                        
                      </>
                    )}
                  </div>
                </>
              );
            } catch (e) {
              // If not JSON, fallback to raw report
              return (
                <pre className="bg-gray-100 p-4 rounded-lg text-xs overflow-x-auto text-gray-600 mb-4">
                  {result.report}
                </pre>
              );
            }
          })()}

          {!result.report && result.valid && (
            <div className="text-green-700 font-semibold">Signature XML valide ✔</div>
          )}
          {!result.valid && (
            <div className="text-red-600">
              <p className="font-medium">Erreur:</p>
              <p className="text-sm mt-1">{result.error || 'Signature XML invalide.'}</p>
            </div>
          )}

          {showRawResponse && result.rawResponse && (
            <div className="mt-6">
              <h4 className="font-medium text-gray-700 mb-2">Réponse brute du serveur:</h4>
              <pre className="bg-gray-100 p-4 rounded-lg text-xs overflow-x-auto text-gray-600">
                {result.rawResponse}
              </pre>
            </div>
          )}
        </motion.div>
      )}
    </div>
  )
}
