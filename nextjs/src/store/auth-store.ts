'use client'

import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface User {
  id: number
  username: string
  email: string
  firstName: string
  lastName: string
  companyName?: string
  role: string
  hasCredentials: boolean
}

export interface AuthState {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  isLoading: boolean
  error: string | null
}

export interface AuthActions {
  signIn: (email: string, password: string) => Promise<void>
  signOut: () => void
  clearError: () => void
  checkAuth: () => Promise<void>
}

export const useAuthStore = create<AuthState & AuthActions>()(
  persist(
    (set, get) => ({
      // State
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,

      // Actions
      signIn: async (email: string, password: string) => {
        set({ isLoading: true, error: null })

        try {
          const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/auth/signin`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              usernameOrEmail: email,
              password: password,
            }),
          })

          const data = await response.json()

          if (!response.ok) {
            throw new Error(data.message || 'Erreur de connexion')
          }

          if (data.success && data.data) {
            const { token, ...userData } = data.data
            
            set({
              user: userData,
              token: token,
              isAuthenticated: true,
              isLoading: false,
              error: null,
            })

            // Store token in localStorage for API requests
            localStorage.setItem('auth_token', token)
          } else {
            throw new Error(data.message || 'Réponse invalide du serveur')
          }
        } catch (error) {
          console.error('Sign in error:', error)
          set({
            user: null,
            token: null,
            isAuthenticated: false,
            isLoading: false,
            error: error instanceof Error ? error.message : 'Erreur de connexion inconnue',
          })
          throw error
        }
      },

      signOut: () => {
        localStorage.removeItem('auth_token')
        set({
          user: null,
          token: null,
          isAuthenticated: false,
          isLoading: false,
          error: null,
        })
      },

      clearError: () => {
        set({ error: null })
      },

      checkAuth: async () => {
        const token = localStorage.getItem('auth_token')
        
        if (!token) {
          set({ isAuthenticated: false, user: null, token: null })
          return
        }

        set({ isLoading: true })

        try {
          const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/auth/me`, {
            headers: {
              'Authorization': `Bearer ${token}`,
            },
          })

          if (!response.ok) {
            throw new Error('Token invalide')
          }

          const data = await response.json()

          if (data.success && data.data) {
            set({
              user: data.data,
              token: token,
              isAuthenticated: true,
              isLoading: false,
              error: null,
            })
          } else {
            throw new Error('Données utilisateur invalides')
          }
        } catch (error) {
          console.error('Auth check error:', error)
          localStorage.removeItem('auth_token')
          set({
            user: null,
            token: null,
            isAuthenticated: false,
            isLoading: false,
            error: null,
          })
        }
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
)
