'use client'

import React, { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Loader2, Shield } from 'lucide-react'
import { useAuthStore } from '@/store/auth-store'
import { SignInForm } from './signin-form'

interface AuthGuardProps {
  children: React.ReactNode
}

export function AuthGuard({ children }: AuthGuardProps) {
  const { isAuthenticated, isLoading, checkAuth, user } = useAuthStore()
  const [isInitializing, setIsInitializing] = useState(true)

  useEffect(() => {
    const initAuth = async () => {
      await checkAuth()
      setIsInitializing(false)
    }

    initAuth()
  }, [checkAuth])

  // Show loading spinner during initial authentication check
  if (isInitializing || isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-adorable-blue via-adorable-purple to-adorable-pink">
        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          className="text-center text-white"
        >
          <motion.div
            animate={{ rotate: 360 }}
            transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
            className="w-16 h-16 mx-auto mb-4"
          >
            <Loader2 className="w-16 h-16" />
          </motion.div>
          <h2 className="text-xl font-semibold mb-2">Vérification de l'authentification</h2>
          <p className="text-white/80">Veuillez patienter...</p>
        </motion.div>
      </div>
    )
  }

  // Show sign-in form if not authenticated
  if (!isAuthenticated) {
    return <SignInForm onSuccess={() => window.location.reload()} />
  }

  // Show warning if user doesn't have credentials configured
  if (user && !user.hasCredentials) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-adorable-blue via-adorable-purple to-adorable-pink p-4">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-white/95 backdrop-blur-sm rounded-2xl shadow-2xl p-8 border border-white/20 max-w-md w-full text-center"
        >
          <div className="w-16 h-16 bg-gradient-to-r from-yellow-400 to-orange-500 rounded-full flex items-center justify-center mx-auto mb-4">
            <Shield className="w-8 h-8 text-white" />
          </div>
          
          <h2 className="text-2xl font-bold text-gray-800 mb-4">
            Configuration requise
          </h2>
          
          <p className="text-gray-600 mb-6">
            Vos identifiants TTN et ANCE SEAL ne sont pas configurés. 
            Veuillez contacter l'administrateur pour configurer vos accès.
          </p>
          
          <div className="space-y-3 text-sm text-gray-500">
            <p>✓ Compte utilisateur créé</p>
            <p>✗ Identifiants TTN manquants</p>
            <p>✗ Identifiants ANCE SEAL manquants</p>
          </div>
          
          <button
            onClick={() => useAuthStore.getState().signOut()}
            className="mt-6 px-4 py-2 text-sm text-gray-600 hover:text-gray-800 transition-colors"
          >
            Se déconnecter
          </button>
        </motion.div>
      </div>
    )
  }

  // Render protected content
  return <>{children}</>
}
