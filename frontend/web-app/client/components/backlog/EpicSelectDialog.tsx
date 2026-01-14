import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useState } from "react";
import { Loader2 } from "lucide-react";

interface Epic {
    id: string;
    name: string;
}

interface EpicSelectDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    epics: Epic[];
    onSelect: (epicId: string) => Promise<void>;
    isLoading?: boolean;
}

export default function EpicSelectDialog({
    open,
    onOpenChange,
    epics,
    onSelect,
    isLoading
}: EpicSelectDialogProps) {
    const [selectedEpicId, setSelectedEpicId] = useState<string>("");

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!selectedEpicId) return;
        await onSelect(selectedEpicId);
        setSelectedEpicId("");
        onOpenChange(false);
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[425px]">
                <DialogHeader>
                    <DialogTitle>Add to Epic</DialogTitle>
                    <DialogDescription>
                        Select an epic for this task.
                    </DialogDescription>
                </DialogHeader>
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="space-y-2">
                        <Select value={selectedEpicId} onValueChange={setSelectedEpicId}>
                            <SelectTrigger>
                                <SelectValue placeholder="Select an epic..." />
                            </SelectTrigger>
                            <SelectContent>
                                {epics.map((epic) => (
                                    <SelectItem key={epic.id} value={epic.id}>
                                        {epic.name}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                    <DialogFooter>
                        <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={!selectedEpicId || isLoading}>
                            {isLoading && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                            Add to Epic
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    );
}
