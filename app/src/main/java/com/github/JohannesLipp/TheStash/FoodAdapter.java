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

    public interface OnItemLongClickListener {
        boolean onItemLongClick(FoodItem item);
    }

    private List<FoodItem> items = new ArrayList<>();
    private final OnItemClickListener listener;
    private final OnItemLongClickListener longClickListener;

    public FoodAdapter(OnItemClickListener listener, OnItemLongClickListener longClickListener) {
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    public void setItems(List<FoodItem> items) {
        this.items = items;
        notifyDataSetChanged(); // Consider using DiffUtil for better performance
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
        holder.bind(item, listener, longClickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class FoodViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtBrand, txtBarcode, txtExpiry, txtCount;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            txtBrand = itemView.findViewById(R.id.txtBrand);
            txtBarcode = itemView.findViewById(R.id.txtBarcode);
            txtExpiry = itemView.findViewById(R.id.txtExpiry);
            txtCount = itemView.findViewById(R.id.txtCount);
        }

        public void bind(final FoodItem item, final OnItemClickListener listener, final OnItemLongClickListener longClickListener) {
            txtName.setText("Name:" +item.getName());
            txtBrand.setText("Brand:" +item.getBrands());
            txtBarcode.setText("Barcode:" +item.getBarcode());
            txtExpiry.setText("Expiry:" +item.getExpiryFormatted());
            txtCount.setText("Count:" + item.getCount());

            if (longClickListener != null) {
                itemView.setOnClickListener(v -> listener.onItemClick(item));
            } else {
                itemView.setOnClickListener(null); // Clear if no listener
            }

            if (longClickListener != null) {
                itemView.setOnLongClickListener(v -> longClickListener.onItemLongClick(item));
            } else {
                itemView.setOnLongClickListener(null); // Clear if no listener
            }

        }
    }
}