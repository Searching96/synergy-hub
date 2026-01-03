/**
 * SsoProviderForm Component
 * Adaptive form for SAML and OIDC SSO providers
 * Uses React Hook Form + Zod for validation
 * Handles sensitive field masking (Client Secret with eye icon)
 */

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { SsoProviderType } from "@/types/sso.types";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Eye, EyeOff } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * Base schema for all SSO providers
 */
const baseSsoSchema = z.object({
  providerType: z.nativeEnum(SsoProviderType),
  providerName: z.string().min(3, "Provider name must be at least 3 characters"),
  clientId: z.string().min(1, "Client ID is required"),
});

/**
 * OIDC-specific schema
 */
const oidcSchema = baseSsoSchema.extend({
  clientSecret: z.string().min(1, "Client secret is required"),
  metadataUrl: z.string().url("Invalid metadata URL").optional().or(z.literal("")),
});

/**
 * SAML-specific schema
 */
const samlSchema = baseSsoSchema.extend({
  ssoUrl: z.string().url("Invalid SSO URL"),
  entityId: z.string().min(1, "Entity ID is required"),
  certificate: z.string().min(1, "Certificate is required"),
});

/**
 * Union schema that validates based on provider type
 */
const ssoProviderFormSchema = z
  .union([oidcSchema, samlSchema])
  .refine((data) => {
    if (data.providerType === SsoProviderType.OIDC) {
      return "clientSecret" in data && data.clientSecret;
    }
    return true;
  }, "Client secret is required for OIDC");

type SsoProviderFormData = z.infer<typeof ssoProviderFormSchema>;

interface SsoProviderFormProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (data: SsoProviderFormData) => void;
  isLoading?: boolean;
  providerType: SsoProviderType;
  onProviderTypeChange?: (type: SsoProviderType) => void;
}

export function SsoProviderForm({
  open,
  onOpenChange,
  onSubmit,
  isLoading = false,
  providerType,
  onProviderTypeChange,
}: SsoProviderFormProps) {
  const [showSecret, setShowSecret] = useState(false);

  // Select the appropriate schema based on provider type
  const schema =
    providerType === SsoProviderType.OIDC ? oidcSchema : samlSchema;

  const form = useForm<SsoProviderFormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      providerType,
      providerName: "",
      clientId: "",
      ...(providerType === SsoProviderType.OIDC && {
        clientSecret: "",
        metadataUrl: "",
      }),
      ...(providerType === SsoProviderType.SAML && {
        ssoUrl: "",
        entityId: "",
        certificate: "",
      }),
    },
  });

  const handleProviderTypeChange = (newType: SsoProviderType) => {
    form.reset(
      {
        ...form.getValues(),
        providerType: newType,
        // Clear provider-specific fields
        ...(newType === SsoProviderType.OIDC && {
          ssoUrl: undefined,
          entityId: undefined,
          certificate: undefined,
        }),
        ...(newType === SsoProviderType.SAML && {
          clientSecret: undefined,
          metadataUrl: undefined,
        }),
      },
      { keepDefaultValues: false }
    );
    onProviderTypeChange?.(newType);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Register SSO Provider</DialogTitle>
          <DialogDescription>
            Configure a new SSO provider for your organization
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form
            onSubmit={form.handleSubmit(onSubmit)}
            className="space-y-6"
          >
            {/* Provider Type Selection */}
            <FormField
              control={form.control}
              name="providerType"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Provider Type</FormLabel>
                  <div className="flex gap-3">
                    {[SsoProviderType.OIDC, SsoProviderType.SAML].map(
                      (type) => (
                        <button
                          key={type}
                          type="button"
                          onClick={() => {
                            field.onChange(type);
                            handleProviderTypeChange(type);
                          }}
                          className={cn(
                            "flex-1 px-4 py-2 rounded-lg border-2 transition-colors font-medium",
                            field.value === type
                              ? "border-blue-600 bg-blue-50 text-blue-900"
                              : "border-gray-200 bg-white text-gray-700 hover:border-gray-300"
                          )}
                        >
                          {type}
                        </button>
                      )
                    )}
                  </div>
                </FormItem>
              )}
            />

            {/* Common Fields */}
            <FormField
              control={form.control}
              name="providerName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Provider Name</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="e.g., Google Workspace, Okta"
                      disabled={isLoading}
                      {...field}
                    />
                  </FormControl>
                  <FormDescription>
                    A friendly name to identify this provider
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="clientId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Client ID</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="Your provider's client ID"
                      disabled={isLoading}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* OIDC-specific Fields */}
            {providerType === SsoProviderType.OIDC && (
              <>
                <FormField
                  control={form.control}
                  name="clientSecret"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Client Secret</FormLabel>
                      <FormControl>
                        <div className="relative">
                          <Input
                            type={showSecret ? "text" : "password"}
                            placeholder="Your provider's client secret"
                            disabled={isLoading}
                            className="pr-10"
                            {...field}
                          />
                          <button
                            type="button"
                            onClick={() => setShowSecret(!showSecret)}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                          >
                            {showSecret ? (
                              <EyeOff className="h-4 w-4" />
                            ) : (
                              <Eye className="h-4 w-4" />
                            )}
                          </button>
                        </div>
                      </FormControl>
                      <FormDescription>
                        Kept secure and never displayed after creation
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="metadataUrl"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Metadata URL (Optional)</FormLabel>
                      <FormControl>
                        <Input
                          type="url"
                          placeholder="https://your-provider.com/.well-known/openid-configuration"
                          disabled={isLoading}
                          {...field}
                        />
                      </FormControl>
                      <FormDescription>
                        OpenID Connect metadata endpoint
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </>
            )}

            {/* SAML-specific Fields */}
            {providerType === SsoProviderType.SAML && (
              <>
                <FormField
                  control={form.control}
                  name="ssoUrl"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>SSO URL</FormLabel>
                      <FormControl>
                        <Input
                          type="url"
                          placeholder="https://your-provider.com/sso"
                          disabled={isLoading}
                          {...field}
                        />
                      </FormControl>
                      <FormDescription>
                        SAML Identity Provider SSO URL
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="entityId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Entity ID</FormLabel>
                      <FormControl>
                        <Input
                          placeholder="Your provider's entity ID"
                          disabled={isLoading}
                          {...field}
                        />
                      </FormControl>
                      <FormDescription>
                        SAML service provider entity identifier
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="certificate"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>X.509 Certificate</FormLabel>
                      <FormControl>
                        <Textarea
                          placeholder="-----BEGIN CERTIFICATE-----&#10;MIIDXTCCAkWgAwIBAgIJAJ...&#10;-----END CERTIFICATE-----"
                          disabled={isLoading}
                          rows={6}
                          className="font-mono text-xs"
                          {...field}
                        />
                      </FormControl>
                      <FormDescription>
                        SAML IdP X.509 certificate in PEM format
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </>
            )}

            {/* Form Actions */}
            <div className="flex gap-3 justify-end pt-4">
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={isLoading}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={isLoading}>
                {isLoading ? "Registering..." : "Register Provider"}
              </Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
