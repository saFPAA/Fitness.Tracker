import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'prompt',
      includeAssets: ['favicon.svg', 'icon-180.png'],
      manifest: {
        name: 'Fitness',
        short_name: 'Fitness',
        start_url: '/',
        scope: '/',
        display: 'standalone',
        background_color: '#101412',
        theme_color: '#101412',
        icons: [
          { src: 'icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'any maskable' },
        ],
      },
      workbox: {
        // API и Swagger никогда не отдаются из кэша и не подменяются index.html
        navigateFallbackDenylist: [/^\/api\//, /^\/swagger-ui/, /^\/v3\//],
      },
    }),
  ],
  server: {
    // В разработке запросы к /api идут на Spring Boot, CORS не нужен
    proxy: { '/api': 'http://localhost:8080' },
  },
})
