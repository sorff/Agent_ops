package models;

import java.sql.Date;

/**
 * Represents a measurable goal for a department.
 * Goals track progress toward a numeric target (e.g., "Close 50 deals by Q2").
 */
public class Goal {

    private int id;
    private int departmentId;
    private String title;
    private double targetValue;    // what we're aiming for (e.g., 100)
    private double currentValue;   // how far we've gotten (e.g., 67)
    private Date deadline;

    // Display-only
    private String departmentName;

    public Goal() {}

    public Goal(int id, int departmentId, String title,
                double targetValue, double currentValue, Date deadline) {
        this.id            = id;
        this.departmentId  = departmentId;
        this.title         = title;
        this.targetValue   = targetValue;
        this.currentValue  = currentValue;
        this.deadline      = deadline;
    }

    // ── Getters ───────────────────────────────────────────────

    public int getId()                  { return id; }
    public int getDepartmentId()        { return departmentId; }
    public String getTitle()            { return title; }
    public double getTargetValue()      { return targetValue; }
    public double getCurrentValue()     { return currentValue; }
    public Date getDeadline()           { return deadline; }
    public String getDepartmentName()   { return departmentName; }

    // ── Setters ───────────────────────────────────────────────

    public void setId(int id)                            { this.id = id; }
    public void setDepartmentId(int departmentId)        { this.departmentId = departmentId; }
    public void setTitle(String title)                   { this.title = title; }
    public void setTargetValue(double targetValue)       { this.targetValue = targetValue; }
    public void setCurrentValue(double currentValue)     { this.currentValue = currentValue; }
    public void setDeadline(Date deadline)               { this.deadline = deadline; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    /**
     * Returns progress as a percentage from 0.0 to 100.0.
     * Caps at 100 even if current exceeds target.
     */
    public double getProgressPercent() {
        if (targetValue <= 0) return 0;
        return Math.min(100.0, (currentValue / targetValue) * 100.0);
    }

    @Override
    public String toString() {
        return "Goal{id=" + id + ", title='" + title + "', progress=" + getProgressPercent() + "%}";
    }
}
