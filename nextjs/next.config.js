/** @type {import('next').NextConfig} */
const nextConfig = {
  experimental: {
    appDir: true,
  },
  images: {
    domains: ['localhost'],
  },
  env: {
    // For client-side, use relative path so it works from any domain
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL || '',
  },
  async rewrites() {
    // Use environment variable for internal API URL (works in both Docker and K8s)
    const internalApiUrl = process.env.INTERNAL_API_URL || 'http://springboot-app:8080';
    
    return [
      {
        source: '/api/:path*',
        destination: `${internalApiUrl}/api/v1/:path*`, // Add /api/v1 context path to Spring Boot calls
      },
    ]
  },
}

module.exports = nextConfig