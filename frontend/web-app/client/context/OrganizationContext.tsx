import React, { createContext, useContext, useEffect, useState, ReactNode, useCallback } from "react";

export interface OrgContextValue {
  organizationId: number | null;
  loading: boolean;
  error: Error | null;
  refresh: () => Promise<void>;
  setOrganizationId: (id: number) => void;
}

const OrganizationContext = createContext<OrgContextValue | undefined>(undefined);

const getOrgIdFromStorage = (): number | null => {
  const orgId = localStorage.getItem("organizationId");
  if (orgId) return parseInt(orgId, 10);

  try {
    const userStr = localStorage.getItem("user");
    if (userStr) {
      const user = JSON.parse(userStr);
      if (user.organizationId) return user.organizationId;
    }
  } catch (e) {
    console.debug("OrganizationContext: failed parsing user", e);
  }

  return null;
};

export const OrganizationProvider = ({ children }: { children: ReactNode }) => {
  const [organizationId, setOrganizationIdState] = useState<number | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<Error | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const id = getOrgIdFromStorage();
      if (id !== null) {
        setOrganizationIdState(id);
      } else {
        setOrganizationIdState(null);
        setError(new Error("Organization context is missing"));
      }
    } catch (e: any) {
      setOrganizationIdState(null);
      setError(e instanceof Error ? e : new Error(String(e)));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const refresh = async () => {
    await load();
  };

  const setOrganizationId = (id: number) => {
    try {
      localStorage.setItem("organizationId", String(id));
      setOrganizationIdState(id);
      setError(null);
    } catch (e) {
      console.error("Failed to set organizationId", e);
      setError(e instanceof Error ? e : new Error(String(e)));
    }
  };

  return (
    <OrganizationContext.Provider value={{ organizationId, loading, error, refresh, setOrganizationId }}>
      {children}
    </OrganizationContext.Provider>
  );
};

export const useOrganization = (): OrgContextValue => {
  const ctx = useContext(OrganizationContext);
  if (!ctx) throw new Error("useOrganization must be used within OrganizationProvider");
  return ctx;
};
