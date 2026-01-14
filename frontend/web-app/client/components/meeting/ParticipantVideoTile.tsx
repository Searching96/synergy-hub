import { cn } from "@/lib/utils";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Mic, MicOff, VideoOff, Video, Hand } from "lucide-react";
import type { MeetingParticipant } from "@/types/meeting.types";

interface ParticipantVideoTileProps {
  participant: MeetingParticipant;
  isLocalUser?: boolean;
  className?: string;
}

export default function ParticipantVideoTile({
  participant,
  isLocalUser = false,
  className,
}: ParticipantVideoTileProps) {
  const initials = (participant.userName || "?")
    .split(" ")
    .map((n) => n[0])
    .filter(Boolean)
    .join("")
    .toUpperCase()
    .slice(0, 2) || "??";

  return (
    <div
      className={cn(
        "relative rounded-lg overflow-hidden bg-gray-900",
        "border-2",
        participant.isSpeaking ? "border-green-500" : "border-transparent",
        className
      )}
    >
      {/* Video Stream or Avatar */}
      {participant.isVideoEnabled ? (
        <div className="w-full h-full bg-gray-800 flex items-center justify-center">
          <video
            ref={(el) => {
              if (el && participant.videoTrack) {
                participant.videoTrack.attach(el);
              }
            }}
            className="w-full h-full object-cover"
            autoPlay
            playsInline
            muted={isLocalUser}
          />
        </div>
      ) : (
        <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-gray-800 to-gray-900">
          <Avatar className="h-24 w-24">
            <AvatarImage src={participant.avatarUrl} />
            <AvatarFallback className="text-2xl bg-blue-600">
              {initials}
            </AvatarFallback>
          </Avatar>
        </div>
      )}

      {/* Participant Info Overlay */}
      <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/80 to-transparent p-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-white text-sm font-medium truncate">
              {participant.userName} {isLocalUser && "(You)"}
            </span>
            {participant.isHandRaised && (
              <Hand className="h-4 w-4 text-yellow-400 fill-yellow-400" />
            )}
          </div>

          <div className="flex items-center gap-1">
            {participant.isAudioEnabled ? (
              <Mic className="h-4 w-4 text-white" />
            ) : (
              <MicOff className="h-4 w-4 text-red-500" />
            )}
          </div>
        </div>
      </div>

      {/* Screen Sharing Indicator */}
      {participant.isScreenSharing && (
        <div className="absolute top-2 right-2 bg-blue-600 text-white text-xs px-2 py-1 rounded">
          Presenting
        </div>
      )}
    </div>
  );
}
