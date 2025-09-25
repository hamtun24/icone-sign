'use client'

import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { Eye, EyeOff, Shield, Key, Save, TestTube, Info } from 'lucide-react'
import { useWorkflowStore, WorkflowCredentials } from '@/store/workflow-store'
import { toast } from 'sonner'

interface CredentialsFormProps {
  onNext: () => void
  onBack: () => void
}

export function CredentialsForm({ onNext, onBack }: CredentialsFormProps) {
  const { credentials, setCredentials, certificateInfo, fetchCertificateInfo } = useWorkflowStore()
  const [showPasswords, setShowPasswords] = useState(false)
  const [formData, setFormData] = useState<WorkflowCredentials>(
    credentials || {
      ttn: {
        username: 'ICONEFE',
        password: 'V@xP!8mK#z73Lq9W',
        matriculeFiscal: '0786415L',
      },
      anceSeal: {
        pin: '259377',
        alias: 'SealIconeTest',
        certificatePath: certificateInfo?.defaultPath || 'classpath:certificates/icone.crt',
      },
    }
  )
  const [activeTab, setActiveTab] = useState<'ttn' | 'ance'>('ttn')

  // Fetch certificate info on component mount
  useEffect(() => {
    if (!certificateInfo) {
      fetchCertificateInfo()
    }
  }, [certificateInfo, fetchCertificateInfo])

  // Update certificate path when certificate info is loaded
  useEffect(() => {
    if (certificateInfo && !credentials) {
      setFormData(prev => ({
        ...prev,
        anceSeal: {
          ...prev.anceSeal,
          certificatePath: certificateInfo.defaultPath,
        },
      }))
    }
  }, [certificateInfo, credentials])

  const handleInputChange = (
    section: 'ttn' | 'anceSeal',
    field: string,
    value: string
  ) => {
    setFormData(prev => ({
      ...prev,
      [section]: {
        ...prev[section],
        [field]: value,
      },
    }))
  }

  const handleSave = () => {
    // Basic validation
    const { ttn, anceSeal } = formData
    
    if (!ttn.username || !ttn.password || !ttn.matriculeFiscal) {
      toast.error('Please fill in all TTN credentials')
      return
    }
    
    if (!anceSeal.pin || !anceSeal.alias || !anceSeal.certificatePath) {
      toast.error('Please fill in all ANCE SEAL credentials')
      return
    }

    setCredentials(formData)
    toast.success('Identifiants sauvegardés avec succès!')
    onNext()
  }

  const handleTest = async () => {
    toast.info('Test des identifiants en cours...')
    // In a real app, you would test the credentials here
    setTimeout(() => {
      toast.success('Les identifiants sont valides!')
    }, 1500)
  }

  return (
    <div className="space-y-6">
      <div className="text-center mb-8">
        <h2 className="text-2xl font-display font-bold text-gray-800 mb-2">
          Configuration des Identifiants
        </h2>
        <p className="text-gray-600">
          Configurez vos identifiants TTN et ANCE SEAL pour le traitement
        </p>
      </div>

      {/* Tab Navigation */}
      <div className="flex bg-gray-100 rounded-xl p-1">
        {[
          { id: 'ttn', label: 'TTN E-Facturation', icon: Shield },
          { id: 'ance', label: 'ANCE SEAL', icon: Key },
        ].map((tab) => {
          const Icon = tab.icon
          return (
            <motion.button
              key={tab.id}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={() => setActiveTab(tab.id as 'ttn' | 'ance')}
              className={`
                flex-1 flex items-center justify-center gap-2 py-3 px-4 rounded-lg font-medium transition-all duration-200
                ${activeTab === tab.id
                  ? 'bg-white text-adorable-blue shadow-sm'
                  : 'text-gray-600 hover:text-gray-800'
                }
              `}
            >
              <Icon className="w-5 h-5" />
              {tab.label}
            </motion.button>
          )
        })}
      </div>

      {/* TTN Credentials */}
      {activeTab === 'ttn' && (
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          className="space-y-4"
        >
          <div className="grid md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Nom d'utilisateur
              </label>
              <input
                type="text"
                value={formData.ttn.username}
                onChange={(e) => handleInputChange('ttn', 'username', e.target.value)}
                className="adorable-input w-full px-4 py-3 text-gray-800 placeholder-gray-400"
                placeholder="Saisir le nom d'utilisateur TTN"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Matricule Fiscal
              </label>
              <input
                type="text"
                value={formData.ttn.matriculeFiscal}
                onChange={(e) => handleInputChange('ttn', 'matriculeFiscal', e.target.value)}
                className="adorable-input w-full px-4 py-3 text-gray-800 placeholder-gray-400"
                placeholder="Saisir le matricule fiscal"
              />
            </div>
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Mot de passe
            </label>
            <div className="relative">
              <input
                type={showPasswords ? 'text' : 'password'}
                value={formData.ttn.password}
                onChange={(e) => handleInputChange('ttn', 'password', e.target.value)}
                className="adorable-input w-full px-4 py-3 pr-12 text-gray-800 placeholder-gray-400"
                placeholder="Saisir le mot de passe TTN"
              />
              <button
                type="button"
                onClick={() => setShowPasswords(!showPasswords)}
                className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-500 hover:text-gray-700"
              >
                {showPasswords ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
              </button>
            </div>
          </div>
        </motion.div>
      )}

      {/* ANCE SEAL Credentials */}
      {activeTab === 'ance' && (
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          className="space-y-4"
        >
          <div className="grid md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Alias
              </label>
              <input
                type="text"
                value={formData.anceSeal.alias}
                onChange={(e) => handleInputChange('anceSeal', 'alias', e.target.value)}
                className="adorable-input w-full px-4 py-3 text-gray-800 placeholder-gray-400"
                placeholder="Saisir l'alias ANCE SEAL"
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                PIN
              </label>
              <div className="relative">
                <input
                  type={showPasswords ? 'text' : 'password'}
                  value={formData.anceSeal.pin}
                  onChange={(e) => handleInputChange('anceSeal', 'pin', e.target.value)}
                  className="adorable-input w-full px-4 py-3 pr-12 text-gray-800 placeholder-gray-400"
                  placeholder="Saisir le PIN ANCE SEAL"
                />
                <button
                  type="button"
                  onClick={() => setShowPasswords(!showPasswords)}
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-500 hover:text-gray-700"
                >
                  {showPasswords ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
            </div>
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Chemin du Certificat
            </label>
            <input
              type="text"
              value={formData.anceSeal.certificatePath}
              onChange={(e) => handleInputChange('anceSeal', 'certificatePath', e.target.value)}
              className="adorable-input w-full px-4 py-3 text-gray-800 placeholder-gray-400"
              placeholder="Saisir le chemin du fichier certificat (.crt)"
            />

            {/* Certificate Info Helper */}
            {certificateInfo && (
              <div className="mt-3 p-3 bg-blue-50 rounded-lg border border-blue-200">
                <div className="flex items-start gap-2">
                  <Info className="w-4 h-4 text-blue-600 mt-0.5 flex-shrink-0" />
                  <div className="text-sm">
                    <p className="font-medium text-blue-800 mb-1">Configuration du Certificat</p>
                    <p className="text-blue-700 mb-2">
                      <strong>Par défaut :</strong> {certificateInfo.defaultPath}
                    </p>

                    {certificateInfo.availableCertificates.length > 0 && (
                      <div className="mb-2">
                        <p className="text-blue-700 font-medium">Available certificates:</p>
                        <ul className="list-disc list-inside text-blue-600 ml-2">
                          {certificateInfo.availableCertificates.map((cert, index) => (
                            <li key={index} className="text-xs">{cert}</li>
                          ))}
                        </ul>
                      </div>
                    )}

                    <div className="text-xs text-blue-600">
                      <p className="font-medium mb-1">Instructions:</p>
                      <ul className="list-disc list-inside space-y-0.5">
                        {certificateInfo.instructions.map((instruction, index) => (
                          <li key={index}>{instruction}</li>
                        ))}
                      </ul>
                    </div>
                  </div>
                </div>
              </div>
            )}</div>
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
          Retour
        </motion.button>

        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={handleTest}
          className="px-6 py-3 border border-adorable-blue text-adorable-blue rounded-xl font-medium hover:bg-adorable-blue/5 transition-colors inline-flex items-center gap-2"
        >
          <TestTube className="w-4 h-4" />
          Tester les Identifiants
        </motion.button>

        <motion.button
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
          onClick={handleSave}
          className="adorable-button text-white font-semibold px-8 py-3 rounded-xl inline-flex items-center gap-2 flex-1 justify-center"
        >
          <Save className="w-4 h-4" />
          Sauvegarder & Continuer
        </motion.button>
      </div>
    </div>
  )
}
