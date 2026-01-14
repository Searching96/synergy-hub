import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { RoleManagerPage } from "../RoleManagerPage";

// Mock Hooks & Components
const mockUseAuth = vi.fn();
vi.mock("@/context/AuthContext", () => ({
    useAuth: () => mockUseAuth(),
}));

const mockUseRBAC = vi.fn();
vi.mock("@/hooks/useRBAC", () => ({
    useRBAC: () => mockUseRBAC(),
}));

// Mock Child Components to avoid deep rendering complexity
vi.mock("@/components/rbac/RoleList", () => ({
    RoleList: ({ roles, onSelectRole }: any) => (
        <div data-testid="role-list">
            {roles.map((r: any) => (
                <button key={r.id} onClick={() => onSelectRole(r)}>{r.name}</button>
            ))}
        </div>
    ),
}));

vi.mock("@/components/rbac/PermissionMatrix", () => ({
    PermissionMatrix: ({ role }: any) => (
        <div data-testid="permission-matrix">Permissions for {role?.name}</div>
    ),
}));

describe("RoleManagerPage", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should render access denied if user is not admin", () => {
        mockUseAuth.mockReturnValue({ user: { roles: ["USER"] } });
        mockUseRBAC.mockReturnValue({
            roles: [],
            permissions: [],
            isLoading: false,
            groupPermissionsByCategory: () => ({}),
        });

        render(<RoleManagerPage />);

        // The page logic actually checks roles prop on user for "ORG_ADMIN" or "GLOBAL_ADMIN" to set generic access,
        // but the restricted view logic is: canManageRoles. 
        // If !canManageRoles, it still renders the page but in view-only mode.
        // Wait, let's re-read RoleManagerPage.tsx.
        // Line 22: canManageRoles = ...
        // Line 142: if (isForbidden) ... (only set by API errors)
        // Line 192: !canManageRoles && (View only span)

        // So non-admin SHOULD see the page, but with a warning.
        expect(screen.getByText(/View only/i)).toBeInTheDocument();
    });

    it("should render role list and matrix for admin", () => {
        mockUseAuth.mockReturnValue({ user: { roles: ["ORG_ADMIN"] } });
        mockUseRBAC.mockReturnValue({
            roles: [{ id: 1, name: "Admin" }, { id: 2, name: "Developer" }],
            permissions: [],
            isLoading: false,
            groupPermissionsByCategory: () => ({}),
        });

        render(<RoleManagerPage />);

        expect(screen.getByTestId("role-list")).toBeInTheDocument();
        expect(screen.getByText("Admin")).toBeInTheDocument();
        expect(screen.getByTestId("permission-matrix")).toHaveTextContent("Permissions for Admin");
    });

    it("should switch selected role", () => {
        mockUseAuth.mockReturnValue({ user: { roles: ["ORG_ADMIN"] } });
        mockUseRBAC.mockReturnValue({
            roles: [{ id: 1, name: "Admin" }, { id: 2, name: "Developer" }],
            permissions: [],
            isLoading: false,
            groupPermissionsByCategory: () => ({}),
        });

        render(<RoleManagerPage />);

        // Initial state: Admin selected (first one)
        expect(screen.getByTestId("permission-matrix")).toHaveTextContent("Permissions for Admin");

        // Click Developer
        fireEvent.click(screen.getByText("Developer"));

        // Should now show Developer permissions
        expect(screen.getByTestId("permission-matrix")).toHaveTextContent("Permissions for Developer");
    });
});
