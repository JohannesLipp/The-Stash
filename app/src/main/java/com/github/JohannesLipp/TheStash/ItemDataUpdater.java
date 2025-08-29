package com.github.JohannesLipp.TheStash;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;

public class ItemDataUpdater {

    private static final String TAG = "ItemDataUpdater";

    public static boolean downloadFoodDataAndImage(FoodItem foodItem, AppDatabase database, Activity callbackActivity, FoodAdapter adapter) {
        Log.d(TAG, "Starting download for item ID: " + foodItem.getId() + ", Barcode: " + foodItem.getBarcode());
        OpenFoodFacts.fetchProductData(
                foodItem,
                Executors.newSingleThreadExecutor(),
                new ProductDataCallbackHandler(foodItem, database, callbackActivity, adapter)
        );
        return true;
    }

    private static class ProductDataCallbackHandler implements OpenFoodFacts.ProductDataCallback {
        private final FoodItem itemToUpdate;
        private final AppDatabase database;
        private final Activity callbackActivity;
        private final FoodAdapter adapter;

        public ProductDataCallbackHandler(FoodItem itemToUpdate, AppDatabase database, Activity callbackActivity, FoodAdapter adapter) {
            this.itemToUpdate = itemToUpdate;
            this.database = database;
            this.callbackActivity = callbackActivity;
            this.adapter = adapter;
        }

        @Override
        public void onSuccess(OpenFoodFactsResultDTO productData) {
            Log.d(TAG, "Download successful, updating DB & UI for: " + productData.toString());

            // Update database entry (product_name_de, or product_name as fallback; brands, image_url), and update the recyclerview if necessary
            new Thread(() -> {
                itemToUpdate.setName(productData.getProductName());
                itemToUpdate.setBrands(productData.getBrands());
                itemToUpdate.setImageUrl(productData.getImageUrl());

                database.foodItemDao().update(itemToUpdate); // Save the updated item to the database
                Log.d(TAG, "Updated item in DB: " + itemToUpdate);

                // Update RecyclerView with latest database state
                List<FoodItem> foodItems = database.foodItemDao().getAllItemsSorted();
                Log.d(TAG, "Latest food items from the database: " + foodItems);
                callbackActivity.runOnUiThread(() -> {
                    adapter.setItems(foodItems);
                    Log.d(TAG, "RecyclerView updated with latest data.");
                });

                // In parallel to the UI update, download and store item image if available
                if (itemToUpdate.getImageUrl() != null && !itemToUpdate.getImageUrl().isEmpty()) {
                    Log.d(TAG, "Downloading image for item: " + itemToUpdate);
                    downloadAndStoreImage(itemToUpdate, database, callbackActivity, adapter);
                } else {
                    Log.d(TAG, "No image URL available for item: " + itemToUpdate.getName());
                }
            }).start();
        }

        @Override
        public void onError(String errorMessage) {
            Log.e("MainActivity", "Download failed. Error: " + errorMessage);
            callbackActivity.runOnUiThread(() -> Toast.makeText(callbackActivity, "Failed to get details: " + errorMessage.substring(0, Math.min(errorMessage.length(), 50)), Toast.LENGTH_LONG).show());
        }
    }

    /**
     * Downloads an image from the given URL and stores it in the database. Must be called from
     * a background thread.
     *
     * @param itemToUpdateWithImage
     * @param database
     * @param callbackActivity
     * @param adapter
     */
    private static void downloadAndStoreImage(FoodItem itemToUpdateWithImage, AppDatabase database, Activity callbackActivity, FoodAdapter adapter) {
        Log.d(TAG, "Downloading image for item: " + itemToUpdateWithImage);

        try {
            URL url = new URL(itemToUpdateWithImage.getImageUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            Log.d(TAG, "Connection established, downloading image...");
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();
            connection.disconnect();

            if (bitmap != null) {
                Log.d(TAG, "Image downloaded, converting to JPEG...");
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                byte[] imageByteArray = stream.toByteArray();
                stream.close();
                Log.d(TAG, "Image converted to JPEG, storing in DB...");

                itemToUpdateWithImage.setImageData(imageByteArray);
                database.foodItemDao().update(itemToUpdateWithImage);
                Log.d(TAG, "Image stored in DB for: " + itemToUpdateWithImage);

                // Update RecyclerView with latest database state
                List<FoodItem> foodItems = database.foodItemDao().getAllItemsSorted();
                Log.d(TAG, "Loaded latest food items from database: " + foodItems);

                callbackActivity.runOnUiThread(() -> {
                    adapter.setItems(foodItems);
                    Log.d(TAG, "RecyclerView updated with latest data.");
                });
            } else {
                Log.e(TAG, "Failed to decode bitmap from URL: " + itemToUpdateWithImage.getImageUrl());
            }
        } catch (IOException e) {
            Log.e(TAG, "Error downloading image: " + e.getMessage(), e);
            callbackActivity.runOnUiThread(() -> Toast.makeText(callbackActivity, "Failed to download image.", Toast.LENGTH_LONG).show());
        }
    }
}
