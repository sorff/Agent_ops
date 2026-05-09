package db;

import models.*;
import utils.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseHelper is the single class responsible for ALL database operations.
 * Every other class in the app calls methods here — no other class touches the DB.
 *
 * We use SQLite because it requires zero installation — the database is just a file.
 * To switch to MySQL, see the comment below near the connection setup.
 *
 * Design Pattern: Singleton — there's exactly one DatabaseHelper shared by the whole app.
 * Access it with:  DatabaseHelper db = DatabaseHelper.getInstance();
 */
public class DatabaseHelper {

    // ── SQLite connection (default — works out of the box) ────
    private static final String DB_URL = "jdbc:sqlite:agentops.db";

    // ── MySQL connection (uncomment to use MySQL instead) ─────
    // private static final String DB_URL      = "jdbc:mysql://localhost:3306/agentops";
    // private static final String DB_USER     = "root";
    // private static final String DB_PASSWORD = "";

    // The single shared instance of this class
    private static DatabaseHelper instance;

    // The single open database connection
    private Connection connection;

    // ── Singleton accessor ────────────────────────────────────

    /**
     * Returns the one shared DatabaseHelper.
     * Creates it the first time it's called, reuses it afterward.
     */
    public static DatabaseHelper getInstance() {
        if (instance == null) {
            instance = new DatabaseHelper();
        }
        return instance;
    }

    /** Private constructor — enforces singleton by preventing direct instantiation */
    private DatabaseHelper() {
        try {
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);

            // WAL mode makes SQLite faster for concurrent reads
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA foreign_keys=ON");
            }

