package models;

import java.sql.Timestamp;

/**
 * Represents an action proposed by an agent that requires human approval
 * before it can be executed.
 *
 * Examples:
 *   - "Send this email to a client"
 *   - "Approve this invoice for $4,200"
 *   - "Publish this social media post"
 *
 * This is the core of the "human-in-the-loop" feature.
 */
public class Approval {

    // ── Status constants ──────────────────────────────────────
    public static final String STATUS_PENDING  = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private int id;
    private int agentId;
    private String actionType;    // e.g., "SEND_EMAIL", "APPROVE_EXPENSE", "PUBLISH_POST"
    private String payload;       // JSON-like description of what the agent wants to do
    private String status;
    private Integer decidedBy;    // user ID of who approved/rejected (null if still pending)
    private Timestamp decidedAt;
    private Timestamp createdAt;

    // Display-only fields
    private String agentName;
    private String decidedByName;
    private String departmentName;

    public Approval() {}

    public Approval(int id, int agentId, String actionType, String payload,
                    String status, Integer decidedBy,
                    Timestamp decidedAt, Timestamp createdAt) {
        this.id         = id;
        this.agentId    = agentId;
        this.actionType = actionType;
        this.payload    = payload;
        this.status     = status;
        this.decidedBy  = decidedBy;
        this.decidedAt  = decidedAt;
        this.createdAt  = createdAt;
    }

    // ── Getters ───────────────────────────────────────────────

    public int getId()                  { return id; }
    public int getAgentId()             { return agentId; }
    public String getActionType()       { return actionType; }
    public String getPayload()          { return payload; }
    public String getStatus()           { return status; }
    public Integer getDecidedBy()       { return decidedBy; }
    public Timestamp getDecidedAt()     { return decidedAt; }
    public Timestamp getCreatedAt()     { return createdAt; }
    public String getAgentName()        { return agentName; }
    public String getDecidedByName()    { return decidedByName; }
    public String getDepartmentName()   { return departmentName; }

    // ── Setters ───────────────────────────────────────────────

    public void setId(int id)                            { this.id = id; }
    public void setAgentId(int agentId)                  { this.agentId = agentId; }
    public void setActionType(String actionType)         { this.actionType = actionType; }
    public void setPayload(String payload)               { this.payload = payload; }
    public void setStatus(String status)                 { this.status = status; }
    public void setDecidedBy(Integer decidedBy)          { this.decidedBy = decidedBy; }
    public void setDecidedAt(Timestamp decidedAt)        { this.decidedAt = decidedAt; }
    public void setCreatedAt(Timestamp createdAt)        { this.createdAt = createdAt; }
    public void setAgentName(String agentName)           { this.agentName = agentName; }
    public void setDecidedByName(String name)            { this.decidedByName = name; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public boolean isPending() { return STATUS_PENDING.equals(status); }

    @Override
    public String toString() {
        return "Approval{id=" + id + ", action='" + actionType + "', status='" + status + "'}";
    }
}
