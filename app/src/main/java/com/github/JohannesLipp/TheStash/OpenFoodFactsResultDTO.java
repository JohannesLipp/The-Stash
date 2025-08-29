package com.github.JohannesLipp.TheStash;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFoodFactsResultDTO {
    @JsonIgnore
    private int id;

    @JsonProperty("product_name")
    private final String productName;
    @JsonProperty("product_name_de")
    private final String productNameDE;
    @JsonProperty("brands")
    private final String brands;
    @JsonProperty("image_url")
    private final String imageUrl;

    @JsonCreator
    public OpenFoodFactsResultDTO(
            @JsonProperty("product_name") String productName,
            @JsonProperty("product_name_de") String productNameDE,
            @JsonProperty("brands") String brands,
            @JsonProperty("image_url") String imageUrl
    ) {
        this.productName = productName;
        this.productNameDE = productNameDE;
        this.brands = brands;
        this.imageUrl = imageUrl;
    }

    public String getProductName() {
        if (productNameDE != null && !productNameDE.isEmpty()) {
            return productNameDE;
        } else {
            return productName;
        }
    }

    public String getBrands() {
        return brands;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    @Override
    public String toString() {
        return "OpenFoodFactsResultDTO{" +
                "productName='" + productName + '\'' +
                ", productNameDE='" + productNameDE + '\'' +
                ", brands='" + brands + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
