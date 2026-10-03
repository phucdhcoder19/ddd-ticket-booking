import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': new URL('./src', import.meta.url).pathname },
  },
  server: {
    port: 5180,
    proxy: {
      // With a real backend, set VITE_USE_MOCK=false and it works right away
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
