package com.example.ethiospand;

import androidx.annotation.NonNull;

public class Transaction {
    public String type;
    public String amount;
    public String person;
    public String date;
    public String time;
    public String balance;
    public String refNo;

    public Transaction(String type, String amount, String person, String date, String time, String balance, String refNo) {
        this.type = type;
        this.amount = amount;
        this.person = person;
        this.date = date;
        this.time = time;
        this.balance = balance;
        this.refNo = refNo;
    }

    @NonNull
    @Override
    public String toString() {
        return "Type: " + type + 
               "\nAmount: ETB " + amount + 
               "\nParty: " + person + 
               "\nDate: " + date + " " + time +
               "\nBalance: ETB " + balance +
               "\nRef: " + refNo;
    }
}
