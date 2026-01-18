import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import { useQuery } from "@tanstack/react-query";
import { teamService } from "@/services/team.service";
import { Loader2, Users } from "lucide-react";

interface TeamSelectProps {
    value?: string;
    onValueChange: (value: string) => void;
    disabled?: boolean;
}

export function TeamSelect({ value, onValueChange, disabled }: TeamSelectProps) {
    const { data: teams = [], isLoading } = useQuery({
        queryKey: ["teams"],
        queryFn: () => teamService.getOrganizationTeams(),
    });

    return (
        <div className="grid gap-2">
            <label className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
                Team
            </label>
            <Select
                value={value ? String(value) : undefined}
                onValueChange={onValueChange}
                disabled={disabled || isLoading}
            >
                <SelectTrigger className="w-full">
                    <SelectValue placeholder="Select a team" />
                </SelectTrigger>
                <SelectContent>
                    {isLoading ? (
                        <div className="flex items-center justify-center p-2">
                            <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                        </div>
                    ) : teams.length === 0 ? (
                        <div className="p-2 text-sm text-muted-foreground text-center">
                            No teams found
                        </div>
                    ) : (
                        teams.map((team) => (
                            <SelectItem key={team.id} value={String(team.id)}>
                                <div className="flex items-center gap-2">
                                    <Users className="h-4 w-4 text-muted-foreground" />
                                    <span>{team.name}</span>
                                </div>
                            </SelectItem>
                        ))
                    )}
                </SelectContent>
            </Select>
        </div>
    );
}
