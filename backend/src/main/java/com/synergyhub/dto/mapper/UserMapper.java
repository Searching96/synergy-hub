package com.synergyhub.dto.mapper;

import com.synergyhub.domain.entity.Permission;
import com.synergyhub.domain.entity.Role;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.entity.UserOrganization;
import com.synergyhub.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {


    @Mapping(target = "organizationId", expression = "java(getPrimaryOrganizationId(user))")
    @Mapping(target = "organizationName", expression = "java(getPrimaryOrganizationName(user))")
    @Mapping(target = "roles", expression = "java(mapRolesFromMemberships(user))")
    @Mapping(target = "permissions", expression = "java(mapPermissionsFromMemberships(user))")
    UserResponse toUserResponse(User user);


    // Multi-org: get primary org from memberships
    default Long getPrimaryOrganizationId(User user) {
        if (user.getMemberships() == null || user.getMemberships().isEmpty()) {
            return null;
        }
        return user.getMemberships().stream()
                .filter(uo -> Boolean.TRUE.equals(uo.getIsPrimary()))
                .findFirst()
                .map(uo -> {
                    if (uo.getOrganization() != null && uo.getOrganization().getId() != null) {
                        return uo.getOrganization().getId();
                    }
                    return null;
                })
                .orElse(null);
    }


    default String getPrimaryOrganizationName(User user) {
        if (user.getMemberships() == null || user.getMemberships().isEmpty()) {
            return null;
        }
        return user.getMemberships().stream()
                .filter(uo -> Boolean.TRUE.equals(uo.getIsPrimary()))
                .findFirst()
                .map(uo -> uo.getOrganization() != null ? uo.getOrganization().getName() : null)
                .orElse(null);
    }

    // Map roles from all memberships
    default Set<String> mapRolesFromMemberships(User user) {
        if (user.getMemberships() == null) return Set.of();
        return user.getMemberships().stream()
                .map(UserOrganization::getRole)
                .filter(java.util.Objects::nonNull)
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    // Map permissions from all roles in memberships
    default Set<String> mapPermissionsFromMemberships(User user) {
        if (user.getMemberships() == null) return Set.of();
        return user.getMemberships().stream()
                .map(UserOrganization::getRole)
                .filter(java.util.Objects::nonNull)
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }
}