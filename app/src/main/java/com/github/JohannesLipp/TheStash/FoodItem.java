package com.github.JohannesLipp.TheStash;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "food_items")
public class FoodItem {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private final String barcode;
    private final int expiryDay;
    private final int expiryMonth;
    private final int expiryYear;
    private int quantity;


    public FoodItem(String barcode, int expiryDay, int expiryMonth, int expiryYear, int quantity) {
        this.barcode = barcode;
        this.expiryDay = expiryDay;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.quantity = quantity;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBarcode() {
        return barcode;
    }

    public int getExpiryDay() {
        return expiryDay;
    }

    public int getExpiryMonth() {
        return expiryMonth;
    }

    public int getExpiryYear() {
        return expiryYear;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}