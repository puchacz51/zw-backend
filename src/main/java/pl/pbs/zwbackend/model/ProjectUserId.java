package pl.pbs.zwbackend.model;

import java.io.Serializable;
import java.util.Objects;

public class ProjectUserId implements Serializable {
    private Long project;
    private Long user;

    public ProjectUserId() {}

    public ProjectUserId(Long project, Long user) {
        this.project = project;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectUserId that = (ProjectUserId) o;
        return Objects.equals(project, that.project) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, user);
    }
}
