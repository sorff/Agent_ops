package utils;

import db.DatabaseHelper;
import java.sql.*;
import java.util.Random;

/**
 * Populates the database with realistic seed data for the demo.
 *
 * Call MockDataGenerator.seed() once at app startup if the database is empty.
 * It creates:
 *   - 2 users (admin + supervisor)
 *   - 6 departments
 *   - 1 manager + 3 specialists per department = 24 agents
 *   - 40+ tasks in various states
 *   - 8 pending approvals
 *   - 15 alerts
 *   - 30 activity log entries
 *   - Goals for each department
 */
public class MockDataGenerator {

    private static final Random RNG = new Random();

    /**
     * Main entry point — seeds the database if it's empty.
     * Called from Main.java before the login screen appears.
     */
    public static void seedIfEmpty() {
        DatabaseHelper db = DatabaseHelper.getInstance();
        if (!db.isDatabaseEmpty()) {
            System.out.println("Database already has data — skipping seed.");
            return;
        }
        System.out.println("Seeding database with demo data...");
        seed(db);
        System.out.println("✅ Demo data seeded successfully.");
    }

    private static void seed(DatabaseHelper db) {
        // ── Users ─────────────────────────────────────────────
        // Password for both accounts is "admin123"
        db.createUser("Alex Morgan",    "admin@agentops.com",  "admin123", "ADMIN");
        db.createUser("Sarah Mitchell", "sarah@agentops.com",  "admin123", "SUPERVISOR");

        // ── Departments ───────────────────────────────────────
        int salesId    = db.createDepartment("Sales",     "Lead generation and deal closure",          "TECH", 85000);
        int mktId      = db.createDepartment("Marketing", "Brand awareness and content creation",      "TECH", 60000);
        int supportId  = db.createDepartment("Support",   "Customer care and issue resolution",        "TECH", 45000);
        int hrId       = db.createDepartment("HR",        "Talent acquisition and onboarding",         "TECH", 40000);
        int financeId  = db.createDepartment("Finance",   "Bookkeeping and invoice management",        "TECH", 55000);
        int logisticsId= db.createDepartment("Logistics", "Inventory monitoring and shipment tracking","TECH", 70000);

        // ── Sales Agents ──────────────────────────────────────
        int salesMgr = db.createAgent("ARIA — Sales Director",    "MANAGER",    salesId, null,    4);
        setScore(db, salesMgr, 91.5, "ACTIVE", "Reviewing Q2 pipeline report");
        addCaps(db, salesMgr, "Pipeline Management", "Revenue Forecasting", "Team Oversight");

        int qualifier = db.createAgent("NOVA — Lead Qualifier",   "SPECIALIST", salesId, salesMgr, 3);
        setScore(db, qualifier, 88.2, "ACTIVE", "Qualifying inbound leads from campaign");
        addCaps(db, qualifier, "Lead Scoring", "CRM Integration", "Email Outreach");

        int outreach  = db.createAgent("ECHO — Outreach Agent",   "SPECIALIST", salesId, salesMgr, 3);
        setScore(db, outreach, 79.4, "IDLE", null);
        addCaps(db, outreach, "Cold Email Sequencing", "LinkedIn Automation", "Follow-up Scheduling");

        int closer    = db.createAgent("ZEUS — Closer",           "SPECIALIST", salesId, salesMgr, 4);
        setScore(db, closer, 94.1, "ACTIVE", "Preparing proposal for Enterprise Deal #44");
        addCaps(db, closer, "Contract Drafting", "Negotiation Assistance", "CRM Updates");

        // ── Marketing Agents ──────────────────────────────────
        int mktMgr   = db.createAgent("LUNA — Marketing Director", "MANAGER",   mktId, null,   4);
        setScore(db, mktMgr, 86.3, "ACTIVE", "Reviewing May content calendar");
        addCaps(db, mktMgr, "Campaign Planning", "Budget Allocation", "Analytics Review");

        int writer   = db.createAgent("MUSE — Content Writer",    "SPECIALIST", mktId, mktMgr, 3);
        setScore(db, writer, 82.7, "ACTIVE", "Drafting blog post: '5 AI Trends in 2026'");
        addCaps(db, writer, "Blog Writing", "SEO Copywriting", "Social Captions");

        int seo      = db.createAgent("RANK — SEO Analyst",       "SPECIALIST", mktId, mktMgr, 2);
        setScore(db, seo, 75.9, "IDLE", null);
        addCaps(db, seo, "Keyword Research", "On-Page SEO", "Backlink Analysis");

        int social   = db.createAgent("SPARK — Social Media",     "SPECIALIST", mktId, mktMgr, 3);
        setScore(db, social, 90.0, "AWAITING_APPROVAL".equals("ACTIVE") ? "ACTIVE" : "ACTIVE",
                 "Scheduling LinkedIn post about product launch");
        addCaps(db, social, "Post Scheduling", "Hashtag Strategy", "Engagement Tracking");

        // ── Support Agents ────────────────────────────────────
        int supMgr   = db.createAgent("VERA — Support Director",  "MANAGER",   supportId, null,    4);
        setScore(db, supMgr, 93.2, "ACTIVE", "Monitoring open ticket queue");
        addCaps(db, supMgr, "Queue Management", "Escalation Handling", "SLA Monitoring");

        int tier1    = db.createAgent("HELP — Tier-1 Responder",  "SPECIALIST",supportId, supMgr, 3);
        setScore(db, tier1, 85.5, "ACTIVE", "Responding to Ticket #1042: login issue");
        addCaps(db, tier1, "FAQ Resolution", "Chat Support", "Ticket Triage");

        int escalation= db.createAgent("RISE — Escalation Handler","SPECIALIST",supportId, supMgr, 4);
        setScore(db, escalation, 88.9, "IDLE", null);
        addCaps(db, escalation, "Complex Issue Resolution", "Refund Processing", "VIP Support");

        // ── HR Agents ─────────────────────────────────────────
        int hrMgr    = db.createAgent("SAGE — HR Director",       "MANAGER",   hrId, null,    4);
        setScore(db, hrMgr, 89.1, "ACTIVE", "Reviewing new hire pipeline");
        addCaps(db, hrMgr, "Hiring Strategy", "Policy Management", "Team Culture");

        int recruiter= db.createAgent("HUNT — Recruiter",         "SPECIALIST",hrId, hrMgr,  3);
        setScore(db, recruiter, 77.3, "ACTIVE", "Screening 12 applications for Backend Dev role");
        addCaps(db, recruiter, "Job Posting", "Resume Screening", "Interview Scheduling");

        int onboarding=db.createAgent("ONBO — Onboarding Agent",  "SPECIALIST",hrId, hrMgr,  2);
        setScore(db, onboarding, 80.6, "IDLE", null);
        addCaps(db, onboarding, "New Hire Paperwork", "IT Setup Requests", "Day-1 Welcome");

        // ── Finance Agents ────────────────────────────────────
        int finMgr   = db.createAgent("CASH — Finance Director",  "MANAGER",   financeId, null,    4);
        setScore(db, finMgr, 92.7, "ACTIVE", "Preparing monthly P&L summary");
        addCaps(db, finMgr, "Budget Oversight", "Audit Preparation", "Reporting");

        int bookkeeper=db.createAgent("LEDG — Bookkeeper",        "SPECIALIST",financeId, finMgr, 3);
        setScore(db, bookkeeper, 84.4, "ACTIVE", "Reconciling April transactions");
        addCaps(db, bookkeeper, "Transaction Logging", "Bank Reconciliation", "Expense Categorization");

        int invoicer = db.createAgent("BILL — Invoice Tracker",   "SPECIALIST",financeId, finMgr, 3);
        setScore(db, invoicer, 91.2, "ACTIVE", "Chasing overdue invoice INV-2049");
        addCaps(db, invoicer, "Invoice Generation", "Payment Follow-up", "Vendor Management");

        // ── Logistics Agents ──────────────────────────────────
        int logMgr   = db.createAgent("FLEET — Logistics Director","MANAGER",  logisticsId, null,    4);
        setScore(db, logMgr, 87.8, "ACTIVE", "Reviewing weekly shipment KPIs");
        addCaps(db, logMgr, "Route Optimization", "Vendor Relations", "KPI Reporting");

        int inventory= db.createAgent("STOCK — Inventory Monitor", "SPECIALIST",logisticsId, logMgr, 3);
        setScore(db, inventory, 76.5, "ERROR",  "Low stock alert triggered for SKU-4421");
        addCaps(db, inventory, "Stock Level Monitoring", "Reorder Alerts", "Supplier Coordination");

        int shipment = db.createAgent("TRACK — Shipment Tracker",  "SPECIALIST",logisticsId, logMgr, 3);
        setScore(db, shipment, 83.3, "ACTIVE", "Tracking 7 active shipments");
        addCaps(db, shipment, "Carrier Integration", "Delivery ETA Updates", "Exception Handling");

        // ── Tasks ─────────────────────────────────────────────
        int adminId = getUserId(db, "admin@agentops.com");
        seedTasks(db, qualifier, outreach, closer, writer, seo, social,
                  tier1, recruiter, bookkeeper, invoicer, inventory, adminId);

        // ── Goals ─────────────────────────────────────────────
        db.createGoal(salesId,    "Close 50 enterprise deals",      50,  DateUtils.futureDate(45));
        db.createGoal(salesId,    "Qualify 200 leads",             200,  DateUtils.futureDate(30));
        db.createGoal(mktId,      "Publish 20 blog posts",          20,  DateUtils.futureDate(30));
        db.createGoal(mktId,      "Reach 10K social followers",  10000,  DateUtils.futureDate(90));
        db.createGoal(supportId,  "Achieve 95% CSAT score",         95,  DateUtils.futureDate(30));
        db.createGoal(hrId,       "Fill 5 open positions",           5,  DateUtils.futureDate(60));
        db.createGoal(financeId,  "Process all invoices < 3 days",  30,  DateUtils.futureDate(30));
        db.createGoal(logisticsId,"Reduce delivery delay to < 2%",   2,  DateUtils.futureDate(60));

        // Set some progress on goals
        updateGoalProgress(db, salesId,    1, 31.0);
        updateGoalProgress(db, salesId,    2, 143.0);
        updateGoalProgress(db, mktId,      1, 11.0);
        updateGoalProgress(db, mktId,      2, 7241.0);
        updateGoalProgress(db, supportId,  1, 88.0);
        updateGoalProgress(db, hrId,       1, 2.0);
        updateGoalProgress(db, financeId,  1, 22.0);
        updateGoalProgress(db, logisticsId,1, 3.1);

        // ── Approvals ─────────────────────────────────────────
        db.createApproval(social,    "PUBLISH_POST",
            "Publish LinkedIn post: '🚀 We just hit 10K followers!' with image #social-1024");
        db.createApproval(closer,    "SEND_PROPOSAL",
            "Send proposal PDF to Acme Corp (Value: $48,000/yr) — attached as deal_acme_v3.pdf");
        db.createApproval(invoicer,  "APPROVE_EXPENSE",
            "Approve vendor payment: CloudFlare Pro Plan — $2,400/year (due May 15)");
        db.createApproval(inventory, "REORDER_STOCK",
            "Reorder SKU-4421 (USB-C Cables) — 500 units @ $4.20 each = $2,100 from TechSupplies Inc.");
        db.createApproval(recruiter, "SEND_OFFER",
            "Send job offer to candidate: Maya Chen — Backend Dev, $95K base, start June 1");
        db.createApproval(writer,    "PUBLISH_BLOG",
            "Publish blog post: '5 Ways AI Agents Are Transforming B2B Sales in 2026' (1,800 words)");
        db.createApproval(tier1,     "ISSUE_REFUND",
            "Process refund $149.00 for customer #C-8821 (order #ORD-9912 — wrong item shipped)");
        db.createApproval(bookkeeper,"CLOSE_MONTH",
            "Close April 2026 books and generate P&L statement for CFO review");

        // ── Alerts ────────────────────────────────────────────
        db.createAlert("CRITICAL", "STOCK — Inventory Monitor is in ERROR state", "Agent: STOCK");
        db.createAlert("WARNING",  "Approval queue has 8 pending items awaiting review", "Approvals");
        db.createAlert("CRITICAL", "Shipment SHP-0892 is 3 days overdue",         "Logistics");
        db.createAlert("WARNING",  "Sales goal 'Close 50 deals' is 62% complete with 45 days left", "Sales");
        db.createAlert("INFO",     "ARIA completed Q2 pipeline review report",    "Agent: ARIA");
        db.createAlert("INFO",     "New agent ONBO joined the HR department",     "HR");
        db.createAlert("WARNING",  "SEO score for landing page dropped from 82 to 71", "Marketing");
        db.createAlert("CRITICAL", "Invoice INV-2049 is 14 days overdue ($4,800)","Finance");
        db.createAlert("INFO",     "MUSE published blog post #11 successfully",   "Marketing");
        db.createAlert("WARNING",  "ECHO — Outreach Agent has been idle for 4 hours", "Agent: ECHO");
        db.createAlert("INFO",     "Support CSAT score improved to 88% this week","Support");
        db.createAlert("CRITICAL", "Finance budget utilization at 94% — 6% remaining", "Finance");

        // ── Activity Logs ─────────────────────────────────────
        seedActivityLogs(db, adminId, qualifier, closer, writer, social,
                         tier1, bookkeeper, inventory, salesId);

        // ── Messages ──────────────────────────────────────────
        seedMessages(db, adminId, closer, social, tier1);
    }

