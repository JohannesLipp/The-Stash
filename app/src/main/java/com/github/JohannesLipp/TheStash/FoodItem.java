package com.github.JohannesLipp.TheStash;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Locale;

@Entity(tableName = "food_items")
public class FoodItem {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private String brands;
    private String imageUrl;
    private final String barcode;
    private final int expiryDay;
    private final int expiryMonth;
    private final int expiryYear;
    private final int count;
    private byte[] imageData;

    @Ignore
    public FoodItem(String barcode, int expiryDay, int expiryMonth, int expiryYear, int count) {
        this("", "", "", barcode, expiryDay, expiryMonth, expiryYear, count);
    }

    public FoodItem(String name, String brands, String imageUrl, String barcode, int expiryDay, int expiryMonth, int expiryYear, int count) {
        this.name = name;
        this.brands = brands;
        this.imageUrl = imageUrl;
        this.barcode = barcode;
        this.expiryDay = expiryDay;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.count = count;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBrands() {
        return brands;
    }

    public String getImageUrl() {
        return imageUrl;
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

    public String getExpiryFormatted() {
        return String.format(Locale.GERMANY, "%02d.%02d.%04d", expiryDay, expiryMonth, expiryYear);
    }

    public int getCount() {
        return count;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBrands(String brands) {
        this.brands = brands;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }
}