package models;

import java.sql.Timestamp;

/**
 * Represents a chat message in the Communication Hub.
 * Messages can be sent between users and agents, or broadcast to a department.
 */
public class Message {

    private int id;
    private String senderType;     // "USER" or "AGENT"
    private int senderId;
    private String recipientType;  // "USER", "AGENT", or "DEPARTMENT"
    private int recipientId;
    private String body;
    private Timestamp sentAt;

    // Display-only fields
    private String senderName;
    private String recipientName;

    public Message() {}

    public Message(int id, String senderType, int senderId, String recipientType,
                   int recipientId, String body, Timestamp sentAt) {
        this.id            = id;
        this.senderType    = senderType;
        this.senderId      = senderId;
        this.recipientType = recipientType;
        this.recipientId   = recipientId;
        this.body          = body;
        this.sentAt        = sentAt;
    }

    // ── Getters ───────────────────────────────────────────────

    public int getId()                  { return id; }
    public String getSenderType()       { return senderType; }
    public int getSenderId()            { return senderId; }
    public String getRecipientType()    { return recipientType; }
    public int getRecipientId()         { return recipientId; }
    public String getBody()             { return body; }
    public Timestamp getSentAt()        { return sentAt; }
    public String getSenderName()       { return senderName; }
    public String getRecipientName()    { return recipientName; }

    // ── Setters ───────────────────────────────────────────────

    public void setId(int id)                           { this.id = id; }
    public void setSenderType(String senderType)        { this.senderType = senderType; }
    public void setSenderId(int senderId)               { this.senderId = senderId; }
    public void setRecipientType(String recipientType)  { this.recipientType = recipientType; }
    public void setRecipientId(int recipientId)         { this.recipientId = recipientId; }
    public void setBody(String body)                    { this.body = body; }
    public void setSentAt(Timestamp sentAt)             { this.sentAt = sentAt; }
    public void setSenderName(String senderName)        { this.senderName = senderName; }
    public void setRecipientName(String recipientName)  { this.recipientName = recipientName; }

    @Override
    public String toString() {
        return "Message{id=" + id + ", from=" + senderType + ":" + senderId + "}";
    }
}
