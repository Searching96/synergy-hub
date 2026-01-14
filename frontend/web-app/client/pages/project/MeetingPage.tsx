import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import MeetingRoom from "@/components/meeting/MeetingRoom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { AlertCircle, Video, Loader2 } from "lucide-react";
import { meetingService } from "@/services/meeting.service";
import authService from "@/services/auth.service";
import type { Meeting } from "@/types/meeting.types";

export default function MeetingPage() {
  const { projectId, meetingId } = useParams<{ projectId: string; meetingId: string }>();
  const navigate = useNavigate();

  const [meeting, setMeeting] = useState<Meeting | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isJoining, setIsJoining] = useState(false);
  const [hasJoined, setHasJoined] = useState(false);

  // Pre-join settings
  const [audioEnabled, setAudioEnabled] = useState(true);
  const [videoEnabled, setVideoEnabled] = useState(true);

  const currentUser = authService.getCurrentUser();
  const currentUserId = currentUser?.id || 0;
  const currentUserName = currentUser?.name || "Guest";

  useEffect(() => {
    loadMeeting();
  }, [meetingId]);

  const loadMeeting = async () => {
    if (!meetingId) {
      setError("Meeting ID is required");
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      const meetingData = await meetingService.getMeeting(meetingId);

      if (!meetingData) {
        setError("Meeting not found");
      } else if (meetingData.status === "ENDED") {
        setError("This meeting has ended");
      } else if (meetingData.status === "CANCELLED") {
        setError("This meeting has been cancelled");
      } else {
        setMeeting(meetingData);
        setError(null);
      }
    } catch (err) {
      setError("Failed to load meeting");
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleJoinMeeting = async () => {
    if (!meeting) return;

    try {
      setIsJoining(true);
      const updatedMeeting = await meetingService.joinMeeting(meeting.id);
      setMeeting(updatedMeeting);
      setHasJoined(true);
    } catch (err) {
      setError("Failed to join meeting");
      console.error(err);
    } finally {
      setIsJoining(false);
    }
  };

  const handleLeaveMeeting = async () => {
    if (!meeting) return;

    try {
      // await meetingService.leaveMeeting(meeting.id); // Not implemented yet
      navigate(`/projects/${projectId}/meetings`);
    } catch (err) {
      console.error("Failed to leave meeting:", err);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-center">
          <Loader2 className="h-12 w-12 text-primary mx-auto mb-4 animate-spin" />
          <p className="text-muted-foreground">Loading meeting...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-center">
          <AlertCircle className="h-12 w-12 text-destructive mx-auto mb-4" />
          <p className="text-destructive text-lg font-semibold mb-2">{error}</p>
          <Button onClick={() => navigate(`/projects/${projectId}`)}>
            Return to Project
          </Button>
        </div>
      </div>
    );
  }

  if (!meeting) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-center">
          <AlertCircle className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
          <p className="text-muted-foreground">Meeting not found</p>
        </div>
      </div>
    );
  }

  // Show meeting room if user has joined
  if (hasJoined) {
    return (
      <MeetingRoom
        meeting={meeting}
        currentUserId={currentUserId}
        onLeaveMeeting={handleLeaveMeeting}
      />
    );
  }

  // Show pre-join screen
  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 to-gray-950 flex items-center justify-center p-4">
      <Card className="w-full max-w-2xl">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Video className="h-6 w-6" />
            {meeting.title}
          </CardTitle>
          <CardDescription>
            {meeting.description || `Meeting in ${meeting.projectName}`}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Video Preview */}
          <div className="aspect-video bg-gray-900 rounded-lg overflow-hidden relative">
            <div className="absolute inset-0 flex items-center justify-center">
              {videoEnabled ? (
                <div className="text-white text-center">
                  <Video className="h-16 w-16 mx-auto mb-2 opacity-50" />
                  <p className="text-sm text-gray-400">Camera preview</p>
                </div>
              ) : (
                <div className="text-white text-center">
                  <div className="w-24 h-24 rounded-full bg-blue-600 flex items-center justify-center text-3xl font-bold mx-auto mb-2">
                    {currentUserName
                      .split(" ")
                      .map((n) => n[0])
                      .join("")
                      .toUpperCase()}
                  </div>
                  <p className="text-sm text-gray-400">Camera is off</p>
                </div>
              )}
            </div>
          </div>

          {/* Meeting Info */}
          <div className="flex items-center justify-between text-sm">
            <div>
              <p className="text-muted-foreground">Meeting Code</p>
              <p className="font-mono font-semibold">{meeting.meetingCode}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Participants</p>
              <p className="font-semibold">{meeting.participants.length} joined</p>
            </div>
          </div>

          {/* Pre-join Controls */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <Label htmlFor="audio" className="flex items-center gap-2">
                <span>Microphone</span>
              </Label>
              <Switch
                id="audio"
                checked={audioEnabled}
                onCheckedChange={setAudioEnabled}
              />
            </div>

            <div className="flex items-center justify-between">
              <Label htmlFor="video" className="flex items-center gap-2">
                <span>Camera</span>
              </Label>
              <Switch
                id="video"
                checked={videoEnabled}
                onCheckedChange={setVideoEnabled}
              />
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex gap-3">
            <Button
              variant="outline"
              onClick={() => navigate(`/projects/${projectId}/meetings`)}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              onClick={handleJoinMeeting}
              disabled={isJoining}
              className="flex-1"
            >
              {isJoining ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Joining...
                </>
              ) : (
                "Join Meeting"
              )}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
