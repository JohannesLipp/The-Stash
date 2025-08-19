package com.github.JohannesLipp.TheStash;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFoodFactsResultDTO {
    @JsonProperty("product_name")
    private final String productName;
    @JsonProperty("product_name_de")
    private final String productNameDE;
    @JsonProperty("product_quantity")
    private final String productQuantity;
    @JsonProperty("brands")
    private final String brands;
    @JsonProperty("image_url")
    private final String imageUrl;

    @JsonCreator
    public OpenFoodFactsResultDTO(
            @JsonProperty("product_name") String productName,
            @JsonProperty("product_name_de") String productNameDE,
            @JsonProperty("product_quantity") String productQuantity,
            @JsonProperty("brands") String brands,
            @JsonProperty("image_url") String imageUrl
    ) {
        this.productName = productName;
        this.productNameDE = productNameDE;
        this.productQuantity = productQuantity;
        this.brands = brands;
        this.imageUrl = imageUrl;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductNameDE() {
        return productNameDE;
    }

    public String getProductQuantity() {
        return productQuantity;
    }

    public String getBrands() {
        return brands;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    @Override
    public String toString() {
        return "OpenFoodFactsResultDTO{" +
                "productName='" + productName + '\'' +
                ", productNameDE='" + productNameDE + '\'' +
                ", productQuantity='" + productQuantity + '\'' +
                ", brands='" + brands + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
