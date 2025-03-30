package pl.pbs.zwbackend.model;

import jakarta.persistence.*;
import lombok.*;
import pl.pbs.zwbackend.model.enums.ProjectRole;

@Entity
@Table(name = "project_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ProjectUserId.class)
public class ProjectUser {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectRole role;
}
