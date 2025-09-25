'use client'

import React, { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Loader2, FileText, Shield, CheckCircle, Globe, Package, RefreshCw, Clock, AlertTriangle } from 'lucide-react'
import { useWorkflowStore } from '@/store/workflow-store'

interface WorkflowProgressProps {
  onComplete: () => void
}

export function WorkflowProgress({ onComplete }: WorkflowProgressProps) {
  const { files, startProcessing, isProcessing, currentStage, results, refreshProgress, overallProgress, sessionId } = useWorkflowStore()
  const [lastRefresh, setLastRefresh] = useState<Date>(new Date())
  const [autoRefreshEnabled, setAutoRefreshEnabled] = useState(true)
  const [refreshInterval, setRefreshInterval] = useState(3000) // 3 seconds
  const [showDebug, setShowDebug] = useState(false) // Debug panel toggle

  // Enhanced auto-refresh with adaptive intervals
  useEffect(() => {
    if (!autoRefreshEnabled || !isProcessing) return

    // Adaptive refresh interval based on progress
    const adaptiveInterval = overallProgress > 80 ? 1000 : // Fast refresh near completion
                            overallProgress > 50 ? 2000 : // Medium refresh mid-process
                            3000 // Standard refresh at start

    const interval = setInterval(() => {
      refreshProgress?.()
      setLastRefresh(new Date())
    }, adaptiveInterval)

    return () => clearInterval(interval)
  }, [autoRefreshEnabled, isProcessing, overallProgress, refreshProgress])

  useEffect(() => {
    if (!isProcessing && !results) {
      startProcessing()
    }
  }, [startProcessing, isProcessing, results])

  useEffect(() => {
    if (results) {
      console.log('Results detected, transitioning to results view in 2 seconds...', results)
      setTimeout(() => {
        console.log('Calling onComplete to show results display')
        onComplete()
      }, 2000)
    }
  }, [results, onComplete])

  const handleManualRefresh = () => {
    refreshProgress?.()
    setLastRefresh(new Date())
  }

  // Real-time status indicator
  const getWorkflowStatus = () => {
    if (results?.success) return { status: 'completed', color: 'text-green-600', bg: 'bg-green-100', icon: CheckCircle }
    if (results && !results.success) return { status: 'failed', color: 'text-red-600', bg: 'bg-red-100', icon: AlertTriangle }
    if (isProcessing) return { status: 'processing', color: 'text-blue-600', bg: 'bg-blue-100', icon: Loader2 }
    return { status: 'idle', color: 'text-gray-600', bg: 'bg-gray-100', icon: Clock }
  }

  // Get current active stage
  const getCurrentActiveStage = () => {
    if (!isProcessing || files.length === 0) return null

    // Find the most common current stage among files
    const stageCounts = files.reduce((acc, file) => {
      acc[file.stage] = (acc[file.stage] || 0) + 1
      return acc
    }, {} as Record<string, number>)

    return Object.entries(stageCounts).reduce((a, b) => stageCounts[a[0]] > stageCounts[b[0]] ? a : b)[0]
  }

  // Enhanced stages with better API alignment and real-time tracking
  const stages = [
    {
      id: 'sign',
      title: 'Signature Numérique',
      description: 'Signature électronique avec ANCE SEAL',
      icon: Shield,
      color: 'text-blue-500',
      bgColor: 'bg-blue-500',
      gradientFrom: 'from-blue-400',
      gradientTo: 'to-blue-600',
      apiStage: 'SIGN',
      estimatedDuration: '30-60s',
      details: 'Génération de la signature XAdES-BASELINE-B'
    },
    {
      id: 'save',
      title: 'Sauvegarde TTN',
      description: 'Envoi vers le système e-facturation TTN',
      icon: FileText,
      color: 'text-green-500',
      bgColor: 'bg-green-500',
      gradientFrom: 'from-green-400',
      gradientTo: 'to-green-600',
      apiStage: 'SAVE',
      estimatedDuration: '15-30s',
      details: 'Enregistrement dans le système TTN'
    },
    {
      id: 'validate',
      title: 'Validation ANCE',
      description: 'Vérification de la signature numérique',
      icon: CheckCircle,
      color: 'text-purple-500',
      bgColor: 'bg-purple-500',
      gradientFrom: 'from-purple-400',
      gradientTo: 'to-purple-600',
      apiStage: 'VALIDATE',
      estimatedDuration: '20-40s',
      details: 'Validation de l\'intégrité de la signature'
    },
    {
      id: 'transform',
      title: 'Transformation HTML',
      description: 'Génération de l\'aperçu HTML',
      icon: Globe,
      color: 'text-orange-500',
      bgColor: 'bg-orange-500',
      gradientFrom: 'from-orange-400',
      gradientTo: 'to-orange-600',
      apiStage: 'TRANSFORM',
      estimatedDuration: '10-20s',
      details: 'Conversion XML vers format HTML'
    },
    {
      id: 'package',
      title: 'Archivage ZIP',
      description: 'Création de l\'archive finale',
      icon: Package,
      color: 'text-indigo-500',
      bgColor: 'bg-indigo-500',
      gradientFrom: 'from-indigo-400',
      gradientTo: 'to-indigo-600',
      apiStage: 'PACKAGE',
      estimatedDuration: '5-10s',
      details: 'Assemblage des fichiers traités'
    },
  ]

  const getCurrentStageFromAPI = () => {
    // First check if we have files and their actual stages
    if (files.length > 0) {
      // Find the most advanced stage among all files
      const stageOrder = ['sign', 'save', 'validate', 'transform', 'package']
      let mostAdvancedStage = 'sign'

      files.forEach(file => {
        const fileStageIndex = stageOrder.indexOf(file.stage)
        const currentStageIndex = stageOrder.indexOf(mostAdvancedStage)
        if (fileStageIndex > currentStageIndex) {
          mostAdvancedStage = file.stage
        }
      })

      return mostAdvancedStage
    }

    // Fallback to parsing the currentStage message
    if (!currentStage) return 'sign'
    const stage = currentStage.toLowerCase()
    if (stage.includes('sign')) return 'sign'
    if (stage.includes('save')) return 'save'
    if (stage.includes('validate')) return 'validate'
    if (stage.includes('transform')) return 'transform'
    if (stage.includes('package')) return 'package'
    return 'sign'
  }

  const getStageStatus = (stageId: string) => {
    const allFiles = files.length > 0 ? files : []
    if (allFiles.length === 0) return 'pending'

    const stageOrder = ['sign', 'save', 'validate', 'transform', 'package']
    const currentStageIndex = stageOrder.indexOf(stageId)

    // Count files in different states for this stage
    const filesInStage = allFiles.filter(f => f.stage === stageId)
    const processingFilesInStage = filesInStage.filter(f => f.status === 'processing')

    // Count files that have completed this stage (moved to next stage or completed)
    const filesCompletedStage = allFiles.filter(f => {
      const fileStageIndex = stageOrder.indexOf(f.stage)
      return fileStageIndex > currentStageIndex ||
             (fileStageIndex === currentStageIndex && f.status === 'completed')
    })

    // More conservative status determination - only mark as completed when ALL files have passed this stage
    if (processingFilesInStage.length > 0) {
      return 'active' // Files currently processing in this stage
    } else if (filesCompletedStage.length === allFiles.length) {
      return 'completed' // ALL files have completed this stage
    } else {
      // Check if this stage should be active based on current API stage
      const currentAPIStage = getCurrentStageFromAPI()
      const apiStageIndex = stageOrder.indexOf(currentAPIStage)

      if (currentStageIndex === apiStageIndex) {
        return 'active' // This is the current stage being processed
      } else if (currentStageIndex < apiStageIndex) {
        return 'completed' // This stage has been passed
      } else {
        return 'pending' // This stage hasn't been reached yet
      }
    }
  }

  const getStageProgress = (stageId: string) => {
    const allFiles = files.length > 0 ? files : []
    if (allFiles.length === 0) return 0

    const stageOrder = ['sign', 'save', 'validate', 'transform', 'package']
    const currentStageIndex = stageOrder.indexOf(stageId)

    // Get files currently in this stage
    const filesInStage = allFiles.filter(f => f.stage === stageId)

    // Get files that have completed this stage
    const filesCompletedStage = allFiles.filter(f => {
      const fileStageIndex = stageOrder.indexOf(f.stage)
      return fileStageIndex > currentStageIndex ||
             (fileStageIndex === currentStageIndex && f.status === 'completed')
    })

    // If all files have completed this stage, return 100%
    if (filesCompletedStage.length === allFiles.length) {
      return 100
    }

    // If this is the current active stage, return the actual progress from files
    const currentAPIStage = getCurrentStageFromAPI()
    if (stageId === currentAPIStage && filesInStage.length > 0) {
      const totalProgress = filesInStage.reduce((sum, file) => sum + file.progress, 0)
      return Math.round(totalProgress / filesInStage.length)
    }

    // If some files have completed but not all, show partial progress
    if (filesCompletedStage.length > 0) {
      return Math.round((filesCompletedStage.length / allFiles.length) * 100)
    }

    // If files are currently in this stage, calculate their average progress
    if (filesInStage.length > 0) {
      const totalProgress = filesInStage.reduce((sum, file) => sum + file.progress, 0)
      return Math.round(totalProgress / filesInStage.length)
    }

    // If some files have completed but not all, show partial completion
    if (filesCompletedStage.length > 0) {
      return Math.round((filesCompletedStage.length / allFiles.length) * 100)
    }

    return 0 // No progress in this stage yet
  }

  const getActiveFilesCount = (stageId: string) => {
    return files.filter(f => f.stage === stageId && f.status === 'processing').length
  }

  // Add completion celebration effect
  const isCompleted = results?.success || (files.length > 0 && files.every(f => f.status === 'completed'))
  const allFilesCompleted = files.length > 0 && files.every(f => f.status === 'completed' || f.status === 'error')

  // Trigger celebration when all files are processed
  useEffect(() => {
    if (allFilesCompleted && !results) {
      // Add a small delay before showing completion
      setTimeout(() => {
        // This will trigger the confetti effect
      }, 500)
    }
  }, [allFilesCompleted, results])

  return (
    <div className="space-y-8 relative">
      {/* Dynamic background effects */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        {isProcessing && (
          <>
            {/* Animated gradient background */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 0.1 }}
              className="absolute inset-0 bg-gradient-to-br from-blue-400 via-purple-400 to-pink-400"
            />

            {/* Floating particles */}
            {[...Array(6)].map((_, i) => (
              <motion.div
                key={i}
                initial={{
                  x: Math.random() * window.innerWidth,
                  y: window.innerHeight + 50,
                  opacity: 0
                }}
                animate={{
                  y: -50,
                  opacity: [0, 1, 0],
                  x: Math.random() * window.innerWidth
                }}
                transition={{
                  duration: 4 + Math.random() * 2,
                  repeat: Infinity,
                  delay: i * 0.5
                }}
                className="absolute w-2 h-2 bg-blue-400 rounded-full"
              />
            ))}
          </>
        )}

        {/* Celebration confetti for completion */}
        {isCompleted && (
          <>
            {[...Array(20)].map((_, i) => (
              <motion.div
                key={`confetti-${i}`}
                initial={{
                  x: window.innerWidth / 2,
                  y: window.innerHeight / 2,
                  opacity: 1,
                  scale: 0
                }}
                animate={{
                  x: Math.random() * window.innerWidth,
                  y: Math.random() * window.innerHeight,
                  opacity: 0,
                  scale: 1,
                  rotate: Math.random() * 360
                }}
                transition={{
                  duration: 2,
                  ease: "easeOut"
                }}
                className={`absolute w-3 h-3 ${
                  ['bg-green-400', 'bg-blue-400', 'bg-purple-400', 'bg-pink-400', 'bg-yellow-400'][i % 5]
                } rounded-full`}
              />
            ))}
          </>
        )}
      </div>

      <div className="text-center relative z-10">
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
        >
          <h2 className="text-3xl font-display font-bold text-gray-800 mb-2 bg-gradient-to-r from-adorable-blue to-adorable-purple bg-clip-text text-transparent">
            Traitement de vos Factures Électroniques
          </h2>
          <p className="text-gray-600 mb-2">
            Workflow unifié IconeSign - Signature, Validation & Archivage
          </p>

          {/* Enhanced dynamic status indicator */}
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className={`inline-flex items-center gap-3 px-4 py-2 rounded-full text-sm font-medium mb-4 ${
              getWorkflowStatus().bg
            } border border-opacity-20`}
          >
            <motion.div
              animate={isProcessing ? { rotate: 360 } : {}}
              transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
            >
              {React.createElement(getWorkflowStatus().icon, {
                className: `w-4 h-4 ${getWorkflowStatus().color}`
              })}
            </motion.div>

            <div className="flex flex-col">
              <span className={`font-medium ${getWorkflowStatus().color}`}>
                {isProcessing ? (
                  `Traitement en cours... (${overallProgress}%)`
                ) : results?.success ? (
                  'Traitement terminé avec succès'
                ) : results && !results.success ? (
                  'Traitement terminé avec des erreurs'
                ) : (
                  'En attente de démarrage'
                )}
              </span>

              {isProcessing && getCurrentActiveStage() && (
                <span className="text-xs opacity-75">
                  Étape active: {stages.find(s => s.id === getCurrentActiveStage())?.title || getCurrentActiveStage()}
                </span>
              )}
            </div>

            {/* Real-time session indicator */}
            {sessionId && (
              <div className="flex items-center gap-1 text-xs opacity-60">
                <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
                <span>Session: {sessionId.slice(-8)}</span>
              </div>
            )}
          </motion.div>
        </motion.div>

        {/* Enhanced Refresh Controls */}
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="flex flex-wrap items-center justify-center gap-4 text-sm text-gray-500 bg-gray-50 rounded-lg p-3"
        >
          <div className="flex items-center gap-2">
            <motion.div
              animate={{ scale: [1, 1.1, 1] }}
              transition={{ duration: 2, repeat: Infinity }}
            >
              <Clock className="w-4 h-4 text-blue-500" />
            </motion.div>
            <span>Dernière mise à jour: {lastRefresh.toLocaleTimeString('fr-FR')}</span>
          </div>

          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={handleManualRefresh}
            disabled={!isProcessing}
            className="flex items-center gap-1 px-3 py-1 rounded-full bg-white hover:bg-blue-50 transition-colors disabled:opacity-50 border border-gray-200 shadow-sm"
          >
            <motion.div
              animate={isProcessing ? { rotate: 360 } : {}}
              transition={{ duration: 1, repeat: isProcessing ? Infinity : 0, ease: "linear" }}
            >
              <RefreshCw className="w-3 h-3" />
            </motion.div>
            <span>Actualiser</span>
          </motion.button>

          <div className="flex items-center gap-3">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={autoRefreshEnabled}
                onChange={(e) => setAutoRefreshEnabled(e.target.checked)}
                className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              />
              <span className="select-none">Auto-actualisation</span>
              {autoRefreshEnabled && (
                <motion.div
                  animate={{ scale: [1, 1.2, 1] }}
                  transition={{ duration: 1, repeat: Infinity }}
                  className="w-2 h-2 bg-green-500 rounded-full"
                />
              )}
            </label>

            {autoRefreshEnabled && (
              <div className="text-xs text-gray-500 flex items-center gap-1">
                <Clock className="w-3 h-3" />
                <span>Toutes les {refreshInterval / 1000}s</span>
              </div>
            )}

            {/* Debug: Manual progress test */}
            {sessionId && (
              <button
                onClick={async () => {
                  console.log('Manual progress fetch for session:', sessionId)
                  await refreshProgress?.()
                }}
                className="text-xs px-2 py-1 bg-yellow-100 text-yellow-700 rounded hover:bg-yellow-200"
              >
                Test Progress API
              </button>
            )}
          </div>
        </motion.div>
      </div>

      {/* Overall Progress */}
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="text-lg font-bold text-gray-800">Progression Globale</span>
            <motion.div
              animate={{ rotate: isProcessing ? 360 : 0 }}
              transition={{ duration: 2, repeat: isProcessing ? Infinity : 0, ease: "linear" }}
              className={`w-6 h-6 rounded-full ${isProcessing ? 'bg-blue-500' : 'bg-green-500'}`}
            >
              {isProcessing ? (
                <Loader2 className="w-6 h-6 text-white" />
              ) : (
                <CheckCircle className="w-6 h-6 text-white" />
              )}
            </motion.div>
          </div>
          <div className="text-right">
            <div className="text-sm font-bold text-gray-700">
              {files.filter(f => f.status === 'completed').length} / {files.length} fichiers
            </div>
            <div className="text-xs text-gray-500">
              {Math.round((files.filter(f => f.status === 'completed').length / files.length) * 100)}% terminé
            </div>
          </div>
        </div>

        <div className="relative">
          <div className="w-full bg-gray-200 rounded-full h-4 shadow-inner">
            <motion.div
              initial={{ width: 0 }}
              animate={{
                width: `${(files.filter(f => f.status === 'completed').length / files.length) * 100}%`
              }}
              transition={{ duration: 1, ease: "easeOut" }}
              className="bg-gradient-to-r from-adorable-pink via-adorable-purple to-adorable-blue h-4 rounded-full shadow-lg relative overflow-hidden"
            >
              {/* Animated shine effect */}
              <motion.div
                initial={{ x: '-100%' }}
                animate={{ x: '100%' }}
                transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
                className="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent"
              />
            </motion.div>
          </div>

          {/* Progress percentage overlay */}
          <motion.div
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            className="absolute inset-0 flex items-center justify-center"
          >
            <span className="text-xs font-bold text-white drop-shadow-lg">
              {Math.round((files.filter(f => f.status === 'completed').length / files.length) * 100)}%
            </span>
          </motion.div>
        </div>
      </div>

      {/* Current Stage */}
      <div className="text-center">
        <motion.div
          animate={{ rotate: 360 }}
          transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
          className="inline-flex items-center justify-center w-16 h-16 bg-adorable-blue/10 rounded-full mb-4"
        >
          <Loader2 className="w-8 h-8 text-adorable-blue" />
        </motion.div>
        
        <p className="text-lg font-semibold text-gray-800 mb-2">
          {currentStage}
        </p>
      </div>

      {/* Workflow Stages */}
      <div className="space-y-6">
        <h3 className="text-lg font-semibold text-gray-800 text-center">Étapes du Workflow</h3>

        <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
          {stages.map((stage, index) => {
            const Icon = stage.icon
            const status = getStageStatus(stage.id)
            const progress = getStageProgress(stage.id)

            return (
              <motion.div
                key={stage.id}
                initial={{ opacity: 0, scale: 0 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: index * 0.1 }}
                className={`
                  flex flex-col items-center gap-3 p-4 rounded-xl border-2 transition-all duration-300 relative overflow-hidden
                  ${status === 'completed'
                    ? 'bg-gradient-to-br from-green-50 to-green-100 border-green-200 shadow-lg'
                    : status === 'active'
                    ? 'bg-gradient-to-br from-blue-50 to-blue-100 border-blue-200 shadow-lg'
                    : 'bg-white/50 border-gray-200'
                  }
                `}
              >
                {/* Background animation for active stage */}
                {status === 'active' && (
                  <motion.div
                    initial={{ x: '-100%' }}
                    animate={{ x: '100%' }}
                    transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
                    className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent"
                  />
                )}

                <motion.div
                  animate={status === 'active' ? { scale: [1, 1.05, 1] } : {}}
                  transition={{ duration: 2, repeat: Infinity }}
                  className={`
                    w-20 h-20 rounded-full border-3 flex items-center justify-center relative z-10
                    ${status === 'completed'
                      ? `bg-gradient-to-br ${stage.gradientFrom} ${stage.gradientTo} border-transparent text-white shadow-xl`
                      : status === 'active'
                      ? 'bg-gradient-to-br from-adorable-blue to-blue-600 border-blue-300 text-white shadow-xl'
                      : 'bg-gray-100 border-gray-300 text-gray-400'
                    }
                  `}
                >
                  {status === 'completed' ? (
                    <motion.div
                      initial={{ scale: 0 }}
                      animate={{ scale: 1 }}
                      transition={{ type: "spring", stiffness: 200 }}
                    >
                      <CheckCircle className="w-10 h-10" />
                    </motion.div>
                  ) : status === 'active' ? (
                    <motion.div
                      animate={{ rotate: 360 }}
                      transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
                    >
                      <Loader2 className="w-10 h-10" />
                    </motion.div>
                  ) : (
                    <Icon className="w-10 h-10" />
                  )}

                  {/* Progress ring for active stages */}
                  {status === 'active' && progress > 0 && (
                    <svg className="absolute inset-0 w-20 h-20 transform -rotate-90">
                      <circle
                        cx="40"
                        cy="40"
                        r="36"
                        stroke="currentColor"
                        strokeWidth="3"
                        fill="none"
                        className="text-blue-200"
                      />
                      <motion.circle
                        cx="40"
                        cy="40"
                        r="36"
                        stroke="currentColor"
                        strokeWidth="3"
                        fill="none"
                        strokeLinecap="round"
                        className="text-white"
                        initial={{ strokeDasharray: "0 226" }}
                        animate={{ strokeDasharray: `${(progress / 100) * 226} 226` }}
                        transition={{ duration: 1 }}
                      />
                    </svg>
                  )}

                  {/* Status indicator */}
                  <motion.div
                    initial={{ scale: 0 }}
                    animate={{ scale: 1 }}
                    transition={{ delay: 0.5 }}
                    className={`
                      absolute -top-1 -right-1 w-6 h-6 rounded-full border-3 border-white flex items-center justify-center
                      ${status === 'completed'
                        ? 'bg-green-500'
                        : status === 'active'
                        ? 'bg-blue-500 animate-pulse'
                        : 'bg-gray-300'
                      }
                    `}
                  >
                    {status === 'completed' && (
                      <CheckCircle className="w-4 h-4 text-white" />
                    )}
                    {status === 'active' && progress > 0 && (
                      <span className="text-xs font-bold text-white">{progress}%</span>
                    )}
                  </motion.div>
                </motion.div>

                <div className="text-center z-10 relative">
                  <motion.div
                    className={`text-sm font-bold mb-1 transition-colors duration-300 ${
                      status === 'completed' ? stage.color
                      : status === 'active' ? 'text-adorable-blue'
                      : 'text-gray-500'
                    }`}
                    animate={status === 'active' ? { scale: [1, 1.02, 1] } : {}}
                    transition={{ duration: 2, repeat: Infinity }}
                  >
                    {stage.title}
                  </motion.div>

                  <div className="text-xs text-gray-600 mb-2 leading-tight">
                    {stage.description}
                  </div>

                  {/* Dynamic status with progress */}
                  <motion.div
                    className={`text-xs font-bold px-2 py-1 rounded-full transition-all duration-300 ${
                      status === 'completed'
                        ? 'bg-green-100 text-green-700 border border-green-200'
                        : status === 'active'
                        ? 'bg-blue-100 text-blue-700 border border-blue-200'
                        : 'bg-gray-100 text-gray-500 border border-gray-200'
                    }`}
                    animate={status === 'active' ? { scale: [1, 1.05, 1] } : {}}
                    transition={{ duration: 1.5, repeat: Infinity }}
                  >
                    {status === 'completed' ? (
                      <span className="flex items-center gap-1">
                        <CheckCircle className="w-3 h-3" />
                        Terminé
                      </span>
                    ) : status === 'active' ? (
                      <span className="flex items-center gap-1">
                        <Loader2 className="w-3 h-3 animate-spin" />
                        {progress > 0 ? `${progress}%` : 'En cours...'}
                      </span>
                    ) : (
                      <span className="flex items-center gap-1">
                        <Clock className="w-3 h-3" />
                        En attente
                      </span>
                    )}
                  </motion.div>

                  {/* Files count and progress details */}
                  {status === 'active' && (
                    <motion.div
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      className="text-xs text-blue-600 mt-1 font-medium space-y-1"
                    >
                      <div>{getActiveFilesCount(stage.id)} fichier(s) actif(s)</div>
                      {progress > 0 && progress < 100 && (
                        <div className="w-full bg-blue-200 rounded-full h-1">
                          <motion.div
                            initial={{ width: 0 }}
                            animate={{ width: `${progress}%` }}
                            transition={{ duration: 0.5 }}
                            className="bg-blue-600 h-1 rounded-full"
                          />
                        </div>
                      )}
                    </motion.div>
                  )}

                  {status === 'completed' && (
                    <motion.div
                      initial={{ opacity: 0, scale: 0.8 }}
                      animate={{ opacity: 1, scale: 1 }}
                      className="text-xs text-green-600 mt-1 font-medium"
                    >
                      ✓ Tous les fichiers terminés
                    </motion.div>
                  )}
                </div>

                {/* Connection line to next stage */}
                {index < stages.length - 1 && (
                  <div className="hidden md:block absolute top-10 left-full w-4 h-0.5 bg-gray-300 transform translate-x-2">
                    {status === 'completed' && (
                      <motion.div
                        initial={{ width: 0 }}
                        animate={{ width: '100%' }}
                        transition={{ duration: 0.5, delay: 0.2 }}
                        className="h-full bg-green-400"
                      />
                    )}
                  </div>
                )}
              </motion.div>
            )
          })}
        </div>
      </div>

      {/* File Progress */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="font-semibold text-gray-800">Progression Détaillée des Fichiers</h3>
          <div className="text-sm text-gray-500">
            {files.filter(f => f.status === 'completed').length} terminé(s) • {' '}
            {files.filter(f => f.status === 'processing').length} en cours • {' '}
            {files.filter(f => f.status === 'error').length} erreur(s)
          </div>
        </div>

        <div className="space-y-3 max-h-64 overflow-y-auto">
          {files.map((file) => {
            const getStageLabel = (stage: string) => {
              const stageLabels: Record<string, string> = {
                'sign': 'Signature numérique',
                'save': 'Sauvegarde TTN',
                'validate': 'Validation ANCE',
                'transform': 'Transformation HTML',
                'package': 'Archivage ZIP',
                'idle': 'En attente',
                'processing': 'Traitement en cours',
                'complete': 'Terminé',
                'completed': 'Terminé avec succès',
                'failed': 'Échec'
              }
              return stageLabels[stage.toLowerCase()] || stage.charAt(0).toUpperCase() + stage.slice(1)
            }

            const getStageIcon = (stage: string) => {
              const stageIcons: Record<string, any> = {
                'sign': Shield,
                'save': FileText,
                'validate': CheckCircle,
                'transform': Globe,
                'package': Package,
                'complete': CheckCircle,
                'completed': CheckCircle,
                'failed': AlertTriangle
              }
              return stageIcons[stage.toLowerCase()] || FileText
            }

            const getStatusColor = (status: string, progress: number) => {
              if (status === 'error' || status === 'failed') return 'text-red-600 bg-red-50 border-red-200'
              if (status === 'completed' || progress === 100) return 'text-green-600 bg-green-50 border-green-200'
              if (status === 'processing') return 'text-blue-600 bg-blue-50 border-blue-200'
              return 'text-gray-600 bg-gray-50 border-gray-200'
            }

            return (
              <motion.div
                key={file.id}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                className={`flex items-center justify-between p-4 rounded-lg border-2 transition-all ${getStatusColor(file.status, file.progress)}`}
              >
                <div className="flex items-center gap-4 flex-1">
                  <div className={`p-2 rounded-lg ${
                    file.status === 'completed' ? 'bg-green-100' :
                    file.status === 'error' ? 'bg-red-100' :
                    'bg-adorable-blue/10'
                  }`}>
                    {React.createElement(getStageIcon(file.stage), {
                      className: 'w-5 h-5'
                    })}
                  </div>
                  <div className="flex-1">
                    <div className="font-medium text-gray-800">{file.file.name}</div>
                    <div className="text-xs text-gray-500">{getStageLabel(file.stage)}</div>
                    {file.ttnInvoiceId && (
                      <div className="text-xs text-gray-600">TTN ID: {file.ttnInvoiceId}</div>
                    )}
                    {file.error && (
                      <div className={`text-xs mt-1 ${file.error.includes('TTN') ? 'text-orange-600 font-semibold' : 'text-red-600'}`}>
                        {file.error.includes('TTN')
                          ? `Erreur TTN : ${file.error.replace(/TTN (a retourné une erreur|save failed):/i, '').trim()}`
                          : `Erreur : ${file.error}`}
                      </div>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-xs text-gray-500">
                    {(file.file.size / 1024).toFixed(1)} KB
                  </span>
                  <div className="flex items-center">
                    {file.status === 'completed' ? (
                      <div className="flex items-center gap-1">
                        <CheckCircle className="w-5 h-5 text-green-500" />
                        <span className="text-xs text-green-600 font-medium">Terminé</span>
                      </div>
                    ) : file.status === 'error' ? (
                      <div className="flex items-center gap-1">
                        <AlertTriangle className="w-5 h-5 text-red-500" />
                        <span className="text-xs text-red-600 font-medium">Erreur</span>
                      </div>
                    ) : (
                      <div className="flex items-center gap-1">
                        <motion.div
                          animate={{ rotate: 360 }}
                          transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
                        >
                          <Loader2 className="w-5 h-5 text-adorable-blue" />
                        </motion.div>
                        <div className="flex flex-col">
                          <span className="text-xs text-blue-600 font-medium">
                            {file.progress > 0 ? `${file.progress}%` : 'En cours'}
                          </span>
                          <span className="text-xs text-gray-500">
                            {getStageLabel(file.stage)}
                          </span>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </motion.div>
            )
          })}
        </div>

        {files.length === 0 && (
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className="text-center py-12 text-gray-500"
          >
            <motion.div
              animate={{ rotate: [0, 10, -10, 0] }}
              transition={{ duration: 2, repeat: Infinity }}
            >
              <FileText className="w-16 h-16 mx-auto mb-4 text-gray-300" />
            </motion.div>
            <p className="text-lg font-medium">Aucun fichier en cours de traitement</p>
            <p className="text-sm">Le workflow démarrera automatiquement</p>
          </motion.div>
        )}
      </div>

      {/* Debug Panel */}
      <div className="mt-4">
        <button
          onClick={() => setShowDebug(!showDebug)}
          className="text-xs text-gray-500 hover:text-gray-700 underline"
        >
          {showDebug ? 'Masquer' : 'Afficher'} les informations de débogage
        </button>

        {showDebug && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            className="mt-2 p-4 bg-gray-100 rounded-lg text-xs font-mono"
          >
            <div className="grid grid-cols-2 gap-4">
              <div>
                <h4 className="font-bold mb-2">État du Store:</h4>
                <div>Session ID: {sessionId || 'Non défini'}</div>
                <div>En traitement: {isProcessing ? 'Oui' : 'Non'}</div>
                <div>Étape actuelle: {currentStage}</div>
                <div>Progression globale: {overallProgress}%</div>
                <div>Nombre de fichiers: {files.length}</div>
                <div>Auto-refresh: {autoRefreshEnabled ? 'Activé' : 'Désactivé'}</div>
              </div>

              <div>
                <h4 className="font-bold mb-2">Fichiers:</h4>
                {files.map((file, index) => (
                  <div key={index} className="mb-1">
                    <div>{file.file.name}</div>
                    <div className="ml-2 text-gray-600">
                      Status: {file.status} | Stage: {file.stage} | Progress: {file.progress}%
                      {file.ttnInvoiceId && ` | TTN ID: ${file.ttnInvoiceId}`}
                      {file.error && ` | Error: ${file.error}`}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="mt-4">
              <h4 className="font-bold mb-2">Détection des étapes:</h4>
              <div>Étape API détectée: {getCurrentStageFromAPI()}</div>
              {stages.map(stage => (
                <div key={stage.id} className="ml-2">
                  {stage.id}: {getStageStatus(stage.id)} ({getStageProgress(stage.id)}%)
                </div>
              ))}
            </div>
          </motion.div>
        )}
      </div>

      {/* Dynamic Summary Section */}
      {files.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
          className="bg-gradient-to-r from-gray-50 to-blue-50 rounded-xl p-6 border border-gray-200"
        >
          <h3 className="text-lg font-bold text-gray-800 mb-4 flex items-center gap-2">
            <motion.div
              animate={{ rotate: isProcessing ? 360 : 0 }}
              transition={{ duration: 3, repeat: isProcessing ? Infinity : 0, ease: "linear" }}
            >
              📊
            </motion.div>
            Résumé du Traitement
          </h3>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <motion.div
              whileHover={{ scale: 1.05 }}
              className="bg-white rounded-lg p-4 text-center shadow-sm border"
            >
              <div className="text-2xl font-bold text-blue-600">
                {files.length}
              </div>
              <div className="text-xs text-gray-600">Total Fichiers</div>
            </motion.div>

            <motion.div
              whileHover={{ scale: 1.05 }}
              className="bg-white rounded-lg p-4 text-center shadow-sm border"
            >
              <div className="text-2xl font-bold text-green-600">
                {files.filter(f => f.status === 'completed').length}
              </div>
              <div className="text-xs text-gray-600">Terminés</div>
            </motion.div>

            <motion.div
              whileHover={{ scale: 1.05 }}
              className="bg-white rounded-lg p-4 text-center shadow-sm border"
            >
              <div className="text-2xl font-bold text-blue-600">
                {files.filter(f => f.status === 'processing').length}
              </div>
              <div className="text-xs text-gray-600">En Cours</div>
            </motion.div>

            <motion.div
              whileHover={{ scale: 1.05 }}
              className="bg-white rounded-lg p-4 text-center shadow-sm border"
            >
              <div className="text-2xl font-bold text-red-600">
                {files.filter(f => f.status === 'error').length}
              </div>
              <div className="text-xs text-gray-600">Erreurs</div>
            </motion.div>
          </div>

          {/* Current stage indicator with progress */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.8 }}
            className="mt-6 bg-white rounded-lg p-4 border border-gray-200"
          >
            <div className="text-sm text-gray-600 mb-2">Étape Actuelle:</div>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-adorable-blue flex items-center justify-center">
                  <motion.div
                    animate={{ rotate: isProcessing ? 360 : 0 }}
                    transition={{ duration: 2, repeat: isProcessing ? Infinity : 0, ease: "linear" }}
                  >
                    {React.createElement(stages.find(s => s.id === getCurrentStageFromAPI())?.icon || Shield, {
                      className: "w-4 h-4 text-white"
                    })}
                  </motion.div>
                </div>
                <div>
                  <div className="text-lg font-bold text-adorable-blue">
                    {stages.find(s => s.id === getCurrentStageFromAPI())?.title}
                  </div>
                  <div className="text-xs text-gray-500">
                    {stages.find(s => s.id === getCurrentStageFromAPI())?.description}
                  </div>
                </div>
              </div>

              <div className="text-right">
                <div className="text-2xl font-bold text-adorable-blue">
                  {Math.round(overallProgress)}%
                </div>
                <div className="text-xs text-gray-500">Progression</div>
              </div>
            </div>

            {/* Stage progress bar */}
            <div className="mt-3 w-full bg-gray-200 rounded-full h-2">
              <motion.div
                initial={{ width: 0 }}
                animate={{ width: `${overallProgress}%` }}
                transition={{ duration: 1 }}
                className="bg-gradient-to-r from-adorable-blue to-blue-600 h-2 rounded-full"
              />
            </div>
          </motion.div>
        </motion.div>
      )}
    </div>
  )
}
