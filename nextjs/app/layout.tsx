import type { Metadata } from 'next'
import { Inter, Poppins } from 'next/font/google'
import './globals.css'
import { Toaster } from '@/components/ui/sonner'
import { ThemeProvider } from '@/components/theme-provider'
import { AuthGuard } from '@/components/auth/auth-guard'

const inter = Inter({ 
  subsets: ['latin'],
  variable: '--font-inter',
})

const poppins = Poppins({ 
  subsets: ['latin'],
  weight: ['300', '400', '500', '600', '700'],
  variable: '--font-poppins',
})

export const metadata: Metadata = {
  title: 'IconeSign - Plateforme de Signature Électronique',
  description: 'Interface moderne et élégante pour traiter vos factures électroniques : Signer → Sauvegarder → Valider → Transformer → Télécharger',
  keywords: ['facture', 'signature électronique', 'TTN', 'ANCE SEAL', 'e-facturation', 'IconeSign'],
  authors: [{ name: 'Équipe IconeSign' }],
  viewport: 'width=device-width, initial-scale=1',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={`${inter.variable} ${poppins.variable} font-sans antialiased`}>
        <ThemeProvider
          attribute="class"
          defaultTheme="light"
          enableSystem
          disableTransitionOnChange
        >
          <AuthGuard>
            <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50">
              {children}
            </div>
          </AuthGuard>
          <Toaster position="top-right" />
        </ThemeProvider>
      </body>
    </html>
  )
}