    // ── Helper: set agent score + status + task ───────────────

    private static void setScore(DatabaseHelper db, int agentId, double score,
                                 String status, String task) {
        db.updateAgentScore(agentId, score);
        db.updateAgentStatus(agentId, status, task);
    }

    private static void addCaps(DatabaseHelper db, int agentId, String... caps) {
        for (String cap : caps) db.addCapability(agentId, cap);
    }

    // ── Helper: get user ID by email ──────────────────────────

    private static int getUserId(DatabaseHelper db, String email) {
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "SELECT id FROM users WHERE email = ?")) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            System.err.println("getUserId error: " + e.getMessage());
        }
        return 1;
    }

    // ── Helper: update goal progress by position in dept ─────

    private static void updateGoalProgress(DatabaseHelper db, int deptId,
                                           int goalNumber, double value) {
        String sql = "UPDATE goals SET current_value = ? " +
                     "WHERE department_id = ? " +
                     "ORDER BY id LIMIT 1 OFFSET " + (goalNumber - 1);
        // SQLite-compatible approach using subquery
        String sql2 = "UPDATE goals SET current_value = ? WHERE id = (" +
                      "  SELECT id FROM goals WHERE department_id = ? " +
                      "  ORDER BY id LIMIT 1 OFFSET " + (goalNumber - 1) + ")";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql2)) {
            ps.setDouble(1, value);
            ps.setInt(2, deptId);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("updateGoalProgress error: " + e.getMessage());
        }
    }

    // ── Task seeding ──────────────────────────────────────────

    private static void seedTasks(DatabaseHelper db,
                                  int qualifier, int outreach, int closer,
                                  int writer, int seo, int social,
                                  int tier1, int recruiter, int bookkeeper,
                                  int invoicer, int inventory, int adminId) {
        // Sales tasks
        createTask(db, "Qualify Inbound Leads — Batch #88",
                   "Score and categorize 45 inbound leads from last week's campaign",
                   qualifier, "IN_PROGRESS",   "HIGH",   10, adminId);
        createTask(db, "Outreach Campaign — Tech Startups Q2",
                   "Send personalized emails to 120 tech startup founders",
                   outreach,  "ASSIGNED",      "MEDIUM", 14, adminId);
        createTask(db, "Close Deal — Acme Corp",
                   "Finalize enterprise agreement with Acme Corp ($48K ARR)",
                   closer,    "AWAITING_APPROVAL","URGENT", 5, adminId);
        createTask(db, "Follow-up — Lead #201 (TechFlow Inc)",
                   "Second follow-up email to TechFlow Inc after demo call",
                   outreach,  "DONE",          "HIGH",   -1, adminId);
        createTask(db, "Qualify Batch #87",
                   "Previous batch of leads — all scored",
                   qualifier, "DONE",          "MEDIUM", -5, adminId);
        createTask(db, "Discovery Call Notes — Beta Ltd",
                   "Document key pain points from today's discovery call",
                   closer,    "IN_PROGRESS",   "HIGH",   3, adminId);
        createTask(db, "Update CRM Pipeline",
                   "Sync all Q2 deals to CRM system",
                   qualifier, "BACKLOG",       "LOW",    20, adminId);

        // Marketing tasks
        createTask(db, "Blog Post — AI Trends 2026",
                   "Write 1,800-word SEO blog post on AI agent trends",
                   writer,    "IN_PROGRESS",   "HIGH",   7, adminId);
        createTask(db, "LinkedIn Post — Follower Milestone",
                   "Celebrate 10K LinkedIn followers with an engaging post",
                   social,    "AWAITING_APPROVAL","MEDIUM", 2, adminId);
        createTask(db, "SEO Audit — Landing Page",
                   "Fix on-page SEO issues on main landing page (score dropped from 82→71)",
                   seo,       "ASSIGNED",      "HIGH",   5, adminId);
        createTask(db, "Monthly Newsletter",
                   "Draft and schedule May 2026 newsletter",
                   writer,    "BACKLOG",       "MEDIUM", 12, adminId);
        createTask(db, "Instagram Reels — Product Demo",
                   "Create 3 short video scripts for product demo reels",
                   social,    "DONE",          "HIGH",   -2, adminId);

        // Support tasks
        createTask(db, "Resolve Ticket #1042 — Login Issue",
                   "Customer unable to log in after password reset",
                   tier1,     "IN_PROGRESS",   "URGENT", 1, adminId);
        createTask(db, "Process Refund — Order #ORD-9912",
                   "Wrong item shipped; issue $149 refund to customer #C-8821",
                   tier1,     "AWAITING_APPROVAL","HIGH",2, adminId);
        createTask(db, "Weekly CSAT Report",
                   "Compile customer satisfaction scores for week of May 5",
                   tier1,     "DONE",          "MEDIUM", -3, adminId);

        // HR tasks
        createTask(db, "Screen Backend Dev Applications",
                   "Review 12 applications for Senior Backend Developer role",
                   recruiter, "IN_PROGRESS",   "HIGH",   8, adminId);
        createTask(db, "Schedule Interviews — Round 2",
                   "Book 4 second-round interviews for Product Designer role",
                   recruiter, "ASSIGNED",      "MEDIUM", 14, adminId);
        createTask(db, "Prepare Offer Letter — Maya Chen",
                   "Draft and send offer letter for Backend Dev position",
                   recruiter, "AWAITING_APPROVAL","URGENT", 3, adminId);

        // Finance tasks
        createTask(db, "Reconcile April Transactions",
                   "Match all April bank transactions with ledger entries",
                   bookkeeper,"IN_PROGRESS",   "HIGH",   5, adminId);
        createTask(db, "Chase Overdue Invoice INV-2049",
                   "Follow up on $4,800 invoice 14 days past due",
                   invoicer,  "IN_PROGRESS",   "URGENT", 2, adminId);
        createTask(db, "Approve Vendor Payment — CloudFlare",
                   "Process annual renewal for CloudFlare Pro ($2,400)",
                   invoicer,  "AWAITING_APPROVAL","HIGH",3, adminId);
        createTask(db, "Prepare April P&L Statement",
                   "Generate and send P&L report to management for April",
                   bookkeeper,"BACKLOG",       "MEDIUM", 10, adminId);

        // Logistics tasks
        createTask(db, "Reorder SKU-4421 — USB-C Cables",
                   "Stock level critical; reorder 500 units from TechSupplies Inc.",
                   inventory, "AWAITING_APPROVAL","URGENT", 2, adminId);
        createTask(db, "Track Shipment SHP-0892",
                   "Overdue delivery — contact carrier for status update",
                   inventory, "IN_PROGRESS",   "CRITICAL".equals("URGENT") ? "URGENT" : "HIGH",  1, adminId);
        createTask(db, "Weekly Shipment Report",
                   "Compile weekly KPI report: on-time delivery, damage rate",
                   inventory, "DONE",          "MEDIUM", -4, adminId);
    }

    /** Helper to insert a task with a deadline relative to today */
    private static void createTask(DatabaseHelper db, String title, String description,
                                   int agentId, String status, String priority,
                                   int deadlineDays, int createdBy) {
        java.sql.Date deadline = deadlineDays >= 0 ? DateUtils.futureDate(deadlineDays) : null;
        int taskId = db.createTask(title, description, agentId, priority, deadline, createdBy);
        if (taskId > 0 && !status.equals("ASSIGNED")) {
            db.updateTaskStatus(taskId, status);
        }
    }

    // ── Activity log seeding ──────────────────────────────────

    private static void seedActivityLogs(DatabaseHelper db, int adminId,
                                         int qualifier, int closer, int writer,
                                         int social, int tier1, int bookkeeper,
                                         int inventory, int salesDeptId) {
        String[] actions = {
            "Completed task: Qualify Leads Batch #87",
            "Status changed to ACTIVE",
            "Submitted approval request: Close Deal — Acme Corp",
            "Completed task: Instagram Reels — Product Demo",
            "Submitted approval request: LinkedIn Post — Follower Milestone",
            "Updated ticket #1042 — added comment",
            "Completed task: Weekly CSAT Report",
            "Started task: Reconcile April Transactions",
            "Triggered low-stock alert for SKU-4421",
            "Completed task: Track Shipment SHP-0932"
        };
        int[] agentIds = {qualifier, closer, closer, writer, social, tier1, tier1, bookkeeper, inventory, inventory};

        for (int i = 0; i < actions.length; i++) {
            db.logActivity("AGENT", agentIds[i], actions[i], null);
        }

        // User actions
        db.logActivity("USER", adminId, "Approved task: Close Deal — Acme Corp proposal sent", null);
        db.logActivity("USER", adminId, "Hired agent: ARIA — Sales Director", null);
        db.logActivity("USER", adminId, "Created department: Logistics", null);
        db.logActivity("USER", adminId, "Rejected approval: Send mass marketing email (policy violation)", null);
        db.logActivity("USER", adminId, "Updated autonomy level for CASH to 4", null);
    }

    // ── Message seeding ───────────────────────────────────────

    private static void seedMessages(DatabaseHelper db, int adminId,
                                     int closer, int social, int tier1) {
        // Conversation with ZEUS (Closer)
        db.sendMessage("USER",  adminId, "AGENT", closer,
                "Hey ZEUS, how's the Acme deal looking?");
        db.sendMessage("AGENT", closer,  "USER",  adminId,
                "Hi! The proposal is ready. I need your approval before sending it. " +
                "Key terms: $48K ARR, 12-month contract, 30-day onboarding. I've queued it for approval.");
        db.sendMessage("USER",  adminId, "AGENT", closer,
                "Looks good. I'll review the full proposal and approve shortly.");
        db.sendMessage("AGENT", closer,  "USER",  adminId,
                "Understood! I'll prepare the counter-proposal backup in case they negotiate.");

        // Conversation with SPARK (Social)
        db.sendMessage("USER",  adminId, "AGENT", social,
                "SPARK, what's the plan for the follower milestone post?");
        db.sendMessage("AGENT", social,  "USER",  adminId,
                "I've drafted a post celebrating 10K followers on LinkedIn! " +
                "It includes a thank-you message, a stat about our growth, and a CTA. " +
                "I've submitted it for approval — want me to add an image?");
        db.sendMessage("USER",  adminId, "AGENT", social,
                "Yes please add the banner image from the brand kit. Good work!");

        // Conversation with HELP (Tier-1 Support)
        db.sendMessage("AGENT", tier1,   "USER",  adminId,
                "⚠️ Alert: Ticket #1042 is escalating. Customer is requesting a manager callback.");
        db.sendMessage("USER",  adminId, "AGENT", tier1,
                "Noted — escalate to RISE and flag as high priority. I'll personally follow up.");
        db.sendMessage("AGENT", tier1,   "USER",  adminId,
                "Done! Escalated to RISE. Reference: ESC-2026-051. Customer notified.");
    }
}
