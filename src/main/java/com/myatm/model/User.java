package com.myatm.model;


public class User {
    private int id;
    private String username;
    private String pin;
    private double balance;

    public User(int id,String username, String pin, double balance){
        this.id=id;
        this.username = username;
        this.pin = pin;
        this.balance = balance;
    }

    public int getId(){ return id; }
    public String getUsername(){ return username; }
    public String getPin(){ return pin; }
    public double getBalance(){ return balance; }

    public void setBalance(double balance){
        this.balance = balance;
    }

    
}
