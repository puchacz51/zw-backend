package pl.pbs.zwbackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.pbs.zwbackend.model.Message;

import java.time.LocalDateTime;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m LEFT JOIN FETCH m.sender ORDER BY m.timestamp DESC")
    Page<Message> findAllWithSenderOrderByTimestampDesc(Pageable pageable);

    @Query("SELECT m FROM Message m LEFT JOIN FETCH m.sender " +
           "WHERE (:fromDate IS NULL OR m.timestamp >= :fromDate) " +
           "AND (:toDate IS NULL OR m.timestamp <= :toDate) " +
           "AND (:senderEmail IS NULL OR m.sender.email = :senderEmail) " +
           "AND (:searchKeyword IS NULL OR LOWER(m.content) LIKE LOWER(CONCAT('%', :searchKeyword, '%'))) " +
           "ORDER BY m.timestamp DESC")
    Page<Message> findMessagesWithFilters(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("senderEmail") String senderEmail,
            @Param("searchKeyword") String searchKeyword,
            Pageable pageable
    );

    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE (:fromDate IS NULL OR m.timestamp >= :fromDate) " +
           "AND (:toDate IS NULL OR m.timestamp <= :toDate) " +
           "AND (:senderEmail IS NULL OR m.sender.email = :senderEmail) " +
           "AND (:searchKeyword IS NULL OR LOWER(m.content) LIKE LOWER(CONCAT('%', :searchKeyword, '%')))")
    long countMessagesWithFilters(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("senderEmail") String senderEmail,
            @Param("searchKeyword") String searchKeyword
    );
}
