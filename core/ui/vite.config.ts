import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * The dashboard is served two different ways, and both have to work.
 *
 * In development `npm run dev` serves it from Vite's own port and proxies /api
 * to the Java host, so the browser sees one origin. In production `npm run
 * build` emits dist/, which ApiServer serves directly — same relative /api
 * paths, no proxy involved. Code that fetches '/api/...' is correct in both.
 */
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
