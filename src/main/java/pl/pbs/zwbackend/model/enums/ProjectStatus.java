package pl.pbs.zwbackend.model.enums;

public enum ProjectStatus {
    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"), 
    COMPLETED("Completed"),
    ON_HOLD("On Hold"),
    CANCELED("Canceled"),
    UNDER_REVIEW("Under Review");

    private final String displayName;

    ProjectStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
