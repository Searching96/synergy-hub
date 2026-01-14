import { useState, useEffect } from "react";
import { WifiOff, Wifi } from "lucide-react";
import { AnimatePresence, motion } from "framer-motion";

export function OfflineBanner() {
    const [isOnline, setIsOnline] = useState(navigator.onLine);
    const [showReconnected, setShowReconnected] = useState(false);

    useEffect(() => {
        const handleOnline = () => {
            setIsOnline(true);
            setShowReconnected(true);
            setTimeout(() => setShowReconnected(false), 3000);
        };
        const handleOffline = () => setIsOnline(false);

        window.addEventListener("online", handleOnline);
        window.addEventListener("offline", handleOffline);

        return () => {
            window.removeEventListener("online", handleOnline);
            window.removeEventListener("offline", handleOffline);
        };
    }, []);

    return (
        <AnimatePresence>
            {!isOnline && (
                <motion.div
                    initial={{ y: -50, opacity: 0 }}
                    animate={{ y: 0, opacity: 1 }}
                    exit={{ y: -50, opacity: 0 }}
                    className="fixed top-0 left-0 right-0 z-[100] bg-red-600 text-white py-2 px-4 flex items-center justify-center gap-3 shadow-lg"
                >
                    <WifiOff className="h-4 w-4" />
                    <span className="text-sm font-medium">You are offline. Changes may not be saved.</span>
                </motion.div>
            )}
            {showReconnected && (
                <motion.div
                    initial={{ y: -50, opacity: 0 }}
                    animate={{ y: 0, opacity: 1 }}
                    exit={{ y: -50, opacity: 0 }}
                    className="fixed top-0 left-0 right-0 z-[100] bg-green-600 text-white py-2 px-4 flex items-center justify-center gap-3 shadow-lg"
                >
                    <Wifi className="h-4 w-4" />
                    <span className="text-sm font-medium">Connected! Syncing data...</span>
                </motion.div>
            )}
        </AnimatePresence>
    );
}
