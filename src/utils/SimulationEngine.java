package utils;

import db.DatabaseHelper;
import models.Agent;

import javax.swing.*;
import java.util.List;
import java.util.Random;

/**
 * SimulationEngine makes the app feel alive by running a background loop that:
 *  - Randomly changes agent statuses (IDLE ↔ ACTIVE)
 *  - Updates what task each agent is currently working on
 *  - Occasionally generates new approval requests
 *  - Occasionally generates new alerts
 *  - Logs agent activity in the audit trail
 *
 * This is how we simulate "AI agents doing real work" without calling any real AI.
 * The simulation ticks every 8 seconds by default.
 *
 * Usage:
 *   SimulationEngine engine = new SimulationEngine(refreshCallback);
 *   engine.start();    // begin simulation
 *   engine.stop();     // stop simulation (called on app close)
 */
public class SimulationEngine {

    // How often the simulation updates (milliseconds)
    private static final int TICK_INTERVAL_MS = 8000;

    // Probability that a given agent changes its task on each tick
    private static final double TASK_CHANGE_PROBABILITY = 0.25;

    // Probability that a new approval request is generated per tick
    private static final double APPROVAL_PROBABILITY = 0.15;

    // Probability that a new alert is generated per tick
    private static final double ALERT_PROBABILITY = 0.10;

    private final DatabaseHelper db;
    private final Runnable onTick;       // called after each tick so UI can refresh
    private final Random rng = new Random();
    private Timer timer;                 // Swing timer — runs on the EDT

    // Sample tasks agents can be doing (keyed by department concept)
    private static final String[] SALES_TASKS = {
        "Scoring 12 new inbound leads",
        "Preparing outreach sequence for Tech Startups batch",
        "Updating deal stage for Acme Corp in CRM",
        "Running lead scoring model on Q2 pipeline",
        "Drafting follow-up email for prospect #B-224",
        "Reviewing competitor pricing intelligence",
        "Generating weekly lead quality report"
    };

    private static final String[] MARKETING_TASKS = {
        "Drafting blog post outline on AI productivity",
        "Analyzing keyword gap vs top 3 competitors",
        "Scheduling social posts for the next 7 days",
        "Writing meta descriptions for 5 landing pages",
        "Compiling monthly content performance report",
        "Building UTM tracking links for summer campaign",
        "Researching trending hashtags in #B2BSaaS"
    };

    private static final String[] SUPPORT_TASKS = {
        "Responding to Ticket #1107 — billing inquiry",
        "Updating FAQ article on password resets",
        "Triaging 8 new support tickets from overnight",
        "Processing refund for Order #ORD-10022",
        "Escalating ticket #1083 to engineering team",
        "Compiling CSAT feedback from past 7 days",
        "Following up on ticket #1042 — still open 2d"
    };

    private static final String[] HR_TASKS = {
        "Screening 8 applications for UX Designer role",
        "Scheduling interviews for Backend Dev round 2",
        "Drafting offer letter template for Q2 hires",
        "Sending onboarding checklist to new hire Maya",
        "Posting updated job description for Sales AE",
        "Running background check on finalist candidate",
        "Compiling weekly hiring funnel metrics"
    };

    private static final String[] FINANCE_TASKS = {
        "Reconciling May 1–7 bank transactions",
        "Processing 3 vendor invoices for approval",
        "Updating expense tracker with April actuals",
        "Generating cashflow projection for Q3",
        "Sending payment reminder for INV-2060",
        "Categorizing 24 transactions from last week",
        "Preparing CFO weekly finance summary"
    };

    private static final String[] LOGISTICS_TASKS = {
        "Monitoring stock levels across 6 SKUs",
        "Tracking shipments SHP-0900 to SHP-0907",
        "Coordinating restock with TechSupplies Inc.",
        "Flagging delayed delivery SHP-0892 for review",
        "Generating weekly on-time delivery report",
        "Updating inventory count after warehouse audit",
        "Reviewing carrier performance for Q1 vs Q2"
    };

    private static final String[] APPROVAL_ACTIONS = {
        "Send follow-up email to prospect #{n}",
        "Publish social media post: '{t}'",
        "Approve expense $#{a} for software subscription",
        "Reorder inventory SKU-#{s} (qty: {q})",
        "Send offer letter to candidate #{c}",
        "Close ticket #{t2} with resolution: refund issued",
        "Update CRM stage for deal #{d} to 'Proposal Sent'"
    };

    private static final String[] ALERT_MESSAGES = {
        "Agent performance score dropped below 75%",
        "Approval queue growing — 10+ items pending",
        "Task deadline missed by agent",
        "Agent has been idle for more than 2 hours",
        "Goal progress falling behind schedule",
        "New high-priority task auto-assigned by system"
    };

    /**
     * Creates a simulation engine.
     * @param onTick  A callback (Runnable) that refreshes the UI after each tick.
     *                Typically calls panel.refreshData() on the currently visible panel.
     */
    public SimulationEngine(Runnable onTick) {
        this.db     = DatabaseHelper.getInstance();
        this.onTick = onTick;
    }

