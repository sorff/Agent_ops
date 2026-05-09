package models;

import java.sql.Timestamp;

/**
 * Represents one entry in the audit trail.
 * Every action taken by a user OR an agent is recorded here.
 * This is critical for accountability in a real business.
 *
 * Examples:
 *   - USER admin approved task #42
 *   - AGENT Nova Closer completed outreach to Lead #89
 *   - USER sarah hired agent "Marketing Writer"
 */
public class ActivityLog {

    private int id;
    private String actorType;   // "USER" or "AGENT"
    private int actorId;
    private String action;      // short description of what happened
    private String details;     // longer explanation (optional)
    private Timestamp timestamp;

    // Display-only
    private String actorName;

    public ActivityLog() {}

    public ActivityLog(int id, String actorType, int actorId,
                       String action, String details, Timestamp timestamp) {
        this.id        = id;
        this.actorType = actorType;
        this.actorId   = actorId;
        this.action    = action;
        this.details   = details;
        this.timestamp = timestamp;
    }

    // ── Getters ───────────────────────────────────────────────

    public int getId()              { return id; }
    public String getActorType()    { return actorType; }
    public int getActorId()         { return actorId; }
    public String getAction()       { return action; }
    public String getDetails()      { return details; }
    public Timestamp getTimestamp() { return timestamp; }
    public String getActorName()    { return actorName; }

    // ── Setters ───────────────────────────────────────────────

    public void setId(int id)                     { this.id = id; }
    public void setActorType(String actorType)    { this.actorType = actorType; }
    public void setActorId(int actorId)           { this.actorId = actorId; }
    public void setAction(String action)          { this.action = action; }
    public void setDetails(String details)        { this.details = details; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public void setActorName(String actorName)    { this.actorName = actorName; }

    @Override
    public String toString() {
        return "ActivityLog{" + actorType + " #" + actorId + " → " + action + "}";
    }
}
