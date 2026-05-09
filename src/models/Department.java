package models;

import java.sql.Timestamp;

/**
 * Represents one department in the company (e.g., Sales, HR, Marketing).
 * Each department contains agents that handle work for that business area.
 */
public class Department {

    private int id;
    private String name;
    private String description;
    private String industryTemplate;   // e.g., "TECH", "RETAIL", "HEALTHCARE"
    private double budget;
    private Timestamp createdAt;

    // Extra fields used for display (not stored in DB — we calculate them)
    private int agentCount;            // how many agents are in this department
    private String healthStatus;       // "Healthy", "Warning", or "Critical"

    public Department() {}

    public Department(int id, String name, String description,
                      String industryTemplate, double budget, Timestamp createdAt) {
        this.id              = id;
        this.name            = name;
        this.description     = description;
        this.industryTemplate = industryTemplate;
        this.budget          = budget;
        this.createdAt       = createdAt;
    }

    // ── Getters ───────────────────────────────────────────────

    public int getId()                   { return id; }
    public String getName()              { return name; }
    public String getDescription()       { return description; }
    public String getIndustryTemplate()  { return industryTemplate; }
    public double getBudget()            { return budget; }
    public Timestamp getCreatedAt()      { return createdAt; }
    public int getAgentCount()           { return agentCount; }
    public String getHealthStatus()      { return healthStatus; }

    // ── Setters ───────────────────────────────────────────────

    public void setId(int id)                              { this.id = id; }
    public void setName(String name)                       { this.name = name; }
    public void setDescription(String description)         { this.description = description; }
    public void setIndustryTemplate(String template)       { this.industryTemplate = template; }
    public void setBudget(double budget)                   { this.budget = budget; }
    public void setCreatedAt(Timestamp createdAt)          { this.createdAt = createdAt; }
    public void setAgentCount(int agentCount)              { this.agentCount = agentCount; }
    public void setHealthStatus(String healthStatus)       { this.healthStatus = healthStatus; }

    @Override
    public String toString() {
        return "Department{id=" + id + ", name='" + name + "'}";
    }
}
