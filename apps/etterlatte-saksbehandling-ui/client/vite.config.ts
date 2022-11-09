import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/brev': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/modiacontextholder': 'http://localhost:8080',
    },
  },
  build: {
    sourcemap: true,
  },
})
