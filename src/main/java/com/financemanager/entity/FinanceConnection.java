package com.financemanager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "finance_connections")
public class FinanceConnection {
    public enum Type { BANK, PAYMENT_APP }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Type type;
    @Column(nullable = false, length = 80)
    private String provider;
    @Column(nullable = false, length = 100)
    private String displayName;
    @Column(length = 100)
    private String identifierHint;
    @Column(length = 180)
    private String note;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Long getId() { return id; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getIdentifierHint() { return identifierHint; }
    public void setIdentifierHint(String identifierHint) { this.identifierHint = identifierHint; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
