'use client'

import { useCallback, useState } from 'react'
import { useDropzone } from 'react-dropzone'
import { motion, AnimatePresence } from 'framer-motion'
import { Upload, FileText, X, AlertCircle, CheckCircle } from 'lucide-react'
import { useWorkflowStore } from '@/store/workflow-store'
import { toast } from 'sonner'

interface FileUploadZoneProps {
  onNext: () => void
}

export function FileUploadZone({ onNext }: FileUploadZoneProps) {
  const { files, addFiles, removeFile } = useWorkflowStore()
  const [dragActive, setDragActive] = useState(false)

  const onDrop = useCallback((acceptedFiles: File[], rejectedFiles: any[]) => {
    // Handle rejected files
    if (rejectedFiles.length > 0) {
      rejectedFiles.forEach(({ file, errors }) => {
        errors.forEach((error: any) => {
          toast.error(`${file.name}: ${error.message}`)
        })
      })
    }

    // Add accepted files
    if (acceptedFiles.length > 0) {
      addFiles(acceptedFiles)
      toast.success(`${acceptedFiles.length} fichier(s) ajouté(s)`)
    }
  }, [addFiles])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'text/xml': ['.xml'],
      'application/xml': ['.xml']
    },
    maxSize: 16 * 1024 * 1024, // 16MB
    multiple: true,
    onDragEnter: () => setDragActive(true),
    onDragLeave: () => setDragActive(false),
  })

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Octets'
    const k = 1024
    const sizes = ['Octets', 'Ko', 'Mo', 'Go']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  return (
    <div className="space-y-6">
      <div className="text-center mb-8">
        <h2 className="text-2xl font-display font-bold text-gray-800 mb-2">
          Téléchargez vos Factures XML
        </h2>
        <p className="text-gray-600">
          Glissez-déposez vos fichiers XML ici, ou cliquez pour parcourir
        </p>
      </div>

      {/* Upload Zone */}
      <div
        {...getRootProps()}
        className={`
          file-upload-zone cursor-pointer
          ${isDragActive || dragActive ? 'dragover' : ''}
        `}
      >
        <input {...getInputProps()} />

        <motion.div
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          animate={{
            y: isDragActive ? -10 : 0,
            scale: isDragActive ? 1.1 : 1,
          }}
          className="flex flex-col items-center gap-4"
        >
          <div className="p-4 bg-adorable-blue/10 rounded-full">
            <Upload className="w-12 h-12 text-adorable-blue" />
          </div>

          <div className="text-center">
            <p className="text-lg font-semibold text-gray-700 mb-2">
              {isDragActive ? 'Déposez les fichiers ici !' : 'Choisir des fichiers XML'}
            </p>
            <p className="text-sm text-gray-500">
              Supporte les fichiers XML jusqu'à 16 Mo chacun
            </p>
          </div>
        </motion.div>
      </div>

      {/* File List */}
      <AnimatePresence>
        {files.length > 0 && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="space-y-3"
          >
            <h3 className="font-semibold text-gray-800 flex items-center gap-2">
              <FileText className="w-5 h-5" />
              Fichiers Sélectionnés ({files.length})
            </h3>

            <div className="space-y-2 max-h-64 overflow-y-auto">
              {files.map((fileItem) => (
                <motion.div
                  key={fileItem.id}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: 20 }}
                  className="flex items-center justify-between p-3 bg-white/50 rounded-lg border border-gray-200 hover:border-adorable-blue/50 transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <div className="p-2 bg-adorable-blue/10 rounded-lg">
                      <FileText className="w-5 h-5 text-adorable-blue" />
                    </div>

                    <div>
                      <p className="font-medium text-gray-800 truncate max-w-xs">
                        {fileItem.file.name}
                      </p>
                      <p className="text-sm text-gray-500">
                        {formatFileSize(fileItem.file.size)}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    {fileItem.status === 'completed' && (
                      <CheckCircle className="w-5 h-5 text-green-500" />
                    )}
                    {fileItem.status === 'error' && (
                      <AlertCircle className="w-5 h-5 text-red-500" />
                    )}

                    <motion.button
                      whileHover={{ scale: 1.1 }}
                      whileTap={{ scale: 0.9 }}
                      onClick={() => removeFile(fileItem.id)}
                      className="p-1 hover:bg-red-100 rounded-full transition-colors"
                    >
                      <X className="w-4 h-4 text-red-500" />
                    </motion.button>
                  </div>
                </motion.div>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Next Button */}
      {files.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex justify-end pt-4"
        >
          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={onNext}
            className="adorable-button text-white font-semibold px-8 py-3 rounded-xl inline-flex items-center gap-2"
          >
            Continuer
            <motion.div
              animate={{ x: [0, 5, 0] }}
              transition={{ repeat: Infinity, duration: 1.5 }}
            >
              →
            </motion.div>
          </motion.button>
        </motion.div>
      )}
    </div>
  )
}
