package models;

import java.sql.Timestamp;

/**
 * Represents a system notification/alert.
 * Alerts notify supervisors and admins about important events like
 * missed deadlines, KPI breaches, agent errors, or security flags.
 */
public class Alert {

    // ── Severity level constants ──────────────────────────────
    public static final String SEVERITY_INFO     = "INFO";
    public static final String SEVERITY_WARNING  = "WARNING";
    public static final String SEVERITY_CRITICAL = "CRITICAL";

    private int id;
    private String severity;
    private String message;
    private String relatedEntity;   // e.g., "Agent: Nova Closer" or "Department: Sales"
    private boolean acknowledged;   // has a user marked this as read?
    private Timestamp createdAt;

    public Alert() {}

    public Alert(int id, String severity, String message,
                 String relatedEntity, boolean acknowledged, Timestamp createdAt) {
        this.id            = id;
        this.severity      = severity;
        this.message       = message;
        this.relatedEntity = relatedEntity;
        this.acknowledged  = acknowledged;
        this.createdAt     = createdAt;
    }

    // ── Getters ───────────────────────────────────────────────

    public int getId()                  { return id; }
    public String getSeverity()         { return severity; }
    public String getMessage()          { return message; }
    public String getRelatedEntity()    { return relatedEntity; }
    public boolean isAcknowledged()     { return acknowledged; }
    public Timestamp getCreatedAt()     { return createdAt; }

    // ── Setters ───────────────────────────────────────────────

    public void setId(int id)                           { this.id = id; }
    public void setSeverity(String severity)            { this.severity = severity; }
    public void setMessage(String message)              { this.message = message; }
    public void setRelatedEntity(String relatedEntity)  { this.relatedEntity = relatedEntity; }
    public void setAcknowledged(boolean acknowledged)   { this.acknowledged = acknowledged; }
    public void setCreatedAt(Timestamp createdAt)       { this.createdAt = createdAt; }

    /** Returns the emoji icon that goes with this severity level */
    public String getSeverityEmoji() {
        switch (severity) {
            case SEVERITY_CRITICAL: return "🔴";
            case SEVERITY_WARNING:  return "🟠";
            case SEVERITY_INFO:     return "🔵";
            default:                return "⚪";
        }
    }

    @Override
    public String toString() {
        return "Alert{id=" + id + ", severity='" + severity + "', message='" + message + "'}";
    }
}
