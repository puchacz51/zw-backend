package pl.pbs.zwbackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.pbs.zwbackend.model.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.project.id = :projectId ORDER BY cm.timestamp DESC")
    Page<ChatMessage> findByProjectIdOrderByTimestampDesc(@Param("projectId") Long projectId, Pageable pageable);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.project IS NULL ORDER BY cm.timestamp DESC")
    Page<ChatMessage> findGlobalMessagesOrderByTimestampDesc(Pageable pageable);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.project.id = :projectId AND cm.timestamp > :since ORDER BY cm.timestamp ASC")
    List<ChatMessage> findRecentProjectMessages(@Param("projectId") Long projectId, @Param("since") LocalDateTime since);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.project IS NULL AND cm.timestamp > :since ORDER BY cm.timestamp ASC")
    List<ChatMessage> findRecentGlobalMessages(@Param("since") LocalDateTime since);
}
