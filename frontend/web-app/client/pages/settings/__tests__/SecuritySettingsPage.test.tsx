import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import SecuritySettingsPage from "../SecuritySettingsPage";

// Mock Hooks
const mockUseSSO = vi.fn();
vi.mock("@/hooks/useSSOConfigs", () => ({
    useSSOConfigs: () => mockUseSSO(),
}));

// Mock Child Components
vi.mock("@/components/sso/SsoProviderList", () => ({
    SsoProviderList: ({ providers, onDelete }: any) => (
        <div>
            {providers.map((p: any) => (
                <div key={p.id}>
                    {p.name}
                    <button onClick={() => onDelete(p.id)}>Delete</button>
                </div>
            ))}
        </div>
    ),
}));

vi.mock("@/components/sso/SsoProviderForm", () => ({
    SsoProviderForm: ({ open }: any) => open ? <div>SSO Form Modal</div> : null,
}));

describe("SecuritySettingsPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should render loading state", () => {
        mockUseSSO.mockReturnValue({
            providers: [],
            isLoadingProviders: true,
            hasAccess: true,
        });

        render(<SecuritySettingsPage />);
        expect(document.querySelector(".animate-spin")).toBeInTheDocument();
    });

    it("should render provider list", () => {
        mockUseSSO.mockReturnValue({
            providers: [{ id: 1, name: "Custom Provider" }, { id: 2, name: "Okta Test" }],
            isLoadingProviders: false,
            hasAccess: true,
        });

        render(<SecuritySettingsPage />);
        expect(screen.getByText("Custom Provider")).toBeInTheDocument();
        expect(screen.getByText("Okta Test")).toBeInTheDocument();
    });

    it("should open registration modal on click", async () => {
        mockUseSSO.mockReturnValue({
            providers: [],
            isLoadingProviders: false,
            hasAccess: true,
            register: vi.fn(),
        });

        render(<SecuritySettingsPage />);

        const registerBtn = screen.getByText("Register Provider");
        fireEvent.click(registerBtn);

        expect(screen.getByText("SSO Form Modal")).toBeInTheDocument();
    });

    it("should show access denied alert if unauthorized", () => {
        mockUseSSO.mockReturnValue({
            providers: [],
            isLoadingProviders: false,
            hasAccess: false,
            errorProviders: { isAxiosError: true, response: { status: 403 } }, // Axio error shape
        });

        render(<SecuritySettingsPage />);
        expect(screen.getByText("Access Denied")).toBeInTheDocument();
        expect(screen.getByText(/You don't have permission/i)).toBeInTheDocument();
    });
});
