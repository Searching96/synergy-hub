import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { CheckCircle2, XCircle, Loader2, Mail } from "lucide-react";
import authService from "@/services/auth.service";
import { toast } from "sonner";

const EmailVerificationPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState<"loading" | "success" | "error">("loading");
  const [message, setMessage] = useState("");
  const token = searchParams.get("token");

  useEffect(() => {
    const verifyEmail = async () => {
      if (!token) {
        setStatus("error");
        setMessage("Invalid verification link. No token provided.");
        return;
      }

      try {
        // Call the backend API to verify email
        const response = await authService.verifyEmail("", token);
        
        if (response.success) {
          setStatus("success");
          setMessage(response.message || "Email verified successfully!");
          toast.success("Email verified! You can now log in.");
          
          // Redirect to login after 3 seconds
          setTimeout(() => {
            navigate("/login", { replace: true });
          }, 3000);
        } else {
          setStatus("error");
          setMessage(response.message || "Verification failed. Please try again.");
        }
      } catch (error: any) {
        setStatus("error");
        const errorMsg = error.response?.data?.error || error.response?.data?.message || "Verification failed. The link may be expired or invalid.";
        setMessage(errorMsg);
        toast.error(errorMsg);
      }
    };

    verifyEmail();
  }, [token, navigate]);

  const handleResendEmail = async () => {
    // You could add email input here or redirect to a resend page
    toast.info("Please use the 'Resend Verification' option on the login page");
    navigate("/login");
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 via-white to-purple-50 p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <div className="mx-auto mb-4">
            {status === "loading" && (
              <Loader2 className="h-16 w-16 text-blue-500 animate-spin" />
            )}
            {status === "success" && (
              <CheckCircle2 className="h-16 w-16 text-green-500" />
            )}
            {status === "error" && (
              <XCircle className="h-16 w-16 text-red-500" />
            )}
          </div>
          <CardTitle className="text-2xl">
            {status === "loading" && "Verifying Email..."}
            {status === "success" && "Email Verified!"}
            {status === "error" && "Verification Failed"}
          </CardTitle>
          <CardDescription className="mt-2">
            {message}
          </CardDescription>
        </CardHeader>
        
        <CardContent className="space-y-4">
          {status === "success" && (
            <div className="text-center text-sm text-muted-foreground">
              Redirecting to login page...
            </div>
          )}
          
          {status === "error" && (
            <div className="space-y-2">
              <Button 
                onClick={() => navigate("/login")} 
                className="w-full"
                variant="default"
              >
                Go to Login
              </Button>
              <Button 
                onClick={handleResendEmail} 
                className="w-full"
                variant="outline"
              >
                <Mail className="mr-2 h-4 w-4" />
                Resend Verification Email
              </Button>
            </div>
          )}
          
          {status === "loading" && (
            <div className="text-center text-sm text-muted-foreground">
              Please wait while we verify your email address...
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default EmailVerificationPage;
