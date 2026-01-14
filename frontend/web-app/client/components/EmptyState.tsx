import { LucideIcon } from "lucide-react";
import { Button } from "@/components/ui/button";

interface EmptyStateProps {
    icon: LucideIcon;
    title: string;
    description: string;
    actionLabel?: string;
    onAction?: () => void;
}

export function EmptyState({
    icon: Icon,
    title,
    description,
    actionLabel,
    onAction }: EmptyStateProps) {
    return (
        <div className="flex flex-col items-center justify-center p-12 text-center bg-gray-50/50 rounded-xl border-2 border-dashed border-gray-200 animate-in fade-in zoom-in duration-300">
            <div className="bg-white p-4 rounded-full shadow-sm mb-4 border border-gray-100">
                <Icon className="h-10 w-10 text-gray-400" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-1">{title}</h3>
            <p className="text-sm text-gray-500 max-w-xs mx-auto mb-6">
                {description}
            </p>
            {actionLabel && onAction && (
                <Button onClick={onAction} className="bg-blue-600 hover:bg-blue-700">
                    {actionLabel}
                </Button>
            )}
        </div>
    );
}
