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

            // Fetch user details immediately to ensure AuthContext initializes correctly
            // Use dynamic import or direct service call if possible, but simplest is to handle inside the effect
            import("@/services/user.service").then(({ userService }) => {
                userService.getCurrentProfile()
                    .then(response => {
                        if (response.success && response.data) {
                            localStorage.setItem("user", JSON.stringify(response.data));
                            toast.success("Successfully logged in via SSO");
                            window.location.href = "/projects";
                        } else {
                            throw new Error("Failed to load user profile");
                        }
                    })
                    .catch(err => {
                        console.error("Failed to fetch user profile after SSO", err);
                        toast.error("Login succeeded but failed to load user data. Please try again.");
                        navigate("/login");
                    });
            });
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
