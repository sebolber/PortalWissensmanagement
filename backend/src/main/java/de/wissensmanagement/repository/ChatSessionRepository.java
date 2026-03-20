package de.wissensmanagement.repository;

import de.wissensmanagement.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    List<ChatSession> findByTenantIdAndUserIdOrderByUpdatedAtDesc(String tenantId, String userId);

    Optional<ChatSession> findByIdAndTenantIdAndUserId(String id, String tenantId, String userId);
}
