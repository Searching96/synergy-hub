/**
 * Full OrganizationWelcome Page Implementation
 * Save as: src/pages/OrganizationWelcome.tsx
 */

import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Building2, Users, Plus, ArrowRight, Mail, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { organizationService } from "@/services/organization.service";
import { toast } from "sonner";

export default function OrganizationWelcome() {
  const navigate = useNavigate();
  const [activeOption, setActiveOption] = useState<"create" | "join" | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");

  const [createForm, setCreateForm] = useState({
    name: "",
    address: "",
    contactEmail: "",
  });

  const [joinForm, setJoinForm] = useState({
    inviteCode: "",
    organizationEmail: "",
  });

  const handleCreateOrganization = async () => {
    if (!createForm.name.trim()) {
      toast.error("Organization name is required");
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await organizationService.createOrganization({
        name: createForm.name,
        address: createForm.address || undefined,
        contactEmail: createForm.contactEmail || undefined,
      });

      if (response.success) {
        setSuccessMessage("Organization created successfully! Redirecting...");

        // Update user in localStorage with organization
        const currentUser = JSON.parse(localStorage.getItem("user") || "{}");
        currentUser.organizationId = response.data.id;
        localStorage.setItem("user", JSON.stringify(currentUser));

        setTimeout(() => {
          navigate("/dashboard");
        }, 2000);
      } else {
        toast.error(response.message || "Failed to create organization");
      }
    } catch (error: any) {
      console.error("Failed to create organization:", error);
      toast.error(error.response?.data?.message || "Failed to create organization");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleJoinOrganization = async () => {
    if (!joinForm.inviteCode && !joinForm.organizationEmail) {
      toast.error("Please provide either an invite code or organization email");
      return;
    }

    setIsSubmitting(true);

    try {
      let response;

      if (joinForm.inviteCode) {
        // Join with invite code
        response = await organizationService.joinOrganization({
          inviteCode: joinForm.inviteCode,
        });

        if (response.success) {
          setSuccessMessage("Joined organization successfully! Redirecting...");

          // Update user in localStorage
          const currentUser = JSON.parse(localStorage.getItem("user") || "{}");
          currentUser.organizationId = response.data.id;
          localStorage.setItem("user", JSON.stringify(currentUser));

          setTimeout(() => {
            navigate("/dashboard");
          }, 2000);
        }
      } else if (joinForm.organizationEmail) {
        // Request to join via email
        response = await organizationService.requestJoinOrganization(
          joinForm.organizationEmail
        );

        if (response.success) {
          setSuccessMessage(
            "Join request sent! You'll be notified once the admin approves your request."
          );

          setTimeout(() => {
            navigate("/dashboard");
          }, 3000);
        }
      }

      if (!response?.success) {
        toast.error(response?.message || "Failed to join organization");
      }
    } catch (error: any) {
      console.error("Failed to join organization:", error);
      toast.error(error.response?.data?.message || "Failed to join organization");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50 flex items-center justify-center p-4">
      <div className="w-full max-w-4xl">
        <div className="text-center mb-12">
          <div className="inline-flex items-center justify-center h-16 w-16 rounded-full bg-blue-600 mb-4">
            <Building2 className="h-8 w-8 text-white" />
          </div>
          <h1 className="text-4xl font-bold text-gray-900 mb-3">
            Welcome to Your Workspace
          </h1>
          <p className="text-lg text-gray-600 max-w-2xl mx-auto">
            To get started, you'll need to either create a new organization or join an existing one.
          </p>
        </div>

        {!activeOption && !successMessage && (
          <div className="grid md:grid-cols-2 gap-6">
            {/* Create Organization Card */}
            <button
              onClick={() => setActiveOption("create")}
              className="group relative bg-white rounded-2xl p-8 shadow-sm border-2 border-gray-200 hover:border-blue-500 hover:shadow-xl transition-all duration-300 text-left"
            >
              <div className="absolute top-6 right-6 h-12 w-12 rounded-full bg-blue-100 group-hover:bg-blue-600 flex items-center justify-center transition-colors">
                <Plus className="h-6 w-6 text-blue-600 group-hover:text-white transition-colors" />
              </div>

              <div className="pr-16">
                <h3 className="text-2xl font-bold text-gray-900 mb-3">
                  Create Organization
                </h3>
                <p className="text-gray-600 mb-6">
                  Start fresh by creating your own organization. You'll be the admin and can invite team members.
                </p>

                <div className="space-y-2">
                  <div className="flex items-center gap-2 text-sm text-gray-700">
                    <CheckCircle2 className="h-4 w-4 text-green-600" />
                    <span>Full admin control</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-gray-700">
                    <CheckCircle2 className="h-4 w-4 text-green-600" />
                    <span>Invite unlimited members</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-gray-700">
                    <CheckCircle2 className="h-4 w-4 text-green-600" />
                    <span>Customize settings</span>
                  </div>
                </div>
              </div>

              <div className="mt-6 flex items-center text-blue-600 font-medium group-hover:translate-x-2 transition-transform">
                Get Started
                <ArrowRight className="ml-2 h-4 w-4" />
              </div>
            </button>

            {/* Join Organization Card */}
            <button
              onClick={() => setActiveOption("join")}
              className="group relative bg-white rounded-2xl p-8 shadow-sm border-2 border-gray-200 hover:border-purple-500 hover:shadow-xl transition-all duration-300 text-left"
            >
              <div className="absolute top-6 right-6 h-12 w-12 rounded-full bg-purple-100 group-hover:bg-purple-600 flex items-center justify-center transition-colors">
                <Users className="h-6 w-6 text-purple-600 group-hover:text-white transition-colors" />
              </div>

              <div className="pr-16">
                <h3 className="text-2xl font-bold text-gray-900 mb-3">
                  Join Organization
                </h3>
                <p className="text-gray-600 mb-6">
                  Join an existing organization using an invite code or request to join via email.
                </p>

                <div className="space-y-2">
                  <div className="flex items-center gap-2 text-sm text-gray-700">
                    <CheckCircle2 className="h-4 w-4 text-green-600" />
                    <span>Use invite code</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-gray-700">
                    <CheckCircle2 className="h-4 w-4 text-green-600" />
                    <span>Request to join</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-gray-700">
                    <CheckCircle2 className="h-4 w-4 text-green-600" />
                    <span>Instant access</span>
                  </div>
                </div>
              </div>

              <div className="mt-6 flex items-center text-purple-600 font-medium group-hover:translate-x-2 transition-transform">
                Get Started
                <ArrowRight className="ml-2 h-4 w-4" />
              </div>
            </button>
          </div>
        )}

        {/* Success Message */}
        {successMessage && (
          <div className="bg-white rounded-2xl p-12 shadow-lg border-2 border-green-200 text-center">
            <div className="inline-flex items-center justify-center h-16 w-16 rounded-full bg-green-100 mb-4">
              <CheckCircle2 className="h-8 w-8 text-green-600" />
            </div>
            <h3 className="text-2xl font-bold text-gray-900 mb-2">
              {successMessage}
            </h3>
            <div className="flex items-center justify-center gap-2 mt-6">
              <div className="h-2 w-2 bg-green-600 rounded-full animate-pulse" />
              <div className="h-2 w-2 bg-green-600 rounded-full animate-pulse" style={{ animationDelay: "0.2s" }} />
              <div className="h-2 w-2 bg-green-600 rounded-full animate-pulse" style={{ animationDelay: "0.4s" }} />
            </div>
          </div>
        )}

        {/* Create Organization Dialog */}
        <Dialog open={activeOption === "create"} onOpenChange={() => setActiveOption(null)}>
          <DialogContent className="sm:max-w-[500px]">
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2 text-2xl">
                <div className="h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center">
                  <Building2 className="h-5 w-5 text-blue-600" />
                </div>
                Create Your Organization
              </DialogTitle>
              <DialogDescription>
                Fill in the details below to create your organization. You can update these later.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4 mt-4">
              <div className="space-y-2">
                <Label htmlFor="org-name">
                  Organization Name <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="org-name"
                  placeholder="Acme Corporation"
                  value={createForm.name}
                  onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="org-email">Contact Email</Label>
                <Input
                  id="org-email"
                  type="email"
                  placeholder="contact@acme.com"
                  value={createForm.contactEmail}
                  onChange={(e) => setCreateForm({ ...createForm, contactEmail: e.target.value })}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="org-address">Address</Label>
                <Textarea
                  id="org-address"
                  placeholder="123 Main Street, City, Country"
                  rows={3}
                  value={createForm.address}
                  onChange={(e) => setCreateForm({ ...createForm, address: e.target.value })}
                />
              </div>

              <div className="flex items-center gap-3 pt-4">
                <Button
                  variant="outline"
                  onClick={() => setActiveOption(null)}
                  className="flex-1"
                  disabled={isSubmitting}
                >
                  Cancel
                </Button>
                <Button
                  onClick={handleCreateOrganization}
                  className="flex-1 bg-blue-600 hover:bg-blue-700"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? (
                    <>
                      <div className="h-4 w-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2" />
                      Creating...
                    </>
                  ) : (
                    <>
                      <Building2 className="h-4 w-4 mr-2" />
                      Create Organization
                    </>
                  )}
                </Button>
              </div>
            </div>
          </DialogContent>
        </Dialog>

        {/* Join Organization Dialog */}
        <Dialog open={activeOption === "join"} onOpenChange={() => setActiveOption(null)}>
          <DialogContent className="sm:max-w-[500px]">
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2 text-2xl">
                <div className="h-10 w-10 rounded-full bg-purple-100 flex items-center justify-center">
                  <Users className="h-5 w-5 text-purple-600" />
                </div>
                Join an Organization
              </DialogTitle>
              <DialogDescription>
                Enter an invite code or request to join an organization via email.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4 mt-4">
              <div className="space-y-2">
                <Label htmlFor="invite-code">Invite Code</Label>
                <Input
                  id="invite-code"
                  placeholder="ACME-2024-XYZ123"
                  value={joinForm.inviteCode}
                  onChange={(e) => setJoinForm({ ...joinForm, inviteCode: e.target.value })}
                />
                <p className="text-xs text-muted-foreground">
                  Ask your organization admin for an invite code
                </p>
              </div>

              <div className="relative">
                <div className="absolute inset-0 flex items-center">
                  <span className="w-full border-t" />
                </div>
                <div className="relative flex justify-center text-xs uppercase">
                  <span className="bg-white px-2 text-muted-foreground">Or</span>
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="org-request-email">Request via Email</Label>
                <Input
                  id="org-request-email"
                  type="email"
                  placeholder="admin@organization.com"
                  value={joinForm.organizationEmail}
                  onChange={(e) => setJoinForm({ ...joinForm, organizationEmail: e.target.value })}
                />
                <p className="text-xs text-muted-foreground">
                  Send a request to join the organization
                </p>
              </div>

              <div className="flex items-center gap-3 pt-4">
                <Button
                  variant="outline"
                  onClick={() => setActiveOption(null)}
                  className="flex-1"
                  disabled={isSubmitting}
                >
                  Cancel
                </Button>
                <Button
                  onClick={handleJoinOrganization}
                  className="flex-1 bg-purple-600 hover:bg-purple-700"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? (
                    <>
                      <div className="h-4 w-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2" />
                      Processing...
                    </>
                  ) : (
                    <>
                      <Mail className="h-4 w-4 mr-2" />
                      {joinForm.inviteCode ? "Join Now" : "Send Request"}
                    </>
                  )}
                </Button>
              </div>
            </div>
          </DialogContent>
        </Dialog>
      </div>
    </div>
  );
}