import { createRoot } from "react-dom/client";
import App from "./App";
import "./global.css";
import { errorTracking } from "@/services/errorTracking";

// Simple Production Performance Monitoring
if (import.meta.env.PROD) {
    window.addEventListener("load", () => {
        // Basic Web Vitals - Performance Paint Timing
        const paintTimings = performance.getEntriesByType("paint");
        paintTimings.forEach((entry) => {
            errorTracking.captureMessage(`${entry.name}: ${entry.startTime.toFixed(2)}ms`, "info", {
                type: "performance",
                metric: entry.name,
                value: entry.startTime,
            });
        });
    });
}

createRoot(document.getElementById("root")!).render(<App />);
