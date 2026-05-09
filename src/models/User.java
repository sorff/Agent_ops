package models;

import java.sql.Timestamp;

/**
 * Represents a human user of the AgentOps system.
 * Users are either ADMIN (full access) or SUPERVISOR (manages specific departments).
 */
public class User {

    private int id;
    private String name;
    private String email;
    private String passwordHash;
    private String role;          // "ADMIN" or "SUPERVISOR"
    private Timestamp createdAt;

    /** Empty constructor — needed when we build a User object field by field */
    public User() {}

    /** Full constructor — creates a User with every field filled in */
    public User(int id, String name, String email,
                String passwordHash, String role, Timestamp createdAt) {
        this.id           = id;
        this.name         = name;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.createdAt    = createdAt;
    }

    // ── Getters (read the value) ──────────────────────────────

    public int getId()              { return id; }
    public String getName()         { return name; }
    public String getEmail()        { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole()         { return role; }
    public Timestamp getCreatedAt() { return createdAt; }

    // ── Setters (change the value) ────────────────────────────

    public void setId(int id)                      { this.id = id; }
    public void setName(String name)               { this.name = name; }
    public void setEmail(String email)             { this.email = email; }
    public void setPasswordHash(String hash)       { this.passwordHash = hash; }
    public void setRole(String role)               { this.role = role; }
    public void setCreatedAt(Timestamp createdAt)  { this.createdAt = createdAt; }

    /** Convenience check — is this user an Admin? */
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    /** Returns a readable summary — helpful for debugging */
    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', role='" + role + "'}";
    }
}
