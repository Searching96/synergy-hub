package com.synergyhub.dto.mapper;

import com.synergyhub.domain.entity.AuditLog;
import com.synergyhub.dto.response.ActivityLogResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ActivityLogMapper {

    @Mapping(target = "actorId", source = "user.id")
    @Mapping(target = "actorName", source = "user.name")
    @Mapping(target = "actorEmail", source = "user.email")
    // Assuming you added projectId to AuditLog, map it here. If not, ignore.
    @Mapping(target = "projectId", ignore = true) 
    ActivityLogResponse toResponse(AuditLog log);

    List<ActivityLogResponse> toResponseList(List<AuditLog> logs);
}