package com.synergyhub.repository;

import com.synergyhub.domain.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    Optional<Meeting> findByMeetingCode(String meetingCode);
}
