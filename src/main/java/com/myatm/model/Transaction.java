package com.myatm.model;

import java.time.LocalDateTime;
//import java.sql.Timestamp;

public class Transaction {

    private LocalDateTime date;
    private String type;
    private double amount;

    // Constructor pentru tranzacții din DB
    public Transaction(LocalDateTime date, String type, double amount) {
        this.date = date;
        this.type = type;
        this.amount = amount;
    }

    // Constructor pentru tranzacții noi
    public Transaction(String type, double amount) {
        this(LocalDateTime.now(), type, amount);
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return date + " | " + type + " | " + amount + " RON";
    }
}
