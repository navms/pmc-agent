import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/stream': { target: 'http://localhost:8080', changeOrigin: true },
      '/resume_stream': { target: 'http://localhost:8080', changeOrigin: true },
      '/sessions': { target: 'http://localhost:8080', changeOrigin: true },
      '/files': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
