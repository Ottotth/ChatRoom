import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/user': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/rooms': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/messages': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/avatars': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
})
