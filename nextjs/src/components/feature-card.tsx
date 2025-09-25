'use client'

import { motion } from 'framer-motion'
import { ReactNode } from 'react'

interface FeatureCardProps {
  icon: ReactNode
  title: string
  description: string
  color: string
}

export function FeatureCard({ icon, title, description, color }: FeatureCardProps) {
  return (
    <motion.div
      whileHover={{ 
        scale: 1.05,
        rotateY: 5,
      }}
      whileTap={{ scale: 0.95 }}
      className="adorable-card p-6 cursor-pointer group"
    >
      <motion.div
        whileHover={{ rotate: 360 }}
        transition={{ duration: 0.6 }}
        className={`w-16 h-16 bg-gradient-to-r ${color} rounded-2xl flex items-center justify-center text-white mb-4 shadow-lg group-hover:shadow-xl transition-shadow duration-300`}
      >
        {icon}
      </motion.div>
      
      <h3 className="text-xl font-display font-semibold text-gray-800 mb-2 group-hover:text-adorable-purple transition-colors duration-300">
        {title}
      </h3>
      
      <p className="text-gray-600 leading-relaxed group-hover:text-gray-700 transition-colors duration-300">
        {description}
      </p>
      
      {/* Hover Effect */}
      <motion.div
        initial={{ width: 0 }}
        whileHover={{ width: '100%' }}
        transition={{ duration: 0.3 }}
        className={`h-1 bg-gradient-to-r ${color} rounded-full mt-4`}
      />
    </motion.div>
  )
}
