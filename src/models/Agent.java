package models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an AI agent "employee" in the system.
 *
 * Agents have two tiers:
 *   MANAGER   — one per department, oversees specialist agents
 *   SPECIALIST — does the actual work (e.g. Lead Qualifier, Content Writer)
 *
 * Agents are SIMULATED — their activity is mocked by SimulationEngine.
 */
public class Agent {

    // ── Status constants ──────────────────────────────────────
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_IDLE   = "IDLE";
    public static final String STATUS_PAUSED = "PAUSED";
    public static final String STATUS_ERROR  = "ERROR";

    // ── Role type constants ───────────────────────────────────
    public static final String ROLE_MANAGER    = "MANAGER";
    public static final String ROLE_SPECIALIST = "SPECIALIST";

    private int id;
    private String name;
    private String roleType;           // MANAGER or SPECIALIST
    private int departmentId;
    private Integer managerAgentId;    // null if this agent IS the manager
    private String status;
    private double performanceScore;   // 0.0 – 100.0
    private int autonomyLevel;         // 1 (needs approval for everything) to 5 (fully autonomous)
    private String currentTask;        // what the agent is doing right now
    private Timestamp createdAt;

    // Display-only fields (joined from other tables when querying)
    private String departmentName;
    private String managerAgentName;
    private List<String> capabilities = new ArrayList<>();

    public Agent() {}

    public Agent(int id, String name, String roleType, int departmentId,
                 Integer managerAgentId, String status, double performanceScore,
                 int autonomyLevel, String currentTask, Timestamp createdAt) {
        this.id              = id;
        this.name            = name;
        this.roleType        = roleType;
        this.departmentId    = departmentId;
        this.managerAgentId  = managerAgentId;
        this.status          = status;
        this.performanceScore = performanceScore;
        this.autonomyLevel   = autonomyLevel;
        this.currentTask     = currentTask;
        this.createdAt       = createdAt;
    }

    // ── Getters ───────────────────────────────────────────────

    public int getId()                    { return id; }
    public String getName()               { return name; }
    public String getRoleType()           { return roleType; }
    public int getDepartmentId()          { return departmentId; }
    public Integer getManagerAgentId()    { return managerAgentId; }
    public String getStatus()             { return status; }
    public double getPerformanceScore()   { return performanceScore; }
    public int getAutonomyLevel()         { return autonomyLevel; }
    public String getCurrentTask()        { return currentTask; }
    public Timestamp getCreatedAt()       { return createdAt; }
    public String getDepartmentName()     { return departmentName; }
    public String getManagerAgentName()   { return managerAgentName; }
    public List<String> getCapabilities() { return capabilities; }

    // ── Setters ───────────────────────────────────────────────

    public void setId(int id)                              { this.id = id; }
    public void setName(String name)                       { this.name = name; }
    public void setRoleType(String roleType)               { this.roleType = roleType; }
    public void setDepartmentId(int departmentId)          { this.departmentId = departmentId; }
    public void setManagerAgentId(Integer managerAgentId)  { this.managerAgentId = managerAgentId; }
    public void setStatus(String status)                   { this.status = status; }
    public void setPerformanceScore(double score)          { this.performanceScore = score; }
    public void setAutonomyLevel(int level)                { this.autonomyLevel = level; }
    public void setCurrentTask(String currentTask)         { this.currentTask = currentTask; }
    public void setCreatedAt(Timestamp createdAt)          { this.createdAt = createdAt; }
    public void setDepartmentName(String departmentName)   { this.departmentName = departmentName; }
    public void setManagerAgentName(String name)           { this.managerAgentName = name; }
    public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }

    /** Returns a short role label for display in UI cards */
    public String getRoleLabel() {
        return ROLE_MANAGER.equals(roleType) ? "Dept. Manager" : "Specialist";
    }

    /** Returns an emoji icon representing this agent's status */
    public String getStatusEmoji() {
        switch (status) {
            case STATUS_ACTIVE: return "🟢";
            case STATUS_IDLE:   return "🟡";
            case STATUS_PAUSED: return "⏸";
            case STATUS_ERROR:  return "🔴";
            default:            return "⚪";
        }
    }

    @Override
    public String toString() {
        return "Agent{id=" + id + ", name='" + name + "', status='" + status + "'}";
    }
}
