package pl.pbs.zwbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.pbs.zwbackend.model.Project;
import pl.pbs.zwbackend.model.User;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByCreatedBy(User createdBy);
    List<Project> findByCreatedById(Long userId);
}
