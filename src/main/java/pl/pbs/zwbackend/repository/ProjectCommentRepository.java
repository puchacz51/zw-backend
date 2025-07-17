package pl.pbs.zwbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.pbs.zwbackend.model.ProjectComment;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectCommentRepository extends JpaRepository<ProjectComment, Long> {
    
    @Query("SELECT pc FROM ProjectComment pc JOIN FETCH pc.user WHERE pc.project.id = :projectId ORDER BY pc.createdAt DESC")
    List<ProjectComment> findByProjectIdWithUser(@Param("projectId") Long projectId);
    
    List<ProjectComment> findByProjectIdOrderByCreatedAtDesc(Long projectId);
      List<ProjectComment> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    Optional<ProjectComment> findByIdAndProjectId(Long id, Long projectId);
    
    Long countByProjectId(Long projectId);
    
    void deleteByProjectId(Long projectId);
}
