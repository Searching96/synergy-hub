import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "sonner";

const OAuth2RedirectHandler = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    useEffect(() => {
        const token = searchParams.get("token");
        const refreshToken = searchParams.get("refreshToken");
        const error = searchParams.get("error");

        if (error) {
            toast.error(error);
            navigate("/login");
            return;
        }

        if (token) {
            localStorage.setItem("token", token);
            if (refreshToken) {
                localStorage.setItem("refreshToken", refreshToken);
            }

            // We don't have the user object here, but the AuthContext or next API call will fetch it.
            // Force a reload or navigate to ensure AuthContext picks up the new token
            // window.location.href = "/projects"; 
            // Better to use navigate if possible, but AuthContext might need a trigger.
            // Let's assume navigating to /projects triggers the protected route check which verifies token.

            // Optionally fetch user details immediately if needed, but for now let's rely on dashboard loader.
            toast.success("Successfully logged in via SSO");
            window.location.href = "/projects"; // Hard reload to ensure apps state is clean
        } else {
            navigate("/login");
        }
    }, [searchParams, navigate]);

    return (
        <div className="flex items-center justify-center min-h-screen bg-gray-50">
            <div className="flex flex-col items-center gap-4">
                <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 border-t-transparent"></div>
                <p className="text-gray-500 font-medium">Completing secure sign in...</p>
            </div>
        </div>
    );
};

export default OAuth2RedirectHandler;
