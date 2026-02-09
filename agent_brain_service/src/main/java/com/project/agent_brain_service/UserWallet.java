package com.project.agent_brain_service;

import java.util.ArrayList;
import java.util.List;

public class UserWallet {
    public String userId;
    public double balance;
    public List<String> transactionHistory = new ArrayList<>();

    public UserWallet(String userId, double initialBalance) {
        this.userId = userId;
        this.balance = initialBalance;
        this.transactionHistory.add("INIT: Account opened with ₹" + initialBalance);
    }

    // 🆕 ATOMIC TRANSACTION LOGIC
    public boolean deductAmount(double amount, String reason) {
        if (balance >= amount) {
            balance -= amount;
            transactionHistory.add("DEBIT: ₹" + amount + " for " + reason + ". Bal: ₹" + balance);
            return true;
        } else {
            transactionHistory.add("FAILED: Attempted ₹" + amount + " for " + reason + ". Insufficient Funds.");
            return false;
        }
    }
    
    public void refund(double amount, String reason) {
        balance += amount;
        transactionHistory.add("REFUND: ₹" + amount + " for " + reason);
    }
}