            // Create all tables if they don't exist yet
            createTables();
            System.out.println("✅ Database connected: " + DB_URL);

        } catch (ClassNotFoundException e) {
            System.err.println("❌ SQLite JDBC driver not found. Add sqlite-jdbc.jar to /lib folder.");
            System.err.println("   See lib/README.txt for instructions.");
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
        }
    }

    /** Returns the raw Connection — used internally for prepared statements */
    public Connection getConnection() {
        return connection;
    }

    // ── Table creation ────────────────────────────────────────

    /**
     * Creates every database table if it doesn't already exist.
     * Safe to call on every startup — won't overwrite existing data.
     */
    private void createTables() throws SQLException {
        String[] ddl = {
            // Users table
            "CREATE TABLE IF NOT EXISTS users (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  name TEXT NOT NULL," +
            "  email TEXT UNIQUE NOT NULL," +
            "  password_hash TEXT NOT NULL," +
            "  role TEXT NOT NULL DEFAULT 'SUPERVISOR'," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")",

            // Departments table
            "CREATE TABLE IF NOT EXISTS departments (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  name TEXT NOT NULL," +
            "  description TEXT," +
            "  industry_template TEXT," +
            "  budget REAL DEFAULT 0," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")",

            // Agents table
            "CREATE TABLE IF NOT EXISTS agents (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  name TEXT NOT NULL," +
            "  role_type TEXT NOT NULL," +
            "  department_id INTEGER," +
            "  manager_agent_id INTEGER," +
            "  status TEXT DEFAULT 'IDLE'," +
            "  performance_score REAL DEFAULT 0," +
            "  autonomy_level INTEGER DEFAULT 3," +
            "  current_task TEXT," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (department_id) REFERENCES departments(id)," +
            "  FOREIGN KEY (manager_agent_id) REFERENCES agents(id)" +
            ")",

            // Agent capabilities
            "CREATE TABLE IF NOT EXISTS agent_capabilities (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  agent_id INTEGER NOT NULL," +
            "  capability_name TEXT NOT NULL," +
            "  FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE" +
            ")",

            // Tasks table
            "CREATE TABLE IF NOT EXISTS tasks (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  title TEXT NOT NULL," +
            "  description TEXT," +
            "  agent_id INTEGER," +
            "  status TEXT DEFAULT 'BACKLOG'," +
            "  priority TEXT DEFAULT 'MEDIUM'," +
            "  deadline DATE," +
            "  created_by INTEGER," +
            "  completed_at TIMESTAMP," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (agent_id) REFERENCES agents(id)," +
            "  FOREIGN KEY (created_by) REFERENCES users(id)" +
            ")",

            // Goals table
            "CREATE TABLE IF NOT EXISTS goals (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  department_id INTEGER NOT NULL," +
            "  title TEXT NOT NULL," +
            "  target_value REAL DEFAULT 100," +
            "  current_value REAL DEFAULT 0," +
            "  deadline DATE," +
            "  FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE" +
            ")",

            // Approvals table
            "CREATE TABLE IF NOT EXISTS approvals (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  agent_id INTEGER," +
            "  action_type TEXT NOT NULL," +
            "  payload TEXT," +
            "  status TEXT DEFAULT 'PENDING'," +
            "  decided_by INTEGER," +
            "  decided_at TIMESTAMP," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (agent_id) REFERENCES agents(id)," +
            "  FOREIGN KEY (decided_by) REFERENCES users(id)" +
            ")",

            // Activity logs table
            "CREATE TABLE IF NOT EXISTS activity_logs (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  actor_type TEXT NOT NULL," +
            "  actor_id INTEGER NOT NULL," +
            "  action TEXT NOT NULL," +
            "  details TEXT," +
            "  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")",

            // Alerts table
            "CREATE TABLE IF NOT EXISTS alerts (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  severity TEXT NOT NULL DEFAULT 'INFO'," +
            "  message TEXT NOT NULL," +
            "  related_entity TEXT," +
            "  acknowledged INTEGER DEFAULT 0," +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")",

            // Messages table
            "CREATE TABLE IF NOT EXISTS messages (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  sender_type TEXT NOT NULL," +
            "  sender_id INTEGER NOT NULL," +
            "  recipient_type TEXT NOT NULL," +
            "  recipient_id INTEGER NOT NULL," +
            "  body TEXT NOT NULL," +
            "  sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")"
        };

        try (Statement st = connection.createStatement()) {
            for (String sql : ddl) {
                st.execute(sql);
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  USER METHODS
    // ═══════════════════════════════════════════════════════

    /**
     * Checks credentials for login.
     * Returns the User object if email+password match, or null if login fails.
     * The password is hashed with MD5 before comparing (simple, good enough for a demo).
     */
    public User login(String email, String password) {
        String hash = md5(password);
        String sql  = "SELECT * FROM users WHERE email = ? AND password_hash = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, hash);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
        }
        return null;
    }

    /** Returns all users in the system */
    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM users ORDER BY name")) {
            while (rs.next()) list.add(mapUser(rs));
        } catch (SQLException e) {
            System.err.println("getAllUsers error: " + e.getMessage());
        }
        return list;
    }

    /** Inserts a new user. Password is MD5-hashed before storing. */
    public boolean createUser(String name, String email, String password, String role) {
        String sql = "INSERT INTO users (name, email, password_hash, role) VALUES (?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, md5(password));
            ps.setString(4, role);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("createUser error: " + e.getMessage());
            return false;
        }
    }

    /** Checks whether there are any users in the database (used to detect first-time setup) */
    public boolean isDatabaseEmpty() {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) AS cnt FROM users")) {
            return rs.next() && rs.getInt("cnt") == 0;
        } catch (SQLException e) {
            return true;
        }
    }

    // ═══════════════════════════════════════════════════════
    //  DEPARTMENT METHODS
    // ═══════════════════════════════════════════════════════

    /** Returns all departments, each with an agent count */
    public List<Department> getAllDepartments() {
        List<Department> list = new ArrayList<>();
        String sql = "SELECT d.*, " +
                     "(SELECT COUNT(*) FROM agents a WHERE a.department_id = d.id) AS agent_count " +
                     "FROM departments d ORDER BY d.name";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Department d = mapDepartment(rs);
                d.setAgentCount(rs.getInt("agent_count"));
                list.add(d);
            }
        } catch (SQLException e) {
            System.err.println("getAllDepartments error: " + e.getMessage());
        }
        return list;
    }

    /** Returns a single department by ID */
    public Department getDepartmentById(int id) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM departments WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapDepartment(rs);
        } catch (SQLException e) {
            System.err.println("getDepartmentById error: " + e.getMessage());
        }
        return null;
    }

    /** Inserts a new department and returns its generated ID */
    public int createDepartment(String name, String description, String template, double budget) {
        String sql = "INSERT INTO departments (name, description, industry_template, budget) VALUES (?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, template);
            ps.setDouble(4, budget);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            System.err.println("createDepartment error: " + e.getMessage());
        }
        return -1;
    }

    // ═══════════════════════════════════════════════════════
    //  AGENT METHODS
    // ═══════════════════════════════════════════════════════

    /** Returns all agents with their department names joined in */
    public List<Agent> getAllAgents() {
        List<Agent> list = new ArrayList<>();
        String sql = "SELECT a.*, d.name AS dept_name " +
                     "FROM agents a " +
                     "LEFT JOIN departments d ON a.department_id = d.id " +
                     "ORDER BY a.role_type, a.name";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Agent ag = mapAgent(rs);
                ag.setDepartmentName(rs.getString("dept_name"));
                list.add(ag);
            }
        } catch (SQLException e) {
            System.err.println("getAllAgents error: " + e.getMessage());
        }
        return list;
    }

    /** Returns all agents belonging to a specific department */
    public List<Agent> getAgentsByDepartment(int departmentId) {
        List<Agent> list = new ArrayList<>();
        String sql = "SELECT a.*, d.name AS dept_name " +
                     "FROM agents a " +
                     "LEFT JOIN departments d ON a.department_id = d.id " +
                     "WHERE a.department_id = ? " +
                     "ORDER BY a.role_type DESC, a.name";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, departmentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Agent ag = mapAgent(rs);
                ag.setDepartmentName(rs.getString("dept_name"));
                list.add(ag);
            }
        } catch (SQLException e) {
            System.err.println("getAgentsByDepartment error: " + e.getMessage());
        }
        return list;
    }

    /** Returns a single agent by ID (with department name) */
    public Agent getAgentById(int id) {
        String sql = "SELECT a.*, d.name AS dept_name " +
                     "FROM agents a LEFT JOIN departments d ON a.department_id = d.id " +
                     "WHERE a.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Agent ag = mapAgent(rs);
                ag.setDepartmentName(rs.getString("dept_name"));
                // Load the agent's capabilities
                ag.setCapabilities(getCapabilities(id));
                return ag;
            }
        } catch (SQLException e) {
            System.err.println("getAgentById error: " + e.getMessage());
        }
        return null;
    }

    /** Inserts a new agent and returns its generated ID */
    public int createAgent(String name, String roleType, int departmentId,
                           Integer managerAgentId, int autonomyLevel) {
        String sql = "INSERT INTO agents (name, role_type, department_id, manager_agent_id, " +
                     "status, autonomy_level) VALUES (?,?,?,?,'IDLE',?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, roleType);
            ps.setInt(3, departmentId);
            if (managerAgentId != null) ps.setInt(4, managerAgentId); else ps.setNull(4, Types.INTEGER);
            ps.setInt(5, autonomyLevel);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            System.err.println("createAgent error: " + e.getMessage());
        }
        return -1;
    }

    /** Updates an agent's status and current task (called by SimulationEngine) */
    public void updateAgentStatus(int agentId, String status, String currentTask) {
        String sql = "UPDATE agents SET status = ?, current_task = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, currentTask);
            ps.setInt(3, agentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("updateAgentStatus error: " + e.getMessage());
        }
    }

    /** Updates an agent's performance score */
    public void updateAgentScore(int agentId, double score) {
        String sql = "UPDATE agents SET performance_score = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, score);
            ps.setInt(2, agentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("updateAgentScore error: " + e.getMessage());
        }
    }

    /** Updates an agent's autonomy level (from the Settings tab in AgentDetailPanel) */
    public void updateAgentAutonomy(int agentId, int level) {
        String sql = "UPDATE agents SET autonomy_level = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, level);
            ps.setInt(2, agentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("updateAgentAutonomy error: " + e.getMessage());
        }
    }

    /** Returns a list of capability strings for an agent */
    public List<String> getCapabilities(int agentId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT capability_name FROM agent_capabilities WHERE agent_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, agentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getString("capability_name"));
        } catch (SQLException e) {
            System.err.println("getCapabilities error: " + e.getMessage());
        }
        return list;
    }

    /** Adds a capability to an agent */
    public void addCapability(int agentId, String capability) {
        String sql = "INSERT INTO agent_capabilities (agent_id, capability_name) VALUES (?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, agentId);
            ps.setString(2, capability);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("addCapability error: " + e.getMessage());
        }
    }

    /** Returns the total count of agents in the system */
    public int getTotalAgentCount() {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) AS cnt FROM agents")) {
            if (rs.next()) return rs.getInt("cnt");
        } catch (SQLException e) {
            System.err.println("getTotalAgentCount error: " + e.getMessage());
        }
        return 0;
    }

    // ═══════════════════════════════════════════════════════
    //  TASK METHODS
    // ═══════════════════════════════════════════════════════

    /** Returns all tasks with the assigned agent's name joined in */
    public List<Task> getAllTasks() {
        List<Task> list = new ArrayList<>();
        String sql = "SELECT t.*, a.name AS agent_name " +
                     "FROM tasks t LEFT JOIN agents a ON t.agent_id = a.id " +
                     "ORDER BY t.created_at DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Task task = mapTask(rs);
                task.setAgentName(rs.getString("agent_name"));
                list.add(task);
            }
        } catch (SQLException e) {
            System.err.println("getAllTasks error: " + e.getMessage());
        }
        return list;
    }

    /** Returns tasks for a specific agent */
    public List<Task> getTasksByAgent(int agentId) {
        List<Task> list = new ArrayList<>();
        String sql = "SELECT t.*, a.name AS agent_name FROM tasks t " +
                     "LEFT JOIN agents a ON t.agent_id = a.id " +
                     "WHERE t.agent_id = ? ORDER BY t.created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, agentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Task task = mapTask(rs);
                task.setAgentName(rs.getString("agent_name"));
                list.add(task);
            }
        } catch (SQLException e) {
            System.err.println("getTasksByAgent error: " + e.getMessage());
        }
        return list;
    }

    /** Returns the count of active (IN_PROGRESS) tasks */
    public int getActiveTaskCount() {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COUNT(*) AS cnt FROM tasks WHERE status = 'IN_PROGRESS'")) {
            if (rs.next()) return rs.getInt("cnt");
        } catch (SQLException e) {
            System.err.println("getActiveTaskCount error: " + e.getMessage());
        }
        return 0;
    }

    /** Inserts a new task and returns its ID */
    public int createTask(String title, String description, int agentId,
                          String priority, java.sql.Date deadline, int createdBy) {
        String sql = "INSERT INTO tasks (title, description, agent_id, status, priority, " +
                     "deadline, created_by) VALUES (?,?,'IN_PROGRESS',?,?,?,?)";
        // Fix column order
        sql = "INSERT INTO tasks (title, description, agent_id, status, priority, deadline, created_by) " +
              "VALUES (?,?,?,'ASSIGNED',?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setInt(3, agentId);
            ps.setString(4, priority);
            if (deadline != null) ps.setDate(5, deadline); else ps.setNull(5, Types.DATE);
            ps.setInt(6, createdBy);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            System.err.println("createTask error: " + e.getMessage());
        }
        return -1;
    }

    /** Moves a task to a new status column */
    public void updateTaskStatus(int taskId, String newStatus) {
        String sql;
        if ("DONE".equals(newStatus) || "FAILED".equals(newStatus)) {
            sql = "UPDATE tasks SET status = ?, completed_at = CURRENT_TIMESTAMP WHERE id = ?";
        } else {
            sql = "UPDATE tasks SET status = ? WHERE id = ?";
        }
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("updateTaskStatus error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  GOAL METHODS
    // ═══════════════════════════════════════════════════════

    /** Returns all goals for a given department */
    public List<Goal> getGoalsByDepartment(int departmentId) {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT * FROM goals WHERE department_id = ? ORDER BY deadline";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, departmentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapGoal(rs));
        } catch (SQLException e) {
            System.err.println("getGoalsByDepartment error: " + e.getMessage());
        }
        return list;
    }

    /** Returns all goals across all departments */
    public List<Goal> getAllGoals() {
        List<Goal> list = new ArrayList<>();
        String sql = "SELECT g.*, d.name AS dept_name FROM goals g " +
                     "LEFT JOIN departments d ON g.department_id = d.id " +
                     "ORDER BY g.deadline";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Goal g = mapGoal(rs);
                g.setDepartmentName(rs.getString("dept_name"));
                list.add(g);
            }
        } catch (SQLException e) {
            System.err.println("getAllGoals error: " + e.getMessage());
        }
        return list;
    }

    /** Updates a goal's current progress value */
    public void updateGoalProgress(int goalId, double newValue) {
        String sql = "UPDATE goals SET current_value = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, newValue);
            ps.setInt(2, goalId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("updateGoalProgress error: " + e.getMessage());
        }
    }

    /** Inserts a new goal */
    public void createGoal(int departmentId, String title, double target, java.sql.Date deadline) {
        String sql = "INSERT INTO goals (department_id, title, target_value, deadline) VALUES (?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, departmentId);
            ps.setString(2, title);
            ps.setDouble(3, target);
            if (deadline != null) ps.setDate(4, deadline); else ps.setNull(4, Types.DATE);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("createGoal error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  APPROVAL METHODS
    // ═══════════════════════════════════════════════════════

    /** Returns all approvals (pending ones first) with agent name joined in */
    public List<Approval> getAllApprovals() {
        List<Approval> list = new ArrayList<>();
        String sql = "SELECT ap.*, ag.name AS agent_name, d.name AS dept_name " +
                     "FROM approvals ap " +
                     "LEFT JOIN agents ag ON ap.agent_id = ag.id " +
                     "LEFT JOIN departments d ON ag.department_id = d.id " +
                     "ORDER BY CASE ap.status WHEN 'PENDING' THEN 0 ELSE 1 END, ap.created_at DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Approval a = mapApproval(rs);
                a.setAgentName(rs.getString("agent_name"));
                a.setDepartmentName(rs.getString("dept_name"));
                list.add(a);
            }
        } catch (SQLException e) {
            System.err.println("getAllApprovals error: " + e.getMessage());
        }
        return list;
    }

    /** Returns only the PENDING approvals */
    public List<Approval> getPendingApprovals() {
        List<Approval> list = new ArrayList<>();
        String sql = "SELECT ap.*, ag.name AS agent_name, d.name AS dept_name " +
                     "FROM approvals ap " +
                     "LEFT JOIN agents ag ON ap.agent_id = ag.id " +
                     "LEFT JOIN departments d ON ag.department_id = d.id " +
                     "WHERE ap.status = 'PENDING' ORDER BY ap.created_at DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Approval a = mapApproval(rs);
                a.setAgentName(rs.getString("agent_name"));
                a.setDepartmentName(rs.getString("dept_name"));
                list.add(a);
            }
        } catch (SQLException e) {
            System.err.println("getPendingApprovals error: " + e.getMessage());
        }
        return list;
    }

    /** Returns the count of pending approvals (shown on the dashboard KPI card) */
    public int getPendingApprovalCount() {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COUNT(*) AS cnt FROM approvals WHERE status = 'PENDING'")) {
            if (rs.next()) return rs.getInt("cnt");
        } catch (SQLException e) {
            System.err.println("getPendingApprovalCount error: " + e.getMessage());
        }
        return 0;
    }

    /** Records an approval decision (APPROVED or REJECTED) */
    public void decideApproval(int approvalId, String decision, int decidedBy) {
        String sql = "UPDATE approvals SET status = ?, decided_by = ?, " +
                     "decided_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, decision);
            ps.setInt(2, decidedBy);
            ps.setInt(3, approvalId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("decideApproval error: " + e.getMessage());
        }
    }

    /** Inserts a new approval request (called by SimulationEngine) */
    public void createApproval(int agentId, String actionType, String payload) {
        String sql = "INSERT INTO approvals (agent_id, action_type, payload) VALUES (?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, agentId);
            ps.setString(2, actionType);
            ps.setString(3, payload);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("createApproval error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  ALERT METHODS
    // ═══════════════════════════════════════════════════════

    /** Returns all alerts, unacknowledged ones first */
    public List<Alert> getAllAlerts() {
        List<Alert> list = new ArrayList<>();
        String sql = "SELECT * FROM alerts " +
                     "ORDER BY acknowledged ASC, created_at DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapAlert(rs));
        } catch (SQLException e) {
            System.err.println("getAllAlerts error: " + e.getMessage());
        }
        return list;
    }

    /** Returns the count of unacknowledged alerts */
    public int getUnacknowledgedAlertCount() {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COUNT(*) AS cnt FROM alerts WHERE acknowledged = 0")) {
            if (rs.next()) return rs.getInt("cnt");
        } catch (SQLException e) {
            System.err.println("getUnacknowledgedAlertCount error: " + e.getMessage());
        }
        return 0;
    }

    /** Marks an alert as acknowledged */
    public void acknowledgeAlert(int alertId) {
        String sql = "UPDATE alerts SET acknowledged = 1 WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, alertId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("acknowledgeAlert error: " + e.getMessage());
        }
    }

    /** Inserts a new alert */
    public void createAlert(String severity, String message, String relatedEntity) {
        String sql = "INSERT INTO alerts (severity, message, related_entity) VALUES (?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, severity);
            ps.setString(2, message);
            ps.setString(3, relatedEntity);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("createAlert error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  ACTIVITY LOG METHODS
    // ═══════════════════════════════════════════════════════

    /** Returns the most recent 200 activity log entries */
    public List<ActivityLog> getRecentActivityLogs() {
        List<ActivityLog> list = new ArrayList<>();
        String sql = "SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 200";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapActivityLog(rs));
        } catch (SQLException e) {
            System.err.println("getRecentActivityLogs error: " + e.getMessage());
        }
        return list;
    }

    /** Inserts an activity log entry. Call this whenever anything notable happens. */
    public void logActivity(String actorType, int actorId, String action, String details) {
        String sql = "INSERT INTO activity_logs (actor_type, actor_id, action, details) VALUES (?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, actorType);
            ps.setInt(2, actorId);
            ps.setString(3, action);
            ps.setString(4, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("logActivity error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  MESSAGE METHODS
    // ═══════════════════════════════════════════════════════

    /** Returns messages between a user and an agent */
    public List<Message> getMessageThread(int userId, int agentId) {
        List<Message> list = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE " +
                     "(sender_type='USER' AND sender_id=? AND recipient_type='AGENT' AND recipient_id=?) OR " +
                     "(sender_type='AGENT' AND sender_id=? AND recipient_type='USER' AND recipient_id=?) " +
                     "ORDER BY sent_at ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);  ps.setInt(2, agentId);
            ps.setInt(3, agentId); ps.setInt(4, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapMessage(rs));
        } catch (SQLException e) {
            System.err.println("getMessageThread error: " + e.getMessage());
        }
        return list;
    }

    /** Saves a message to the database */
    public void sendMessage(String senderType, int senderId,
                            String recipientType, int recipientId, String body) {
        String sql = "INSERT INTO messages (sender_type, sender_id, recipient_type, recipient_id, body) " +
                     "VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, senderType);
            ps.setInt(2, senderId);
            ps.setString(3, recipientType);
            ps.setInt(4, recipientId);
            ps.setString(5, body);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("sendMessage error: " + e.getMessage());
        }
    }

    /** Updates a user's display name */
    public void updateUserName(int userId, String name) {
        String sql = "UPDATE users SET name=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("updateUserName error: " + e.getMessage());
        }
    }

    /** Updates a user's password hash */
    public void updateUserPassword(int userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("updateUserPassword error: " + e.getMessage());
        }
    }

    /** Returns up to {@code limit} most-recent activity log entries */
    public List<ActivityLog> getActivityLogs(int limit) {
        List<ActivityLog> list = new ArrayList<>();
        String sql = "SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapActivityLog(rs));
        } catch (SQLException e) {
            System.err.println("getActivityLogs error: " + e.getMessage());
        }
        return list;
    }

    /** Deletes all rows from the alerts table */
    public void clearAllAlerts() {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("DELETE FROM alerts");
        } catch (SQLException e) {
            System.err.println("clearAllAlerts error: " + e.getMessage());
        }
    }

    /** Deletes all rows from the activity_logs table */
    public void clearActivityLog() {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("DELETE FROM activity_logs");
        } catch (SQLException e) {
            System.err.println("clearActivityLog error: " + e.getMessage());
        }
    }

    /** Sets every agent's status back to IDLE and clears current_task */
    public void resetAllAgentStatuses() {
        String sql = "UPDATE agents SET status='IDLE', current_task=NULL";
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("resetAllAgentStatuses error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  ROW MAPPERS (ResultSet → Model object)
    // ═══════════════════════════════════════════════════════

    /** Reads one row from a ResultSet and turns it into a User object */
    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("role"),
            rs.getTimestamp("created_at")
        );
    }

    /** Reads one row and turns it into a Department object */
    private Department mapDepartment(ResultSet rs) throws SQLException {
        return new Department(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("industry_template"),
            rs.getDouble("budget"),
            rs.getTimestamp("created_at")
        );
    }

    /** Reads one row and turns it into an Agent object */
    private Agent mapAgent(ResultSet rs) throws SQLException {
        int managerIdRaw = rs.getInt("manager_agent_id");
        Integer managerId = rs.wasNull() ? null : managerIdRaw;
        return new Agent(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("role_type"),
            rs.getInt("department_id"),
            managerId,
            rs.getString("status"),
            rs.getDouble("performance_score"),
            rs.getInt("autonomy_level"),
            rs.getString("current_task"),
            rs.getTimestamp("created_at")
        );
    }

    /** Reads one row and turns it into a Task object */
    private Task mapTask(ResultSet rs) throws SQLException {
        return new Task(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getInt("agent_id"),
            rs.getString("status"),
            rs.getString("priority"),
            rs.getDate("deadline"),
            rs.getInt("created_by"),
            rs.getTimestamp("completed_at"),
            rs.getTimestamp("created_at")
        );
    }

    /** Reads one row and turns it into a Goal object */
    private Goal mapGoal(ResultSet rs) throws SQLException {
        return new Goal(
            rs.getInt("id"),
            rs.getInt("department_id"),
            rs.getString("title"),
            rs.getDouble("target_value"),
            rs.getDouble("current_value"),
            rs.getDate("deadline")
        );
    }

    /** Reads one row and turns it into an Approval object */
    private Approval mapApproval(ResultSet rs) throws SQLException {
        int decidedByRaw = rs.getInt("decided_by");
        Integer decidedBy = rs.wasNull() ? null : decidedByRaw;
        return new Approval(
            rs.getInt("id"),
            rs.getInt("agent_id"),
            rs.getString("action_type"),
            rs.getString("payload"),
            rs.getString("status"),
            decidedBy,
            rs.getTimestamp("decided_at"),
            rs.getTimestamp("created_at")
        );
    }

    /** Reads one row and turns it into an Alert object */
    private Alert mapAlert(ResultSet rs) throws SQLException {
        return new Alert(
            rs.getInt("id"),
            rs.getString("severity"),
            rs.getString("message"),
            rs.getString("related_entity"),
            rs.getInt("acknowledged") == 1,
            rs.getTimestamp("created_at")
        );
    }

    /** Reads one row and turns it into a Message object */
    private Message mapMessage(ResultSet rs) throws SQLException {
        return new Message(
            rs.getInt("id"),
            rs.getString("sender_type"),
            rs.getInt("sender_id"),
            rs.getString("recipient_type"),
            rs.getInt("recipient_id"),
            rs.getString("body"),
            rs.getTimestamp("sent_at")
        );
    }

    /** Reads one row and turns it into an ActivityLog object */
    private ActivityLog mapActivityLog(ResultSet rs) throws SQLException {
        return new ActivityLog(
            rs.getInt("id"),
            rs.getString("actor_type"),
            rs.getInt("actor_id"),
            rs.getString("action"),
            rs.getString("details"),
            rs.getTimestamp("timestamp")
        );
    }

    // ═══════════════════════════════════════════════════════
    //  UTILITY
    // ═══════════════════════════════════════════════════════

    /**
     * Simple MD5 hash for passwords.
     * NOTE: In a real production app you'd use BCrypt — MD5 is fine for a demo.
     */
    private String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return input; // fallback — shouldn't happen
        }
    }

    /** Closes the database connection when the app shuts down */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database: " + e.getMessage());
        }
    }
}
