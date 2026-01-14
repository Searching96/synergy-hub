package com.synergyhub.service.team;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.domain.entity.Team;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.request.CreateTeamRequest;
import com.synergyhub.dto.response.TeamResponse;
import com.synergyhub.exception.ResourceNotFoundException;
import com.synergyhub.repository.OrganizationRepository;
import com.synergyhub.repository.TeamRepository;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.security.OrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request, User creator) {
        Long orgId = OrganizationContext.getcurrentOrgId();
        log.info("Creating team: {} for organization: {}", request.getName(), orgId);
        Organization organization = organizationRepository.findById(orgId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));
        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .organization(organization)
                .build();
        Team savedTeam = teamRepository.save(team);
        log.info("Team created successfully: {}", savedTeam.getId());
        return mapToResponse(savedTeam);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getOrganizationTeams(Long organizationId, User user) {
        Long orgId = OrganizationContext.getcurrentOrgId();
        if (!orgId.equals(organizationId)) {
            throw new ResourceNotFoundException("Organization", "id", organizationId);
        }
        return teamRepository.findByOrganizationId(organizationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeam(Long teamId, User user) {
        Long orgId = OrganizationContext.getcurrentOrgId();
        Team team = teamRepository.findByIdInOrganization(teamId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        return mapToResponse(team);
    }

    @Transactional
    public void deleteTeam(Long teamId, User user) {
        Long orgId = OrganizationContext.getcurrentOrgId();
        Team team = teamRepository.findByIdInOrganization(teamId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        teamRepository.delete(team);
        log.info("Team deleted: {}", teamId);
    }

    @Transactional
    public TeamResponse addMember(Long teamId, Long userId, User currentUser) {
        Long orgId = OrganizationContext.getcurrentOrgId();
        Team team = teamRepository.findByIdInOrganization(teamId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        User memberToAdd = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        team.addMember(memberToAdd);
        Team savedTeam = teamRepository.save(team);
        log.info("Added user {} to team {}", userId, teamId);
        return mapToResponse(savedTeam);
    }

    @Transactional
    public TeamResponse removeMember(Long teamId, Long userId, User currentUser) {
        Long orgId = OrganizationContext.getcurrentOrgId();
        Team team = teamRepository.findByIdInOrganization(teamId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        User memberToRemove = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        team.removeMember(memberToRemove);
        Team savedTeam = teamRepository.save(team);
        log.info("Removed user {} from team {}", userId, teamId);
        return mapToResponse(savedTeam);
    }

    private TeamResponse mapToResponse(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .organizationId(team.getOrganization().getId())
                .name(team.getName())
                .description(team.getDescription())
                .memberIds(team.getMembers().stream()
                        .map(User::getId)
                        .collect(Collectors.toSet()))
                .memberCount(team.getMembers().size())
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }
}
