package com.synergyhub.repository;

import com.synergyhub.domain.entity.UserOrganization;
import com.synergyhub.domain.entity.UserOrganizationId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, UserOrganizationId> {
    boolean existsByIdUserIdAndIdOrganizationId(Long userId, Long organizationId);

    List<UserOrganization> findByIdUserId(Long userId);

    Optional<UserOrganization> findByIdUserIdAndIsPrimaryTrue(Long userId);

    @Query("SELECT uo FROM UserOrganization uo WHERE uo.user.id = :userId AND uo.status = 'ACTIVE'")
    List<UserOrganization> findActiveOrganizationsByUserId(@Param("userId") Long userId);

    Page<UserOrganization> findByOrganizationId(Long organizationId, Pageable pageable);
}
