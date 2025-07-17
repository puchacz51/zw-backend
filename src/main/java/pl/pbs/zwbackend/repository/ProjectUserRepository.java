package pl.pbs.zwbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.pbs.zwbackend.model.ProjectUser;
import pl.pbs.zwbackend.model.ProjectUserId;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectUserRepository extends JpaRepository<ProjectUser, ProjectUserId> {
    
    List<ProjectUser> findByProjectId(Long projectId);
    
    List<ProjectUser> findByUserId(Long userId);
    
    Optional<ProjectUser> findByProjectIdAndUserId(Long projectId, Long userId);
    
    @Query("SELECT pu FROM ProjectUser pu JOIN FETCH pu.user WHERE pu.project.id = :projectId")
    List<ProjectUser> findByProjectIdWithUsers(@Param("projectId") Long projectId);
    
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);
    
    void deleteByProjectIdAndUserId(Long projectId, Long userId);
    
    void deleteByProjectId(Long projectId);
}
