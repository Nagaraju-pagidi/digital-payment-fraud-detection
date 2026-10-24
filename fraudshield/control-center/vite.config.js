import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// During local development (npm run dev) every /api call is forwarded to the API Gateway.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    proxy: {
      '/api': { target: 'http://localhost:8060', changeOrigin: true },
    },
  },
})
