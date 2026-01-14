import { useProject } from "@/context/ProjectContext";
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { projectService } from "@/services/project.service";
import { useUpdateProject, useDeleteProject } from "@/hooks/useProjects";
import { useToast } from "@/hooks/use-toast";
import type { ProjectMember } from "@/types/project.types";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Trash2, UserPlus, Loader2, Mail, Archive } from "lucide-react";
import { ProjectBreadcrumb } from "@/components/project/ProjectBreadcrumb";

export default function ProjectSettingsPage() {
  const { project } = useProject();
  const navigate = useNavigate();
  const { toast } = useToast();
  const queryClient = useQueryClient();

  // Helper to format role
  const formatRole = (role: string) => {
    return role.split('_').map(word =>
      word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
    ).join(' ');
  };

  // Details form state
  const [name, setName] = useState(project?.name || "");
  const [description, setDescription] = useState(project?.description || "");

  // Add member dialog state
  const [addMemberOpen, setAddMemberOpen] = useState(false);
  const [memberEmail, setMemberEmail] = useState("");
  const [memberRole, setMemberRole] = useState("DEVELOPER");

  // Delete confirmation state
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [archiveDialogOpen, setArchiveDialogOpen] = useState(false);

  // Fetch project members
  const { data: membersResponse, isLoading: loadingMembers } = useQuery({
    queryKey: ["projectMembers", project?.id],
    queryFn: () => projectService.getProjectMembers(project?.id),
    enabled: !!project?.id,
  });

  const members: ProjectMember[] = membersResponse?.data || [];

  // Mutations
  const updateProjectMutation = useUpdateProject(project?.id);
  const deleteProjectMutation = useDeleteProject();

  const archiveProjectMutation = useMutation({
    mutationFn: (projectId: number) => projectService.archiveProject(projectId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["projects"] });
      toast({
        title: "Success",
        description: "Project archived successfully",
      });
      navigate("/projects");
    },
    onError: (error: any) => {
      toast({
        title: "Error",
        description: error?.response?.data?.error || error?.response?.data?.message || "Failed to archive project",
        variant: "destructive",
      });
    },
  });

  const addMemberMutation = useMutation({
    mutationFn: (data: { email: string; role: string }) =>
      projectService.addProjectMember(project?.id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["projectMembers", project?.id] });
      setAddMemberOpen(false);
      setMemberEmail("");
      setMemberRole("DEVELOPER");
      toast({
        title: "Success",
        description: "Member added successfully",
      });
    },
    onError: (error: any) => {
      toast({
        title: "Error",
        description: error?.response?.data?.error || error?.response?.data?.message || "Failed to add member",
        variant: "destructive",
      });
    },
  });

  const removeMemberMutation = useMutation({
    mutationFn: (userId: number) =>
      projectService.removeProjectMember(project?.id, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["projectMembers", project?.id] });
      toast({
        title: "Success",
        description: "Member removed successfully",
      });
    },
    onError: (error: any) => {
      toast({
        title: "Error",
        description: error?.response?.data?.message || "Failed to remove member",
        variant: "destructive",
      });
    },
  });

  const handleUpdateDetails = () => {
    updateProjectMutation.mutate({ name, description });
  };

  const handleAddMember = () => {
    if (!memberEmail) {
      toast({
        title: "Error",
        description: "Email is required",
        variant: "destructive",
      });
      return;
    }
    addMemberMutation.mutate({ email: memberEmail, role: memberRole });
  };

  const handleRemoveMember = (userId: number) => {
    removeMemberMutation.mutate(userId);
  };

  const handleDeleteProject = () => {
    if (!project?.id) return;
    deleteProjectMutation.mutate(project.id, {
      onSuccess: () => {
        navigate("/projects");
      },
    });
  };

  const handleArchiveProject = () => {
    if (!project?.id) return;
    archiveProjectMutation.mutate(project.id);
  };

  // Sync form state with project data
  // Sync form state with project data
  useEffect(() => {
    if (project) {
      setName(project.name);
      setDescription(project.description || "");
    }
  }, [project]);

  return (
    <div className="p-6">
      <div className="mb-4">
        <ProjectBreadcrumb current="Settings" />
      </div>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold">Project Settings</h1>
          <p className="text-muted-foreground mt-1">
            Configure settings for {project?.name}
          </p>
        </div>

        {/* Details Section */}
        <Card>
          <CardHeader>
            <CardTitle>Project Details</CardTitle>
            <CardDescription>
              Update your project name and description
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">Project Name</Label>
              <Input
                id="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Enter project name"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Enter project description"
                rows={4}
              />
            </div>

            <Button
              onClick={handleUpdateDetails}
              disabled={updateProjectMutation.isPending}
              className="bg-blue-600 hover:bg-blue-700"
            >
              {updateProjectMutation.isPending && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              Save Changes
            </Button>
          </CardContent>
        </Card>

        {/* Access Section */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle>Project Members</CardTitle>
                <CardDescription>
                  Manage who has access to this project
                </CardDescription>
              </div>
              <Button
                onClick={() => setAddMemberOpen(true)}
                className="bg-blue-600 hover:bg-blue-700"
              >
                <UserPlus className="mr-2 h-4 w-4" />
                Add Member
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {loadingMembers ? (
              <div className="flex items-center justify-center py-8">
                <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
              </div>
            ) : members.length === 0 ? (
              <p className="text-center text-muted-foreground py-8">
                No members yet. Add your first member!
              </p>
            ) : (
              <div className="space-y-2">
                {members.map((member: any) => (
                  <div
                    key={member.user?.id || member.userId}
                    className="flex items-center justify-between p-3 border rounded-lg hover:bg-muted/50 transition-colors"
                  >
                    <div className="flex items-center gap-3">
                      <div className="h-10 w-10 rounded-full bg-blue-600 text-white flex items-center justify-center font-semibold">
                        {(member.user?.name || member.name)?.charAt(0).toUpperCase()}
                      </div>
                      <div>
                        <p className="font-medium">{member.user?.name || member.name}</p>
                        <p className="text-sm text-muted-foreground">{member.user?.email || member.email}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium px-3 py-1 bg-muted rounded-full">
                        {formatRole(member.role)}
                      </span>
                      {member.role !== "OWNER" && member.role !== "PROJECT_LEAD" && (
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleRemoveMember(member.user?.id || member.userId)}
                          disabled={removeMemberMutation.isPending}
                        >
                          <Trash2 className="h-4 w-4 text-destructive" />
                        </Button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Danger Zone */}
        <Card className="border-destructive">
          <CardHeader>
            <CardTitle className="text-destructive">Danger Zone</CardTitle>
            <CardDescription>
              Irreversible actions that will permanently affect this project
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between p-4 border border-orange-300 rounded-lg">
              <div>
                <h3 className="font-semibold">Archive Project</h3>
                <p className="text-sm text-muted-foreground">
                  Archive this project. It can be restored from the archived projects list.
                </p>
              </div>
              <Button
                variant="outline"
                className="border-orange-300 text-orange-600 hover:bg-orange-50"
                onClick={() => setArchiveDialogOpen(true)}
              >
                <Archive className="mr-2 h-4 w-4" />
                Archive
              </Button>
            </div>

            <div className="flex items-center justify-between p-4 border border-destructive rounded-lg">
              <div>
                <h3 className="font-semibold">Delete Project Permanently</h3>
                <p className="text-sm text-muted-foreground">
                  Permanently delete this project and all its data. This cannot be undone.
                </p>
              </div>
              <Button
                variant="destructive"
                onClick={() => setDeleteDialogOpen(true)}
              >
                <Trash2 className="mr-2 h-4 w-4" />
                Delete Forever
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* Add Member Dialog */}
        <Dialog open={addMemberOpen} onOpenChange={setAddMemberOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Add Project Member</DialogTitle>
              <DialogDescription>
                Invite a new member to this project by their email address
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="member-email">Email Address</Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="member-email"
                    type="email"
                    placeholder="member@example.com"
                    value={memberEmail}
                    onChange={(e) => setMemberEmail(e.target.value)}
                    className="pl-9"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="member-role">Role</Label>
                <Select value={memberRole} onValueChange={setMemberRole}>
                  <SelectTrigger id="member-role">
                    <SelectValue placeholder="Select role" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem key="PROJECT_LEAD" value="PROJECT_LEAD">Project Lead</SelectItem>
                    <SelectItem key="PRODUCT_OWNER" value="PRODUCT_OWNER">Product Owner</SelectItem>
                    <SelectItem key="SCRUM_MASTER" value="SCRUM_MASTER">Scrum Master</SelectItem>
                    <SelectItem key="DEVELOPER" value="DEVELOPER">Developer</SelectItem>
                    <SelectItem key="TESTER" value="TESTER">Tester</SelectItem>
                    <SelectItem key="DESIGNER" value="DESIGNER">Designer</SelectItem>
                    <SelectItem key="BUSINESS_ANALYST" value="BUSINESS_ANALYST">Business Analyst</SelectItem>
                    <SelectItem key="STAKEHOLDER" value="STAKEHOLDER">Stakeholder</SelectItem>
                  </SelectContent>
                </Select>
                <p className="text-xs text-muted-foreground">
                  Project Leads and Product Owners have full access. Developers, Testers, and Designers can manage tasks. Stakeholders are read-only.
                </p>
              </div>
            </div>

            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => setAddMemberOpen(false)}
              >
                Cancel
              </Button>
              <Button
                onClick={handleAddMember}
                disabled={addMemberMutation.isPending}
                className="bg-blue-600 hover:bg-blue-700"
              >
                {addMemberMutation.isPending && (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                )}
                Add Member
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* Archive Confirmation Dialog */}
        <AlertDialog open={archiveDialogOpen} onOpenChange={setArchiveDialogOpen}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Archive this project?</AlertDialogTitle>
              <AlertDialogDescription>
                This will archive the project <span className="font-semibold">{project?.name}</span>.
                You can restore it later from the archived projects tab.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Cancel</AlertDialogCancel>
              <AlertDialogAction
                onClick={handleArchiveProject}
                className="bg-orange-600 text-white hover:bg-orange-700"
                disabled={archiveProjectMutation.isPending}
              >
                {archiveProjectMutation.isPending && (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                )}
                Archive Project
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>

        {/* Delete Confirmation Dialog */}
        <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>
              <AlertDialogDescription>
                This action cannot be undone. This will permanently delete the project
                <span className="font-semibold"> {project?.name}</span> and remove all
                associated data including sprints, tasks, and comments.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Cancel</AlertDialogCancel>
              <AlertDialogAction
                onClick={handleDeleteProject}
                className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                disabled={deleteProjectMutation.isPending}
              >
                {deleteProjectMutation.isPending && (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                )}
                Delete Project
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </div>
  );
}
