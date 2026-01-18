import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Loader2, AlertCircle } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";

const DebugLoginPage = () => {
    const { email } = useParams<{ email: string }>();
    const navigate = useNavigate();
    const { login } = useAuth();
    const [error, setError] = useState<string | null>(null);
    const [status, setStatus] = useState<string>("Initializing debug login...");

    useEffect(() => {
        const performDebugLogin = async () => {
            if (!email) {
                setError("Email is required for debug login.");
                return;
            }

            try {
                setStatus(`Attempting login for ${email}...`);
                const defaultPassword = "Password123#";
                const response = await login(email, defaultPassword);

                if (response.success) {
                    setStatus("Login successful! Redirecting...");
                    // Give a small delay so user can see it's working
                    setTimeout(() => {
                        navigate("/projects");
                    }, 1000);
                } else {
                    setError(response.message || "Debug login failed.");
                }
            } catch (err: any) {
                const errorMessage =
                    err.response?.data?.error ||
                    err.response?.data?.message ||
                    "An error occurred during debug login";
                setError(errorMessage);
            }
        };

        performDebugLogin();
    }, [email, login, navigate]);

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
            <Card className="w-full max-w-md">
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        Debug Login
                        {error ? (
                            <AlertCircle className="h-5 w-5 text-destructive" />
                        ) : (
                            <Loader2 className="h-5 w-5 animate-spin text-blue-600" />
                        )}
                    </CardTitle>
                    <CardDescription>
                        {email ? `Logging in as ${email}` : "No email provided"}
                    </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                    {!error ? (
                        <div className="flex flex-col items-center gap-3 py-4">
                            <p className="text-sm font-medium text-muted-foreground">{status}</p>
                        </div>
                    ) : (
                        <Alert variant="destructive">
                            <AlertDescription>{error}</AlertDescription>
                        </Alert>
                    )}
                </CardContent>
            </Card>
        </div>
    );
};

export default DebugLoginPage;
