import { defineConfig } from 'vite'

// Standard: nur localhost. Heimnetz nur bewusst mit `npm run dev:lan`.
export default defineConfig({
  server: { host: 'localhost', port: 5173, strictPort: true, fs: { allow: ['..'] } },
})
