package com.synergyhub.dto.mapper;

import com.synergyhub.domain.entity.Organization;
import com.synergyhub.dto.response.OrganizationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    @Mapping(target = "userCount", ignore = true)
    OrganizationResponse toOrganizationResponse(Organization organization);
}