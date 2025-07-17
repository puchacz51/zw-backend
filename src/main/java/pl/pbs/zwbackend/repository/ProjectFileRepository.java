package pl.pbs.zwbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.pbs.zwbackend.model.ProjectFile;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectFileRepository extends JpaRepository<ProjectFile, Long> {
    List<ProjectFile> findAllByProjectId(Long projectId);
    List<ProjectFile> findAllByUploadedById(Long userId);
    Optional<ProjectFile> findByIdAndProjectId(Long id, Long projectId);
    Optional<ProjectFile> findByStoredFileName(String storedFileName);
}
