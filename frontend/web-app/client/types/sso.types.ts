/**
 * SSO Type Definitions
 * Matches backend DTOs: SsoProviderResponse, RegisterSsoProviderRequest, UpdateSsoProviderRequest
 */

export enum SsoProviderType {
  SAML = "SAML",
  OIDC = "OIDC",
  OAUTH2 = "OAUTH2",
}

export interface SsoProvider {
  id: number;
  providerType: SsoProviderType;
  providerName: string;
  clientId: string;
  clientSecret?: string; // Only in requests, never in responses
  metadataUrl?: string;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface SsoProviderResponse {
  id: number;
  providerType: SsoProviderType;
  providerName: string;
  clientId: string;
  metadataUrl?: string;
  enabled: boolean;
  createdAt: string;
}

export interface RegisterSsoProviderRequest {
  providerType: SsoProviderType;
  providerName: string;
  clientId: string;
  clientSecret: string;
  metadataUrl?: string;
  // SAML-specific fields
  ssoUrl?: string;
  entityId?: string;
  certificate?: string;
}

export interface UpdateSsoProviderRequest {
  providerName?: string;
  clientId?: string;
  clientSecret?: string;
  metadataUrl?: string;
  ssoUrl?: string;
  entityId?: string;
  certificate?: string;
}

export interface SamlConfig {
  ssoUrl: string;
  entityId: string;
  certificate: string;
}

export interface OidcConfig {
  clientId: string;
  clientSecret: string;
  metadataUrl?: string;
}

export interface ApiResponse<T> {
  success?: boolean;
  data: T;
  message: string;
  timestamp?: string;
}
