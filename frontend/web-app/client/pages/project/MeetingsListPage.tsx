import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useProject } from "@/context/ProjectContext";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Video, Plus, Users, Clock, AlertCircle, ExternalLink, Copy, Check } from "lucide-react";
import { mockCreateMeeting, mockGetProjectMeetings, generateMeetingCode } from "@/lib/mockMeeting";
import type { Meeting } from "@/types/meeting.types";
import { formatDistanceToNow } from "date-fns";

export default function MeetingsListPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const { project } = useProject();
  const navigate = useNavigate();

  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [copiedCode, setCopiedCode] = useState<string | null>(null);

  // Form state
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");

  useEffect(() => {
    loadMeetings();
  }, [projectId]);

  const loadMeetings = async () => {
    if (!projectId) return;

    try {
      setIsLoading(true);
      const projectMeetings = await mockGetProjectMeetings(parseInt(projectId));
      setMeetings(projectMeetings);
    } catch (err) {
      console.error("Failed to load meetings:", err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateMeeting = async () => {
    if (!projectId || !title.trim()) return;

    try {
      setIsCreating(true);
      const newMeeting = await mockCreateMeeting({
        projectId: parseInt(projectId),
        title: title.trim(),
        description: description.trim() || undefined,
      });

      // Navigate to meeting room
      navigate(`/projects/${projectId}/meetings/${newMeeting.id}`);
    } catch (err) {
      console.error("Failed to create meeting:", err);
    } finally {
      setIsCreating(false);
      setIsCreateDialogOpen(false);
      setTitle("");
      setDescription("");
    }
  };

  const handleJoinMeeting = (meetingId: string) => {
    navigate(`/projects/${projectId}/meetings/${meetingId}`);
  };

  const handleCopyCode = (code: string) => {
    navigator.clipboard.writeText(code);
    setCopiedCode(code);
    setTimeout(() => setCopiedCode(null), 2000);
  };

  const getStatusBadge = (status: Meeting["status"]) => {
    switch (status) {
      case "IN_PROGRESS":
        return <Badge variant="default" className="bg-green-600">Live</Badge>;
      case "SCHEDULED":
        return <Badge variant="secondary">Scheduled</Badge>;
      case "ENDED":
        return <Badge variant="outline">Ended</Badge>;
      case "CANCELLED":
        return <Badge variant="destructive">Cancelled</Badge>;
    }
  };

  if (!projectId) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-center">
          <AlertCircle className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
          <p className="text-muted-foreground">Project not found</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold mb-1">Meetings</h1>
          <p className="text-muted-foreground">
            Video conferences for {project?.name || "this project"}
          </p>
        </div>

        <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="h-4 w-4 mr-2" />
              New Meeting
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Create New Meeting</DialogTitle>
              <DialogDescription>
                Start an instant meeting or schedule one for later
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4">
              <div>
                <Label htmlFor="title">Meeting Title *</Label>
                <Input
                  id="title"
                  placeholder="Sprint Planning Meeting"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                />
              </div>

              <div>
                <Label htmlFor="description">Description (optional)</Label>
                <Textarea
                  id="description"
                  placeholder="Discuss upcoming sprint goals..."
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  rows={3}
                />
              </div>
            </div>

            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => setIsCreateDialogOpen(false)}
              >
                Cancel
              </Button>
              <Button
                onClick={handleCreateMeeting}
                disabled={!title.trim() || isCreating}
              >
                {isCreating ? "Creating..." : "Start Meeting"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>

      {/* Meetings List */}
      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <p className="text-muted-foreground">Loading meetings...</p>
        </div>
      ) : meetings.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Video className="h-16 w-16 text-muted-foreground mb-4" />
            <h3 className="text-lg font-semibold mb-2">No meetings yet</h3>
            <p className="text-muted-foreground text-center mb-4">
              Start your first video meeting with your team
            </p>
            <Button onClick={() => setIsCreateDialogOpen(true)}>
              <Plus className="h-4 w-4 mr-2" />
              Create Meeting
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {meetings.map((meeting) => (
            <Card key={meeting.id} className="hover:shadow-lg transition-shadow">
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <CardTitle className="text-lg">{meeting.title}</CardTitle>
                    <CardDescription className="mt-1">
                      {meeting.description}
                    </CardDescription>
                  </div>
                  {getStatusBadge(meeting.status)}
                </div>
              </CardHeader>

              <CardContent className="space-y-3">
                <div className="flex items-center gap-2 text-sm">
                  <Users className="h-4 w-4 text-muted-foreground" />
                  <span>{meeting.participants.length} participants</span>
                </div>

                {meeting.startedAt && (
                  <div className="flex items-center gap-2 text-sm">
                    <Clock className="h-4 w-4 text-muted-foreground" />
                    <span>
                      {meeting.status === "IN_PROGRESS"
                        ? "Started " +
                          formatDistanceToNow(new Date(meeting.startedAt), {
                            addSuffix: true,
                          })
                        : meeting.status === "ENDED"
                        ? "Ended " +
                          formatDistanceToNow(new Date(meeting.endedAt!), {
                            addSuffix: true,
                          })
                        : ""}
                    </span>
                  </div>
                )}

                <div className="flex items-center gap-2">
                  <code className="flex-1 px-2 py-1 bg-muted rounded text-sm font-mono">
                    {meeting.meetingCode}
                  </code>
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => handleCopyCode(meeting.meetingCode)}
                  >
                    {copiedCode === meeting.meetingCode ? (
                      <Check className="h-4 w-4 text-green-600" />
                    ) : (
                      <Copy className="h-4 w-4" />
                    )}
                  </Button>
                </div>
              </CardContent>

              <CardFooter>
                {meeting.status === "IN_PROGRESS" ? (
                  <Button
                    className="w-full"
                    onClick={() => handleJoinMeeting(meeting.id)}
                  >
                    <ExternalLink className="h-4 w-4 mr-2" />
                    Join Meeting
                  </Button>
                ) : meeting.status === "ENDED" ? (
                  <Button variant="outline" className="w-full" disabled>
                    Meeting Ended
                  </Button>
                ) : (
                  <Button
                    variant="secondary"
                    className="w-full"
                    onClick={() => handleJoinMeeting(meeting.id)}
                  >
                    View Details
                  </Button>
                )}
              </CardFooter>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
