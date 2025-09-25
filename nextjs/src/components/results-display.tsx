'use client'

import { motion } from 'framer-motion'
import { Download, RefreshCw, CheckCircle, AlertCircle, FileText, Package, Sparkles } from 'lucide-react'
import { useWorkflowStore } from '@/store/workflow-store'
import { useState } from 'react'
import { toast } from 'sonner'

interface ResultsDisplayProps {
  onBack: () => void
  onStartOver: () => void
}

const ResultsDisplay: React.FC<ResultsDisplayProps> = ({ onBack, onStartOver }) => {
  const { results, reset } = useWorkflowStore()
  // Removed HTML generation state and logic

  if (!results) return null

  const handleDownload = async () => {
    if (results.zipDownloadUrl) {
      try {
        // Create a temporary link to trigger download
        const link = document.createElement('a')
        link.href = results.zipDownloadUrl
        link.download = 'processed_invoices.zip'
        link.style.display = 'none'
        
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        
        toast.success('Téléchargement commencé!')
      } catch (error) {
        toast.error('Échec du téléchargement des fichiers')
      }
    }
  }

  const handleStartOver = () => {
    reset()
    onStartOver()
  }

  const successRate = Math.round((results.successfulFiles / results.totalFiles) * 100)

  // HTML generation logic removed

  return (
    <div className="space-y-8">
      <div className="text-center">
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ type: "spring", stiffness: 200 }}
          className={`inline-flex items-center justify-center w-20 h-20 rounded-full mb-4 ${
            results.success
              ? 'bg-gradient-to-r from-green-400 to-green-600'
              : 'bg-gradient-to-r from-red-400 to-red-600'
          }`}
        >
          {results.success ? (
            <CheckCircle className="w-10 h-10 text-white" />
          ) : (
            <AlertCircle className="w-10 h-10 text-white" />
          )}
        </motion.div>
        
        <h2 className="text-2xl font-display font-bold text-gray-800 mb-2">
          {results.success ? 'Traitement Terminé ! 🎉' : 'Traitement Terminé avec Erreurs ⚠️'}
        </h2>

        <p className={`mb-6 ${results.success ? 'text-gray-600' : 'text-red-600'}`}>
          {results.message || (results.success ? 'Tous les fichiers ont été traités avec succès.' : 'Certains fichiers ont échoué lors du traitement.')}
        </p>
      </div>

      {/* Summary Statistics */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="grid grid-cols-2 md:grid-cols-4 gap-4"
      >
        <div className="text-center p-4 bg-blue-50 rounded-xl">
          <div className="text-2xl font-bold text-blue-600">{results.totalFiles}</div>
          <div className="text-sm text-blue-800">Total Fichiers</div>
        </div>

        <div className="text-center p-4 bg-green-50 rounded-xl">
          <div className="text-2xl font-bold text-green-600">{results.successfulFiles}</div>
          <div className="text-sm text-green-800">Réussis</div>
        </div>

        <div className="text-center p-4 bg-red-50 rounded-xl">
          <div className="text-2xl font-bold text-red-600">{results.failedFiles}</div>
          <div className="text-sm text-red-800">Échoués</div>
        </div>

        <div className="text-center p-4 bg-purple-50 rounded-xl">
          <div className="text-2xl font-bold text-purple-600">{successRate}%</div>
          <div className="text-sm text-purple-800">Taux de Réussite</div>
        </div>
      </motion.div>

      {/* File Results */}
      <div className="space-y-4">
        <h3 className="font-semibold text-gray-800 flex items-center gap-2">
          <FileText className="w-5 h-5" />
          Résultats des Fichiers
        </h3>
        
        <div className="space-y-2 max-h-64 overflow-y-auto">
          {results.files.map((file) => (
            <motion.div
              key={file.id}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              className={`
                p-4 rounded-lg border-2 transition-all duration-300
                ${file.status === 'completed' 
                  ? 'bg-green-50 border-green-200' 
                  : 'bg-red-50 border-red-200'
                }
              `}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  {file.status === 'completed' ? (
                    <CheckCircle className="w-5 h-5 text-green-600" />
                  ) : (
                    <AlertCircle className="w-5 h-5 text-red-600" />
                  )}
                  <div>
                    <p className="font-medium text-gray-800">
                      {file.file.name}
                    </p>
                    {file.ttnInvoiceId && (
                      <p className="text-sm text-gray-600">
                        TTN ID: {file.ttnInvoiceId}
                      </p>
                    )}
                    {file.error && (
                      <p className={`text-sm ${file.error.includes('TTN') ? 'text-orange-600 font-semibold' : 'text-red-600'}`}>
                        {file.error.includes('TTN')
                          ? `Erreur : ${file.error.replace(/TTN (a retourné une erreur|save failed):/i, '').trim()}`
                          : `Erreur : ${file.error}`}
                      </p>
                    )}
                    {/* HTML Generation Section removed */}
                  </div>
                </div>
                <div className="text-right">
                  <div className={`
                    text-sm font-medium capitalize
                    ${file.status === 'completed' ? 'text-green-600' : 'text-red-600'}
                  `}>
                    {file.status}
                  </div>
                  <div className="text-xs text-gray-500 capitalize">
                    {file.stage.replace('_', ' ')}
                  </div>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>

      {/* Download Section */}
      {results.zipDownloadUrl && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-gradient-to-r from-adorable-pink/10 to-adorable-blue/10 rounded-2xl p-6 border border-adorable-blue/20"
        >
          <div className="flex items-center gap-3 mb-4">
            <Package className="w-6 h-6 text-adorable-blue" />
            <h3 className="text-lg font-semibold text-gray-800">
              Vos Fichiers sont Prêts !
            </h3>
            <Sparkles className="w-5 h-5 text-adorable-pink" />
          </div>

          <p className="text-gray-600 mb-4">
            Téléchargez votre archive ZIP complète contenant :
          </p>

          <ul className="text-sm text-gray-600 space-y-1 mb-6">
            <li>📁 <strong>Facture_xml_Signee/</strong> - Factures XML signées numériquement</li>
            <li>📁 <strong>Rapport_de_Validation/</strong> - Rapports de validation JSON</li>
            <li>📁 <strong>Facture_Pdf/</strong> - Fichiers d'aperçu HTML</li>
            <li>📄 <strong>resume_de_traitement.txt</strong> - Résumé complet</li>
          </ul>
          
          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={handleDownload}
            className="adorable-button text-white font-semibold px-8 py-3 rounded-xl inline-flex items-center gap-2 w-full justify-center"
          >
            <Download className="w-5 h-5" />
            Télécharger l'Archive Complète
          </motion.button>
        </motion.div>
      )}

      {/* Action Buttons */}
      <div className="flex flex-col sm:flex-row gap-3 pt-6 border-t border-gray-200">
        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={onBack}
          className="px-6 py-3 border border-gray-300 text-gray-700 rounded-xl font-medium hover:bg-gray-50 transition-colors"
        >
          Voir la Progression
        </motion.button>

        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={handleStartOver}
          className="flex-1 px-6 py-3 border border-adorable-blue text-adorable-blue rounded-xl font-medium hover:bg-adorable-blue/5 transition-colors inline-flex items-center justify-center gap-2"
        >
          <RefreshCw className="w-4 h-4" />
          Traiter Plus de Fichiers
        </motion.button>
      </div>

    </div>
  )
}
export default ResultsDisplay;
