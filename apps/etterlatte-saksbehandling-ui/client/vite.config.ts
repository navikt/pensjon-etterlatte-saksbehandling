import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'
import tsconfigPaths from 'vite-tsconfig-paths'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react(), tsconfigPaths()],
  server: {
    host: true,
    proxy: {
      '/api': {
        target: 'http://0.0.0.0:8080',
        changeOrigin: true,
      },
      '/brev': {
        target: 'http://0.0.0.0:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    sourcemap: true,
  },
})
