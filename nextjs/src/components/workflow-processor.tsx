'use client'

import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { ArrowLeft, Upload, Play, Download, CheckCircle, AlertCircle } from 'lucide-react'
import { FileUploadZone } from './file-upload-zone'
import { WorkflowProgress } from './workflow-progress'
import ResultsDisplay from './results-display'
import { UserProfile } from './auth/user-profile'
import { useWorkflowStore } from '@/store/workflow-store'

interface WorkflowProcessorProps {
  onBack: () => void
}

export function WorkflowProcessor({ onBack }: WorkflowProcessorProps) {
  const [currentStep, setCurrentStep] = useState<'upload' | 'processing' | 'results'>('upload')
  const { files, results, startProcessing } = useWorkflowStore()

  // Debug logging for results
  console.log('WorkflowProcessor state:', { currentStep, hasResults: !!results, filesCount: files.length })

  // Auto-start processing when files are uploaded
  useEffect(() => {
    if (files.length > 0 && currentStep === 'upload') {
      console.log('🚀 Files detected, starting processing automatically...')
      setCurrentStep('processing')
      startProcessing()
    }
  }, [files.length, currentStep, startProcessing])

  const steps = [
    { id: 'upload', title: 'Télécharger', icon: Upload, color: 'bg-blue-500' },
    { id: 'processing', title: 'Traiter', icon: Play, color: 'bg-green-500' },
    { id: 'results', title: 'Résultats', icon: Download, color: 'bg-pink-500' },
  ]

  const handleNext = async () => {
    if (currentStep === 'upload' && files.length > 0) {
      setCurrentStep('processing')
      // Start processing immediately
      await startProcessing()
    } else if (currentStep === 'processing' && results) {
      setCurrentStep('results')
    }
  }

  const handleBack = () => {
    if (currentStep === 'processing') {
      setCurrentStep('upload')
    } else if (currentStep === 'results') {
      setCurrentStep('processing')
    }
  }

  return (
    <div className="min-h-screen py-8">
      <div className="container mx-auto px-4">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center justify-between mb-8"
        >
          <div className="flex items-center gap-4">
            <motion.button
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
              onClick={onBack}
              className="p-3 rounded-full bg-white/80 backdrop-blur-sm border border-white/20 shadow-lg hover:shadow-xl transition-all duration-300"
            >
              <ArrowLeft className="w-6 h-6 text-gray-700" />
            </motion.button>

            <div>
              <h1 className="text-3xl font-display font-bold text-gray-800">
                Procesus de Traitement des Factures
              </h1>
              <p className="text-gray-600">
                Workflow complet pour vos factures XML
              </p>
            </div>
          </div>

          {/* User Profile */}
          <UserProfile />
        </motion.div>

        {/* Progress Steps */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="adorable-card p-6 mb-8"
        >
          <div className="flex items-center justify-between">
            {steps.map((step, index) => {
              const Icon = step.icon
              const isActive = step.id === currentStep
              const isCompleted = steps.findIndex(s => s.id === currentStep) > index
              
              return (
                <div key={step.id} className="flex items-center">
                  <motion.div
                    whileHover={{ scale: 1.1 }}
                    className={`
                      workflow-step
                      ${isActive ? 'active' : ''}
                      ${isCompleted ? 'completed' : ''}
                      ${!isActive && !isCompleted ? 'pending' : ''}
                    `}
                  >
                    {isCompleted ? (
                      <CheckCircle className="w-6 h-6" />
                    ) : (
                      <Icon className="w-6 h-6" />
                    )}
                  </motion.div>
                  
                  <div className="ml-3 hidden md:block">
                    <div className={`font-semibold ${isActive ? 'text-adorable-blue' : isCompleted ? 'text-green-600' : 'text-gray-500'}`}>
                      {step.title}
                    </div>
                  </div>
                  
                  {index < steps.length - 1 && (
                    <div className={`hidden md:block w-16 h-0.5 mx-4 ${isCompleted ? 'bg-green-500' : 'bg-gray-300'}`} />
                  )}
                </div>
              )
            })}
          </div>
        </motion.div>

        {/* Main Content */}
        <AnimatePresence mode="wait">
          <motion.div
            key={currentStep}
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            transition={{ duration: 0.3 }}
            className="adorable-card p-8"
          >
            {currentStep === 'upload' && (
              <FileUploadZone onNext={handleNext} />
            )}
            

            
            {currentStep === 'processing' && (
              <WorkflowProgress onComplete={() => setCurrentStep('results')} />
            )}
            
            {currentStep === 'results' && (
              <ResultsDisplay onBack={handleBack} onStartOver={() => {
                setCurrentStep('upload')
                // Reset store state
              }} />
            )}
          </motion.div>
        </AnimatePresence>
      </div>
    </div>
  )
}
