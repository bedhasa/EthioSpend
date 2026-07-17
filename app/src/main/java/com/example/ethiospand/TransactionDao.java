package com.example.ethiospand;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insert(TransactionEntity transaction);

    @Update
    void update(TransactionEntity transaction);

    @Query("SELECT * FROM transactions ORDER BY id DESC")
    List<TransactionEntity> getAllTransactions();

    @Query("SELECT COUNT(*) FROM transactions WHERE refNo = :refNo AND refNo IS NOT NULL AND refNo != '' AND refNo != 'N/A'")
    int countByReference(String refNo);

    @Query("SELECT COUNT(*) FROM transactions WHERE type = :type AND amount = :amount AND person = :person AND date = :date AND time = :time AND balance = :balance")
    int countByTransactionFingerprint(String type, String amount, String person, String date, String time, String balance);

    @Query("SELECT SUM(CAST(REPLACE(amount, ',', '') AS REAL)) FROM transactions WHERE type = 'Income'")
    double getTotalIncome();

    @Query("SELECT SUM(CAST(REPLACE(amount, ',', '') AS REAL)) FROM transactions WHERE type = 'Expense'")
    double getTotalExpense();
    
    @Query("SELECT balance FROM transactions ORDER BY id DESC LIMIT 1")
    String getLatestBalance();
}
