import { create } from 'zustand'
import { devtools } from 'zustand/middleware'

// Helper function to map API stages to UI stages
function mapApiStageToUiStage(apiStage: string): WorkflowFile['stage'] {
  if (!apiStage) return 'sign'

  const stageMap: Record<string, WorkflowFile['stage']> = {
    'SIGN': 'sign',
    'SAVE': 'save',
    'VALIDATE': 'validate',
    'TRANSFORM': 'transform',
    'PACKAGE': 'complete',
    'COMPLETED': 'complete',
    'FAILED': 'complete'
  }

  return stageMap[apiStage.toUpperCase()] || apiStage.toLowerCase() as WorkflowFile['stage']
}

// Helper function to get readable stage messages
function getReadableStageMessage(stage: string, progress: number): string {
  const stageMessages: Record<string, string> = {
    'sign': 'Signature numérique en cours...',
    'save': 'Sauvegarde vers TTN...',
    'validate': 'Validation ANCE en cours...',
    'transform': 'Transformation HTML...',
    'complete': 'Finalisation...',
    'package': 'Création de l\'archive ZIP...'
  }

  const message = stageMessages[stage.toLowerCase()] || `Étape ${stage.toUpperCase()} en cours...`

  if (progress > 90) {
    return message.replace('en cours...', 'presque terminée...')
  }

  return message
}

export interface WorkflowFile {
  id: string
  file: File
  status: 'pending' | 'processing' | 'completed' | 'error'
  progress: number
  stage: 'upload' | 'sign' | 'save' | 'validate' | 'transform' | 'complete'
  error?: string
  ttnInvoiceId?: string
  completionMessage?: string
}

export interface WorkflowCredentials {
  ttn: {
    username: string
    password: string
    matriculeFiscal: string
  }
  anceSeal: {
    pin: string
    alias: string
    certificatePath: string
  }
}

export interface CertificateInfo {
  defaultPath: string
  availableCertificates: string[]
  supportedFormats: string[]
  instructions: string[]
}

export interface WorkflowResults {
  success: boolean
  totalFiles: number
  successfulFiles: number
  failedFiles: number
  zipDownloadUrl?: string
  message: string
  files: WorkflowFile[]
}

interface WorkflowState {
  // State
  files: WorkflowFile[]
  credentials: WorkflowCredentials | null
  certificateInfo: CertificateInfo | null
  isProcessing: boolean
  results: WorkflowResults | null
  currentStage: string
  overallProgress: number
  sessionId: string | null
  error: string | null

  // Actions
  addFiles: (files: File[]) => void
  removeFile: (fileId: string) => void
  clearFiles: () => void
  setCredentials: (credentials: WorkflowCredentials) => void
  fetchCertificateInfo: () => Promise<void>
  startProcessing: () => Promise<void>
  refreshProgress: () => Promise<void>
  updateFileProgress: (fileId: string, progress: number, stage: WorkflowFile['stage']) => void
  setResults: (results: WorkflowResults) => void
  setError: (error: string) => void
  clearError: () => void
  reset: () => void
}

