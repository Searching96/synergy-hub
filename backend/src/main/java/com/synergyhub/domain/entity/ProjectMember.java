package com.synergyhub.domain.entity;

import com.synergyhub.domain.enums.ProjectRole;
import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "project_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMember {
    
    @EmbeddedId
    private ProjectMemberId id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("projectId")
    @JoinColumn(name = "project_id")
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private ProjectRole role;
    
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ProjectMemberId implements Serializable {
        @Column(name = "project_id")
        private Integer projectId;
        
        @Column(name = "user_id")
        private Integer userId;
    }
}