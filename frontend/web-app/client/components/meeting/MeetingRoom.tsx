import { useState, useEffect } from "react";
import { LiveKitRoom, VideoConference } from "@livekit/components-react";
import "@livekit/components-styles";
import { Loader2 } from "lucide-react";
import type { Meeting } from "@/types/meeting.types";
import { meetingService } from "@/services/meeting.service";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";

interface MeetingRoomProps {
  meeting: Meeting;
  currentUserId: number;
  onLeaveMeeting: () => void;
}

export default function MeetingRoom({
  meeting,
  onLeaveMeeting,
}: MeetingRoomProps) {
  const [token, setToken] = useState<string>("");

  useEffect(() => {
    const fetchToken = async () => {
      try {
        const accessToken = await meetingService.getJoinToken(meeting.id);
        setToken(accessToken);
      } catch (error) {
        console.error("Failed to join meeting:", error);
        toast.error("Failed to join meeting room");
      }
    };

    fetchToken();
  }, [meeting.id]);

  if (!token) {
    return (
      <div className="fixed inset-0 bg-gray-950 flex flex-col items-center justify-center text-white">
        <Loader2 className="h-10 w-10 animate-spin mb-4 text-blue-500" />
        <p>Connecting to secure room...</p>
        <Button
          variant="outline"
          className="mt-8 text-black"
          onClick={onLeaveMeeting}
        >
          Cancel
        </Button>
      </div>
    );
  }

  const liveKitUrl = import.meta.env.VITE_LIVEKIT_URL || "ws://localhost:7880";

  return (
    <div className="fixed inset-0 bg-gray-950 z-50">
      <LiveKitRoom
        video={true}
        audio={true}
        token={token}
        serverUrl={liveKitUrl}
        data-lk-theme="default"
        style={{ height: "100dvh" }}
        onDisconnected={onLeaveMeeting}
      >
        <VideoConference />
      </LiveKitRoom>
    </div>
  );
}
