package com.project.agent_brain_service;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@NoArgsConstructor
@DynamoDbBean
public class UserWallet {
    public String userId;
    public double balance;
    public List<String> transactionHistory = new ArrayList<>();

    @DynamoDbPartitionKey
    public String getUserId() {
        return userId;
    }

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

    public void addFunds(double amount, String reason) {
        balance += amount;
        transactionHistory.add("CREDIT: ₹" + amount + " for " + reason + ". Bal: ₹" + balance);
    }
}