    /** Starts the background simulation loop */
    public void start() {
        // javax.swing.Timer fires on the EDT, so UI updates are always thread-safe
        timer = new Timer(TICK_INTERVAL_MS, e -> tick());
        timer.start();
        System.out.println("🤖 SimulationEngine started (tick every " + TICK_INTERVAL_MS/1000 + "s)");
    }

    /** Stops the simulation (call this when the app closes) */
    public void stop() {
        if (timer != null) timer.stop();
        System.out.println("🤖 SimulationEngine stopped.");
    }

    /**
     * One simulation tick — updates the world.
     * Called automatically every TICK_INTERVAL_MS milliseconds.
     */
    private void tick() {
        List<Agent> agents = db.getAllAgents();
        if (agents.isEmpty()) return;

        // Update some agent statuses and tasks
        for (Agent agent : agents) {
            if (rng.nextDouble() < TASK_CHANGE_PROBABILITY) {
                updateAgentSimulation(agent);
            }
        }

        // Maybe generate a new approval request
        if (rng.nextDouble() < APPROVAL_PROBABILITY && !agents.isEmpty()) {
            Agent agent = agents.get(rng.nextInt(agents.size()));
            generateApproval(agent);
        }

        // Maybe generate a new alert
        if (rng.nextDouble() < ALERT_PROBABILITY) {
            generateAlert(agents);
        }

        // Notify the UI to refresh
        if (onTick != null) onTick.run();
    }

    /** Updates a single agent's status and current task */
    private void updateAgentSimulation(Agent agent) {
        // Decide new status (agents mostly stay active, occasionally go idle)
        String newStatus;
        double roll = rng.nextDouble();
        if (roll < 0.05) {
            newStatus = Agent.STATUS_ERROR;  // 5% chance of error
        } else if (roll < 0.20) {
            newStatus = Agent.STATUS_IDLE;   // 15% chance of idle
        } else {
            newStatus = Agent.STATUS_ACTIVE; // 80% chance of active
        }

        // If already paused, keep paused (only human can un-pause)
        if (Agent.STATUS_PAUSED.equals(agent.getStatus())) return;

        // Pick a realistic task based on department
        String newTask = pickTask(agent);

        db.updateAgentStatus(agent.getId(), newStatus, newStatus.equals(Agent.STATUS_IDLE) ? null : newTask);

        // Log active changes
        if (Agent.STATUS_ACTIVE.equals(newStatus) && newTask != null) {
            db.logActivity("AGENT", agent.getId(), "Started task: " + newTask, null);
        }

        // Update performance score slightly (random walk ±2 points)
        double delta  = (rng.nextDouble() * 4) - 2;  // -2 to +2
        double newScore = Math.min(100, Math.max(40, agent.getPerformanceScore() + delta));
        db.updateAgentScore(agent.getId(), Math.round(newScore * 10.0) / 10.0);
    }

    /** Picks a realistic current task string based on the agent's department */
    private String pickTask(Agent agent) {
        String deptName = agent.getDepartmentName();
        if (deptName == null) deptName = "";
        String[] pool;
        switch (deptName.toLowerCase()) {
            case "sales":     pool = SALES_TASKS;     break;
            case "marketing": pool = MARKETING_TASKS; break;
            case "support":   pool = SUPPORT_TASKS;   break;
            case "hr":        pool = HR_TASKS;         break;
            case "finance":   pool = FINANCE_TASKS;    break;
            case "logistics": pool = LOGISTICS_TASKS;  break;
            default:          pool = SALES_TASKS;      break;
        }
        return pool[rng.nextInt(pool.length)];
    }

    /** Creates a fake approval request from a random agent */
    private void generateApproval(Agent agent) {
        int n = rng.nextInt(900) + 100;
        String[] templates = {
            "Approve sending outreach email to prospect #" + n,
            "Publish weekly performance report to stakeholders",
            "Process vendor invoice payment of $" + (rng.nextInt(5000) + 500),
            "Schedule automated follow-up for " + n + " dormant leads",
            "Update pricing in CRM for Q3 enterprise tier"
        };
        String action = templates[rng.nextInt(templates.length)];
        db.createApproval(agent.getId(), "AGENT_ACTION", action);
        db.logActivity("AGENT", agent.getId(), "Requested approval: " + action, null);
    }

    /** Creates a random alert */
    private void generateAlert(List<Agent> agents) {
        String[] severities = {"INFO", "INFO", "WARNING", "WARNING", "CRITICAL"};
        String severity = severities[rng.nextInt(severities.length)];
        String message  = ALERT_MESSAGES[rng.nextInt(ALERT_MESSAGES.length)];

        Agent agent = agents.get(rng.nextInt(agents.size()));
        db.createAlert(severity, message, "Agent: " + agent.getName());
    }
}
