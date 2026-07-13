package com.example.ethiospand;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class TransactionEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String type;
    public String amount;
    public String person;
    public String date;
    public String time;
    public String balance;
    public String refNo;
    public String category;
    public String remarks; // Added remarks

    public TransactionEntity(String type, String amount, String person, String date, String time, String balance, String refNo, String category, String remarks) {
        this.type = type;
        this.amount = amount;
        this.person = person;
        this.date = date;
        this.time = time;
        this.balance = balance;
        this.refNo = refNo;
        this.category = category;
        this.remarks = remarks;
    }
}
