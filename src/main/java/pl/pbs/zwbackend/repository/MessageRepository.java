package pl.pbs.zwbackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.pbs.zwbackend.model.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    @Query("SELECT m FROM Message m ORDER BY m.timestamp DESC")
    Page<Message> findAllMessagesOrderByTimestampDesc(Pageable pageable);
    
    @Query("SELECT m FROM Message m JOIN FETCH m.sender ORDER BY m.timestamp DESC")
    Page<Message> findAllMessagesWithSenderOrderByTimestampDesc(Pageable pageable);
}
