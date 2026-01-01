package com.synergyhub.dto.mapper;

import com.synergyhub.domain.entity.AuditLog;
import com.synergyhub.dto.response.ActivityLogResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ActivityLogMapper {

    @Mapping(target = "id", source = "id")
    
    // ✅ User fields (with null-safety)
    @Mapping(target = "actorId", source = "user.id")
    @Mapping(target = "actorName", source = "user.name")
    @Mapping(target = "actorEmail", source = "user.email")
    
    // ✅ FIXED: Map to correct field names from entity
    @Mapping(target = "eventType", source = "eventType")      // Was "action"
    @Mapping(target = "eventDetails", source = "eventDetails") // Was "details"
    
    // ✅ Context
    @Mapping(target = "ipAddress", source = "ipAddress")
    @Mapping(target = "userAgent", source = "userAgent")
    @Mapping(target = "projectId", source = "projectId")
    
    // ✅ Timestamp
    @Mapping(target = "timestamp", source = "timestamp")
    
    // ✅ Computed field
    @Mapping(target = "systemEvent", source = "user", qualifiedByName = "isSystemEvent")
    ActivityLogResponse toResponse(AuditLog log);

    List<ActivityLogResponse> toResponseList(List<AuditLog> logs);

    /**
     * Helper method to determine if this is a system event (no user).
     */
    @Named("isSystemEvent")
    default boolean isSystemEvent(com.synergyhub.domain.entity.User user) {
        return user == null;
    }
}