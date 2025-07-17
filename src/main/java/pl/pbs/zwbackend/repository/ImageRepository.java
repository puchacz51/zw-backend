package pl.pbs.zwbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.pbs.zwbackend.model.Image;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    Optional<Image> findByStoredFileNameAndSubDirectory(String storedFileName, String subDirectory);
    List<Image> findAllByProjectId(Long projectId);
    List<Image> findAllByUploadedById(Long userId);
    Optional<Image> findByIdAndUploadedById(Long id, Long userId);
}
