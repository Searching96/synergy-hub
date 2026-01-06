import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import ParticipantVideoTile from "./ParticipantVideoTile";
import MeetingControlsBar from "./MeetingControlsBar";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Button } from "@/components/ui/button";
import { X, Users, MessageSquare } from "lucide-react";
import { cn } from "@/lib/utils";
import type { Meeting, MeetingParticipant, MeetingReaction } from "@/types/meeting.types";

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
  const navigate = useNavigate();
  const [isAudioEnabled, setIsAudioEnabled] = useState(true);
  const [isVideoEnabled, setIsVideoEnabled] = useState(true);
  const [isScreenSharing, setIsScreenSharing] = useState(false);
  const [isHandRaised, setIsHandRaised] = useState(false);
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [isParticipantsOpen, setIsParticipantsOpen] = useState(false);
  const [reactions, setReactions] = useState<MeetingReaction[]>([]);

  const currentParticipant = meeting.participants.find(
    (p) => p.userId === currentUserId
  );

  const otherParticipants = meeting.participants.filter(
    (p) => p.userId !== currentUserId
  );

  // Find active screen sharer
  const screenSharer = meeting.participants.find((p) => p.isScreenSharing);

  const handleToggleAudio = () => {
    setIsAudioEnabled((prev) => !prev);
    // TODO: Actual media stream control
  };

  const handleToggleVideo = () => {
    setIsVideoEnabled((prev) => !prev);
    // TODO: Actual media stream control
  };

  const handleToggleScreenShare = () => {
    setIsScreenSharing((prev) => !prev);
    // TODO: Actual screen share logic
  };

  const handleToggleHandRaise = () => {
    setIsHandRaised((prev) => !prev);
    // TODO: Broadcast hand raise status
  };

  const handleReaction = (emoji: string) => {
    const newReaction: MeetingReaction = {
      emoji,
      userId: currentUserId,
      userName: currentParticipant?.userName || "Unknown",
      timestamp: new Date().toISOString(),
    };

    setReactions((prev) => [...prev, newReaction]);

    // Remove reaction after 3 seconds
    setTimeout(() => {
      setReactions((prev) => prev.filter((r) => r !== newReaction));
    }, 3000);
  };

  const handleLeaveMeeting = () => {
    if (confirm("Are you sure you want to leave this meeting?")) {
      onLeaveMeeting();
    }
  };

  // Grid layout calculation
  const getGridLayout = (count: number) => {
    if (count === 1) return "grid-cols-1";
    if (count === 2) return "grid-cols-2";
    if (count <= 4) return "grid-cols-2 grid-rows-2";
    if (count <= 6) return "grid-cols-3 grid-rows-2";
    if (count <= 9) return "grid-cols-3 grid-rows-3";
    return "grid-cols-4 grid-rows-3";
  };

  return (
    <div className="fixed inset-0 bg-gray-950 z-50">
      {/* Main Video Area */}
      <div className="h-full w-full flex">
        {/* Video Grid */}
        <div className="flex-1 relative">
          {screenSharer ? (
            // Screen Share View
            <div className="h-full flex flex-col p-4 pb-24">
              {/* Main Screen Share */}
              <div className="flex-1 rounded-lg overflow-hidden bg-black mb-4">
                <ParticipantVideoTile
                  participant={screenSharer}
                  className="w-full h-full"
                />
              </div>

              {/* Participant Thumbnails */}
              <div className="flex gap-2 overflow-x-auto pb-2">
                {currentParticipant && (
                  <div className="flex-shrink-0 w-48 h-32">
                    <ParticipantVideoTile
                      participant={currentParticipant}
                      isLocalUser
                    />
                  </div>
                )}
                {otherParticipants
                  .filter((p) => !p.isScreenSharing)
                  .map((participant) => (
                    <div key={participant.id} className="flex-shrink-0 w-48 h-32">
                      <ParticipantVideoTile participant={participant} />
                    </div>
                  ))}
              </div>
            </div>
          ) : (
            // Gallery View
            <div className="h-full p-4 pb-24">
              <div
                className={cn(
                  "grid gap-4 h-full",
                  getGridLayout(meeting.participants.length)
                )}
              >
                {currentParticipant && (
                  <ParticipantVideoTile
                    participant={currentParticipant}
                    isLocalUser
                  />
                )}
                {otherParticipants.map((participant) => (
                  <ParticipantVideoTile
                    key={participant.id}
                    participant={participant}
                  />
                ))}
              </div>
            </div>
          )}

          {/* Floating Reactions */}
          <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 pointer-events-none">
            {reactions.map((reaction, index) => (
              <div
                key={`${reaction.timestamp}-${index}`}
                className="text-6xl animate-bounce"
                style={{
                  animation: "floatUp 3s ease-out",
                  animationFillMode: "forwards",
                }}
              >
                {reaction.emoji}
              </div>
            ))}
          </div>
        </div>

        {/* Chat Sidebar */}
        {isChatOpen && (
          <div className="w-80 bg-gray-900 border-l border-gray-800 flex flex-col">
            <div className="p-4 border-b border-gray-800 flex items-center justify-between">
              <h3 className="text-white font-semibold flex items-center gap-2">
                <MessageSquare className="h-5 w-5" />
                Meeting Chat
              </h3>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setIsChatOpen(false)}
                className="text-gray-400 hover:text-white"
              >
                <X className="h-5 w-5" />
              </Button>
            </div>
            <ScrollArea className="flex-1 p-4">
              <div className="text-gray-400 text-center text-sm">
                Chat messages will appear here
              </div>
            </ScrollArea>
          </div>
        )}

        {/* Participants Sidebar */}
        {isParticipantsOpen && (
          <div className="w-80 bg-gray-900 border-l border-gray-800 flex flex-col">
            <div className="p-4 border-b border-gray-800 flex items-center justify-between">
              <h3 className="text-white font-semibold flex items-center gap-2">
                <Users className="h-5 w-5" />
                Participants ({meeting.participants.length})
              </h3>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setIsParticipantsOpen(false)}
                className="text-gray-400 hover:text-white"
              >
                <X className="h-5 w-5" />
              </Button>
            </div>
            <ScrollArea className="flex-1 p-4">
              <div className="space-y-2">
                {meeting.participants.map((participant) => (
                  <div
                    key={participant.id}
                    className="flex items-center gap-3 p-2 rounded hover:bg-gray-800 transition-colors"
                  >
                    <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center text-white text-sm font-medium">
                      {participant.userName
                        .split(" ")
                        .map((n) => n[0])
                        .join("")
                        .toUpperCase()
                        .slice(0, 2)}
                    </div>
                    <div className="flex-1">
                      <div className="text-white text-sm">
                        {participant.userName}
                        {participant.userId === currentUserId && " (You)"}
                        {participant.userId === meeting.hostUserId && " (Host)"}
                      </div>
                    </div>
                    <div className="flex items-center gap-1">
                      {!participant.isAudioEnabled && (
                        <div className="text-red-500 text-xs">🔇</div>
                      )}
                      {!participant.isVideoEnabled && (
                        <div className="text-red-500 text-xs">📹</div>
                      )}
                      {participant.isHandRaised && (
                        <div className="text-yellow-400 text-xs">✋</div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </ScrollArea>
          </div>
        )}
      </div>

      {/* Meeting Controls */}
      <MeetingControlsBar
        isAudioEnabled={isAudioEnabled}
        isVideoEnabled={isVideoEnabled}
        isScreenSharing={isScreenSharing}
        isHandRaised={isHandRaised}
        isChatOpen={isChatOpen}
        isParticipantsOpen={isParticipantsOpen}
        onToggleAudio={handleToggleAudio}
        onToggleVideo={handleToggleVideo}
        onToggleScreenShare={handleToggleScreenShare}
        onToggleHandRaise={handleToggleHandRaise}
        onToggleChat={() => setIsChatOpen((prev) => !prev)}
        onToggleParticipants={() => setIsParticipantsOpen((prev) => !prev)}
        onReaction={handleReaction}
        onLeaveMeeting={handleLeaveMeeting}
        meetingCode={meeting.meetingCode}
        participantCount={meeting.participants.length}
      />

      <style>{`
        @keyframes floatUp {
          0% {
            opacity: 1;
            transform: translateY(0) scale(1);
          }
          100% {
            opacity: 0;
            transform: translateY(-200px) scale(1.5);
          }
        }
      `}</style>
    </div>
  );
}
