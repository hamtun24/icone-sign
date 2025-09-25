'use client'

import { useState } from 'react'
import { motion } from 'framer-motion'
import { Search, FileText, Calendar, User, Building, AlertCircle, CheckCircle, Loader2, ArrowLeft } from 'lucide-react'
import { toast } from 'sonner'

interface TtnConsultProps {
  onBack: () => void
}

interface ConsultCriteria {
  idSaveEfact?: string
  generatedRef?: string
  dateFrom?: string
  dateTo?: string
  status?: string
}

interface Invoice {
  id: string
  reference: string
  status: string
  date: string
  amount?: number
  [key: string]: any
}

interface ConsultResponse {
  success: boolean
  count: number
  invoices: Invoice[]
  rawResponse: string
  htmlContent?: string
  error?: string
}

export function TtnConsult({ onBack }: TtnConsultProps) {
  // Remove credentials state - using stored user credentials now
  
  const [criteria, setCriteria] = useState<ConsultCriteria>({
    idSaveEfact: '',
    generatedRef: '',
    dateFrom: '',
    dateTo: '',
    status: ''
  })
  
  const [isLoading, setIsLoading] = useState(false)
  const [results, setResults] = useState<ConsultResponse | null>(null)
  const [showRawResponse, setShowRawResponse] = useState(false)

  const handleConsult = async () => {
    setIsLoading(true)
    setResults(null)

    try {
      // Filter out empty criteria
      const filteredCriteria = Object.fromEntries(
        Object.entries(criteria).filter(([_, value]) => value && value.trim() !== '')
      )

      // Get auth token
      const token = localStorage.getItem('auth_token')
      if (!token) {
        toast.error('Vous devez être connecté pour consulter les factures')
        return
      }

      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1'
      const response = await fetch(`${apiUrl}/ttn/consulthtml`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          criteria: filteredCriteria
        }),
      })

      const data = await response.json()

      if (response.ok) {
        setResults(data)
        if (data.success) {
          toast.success(`Consultation réussie - ${data.count} facture(s) trouvée(s)`)
        } else {
          toast.error(data.error || 'Erreur lors de la consultation')
        }
      } else {
        toast.error(data.error || 'Erreur lors de la consultation')
        setResults(data)
      }
    } catch (error) {
      console.error('Erreur de consultation:', error)
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
            Consultation TTN E-Facturation
          </h2>
          <p className="text-gray-600">
            Consultez vos factures électroniques dans le système TTN
          </p>
        </div>
      </div>

      {/* Info Message */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-blue-50 rounded-2xl p-6 border border-blue-200"
      >
        <div className="flex items-center gap-3">
          <User className="w-5 h-5 text-blue-600" />
          <div>
            <h3 className="text-lg font-semibold text-blue-800">Identifiants TTN</h3>
            <p className="text-blue-600 text-sm">
              Utilisation des identifiants TTN configurés dans votre profil utilisateur.
            </p>
          </div>
        </div>
      </motion.div>

      {/* Search Criteria */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="bg-white rounded-2xl p-6 border border-gray-200 shadow-sm"
      >
        <h3 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
          <Search className="w-5 h-5 text-adorable-purple" />
          Critères de Recherche
        </h3>
        
        <div className="grid md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              ID Save E-Fact
            </label>
            <input
              type="text"
              value={criteria.idSaveEfact}
              onChange={(e) => setCriteria(prev => ({ ...prev, idSaveEfact: e.target.value }))}
              className="adorable-input w-full px-4 py-3 text-gray-800 placeholder-gray-400"
              placeholder="ID de sauvegarde"
            />
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Référence Générée
            </label>
            <input
              type="text"
              value={criteria.generatedRef}
              onChange={(e) => setCriteria(prev => ({ ...prev, generatedRef: e.target.value }))}
              className="adorable-input w-full px-4 py-3 text-gray-800 placeholder-gray-400"
              placeholder="Référence de la facture"
            />
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Date de début
            </label>
            <input
              type="date"
              value={criteria.dateFrom}
              onChange={(e) => setCriteria(prev => ({ ...prev, dateFrom: e.target.value }))}
              className="adorable-input w-full px-4 py-3 text-gray-800"
            />
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Date de fin
            </label>
            <input
              type="date"
              value={criteria.dateTo}
              onChange={(e) => setCriteria(prev => ({ ...prev, dateTo: e.target.value }))}
              className="adorable-input w-full px-4 py-3 text-gray-800"
            />
          </div>
        </div>
        
        <div className="mt-6">
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={handleConsult}
            disabled={isLoading}
            className="adorable-button text-white font-semibold px-8 py-3 rounded-xl inline-flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isLoading ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
              <Search className="w-5 h-5" />
            )}
            {isLoading ? 'Consultation en cours...' : 'Consulter'}
          </motion.button>
        </div>
      </motion.div>

      {/* Results */}
      {results && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-white rounded-2xl p-6 border border-gray-200 shadow-sm"
        >
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-800 flex items-center gap-2">
              {results.success ? (
                <CheckCircle className="w-5 h-5 text-green-500" />
              ) : (
                <AlertCircle className="w-5 h-5 text-red-500" />
              )}
              Résultats de la Consultation
            </h3>
            <button
              onClick={() => setShowRawResponse(!showRawResponse)}
              className="text-sm text-adorable-blue hover:text-adorable-purple transition-colors"
            >
              {showRawResponse ? 'Masquer' : 'Voir'} la réponse brute
            </button>
          </div>
          {results.success ? (
            <div className="space-y-4">
              <div className="flex items-center gap-4 text-sm text-gray-600">
                <span className="flex items-center gap-1">
                  <FileText className="w-4 h-4" />
                  {results.count} facture(s) trouvée(s)
                </span>
              </div>
              {results.invoices && results.invoices.length > 0 ? (
                <div className="space-y-2">
                  {results.invoices.map((invoice, index) => (
                    <div
                      key={invoice.id || index}
                      className="p-4 bg-gray-50 rounded-lg border border-gray-200"
                    >
                      <div className="grid md:grid-cols-3 gap-4 text-sm">
                        <div>
                          <span className="font-medium text-gray-700">ID:</span>
                          <span className="ml-2 text-gray-600">{invoice.id || 'N/A'}</span>
                        </div>
                        <div>
                          <span className="font-medium text-gray-700">Référence:</span>
                          <span className="ml-2 text-gray-600">{invoice.reference || 'N/A'}</span>
                        </div>
                        <div>
                          <span className="font-medium text-gray-700">Statut:</span>
                          <span className="ml-2 text-gray-600">{invoice.status || 'N/A'}</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-8 text-gray-500">
                  <FileText className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                  <p>Aucune facture trouvée avec ces critères</p>
                </div>
              )}
            </div>
          ) : (
            <div className="text-red-600">
              <p className="font-medium">Erreur:</p>
              <p className="text-sm mt-1">{results.error}</p>
            </div>
          )}
          {/* HTML Content Display */}
          {results.htmlContent && (
            <div className="mt-8">
              <h4 className="font-medium text-gray-700 mb-2">Aperçu HTML de la facture :</h4>
              <div className="bg-gray-50 rounded-lg p-4 overflow-x-auto text-sm" dangerouslySetInnerHTML={{ __html: results.htmlContent }} />
            </div>
          )}
          {showRawResponse && results.rawResponse && (
            <div className="mt-6">
              <h4 className="font-medium text-gray-700 mb-2">Réponse brute du serveur:</h4>
              <pre className="bg-gray-100 p-4 rounded-lg text-xs overflow-x-auto text-gray-600">
                {results.rawResponse}
              </pre>
            </div>
          )}
        </motion.div>
      )}
    </div>
  )
}
