'use client'

import React, { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { User, LogOut, Settings, ChevronDown, Building, Mail, Shield, CheckCircle } from 'lucide-react'
import { useAuthStore } from '@/store/auth-store'

export function UserProfile() {
  const { user, signOut } = useAuthStore()
  const [isOpen, setIsOpen] = useState(false)

  if (!user) return null

  const handleSignOut = () => {
    signOut()
    window.location.reload()
  }

  return (
    <div className="relative">
      {/* Profile Button */}
      <motion.button
        onClick={() => setIsOpen(!isOpen)}
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.98 }}
        className="flex items-center gap-3 bg-white/10 backdrop-blur-sm rounded-lg px-4 py-2 text-white hover:bg-white/20 transition-colors"
      >
        <div className="w-8 h-8 bg-gradient-to-r from-adorable-pink to-adorable-purple rounded-full flex items-center justify-center">
          <User className="w-4 h-4 text-white" />
        </div>
        
        <div className="text-left hidden sm:block">
          <div className="text-sm font-medium">
            {user.firstName} {user.lastName}
          </div>
          <div className="text-xs text-white/70">
            {user.companyName || user.email}
          </div>
        </div>
        
        <ChevronDown className={`w-4 h-4 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
      </motion.button>

      {/* Dropdown Menu */}
      <AnimatePresence>
        {isOpen && (
          <>
            {/* Backdrop */}
            <div
              className="fixed inset-0 z-40"
              onClick={() => setIsOpen(false)}
            />
            
            {/* Menu */}
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: -10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: -10 }}
              transition={{ duration: 0.2 }}
              className="absolute right-0 top-full mt-2 w-80 bg-white rounded-xl shadow-2xl border border-gray-200 z-50 overflow-hidden"
            >
              {/* Header */}
              <div className="bg-gradient-to-r from-adorable-blue to-adorable-purple p-4 text-white">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 bg-white/20 rounded-full flex items-center justify-center">
                    <User className="w-6 h-6" />
                  </div>
                  <div>
                    <div className="font-semibold">
                      {user.firstName} {user.lastName}
                    </div>
                    <div className="text-sm text-white/80">
                      @{user.username}
                    </div>
                  </div>
                </div>
              </div>

              {/* User Info */}
              <div className="p-4 space-y-3">
                <div className="flex items-center gap-3 text-sm text-gray-600">
                  <Mail className="w-4 h-4" />
                  <span>{user.email}</span>
                </div>
                
                {user.companyName && (
                  <div className="flex items-center gap-3 text-sm text-gray-600">
                    <Building className="w-4 h-4" />
                    <span>{user.companyName}</span>
                  </div>
                )}
                
                <div className="flex items-center gap-3 text-sm text-gray-600">
                  <Shield className="w-4 h-4" />
                  <span>Rôle: {user.role}</span>
                </div>

                {/* Credentials Status */}
                <div className="pt-2 border-t border-gray-100">
                  <div className="flex items-center gap-2 text-sm">
                    {user.hasCredentials ? (
                      <>
                        <CheckCircle className="w-4 h-4 text-green-500" />
                        <span className="text-green-600">Identifiants configurés</span>
                      </>
                    ) : (
                      <>
                        <div className="w-4 h-4 rounded-full bg-yellow-500" />
                        <span className="text-yellow-600">Identifiants manquants</span>
                      </>
                    )}
                  </div>
                </div>
              </div>

              {/* Actions */}
              <div className="border-t border-gray-100">
                <button
                  onClick={() => {
                    setIsOpen(false)
                    // TODO: Open settings modal
                  }}
                  className="w-full flex items-center gap-3 px-4 py-3 text-gray-700 hover:bg-gray-50 transition-colors"
                >
                  <Settings className="w-4 h-4" />
                  <span>Paramètres</span>
                </button>
                
                <button
                  onClick={handleSignOut}
                  className="w-full flex items-center gap-3 px-4 py-3 text-red-600 hover:bg-red-50 transition-colors"
                >
                  <LogOut className="w-4 h-4" />
                  <span>Se déconnecter</span>
                </button>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  )
}
