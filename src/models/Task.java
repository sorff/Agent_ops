package models;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * Represents a unit of work assigned to an agent.
 * Tasks move through a Kanban pipeline:
 *   BACKLOG → ASSIGNED → IN_PROGRESS → AWAITING_APPROVAL → DONE (or FAILED)
 */
public class Task {

    // ── Status pipeline constants ─────────────────────────────
    public static final String STATUS_BACKLOG            = "BACKLOG";
    public static final String STATUS_ASSIGNED           = "ASSIGNED";
    public static final String STATUS_IN_PROGRESS        = "IN_PROGRESS";
    public static final String STATUS_AWAITING_APPROVAL  = "AWAITING_APPROVAL";
    public static final String STATUS_DONE               = "DONE";
    public static final String STATUS_FAILED             = "FAILED";

    // ── Priority constants ────────────────────────────────────
    public static final String PRIORITY_LOW    = "LOW";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_HIGH   = "HIGH";
    public static final String PRIORITY_URGENT = "URGENT";

    private int id;
    private String title;
    private String description;
    private int agentId;
    private String status;
    private String priority;
    private Date deadline;
    private int createdBy;
    private Timestamp completedAt;
    private Timestamp createdAt;

    // Display-only fields
    private String agentName;
    private String createdByName;

    public Task() {}

    public Task(int id, String title, String description, int agentId,
                String status, String priority, Date deadline,
                int createdBy, Timestamp completedAt, Timestamp createdAt) {
        this.id          = id;
        this.title       = title;
        this.description = description;
        this.agentId     = agentId;
        this.status      = status;
        this.priority    = priority;
        this.deadline    = deadline;
        this.createdBy   = createdBy;
        this.completedAt = completedAt;
        this.createdAt   = createdAt;
    }

    // ── Getters ───────────────────────────────────────────────

    public int getId()                  { return id; }
    public String getTitle()            { return title; }
    public String getDescription()      { return description; }
    public int getAgentId()             { return agentId; }
    public String getStatus()           { return status; }
    public String getPriority()         { return priority; }
    public Date getDeadline()           { return deadline; }
    public int getCreatedBy()           { return createdBy; }
    public Timestamp getCompletedAt()   { return completedAt; }
    public Timestamp getCreatedAt()     { return createdAt; }
    public String getAgentName()        { return agentName; }
    public String getCreatedByName()    { return createdByName; }

    // ── Setters ───────────────────────────────────────────────

    public void setId(int id)                       { this.id = id; }
    public void setTitle(String title)              { this.title = title; }
    public void setDescription(String description)  { this.description = description; }
    public void setAgentId(int agentId)             { this.agentId = agentId; }
    public void setStatus(String status)            { this.status = status; }
    public void setPriority(String priority)        { this.priority = priority; }
    public void setDeadline(Date deadline)          { this.deadline = deadline; }
    public void setCreatedBy(int createdBy)         { this.createdBy = createdBy; }
    public void setCompletedAt(Timestamp ts)        { this.completedAt = ts; }
    public void setCreatedAt(Timestamp ts)          { this.createdAt = ts; }
    public void setAgentName(String agentName)      { this.agentName = agentName; }
    public void setCreatedByName(String name)       { this.createdByName = name; }

    /** Returns a colored label tag for priority — used in Kanban cards */
    public String getPriorityLabel() {
        switch (priority) {
            case PRIORITY_URGENT: return "🔴 URGENT";
            case PRIORITY_HIGH:   return "🟠 HIGH";
            case PRIORITY_MEDIUM: return "🟡 MEDIUM";
            case PRIORITY_LOW:    return "🟢 LOW";
            default:              return priority;
        }
    }

    @Override
    public String toString() {
        return "Task{id=" + id + ", title='" + title + "', status='" + status + "'}";
    }
}
