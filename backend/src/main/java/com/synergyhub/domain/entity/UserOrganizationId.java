package com.synergyhub.domain.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UserOrganizationId implements Serializable {
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "organization_id")
    private Long organizationId;
}
