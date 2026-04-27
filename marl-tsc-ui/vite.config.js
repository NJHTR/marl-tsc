import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api/v1/web': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api/v1/drl': {
        target: 'http://localhost:8084',
        changeOrigin: true
      },
      '/api/v1/signal': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/api/v1/route': {
        target: 'http://localhost:8082',
        changeOrigin: true
      },
      '/api/v1/fusion': {
        target: 'http://localhost:8085',
        changeOrigin: true
      },
      '/api/v1/coopt': {
        target: 'http://localhost:8083',
        changeOrigin: true
      },
      '/api/v1/stream': {
        target: 'http://localhost:8086',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: resolve(__dirname, '../marl-tsc-web/src/main/resources/static'),
    emptyOutDir: true
  }
})
