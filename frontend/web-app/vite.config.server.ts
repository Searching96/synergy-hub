// Server build configuration - DISABLED
// This configuration is no longer used since we're building client-only
// The server build was failing due to missing server/node-build.ts file

import { defineConfig } from "vite";
import path from "path";

export default defineConfig({
  // Empty configuration - server builds disabled
  build: {
    outDir: "dist/server",
    emptyOutDir: true,
    rollupOptions: {
      // No entry points configured
      external: [],
    },
  },
});
