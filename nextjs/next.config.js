/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  images: {
    domains: ['localhost'], // you might want to replace/remove this for prod
  },
  env: {
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL || '/api',
  },
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
    const internalApiUrl = process.env.INTERNAL_API_URL;
    if (!internalApiUrl) {
      throw new Error("INTERNAL_API_URL is not defined!");
    }

    return [
      {
        source: '/api/:path*',
        destination: `${internalApiUrl}/api/v1/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;
