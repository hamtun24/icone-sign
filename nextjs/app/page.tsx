'use client'

import { useState } from 'react'
import { motion } from 'framer-motion'
import { FileText, Sparkles, Zap, Heart, Search, Shield } from 'lucide-react'
import { WorkflowProcessor } from '@/components/workflow-processor'
import { TtnConsult } from '@/components/ttn-consult'
import { FeatureCard } from '@/components/feature-card'
import { AnimatedBackground } from '@/components/animated-background'
import { AnceValidationPage } from '../src/components/ance-validation-page';



export default function HomePage() {
  const [showProcessor, setShowProcessor] = useState(false)
  const [showConsult, setShowConsult] = useState(false)
  const [showAnceValidator, setShowAnceValidator] = useState(false)

  const features = [
    {
      icon: <FileText className="w-8 h-8" />,
      title: "Signature Numérique",
      description: "Signature conforme XAdES-B avec ANCE SEAL",
      color: "from-blue-400 to-blue-600"
    },
    {
      icon: <Zap className="w-8 h-8" />,
      title: "Intégration TTN",
      description: "Intégration transparente du système e-facturation",
      color: "from-green-400 to-green-600"
    },
    {
      icon: <Sparkles className="w-8 h-8" />,
      title: "Validation Intelligente",
      description: "Validation complète des signatures XML",
      color: "from-purple-400 to-purple-600"
    },
    {
      icon: <Heart className="w-8 h-8" />,
      title: "Résultats Élégants",
      description: "Archive ZIP organisée avec tous les fichiers traités",
      color: "from-pink-400 to-pink-600"
    }
  ]

  return (
    <div className="min-h-screen relative overflow-hidden">
      <AnimatedBackground />
      <div className="relative z-10">
        {!showProcessor && !showConsult && !showAnceValidator ? (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
            className="container mx-auto px-4 py-16"
          >
            {/* Hero Section */}
            <div className="text-center mb-16">
              {/* Logo */}
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ delay: 0.2, type: "spring", stiffness: 200 }}
                className="mb-8"
              >
                <img
                  src="/icone.png"
                  alt="IconeSign Logo"
                  className="h-24 w-auto mx-auto mb-4"
                />
              </motion.div>

              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ delay: 0.3, type: "spring", stiffness: 200 }}
                className="inline-flex items-center gap-2 bg-white/20 backdrop-blur-sm rounded-full px-6 py-3 mb-8 border border-white/30"
              >
                <Sparkles className="w-5 h-5 text-adorable-pink animate-pulse" />
                <span className="text-sm font-medium text-gray-700">
                  Plateforme de Signature Électronique 
                </span>
              </motion.div>

              <motion.h1
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.4 }}
                className="text-6xl md:text-7xl font-display font-bold mb-6 bg-gradient-to-r from-adorable-pink via-adorable-purple to-adorable-blue bg-clip-text text-transparent"
              >
                IconeSign ✨
              </motion.h1>

              <motion.p
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.6 }}
                className="text-xl md:text-2xl text-gray-600 mb-12 max-w-3xl mx-auto leading-relaxed"
              >
                Transformez vos factures avec notre plateforme élégante qui gère la
                <span className="text-adorable-pink font-semibold"> signature</span>, la
                <span className="text-adorable-purple font-semibold"> validation</span> et le
                <span className="text-adorable-blue font-semibold"> traitement</span> en toute Securite
              </motion.p>

              <div className="flex flex-col sm:flex-row gap-4 justify-center">
                <motion.button
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: 0.8, type: "spring", stiffness: 200 }}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => setShowProcessor(true)}
                  className="adorable-button text-white font-semibold px-12 py-4 rounded-2xl text-lg inline-flex items-center gap-3 animate-glow"
                >
                 <Sparkles className="w-6 h-6" />
                  Signeé Des Factures
                 
                </motion.button>

                <motion.button
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: 0.9, type: "spring", stiffness: 200 }}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => setShowConsult(true)}
                  className="px-12 py-4 border-2 border-adorable-blue text-adorable-blue rounded-2xl text-lg font-semibold hover:bg-adorable-blue hover:text-white transition-all duration-300 inline-flex items-center gap-3"
                >
                  <Search className="w-6 h-6" />
                  Consulter Tradenet
                </motion.button>

                <motion.button
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: 1.0, type: "spring", stiffness: 200 }}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => {
                    setShowProcessor(false);
                    setShowConsult(false);
                    setShowAnceValidator(true);
                  }}
                  className="px-12 py-4 border-2 border-purple-500 text-purple-600 rounded-2xl text-lg font-semibold hover:bg-purple-500 hover:text-white transition-all duration-300 inline-flex items-center gap-3"
                >
                  <Shield className="w-6 h-6" />
                  Valider Avec ANCE
                </motion.button>
              </div>
            </div>

            {/* Features Grid */}
            <motion.div
              initial={{ opacity: 0, y: 40 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 1 }}
              className="grid md:grid-cols-2 lg:grid-cols-4 gap-8 mb-16"
            >
              {features.map((feature, index) => (
                <motion.div
                  key={index}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 1.2 + index * 0.1 }}
                >
                  <FeatureCard {...feature} />
                </motion.div>
              ))}
            </motion.div>

            {/* Workflow Steps */}
            <motion.div
              initial={{ opacity: 0, y: 40 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 1.4 }}
              className="adorable-card p-8 max-w-4xl mx-auto"
            >
              <h2 className="text-3xl font-display font-bold text-center mb-8 text-gray-800">
                Comment ça Fonctionne 🎯
              </h2>

              <div className="flex flex-col md:flex-row items-center justify-between gap-4">
                {[
                  { step: "1", title: "Télécharger", emoji: "📁", color: "bg-blue-500" },
                  { step: "2", title: "Signer", emoji: "✍️", color: "bg-green-500" },
                  { step: "3", title: "Valider", emoji: "✅", color: "bg-purple-500" },
                  { step: "4", title: "Transformer", emoji: "🔄", color: "bg-orange-500" },
                  { step: "5", title: "Télécharger", emoji: "📦", color: "bg-pink-500" }
                ].map((item, index) => (
                  <motion.div
                    key={index}
                    initial={{ opacity: 0, scale: 0 }}
                    animate={{ opacity: 1, scale: 1 }}
                    transition={{ delay: 1.6 + index * 0.1, type: "spring" }}
                    className="flex flex-col items-center gap-2"
                  >
                    <div className={`w-16 h-16 ${item.color} rounded-full flex items-center justify-center text-white font-bold text-xl shadow-lg`}>
                      {item.step}
                    </div>
                    <div className="text-2xl">{item.emoji}</div>
                    <div className="font-semibold text-gray-700">{item.title}</div>
                    {index < 4 && (
                      <div className="hidden md:block absolute top-8 left-full w-8 h-0.5 bg-gray-300 transform translate-x-4"></div>
                    )}
                  </motion.div>
                ))}
              </div>
            </motion.div>
          </motion.div>
        ) : showProcessor ? (
          <WorkflowProcessor onBack={() => setShowProcessor(false)} />
        ) : showConsult ? (
          <TtnConsult onBack={() => setShowConsult(false)} />
        ) : showAnceValidator ? (
          <AnceValidationPage onBack={() => setShowAnceValidator(false)} />
        ) : null}
      </div>
    </div>
  )
}
