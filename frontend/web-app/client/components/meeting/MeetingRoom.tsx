import { useState, useEffect } from "react";
import { LiveKitRoom, VideoConference } from "@livekit/components-react";
import "@livekit/components-styles";
import { Loader2, MessageSquare, X } from "lucide-react";
import type { Meeting } from "@/types/meeting.types";
import { meetingService } from "@/services/meeting.service";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { chatService } from "@/services/chat.service";
import { ProjectChatRoom } from "@/components/chat/ProjectChatRoom";
import { cn } from "@/lib/utils";

interface MeetingRoomProps {
  meeting: Meeting;
  currentUserId: number;
  onLeaveMeeting: () => void;
}

export default function MeetingRoom({
  meeting,
  currentUserId,
  onLeaveMeeting,
}: MeetingRoomProps) {
  const [token, setToken] = useState<string>("");
  const [showChat, setShowChat] = useState(false);
  const queryClient = useQueryClient();

  // Fetch project chat messages
  const { data: messages = [] } = useQuery({
    queryKey: ['chat', meeting.projectId],
    queryFn: () => chatService.getProjectMessages(meeting.projectId),
    refetchInterval: 5000, // Simple polling for now
    enabled: !!meeting.projectId
  });

  // Send message mutation
  const sendMessageMutation = useMutation({
    mutationFn: (content: string) => chatService.sendMessage({
      projectId: meeting.projectId,
      message: content
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['chat', meeting.projectId] });
    },
    onError: () => {
      toast.error("Failed to send message");
    }
  });

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
    <div className="fixed inset-0 bg-gray-950 z-50 flex">
      <div className="flex-1 relative">
        <LiveKitRoom
          video={true}
          audio={true}
          token={token}
          serverUrl={liveKitUrl}
          data-lk-theme="default"
          style={{ height: "100vh" }}
          onDisconnected={onLeaveMeeting}
        >
          <VideoConference />
        </LiveKitRoom>

        {/* Chat Toggle Button (Floating) */}
        {!showChat && (
          <Button
            onClick={() => setShowChat(true)}
            className="absolute bottom-6 right-6 z-[60] h-12 w-12 rounded-full shadow-lg bg-primary hover:bg-primary/90"
          >
            <MessageSquare className="h-6 w-6" />
          </Button>
        )}
      </div>

      {/* Chat Sidebar Overlay */}
      <div
        className={cn(
          "fixed inset-y-0 right-0 z-[60] w-80 bg-background border-l shadow-2xl transition-transform duration-300 ease-in-out transform",
          showChat ? "translate-x-0" : "translate-x-full"
        )}
      >
        <div className="flex flex-col h-full">
          <div className="flex items-center justify-between p-4 border-b">
            <h3 className="font-semibold">Project Chat</h3>
            <Button variant="ghost" size="icon" onClick={() => setShowChat(false)}>
              <X className="h-4 w-4" />
            </Button>
          </div>
          <div className="flex-1 overflow-hidden">
            <ProjectChatRoom
              projectId={meeting.projectId}
              projectName={meeting.projectName}
              currentUserId={currentUserId}
              messages={messages}
              onSendMessage={(msg) => sendMessageMutation.mutate(msg)}
              className="h-full border-0 rounded-none"
            />
          </div>
        </div>
      </div>
    </div>
  );
}
