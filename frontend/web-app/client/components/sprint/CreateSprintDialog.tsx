import { useState } from "react";
import { useParams } from "react-router-dom";
import { useCreateSprint } from "@/hooks/useSprints";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";

interface CreateSprintDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export default function CreateSprintDialog({ open, onOpenChange }: CreateSprintDialogProps) {
  const { projectId } = useParams<{ projectId: string }>();
  const createSprint = useCreateSprint(projectId);

  const [formData, setFormData] = useState({
    name: "",
    goal: "",
    startDate: new Date().toISOString().split("T")[0],
    endDate: "",
  });

  // Set default end date to 2 weeks from start
  const handleStartDateChange = (date: string) => {
    setFormData((prev) => {
      const start = new Date(date);
      const end = new Date(start);
      end.setDate(end.getDate() + 14);

      const prevStart = new Date(prev.startDate);
      const prevEnd = prev.endDate ? new Date(prev.endDate) : null;

      // Auto-update if end date is empty OR if it matches the default 2-week duration from previous start date
      const isDefaultDuration = prevEnd && prevStart &&
        prevEnd.getTime() === prevStart.getTime() + 14 * 24 * 60 * 60 * 1000;

      return {
        ...prev,
        startDate: date,
        endDate: !prev.endDate || isDefaultDuration ? end.toISOString().split("T")[0] : prev.endDate,
      };
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.name.trim()) {
      toast.error("Sprint name is required");
      return;
    }

    if (!formData.startDate || !formData.endDate) {
      toast.error("Start and end dates are required");
      return;
    }

    if (new Date(formData.endDate) <= new Date(formData.startDate)) {
      toast.error("End date must be after start date");
      return;
    }

    try {
      await createSprint.mutateAsync(formData);
      toast.success("Sprint created successfully");
      onOpenChange(false);
      setFormData({
        name: "",
        goal: "",
        startDate: new Date().toISOString().split("T")[0],
        endDate: "",
      });
    } catch (error: any) {
      const errorMessage = error?.response?.data?.error || error?.response?.data?.message || "Failed to create sprint";
      toast.error(errorMessage);
      console.error(error);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(isOpen) => {
      if (!isOpen && !createSprint.isPending) {
        setFormData({
          name: "",
          goal: "",
          startDate: new Date().toISOString().split("T")[0],
          endDate: "",
        });
      }
      onOpenChange(isOpen);
    }}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>Create New Sprint</DialogTitle>
          <DialogDescription>
            Create a new sprint to organize your work. Sprints typically last 1-4 weeks.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit}>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="name">Sprint Name *</Label>
              <Input
                id="name"
                placeholder="Sprint 1"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                maxLength={100}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="goal">Sprint Goal</Label>
              <Textarea
                id="goal"
                placeholder="What do you want to achieve in this sprint?"
                value={formData.goal}
                onChange={(e) => setFormData({ ...formData, goal: e.target.value })}
                maxLength={500}
                rows={3}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="startDate">Start Date *</Label>
                <Input
                  id="startDate"
                  type="date"
                  value={formData.startDate}
                  onChange={(e) => handleStartDateChange(e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="endDate">End Date *</Label>
                <Input
                  id="endDate"
                  type="date"
                  value={formData.endDate}
                  onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                  min={formData.startDate}
                />
              </div>
            </div>
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={createSprint.isPending}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={createSprint.isPending}>
              {createSprint.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Create Sprint
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
