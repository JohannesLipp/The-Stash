package com.github.JohannesLipp.TheStash;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(tableName = "food_items")
public class FoodItem {
    @PrimaryKey(autoGenerate = true)
    @JsonProperty
    private long id;

    @JsonProperty
    private String name;
    @JsonProperty
    private String brands;
    @JsonProperty
    private String imageUrl;
    @JsonProperty
    private final String barcode;
    @JsonIgnore
    private final int expiryDay;
    @JsonIgnore
    private final int expiryMonth;
    @JsonIgnore
    private final int expiryYear;
    @JsonProperty
    private final int count;
    @JsonIgnore
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

    public long getId() {
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

    @JsonProperty("expires")
    public String getExpiryFormatted() {
        return String.format(Locale.GERMANY, Constants.DATE_FORMAT, expiryDay, expiryMonth, expiryYear);
    }

    public int getCount() {
        return count;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setId(long id) {
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

    @NonNull
    @Override
    public String toString() {
        return "FoodItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", brands='" + brands + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", barcode='" + barcode + '\'' +
                ", expires=" + getExpiryFormatted() +
                ", count=" + count +
                ", imageData(size)=" + (imageData == null ? "0" : imageData.length) + "(B)" +
                '}';
    }
}