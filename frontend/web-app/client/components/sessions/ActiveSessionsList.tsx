/**
 * ActiveSessionsList Component
 * Displays and manages active user sessions
 * Route: /profile/security
 */

import { useSessions } from "@/hooks/useSessions";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Monitor, Smartphone, Shield, LogOut, AlertCircle } from "lucide-react";
import { useState } from "react";
import { cn } from "@/lib/utils";
import type { Session } from "@/types/session.types";
import axios from "axios";

/**
 * Parse user agent to determine device type
 * Mock logic for now - can be enhanced with a proper UA parser library
 */
const getDeviceIcon = (userAgent: string) => {
  const ua = userAgent.toLowerCase();
  
  if (ua.includes("mobile") || ua.includes("android") || ua.includes("iphone")) {
    return Smartphone;
  }
  
  return Monitor;
};

/**
 * Format date to relative time
 */
const formatRelativeTime = (dateString: string) => {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMins < 1) return "Just now";
  if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? "s" : ""} ago`;
  if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? "s" : ""} ago`;
  return `${diffDays} day${diffDays > 1 ? "s" : ""} ago`;
};

/**
 * Extract browser name from user agent
 */
const getBrowserName = (userAgent: string) => {
  if (userAgent.includes("Chrome")) return "Chrome";
  if (userAgent.includes("Firefox")) return "Firefox";
  if (userAgent.includes("Safari")) return "Safari";
  if (userAgent.includes("Edge")) return "Edge";
  return "Unknown Browser";
};

/**
 * Session Item Component
 */
interface SessionItemProps {
  session: Session;
  onRevoke: (sessionId: string) => void;
  isRevoking: boolean;
}

const SessionItem = ({ session, onRevoke, isRevoking }: SessionItemProps) => {
  const DeviceIcon = getDeviceIcon(session.userAgent);
  const browserName = getBrowserName(session.userAgent);

  return (
    <div
      className={cn(
        "flex items-center justify-between p-4 border rounded-lg",
        session.isCurrent && "bg-blue-50 border-blue-200"
      )}
    >
      <div className="flex items-start gap-4 flex-1">
        <div className="p-2 rounded-lg bg-gray-100">
          <DeviceIcon className="h-5 w-5 text-gray-600" />
        </div>
        
        <div className="flex-1 space-y-1">
          <div className="flex items-center gap-2">
            <span className="font-medium text-gray-900">{browserName}</span>
            {session.isCurrent && (
              <Badge variant="default" className="bg-blue-600">
                Current Session
              </Badge>
            )}
          </div>
          
          <div className="text-sm text-gray-600 space-y-0.5">
            <div>IP Address: {session.ipAddress}</div>
            <div>Last accessed: {formatRelativeTime(session.lastAccessedAt)}</div>
            <div className="text-xs text-gray-500">
              Signed in: {new Date(session.createdAt).toLocaleString()}
            </div>
          </div>
        </div>
      </div>

      {!session.isCurrent && (
        <Button
          variant="destructive"
          size="sm"
          onClick={() => onRevoke(session.id)}
          disabled={isRevoking}
        >
          <LogOut className="h-4 w-4 mr-2" />
          Revoke
        </Button>
      )}
    </div>
  );
};

/**
 * Main ActiveSessionsList Component
 */
export default function ActiveSessionsList() {
  const {
    sessions,
    isLoading,
    isError,
    error,
    hasAccess,
    revokeSession,
    isRevoking,
    revokeAllOther,
    isRevokingAll,
  } = useSessions();

  const [revokeAllDialog, setRevokeAllDialog] = useState(false);

  // Handle API errors gracefully
  const isUnauthorized =
    axios.isAxiosError(error) && error.response?.status === 403;

  const otherSessionsCount = sessions.filter((s) => !s.isCurrent).length;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Shield className="h-6 w-6 text-blue-600" />
            <h1 className="text-3xl font-bold text-gray-900">Active Sessions</h1>
          </div>
          <p className="text-gray-600">
            Manage and monitor your active login sessions across devices
          </p>
        </div>
        
        {otherSessionsCount > 0 && hasAccess && (
          <Button
            variant="destructive"
            onClick={() => setRevokeAllDialog(true)}
            disabled={isRevokingAll || isRevoking}
          >
            <LogOut className="h-4 w-4 mr-2" />
            Sign out of all other devices
          </Button>
        )}
      </div>

      {/* 403 Forbidden Alert */}
      {isUnauthorized && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            Access Denied: You do not have permission to view active sessions.
          </AlertDescription>
        </Alert>
      )}

      {/* Sessions List Card */}
      <Card>
        <CardHeader>
          <CardTitle>Your Active Sessions</CardTitle>
          <CardDescription>
            You are currently signed in to these devices. If you notice any suspicious
            activity, revoke the session immediately.
          </CardDescription>
        </CardHeader>
        
        <CardContent className="space-y-4">
          {isLoading && (
            <>
              <Skeleton className="h-24 w-full" />
              <Skeleton className="h-24 w-full" />
              <Skeleton className="h-24 w-full" />
            </>
          )}

          {isError && !isUnauthorized && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                Failed to load sessions. Please try again later.
              </AlertDescription>
            </Alert>
          )}

          {!isLoading && !isError && sessions.length === 0 && (
            <div className="text-center py-8 text-gray-500">
              No active sessions found
            </div>
          )}

          {!isLoading && !isError && sessions.length > 0 && (
            <div className="space-y-3">
              {sessions.map((session) => (
                <SessionItem
                  key={session.id}
                  session={session}
                  onRevoke={revokeSession}
                  isRevoking={isRevoking}
                />
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Revoke All Confirmation Dialog */}
      <AlertDialog open={revokeAllDialog} onOpenChange={setRevokeAllDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Sign out of all other devices?</AlertDialogTitle>
            <AlertDialogDescription>
              This will immediately end all other active sessions ({otherSessionsCount}{" "}
              {otherSessionsCount === 1 ? "device" : "devices"}). You will remain signed
              in on this device. Other devices will need to sign in again.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                revokeAllOther();
                setRevokeAllDialog(false);
              }}
              className="bg-red-600 hover:bg-red-700"
            >
              Sign out all other devices
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
