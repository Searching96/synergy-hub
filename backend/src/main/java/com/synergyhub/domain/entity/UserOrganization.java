package com.synergyhub.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
// Ensure this import is correct based on your project structure
import com.synergyhub.domain.entity.UserOrganizationId; 

@Entity
@Table(name = "user_organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOrganization {
    @EmbeddedId
    private UserOrganizationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("organizationId")
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "is_primary")
    private Boolean isPrimary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MembershipStatus status;

    // Convenience constructor for User.addMembership
    public UserOrganization(User user, Organization organization, Role role) {
        this.user = user;
        this.organization = organization;
        this.id = new UserOrganizationId(
            user != null ? user.getId() : null,
            organization != null ? organization.getId() : null
        );
        this.role = role;
        this.joinedAt = java.time.LocalDateTime.now();
        this.isPrimary = false;
        this.status = MembershipStatus.ACTIVE;
    }

    public enum MembershipStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }
}