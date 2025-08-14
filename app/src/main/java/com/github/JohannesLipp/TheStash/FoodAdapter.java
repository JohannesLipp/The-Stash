package com.github.JohannesLipp.TheStash;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(FoodItem item);
    }

    private List<FoodItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public FoodAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<FoodItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food, parent, false);
        return new FoodViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        FoodItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class FoodViewHolder extends RecyclerView.ViewHolder {
        TextView txtBarcode, txtExpiry, txtQuantity;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            txtBarcode = itemView.findViewById(R.id.txtBarcode);
            txtExpiry = itemView.findViewById(R.id.txtExpiry);
            txtQuantity = itemView.findViewById(R.id.txtQuantity);
        }

        public void bind(FoodItem item, OnItemClickListener listener) {
            txtBarcode.setText("Barcode: " + item.getBarcode());
            txtExpiry.setText("Expires: " + item.getExpiryMonth() + "/" + item.getExpiryYear());
            txtQuantity.setText("Count: " + item.getQuantity());

            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}