export const useWorkflowStore = create<WorkflowState>()(
  devtools(
    (set, get) => ({
      // Initial state
      files: [],
      credentials: null,
      certificateInfo: null,
      isProcessing: false,
      results: null,
      currentStage: 'Prêt',
      overallProgress: 0,
      sessionId: null,
      error: null,

      // Actions
      addFiles: (newFiles: File[]) => {
        const workflowFiles: WorkflowFile[] = newFiles.map(file => ({
          id: crypto.randomUUID(),
          file,
          status: 'pending',
          progress: 0,
          stage: 'upload',
        }))
        
        set(state => ({
          files: [...state.files, ...workflowFiles]
        }))
      },

      removeFile: (fileId: string) => {
        set(state => ({
          files: state.files.filter(f => f.id !== fileId)
        }))
      },

      clearFiles: () => {
        set({ files: [] })
      },

      setCredentials: (credentials: WorkflowCredentials) => {
        set({ credentials })
      },

      fetchCertificateInfo: async () => {
        try {
          const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/certificates/info`)

          if (!response.ok) {
            throw new Error(`Failed to fetch certificate info: ${response.status}`)
          }

          const certificateInfo = await response.json()
          set({ certificateInfo })

        } catch (error) {
          console.error('Failed to fetch certificate info:', error)
          // Set fallback certificate info
          set({
            certificateInfo: {
              defaultPath: 'classpath:certificates/icone.crt',
              availableCertificates: [],
              supportedFormats: ['.crt', '.pem', '.cer'],
              instructions: [
                'Placez votre fichier de certificat dans src/main/resources/certificates/',
                'Le certificat par défaut doit être nommé icone.crt'
              ]
            }
          })
        }
      },

      refreshProgress: async () => {
        const { isProcessing, sessionId } = get()

        if (!isProcessing || !sessionId) {
          console.log('⏸️ Skipping progress refresh - not processing or no session')
          return
        }

        try {
          // Get auth token
          const token = localStorage.getItem('auth_token')

          const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/progress/${sessionId}`, {
            method: 'GET',
            headers: {
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json',
              'Cache-Control': 'no-cache'
            },
          })

          if (!response.ok) {
            if (response.status === 404) {
              console.warn('⚠️ Session not found - may have expired:', sessionId.slice(-8))
              set({
                isProcessing: false,
                error: 'Session expired or not found'
              })
              return
            }
            console.warn('⚠️ Failed to fetch progress:', response.status, response.statusText)
            return
          }

          const progressData = await response.json()
          console.log('📊 Progress data received:', {
            sessionId: sessionId.slice(-8),
            status: progressData.status,
            overallProgress: progressData.overallProgress,
            filesCount: progressData.files?.length || 0,
            timestamp: new Date().toLocaleTimeString()
          })

          // Enhanced progress data validation and mapping
          if (!progressData.files || !Array.isArray(progressData.files)) {
            console.warn('⚠️ Invalid progress data structure - missing or invalid files array')
            return
          }

          // Update state with real progress data
          set(state => {
            const updatedFiles = state.files.map(file => {
              const fileProgress = progressData.files?.find((fp: any) =>
                fp.filename === file.file.name || fp.fileName === file.file.name
              )

              if (fileProgress) {
                // Enhanced API status to UI status mapping
                const uiStatus = fileProgress.status === 'COMPLETED' ? 'completed' :
                               fileProgress.status === 'FAILED' ? 'error' :
                               fileProgress.status === 'ERROR' ? 'error' :
                               fileProgress.status === 'PROCESSING' ? 'processing' :
                               fileProgress.status === 'PENDING' ? 'pending' : 'pending'

                // Map API stage to UI stage with fallback
                const uiStage = mapApiStageToUiStage(fileProgress.stage || fileProgress.currentStage) || 'sign'

                // Don't treat completion messages as errors
                const errorMessage = fileProgress.errorMessage || fileProgress.error
                const isCompletionMessage = errorMessage && (
                  errorMessage.includes('Traitement terminé') ||
                  errorMessage.includes('terminé avec succès') ||
                  errorMessage.includes('TTN ID:') ||
                  errorMessage.includes('Validation ANCE terminée') ||
                  errorMessage.includes('Transformation HTML terminée')
                )

                const updatedFile = {
                  ...file,
                  status: uiStatus as WorkflowFile['status'],
                  stage: uiStage as WorkflowFile['stage'],
                  progress: Math.min(100, Math.max(0, fileProgress.progress || 0)), // Clamp between 0-100
                  error: (uiStatus === 'completed' ) ? null : errorMessage, // Don't show completion messages as errors
                  ttnInvoiceId: fileProgress.ttnInvoiceId || fileProgress.invoiceId,
                  completionMessage: (uiStatus === 'completed' && isCompletionMessage) ? errorMessage : null // Store completion message separately
                }

                console.log(`📄 Updated file ${file.file.name}:`, {
                  status: updatedFile.status,
                  stage: updatedFile.stage,
                  progress: updatedFile.progress,
                  hasError: !!updatedFile.error,
                  hasTtnId: !!updatedFile.ttnInvoiceId,
                  ttnId: updatedFile.ttnInvoiceId || 'Not set'
                })

                // Special log for TTN ID updates
                if (updatedFile.ttnInvoiceId && !file.ttnInvoiceId) {
                  console.log(`🎯 TTN ID received for ${file.file.name}: ${updatedFile.ttnInvoiceId}`)
                }

                return updatedFile
              }
              return file
            })

            // Calculate overall progress more accurately
            const totalProgress = updatedFiles.reduce((sum, file) => sum + file.progress, 0)
            const averageProgress = updatedFiles.length > 0 ? totalProgress / updatedFiles.length : 0

            // Determine current stage from most advanced file
            const currentStageFromFiles = updatedFiles.reduce((mostAdvanced, file) => {
              const stageOrder = ['sign', 'save', 'validate', 'transform', 'package']
              const currentIndex = stageOrder.indexOf(mostAdvanced)
              const fileIndex = stageOrder.indexOf(file.stage)
              return fileIndex > currentIndex ? file.stage : mostAdvanced
            }, 'sign')

            // Check if workflow is complete
            const isWorkflowComplete = progressData.status === 'COMPLETED' || progressData.status === 'FAILED'

            if (isWorkflowComplete) {
              // Set final results when workflow is complete
              const finalResults: WorkflowResults = {
                success: progressData.status === 'COMPLETED',
                totalFiles: updatedFiles.length,
                successfulFiles: updatedFiles.filter(f => f.status === 'completed').length,
                failedFiles: updatedFiles.filter(f => f.status === 'error').length,
                zipDownloadUrl: progressData.zipDownloadUrl,
                message: progressData.message || (progressData.status === 'COMPLETED' ? 'Traitement terminé avec succès' : 'Traitement terminé avec des erreurs'),
                files: updatedFiles // Include the files array
              }

              return {
                files: updatedFiles,
                overallProgress: 100,
                currentStage: progressData.status === 'COMPLETED' ? 'Traitement terminé avec succès' : 'Traitement terminé avec erreurs',
                isProcessing: false,
                results: finalResults
              }
            }

            return {
              files: updatedFiles,
              overallProgress: Math.round(averageProgress),
              currentStage: progressData.message || getReadableStageMessage(currentStageFromFiles, averageProgress)
            }
          })
        } catch (error) {
          console.error('❌ Failed to refresh progress:', error)

          // Enhanced error handling
          const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred'

          set(state => ({
            error: `Progress refresh failed: ${errorMessage}`,
            // Don't stop processing immediately on network errors - allow retries
            isProcessing: errorMessage.includes('Session') ? false : state.isProcessing
          }))
        }
      },

      // Enhanced error handling method
      setError: (error: string) => {
        console.error('🚨 Workflow error:', error)
        set({ error, isProcessing: false })
      },

      // Clear error state
      clearError: () => {
        set({ error: null })
      },

      startProcessing: async () => {
        const { files } = get()

        if (files.length === 0) {
          return
        }

        set({ isProcessing: true, currentStage: 'Préparation des fichiers...' })

        // Update files to processing status
        set(state => ({
          files: state.files.map(file => ({
            ...file,
            status: 'processing' as const,
            stage: 'sign' as const,
            progress: 10
          }))
        }))

        try {
          // Prepare form data
          const formData = new FormData()

          // Add files
          files.forEach(({ file }) => {
            formData.append('files', file)
          })

          // Credentials are now handled by user authentication - no need to send them

          // Update stage
          set({ currentStage: 'Traitement des fichiers via le workflow unifié IconeSign...' })

          // Call the unified API
          // Get auth token
          const token = localStorage.getItem('auth_token')

          const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/workflow/process-invoices`, {
            method: 'POST',
            headers: {
              'Authorization': `Bearer ${token}`,
            },
            body: formData,
          })

          if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`)
          }

          const result = await response.json()

          // Store session ID for progress tracking
          const sessionId = result.sessionId
          if (sessionId) {
            set({ sessionId })
            console.log('Session ID received:', sessionId)

            // Don't update files to completed - let the progress tracking handle this
            // The backend now returns immediately with just the session ID
            // Progress updates will come through the refreshProgress mechanism

            // Keep files in processing state and let progress tracking update them
            set(state => ({
              files: state.files.map(file => ({
                ...file,
                status: 'processing' as const,
                stage: 'sign' as const,
                progress: 5 // Small initial progress
              }))
            }))
          } else {
            // If no session ID, this might be an error response
            console.error('No session ID received:', result)
            set({
              isProcessing: false,
              currentStage: 'Erreur: ' + (result.message || 'Pas de session ID reçu')
            })
          }

        } catch (error) {
          console.error('Échec du traitement du workflow:', error)

          // Update all files to error state
          const errorFiles = files.map(file => ({
            ...file,
            status: 'error' as const,
            error: error instanceof Error ? error.message : 'Erreur inconnue',
          }))

          set({
            files: errorFiles,
            isProcessing: false,
            currentStage: 'Erreur survenue',
            results: {
              success: false,
              totalFiles: files.length,
              successfulFiles: 0,
              failedFiles: files.length,
              message: error instanceof Error ? error.message : 'Échec du traitement',
              files: errorFiles,
            } as WorkflowResults
          })
        }
      },

      updateFileProgress: (fileId: string, progress: number, stage: WorkflowFile['stage']) => {
        set(state => ({
          files: state.files.map(file =>
            file.id === fileId
              ? { ...file, progress, stage, status: 'processing' as const }
              : file
          )
        }))
      },

      setResults: (results: WorkflowResults) => {
        set({ results })
      },

      reset: () => {
        set({
          files: [],
          credentials: null,
          isProcessing: false,
          results: null,
          currentStage: 'Prêt',
          overallProgress: 0,
          sessionId: null,
        })
      },
    }),
    {
      name: 'workflow-store',
    }
  )
)
