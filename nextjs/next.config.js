/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  images: {
    domains: ['localhost'],
  },
  env: {
    // For client-side, use relative path so it works from any domain
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL || '/api',
  },
  // Allow all hosts for Replit proxy environment
  async headers() {
    return [
      {
        source: '/(.*)',
        headers: [
          {
            key: 'Access-Control-Allow-Origin',
            value: '*',
          },
        ],
      },
    ]
  },
  async rewrites() {
    // Use localhost for Replit environment
    const internalApiUrl = process.env.INTERNAL_API_URL || 'http://localhost:8080';
    
    return [
      {
        source: '/api/:path*',
        destination: `${internalApiUrl}/api/v1/:path*`, // Add /api/v1 context path to Spring Boot calls
      },
    ]
  },
}

module.exports = nextConfig