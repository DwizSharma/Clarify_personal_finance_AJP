package com.financemanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name="recurring_bills")
public class RecurringBill {
    public enum Frequency { WEEKLY, MONTHLY, YEARLY }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String billName;
    @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
    @Column(nullable=false) private LocalDate nextDueDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Frequency frequency=Frequency.MONTHLY;
    @Column(nullable=false) private boolean active=true;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private User user;
    public RecurringBill(){}
    public Long getId(){return id;} public String getBillName(){return billName;} public void setBillName(String v){billName=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public LocalDate getNextDueDate(){return nextDueDate;} public void setNextDueDate(LocalDate v){nextDueDate=v;}
    public Frequency getFrequency(){return frequency;} public void setFrequency(Frequency v){frequency=v;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public User getUser(){return user;} public void setUser(User v){user=v;}
}
