import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'

// https://vitejs.dev/config/
export default defineConfig(() => {
  return {
    plugins: [react()],
    resolve: {
      tsconfigPaths: true,
    },
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
  }
})
