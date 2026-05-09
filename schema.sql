-- ============================================================
-- AgentOps — Database Schema
-- Run with: mysql -u root -p < schema.sql
-- Or paste into MySQL Workbench / DBeaver
-- ============================================================

CREATE DATABASE IF NOT EXISTS agentops
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE agentops;

-- ── Users ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    email        VARCHAR(150)  UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role         ENUM('ADMIN','SUPERVISOR') NOT NULL DEFAULT 'SUPERVISOR',
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Departments ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS departments (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    description        TEXT,
    industry_template  VARCHAR(100),
    budget             DECIMAL(15,2) DEFAULT 0.00,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Agents ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS agents (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    role_type         ENUM('MANAGER','SPECIALIST') NOT NULL,
    department_id     INT,
    manager_agent_id  INT NULL,
    status            ENUM('ACTIVE','IDLE','PAUSED','ERROR') DEFAULT 'IDLE',
    performance_score DECIMAL(5,2) DEFAULT 0.00,
    autonomy_level    INT DEFAULT 3,
    current_task      VARCHAR(255),
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id)    REFERENCES departments(id) ON DELETE SET NULL,
    FOREIGN KEY (manager_agent_id) REFERENCES agents(id)      ON DELETE SET NULL
);

-- ── Agent Capabilities ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS agent_capabilities (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    agent_id         INT NOT NULL,
    capability_name  VARCHAR(100) NOT NULL,
    FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE
);

-- ── Tasks ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tasks (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    agent_id     INT,
    status       ENUM('BACKLOG','ASSIGNED','IN_PROGRESS',
                      'AWAITING_APPROVAL','DONE','FAILED') DEFAULT 'BACKLOG',
    priority     ENUM('LOW','MEDIUM','HIGH','URGENT')      DEFAULT 'MEDIUM',
    deadline     DATE,
    created_by   INT,
    completed_at TIMESTAMP NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (agent_id)   REFERENCES agents(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id)  ON DELETE SET NULL
);

-- ── Goals ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS goals (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    department_id  INT NOT NULL,
    title          VARCHAR(200) NOT NULL,
    target_value   DECIMAL(15,2) DEFAULT 100.00,
    current_value  DECIMAL(15,2) DEFAULT 0.00,
    deadline       DATE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
);

-- ── Approvals ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS approvals (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    agent_id     INT,
    action_type  VARCHAR(100) NOT NULL,
    payload      TEXT,
    status       ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
    decided_by   INT NULL,
    decided_at   TIMESTAMP NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (agent_id)   REFERENCES agents(id) ON DELETE SET NULL,
    FOREIGN KEY (decided_by) REFERENCES users(id)  ON DELETE SET NULL
);

-- ── Activity Logs ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS activity_logs (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    actor_type ENUM('USER','AGENT') NOT NULL,
    actor_id   INT NOT NULL,
    action     VARCHAR(200) NOT NULL,
    details    TEXT,
    timestamp  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Alerts ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alerts (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    severity        ENUM('INFO','WARNING','CRITICAL') NOT NULL DEFAULT 'INFO',
    message         TEXT NOT NULL,
    related_entity  VARCHAR(100),
    acknowledged    BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Messages ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS messages (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    sender_type     ENUM('USER','AGENT') NOT NULL,
    sender_id       INT NOT NULL,
    recipient_type  ENUM('USER','AGENT','DEPARTMENT') NOT NULL,
    recipient_id    INT NOT NULL,
    body            TEXT NOT NULL,
    sent_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Default Admin Account ────────────────────────────────────
-- Password is "admin123" — change this before any real deployment!
INSERT IGNORE INTO users (name, email, password_hash, role)
VALUES ('Admin User', 'admin@agentops.com',
        '0192023a7bbd73250516f069df18b500', 'ADMIN');

-- Default Supervisor
INSERT IGNORE INTO users (name, email, password_hash, role)
VALUES ('Sarah Mitchell', 'sarah@agentops.com',
        '0192023a7bbd73250516f069df18b500', 'SUPERVISOR');
