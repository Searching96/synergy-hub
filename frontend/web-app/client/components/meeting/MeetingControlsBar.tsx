import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Mic,
  MicOff,
  Video,
  VideoOff,
  MonitorUp,
  MonitorOff,
  Hand,
  MoreVertical,
  MessageSquare,
  Users,
  Settings,
  PhoneOff,
  Smile,
  PenTool,
} from "lucide-react";
import { cn } from "@/lib/utils";

interface MeetingControlsBarProps {
  isAudioEnabled: boolean;
  isVideoEnabled: boolean;
  isScreenSharing: boolean;
  isHandRaised: boolean;
  isChatOpen: boolean;
  isParticipantsOpen: boolean;
  onToggleAudio: () => void;
  onToggleVideo: () => void;
  onToggleScreenShare: () => void;
  onToggleHandRaise: () => void;
  onToggleChat: () => void;
  onToggleParticipants: () => void;
  onReaction: (emoji: string) => void;
  onLeaveMeeting: () => void;
  meetingCode: string;
  participantCount: number;
}

export default function MeetingControlsBar({
  isAudioEnabled,
  isVideoEnabled,
  isScreenSharing,
  isHandRaised,
  isChatOpen,
  isParticipantsOpen,
  onToggleAudio,
  onToggleVideo,
  onToggleScreenShare,
  onToggleHandRaise,
  onToggleChat,
  onToggleParticipants,
  onReaction,
  onLeaveMeeting,
  meetingCode,
  participantCount,
}: MeetingControlsBarProps) {
  const controlButtonClass =
    "h-12 w-12 rounded-full bg-gray-800 hover:bg-gray-700 text-white";

  return (
    <div className="absolute bottom-0 left-0 right-0 bg-gray-900/95 backdrop-blur-sm border-t border-gray-800 px-4 py-3">
      <div className="flex items-center justify-between max-w-7xl mx-auto">
        {/* Left Side - Meeting Info */}
        <div className="flex items-center gap-4 min-w-[200px]">
          <div className="text-white">
            <div className="text-xs text-gray-400">Meeting Code</div>
            <div className="text-sm font-mono font-medium">{meetingCode}</div>
          </div>
        </div>

        {/* Center - Main Controls */}
        <div className="flex items-center gap-2">
          {/* More Options */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className={controlButtonClass}>
                <MoreVertical className="h-5 w-5" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-48">
              <DropdownMenuItem>
                <Settings className="h-4 w-4 mr-2" />
                Settings
              </DropdownMenuItem>
              <DropdownMenuItem>
                <PenTool className="h-4 w-4 mr-2" />
                Whiteboard
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>

          {/* Microphone Toggle */}
          <Button
            variant="ghost"
            size="icon"
            onClick={onToggleAudio}
            className={cn(
              controlButtonClass,
              !isAudioEnabled && "bg-red-600 hover:bg-red-700"
            )}
          >
            {isAudioEnabled ? (
              <Mic className="h-5 w-5" />
            ) : (
              <MicOff className="h-5 w-5" />
            )}
          </Button>

          {/* Camera Toggle */}
          <Button
            variant="ghost"
            size="icon"
            onClick={onToggleVideo}
            className={cn(
              controlButtonClass,
              !isVideoEnabled && "bg-red-600 hover:bg-red-700"
            )}
          >
            {isVideoEnabled ? (
              <Video className="h-5 w-5" />
            ) : (
              <VideoOff className="h-5 w-5" />
            )}
          </Button>

          {/* Screen Share Toggle */}
          <Button
            variant="ghost"
            size="icon"
            onClick={onToggleScreenShare}
            className={cn(
              controlButtonClass,
              isScreenSharing && "bg-blue-600 hover:bg-blue-700"
            )}
          >
            {isScreenSharing ? (
              <MonitorOff className="h-5 w-5" />
            ) : (
              <MonitorUp className="h-5 w-5" />
            )}
          </Button>

          {/* Reactions */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className={controlButtonClass}>
                <Smile className="h-5 w-5" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="center" className="w-72">
              <div className="grid grid-cols-6 gap-2 p-2">
                {["👍", "❤️", "😂", "😮", "👏", "🎉", "🤔", "👎", "🔥", "✨", "💯", "🚀"].map(
                  (emoji) => (
                    <button
                      key={emoji}
                      onClick={() => onReaction(emoji)}
                      className="text-2xl hover:bg-gray-100 dark:hover:bg-gray-800 rounded p-2 transition-colors"
                    >
                      {emoji}
                    </button>
                  )
                )}
              </div>
            </DropdownMenuContent>
          </DropdownMenu>

          {/* Chat Toggle */}
          <Button
            variant="ghost"
            size="icon"
            onClick={onToggleChat}
            className={cn(
              controlButtonClass,
              isChatOpen && "bg-gray-700"
            )}
          >
            <MessageSquare className="h-5 w-5" />
          </Button>

          {/* Raise Hand */}
          <Button
            variant="ghost"
            size="icon"
            onClick={onToggleHandRaise}
            className={cn(
              controlButtonClass,
              isHandRaised && "bg-yellow-600 hover:bg-yellow-700"
            )}
          >
            <Hand className="h-5 w-5" />
          </Button>

          {/* More Options Menu */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className={controlButtonClass}>
                <MoreVertical className="h-5 w-5" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-48">
              <DropdownMenuItem>
                <PenTool className="h-4 w-4 mr-2" />
                Whiteboard
              </DropdownMenuItem>
              <DropdownMenuItem>
                <Settings className="h-4 w-4 mr-2" />
                Settings
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>

          {/* Leave Meeting */}
          <Button
            variant="ghost"
            size="icon"
            onClick={onLeaveMeeting}
            className="h-12 w-12 rounded-full bg-red-600 hover:bg-red-700 text-white"
          >
            <PhoneOff className="h-5 w-5" />
          </Button>
        </div>

        {/* Right Side - Participants */}
        <div className="flex items-center gap-2 min-w-[200px] justify-end">
          <Button
            variant="ghost"
            size="sm"
            onClick={onToggleParticipants}
            className={cn(
              "gap-2 text-white hover:bg-gray-800",
              isParticipantsOpen && "bg-gray-800"
            )}
          >
            <Users className="h-5 w-5" />
            <span>{participantCount}</span>
          </Button>
        </div>
      </div>
    </div>
  );
}
