package com.example.ethiospand;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<TransactionEntity> transactions = new ArrayList<>();
    private OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(TransactionEntity transaction);
    }

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    public void setTransactions(List<TransactionEntity> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionEntity transaction = transactions.get(position);
        holder.tvTitle.setText(transaction.person);
        
        String subtitle = transaction.date + " • " + transaction.type;
        if (transaction.category != null && !transaction.category.isEmpty()) {
            subtitle += " • " + transaction.category;
        }
        holder.tvSubtitle.setText(subtitle);
        
        if (transaction.type.equalsIgnoreCase("Income")) {
            holder.tvAmount.setText("+ETB " + transaction.amount);
            holder.tvAmount.setTextColor(Color.parseColor("#027A48"));
            holder.ivIcon.setImageResource(android.R.drawable.ic_input_add);
            holder.ivIcon.setColorFilter(Color.parseColor("#027A48"));
        } else {
            holder.tvAmount.setText("-ETB " + transaction.amount);
            holder.tvAmount.setTextColor(Color.parseColor("#B42318"));
            holder.ivIcon.setImageResource(android.R.drawable.ic_delete);
            holder.ivIcon.setColorFilter(Color.parseColor("#B42318"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTransactionClick(transaction);
            }
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvAmount;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            ivIcon = itemView.findViewById(R.id.ivIcon);
        }
    }
}
